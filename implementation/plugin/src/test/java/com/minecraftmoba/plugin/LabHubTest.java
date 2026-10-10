package com.minecraftmoba.plugin;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashSet;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

class LabHubTest {
    @Test void everyBenchHasADistinctPedestalBlockAndId() {
        var ids = new HashSet<String>(); var blocks = new HashSet<Material>();
        for (var b : LabHub.BENCHES) { assertTrue(ids.add(b.id())); assertTrue(blocks.add(b.pedestal())); }
        for (Material taken : new Material[]{Material.BOOKSHELF, Material.CARTOGRAPHY_TABLE, Material.EMERALD_BLOCK})
            assertFalse(blocks.contains(taken), taken + " already means something in the room");
    }

    @Test void pedestalsAreInsideTheHallAndDoNotCollide() {
        var seen = new HashSet<String>();
        for (var e : LabHub.EXISTING_PEDESTALS) seen.add(e[0] + "," + e[1]);
        for (var b : LabHub.BENCHES) {
            assertTrue(Math.abs(b.x()) <= 8 && Math.abs(b.z()) <= 8, b.id() + " is outside the hall");
            assertTrue(seen.add(b.x() + "," + b.z()), b.id() + " collides");
        }
    }

    @Test void pedestalsAreFarEnoughApartToClickAndReadLabels() {
        for (var a : LabHub.BENCHES) for (var b : LabHub.BENCHES)
            if (a != b) assertTrue(Math.hypot(a.x() - b.x(), a.z() - b.z()) >= 3, a.id() + " / " + b.id());
        for (var b : LabHub.BENCHES) for (var e : LabHub.EXISTING_PEDESTALS)
            assertTrue(Math.hypot(b.x() - e[0], b.z() - e[1]) >= 3, b.id());
    }

    @Test void onlyTheBenchesThatDrawOnAScoopNeedOneAndSaySo() {
        for (var b : LabHub.BENCHES) {
            boolean needs = b.id().equals("terrain") || b.id().equals("overlay");
            if (needs) {
                assertNotNull(LabHub.refusal(b, new LabHub.State(false)), b.id());
                assertTrue(LabHub.refusal(b, new LabHub.State(false)).contains("launched scoop"));
                assertNull(LabHub.refusal(b, new LabHub.State(true)));
            } else assertNull(LabHub.refusal(b, new LabHub.State(false)), b.id());
        }
    }

    @Test void lookupsByIdAndByBlockAgree() {
        for (var b : LabHub.BENCHES) { assertSame(b, LabHub.bench(b.id())); assertSame(b, LabHub.atBlock(b.pedestal())); }
        assertNull(LabHub.bench("nonsense")); assertNull(LabHub.atBlock(Material.DIRT));
    }

    @Test void everyBenchHasADescription() {
        for (var b : LabHub.BENCHES) assertFalse(b.blurb().isBlank(), b.id());
    }
}
