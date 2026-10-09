package com.minecraftmoba.plugin;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class LabTimeTest {
    @Test void wordsMapToTimes() {
        assertEquals(LabTime.TimeOfDay.DUSK, LabTime.TimeOfDay.parse("Sunset"));
        assertEquals(LabTime.TimeOfDay.MIDNIGHT, LabTime.TimeOfDay.parse("night"));
        assertEquals(LabTime.TimeOfDay.DAWN, LabTime.TimeOfDay.parse("day"));
        assertThrows(IllegalArgumentException.class, () -> LabTime.TimeOfDay.parse("teatime"));
    }

    @Test void jumpsToTheNextOccurrenceNeverBackwards() {
        assertEquals(12000, LabTime.ticksUntil(0, LabTime.TimeOfDay.DUSK));
        assertEquals(2000, LabTime.ticksUntil(10000, LabTime.TimeOfDay.DUSK));
        // Already past dusk this cycle: the next dusk is tomorrow's.
        assertEquals(23000, LabTime.ticksUntil(13000, LabTime.TimeOfDay.DUSK));
    }

    @Test void askingForTheTimeItAlreadyIsMovesAFullCycleNotZero() {
        assertEquals(24000, LabTime.ticksUntil(12000, LabTime.TimeOfDay.DUSK));
        assertEquals(24000, LabTime.ticksUntil(0, LabTime.TimeOfDay.DAWN));
    }

    @Test void landingOnTheTargetIsExact() {
        for (var t : LabTime.TimeOfDay.values())
            for (long e : new long[]{0, 1, 5999, 12000, 17999, 24000, 50000}) {
                long after = e + LabTime.ticksUntil(e, t);
                assertEquals(t.tick, after % MatchClock.CYCLE_TICKS);
                assertTrue(after > e);
            }
    }

    @Test void sunsetsFallWhereTheMatchClockSaysTheyDo() {
        for (int n = 1; n <= 6; n++) {
            assertTrue(MatchClock.isSunsetBoundary(LabTime.sunsetTick(n)), "night " + n);
            assertEquals(n, MatchClock.sunsetOrdinal(LabTime.sunsetTick(n)));
        }
    }

    @Test void nightJumpsLandOnTheSunsetAndRefuseThePast() {
        assertEquals(12000, LabTime.ticksUntilNight(0, 1));
        assertEquals(36000 - 5000, LabTime.ticksUntilNight(5000, 2));
        var ex = assertThrows(IllegalArgumentException.class, () -> LabTime.ticksUntilNight(13000, 1));
        assertTrue(ex.getMessage().contains("only runs forward"));
        assertThrows(IllegalArgumentException.class, () -> LabTime.sunsetTick(0));
    }

    @Test void minutesBecomeTicksAndMustBePositive() {
        assertEquals(1200, LabTime.ticksForMinutes(1));
        assertThrows(IllegalArgumentException.class, () -> LabTime.ticksForMinutes(0));
    }
}
