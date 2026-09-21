package com.minecraftmoba.plugin;

/**
 * The lifecycle of one Regenerative Opportunity Relationship.
 *
 * Pure state, deliberately: the interesting properties of this system are all
 * transitions, and a transition that can only be tested on a running server is a
 * transition nobody tests. The runtime owns the world; this owns what the
 * opportunity currently IS.
 *
 * <h2>Three states, and why the third exists</h2>
 *
 * <pre>
 *   MANIFESTED             a finite manifestation is standing in the world
 *   RECOVERING             it was fully resolved; progress is accumulating
 *   READY_AWAITING_LOCUS   recovery is complete, but no eligible locus exists
 * </pre>
 *
 * The third is not bookkeeping. Without it the only honest responses to "the
 * herd's meadow is now entirely under a player's base" are to force a spawn
 * anyway, to fall back to the authored origin, or to quietly widen the region --
 * all three of which make the eligibility query decorative. Stating "ready, no
 * valid locus" keeps the refusal visible and diagnosable.
 *
 * <h2>What does NOT happen</h2>
 *
 * A partially harvested manifestation is not topped up, and does not begin
 * recovering: it is still the current manifestation, and taking three of five
 * sheep leaves two sheep, not a countdown. An ignored manifestation therefore
 * keeps its stock indefinitely -- the cost of ignoring it is that the
 * opportunity cannot begin its next cycle, which is turnover pressure rather
 * than decay.
 *
 * Recovery does not accumulate past completion, so nothing banks missed
 * generations. An opportunity that spent a whole night ready-but-blocked
 * manifests once when a locus frees up, not four times.
 */
public final class Opportunity {

    public enum State {
        /** A finite manifestation exists and is available, whole or partly harvested. */
        MANIFESTED,
        /** Fully resolved; recovery progress is accumulating. */
        RECOVERING,
        /** Recovery complete; waiting for the world to offer an eligible locus. */
        READY_AWAITING_LOCUS
    }

    private State state = State.READY_AWAITING_LOCUS;
    private double recoveryProgress;
    private int remaining;
    private Eligibility.Locus locus;
    private Eligibility.Locus previousLocus;
    /** Attempts that found no eligible locus, so the refusal is measurable. */
    private long blockedAttempts;
    private boolean hasManifested;

    public State state() { return state; }
    public double recoveryProgress() { return recoveryProgress; }
    public int remaining() { return remaining; }
    public Eligibility.Locus locus() { return locus; }
    public Eligibility.Locus previousLocus() { return previousLocus; }
    public long blockedAttempts() { return blockedAttempts; }
    /** False until the opportunity's first manifestation of the match. */
    public boolean hasManifested() { return hasManifested; }

    /**
     * Hold the first manifestation back until the opening has had time to breathe.
     *
     * A regenerative economy that is live on the first tick competes with the
     * Bootstrap it is meant to follow. The delay is expressed as a fraction of
     * the Temporal Phase and is applied by recovering from zero at a first-cycle
     * rate, so it needs no separate timer and shifts automatically if the phase
     * length changes.
     */
    public void beginInitialDelay() {
        state = State.RECOVERING;
        recoveryProgress = 0;
    }

    /** A fresh opportunity has never manifested and is ready to try. */
    public static Opportunity fresh() { return new Opportunity(); }

    /**
     * Accumulate recovery. Only RECOVERING accumulates, and only up to complete.
     *
     * Completion moves to READY_AWAITING_LOCUS rather than to MANIFESTED,
     * because whether a manifestation can happen is a question about the world
     * and this class does not have one.
     */
    public void tickRecovery(long ticks, boolean night, Recovery.Rates rates) {
        if (state != State.RECOVERING) return;
        recoveryProgress = Recovery.advance(recoveryProgress, ticks, night, rates);
        if (Recovery.complete(recoveryProgress)) {
            state = State.READY_AWAITING_LOCUS;
            recoveryProgress = 1.0;
        }
    }

    /** Whether a manifestation attempt is currently warranted. */
    public boolean readyToManifest() { return state == State.READY_AWAITING_LOCUS; }

    /** Record that an attempt found nowhere eligible. The state does not change. */
    public void noEligibleLocus() {
        if (state == State.READY_AWAITING_LOCUS) blockedAttempts++;
    }

    /** A manifestation was created at this locus with this much in it. */
    public void manifested(Eligibility.Locus at, int size) {
        if (size <= 0) throw new IllegalArgumentException("a manifestation needs members");
        previousLocus = locus;
        locus = at;
        hasManifested = true;
        remaining = size;
        recoveryProgress = 0;
        state = State.MANIFESTED;
    }

    /**
     * One member harvested, captured or otherwise removed from the wild.
     *
     * Partial removal changes only the count. Recovery begins at zero and not
     * before, which is what makes an unresolved manifestation a standing
     * opportunity rather than a decaying one.
     */
    public void memberRemoved() {
        if (state != State.MANIFESTED) return;
        remaining = Math.max(0, remaining - 1);
        if (remaining == 0) {
            state = State.RECOVERING;
            recoveryProgress = 0;
            locus = null;          // the locus was the manifestation's, not the opportunity's
        }
    }

    /** Re-sync to an externally counted membership, e.g. after a chunk reload. */
    public void observeRemaining(int count) {
        if (state != State.MANIFESTED) return;
        remaining = Math.max(0, count);
        if (remaining == 0) { state = State.RECOVERING; recoveryProgress = 0; locus = null; }
    }

    /** Match reset: the opportunity persists, every manifestation is discarded. */
    public void discardManifestation() {
        state = State.READY_AWAITING_LOCUS;
        recoveryProgress = 1.0;
        remaining = 0;
        locus = null;
        previousLocus = null;
        blockedAttempts = 0;
        hasManifested = false;
    }

    @Override public String toString() {
        return state + (state == State.RECOVERING
                ? String.format(" %.0f%%", recoveryProgress * 100)
                : state == State.MANIFESTED ? " x" + remaining + " at " + locus
                : blockedAttempts > 0 ? " (no eligible locus, " + blockedAttempts + " attempt(s))" : "");
    }
}
