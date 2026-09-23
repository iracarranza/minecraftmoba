package com.minecraftmoba.plugin;

import org.bukkit.Material;

import java.util.HashSet;
import java.util.Set;

/**
 * Which transformations count as Production work.
 *
 * Production recognizes a **completed transformation into a strategically
 * useful output**, which is not the same claim as "a CraftItemEvent occurred".
 * A recipe firing tells you an inventory operation happened; it does not tell
 * you anything was produced that the team can use. So membership here is
 * enumerated per established category -- tools, equipment/weapons, utility
 * items, food/consumables, strategic inputs, Construction Blocks -- and
 * anything not enumerated is UNRESOLVED and earns nothing.
 *
 * That default is deliberate. Leaving planks, sticks and the long tail of
 * shapeless recipes unresolved understates Production; inventing a rule for
 * them would misstate it, and the directive is explicit that an ambiguous
 * recipe stays unresolved rather than acquiring a guessed classification.
 *
 * <h2>Why compression cannot loop</h2>
 *
 * Nine ingots make a block and a block makes nine ingots back. If both
 * directions paid, a single iron ingot would be an unbounded WP generator for
 * as long as a player kept clicking. The cycle is broken by classifying the
 * *result*, not the recipe:
 *
 *   - storage blocks are excluded outright;
 *   - the unit items recoverable by uncrafting one -- ingots, gems, coal,
 *     redstone, lapis, wheat, and so on -- earn nothing **when crafted**.
 *
 * Ingots still pay as strategic inputs when they come out of a furnace,
 * because smelting raw ore is irreversible: the raw ore is gone. The direction
 * that could loop is exactly the direction that pays nothing, so the cycle
 * nets zero rather than being rate-limited or given a cooldown.
 *
 * Every other enumerated output is uncraftable -- a chest does not become
 * planks again -- so each craft permanently consumes its inputs and is bounded
 * by the extraction or renewable work that supplied them.
 */
public final class ProductionRecipes {

    /** Established Production categories. UNRESOLVED earns nothing. */
    public enum Output { TOOL, EQUIPMENT, UTILITY, CONSUMABLE, STRATEGIC_INPUT, CONSTRUCTION_BLOCK, UNRESOLVED }

    private static final Set<Material> UTILITY = Set.of(
            Material.CRAFTING_TABLE, Material.FURNACE, Material.BLAST_FURNACE, Material.SMOKER,
            Material.CHEST, Material.BARREL, Material.TORCH, Material.SOUL_TORCH, Material.LANTERN,
            Material.LADDER, Material.SCAFFOLDING, Material.CAMPFIRE, Material.ANVIL,
            Material.ENCHANTING_TABLE, Material.CAULDRON, Material.HOPPER, Material.RAIL,
            Material.POWERED_RAIL, Material.DETECTOR_RAIL, Material.MINECART, Material.SMITHING_TABLE,
            Material.STONECUTTER, Material.GRINDSTONE, Material.LOOM, Material.CARTOGRAPHY_TABLE,
            Material.COMPOSTER, Material.BREWING_STAND, Material.BEACON, Material.SHULKER_BOX,
            Material.MAP, Material.COMPASS, Material.CLOCK, Material.SHEARS, Material.FLINT_AND_STEEL,
            Material.BUCKET, Material.FISHING_ROD);

    private static final Set<Material> CONSUMABLE = Set.of(
            Material.BREAD, Material.BAKED_POTATO, Material.MUSHROOM_STEW, Material.RABBIT_STEW,
            Material.BEETROOT_SOUP, Material.SUSPICIOUS_STEW, Material.GOLDEN_APPLE,
            Material.ENCHANTED_GOLDEN_APPLE, Material.GOLDEN_CARROT, Material.CAKE,
            Material.COOKIE, Material.PUMPKIN_PIE, Material.DRIED_KELP);

    /**
     * Irreversible refinements. These pay when SMELTED; see craftedOutput() for
     * why the same materials pay nothing when they come off a crafting grid.
     */
    private static final Set<Material> STRATEGIC_INPUT = Set.of(
            Material.IRON_INGOT, Material.GOLD_INGOT, Material.COPPER_INGOT,
            Material.NETHERITE_SCRAP, Material.CHARCOAL, Material.BRICK, Material.NETHER_BRICK);

