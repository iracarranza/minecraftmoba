package com.minecraftmoba.plugin;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashSet;
import org.junit.jupiter.api.Test;

class CombatMenuTest {
    private static CombatMenu.State s(boolean op, boolean rec, boolean take, boolean replay) {
        return new CombatMenu.State(true, op, rec, take, replay);
    }

    @Test void everyPageFitsAndNoSessionVerbIsOnTheHotbar() {
        CombatMenu.menu();   // construction refuses a page over eight entries or a session verb
    }

    @Test void everyActionIdOnTheMenuHasAnIntent() {
        var m = CombatMenu.menu();
        for (String page : new String[]{"root", "record", "replay"}) {
            m.reset(); if (!page.equals("root")) m.open(page);
            for (var item : m.view(ChamberMenu.Gate.OPEN).slots())
                if (item != null && item.kind() == ChamberMenu.Kind.ACTION)
                    assertNotNull(CombatMenu.intent(item.id()), item.id());
        }
    }

    @Test void everyConsoleVerbHasAnIntentAndTheHotbarActionsAreAllOnTheConsole() {
        for (String v : CombatMenu.CONSOLE_VERBS) assertNotNull(CombatMenu.intent(v), v);
        var m = CombatMenu.menu();
        for (String page : new String[]{"root", "record", "replay"}) {
            m.reset(); if (!page.equals("root")) m.open(page);
            for (var item : m.view(ChamberMenu.Gate.OPEN).slots())
                if (item != null && item.kind() == ChamberMenu.Kind.ACTION)
                    assertTrue(CombatMenu.CONSOLE_VERBS.contains(item.id()), item.id() + " is on the hotbar but not the console");
        }
    }

    @Test void leaveIsNotAHotbarVerb() {
        var m = CombatMenu.menu();
        for (String page : new String[]{"root", "record", "replay"}) {
            m.reset(); if (!page.equals("root")) m.open(page);
            for (var item : m.view(ChamberMenu.Gate.OPEN).slots()) if (item != null) assertNotEquals("leave", item.id());
        }
    }

    @Test void nothingWorksOutsideTheChamber() {
        var out = new CombatMenu.State(false, true, false, false, false);
        for (String v : CombatMenu.CONSOLE_VERBS) assertNotNull(CombatMenu.refusal(v, out), v);
    }

    @Test void castIsForRecipientAndObserverOnly() {
        assertNotNull(CombatMenu.refusal("cast", s(true, false, false, false)));
        assertNull(CombatMenu.refusal("cast", s(false, false, false, false)));
    }

    @Test void recordingRules() {
        assertNull(CombatMenu.refusal("record.start", s(true, false, false, false)));
        assertNotNull(CombatMenu.refusal("record.start", s(false, false, false, false)));   // not an operator
        assertNotNull(CombatMenu.refusal("record.start", s(true, true, false, false)));     // already recording
        assertNotNull(CombatMenu.refusal("record.start", s(true, false, true, true)));      // ghost running
        assertNull(CombatMenu.refusal("record.stop", s(true, true, false, false)));
        assertNotNull(CombatMenu.refusal("record.stop", s(true, false, false, false)));
    }

    @Test void replayRules() {
        assertNotNull(CombatMenu.refusal("replay.once", s(true, false, false, false)));     // no take
        assertNull(CombatMenu.refusal("replay.once", s(true, false, true, false)));
        assertNotNull(CombatMenu.refusal("replay.loop", s(true, true, true, false)));       // still recording
        assertNotNull(CombatMenu.refusal("replay.once", s(true, false, true, true)));       // already replaying
        assertNull(CombatMenu.refusal("replay.stop", s(true, false, true, true)));
        assertNotNull(CombatMenu.refusal("replay.stop", s(true, false, true, false)));
    }

    @Test void measurementAndResetAlwaysWorkInside() {
        for (String v : new String[]{"log", "clear", "reset", "leave"})
            assertNull(CombatMenu.refusal(v, s(true, true, false, true)), v);
    }

    @Test void consoleButtonsAreOnTheDeckApartFromTheSlabAndEachOther() {
        var c = CombatConsole.of();
        assertFalse(c.deck().overlapsSlab());
        var seen = new HashSet<CombatConsole.Pos>();
        CombatConsole.Button prev = null;
        for (var b : c.buttons()) {
            assertTrue(seen.add(b.at()));
            assertTrue(c.deck().contains(new CombatConsole.Pos(b.at().x(), c.deck().minY(), b.at().z())), b.verb());
            assertEquals(CombatSlab.FLOOR_Y + 1, b.at().y());
            if (prev != null) assertTrue(b.at().z() - prev.at().z() >= 2, "labels would overlap at " + b.verb());
            prev = b;
        }
        assertEquals(CombatMenu.CONSOLE_VERBS.size(), c.buttons().size());
    }

    @Test void leaveIsSetApartFromTheWorkingButtons() {
        var c = CombatConsole.of();
        var b = c.buttons();
        int gapToLeave = b.get(b.size() - 1).at().z() - b.get(b.size() - 2).at().z();
        assertTrue(gapToLeave >= 4, "leave is " + gapToLeave + " from the nearest button");
        assertEquals("leave", b.get(b.size() - 1).verb());
    }

    @Test void deckIsFlushWithTheSlabFloorAndDoesNotTouchTheControlBand() {
        var d = CombatConsole.of().deck();
        assertEquals(CombatSlab.FLOOR_Y, d.minY());
        assertTrue(d.maxX() < CombatSlab.minX());
    }
}
