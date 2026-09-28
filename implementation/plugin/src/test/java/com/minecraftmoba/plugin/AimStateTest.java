package com.minecraftmoba.plugin;

import org.junit.jupiter.api.Test;

import static com.minecraftmoba.plugin.AimState.Decision.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Aiming: when a held ability commits, and when it is abandoned.
 *
 * The decisive asymmetry is {@link #theSameInputFiresADoubleCastAndSustainsAHold}.
 * Under Hold the repeats ARE the button still being down, so treating one as a
 * second press would fire instantly and make the mode useless.
 */
class AimStateTest {

    private static final long GRACE = 6, MAX = 200;
    private static final AbilityInputs.Input A1 = AbilityInputs.Input.LEFT_CLICK;
    private static final AbilityInputs.Input A2 = AbilityInputs.Input.RIGHT_CLICK;

    private AimState aim(CastMode mode) { return AimState.begin("tunnelling", A1, mode, 100); }

    // ---- another activation arrives ---------------------------------------

    @Test void theSameInputFiresADoubleCastAndSustainsAHold() {
        assertEquals(FIRE, aim(CastMode.DOUBLE).onInput(A1));
        assertEquals(HOLD, aim(CastMode.HOLD).onInput(A1),
                "under Hold the repeats are the button still being down");
    }

    /**
     * A different ability always cancels, in both modes.
     *
     * Casting a second ability is an unambiguous statement that the first is
     * not wanted, and it is the only way to abort a Double cast.
     */
    @Test void aDifferentInputCancelsEitherMode() {
        assertEquals(CANCEL, aim(CastMode.DOUBLE).onInput(A2));
        assertEquals(CANCEL, aim(CastMode.HOLD).onInput(A2));
    }

    // ---- silence ----------------------------------------------------------

    /**
     * Release is detected by ABSENCE, because Bukkit has no button-up.
     *
     * org.bukkit.Input carries movement keys only, not clicks. What can be
     * read is that a held button keeps arriving, so release is the input going
     * quiet for longer than the client's repeat gap.
     */
    @Test void holdFiresOnceTheInputGoesQuiet() {
        var held = aim(CastMode.HOLD);
        assertEquals(HOLD, held.onTick(100 + GRACE, GRACE, MAX), "still within the repeat gap");
        assertEquals(FIRE, held.onTick(100 + GRACE + 1, GRACE, MAX));
    }

    /** A refreshed aim starts the grace window again, but not the aim. */
    @Test void arrivingAgainKeepsItAliveWithoutRestartingIt() {
        var held = aim(CastMode.HOLD).refreshed(140);
        assertEquals(HOLD, held.onTick(145, GRACE, MAX));
        assertEquals(FIRE, held.onTick(147, GRACE, MAX));
        assertEquals(100, held.startedTick(), "the aim began when it began");
        assertEquals(47, held.aimedTicks(147));
    }

    /** Double never fires on silence: it is waiting for a press that may not come. */
    @Test void doubleIgnoresSilenceEntirely() {
        var pressed = aim(CastMode.DOUBLE);
        assertEquals(HOLD, pressed.onTick(100 + GRACE + 1, GRACE, MAX));
        assertEquals(HOLD, pressed.onTick(100 + MAX - 1, GRACE, MAX));
    }

    // ---- the ceiling -------------------------------------------------------

    /**
     * An aim nobody resolves must not persist forever, and the two modes
     * resolve it opposite ways.
     *
     * Under Hold the button was evidently still down, so the ceiling is a very
     * long press and fires. Under Double no second press ever came, so it is
     * an abandoned aim and cancels. Firing an unconfirmed Double cast at the
     * ceiling would cast something the player decided against.
     */
    @Test void theCeilingFiresAHoldAndCancelsADouble() {
        assertEquals(FIRE, aim(CastMode.HOLD).onTick(100 + MAX, GRACE, MAX));
        assertEquals(CANCEL, aim(CastMode.DOUBLE).onTick(100 + MAX, GRACE, MAX));
    }

    /** The ceiling wins over the grace window, so Hold cannot outlive it. */
    @Test void theCeilingIsCheckedBeforeTheGraceWindow() {
        var held = AimState.begin("tunnelling", A1, CastMode.HOLD, 0).refreshed(MAX);
        assertEquals(FIRE, held.onTick(MAX, GRACE, MAX));
    }

    // ---- shape -------------------------------------------------------------

    @Test void anAimAlwaysKnowsWhatItIs() {
        assertThrows(NullPointerException.class,
                () -> new AimState(null, A1, CastMode.HOLD, 0, 0));
        assertThrows(NullPointerException.class,
                () -> new AimState("x", null, CastMode.HOLD, 0, 0));
        assertThrows(NullPointerException.class,
                () -> new AimState("x", A1, null, 0, 0));
    }

    /**
     * Quick cast never reaches here.
     *
     * It is filtered at the input layer by CastMode.previews(), so an aim in
     * QUICK would be a bug elsewhere -- but if one existed it must not hang
     * forever, so silence still resolves it.
     */
    @Test void aQuickAimWouldStillResolveRatherThanHang() {
        assertFalse(CastMode.QUICK.previews());
        assertEquals(CANCEL, aim(CastMode.QUICK).onTick(100 + MAX, GRACE, MAX));
    }
}
