package com.minecraftmoba.plugin;

import java.util.List;

/**
 * The opportunity bench's venue as pure geometry: three plots on one built platform,
 * and one viewing station placed so that watching does not disturb them.
 *
 * <h2>Why the station is where it is</h2>
 *
 * A manifestation is never placed within the player-exclusion distance of a player
 * ("an opportunity is discovered, not materialised underfoot"), and that distance is
 * measured to the NEAREST player in the world. A tester standing in a plot would make
 * the bench report "no eligible locus" for the wrong reason. The station is therefore
 * at least the exclusion distance from every plot's region, so what the bench shows is
 * the opportunity's own behaviour; walking closer to see the exclusion is a deliberate
 * act, and the report counts it.
 *
 * <h2>Why two biomes</h2>
 *
 * Swarm definitions are biome- and time-gated ("mountain_ravager": mountain biomes,
 * daytime). A plains-only bench could not manifest one. The east side of the platform is
 * a mountain biome and the west is plains, so the same bench shows a swarm manifesting
 * where it is allowed and refusing where it is not.
 *
 * Pure. Tested without a server.
 */
public final class OpportunityPlots {
    private OpportunityPlots() {}

    public static final int FLOOR_Y = 64;
    public static final int MIN_X = -48, MAX_X = 48, MIN_Z = -44, MAX_Z = 56;
    /** From this x eastward the biome is a mountain one. */
    public static final int MOUNTAIN_FROM_X = 15;
    /** Half the side of a plot's square opportunity region. */
    public static final int HALF = 12;

    /** The viewing station: south of every plot, on the centre line. */
    public static double[] station() { return new double[]{0.5, FLOOR_Y + 1, -36.5}; }

    public record Plot(String id, String label, String kind, int x, int z, int capacity, boolean swarm) {
        public OpportunityRegion region() { return OpportunityRegion.square(x, z, HALF); }
        public boolean inMountain() { return region().cells().stream().allMatch(c -> c.minX() >= MOUNTAIN_FROM_X); }
    }

    public static final List<Plot> PLOTS = List.of(
            new Plot("herd", "Herd (sheep)", "sheep", -30, 0, 5, false),
            new Plot("patch", "Crop Patch (wheat)", "wheat", 0, 30, 8, false),
            new Plot("swarm", "Swarm (mountain ravager)", "ravagers", 30, 0, 1, true));

    public static Plot plot(String id) {
        for (Plot p : PLOTS) if (p.id().equals(id)) return p;
        throw new IllegalArgumentException("No plot " + id);
    }

    public static boolean onPlatform(int x, int z) { return x >= MIN_X && x <= MAX_X && z >= MIN_Z && z <= MAX_Z; }

    /** Distance from the station to the nearest point of a plot's region. */
    public static double stationDistance(Plot p) {
        double[] s = station();
        var c = p.region().cells().get(0);
        double dx = Math.max(Math.max(c.minX() - s[0], 0), s[0] - c.maxX());
        double dz = Math.max(Math.max(c.minZ() - s[2], 0), s[2] - c.maxZ());
        return Math.hypot(dx, dz);
    }
}
