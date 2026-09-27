package com.minecraftmoba.plugin;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The combat-state arithmetic, which is all of it that is not a Bukkit event.
 *
 * Constructed with an explicit duration and driven with explicit ticks, so
 * these run without a server. What the listener DOES with these calls -- which
 * damage events count -- is documented at the handler and is not reachable
 * without one.
 */
class CombatStateTest {

    private static final long SEVEN_SECONDS = 140;
    private final UUID entity = UUID.randomUUID();
    private final UUID other = UUID.randomUUID();

    private CombatState state() { return new CombatState(null, SEVEN_SECONDS); }

    @Test void anUnknownEntityIsNotInCombat() {
        assertFalse(state().inCombat(entity, 0));
        assertEquals(0, state().remaining(entity, 0));
    }

    @Test void aQualifyingActionHoldsForTheFullDuration() {
        var s = state();
        s.mark(entity, 1000);
        assertTrue(s.inCombat(entity, 1000));
        assertEquals(SEVEN_SECONDS, s.remaining(entity, 1000));
        assertTrue(s.inCombat(entity, 1000 + SEVEN_SECONDS - 1));
    }

    /** The boundary is exclusive: at exactly duration ticks later, combat is over. */
    @Test void combatEndsExactlyOnTheDeadline() {
        var s = state();
        s.mark(entity, 1000);
        assertFalse(s.inCombat(entity, 1000 + SEVEN_SECONDS));
        assertEquals(0, s.remaining(entity, 1000 + SEVEN_SECONDS));
    }

    @Test void aLaterActionRefreshesRatherThanExtending() {
        var s = state();
        s.mark(entity, 1000);
        s.mark(entity, 1100);
        assertEquals(SEVEN_SECONDS, s.remaining(entity, 1100));
        assertTrue(s.inCombat(entity, 1100 + SEVEN_SECONDS - 1));
        assertFalse(s.inCombat(entity, 1100 + SEVEN_SECONDS));
    }

    /**
     * Keyed by entity, which is Deadline's requirement: a Crew Member's combat
     * state is its own, and a summon's never propagates to its commander.
     */
    @Test void entitiesDoNotShareState() {
        var s = state();
        s.mark(entity, 1000);
        assertTrue(s.inCombat(entity, 1000));
        assertFalse(s.inCombat(other, 1000));
    }

    @Test void clearRemovesAnEntity() {
        var s = state();
        s.mark(entity, 1000);
        s.clear(entity);
        assertFalse(s.inCombat(entity, 1000));
    }

    /** The map must not grow with the world; expired entries are dropped. */
    @Test void sweepDropsOnlyExpiredEntries() {
        var s = state();
        s.mark(entity, 1000);
        s.mark(other, 1100);
        s.sweep(1000 + SEVEN_SECONDS);
        assertEquals(1, s.tracked());
        assertFalse(s.inCombat(entity, 1000 + SEVEN_SECONDS));
        assertTrue(s.inCombat(other, 1000 + SEVEN_SECONDS));
    }

    @Test void remainingCountsDown() {
        var s = state();
        s.mark(entity, 1000);
        assertEquals(SEVEN_SECONDS - 40, s.remaining(entity, 1040));
    }
}
