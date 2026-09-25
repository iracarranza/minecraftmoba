package com.minecraftmoba.plugin;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class MapDraftTest {
    private List<MapDraft.Option> options() {
        var out = new ArrayList<MapDraft.Option>();
        for (int i = 0; i < 6; i++)
            out.add(new MapDraft.Option("m" + i, i < 2 ? "island" : "landmass",
                    "wide", "rich", i < 2 ? "gentle" : "mixed", "thumb" + i));
        return out;
    }

    @Test void sixBoardUsesA1B1A1ThenBPick() {
        var d = new MapDraft(MapDraft.Rules.sixBoard(), options(), Team.NORTH);
        assertEquals(Set.of("island", "landmass"), d.revealedTypes());
        assertNull(d.strike(Team.NORTH, "m0"));
        assertNull(d.strike(Team.SOUTH, "m1"));
        assertNull(d.strike(Team.NORTH, "m2"));
        assertEquals(MapDraft.Phase.PICK, d.phase());
        assertEquals(Team.SOUTH, d.onTurn());
        assertNull(d.pick(Team.SOUTH, "m3"));
        assertEquals("m3", d.chosen().id());
        assertEquals(MapDraft.Phase.COMPLETE, d.phase());
    }

    @Test void rejectedActionsDoNotChangeState() {
        var d = new MapDraft(MapDraft.Rules.sixBoard(), options(), Team.NORTH);
        assertEquals("not your turn", d.strike(Team.SOUTH, "m0"));
        assertEquals("no such map option", d.strike(Team.NORTH, "missing"));
        assertEquals(6, d.board().size());
    }

    @Test void struckOptionsReturnToPoolConceptuallyAndAreNotPlayed() {
        var d = new MapDraft(MapDraft.Rules.sixBoard(), options(), Team.NORTH);
        d.strike(Team.NORTH, "m0"); d.strike(Team.SOUTH, "m1"); d.strike(Team.NORTH, "m2");
        assertEquals(3, d.board().size());
        assertEquals(Set.of("m0", "m1", "m2"), new HashSet<>(d.struck().stream().map(MapDraft.Option::id).toList()));
        assertNull(d.pick(Team.SOUTH, "m3"));
        assertFalse(d.chosen().id().equals("m0"));
    }
}
