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
    /** A locked-slot marker is furniture; nothing may pick it up or move it. */
    private boolean marker(Player p, int slot) {
        return plugin.lockedSlots() != null
                && plugin.lockedSlots().isMarker(p.getInventory().getItem(slot));
    }

    private boolean locked(Player p, int slot) {
        return isLockedStorageSlot(slot, plugin.unlockedSlots(p));
    }

    /**
     * Whether a player-inventory slot index is locked storage.
     *
     * This is the whole of the capacity rule as the guard enforces it: storage
     * is slots 0-35, and anything at or above the unlocked count is locked.
     * Slots 36-40 are armour and offhand, which capacity does not govern, and a
     * negative index means the click was not in the player's inventory at all.
     *
     * Expressed as a static so it can be asserted without a server. The version
     * of this guard that shipped the crafting blocker hid its decision behind a
     * mocked seam, and nothing could tell CRAFTING from WORKBENCH.
     */
    static boolean isLockedStorageSlot(int slot, int unlocked) {
        return slot >= unlocked && slot >= 0 && slot < 36;
    }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void click(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p) || !plugin.enrolled(p)) return;
        boolean own = e.getClickedInventory() instanceof PlayerInventory;
        // Lifting the tome out of the offhand is the recall gesture. The item
        // is never actually removed; the click is consumed instead.
        if (own && e.getSlot() == 40 && plugin.recall() != null
                && plugin.offhandMap().isMap(p.getInventory().getItemInOffHand())
                && plugin.recall().beginFromOffhandClick(p)) { e.setCancelled(true); return; }
        if ((own && (locked(p, e.getSlot()) || e.getSlot() == 40))
                || (e.getHotbarButton() >= 0 && locked(p, e.getHotbarButton()))
                || e.getClick() == ClickType.SWAP_OFFHAND
                || plugin.isMap(e.getCurrentItem()) || plugin.isMap(e.getCursor())) {
            e.setCancelled(true); return;
        }
        // Everything below the direct-slot rules above is ordinary vanilla.
        //
        // This block used to cancel, at partial capacity: any click inside a
        // temporary menu, HOTBAR_MOVE_AND_READD, most cursor actions, unsafe
        // pickups, every shift-click outside the player's own inventory, and
        // double-click collection. It was written as prevention, before
        // anything repaired a locked slot. The first playtest found what that
        // cost: the 2x2 grid, the crafting table, shift-clicking a result and
        // number-keying a result out were all unusable for any player below
        // full capacity, which is every player at the start of a match.
        //
        // Capacity is now an invariant maintained by repair rather than by
        // forbidding interactions that might reach a locked slot:
        //   - a locked slot cannot be clicked directly (above);
        //   - a number key cannot target a locked slot (above);
        //   - LockedSlots keeps a marker in every empty locked slot, so
        //     vanilla's own placement never chooses one;
        //   - anything that lands in a locked slot anyway is evicted on the
        //     next refresh, into unlocked space or onto the ground;
        //   - ground pickup is still capacity-checked, so a player cannot
        //     passively acquire more than they can hold.
        // The worst case is that a player holds a few extra items for under a
        // second before eviction. Nothing is duplicated and nothing is lost.
        if (e.getAction() == InventoryAction.UNKNOWN) e.setCancelled(true);
    }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void drag(InventoryDragEvent e) {
        if (!(e.getWhoClicked() instanceof Player p) || !plugin.enrolled(p)) return;
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
    /**
     * Whether a temporary menu's contents must be guarded against capacity bypass.
     *
     * Every {@link #returnsOnClose} type returns its contents to the player when
     * it closes, outside any cancellable transfer event, so a player with locked
     * slots could park items there and have them come back into locked storage.
     *
     * CRAFTING is the exception, and treating it like the rest was a real bug:
     * it is not a menu the player opens, it is the 2x2 grid on their own
     * inventory screen. Guarding it blanket-cancelled every click on the
     * crafting inputs and the result, which made baseline crafting impossible
     * for anyone below full inventory capacity -- that is, every player at the
     * start of a match. Capacity there is enforced by marker occupancy and by
     * LockedSlots evicting anything that reaches a locked slot, not by
     * forbidding the interaction.
     */
    public static boolean returnsOnClose(InventoryType type) {
        return returnsOnClose(type.name());
    }

    static boolean returnsOnClose(String typeName) {
        return switch (typeName) {
            case "CRAFTING", "WORKBENCH", "ANVIL", "SMITHING", "ENCHANTING", "GRINDSTONE",
                 "CARTOGRAPHY", "STONECUTTER", "LOOM", "MERCHANT" -> true;
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
