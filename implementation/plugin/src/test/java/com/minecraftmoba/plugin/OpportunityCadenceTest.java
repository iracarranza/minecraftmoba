package com.minecraftmoba.plugin;

import org.junit.jupiter.api.Test;

import static com.minecraftmoba.plugin.OpportunityCadence.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * The alternating cadence, as arithmetic over the one match clock.
 *
 * The cadence replaces the Overworld -> Nether -> End -> Aether phase sequence
 * as the top-level temporal organizer. That sequence survives as thematic
 * escalation vocabulary; what it no longer does is decide what exists.
 */
class OpportunityCadenceTest {

    @Test void worksitesAndLairNightsAlternateAndEscalate() {
        assertEquals(Stage.OPENING, atNight(0));
        assertEquals(Stage.WORKSITE_I, atNight(1));
        assertEquals(Stage.GIANT, atNight(2));
        assertEquals(Stage.WORKSITE_II, atNight(3));
        assertEquals(Stage.GHAST, atNight(4));
        assertEquals(Stage.WORKSITE_III, atNight(5));
        assertEquals(Stage.DRAGON, atNight(6));
    }

    @Test void everyScheduledNightIsAWorksiteOrALairNeverBoth() {
        for (int n = 1; n <= 6; n++) {
            Stage s = atNight(n);
            assertTrue((s.tier() == null) != (s.boss() == null),
                    "night " + n + " must be exactly one of Worksite or Lair");
        }
    }

    @Test void afterTheDragonTheScheduleIsOpenNotARepeat() {
        // The temptation is to loop, or to repeat the last entry the way the
        // old per-sunset activation list did. Both would be inventing design.
        for (int n = 7; n <= 12; n++) {
            assertEquals(Stage.UNSCHEDULED, atNight(n));
            assertNull(atNight(n).tier());
            assertNull(atNight(n).boss());
        }
    }

    @Test void theStageIsDerivedFromTheMatchClockNotASecondCounter() {
        long minute = 60L * 20L;
        assertEquals(Stage.OPENING, at(0));
        assertEquals(Stage.WORKSITE_I, at(10 * minute));
        assertEquals(Stage.GIANT, at(30 * minute));
        assertEquals(Stage.WORKSITE_II, at(50 * minute));
        assertEquals(Stage.GHAST, at(70 * minute));
        assertEquals(Stage.WORKSITE_III, at(90 * minute));
        assertEquals(Stage.DRAGON, at(110 * minute));
        // Mid-night, not only on the boundary tick.
        assertEquals(Stage.GIANT, at(35 * minute));
    }

    @Test void eachBossIsPairedWithOneEnemyObjective() {
        assertEquals(TeamObjectives.Kind.PILLAGER_OUTPOST, TeamObjectives.pairedObjective(Boss.GIANT));
        assertEquals(TeamObjectives.Kind.NETHER_BASTION, TeamObjectives.pairedObjective(Boss.GHAST));
        assertEquals(TeamObjectives.Kind.END_SPIKE, TeamObjectives.pairedObjective(Boss.ENDER_DRAGON));
    }
}
