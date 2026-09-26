package com.minecraftmoba.plugin;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Mole's authored Lv0-30 curve, as the design record states it.
 *
 * Health values are engine HP. The player-facing scale is x100 and belongs to
 * the HUD, so 10 -> 32 HP is what a player reads as 1,000 -> 3,200 Health.
 */
class GrowthCurveTest {
    private static final Set<Integer> GROWTH = new LinkedHashSet<>(List.of(3,6,9,12,15,18,21,24,27,30));

    private static Capacity.Settings settings() {
        var mole = new Capacity.Profile(
            new Capacity.Curve(10, 2.2, 32),
            new Capacity.Curve(10, 2, 20),
            new Capacity.Curve(0, 0, 36, List.of(6.,9.,9.,12.,15.,15.,18.,18.,21.,21.,24.)));
        var fallback = new Capacity.Profile(
            new Capacity.Curve(9,1,20), new Capacity.Curve(9,1,20), new Capacity.Curve(6,3,36));
        return new Capacity.Settings(fallback, Map.of("mole", mole), GROWTH, Map.of());
    }

    private static Capacity.DerivedCapacity mole(int level) {
        return Capacity.recompute(level, List.of(), settings(), "mole");
    }

    @Test void moleMatchesItsAuthoredCurve() {
        int[] levels =  {  0,   3,   6,   9,  12,  15,  18,  21,  24,  27,  30};
        double[] health = {10,12.2,14.4,16.6,18.8,21.0,23.2,25.4,27.6,29.8,32.0};
        int[] hunger =  { 10,  12,  14,  16,  18,  20,  20,  20,  20,  20,  20};
        int[] slots =   {  6,   9,   9,  12,  15,  15,  18,  18,  21,  21,  24};
        for (int i = 0; i < levels.length; i++) {
            var d = mole(levels[i]);
            assertEquals(health[i], d.maxHealth(), 1e-9, "health at Lv" + levels[i]);
            assertEquals(hunger[i], d.effectiveHunger(), "hunger at Lv" + levels[i]);
            assertEquals(slots[i], d.unlockedSlots(), "slots at Lv" + levels[i]);
        }
    }

    @Test void levelZeroCompletesNoGrowth() {
        assertEquals(10, mole(0).maxHealth(), 1e-9);
        assertEquals(6, mole(0).unlockedSlots());
    }

    @Test void nothingChangesBetweenGrowthLevels() {
        assertEquals(mole(3).maxHealth(), mole(5).maxHealth(), 1e-9);
        assertEquals(mole(3).unlockedSlots(), mole(5).unlockedSlots());
    }

    /** The cap produces the Lv15 Hunger plateau. There is no Lv15 special case. */
    @Test void hungerPlateausByCapAlone() {
        assertEquals(20, mole(15).effectiveHunger());
        assertEquals(20, mole(18).effectiveHunger());
        assertEquals(20, mole(30).effectiveHunger());
    }

    /** A stepped curve may hold flat across a Growth; that is what steps are for. */
    @Test void steppedCurveHoldsFlatAcrossAGrowth() {
        assertEquals(mole(3).unlockedSlots(), mole(6).unlockedSlots());
        assertEquals(mole(12).unlockedSlots(), mole(15).unlockedSlots());
    }

    @Test void unknownProfileUsesTheFallback() {
        assertEquals(6, Capacity.recompute(0, List.of(), settings(), "nosuchclass").unlockedSlots());
        assertEquals(9, Capacity.recompute(0, List.of(), settings(), null).effectiveHunger());
    }

    @Test void steppedCurveReadsLastValueBeyondItsEnd() {
        var curve = new Capacity.Curve(0, 0, 36, List.of(6.,9.,24.));
        assertEquals(24, curve.at(2), 1e-9);
        assertEquals(24, curve.at(99), 1e-9);
        assertEquals(6, curve.at(0), 1e-9);
    }
}
