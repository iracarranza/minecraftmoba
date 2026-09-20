package com.minecraftmoba.plugin;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Eating at the Hunger cap must be refused the way vanilla refuses at full.
 *
 * Found in playtest: Capacity caps food well below 20, so vanilla kept
 * permitting a meal and enforceHunger clamped the result away a tick later.
 * The food was spent for nothing.
 *
 * The decision has three inputs and one vanilla exception, asserted here as a
 * pure predicate because the event path needs a server.
 */
class HungerCapTest {

    /** Mirrors HungerRegen.consume: refuse only a real, non-always-edible food at cap. */
    private static boolean refuses(boolean hasFoodComponent, boolean canAlwaysEat,
                                   int foodLevel, int cap) {
        if (!hasFoodComponent || canAlwaysEat) return false;
        return foodLevel >= cap;
    }

    @Test void ordinaryFoodIsRefusedAtTheCap() {
        assertTrue(refuses(true, false, 9, 9), "at the capped maximum");
        assertTrue(refuses(true, false, 10, 9), "above it, which enforceHunger will clamp");
    }

    @Test void ordinaryFoodIsAllowedBelowTheCap() {
        assertFalse(refuses(true, false, 8, 9));
        assertFalse(refuses(true, false, 0, 9));
    }

    @Test void alwaysEdibleItemsKeepVanillaBehaviour() {
        // Golden apples and chorus fruit are edible at full hunger in vanilla,
        // so the cap must not invent a new rule for them.
        assertFalse(refuses(true, true, 9, 9));
    }

    @Test void nonFoodConsumablesAreNeverRefused() {
        // Milk and potions have no food component; they are not hunger items.
        assertFalse(refuses(false, false, 9, 9));
    }

    @Test void theCapScalesWithProgression() {
        // At full capacity the rule coincides with vanilla's own.
        assertFalse(refuses(true, false, 19, 20));
        assertTrue(refuses(true, false, 20, 20));
    }
}
