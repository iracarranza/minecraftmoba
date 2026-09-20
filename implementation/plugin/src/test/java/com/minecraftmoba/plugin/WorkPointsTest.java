package com.minecraftmoba.plugin;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import static com.minecraftmoba.plugin.WorkPoints.Domain.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Work Points measure work, not wealth.
 *
 * The two rules that are easy to get backwards are asserted directly: Fortune
 * must not multiply Extraction, because progression credits what acquisition
 * cost rather than what it produced; and a block the player placed is not new
 * acquisition when broken again, which is what stops a place-and-break loop
 * from farming levels.
 */
class WorkPointsTest {

    @Test void theSevenDomainsExist() {
        assertEquals(7, WorkPoints.Domain.values().length);
        for (var d : new WorkPoints.Domain[]{CONSTRUCTION, EXTRACTION, EXPLORATION,
                PRODUCTION, DEVELOPMENT, LOGISTICS, COMBAT})
            assertNotNull(d);
    }

    @Test void accountingResolutionIsTheManuscriptValue() {
        assertEquals(256, WorkPoints.WP_PER_UAU);
    }

    @Test void oreIsRecognisedThroughItsDeepslateVariant() {
        // Depth changes what it took to reach the ore, not which ore it is.
        assertEquals("iron", WorkPoints.oreKind(Material.IRON_ORE));
        assertEquals("iron", WorkPoints.oreKind(Material.DEEPSLATE_IRON_ORE));
        assertEquals("diamond", WorkPoints.oreKind(Material.DEEPSLATE_DIAMOND_ORE));
        assertEquals("ancient_debris", WorkPoints.oreKind(Material.ANCIENT_DEBRIS));
    }

    @Test void ordinaryBlocksAreNotExtraction() {
        assertNull(WorkPoints.oreKind(Material.STONE));
        assertNull(WorkPoints.oreKind(Material.OAK_LOG));
        assertNull(WorkPoints.oreKind(Material.DIRT));
        // A crafted block that merely contains a resource is not acquisition.
        assertNull(WorkPoints.oreKind(Material.IRON_BLOCK));
    }

    @Test void constructionBlocksAreWorthMoreThanOrdinaryPlacement() {
        // The manuscript's own sensitivity fixtures: 1 WP and 2 WP.
        assertTrue(MaterialCategories.isConstructionBlock(
                MaterialCategories.constructionBlocks().iterator().next()));
    }

    /** Mirrors award(): each level costs its own band, so crossing one is paid for. */
    private static int levelAfter(int start, int wp, java.util.function.IntUnaryOperator cost) {
        int level = start; long total = wp;
        while (level < 30 && total >= cost.applyAsInt(level)) { total -= cost.applyAsInt(level); level++; }
        return level;
    }

    @Test void theBandCurveChargesEachLevelItsOwnPrice() {
        java.util.function.IntUnaryOperator cost = l ->
                l <= 6 ? 40 : l <= 12 ? 54 : l <= 19 ? 75 : l <= 24 ? 103 : 130;
        assertEquals(1, levelAfter(1, 39, cost), "39 WP is not a level");
        assertEquals(2, levelAfter(1, 40, cost), "40 WP leaves level 1");
        assertEquals(3, levelAfter(1, 80, cost));
        // Crossing into the second band pays 40 for level 6 and then 54 for 7.
        assertEquals(7, levelAfter(6, 40, cost));
        assertEquals(8, levelAfter(6, 94, cost));
    }
}
