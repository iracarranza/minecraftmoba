package com.minecraftmoba.plugin;

/**
 * Recovery as accumulated progress, expressed relative to the Temporal Phase.
 *
 * Two structural commitments here, both from the working direction.
 *
 * **Everything is a fraction of P**, one day or one night, rather than a number
 * of minutes. The phase length is a calibration hypothesis that has already
 * changed once today -- the clock moved from a 6-minute phase to vanilla's
 * 10-minute one -- and a regenerative system whose relationships are written in
 * absolute minutes silently re-tunes itself every time that happens.
 *
 * **Recovery is a rate, not a duration chosen at the moment of resolution.**
 * Picking a fixed timer when the last resource is taken produces the arbitrary
 * result that clearing a herd at 5:59 recovers on daytime terms while clearing
 * it at 6:01 recovers on nighttime terms, for two acts a minute apart. Progress
 * that accumulates at the current world's rate crosses sunset smoothly: work
 * done before it counts at the day rate, the remainder continues more slowly,
 * and a recovery spanning sunrise speeds up for what is left.
 *
 * Values are NON-CANON WORKING CALIBRATION. The night multiplier in particular
 * has two live candidates (~0.73x and ~0.67x) and neither is settled.
 */
public final class Recovery {

    /**
     * @param phaseTicks       P, one day or one night, in ticks
     * @param dayFraction      recovery time in daylight, as a fraction of P
     * @param nightRate        night recovery rate relative to the daytime baseline
     */
    public record Rates(long phaseTicks, double dayFraction, double nightRate) {
        public Rates {
            if (phaseTicks <= 0) throw new IllegalArgumentException("phaseTicks must be positive");
            if (dayFraction <= 0) throw new IllegalArgumentException("dayFraction must be positive");
            if (nightRate < 0) throw new IllegalArgumentException("nightRate must not be negative");
        }
        /** Ticks of daylight that complete a recovery from nothing. */
        public double dayTicks() { return phaseTicks * dayFraction; }
    }

    private Recovery() {}

    /** Progress per tick under the current world state, in units of 1.0 = complete. */
    public static double ratePerTick(Rates rates, boolean night) {
        double base = 1.0 / rates.dayTicks();
        return night ? base * rates.nightRate() : base;
    }

    /**
     * Advance progress by a span of ticks spent entirely in one phase.
     *
     * Callers tick this on the same cadence they sample the world, so a span
     * that straddles sunset is two calls rather than one approximation.
     */
    public static double advance(double progress, long ticks, boolean night, Rates rates) {
        if (ticks <= 0) return progress;
        return Math.min(1.0, progress + ticks * ratePerTick(rates, night));
    }

    public static boolean complete(double progress) { return progress >= 1.0; }

    /** Ticks still needed at the current rate, for diagnostics. */
    public static long remainingTicks(double progress, boolean night, Rates rates) {
        if (complete(progress)) return 0;
        return (long) Math.ceil((1.0 - progress) / ratePerTick(rates, night));
    }
}
