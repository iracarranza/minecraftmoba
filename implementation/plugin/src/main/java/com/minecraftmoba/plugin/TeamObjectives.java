package com.minecraftmoba.plugin;

import java.util.*;

/** Concurrent objective identities. Completion evidence/thresholds are intentionally unresolved. */
public final class TeamObjectives {
    public enum Kind { PILLAGER_OUTPOST, NETHER_BASTION, END_SPIKE }
    public enum State { STANDING, TOPPLED }
    public static final List<String> MIDLINE_TO_FOUNTAIN = List.of(
            "pillager_outpost", "nether_bastion", "end_spike", "aether_fountain");
    private final Map<Team, EnumMap<Kind, State>> states = new EnumMap<>(Team.class);
    public TeamObjectives() { reset(); }
    public void reset() {
        states.clear();
        for (Team team : Team.values()) {
            var map = new EnumMap<Kind, State>(Kind.class);
            for (Kind kind : Kind.values()) map.put(kind, State.STANDING);
            states.put(team, map);
        }
    }
    public State state(Team team, Kind kind) { return states.get(team).get(kind); }
    /** No time or prerequisite argument: bypassing outer defenses is valid. */
    public boolean attackable(Team team, Kind kind) { return state(team, kind) == State.STANDING; }
    /** Called only after a future evidence adapter has validated a toppling verb. No XP hierarchy. */
    public void recordValidatedToppling(Team team, Kind kind) { states.get(team).put(kind, State.TOPPLED); }
    public static Kind pairedObjective(OpportunityCadence.Boss boss) {
        return switch (boss) {
            case GIANT -> Kind.PILLAGER_OUTPOST;
            case GHAST -> Kind.NETHER_BASTION;
            case ENDER_DRAGON -> Kind.END_SPIKE;
        };
    }
    public List<String> report() {
        return Arrays.stream(Team.values()).map(t -> "objectives " + t.lower() + "=" + states.get(t)
                + " (concurrent; physical toppling adapter OPEN)").toList();
    }
}
