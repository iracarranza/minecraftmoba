package com.minecraftmoba.plugin;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class HotbarCustodyTest {
    @Test void repeatedSeatingCannotOverwriteRealItems() {
        var c = new HotbarCustody<String>(); var id = UUID.randomUUID();
        var original = new ArrayList<>(Arrays.asList("sword", null, "bow", null, null, null, null, null, "food"));
        assertTrue(c.hold(id, original));
        original.set(0, "changed outside custody");
        assertFalse(c.hold(id, Collections.nCopies(9, "serving")));
        assertEquals("sword", c.release(id).getFirst());
    }
    @Test void deathDisconnectAndResetMayRaceButOnlyOneGetsTheItems() {
        var c = new HotbarCustody<String>(); var id = UUID.randomUUID();
        assertTrue(c.hold(id, Collections.nCopies(9, "real")));
        assertEquals(9, c.release(id).size()); // death
        assertNull(c.release(id)); // disconnect
        assertNull(c.release(id)); // reset/disable
        assertFalse(c.contains(id));
    }
    @Test void emptySlotsAndOtherPlayersArePreserved() {
        var c = new HotbarCustody<String>(); var a = UUID.randomUUID(); var b = UUID.randomUUID();
        c.hold(a, Collections.nCopies(9, null)); c.hold(b, Collections.nCopies(9, "b"));
        assertEquals(Collections.nCopies(9, null), c.release(a));
        assertTrue(c.contains(b));
        assertEquals(Collections.nCopies(9, "b"), c.release(b));
    }
}
