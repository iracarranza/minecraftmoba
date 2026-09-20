package com.minecraftmoba.plugin;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Baseline crafting must work for every player at every inventory capacity.
 *
 * The first manual playtest found four things broken below full capacity: the
 * 2x2 grid, the crafting table, shift-clicking a result out, and number-keying
 * a result out. All four came from the same preventive block, which cancelled
 * whole classes of interaction because they *might* put an item in a locked
 * slot.
 *
 * Capacity is now an invariant maintained by repair, so the rule the guard
 * enforces is only about slot indices. These assert that rule directly.
 */
class CraftingAccessTest {

    @Test void lockedStorageIsExactlyTheSlotsAtOrAboveCapacity() {
        assertFalse(InventoryGuard.isLockedStorageSlot(0, 9));
        assertFalse(InventoryGuard.isLockedStorageSlot(8, 9));
        assertTrue(InventoryGuard.isLockedStorageSlot(9, 9));
        assertTrue(InventoryGuard.isLockedStorageSlot(35, 9));
    }

    @Test void fullCapacityLocksNothing() {
        for (int slot = 0; slot < 36; slot++)
            assertFalse(InventoryGuard.isLockedStorageSlot(slot, 36), "slot " + slot);
    }

    @Test void armourAndOffhandAreNotLockedStorage() {
        // Slots 36-40 are equipment. Capacity does not govern them, and treating
        // them as locked storage would block armour at low levels.
        for (int slot = 36; slot <= 40; slot++)
            assertFalse(InventoryGuard.isLockedStorageSlot(slot, 9), "slot " + slot);
    }

    @Test void craftingGridAndResultAreNotPlayerStorageSlots() {
        // A click outside the player's inventory reports a negative converted
        // slot or belongs to the top inventory. Either way it is not locked
        // storage, which is what makes the 2x2 grid, a crafting table, and
        // taking a result all ordinary vanilla again.
        assertFalse(InventoryGuard.isLockedStorageSlot(-1, 9));
    }

    @Test void craftingStillReturnsItsContentsOnClose() {
        // safeToReduce still checks an open grid before lowering capacity, so
        // this classification must survive even though it no longer cancels
        // clicks.
        assertTrue(InventoryGuard.returnsOnClose("CRAFTING"));
        assertTrue(InventoryGuard.returnsOnClose("WORKBENCH"));
        assertFalse(InventoryGuard.returnsOnClose("CHEST"));
    }
}
