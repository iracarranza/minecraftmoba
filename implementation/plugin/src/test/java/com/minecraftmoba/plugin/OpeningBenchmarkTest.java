package com.minecraftmoba.plugin;

import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The 2026-09-21 opening expedition, as a deterministic fixture.
 *
 * Live play produced Lv8 with 3,365 WP from a single first mining trip, which
 * the player judged clearly too fast. A total that large cannot be calibrated
 * against until it can be decomposed, so this replays the reported workload
 * through {@link WorkLedger}'s own scoring -- the same two lines the live
 * handlers run -- against the coefficients config.yml actually ships.
 *
 * The workload is the reported activity, not a total worked backwards from
 * 3,365: ~95 raw copper, ~44 coal, a few raw iron, near-full copper armour,
 * the listed tools, a bucket, and 48 bread from village hay bales.
 */
class OpeningBenchmarkTest {

    // ---- the reported workload -------------------------------------------

    /** Ore kind -> blocks broken, units obtained. Copper is the multi-drop one. */
    private static final Map<String, long[]> MINED = new LinkedHashMap<>(Map.of(
            "copper", new long[]{30, 95},   // 2-5 raw copper per block
            "coal",   new long[]{44, 44},   // one coal per block
            "iron",   new long[]{3, 3}));   // "a few raw iron"

    private record Output(String category, String name, int made) {}

    private static final List<Output> PRODUCED = List.of(
            new Output("strategic_input", "copper_ingot", 24),   // smelted for armour
            new Output("strategic_input", "iron_ingot", 3),
            new Output("equipment", "copper_helmet", 1),
            new Output("equipment", "copper_chestplate", 1),
            new Output("equipment", "copper_leggings", 1),
            new Output("equipment", "copper_boots", 1),
            new Output("tool", "wooden_pickaxe", 1),
            new Output("tool", "wooden_axe", 1),
            new Output("tool", "stone_pickaxe", 1),
            new Output("tool", "iron_pickaxe", 1),
            new Output("utility", "bucket", 1),
            new Output("consumable", "bread", 48));

    private static final long PLACEMENTS = 36;   // the live Construction total, at 1 WP

    // ---- coefficients, read from the file that ships them ----------------

    private record Coefficients(Map<String, Integer> oreA, Map<String, Integer> oreQ,
                                Map<String, int[]> production) {}

    @SuppressWarnings("unchecked")
    private static Map<String, Object> config() throws Exception {
        try (InputStream in = Files.newInputStream(Path.of("src/main/resources/config.yml"))) {
            return (Map<String, Object>) new Yaml().load(in);
        }
    }

    @SuppressWarnings("unchecked")
    private static Object at(Map<String, Object> root, String dotted) {
        Object node = root;
        for (String part : dotted.split("\\.")) {
            if (!(node instanceof Map)) return null;
            node = ((Map<String, Object>) node).get(part);
            if (node == null) return null;
        }
        return node;
    }

    private static int intAt(Map<String, Object> cfg, String path, int fallback) {
        Object v = at(cfg, path);
        return v instanceof Number n ? n.intValue() : fallback;
    }

    private static Coefficients shipped() throws Exception {
        var cfg = config();
        var a = new LinkedHashMap<String, Integer>();
        var q = new LinkedHashMap<String, Integer>();
        int flatQ = intAt(cfg, "progression.work.extraction.harvestCoefficient", 1);
        for (String kind : MINED.keySet()) {
            a.put(kind, intAt(cfg, "progression.work.extraction.opportunity." + kind, 0));
            q.put(kind, intAt(cfg, "progression.work.extraction.harvest." + kind, flatQ));
        }
        var prod = new LinkedHashMap<String, int[]>();
        for (Output o : PRODUCED) {
            String base = "progression.work.production." + o.category();
            prod.put(o.category(), new int[]{
                    intAt(cfg, base + ".undertaking", 0),
                    intAt(cfg, base + ".perOutput", intAt(cfg, base, 0))});
        }
        return new Coefficients(a, q, prod);
    }

    // ---- running the fixture ---------------------------------------------

    private static WorkLedger run(Coefficients c, int ordinaryPlacement) {
        var ledger = new WorkLedger();
        MINED.forEach((kind, counts) -> ledger.extraction(kind,
                counts[0], WorkLedger.opportunityWp(counts[0], c.oreA().get(kind)),
                counts[1], WorkLedger.harvestWp(counts[1], c.oreQ().get(kind))));
        var seen = new HashSet<String>();
        for (Output o : PRODUCED) {
            int[] k = c.production().get(o.category());
            boolean first = seen.add(o.name());
            ledger.produced(o.category(), o.name(), o.made(),
                    WorkLedger.productionWp(first, k[0], k[1], o.made()), first);
        }
        for (long i = 0; i < PLACEMENTS; i++) ledger.placed(ordinaryPlacement);
        return ledger;
    }

