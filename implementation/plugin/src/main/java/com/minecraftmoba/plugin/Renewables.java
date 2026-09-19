package com.minecraftmoba.plugin;

import org.bukkit.*;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.persistence.PersistentDataType;
import java.io.*;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;

/**
 * Regenerative Sources seam. Renewal restores availability at a location and
 * grants nothing to anyone; see docs/proposals/2026-09-19-regenerative-sources-seam.md.
 *
 * Source tables ship empty. This registers sources it is given and never
 * generates them, because the depth gradient, regional tables, species and
 * cadence are all recorded [OPEN] in canon.
 *
 * Recovery is evaluated lazily on access, so cost follows interaction rather
 * than the number of sources in the world. All access is on the server thread.
 */
public final class Renewables implements Listener {
    public enum Type { CROP, ANIMAL, SWARM }

    /** Availability state only. Nothing here is an inventory, a drop or a reward. */
    public static final class Source {
        final String id; final Type type; final UUID world; final String kind;
        final int x, y, z, radius, capacity; final long recoverTicks;
        int available; long recoveringUntil;
        Source(String id, Type type, UUID world, int x, int y, int z,
               int radius, int capacity, long recoverTicks) {
            this(id, type, world, x, y, z, radius, capacity, recoverTicks, null);
        }
        Source(String id, Type type, UUID world, int x, int y, int z,
               int radius, int capacity, long recoverTicks, String kind) {
            this.kind = kind;
            this.id = id; this.type = type; this.world = world;
            this.x = x; this.y = y; this.z = z;
            this.radius = radius; this.capacity = capacity;
            this.recoverTicks = recoverTicks; this.available = capacity;
        }
        boolean contains(int bx, int by, int bz) {
            return Math.abs(bx - x) <= radius && Math.abs(by - y) <= radius && Math.abs(bz - z) <= radius;
        }
        public String id() { return id; }
        public Type type() { return type; }
        public int available() { return available; }
        public int capacity() { return capacity; }
        public String kind() { return kind; }
    }

    private final MobaPlugin plugin;
    private final NamespacedKey key;
    private final Map<String, Source> sources = new LinkedHashMap<>();
    private final Path csv;
    private long harvests, recoveries, recoveryChecks, depletions, harvestNanos, harvestCalls;
    private long restoresApplied, persistsDeferred;
    /** Sources whose chunk was not loaded when state changed or registration ran. */
    private final Set<String> pendingPersist = new HashSet<>();
    private final Set<String> pendingRestore = new HashSet<>();
    /** Must stay zero. A nonzero value means this became passive income. */
    private long grantedByRenewal;

    public Renewables(MobaPlugin plugin) {
        this.plugin = plugin;
        this.key = new NamespacedKey(plugin, "renewable_sources_v1");
        csv = plugin.getDataFolder().toPath().resolve("measurements")
                .resolve("renewables-" + Instant.now().toEpochMilli() + ".csv");
        try {
            Files.createDirectories(csv.getParent());
            Files.writeString(csv, "timestamp,sources,harvests,depletions,recoveries,recoveryChecks,"
                    + "harvestCalls,harvestMeanNs,grantedByRenewal\n");
        } catch (IOException ex) { throw new IllegalStateException("Cannot create renewables measurements", ex); }
        loadConfigured();
        long period = plugin.getConfig().getLong("renewables.sampleTicks");
        if (period <= 0) throw new IllegalArgumentException("renewables.sampleTicks must be positive");
        Bukkit.getScheduler().runTaskTimer(plugin, this::sample, period, period);
        long pt = plugin.getConfig().getLong("features.renewableParticles.ticks", 20L);
        if (pt > 0) Bukkit.getScheduler().runTaskTimer(plugin, this::particles, pt, pt);
    }

    /** Ships empty. An absent or empty list is the expected state, not an error. */
    private void loadConfigured() {
        var section = plugin.getConfig().getConfigurationSection("renewables.sources");
        if (section == null) return;
        for (String id : section.getKeys(false)) {
            String base = "renewables.sources." + id + ".";
            String worldName = plugin.getConfig().getString(base + "world");
            World w = worldName == null ? null : Bukkit.getWorld(worldName);
            if (w == null) throw new IllegalArgumentException(base + "world is not a loaded world");
            Type type;
            try { type = Type.valueOf(plugin.getConfig().getString(base + "type", "").toUpperCase(Locale.ROOT)); }
            catch (IllegalArgumentException ex) { throw new IllegalArgumentException(base + "type must be CROP, ANIMAL or SWARM"); }
            int radius = plugin.getConfig().getInt(base + "radius");
            int capacity = plugin.getConfig().getInt(base + "capacity");
            long recover = plugin.getConfig().getLong(base + "recoverTicks");
            if (radius <= 0 || capacity <= 0 || recover <= 0)
                throw new IllegalArgumentException(base + "radius, capacity and recoverTicks must be positive");
            String kind = plugin.getConfig().getString(base + "kind");
            if (kind != null) RenewableKinds.require(kind);   // fail loudly on an unknown kind
            register(new Source(id, type, w.getUID(),
                    plugin.getConfig().getInt(base + "x"), plugin.getConfig().getInt(base + "y"),
                    plugin.getConfig().getInt(base + "z"), radius, capacity, recover, kind));
        }
    }

