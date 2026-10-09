package com.minecraftmoba.plugin;

import java.util.List;
import java.util.Random;

/**
 * Where to sample a certified scoop for a chamber bay: around what the map itself says is
 * there, not around the world spawn.
 *
 * A compiled scoop generates a WINDOW of terrain, often thousands of blocks from the
 * world's spawn point (the first three lab scoops sit around x -2100, z 1500 while the
 * spawn is at the origin). Sampling "near spawn" therefore never found a fully generated
 * stretch, and the CERTIFIED SCOOP button refused every time. The map's bindings (the two
 * Fountains, the Objectives, the Lair) are exactly the places the compiler certified as
 * generated and authored, so a bay is cut around one of them.
 *
 * Pure. Tested without a server.
 */
public final class CertifiedCentres {
    private CertifiedCentres() {}

    /** How far from an anchor a centre may be, in blocks. */
    public static final int SPREAD = 24;

    /**
     * One sampling centre: a random anchor, shifted by up to {@code spread} blocks each way.
     * Null when there are no anchors, so the caller can say so instead of inventing a place.
     */
    public static int[] pick(List<int[]> anchors, Random random, int spread) {
        if (anchors == null || anchors.isEmpty()) return null;
        int[] a = anchors.get(random.nextInt(anchors.size()));
        return new int[]{a[0] + random.nextInt(2 * spread + 1) - spread, a[1] + random.nextInt(2 * spread + 1) - spread};
    }
}
