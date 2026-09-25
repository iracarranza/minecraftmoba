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
                Map.of(Team.NORTH, List.of(n1, n2), Team.SOUTH, List.of(s1, s2)),
                Team.NORTH);
    }

    private ClassDraft draft() {
        return draft(new ClassDraft.Rules(1, new int[]{1, 1, 1, 1}, true, true));
    }

    @Test
    void every_player_may_ban_and_bans_are_global() {
        ClassDraft d = draft(new ClassDraft.Rules(2, new int[]{1, 1, 1, 1}, true, true));
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
        ClassDraft global = draft(new ClassDraft.Rules(1, new int[]{2, 2}, true, true));
        global.ban(n1, "mole"); global.ban(s1, "gardener");
        global.pick(n1, "kitfighter");
        global.pick(n2, "sentinel");
        assertNotNull(global.pick(s1, "kitfighter"), "no mirrors when exclusive");

        ClassDraft mirrors = draft(new ClassDraft.Rules(1, new int[]{2, 2}, false, true));
        mirrors.ban(n1, "mole"); mirrors.ban(s1, "gardener");
        // NORTH won the coinflip and holds the first window of two, so both
        // of its players must act before SOUTH is on turn. An earlier version
        // assumed either side could pick immediately, which was true only
        // under the plain alternation this replaced.
        mirrors.pick(n1, "kitfighter");
        mirrors.pick(n2, "sentinel");
        assertTrue(mirrors.onTurn().contains(s1), "now SOUTH's window");
        assertNull(mirrors.pick(s1, "kitfighter"), "mirrors allowed when not exclusive");
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
        ClassDraft d = draft(new ClassDraft.Rules(3, new int[]{1, 1, 1, 1}, true, true));
        assertEquals(ClassDraft.Phase.BAN, d.phase());
        d.timeout();
        assertEquals(ClassDraft.Phase.PICK, d.phase());
        assertTrue(d.banned().isEmpty(),
                "auto-banning would remove a class from everyone on nobody's decision");
    }

    @Test
    void hover_works_off_turn_because_that_is_the_point_of_it() {
        // A ghost hovering a class is broadcasting intent to both teams. That
        // is the pre-commitment channel the physical hall exists for, so the
        // enforcement is on `pick`, not on showing something on your stand.
        ClassDraft d = draft();
        d.ban(n1, "mole"); d.ban(s1, "gardener");
        UUID acting = d.onTurn().iterator().next();
        UUID ghost = acting.equals(n1) ? s1 : n1;
        assertTrue(d.isGhost(ghost));
        assertNull(d.hover(ghost, "sentinel"), "a ghost may hover");
        assertEquals("sentinel", d.stands().get(ghost));
        assertEquals("not your turn", d.pick(ghost, "sentinel"),
                "but may not commit");
    }

    @Test
    void a_banned_class_cannot_be_worn() {
        ClassDraft d = draft(new ClassDraft.Rules(2, new int[]{1, 1, 1, 1}, true, true));
        d.ban(n1, "mole");
        assertEquals("banned", d.hover(n2, "mole"),
                "the banned section already shows it; a stand wearing one "
                        + "would say a thing that is not true");
    }

    @Test
    void a_timeout_assigns_what_the_player_was_hovering() {
        // The hover is a declared preference, made in public. Assigning the
        // first available class instead would ignore a choice the player had
        // already broadcast to the room.
        ClassDraft d = draft();
        d.ban(n1, "mole"); d.ban(s1, "gardener");
        UUID acting = d.onTurn().iterator().next();
        assertNull(d.hover(acting, "ranger"));
        var assigned = d.timeout();
        assertEquals("ranger", assigned.get(acting));
    }

    @Test
    void a_timeout_falls_back_when_the_hover_is_no_longer_available() {
        ClassDraft d = draft(new ClassDraft.Rules(1, new int[]{2, 2}, true, true));
        d.ban(n1, "mole"); d.ban(s1, "gardener");
        var window = new ArrayList<>(d.onTurn());
        assertNull(d.hover(window.get(0), "ranger"));
        assertNull(d.hover(window.get(1), "ranger"));
        var assigned = d.timeout();
        assertEquals(2, assigned.size());
        assertNotEquals(assigned.get(window.get(0)), assigned.get(window.get(1)),
                "both wanted the same class; only one can have it");
    }

    @Test
    void picking_clears_the_hover_so_the_stand_shows_the_commitment() {
        ClassDraft d = draft();
        d.ban(n1, "mole"); d.ban(s1, "gardener");
        UUID acting = d.onTurn().iterator().next();
        d.hover(acting, "ranger");
        assertNull(d.pick(acting, "sentinel"));
        assertEquals("sentinel", d.stands().get(acting),
                "the stand shows what was committed, not what was considered");
    }

    @Test
    void the_snake_is_2_3_2_3_3_1_and_gives_each_team_seven() {
        // The decided order. 2-3-3-3-2-1 was proposed first and is not
        // balanced: A sees 21 enemy picks against B's 28. Moving one pick
        // from A's second window to its third closes the gap to 24 against
        // 25 without adding a window.
        int[] w = ClassDraft.Rules.SNAKE_7V7;
        assertArrayEquals(new int[]{2, 3, 2, 3, 3, 1}, w);
        var rules = ClassDraft.Rules.provisional(7);
        assertEquals(7, rules.picksFor(true), "the coinflip winner picks seven");
        assertEquals(7, rules.picksFor(false), "and so does the other team");
    }

    @Test
    void the_windows_are_not_a_constant_size() {
        // A fixed window would flatten 2-3-2-3-3-1 into alternation, which is
        // what this class did before the order was decided.
        var seven = new java.util.ArrayList<UUID>();
        var other = new java.util.ArrayList<UUID>();
        for (int i = 0; i < 7; i++) { seven.add(UUID.randomUUID()); other.add(UUID.randomUUID()); }
        var big = new java.util.ArrayList<String>();
        for (int i = 0; i < 20; i++) big.add("class_" + i);
        var d = new ClassDraft(new ClassDraft.Rules(0, ClassDraft.Rules.SNAKE_7V7, true, true),
                big, Map.of(Team.NORTH, seven, Team.SOUTH, other), Team.NORTH);
        assertEquals(ClassDraft.Phase.PICK, d.phase(), "no bans configured");
        var sizes = new java.util.ArrayList<Integer>();
        while (d.phase() == ClassDraft.Phase.PICK) {
            sizes.add(d.onTurn().size());
            d.timeout();
        }
        assertEquals(List.of(2, 3, 2, 3, 3, 1), sizes);
    }

    @Test
    void a_roster_too_small_ends_the_draft_instead_of_hanging() {
        // Ten players against six classes under global exclusivity is
        // unsatisfiable. The draft must terminate and say who it could not
        // serve; an earlier version parked the cursor on a player who could
        // never pick and spun for ever.
        var many = new java.util.ArrayList<UUID>();
        for (int i = 0; i < 10; i++) many.add(UUID.randomUUID());
        var d = new ClassDraft(new ClassDraft.Rules(0, new int[]{2, 2}, true, true),
                ROSTER, Map.of(Team.NORTH, many, Team.SOUTH, List.of(s1)), Team.NORTH);
        int guard = 0;
        while (d.phase() == ClassDraft.Phase.PICK && guard++ < 100) d.timeout();
        assertEquals(ClassDraft.Phase.COMPLETE, d.phase(), "the draft terminates");
        assertFalse(d.unassignable().isEmpty(), "and says who it could not serve");
    }

    @Test
    void the_coinflip_decides_who_picks_first() {
        var north = new ClassDraft(ClassDraft.Rules.provisional(2), ROSTER,
                Map.of(Team.NORTH, List.of(n1, n2), Team.SOUTH, List.of(s1, s2)), Team.NORTH);
        var south = new ClassDraft(ClassDraft.Rules.provisional(2), ROSTER,
                Map.of(Team.NORTH, List.of(n1, n2), Team.SOUTH, List.of(s1, s2)), Team.SOUTH);
        assertEquals(Team.NORTH, north.firstPick());
        assertEquals(Team.SOUTH, south.firstPick());
    }

    @Test
    void a_roster_larger_than_the_windows_still_picks_everyone() {
        var big = new java.util.ArrayList<UUID>();
        for (int i = 0; i < 9; i++) big.add(UUID.randomUUID());
        var roster = new java.util.ArrayList<String>();
        for (int i = 0; i < 20; i++) roster.add("class_" + i);
        var d = new ClassDraft(new ClassDraft.Rules(0, new int[]{1, 1}, true, true),
                roster, Map.of(Team.NORTH, big, Team.SOUTH, List.of(s1)), Team.NORTH);
        while (d.phase() == ClassDraft.Phase.PICK) d.timeout();
        assertEquals(10, d.picks().size(), "nobody is silently dropped");
    }

    @Test
    void the_lifecycle_puts_classes_before_the_map() {
        // PRE_MATCH_SELECTION_FLOW.md: "/moba match start should begin the
        // match and its pre-match process, not immediately teleport players to
        // their Fountains." The order is the decision, so this pins the states
        // rather than the command text.
        var states = java.util.List.of(Match.State.values());
        assertTrue(states.indexOf(Match.State.CLASS_SELECT)
                        < states.indexOf(Match.State.PRE_MATCH),
                "class selection precedes map selection");
        assertTrue(states.indexOf(Match.State.PRE_MATCH)
                        < states.indexOf(Match.State.RUNNING),
                "the map is resolved before active play");
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
