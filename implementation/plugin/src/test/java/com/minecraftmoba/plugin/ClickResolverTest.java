package com.minecraftmoba.plugin;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/** What a click would carry, with no server. */
class ClickResolverTest {
    private static final ClickResolver.Kind AIR = ClickResolver.Kind.AIR, BLOCK = ClickResolver.Kind.BLOCK, ENTITY = ClickResolver.Kind.ENTITY;

    @Test void nothingInReachIsAir() { assertEquals(AIR, ClickResolver.choose(null, null)); }

    @Test void aBlockAloneIsTheBlockAndACreatureAloneIsTheCreature() {
        assertEquals(BLOCK, ClickResolver.choose(null, 2.0));
        assertEquals(ENTITY, ClickResolver.choose(1.5, null));
    }

    @Test void aCreatureNearerThanTheBlockWinsAndOneBehindItDoesNot() {
        assertEquals(ENTITY, ClickResolver.choose(1.0, 2.0));
        assertEquals(BLOCK, ClickResolver.choose(2.5, 2.0), "a wall in front of a creature is what the crosshair is on");
    }

    @Test void aTieGoesToTheCreature() { assertEquals(ENTITY, ClickResolver.choose(2.0, 2.0)); }

    @Test void vanillaSurvivalReach() {
        assertEquals(4.5, ClickResolver.BLOCK_REACH);
        assertEquals(3.0, ClickResolver.ENTITY_REACH);
    }

    @Test void aRightClickCarriesTheBlockOrTheCreatureAndNothingForAir() {
        var r = AbilityInputs.Input.RIGHT_CLICK;
        assertEquals(new ClickResolver.Carries(true, false), ClickResolver.carries(r, BLOCK));
        assertEquals(new ClickResolver.Carries(false, true), ClickResolver.carries(r, ENTITY));
        assertEquals(ClickResolver.Carries.NOTHING, ClickResolver.carries(r, AIR));
    }

    @Test void aLeftClickCarriesTheBlockButNotACreatureBecauseHittingOneIsADamageEvent() {
        var l = AbilityInputs.Input.LEFT_CLICK;
        assertEquals(new ClickResolver.Carries(true, false), ClickResolver.carries(l, BLOCK));
        assertEquals(ClickResolver.Carries.NOTHING, ClickResolver.carries(l, ENTITY));
        assertEquals(ClickResolver.Carries.NOTHING, ClickResolver.carries(l, AIR));
    }

    @Test void theHandSwapAndTheDropCarryNothingWhateverIsUnderTheCrosshair() {
        for (var input : new AbilityInputs.Input[]{AbilityInputs.Input.SWAP_HAND, AbilityInputs.Input.DROP})
            for (var kind : ClickResolver.Kind.values())
                assertEquals(ClickResolver.Carries.NOTHING, ClickResolver.carries(input, kind), input + " on " + kind);
    }

    @Test void everyInputAndKindIsCovered() {
        for (var input : AbilityInputs.Input.values())
            for (var kind : ClickResolver.Kind.values())
                assertNotNull(ClickResolver.carries(input, kind));
    }

    @Test void aCastActAcceptsTheTwoClickModesAndRefusesTheRest() {
        for (String ok : new String[]{"air", "resolve"}) assertTrue(ScenarioActs.CLICKS.contains(ok));
        var roster = java.util.List.of(new Scenario.Member("a", Team.NORTH, "mole", 5, 0, 0, 0), new Scenario.Member("b", Team.SOUTH, "mole", 5, 4, 0, 0));
        java.util.function.Function<String, String> problem = click -> ScenarioActs.problem(new Scenario("s", "d", roster,
                java.util.List.<Scenario.Step>of(new Scenario.Act(10, "a", "cast", java.util.Map.of("slot", "a1", "at", "b", "click", click))), false, "fountain_north"));
        assertNull(problem.apply("air"));
        assertNull(problem.apply("resolve"));
        assertTrue(problem.apply("mouse").contains("mouse"));
    }
}
