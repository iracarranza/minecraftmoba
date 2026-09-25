package com.minecraftmoba.plugin;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class ColosseumBanTest {
    @Test void duplicateCollisionDoesNotSpendSecondPlayersBan() {
        UUID a=UUID.randomUUID(), b=UUID.randomUUID();
        ClassDraft draft=new ClassDraft(ClassDraft.Rules.provisional(2),List.of("a","b","c"),Map.of(Team.NORTH,List.of(a,b),Team.SOUTH,List.of()),Team.NORTH);
        assertNull(draft.ban(a,"a"));
        assertNotNull(draft.ban(a,"b"));
        assertNotNull(draft.ban(b,"a"));
        assertFalse(draft.personalBans().containsKey(b));
        assertNull(draft.ban(b,"b"));
        assertEquals(ClassDraft.Phase.PICK,draft.phase());
    }
}
