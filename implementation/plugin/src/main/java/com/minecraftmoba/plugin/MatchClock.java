package com.minecraftmoba.plugin;

/**
 * The match clock (Economic Calibration Supplement, 14 September).
 *
 * Day lasts 6 minutes and night lasts 6 minutes, a 12-minute cycle, over a
 * 48-minute analytical match: Day 0-6, Night 6-12, Day 12-18, ... Night 42-48.
 * Worksites open at sunset, giving macro pulses at 6, 18, 30 and 42 minutes.
 *
 * This is arithmetic over elapsed ticks, not a second source of truth: the
 * match owns the tick count and asks the clock what it means. The world's own
 * daylight cycle is driven from here so that what a player sees matches the
 * phase the economy is in.
 */
public final class MatchClock {
    public static final long PHASE_MINUTES = 6;
    public static final long PHASE_TICKS = PHASE_MINUTES * 60 * 20;
    public static final long CYCLE_TICKS = PHASE_TICKS * 2;
    public static final long MATCH_MINUTES = 48;
    public static final long MATCH_TICKS = MATCH_MINUTES * 60 * 20;

    public enum Phase { DAY, NIGHT }

    private MatchClock() {}

    public static Phase phaseAt(long elapsedTicks) {
        return (elapsedTicks % CYCLE_TICKS) < PHASE_TICKS ? Phase.DAY : Phase.NIGHT;
    }

    /** Zero-based index of the phase, so Day 0-6 is 0 and Night 6-12 is 1. */
    public static long phaseIndex(long elapsedTicks) { return elapsedTicks / PHASE_TICKS; }

    /** True on the exact tick a sunset begins: 6, 18, 30, 42 minutes. */
    public static boolean isSunsetBoundary(long elapsedTicks) {
        return elapsedTicks > 0 && elapsedTicks % CYCLE_TICKS == PHASE_TICKS;
    }

    /** True on the exact tick a sunrise begins: 12, 24, 36 minutes. */
    public static boolean isSunriseBoundary(long elapsedTicks) {
        return elapsedTicks > 0 && elapsedTicks % CYCLE_TICKS == 0;
    }

    /** Which sunset this is, 1-based; 0 before the first. */
    public static int sunsetOrdinal(long elapsedTicks) {
        return (int) ((elapsedTicks + CYCLE_TICKS - PHASE_TICKS) / CYCLE_TICKS);
    }

    public static long minutes(long elapsedTicks) { return elapsedTicks / (60 * 20); }

    public static boolean pastHorizon(long elapsedTicks) { return elapsedTicks >= MATCH_TICKS; }

    /**
     * Vanilla time for a compressed phase. Day runs 0..12000 and night
     * 12000..24000 in vanilla ticks; a 6-minute phase covers that range in
     * 7200 real ticks, so the world reads as a full day without accelerating
     * anything else in the simulation.
     */
    public static long worldTime(long elapsedTicks) {
        long within = elapsedTicks % PHASE_TICKS;
        long base = phaseAt(elapsedTicks) == Phase.DAY ? 0 : 12000;
        return base + (within * 12000 / PHASE_TICKS);
    }

    public static String describe(long elapsedTicks) {
        return String.format("%s %d:%02d (%s, phase %d)",
                phaseAt(elapsedTicks), minutes(elapsedTicks),
                (elapsedTicks / 20) % 60, phaseAt(elapsedTicks) == Phase.DAY ? "day" : "night",
                phaseIndex(elapsedTicks));
    }
}
