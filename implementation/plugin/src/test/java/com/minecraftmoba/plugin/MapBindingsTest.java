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
}
