package com.minecraftmoba.plugin;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

/** The loadout model, with strings standing in for item stacks. */
class LoadoutTest {
    @Test void anEmptyLoadoutHoldsNothingAcrossEveryDeclaredSlot() {
        var l = Loadout.<String>empty();
        assertEquals(0, l.count());
        assertTrue(l.isEmpty());
        assertEquals(40, l.items().size());
        for (int i = 0; i < Loadout.SIZE; i++) assertNull(l.at(i));
    }

    @Test void slotNumberingIsBukkitsOwn() {
        assertEquals(36, Loadout.INVENTORY);
        assertEquals(36, Loadout.HELMET);
        assertEquals(39, Loadout.BOOTS);
        assertEquals(40, Loadout.SIZE, "36 inventory slots and four armor pieces; the offhand belongs to the plugin's map");
        assertEquals(8, Loadout.EXIT_SLOT, "the last hotbar slot is the reserved one, so the exit item can be held");
    }

    @Test void withReturnsACopyAndLeavesTheOriginalAlone() {
        var a = Loadout.<String>empty();
        var b = a.with(3, "sword");
        assertNull(a.at(3));
        assertEquals("sword", b.at(3));
        assertEquals(1, b.count());
        assertEquals(b, a.with(3, "sword"));
        assertNotEquals(a, b);
    }

    @Test void theReservedSlotRefusesAnItemButAcceptsClearing() {
        var l = Loadout.<String>empty();
        assertThrows(IllegalArgumentException.class, () -> l.with(Loadout.EXIT_SLOT, "x"));
        assertNull(l.with(Loadout.EXIT_SLOT, null).at(Loadout.EXIT_SLOT));
    }

    @Test void ofDropsWhateverTheReservedSlotHeld() {
        var items = new ArrayList<>(Collections.<String>nCopies(Loadout.SIZE, null));
        items.set(Loadout.EXIT_SLOT, "real item");
        items.set(0, "kept");
        var l = Loadout.of(items);
        assertNull(l.at(Loadout.EXIT_SLOT));
        assertEquals("kept", l.at(0));
    }

    @Test void ofRefusesTheWrongNumberOfSlots() {
        assertThrows(IllegalArgumentException.class, () -> Loadout.of(List.of("a")));
    }

    @Test void slotsOutsideTheRangeAreRefused() {
        var l = Loadout.<String>empty();
        assertThrows(IllegalArgumentException.class, () -> l.at(-1));
        assertThrows(IllegalArgumentException.class, () -> l.at(40));
        assertThrows(IllegalArgumentException.class, () -> l.with(40, "x"));
    }

    @Test void mapTransformsItemsAndKeepsEmptySlotsEmpty() {
        var l = Loadout.<String>empty().with(0, "ab").with(39, "c").map(String::length);
        assertEquals(2, l.at(0));
        assertEquals(1, l.at(39));
        assertNull(l.at(1));
    }

    @Test void theItemsListCannotBeUsedToChangeTheLoadout() {
        var l = Loadout.<String>empty().with(0, "a");
        assertThrows(UnsupportedOperationException.class, () -> l.items().set(1, "b"));
    }
}
