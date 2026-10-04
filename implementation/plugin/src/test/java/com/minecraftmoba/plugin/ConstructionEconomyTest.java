package com.minecraftmoba.plugin;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * What limits a Construction player.
 *
 * The first version of the progression model took placements-per-minute as a
 * free parameter, which answered "if a builder could place twelve a minute
 * forever, how long to the cap" -- a question nobody asked. Every construction
 * material is manufactured, so the rate is set upstream, by furnace time and
 * by how much a player can carry.
 */
class ConstructionEconomyTest {

    private static final int WP_PER_PLACEMENT = 20;

    @Test void oneFurnaceIsSixItemsAMinute() {
        var glass = new ConstructionEconomy(ConstructionEconomy.Route.GLASS, 1, 0, 1);
        assertEquals(6.0, glass.supplyPerMinute(), 1e-9);
    }

    @Test void bricksCostFourTimesTheFurnaceTimeOfGlass() {
        // The widest spread in the category, and the one that decides which
        // material a Construction class can actually be built on.
        var perBlock = ConstructionEconomy.furnaceSecondsPerBlock();
        assertEquals(10.0, perBlock.get(ConstructionEconomy.Route.GLASS), 1e-9);
        assertEquals(10.0, perBlock.get(ConstructionEconomy.Route.TERRACOTTA), 1e-9);
        assertEquals(40.0, perBlock.get(ConstructionEconomy.Route.BRICKS), 1e-9,
                "four clay balls, four smelts, one block");
        assertEquals(0.0, perBlock.get(ConstructionEconomy.Route.CONCRETE), 1e-9,
                "concrete never sees a furnace, which is the whole of its advantage");
    }

    @Test void concreteSupplyIsRationedByDyeRatherThanHeat() {
        // Eight blocks per craft, and the crafting is instant -- so the number
        // that matters is how often a batch can be assembled at all.
        var concrete = new ConstructionEconomy(ConstructionEconomy.Route.CONCRETE, 0, 1.5, 1);
        assertEquals(12.0, concrete.supplyPerMinute(), 1e-9);
        assertEquals(0.0, ConstructionEconomy.furnacesFor(ConstructionEconomy.Route.CONCRETE, 100),
                "no quantity of concrete requires a furnace");
    }

    @Test void twelveBlocksAMinuteIsATeamCommitment() {
        // The original model's rate, costed honestly.
        assertEquals(2.0, ConstructionEconomy.furnacesFor(ConstructionEconomy.Route.GLASS, 12), 1e-9,
                "two furnaces fed continuously, for glass");
        assertEquals(8.0, ConstructionEconomy.furnacesFor(ConstructionEconomy.Route.BRICKS, 12), 1e-9,
                "eight furnaces fed continuously, for bricks");
    }

    @Test void theBindingConstraintIsReportedRatherThanHidden() {
        // A single blocks-per-minute figure cannot say WHY it is that figure,
        // and "why" is the only actionable part.
        var starved = new ConstructionEconomy(ConstructionEconomy.Route.GLASS, 1, 0, 2);
        assertEquals(ConstructionEconomy.Binding.SUPPLY, starved.limit(24).binding());

        var flooded = new ConstructionEconomy(ConstructionEconomy.Route.CONCRETE, 0, 100, 4);
        assertEquals(ConstructionEconomy.Binding.CARRY, flooded.limit(6).binding());
    }

    @Test void carryRisesWithTheCapacityCurveAndSupplyDoesNot() {
        // Why construction needs a level-varying rate at all: one of its two
        // gates moves with Growth and the other does not.
        var e = new ConstructionEconomy(ConstructionEconomy.Route.CONCRETE, 0, 100, 4);
        double atSix = e.carryPerMinute(6), atThirtySix = e.carryPerMinute(36);
        assertTrue(atThirtySix > atSix);
        assertEquals(6.0, atThirtySix / atSix, 1e-9, "carry is linear in slots");
        assertEquals(e.supplyPerMinute(), e.limit(6).supply(), 1e-9);
        assertEquals(e.supplyPerMinute(), e.limit(36).supply(), 1e-9, "supply is indifferent to level");
    }

    @Test void aRealisticBuilderIsSupplyBoundAtEveryLevel() {
        // One furnace of glass against the default slot curve: carry exceeds
        // supply by two orders of magnitude even at the floor, so levelling
        // does nothing for this builder. That is the finding -- capacity is not
        // what is holding them back.
        var e = new ConstructionEconomy(ConstructionEconomy.Route.GLASS, 1, 0, 3);
        for (int slots : new int[]{6, 12, 24, 36})
            assertEquals(ConstructionEconomy.Binding.SUPPLY, e.limit(slots).binding(),
                    "at " + slots + " slots");
        // At the 6-slot floor a three-minute trip ferries 384 blocks, or 128 a
        // minute, against one furnace's 6. Twenty-one to one, at the WORST
        // point of the Capacity curve.
        assertEquals(128.0, e.limit(6).carry(), 1e-9);
        assertEquals(6.0, e.limit(6).supply(), 1e-9);
        assertTrue(e.limit(6).carry() > 20 * e.limit(6).supply(),
                "even the smallest inventory out-carries a furnace by over twenty to one");
    }

    @Test void aSupplyLimitedBuilderLevelsNoFasterForHavingMoreSlots() {
        var curve = new ProgressionCurve(java.util.Map.of(1, 300, 2, 345), 3, 300.0, 1.15);
        var economy = new ConstructionEconomy(ConstructionEconomy.Route.GLASS, 1, 0, 3);

        var small = ProgressionSimulation.construction("small", economy, l -> 6, WP_PER_PLACEMENT);
        var large = ProgressionSimulation.construction("large", economy, l -> 36, WP_PER_PLACEMENT);
        var sim = new ProgressionSimulation(curve, java.util.Map.of());

        assertEquals(sim.minutesToCap(small), sim.minutesToCap(large), 1e-9,
                "more inventory cannot help a builder the furnaces are starving");
    }

    @Test void aCarryLimitedBuilderDoesSpeedUpWithLevel() {
        var curve = new ProgressionCurve(java.util.Map.of(1, 300, 2, 345), 3, 300.0, 1.15);
        var economy = new ConstructionEconomy(ConstructionEconomy.Route.CONCRETE, 0, 500, 5);
        var sim = new ProgressionSimulation(curve, java.util.Map.of());

        double small = sim.minutesToCap(
                ProgressionSimulation.construction("small", economy, l -> 6, WP_PER_PLACEMENT));
        double large = sim.minutesToCap(
                ProgressionSimulation.construction("large", economy, l -> 36, WP_PER_PLACEMENT));
        assertTrue(large < small, "when carry binds, Capacity is the progression");
    }
}
