package com.minecraftmoba.plugin;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Random;
import org.junit.jupiter.api.Test;

class CastAimTest {
    private static final double EPS = 1e-4;

    @Test void plusZIsYawZero() {
        var f = CastAim.toward(0, 0, 0, 0, 0, 5);
        assertEquals(0f, f.yaw(), EPS); assertEquals(0f, f.pitch(), EPS);
    }
    @Test void plusXIsYawMinus90() { assertEquals(-90f, CastAim.toward(0, 0, 0, 5, 0, 0).yaw(), EPS); }
    @Test void minusXIsYaw90() { assertEquals(90f, CastAim.toward(0, 0, 0, -5, 0, 0).yaw(), EPS); }
    @Test void minusZIsYaw180() { assertEquals(180f, Math.abs(CastAim.toward(0, 0, 0, 0, 0, -5).yaw()), EPS); }
    @Test void lookingDownIsPositivePitch() { assertEquals(45f, CastAim.toward(0, 5, 0, 0, 0, 5).pitch(), EPS); }
    @Test void lookingUpIsNegativePitch() { assertEquals(-45f, CastAim.toward(0, 0, 0, 0, 5, 5).pitch(), EPS); }
    @Test void straightUpAndDownHaveFullPitch() {
        assertEquals(-90f, CastAim.toward(0, 0, 0, 0, 3, 0).pitch(), EPS);
        assertEquals(90f, CastAim.toward(0, 3, 0, 0, 0, 0).pitch(), EPS);
    }
    @Test void samePointIsStraightAhead() {
        var f = CastAim.toward(1, 2, 3, 1, 2, 3);
        assertEquals(0f, f.yaw()); assertEquals(0f, f.pitch());
    }
    @Test void directionRoundTripsTheFacing() {
        var f = CastAim.toward(0, 0, 0, 3, 2, -4);
        double[] d = CastAim.direction(f);
        double len = Math.sqrt(9 + 4 + 16);
        assertEquals(3 / len, d[0], EPS); assertEquals(2 / len, d[1], EPS); assertEquals(-4 / len, d[2], EPS);
    }
    @Test void randomIsLevelAndReproducibleBySeed() {
        var a = CastAim.random(new Random(7)); var b = CastAim.random(new Random(7));
        assertEquals(a, b); assertEquals(0f, a.pitch());
        assertTrue(a.yaw() >= -180f && a.yaw() < 180f);
    }
}
