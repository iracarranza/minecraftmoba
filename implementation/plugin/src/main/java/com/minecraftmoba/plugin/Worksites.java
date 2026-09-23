package com.minecraftmoba.plugin;

import org.bukkit.Location;
import org.bukkit.World;

import java.util.*;

/**
 * Worksite state and the sunset/sunrise transitions canon specifies.
 *
 * objectives.md: "Worksites open only at sunset. At sunset a limited number of
 * Worksites activate; at sunrise the active Worksites close. The number of
 * simultaneously active Worksites depends on match phase, and which eligible
 * Worksites activate is chosen randomly from the eligible pool."
 *
 * "Depends on match phase" is now read as the Worksite TIER, not the sunset
 * count. Worksites no longer open on every sunset: they open on the Worksite
 * nights of the alternating cadence (I, II, III), and the Lair nights between
 * them open nothing here. That is what keeps the two systems' spatial rhythms
 * in contrast -- distributed sites on one night, the single shared landmark on
 * the next -- instead of every important night resolving to "go middle".
 *
 * The lifecycle is Dormant -> Activated -> Capitalized -> Exploited, and the
 * distinction that matters is that **activation does not grant
 * capitalization**. Activation makes a Worksite available; capitalization is a
 * separate, once-only event that earns the team a shared opportunity through
 * the existing {@link Contributions#capitalize}. This class never awards that
 * itself.
 *
 * Deliberately absent, because canon does not settle them:
 *  - the activation package contents. The Economic Calibration Supplement gives
 *    phase vocabularies but states they are "not guaranteed contents", so no
 *    resource package is generated here.
 *  - what physically qualifies as capitalization. objectives.md defers it to
 *    the Construct system, so capitalization is triggered explicitly rather
 *    than inferred from a player's build.
 *  - the per-tier activation count, which config supplies as a declared
 *    NON-CANON ANALYTICAL FIXTURE rather than a silent default.
 *  - the tier packages themselves. Worksite I anchors iron/coal with a Blast
 *    Furnace and Smoker, II second-tier resources with an Enchanting Table and
 *    Anvil, and III is OPEN. None of those are granted as stock contents here:
 *    the tier is recorded on the activated site and the package is reported as
 *    unresolved, because handing out a facility would be inventing the reward
 *    design rather than implementing it, and the existing Mining Outpost /
 *    Industrial Enchanter work is more specific than a bullet list of items.
 */
public final class Worksites {
    /** Canon's lifecycle. Exploited is terminal within a match. */
    public enum State { DORMANT, ACTIVATED, CAPITALIZED, EXPLOITED }

    public static final class Worksite {
        public final String id;
        public final int x, y, z;
        public final String bias;
        public State state = State.DORMANT;
        public String capitalizedBy;
        /** The tier of the night that activated it; null while never activated. */
        public OpportunityCadence.WorksiteTier tier;
        Worksite(String id, int x, int y, int z, String bias) {
            this.id = id; this.x = x; this.y = y; this.z = z; this.bias = bias;
        }
        public Location location(World w) { return new Location(w, x + 0.5, y, z + 0.5); }
        @Override public String toString() {
            return id + "@" + x + "," + z + " " + state
                    + (tier != null ? " tier " + tier + " (package UNRESOLVED)" : "")
                    + (capitalizedBy != null ? " by " + capitalizedBy : "");
        }
    }

    private final MobaPlugin plugin;
    private final Map<String, Worksite> sites = new LinkedHashMap<>();
    private final Random random;
    private final List<String> activeNow = new ArrayList<>();
    private boolean legacyWarned;

    public Worksites(MobaPlugin plugin) {
        this.plugin = plugin;
        long seed = plugin.getConfig().getLong("alpha.worksites.randomSeed", 0L);
        this.random = seed == 0 ? new Random() : new Random(seed);
        reload();
    }

    /** Load the registry from config. Positions come from the frozen Alpha map. */
    public void reload() {
        sites.clear(); activeNow.clear();
        for (Map<?, ?> raw : plugin.getConfig().getMapList("alpha.worksites.sites")) {
            Object idValue = raw.get("id");
            Object xyzValue = raw.get("xyz");
            if (idValue == null || !(xyzValue instanceof List<?> xyz) || xyz.size() < 3) continue;
            Object bias = raw.get("bias");
            sites.put(String.valueOf(idValue), new Worksite(
                    String.valueOf(idValue),
                    ((Number) xyz.get(0)).intValue(),
                    ((Number) xyz.get(1)).intValue(),
                    ((Number) xyz.get(2)).intValue(),
                    bias == null ? "neutral" : String.valueOf(bias)));
        }
    }

    public Collection<Worksite> all() { return sites.values(); }
    public Worksite get(String id) { return sites.get(id); }
    public List<Worksite> inState(State s) {
        return sites.values().stream().filter(w -> w.state == s).toList();
    }

