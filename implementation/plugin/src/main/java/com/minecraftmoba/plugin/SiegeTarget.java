package com.minecraftmoba.plugin;

import java.util.ArrayList;
import java.util.List;

/**
 * Which objective a siege act is aimed at, and the acts themselves.
 *
 * There are six targets (each team's Pillager Outpost, Nether Bastion and End Spike). The bench
 * cycles through them rather than asking for two arguments, because a hotbar has one press.
 *
 * The four routes are the ones {@link DefensiveCapacity} reduces: they all take the SAME capacity,
 * which is the point of the design ("one siege, not three minigames"). The amounts here are a
 * bench's convenience, not tuning: a wave of five defenders, twenty-five blocks of structure, and
 * the signature and Lair acts at their own shares.
 *
 * Pure. Tested without a server.
 */
public final class SiegeTarget {
    private SiegeTarget() {}

    public record Target(Team team, TeamObjectives.Kind kind) {
        /** The name {@code Match.siege} accepts. */
        public String objectiveName() {
            return switch (kind) { case PILLAGER_OUTPOST -> "outpost"; case NETHER_BASTION -> "bastion"; case END_SPIKE -> "spike"; };
        }
        public String label() { return team.lower() + " " + kind.name().toLowerCase(java.util.Locale.ROOT).replace('_', ' '); }
    }

    /** One way of attacking an objective, and how much of it the bench does at once. */
    public record Route(String id, String match, int amount, String describes) {}

    public static final List<Route> ROUTES = List.of(
            new Route("combat", "combat", 5, "five defenders killed (2% of capacity each)"),
            new Route("structural", "structural", 25, "twenty-five usable blocks destroyed (8% of capacity per block fraction)"),
            new Route("signature", "signature", 1, "the signature target taken (45%)"),
            new Route("lair", "lair", 1, "the Lair's victor assaults it (55%)"));

    /** All six targets, team-major so cycling visits one side's three before the other's. */
    public static List<Target> all() {
        var out = new ArrayList<Target>();
        for (Team t : Team.values()) for (var k : TeamObjectives.Kind.values()) out.add(new Target(t, k));
        return out;
    }

    public static Target next(Target current) {
        var all = all();
        if (current == null) return all.get(0);
        return all.get((all.indexOf(current) + 1) % all.size());
    }

    public static Route route(String id) {
        for (Route r : ROUTES) if (r.id().equals(id)) return r;
        return null;
    }
}
