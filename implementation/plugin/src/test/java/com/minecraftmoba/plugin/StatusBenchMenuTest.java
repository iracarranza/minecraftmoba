package com.minecraftmoba.plugin;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class StatusBenchMenuTest {
    @Test void everyVerbHasAnIntentAndADeckButton() {
        var deck = StatusBenchMenu.deck();
        for (String v : StatusBenchMenu.VERBS) {
            assertNotNull(StatusBenchMenu.intent(v), v);
            assertNotNull(deck.button(v), v);
        }
        assertNull(StatusBenchMenu.intent("nonsense"));
    }

    @Test void leavingIsNotOnTheHotbarMenuButIsOnTheDeck() {
        for (var item : StatusBenchMenu.menu().view(ChamberMenu.Gate.OPEN).slots())
            if (item != null) assertNotEquals(StatusBenchMenu.LEAVE, item.id());
        assertNotNull(StatusBenchMenu.deck().button(StatusBenchMenu.LEAVE));
    }

    @Test void theMenuFitsTheHotbarAndEveryActionIsAConsoleVerb() {
        for (var item : StatusBenchMenu.menu().view(ChamberMenu.Gate.OPEN).slots())
            if (item != null && item.kind() == ChamberMenu.Kind.ACTION) assertTrue(StatusBenchMenu.VERBS.contains(item.id()), item.id());
    }

    @Test void nothingWorksUntilTheTesterIsInTheBench() {
        assertNotNull(StatusBenchMenu.refusal("apply", new StatusBenchMenu.State(false)));
        assertNull(StatusBenchMenu.refusal("apply", new StatusBenchMenu.State(true)));
    }

    @Test void theDeckStandsClearOfTheArenaFloor() {
        var box = StatusBenchMenu.deck().deck();
        assertTrue(box.maxX() < StatusArena.MIN_X, "the deck is west of the floor, not on it");
    }

    @Test void durationsCycleAndSelfApplicationIsCapped() {
        int d = StatusArena.DURATIONS.get(0);
        for (int i = 0; i < StatusArena.DURATIONS.size(); i++) d = StatusArena.nextDuration(d);
        assertEquals(StatusArena.DURATIONS.get(0), d);
        assertEquals(StatusArena.SELF_MAX_TICKS, StatusArena.selfTicks(1200));
        assertEquals(60, StatusArena.selfTicks(60));
        assertEquals("10s", StatusArena.describe(200));
        assertEquals("0.5s", StatusArena.describe(10));
    }

    @Test void theArenaPlacesEveryoneOnTheFloor() {
        assertTrue(StatusArena.onFloor((int) StatusArena.SOURCE_X, (int) StatusArena.SOURCE_Z));
        assertTrue(StatusArena.onFloor((int) StatusArena.SUBJECT_X, (int) StatusArena.SUBJECT_Z));
        assertTrue(StatusArena.onFloor((int) StatusArena.TESTER_X, (int) StatusArena.TESTER_Z));
        assertFalse(StatusArena.onFloor(StatusArena.MAX_X + 1, 0));
    }
}
