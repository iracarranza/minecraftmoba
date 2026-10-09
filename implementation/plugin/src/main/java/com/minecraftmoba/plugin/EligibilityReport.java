package com.minecraftmoba.plugin;

import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;

/**
 * WHY a region can or cannot hold a manifestation right now, as counts.
 *
 * {@link Eligibility#loci} answers with the places that qualify, and
 * {@link Eligibility#select} with the one chosen; neither says what the rest were
 * rejected for, and "no eligible locus" is the answer a tester most needs explained: it
 * can mean the ground is built over, the headroom is blocked, a player is standing too
 * close, or the previous site is too near. This walks the same columns in the same order
 * and classifies each, so the bench can say which.
 *
 * <h2>The drift this must not allow</h2>
 *
 * That is a second copy of the rule in {@code Eligibility}, which is the repeated defect
 * in this codebase. It is kept honest by tests that assert the counts agree with
 * {@code Eligibility.loci} and {@code select} on varied terrain: if the rule changes and
 * this does not, those tests fail.
 *
 * Pure. Tested without a server.
 */
public final class EligibilityReport {
    private EligibilityReport() {}

    /** Counts per outcome. Every sampled column is in exactly one of the first five. */
    public record Report(int columns, int unknown, int wrongGround, int playerPlaced, int noHeadroom,
                         int eligible, int tooCloseToPrevious, int tooCloseToPlayer, int viable,
                         List<Eligibility.Locus> eligibleLoci, List<Eligibility.Locus> viableLoci) {
        public boolean blocked() { return viable == 0; }
    }

    public static Report explain(OpportunityRegion region, TerrainView world, Eligibility.Rules rules,
                                 Eligibility.Locus previous) {
        int columns = 0, unknown = 0, wrongGround = 0, placed = 0, headroom = 0;
        var eligibleLoci = new ArrayList<Eligibility.Locus>();
        for (int[] column : region.columns(rules.sampleStride())) {
            columns++;
            int x = column[0], z = column[1];
            if (!world.known(x, z)) { unknown++; continue; }
            int groundY = world.surfaceY(x, z);
            Material ground = world.blockAt(x, groundY, z);
            if (!rules.ground().contains(ground)) { wrongGround++; continue; }
            if (rules.rejectPlayerPlaced() && world.isPlayerPlaced(x, groundY, z)) { placed++; continue; }
            boolean clear = true;
            for (int dy = 1; dy <= rules.headroom(); dy++) {
                Material above = world.blockAt(x, groundY + dy, z);
                if (above != Material.AIR && above != Material.CAVE_AIR
                        && above != Material.SHORT_GRASS && above != Material.TALL_GRASS) { clear = false; break; }
            }
            if (!clear) { headroom++; continue; }
            eligibleLoci.add(new Eligibility.Locus(x, groundY + 1, z));
        }
        int nearPrev = 0, nearPlayer = 0;
        var viable = new ArrayList<Eligibility.Locus>();
        for (var l : eligibleLoci) {
            if (previous != null && l.distanceTo(previous) < rules.minDisplacement()) { nearPrev++; continue; }
            if (world.distanceToNearestPlayer(l.x(), l.y(), l.z()) < rules.playerExclusion()) { nearPlayer++; continue; }
            viable.add(l);
        }
        return new Report(columns, unknown, wrongGround, placed, headroom, eligibleLoci.size(),
                nearPrev, nearPlayer, viable.size(), List.copyOf(eligibleLoci), List.copyOf(viable));
    }

    /**
     * The fraction of a region's columns that lie OUTSIDE the cube of a given radius around a
     * source's origin (0 to 1).
     *
     * Eligibility chooses a manifestation site anywhere in the REGION, but the membership
     * sweep and the harvest scan judge "still in the wild population" by the source's RADIUS
     * cube around its authored origin. When the region is larger than that cube, members
     * placed outside it are swept as having left. This is how much of the region that is.
     */
    public static double fractionOutsideRadius(OpportunityRegion region, int originX, int originZ, int radius) {
        long outside = 0, total = 0;
        for (var cell : region.cells())
            for (int x = cell.minX(); x <= cell.maxX(); x++)
                for (int z = cell.minZ(); z <= cell.maxZ(); z++) {
                    total++;
                    if (Math.abs(x - originX) > radius || Math.abs(z - originZ) > radius) outside++;
                }
        return total == 0 ? 0 : (double) outside / total;
    }

    /** The report in words, leading with the verdict. */
    public static List<String> describe(Report r) {
        var out = new ArrayList<String>();
        out.add(r.viable() > 0
                ? r.viable() + " viable locus(es) of " + r.columns() + " sampled columns: a manifestation can happen."
                : "NO viable locus among " + r.columns() + " sampled columns: " + dominant(r));
        out.add("  eligible ground " + r.eligible() + " | unknown (unloaded) " + r.unknown()
                + " | wrong ground " + r.wrongGround() + " | player-placed " + r.playerPlaced()
                + " | no headroom " + r.noHeadroom());
        if (r.eligible() > 0)
            out.add("  of the eligible: too close to the previous site " + r.tooCloseToPrevious()
                    + " | too close to a player " + r.tooCloseToPlayer() + " | viable " + r.viable());
        return out;
    }

    /** The reason that accounts for the most rejected columns, in a sentence. */
    static String dominant(Report r) {
        if (r.eligible() > 0 && r.viable() == 0)
            return r.tooCloseToPlayer() >= r.tooCloseToPrevious()
                    ? "every eligible site is too close to a player." : "every eligible site is too close to the previous one.";
        int max = Math.max(Math.max(r.unknown(), r.wrongGround()), Math.max(r.playerPlaced(), r.noHeadroom()));
        if (max == 0) return "the region has no columns.";
        if (max == r.playerPlaced()) return "the ground is player-placed.";
        if (max == r.wrongGround()) return "the ground is not natural ground.";
        if (max == r.noHeadroom()) return "there is no headroom.";
        return "the columns are not loaded.";
    }
}
