package com.minecraftmoba.plugin;

import org.bukkit.util.Vector;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Runway's conversion: speed comes from travel, heading comes from facing.
 *
 * Separating those two is the whole of "convert momentum into a launch". If
 * heading came from travel, Runway could not turn; if speed came from facing,
 * a standing Daredevil could manufacture one by looking around.
 */
class DaredevilLaunchTest {

    private static final Vector EAST = new Vector(1, 0, 0);
    private static final Vector NORTH = new Vector(0, 0, -1);

    @Test void speedComesFromTravelAndHeadingFromFacing() {
        Vector travel = EAST.clone().multiply(0.8);
        Vector out = DaredevilState.launch(travel, NORTH, 1.0, 0.5);

        assertEquals(0.8, out.clone().setY(0).length(), 1e-9, "speed is the travel's");
        assertEquals(0, out.getX(), 1e-9);
        assertTrue(out.getZ() < 0, "heading is the facing's, so Runway can turn");
        assertEquals(0.5, out.getY(), 1e-9, "lift is added, not scaled from travel");
    }

    @Test void aStandingDaredevilLaunchesNowhereHorizontally() {
        Vector out = DaredevilState.launch(new Vector(), NORTH, 1.8, 0.5);
        assertEquals(0, out.clone().setY(0).length(), 1e-9,
                "no momentum means no horizontal launch, whatever the branch multiplier");
        assertEquals(0.5, out.getY(), 1e-9, "the lift is still honest about what happened");
    }

    @Test void lookingStraightUpFallsBackToTravelRatherThanProducingNaN() {
        Vector straightUp = new Vector(0, 1, 0);
        Vector out = DaredevilState.launch(EAST.clone().multiply(0.6), straightUp, 1.0, 0.5);

        assertFalse(Double.isNaN(out.getX()) || Double.isNaN(out.getZ()),
                "normalising a zero horizontal vector must not reach setVelocity");
        assertTrue(out.getX() > 0, "with no usable facing, the existing travel is the heading");
    }

    @Test void neitherTravelNorFacingLeavesOnlyLift() {
        Vector out = DaredevilState.launch(new Vector(), new Vector(0, -1, 0), 1.0, 0.5);
        assertEquals(0, out.getX(), 1e-9);
        assertEquals(0, out.getZ(), 1e-9);
        assertEquals(0.5, out.getY(), 1e-9);
    }

    @Test void theBranchMultiplierScalesWhatWasEarned() {
        Vector travel = EAST.clone().multiply(0.5);
        double plain = DaredevilState.launch(travel, EAST, 1.0, 0).clone().setY(0).length();
        double rocket = DaredevilState.launch(travel, EAST, 1.8, 0).clone().setY(0).length();
        assertEquals(plain * 1.8, rocket, 1e-9);
    }

    @Test void theThresholdIsInclusive() {
        assertTrue(DaredevilState.qualifies(0.25, 0.25), "exactly at the threshold qualifies");
        assertFalse(DaredevilState.qualifies(0.2499, 0.25));
        assertTrue(DaredevilState.qualifies(0.0, 0.0), "a zero threshold never refuses");
    }
}
