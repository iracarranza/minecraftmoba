package com.minecraftmoba.plugin;

import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The shipped level curve, checked against the rule the config says generated
 * it, and walked by a few crude policies.
 *
 * **Everything asserted here is internal consistency.** Whether 30 levels in
 * N minutes is correct is a balance question and no simulation settles it; what
 * a simulation can settle is that the curve is the curve the comment describes,
 * that no level is free or unreachable, and that the band structure the design
 * claims is actually present in the numbers.
 */
class ProgressionSimulationTest {

    @SuppressWarnings("unchecked")
    private static Map<String, Object> progression() throws Exception {
        try (InputStream in = Files.newInputStream(Path.of("src/main/resources/config.yml"))) {
            return (Map<String, Object>) ((Map<String, Object>) new Yaml().load(in)).get("progression");
        }
    }

    @SuppressWarnings("unchecked")
    private static ProgressionCurve curve() throws Exception {
        var p = progression();
        var raw = (Map<Object, Object>) p.get("levelCosts");
        var costs = new TreeMap<Integer, Integer>();
        raw.forEach((k, v) -> costs.put(Integer.parseInt(String.valueOf(k)), ((Number) v).intValue()));
        return new ProgressionCurve(costs, ((Number) p.get("maxLevel")).intValue(), 300.0, 1.15);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Double> configuredMultipliers() throws Exception {
        var out = new LinkedHashMap<String, Double>();
        ((Map<Object, Object>) progression().get("bandMultipliers"))
                .forEach((k, v) -> out.put(String.valueOf(k), ((Number) v).doubleValue()));
        return out;
    }

    // ---- the curve is the curve the config describes -----------------------

    @Test void everyLevelBelowTheCapCostsSomethingAndTheCapCostsNothing() throws Exception {
        var c = curve();
        for (int level = 1; level < c.maxLevel(); level++)
            assertTrue(c.costToLeave(level) > 0, "level " + level + " is free to leave");
        assertEquals(0, c.costToLeave(c.maxLevel()), "the cap has nothing beyond it");
    }

    @Test void theCurveMatchesTheFormulaExactly() throws Exception {
        // cost(b, i) = 300 * multiplier[b] * 1.15^i, rounded to 5. The rounding
        // is part of the published rule, so this is exact rather than a
        // tolerance -- a drifted hand-edit shows up as a disagreement.
        assertEquals(Map.of(), curve().disagreementsWithFormula(configuredMultipliers().values()));
    }

    @Test void theBandsRecoveredFromTheNumbersAreTheBandsTheConfigNames() throws Exception {
        // The level-to-band mapping lives only in a comment, so nothing could
        // check it. Recovering bands from where cost falls makes it checkable.
        var declared = configuredMultipliers().values().stream().sorted().toList();
        var bands = curve().bands(configuredMultipliers().values());
        var recovered = bands.stream().map(ProgressionCurve.Band::multiplier).sorted().toList();

        assertEquals(declared.size(), recovered.size(),
                "the curve has " + recovered.size() + " phases, the config names " + declared.size());
        for (int i = 0; i < declared.size(); i++)
            assertEquals(declared.get(i), recovered.get(i), 1e-9,
                    "recovered multiplier " + recovered.get(i) + " is not one the config declares");
    }

    @Test void costRisesWithinABandAndDropsAtEveryBoundary() throws Exception {
        // The drop is documented as intentional. Asserting it means an edit
        // that accidentally smooths the curve into monotonic gets caught --
        // which would silently delete the phase structure.
        var c = curve();
        for (ProgressionCurve.Band band : c.bands(configuredMultipliers().values())) {
            for (int level = band.firstLevel(); level < band.lastLevel(); level++)
                assertTrue(c.costToLeave(level + 1) > c.costToLeave(level),
                        "cost must compound within band " + band.firstLevel() + "-" + band.lastLevel());
        }
        var bands = c.bands(configuredMultipliers().values());
        for (int i = 1; i < bands.size(); i++) {
            int lastOfPrevious = c.costToLeave(bands.get(i - 1).lastLevel());
            int firstOfThis = c.costToLeave(bands.get(i).firstLevel());
            assertTrue(firstOfThis < lastOfPrevious,
                    "entering a phase is a step DOWN in per-level cost, by design");
        }
    }

    @Test void totalCostAlwaysRisesSoNoLevelIsFree() throws Exception {
        var c = curve();
        for (int level = 1; level < c.maxLevel(); level++)
            assertTrue(c.cumulativeTo(level + 1) > c.cumulativeTo(level), "level " + level);
    }

    @Test void eachPhaseReachesFurtherThanTheLastDespiteStartingLower() throws Exception {
        // The design's claim in full: a step down on entry, a step up in where
        // the phase ends. Only the second half is non-obvious.
        var c = curve();
        var bands = c.bands(configuredMultipliers().values());
        for (int i = 1; i < bands.size(); i++)
            assertTrue(c.costToLeave(bands.get(i).lastLevel()) > c.costToLeave(bands.get(i - 1).lastLevel()),
                    "phase " + i + " must peak above the one before it");
    }

    // ---- the simulation ----------------------------------------------------

    @SuppressWarnings("unchecked")
    private static ProgressionSimulation sim() throws Exception {
        var work = (Map<String, Object>) progression().get("work");
        var extraction = (Map<String, Object>) work.get("extraction");
        var opportunity = (Map<String, Object>) extraction.get("opportunity");
        int harvest = ((Number) extraction.get("harvestCoefficient")).intValue();

        var awards = new LinkedHashMap<String, Integer>();
        awards.put("ordinaryPlacement", ((Number) work.get("ordinaryPlacement")).intValue());
        awards.put("constructionBlockPlacement", ((Number) work.get("constructionBlockPlacement")).intValue());
        // One iron ore: its opportunity award plus one drop at the default
        // harvest coefficient, which is the shape of WP = A(O) + qH.
        awards.put("ironOre", ((Number) opportunity.get("iron")).intValue() + harvest);
        awards.put("coalOre", ((Number) opportunity.get("coal")).intValue() + harvest);
        return new ProgressionSimulation(curve(), awards);
    }

    private static final ProgressionSimulation.Policy BUILDER =
            new ProgressionSimulation.Policy("builder", Map.of("constructionBlockPlacement", 12.0));
    private static final ProgressionSimulation.Policy MINER =
            new ProgressionSimulation.Policy("miner", Map.of("ironOre", 6.0, "coalOre", 14.0));
    private static final ProgressionSimulation.Policy DABBLER =
            new ProgressionSimulation.Policy("dabbler", Map.of("ordinaryPlacement", 30.0));
    private static final ProgressionSimulation.Policy IDLE =
            new ProgressionSimulation.Policy("idle", Map.of());

    @Test void everyEarningPolicyReachesTheCapAndTheIdleOneNeverDoes() throws Exception {
        var s = sim();
        for (var p : List.of(BUILDER, MINER, DABBLER))
            assertTrue(s.minutesToCap(p) > 0, p.name() + " should reach the cap");
        assertEquals(-1, s.minutesToCap(IDLE), "a player earning nothing never levels, and says so");
        assertTrue(s.run(IDLE).isEmpty(), "and returns no arrivals rather than hanging");
    }

    @Test void aFasterPolicyIsNeverSlowerToAnyLevel() throws Exception {
        // A property of the model rather than of the game: if this fails the
        // simulator is wrong, and nothing it says about the curve can be read.
        var s = sim();
        var fast = s.run(BUILDER);
        var slow = s.run(DABBLER);
        assertTrue(BUILDER.workPerMinute(Map.of("constructionBlockPlacement", 20)) > 0);
        assertEquals(fast.size(), slow.size());
        for (int i = 0; i < fast.size(); i++)
            assertTrue(fast.get(i).minutesTotal() <= slow.get(i).minutesTotal(),
                    "level " + fast.get(i).level());
    }

    @Test void perLevelTimeFallsWhenAPhaseIsEntered() throws Exception {
        // The band drop expressed in the currency a player actually feels.
        var s = sim();
        var arrivals = s.run(MINER);
        var bands = curve().bands(configuredMultipliers().values());
        for (int i = 1; i < bands.size(); i++) {
            int entering = bands.get(i).firstLevel();
            double atEntry = arrivals.stream().filter(a -> a.level() == entering + 1)
                    .findFirst().orElseThrow().minutesForThisLevel();
            double justBefore = arrivals.stream().filter(a -> a.level() == entering)
                    .findFirst().orElseThrow().minutesForThisLevel();
            assertTrue(atEntry < justBefore,
                    "entering the phase at level " + entering + " should take less time, not more");
        }
    }

    @Test void everyPhaseTakesRealTimeForEveryPolicy() throws Exception {
        // A phase that passes instantly is a phase that does not exist, which
        // is an internal inconsistency rather than a balance opinion.
        var s = sim();
        for (var p : List.of(BUILDER, MINER, DABBLER))
            for (var e : s.minutesPerBand(p).entrySet())
                assertTrue(e.getValue() > 0.5,
                        p.name() + " passes band " + e.getKey() + " in " + e.getValue() + " minutes");
    }

    @Test void theReportNamesEveryPolicyAndEveryBand() throws Exception {
        String report = sim().report(List.of(BUILDER, MINER, DABBLER, IDLE));
        // Printed because this is the instrument's actual output, and a design
        // instrument nobody reads is a test that happens to be slow.
        System.out.println(report);
        for (var p : List.of("builder", "miner", "dabbler", "idle"))
            assertTrue(report.contains(p), "report omits " + p);
        for (var band : curve().bands(configuredMultipliers().values()))
            assertTrue(report.contains(band.firstLevel() + "-" + band.lastLevel()),
                    "report omits band " + band.firstLevel());
        assertTrue(report.contains("not evidence of balance"),
                "the instrument must say what it is every time it speaks");
    }
}
