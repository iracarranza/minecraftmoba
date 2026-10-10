package com.minecraftmoba.plugin;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class ScenarioPlacementTest {
    private static final Scenario.Member M = new Scenario.Member("a", Team.NORTH, "mole", 1, 4, 0, 8);

    @Test void anOffsetIsMeasuredFromTheAnchorAndStandsOnTheGround() {
        double[] p = ScenarioPlacement.resolve(M, false, -2148, 1460, (x, z) -> 70);
        assertArrayEquals(new double[]{-2144 + 0.5, 71, 1468 + 0.5}, p, 1e-9);
    }

    @Test void theGroundIsLookedUpAtTheMembersOwnColumn() {
        double[] p = ScenarioPlacement.resolve(M, false, 0, 0, (x, z) -> x * 10 + z);
        assertEquals(4 * 10 + 8 + 1, p[1], 1e-9);
    }

    @Test void aVerticalOffsetIsFromTheGroundNotFromTheAnchor() {
        var high = new Scenario.Member("a", Team.NORTH, "mole", 1, 0, 5, 0);
        assertEquals(64 + 1 + 5, ScenarioPlacement.resolve(high, false, 0, 0, (x, z) -> 64)[1], 1e-9);
    }

    @Test void anAbsoluteScenarioUsesItsCoordinatesAsGiven() {
        assertArrayEquals(new double[]{4, 0, 8}, ScenarioPlacement.resolve(M, true, 999, 999, (x, z) -> 1), 1e-9);
    }

    @Test void negativeOffsetsFloorCorrectly() {
        var m = new Scenario.Member("a", Team.NORTH, "mole", 1, -3, 0, -3);
        double[] p = ScenarioPlacement.resolve(m, false, 0, 0, (x, z) -> 60);
        assertEquals(-3 + 0.5, p[0], 1e-9); assertEquals(-3 + 0.5, p[2], 1e-9);
    }

    @Test void anUnknownAnchorIsRefusedAndKnownOnesAreNot() {
        assertNull(ScenarioPlacement.refusal("lair")); assertNull(ScenarioPlacement.refusal("fountain_south"));
        assertTrue(ScenarioPlacement.refusal("moon").contains("Anchors"));
    }
}
