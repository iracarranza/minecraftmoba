package com.minecraftmoba.plugin;

import java.util.Objects;

/**
 * An ability being aimed rather than cast: what it is, and when it commits.
 *
 * <h2>A held AIM is not a held CAST</h2>
 *
 * The three cast modes are about <b>targeting and verification</b>, and nothing
 * else. They decide whether the player sees what they are about to affect
 * before it happens:
 *
 * <ul>
 *   <li><b>Quick</b> — cast with no verification.</li>
 *   <li><b>Hold</b> — hold the input, see the indicator, release to commit.</li>
 *   <li><b>Double</b> — press to see the indicator, press again to confirm.</li>
 * </ul>
 *
 * Whether an ability then <i>does something while held</i> is a separate
 * question that belongs to the ability. A persistent beam that runs until
 * released is a held CAST; it is not a held aim, and its length has nothing to
 * do with verification.
 *
 * This record serves both, because both ask the identical question of the
 * input layer — <i>is the button still down</i> — and answering it twice with
 * two heuristics would let them disagree. But they are different phases with
 * different meanings, and {@link Decision#FIRE} means "commit" while aiming and
 * "let go" while sustaining. Read the map it came out of.
 *
 * <h2>Release is detected by ABSENCE</h2>
 *
 * Bukkit has no button-up. There is no event for letting go of a mouse button,
 * and {@code org.bukkit.Input} -- the 1.21.2 raw input API -- carries movement
 * keys only, not clicks. So "held" cannot be read directly.
 *
 * What can be read is that a held button <b>keeps arriving</b>: the client
 * repeats its use and attack packets while aimed at a block, so
 * {@code PlayerInteractEvent} fires again every few ticks. Release is therefore
 * the input going quiet for longer than that gap.
 *
 * That makes {@link CastMode#HOLD} a heuristic and {@link CastMode#DOUBLE}
 * exact, which is worth knowing when one of them misbehaves: a Hold cast that
 * fires early was a dropped packet, and no amount of tuning makes it a promise.
 * The grace window is the whole tuning surface.
 *
 * <h2>Pure</h2>
 *
 * The tick comes in, like {@link CombatState} and {@link Charge}. What to do is
 * returned rather than done, so the decision can be checked against a table
 * without a server and without an ability.
 */
public record AimState(String abilityId, AbilityInputs.Input input, CastMode mode,
                       long startedTick, long lastInputTick) {

    public AimState {
        Objects.requireNonNull(abilityId, "An aim is always of some ability.");
        Objects.requireNonNull(input, "An aim is always on some input.");
        Objects.requireNonNull(mode, "An aim is always in some cast mode.");
    }

    /** What the input layer should do with an aim in progress. */
    public enum Decision {
        /** Keep aiming and keep drawing the preview. */
        HOLD,
        /** Commit: the release happened, or the second press did. */
        FIRE,
        /** Abandon it without casting. */
        CANCEL
    }

    /** A fresh aim, started now. */
    public static AimState begin(String abilityId, AbilityInputs.Input input,
                                 CastMode mode, long now) {
        return new AimState(abilityId, input, mode, now, now);
    }

    /** The same input arriving again while held keeps it alive. */
    public AimState refreshed(long now) {
        return new AimState(abilityId, input, mode, startedTick, now);
    }

    /**
     * Another activation arrived. Which one decides everything.
     *
     * The SAME input fires a Double cast and merely sustains a Hold -- under
     * Hold the repeats are the button still being down, so treating one as a
     * second press would fire instantly and make the mode useless.
     *
     * A DIFFERENT ability input always cancels, in both modes. Casting a
     * second ability is an unambiguous statement that the first is not
     * wanted, and it is the only way to abort a Double cast.
     */
    public Decision onInput(AbilityInputs.Input incoming) {
        if (incoming != input) return Decision.CANCEL;
        return mode == CastMode.DOUBLE ? Decision.FIRE : Decision.HOLD;
    }

    /**
     * No input this tick. Has it been let go, or timed out?
     *
     * @param graceTicks how long the input may be quiet before Hold counts it
     *                   as released. It must exceed the client's repeat gap
     *                   for THIS input, or a genuine hold fires between
     *                   packets -- and it is also the floor on how fast a TAP
     *                   can fire, so the two pull opposite ways and the repeat
     *                   gap is the only thing that settles it
     * @param maxTicks   the ceiling on aiming at all, for both modes; an aim
     *                   nobody resolves must not persist forever
     */
    public Decision onTick(long now, long graceTicks, long maxTicks) {
        if (now - startedTick >= maxTicks)
            return mode == CastMode.HOLD ? Decision.FIRE : Decision.CANCEL;
        if (mode == CastMode.HOLD && now - lastInputTick > graceTicks) return Decision.FIRE;
        return Decision.HOLD;
    }

    public long aimedTicks(long now) { return Math.max(0, now - startedTick); }
}
