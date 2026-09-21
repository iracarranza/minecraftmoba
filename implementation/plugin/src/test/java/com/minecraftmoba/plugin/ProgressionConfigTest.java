package com.minecraftmoba.plugin;

import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Every config key the progression code reads must exist in config.yml.
 *
 * This exists because it did not. WorkPoints read
 * `progression.work.extraction.*` while config.yml defined
 * `progression.extraction.*`, so every Extraction value in a live match came
 * from the Java fallback argument instead of the documented Alpha fixture --
 * silently, because `getInt(path, default)` cannot tell a missing key from a
 * key that happens to equal the default. Nothing crashed and nothing warned;
 * the file simply was not the authority it looked like.
 *
 * The scan that catches this generally now lives in ConfigKeysDefinedTest,
 * which covers every class rather than this one. What stays here is the part
 * specific to progression: that the file carries values the fallbacks do not,
 * and that the band curve is the calibration it claims to be.
 */
class ProgressionConfigTest {

    @SuppressWarnings("unchecked")
    private Map<String, Object> config() throws Exception {
        try (InputStream in = Files.newInputStream(Path.of("src/main/resources/config.yml"))) {
            return (Map<String, Object>) new Yaml().load(in);
        }
    }

    @SuppressWarnings("unchecked")
    private Object at(Map<String, Object> root, String dotted) {
        Object node = root;
        for (String part : dotted.split("\\.")) {
            if (!(node instanceof Map)) return null;
            node = ((Map<String, Object>) node).get(part);
            if (node == null) return null;
        }
        return node;
    }

    @Test void configuredValuesDifferFromTheJavaFallbacks() throws Exception {
        // If the file only ever repeated the fallbacks, the scan above could
        // pass while the file was still doing no work. These are values that
        // exist only because config.yml says so.
        var cfg = config();
        assertEquals(20, at(cfg, "progression.work.extraction.opportunity.iron"),
                "iron opportunity comes from config against a Java fallback of 0");
        assertEquals(40, at(cfg, "progression.work.extraction.opportunity.diamond"));
        assertEquals(60, at(cfg, "progression.work.extraction.opportunity.ancient_debris"));
        assertEquals(0, at(cfg, "progression.work.extraction.opportunity.default"));
    }

    @Test void ordinaryPlacementIsFlaggedUnresolvedRatherThanAssumedUseful() throws Exception {
        // Paying every BlockPlaceEvent made logs, torches and dirt into
        // Construction progression. The fixture is preserved in the file; what
        // is not asserted is that placement is useful.
        assertEquals(0, at(config(), "progression.work.ordinaryPlacement"));
        assertEquals(20, at(config(), "progression.work.constructionBlockPlacement"));
    }

    /** The generating function: cost(b, i) = 300 * bandMultiplier[b] * 1.15^i. */
    private static double generated(double multiplier, int within) {
        return 300.0 * multiplier * Math.pow(1.15, within);
    }

    /** Level ranges per economic band, and each band's established multiplier. */
    private static final int[][] BANDS = {{1, 6}, {7, 12}, {13, 19}, {20, 24}, {25, 29}};
    private static final double[] MULTIPLIERS = {1.000, 1.350, 1.875, 2.575, 3.250};
    private static final String[] BAND_NAMES =
            {"bootstrap", "established", "developed", "advanced", "endgame"};

    @Test void everyLevelCostIsTheFormulaRoundedToFiveWP() throws Exception {
        // The table is hand-supplied, so this checks it against the function it
        // claims to be rather than against itself. A transcription slip in one
        // of twenty-nine numbers is otherwise invisible.
        var cfg = config();
        for (int b = 0; b < BANDS.length; b++) {
            for (int level = BANDS[b][0]; level <= BANDS[b][1]; level++) {
                int actual = (Integer) at(cfg, "progression.levelCosts." + level);
                double want = generated(MULTIPLIERS[b], level - BANDS[b][0]);
                assertEquals(0, actual % 5, "level " + level + " is not a 5 WP step");
                assertTrue(Math.abs(actual - want) <= 2.5,
                        "level " + level + " is " + actual + ", formula gives "
                                + String.format("%.1f", want));
            }
        }
    }

    @Test void theRequirementDropsAtEveryBandBoundary() throws Exception {
        // The sawtooth is the design. If a future edit smooths it into a
        // monotonic curve, the economic-phase structure is gone even though
        // every individual number still looks reasonable.
        var cfg = config();
        for (int b = 1; b < BANDS.length; b++) {
            int lastOfPrevious = (Integer) at(cfg, "progression.levelCosts." + (BANDS[b][0] - 1));
            int firstOfThis = (Integer) at(cfg, "progression.levelCosts." + BANDS[b][0]);
            assertTrue(firstOfThis < lastOfPrevious,
                    "entering " + BAND_NAMES[b] + " must cost less per level than leaving "
                            + BAND_NAMES[b - 1] + ", got " + firstOfThis + " after " + lastOfPrevious);
        }
    }

    @Test void eachBandPeaksHigherThanTheOneBefore() throws Exception {
        // The other half of the sawtooth: a phase is cheaper to enter and more
        // expensive to finish than its predecessor ever became.
        var cfg = config();
        int previousPeak = 0;
        for (int[] band : BANDS) {
            int peak = (Integer) at(cfg, "progression.levelCosts." + band[1]);
            assertTrue(peak > previousPeak, "band peaks must rise, got " + peak + " after " + previousPeak);
            previousPeak = peak;
        }
    }

    @Test void theBandMultipliersAreTheEstablishedIndices() throws Exception {
        var cfg = config();
        for (int b = 0; b < BAND_NAMES.length; b++)
            assertEquals(MULTIPLIERS[b],
                    ((Number) at(cfg, "progression.bandMultipliers." + BAND_NAMES[b])).doubleValue(),
                    1e-9, BAND_NAMES[b]);
    }

    @Test void everyLevelBelowTheCapHasACost() throws Exception {
        var cfg = config();
        for (int level = 1; level <= 29; level++)
            assertNotNull(at(cfg, "progression.levelCosts." + level),
                    "level " + level + " would silently fall back to xpPerLevel");
        assertNull(at(cfg, "progression.levelCosts.30"), "Lv30 is the cap; nothing to leave it for");
    }

    @Test void everyProductionCategoryHasAFixture() throws Exception {
        var cfg = config();
        for (ProductionRecipes.Output out : ProductionRecipes.Output.values()) {
            if (out == ProductionRecipes.Output.UNRESOLVED) continue;
            String key = "progression.work.production." + out.name().toLowerCase();
            assertNotNull(at(cfg, key), "a recognized category with no fixture pays nothing: " + key);
        }
    }
}
