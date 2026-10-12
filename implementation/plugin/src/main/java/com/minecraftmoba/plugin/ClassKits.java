package com.minecraftmoba.plugin;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * What a class carries in the lab, from config ({@code alpha.lab.classKits}).
 *
 * Chef's Food Fight needs food and a shovel, Paver's Mixer needs sand, gravel and dye, Quarryman's Strike needs a
 * pickaxe. A dummy body of one of those classes used to carry nothing unless someone had hardcoded its items into
 * {@link NmsBodies}, which five classes had and the six newest did not, so their bodies could not cast and their testers
 * had to build a kit by hand. This is that list, as data, applied to bodies at spawn and offered to testers as a loadout.
 *
 * Prototype/test fixtures, not design. Every class must be listed (empty means it consumes no items), and a unit
 * test fails when one is not, so the next class cannot be forgotten.
 */
public final class ClassKits {
    private ClassKits() {}

    public static final String KEY = "alpha.lab.classKits";

    /** One stack of a kit. */
    public record Entry(Material material, int amount) {}

    /** Pure: parse {@code MATERIAL:amount} entries, refusing an unknown material or an amount outside 1 to 64. */
    public static List<Entry> parse(List<String> raw) {
        var out = new ArrayList<Entry>();
        for (String s : raw) {
            String[] parts = s.trim().split(":");
            if (parts.length != 2) throw new IllegalArgumentException("'" + s + "' is not MATERIAL:amount");
            Material m;
            try { m = Material.valueOf(parts[0].trim().toUpperCase(java.util.Locale.ROOT)); }
            catch (IllegalArgumentException ex) { throw new IllegalArgumentException("'" + parts[0] + "' is not an item"); }
            int amount;
            try { amount = Integer.parseInt(parts[1].trim()); }
            catch (NumberFormatException ex) { throw new IllegalArgumentException("'" + parts[1] + "' is not a number in '" + s + "'"); }
            if (amount < 1 || amount > 64)
                throw new IllegalArgumentException("'" + s + "': an amount of " + amount + " does not fit a stack (1 to 64)");
            out.add(new Entry(m, amount));
        }
        return out;
    }

    /** The kit for a class, or an empty list when it has none or is not listed. */
    public static List<Entry> of(ConfigurationSection config, String classId) {
        if (classId == null) return List.of();
        var section = config.getConfigurationSection(KEY);
        return section == null ? List.of() : parse(section.getStringList(classId));
    }

    /** Whether the class is listed at all (an empty list counts). */
    public static boolean listed(ConfigurationSection config, String classId) {
        var section = config.getConfigurationSection(KEY);
        return section != null && section.contains(classId);
    }

    /** The kit as items, in slot order. */
    public static List<ItemStack> items(ConfigurationSection config, String classId) {
        var out = new ArrayList<ItemStack>();
        for (Entry e : of(config, classId)) {
            // Checked here, where the server's own stack sizes are available: a stack of two shovels is a config mistake.
            if (e.amount() > e.material().getMaxStackSize())
                throw new IllegalArgumentException(classId + " kit: " + e.amount() + " x " + e.material() + " does not fit a stack of " + e.material().getMaxStackSize());
            out.add(new ItemStack(e.material(), e.amount()));
        }
        return out;
    }

    /** Put the kit in a body's or tester's first slots, one stack each. Does not clear anything first. */
    public static void give(ConfigurationSection config, Player p, String classId) {
        int slot = 0;
        for (ItemStack item : items(config, classId)) p.getInventory().setItem(slot++, item);
    }

    /** The kit as a loadout, for the editor and the testing-mode commands. The reserved slot is never used. */
    public static Loadout<ItemStack> loadout(ConfigurationSection config, String classId) {
        var l = Loadout.<ItemStack>empty();
        int slot = 0;
        for (ItemStack item : items(config, classId)) {
            if (Loadout.reserved(slot)) slot++;
            if (slot >= Loadout.INVENTORY) break;
            l = l.with(slot++, item);
        }
        return l;
    }
}
