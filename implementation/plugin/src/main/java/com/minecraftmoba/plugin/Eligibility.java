package com.minecraftmoba.plugin;

import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Which loci inside a Region could hold a manifestation right now, and which one
 * gets it.
 *
 * This is the layer canon calls Regenerative Eligibility, and the whole point is
 * that it is a QUERY against the current world rather than a table. Two
 * consequences follow, and both are architectural rather than cosmetic:
 *
 *   - a locus is chosen fresh before every manifestation, so Locus(t+1) need not
 *     equal Locus(t). The recurring thing is the opportunity, not a resource
 *     pile at fixed coordinates;
 *   - a player who builds over an ecology changes which loci are eligible
 *     without changing the Opportunity at all, so Development can push a wild
 *     manifestation elsewhere inside its own region.
 *
 * THE PREDICATES BELOW ARE MINIMAL ALPHA FIXTURES. They exist to exercise the
 * architecture, not to settle what terrain suits sheep or carrots. Exact
 * eligibility by resource kind, and the weighting between eligible loci, are
 * recorded unresolved; see docs/proposals/2026-09-21-regenerative-opportunity-model.md.
 */
public final class Eligibility {

    /** A position a manifestation could occupy, with the ground it sits on. */
    public record Locus(int x, int y, int z) {
        public double distanceTo(Locus other) {
            double dx = x - other.x, dy = y - other.y, dz = z - other.z;
            return Math.sqrt(dx * dx + dy * dy + dz * dz);
        }
    }

    /**
     * Alpha fixture rules. Every field is a value the design has NOT settled.
     *
     * @param headroom          air blocks needed above the ground block
     * @param ground            substrates a manifestation may sit on
     * @param sampleStride      column sampling stride inside the region
     * @param minDisplacement   how far a new locus must be from the previous one
     * @param playerExclusion   how close to a player a locus may be chosen
     * @param rejectPlayerPlaced whether player-built ground disqualifies a locus
     */
    public record Rules(int headroom, Set<Material> ground, int sampleStride,
                        double minDisplacement, double playerExclusion,
                        boolean rejectPlayerPlaced) {}

    private Eligibility() {}

    /** Natural ground a wild manifestation can plausibly occupy. An Alpha fixture. */
    public static final Set<Material> NATURAL_GROUND = Set.of(
            Material.GRASS_BLOCK, Material.DIRT, Material.COARSE_DIRT, Material.ROOTED_DIRT,
            Material.PODZOL, Material.MYCELIUM, Material.MOSS_BLOCK, Material.SAND,
            Material.RED_SAND, Material.GRAVEL, Material.STONE, Material.SNOW_BLOCK);

    /**
     * Every locus in the region that satisfies the rules against the world as it
     * is right now. Unknown (unloaded) columns are skipped rather than guessed.
     */
    public static List<Locus> loci(OpportunityRegion region, TerrainView world, Rules rules) {
        var out = new ArrayList<Locus>();
        for (int[] column : region.columns(rules.sampleStride())) {
            int x = column[0], z = column[1];
            if (!world.known(x, z)) continue;
            int groundY = world.surfaceY(x, z);
            Material ground = world.blockAt(x, groundY, z);
            if (!rules.ground().contains(ground)) continue;
            // A manifestation must not appear inside a player's work, which is
            // also what lets Development displace it within its own region.
            if (rules.rejectPlayerPlaced() && world.isPlayerPlaced(x, groundY, z)) continue;
            boolean clear = true;
            for (int dy = 1; dy <= rules.headroom(); dy++) {
                Material above = world.blockAt(x, groundY + dy, z);
                if (above != Material.AIR && above != Material.CAVE_AIR
                        && above != Material.SHORT_GRASS && above != Material.TALL_GRASS) {
                    clear = false; break;
                }
            }
            if (!clear) continue;
            out.add(new Locus(x, groundY + 1, z));
        }
        return out;
    }

    /**
     * Choose one locus, or none.
     *
     * Two exclusions, both fixtures, both with a stated reason. A new
     * manifestation is kept away from the previous one so that a region does not
     * decay into a camp coordinate; and away from players so that an opportunity
     * is discovered rather than materialising around someone's feet.
     *
     * Returning null is a real answer and the caller must handle it. Nothing
     * here falls back to an authored origin, relaxes a predicate, or widens the
     * region -- that would make the eligibility query decorative.
     */
    public static Locus select(List<Locus> candidates, Rules rules, Locus previous,
                               TerrainView world, Random random) {
        var viable = new ArrayList<Locus>();
        for (Locus l : candidates) {
            if (previous != null && l.distanceTo(previous) < rules.minDisplacement()) continue;
            if (world.distanceToNearestPlayer(l.x(), l.y(), l.z()) < rules.playerExclusion()) continue;
            viable.add(l);
        }
        if (viable.isEmpty()) return null;
        // Uniform among viable loci. Weighted selection is explicitly
        // unresolved, and a uniform draw is the one choice that asserts nothing
        // about what a good site looks like.
        return viable.get(random.nextInt(viable.size()));
    }
}
