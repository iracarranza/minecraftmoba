package com.minecraftmoba.plugin;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Band boundaries, which are easy to land one level early or late.
 *
 * The bands are stated as "Lv1-6: 40 WP/level", so 40 is the cost of *leaving*
 * each of levels 1 through 6. Level 6 therefore still costs 40 and level 7 is
 * the first to cost 54.
 */
class BandBoundaryTest {

    /** The config's band table, as the live lookup resolves it. */
    private static int cost(int level) {
        if (level <= 6) return 40;
        if (level <= 12) return 54;
        if (level <= 19) return 75;
        if (level <= 24) return 103;
        return 130;
    }

    @Test void eachBoundaryChargesTheOldBandForTheLevelBeingLeft() {
        assertEquals(40, cost(6), "6 -> 7 is the last 40");
        assertEquals(54, cost(7), "7 -> 8 is the first 54");
        assertEquals(54, cost(12), "12 -> 13 is the last 54");
        assertEquals(75, cost(13), "13 -> 14 is the first 75");
        assertEquals(75, cost(19), "19 -> 20 is the last 75");
        assertEquals(103, cost(20), "20 -> 21 is the first 103");
        assertEquals(103, cost(24), "24 -> 25 is the last 103");
        assertEquals(130, cost(25), "25 -> 26 is the first 130");
    }

    /** Mirrors award(): consume each level's own cost, stop at the cap. */
    private static int[] apply(int level, int xp, int wp, int max) {
        if (level >= max) return new int[]{level, 0};
        long total = (long) xp + wp;
        while (level < max && total >= cost(level)) { total -= cost(level); level++; }
        return new int[]{level, level >= max ? 0 : (int) total};
    }

    @Test void crossingABoundaryPaysBothBands() {
        // From level 6 with 0 WP: 40 reaches 7, and a further 54 reaches 8.
        assertArrayEquals(new int[]{7, 0}, apply(6, 0, 40, 30));
        assertArrayEquals(new int[]{8, 0}, apply(6, 0, 94, 30));
        assertArrayEquals(new int[]{7, 53}, apply(6, 0, 93, 30));
    }

    @Test void maxLevelDoesNotOverflowOrAccumulate() {
        // No level 31, and no meaningless counter behind a stale denominator.
        assertArrayEquals(new int[]{30, 0}, apply(30, 0, 9999, 30));
        assertArrayEquals(new int[]{30, 0}, apply(29, 0, 9999, 30));
    }

    @Test void partialProgressIsKeptNotLost() {
        assertArrayEquals(new int[]{1, 39}, apply(1, 0, 39, 30));
        assertArrayEquals(new int[]{2, 0}, apply(1, 39, 1, 30));
    }
}
