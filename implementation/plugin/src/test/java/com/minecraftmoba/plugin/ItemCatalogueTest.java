package com.minecraftmoba.plugin;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

class ItemCatalogueTest {
    private static final ItemCatalogue.Facts PLAIN = new ItemCatalogue.Facts(false, false);
    private static final ItemCatalogue.Facts FOOD = new ItemCatalogue.Facts(true, false);
    private static final ItemCatalogue.Facts BLOCK = new ItemCatalogue.Facts(false, true);

    private static ItemCatalogue.Category c(String n) { return ItemCatalogue.categorize(n, PLAIN); }

    @Test void armorWeaponsAndToolsFindTheirShelves() {
        assertEquals(ItemCatalogue.Category.ARMOR, c("IRON_CHESTPLATE"));
        assertEquals(ItemCatalogue.Category.ARMOR, c("TURTLE_HELMET"));
        assertEquals(ItemCatalogue.Category.ARMOR, c("ELYTRA"));
        assertEquals(ItemCatalogue.Category.COMBAT, c("DIAMOND_SWORD"));
        assertEquals(ItemCatalogue.Category.COMBAT, c("CROSSBOW"));
        assertEquals(ItemCatalogue.Category.COMBAT, c("SPECTRAL_ARROW"));
        assertEquals(ItemCatalogue.Category.TOOLS, c("DIAMOND_PICKAXE"));
        assertEquals(ItemCatalogue.Category.TOOLS, c("WATER_BUCKET"));
        assertEquals(ItemCatalogue.Category.TOOLS, c("FISHING_ROD"));
    }

    @Test void foodComesFromWhatTheServerSaysIsEdible() {
        assertEquals(ItemCatalogue.Category.FOOD, ItemCatalogue.categorize("COOKED_BEEF", FOOD));
        assertEquals(ItemCatalogue.Category.FOOD, ItemCatalogue.categorize("GOLDEN_APPLE", FOOD));
        assertNotEquals(ItemCatalogue.Category.FOOD, c("COOKED_BEEF"), "without the edible flag the name alone does not make it food");
    }

    @Test void redstoneMachinesAndVehicles() {
        for (String n : List.of("REDSTONE", "STICKY_PISTON", "OBSERVER", "HOPPER", "TNT", "OAK_BOAT", "MINECART", "POWERED_RAIL", "STONE_BUTTON"))
            assertEquals(ItemCatalogue.Category.REDSTONE, c(n), n);
    }

    @Test void plantsAndMaterials() {
        for (String n : List.of("WHEAT_SEEDS", "OAK_SAPLING", "TORCHFLOWER", "SWEET_BERRIES", "BAMBOO", "KELP", "RED_MUSHROOM"))
            assertEquals(ItemCatalogue.Category.PLANTS, c(n), n);
        for (String n : List.of("IRON_INGOT", "DIAMOND", "STICK", "STRING", "WHITE_DYE", "RAW_COPPER", "COAL"))
            assertEquals(ItemCatalogue.Category.MATERIALS, c(n), n);
    }

    @Test void blocksAreWhatIsLeftAndABlockFlagMatters() {
        assertEquals(ItemCatalogue.Category.BLOCKS, ItemCatalogue.categorize("WHITE_CONCRETE", BLOCK));
        assertEquals(ItemCatalogue.Category.BLOCKS, ItemCatalogue.categorize("SAND", BLOCK));
        assertEquals(ItemCatalogue.Category.MISC, c("SADDLE"));
    }

    @Test void theFirstShelfThatClaimsAnItemWins() {
        assertEquals(ItemCatalogue.Category.COMBAT, c("IRON_SWORD"), "not Materials, though it is made of iron");
        assertEquals(ItemCatalogue.Category.FOOD, ItemCatalogue.categorize("SWEET_BERRIES", FOOD), "edible plants are Food");
        assertEquals(ItemCatalogue.Category.PLANTS, c("SWEET_BERRIES"));
    }

    @Test void nothingIsEverCategorisedAsAll() {
        for (Material m : Material.values()) assertNotEquals(ItemCatalogue.Category.ALL, ItemCatalogue.categorize(m.name(), PLAIN), m.name());
    }

