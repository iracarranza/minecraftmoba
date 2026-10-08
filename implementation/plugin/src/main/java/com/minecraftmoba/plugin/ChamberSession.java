package com.minecraftmoba.plugin;

import java.util.List;
import java.util.Objects;

/**
 * A placement the tester is looking at but has not committed to.
 *
 * <h2>Confirm and cancel are not reimplemented here</h2>
 *
 * {@link AimState} with {@link CastMode#DOUBLE} already means "the first press
 * previews, the same input confirms, another input cancels". That is this
 * gesture exactly, against a different verb, and it has had no consumer since
 * it was written. Using it rather than writing a second state machine is what
 * stops the chamber and the ability system disagreeing about what a second
 * press means.
 *
 * <h2>What this adds that an ability does not need</h2>
 *
 * An ability's preview is advisory: a Mole can tunnel into bad ground and find
 * out. A placement's preview is a <b>verdict</b> -- it already knows whether
 * the thing fits, whether it escapes the chamber and what its faults are -- so
 * a session carries that verdict alongside the geometry and refuses to commit
 * a placement that was never acceptable.
 */
public final class ChamberSession {

    /** What a pending placement is, and what was found wrong with it. */
    public record Pending(String description,
                          List<Chamber.At> affected,
                          List<String> faults,
                          boolean escapes) {

        public Pending {
            Objects.requireNonNull(description);
            affected = List.copyOf(affected);
            faults = List.copyOf(faults);
        }

        /** Whether this may be committed at all. */
        public boolean acceptable() { return !escapes() && faults().isEmpty() && !affected().isEmpty(); }

        /** One line for the notice row, saying the worst thing first. */
        public String verdict() {
            if (affected().isEmpty()) return "NOTHING TO PLACE";
            if (escapes()) return "LEAVES THE CHAMBER";
            if (!faults().isEmpty()) return faults().size() + " FAULT" + (faults().size() == 1 ? "" : "S");
            return affected().size() + " BLOCKS";
        }
    }

    /** What the session decided this tick. */
    public enum Outcome { WAITING, APPLIED, CANCELLED, REFUSED }

    private Pending pending;
    private AimState aim;

    public boolean idle() { return pending == null; }
    public Pending pending() { return pending; }
    public AimState aim() { return aim; }

    /** Offer a placement for confirmation. Replaces anything already pending. */
    public void offer(Pending candidate, AimState state) {
        this.pending = Objects.requireNonNull(candidate);
        this.aim = state;
    }

    public void clear() { pending = null; aim = null; }

    /**
     * Resolve an aim decision into what the session does.
     *
     * A FIRE on an unacceptable placement is REFUSED rather than applied, and
     * the pending placement is kept. The tester pressed confirm because the
     * preview was in front of them, so throwing it away would make them rebuild
     * it to read why it was refused -- and the reason is the thing they now
     * want.
     */
    public Outcome resolve(AimState.Decision decision) {
        if (pending == null) return Outcome.WAITING;
        return switch (decision) {
            case HOLD -> Outcome.WAITING;
            case CANCEL -> { clear(); yield Outcome.CANCELLED; }
            case FIRE -> {
                if (!pending.acceptable()) yield Outcome.REFUSED;
                clear();
                yield Outcome.APPLIED;
            }
        };
    }
}
