package com.minecraftmoba.plugin;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class InventoryGuardTest {
    private ItemStack stack(int amount) {
        ItemStack s = mock(ItemStack.class);
        when(s.getType()).thenReturn(Material.STONE);
        when(s.getAmount()).thenReturn(amount);
        when(s.getMaxStackSize()).thenReturn(64);
        return s;
    }
    @Test void pickupRejectsWholeStackWhenOnlyLockedSlotsHaveSpace() {
        var inv = mock(PlayerInventory.class);
        when(inv.getMaxStackSize()).thenReturn(64);
        ItemStack incoming = stack(2);
        for (int i = 0; i < 6; i++) {
            var full = stack(64); when(full.isSimilar(incoming)).thenReturn(true);
            when(inv.getItem(i)).thenReturn(full);
        }
        assertFalse(InventoryGuard.canAccept(inv, incoming, 6));
        assertTrue(InventoryGuard.canAccept(inv, incoming, 9));
    }
    @Test void pickupRejectsLockedMergeEvenWithEmptyUnlockedSlots() {
        var inv = mock(PlayerInventory.class); when(inv.getMaxStackSize()).thenReturn(64);
        var incoming = stack(1); var locked = stack(1);
        when(locked.isSimilar(incoming)).thenReturn(true); when(inv.getItem(20)).thenReturn(locked);
        assertFalse(InventoryGuard.canAccept(inv, incoming, 6));
    }
    @Test void offhandMergeIsNeverUsed() {
        var inv = mock(PlayerInventory.class); when(inv.getMaxStackSize()).thenReturn(64);
        var incoming = stack(1); var offhand = stack(1);
        when(offhand.isSimilar(incoming)).thenReturn(true); when(inv.getItemInOffHand()).thenReturn(offhand);
        assertFalse(InventoryGuard.canAccept(inv, incoming, 36));
    }
    @Test void slotAndNumberKeyPathsCancelWithoutWritingInventory() {
        var plugin = mock(MobaPlugin.class); var player = mock(Player.class);
        when(plugin.enrolled(player)).thenReturn(true); when(plugin.unlockedSlots(player)).thenReturn(6);
        var inventory = mock(PlayerInventory.class); var guard = new InventoryGuard(plugin);
        var click = mock(InventoryClickEvent.class);
        when(click.getWhoClicked()).thenReturn(player); when(click.getClickedInventory()).thenReturn(inventory);
        when(click.getSlot()).thenReturn(8); when(click.getHotbarButton()).thenReturn(-1);
        guard.click(click); verify(click).setCancelled(true); verifyNoInteractions(inventory);
        var hotkey = mock(InventoryClickEvent.class);
        when(hotkey.getWhoClicked()).thenReturn(player); when(hotkey.getHotbarButton()).thenReturn(8);
        guard.click(hotkey); verify(hotkey).setCancelled(true);
        var offhand = mock(InventoryClickEvent.class);
        when(offhand.getWhoClicked()).thenReturn(player); when(offhand.getClick()).thenReturn(ClickType.SWAP_OFFHAND);
        guard.click(offhand); verify(offhand).setCancelled(true);
    }
    @Test void temporaryMenuInsertionAndUnsafeExternalCursorPickupAreCancelled() {
        var plugin=mock(MobaPlugin.class); var player=mock(Player.class);
        when(plugin.enrolled(player)).thenReturn(true); when(plugin.unlockedSlots(player)).thenReturn(6);
        var guard=new InventoryGuard(plugin); var top=mock(Inventory.class); var view=mock(InventoryView.class);
        when(view.getTopInventory()).thenReturn(top);
        // Registry-backed InventoryType needs a live server; stub only that classification seam.
        // The classification itself is covered by CraftingAccessTest, which is
        // what this stub hid: it never distinguished one menu type from another.
        try (var policy=mockStatic(InventoryGuard.class)) {
        policy.when(() -> InventoryGuard.guardsTemporaryMenu((org.bukkit.event.inventory.InventoryType) null)).thenReturn(true);
        var insert=mock(InventoryClickEvent.class);
        when(insert.getWhoClicked()).thenReturn(player); when(insert.getView()).thenReturn(view);
        when(insert.getClickedInventory()).thenReturn(top); when(insert.getHotbarButton()).thenReturn(-1);
        guard.click(insert); verify(insert).setCancelled(true);
        policy.when(() -> InventoryGuard.guardsTemporaryMenu((org.bukkit.event.inventory.InventoryType) null)).thenReturn(false);
        var inv=mock(PlayerInventory.class); when(player.getInventory()).thenReturn(inv);
        when(inv.getMaxStackSize()).thenReturn(64);
        var incoming=stack(64);
        for(int n=0;n<6;n++) { var full=stack(64); when(inv.getItem(n)).thenReturn(full); }
        var pickup=mock(InventoryClickEvent.class);
        when(pickup.getWhoClicked()).thenReturn(player); when(pickup.getView()).thenReturn(view);
        when(pickup.getClickedInventory()).thenReturn(top); when(pickup.getHotbarButton()).thenReturn(-1);
        when(pickup.getAction()).thenReturn(InventoryAction.PICKUP_ALL); when(pickup.getCurrentItem()).thenReturn(incoming);
        guard.click(pickup); verify(pickup).setCancelled(true);
        }
    }
}