    /** Level reached from zero under the SHIPPED curve. Never recalibrated here. */
    private static int[] levelFor(long totalWp) throws Exception {
        var cfg = config();
        int level = 1;
        long remaining = totalWp;
        while (true) {
            Object cost = at(cfg, "progression.levelCosts." + level);
            if (!(cost instanceof Number n) || remaining < n.intValue()) break;
            remaining -= n.intValue();
            level++;
        }
        Object next = at(cfg, "progression.levelCosts." + level);
        return new int[]{level, (int) remaining, next instanceof Number n ? n.intValue() : 0};
    }

    // ---- what the decomposition has to show ------------------------------

    @Test void theFixtureReproducesTheLiveMagnitudes() throws Exception {
        var ledger = run(shipped(), 1);
        ledger.report().forEach(System.out::println);
        int[] pos = levelFor(ledger.totalWp());
        System.out.printf("LEVEL %d (%d/%d)%n", pos[0], pos[1], pos[2]);

        // Magnitudes, not exact live values: the workload is approximate and
        // pinning 3365 would make this a restatement of the report rather than
        // a model of it.
        assertTrue(ledger.extractionWp() > 1200,
                "the fixture must reproduce an Extraction total of live magnitude");
        assertTrue(ledger.productionWp() > 300);
        assertEquals(36, ledger.constructionWp(), "Construction is 1 WP per first placement");
    }

    @Test void constructionIsNotTheInflationSource() throws Exception {
        var ledger = run(shipped(), 1);
        double share = ledger.constructionWp() / (double) ledger.totalWp();
        assertTrue(share < 0.05,
                "Construction is ~1% of the total; zeroing mundane work would not "
                        + "have addressed the inflation and would have cost the atomic floor");
    }

    @Test void neitherExtractionTermDominatesTheOther() throws Exception {
        // The live finding was that qH dominated: 1,420 against 800, 64% of
        // Extraction, which made the driver bulk material rather than
        // opportunity count. With per-ore q the two terms are comparable again.
        var ledger = run(shipped(), 1);
        double share = ledger.harvestShare();
        assertTrue(share > 0.35 && share < 0.65,
                "A(O) and qH should be of the same order; harvest share was " + share);
    }

    @Test void breakingOneBlockIsWorthTheSameWhateverTheOreDrops() throws Exception {
        // The defect, stated as the property that fixes it. Breaking one copper
        // ore is the same physical act as breaking one coal ore; at a flat q it
        // paid 41.7 WP against coal's 20, because copper drops 2-5 per block.
        var ledger = run(shipped(), 1);
        var copper = ledger.ore("copper");
        var coal = ledger.ore("coal");
        assertTrue(copper.unitsPerOpportunity() > 3.0, "copper still drops 2-5 per block");
        assertEquals(1.0, coal.unitsPerOpportunity(), 0.01, "coal still drops one");

        double copperPerBlock = copper.total() / (double) copper.opportunities;
        double coalPerBlock = coal.total() / (double) coal.opportunities;
        assertEquals(coalPerBlock, copperPerBlock, coalPerBlock * 0.15,
                "per block broken, copper (" + copperPerBlock + ") and coal ("
                        + coalPerBlock + ") should now be comparable");
    }

    @Test void fortuneStillPays() throws Exception {
        // The correction must not flatten yield into irrelevance: more material
        // actually obtained is still more Extraction work.
        var c = shipped();
        int q = c.oreQ().get("copper");
        assertTrue(q > 0, "per-ore q must not be zero, or Yield stops meaning anything");
        assertTrue(WorkLedger.harvestWp(5, q) > WorkLedger.harvestWp(2, q),
                "a Fortune-boosted copper break is worth more than a plain one");
    }

    @Test void bulkConsumablesDoNotOutweighCapital() throws Exception {
        // 48 bread against a full set of armour. Linear per-output valuation had
        // the bread ahead, which says feeding yourself at scale is the same kind
        // of accomplishment as equipping yourself and merely more of it.
        var ledger = run(shipped(), 1);
        long bread = ledger.category("consumable").wp;
        long armour = ledger.category("equipment").wp;
        assertTrue(bread < armour,
                "48 loaves (" + bread + " WP) must not outweigh a set of armour ("
                        + armour + " WP)");
    }

    @Test void moreOutputIsStillWorthMoreThanLessOutput() throws Exception {
        // The property that must survive any anti-inflation change: bulk
        // production must not become worthless, only sublinear.
        var c = shipped();
        int[] k = c.production().get("consumable");
        long three = WorkLedger.productionWp(true, k[0], k[1], 3);
        long fortyEight = WorkLedger.productionWp(true, k[0], k[1], 48);
        assertTrue(fortyEight > three, "48 bread > 3 bread");
        assertTrue(fortyEight < 16 * three,
                "but not 16x: the undertaking is shared, only the units are marginal");
    }
}
