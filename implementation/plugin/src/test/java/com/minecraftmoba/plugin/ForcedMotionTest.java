package com.minecraftmoba.plugin;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/** The geometry of Lured and Frightened, with no server. */
class ForcedMotionTest {
    private static final double[] FEET = {0, 64, 0};
    private static final double[] EYE = {0, 65.62, 0};
    private static final double SPEED = 0.2158, STOP = 1.5;

    private static ForcedMotion.Step step(ForcedMotion.Kind k, double sx, double sz) {
        return ForcedMotion.step(k, EYE, FEET, new double[]{sx, 64, sz}, SPEED, STOP);
    }

    @Test void luredStepsTowardTheSource() {
        var s = step(ForcedMotion.Kind.LURED, 10, 0);
        assertEquals(SPEED, s.dx(), 1e-9);
        assertEquals(0, s.dz(), 1e-9);
        assertTrue(s.moves());
    }

    @Test void frightenedStepsTheOppositeWay() {
        var l = step(ForcedMotion.Kind.LURED, 3, 4);
        var f = step(ForcedMotion.Kind.FRIGHTENED, 3, 4);
        assertEquals(-l.dx(), f.dx(), 1e-9);
        assertEquals(-l.dz(), f.dz(), 1e-9);
    }

    @Test void bothFaceTheSourceNotTheirDirectionOfTravel() {
        var l = step(ForcedMotion.Kind.LURED, 0, 10);
        var f = step(ForcedMotion.Kind.FRIGHTENED, 0, 10);
        assertEquals(l.yaw(), f.yaw(), 1e-4f, "a Frightened body backs away, still looking at what it fears");
        assertEquals(0f, l.yaw(), 1e-4f, "south is yaw 0");
    }

    @Test void yawFollowsMinecraftsConvention() {
        assertEquals(0f, ForcedMotion.yawOf(0, 1), 1e-4f);      // south
        assertEquals(90f, ForcedMotion.yawOf(-1, 0), 1e-4f);    // west
        assertEquals(-90f, ForcedMotion.yawOf(1, 0), 1e-4f);    // east
        assertEquals(180f, Math.abs(ForcedMotion.yawOf(0, -1)), 1e-4f);   // north
    }

    @Test void pitchLooksDownAtAThingBelow() {
        assertTrue(ForcedMotion.pitchOf(5, -5, 0) > 0, "positive pitch looks down");
        assertTrue(ForcedMotion.pitchOf(5, 5, 0) < 0);
        assertEquals(0f, ForcedMotion.pitchOf(0, 0, 0), 1e-9f);
    }

    @Test void aLuredBodyStopsAtTheStopDistanceAndDoesNotOvershoot() {
        var s = step(ForcedMotion.Kind.LURED, 1.6, 0);          // 0.1 beyond the stop distance
        assertEquals(0.1, s.dx(), 1e-9, "the last step is only as long as the room left");
        assertTrue(s.arrived());
        var done = step(ForcedMotion.Kind.LURED, 1.4, 0);
        assertFalse(done.moves());
        assertTrue(done.arrived());
    }

    @Test void aFarSourceIsNotArrivedAt() { assertFalse(step(ForcedMotion.Kind.LURED, 20, 0).arrived()); }

    @Test void aFrightenedBodyNeverArrivesAndNeverStops() {
        for (double d : new double[]{0.5, 5, 50}) {
            var s = step(ForcedMotion.Kind.FRIGHTENED, d, 0);
            assertFalse(s.arrived());
            assertEquals(-SPEED, s.dx(), 1e-9);
        }
    }

    @Test void theStepIsOneSpeedLongInAnyDirection() {
        var s = step(ForcedMotion.Kind.LURED, 30, 40);
        assertEquals(SPEED, Math.hypot(s.dx(), s.dz()), 1e-9);
    }

    @Test void standingExactlyOnTheSourceMovesNoWayAndHasNoYaw() {
        var l = step(ForcedMotion.Kind.LURED, 0, 0);
        assertFalse(l.moves());
        assertTrue(Float.isNaN(l.yaw()));
        assertFalse(step(ForcedMotion.Kind.FRIGHTENED, 0, 0).moves(), "there is no direction away from where you stand");
    }

    @Test void yawDifferenceWrapsAroundTheCircle() {
        assertEquals(10, ForcedMotion.yawDifference(175, -175), 1e-9);
        assertEquals(0, ForcedMotion.yawDifference(90, 90), 1e-9);
        assertEquals(180, ForcedMotion.yawDifference(0, 180), 1e-9);
        assertEquals(20, ForcedMotion.yawDifference(-350, -10), 1e-9);
    }

    @Test void aSourceAboveStillTurnsTheCameraUp() {
        var s = ForcedMotion.step(ForcedMotion.Kind.LURED, EYE, FEET, new double[]{0, 80, 0}, SPEED, STOP);
        assertFalse(s.moves());
        assertTrue(s.pitch() < -80f);
    }
}
