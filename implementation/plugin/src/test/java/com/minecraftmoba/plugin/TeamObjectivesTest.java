package com.minecraftmoba.plugin;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * All three defensive objectives exist at once.
 *
 * The superseded reading made them successive: an Overworld/Nether/End phase
 * decided which one was even present. They now coexist, and their ordering is
 * spatial rather than mechanical -- bypassing an outer defense by tunnelling,
 * bridging or an unusual approach is ordinary Minecraft play, not a rule
 * violation, so nothing here may gate attackability on another objective.
 */
class TeamObjectivesTest {

    @Test void allThreeStandForBothTeamsFromTheOutset() {
        var o = new TeamObjectives();
        for (Team t : Team.values())
            for (TeamObjectives.Kind k : TeamObjectives.Kind.values()) {
                assertEquals(TeamObjectives.State.STANDING, o.state(t, k));
                assertTrue(o.attackable(t, k));
            }
    }

    @Test void nothingGatesOneObjectiveBehindAnother() {
        var o = new TeamObjectives();
        Team t = Team.values()[0];
        // The End Spike is the innermost, and is attackable with the Outpost
        // and Bastion both still standing.
        assertTrue(o.attackable(t, TeamObjectives.Kind.END_SPIKE));
        o.recordValidatedToppling(t, TeamObjectives.Kind.END_SPIKE);
        assertEquals(TeamObjectives.State.TOPPLED, o.state(t, TeamObjectives.Kind.END_SPIKE));
        assertTrue(o.attackable(t, TeamObjectives.Kind.PILLAGER_OUTPOST),
                "toppling out of order leaves the others attackable");
    }

    @Test void attackabilityTakesNoTimeOrPrerequisiteArgument() {
        // A signature check, because the defect would be a new parameter rather
        // than a new failing assertion: any temporal or prerequisite argument
        // here is the phase gate coming back.
        var params = java.util.Arrays.stream(TeamObjectives.class.getDeclaredMethods())
                .filter(m -> m.getName().equals("attackable")).findFirst().orElseThrow()
                .getParameterTypes();
        assertArrayEquals(new Class<?>[]{Team.class, TeamObjectives.Kind.class}, params);
    }

    @Test void theSpatialOrderRunsMidlineToFountain() {
        assertEquals(java.util.List.of("pillager_outpost", "nether_bastion",
                                       "end_spike", "aether_fountain"),
                TeamObjectives.MIDLINE_TO_FOUNTAIN);
    }

    @Test void topplingOneTeamsObjectiveLeavesTheOthersAlone() {
        var o = new TeamObjectives();
        o.recordValidatedToppling(Team.values()[0], TeamObjectives.Kind.NETHER_BASTION);
        assertEquals(TeamObjectives.State.STANDING,
                o.state(Team.values()[1], TeamObjectives.Kind.NETHER_BASTION));
    }

    @Test void resetReturnsEverythingToStanding() {
        var o = new TeamObjectives();
        for (Team t : Team.values())
            for (TeamObjectives.Kind k : TeamObjectives.Kind.values())
                o.recordValidatedToppling(t, k);
        o.reset();
        for (Team t : Team.values())
            for (TeamObjectives.Kind k : TeamObjectives.Kind.values())
                assertTrue(o.attackable(t, k));
    }
}
