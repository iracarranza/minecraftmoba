package com.minecraftmoba.plugin;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

class CertifiedCentresTest {
    private static final List<int[]> ANCHORS = List.of(new int[]{-2148, 1460}, new int[]{-1700, 1540});

    @Test void everyCentreIsNearSomeAnchor() {
        var rng = new Random(3);
        for (int i = 0; i < 500; i++) {
            int[] c = CertifiedCentres.pick(ANCHORS, rng, 24);
            boolean near = ANCHORS.stream().anyMatch(a -> Math.abs(c[0] - a[0]) <= 24 && Math.abs(c[1] - a[1]) <= 24);
            assertTrue(near, c[0] + "," + c[1]);
        }
    }

    @Test void bothAnchorsGetUsedAndTheSpreadIsExercised() {
        var rng = new Random(9); boolean first = false, second = false, shifted = false;
        for (int i = 0; i < 200; i++) {
            int[] c = CertifiedCentres.pick(ANCHORS, rng, 24);
            if (c[0] < -2000) first = true; else second = true;
            if (c[0] != -2148 && c[0] != -1700) shifted = true;
        }
        assertTrue(first && second && shifted);
    }

    @Test void noAnchorsMeansNoCentreRatherThanAnInventedOne() {
        assertNull(CertifiedCentres.pick(List.of(), new Random(1), 24));
        assertNull(CertifiedCentres.pick(null, new Random(1), 24));
    }

    @Test void theSameSeedGivesTheSameCentre() {
        assertArrayEquals(CertifiedCentres.pick(ANCHORS, new Random(5), 24), CertifiedCentres.pick(ANCHORS, new Random(5), 24));
    }

    @Test void aZeroSpreadReturnsTheAnchorItself() {
        int[] c = CertifiedCentres.pick(List.of(new int[]{10, 20}), new Random(2), 0);
        assertArrayEquals(new int[]{10, 20}, c);
    }
}
