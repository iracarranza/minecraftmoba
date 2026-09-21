package com.minecraftmoba.plugin;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Per-source accounting behind a WP total.
 *
 * A domain total says a number moved; it does not say which rule moved it. When
 * live play produced 2,220 Extraction and 1,109 Production in a single opening
 * expedition, nothing in the game or the logs could answer the only question
 * that mattered -- whether that was many opportunities or a few opportunities
 * with very large harvests -- and a coefficient cannot be calibrated against a
 * total it cannot decompose.
 *
 * So this records the terms, not the sum: for Extraction, opportunities and
 * harvested units separately per ore kind, with A(O) and qH kept apart; for
 * Production, count and WP per category with the outputs that produced them.
 *
 * It is deliberately pure and Bukkit-free, so the same ledger a live match
 * fills can be filled by a benchmark fixture.
 */
public final class WorkLedger {

    // ---- the scoring itself ---------------------------------------------
    //
    // Pure, and shared with the benchmark fixture on purpose. A benchmark that
    // re-implemented these would only ever confirm its own arithmetic; these
    // are the same two lines the live handlers run.

    /** Extraction: WP_X = A(O) + qH. The terms are returned separately. */
    public static long opportunityWp(long opportunities, int a) { return opportunities * a; }

    /** qH. Fortune raises H, never A(O), so it enters here and nowhere else. */
    public static long harvestWp(long harvestedUnits, int q) { return harvestedUnits * q; }

    /** Production: WP_P = A(P) + qQ, with A(P) paid once per output type. */
    public static long productionWp(boolean firstTime, int undertaking, int perOutput, int made) {
        return (firstTime ? undertaking : 0) + (long) perOutput * made;
    }

    /** One ore kind's Extraction terms. A(O) and qH never merge. */
    public static final class Ore {
        public long opportunities;
        public long harvestedUnits;
        public long opportunityWp;
        public long harvestWp;
        public long total() { return opportunityWp + harvestWp; }
        /** Units obtained per block broken: the multi-drop factor. */
        public double unitsPerOpportunity() {
            return opportunities == 0 ? 0 : harvestedUnits / (double) opportunities;
        }
    }

    /** One Production category's terms. */
    public static final class Category {
        public long outputs;          // items produced
        public long undertakings;     // distinct transformations begun
        public long wp;
        public final Map<String, Long> byOutput = new LinkedHashMap<>();
    }

    private final Map<String, Ore> ores = new LinkedHashMap<>();
    private final Map<String, Category> production = new LinkedHashMap<>();
    private long constructionWp, constructionPlacements;
    private long explorationWp, developmentWp;

    public Ore ore(String kind) { return ores.computeIfAbsent(kind, k -> new Ore()); }
    public Category category(String name) { return production.computeIfAbsent(name, k -> new Category()); }
    public Map<String, Ore> ores() { return ores; }
    public Map<String, Category> production() { return production; }

    public void extraction(String kind, long opportunities, long opportunityWp,
                           long harvestedUnits, long harvestWp) {
        Ore o = ore(kind);
        o.opportunities += opportunities;
        o.opportunityWp += opportunityWp;
        o.harvestedUnits += harvestedUnits;
        o.harvestWp += harvestWp;
    }

    public void produced(String category, String output, long count, long wp, boolean undertaking) {
        Category c = category(category);
        c.outputs += count;
        c.wp += wp;
        if (undertaking) c.undertakings++;
        c.byOutput.merge(output, count, Long::sum);
    }

    public void placed(long wp) { constructionWp += wp; constructionPlacements++; }
    public void explored(long wp) { explorationWp += wp; }
    public void developed(long wp) { developmentWp += wp; }

    public long opportunityWp() { return ores.values().stream().mapToLong(o -> o.opportunityWp).sum(); }
    public long harvestWp() { return ores.values().stream().mapToLong(o -> o.harvestWp).sum(); }
    public long opportunities() { return ores.values().stream().mapToLong(o -> o.opportunities).sum(); }
    public long harvestedUnits() { return ores.values().stream().mapToLong(o -> o.harvestedUnits).sum(); }
    public long extractionWp() { return opportunityWp() + harvestWp(); }
    public long productionWp() { return production.values().stream().mapToLong(c -> c.wp).sum(); }
    public long constructionWp() { return constructionWp; }
    public long constructionPlacements() { return constructionPlacements; }
    public long explorationWp() { return explorationWp; }
    public long developmentWp() { return developmentWp; }
    public long totalWp() {
        return extractionWp() + productionWp() + constructionWp + explorationWp + developmentWp;
    }

    /** Share of Extraction that came from harvest rather than opportunity. */
    public double harvestShare() {
        long t = extractionWp();
        return t == 0 ? 0 : harvestWp() / (double) t;
    }

    public void clear() {
        ores.clear(); production.clear();
        constructionWp = constructionPlacements = explorationWp = developmentWp = 0;
    }

    /** Human-readable decomposition, for /moba work and for benchmark output. */
    public java.util.List<String> report() {
        var out = new java.util.ArrayList<String>();
        out.add(String.format("EXTRACTION %d WP = A(O) %d + qH %d  (harvest share %.0f%%)",
                extractionWp(), opportunityWp(), harvestWp(), harvestShare() * 100));
        out.add(String.format("  %d opportunities, %d harvested units", opportunities(), harvestedUnits()));
        ores.forEach((kind, o) -> out.add(String.format(
                "  %-16s opp %4d  units %5d  (%.2f/opp)  A %5d  qH %5d  = %5d WP",
                kind, o.opportunities, o.harvestedUnits, o.unitsPerOpportunity(),
                o.opportunityWp, o.harvestWp, o.total())));
        out.add(String.format("PRODUCTION %d WP", productionWp()));
        production.forEach((name, c) -> {
            out.add(String.format("  %-18s outputs %4d  undertakings %3d  = %5d WP",
                    name, c.outputs, c.undertakings, c.wp));
            c.byOutput.forEach((output, n) -> out.add(String.format("      %-24s x%d", output, n)));
        });
        out.add(String.format("CONSTRUCTION %d WP from %d first-time placements",
                constructionWp, constructionPlacements));
        out.add(String.format("DEVELOPMENT %d WP   EXPLORATION %d WP", developmentWp, explorationWp));
        out.add(String.format("TOTAL %d WP", totalWp()));
        return out;
    }
}
