package com.minecraftmoba.plugin;

/**
 * The match clock, restored to vanilla day/night timing.
 *
 * A full cycle is 24,000 ticks -- twenty real minutes -- and match ticks run
 * 1:1 with world ticks, so the world's phases are vanilla's own rather than a
 * compression of them. Sunset falls at tick 12,000 of each cycle: ten, thirty,
 * fifty and seventy minutes into a match.
 *
 * It previously ran a 6-minute day and a 6-minute night, and that compression
 * was not neutral. In live play on 2026-09-21 the entire first Worksite
 * activation window opened and closed while the player was still on their first
 * mining expedition, which is evidence about the clock rather than about the
 * player: a macro pulse that a normal opening cannot attend to is not pacing
 * the match, it is passing it by.
 *
 * Vanilla timing is the BASELINE/CONTROL here, not settled canon. The honest
 * comparison is against the timing every Minecraft player already has in their
 * hands, and the right response to a long opening is to make the opening
 * better -- directionality, Route usability, opportunity readability -- rather
 * than to shorten the day until the opening fits.
 *
 * The match has no scheduled end. An earlier MATCH_MINUTES=80 was inferred
 * from an activation series that happened to have four entries, and the tick
 * loop then announced a "48-minute analytical horizon" past it while
 * double-incrementing the counter -- so boundaries after the horizon were
 * skipped outright. Nothing in canon ends a match on a clock; victory is the
 * Fountain predicate. The horizon is gone rather than renumbered, and in
 * particular the six-event opportunity cadence does NOT establish a new
 * 120-minute duration.
 *
 * This is arithmetic over elapsed ticks, not a second source of truth: the
 * match owns the tick count and asks the clock what it means.
 */
public final class MatchClock {
    /** Vanilla: a full day/night cycle is 24,000 ticks, i.e. 20 minutes. */
    public static final long CYCLE_TICKS = 24000;
    /** Vanilla sunset begins at tick 12,000 of the cycle. */
    public static final long SUNSET_TICK = 12000;
    /** Day and night are each half the cycle in vanilla tick terms. */
    public static final long PHASE_TICKS = SUNSET_TICK;
    public static final long PHASE_MINUTES = PHASE_TICKS / (60 * 20);
    public enum Phase { DAY, NIGHT }

    private MatchClock() {}

    public static Phase phaseAt(long elapsedTicks) {
        return (elapsedTicks % CYCLE_TICKS) < SUNSET_TICK ? Phase.DAY : Phase.NIGHT;
    }

    /** Zero-based index of the phase, so the opening day is 0 and its night 1. */
    public static long phaseIndex(long elapsedTicks) { return elapsedTicks / PHASE_TICKS; }

    /** True on the exact tick a sunset begins: 10, 30, 50, 70, 90, 110 minutes. */
    public static boolean isSunsetBoundary(long elapsedTicks) {
        return elapsedTicks > 0 && elapsedTicks % CYCLE_TICKS == SUNSET_TICK;
    }

    /** True on the exact tick a sunrise begins: 20, 40, 60 minutes. */
    public static boolean isSunriseBoundary(long elapsedTicks) {
        return elapsedTicks > 0 && elapsedTicks % CYCLE_TICKS == 0;
    }

    /**
     * Which sunset this is, 1-based; 0 before the first.
     *
     * This is also the opportunity-night ordinal {@link OpportunityCadence}
     * reads. The cadence derives its stage from this arithmetic rather than
     * keeping a counter of its own, so there is exactly one temporal source of
     * truth and {@code skip} cannot desynchronize it.
     */
    public static int sunsetOrdinal(long elapsedTicks) {
        return (int) ((elapsedTicks + CYCLE_TICKS - SUNSET_TICK) / CYCLE_TICKS);
    }

    public static long minutes(long elapsedTicks) { return elapsedTicks / (60 * 20); }

    /**
     * World time is now the elapsed time. At vanilla rate there is nothing to
     * convert: a match tick IS a world tick, so sunset, moonrise and mob
     * spawning all happen exactly when a player's instincts expect.
     */
    public static long worldTime(long elapsedTicks) { return elapsedTicks % CYCLE_TICKS; }

    public static String describe(long elapsedTicks) {
        return String.format("%s %d:%02d (%s, phase %d)",
                phaseAt(elapsedTicks), minutes(elapsedTicks),
                (elapsedTicks / 20) % 60, phaseAt(elapsedTicks) == Phase.DAY ? "day" : "night",
                phaseIndex(elapsedTicks));
    }
}