    public void register(Source s) {
        if (sources.putIfAbsent(s.id, s) != null) throw new IllegalArgumentException("duplicate source id " + s.id);
        // onEnable runs before any chunk is loaded, so saved state cannot be read
        // yet. Defer to ChunkLoadEvent rather than force-loading during startup.
        if (!restore(s)) pendingRestore.add(s.id);
    }

    /** Saved state only becomes readable once the owning chunk loads. */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onChunkLoad(ChunkLoadEvent e) {
        Chunk c = e.getChunk();
        for (Source s : sources.values()) {
            if (!s.world.equals(c.getWorld().getUID())) continue;
            if ((s.x >> 4) != c.getX() || (s.z >> 4) != c.getZ()) continue;
            if (pendingRestore.remove(s.id) && restore(s)) restoresApplied++;
            if (pendingPersist.remove(s.id)) persist(s);
        }
    }

    public Collection<Source> sources() { return Collections.unmodifiableCollection(sources.values()); }

    /** Runtime registration for the authoring commands. */
    public Source createRuntime(String id, String kind, World w, int x, int y, int z,
                                int radius, int capacity, long recoverTicks) {
        var k = RenewableKinds.require(kind);
        var s = new Source(id, k.type(), w.getUID(), x, y, z, radius, capacity, recoverTicks, kind);
        register(s);
        return s;
    }

    public boolean remove(String id) { return sources.remove(id) != null; }

    /**
     * Lazily settles recovery, then reports availability. Recovery restores the
     * opportunity at the location and writes nothing to any player.
     */
    public int available(Source s) {
        recoveryChecks++;
        if (s.available < s.capacity && s.recoveringUntil > 0
                && s.world != null && Bukkit.getWorld(s.world) != null
                && Bukkit.getWorld(s.world).getFullTime() >= s.recoveringUntil) {
            s.available = s.capacity;
            s.recoveringUntil = 0;
            recoveries++;
            persist(s);
        }
        return s.available;
    }

    private Optional<Source> at(World w, int x, int y, int z, Type type) {
        for (Source s : sources.values())
            if (s.type == type && s.world.equals(w.getUID()) && s.contains(x, y, z)) return Optional.of(s);
        return Optional.empty();
    }

    /** Returns true when the harvest counted against a source. */
    private boolean harvest(Source s) {
        if (available(s) <= 0) return false;
        s.available--;
        harvests++;
        if (s.available == 0) {
            depletions++;
            World w = Bukkit.getWorld(s.world);
            s.recoveringUntil = (w == null ? 0 : w.getFullTime()) + s.recoverTicks;
        }
        persist(s);
        return true;
    }

