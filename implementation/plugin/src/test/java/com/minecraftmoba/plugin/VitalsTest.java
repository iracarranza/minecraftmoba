package com.minecraftmoba.plugin;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Two units for one quantity, which is this codebase's most reliable source of
 * bugs -- two config schemas, two eligibility predicates, two world-load paths
 * and two resets all shipped broken because one copy was fixed and the other
 * was not.
 *
 * So the conversion is pinned: the round trip, the claim that scaling changes
 * nothing about combat, and the boundaries.
 */
class VitalsTest {

    @Test void theBarIsAlwaysTwentyPointsLong() {
        for (double capacity : new double[]{9, 11, 20, 24}) {
            assertEquals(20.0, Vitals.toDisplay(capacity, capacity), 1e-9,
                    "a full player reads as a full bar at Capacity " + capacity);
            assertEquals(0.0, Vitals.toDisplay(0, capacity), 1e-9);
        }
    }

    @Test void theRoundTripIsExact() {
        for (double capacity : new double[]{9, 11, 20, 24})
            for (double effective = 0; effective <= capacity; effective += 0.5)
                assertEquals(effective,
                        Vitals.toEffective(Vitals.toDisplay(effective, capacity), capacity), 1e-9);
    }

    @Test void scalingChangesNothingAboutCombat() {
        // The load-bearing claim. A hit removes the same FRACTION of a player
        // as it does today; only the bar's length changes.
        for (double capacity : new double[]{9, 11, 20, 24}) {
            double hit = 3.0;
            double fractionToday = hit / capacity;
            double fractionScaled = Vitals.scaleDamage(hit, capacity) / Vitals.DISPLAY_MAX;
            assertEquals(fractionToday, fractionScaled, 1e-9,
                    "Capacity " + capacity + " must take the same share of the player");
        }
    }

    @Test void aBiggerPoolTakesASmallerBite() {
        assertTrue(Vitals.scaleDamage(4, 9) > Vitals.scaleDamage(4, 24),
                "the same hit is worth less of a larger Capacity");
        assertEquals(4.0, Vitals.scaleDamage(4, 20), 1e-9, "Capacity 20 is the identity");
    }

    @Test void healingScalesTheSameWayAsDamage() {
        // Otherwise a fixed healing rate would be worth more to a small pool in
        // one direction and less in the other, which is not a rate at all.
        for (double capacity : new double[]{9, 20, 24})
            assertEquals(Vitals.scaleDamage(1.5, capacity), Vitals.scaleHealing(1.5, capacity), 1e-9);
    }

    @Test void reconstructionStillTakesLongerWithABiggerPool() {
        // ALPHA-D4 states the rate absolutely: raising maxima must lengthen
        // reconstruction. On a fixed-length bar that has to survive as a
        // smaller fraction filled per tick.
        double perTick = 1.0;
        double smallPool = Vitals.scaleHealing(perTick, 9) / Vitals.DISPLAY_MAX;
        double largePool = Vitals.scaleHealing(perTick, 24) / Vitals.DISPLAY_MAX;
        assertTrue(smallPool > largePool, "a larger Capacity must rebuild more slowly");
    }

    @Test void hungerCapacityBecomesARateAndEveryPositionIsReachable() {
        // The unreachable-drumstick problem, dissolved: nothing refuses to
        // fill, the row just empties faster.
        assertTrue(Vitals.exhaustionMultiplier(9) > 1.0, "a small Capacity burns faster");
        assertEquals(1.0, Vitals.exhaustionMultiplier(20), 1e-9, "Capacity 20 is the identity");
        assertTrue(Vitals.exhaustionMultiplier(24) < 1.0, "a large Capacity burns slower");
    }

    @Test void nonsenseCapacitiesDoNotDivideByZero() {
        assertEquals(0.0, Vitals.toDisplay(5, 0));
        assertEquals(0.0, Vitals.toEffective(5, 0));
        assertEquals(1.0, Vitals.exhaustionMultiplier(0));
        assertEquals(5.0, Vitals.scaleDamage(5, 0), 1e-9, "unscaled rather than infinite");
    }

    @Test void theBarCannotOverflow() {
        assertEquals(20.0, Vitals.toDisplay(99, 9), 1e-9);
        assertEquals(0.0, Vitals.toDisplay(-5, 9), 1e-9);
    }
}
