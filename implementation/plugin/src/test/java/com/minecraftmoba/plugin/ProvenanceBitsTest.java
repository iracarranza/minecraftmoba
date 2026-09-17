package com.minecraftmoba.plugin;
import org.junit.jupiter.api.Test;
import java.util.HashSet;
import static org.junit.jupiter.api.Assertions.*;
class ProvenanceBitsTest {
    @Test void fullHeightCoordinatesDoNotAlias() {
        var seen = new HashSet<Integer>();
        for (int y = -64; y < 320; y++) for (int x = -16; x < 0; x++) for (int z = -16; z < 0; z++)
            assertTrue(seen.add(ProvenanceBits.index(x,y,z,-64,320)));
        assertEquals(16*16*384, seen.size());
        assertThrows(IllegalArgumentException.class, () -> ProvenanceBits.index(0,320,0,-64,320));
    }
    @Test void breakReclaimsPayloadAndRepeatCyclesDoNotGrow() {
        byte[] bytes = null;
        for (int cycle = 0; cycle < 100; cycle++) {
            bytes = ProvenanceBits.change(bytes, 98303, true);
            bytes = ProvenanceBits.change(bytes, 0, true);
            assertEquals(12288, bytes.length); assertTrue(ProvenanceBits.contains(bytes, 98303));
            bytes = ProvenanceBits.change(bytes, 98303, false);
            assertEquals(1, bytes.length); assertTrue(ProvenanceBits.contains(bytes, 0));
            bytes = ProvenanceBits.change(bytes, 0, false); assertEquals(0, bytes.length);
        }
    }
}
