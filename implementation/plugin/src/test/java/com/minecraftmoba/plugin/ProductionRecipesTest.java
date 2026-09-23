package com.minecraftmoba.plugin;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import static com.minecraftmoba.plugin.ProductionRecipes.Output.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Production recognizes completed transformations, not CraftItemEvents.
 *
 * The two things worth testing are that the enumerated categories are
 * recognized, and that the compression cycle cannot pay -- the second being the
 * one that would otherwise turn a single iron ingot into unbounded WP.
 */
class ProductionRecipesTest {

    @Test void toolsEquipmentUtilityAndFoodAreRecognized() {
        assertEquals(TOOL, ProductionRecipes.craftedOutput(Material.IRON_PICKAXE));
        assertEquals(TOOL, ProductionRecipes.craftedOutput(Material.WOODEN_SHOVEL));
        assertEquals(EQUIPMENT, ProductionRecipes.craftedOutput(Material.IRON_SWORD));
        assertEquals(EQUIPMENT, ProductionRecipes.craftedOutput(Material.SHIELD));
        assertEquals(EQUIPMENT, ProductionRecipes.craftedOutput(Material.DIAMOND_CHESTPLATE));
        assertEquals(UTILITY, ProductionRecipes.craftedOutput(Material.FURNACE));
        assertEquals(UTILITY, ProductionRecipes.craftedOutput(Material.CHEST));
        assertEquals(CONSUMABLE, ProductionRecipes.craftedOutput(Material.BREAD));
        assertEquals(CONSUMABLE, ProductionRecipes.smeltedOutput(Material.COOKED_BEEF));
        assertEquals(CONSTRUCTION_BLOCK, ProductionRecipes.craftedOutput(Material.BRICKS));
        assertEquals(CONSTRUCTION_BLOCK, ProductionRecipes.smeltedOutput(Material.GLASS));
    }

    @Test void smeltingRawOreIsAStrategicInput() {
        // Irreversible: the raw ore is consumed and the fuel is spent.
        assertEquals(STRATEGIC_INPUT, ProductionRecipes.smeltedOutput(Material.IRON_INGOT));
        assertEquals(STRATEGIC_INPUT, ProductionRecipes.smeltedOutput(Material.COPPER_INGOT));
        assertEquals(STRATEGIC_INPUT, ProductionRecipes.smeltedOutput(Material.NETHERITE_SCRAP));
    }

    @Test void theCompressionCycleNetsZero() {
        // 9 ingots -> block -> 9 ingots. If either direction paid, one ingot
        // would be an unbounded WP generator for as long as a player clicked.
        assertEquals(UNRESOLVED, ProductionRecipes.craftedOutput(Material.IRON_BLOCK),
                "compressing pays nothing");
        assertEquals(UNRESOLVED, ProductionRecipes.craftedOutput(Material.IRON_INGOT),
                "uncompressing pays nothing");
        for (Material unit : new Material[]{Material.GOLD_INGOT, Material.COPPER_INGOT,
                Material.DIAMOND, Material.EMERALD, Material.COAL, Material.REDSTONE,
                Material.LAPIS_LAZULI, Material.WHEAT, Material.RAW_IRON, Material.BONE_MEAL}) {
            assertEquals(UNRESOLVED, ProductionRecipes.craftedOutput(unit), unit + " crafted");
        }
        for (Material block : new Material[]{Material.GOLD_BLOCK, Material.DIAMOND_BLOCK,
                Material.COAL_BLOCK, Material.REDSTONE_BLOCK, Material.HAY_BLOCK,
                Material.SLIME_BLOCK, Material.RAW_COPPER_BLOCK}) {
            assertEquals(UNRESOLVED, ProductionRecipes.craftedOutput(block), block + " crafted");
        }
    }

    @Test void theIrreversibleDirectionStillPays() {
        // Smelting is not part of any cycle, so refinement is not collateral
        // damage from closing the loop.
        assertNotEquals(UNRESOLVED, ProductionRecipes.smeltedOutput(Material.IRON_INGOT));
        // Netherite ingots cannot be recovered by uncrafting a netherite block
        // into scrap and gold, so crafting one is a genuine refinement.
        assertEquals(STRATEGIC_INPUT, ProductionRecipes.craftedOutput(Material.NETHERITE_INGOT));
    }

    @Test void driedKelpPaysOnlyInTheDirectionThatConsumesSomething() {
        assertEquals(CONSUMABLE, ProductionRecipes.smeltedOutput(Material.DRIED_KELP));
        assertEquals(UNRESOLVED, ProductionRecipes.craftedOutput(Material.DRIED_KELP),
                "recovered from a dried kelp block");
    }

    @Test void ambiguousRecipesStayUnresolved() {
        // Understating Production is recoverable; inventing a classification
        // for the long tail of shapeless recipes is not.
        assertEquals(UNRESOLVED, ProductionRecipes.craftedOutput(Material.OAK_PLANKS));
        assertEquals(UNRESOLVED, ProductionRecipes.craftedOutput(Material.STICK));
        assertEquals(UNRESOLVED, ProductionRecipes.craftedOutput(Material.WHITE_WOOL));
        assertEquals(UNRESOLVED, ProductionRecipes.craftedOutput(Material.OAK_SLAB));
        assertEquals(UNRESOLVED, ProductionRecipes.craftedOutput(Material.STONE_BRICKS));
    }

    @Test void constructionBlockStatusStillDoesNotPropagateToDerivatives() {
        // The category is classes.md's and is untouched by this pass.
        assertEquals(CONSTRUCTION_BLOCK, ProductionRecipes.craftedOutput(Material.WHITE_STAINED_GLASS));
        assertEquals(UNRESOLVED, ProductionRecipes.craftedOutput(Material.WHITE_STAINED_GLASS_PANE));
    }
}
