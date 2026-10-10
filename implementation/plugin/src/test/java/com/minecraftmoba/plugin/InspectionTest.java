package com.minecraftmoba.plugin;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashSet;
import java.util.List;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

class InspectionTest {
    private static final String SAMPLE = """
        {"schema":"moba_map_inspection/1","cell_size":64,"opening_cost":120.0,"units":"cost","note":"n",
         "cells":[
          {"cell":[0,0],"origin":[0,0],"centroid":[32,32],"cost":{"north":40.0,"south":700.0},"relation":"cheap_north","band":{"north":"near","south":"deeper"},"surface_y":70.4},
          {"cell":[1,0],"origin":[64,0],"centroid":[96,32],"cost":{"north":100.0,"south":600.0},"relation":"cheap_north","band":{"north":"near","south":"deeper"},"surface_y":null},
          {"cell":[2,0],"origin":[128,0],"centroid":[160,32],"cost":{"north":400.0,"south":380.0},"relation":"equal","band":{"north":"farther","south":"farther"},"surface_y":65},
          {"cell":[0,1],"origin":[0,64],"centroid":[32,96],"cost":{"north":null,"south":500.0},"relation":"only_south","band":{"south":"deeper"},"surface_y":60}],
         "fountains":{"north":[-10,67,5],"south":[900,67,5]},
         "objectives":[{"team":"north","kind":"end_spike","x":1,"z":2}],
         "lair":[400,68,10],
         "worksites":[{"id":"ws_1","x":10,"z":20,"reach":null}],
         "points":[{"id":"p1","kind":"rabbit","type":"ANIMAL","band":"near","near_team":"north","cell":[0,0],"x":5,"y":70,"z":6,"radius":24,"capacity":6},
                   {"id":"p2","kind":"wheat","type":"CROP","band":"farther","near_team":"south","cell":[2,0],"x":150,"y":null,"z":40,"radius":null,"capacity":null}]}
        """;

    // ---- parsing --------------------------------------------------------------

    @Test void parsesCellsWithNullCostsAndMissingBands() {
        var m = MapInspection.parse(SAMPLE);
        assertEquals(4, m.cells().size()); assertEquals(64, m.cellSize()); assertEquals(120.0, m.openingCost());
        var unreachable = m.cells().get(3);
        assertNull(unreachable.north()); assertEquals(500.0, unreachable.south());
        assertNull(unreachable.bandNorth()); assertEquals("deeper", unreachable.bandSouth());
        assertEquals(70, m.cells().get(0).surfaceY()); assertNull(m.cells().get(1).surfaceY());
    }

    @Test void parsesLandmarksAndPoints() {
        var m = MapInspection.parse(SAMPLE);
        assertArrayEquals(new int[]{-10, 67, 5}, m.fountains().get("north"));
        assertEquals("north end_spike", m.objectives().get(0).label());
        assertArrayEquals(new int[]{400, 68, 10}, m.lair());
        assertEquals("ws_1", m.worksites().get(0).label());
        assertEquals("rabbit", m.points().get(0).kind()); assertEquals(24, m.points().get(0).radius());
        assertEquals(0, m.points().get(1).y(), "a missing y is zero, not a failure"); assertEquals(0, m.points().get(1).capacity());
    }

    @Test void refusesAFileThatIsNotAnInspection() {
        assertThrows(IllegalArgumentException.class, () -> MapInspection.parse("{\"schema\":\"something_else\"}"));
    }

    @Test void selectsCellsAndPointsWithinARadius() {
        var m = MapInspection.parse(SAMPLE);
        assertEquals(3, m.cellsWithin(40, 40, 70).size());          // (32,32), (96,32), (32,96)
        assertEquals(1, m.cellsWithin(32, 32, 5).size());
        assertEquals(1, m.pointsWithin(5, 6, 10).size());
        assertEquals(700.0, m.maxCost("south")); assertEquals(400.0, m.maxCost("north"));
    }

    // ---- colours ------------------------------------------------------------------

    @Test void costBucketsAreAnchoredOnTheOpeningCost() {
        assertEquals("LIME_STAINED_GLASS", InspectionDraw.costGlass(120.0, 120.0));     // the opening is inclusive
        assertEquals("YELLOW_STAINED_GLASS", InspectionDraw.costGlass(121.0, 120.0));
        assertEquals("ORANGE_STAINED_GLASS", InspectionDraw.costGlass(480.0, 120.0));
        assertEquals("RED_STAINED_GLASS", InspectionDraw.costGlass(720.0, 120.0));
        assertEquals("PURPLE_STAINED_GLASS", InspectionDraw.costGlass(721.0, 120.0));
        assertEquals("GRAY_STAINED_GLASS", InspectionDraw.costGlass(null, 120.0));
    }

