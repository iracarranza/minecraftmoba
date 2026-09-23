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
    /**
     * A click in a crafting surface must pass through at partial capacity.
     *
     * This replaces a test that asserted the opposite. That test encoded the
     * crafting blocker as intended behaviour: it cancelled any click whose
     * clicked inventory was the top one, which is the 2x2 grid on the player's
     * own screen and the grid of a crafting table.
     */
    @Test void craftingSurfaceClicksPassThroughAtPartialCapacity() {
        var plugin=mock(MobaPlugin.class); var player=mock(Player.class);
        when(plugin.enrolled(player)).thenReturn(true); when(plugin.unlockedSlots(player)).thenReturn(6);
        var guard=new InventoryGuard(plugin); var top=mock(Inventory.class); var view=mock(InventoryView.class);
        when(view.getTopInventory()).thenReturn(top);
        var click=mock(InventoryClickEvent.class);
        when(click.getWhoClicked()).thenReturn(player); when(click.getView()).thenReturn(view);
        when(click.getClickedInventory()).thenReturn(top); when(click.getHotbarButton()).thenReturn(-1);
        when(click.getAction()).thenReturn(InventoryAction.PLACE_ALL);
        guard.click(click);
        verify(click,never()).setCancelled(true);
    }

    /** Shift-clicking a crafting result out must work; it is how you craft. */
    @Test void shiftClickFromACraftingResultIsNotCancelled() {
        var plugin=mock(MobaPlugin.class); var player=mock(Player.class);
        when(plugin.enrolled(player)).thenReturn(true); when(plugin.unlockedSlots(player)).thenReturn(6);
        var guard=new InventoryGuard(plugin); var top=mock(Inventory.class); var view=mock(InventoryView.class);
        when(view.getTopInventory()).thenReturn(top);
        var click=mock(InventoryClickEvent.class);
        when(click.getWhoClicked()).thenReturn(player); when(click.getView()).thenReturn(view);
        when(click.getClickedInventory()).thenReturn(top); when(click.getHotbarButton()).thenReturn(-1);
        when(click.isShiftClick()).thenReturn(true);
        when(click.getAction()).thenReturn(InventoryAction.MOVE_TO_OTHER_INVENTORY);
        guard.click(click);
        verify(click,never()).setCancelled(true);
    }

    /** A number key may take a result, but never into a locked slot. */
    @Test void numberKeyIsAllowedToAnUnlockedSlotAndRefusedToALockedOne() {
        var plugin=mock(MobaPlugin.class); var player=mock(Player.class);
        when(plugin.enrolled(player)).thenReturn(true); when(plugin.unlockedSlots(player)).thenReturn(6);
        var guard=new InventoryGuard(plugin); var top=mock(Inventory.class); var view=mock(InventoryView.class);
        when(view.getTopInventory()).thenReturn(top);

        var allowed=mock(InventoryClickEvent.class);
        when(allowed.getWhoClicked()).thenReturn(player); when(allowed.getView()).thenReturn(view);
        when(allowed.getClickedInventory()).thenReturn(top); when(allowed.getHotbarButton()).thenReturn(2);
        when(allowed.getAction()).thenReturn(InventoryAction.HOTBAR_SWAP);
        guard.click(allowed);
        verify(allowed,never()).setCancelled(true);

        var refused=mock(InventoryClickEvent.class);
        when(refused.getWhoClicked()).thenReturn(player); when(refused.getView()).thenReturn(view);
        when(refused.getClickedInventory()).thenReturn(top); when(refused.getHotbarButton()).thenReturn(7);
        when(refused.getAction()).thenReturn(InventoryAction.HOTBAR_SWAP);
        guard.click(refused);
        verify(refused).setCancelled(true);
    }
}
