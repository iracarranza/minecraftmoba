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
 *  - the per-phase activation count, which config supplies as a declared
 *    NON-CANON ANALYTICAL FIXTURE rather than a silent default.
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
        Worksite(String id, int x, int y, int z, String bias) {
            this.id = id; this.x = x; this.y = y; this.z = z; this.bias = bias;
        }
        public Location location(World w) { return new Location(w, x + 0.5, y, z + 0.5); }
        @Override public String toString() {
            return id + "@" + x + "," + z + " " + state
                    + (capitalizedBy != null ? " by " + capitalizedBy : "");
        }
    }

    private final MobaPlugin plugin;
    private final Map<String, Worksite> sites = new LinkedHashMap<>();
    private final Random random;
    private final List<String> activeNow = new ArrayList<>();

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
     * How many activate at this sunset.
     *
     * NON-CANON ANALYTICAL FIXTURE. Canon says the count "depends on match
     * phase" but does not supply the series, so it is configured per sunset and
     * labelled rather than guessed inline.
     */
    public int countForSunset(int ordinal) {
        var counts = plugin.getConfig().getIntegerList("alpha.worksites.activationsPerSunset");
        if (counts.isEmpty()) return 0;
        return counts.get(Math.min(Math.max(ordinal, 1), counts.size()) - 1);
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

    /** Sunset: activate a limited number from the eligible pool. */
    public List<Worksite> onSunset(int ordinal) {
        int want = countForSunset(ordinal);
        List<Worksite> pool = new ArrayList<>(eligible());
        Collections.shuffle(pool, random);
        List<Worksite> opened = new ArrayList<>();
        for (Worksite w : pool) {
            if (opened.size() >= want) break;
            w.state = State.ACTIVATED;
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
        for (Worksite w : sites.values()) { w.state = State.DORMANT; w.capitalizedBy = null; }
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
