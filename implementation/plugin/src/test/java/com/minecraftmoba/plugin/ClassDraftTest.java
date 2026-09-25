package com.minecraftmoba.plugin;

import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The five decided properties of the class draft, and nothing the spec left open.
 *
 * PRE_MATCH_SELECTION_FLOW.md decides that classes are drafted first and
 * blind, that every player may ban and bans are global, that the turn IS the
 * ability to move with off-turn players ghosted, that the class alone is the
 * unit of selection, and that a timeout assigns rather than stalls. Everything
 * else is a parameter, so these tests assert the decided behaviour and
 * deliberately assert nothing about window size, ban counts or exclusivity
 * beyond that the parameter is honoured.
 */
class ClassDraftTest {

    private static final List<String> ROSTER =
            List.of("kitfighter", "mole", "gardener", "merchant", "sentinel", "ranger");

    private final UUID n1 = UUID.randomUUID(), n2 = UUID.randomUUID();
    private final UUID s1 = UUID.randomUUID(), s2 = UUID.randomUUID();

    private ClassDraft draft(ClassDraft.Rules rules) {
        return new ClassDraft(rules, ROSTER,
                Map.of(Team.NORTH, List.of(n1, n2), Team.SOUTH, List.of(s1, s2)));
    }

    private ClassDraft draft() { return draft(new ClassDraft.Rules(1, 1, true, true)); }

    @Test
    void every_player_may_ban_and_bans_are_global() {
        ClassDraft d = draft(new ClassDraft.Rules(2, 1, true, true));
        assertTrue(d.onTurn().containsAll(List.of(n1, n2, s1, s2)),
                "the ban phase is open to everyone");
        assertNull(d.ban(n1, "mole"));
        assertFalse(d.available().contains("mole"),
                "a banned station is gone for BOTH teams, not denied to one");
        assertNotNull(d.ban(s1, "mole"), "the same class cannot be banned twice");
    }

    @Test
    void the_turn_is_the_ability_to_move() {
        ClassDraft d = draft();
        d.ban(n1, "mole");
        d.ban(s1, "gardener");
        assertEquals(ClassDraft.Phase.PICK, d.phase());

        Set<UUID> turn = d.onTurn();
        assertEquals(1, turn.size(), "window size is the parameter, here one");
        UUID actor = turn.iterator().next();
        for (UUID other : List.of(n1, n2, s1, s2)) {
            assertEquals(!other.equals(actor), d.isGhost(other),
                    "exactly the players who may walk are solid");
        }
        assertEquals("not your turn",
                d.pick(actor.equals(n1) ? s1 : n1, "kitfighter"),
                "a ghost cannot claim a station");
    }

    @Test
    void the_unit_of_selection_is_the_class_alone() {
        ClassDraft d = draft();
        d.ban(n1, "mole"); d.ban(s1, "gardener");
        assertNull(d.pick(d.onTurn().iterator().next(), "kitfighter"));
        assertEquals(1, d.picks().size());
        assertEquals("kitfighter", d.picks().values().iterator().next(),
                "a pick is a class id and carries no branch or upgrade");
    }

    @Test
    void exclusivity_across_teams_is_a_parameter_not_a_decision() {
        ClassDraft global = draft(new ClassDraft.Rules(1, 2, true, true));
        global.ban(n1, "mole"); global.ban(s1, "gardener");
        UUID first = global.onTurn().iterator().next();
        global.pick(first, "kitfighter");
        UUID enemy = first.equals(n1) || first.equals(n2) ? s1 : n1;
        assertNotNull(global.pick(enemy, "kitfighter"), "no mirrors when exclusive");

        ClassDraft mirrors = draft(new ClassDraft.Rules(1, 2, false, true));
        mirrors.ban(n1, "mole"); mirrors.ban(s1, "gardener");
        UUID a = mirrors.onTurn().iterator().next();
        mirrors.pick(a, "kitfighter");
        UUID opp = a.equals(n1) || a.equals(n2) ? s1 : n1;
        assertNull(mirrors.pick(opp, "kitfighter"), "mirrors allowed when not exclusive");
    }

    @Test
    void a_timeout_assigns_rather_than_stalls() {
        ClassDraft d = draft();
        d.ban(n1, "mole"); d.ban(s1, "gardener");
        var assigned = d.timeout();
        assertEquals(1, assigned.size(), "the window's player received a class");
        assertFalse(assigned.values().iterator().next().isEmpty());
        assertNotEquals(ClassDraft.Phase.PICK, ClassDraft.Phase.COMPLETE);
    }

    @Test
    void an_absent_player_cannot_hold_the_match() {
        ClassDraft d = draft();
        d.ban(n1, "mole"); d.ban(s1, "gardener");
        for (int i = 0; i < 8 && d.phase() != ClassDraft.Phase.COMPLETE; i++) d.timeout();
        assertEquals(ClassDraft.Phase.COMPLETE, d.phase(), "the draft terminates");
        assertEquals(4, d.picks().size(), "everyone has a class");
    }

    @Test
    void an_unspent_ban_lapses_rather_than_removing_a_class_nobody_chose() {
        ClassDraft d = draft(new ClassDraft.Rules(3, 1, true, true));
        assertEquals(ClassDraft.Phase.BAN, d.phase());
        d.timeout();
        assertEquals(ClassDraft.Phase.PICK, d.phase());
        assertTrue(d.banned().isEmpty(),
                "auto-banning would remove a class from everyone on nobody's decision");
    }

    @Test
    void the_draft_knows_nothing_about_the_played_map() {
        // Classes are committed BLIND, so this type must not depend on which
        // map will be played. An earlier version of this test matched any
        // field whose TYPE name contained "map" and flagged
        // `Map<Team, List<UUID>> players` -- java.util.Map, not the game's.
        // Naming the actual types is the check that means something.
        for (var f : ClassDraft.class.getDeclaredFields()) {
            String type = f.getType().getName();
            assertFalse(type.contains("MapBindings") || type.contains("MapPool")
                            || type.contains("MapConfigurations"),
                    "the class draft must not see the played map: " + f.getName());
        }
    }
}
