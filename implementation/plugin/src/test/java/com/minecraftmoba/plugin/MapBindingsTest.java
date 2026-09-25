package com.minecraftmoba.plugin;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Generated maps carry their own positions.
 *
 * The runtime used to read `alpha.homelands` and `alpha.lair.site` from config,
 * which is correct for the one frozen template and describes a different world
 * for every generated map. That is how a claimed realization could reach the
 * cadence with its Lair UNCONFIGURED.
 */
class MapBindingsTest {

    private static final String MANIFEST = """
        {"world":{"name":"gen","map_type":"default"},
         "homelands":{"north":[0,0,0,0],"south":[1,1,1,1]},
         "fountains":{"north":[-316,71,308],"south":[300,71,140]},
         "objectives":{"north":{"pillager_outpost":[-60,172],"nether_bastion":[-132,108],
                                "end_spike":[-188,140]},
                       "south":{"pillager_outpost":[236,196],"nether_bastion":[228,140],
                                "end_spike":[268,108]}},
         "lair":{"anchor":{"xyz":[148,95,-180]},"centre_xz":[148,-180],"count":1},
         "worksites":[{"id":"ws_a","world_xz":[10,20]},{"id":"ws_b","world_xz":[30,40]}]}
        """;

    @Test void fountainsComeFromTheManifest() {
        var b = MapBindings.parse(MANIFEST);
        var north = b.fountain(null, Team.values()[0]);
        assertNotNull(north);
        assertEquals(-316, north.getBlockX());
        assertEquals(71, north.getBlockY());
        assertEquals(308, north.getBlockZ());
    }

    @Test void allThreeObjectivesBindForBothTeams() {
        var b = MapBindings.parse(MANIFEST);
        for (Team t : Team.values())
            assertEquals(3, b.objectiveKinds(t).size(), t.lower());
    }

    @Test void theLairAnchorIsPresentAndSingular() {
        var b = MapBindings.parse(MANIFEST);
        assertEquals(1, b.lairCount());
        var anchor = b.lairAnchor(null);
        assertNotNull(anchor);
        assertEquals(95, anchor.getBlockY());
    }

    @Test void aManifestWithoutALairBindsNothingRatherThanDefaulting() {
        // Absent is absent. A silently defaulted anchor would put a boss in the
        // wrong place on a map that had never had one established.
        var b = MapBindings.parse("""
            {"world":{"name":"gen"},"lair":{"count":0}}""");
        assertNull(b.lairAnchor(null));
        assertEquals(0, b.lairCount());
    }

    @Test void aMalformedManifestDoesNotThrowIntoMatchStart() {
        var b = MapBindings.parse("""
            {"world":{"name":"gen"},"fountains":{"north":[1]}}""");
        assertNull(b.fountain(null, Team.values()[0]), "a short array is not a position");
    }

    @Test void theReportNamesWhatIsMissing() {
        assertTrue(MapBindings.parse(MANIFEST).report().contains("lair=anchored"));
        assertTrue(MapBindings.parse("{\"world\":{\"name\":\"g\"}}").report().contains("MISSING"));
    }

    /**
     * A portfolio the COMPILER actually derived, not one written by hand.
     *
     * This slice comes from a real compiled map's `cell_grid`, run through
     * `map_compiler._renewables`. Hand-written fixtures cannot catch a
     * producer/consumer mismatch, because they are written to the shape the
     * consumer expects -- which is exactly how a merge bug that replaced the
     * source LIST with its COUNT survived a green Python suite.
     */
    private static final String DERIVED = """
        {"derived": true, "sources": [{"id": "near_rabbit_north_[1, 6]", "cell": [1, 6], "type": "ANIMAL", "kind": "rabbit", "band": "near", "near_team": "north", "world": null, "x": -3154, "y": 64, "z": -530, "radius": 24, "capacity": 6, "herd_core": 3, "harvestable_surplus": 3, "recover_ticks": 12000, "strategic_depth_cost": {"north": 16.97, "south": 1311.71}}, {"id": "near_rabbit_north_[0, 6]", "cell": [0, 6], "type": "ANIMAL", "kind": "rabbit", "band": "near", "near_team": "north", "world": null, "x": -3218, "y": 65, "z": -530, "radius": 24, "capacity": 6, "herd_core": 3, "harvestable_surplus": 3, "recover_ticks": 12000, "strategic_depth_cost": {"north": 65.94, "south": 1345.28}}, {"id": "near_rabbit_north_[1, 5]", "cell": [1, 5], "type": "ANIMAL", "kind": "rabbit", "band": "near", "near_team": "north", "world": null, "x": -3154, "y": 64, "z": -594, "radius": 24, "capacity": 6, "herd_core": 3, "harvestable_surplus": 3, "recover_ticks": 12000, "strategic_depth_cost": {"north": 79.31, "south": 1236.42}}], "source_count": 10, "certified": true, "problems": [], "per_team": {"north": {"opening_cells": 5, "wild_developable": ["rabbit"], "manifestations_in_opening": 3, "manifestation_kinds": ["rabbit"]}, "south": {"opening_cells": 5, "wild_developable": ["rabbit"], "manifestations_in_opening": 3, "manifestation_kinds": ["rabbit"]}}, "by_band": {"near": ["rabbit"], "farther": ["cow"], "strategic_access": ["sheep"]}, "skipped_total": 942, "quantities_are": "DECLARED FIXTURES. Patch sizes, herd sizes and recovery cadence are OPEN in maps.md and the recalled nourishment figures need re-derivation; the ORDERING is what is recovered, not the numbers.", "floor_is": "each team reaching one developable opportunity in its own opening, WILD OR MANIFESTED. Not a count -- one team's twelve cannot cover another team's none -- and not a species list, which would require both openings to hold the same kinds. Ordinary vanilla incidence counts, because strategic manifestations are layered over it rather than replacing it.", "opening_cost": 120.0}""";

    @Test
    void reads_a_portfolio_the_compiler_derived() {
        MapBindings b = MapBindings.parse("{\"renewables\":" + DERIVED + "}");
        var sources = b.renewables(null);
        assertEquals(3, sources.size(), "the derived sources must survive the manifest");
        var first = sources.get(0);
        assertNotNull(first.get("kind"));
        assertNotNull(first.get("type"));
        assertTrue((Integer) first.get("capacity") > 0, "capacity is what may manifest");
        assertTrue(b.renewablesCertified(), "this portfolio met its own opening floor");
    }

    @Test
    void capacity_and_herd_core_are_kept_apart() {
        MapBindings b = MapBindings.parse("{\"renewables\":" + DERIVED + "}");
        for (var s : b.renewables(null)) {
            int core = (Integer) s.get("herdCore");
            int surplus = (Integer) s.get("harvestableSurplus");
            assertEquals((Integer) s.get("capacity"), core + surplus,
                    "a population reduced to its core is depleted, not extinct");
        }
    }

    @Test
    void a_manifest_with_no_portfolio_returns_none_rather_than_defaults() {
        MapBindings b = MapBindings.parse("{\"world\":{\"name\":\"gen\"}}");
        assertTrue(b.renewables(null).isEmpty(), "absent is absent");
        assertFalse(b.renewablesCertified());
    }

    @Test
    void an_uncertified_portfolio_is_reported_as_such() {
        MapBindings b = MapBindings.parse(
                "{\"renewables\":{\"certified\":false,\"sources\":[]}}");
        assertFalse(b.renewablesCertified());
    }
}
