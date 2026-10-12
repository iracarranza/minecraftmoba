package com.minecraftmoba.plugin;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

/** What each class carries in the lab, as data, and the rule that none is forgotten. */
class ClassKitsTest {
    private static YamlConfiguration config() { return YamlConfiguration.loadConfiguration(new File("src/main/resources/config.yml")); }

    @Test void entriesParseAsMaterialAndAmount() {
        var kit = ClassKits.parse(List.of("IRON_SHOVEL:1", "cooked_beef:64"));
        assertEquals(new ClassKits.Entry(Material.IRON_SHOVEL, 1), kit.get(0));
        assertEquals(new ClassKits.Entry(Material.COOKED_BEEF, 64), kit.get(1));
        assertEquals(List.of(), ClassKits.parse(List.of()));
    }

    @Test void badEntriesAreRefusedWithTheirReason() {
        assertTrue(assertThrows(IllegalArgumentException.class, () -> ClassKits.parse(List.of("IRON_SHOVEL"))).getMessage().contains("MATERIAL:amount"));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> ClassKits.parse(List.of("NOPE:1"))).getMessage().contains("not an item"));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> ClassKits.parse(List.of("SAND:many"))).getMessage().contains("not a number"));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> ClassKits.parse(List.of("SAND:65"))).getMessage().contains("stack"));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> ClassKits.parse(List.of("SAND:0"))).getMessage().contains("stack"));
    }

    @Test void everyShippedKitParses() {
        var section = config().getConfigurationSection(ClassKits.KEY);
        assertNotNull(section);
        for (String id : section.getKeys(false)) assertDoesNotThrow(() -> ClassKits.parse(section.getStringList(id)), id);
    }

    @Test void everyClassIsListedSoANewOneCannotBeForgotten() {
        var cfg = config();
        var classes = cfg.getConfigurationSection("abilities.classes");
        assertNotNull(classes);
        for (String id : classes.getKeys(false))
            assertTrue(ClassKits.listed(cfg, id), id + " has no entry in " + ClassKits.KEY + " (an empty list is fine: it says the class consumes no items)");
    }

    @Test void noKitNamesAClassThatDoesNotExist() {
        var cfg = config();
        var classes = cfg.getConfigurationSection("abilities.classes").getKeys(false);
        for (String id : cfg.getConfigurationSection(ClassKits.KEY).getKeys(false)) assertTrue(classes.contains(id), id);
    }

    @Test void theFiveKitsThatWereHardcodedInTheBodyFactoryAreUnchanged() {
        var cfg = config();
        assertEquals(List.of(new ClassKits.Entry(Material.IRON_SHOVEL, 1), new ClassKits.Entry(Material.COOKED_BEEF, 64), new ClassKits.Entry(Material.BEEF, 32)), ClassKits.of(cfg, "chef"));
        assertEquals(List.of(new ClassKits.Entry(Material.IRON_SWORD, 1)), ClassKits.of(cfg, "werewolf"));
        assertEquals(List.of(new ClassKits.Entry(Material.IRON_SWORD, 1)), ClassKits.of(cfg, "skeleton_crew"));
        assertEquals(List.of(new ClassKits.Entry(Material.WHEAT_SEEDS, 64)), ClassKits.of(cfg, "gardener"));
        assertEquals(List.of(Material.IRON_SWORD, Material.BOW, Material.CROSSBOW, Material.FISHING_ROD, Material.GOLDEN_APPLE, Material.ARROW),
                ClassKits.of(cfg, "kitfighter").stream().map(ClassKits.Entry::material).toList());
        assertEquals(16, ClassKits.of(cfg, "kitfighter").get(4).amount());
        assertEquals(16, ClassKits.of(cfg, "kitfighter").get(5).amount());
    }

    @Test void thePaverCarriesWhatItsMixerConsumes() {
        var materials = ClassKits.of(config(), "paver").stream().map(ClassKits.Entry::material).toList();
        for (var m : List.of(Material.SAND, Material.GRAVEL, Material.WHITE_DYE)) assertTrue(materials.contains(m), m.name());
    }

    @Test void theQuarrymanCarriesAPickaxe() {
        assertTrue(ClassKits.of(config(), "quarryman").stream().anyMatch(e -> e.material().name().endsWith("_PICKAXE")));
    }

    @Test void anUnlistedClassOrNoClassIsEmptyNotAnError() {
        assertEquals(List.of(), ClassKits.of(config(), "not_a_class"));
        assertEquals(List.of(), ClassKits.of(config(), null));
        assertFalse(ClassKits.listed(config(), "not_a_class"));
    }
}
