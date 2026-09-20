package com.minecraftmoba.plugin;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Baseline crafting must work for every player, at every inventory capacity.
 *
 * The first manual playtest hit a blocker: with locked slots, every click on
 * the player's own 2x2 crafting grid and its result was cancelled, so no one
 * could turn a log into planks. The cause was classification -- InventoryGuard
 * treated CRAFTING as a temporary menu whose contents return on close, like a
 * workbench or an anvil.
 *
 * The existing guard test could not catch it: it stubbed the classification
 * seam with a null type, so it never distinguished CRAFTING from WORKBENCH.
 * These assert the classification itself, on real InventoryType values.
 */
class CraftingAccessTest {

    @Test void theOwnInventoryCraftingGridIsNotAGuardedTemporaryMenu() {
        // The regression: guarding this is what blocked baseline crafting.
        assertFalse(InventoryGuard.guardsTemporaryMenu("CRAFTING"),
                "the player's own 2x2 grid must stay usable at partial capacity");
    }

    @Test void realTemporaryMenusAreStillGuarded() {
        for (String t : new String[]{"WORKBENCH", "ANVIL", "ENCHANTING", "GRINDSTONE",
                "SMITHING", "LOOM", "CARTOGRAPHY", "STONECUTTER", "MERCHANT"}) {
            assertTrue(InventoryGuard.guardsTemporaryMenu(t), t + " must remain guarded");
        }
    }

    @Test void craftingStillReturnsItsContentsOnClose() {
        // The exemption is about interaction, not about pretending the grid
        // does not empty itself on close. safeToReduce still checks it before
        // lowering a player's capacity.
        assertTrue(InventoryGuard.returnsOnClose("CRAFTING"));
    }

    @Test void ordinaryStorageIsNeitherGuardedNorReturned() {
        for (String t : new String[]{"CHEST", "HOPPER", "BARREL", "SHULKER_BOX"}) {
            assertFalse(InventoryGuard.guardsTemporaryMenu(t));
            assertFalse(InventoryGuard.returnsOnClose(t));
        }
    }
}
