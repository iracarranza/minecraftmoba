package com.minecraftmoba.plugin;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/** The pure half of the chamber UI: item stacks, intents, refusals, platform, region mapping. */
class ChamberUiTest {
    private static final ChamberController.State OUT = new ChamberController.State(false, 0, false, false, false, false, false);
    private static final ChamberController.State IN = new ChamberController.State(true, 0, false, false, false, false, false);

    // ---- items ---------------------------------------------------------------

    @Test void theWayBackIsAlwaysAnArrow() {
        var back = new ChamberMenu.Item("back", "Go back", ChamberMenu.Kind.BACK);
        assertEquals(Material.ARROW, ChamberItems.spec(back, null).material());
        assertEquals(Material.ARROW, ChamberItems.spec(back, "ignored").material(),
                "BACK is never refused, so a refusal passed for it must not grey it");
    }

    @Test void anUnavailableItemIsGreyedAndSaysWhy() {
        var undo = ChamberMenu.Item.action("undo", "Undo");
        var spec = ChamberItems.spec(undo, "Nothing to undo in this bay.");
        assertFalse(spec.available());
        assertEquals(ChamberItems.GREYED, spec.material());
        assertTrue(spec.lore().get(0).contains("Nothing to undo"));
        assertEquals("Undo", spec.name(), "greying changes the look, not the label");
    }

    @Test void everyShippedItemHasADistinctLookWhenAvailable() {
        var menu = ChamberMenu.chamberMenu();
        var seen = new HashMap<Material, String>();
        for (String page : List.of("root", "objective", "renewable", "route", "pending")) {
            menu.reset(); if (!page.equals("root")) menu.open(page);
            var view = menu.view(ChamberMenu.Gate.OPEN);
            for (var item : view.slots()) {
                if (item == null || item.kind() == ChamberMenu.Kind.BACK) continue;
                var spec = ChamberItems.spec(item, null);
                assertNotEquals(ChamberItems.GREYED, spec.material(), item.id());
                String clash = seen.put(spec.material(), item.id());
                // The same material on two ids is legal only across different pages.
                if (clash != null && !clash.equals(item.id()))
                    assertNotEquals(page, "root", item.id() + " clashes with " + clash);
            }
        }
    }

    // ---- intents ---------------------------------------------------------------

    @Test void everyActionTheMenuOffersHasAnIntentOrIsDeliberatelyUnavailable() {
        var menu = ChamberMenu.chamberMenu();
        for (String page : List.of("root", "objective", "renewable", "route", "pending")) {
            menu.reset(); if (!page.equals("root")) menu.open(page);
            for (var item : menu.view(ChamberMenu.Gate.OPEN).slots()) {
                if (item == null || item.kind() != ChamberMenu.Kind.ACTION) continue;
                var intent = ChamberController.intent(item.id());
                if (intent.kind() == ChamberController.Kind.UNAVAILABLE)
                    assertEquals("redo", item.id(), item.id() + " has no verb behind it");
            }
        }
    }

    @Test void theMenuAndTheAuthoringTemplatesAgreeOnNames() {
        assertEquals(List.of("preview", "bastion"), ChamberController.intent("rampart").args(),
                "the menu's Rampart is the authoring bastion template");
        assertEquals(List.of("preview", "outpost"), ChamberController.intent("outpost").args());
        assertEquals(List.of("fauna", "sheep"), ChamberController.intent("fauna.sheep").args());
        assertEquals(List.of("swarm", "mountain_ravager"),
                ChamberController.intent("swarm.mountain_ravager").args());
    }

    @Test void routeEndpointsMapToThePathVerbs() {
        assertEquals(List.of("path", "from"), ChamberController.intent("route.from").args());
        assertEquals(List.of("path", "to"), ChamberController.intent("route.to").args());
    }

    // ---- refusals -------------------------------------------------------------

    @Test void nothingWorksUntilTheTesterIsInsideTheirBay() {
        for (String id : List.of("fountain", "undo", "regenerate", "route.from"))
            assertNotNull(ChamberController.refusal(id, OUT), id);
    }

    @Test void undoIsGreyedUntilThereIsHistory() {
        assertNotNull(ChamberController.refusal("undo", IN));
        assertNull(ChamberController.refusal("undo", new ChamberController.State(true, 1, false, false, false, false, false)));
    }

    @Test void redoIsAlwaysUnavailableBecauseNoForwardJournalExists() {
        var busy = new ChamberController.State(true, 5, true, true, false, false, false);
        assertNotNull(ChamberController.refusal("redo", busy));
        assertEquals(ChamberController.Kind.UNAVAILABLE, ChamberController.intent("redo").kind());
    }

