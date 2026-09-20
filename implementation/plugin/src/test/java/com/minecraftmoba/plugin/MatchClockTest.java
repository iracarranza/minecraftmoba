package com.minecraftmoba.plugin;

import org.junit.jupiter.api.Test;

import static com.minecraftmoba.plugin.MatchClock.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * The schedule is specified, not invented: day 6 min, night 6 min, 48-minute
 * match, sunsets at 6/18/30/42 (Economic Calibration Supplement, 14 Sept).
 * These assert the published numbers rather than the implementation's opinion.
 */
class MatchClockTest {
    private static long atMinute(long m) { return m * 60 * 20; }

    @Test void phasesFollowThePublishedTable() {
        assertEquals(Phase.DAY, phaseAt(atMinute(0)));
        assertEquals(Phase.DAY, phaseAt(atMinute(5)));
        assertEquals(Phase.NIGHT, phaseAt(atMinute(6)));
        assertEquals(Phase.NIGHT, phaseAt(atMinute(11)));
        assertEquals(Phase.DAY, phaseAt(atMinute(12)));
        assertEquals(Phase.DAY, phaseAt(atMinute(36)));
        assertEquals(Phase.NIGHT, phaseAt(atMinute(42)));
        assertEquals(Phase.NIGHT, phaseAt(atMinute(47)));
    }

    @Test void sunsetsFireAtSixEighteenThirtyFortyTwo() {
        for (long m : new long[]{6, 18, 30, 42})
            assertTrue(isSunsetBoundary(atMinute(m)), "sunset expected at " + m + "m");
        for (long m : new long[]{0, 5, 7, 12, 24, 36, 48})
            assertFalse(isSunsetBoundary(atMinute(m)), "no sunset at " + m + "m");
    }

    @Test void exactlyFourSunsetsInAMatch() {
        int count = 0;
        for (long t = 1; t <= MATCH_TICKS; t++) if (isSunsetBoundary(t)) count++;
        assertEquals(4, count);
    }

    @Test void sunsetsAreNumberedInOrder() {
        assertEquals(1, sunsetOrdinal(atMinute(6)));
        assertEquals(2, sunsetOrdinal(atMinute(18)));
        assertEquals(3, sunsetOrdinal(atMinute(30)));
        assertEquals(4, sunsetOrdinal(atMinute(42)));
    }

    @Test void sunrisesFireAtTwelveTwentyFourThirtySix() {
        for (long m : new long[]{12, 24, 36})
            assertTrue(isSunriseBoundary(atMinute(m)), "sunrise expected at " + m + "m");
        assertFalse(isSunriseBoundary(0), "match start is not a sunrise");
    }

    @Test void cycleIsTwelveMinutesAndMatchIsFortyEight() {
        assertEquals(12 * 60 * 20, CYCLE_TICKS);
        assertEquals(48 * 60 * 20, MATCH_TICKS);
        assertFalse(pastHorizon(MATCH_TICKS - 1));
        assertTrue(pastHorizon(MATCH_TICKS));
    }

    @Test void worldTimeTracksTheCompressedPhase() {
        assertEquals(0, worldTime(0));
        assertEquals(6000, worldTime(atMinute(3)));      // midday
        assertEquals(12000, worldTime(atMinute(6)));     // dusk
        assertEquals(18000, worldTime(atMinute(9)));     // midnight
        assertEquals(0, worldTime(atMinute(12)));        // dawn again
    }
}
