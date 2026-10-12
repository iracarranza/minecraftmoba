package com.minecraftmoba.plugin;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

/** The loadout model, with strings standing in for item stacks. */
class LoadoutEditorLayoutTest {
    @Test void theWindowIsSixRows() { assertEquals(54, LoadoutEditorLayout.SIZE); }

    @Test void everyLoadoutSlotIsAnEditorSlotWithTheSameNumberExceptTheReservedOne() {
        for (int i = 0; i < Loadout.SIZE; i++) {
            var kind = LoadoutEditorLayout.kind(i);
            assertEquals(Loadout.reserved(i) ? LoadoutEditorLayout.Kind.RESERVED : LoadoutEditorLayout.Kind.ITEM, kind, "slot " + i);
        }
    }

    @Test void onlyItemSlotsAreEditable() {
        for (int i = 0; i < LoadoutEditorLayout.SIZE; i++)
            assertEquals(LoadoutEditorLayout.kind(i) == LoadoutEditorLayout.Kind.ITEM, LoadoutEditorLayout.editable(i), "slot " + i);
    }

    @Test void theBottomRowCarriesTheControls() {
        for (int s : new int[]{LoadoutEditorLayout.SELECT, LoadoutEditorLayout.FILL_ME, LoadoutEditorLayout.FILL_DUMMY,
                LoadoutEditorLayout.CLEAR, LoadoutEditorLayout.ITEMS, LoadoutEditorLayout.DONE})
            assertEquals(LoadoutEditorLayout.Kind.CONTROL, LoadoutEditorLayout.kind(s));
        assertEquals(6, java.util.stream.IntStream.range(0, 54).filter(i -> LoadoutEditorLayout.kind(i) == LoadoutEditorLayout.Kind.CONTROL).count());
    }

    @Test void thereAreThirtyNineEditableSlotsAndOneReserved() {
        long items = java.util.stream.IntStream.range(0, 54).filter(LoadoutEditorLayout::editable).count();
        assertEquals(39, items, "36 inventory slots less the reserved one, plus four armor pieces; the offhand is not a loadout slot");
        assertEquals(1, java.util.stream.IntStream.range(0, 54).filter(i -> LoadoutEditorLayout.kind(i) == LoadoutEditorLayout.Kind.RESERVED).count());
    }

    @Test void armorSlotsAreLabelled() {
        assertEquals("Helmet", LoadoutEditorLayout.armorLabel(Loadout.HELMET));
        assertEquals("Boots", LoadoutEditorLayout.armorLabel(Loadout.BOOTS));
        assertNull(LoadoutEditorLayout.armorLabel(40));
        assertNull(LoadoutEditorLayout.armorLabel(3));
    }

    @Test void slotsOutsideTheWindowAreRefused() {
        assertThrows(IllegalArgumentException.class, () -> LoadoutEditorLayout.kind(54));
        assertThrows(IllegalArgumentException.class, () -> LoadoutEditorLayout.kind(-1));
    }
}
