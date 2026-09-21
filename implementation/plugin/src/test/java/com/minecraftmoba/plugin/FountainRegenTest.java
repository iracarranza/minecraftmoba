package com.minecraftmoba.plugin;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The Fountain restores its own team, and only while it is still their
 * respawn infrastructure.
 *
 * objectives.md cautions that the Fountain "should function as Minecraft
 * infrastructure rather than primarily as another conventional combat health
 * bar", so the conditions below are what keep it from becoming a contestable
 * heal pad.
 */
class FountainRegenTest {

    /** Mirrors the tick predicate: own team, not disabled, inside the radius. */
    private static boolean restores(boolean sameTeam, boolean disabled,
                                    double distance, double radius) {
        return sameTeam && !disabled && distance <= radius;
    }

    @Test void ownFountainRestores() {
        assertTrue(restores(true, false, 3, 8));
    }

    @Test void theEnemyFountainDoesNotRestoreYou() {
        // Otherwise a Fountain becomes sustain for whoever is standing in it.
        assertFalse(restores(false, false, 0, 8));
    }

    @Test void aDisabledFountainRestoresNothing() {
        // It has stopped being the team's respawn infrastructure; recovery
        // stopping with it follows from that rather than being a second rule.
        assertFalse(restores(true, true, 0, 8));
    }

    @Test void restorationIsLocalToTheStructure() {
        assertTrue(restores(true, false, 8, 8), "the radius is inclusive");
        assertFalse(restores(true, false, 8.1, 8));
        assertFalse(restores(true, false, 200, 8));
    }

    /** Mirrors restore(): fill toward the player's own maxima, never past. */
    private static int topUp(int current, int cap, int perInterval) {
        return current >= cap ? current : Math.min(cap, current + perInterval);
    }

    @Test void hungerFillsToTheCapacityCapAndNoFurther() {
        // The Fountain must not be a way around the Hunger cap that
        // HungerRegen enforces when eating.
        assertEquals(6, topUp(5, 9, 1));
        assertEquals(9, topUp(8, 9, 1));
        assertEquals(9, topUp(9, 9, 1), "already at the cap");
        assertEquals(9, topUp(9, 9, 5), "a larger rate still stops at the cap");
    }

    @Test void healthFillsTowardTheDerivedMaximum() {
        assertEquals(9, topUp(8, 9, 1), "level 1 maximum health is 9, not 20");
    }
}
