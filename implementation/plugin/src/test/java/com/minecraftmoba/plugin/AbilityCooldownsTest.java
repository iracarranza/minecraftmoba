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

    // ---- charges -----------------------------------------------------------
    //
    // Toolbox's A2 is three charges on an eight-second recharge, and
    // TOOLBOX_CIRCUIT_GRAMMAR.md 10D gives the arithmetic. 160 ticks is eight
    // seconds.

    private static final int N = 3;
    private static final long R = 160;

    @Test void anAbilityNeverUsedIsFullRatherThanEmpty() {
        var c = new AbilityCooldowns();
        assertEquals(N, c.available(a, "a2", 0, N, R));
        assertEquals(0, c.untilNextCharge(a, "a2", 0, N, R));
    }

    @Test void chargesSpendDownAndComeBackOneAtATime() {
        var c = new AbilityCooldowns();
        for (int i = N; i > 0; i--) {
            assertEquals(i, c.available(a, "a2", 0, N, R));
            c.spend(a, "a2", 0, N, R);
        }
        assertEquals(0, c.available(a, "a2", 0, N, R), "three spent, none left");
        assertEquals(R, c.untilNextCharge(a, "a2", 0, N, R));

        assertEquals(1, c.available(a, "a2", R, N, R), "one back after one recharge");
        assertEquals(2, c.available(a, "a2", 2 * R, N, R));
        assertEquals(3, c.available(a, "a2", 3 * R, N, R));
    }

    @Test void spendingOneDoesNotRestartTheOthers() {
        // The property that makes charges charges rather than a cooldown that
        // happens three times: advancing charge time by ONE recharge leaves
        // the rest of the accrual where it was.
        var c = new AbilityCooldowns();
        c.spend(a, "a2", 0, N, R);
        c.spend(a, "a2", 0, N, R);
        assertEquals(1, c.available(a, "a2", 0, N, R));

        // Spending the third at t=0 then waiting one recharge returns exactly
        // one, not all three.
        c.spend(a, "a2", 0, N, R);
        assertEquals(1, c.available(a, "a2", R, N, R));
    }

    @Test void chargesDoNotBankAboveTheMaximum() {
        // The max() clamp. An ability untouched for a very long time holds its
        // declared maximum and not a minute's worth.
        var c = new AbilityCooldowns();
        assertEquals(N, c.available(a, "a2", 100 * R, N, R));
        c.spend(a, "a2", 100 * R, N, R);
        assertEquals(N - 1, c.available(a, "a2", 100 * R, N, R),
                "spending from full leaves exactly one short, however long it idled");
    }

    @Test void aPartialRechargeGrantsNothingUntilItCompletes() {
        var c = new AbilityCooldowns();
        for (int i = 0; i < N; i++) c.spend(a, "a2", 0, N, R);
        assertEquals(0, c.available(a, "a2", R - 1, N, R));
        assertEquals(1, c.untilNextCharge(a, "a2", R - 1, N, R), "one tick short, and it says so");
        assertEquals(1, c.available(a, "a2", R, N, R));
    }

    @Test void aSingleChargeAbilityIsTheSameArithmeticWithNOfOne() {
        // The claim that justified not adding a second mechanism.
        var charges = new AbilityCooldowns();
        var plain = new AbilityCooldowns();
        charges.spend(a, "a1", 7, 1, 40);
        plain.start(b, "a1", 7, 40);

        for (long t = 7; t <= 50; t++)
            assertEquals(plain.remaining(b, "a1", t), charges.untilNextCharge(a, "a1", t, 1, 40),
                    "tick " + t);
    }

    @Test void remainingReadsBackTheTermsTheActivationUsed() {
        // Why the recharge is stored: remaining() is what the HUD asks, and a
        // charge time is an origin that means nothing on its own.
        var c = new AbilityCooldowns();
        for (int i = 0; i < N; i++) c.spend(a, "a2", 0, N, R);
        assertEquals(R, c.remaining(a, "a2", 0));
        assertEquals(0, c.remaining(a, "a2", R), "a charge is available, so nothing is remaining");
        assertEquals(1, c.available(a, "a2", R));
    }

    @Test void clearingRestoresAFullMagazine() {
        var c = new AbilityCooldowns();
        for (int i = 0; i < N; i++) c.spend(a, "a2", 0, N, R);
        assertEquals(0, c.available(a, "a2", 0, N, R));
        c.clear(a);
        assertEquals(N, c.available(a, "a2", 0, N, R), "a match reset returns a full set");
    }

    @Test void aZeroRechargeIsAlwaysFullyAvailable() {
        var c = new AbilityCooldowns();
        c.spend(a, "a2", 0, N, 0);
        assertEquals(N, c.available(a, "a2", 0, N, 0));
        assertEquals(0, c.untilNextCharge(a, "a2", 0, N, 0));
    }
}
