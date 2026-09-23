package com.minecraftmoba.plugin;

import java.util.*;

/**
 * An objective's finite ability to defend its Fountain, and the one state all
 * three sieges attack.
 *
 * Doctrine: a defensive objective topples when its ability to defend has been
 * exhausted. The three Minecraft-native approaches are not three completion
 * buttons and not three unrelated minigames -- they reduce the same capacity, so
 * a team can fight through a wave, demolish a wall, take the signature target
 * and finish the survivors, and all four acts count toward the same siege.
 *
 * Deliberately NOT a player-facing HP bar. The player-facing system is world
 * state: defenders, replenishment, physical damage and signature state. This is
 * the internal scalar those things move, which doctrine explicitly permits.
 *
 * What each act is WORTH is unresolved design. Defender counts, wave cadence,
 * the structural-integrity algorithm and signature magnitudes are all listed as
 * open, so the weights here are PROVISIONAL_ALPHA, configurable, and derived
 * from the authored structures rather than chosen: capacity comes from the
 * measured block count of the form actually built, so a Bastion is harder to
 * exhaust than a watchtower because it is physically bigger.
 */
public final class DefensiveCapacity {

    /** How a reduction arrived. Recorded so play can tell the routes apart. */
    public enum Source { COMBAT, STRUCTURAL, SIGNATURE, LAIR_ASSAULT }

    public enum State { STANDING, TOPPLED }

    /** One objective's siege state. */
    public static final class Objective {
        public final Team team;
        public final TeamObjectives.Kind kind;
        public final double initial;
        int authoredBlocks = 1;
        private double remaining;
        private final EnumMap<Source, Double> bySource = new EnumMap<>(Source.class);

        Objective(Team team, TeamObjectives.Kind kind, double initial) {
            this.team = team; this.kind = kind;
            this.initial = initial; this.remaining = initial;
            for (Source s : Source.values()) bySource.put(s, 0.0);
        }

        public double remaining() { return remaining; }
        public double fraction() { return initial <= 0 ? 0 : remaining / initial; }
        public State state() { return remaining > 0 ? State.STANDING : State.TOPPLED; }
        public Map<Source, Double> contributions() { return Collections.unmodifiableMap(bySource); }

        /** Reduce capacity. Returns true when this act toppled the objective. */
        boolean reduce(Source source, double amount) {
            if (amount <= 0 || remaining <= 0) return false;
            double applied = Math.min(amount, remaining);
            remaining -= applied;
            bySource.merge(source, applied, Double::sum);
            return remaining <= 0;
        }

        @Override public String toString() {
            return team.lower() + "/" + kind + " " + String.format("%.0f/%.0f", remaining, initial)
                    + " (" + state() + ") by " + bySource;
        }
    }

    /**
     * PROVISIONAL_ALPHA weights. Not balance, and not invented: they are
     * fractions of a capacity that itself comes from the built structure, so
     * the shape is "a signature act is worth a large share of the defence" and
     * the numbers move when the structures do.
     */
    public static final double COMBAT_PER_DEFENDER = 0.02;   // one wave member
    public static final double SIGNATURE_SHARE = 0.45;        // a massive hit, not a win button
    public static final double LAIR_ASSAULT_SHARE = 0.55;     // major progress, not a scripted topple

    /**
     * Structural damage is a FRACTION OF THE STRUCTURE, not a rate per block.
     *
     * A per-block constant multiplied by a capacity that itself came from the
     * block count squared the structure's size: 2000 blocks removed from a
     * 1156-block watchtower exhausted it twice over, which is not a number that
     * can happen. Destroying a given share of a structure now removes that
     * share of its defence, so a Bastion takes twenty times the work of a
     * watchtower because it is twenty times the building -- and no separate
     * constant has to be kept in step with the meshes.
     */
    public static final double CAPACITY_PER_BLOCK = 0.08;

    private final Map<String, Objective> objectives = new LinkedHashMap<>();

    private static String key(Team team, TeamObjectives.Kind kind) {
        return team.lower() + ":" + kind;
    }

    /**
     * Register an objective with capacity derived from what was actually built.
     *
     * `blocks` is the authored structure's own block count. Using it means the
     * Bastion's 22,000 blocks and the watchtower's 1,156 are not asserted to be
     * equally defensible, and nobody has to maintain a second table of numbers
     * that drifts from the structures.
     */
    public Objective register(Team team, TeamObjectives.Kind kind, int blocks) {
        Objective o = new Objective(team, kind, Math.max(1.0, blocks * CAPACITY_PER_BLOCK));
        o.authoredBlocks = Math.max(1, blocks);
        objectives.put(key(team, kind), o);
        return o;
    }

    public Objective get(Team team, TeamObjectives.Kind kind) {
        return objectives.get(key(team, kind));
    }

    public Collection<Objective> all() { return objectives.values(); }

    public void reset() { objectives.clear(); }

    /** A defender of this objective was killed. */
    public boolean defenderKilled(Team team, TeamObjectives.Kind kind) {
        return apply(team, kind, Source.COMBAT, COMBAT_PER_DEFENDER * capacityOf(team, kind));
    }

    /**
     * Usable defensive structure was destroyed.
     *
     * Deliberately a share of the building rather than a flat per-block rate:
     * doctrine says decorative block-breaking must not equal destroying usable
     * defensive infrastructure, and the first thing a flat rate gets wrong is
     * the relationship between a small structure and a large one.
     */
    public boolean structureDestroyed(Team team, TeamObjectives.Kind kind, int blocks) {
        Objective o = get(team, kind);
        if (o == null) return false;
        double share = Math.min(1.0, (double) blocks / o.authoredBlocks);
        return apply(team, kind, Source.STRUCTURAL, share * o.initial);
    }

    /** The objective's signature target was compromised. */
    public boolean signature(Team team, TeamObjectives.Kind kind) {
        return apply(team, kind, Source.SIGNATURE, SIGNATURE_SHARE * capacityOf(team, kind));
    }

    /**
     * A defeated Lair monster performed its siege here.
     *
     * Major progress rather than an automatic topple -- but because it acts on
     * the objective's real current state, it CAN topple one players have
     * already weakened, which is the setup doctrine explicitly permits.
     */
    public boolean lairAssault(Team team, TeamObjectives.Kind kind) {
        return apply(team, kind, Source.LAIR_ASSAULT, LAIR_ASSAULT_SHARE * capacityOf(team, kind));
    }

    private double capacityOf(Team team, TeamObjectives.Kind kind) {
        Objective o = get(team, kind);
        return o == null ? 0 : o.initial;
    }

    private boolean apply(Team team, TeamObjectives.Kind kind, Source source, double amount) {
        Objective o = get(team, kind);
        return o != null && o.reduce(source, amount);
    }

    public List<String> report() {
        if (objectives.isEmpty()) return List.of("defensive capacity: no objectives registered");
        List<String> out = new ArrayList<>();
        out.add("defensive capacity (PROVISIONAL_ALPHA weights; capacity from built blocks)");
        for (Objective o : objectives.values()) out.add("  " + o);
        return out;
    }
}
