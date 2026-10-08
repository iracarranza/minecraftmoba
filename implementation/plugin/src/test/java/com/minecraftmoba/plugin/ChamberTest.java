package com.minecraftmoba.plugin;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The chamber's rules, and what a preview actually costs a viewer.
 *
 * Both are ordinary logic, so both are settled here rather than on a server.
 * The Bukkit half -- particles, block writes, world generation -- is a thin
 * layer over these and is where the untestable part is confined.
 */
class ChamberTest {

    private static final UUID WORLD = UUID.randomUUID();
    private static final UUID OTHER_WORLD = UUID.randomUUID();
    private final UUID owner = UUID.randomUUID();

    private Chamber chamber() {
        return Chamber.around(WORLD, 0, 60, 0, 16, 20, owner);
    }

    private static Chamber.At at(int x, int y, int z) { return new Chamber.At(WORLD, x, y, z); }

    // ---- containment -------------------------------------------------------

    @Test void aChamberKnowsItsOwnSpace() {
        var c = chamber();
        assertTrue(c.contains(at(0, 60, 0)));
        assertTrue(c.contains(at(16, 80, 16)), "the far corner is inside");
        assertFalse(c.contains(at(17, 60, 0)), "one block past is outside");
        assertFalse(c.contains(at(0, 59, 0)), "below the floor is outside");
    }

    @Test void theSameCoordinatesInAnotherWorldAreNotInside() {
        assertFalse(chamber().contains(new Chamber.At(OTHER_WORLD, 0, 60, 0)));
    }

    @Test void enclosureAsksAboutTheWholePlacementNotItsAnchor() {
        // The check that matters. An anchor inside a chamber says nothing
        // about where a forty-block corridor ends up.
        var c = chamber();
        var corridor = new ArrayList<Chamber.At>();
        for (int x = 0; x < 40; x++) corridor.add(at(x, 65, 0));

        assertTrue(c.contains(corridor.get(0)), "the anchor is inside");
        assertFalse(c.encloses(corridor), "and the route is not");
    }

    @Test void aRefusalSaysWhereItEscaped() {
        var c = chamber();
        var escapes = c.escapes(List.of(at(0, 65, 0), at(40, 65, 0), at(50, 65, 0)));
        assertEquals(2, escapes.size());
        assertEquals(40, escapes.get(0).x());
    }

    @Test void anEmptyPlacementIsNotEnclosed() {
        // A placement that affects nothing is not one that fits; it is one
        // that failed to produce anything, and "yes, it fits" would let it
        // through the one gate that would have noticed.
        assertFalse(chamber().encloses(List.of()));
        assertFalse(chamber().encloses(null));
    }

    // ---- allotment ---------------------------------------------------------

    @Test void overlappingChambersAreDetectedBeforeTheyAreAllotted() {
        // Two testers on shared ground would see each other's terrain change,
        // and an undo in one would revert blocks the other placed.
        var a = Chamber.around(WORLD, 0, 60, 0, 16, 20, owner);
        var b = Chamber.around(WORLD, 20, 60, 0, 16, 20, UUID.randomUUID());
        var far = Chamber.around(WORLD, 100, 60, 0, 16, 20, UUID.randomUUID());

        assertTrue(a.overlaps(b), "33 wide at 20 apart must overlap");
        assertFalse(a.overlaps(far));
        assertFalse(a.overlaps(Chamber.around(OTHER_WORLD, 0, 60, 0, 16, 20, owner)),
                "another world is not shared ground");
    }

    @Test void ownershipIsExplicitRatherThanInferredFromPresence() {
        var c = chamber();
        assertTrue(c.ownedBy(owner));
        assertFalse(c.ownedBy(UUID.randomUUID()), "standing in a chamber is not owning it");
    }

    @Test void aChamberReportsItsVolume() {
        // 33 x 21 x 33. Worth knowing before a regeneration is offered as
        // undoable: LabUndo's budget is 120,000 blocks.
        assertEquals(33L * 21 * 33, chamber().volume());
        assertTrue(chamber().volume() < 120_000, "this size can be journalled");
        assertTrue(Chamber.around(WORLD, 0, 60, 0, 40, 40, owner).volume() > 120_000,
                "and a larger one cannot, which is a decision not an accident");
    }

