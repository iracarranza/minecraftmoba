package com.minecraftmoba.plugin;

import org.junit.jupiter.api.Test;

import static com.minecraftmoba.plugin.MatchClock.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Vanilla day/night timing: a 24,000-tick cycle, twenty real minutes, with
 * sunset at tick 12,000.
 *
 * The clock previously compressed the cycle to twelve minutes. These assert
 * vanilla's own numbers rather than the implementation's opinion, and the
 * conversion test in particular asserts that there is NO conversion: the
 * compression is what made the first Worksite window open and close inside a
 * single opening expedition.
 */
class MatchClockTest {
    private static long atMinute(long m) { return m * 60 * 20; }

    @Test void aCycleIsVanillasTwentyMinutes() {
        assertEquals(24000, CYCLE_TICKS, "vanilla full day/night cycle");
        assertEquals(20, CYCLE_TICKS / (60 * 20), "minutes per cycle");
        assertEquals(12000, SUNSET_TICK, "vanilla sunset");
    }

    @Test void phasesFollowVanilla() {
        assertEquals(Phase.DAY, phaseAt(atMinute(0)));
        assertEquals(Phase.DAY, phaseAt(atMinute(9)));
        assertEquals(Phase.NIGHT, phaseAt(atMinute(10)));
        assertEquals(Phase.NIGHT, phaseAt(atMinute(19)));
        assertEquals(Phase.DAY, phaseAt(atMinute(20)));
        assertEquals(Phase.NIGHT, phaseAt(atMinute(30)));
    }

    @Test void sunsetsFireAtTenThirtyFiftySeventy() {
        for (long m : new long[]{10, 30, 50, 70})
            assertTrue(isSunsetBoundary(atMinute(m)), "sunset expected at " + m + "m");
        for (long m : new long[]{0, 6, 9, 20, 40, 60, 80})
            assertFalse(isSunsetBoundary(atMinute(m)), "no sunset at " + m + "m");
    }

    @Test void anOpeningExpeditionCannotOutlastTheFirstWindow() {
        // The live failure, stated as a property. The first activation window
        // runs from sunset 1 to the next sunrise; at the old compression that
        // was six minutes, which a first mining trip simply outlasts.
        long window = CYCLE_TICKS - SUNSET_TICK;
        assertEquals(10, window / (60 * 20), "the window is ten minutes, not six");
    }

    @Test void exactlyFourSunsetsInAMatch() {
        // The activation series still has four entries, and that is what sets
        // the match length rather than the other way round.
        int count = 0;
        for (long t = 1; t <= MATCH_TICKS; t++) if (isSunsetBoundary(t)) count++;
        assertEquals(4, count);
    }

    @Test void sunsetsAreNumberedInOrder() {
        assertEquals(1, sunsetOrdinal(atMinute(10)));
        assertEquals(2, sunsetOrdinal(atMinute(30)));
        assertEquals(3, sunsetOrdinal(atMinute(50)));
        assertEquals(4, sunsetOrdinal(atMinute(70)));
    }

    @Test void sunrisesFireAtTwentyFortySixty() {
        for (long m : new long[]{20, 40, 60})
            assertTrue(isSunriseBoundary(atMinute(m)), "sunrise expected at " + m + "m");
        assertFalse(isSunriseBoundary(0), "match start is not a sunrise");
    }

    @Test void theMatchIsEightyMinutes() {
        assertEquals(80 * 60 * 20, MATCH_TICKS);
        assertFalse(pastHorizon(MATCH_TICKS - 1));
        assertTrue(pastHorizon(MATCH_TICKS));
    }

    @Test void worldTimeIsElapsedTimeWithNoCompression() {
        // A match tick IS a world tick, so sunset, moonrise and mob spawning
        // land exactly where a player's vanilla instincts expect.
        assertEquals(0, worldTime(0));
        assertEquals(6000, worldTime(atMinute(5)));       // midday
        assertEquals(12000, worldTime(atMinute(10)));     // dusk
        assertEquals(18000, worldTime(atMinute(15)));     // midnight
        assertEquals(0, worldTime(atMinute(20)));         // dawn again
        for (long t : new long[]{0, 1234, 9999, 23999})
            assertEquals(t, worldTime(t), "no scaling factor may reappear");
    }
}
