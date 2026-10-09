package com.minecraftmoba.plugin;

import java.util.Locale;

/**
 * Time controls for a lab session, as arithmetic over the match clock's elapsed ticks.
 *
 * The match clock only runs FORWARD: boundaries (sunset, sunrise, a night's Worksite
 * activation) fire as the counter crosses them, and winding it back would either
 * re-fire them or skip them. So every jump here is to the NEXT occurrence of what
 * was asked for, never an earlier one, and a request that is already behind the
 * clock is answered with the next one rather than refused. "Night 3" is the one
 * target that can be genuinely behind, and it is refused with the reason.
 *
 * The jump itself is carried out by the match (so boundaries fire for real: a skipped
 * night is a real night). This class only decides how many ticks.
 *
 * Pure. Tested without a server.
 */
public final class LabTime {
    private LabTime() {}

    /** A time of day within the 24,000-tick cycle, in world ticks. Day starts at 0, sunset is 12,000. */
    public enum TimeOfDay {
        DAWN(0), NOON(6000), DUSK(MatchClock.SUNSET_TICK), MIDNIGHT(18000);
        public final long tick;
        TimeOfDay(long tick) { this.tick = tick; }

        public static TimeOfDay parse(String word) {
            return switch (word.toLowerCase(Locale.ROOT)) {
                case "dawn", "sunrise", "morning", "day" -> DAWN;
                case "noon" -> NOON;
                case "dusk", "sunset", "evening" -> DUSK;
                case "midnight", "night" -> MIDNIGHT;
                default -> throw new IllegalArgumentException("Unknown time of day '" + word
                        + "'. Use dawn, noon, dusk or midnight.");
            };
        }
    }

    /** Ticks from now to the NEXT occurrence of a time of day. Always at least one tick, never zero. */
    public static long ticksUntil(long elapsed, TimeOfDay target) {
        long into = elapsed % MatchClock.CYCLE_TICKS;
        long delta = target.tick - into;
        if (delta <= 0) delta += MatchClock.CYCLE_TICKS;
        return delta;
    }

    /** The elapsed tick at which the n-th sunset begins (1-based). */
    public static long sunsetTick(int night) {
        if (night < 1) throw new IllegalArgumentException("Nights are numbered from 1.");
        return (night - 1L) * MatchClock.CYCLE_TICKS + MatchClock.SUNSET_TICK;
    }

    /**
     * Ticks to the start of night n, so its sunset event fires on arrival.
     *
     * @throws IllegalArgumentException when night n has already begun: the clock does
     *         not run backwards, and a fresh launch is how to see it again.
     */
    public static long ticksUntilNight(long elapsed, int night) {
        long at = sunsetTick(night);
        if (at <= elapsed)
            throw new IllegalArgumentException("Night " + night + " began at " + MatchClock.describe(at)
                    + "; the clock only runs forward. Launch the scoop again to see it from the start.");
        return at - elapsed;
    }

    /** Ticks for a whole number of minutes. */
    public static long ticksForMinutes(int minutes) {
        if (minutes <= 0) throw new IllegalArgumentException("Minutes must be positive.");
        return minutes * 60L * 20L;
    }
}