    // HIGHEST, not MONITOR: Provenance clears the placed mark in its own MONITOR
    // handler, and it is registered first, so a MONITOR read here sees every
    // player-placed block as unmarked and counts a farm as a wild patch.
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent e) {
        long t0 = System.nanoTime();
        try {
            Block b = e.getBlock();
            // A player's own farm is not a wild patch. This is why the seam
            // depends on the Phase 1 provenance result rather than assuming it.
            if (plugin.provenance().isPlayerPlaced(b)) return;
            // Vanilla's own crop tag, not an invented species list. Which further
            // plant resources count is content and stays [OPEN].
            at(b.getWorld(), b.getX(), b.getY(), b.getZ(), Type.CROP).ifPresent(s -> {
                // A kinded source counts only its own materials; an unkinded one
                // falls back to vanilla's crop tag.
                if (s.kind != null) {
                    if (!RenewableKinds.require(s.kind).blocks().contains(b.getType())) return;
                } else if (!Tag.CROPS.isTagged(b.getType())) return;
                harvest(s);
            });
        } finally { harvestCalls++; harvestNanos += System.nanoTime() - t0; }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(EntityDeathEvent e) {
        long t0 = System.nanoTime();
        try {
            LivingEntity victim = e.getEntity();
            if (victim.getKiller() == null) return;   // only player-caused harvest counts
            var loc = victim.getLocation();
            at(loc.getWorld(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(),
                    victim instanceof Monster ? Type.SWARM : Type.ANIMAL).ifPresent(s -> {
                if (s.kind != null && !RenewableKinds.require(s.kind).entities().contains(victim.getType())) return;
                harvest(s);
            });
        } finally { harvestCalls++; harvestNanos += System.nanoTime() - t0; }
    }

    // ---- persistence: chunk PDC, matching Provenance ----

    /** Never force-loads: an absent chunk means the answer is not available yet. */
    private Chunk loadedChunkOf(Source s) {
        World w = Bukkit.getWorld(s.world);
        if (w == null) return null;
        int cx = s.x >> 4, cz = s.z >> 4;
        return w.isChunkLoaded(cx, cz) ? w.getChunkAt(cx, cz) : null;
    }

    private void persist(Source s) {
        Chunk c = loadedChunkOf(s);
        // Losing a write would silently hand out free harvests after a restart.
        if (c == null) { pendingPersist.add(s.id); persistsDeferred++; return; }
        var pdc = c.getPersistentDataContainer();
        var existing = pdc.get(key, PersistentDataType.BYTE_ARRAY);
        Map<String, long[]> state = decode(existing);
        state.put(s.id, new long[]{ s.available, s.recoveringUntil });
        pdc.set(key, PersistentDataType.BYTE_ARRAY, encode(state));
    }

    /** Returns true when the chunk was available to read, saved state or not. */
    private boolean restore(Source s) {
        Chunk c = loadedChunkOf(s);
        if (c == null) return false;
        var payload = c.getPersistentDataContainer().get(key, PersistentDataType.BYTE_ARRAY);
        long[] saved = decode(payload).get(s.id);
        if (saved == null) return true;
        s.available = (int) Math.max(0, Math.min(s.capacity, saved[0]));
        s.recoveringUntil = saved[1];
        return true;
    }

    static Map<String, long[]> decode(byte[] payload) {
        Map<String, long[]> out = new LinkedHashMap<>();
        if (payload == null || payload.length == 0) return out;
        try (var in = new DataInputStream(new ByteArrayInputStream(payload))) {
            int n = in.readInt();
            for (int i = 0; i < n; i++) out.put(in.readUTF(), new long[]{ in.readLong(), in.readLong() });
        } catch (IOException ex) { throw new IllegalStateException("corrupt renewables payload", ex); }
        return out;
    }

    static byte[] encode(Map<String, long[]> state) {
        var bytes = new ByteArrayOutputStream();
        try (var out = new DataOutputStream(bytes)) {
            out.writeInt(state.size());
            for (var e : state.entrySet()) {
                out.writeUTF(e.getKey());
                out.writeLong(e.getValue()[0]);
                out.writeLong(e.getValue()[1]);
            }
        } catch (IOException ex) { throw new IllegalStateException(ex); }
        return bytes.toByteArray();
    }

    /** Legibility, not decoration: a depleted source shows nothing. */
    private void particles() {
        if (!plugin.getConfig().getBoolean("features.renewableParticles.enabled")) return;
        for (Source s : sources.values()) {
            World w = Bukkit.getWorld(s.world);
            if (w == null || !w.isChunkLoaded(s.x >> 4, s.z >> 4)) continue;
            int avail = available(s);
            if (avail <= 0) continue;
            int points = Math.max(4, (int) Math.round(
                    plugin.getConfig().getDouble("features.renewableParticles.pointsPerRing", 16)
                            * ((double) avail / s.capacity)));
            for (int i = 0; i < points; i++) {
                double angle = 2 * Math.PI * i / points;
                w.spawnParticle(Particle.END_ROD,
                        s.x + 0.5 + Math.cos(angle) * s.radius, s.y + 1.2,
                        s.z + 0.5 + Math.sin(angle) * s.radius, 1, 0, 0, 0, 0);
            }
        }
    }

    private void sample() {
        // Settle recovery for every source so the sample reflects real state.
        for (Source s : sources.values()) available(s);
        String line = Instant.now() + "," + sources.size() + "," + harvests + "," + depletions + ","
                + recoveries + "," + recoveryChecks + "," + harvestCalls + ","
                + (harvestCalls == 0 ? 0 : harvestNanos / harvestCalls) + "," + grantedByRenewal + "\n";
        try { Files.writeString(csv, line, StandardOpenOption.APPEND); }
        catch (IOException ex) { plugin.getLogger().warning("renewables sample failed: " + ex); }
        if (grantedByRenewal != 0)
            plugin.getLogger().severe("INVARIANT BROKEN: renewal granted " + grantedByRenewal + " item(s)");
    }

    public long grantedByRenewal() { return grantedByRenewal; }

    /** One line per source, for acceptance assertions and operator inspection. */
    public List<String> report() {
        var out = new ArrayList<String>();
        for (Source s : sources.values())
            out.add("RENEWABLE " + s.id + " type=" + s.type + " kind=" + (s.kind == null ? "-" : s.kind)
                    + " available=" + available(s)
                    + "/" + s.capacity + " recoveringUntil=" + s.recoveringUntil);
        out.add("RENEWABLE_TOTALS sources=" + sources.size() + " harvests=" + harvests
                + " depletions=" + depletions + " recoveries=" + recoveries
                + " grantedByRenewal=" + grantedByRenewal);
        return out;
    }
}
