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
        return slot >= plugin.unlockedSlots(p) && slot < 36;
    }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void click(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p) || !plugin.enrolled(p)) return;
        boolean partial = plugin.unlockedSlots(p) < 36;
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
        if (partial) {
            // Temporary menu contents are returned on close, outside cancellable transfer events.
            if (guardsTemporaryMenu(e.getView().getTopInventory().getType())
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
        // From storage to a real external inventory is safe; armor -> player is not.
        // The player's own inventory screen is exempt: shift-clicking between
        // hotbar and storage, and shift-crafting out of the 2x2 result, are
        // baseline actions. A locked slot cannot receive them because
        // LockedSlots keeps a marker in it, and anything that does land in one
        // is evicted on the next refresh.
        if (e.isShiftClick() && plugin.unlockedSlots(p) < 36
                && (!own || e.getView().getTopInventory().getType() == InventoryType.CREATIVE)) {
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
        if (plugin.unlockedSlots(p)<36 && guardsTemporaryMenu(e.getView().getTopInventory().getType())
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
    public static boolean guardsTemporaryMenu(InventoryType type) {
        return guardsTemporaryMenu(type.name());
    }

    /**
     * The same policy keyed by type name, so it is testable.
     *
     * InventoryType is an enum whose static initializer reaches a registry that
     * only exists on a running server, so a unit test cannot name its constants.
     * That is why the original guard test stubbed this classification out
     * entirely and never distinguished CRAFTING from WORKBENCH -- which is how
     * the crafting blocker survived. Keying the policy by name makes the real
     * decision assertable off-server.
     */
    static boolean guardsTemporaryMenu(String typeName) {
        return !"CRAFTING".equals(typeName) && returnsOnClose(typeName);
    }

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
