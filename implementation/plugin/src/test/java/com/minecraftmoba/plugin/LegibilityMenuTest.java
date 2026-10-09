package com.minecraftmoba.plugin;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashSet;
import org.junit.jupiter.api.Test;

class LegibilityMenuTest {
    @Test void theMenuFitsTheHotbarAndHasNoSessionVerb() { LegibilityMenu.menu(); }

    @Test void everyHotbarActionIsAConsoleVerbWithAnIntent() {
        var view = LegibilityMenu.menu().view(ChamberMenu.Gate.OPEN);
        for (var item : view.slots())
            if (item != null && item.kind() == ChamberMenu.Kind.ACTION) {
                assertTrue(LegibilityMenu.VERBS.contains(item.id()), item.id());
                assertNotNull(LegibilityMenu.intent(item.id()), item.id());
            }
        assertNull(LegibilityMenu.intent("nonsense"));
    }

    @Test void leaveIsOnTheDeckButNotTheHotbar() {
        assertTrue(LegibilityMenu.VERBS.contains("leave"));
        for (var item : LegibilityMenu.menu().view(ChamberMenu.Gate.OPEN).slots())
            if (item != null) assertNotEquals("leave", item.id());
    }

    @Test void nothingWorksOutsideTheBench() {
        var out = new LegibilityMenu.State(false);
        for (String v : LegibilityMenu.VERBS) assertNotNull(LegibilityMenu.refusal(v, out), v);
        for (String v : LegibilityMenu.VERBS) assertNull(LegibilityMenu.refusal(v, new LegibilityMenu.State(true)), v);
    }

    @Test void theDeckStandsBesideTheTrackNotOnIt() {
        var deck = LegibilityMenu.deck();
        var track = new DeckLayout.Box(LegibilityTrack.MIN_X, LegibilityTrack.FLOOR_Y, LegibilityTrack.MIN_Z,
                LegibilityTrack.MAX_X, LegibilityTrack.FLOOR_Y + LegibilityTrack.PILLAR_HEIGHT, LegibilityTrack.MAX_Z);
        assertFalse(deck.deck().overlaps(track));
        assertEquals(LegibilityTrack.FLOOR_Y, deck.deck().minY());
    }

    @Test void buttonsAreOnTheDeckTwoApartAndLeaveIsSetApart() {
        var d = LegibilityMenu.deck();
        var seen = new HashSet<DeckLayout.Pos>();
        DeckLayout.Button prev = null;
        for (var b : d.buttons()) {
            assertTrue(seen.add(b.at()));
            assertTrue(d.deck().contains(new DeckLayout.Pos(b.at().x(), d.deck().minY(), b.at().z())), b.verb());
            if (prev != null) assertTrue(b.at().z() - prev.at().z() >= 2, b.verb());
            prev = b;
        }
        var b = d.buttons();
        assertEquals("leave", b.get(b.size() - 1).verb());
        assertTrue(b.get(b.size() - 1).at().z() - b.get(b.size() - 2).at().z() >= 4);
        assertEquals(LegibilityMenu.VERBS.size(), b.size());
        assertNotNull(d.button("mark"));
    }
}
