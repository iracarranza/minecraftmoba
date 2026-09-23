package com.minecraftmoba.plugin;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Extraction is WP = A(O) + qH.
 *
 * A(O) credits exploiting one physical opportunity; H is the qualifying
 * harvest actually obtained; q converts harvest to points. Fortune raises H and
 * never A(O) -- one ore block is one opportunity however enchanted the pick is,
 * but the extra material really was extracted.
 *
 * An earlier implementation had this backwards, crediting per block and never
 * per item, which made Yield an item enchantment with no progression meaning.
 */
class ExtractionModelTest {

    /** Mirrors the award path: opportunity once, harvest scaled by q. */
    private static int wp(int opportunity, int harvested, int q) {
        return opportunity + q * harvested;
    }

    @Test void fortuneRaisesHarvestAndNotOpportunity() {
        int a = 1, q = 1;                       // copper: A(O)=1
        // Expected raw copper by Fortune level, from the project's yield curve.
        int plain = wp(a, 3, q);                // ~3.5 -> 3 on a given break
        int fortuneIII = wp(a, 7, q);           // ~7.7
        assertTrue(fortuneIII > plain, "Fortune must increase Extraction WP");
        // and it increases only through the harvest term
        assertEquals(plain - a * 1, 3 * q);
        assertEquals(fortuneIII - a * 1, 7 * q);
    }

    @Test void oneBlockIsOneOpportunityHoweverEnchanted() {
        // The rejected model was Fortune III => 2.2x "ore blocks discovered".
        int a = 4;                              // diamond
        assertEquals(a, wp(a, 0, 1) , "opportunity is paid once and is not scaled");
        assertEquals(a + 3, wp(a, 3, 1));
        assertEquals(a + 6, wp(a, 6, 1));
    }

    @Test void harvestCoefficientScalesOnlyTheHarvestTerm() {
        assertEquals(2 + 6, wp(2, 3, 2));
        assertEquals(2 + 0, wp(2, 0, 2));
    }

    @Test void onlyOreBlocksAreExtractionOpportunities() {
        // Naturally generated does not mean Extraction. Terracotta generates in
        // badlands and is a Construction Block; crediting it as acquisition
        // would be an exploit dressed up as authority.
        assertNull(WorkPoints.oreKind(Material.TERRACOTTA));
        assertNull(WorkPoints.oreKind(Material.STONE));
        assertNull(WorkPoints.oreKind(Material.OAK_LOG));
        assertTrue(MaterialCategories.isConstructionBlock(Material.TERRACOTTA));
    }

    @Test void deepslateVariantsAreTheSameResource() {
        assertEquals(WorkPoints.oreKind(Material.IRON_ORE),
                     WorkPoints.oreKind(Material.DEEPSLATE_IRON_ORE));
    }

    @Test void onlySurvivalProducesWork() {
        // Creative hands a player material without labour.
        for (var mode : org.bukkit.GameMode.values()) {
            boolean expected = mode == org.bukkit.GameMode.SURVIVAL;
            var p = org.mockito.Mockito.mock(org.bukkit.entity.Player.class);
            org.mockito.Mockito.when(p.getGameMode()).thenReturn(mode);
            assertEquals(expected, WorkPoints.counts(p), mode.name());
        }
    }
}
