package com.minecraftmoba.plugin;

import org.bukkit.Material;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * The lab's item shelf: every item a tester might need, browsable by category or findable by name.
 *
 * A class's abilities consume ordinary items (sand for the Paver's Mixer, a pickaxe for the Quarryman, food for the
 * Chef), and nobody should have to hand-author a kit per class or leave the lab to fetch them. The loadout editor
 * opens this; a click puts the item in the loadout being edited. Pure: the categories, the matching and the paging are
 * tests, and the server supplies the list of items and what it knows about them.
 *
 * The categories are for finding things, not definitions: a Golden Apple is Food, a Piston is Redstone, a Crossbow is
 * Combat. An item lives in the FIRST category that claims it, in the order of {@link Category}.
 */
public final class ItemCatalogue {
    private ItemCatalogue() {}

    public enum Category {
        ALL("All items", Material.CHEST),
        COMBAT("Combat", Material.IRON_SWORD),
        ARMOR("Armor", Material.IRON_CHESTPLATE),
        TOOLS("Tools", Material.IRON_PICKAXE),
        FOOD("Food", Material.COOKED_BEEF),
        REDSTONE("Redstone and machines", Material.REDSTONE),
        PLANTS("Plants", Material.WHEAT_SEEDS),
        MATERIALS("Materials", Material.IRON_INGOT),
        BLOCKS("Blocks", Material.STONE),
        MISC("Everything else", Material.BARREL);

        private final String label;
        private final Material icon;
        Category(String label, Material icon) { this.label = label; this.icon = icon; }
        public String label() { return label; }
        public Material icon() { return icon; }
        public Category next() { var v = values(); return v[(ordinal() + 1) % v.length]; }
    }

    /** What the server knows about an item that its name does not say. */
    public record Facts(boolean edible, boolean block) {}

    private static boolean has(String n, String... parts) { for (String p : parts) if (n.contains(p)) return true; return false; }
    private static boolean ends(String n, String... parts) { for (String p : parts) if (n.endsWith(p)) return true; return false; }

    /** The category an item lives in: the first that claims it. Never ALL. */
    public static Category categorize(String name, Facts facts) {
        String n = name.toUpperCase(Locale.ROOT);
        if (ends(n, "_HELMET", "_CHESTPLATE", "_LEGGINGS", "_BOOTS", "_HORSE_ARMOR") || n.equals("ELYTRA") || n.equals("WOLF_ARMOR")) return Category.ARMOR;
        if (ends(n, "_SWORD", "_SPEAR") || n.equals("BOW") || n.equals("CROSSBOW") || n.equals("TRIDENT") || n.equals("MACE") || n.equals("SHIELD")
                || ends(n, "ARROW") || n.equals("FIREWORK_ROCKET") || n.equals("END_CRYSTAL") || n.equals("WIND_CHARGE") || n.equals("FIRE_CHARGE")
                || n.equals("SNOWBALL") || n.equals("EGG")) return Category.COMBAT;
        if (ends(n, "_PICKAXE", "_AXE", "_SHOVEL", "_HOE", "_BUCKET") || has(n, "FLINT_AND_STEEL", "SHEARS", "FISHING_ROD", "SPYGLASS", "BRUSH")
                || n.equals("COMPASS") || n.equals("CLOCK") || n.equals("LEAD") || n.equals("NAME_TAG") || n.equals("BUCKET")) return Category.TOOLS;
        if (facts.edible()) return Category.FOOD;
        if (has(n, "REDSTONE", "PISTON", "OBSERVER", "HOPPER", "DROPPER", "DISPENSER", "REPEATER", "COMPARATOR", "TRIPWIRE", "DETECTOR")
                || n.equals("LEVER") || ends(n, "_BUTTON", "_PRESSURE_PLATE") || n.equals("TNT") || n.equals("TARGET") || n.equals("NOTE_BLOCK")
                || n.equals("SLIME_BLOCK") || n.equals("HONEY_BLOCK") || has(n, "RAIL") || ends(n, "MINECART") || ends(n, "_BOAT", "_RAFT")) return Category.REDSTONE;
        if (ends(n, "_SAPLING", "_SEEDS", "_LEAVES", "_MUSHROOM", "_FUNGUS", "_ROOTS", "_VINES") || has(n, "FLOWER", "TULIP", "ORCHID", "ALLIUM", "DANDELION", "POPPY",
                "CORNFLOWER", "LILY", "ROSE_BUSH", "PEONY", "LILAC", "AZALEA", "FERN", "SHORT_GRASS", "TALL_GRASS", "VINE", "MOSS", "BAMBOO", "SUGAR_CANE", "CACTUS",
                "KELP", "WHEAT", "PUMPKIN", "MELON", "SWEET_BERRIES", "GLOW_BERRIES", "PITCHER", "SEAGRASS", "DRIPLEAF", "MANGROVE_PROPAGULE", "CHORUS_FLOWER")) return Category.PLANTS;
        if (ends(n, "_INGOT", "_NUGGET", "_DYE", "_SHARD", "_SCRAP", "_DUST") || n.startsWith("RAW_") || has(n, "DIAMOND", "EMERALD", "COAL", "CHARCOAL", "QUARTZ", "AMETHYST", "LAPIS")
                || in(n, "STICK", "STRING", "FEATHER", "LEATHER", "BONE", "SLIME_BALL", "GUNPOWDER", "CLAY_BALL", "BRICK", "PAPER", "BOOK", "BLAZE_ROD", "BLAZE_POWDER",
                "ENDER_PEARL", "ENDER_EYE", "FLINT", "GHAST_TEAR", "PHANTOM_MEMBRANE", "NAUTILUS_SHELL", "HEART_OF_THE_SEA", "PRISMARINE_CRYSTALS", "ECHO_SHARD", "NETHER_STAR", "SUGAR", "HONEYCOMB", "INK_SAC", "GLOW_INK_SAC"))
            return Category.MATERIALS;
        if (facts.block()) return Category.BLOCKS;
        return Category.MISC;
    }

