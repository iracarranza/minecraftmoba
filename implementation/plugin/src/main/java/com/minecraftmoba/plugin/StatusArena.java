package com.minecraftmoba.plugin;

import java.util.List;

/**
 * The status bench's arena and its tunable durations. Pure.
 *
 * A flat square floor in a void world: a Lured or Frightened subject needs room to walk, and a
 * floor with nothing on it makes the measured distance mean what it says.
 */
public final class StatusArena {
    private StatusArena() {}

    public static final int FLOOR_Y = 64;
    public static final int MIN_X = -24, MAX_X = 24, MIN_Z = -24, MAX_Z = 24;

    /** Where the source marker starts, the subject stands, and the tester enters. */
    public static final double SOURCE_X = 0.5, SOURCE_Z = -14.5;
    public static final double SUBJECT_X = 0.5, SUBJECT_Z = 8.5;
    public static final double TESTER_X = 0.5, TESTER_Z = 18.5;

    /** [PROTOTYPE] The durations the bench offers, in ticks. */
    public static final List<Integer> DURATIONS = List.of(60, 200, 600, 1200);

    /**
     * A tester applying a status to THEMSELVES is capped, because a Stunned tester cannot use the
     * hotbar that would clear it. Commands still work, but the cap is the safe default.
     */
    public static final int SELF_MAX_TICKS = 200;

    public static boolean onFloor(int x, int z) { return x >= MIN_X && x <= MAX_X && z >= MIN_Z && z <= MAX_Z; }

    public static int nextDuration(int current) {
        int i = DURATIONS.indexOf(current);
        return DURATIONS.get((i + 1) % DURATIONS.size());
    }

    public static String describe(int ticks) {
        return ticks % 20 == 0 ? (ticks / 20) + "s" : String.format(java.util.Locale.ROOT, "%.1fs", ticks / 20.0);
    }

    /** What a self-application is allowed to last. */
    public static int selfTicks(int ticks) { return Math.min(ticks, SELF_MAX_TICKS); }
}
