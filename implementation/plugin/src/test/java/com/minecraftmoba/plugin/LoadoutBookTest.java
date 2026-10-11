package com.minecraftmoba.plugin;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

/** The loadout model, with strings standing in for item stacks. */
class LoadoutBookTest {
    @Test void aBookHasFiveNamedLoadoutsAndTheFirstIsSelected() {
        var b = new LoadoutBook<String>();
        assertEquals(5, LoadoutBook.SLOTS);
        assertEquals("Loadout A", LoadoutBook.name(0));
        assertEquals("Loadout E", LoadoutBook.name(4));
        assertEquals(0, b.selected());
        assertEquals("Loadout A", b.selectedName());
        assertTrue(b.current().isEmpty());
    }

    @Test void nextWrapsAround() {
        var b = new LoadoutBook<String>();
        for (int i = 1; i <= 4; i++) assertEquals(i, b.next());
        assertEquals(0, b.next());
    }

    @Test void aLoadoutIsStoredWhereItWasSetAndNotWhereItWasSelected() {
        var b = new LoadoutBook<String>();
        b.set(2, Loadout.<String>empty().with(0, "x"));
        assertTrue(b.current().isEmpty());
        assertEquals("x", b.get(2).at(0));
        b.select(2);
        assertEquals("x", b.current().at(0));
    }

    @Test void settingNullEmptiesThatLoadout() {
        var b = new LoadoutBook<String>();
        b.set(1, Loadout.<String>empty().with(0, "x"));
        b.set(1, null);
        assertTrue(b.get(1).isEmpty());
    }

    @Test void indexesOutsideTheBookAreRefused() {
        var b = new LoadoutBook<String>();
        assertThrows(IllegalArgumentException.class, () -> b.get(5));
        assertThrows(IllegalArgumentException.class, () -> b.select(-1));
        assertThrows(IllegalArgumentException.class, () -> LoadoutBook.name(9));
    }
}
