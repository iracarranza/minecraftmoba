package com.minecraftmoba.plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * The legibility bench's venue as pure geometry: a long straight track with the subject
 * at one end and distance markers every few blocks, so "how far away" is a number read
 * off the floor rather than estimated.
 *
 * Clutter is a deterministic scatter of pillars in the corridor between observer and
 * subject: seeded, so two testers (or one tester twice) see the same obstruction and a
 * difference in what reads is the variant's.
 *
 * Pure. Tested without a server.
 */
public final class LegibilityTrack {
    private LegibilityTrack() {}

    public static final int FLOOR_Y = 64;
    public static final int MIN_X = -4, MAX_X = 4;
    public static final int MIN_Z = -6, MAX_Z = 100;
    /** Markers every this many blocks along the track. */
    public static final int MARKER_EVERY = 8;
    public static final double SUBJECT_Z = 2.5;
    /** Where the observer starts: 16 blocks out. */
    public static final double OBSERVER_Z = 18.5;
    public static final int PILLAR_HEIGHT = 4;
    /** No pillar closer than this to the subject or the observer's start. */
    public static final int CLEAR_RADIUS = 6;

    public record Pillar(int x, int z, int height) {}

    public static boolean onTrack(int x, int z) { return x >= MIN_X && x <= MAX_X && z >= MIN_Z && z <= MAX_Z; }

    /** Distances at which a marker stripe lies, from the subject. */
    public static List<Integer> markerDistances() {
        var out = new ArrayList<Integer>();
        for (int d = MARKER_EVERY; SUBJECT_Z + d <= MAX_Z; d += MARKER_EVERY) out.add(d);
        return out;
    }

    /** The z coordinate of a marker stripe at a distance from the subject. */
    public static int markerZ(int distance) { return (int) Math.floor(SUBJECT_Z) + distance; }

    /** Horizontal distance from the subject to a point on the track. */
    public static double distanceFromSubject(double z) { return Math.abs(z - SUBJECT_Z); }

    /**
     * The pillars for a density (pillars per ten blocks of track), seeded.
     * Centre-line positions are allowed: the point of clutter is to stand in the way.
     */
    public static List<Pillar> clutter(long seed, double perTenBlocks) {
        var rng = new Random(seed);
        var out = new ArrayList<Pillar>();
        int from = (int) Math.ceil(SUBJECT_Z) + CLEAR_RADIUS;
        int to = MAX_Z - 2;
        int count = (int) Math.round((to - from) / 10.0 * perTenBlocks);
        var taken = new java.util.HashSet<Long>();
        for (int i = 0; i < count; i++) {
            int x = MIN_X + 1 + rng.nextInt(MAX_X - MIN_X - 1);
            int z = from + rng.nextInt(to - from + 1);
            if (Math.abs(z - OBSERVER_Z) < CLEAR_RADIUS) continue;
            if (taken.add(((long) x << 32) ^ z)) out.add(new Pillar(x, z, PILLAR_HEIGHT));
        }
        return out;
    }
}
