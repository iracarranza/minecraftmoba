package com.minecraftmoba.plugin;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * The shape of a wild Patch: where its resource blocks go, and nothing else.
 *
 * A Patch is a resource OCCURRENCE, not a farm. The legitimacy test is not
 * whether vanilla worldgen would produce this crop here -- regenerative
 * manifestations are deliberately game-authored, and a wild carrot patch is a
 * valid authored ecology even though vanilla never generates one. The test is
 * whether it reads as a resource occurrence or as a facility somebody has
 * already built.
 *
 * So this generates an irregular, terrain-conforming cluster: a random growth
 * from the locus with gaps and variable density, each block sitting on its own
 * column's own surface. It emits ONLY the columns for resource blocks. It has no
 * concept of a fence, a water source, a prepared field or a flattened floor, and
 * the caller is expected to convert substrate one block at a time directly
 * beneath a crop -- the minimum accommodation for the resource to exist -- and
 * nothing beyond that.
 *
 * The previous authored representation was a 24-span tilled field with moisture-7
 * farmland and a central water column. That is not a wild patch by any reading;
 * it is player Development, pre-built, and it made the world's own manifestation
 * indistinguishable from the thing players are supposed to create.
 *
 * Density, spread and count are Alpha fixtures.
 */
public final class PatchShape {

    private PatchShape() {}

    /**
     * Columns for a patch of `count` resource blocks around a locus.
     *
     * Growth rather than area fill: start at the locus and repeatedly step to a
     * neighbour of an already-chosen column. That produces the irregular
     * outline, the variable density and the occasional gap on its own, without
     * any rule that says "make it look natural".
     */
    public static List<int[]> columns(int originX, int originZ, int count, int spread, Random random) {
        if (count <= 0) return List.of();
        Set<Long> chosen = new LinkedHashSet<>();
        var frontier = new ArrayList<int[]>();
        chosen.add(key(originX, originZ));
        frontier.add(new int[]{originX, originZ});

        int guard = count * 64;
        while (chosen.size() < count && guard-- > 0 && !frontier.isEmpty()) {
            int[] from = frontier.get(random.nextInt(frontier.size()));
            // Eight-way steps of one or two blocks: two-block steps are what
            // leave holes, so the cluster reads as scattered growth rather than
            // as a filled blob.
            int step = random.nextBoolean() ? 1 : 2;
            int nx = from[0] + (random.nextInt(3) - 1) * step;
            int nz = from[1] + (random.nextInt(3) - 1) * step;
            if (nx == from[0] && nz == from[1]) continue;
            if (Math.abs(nx - originX) > spread || Math.abs(nz - originZ) > spread) continue;
            if (!chosen.add(key(nx, nz))) continue;
            frontier.add(new int[]{nx, nz});
        }
        var out = new ArrayList<int[]>();
        for (long k : chosen) out.add(new int[]{(int) (k >> 32), (int) (k & 0xFFFFFFFFL)});
        return out;
    }

    private static long key(int x, int z) {
        return ((long) x << 32) | (z & 0xFFFFFFFFL);
    }

    /**
     * How much of the patch's bounding box it actually fills.
     *
     * A prepared field fills its box; an occurrence does not. Exposed because it
     * is the one property that distinguishes the two shapes numerically, and a
     * test that cannot check it can only check that the code was called.
     */
    public static double fillRatio(List<int[]> columns) {
        if (columns.isEmpty()) return 0;
        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
        int minZ = Integer.MAX_VALUE, maxZ = Integer.MIN_VALUE;
        for (int[] c : columns) {
            minX = Math.min(minX, c[0]); maxX = Math.max(maxX, c[0]);
            minZ = Math.min(minZ, c[1]); maxZ = Math.max(maxZ, c[1]);
        }
        long box = (long) (maxX - minX + 1) * (maxZ - minZ + 1);
        return columns.size() / (double) box;
    }
}