    private static boolean in(String n, String... names) { for (String s : names) if (n.equals(s)) return true; return false; }

    /** Pure: an item matches a search when every word of the query appears in its name; spaces count as underscores. Blank matches all. */
    public static boolean matches(String name, String query) {
        if (query == null || query.isBlank()) return true;
        String n = name.toUpperCase(Locale.ROOT);
        for (String word : query.trim().toUpperCase(Locale.ROOT).split("[\\s_]+")) if (!word.isEmpty() && !n.contains(word)) return false;
        return true;
    }

    /** A shelf entry: the item and the category it lives in. */
    public record Entry(Material material, Category category) {}

    /** The items in a category matching a search, alphabetical by name. */
    public static List<Material> shelf(List<Entry> all, Category category, String query) {
        var out = new ArrayList<Material>();
        for (Entry e : all)
            if ((category == Category.ALL || e.category() == category) && matches(e.material().name(), query)) out.add(e.material());
        out.sort(Comparator.comparing(Material::name));
        return out;
    }

    /** How many pages a shelf of {@code count} items takes. Always at least one, so an empty shelf still has a page 0. */
    public static int pages(int count, int pageSize) { return Math.max(1, (count + pageSize - 1) / pageSize); }

    /** One page of a shelf. A page past the end is clamped to the last. */
    public static <T> List<T> page(List<T> shelf, int page, int pageSize) {
        int p = Math.max(0, Math.min(page, pages(shelf.size(), pageSize) - 1));
        int from = Math.min(shelf.size(), p * pageSize);
        return shelf.subList(from, Math.min(shelf.size(), from + pageSize));
    }

    /** Pure: the loadout slot an armor item belongs in, or -1 for anything else. */
    public static int armorSlot(String name) {
        String n = name.toUpperCase(Locale.ROOT);
        if (n.endsWith("_HELMET")) return Loadout.HELMET;
        if (n.endsWith("_CHESTPLATE") || n.equals("ELYTRA")) return Loadout.CHESTPLATE;
        if (n.endsWith("_LEGGINGS")) return Loadout.LEGGINGS;
        if (n.endsWith("_BOOTS")) return Loadout.BOOTS;
        return -1;
    }

    /** Pure: the first free inventory slot of a loadout (never the reserved one), or -1 when it is full. */
    public static int firstFree(Loadout<?> loadout) {
        for (int i = 0; i < Loadout.INVENTORY; i++) if (!Loadout.reserved(i) && loadout.at(i) == null) return i;
        return -1;
    }
}