    @Test void searchMatchesEveryWordInAnyOrderCaseInsensitively() {
        assertTrue(ItemCatalogue.matches("WHITE_CONCRETE_POWDER", "powder white"));
        assertTrue(ItemCatalogue.matches("WHITE_CONCRETE_POWDER", "concrete_powder"));
        assertFalse(ItemCatalogue.matches("WHITE_CONCRETE", "powder"));
        assertTrue(ItemCatalogue.matches("SAND", ""));
        assertTrue(ItemCatalogue.matches("SAND", null));
        assertTrue(ItemCatalogue.matches("SAND", "   "));
    }

    @Test void aShelfIsFilteredByCategoryAndSearchAndSortedByName() {
        var all = new ArrayList<ItemCatalogue.Entry>();
        for (String n : List.of("SAND", "GRAVEL", "RED_SAND", "IRON_SWORD"))
            all.add(new ItemCatalogue.Entry(Material.valueOf(n), n.equals("IRON_SWORD") ? ItemCatalogue.Category.COMBAT : ItemCatalogue.Category.BLOCKS));
        assertEquals(List.of(Material.GRAVEL, Material.IRON_SWORD, Material.RED_SAND, Material.SAND), ItemCatalogue.shelf(all, ItemCatalogue.Category.ALL, ""));
        assertEquals(List.of(Material.GRAVEL, Material.RED_SAND, Material.SAND), ItemCatalogue.shelf(all, ItemCatalogue.Category.BLOCKS, null));
        assertEquals(List.of(Material.RED_SAND, Material.SAND), ItemCatalogue.shelf(all, ItemCatalogue.Category.ALL, "sand"));
        assertEquals(List.of(), ItemCatalogue.shelf(all, ItemCatalogue.Category.COMBAT, "sand"));
    }

    @Test void pagingAlwaysHasAFirstPageAndClampsTheRest() {
        assertEquals(1, ItemCatalogue.pages(0, 45));
        assertEquals(1, ItemCatalogue.pages(45, 45));
        assertEquals(2, ItemCatalogue.pages(46, 45));
        var shelf = new ArrayList<Integer>(); for (int i = 0; i < 100; i++) shelf.add(i);
        assertEquals(45, ItemCatalogue.page(shelf, 0, 45).size());
        assertEquals(10, ItemCatalogue.page(shelf, 2, 45).size());
        assertEquals(ItemCatalogue.page(shelf, 2, 45), ItemCatalogue.page(shelf, 99, 45), "past the end is the last page");
        assertEquals(ItemCatalogue.page(shelf, 0, 45), ItemCatalogue.page(shelf, -3, 45));
        assertEquals(List.of(), ItemCatalogue.page(List.<Integer>of(), 0, 45));
    }

    @Test void armorGoesToItsOwnSlot() {
        assertEquals(Loadout.HELMET, ItemCatalogue.armorSlot("IRON_HELMET"));
        assertEquals(Loadout.CHESTPLATE, ItemCatalogue.armorSlot("NETHERITE_CHESTPLATE"));
        assertEquals(Loadout.CHESTPLATE, ItemCatalogue.armorSlot("ELYTRA"));
        assertEquals(Loadout.LEGGINGS, ItemCatalogue.armorSlot("LEATHER_LEGGINGS"));
        assertEquals(Loadout.BOOTS, ItemCatalogue.armorSlot("DIAMOND_BOOTS"));
        assertEquals(-1, ItemCatalogue.armorSlot("DIAMOND_SWORD"));
    }

    @Test void theFirstFreeSlotSkipsTheReservedOneAndReportsFull() {
        var l = Loadout.<String>empty();
        for (int i = 0; i < 8; i++) l = l.with(i, "x");
        assertEquals(9, ItemCatalogue.firstFree(l), "slot 8 is the exit item's, so the next free is 9");
        for (int i = 9; i < Loadout.INVENTORY; i++) l = l.with(i, "x");
        assertEquals(-1, ItemCatalogue.firstFree(l));
    }
}
