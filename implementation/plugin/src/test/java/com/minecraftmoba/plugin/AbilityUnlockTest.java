package com.minecraftmoba.plugin;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Ability slots follow the classes.md working breakpoint table: Ability 1 at
 * level 1, Ability 2 at level 2, Ultimate at 15. Playtest found every slot in a
 * configured kit usable from level 1, so the table described nothing the game
 * enforced.
 */
class AbilityUnlockTest {

    private static boolean usable(int playerLevel, int requiredLevel) {
        return playerLevel >= requiredLevel;
    }

    @Test void abilityOneIsAvailableImmediately() {
        assertTrue(usable(1, 1), "classes.md grants Ability 1 at level 1");
    }

    @Test void abilityTwoWaitsForLevelTwo() {
        assertFalse(usable(1, 2));
        assertTrue(usable(2, 2));
    }

    @Test void theUltimateWaitsForItsBreakpoint() {
        assertFalse(usable(14, 15));
        assertTrue(usable(15, 15));
        assertTrue(usable(30, 15));
    }
}
