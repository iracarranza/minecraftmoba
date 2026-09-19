package com.minecraftmoba.plugin;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
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
        final String id; final Type type; final UUID world;
        final int x, y, z, radius, capacity; final long recoverTicks;
        int available; long recoveringUntil;
        Source(String id, Type type, UUID world, int x, int y, int z,
               int radius, int capacity, long recoverTicks) {
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
    }

    private final MobaPlugin plugin;
    private final NamespacedKey key;
    private final Map<String, Source> sources = new LinkedHashMap<>();
    private final Path csv;
    private long harvests, recoveries, recoveryChecks, depletions, harvestNanos, harvestCalls;
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
            register(new Source(id, type, w.getUID(),
                    plugin.getConfig().getInt(base + "x"), plugin.getConfig().getInt(base + "y"),
                    plugin.getConfig().getInt(base + "z"), radius, capacity, recover));
        }
    }

    public void register(Source s) {
        if (sources.putIfAbsent(s.id, s) != null) throw new IllegalArgumentException("duplicate source id " + s.id);
        restore(s);
    }

    public Collection<Source> sources() { return Collections.unmodifiableCollection(sources.values()); }

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

    private Optional<Source> at(World w, int x, int y, int z) {
        for (Source s : sources.values())
            if (s.world.equals(w.getUID()) && s.contains(x, y, z)) return Optional.of(s);
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

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent e) {
        long t0 = System.nanoTime();
        try {
            Block b = e.getBlock();
            // A player's own farm is not a wild patch. This is why the seam
            // depends on the Phase 1 provenance result rather than assuming it.
            if (plugin.provenance().isPlayerPlaced(b)) return;
            at(b.getWorld(), b.getX(), b.getY(), b.getZ())
                    .filter(s -> s.type == Type.CROP)
                    .ifPresent(this::harvest);
        } finally { harvestCalls++; harvestNanos += System.nanoTime() - t0; }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(EntityDeathEvent e) {
        long t0 = System.nanoTime();
        try {
            LivingEntity victim = e.getEntity();
            if (victim.getKiller() == null) return;   // only player-caused harvest counts
            var loc = victim.getLocation();
            at(loc.getWorld(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ())
                    .filter(s -> s.type == (victim instanceof Monster ? Type.SWARM : Type.ANIMAL))
                    .ifPresent(this::harvest);
        } finally { harvestCalls++; harvestNanos += System.nanoTime() - t0; }
    }

    // ---- persistence: chunk PDC, matching Provenance ----

    private Chunk chunkOf(Source s) {
        World w = Bukkit.getWorld(s.world);
        return w == null ? null : w.getChunkAt(s.x >> 4, s.z >> 4);
    }

    private void persist(Source s) {
        Chunk c = chunkOf(s);
        if (c == null) return;
        var pdc = c.getPersistentDataContainer();
        var existing = pdc.get(key, PersistentDataType.BYTE_ARRAY);
        Map<String, long[]> state = decode(existing);
        state.put(s.id, new long[]{ s.available, s.recoveringUntil });
        pdc.set(key, PersistentDataType.BYTE_ARRAY, encode(state));
    }

    private void restore(Source s) {
        Chunk c = chunkOf(s);
        if (c == null) return;
        var payload = c.getPersistentDataContainer().get(key, PersistentDataType.BYTE_ARRAY);
        long[] saved = decode(payload).get(s.id);
        if (saved == null) return;
        s.available = (int) Math.max(0, Math.min(s.capacity, saved[0]));
        s.recoveringUntil = saved[1];
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
}
