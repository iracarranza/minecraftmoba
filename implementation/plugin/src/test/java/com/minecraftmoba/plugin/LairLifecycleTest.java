package com.minecraftmoba.plugin;

import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * One permanent Lair, three successive occupants.
 *
 * The rules being pinned here are the ones that make the Lair a place rather
 * than an event: the monster does not despawn at dawn, missing it does not
 * stall the clock, and killing it empties the site without destroying it.
 */
class LairLifecycleTest {

    /** A bound socket that records what was spawned and removed. */
    static final class Fake implements LairLifecycle.Encounters {
        boolean bound = true;
        boolean spawnFails;
        final List<OpportunityCadence.Boss> spawned = new ArrayList<>();
        final List<UUID> removed = new ArrayList<>();
        public boolean bound() { return bound; }
        public UUID spawn(OpportunityCadence.Boss boss) {
            spawned.add(boss);
            return spawnFails ? null : UUID.randomUUID();
        }
        public void remove(UUID id) { removed.add(id); }
    }

    private static LairLifecycle lair(Fake f) { return new LairLifecycle(f); }

    @Test void theOccupantSucceedsGiantThenGhastThenDragon() {
        var f = new Fake(); var l = lair(f);
        for (int n = 1; n <= 6; n++) l.onNight(n);
        assertEquals(List.of(OpportunityCadence.Boss.GIANT,
                             OpportunityCadence.Boss.GHAST,
                             OpportunityCadence.Boss.ENDER_DRAGON), f.spawned);
    }

    @Test void aSurvivingMonsterPersistsThroughTheWorksiteNightBetween() {
        var f = new Fake(); var l = lair(f);
        l.onNight(2);
        UUID giant = l.occupant();
        l.onNight(3); // Worksite II: the Lair is told, and must do nothing
        assertEquals(LairLifecycle.State.ALIVE, l.state());
        assertEquals(giant, l.occupant(), "a Worksite night is not a despawn");
        assertTrue(f.removed.isEmpty());
    }

    @Test void aSurvivorIsReplacedNotRewardedAtTheNextLairNight() {
        var f = new Fake(); var l = lair(f);
        l.onNight(2);
        UUID giant = l.occupant();
        l.onNight(4);
        assertEquals(List.of(giant), f.removed, "the Giant is removed, not killed");
        assertNull(l.lastVictory(), "replacement must not award a siege opportunity");
        assertEquals(OpportunityCadence.Boss.GHAST, l.scheduled());
    }

    @Test void killingTheMonsterLeavesTheLairDormantAndTheClockRunning() {
        var f = new Fake(); var l = lair(f);
        l.onNight(2);
        assertTrue(l.killed(l.occupant(), Team.values()[0]));
        assertEquals(LairLifecycle.State.DORMANT, l.state());
        assertNull(l.occupant(), "the site remains; the occupant does not");
        assertEquals(OpportunityCadence.Boss.GIANT, l.lastVictory().boss());
        assertEquals(TeamObjectives.Kind.PILLAGER_OUTPOST, l.lastVictory().pairedObjective());
        // The next Lair night still arrives on time.
        l.onNight(4);
        assertEquals(LairLifecycle.State.ALIVE, l.state());
        assertEquals(OpportunityCadence.Boss.GHAST, l.scheduled());
    }

    @Test void missingTheMonsterEntirelyDoesNotStallTheProgression() {
        var f = new Fake(); var l = lair(f);
        l.onNight(2); l.onNight(4); l.onNight(6); // never killed
        assertEquals(OpportunityCadence.Boss.ENDER_DRAGON, l.scheduled());
        assertEquals(3, f.spawned.size());
        assertEquals(2, f.removed.size(), "each survivor replaced exactly once");
    }

    @Test void aStaleOrRepeatedNightCannotResurrectAKilledMonster() {
        var f = new Fake(); var l = lair(f);
        l.onNight(2);
        l.killed(l.occupant(), null);
        l.onNight(2); // the same night delivered twice
        l.onNight(1); // an out-of-order earlier night
        assertEquals(LairLifecycle.State.DORMANT, l.state());
        assertEquals(1, f.spawned.size());
    }

    @Test void anUnknownKillerLeavesAttributionUnknownRatherThanGuessed() {
        var f = new Fake(); var l = lair(f);
        l.onNight(2);
        l.killed(l.occupant(), null);
        assertNull(l.lastVictory().victoriousTeam());
    }

    @Test void onlyTheCurrentOccupantsDeathResolvesTheEncounter() {
        var f = new Fake(); var l = lair(f);
        l.onNight(2);
        assertFalse(l.killed(UUID.randomUUID(), null), "some other entity's death is not the boss's");
        assertEquals(LairLifecycle.State.ALIVE, l.state());
    }

    @Test void anUnconfiguredSocketIsReportedNotInvented() {
        var f = new Fake(); f.bound = false; var l = lair(f);
        l.onNight(2);
        assertEquals(LairLifecycle.State.UNBOUND, l.state());
        assertNull(l.occupant());
        assertTrue(f.spawned.isEmpty(), "no socket means no spawn anywhere else");
    }

    @Test void aFailedSpawnIsBlockedNotAPhantomOccupant() {
        var f = new Fake(); f.spawnFails = true; var l = lair(f);
        l.onNight(2);
        assertEquals(LairLifecycle.State.BLOCKED, l.state());
        assertNull(l.occupant());
    }

    @Test void resetRemovesTheOccupantAndForgetsTheSchedule() {
        var f = new Fake(); var l = lair(f);
        l.onNight(2);
        UUID giant = l.occupant();
        l.reset();
        assertEquals(List.of(giant), f.removed);
        assertEquals(LairLifecycle.State.DORMANT, l.state());
        assertNull(l.scheduled());
        // A reset match starts the cadence over.
        l.onNight(2);
        assertEquals(OpportunityCadence.Boss.GIANT, l.scheduled());
    }
}