    /** Uncraftable from a storage block, so crafting one is a real refinement. */
    private static final Set<Material> CRAFTED_STRATEGIC_INPUT = Set.of(Material.NETHERITE_INGOT);

    /** Recoverable by uncrafting a storage block: the reversible half of the cycle. */
    private static final Set<Material> COMPRESSION_UNITS = Set.of(
            Material.IRON_INGOT, Material.GOLD_INGOT, Material.COPPER_INGOT, Material.NETHERITE_INGOT,
            Material.DIAMOND, Material.EMERALD, Material.COAL, Material.REDSTONE, Material.LAPIS_LAZULI,
            Material.RAW_IRON, Material.RAW_GOLD, Material.RAW_COPPER, Material.WHEAT,
            Material.SLIME_BALL, Material.HONEYCOMB, Material.BONE_MEAL, Material.AMETHYST_SHARD,
            Material.RESIN_BRICK, Material.DRIED_KELP, Material.NETHERITE_SCRAP);

    private static final Set<Material> STORAGE_BLOCKS = buildStorageBlocks();

    private static Set<Material> buildStorageBlocks() {
        var set = new HashSet<Material>();
        for (Material m : Material.values()) {
            String n = m.name();
            if (n.endsWith("_BLOCK") && !n.endsWith("SHULKER_BOX")) set.add(m);
            if (n.equals("HAY_BLOCK") || n.equals("DRIED_KELP_BLOCK") || n.equals("SLIME_BLOCK")
                    || n.equals("HONEY_BLOCK") || n.equals("BONE_BLOCK")) set.add(m);
        }
        return Set.copyOf(set);
    }

    private static boolean suffix(Material m, String... endings) {
        for (String e : endings) if (m.name().endsWith(e)) return true;
        return false;
    }

    /** Category by result material alone, ignoring how it was produced. */
    static Output categorize(Material m) {
        if (m == null) return Output.UNRESOLVED;
        if (suffix(m, "_PICKAXE", "_AXE", "_SHOVEL", "_HOE")) return Output.TOOL;
        if (suffix(m, "_SWORD", "_HELMET", "_CHESTPLATE", "_LEGGINGS", "_BOOTS")
                || m == Material.BOW || m == Material.CROSSBOW || m == Material.SHIELD
                || m == Material.ARROW) return Output.EQUIPMENT;
        if (UTILITY.contains(m)) return Output.UTILITY;
        if (CONSUMABLE.contains(m) || m.name().startsWith("COOKED_")) return Output.CONSUMABLE;
        if (MaterialCategories.isConstructionBlock(m)) return Output.CONSTRUCTION_BLOCK;
        if (STRATEGIC_INPUT.contains(m)) return Output.STRATEGIC_INPUT;
        return Output.UNRESOLVED;
    }

    /**
     * What a crafting-grid result is worth.
     *
     * The compression exclusions are applied here and nowhere else, because
     * crafting is the only direction that can run backwards.
     */
    public static Output craftedOutput(Material m) {
        if (m == null) return Output.UNRESOLVED;
        if (STORAGE_BLOCKS.contains(m) && !MaterialCategories.isConstructionBlock(m)) return Output.UNRESOLVED;
        if (CRAFTED_STRATEGIC_INPUT.contains(m)) return Output.STRATEGIC_INPUT;
        if (COMPRESSION_UNITS.contains(m)) return Output.UNRESOLVED;
        Output out = categorize(m);
        // A furnace-only refinement reached by some other grid recipe is not
        // the refinement this credits, so it stays unresolved.
        return out == Output.STRATEGIC_INPUT ? Output.UNRESOLVED : out;
    }

    /**
     * What a smelted result is worth. Smelting consumes its input irreversibly
     * and costs fuel, so no exclusion is needed here.
     */
    public static Output smeltedOutput(Material m) { return categorize(m); }

    private ProductionRecipes() {}
}
