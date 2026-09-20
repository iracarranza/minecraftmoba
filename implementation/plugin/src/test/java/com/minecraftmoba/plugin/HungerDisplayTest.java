package com.minecraftmoba.plugin;

import org.junit.jupiter.api.Test;

import static com.minecraftmoba.plugin.HungerDisplay.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * The readout must distinguish three states the vanilla row cannot: filled,
 * empty but refillable, and beyond the player's Capacity.
 */
class HungerDisplayTest {

    private static String legible(String glyphs) {
        return glyphs.replace(FULL, "F").replace(HALF, "h")
                     .replace(EMPTY, ".").replace(UNAVAILABLE, "x");
    }

    @Test void alwaysDrawsTenDrumsticks() {
        // The row keeps vanilla's width; only what each drumstick means changes.
        assertEquals(10, render(9, 9, 20).length());
        assertEquals(10, render(0, 20, 20).length());
    }

    @Test void theCapIsDrawnAsUnavailableNotAsEmpty() {
        // Level 1: 9 food points of capacity, so four and a half drumsticks.
        assertEquals("FFFFhxxxxx", legible(render(9, 9, 20)));
    }

    @Test void emptyBelowTheCapIsRefillableAndShownAsSuch() {
        // The fifth drumstick is empty, not half: food 4 fills only two of them,
        // and a cap of 9 still reaches into the fifth, so it is refillable.
        assertEquals("FF...xxxxx", legible(render(4, 9, 20)));
        assertEquals(".....xxxxx", legible(render(0, 9, 20)));
    }

    @Test void aHalfDrumstickAtTheCapIsEmptyBecauseAPointFits() {
        // A cap of 9 leaves one usable point in the fifth drumstick, so it is
        // refillable, not unavailable.
        assertTrue(legible(render(8, 9, 20)).startsWith("FFFF."));
    }

    @Test void fullCapacityShowsNoUnavailableDrumsticks() {
        assertEquals("FFFFFFFFFF", legible(render(20, 20, 20)));
        assertFalse(legible(render(10, 20, 20)).contains("x"));
    }

    @Test void halfStepsRenderWhereFoodIsOdd() {
        assertEquals("Fh........", legible(render(3, 20, 20)));
    }

    @Test void glyphsAreDistinctCodepoints() {
        // A collision would silently draw the wrong state.
        assertEquals(4, java.util.Set.of(FULL, HALF, EMPTY, UNAVAILABLE).size());
    }
}
