package com.minecraftmoba.plugin;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

/** Reading a player's inventory into a {@link Loadout}, and giving one back. Copies, never shares a stack. */
final class LoadoutGear {
    private LoadoutGear() {}

    private static ItemStack copy(ItemStack s) { return s == null || s.getType().isAir() || s.getAmount() <= 0 ? null : s.clone(); }

    /** The player's 36 slots and armor, as copies. The reserved slot is left out; the offhand is never part of a loadout. */
    static Loadout<ItemStack> capture(Player p) { return capture(p, slot -> p.getInventory().getItem(slot)); }

    /** As {@link #capture(Player)}, reading each inventory slot through {@code reader} (to see past a menu). */
    static Loadout<ItemStack> capture(Player p, java.util.function.IntFunction<ItemStack> reader) {
        PlayerInventory inv = p.getInventory();
        var l = Loadout.<ItemStack>empty();
        for (int i = 0; i < Loadout.INVENTORY; i++)
            if (!Loadout.reserved(i)) l = l.with(i, copy(reader.apply(i)));
        l = l.with(Loadout.HELMET, copy(inv.getHelmet()));
        l = l.with(Loadout.CHESTPLATE, copy(inv.getChestplate()));
        l = l.with(Loadout.LEGGINGS, copy(inv.getLeggings()));
        l = l.with(Loadout.BOOTS, copy(inv.getBoots()));
        return l;
    }

    /** Everything the player carries but the offhand, including the reserved slot, for the testing-mode snapshot. */
    static java.util.List<ItemStack> captureAll(Player p) {
        PlayerInventory inv = p.getInventory();
        var out = new java.util.ArrayList<ItemStack>(Loadout.SIZE);
        for (int i = 0; i < Loadout.INVENTORY; i++) out.add(copy(inv.getItem(i)));
        out.add(copy(inv.getHelmet()));
        out.add(copy(inv.getChestplate()));
        out.add(copy(inv.getLeggings()));
        out.add(copy(inv.getBoots()));
        return out;
    }

    /**
     * Empty the 36 slots and the armor (and the cursor, which would otherwise carry an item out). The offhand is
     * left alone: it holds the plugin's map, and the plugin pauses enrolment if anything else is there.
     * {@code PlayerInventory#clear()} is not used because it clears the offhand too.
     */
    static void clear(Player p) {
        PlayerInventory inv = p.getInventory();
        inv.setStorageContents(new ItemStack[Loadout.INVENTORY]);
        inv.setArmorContents(new ItemStack[4]);
        p.setItemOnCursor(null);
    }

    /** Replace everything the player carries with these items. */
    static void give(Player p, java.util.List<ItemStack> items) {
        clear(p);
        PlayerInventory inv = p.getInventory();
        for (int i = 0; i < Loadout.INVENTORY; i++) inv.setItem(i, copy(items.get(i)));
        inv.setHelmet(copy(items.get(Loadout.HELMET)));
        inv.setChestplate(copy(items.get(Loadout.CHESTPLATE)));
        inv.setLeggings(copy(items.get(Loadout.LEGGINGS)));
        inv.setBoots(copy(items.get(Loadout.BOOTS)));
    }

    static void give(Player p, Loadout<ItemStack> l) { give(p, l.items()); }
}
