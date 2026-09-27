package com.minecraftmoba.plugin;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Option placement, derived from the slot count rather than fixed at three.
 *
 * The derivation is the point: a future two- or four-option choice must not
 * silently inherit a gap sized for three, which is exactly the kind of drift
 * that would show up as a mechanic that mysteriously fails half the time.
 */
class SelectionLayoutTest {

    @Test void threeSlotsPutTheMiddleOptionOnTheAimPoint() {
        assertArrayEquals(new double[]{-1, 0, 1}, SelectionLayout.offsets(3, 1.0), 1e-9);
    }

    /** Any odd count centres an option exactly, so "look at open space" means that space. */
    @Test void everyOddCountCentresAnOption() {
        assertEquals(0.0, SelectionLayout.offsets(1, 1.0)[0], 1e-9);
        assertEquals(0.0, SelectionLayout.offsets(5, 1.0)[2], 1e-9);
    }

    @Test void evenCountsStraddleTheAimPoint() {
        assertArrayEquals(new double[]{-0.5, 0.5}, SelectionLayout.offsets(2, 1.0), 1e-9);
    }

    @Test void spacingScalesTheRow() {
        assertArrayEquals(new double[]{-3, 0, 3}, SelectionLayout.offsets(3, 3.0), 1e-9);
    }

    @Test void widthGrowsWithTheSlotCount() {
        assertEquals(1, SelectionLayout.requiredWidth(1, 1.0), 1e-9);
        assertEquals(3, SelectionLayout.requiredWidth(3, 1.0), 1e-9);
        assertEquals(5, SelectionLayout.requiredWidth(5, 1.0), 1e-9);
    }

    /** A single option still needs a block to occupy. */
    @Test void widthIsNeverZero() {
        assertTrue(SelectionLayout.requiredWidth(1, 0.5) > 0);
    }

    /**
     * Ceiling, not exact half-width. Block checks are integers, and rounding
     * down would approve a space one block short at every even count.
     */
    @Test void clearRadiusRoundsUp() {
        assertEquals(2, SelectionLayout.clearRadiusBlocks(3, 1.0));   // width 3 -> 1.5 -> 2
        assertEquals(1, SelectionLayout.clearRadiusBlocks(1, 1.0));   // width 1 -> 0.5 -> 1
        assertEquals(1, SelectionLayout.clearRadiusBlocks(2, 1.0));   // width 2 -> 1.0 -> 1
    }

    @Test void degenerateInputsAreRefusedRatherThanSilentlyEmpty() {
        assertThrows(IllegalArgumentException.class, () -> SelectionLayout.offsets(0, 1.0));
        assertThrows(IllegalArgumentException.class, () -> SelectionLayout.offsets(3, 0));
        assertThrows(IllegalArgumentException.class, () -> SelectionLayout.requiredWidth(0, 1.0));
        assertThrows(IllegalArgumentException.class, () -> SelectionLayout.requiredWidth(3, -1));
    }

    @Test void theShippedConfigsSlotCountMatchesTheDesignsThree() {
        // N is always three: three Task trees, three ability branches.
        assertArrayEquals(new double[]{-1, 0, 1}, SelectionLayout.offsets(3, 1.0), 1e-9);
        assertEquals(2, SelectionLayout.clearRadiusBlocks(3, 1.0));
    }
}
