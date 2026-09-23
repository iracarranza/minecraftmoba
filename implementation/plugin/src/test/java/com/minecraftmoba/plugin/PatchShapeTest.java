package com.minecraftmoba.plugin;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * A wild Patch must read as a resource occurrence, not as a farm somebody has
 * already built.
 *
 * The rejected authoring was `crop_patch(span=24)`: a rectangle of moisture-7
 * farmland with a central water column. It is not merely ugly, it is the wrong
 * category -- irrigation and prepared fields are player Development, and a world
 * manifestation that looks like one makes the thing players are supposed to
 * build indistinguishable from the thing the world hands them.
 *
 * The test of legitimacy is NOT whether vanilla worldgen would place this crop
 * here. Regenerative manifestations are deliberately game-authored, and a wild
 * carrot patch is a valid authored ecology.
 */
class PatchShapeTest {

    private static List<int[]> patch(long seed, int count, int spread) {
        return PatchShape.columns(100, 100, count, spread, new Random(seed));
    }

    @Test void aPatchIsIrregularRatherThanAFilledRectangle() {
        // The numeric difference between an occurrence and a prepared field: a
        // field fills its bounding box, an occurrence does not.
        for (long seed = 1; seed <= 20; seed++) {
            var columns = patch(seed, 12, 6);
            double fill = PatchShape.fillRatio(columns);
            assertTrue(fill < 0.75,
                    "seed " + seed + " filled " + fill + " of its bounding box, which reads as a field");
        }
    }

    @Test void aPatchHasGapsAndVariableDensity() {
        var columns = patch(3, 14, 7);
        assertTrue(PatchShape.fillRatio(columns) < 1.0, "a solid block is a field");
        int minX = columns.stream().mapToInt(c -> c[0]).min().orElseThrow();
        int maxX = columns.stream().mapToInt(c -> c[0]).max().orElseThrow();
        assertTrue(maxX - minX >= 3, "a patch spreads rather than stacking on one column");
    }

    @Test void aPatchStaysWithinItsSpread() {
        for (int[] c : patch(9, 20, 5)) {
            assertTrue(Math.abs(c[0] - 100) <= 5);
            assertTrue(Math.abs(c[1] - 100) <= 5);
        }
    }

    @Test void aPatchProducesOneColumnPerResourceBlockAndNoMore() {
        // The substrate allowance is per resource block -- the minimum needed
        // for the block to exist -- and explicitly not a prepared area. If the
        // shape emitted more columns than crops, the difference would be
        // exactly the "broad prepared farmland" that is rejected.
        var columns = patch(11, 10, 6);
        assertEquals(columns.size(), columns.stream().distinct().count(),
                "no column is claimed twice");
        assertTrue(columns.size() <= 10, "never more ground prepared than crops placed");
    }

    @Test void theShapeKnowsNothingAboutFarmInfrastructure() {
        // It emits columns. There is no API by which it could place a fence, a
        // water source or a flattened floor, which is the structural way to
        // guarantee it does not.
        var source = java.util.Arrays.stream(PatchShape.class.getDeclaredMethods())
                .map(java.lang.reflect.Method::getName).toList();
        assertTrue(source.contains("columns"));
        assertFalse(source.stream().anyMatch(n ->
                n.toLowerCase().contains("fence") || n.toLowerCase().contains("water")
                        || n.toLowerCase().contains("irrigat") || n.toLowerCase().contains("flatten")));
    }

    @Test void anEmptyPatchIsEmptyRatherThanAMinimalFarm() {
        assertTrue(PatchShape.columns(0, 0, 0, 5, new Random()).isEmpty());
    }
}
