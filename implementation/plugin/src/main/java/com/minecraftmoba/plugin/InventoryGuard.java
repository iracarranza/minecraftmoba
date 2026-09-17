package com.minecraftmoba.plugin;

import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.*;

/** Preventive only. No inventory writes, item transport, rollback or scans that repair slots. */
public final class InventoryGuard implements Listener {
    private final MobaPlugin plugin;
    public InventoryGuard(MobaPlugin plugin) { this.plugin = plugin; }
    private boolean locked(Player p, int slot) {
        return slot >= plugin.unlockedSlots(p) && slot < 36;
    }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void click(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p) || !plugin.enrolled(p)) return;
        boolean partial = plugin.unlockedSlots(p) < 36;
        boolean own = e.getClickedInventory() instanceof PlayerInventory;
        if ((own && (locked(p, e.getSlot()) || e.getSlot() == 40))
                || (e.getHotbarButton() >= 0 && locked(p, e.getHotbarButton()))
                || e.getClick() == ClickType.SWAP_OFFHAND
                || plugin.isMap(e.getCurrentItem()) || plugin.isMap(e.getCursor())) {
            e.setCancelled(true); return;
        }
        if (partial) {
            // Temporary menu contents are returned on close, outside cancellable transfer events.
            if (returnsOnClose(e.getView().getTopInventory().getType())
                    && e.getClickedInventory() == e.getView().getTopInventory()) {
                e.setCancelled(true); return;
            }
            if (e.getAction() == InventoryAction.HOTBAR_MOVE_AND_READD) {
                e.setCancelled(true); return;
            }
            boolean cursor = e.getCursor() != null && !e.getCursor().isEmpty();
            if (cursor && e.getAction() != InventoryAction.PLACE_ALL && e.getAction() != InventoryAction.PLACE_ONE
                    && e.getAction() != InventoryAction.PLACE_SOME && e.getAction() != InventoryAction.DROP_ALL_CURSOR
                    && e.getAction() != InventoryAction.DROP_ONE_CURSOR && e.getAction() != InventoryAction.NOTHING) {
                e.setCancelled(true); return;
            }
            InventoryAction action=e.getAction();
            if (!cursor && (action==InventoryAction.PICKUP_ALL || action==InventoryAction.PICKUP_HALF
                    || action==InventoryAction.PICKUP_ONE || action==InventoryAction.PICKUP_SOME)) {
                ItemStack item=e.getCurrentItem();
                if (item != null && !item.isEmpty()) {
                    boolean storageSource=own && e.getSlot()>=0 && e.getSlot()<plugin.unlockedSlots(p);
                    if (!storageSource && !canAccept(p.getInventory(),item,plugin.unlockedSlots(p))) {
                        e.setCancelled(true); return;
                    }
                    if (storageSource && unsafeMerge(p.getInventory(),item,plugin.unlockedSlots(p))) {
                        e.setCancelled(true); return;
                    }
                }
            }
        }
        // Shift insertion does not expose its destination. Do not guess or relocate.
        // From storage to a real external inventory is safe; armor/crafting -> player is not.
        if (e.isShiftClick() && plugin.unlockedSlots(p) < 36
                && (!own || e.getView().getTopInventory().getType() == InventoryType.CRAFTING
                    || e.getView().getTopInventory().getType() == InventoryType.CREATIVE)) {
            e.setCancelled(true); return;
        }
        // Double-click collection can consume stacks from locked slots or the map.
        if (e.getAction() == InventoryAction.COLLECT_TO_CURSOR && plugin.unlockedSlots(p) < 36)
            e.setCancelled(true);
        if (e.getAction() == InventoryAction.UNKNOWN) e.setCancelled(true);
    }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void drag(InventoryDragEvent e) {
        if (!(e.getWhoClicked() instanceof Player p) || !plugin.enrolled(p)) return;
        if (plugin.unlockedSlots(p)<36 && returnsOnClose(e.getView().getTopInventory().getType())
                && e.getRawSlots().stream().anyMatch(s->s<e.getView().getTopInventory().getSize())) {
            e.setCancelled(true); return;
        }
        if (plugin.isMap(e.getOldCursor())) { e.setCancelled(true); return; }
        for (int raw : e.getRawSlots()) {
            if (e.getView().getInventory(raw) instanceof PlayerInventory) {
                int slot = e.getView().convertSlot(raw);
                if (locked(p, slot) || slot == 40) { e.setCancelled(true); return; }
            }
        }
    }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void pickup(EntityPickupItemEvent e) {
        if (e.getEntity() instanceof Player p && plugin.enrolled(p)
                && ((!p.getItemOnCursor().isEmpty() && plugin.unlockedSlots(p)<36)
                    || !canAccept(p.getInventory(), e.getItem().getItemStack(), plugin.unlockedSlots(p))))
            e.setCancelled(true);
    }
    static boolean canAccept(PlayerInventory inventory, ItemStack item, int unlocked) {
        // Vanilla merges before finding empty slots, including offhand/selected/locked stacks.
        // Reject an unsafe merge target even when an earlier empty unlocked slot exists.
        if (unsafeMerge(inventory,item,unlocked)) return false;
        long room = 0;
        for (int slot = 0; slot < unlocked; slot++) {
            ItemStack stack = inventory.getItem(slot);
            room += stack == null || stack.isEmpty()
                ? Math.min(item.getMaxStackSize(), inventory.getMaxStackSize())
                : mergeRoom(stack, item, inventory.getMaxStackSize());
        }
        return room >= item.getAmount();
    }
    private static boolean unsafeMerge(PlayerInventory inventory,ItemStack item,int unlocked) {
        for (int slot = unlocked; slot < 36; slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (mergeRoom(stack, item, inventory.getMaxStackSize()) > 0) return true;
        }
        return mergeRoom(inventory.getItemInOffHand(), item, inventory.getMaxStackSize()) > 0;
    }
    public static boolean returnsOnClose(InventoryType type) {
        return switch(type) {
            case CRAFTING, WORKBENCH, ANVIL, SMITHING, ENCHANTING, GRINDSTONE, CARTOGRAPHY,
                 STONECUTTER, LOOM, MERCHANT -> true;
            default -> false;
        };
    }
    public static boolean safeToReduce(Player p) {
        if (!p.getItemOnCursor().isEmpty()) return false;
        Inventory top=p.getOpenInventory().getTopInventory();
        if (!returnsOnClose(top.getType())) return true;
        return java.util.Arrays.stream(top.getContents()).allMatch(i->i==null || i.isEmpty());
    }
    private static int mergeRoom(ItemStack stack, ItemStack item, int limit) {
        return stack != null && !stack.isEmpty() && stack.isSimilar(item)
            ? Math.max(0, Math.min(stack.getMaxStackSize(), limit) - stack.getAmount()) : 0;
    }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void transfer(InventoryMoveItemEvent e) {
        if (plugin.isMap(e.getItem())) { e.setCancelled(true); return; }
        if (e.getDestination() instanceof PlayerInventory inv && inv.getHolder() instanceof Player p
                && plugin.enrolled(p) && (plugin.unlockedSlots(p) < 36
                    || !canAccept(inv, e.getItem(), plugin.unlockedSlots(p))))
            e.setCancelled(true);
    }
    @EventHandler(priority = EventPriority.HIGHEST)
    public void swap(PlayerSwapHandItemsEvent e) {
        if (plugin.enrolled(e.getPlayer())) e.setCancelled(true);
    }
}
