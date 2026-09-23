package com.minecraftmoba.plugin;

import org.bukkit.Location;

import java.util.*;

/** Concurrent objective identities. Completion evidence/thresholds are intentionally unresolved. */
public final class TeamObjectives {
    public enum Kind { PILLAGER_OUTPOST, NETHER_BASTION, END_SPIKE }
    public enum State { STANDING, TOPPLED }
    public static final List<String> MIDLINE_TO_FOUNTAIN = List.of(
            "pillager_outpost", "nether_bastion", "end_spike", "aether_fountain");
    /**
     * How many blocks each current form is actually built from.
     *
     * Measured from the authored templates rather than declared here, so
     * defensive capacity scales with the structure that exists instead of with
     * a second table that drifts away from it. A Bastion is harder to exhaust
     * than a watchtower because it is physically bigger.
     */
    private static final EnumMap<Kind, Integer> AUTHORED_BLOCKS = new EnumMap<>(Map.of(
            Kind.PILLAGER_OUTPOST, 1156,     // vanilla watchtower.nbt
            Kind.NETHER_BASTION, 22721,      // bridge bastion entrance over entrance_base
            Kind.END_SPIKE, 3257));          // arena spike, radius 3 height 88

    public static int authoredBlocks(Kind kind) { return AUTHORED_BLOCKS.getOrDefault(kind, 1); }

    private final Map<Team, EnumMap<Kind, State>> states = new EnumMap<>(Team.class);
    /** Where each objective physically is on the bound map. */
    private final Map<Team, EnumMap<Kind, Location>> sites = new EnumMap<>(Team.class);
    public TeamObjectives() { reset(); }
    /** Record an objective's position on the currently bound map. */
    public void place(Team team, Kind kind, Location at) {
        sites.computeIfAbsent(team, t -> new EnumMap<>(Kind.class)).put(kind, at);
    }

    public Location site(Team team, Kind kind) {
        var byTeam = sites.get(team);
        return byTeam == null ? null : byTeam.get(kind);
    }

    /** Which objective, if any, this location belongs to. Used to route siege acts. */
    public Map.Entry<Team, Kind> at(Location where, double radius) {
        if (where == null) return null;
        for (var team : sites.entrySet())
            for (var entry : team.getValue().entrySet()) {
                Location site = entry.getValue();
                if (site == null || site.getWorld() == null
                        || !site.getWorld().equals(where.getWorld())) continue;
                if (site.distance(where) <= radius)
                    return Map.entry(team.getKey(), entry.getKey());
            }
        return null;
    }

    public void reset() {
        states.clear(); sites.clear();
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
        return Arrays.stream(Team.values()).map(t -> {
            var placed = sites.getOrDefault(t, new EnumMap<>(Kind.class));
            return "objectives " + t.lower() + "=" + states.get(t)
                    + " sited=" + placed.size() + "/" + Kind.values().length
                    + (placed.size() < Kind.values().length ? " (UNBOUND)" : "")
                    + " (concurrent; ordering is spatial, not prerequisite)";
        }).toList();
    }
}
