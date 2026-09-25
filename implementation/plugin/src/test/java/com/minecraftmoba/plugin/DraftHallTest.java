package com.minecraftmoba.plugin;

import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The ways a draft room fails, asserted without a server.
 *
 * Two teams that do not get mirrored positions, stands that share a block, a
 * pit that undercuts a rank, or a count that grows with the class roster.
 * None of those is visible by reading the loop that places them.
 */
class DraftHallTest {

    private List<DraftHall.Stand> stands() {
        return DraftHall.stands(64, 6, 2, 2);
    }

    @Test
    void twenty_eight_stands_regardless_of_the_class_roster() {
        // The 25 September amendment moved the stand from the class to the
        // PLAYER so a fifty-class catalogue is not a fifty-station rack. This
        // is that property: nothing here takes a roster.
        assertEquals(28, stands().size());
        for (var m : DraftHall.class.getDeclaredMethods())
            for (var p : m.getParameterTypes())
                assertNotEquals(List.class, p, "no method takes a class roster");
    }

    @Test
    void the_two_teams_are_exact_mirrors() {
        var byTeam = stands().stream().collect(
                Collectors.groupingBy(DraftHall.Stand::team));
        var north = byTeam.get(Team.NORTH);
        var south = byTeam.get(Team.SOUTH);
        assertEquals(north.size(), south.size());
        for (var n : north) {
            boolean mirrored = south.stream().anyMatch(s ->
                    s.slot() == n.slot() && s.index() == n.index()
                            && s.x() == n.x() && s.y() == n.y() && s.z() == -n.z());
            assertTrue(mirrored, "no mirror for " + n);
        }
    }

    @Test
    void no_two_stands_share_a_block() {
        var seen = new HashSet<List<Integer>>();
        for (var s : stands())
            assertTrue(seen.add(List.of(s.x(), s.y(), s.z())), "overlap at " + s);
    }

    @Test
    void the_ban_pit_is_below_the_team_ranks() {
        int floorY = 64;
        var all = DraftHall.stands(floorY, 6, 2, 2);
        for (var s : all) {
            if (s.slot() == DraftHall.Slot.TEAM) assertTrue(s.y() > floorY);
            else assertTrue(s.y() <= floorY, "a ban must be sunken: " + s);
        }
    }

    @Test
    void bans_are_attributed_to_the_team_that_spent_them() {
        // Bans are global in EFFECT, but who spent one is strategically real
        // and the draft is open information, so the pit carries it.
        var bans = stands().stream()
                .filter(s -> s.slot() == DraftHall.Slot.BAN).toList();
        assertEquals(14, bans.size());
        assertEquals(7, bans.stream().filter(s -> s.team() == Team.NORTH).count());
        for (var b : bans)
            assertEquals(b.team() == Team.NORTH, b.z() < 0,
                    "a team's bans sit on that team's own side");
    }

    @Test
    void the_pit_does_not_undercut_either_rank() {
        int floorY = 64, rankOffset = 6;
        var ranks = DraftHall.stands(floorY, rankOffset, 2, 2).stream()
                .filter(s -> s.slot() == DraftHall.Slot.TEAM).toList();
        var dug = DraftHall.pit(floorY, 2, 7, 3).stream()
                .map(p -> List.of(p.x(), p.z())).collect(Collectors.toSet());
        for (var s : ranks)
            assertFalse(dug.contains(List.of(s.x(), s.z())),
                    "the pit removes the floor under " + s);
    }

    @Test
    void each_team_arrives_behind_its_own_rank() {
        int floorY = 64, rankOffset = 6;
        int[] north = DraftHall.spawn(Team.NORTH, floorY, rankOffset);
        int[] south = DraftHall.spawn(Team.SOUTH, floorY, rankOffset);
        assertTrue(north[2] < -rankOffset, "north stands behind its own rank");
        assertTrue(south[2] > rankOffset, "south stands behind its own rank");
        assertEquals(-north[2], south[2], "and the two are mirrored");
    }

    @Test
    void a_hall_large_enough_to_hold_it_is_computable() {
        int radius = DraftHall.requiredRadius(6, 2);
        for (var s : stands())
            assertTrue(Math.abs(s.x()) <= radius && Math.abs(s.z()) <= radius,
                    s + " falls outside a hall of radius " + radius);
    }

    @Test
    void a_rank_too_close_to_the_pit_is_refused() {
        assertThrows(IllegalArgumentException.class,
                () -> DraftHall.stands(64, 2, 2, 2));
        assertThrows(IllegalArgumentException.class,
                () -> DraftHall.stands(64, 6, 0, 2));
    }
}