    @Test void regenerationNeedsASourceChosenAtThePlatform() {
        assertNotNull(ChamberController.refusal("regenerate", IN));
        assertNull(ChamberController.refusal("regenerate", new ChamberController.State(true, 0, false, true, false, false, false)));
    }

    @Test void pendingVerbsNeedAPendingPlacement() {
        for (String id : List.of("confirm", "preview", "cancel")) {
            assertNotNull(ChamberController.refusal(id, IN), id);
            assertNull(ChamberController.refusal(id, new ChamberController.State(true, 0, true, false, false, false, false)), id);
        }
    }

    // ---- platform ---------------------------------------------------------------

    private static Chamber bay(int n) {
        var chambers = new Chambers(UUID.randomUUID(), 16, 20, 60);
        Chamber c = null;
        for (int i = 0; i <= n; i++) c = chambers.allot(UUID.randomUUID());
        return c;
    }

    @Test void thePlatformStandsInTheGutterNotOverTheBay() {
        var c = bay(0);
        var plat = ChamberPlatform.of(c, Chambers.GUTTER);
        for (var box : plat.footprint()) {
            assertTrue(box.maxZ() < c.minZ(), "every block lies north of the bay");
            assertTrue(box.minZ() > c.minZ() - Chambers.GUTTER, "and inside the gutter, clear of the next bay");
        }
    }

    @Test void noPlatformTouchesAnyBayOrAnyOtherPlatformAcrossTheFirstBays() {
        var chambers = new Chambers(UUID.randomUUID(), 16, 20, 60);
        var bays = new ArrayList<Chamber>();
        for (int i = 0; i < 25; i++) bays.add(chambers.allot(UUID.randomUUID()));
        var plats = bays.stream().map(b -> ChamberPlatform.of(b, Chambers.GUTTER)).toList();
        for (int i = 0; i < bays.size(); i++) {
            for (int j = 0; j < bays.size(); j++) {
                var bayBox = new ChamberPlatform.Box(bays.get(j).minX(), bays.get(j).minY(), bays.get(j).minZ(),
                        bays.get(j).maxX(), bays.get(j).maxY(), bays.get(j).maxZ());
                for (var f : plats.get(i).footprint())
                    assertFalse(f.overlaps(bayBox), "platform " + i + " intrudes on bay " + j);
                if (i < j) for (var a : plats.get(i).footprint()) for (var b : plats.get(j).footprint())
                    assertFalse(a.overlaps(b), "platform " + i + " overlaps platform " + j);
            }
        }
    }

    @Test void theButtonsAndPlateStandOnTheDeck() {
        var plat = ChamberPlatform.of(bay(3), Chambers.GUTTER);
        var above = new ChamberPlatform.Box(plat.deck().minX(), plat.deck().maxY() + 1, plat.deck().minZ(),
                plat.deck().maxX(), plat.deck().maxY() + 1, plat.deck().maxZ());
        assertTrue(above.contains(plat.certifiedButton()));
        assertTrue(above.contains(plat.randomButton()));
        assertTrue(above.contains(plat.plate()));
        assertNotEquals(plat.certifiedButton(), plat.randomButton(),
                "two buttons, never one toggle");
    }

    @Test void theDoorwayIsInTheWindowWall() {
        var plat = ChamberPlatform.of(bay(0), Chambers.GUTTER);
        assertTrue(plat.doorX() >= plat.window().minX() && plat.doorX() <= plat.window().maxX());
    }

    // ---- region mapping ---------------------------------------------------------

    @Test void theBayCentreMapsToTheSourceCentreAndTheSurfaceLandsAboveTheFloor() {
        var c = bay(0);
        var m = RegionCopy.of(c, 1000, -500, 95);
        int cx = (c.minX() + c.maxX()) / 2, cz = (c.minZ() + c.maxZ()) / 2;
        assertEquals(1000, m.sourceX(cx));
        assertEquals(-500, m.sourceZ(cz));
        assertEquals(95, m.sourceY(c.minY() + RegionCopy.SURFACE_ABOVE_FLOOR),
                "a surface at 95 must land SURFACE_ABOVE_FLOOR above the bay floor");
    }

    @Test void theMappingPreservesRelativeOffsets() {
        var c = bay(0);
        var m = RegionCopy.of(c, 0, 0, 70);
        assertEquals(m.sourceX(c.minX() + 5) - m.sourceX(c.minX()), 5);
        assertEquals(m.sourceY(c.minY() + 3) - m.sourceY(c.minY()), 3);
    }
}
