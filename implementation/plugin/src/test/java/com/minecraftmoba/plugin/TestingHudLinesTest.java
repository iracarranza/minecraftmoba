package com.minecraftmoba.plugin;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashSet;
import org.junit.jupiter.api.Test;

class TestingHudLinesTest {
    private static TestingHudLines.Vitals v(String label, double hp, double max, double armor, double tough) {
        return new TestingHudLines.Vitals(label, hp, max, armor, tough);
    }

    @Test void aBarFillsInProportionAndIsAlwaysTenCellsWide() {
        assertEquals("██████████", TestingHudLines.bar(150, 150));
        assertEquals("█████░░░░░", TestingHudLines.bar(75, 150));
        assertEquals("░░░░░░░░░░", TestingHudLines.bar(0, 150));
        for (double hp : new double[]{0, 1, 37.5, 99, 150, 400}) assertEquals(10, TestingHudLines.bar(hp, 150).length(), "hp " + hp);
    }

    @Test void aBarNeverReadsEmptyWhileAliveAndNeverOverflows() {
        assertEquals("█░░░░░░░░░", TestingHudLines.bar(1, 150), "alive with a sliver is one cell, not none");
        assertEquals("██████████", TestingHudLines.bar(400, 150), "more than max is full");
        assertEquals("░░░░░░░░░░", TestingHudLines.bar(10, 0), "no maximum is an empty bar, not a crash");
    }

    @Test void theLinesNameTheLoadoutBothCombatantsAndTheirArmor() {
        var lines = TestingHudLines.lines("Loadout B", 6, v("You", 120, 150, 15, 3), v("Dummy", 90, 100, 8, 2));
        assertEquals("Testing: Loadout B (6 items)", lines.get(0));
        var text = String.join("\n", lines);
        assertTrue(text.contains("You  120/150"));
        assertTrue(text.contains("Dummy  90/100"));
        assertTrue(text.contains("Armor 15  Tough 3"));
        assertTrue(text.contains("Armor 8  Tough 2"));
    }

    @Test void oneItemIsNotPlural() {
        assertEquals("Testing: Loadout A (1 item)", TestingHudLines.lines("Loadout A", 1, v("You", 1, 1, 0, 0), null).get(0));
    }

    @Test void withNoDummyOnlyTheTestersBlockShows() {
        var lines = TestingHudLines.lines("Loadout A", 0, v("You", 20, 20, 0, 0), null);
        assertEquals(5, lines.size());
        assertFalse(String.join("\n", lines).contains("Dummy"));
    }

    @Test void everyLineIsDistinctBecauseASidebarKeysOnItsText() {
        // Two combatants with identical numbers must still produce different lines.
        var lines = TestingHudLines.lines("Loadout A", 3, v("You", 100, 100, 5, 1), v("Dummy", 100, 100, 5, 1));
        assertEquals(lines.size(), new HashSet<>(lines).size());
    }

    @Test void numbersShowWholeWhenWholeAndOneDecimalOtherwise() {
        assertEquals("15", TestingHudLines.number(15.0));
        assertEquals("7.5", TestingHudLines.number(7.5));
        assertEquals("0", TestingHudLines.number(0));
    }

    @Test void fitsAVanillaSidebar() {
        assertTrue(TestingHudLines.lines("Loadout A", 40, v("You", 1, 2, 3, 4), v("Dummy", 1, 2, 3, 4)).size() <= 15);
    }
}
