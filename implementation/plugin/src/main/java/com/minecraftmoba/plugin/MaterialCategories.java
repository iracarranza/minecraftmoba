package com.minecraftmoba.plugin;

import org.bukkit.Material;
import java.util.*;

/**
 * The two restricted material categories, exactly as classes.md defines them.
 *
 * Membership is canon and enumerated, so this is a lookup rather than a design:
 *
 *   Primary Materials     Iron, Gold, Diamond, Netherite, Leather
 *   Construction Blocks   Bricks, Mud Bricks, Terracotta, Concrete, Glass
 *
 * Canon is equally explicit about what is NOT in each. Wood, Stone, Copper,
 * Redstone, Lapis, Coal, Emerald, Quartz, Flint and String are outside Primary
 * Materials; planks, logs, cobblestone, stone bricks, deepslate, granite,
 * diorite, andesite and their polished forms, sandstone, slabs, stairs, panes
 * and mineral storage blocks are outside Construction Blocks.
 *
 * **Status does not propagate to derivatives.** Glass qualifies and Glass Panes
 * do not; Bricks qualify and Brick Slabs do not. That rule is easy to get wrong
 * with a name-prefix match, so derivatives are excluded explicitly and a test
 * asserts it.
 *
 * Copper became a Primary Material on 19 September 2026, superseding the
 * 10 September passage that excluded it.
 *
 * What this does NOT decide, because canon marks it [OPEN]: the XP premium for
 * incorporating Construction Blocks, per-recipe Primary Material mapping,
 * derivative handling beyond the stated rule, and anti-farming interaction.
 */
public final class MaterialCategories {
    public enum Category { PRIMARY_MATERIAL, CONSTRUCTION_BLOCK, ORDINARY }

    private static final Set<Material> PRIMARY = Set.of(
            Material.IRON_INGOT, Material.IRON_BLOCK, Material.RAW_IRON, Material.RAW_IRON_BLOCK,
            Material.GOLD_INGOT, Material.GOLD_BLOCK, Material.RAW_GOLD, Material.RAW_GOLD_BLOCK,
            Material.DIAMOND, Material.DIAMOND_BLOCK,
            Material.NETHERITE_INGOT, Material.NETHERITE_BLOCK, Material.NETHERITE_SCRAP,
            Material.LEATHER,
            // 19 September 2026: Copper is a Primary Material.
            Material.COPPER_INGOT, Material.COPPER_BLOCK, Material.RAW_COPPER, Material.RAW_COPPER_BLOCK);

    private static final Set<Material> CONSTRUCTION = buildConstruction();

    /** Derivatives that share a name root with a member but are excluded by canon. */
    private static final Set<Material> DERIVATIVE_EXCLUSIONS = buildDerivativeExclusions();

    private static Set<Material> buildConstruction() {
        var set = new HashSet<Material>();
        set.add(Material.BRICKS);
        set.add(Material.MUD_BRICKS);
        set.add(Material.TERRACOTTA);
        set.add(Material.GLASS);
        for (Material m : Material.values()) {
            String n = m.name();
            if (n.endsWith("_TERRACOTTA") && !n.contains("GLAZED")) set.add(m);   // dyed terracotta
            if (n.endsWith("_CONCRETE")) set.add(m);                              // concrete, not powder
            if (n.endsWith("_STAINED_GLASS")) set.add(m);
        }
        return Set.copyOf(set);
    }

    private static Set<Material> buildDerivativeExclusions() {
        var set = new HashSet<Material>();
        for (Material m : Material.values()) {
            String n = m.name();
            boolean derivative = n.endsWith("_SLAB") || n.endsWith("_STAIRS") || n.endsWith("_WALL")
                    || n.endsWith("_PANE") || n.endsWith("_PANES") || n.endsWith("_CONCRETE_POWDER")
                    || n.endsWith("_GLAZED_TERRACOTTA");
            if (derivative) set.add(m);
        }
        return Set.copyOf(set);
    }

    public static Category of(Material m) {
        if (m == null) return Category.ORDINARY;
        if (DERIVATIVE_EXCLUSIONS.contains(m)) return Category.ORDINARY;
        if (PRIMARY.contains(m)) return Category.PRIMARY_MATERIAL;
        if (CONSTRUCTION.contains(m)) return Category.CONSTRUCTION_BLOCK;
        return Category.ORDINARY;
    }

    public static boolean isPrimary(Material m) { return of(m) == Category.PRIMARY_MATERIAL; }
    public static boolean isConstructionBlock(Material m) { return of(m) == Category.CONSTRUCTION_BLOCK; }

    public static Set<Material> primaryMaterials() { return PRIMARY; }
    public static Set<Material> constructionBlocks() { return CONSTRUCTION; }

    public static String report() {
        return "MATERIALS primary=" + PRIMARY.size()
                + " constructionBlocks=" + CONSTRUCTION.size()
                + " derivativeExclusions=" + DERIVATIVE_EXCLUSIONS.size()
                + " (membership is canon; XP premium and per-recipe mapping remain OPEN)";
    }

    private MaterialCategories() {}
}
