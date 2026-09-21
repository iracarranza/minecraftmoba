package com.minecraftmoba.plugin;

import org.bukkit.*;
import org.bukkit.Material;
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

    /**
     * The opportunity's lifecycle, stated rather than inferred.
     *
     * It was previously implied by two integers, so "is there a manifestation
     * standing in the world right now" had no answer that did not involve
     * counting things in a volume. That is the question the no-stacking rule
     * turns on, so it is now a field.
     */
    public enum State { MANIFESTED, DEPLETED, RECOVERING }

    /** Availability state only. Nothing here is an inventory, a drop or a reward. */
    public static final class Source {
        final String id; final Type type; final UUID world; final String kind;
        final int x, y, z, radius, capacity; final long recoverTicks;
        int available; long recoveringUntil;
        State state = State.RECOVERING;   // nothing has manifested yet
        /** Members spawned into the current manifestation, so ones that LEAVE can still be found. */
        final Set<UUID> members = new HashSet<>();
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
        public int x() { return x; }
        public int y() { return y; }
        public int z() { return z; }
        public int radius() { return radius; }
        public UUID world() { return world; }
        public State state() { return state; }
    }

    private final MobaPlugin plugin;
    private final NamespacedKey key;
    /** Stamped on every entity the system manifests, naming its opportunity. */
    private final NamespacedKey memberKey;
    private final Map<String, Source> sources = new LinkedHashMap<>();
    private final Path csv;
    private long harvests, recoveries, recoveryChecks, depletions, harvestNanos, harvestCalls;
    private long restoresApplied, persistsDeferred, remanifested;
    /** Sources whose chunk was not loaded when state changed or registration ran. */
    private final Set<String> pendingPersist = new HashSet<>();
    private final Set<String> pendingRestore = new HashSet<>();
    /** Sources whose chunk was not loaded when they were asked to manifest. */
    private final Set<String> pendingManifest = new HashSet<>();
    /** Must stay zero. A nonzero value means this became passive income. */
    private long grantedByRenewal;

    public Renewables(MobaPlugin plugin) {
        this.plugin = plugin;
        this.key = new NamespacedKey(plugin, "renewable_sources_v1");
        this.memberKey = new NamespacedKey(plugin, "manifestation_member");
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
            if (w == null) {
                // The Alpha instance is not loaded until a match opens, so a
                // source bound to it cannot resolve at startup. That is an
                // ordering fact rather than a bad config, and sources are
                // rebuilt once the world exists. A genuinely wrong world name
                // shows up here by never being bound.
                plugin.getLogger().warning("renewable source '" + id + "' references world '"
                        + worldName + "', which is not loaded; skipped for now");
                continue;
            }
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

    /**
     * Discard all match-scoped renewable state and rebuild from config.
     *
     * Two things make this necessary rather than optional. A source's
     * `available` and `recoveringUntil` live in the plugin, so without this a
     * depleted source stays depleted across a reset even though the world has
     * been restored pristine. And a restored world is a *new* world with a new
     * UUID, so sources bound to the old instance would no longer resolve.
     *
     * ALPHA-D2 specifies that reset clears all match-scoped plugin state, so
     * rebuilding to full capacity follows from the decision rather than
     * inventing a recovery rule.
     */
    public int resetForNewMatch() {
        sources.clear();
        pendingPersist.clear();
        pendingRestore.clear();
        pendingManifest.clear();
        harvests = recoveries = recoveryChecks = depletions = harvestNanos = harvestCalls = 0;
        loadConfigured();
        return sources.size();
    }

    /** Per-source availability, so depletion and recovery are observable. */
    public java.util.List<String> status() {
        var out = new java.util.ArrayList<String>();
        out.add("renewable sources=" + sources.size() + " harvests=" + harvests
                + " depletions=" + depletions + " recoveries=" + recoveries);
        for (Source s : sources.values())
            out.add("  " + s.id + " kind=" + s.kind + " available=" + available(s)
                    + "/" + s.capacity + " at " + s.x + "," + s.y + "," + s.z);
        return out;
    }

    public void register(Source s) {
        if (sources.putIfAbsent(s.id, s) != null) throw new IllegalArgumentException("duplicate source id " + s.id);
        // onEnable runs before any chunk is loaded, so saved state cannot be read
        // yet. Defer to ChunkLoadEvent rather than force-loading during startup.
        if (!restore(s)) pendingRestore.add(s.id);
        // A source is a claim about the world, not a counter. It was only ever
        // manifested on RECOVERY, which needs available < capacity -- so a fresh
        // source, constructed at full availability, never manifested at all.
        // Every animal pen in the Alpha map reported 6/6 while standing empty,
        // and nothing could be harvested, bred or marked in any of them.
        if (manifest(s) == 0 && !Bukkit.isPrimaryThread()) pendingManifest.add(s.id);
        else if (!manifested(s)) pendingManifest.add(s.id);
    }

    /** Whether the world actually holds what the source says it holds. */
    private boolean manifested(Source s) {
        if (s.kind == null) return true;
        World w = Bukkit.getWorld(s.world);
        if (w == null || !w.isChunkLoaded(s.x >> 4, s.z >> 4)) return false;
        return count(s, RenewableKinds.require(s.kind)) >= available(s);
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
            // The chunk is loaded now, so the pen can finally be filled.
            if (pendingManifest.contains(s.id) && manifest(s) > 0) pendingManifest.remove(s.id);
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
            s.state = State.RECOVERING;
            s.members.clear();
            recoveries++;
            persist(s);
            // Restore the opportunity itself, not only the counter.
            remanifested += manifest(s);
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
            s.state = State.DEPLETED;
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
            // Membership, not position. An ordinary animal a player kills to
            // stand in a region is not a harvest of that region's herd.
            String owner = victim.getPersistentDataContainer()
                    .get(memberKey, PersistentDataType.STRING);
            if (owner == null) return;
            Source s = sources.get(owner);
            if (s != null) harvest(s);
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

    /**
     * Physically realise a source up to its remaining availability.
     *
     * Availability is a counter; the opportunity is the thing in the world. A
     * recovered herd with no animals in it, or a recovered patch with no crops,
     * restores nothing a player can act on — canon's "renewal restores world
     * opportunity" means the crops and the animals come back, and the player
     * still has to go and take them.
     *
     * Tops up rather than replacing, so an author's own placement survives and
     * nothing is duplicated.
     */
    public int manifest(Source s) {
        World w = Bukkit.getWorld(s.world);
        if (w == null || !w.isChunkLoaded(s.x >> 4, s.z >> 4)) return 0;
        if (s.kind == null) return 0;
        var kind = RenewableKinds.require(s.kind);
        // ONE MANIFESTATION AT A TIME. This used to top up to capacity, so a
        // half-harvested herd was quietly refilled where it stood -- which makes
        // an ignored opportunity an animal printer and makes the region a camp
        // coordinate. A new manifestation is created only when there is no
        // current one; otherwise the standing manifestation IS the current one
        // and nothing happens.
        int present = count(s, kind);
        if (present > 0) { s.state = State.MANIFESTED; return 0; }
        int wanted = available(s);
        if (wanted <= 0) return 0;
        // SITE SELECTION IS NOT IMPLEMENTED. The manifestation appears at the
        // authored origin, which is exactly the fixed decorative spawn pad the
        // design rejects. It waits on two decisions that are architectural
        // rather than numeric -- what a region IS, and whether the map tool or
        // the plugin owns eligibility -- and is marked here so it cannot be
        // mistaken for the intended behaviour.
        // See docs/proposals/2026-09-21-regenerative-opportunity-model.md F.
        int made = kind.type() == Type.CROP ? placeCrops(w, s, kind, wanted)
                                            : spawnFauna(w, s, kind, wanted);
        if (made > 0) s.state = State.MANIFESTED;
        return made;
    }

    /** Whether this entity is a member of this opportunity's manifestation. */
    public boolean isMember(org.bukkit.entity.Entity e, Source s) {
        if (e == null) return false;
        String owner = e.getPersistentDataContainer().get(memberKey, PersistentDataType.STRING);
        return s.id.equals(owner);
    }

    /** Whether this entity belongs to ANY manifestation. */
    public boolean isMember(org.bukkit.entity.Entity e) {
        return e != null && e.getPersistentDataContainer().has(memberKey, PersistentDataType.STRING);
    }

    /**
     * Stop treating an entity as wild WITHOUT touching the entity.
     *
     * This is the whole point of explicit membership: a captured animal leaves
     * the wild population and stays alive, becoming an ordinary part of the
     * team's economy. Nothing is despawned, then or when the opportunity later
     * regenerates.
     */
    public void revoke(org.bukkit.entity.Entity e) {
        if (e != null) e.getPersistentDataContainer().remove(memberKey);
    }

    /**
     * What of this manifestation is still standing.
     *
     * Membership is explicit, never positional. Counting "entities of the right
     * type inside the radius" made a player's bred sheep a wild herd member, a
     * wild sheep that wandered 25 blocks not one, and any naturally spawned
     * zombie a member of a Swarm. Blocks use Provenance, which the repo already
     * has: a manifestation block is one the SYSTEM placed, so a player's farm
     * inside the region is excluded for free.
     */
    private int count(Source s, RenewableKinds.Kind kind) {
        World w = Bukkit.getWorld(s.world);
        if (w == null) return 0;
        if (kind.type() == Type.CROP) {
            int n = 0;
            for (int x = s.x - s.radius; x <= s.x + s.radius; x++)
                for (int y = s.y - s.radius; y <= s.y + s.radius; y++)
                    for (int z = s.z - s.radius; z <= s.z + s.radius; z++) {
                        Block b = w.getBlockAt(x, y, z);
                        if (!kind.blocks().contains(b.getType())) continue;
                        // A player-planted crop is production, not the wild
                        // patch, and counting it suppressed regeneration.
                        if (plugin.provenance() != null && plugin.provenance().isPlayerPlaced(b)) continue;
                        n++;
                    }
            return n;
        }
        int n = 0;
        for (var e : w.getNearbyEntities(new Location(w, s.x + 0.5, s.y + 0.5, s.z + 0.5),
                s.radius, s.radius, s.radius))
            if (isMember(e, s)) n++;
        return n;
    }

    /** Members of this opportunity currently inside its region. */
    public java.util.List<org.bukkit.entity.Entity> membersOf(Source s) {
        World w = Bukkit.getWorld(s.world);
        var out = new ArrayList<org.bukkit.entity.Entity>();
        if (w == null) return out;
        for (var e : w.getNearbyEntities(new Location(w, s.x + 0.5, s.y + 0.5, s.z + 0.5),
                s.radius, s.radius, s.radius))
            if (isMember(e, s)) out.add(e);
        return out;
    }

    private int placeCrops(World w, Source s, RenewableKinds.Kind kind, int wanted) {
        Material material = kind.blocks().iterator().next();
        int placed = 0;
        for (int x = s.x - s.radius; x <= s.x + s.radius && placed < wanted; x++)
            for (int z = s.z - s.radius; z <= s.z + s.radius && placed < wanted; z++)
                for (int y = s.y - s.radius; y <= s.y + s.radius && placed < wanted; y++) {
                    var cell = w.getBlockAt(x, y, z);
                    if (!cell.getType().isAir()) continue;
                    var below = w.getBlockAt(x, y - 1, z).getType();
                    if (below != Material.FARMLAND && below != Material.GRASS_BLOCK
                            && below != Material.DIRT) continue;
                    cell.setType(material, false);
                    // Server-placed, so BlockPlaceEvent never fires and the
                    // patch correctly reads as wild rather than player-farmed.
                    placed++;
                }
        return placed;
    }

    private int spawnFauna(World w, Source s, RenewableKinds.Kind kind, int wanted) {
        var types = new ArrayList<>(kind.entities());
        if (types.isEmpty()) return 0;
        int spawned = 0;
        for (int i = 0; i < wanted; i++) {
            var type = types.get(i % types.size());
            var at = new Location(w, s.x + 0.5 + (Math.random() - 0.5) * s.radius,
                    s.y + 1, s.z + 0.5 + (Math.random() - 0.5) * s.radius);
            var ground = w.getHighestBlockYAt(at.getBlockX(), at.getBlockZ());
            at.setY(Math.max(s.y, Math.min(s.y + s.radius, ground + 1)));
            try {
                var spawnedEntity = w.spawnEntity(at, type);
                // Marked at birth: membership is a fact about the entity, not
                // about where it happens to be standing.
                spawnedEntity.getPersistentDataContainer()
                        .set(memberKey, PersistentDataType.STRING, s.id);
                s.members.add(spawnedEntity.getUniqueId());
                spawned++;
            } catch (IllegalArgumentException ex) { /* peaceful difficulty refuses hostiles */ }
        }
        return spawned;
    }

    /**
     * A member that has left its region stops being wild, and stays alive.
     *
     * This is the captured-livestock case: two sheep led home are no longer
     * part of the wild herd, so the remaining wild population is three and the
     * opportunity can eventually recover -- but the two sheep are ordinary
     * animals in the team's economy and are never despawned, then or later.
     *
     * THE RULE IS UNRESOLVED. "Left the region" is the narrowest defensible
     * reading and is what ships; whether capture should instead be recognised
     * by leashing, naming, fencing or distance from the manifestation site is a
     * design decision, not a number. See the model proposal, D.1.
     */
    private void sweepMembership() {
        if (!plugin.getConfig().getBoolean("renewables.membership.revokeOnLeavingRegion", true)) return;
        for (Source s : sources.values()) {
            if (s.members.isEmpty()) continue;
            var gone = new ArrayList<UUID>();
            for (UUID id : s.members) {
                var e = Bukkit.getEntity(id);
                if (e == null) continue;              // unloaded: decide nothing
                if (!e.getWorld().getUID().equals(s.world) || !s.contains(
                        e.getLocation().getBlockX(), e.getLocation().getBlockY(),
                        e.getLocation().getBlockZ())) {
                    revoke(e);
                    gone.add(id);
                    // Leaving the wild population is depletion of the wild
                    // opportunity, exactly as killing would be.
                    harvest(s);
                }
            }
            s.members.removeAll(gone);
        }
    }

    private void sample() {
        sweepMembership();
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
