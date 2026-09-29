package com.minecraftmoba.plugin;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The reservation rule from classes.md section 27, pinned.
 *
 * The failure this guards against is not a lost block. It is a position that
 * stays reserved against a block nobody is going to put back -- a hole in the
 * map that nothing can build on, outliving the ability, with nothing in the
 * world to explain it.
 */
class DisplacementLedgerTest {

    private static final UUID WORLD = UUID.randomUUID();
    private final UUID shoes = UUID.randomUUID(), sinkhole = UUID.randomUUID();

    private static DisplacementLedger.Key at(int x, int y, int z) {
        return new DisplacementLedger.Key(WORLD, x, y, z);
    }

    @Test void aPositionMayBeReservedOnce() {
        var l = new DisplacementLedger<String>();
        assertTrue(l.reserve(at(0, 64, 0), shoes, "stone"));
        assertFalse(l.reserve(at(0, 64, 0), sinkhole, "dirt"),
                "a second ability cannot borrow a block the first already took");
        assertFalse(l.reserve(at(0, 64, 0), shoes, "dirt"),
                "not even the same token, which would lose the first payload");
        assertEquals("stone", l.release(at(0, 64, 0)), "the FIRST payload survived");
    }

    @Test void positionsAreDistinctByEveryCoordinateAndByWorld() {
        var l = new DisplacementLedger<String>();
        l.reserve(at(1, 64, 1), shoes, "a");
        assertFalse(l.isReserved(at(2, 64, 1)));
        assertFalse(l.isReserved(at(1, 65, 1)));
        assertFalse(l.isReserved(at(1, 64, 2)));
        assertFalse(l.isReserved(new DisplacementLedger.Key(UUID.randomUUID(), 1, 64, 1)),
                "the same coordinates in another world are another position");
    }

    @Test void releasingATokenReturnsOnlyItsOwnInTakenOrder() {
        var l = new DisplacementLedger<String>();
        l.reserve(at(0, 64, 0), shoes, "first");
        l.reserve(at(0, 65, 0), shoes, "second");
        l.reserve(at(9, 64, 9), sinkhole, "theirs");

        List<Map.Entry<DisplacementLedger.Key, String>> back = l.release(shoes);
        assertEquals(List.of("first", "second"), back.stream().map(Map.Entry::getValue).toList());
        assertFalse(l.isReserved(at(0, 64, 0)));
        assertTrue(l.isReserved(at(9, 64, 9)), "another ability's reservations are untouched");
        assertEquals(1, l.size());
    }

    @Test void releaseIsCompleteSoNothingStaysHeldAgainstABlockNobodyWillReturn() {
        var l = new DisplacementLedger<String>();
        l.reserve(at(0, 64, 0), shoes, "a");
        l.reserve(at(0, 65, 0), shoes, "b");
        l.release(shoes);

        assertEquals(0, l.heldBy(shoes));
        assertTrue(l.isEmpty(), "the whole point: no orphan reservation survives a release");
        assertTrue(l.release(shoes).isEmpty(), "releasing twice is harmless");
    }

    @Test void releasingOneLeavesTheRestOfTheTokenHeld() {
        var l = new DisplacementLedger<String>();
        l.reserve(at(0, 64, 0), shoes, "a");
        l.reserve(at(0, 65, 0), shoes, "b");

        assertEquals("a", l.release(at(0, 64, 0)));
        assertEquals(1, l.heldBy(shoes));
        assertTrue(l.isReserved(at(0, 65, 0)));
        assertNull(l.release(at(0, 64, 0)), "releasing an unheld position reports nothing");
    }

    @Test void releaseAllSweepsEveryToken() {
        var l = new DisplacementLedger<String>();
        l.reserve(at(0, 64, 0), shoes, "a");
        l.reserve(at(9, 64, 9), sinkhole, "b");

        assertEquals(2, l.releaseAll().size());
        assertTrue(l.isEmpty());
        assertEquals(0, l.heldBy(shoes));
        assertEquals(0, l.heldBy(sinkhole));
    }

    @Test void theEmptyCheckIsTheFastPathTheGuardsRelyOn() {
        var l = new DisplacementLedger<String>();
        assertTrue(l.isEmpty(), "no displacement means every event guard exits immediately");
        l.reserve(at(0, 64, 0), shoes, "a");
        assertFalse(l.isEmpty());
        l.release(shoes);
        assertTrue(l.isEmpty(), "and returns to the fast path once the cast is over");
    }

    @Test void holderNamesWhoIsOwedTheBlock() {
        var l = new DisplacementLedger<String>();
        l.reserve(at(0, 64, 0), shoes, "a");
        assertEquals(shoes, l.holderOf(at(0, 64, 0)));
        assertNull(l.holderOf(at(1, 64, 0)));
    }

    @Test void nullsAreRefusedRatherThanStored() {
        var l = new DisplacementLedger<String>();
        assertFalse(l.reserve(null, shoes, "a"));
        assertFalse(l.reserve(at(0, 64, 0), null, "a"));
        assertTrue(l.isEmpty());
        assertFalse(l.isReserved(null));
    }
}