    /**
     * How many activate on this tier's night.
     *
     * NON-CANON ANALYTICAL FIXTURE. Canon says the count "depends on match
     * phase" but does not supply the series, so it is configured per tier and
     * labelled rather than guessed inline.
     *
     * The old key was a list indexed by sunset ordinal, which quietly assumed
     * every sunset was a Worksite sunset and repeated its last value forever
     * afterwards. It is still read if present, mapped I/II/III to its first
     * three entries, so an existing deployment does not silently change
     * behaviour -- but it warns, because the shape it encodes is superseded.
     */
    public int countForTier(OpportunityCadence.WorksiteTier tier) {
        var cfg = plugin.getConfig();
        String key = "alpha.worksites.activationsPerTier." + tier.name();
        if (cfg.isInt(key)) return cfg.getInt(key);
        var legacy = cfg.getIntegerList("alpha.worksites.activationsPerSunset");
        if (legacy.isEmpty()) return 0;
        if (!legacyWarned) {
            legacyWarned = true;
            plugin.getLogger().warning("alpha.worksites.activationsPerSunset is deprecated: "
                    + "Worksites now open on cadence tier nights, not every sunset. "
                    + "Migrate to alpha.worksites.activationsPerTier.{I,II,III}.");
        }
        int index = tier.ordinal();
        return legacy.get(Math.min(index, legacy.size() - 1));
    }

    /**
     * The eligible pool.
     *
     * Canon says activation is chosen "randomly from the eligible pool" but does
     * not define eligibility. The only eligibility this can assert without
     * inventing one is state: a Worksite that is already active, capitalized or
     * exploited is not a candidate for activation.
     */
    public List<Worksite> eligible() { return inState(State.DORMANT); }

    /**
     * A Worksite night: activate a limited number from the eligible pool.
     *
     * Only the cadence calls this, and only on a Worksite night, so a Lair
     * night cannot open Worksites even by accident: there is no ordinal here
     * to get the arithmetic wrong with.
     */
    public List<Worksite> onOpportunityNight(OpportunityCadence.WorksiteTier tier) {
        int want = countForTier(tier);
        List<Worksite> pool = new ArrayList<>(eligible());
        Collections.shuffle(pool, random);
        List<Worksite> opened = new ArrayList<>();
        for (Worksite w : pool) {
            if (opened.size() >= want) break;
            w.state = State.ACTIVATED;
            w.tier = tier;
            activeNow.add(w.id);
            opened.add(w);
        }
        return opened;
    }

    /**
     * Sunrise: active Worksites close.
     *
     * Closing returns an Activated Worksite to Dormant. A Worksite that was
     * capitalized while open keeps that status: capitalization is described as
     * permanent for the team that first took it, so sunrise must not undo it.
     */
    public List<Worksite> onSunrise() {
        List<Worksite> closed = new ArrayList<>();
        for (String id : new ArrayList<>(activeNow)) {
            Worksite w = sites.get(id);
            if (w == null) continue;
            if (w.state == State.ACTIVATED) { w.state = State.DORMANT; closed.add(w); }
        }
        activeNow.clear();
        return closed;
    }

    /**
     * Capitalize an activated Worksite for a team.
     *
     * The shared-opportunity award is delegated to {@link Contributions}, which
     * already implements first-capitalization-is-once-only. This method owns
     * only the Worksite's own state.
     */
    public String capitalize(String id, Team team, Contributions contributions) {
        Worksite w = sites.get(id);
        if (w == null) return "no such worksite: " + id;
        if (!canCapitalize(w.state))
            return id + " is " + w.state + "; only an activated Worksite can be capitalized";
        // Contributions returns null on a successful first capitalization.
        String note = contributions.capitalize(id, team.lower());
        w.state = State.CAPITALIZED;
        w.capitalizedBy = team.lower();
        return id + " capitalized by " + team.lower()
                + (note == null ? " (shared opportunity earned)" : " (" + note + ")");
    }

    /**
     * Only an activated Worksite can be capitalized.
     *
     * This is the rule that keeps the two steps distinct: activation makes a
     * Worksite available and nothing more, so a sunset must never produce a
     * capitalized Worksite on its own.
     */
    public static boolean canCapitalize(State state) { return state == State.ACTIVATED; }

    /** Mark a capitalized Worksite exhausted. Terminal within a match. */
    public String exploit(String id) {
        Worksite w = sites.get(id);
        if (w == null) return "no such worksite: " + id;
        if (w.state != State.CAPITALIZED) return id + " is " + w.state + ", not capitalized";
        w.state = State.EXPLOITED;
        activeNow.remove(id);
        return id + " exploited";
    }

    /** Return every Worksite to Dormant. Called by match reset. */
    public void reset() {
        for (Worksite w : sites.values()) {
            w.state = State.DORMANT; w.capitalizedBy = null; w.tier = null;
        }
        activeNow.clear();
    }

    public List<String> report() {
        List<String> out = new ArrayList<>();
        out.add("worksites=" + sites.size() + " active=" + activeNow.size());
        var counts = new EnumMap<State, Integer>(State.class);
        for (State s : State.values()) counts.put(s, 0);
        for (Worksite w : sites.values()) counts.merge(w.state, 1, Integer::sum);
        out.add("  " + counts);
        for (Worksite w : sites.values())
            if (w.state != State.DORMANT) out.add("  " + w);
        return out;
    }
}
