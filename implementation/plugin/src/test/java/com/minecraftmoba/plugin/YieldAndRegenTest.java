package com.minecraftmoba.plugin;

import org.junit.jupiter.api.Test;

import java.util.function.DoubleSupplier;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Two live defects found on the server on 29 September 2026, pinned here.
 *
 * Both had the same shape: a rule that was correct in its comment and wrong in
 * its code, in a way no unit could catch because neither quantity had one.
 */
class YieldAndRegenTest {

    /** A supplier that walks a fixed script, so "chance" is not a coin flip. */
    private static DoubleSupplier rolls(double... values) {
        int[] i = {0};
        return () -> values[i[0]++ % values.length];
    }

    // ---- Yield -------------------------------------------------------------

    @Test void expectedExtraIsLinearInTier() {
        // Every roll lands just under the per-tier chance, so every trial hits.
        // One hit per tier is the ceiling -- never tier * tier.
        for (int tier = 1; tier <= 3; tier++)
            assertEquals(tier, TaskEffects.extraUnits(tier, 0.25, rolls(0.0)),
                    "tier " + tier + " may roll once per tier, no more");
    }

    @Test void aRollBelowTheChanceMissesAtEveryTier() {
        // The shipped bug scaled the chance by tier, so at tier 3 a roll of
        // 0.5 counted as a hit against a 0.75 chance. At the real 0.25 it is a
        // miss, whatever the tier.
        for (int tier = 1; tier <= 3; tier++)
            assertEquals(0, TaskEffects.extraUnits(tier, 0.25, rolls(0.5)),
                    "0.5 is above a 0.25 chance and must miss at tier " + tier);
    }

    @Test void theChanceIsNeverCertain() {
        // tierMax 3 with the shipped 0.25 produced a per-trial chance of 0.75
        // at tier 3. Anything above 1/tierMax used to pass 1.0 and stop being a
        // roll; the clamp means a misconfiguration degrades predictably.
        assertEquals(3, TaskEffects.extraUnits(3, 5.0, rolls(0.999)),
                "an over-large chance clamps to certain, not to nonsense");
        assertEquals(0, TaskEffects.extraUnits(3, -1.0, rolls(0.0)),
                "a negative chance yields nothing rather than everything");
    }

    @Test void aBlockNeverMultipliesWithoutTheTask() {
        assertEquals(0, TaskEffects.extraUnits(0, 1.0, rolls(0.0)));
        assertEquals(0, TaskEffects.extraUnits(-1, 1.0, rolls(0.0)));
    }

    // ---- Health regeneration ----------------------------------------------

    @Test void vanillaOnlyCoversWhenTheGameruleIsOn() {
        assertTrue(HungerRegen.vanillaCovers(HungerRegen.VANILLA_REGEN_FOOD, true));
        assertTrue(HungerRegen.vanillaCovers(20, true));

        // The defect: a match holds naturalRegeneration OFF, so standing down
        // at food >= 18 handed the healthiest players to nobody at all.
        assertFalse(HungerRegen.vanillaCovers(HungerRegen.VANILLA_REGEN_FOOD, false),
                "with the gamerule off, vanilla covers nothing and the custom heal must run");
        assertFalse(HungerRegen.vanillaCovers(20, false));
    }

    @Test void belowVanillasThresholdIsAlwaysOurs() {
        for (boolean rule : new boolean[]{true, false})
            for (int food = 0; food < HungerRegen.VANILLA_REGEN_FOOD; food++)
                assertFalse(HungerRegen.vanillaCovers(food, rule),
                        "food " + food + " is below vanilla's threshold whatever the gamerule");
    }

    @Test void theBugOnlyAppearedAsAPlayerProgressed() {
        // At a Hunger ceiling of 9 the food level cannot reach 18, so the
        // custom heal always ran and the class looked correct. The failure
        // needed a ceiling high enough to reach vanilla's threshold -- which is
        // why it survived to the server.
        assertFalse(HungerRegen.vanillaCovers(9, false), "a capped early player was never affected");
        assertFalse(HungerRegen.vanillaCovers(18, false), "a grown player stopped healing entirely");
    }
}
