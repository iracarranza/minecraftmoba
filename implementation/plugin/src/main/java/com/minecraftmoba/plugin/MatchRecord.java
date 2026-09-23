package com.minecraftmoba.plugin;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/**
 * What the first real playtests should record, and nothing more.
 *
 * Nothing has been competitively played yet. Every threshold in the compiler is
 * PROVISIONAL_ALPHA, and the question the last several passes keep arriving at
 * -- whether any of these disparities are felt in play -- is not answerable by
 * analysis. It becomes answerable the moment a generated map is played on, and
 * only if the match wrote down enough to compare against what the compiler
 * predicted.
 *
 * Deliberately not an analytics platform. It captures facts the systems already
 * produce, keyed to the realization that produced them, so a compiler
 * prediction and a match outcome can be put side by side. It does not retune
 * anything: one match is evidence, not a mandate.
 */
public final class MatchRecord {
    private final List<String> events = new ArrayList<>();
    private final long started = System.currentTimeMillis();
    private String mapId = "unknown";
    private long seed;

    private void event(String kind, String detail) {
        events.add((System.currentTimeMillis() - started) + "ms\t" + kind + "\t" + detail);
    }

    public void matchBound(String mapId, long seed, MapBindings bindings) {
        this.mapId = mapId == null ? "template" : mapId;
        this.seed = seed;
        event("bound", "map=" + this.mapId + " seed=" + seed
                + (bindings == null ? "" : " " + bindings.report()));
    }

    /** What the compiler predicted, so play can be compared against it. */
    public void prediction(String key, String value) {
        event("prediction", key + "=" + value);
    }

    public void night(int ordinal, String stage, int activatedWorksites, String lair) {
        event("night", "n=" + ordinal + " stage=" + stage
                + " worksites=" + activatedWorksites + " lair=" + lair);
    }

    /** Capacity moved, and by which route: the comparison the model exists for. */
    public void siege(Team team, TeamObjectives.Kind kind, DefensiveCapacity.Source source,
                      double remaining, double initial, boolean toppled) {
        event(toppled ? "topple" : "siege",
                team.lower() + "/" + kind + " by=" + source
                + " remaining=" + String.format("%.1f", remaining)
                + "/" + String.format("%.1f", initial));
    }

    public void lairContest(OpportunityCadence.Boss boss, Team victor, String outcome) {
        event("lair", boss + " victor=" + (victor == null ? "unknown" : victor.lower())
                + " " + outcome);
    }

    /** An UNCONFIGURED or failed binding. The single most important thing to see. */
    public void problem(String what) { event("PROBLEM", what); }

    public void ended(Team winner) {
        event("end", "winner=" + (winner == null ? "none" : winner.lower())
                + " duration_ms=" + (System.currentTimeMillis() - started));
    }

    public List<String> lines() { return Collections.unmodifiableList(events); }

    /**
     * Write the record beside the realization that produced it.
     *
     * Beside it on purpose: a match is only interpretable under the contract
     * that certified its map, and the provenance is already there.
     */
    public void write(Path directory) {
        if (directory == null) return;
        try {
            Files.createDirectories(directory);
            Files.writeString(directory.resolve("match-" + started + ".tsv"),
                    "# map=" + mapId + " seed=" + seed + "\n"
                    + "# PROVISIONAL_ALPHA thresholds; one match is evidence, not a mandate\n"
                    + String.join("\n", events) + "\n",
                    StandardCharsets.UTF_8);
        } catch (IOException io) {
            // A failed record must not take a match down with it.
        }
    }
}
