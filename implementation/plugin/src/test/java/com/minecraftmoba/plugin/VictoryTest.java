package com.minecraftmoba.plugin;

import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ALPHA-D3: a team wins when the opposing team has no remaining living
 * participating players *while that team's Fountain is disabled*.
 *
 * Both halves are load-bearing, and the tests that matter most are the ones
 * asserting that neither half alone is a win.
 */
class VictoryTest {
    private static Match.Participant p(Team t, boolean alive) {
        var part = new Match.Participant(UUID.randomUUID(), t);
        part.alive = alive;
        return part;
    }

    private static Map<Team, Boolean> disabled(Team... teams) {
        var m = new EnumMap<Team, Boolean>(Team.class);
        for (Team t : Team.values()) m.put(t, false);
        for (Team t : teams) m.put(t, true);
        return m;
    }

    @Test void disablingAFountainIsNotVictory() {
        // The whole point of the rule: survivors remain active.
        var parts = List.of(p(Team.NORTH, true), p(Team.SOUTH, true));
        assertNull(Match.victorOf(parts, disabled(Team.SOUTH)));
    }

    @Test void eliminatingEveryoneIsNotVictoryWhileTheFountainStands() {
        // They would simply respawn.
        var parts = List.of(p(Team.NORTH, true), p(Team.SOUTH, false));
        assertNull(Match.victorOf(parts, disabled()));
    }

    @Test void bothConditionsTogetherWin() {
        var parts = List.of(p(Team.NORTH, true), p(Team.SOUTH, false));
        assertEquals(Team.NORTH, Match.victorOf(parts, disabled(Team.SOUTH)));
    }

    @Test void oneSurvivorDeniesVictory() {
        var parts = List.of(p(Team.NORTH, true), p(Team.SOUTH, false), p(Team.SOUTH, true));
        assertNull(Match.victorOf(parts, disabled(Team.SOUTH)));
        var after = List.of(p(Team.NORTH, true), p(Team.SOUTH, false), p(Team.SOUTH, false));
        assertEquals(Team.NORTH, Match.victorOf(after, disabled(Team.SOUTH)));
    }

    @Test void aTeamWithNoParticipantsCannotLose() {
        // Otherwise a solo test would declare victory the moment a Fountain fell.
        var parts = List.of(p(Team.NORTH, true));
        assertNull(Match.victorOf(parts, disabled(Team.SOUTH)));
    }

    @Test void theSurvivingSideWinsRegardlessOfWhichTeamItIs() {
        var parts = List.of(p(Team.SOUTH, true), p(Team.NORTH, false));
        assertEquals(Team.SOUTH, Match.victorOf(parts, disabled(Team.NORTH)));
    }

    @Test void mutualEliminationWithBothFountainsDownPicksAWinnerDeterministically() {
        // Not a canon case, but the predicate must not be ambiguous if it happens.
        var parts = List.of(p(Team.NORTH, false), p(Team.SOUTH, false));
        Team v = Match.victorOf(parts, disabled(Team.NORTH, Team.SOUTH));
        assertNotNull(v);
        assertEquals(v, Match.victorOf(parts, disabled(Team.NORTH, Team.SOUTH)));
    }

    @Test void teamsAreExclusiveAndTotal() {
        assertEquals(Team.SOUTH, Team.NORTH.other());
        assertEquals(Team.NORTH, Team.SOUTH.other());
        assertEquals(Team.NORTH, Team.parse("north"));
        assertEquals(Team.SOUTH, Team.parse("S"));
        assertThrows(IllegalArgumentException.class, () -> Team.parse("spectator"));
        assertThrows(IllegalArgumentException.class, () -> Team.parse(null));
    }
}