    @Test void inverseBoundsAreRefusedRatherThanSilentlyEmpty() {
        assertThrows(IllegalArgumentException.class,
                () -> new Chamber(UUID.randomUUID(), WORLD, 10, 60, 0, 0, 80, 16, owner));
    }

    // ---- preview cost ------------------------------------------------------

    @Test void aRouteIsDrawnAtItsDecisionsNotEveryColumn() {
        // Forty identical columns tell a tester nothing the first and last do
        // not, and cost forty particles to say it.
        var line = new ArrayList<Chamber.At>();
        var characters = new ArrayList<String>();
        for (int x = 0; x < 40; x++) {
            line.add(at(x, 65, 0));
            characters.add(x < 10 ? "STRAIGHT" : x < 20 ? "STAIR" : "STRAIGHT");
        }
        var drawn = ChamberPreview.route(line, characters);

        assertEquals(4, drawn.size(), "both ends and the two changes: " + drawn);
        assertEquals(0, drawn.get(0).x());
        assertEquals(39, drawn.get(drawn.size() - 1).x(), "the far end is always drawn");
    }

    @Test void aRouteWithNoChangesIsStillShownAtBothEnds() {
        var line = List.of(at(0, 65, 0), at(1, 65, 0), at(2, 65, 0));
        var drawn = ChamberPreview.route(line, List.of("STRAIGHT", "STRAIGHT", "STRAIGHT"));
        assertEquals(2, drawn.size(), "extent, which is what is being judged");
    }

    @Test void aVolumeIsDrawnAsEdgesRatherThanAsFog() {
        // A filled 9x5x9 footprint is 405 particles and reads as a block of
        // white where the terrain used to be.
        var volume = new ArrayList<Chamber.At>();
        for (int x = 0; x < 9; x++)
            for (int y = 0; y < 5; y++)
                for (int z = 0; z < 9; z++) volume.add(at(x, 60 + y, z));
        assertEquals(405, volume.size());

        var drawn = ChamberPreview.outline(volume);
        assertTrue(drawn.size() < volume.size() / 2,
                "outline is " + drawn.size() + " of " + volume.size());
        assertTrue(drawn.contains(at(0, 60, 0)) && drawn.contains(at(8, 64, 8)),
                "and it still carries both extreme corners");
    }

    @Test void theBudgetDegradesDetailAndNeverExtent() {
        // A preview that silently dropped its tail would mislead about where a
        // placement stops, which is the one thing a tester is certain to be
        // reading.
        var points = new ArrayList<Chamber.At>();
        for (int x = 0; x < 500; x++) points.add(at(x, 65, 0));

        var drawn = ChamberPreview.withinBudget(points, ChamberPreview.DEFAULT_BUDGET);
        assertEquals(ChamberPreview.DEFAULT_BUDGET, drawn.size());
        assertEquals(points.get(0), drawn.get(0));
        assertEquals(points.get(points.size() - 1), drawn.get(drawn.size() - 1));
    }

    @Test void somethingAlreadyWithinBudgetIsLeftAlone() {
        var points = List.of(at(0, 65, 0), at(1, 65, 0));
        assertSame(points, ChamberPreview.withinBudget(points, 50));
    }

    @Test void aZeroBudgetDrawsNothingRatherThanEverything() {
        assertTrue(ChamberPreview.withinBudget(List.of(at(0, 65, 0)), 0).isEmpty());
    }

    @Test void theWorstCaseRoutePreviewFitsInOneViewersBudget() {
        // The number the review was worried about: a 200-column corridor three
        // wide is 600 blocks. Reduced and budgeted, it is a readable handful.
        var line = new ArrayList<Chamber.At>();
        var characters = new ArrayList<String>();
        for (int x = 0; x < 200; x++) {
            line.add(at(x, 65, 0));
            characters.add(x % 7 == 0 ? "STAIR" : "STRAIGHT");     // a busy path
        }
        var drawn = ChamberPreview.withinBudget(
                ChamberPreview.route(line, characters), ChamberPreview.DEFAULT_BUDGET);
        assertTrue(drawn.size() <= ChamberPreview.DEFAULT_BUDGET, "" + drawn.size());
    }
}
