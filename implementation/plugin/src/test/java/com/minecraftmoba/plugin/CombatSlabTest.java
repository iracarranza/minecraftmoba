package com.minecraftmoba.plugin;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import static com.minecraftmoba.plugin.CombatSlab.*;
import static org.junit.jupiter.api.Assertions.*;

/** The venue's geometry, checked without a server. */
class CombatSlabTest {

    @Test void theBandsAreContiguousAndDoNotOverlap() {
        for (int i = 1; i < BANDS.size(); i++)
            assertEquals(BANDS.get(i - 1).maxX() + 1, BANDS.get(i).minX(), "gap or overlap before " + BANDS.get(i).name());
        for (var b : BANDS) assertEquals(BAND_WIDTH, b.maxX() - b.minX() + 1, b.name());
    }

    @Test void everyColumnBelongsToExactlyOneBand() {
        for (int x = minX(); x <= maxX(); x++) assertNotNull(bandAt(x), "x=" + x);
        assertNull(bandAt(minX() - 1));
        assertNull(bandAt(maxX() + 1));
    }

    @Test void theFirstBandIsAPlainFlatStoneControl() {
        var control = BANDS.get(0);
        assertEquals("control", control.name());
        assertEquals(Material.STONE, control.surface());
        assertEquals(Feature.FLAT, control.feature());
    }

    @Test void theTesterAndTheDummyStandOnTheControlBand() {
        double[] t = testerSpawn(), d = dummySpawn(8);
        assertEquals("control", bandAt((int) Math.floor(t[0])).name());
        assertEquals("control", bandAt((int) Math.floor(d[0])).name());
        assertEquals(Material.STONE, materialAt((int) Math.floor(t[0]), (int) t[1] - 1, (int) Math.floor(t[2])), "standing on stone");
        assertEquals(Material.AIR, materialAt((int) Math.floor(t[0]), (int) t[1], (int) Math.floor(t[2])), "with room to stand");
        assertEquals(Material.AIR, materialAt((int) Math.floor(t[0]), (int) t[1] + 1, (int) Math.floor(t[2])), "and headroom");
        assertEquals(8.0, d[2] - t[2], 1e-9);
    }

    @Test void aDummyTooCloseOrTooFarIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> dummySpawn(1));
        assertThrows(IllegalArgumentException.class, () -> dummySpawn(200));
    }

    @Test void theGroundIsDeepEnoughToDigAtEveryBand() {
        for (var b : BANDS) {
            int x = (b.minX() + b.maxX()) / 2, z = 0;
            int top = surfaceY(x, z);
            for (int y = bottomY(); y <= top; y++)
                assertNotEquals(Material.AIR, materialAt(x, y, z), b.name() + " y=" + y);
            assertTrue(top - bottomY() >= DEPTH - 1, b.name() + " must be at least " + (DEPTH - 1) + " deep");
        }
    }

    @Test void nothingExistsAboveTheSurfaceOrOutsideTheSlab() {
        assertEquals(Material.AIR, materialAt(0, surfaceY(0, 0) + 1, 0));
        assertEquals(Material.AIR, materialAt(minX() - 1, FLOOR_Y, 0));
        assertEquals(Material.AIR, materialAt(0, FLOOR_Y, MAX_Z + 1));
        assertEquals(Material.AIR, materialAt(0, bottomY() - 1, 0));
        assertEquals(Integer.MIN_VALUE, surfaceY(minX() - 1, 0));
    }

    @Test void thePoolIsWaterToItsDepthOverAStoneBasin() {
        var pool = band("water");
        int x = (pool.minX() + pool.maxX()) / 2;
        for (int d = 0; d < POOL_DEPTH; d++)
            assertEquals(Material.WATER, materialAt(x, FLOOR_Y - d, 0), "depth " + d);
        assertEquals(Material.STONE_BRICKS, materialAt(x, FLOOR_Y - POOL_DEPTH, 0), "a basin so it holds");
        assertEquals(Material.AIR, materialAt(x, FLOOR_Y + 1, 0));
    }

    @Test void theRampRisesAlongZ() {
        var ramp = band("ramp");
        int x = ramp.minX() + 1;
        int low = surfaceY(x, MIN_Z), high = surfaceY(x, MAX_Z);
        assertTrue(high > low, "a slope must slope");
        assertEquals(FLOOR_Y + 1, low);
        for (int z = MIN_Z + 1; z <= MAX_Z; z++)
            assertTrue(surfaceY(x, z) - surfaceY(x, z - 1) <= 1, "no step taller than one block at z=" + z);
    }

    // Material#isBlock needs the server's registry and cannot run here; whether each band's
    // material is a placeable block is established by building the slab on a live server.
    @Test void noBandIsMadeOfAir() {
        for (var b : BANDS) {
            assertNotEquals(Material.AIR, b.surface(), b.name());
            assertNotEquals(Material.AIR, b.fill(), b.name());
        }
    }

    @Test void standingPositionsForEachBandAreInsideTheSlabAndClear() {
        for (var b : BANDS) {
            double[] at = bandCentre(b.name());
            int x = (int) Math.floor(at[0]), z = (int) Math.floor(at[2]);
            assertTrue(contains(x, z), b.name());
            assertEquals(b.name(), bandAt(x).name());
            assertEquals(Material.AIR, materialAt(x, (int) at[1] + 1, z), "headroom on " + b.name());
        }
    }

    @Test void theWorldBorderCoversTheSlabWithMargin() {
        assertTrue(borderSize() >= maxX() - minX() + 1);
        assertTrue(borderSize() >= MAX_Z - MIN_Z + 1);
    }
}
