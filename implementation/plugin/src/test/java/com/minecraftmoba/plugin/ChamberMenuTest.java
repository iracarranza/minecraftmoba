package com.minecraftmoba.plugin;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The hotbar-as-menu: what a slot means, and what it refuses.
 *
 * All of it is ordinary state, so none of it needs a server. The Bukkit half
 * only turns a view into items and reads a slot back.
 */
class ChamberMenuTest {

    private static ChamberMenu menu() { return ChamberMenu.chamberMenu(); }

    // ---- descent -----------------------------------------------------------

    @Test void choosingASubmenuReplacesTheHotbarWithItsOptions() {
        var m = menu();
        var root = m.view(ChamberMenu.Gate.OPEN);
        assertEquals("Place objective", root.at(0).label());

        var choice = m.choose(0, ChamberMenu.Gate.OPEN);
        assertEquals(ChamberMenu.Kind.SUBMENU, choice.kind());
        assertEquals("objective", m.pageId());
        assertEquals("Outpost", m.view(ChamberMenu.Gate.OPEN).at(1).label());
    }

    @Test void theWayOutIsAlwaysTheLastSlotAndNeverOnTheRoot() {
        // Fixed position, so four pages deep does not move the exit. And the
        // root has nowhere to climb to, so the slot stays empty rather than
        // offering a button that does nothing.
        var m = menu();
        assertNull(m.view(ChamberMenu.Gate.OPEN).at(ChamberMenu.BACK_SLOT), "nothing to go back to");

        m.choose(0, ChamberMenu.Gate.OPEN);
        var back = m.view(ChamberMenu.Gate.OPEN).at(ChamberMenu.BACK_SLOT);
        assertEquals(ChamberMenu.Kind.BACK, back.kind());

        m.choose(ChamberMenu.BACK_SLOT, ChamberMenu.Gate.OPEN);
        assertTrue(m.atRoot());
    }

    @Test void anActionDoesNotDescendBecauseTheCallerOwnsTheVerb() {
        var m = menu();
        m.choose(0, ChamberMenu.Gate.OPEN);
        var choice = m.choose(1, ChamberMenu.Gate.OPEN);

        assertEquals(ChamberMenu.Kind.ACTION, choice.kind());
        assertEquals("outpost", choice.itemId());
        assertEquals("objective", m.pageId(), "a chosen template does not move the hotbar by itself");
    }

    @Test void aProposalIsOpenedByTheCallerAndLeftByTheSameBack() {
        // The menu cannot know a placement now exists; the thing that proposed
        // it can. But leaving it must not be a second gesture.
        var m = menu();
        m.choose(0, ChamberMenu.Gate.OPEN);
        m.open("pending");

        var v = m.view(ChamberMenu.Gate.OPEN);
        assertEquals("Place", v.at(1).label());
        m.choose(ChamberMenu.BACK_SLOT, ChamberMenu.Gate.OPEN);
        assertEquals("objective", m.pageId());
    }

    @Test void anEmptySlotIsNotAChoice() {
        var m = menu();
        var choice = m.choose(6, ChamberMenu.Gate.OPEN);
        assertNull(choice.kind());
        assertTrue(m.atRoot());
    }

    // ---- refusal -----------------------------------------------------------

    @Test void anUnavailableItemSaysWhyRatherThanDoingNothing() {
        // A silent no-op reads as a broken button, and the tester's next move
        // is to press it again.
        var m = menu();
        ChamberMenu.Gate noHistory = id ->
                Set.of("undo", "redo").contains(id) ? "Nothing to undo in this chamber." : null;

        var v = m.view(noHistory);
        assertEquals("Undo", v.at(4).label(), "and it is still shown, in its place");
        assertNotNull(v.refusalAt(4));

        var choice = m.choose(4, noHistory);
        assertTrue(choice.refused());
        assertEquals("Nothing to undo in this chamber.", choice.refusal());
    }

    @Test void aRefusedSubmenuDoesNotDescend() {
        var m = menu();
        ChamberMenu.Gate closed = id -> id.equals("objective") ? "Take a chamber first." : null;
        assertTrue(m.choose(0, closed).refused());
        assertTrue(m.atRoot(), "a refusal that still moved would strand the tester");
    }

    @Test void refusalIsAskedFreshEachTimeRatherThanBakedIntoTheHotbar() {
        var m = menu();
        boolean[] has = {false};
        ChamberMenu.Gate gate = id -> id.equals("undo") && !has[0] ? "Nothing to undo." : null;

        assertNotNull(m.view(gate).refusalAt(4));
        has[0] = true;
        assertNull(m.view(gate).refusalAt(4), "undo lights up the moment there is history");
    }

    // ---- what may not be built ---------------------------------------------

    @Test void aPageThatWouldNotFitTheHotbarIsRefusedNotTruncated() {
        // A ninth entry is a tool the tester cannot reach and cannot learn is
        // missing. Dropping it quietly is the worst of the options.
        var items = new ChamberMenu.Item[ChamberMenu.MAX_ENTRIES + 1];
        for (int i = 0; i < items.length; i++) items[i] = ChamberMenu.Item.action("a" + i, "A" + i);

        var e = assertThrows(IllegalArgumentException.class,
                () -> new ChamberMenu("root").page("root", "Too big", items));
        assertTrue(e.getMessage().contains("hotbar"));
    }

    @Test void sessionVerbsAreRefusedAHotbarSlotAtAll() {
        // "Return to lab" and "test as player" end or hand over the session,
        // and a hotbar is a thing you scroll past by accident. They live in the
        // inventory, where acting costs an open and a click.
        for (String verb : ChamberMenu.SESSION_VERBS)
            assertThrows(IllegalArgumentException.class,
                    () -> new ChamberMenu("root").page("root", "Chamber",
                            ChamberMenu.Item.action(verb, "x")),
                    verb + " must not be placeable on the hotbar");
    }

    @Test void theShippedMenuDeclaresEverySubmenuItOffers() {
        // A submenu whose page was never declared is a dead slot that only a
        // tester finds, and only by pressing it.
        var m = menu();
        for (String page : new String[]{"objective", "renewable", "route"}) {
            var fresh = menu();
            int slot = indexOf(fresh, page);
            assertTrue(slot >= 0, page + " is on the root");
            assertDoesNotThrow(() -> fresh.choose(slot, ChamberMenu.Gate.OPEN));
            assertEquals(page, fresh.pageId());
        }
        assertTrue(m.atRoot());
    }

    private static int indexOf(ChamberMenu m, String id) {
        var v = m.view(ChamberMenu.Gate.OPEN);
        for (int i = 0; i < ChamberMenu.SLOTS; i++)
            if (v.at(i) != null && v.at(i).id().equals(id)) return i;
        return -1;
    }
}
