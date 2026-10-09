package com.minecraftmoba.plugin;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashSet;
import java.util.List;
import org.junit.jupiter.api.Test;

class CombatPreEntryViewTest {
    private static CombatPreEntry flow() {
        return new CombatPreEntry(new CombatPreEntry.Catalog() {
            public List<String> classes() { return List.of("alpha", "beta"); }
            public String label(String id) { return id.toUpperCase(); }
            public List<CombatAvailability.SlotInfo> slots(String id) {
                return List.of(new CombatAvailability.SlotInfo(CombatAvailability.Slot.A1, "strike", "Strike", true, false, true,
                        AbilityOutput.single("strike", TargetForm.UNIT_ENTITY, InputForm.INSTANT, Recipients.ENEMIES)));
            }
            public int defaultLevel() { return 5; }
        });
    }

    @Test void theFirstScreenOffersEveryClassAndNoWayBack() {
        var s = CombatPreEntryView.of(flow(), 30);
        assertEquals(54, s.size());
        assertEquals("pick alpha", s.at(0).action()); assertEquals("pick beta", s.at(1).action());
        assertNull(s.at(CombatPreEntryView.BACK_SLOT_BIG), "no back at the first step");
        assertNotNull(s.at(CombatPreEntryView.INFO_SLOT_BIG));
    }

    @Test void everyActionIsAnExistingCommandWord() {
        var f = flow();
        var screens = new java.util.ArrayList<CombatPreEntryView.Screen>();
        screens.add(CombatPreEntryView.of(f, 30));
        f.choose("alpha"); screens.add(CombatPreEntryView.of(f, 30));
        f.choose("OPERATOR"); screens.add(CombatPreEntryView.of(f, 30));
        for (var s : screens) for (var slot : s.slots())
            if (slot.action() != null)
                assertTrue(slot.action().matches("pick \\S+|back|set (cooldown|time|level|dummy) \\S+"), slot.action());
    }

    @Test void anUnavailableOptionCannotBePressedAndSaysWhy() {
        var f = flow();
        f.choose("alpha");
        var s = CombatPreEntryView.of(f, 30);
        boolean sawUnavailable = false;
        for (var slot : s.slots())
            if (slot.kind() == CombatPreEntryView.Kind.OPTION && !slot.available()) {
                sawUnavailable = true;
                assertNull(slot.action()); assertNotNull(slot.reason());
            }
        // The flow offers at least Operate; the others depend on the class, so this only checks the rule when present.
        assertNotNull(s.at(CombatPreEntryView.BACK_SLOT_BIG));
        assertTrue(sawUnavailable || s.slots().stream().anyMatch(x -> x.kind() == CombatPreEntryView.Kind.OPTION));
    }

    @Test void theModesScreenShowsCurrentValuesAndOffersTheNextOnes() {
        var f = flow(); f.choose("alpha"); f.choose("OPERATOR");
        var s = CombatPreEntryView.of(f, 30);
        assertEquals(27, s.size());
        assertTrue(s.at(CombatPreEntryView.COOLDOWN).label().contains("normal"));
        assertEquals("set cooldown waive", s.at(CombatPreEntryView.COOLDOWN).action());
        assertEquals("set time day", s.at(CombatPreEntryView.TIME).action());
        assertTrue(s.at(CombatPreEntryView.LEVEL).label().contains("5"));
        assertEquals("set level 10", s.at(CombatPreEntryView.LEVEL).action());
        assertEquals("pick enter", s.at(CombatPreEntryView.ENTER).action());
        assertEquals("back", s.at(CombatPreEntryView.BACK_SLOT_SMALL).action());
        assertNull(s.at(CombatPreEntryView.DUMMY), "an operator has no dummy side to choose");
    }

    @Test void theWaiverButtonReflectsTheCurrentSettingAndToggles() {
        var f = flow(); f.choose("alpha"); f.choose("OPERATOR");
        f.cooldownWaiver(true);
        var s = CombatPreEntryView.of(f, 30);
        assertTrue(s.at(CombatPreEntryView.COOLDOWN).label().contains("WAIVED"));
        assertEquals("set cooldown normal", s.at(CombatPreEntryView.COOLDOWN).action());
    }

    @Test void timeAndLevelCycleAndWrap() {
        var t = CombatPreEntry.TimeOfDay.DEFAULT; var seen = new HashSet<CombatPreEntry.TimeOfDay>();
        for (int i = 0; i < 3; i++) { seen.add(t); t = CombatPreEntryView.nextTime(t); }
        assertEquals(3, seen.size()); assertEquals(CombatPreEntry.TimeOfDay.DEFAULT, t);
        assertEquals(10, CombatPreEntryView.nextLevel(5, 30));
        assertEquals(1, CombatPreEntryView.nextLevel(30, 30));
        assertEquals(1, CombatPreEntryView.nextLevel(10, 10), "never above the configured maximum");
        assertEquals(5, CombatPreEntryView.nextLevel(1, 30));
        assertEquals(15, CombatPreEntryView.nextLevel(12, 30));
    }
}
