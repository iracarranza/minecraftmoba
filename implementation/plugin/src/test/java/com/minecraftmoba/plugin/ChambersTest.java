package com.minecraftmoba.plugin;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** Allotment, and the pending-placement state machine. */
class ChambersTest {

    private static final UUID WORLD = UUID.randomUUID();

    private Chambers chambers() { return new Chambers(WORLD, 16, 20, 60); }

    // ---- allotment ---------------------------------------------------------

    @Test void noTwoAllottedChambersEverOverlap() {
        // The point of a grid. Non-overlap stops being a rule that is enforced
        // and becomes a property of the layout.
        var c = chambers();
        var allotted = new java.util.ArrayList<Chamber>();
        for (int i = 0; i < 25; i++) allotted.add(c.allot(UUID.randomUUID()));

        for (int i = 0; i < allotted.size(); i++)
            for (int j = i + 1; j < allotted.size(); j++)
                assertFalse(allotted.get(i).overlaps(allotted.get(j)),
                        "bays " + i + " and " + j + " overlap");
    }

    @Test void thereIsUntouchedGroundBetweenNeighbours() {
        // A gutter, so an experiment that overruns by a block does not land in
        // somebody else's chamber before the containment check sees it.
        var c = chambers();
        assertEquals(16 * 2 + 1 + Chambers.GUTTER, c.pitch());
        var first = c.allot(UUID.randomUUID());
        var second = c.allot(UUID.randomUUID());
        int gap = Math.max(second.minX() - first.maxX(), second.minZ() - first.maxZ());
        assertTrue(gap > 1, "neighbours are " + gap + " apart");
    }

    @Test void oneChamberPerTesterAndASecondReplacesTheFirst() {
        // Two chambers means two undo histories and no way to say which
        // /moba lab undo meant, so the question is not asked.
        var c = chambers();
        var who = UUID.randomUUID();
        var first = c.allot(who);
        var second = c.allot(who);

        assertEquals(1, c.size());
        assertNotEquals(first.id(), second.id());
        assertEquals(second, c.of(who));
    }

    @Test void aReleasedBayIsReusedRatherThanLeavingTheLabToSprawl() {
        var c = chambers();
        var a = UUID.randomUUID();
        var first = c.allot(a);
        c.allot(UUID.randomUUID());
        c.release(a);

        var reused = c.allot(UUID.randomUUID());
        assertEquals(first.minX(), reused.minX(), "the gap is filled before the next ring");
        assertEquals(first.minZ(), reused.minZ());
    }

    @Test void aPositionFindsTheChamberThatHoldsIt() {
        var c = chambers();
        var mine = c.allot(UUID.randomUUID());
        c.allot(UUID.randomUUID());

        assertEquals(mine, c.at(new Chamber.At(WORLD, mine.minX(), mine.minY(), mine.minZ())));
        assertNull(c.at(new Chamber.At(WORLD, 100_000, 60, 100_000)), "the gutter belongs to nobody");
    }

    @Test void releasingSomethingNobodyHoldsIsHarmless() {
        assertNull(chambers().release(UUID.randomUUID()));
    }

    @Test void aChamberNeedsAPositiveRadius() {
        assertThrows(IllegalArgumentException.class, () -> new Chambers(WORLD, 0, 20, 60));
    }

    // ---- the pending placement --------------------------------------------

    private static ChamberSession.Pending good() {
        return new ChamberSession.Pending("outpost",
                List.of(new Chamber.At(WORLD, 0, 60, 0)), List.of(), false);
    }

    @Test void nothingResolvesWhileNothingIsPending() {
        var s = new ChamberSession();
        assertTrue(s.idle());
        assertEquals(ChamberSession.Outcome.WAITING, s.resolve(AimState.Decision.FIRE));
    }

    @Test void holdingLeavesThePlacementOnScreen() {
        var s = new ChamberSession();
        s.offer(good(), null);
        assertEquals(ChamberSession.Outcome.WAITING, s.resolve(AimState.Decision.HOLD));
        assertFalse(s.idle(), "still being looked at");
    }

    @Test void cancellingDiscardsIt() {
        var s = new ChamberSession();
        s.offer(good(), null);
        assertEquals(ChamberSession.Outcome.CANCELLED, s.resolve(AimState.Decision.CANCEL));
        assertTrue(s.idle());
    }

    @Test void confirmingAnAcceptablePlacementAppliesIt() {
        var s = new ChamberSession();
        s.offer(good(), null);
        assertEquals(ChamberSession.Outcome.APPLIED, s.resolve(AimState.Decision.FIRE));
        assertTrue(s.idle());
    }

    @Test void confirmingSomethingUnacceptableRefusesAndKeepsIt() {
        // The tester pressed confirm because the preview was in front of them.
        // Throwing it away would make them rebuild it to read why it was
        // refused -- and the reason is the thing they now want.
        var s = new ChamberSession();
        s.offer(new ChamberSession.Pending("route",
                List.of(new Chamber.At(WORLD, 0, 60, 0)),
                List.of("anchors are 18 apart over 8 columns"), false), null);

        assertEquals(ChamberSession.Outcome.REFUSED, s.resolve(AimState.Decision.FIRE));
        assertFalse(s.idle(), "the placement and its reason survive the refusal");
    }

    @Test void aPlacementThatLeavesTheChamberIsNotAcceptable() {
        var escaping = new ChamberSession.Pending("route",
                List.of(new Chamber.At(WORLD, 0, 60, 0)), List.of(), true);
        assertFalse(escaping.acceptable());
        assertEquals("LEAVES THE CHAMBER", escaping.verdict());
    }

    @Test void aPlacementThatAffectsNothingIsNotAcceptableEither() {
        var empty = new ChamberSession.Pending("outpost", List.of(), List.of(), false);
        assertFalse(empty.acceptable());
        assertEquals("NOTHING TO PLACE", empty.verdict());
    }

    @Test void theVerdictLeadsWithTheWorstThing() {
        // A tester reads one line. Escaping the chamber matters more than a
        // fault count, and a fault count matters more than a block count.
        var both = new ChamberSession.Pending("route",
                List.of(new Chamber.At(WORLD, 0, 60, 0)), List.of("a fault"), true);
        assertEquals("LEAVES THE CHAMBER", both.verdict());
        assertEquals("1 FAULT", new ChamberSession.Pending("r",
                List.of(new Chamber.At(WORLD, 0, 60, 0)), List.of("a fault"), false).verdict());
        assertEquals("1 BLOCKS", good().verdict());
    }

    @Test void offeringAgainReplacesWhatWasPending() {
        var s = new ChamberSession();
        s.offer(good(), null);
        s.offer(new ChamberSession.Pending("other", List.of(new Chamber.At(WORLD, 9, 60, 9)),
                List.of(), false), null);
        assertEquals("other", s.pending().description());
    }
}
