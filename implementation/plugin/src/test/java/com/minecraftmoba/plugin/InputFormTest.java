package com.minecraftmoba.plugin;

import org.junit.jupiter.api.Test;

import static com.minecraftmoba.plugin.InputForm.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Input form, read against the reference case: Pantheon's Q.
 *
 * A tap is a short spear stab; a hold is a long spear throw. Two outputs of one
 * ability, chosen by gesture -- which is why input form is the ability's
 * property and not the player's. A preference that reshaped the gesture would
 * take an output away.
 */
class InputFormTest {

    /** The stab is a bare press, so every mode's gesture fits. */
    @Test void theStabAcceptsEveryCastMode() {
        for (CastMode mode : CastMode.values()) assertTrue(INSTANT.permits(mode), mode.name());
        assertTrue(INSTANT.pressable());
    }

    /**
     * The throw cannot be Quick cast: Quick is a bare press, which produces
     * the stab instead.
     */
    @Test void theThrowCannotBeQuickCast() {
        assertFalse(CHANNELED.permits(CastMode.QUICK));
        assertFalse(CHANNELED.pressable(), "there is no press that produces it");
    }

    /**
     * And it cannot be Double cast either, which is the less obvious half.
     *
     * Double needs a first press that does not commit, so that a second can
     * confirm. A held form has no such press -- the hold IS the activation, so
     * a double-cast throw would have to be press, release, press, hold, which
     * is not a gesture anyone makes.
     */
    @Test void theThrowCannotBeDoubleCastEither() {
        assertFalse(CHANNELED.permits(CastMode.DOUBLE));
        assertTrue(CHANNELED.permits(CastMode.HOLD), "Hold is the shape it already is");
    }

    /** Charged is Channeled plus measurement, and inherits every restriction. */
    @Test void chargedIsChanneledWithDurationThatMatters() {
        for (CastMode mode : CastMode.values())
            assertEquals(CHANNELED.permits(mode), CHARGED.permits(mode), mode.name());
    }

    /**
     * A preference that cannot be honoured falls back rather than refusing.
     *
     * A player who prefers Quick still gets the throw when they hold; they
     * simply do not get a version of it that fires on a press, because no such
     * version exists.
     */
    @Test void anImpossiblePreferenceFallsBackToTheFormsOwnShape() {
        assertEquals(CastMode.HOLD, CHANNELED.resolve(CastMode.QUICK));
        assertEquals(CastMode.HOLD, CHARGED.resolve(CastMode.DOUBLE));
        assertEquals(CastMode.DOUBLE, INSTANT.resolve(CastMode.DOUBLE),
                "and an instant ability keeps whatever the player chose");
    }

    /**
     * The three axes are independent.
     *
     * Input form is HOW the gesture is made, TargetForm is WHAT is aimed at,
     * CastMode is WHETHER the aim is verified. Pantheon's Q is direction
     * targeted in both of its forms, which is why targeting cannot be the
     * thing that distinguishes them.
     */
    @Test void formAndTargetingAreDifferentQuestions() {
        assertEquals(3, InputForm.values().length);
        assertEquals(4, TargetForm.values().length);
        assertEquals(3, CastMode.values().length);
    }
}
