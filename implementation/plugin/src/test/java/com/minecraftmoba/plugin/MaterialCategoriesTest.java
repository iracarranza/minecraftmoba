package com.minecraftmoba.plugin;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Membership is canon, so these assert canon rather than behaviour. */
class MaterialCategoriesTest {

    @Test void primaryMaterialsAreTheCanonFive_plusCopper() {
        for (Material m : new Material[]{Material.IRON_INGOT, Material.GOLD_INGOT, Material.DIAMOND,
                Material.NETHERITE_INGOT, Material.LEATHER, Material.COPPER_INGOT})
            assertTrue(MaterialCategories.isPrimary(m), m.name());
    }

    @Test void canonExclusionsAreNotPrimary() {
        // classes.md names these explicitly as outside the category.
        for (Material m : new Material[]{Material.OAK_PLANKS, Material.STONE, Material.REDSTONE,
                Material.LAPIS_LAZULI, Material.COAL, Material.EMERALD, Material.QUARTZ,
                Material.FLINT, Material.STRING})
            assertFalse(MaterialCategories.isPrimary(m), m.name());
    }

    @Test void constructionBlocksAreTheCanonFive() {
        for (Material m : new Material[]{Material.BRICKS, Material.MUD_BRICKS, Material.TERRACOTTA,
                Material.WHITE_CONCRETE, Material.GLASS})
            assertTrue(MaterialCategories.isConstructionBlock(m), m.name());
    }

    @Test void statusDoesNotPropagateToDerivatives() {
        // The rule canon states outright, and the one a prefix match gets wrong.
        assertTrue(MaterialCategories.isConstructionBlock(Material.GLASS));
        assertFalse(MaterialCategories.isConstructionBlock(Material.GLASS_PANE));
        assertTrue(MaterialCategories.isConstructionBlock(Material.BRICKS));
        assertFalse(MaterialCategories.isConstructionBlock(Material.BRICK_SLAB));
        assertFalse(MaterialCategories.isConstructionBlock(Material.BRICK_STAIRS));
        assertFalse(MaterialCategories.isConstructionBlock(Material.WHITE_CONCRETE_POWDER));
    }

    @Test void canonConstructionExclusionsAreOrdinary() {
        for (Material m : new Material[]{Material.OAK_PLANKS, Material.OAK_LOG, Material.COBBLESTONE,
                Material.STONE_BRICKS, Material.DEEPSLATE, Material.GRANITE, Material.DIORITE,
                Material.ANDESITE, Material.POLISHED_ANDESITE, Material.SANDSTONE, Material.IRON_BLOCK})
            assertFalse(MaterialCategories.isConstructionBlock(m), m.name());
    }

    @Test void glazedTerracottaIsNotAConstructionBlock() {
        assertTrue(MaterialCategories.isConstructionBlock(Material.RED_TERRACOTTA));
        assertFalse(MaterialCategories.isConstructionBlock(Material.RED_GLAZED_TERRACOTTA));
    }

    @Test void nullIsOrdinaryRatherThanAnError() {
        assertEquals(MaterialCategories.Category.ORDINARY, MaterialCategories.of(null));
    }
}
