package com.minecraftmoba.plugin;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Turns a {@link ChamberMenu.View} into the tester's hotbar, and puts their own
 * hotbar back afterwards.
 *
 * Nothing here decides anything: which items exist and whether each may be used
 * is {@link ChamberMenu} and {@link ChamberController}, both tested without a
 * server. This only writes stacks and reads the slot back.
 *
 * <h2>The tester's own items come back</h2>
 *
 * The hotbar is replaced wholesale, and a lab tester arrives with a class kit
 * in it. So the nine hotbar slots, and the two inventory slots the session
 * verbs occupy, are snapshotted on {@link #engage} and restored on
 * {@link #release}. A menu that ate the kit would be a worse experiment than
 * no menu.
 *
 * <h2>Session verbs live in the inventory</h2>
 *
 * Return-to-lab and test-as-player end or hand over the session, so they sit
 * where acting on one costs an explicit inventory open and click, not in a row
 * you scroll past by accident.
 */
final class ChamberHotbar {
    /** Inventory slots (first row of main inventory) for the two session verbs. */
    static final int RETURN_SLOT = 9, PLAYTEST_SLOT = 10;

    private final NamespacedKey key;
    private final Map<UUID, ItemStack[]> snapshots = new HashMap<>();
    private final Map<UUID, ChamberMenu> menus = new HashMap<>();
    private final Map<UUID, String> lastSignature = new HashMap<>();
    private final java.util.Set<UUID> equipment = new java.util.HashSet<>();

    ChamberHotbar(NamespacedKey key) { this.key = key; }

    boolean engaged(Player p) { return menus.containsKey(p.getUniqueId()); }
    ChamberMenu menu(Player p) { return menus.get(p.getUniqueId()); }

    /** Id carried by a chamber item, or null for anything else. */
    String idOf(ItemStack stack) {
        if (stack == null || !stack.hasItemMeta()) return null;
        return stack.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.STRING);
    }

    void engage(Player p) {
        engage(p, ChamberMenu.chamberMenu(), "Return to lab", "Ends the lab session and returns to setup.", true);
    }

    /**
     * Engage with another menu and its own wording for the one session verb it keeps.
     * The combat chamber has no "test as player", so it leaves that slot alone.
     */
    void engage(Player p, ChamberMenu menu, String returnName, String returnLore, boolean playtest) {
        if (engaged(p)) return;
        var inv = p.getInventory();
        var saved = new ItemStack[ChamberMenu.SLOTS + 2];
        for (int i = 0; i < ChamberMenu.SLOTS; i++) saved[i] = copy(inv.getItem(i));
        saved[ChamberMenu.SLOTS] = copy(inv.getItem(RETURN_SLOT));
        saved[ChamberMenu.SLOTS + 1] = copy(inv.getItem(PLAYTEST_SLOT));
        snapshots.put(p.getUniqueId(), saved);
        menus.put(p.getUniqueId(), menu);
        lastSignature.remove(p.getUniqueId());
        inv.setItem(RETURN_SLOT, sessionItem("return", returnName, Material.ENDER_PEARL, returnLore));
        if (playtest)
            inv.setItem(PLAYTEST_SLOT, sessionItem("playtest", "Test as player", Material.IRON_SWORD,
                    "Leaves the chamber and hands you your own hotbar."));
    }

    void release(Player p) {
        var saved = snapshots.remove(p.getUniqueId());
        menus.remove(p.getUniqueId());
        lastSignature.remove(p.getUniqueId());
        if (saved == null) return;
        var inv = p.getInventory();
        if (!equipment.remove(p.getUniqueId()))
            for (int i = 0; i < ChamberMenu.SLOTS; i++) inv.setItem(i, saved[i]);
        inv.setItem(RETURN_SLOT, saved[ChamberMenu.SLOTS]);
        inv.setItem(PLAYTEST_SLOT, saved[ChamberMenu.SLOTS + 1]);
    }

    /**
     * Write the current page across the hotbar. Skips the write when nothing
     * changed, so a periodic refresh does not flicker the tester's selection.
     */
    void render(Player p, ChamberMenu.Gate gate) {
        if (equipment.contains(p.getUniqueId())) return;
        var menu = menus.get(p.getUniqueId());
        if (menu == null) return;
        var view = menu.view(gate);
        String signature = view.pageId() + "|" + view.slots().stream()
                .map(i -> i == null ? "-" : i.id()).collect(Collectors.joining(","))
                + "|" + String.join(",", view.refusals().stream().map(r -> r == null ? "" : r).toList());
        if (signature.equals(lastSignature.get(p.getUniqueId()))) return;
        lastSignature.put(p.getUniqueId(), signature);
        var inv = p.getInventory();
        for (int slot = 0; slot < ChamberMenu.SLOTS; slot++) {
            var item = view.at(slot);
            inv.setItem(slot, item == null ? null : stack(ChamberItems.spec(item, view.refusalAt(slot))));
        }
        p.updateInventory();
    }

    /**
     * What the player really carries in an inventory slot, seeing through the chamber's menu.
     *
     * While the menu is on the hotbar the real hotbar items are held in a snapshot, and the Leave item
     * stands in slot 9; this answers with the real thing, so a loadout copied from the tester's
     * inventory never contains the chamber's own controls.
     */
    ItemStack real(Player p, int slot) {
        var saved = snapshots.get(p.getUniqueId());
        if (saved != null && !equipment.contains(p.getUniqueId())) {
            if (slot >= 0 && slot < ChamberMenu.SLOTS) return copy(saved[slot]);
            if (slot == RETURN_SLOT) return copy(saved[ChamberMenu.SLOTS]);
            if (slot == PLAYTEST_SLOT) return copy(saved[ChamberMenu.SLOTS + 1]);
        }
        return copy(p.getInventory().getItem(slot));
    }

    /** Swap controls for the real equipment; consumption survives returning to the menu. */
    void toggleEquipment(Player p) {
        var saved = snapshots.get(p.getUniqueId());
        if (saved == null) return;
        if (equipment.remove(p.getUniqueId())) {
            for (int i = 0; i < ChamberMenu.SLOTS; i++) saved[i] = copy(p.getInventory().getItem(i));
            lastSignature.remove(p.getUniqueId());
        } else {
            equipment.add(p.getUniqueId());
            for (int i = 0; i < ChamberMenu.SLOTS; i++) p.getInventory().setItem(i, copy(saved[i]));
        }
    }

    private ItemStack stack(ChamberItems.Spec spec) {
        var stack = new ItemStack(spec.material());
        stack.editMeta(meta -> {
            meta.displayName(Component.text(spec.name(), spec.available()
                    ? NamedTextColor.WHITE : NamedTextColor.DARK_GRAY)
                    .decoration(TextDecoration.ITALIC, false));
            meta.lore(spec.lore().stream().map(l -> (Component) Component.text(l, NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false)).toList());
            meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, spec.itemId());
        });
        return stack;
    }

    private ItemStack sessionItem(String id, String name, Material material, String lore) {
        var stack = new ItemStack(material);
        stack.editMeta(meta -> {
            meta.displayName(Component.text(name, NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(Component.text(lore, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)));
            meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, id);
        });
        return stack;
    }

    private static ItemStack copy(ItemStack s) { return s == null ? null : s.clone(); }

    /** Players currently holding a chamber hotbar, for the periodic sync. */
    java.util.Set<UUID> engagedIds() { return java.util.Set.copyOf(menus.keySet()); }
}
