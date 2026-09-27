package com.minecraftmoba.plugin;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** The beat arithmetic and the four conditions, without a server. */
class SelectionBeatTest {

    private static final long BEAT = 100;      // 5s
    private static final long STILLNESS = 20;  // 1s
    private final UUID player = UUID.randomUUID();

    private SelectionBeat beat() { return new SelectionBeat(BEAT, STILLNESS); }

    private SelectionBeat.Conditions all(boolean crouch, boolean ground, boolean still,
                                         boolean combat, boolean aim) {
        return new SelectionBeat.Conditions(crouch, ground, still, combat, aim);
    }

    // ---- conditions -------------------------------------------------------

    @Test void allFourConditionsAreRequired() {
        assertTrue(all(true, true, true, false, true).satisfied());
        assertFalse(all(false, true, true, false, true).satisfied(), "not crouching");
        assertFalse(all(true, false, true, false, true).satisfied(), "airborne");
        assertFalse(all(true, true, false, false, true).satisfied(), "moving");
        assertFalse(all(true, true, true, true, true).satisfied(), "in combat");
        assertFalse(all(true, true, true, false, false).satisfied(), "no eligible space");
    }

    /**
     * Combat is reported first. It is the one condition imposed on the player
     * rather than chosen by them, so it is the one they cannot see coming.
     */
    @Test void combatIsReportedAheadOfTheChosenConditions() {
        assertEquals("in combat", all(false, false, false, true, false).unmetReason());
        assertEquals("crouch to choose", all(false, true, true, false, true).unmetReason());
        assertEquals("stand on solid ground", all(true, false, true, false, true).unmetReason());
        assertEquals("hold still", all(true, true, false, false, true).unmetReason());
        assertEquals("look at open space", all(true, true, true, false, false).unmetReason());
        assertNull(all(true, true, true, false, true).unmetReason());
    }

    // ---- stillness --------------------------------------------------------

    @Test void aPlayerNeverSeenMovingCountsAsStill() {
        assertTrue(beat().stationary(player, 0));
    }

    @Test void stillnessNeedsTheFullTrailingWindow() {
        var b = beat();
        b.moved(player, 1000);
        assertFalse(b.stationary(player, 1000));
        assertFalse(b.stationary(player, 1000 + STILLNESS - 1));
        assertTrue(b.stationary(player, 1000 + STILLNESS));
    }

    // ---- the countdown ----------------------------------------------------

    /** The first call starts the clock rather than firing, or every beat would fire instantly. */
    @Test void theFirstEvaluationStartsTheClock() {
        var b = beat();
        assertFalse(b.due(player, 500));
        assertFalse(b.due(player, 500 + BEAT - 1));
        assertTrue(b.due(player, 500 + BEAT));
    }

    @Test void restartingPushesTheEvaluationOut() {
        var b = beat();
        b.restart(player, 500);
        b.restart(player, 560);
        assertFalse(b.due(player, 560 + BEAT - 1));
        assertTrue(b.due(player, 560 + BEAT));
    }

    @Test void remainingCountsDownAndFloorsAtZero() {
        var b = beat();
        b.restart(player, 500);
        assertEquals(BEAT, b.remaining(player, 500));
        assertEquals(BEAT - 40, b.remaining(player, 540));
        assertEquals(0, b.remaining(player, 500 + BEAT));
        assertEquals(0, b.remaining(player, 500 + BEAT + 999));
    }

    @Test void progressRunsFromZeroToOne() {
        var b = beat();
        b.restart(player, 500);
        assertEquals(0.0, b.progress(player, 500), 1e-9);
        assertEquals(0.5, b.progress(player, 550), 1e-9);
        assertEquals(1.0, b.progress(player, 500 + BEAT), 1e-9);
    }

    @Test void clearForgetsThePlayer() {
        var b = beat();
        b.restart(player, 500);
        b.moved(player, 500);
        b.clear(player);
        assertEquals(0, b.tracked());
        assertTrue(b.stationary(player, 500));
    }

    /**
     * The design's central case: escape, dig, crouch on the beat.
     *
     * State at the END is what counts, so conduct across the interval -- having
     * been in combat and having been moving, moments ago -- must not disqualify
     * a player who is now still, crouched and safe.
     */
    @Test void conductAcrossTheIntervalDoesNotDisqualify() {
        var b = beat();
        b.restart(player, 0);
        b.moved(player, 60);                       // still running at t=60
        assertTrue(b.due(player, BEAT));           // beat lands at t=100
        assertTrue(b.stationary(player, BEAT));    // and they stopped 40 ticks ago
        assertTrue(all(true, true, b.stationary(player, BEAT), false, true).satisfied());
    }
}