    @Test void everyNamedColourIsARealMaterial() {
        for (String r : new String[]{"contested_opening", "cheap_north", "cheap_south", "equal", "behind_north", "behind_south", "only_north", "only_south", "unreachable", "whatever", null})
            assertNotNull(Material.valueOf(InspectionDraw.relationGlass(r)), r);
        for (String t : new String[]{"CROP", "ANIMAL", "SWARM", "OTHER", null}) assertNotNull(Material.valueOf(InspectionDraw.pointGlass(t)));
        for (double c : new double[]{0, 100, 200, 400, 600, 900}) assertNotNull(Material.valueOf(InspectionDraw.costGlass(c, 120.0)));
    }

    @Test void relationColoursAreDistinctWhereTheMeaningsDiffer() {
        var seen = new HashSet<String>();
        for (String r : new String[]{"contested_opening", "cheap_north", "cheap_south", "equal", "behind_north", "behind_south", "only_north", "only_south"})
            assertTrue(seen.add(InspectionDraw.relationGlass(r)), r + " shares a colour");
    }

    @Test void legendsMatchTheColours() {
        assertEquals(6, InspectionDraw.costLegend(120.0).size()); assertEquals(9, InspectionDraw.relationLegend().size());
        assertEquals("LIME", InspectionDraw.bandColour("near")); assertEquals("WHITE", InspectionDraw.bandColour(null));
    }

    // ---- edges ---------------------------------------------------------------------

    @Test void gridEdgesAreDeduplicatedWhereCellsShareABorder() {
        var m = MapInspection.parse(SAMPLE);
        var two = m.cells().subList(0, 2);                  // side by side
        assertEquals(7, InspectionDraw.gridEdges(two, 64).size(), "8 borders minus the one they share");
        assertEquals(4, InspectionDraw.gridEdges(m.cells().subList(0, 1), 64).size());
    }

    @Test void bandEdgesRunOnlyBetweenAdjacentCellsWhoseBandDiffers() {
        var m = MapInspection.parse(SAMPLE);
        // North: (0,0) near | (1,0) near | (2,0) farther. One edge, between (1,0) and (2,0), at x = 128.
        var north = InspectionDraw.bandEdges(m.cells(), 64, "north");
        assertTrue(north.contains(new InspectionDraw.Edge(128, 0, 128, 64)), north.toString());
        assertFalse(north.contains(new InspectionDraw.Edge(64, 0, 64, 64)), "same band: no edge");
        // (0,0) north=near vs (0,1) north=null: differs, so the reachable frontier shows as an edge.
        assertTrue(north.contains(new InspectionDraw.Edge(0, 64, 64, 64)));
        // South: (1,0) deeper | (2,0) farther -> edge at x = 128.
        assertTrue(InspectionDraw.bandEdges(m.cells(), 64, "south").contains(new InspectionDraw.Edge(128, 0, 128, 64)));
    }

    @Test void aLoneCellHasNoBandEdges() {
        assertTrue(InspectionDraw.bandEdges(MapInspection.parse(SAMPLE).cells().subList(0, 1), 64, "north").isEmpty());
    }

    @Test void edgesAreSampledEndToEnd() {
        var pts = InspectionDraw.along(new InspectionDraw.Edge(0, 0, 64, 0), 8);
        assertEquals(9, pts.size()); assertArrayEquals(new int[]{0, 0}, pts.get(0)); assertArrayEquals(new int[]{64, 0}, pts.get(8));
        assertEquals(2, InspectionDraw.along(new InspectionDraw.Edge(5, 5, 5, 5), 8).size());
    }

    @Test void cellKeysAreUniquePerCell() {
        var m = MapInspection.parse(SAMPLE);
        assertEquals(4, InspectionDraw.keys(m.cells()).size());
        assertTrue(InspectionDraw.keys(m.cells()).contains(InspectionDraw.keyOf(m.cells().get(2))));
    }

    // ---- the menu ---------------------------------------------------------------------

    @Test void theMenuFitsAndEveryActionHasAnIntent() {
        for (var item : InspectionMenu.menu().view(ChamberMenu.Gate.OPEN).slots())
            if (item != null) assertNotNull(InspectionMenu.intent(item.id()), item.id());
        assertNull(InspectionMenu.intent("nonsense"));
        assertEquals(InspectionMenu.Layer.BANDS, InspectionMenu.layer("bands")); assertNull(InspectionMenu.layer("team"));
    }

    @Test void refusalsSayWhetherTheScoopHasDataOrTheOverlayIsOff() {
        assertTrue(InspectionMenu.refusal("cells", new InspectionMenu.State(true, false)).contains("no inspection data"));
        assertTrue(InspectionMenu.refusal("cells", new InspectionMenu.State(false, true)).contains("Start the overlay"));
        assertNull(InspectionMenu.refusal("cells", new InspectionMenu.State(true, true)));
    }
}
