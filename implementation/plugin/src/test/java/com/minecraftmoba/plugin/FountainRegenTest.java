package com.minecraftmoba.plugin;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Reconstruction: a player reappears at their functioning friendly Fountain at
 * roughly 1 Health and 1 Hunger, then reconstructs at fixed absolute rates for
 * as long as they stay.
 *
 * The conditions below are what keep the Fountain infrastructure rather than a
 * contestable heal pad, and what make the rates' absoluteness observable.
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

    /** Mirrors onRespawn(): reappear reconstructing, not restored. */
    private static int arrivalFood(int configured, int cap) { return Math.min(cap, configured); }

    private static double arrivalHealth(double configured, double max) {
        return Math.max(1.0, Math.min(max, configured));
    }

    @Test void deathReturnsYouAtOneHealthAndOneHunger() {
        assertEquals(1.0, arrivalHealth(1.0, 9));
        assertEquals(1, arrivalFood(1, 9));
    }

    @Test void arrivalNeverKillsTheReconstructingPlayer() {
        // The Fountain returns you alive; a 0 or negative fixture must not be
        // able to turn reappearing into dying again.
        assertEquals(1.0, arrivalHealth(0.0, 9));
        assertEquals(1.0, arrivalHealth(-5.0, 9));
    }

    /** Intervals to fill from `from` to `cap` at a fixed absolute rate. */
    private static int intervals(int from, int cap, int perInterval) {
        return (cap - from + perInterval - 1) / perInterval;
    }

    @Test void raisingMaximaLengthensReconstruction() {
        // The rates are absolute, not percentages of maximum and not
        // level-scaled, so Capacity buys a bigger pool at the price of a longer
        // rebuild. A proportional rate would make these two equal.
        assertEquals(8, intervals(1, 9, 1));
        assertEquals(19, intervals(1, 20, 1));
        assertNotEquals(intervals(1, 9, 1), intervals(1, 20, 1));
    }

    @Test void leavingPartlyReconstructedIsAllowed() {
        // Nothing holds the player: reconstruction is simply what happens while
        // they are present, so a partial value is a legitimate resting state.
        assertTrue(restores(true, false, 3, 8), "present: reconstructing");
        assertFalse(restores(true, false, 30, 8), "walked away: simply stops");
    }

    @Test void losingTheFountainStopsReconstructionWithoutHarm() {
        // A player mid-reconstruction keeps their partial Health/Hunger and
        // their normal maxima. Only the NEXT death becomes permanent, which is
        // the mechanism ALPHA-D3 resolves victory through.
        int partial = 4;
        assertFalse(restores(true, true, 0, 8), "reconstruction stops at once");
        assertEquals(partial, topUp(partial, partial, 0), "nothing is taken back");
    }
}
