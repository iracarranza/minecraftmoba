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
 * Source tables began empty; current main supplies Alpha analytical fixtures.
 * This registers supplied sources and manages their manifestations, not a
 * generic Strategic Depth / Regional Character authoring policy. Kind vocabulary
 * carries no depth, value or region; exact policy and cadence remain [OPEN].
 *
 * Current opportunities advance through the scheduled lifecycle; legacy
 * availability access also has a lazy recovery path. All access is on the
 * server thread. See docs/audit/2026-09-22-spatial-doctrine.md.
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
        State state = State.RECOVERING;   // legacy field; see opportunity
        /**
         * The coarse authored ecology, and the live lifecycle.
         *
         * `radius` survives only as the migration of authored origin+radius data
         * into a square region, and as the volume harvest detection still scans.
         * Nothing chooses a manifestation site from it any more.
         */
        OpportunityRegion region;
        final Opportunity opportunity = Opportunity.fresh();
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
        public OpportunityRegion region() { return region; }
        public Opportunity opportunity() { return opportunity; }
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
    private final java.util.Random random = new java.util.Random();
    private final Path csv;
    private long harvests, recoveries, recoveryChecks, depletions, harvestNanos, harvestCalls;
    private long restoresApplied, persistsDeferred, remanifested;
    /** Sources whose chunk was not loaded when state changed or registration ran. */
    private final Set<String> pendingPersist = new HashSet<>();
    private final Set<String> pendingRestore = new HashSet<>();
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
        long lifecycle = plugin.getConfig().getLong("renewables.lifecycleTicks", 20L);
        if (lifecycle > 0) Bukkit.getScheduler().runTaskTimer(plugin,
                () -> tickOpportunities(lifecycle), lifecycle, lifecycle);
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
            int sx = plugin.getConfig().getInt(base + "x");
            int sy = plugin.getConfig().getInt(base + "y");
            int sz = plugin.getConfig().getInt(base + "z");
            var source = new Source(id, type, w.getUID(), sx, sy, sz,
                    radius, capacity, recover, kind);
            source.region = regionFor(base, sx, sz, radius);
            register(source);
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
    /**
     * Whether a generated map is bound, in which case config sources are wrong.
     *
     * Config's renewable coordinates belong to the frozen Alpha map. The match
     * world instance is named `alpha_match` whether it holds the Alpha template
     * or a claimed pool map, so those coordinates were being applied to
     * generated terrain that has never seen them -- Alpha's rabbits and carrot
     * patches landing wherever they happen to fall on a different world.
     *
     * This is the same defect {@link MapBindings} was built to fix for
     * Fountains, objectives, the Lair and Worksites. Renewables were never
     * migrated, because the compiler emits no renewable layer at all: a
     * generated map's `runtime_bindings` has no `renewables` key, and
     * `readiness` does not require one.
     *
     * So this cannot silently do the right thing -- there is nothing to bind
     * to. It refuses to do the wrong one, and says why.
     */
    private boolean generatedMap;

    public void bind(MapBindings bindings) {
        generatedMap = bindings != null;
    }

    public int resetForNewMatch() {
        // Manifestations are match state and are discarded with the world; the
        // Opportunities themselves are authored and are rebuilt from config.
        for (Source s : sources.values()) s.opportunity.discardManifestation();
        sources.clear();
        pendingPersist.clear();
        pendingRestore.clear();
        harvests = recoveries = recoveryChecks = depletions = harvestNanos = harvestCalls = 0;
        if (generatedMap) {
            plugin.getLogger().warning("[renewables] this map has NO regenerative layer. "
                    + "Config's sources are the Alpha map's coordinates and applying them "
                    + "to generated terrain would place opportunities on ground that was "
                    + "never authored for them. maps.md says every viable map carries a "
                    + "baseline regenerative layer; the compiler does not yet author one, "
                    + "so the honest state is none rather than Alpha's.");
            return 0;
        }
        loadConfigured();
        return sources.size();
    }

    /** Per-source availability, so depletion and recovery are observable. */
    public java.util.List<String> status() {
        var out = new java.util.ArrayList<String>();
        out.add("renewable sources=" + sources.size() + " harvests=" + harvests
                + " depletions=" + depletions + " recoveries=" + recoveries);
        for (Source s : sources.values())
            out.add("  " + s.id + " kind=" + s.kind + " " + s.opportunity
                    + " capacity=" + s.capacity + " " + s.region);
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
        s.opportunity.beginInitialDelay();
    }

    /**
     * The authored Opportunity Region.
     *
     * Explicit cells are preferred, because that is the shape the worldgen
     * analysis works in and an ecology worth calling a region often spans
     * several adjacent ones. A source with no cells falls back to a square
     * around its authored point -- which is a MIGRATION of the old origin+radius
     * data into the new shape, not an endorsement of it. The square is a coarse
     * search area; nothing selects a manifestation site from its centre.
     */
    private OpportunityRegion regionFor(String base, int x, int z, int radius) {
        var cells = plugin.getConfig().getMapList(base + "region");
        if (cells != null && !cells.isEmpty()) {
            var parsed = new ArrayList<OpportunityRegion.Cell>();
            for (var cell : cells)
                parsed.add(new OpportunityRegion.Cell(
                        ((Number) cell.get("minX")).intValue(), ((Number) cell.get("minZ")).intValue(),
                        ((Number) cell.get("maxX")).intValue(), ((Number) cell.get("maxZ")).intValue()));
            return new OpportunityRegion(parsed);
        }
        int half = plugin.getConfig().getInt("renewables.migratedRegionHalfSpan", 0);
        return OpportunityRegion.square(x, z, half > 0 ? half : radius * 3);
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
        if (s.opportunity.state() != Opportunity.State.MANIFESTED
                || s.opportunity.remaining() <= 0) return false;
        s.opportunity.memberRemoved();
        s.available = s.opportunity.remaining();
        harvests++;
        if (s.opportunity.state() == Opportunity.State.RECOVERING) {
            depletions++;
            s.state = State.DEPLETED;
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
    /**
     * Attempt one manifestation: query the CURRENT world, select a locus, create
     * a finite manifestation there.
     *
     * Every call re-queries. Nothing is cached and nothing falls back: if the
     * region currently offers nowhere eligible, the opportunity stays ready and
     * says so, because forcing a spawn or reusing the authored origin would make
     * the query decorative.
     */
    public int manifest(Source s) {
        World w = Bukkit.getWorld(s.world);
        if (w == null || s.kind == null) return 0;
        if (!s.opportunity.readyToManifest()) return 0;
        var kind = RenewableKinds.require(s.kind);

        var terrain = new WorldTerrain(w, plugin.provenance());
        var rules = eligibilityRules();
        var candidates = Eligibility.loci(s.region, terrain, rules);
        var locus = Eligibility.select(candidates, rules, s.opportunity.previousLocus(),
                terrain, random);
        if (locus == null) {
            // Explicit, diagnosable, and not an error: the ecology is currently
            // unavailable, which is a thing players can cause and undo.
            s.opportunity.noEligibleLocus();
            return 0;
        }

        int wanted = s.capacity;
        int made = kind.type() == Type.CROP ? placeCrops(w, s, kind, locus, wanted)
                                            : spawnFauna(w, s, kind, locus, wanted);
        if (made > 0) {
            s.opportunity.manifested(locus, made);
            s.available = made;
            s.state = State.MANIFESTED;
        }
        return made;
    }

    /** ALPHA FIXTURES. Exact predicates by resource kind are unresolved. */
    private Eligibility.Rules eligibilityRules() {
        var cfg = plugin.getConfig();
        String base = "renewables.eligibility.";
        return new Eligibility.Rules(
                cfg.getInt(base + "headroom", 2),
                Eligibility.groundFrom(cfg.getStringList(base + "naturalGround")),
                cfg.getInt(base + "sampleStride", 4),
                cfg.getDouble(base + "minDisplacement", 12.0),
                cfg.getDouble(base + "playerExclusion", 24.0),
                cfg.getBoolean(base + "rejectPlayerPlaced", true));
    }

    /** WORKING CALIBRATION, expressed as fractions of the Temporal Phase P. */
    private Recovery.Rates ratesFor(Source s) {
        var cfg = plugin.getConfig();
        String base = "renewables.temporal.";
        double nightRate = s.type == Type.SWARM
                ? cfg.getDouble(base + "swarmNightRate", 1.0)
                : cfg.getDouble(base + "nightRate", 0.73);
        // The first manifestation of a match waits longer than a recovery does:
        // the regenerative economy is meant to follow the opening, not race it.
        double fraction = s.opportunity.hasManifested()
                ? cfg.getDouble(base + "dayFraction", 0.33)
                : cfg.getDouble(base + "initialFraction", 0.4);
        return new Recovery.Rates(
                cfg.getLong(base + "phaseTicks", MatchClock.PHASE_TICKS),
                fraction, nightRate);
    }

    /**
     * Advance every opportunity's lifecycle by one sample interval.
     *
     * Recovery accumulates at the CURRENT world's rate rather than being a
     * duration chosen when the last resource was taken, so a recovery that
     * begins in daylight and runs past sunset keeps what it earned and continues
     * more slowly.
     */
    private void tickOpportunities(long ticks) {
        for (Source s : sources.values()) {
            World w = Bukkit.getWorld(s.world);
            if (w == null) continue;
            boolean night = WorldTerrain.isNight(w);
            s.opportunity.tickRecovery(ticks, night, ratesFor(s));
            if (s.opportunity.readyToManifest()) remanifested += manifest(s);
        }
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

    /**
     * A wild Patch: an irregular, terrain-conforming concentration.
     *
     * Each crop gets the minimum substrate it needs to exist -- one farmland
     * block directly beneath it, unmoistened -- and nothing else. No prepared
     * field, no water source, no flattening. That single-block conversion is the
     * accommodation a resource needs in order to be there; a broad tilled
     * rectangle with irrigation is a farm, which is what players are supposed to
     * build and what the world must not hand them.
     */
    private int placeCrops(World w, Source s, RenewableKinds.Kind kind,
                           Eligibility.Locus locus, int wanted) {
        Material material = kind.blocks().iterator().next();
        int spread = plugin.getConfig().getInt("renewables.patch.spread", 6);
        int placed = 0;
        for (int[] column : PatchShape.columns(locus.x(), locus.z(), wanted, spread, random)) {
            if (placed >= wanted) break;
            int x = column[0], z = column[1];
            if (!w.isChunkLoaded(x >> 4, z >> 4)) continue;
            int groundY = w.getHighestBlockYAt(x, z);
            var ground = w.getBlockAt(x, groundY, z);
            if (!Eligibility.NATURAL_GROUND.contains(ground.getType())) continue;
            var cell = w.getBlockAt(x, groundY + 1, z);
            if (!cell.getType().isAir()) continue;
            if (material.createBlockData() instanceof org.bukkit.block.data.Ageable
                    && ground.getType() != Material.FARMLAND)
                ground.setType(Material.FARMLAND, false);   // minimum substrate, one block
            cell.setType(material, false);
            var data = cell.getBlockData();
            if (data instanceof org.bukkit.block.data.Ageable age) {
                age.setAge(age.getMaximumAge());
                cell.setBlockData(age, false);
            }
            // Server-placed, so BlockPlaceEvent never fires and the patch
            // correctly reads as wild rather than player-farmed.
            placed++;
        }
        return placed;
    }

    /** A Herd or Swarm: members clustered at the selected locus, nothing built. */
    private int spawnFauna(World w, Source s, RenewableKinds.Kind kind,
                           Eligibility.Locus locus, int wanted) {
        var types = new ArrayList<>(kind.entities());
        if (types.isEmpty()) return 0;
        int cluster = plugin.getConfig().getInt("renewables.herd.cluster", 5);
        int spawned = 0;
        for (int i = 0; i < wanted; i++) {
            var type = types.get(i % types.size());
            var at = new Location(w, locus.x() + 0.5 + (random.nextDouble() - 0.5) * cluster,
                    locus.y(), locus.z() + 0.5 + (random.nextDouble() - 0.5) * cluster);
            var ground = w.getHighestBlockYAt(at.getBlockX(), at.getBlockZ());
            at.setY(ground + 1);
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
