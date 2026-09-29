package com.minecraftmoba.plugin;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Ported from {@code codex/lightfooted-from-phase1} with its cases intact.
 *
 * The boundaries are what matter here: a cooldown that is ready one tick early
 * or one tick late is invisible in play and permanent in the code.
 */
class AbilityCooldownsTest {

    private final UUID a = UUID.randomUUID(), b = UUID.randomUUID();

    @Test void activationBlocksUntilTheDeadlineAndNotPast() {
        var c = new AbilityCooldowns();
        assertTrue(c.ready(a, "a1", 0), "nothing is on cooldown before anything is cast");

        c.start(a, "a1", 0, 5);
        assertFalse(c.ready(a, "a1", 0));
        assertEquals(5, c.remaining(a, "a1", 0));
        assertEquals(3, c.remaining(a, "a1", 2));
        assertTrue(c.ready(a, "a1", 5), "ready ON the deadline tick, not after it");
    }

    @Test void clearTouchesOnlyWhatItNames() {
        var c = new AbilityCooldowns();
        c.start(a, "a1", 0, 8);
        c.start(a, "a2", 0, 4);
        c.start(b, "a1", 0, 8);

        c.clear(a, "a1");
        assertTrue(c.ready(a, "a1", 0));
        assertFalse(c.ready(a, "a2", 0), "a player's other abilities keep running");
        assertFalse(c.ready(b, "a1", 0), "another player's cooldown is untouched");

        c.clear(a);
        assertTrue(c.ready(a, "a2", 0));
        assertFalse(c.ready(b, "a1", 0));
    }

    @Test void durationsAreIndependentAndZeroIsReadyNow() {
        var c = new AbilityCooldowns();
        c.start(a, "a1", 10, 2);
        c.start(a, "a2", 10, 7);
        assertEquals(2, c.remaining(a, "a1", 10));
        assertEquals(7, c.remaining(a, "a2", 10));

        c.start(b, "a1", 10, 0);
        assertTrue(c.ready(b, "a1", 10),
                "a zero cooldown does not cost a tick; lastFire is what limits one cast per tick");
    }

    @Test void aNegativeDurationCannotPutAnAbilityInThePast() {
        var c = new AbilityCooldowns();
        c.start(a, "a1", 10, -50);
        assertTrue(c.ready(a, "a1", 10));
        assertEquals(0, c.remaining(a, "a1", 10),
                "clamped at the start, so remaining never reports a negative countdown");
    }

    @Test void remainingNeverGoesNegativeAfterExpiry() {
        var c = new AbilityCooldowns();
        c.start(a, "a1", 0, 5);
        assertEquals(0, c.remaining(a, "a1", 9999));
    }
}
