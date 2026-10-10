package com.minecraftmoba.plugin;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 * Who counts as an ally. Found by the scenario bench: nothing ever put match players on a main
 * scoreboard team, so in a running match nobody was anybody's ally.
 */
class TargetabilityAlliesTest {
    @Test void twoPlayersOnTheSameMatchTeamAreAlliesWhateverTheScoreboardSays() {
        assertTrue(Targetability.decide(Team.NORTH, Team.NORTH, false));
    }

    @Test void twoPlayersOnDifferentMatchTeamsAreNeverAlliesEvenIfTheScoreboardHadThemTogether() {
        assertFalse(Targetability.decide(Team.NORTH, Team.SOUTH, true));
    }

    @Test void outsideAMatchTheScoreboardStillDecides() {
        assertTrue(Targetability.decide(null, null, true));
        assertFalse(Targetability.decide(null, null, false));
    }

    @Test void aPlayerInTheMatchAndOneOutsideFallBackToTheScoreboard() {
        assertFalse(Targetability.decide(Team.NORTH, null, false));
        assertTrue(Targetability.decide(null, Team.SOUTH, true));
    }
}
