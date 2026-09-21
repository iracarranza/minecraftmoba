package com.minecraftmoba.plugin;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Band boundaries, which are easy to land one level early or late.
 *
 * Each entry is the cost of *leaving* that level, so level 6 still costs its
 * Bootstrap price and level 7 is the first charged at Established rates. The
 * curve is a per-level sawtooth: it compounds ~15% inside an economic band and
 * drops on entering the next one, so a boundary is a step DOWN in per-level
 * cost. That makes an off-by-one here easy to miss by eye — 605 then 405 looks
 * like a bug — which is why the boundary levels are pinned individually.
 */
class BandBoundaryTest {

    /** The config's level table, as the live lookup resolves it. */
    private static final int[] COST = {
            0,
            300, 345, 395, 455, 525, 605,                 // Bootstrap    1-6
            405, 465, 535, 615, 710, 815,                 // Established  7-12
            560, 645, 745, 855, 985, 1130, 1300,          // Developed    13-19
            770, 890, 1020, 1175, 1350,                   // Advanced     20-24
            975, 1120, 1290, 1485, 1705};                 // Endgame      25-29

    private static int cost(int level) { return level < COST.length ? COST[level] : 0; }

    @Test void eachBoundaryChargesTheOldBandForTheLevelBeingLeft() {
        assertEquals(605, cost(6), "6 -> 7 is still Bootstrap");
        assertEquals(405, cost(7), "7 -> 8 is the first Established");
        assertEquals(815, cost(12), "12 -> 13 is the last Established");
        assertEquals(560, cost(13), "13 -> 14 is the first Developed");
        assertEquals(1300, cost(19), "19 -> 20 is the last Developed");
        assertEquals(770, cost(20), "20 -> 21 is the first Advanced");
        assertEquals(1350, cost(24), "24 -> 25 is the last Advanced");
        assertEquals(975, cost(25), "25 -> 26 is the first Endgame");
    }

    @Test void theBoundaryIsAStepDownNotAStepUp() {
        assertTrue(cost(7) < cost(6));
        assertTrue(cost(13) < cost(12));
        assertTrue(cost(20) < cost(19));
        assertTrue(cost(25) < cost(24));
    }

    /** Mirrors award(): consume each level's own cost, stop at the cap. */
    private static int[] apply(int level, int xp, int wp, int max) {
        if (level >= max) return new int[]{level, 0};
        long total = (long) xp + wp;
        while (level < max && total >= cost(level)) { total -= cost(level); level++; }
        return new int[]{level, level >= max ? 0 : (int) total};
    }

    @Test void crossingABoundaryPaysBothLevels() {
        // From level 6 with 0 WP: 605 reaches 7, and a further 405 reaches 8.
        assertArrayEquals(new int[]{7, 0}, apply(6, 0, 605, 30));
        assertArrayEquals(new int[]{8, 0}, apply(6, 0, 1010, 30));
        assertArrayEquals(new int[]{7, 404}, apply(6, 0, 1009, 30));
    }

    @Test void maxLevelDoesNotOverflowOrAccumulate() {
        // No level 31, and no meaningless counter behind a stale denominator.
        assertArrayEquals(new int[]{30, 0}, apply(30, 0, 99999, 30));
        assertArrayEquals(new int[]{30, 0}, apply(29, 0, 99999, 30));
    }

    @Test void partialProgressIsKeptNotLost() {
        assertArrayEquals(new int[]{1, 299}, apply(1, 0, 299, 30));
        assertArrayEquals(new int[]{2, 0}, apply(1, 299, 1, 30));
    }

    @Test void aSingleLargeAwardCanCrossSeveralLevelsPayingEachPrice() {
        // 300 + 345 + 395 = 1040 is exactly levels 1 -> 4.
        assertArrayEquals(new int[]{4, 0}, apply(1, 0, 1040, 30));
        assertArrayEquals(new int[]{3, 394}, apply(1, 0, 1039, 30));
    }
}
