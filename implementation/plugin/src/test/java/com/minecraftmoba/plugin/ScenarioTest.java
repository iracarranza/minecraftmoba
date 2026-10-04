package com.minecraftmoba.plugin;

import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The harness's own correctness, which has to be settled before any scenario
 * it runs can be believed.
 *
 * The failures worth guarding against are not "an assertion was wrong". They
 * are **a body left alive in a world that no longer exists**, and **a green
 * report over a rule that never ran**. Both look like success.
 */
class ScenarioTest {

    /** A Bodies that records what it was asked to do, and can refuse to spawn. */
    private static class Fake implements Bodies {
        final List<String> spawned = new ArrayList<>(), despawned = new ArrayList<>();
        boolean connected;
        String failOn;
        Fake(boolean connected) { this.connected = connected; }

        @Override public Body spawn(String id, Team team, String classId, int level, Location at) {
            if (id.equals(failOn)) throw new IllegalStateException("refused " + id);
            spawned.add(id);
            return new Body(id, null, team, classId, level);
        }
        @Override public void despawn(Body body) { despawned.add(body.id()); }
        @Override public boolean connected() { return connected; }
        @Override public String describe() { return connected ? "connected" : "connectionless"; }
    }

    private static Scenario load(String yaml) {
        var cfg = YamlConfiguration.loadConfiguration(new java.io.StringReader(yaml));
        return Scenario.load("t", cfg.getConfigurationSection("t"));
    }

    private static final String TWO_PLAYERS = """
        t:
          description: "two bodies, one check"
          roster:
            - { id: a, team: NORTH, class: mole, level: 5, at: [0, 70, 0] }
            - { id: b, team: SOUTH, class: toolbox, level: 5, at: [4, 70, 0] }
          timeline:
            - { tick: 0, actor: a, do: cast, input: LEFT_CLICK }
            - { tick: 2, assert: health_below, of: b, because: "enemies take authored damage" }
        """;

    // ---- loading -----------------------------------------------------------

    @Test void aScenarioIsDataAndParsesIntoRosterAndTimeline() {
        Scenario s = load(TWO_PLAYERS);
        assertEquals(2, s.roster().size());
        assertEquals(Team.NORTH, s.roster().get(0).team());
        assertEquals("toolbox", s.roster().get(1).classId());
        assertEquals(2, s.timeline().size());
        assertEquals(1, s.checkCount());
        assertEquals(2, s.lastTick());
    }

    @Test void extraKeysReachTheActorAsArguments() {
        Scenario s = load(TWO_PLAYERS);
        var act = (Scenario.Act) s.timeline().get(0);
        assertEquals("cast", act.verb());
        assertEquals("LEFT_CLICK", act.args().get("input"));
    }

    @Test void aCheckCarriesItsReason() {
        var check = (Scenario.Check) load(TWO_PLAYERS).timeline().get(1);
        assertEquals("enemies take authored damage", check.because(),
                "a failure must report the rule it believed in, not just a boolean");
    }

    @Test void anActorWhoIsNotOnTheRosterIsRefusedAtLoad() {
        // Caught here rather than at the tick it is used: otherwise a typo
        // surfaces mid-run, after bodies are spawned, looking like a harness
        // fault rather than a typo.
        var e = assertThrows(IllegalArgumentException.class, () -> load("""
            t:
              roster:
                - { id: a, team: NORTH, class: mole, level: 1, at: [0, 70, 0] }
              timeline:
                - { tick: 0, actor: typo, do: cast }
            """));
        assertTrue(e.getMessage().contains("typo"), e.getMessage());
    }

    @Test void anEmptyRosterOrTimelineIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> load("""
            t:
              roster: []
              timeline:
                - { tick: 0, actor: a, do: cast }
            """));
        assertThrows(IllegalArgumentException.class, () -> load("""
            t:
              roster:
                - { id: a, team: NORTH, class: mole, level: 1, at: [0, 70, 0] }
              timeline: []
            """));
    }

    @Test void aCheckWindowExtendsTheRunRatherThanRacingIt() {
        Scenario s = load("""
            t:
              roster:
                - { id: a, team: NORTH, class: mole, level: 1, at: [0, 70, 0] }
              timeline:
                - { tick: 5, window: 20, assert: health_below, of: a }
            """);
        assertEquals(25, s.lastTick(), "a projectile needs a span, not an instant");
    }

    // ---- running and teardown ---------------------------------------------

    private static ScenarioRun run(Scenario s, Fake bodies, java.util.function.BiFunction<Scenario.Check, Bodies.Body, String> judge) {
        var r = new ScenarioRun(s, bodies, judge);
        r.start();
        while (r.running()) r.advance();
        return r;
    }

    @Test void aCleanRunSpawnsEveryoneAndDespawnsEveryone() {
        Fake bodies = new Fake(false);
        var r = run(load(TWO_PLAYERS), bodies, (c, b) -> null);

        assertEquals(List.of("a", "b"), bodies.spawned);
        assertEquals(List.of("a", "b"), bodies.despawned, "every body is returned");
        assertEquals(0, r.rosterSize());
        assertTrue(r.report().passed(), r.report().summary());
    }

    @Test void aFailedCheckReportsTheRuleItBelievedIn() {
        var r = run(load(TWO_PLAYERS), new Fake(false), (c, b) -> "health was unchanged");
        var report = r.report();
        assertFalse(report.passed());
        assertEquals(1, report.failures().size());
        assertTrue(report.failures().get(0).contains("enemies take authored damage"),
                report.failures().get(0));
    }

    @Test void aStepThatThrowsStillReturnsEveryBody() {
        // The failure this harness must never produce: a body alive in a world
        // that is about to unload.
        Fake bodies = new Fake(false);
        var r = run(load(TWO_PLAYERS), bodies, (c, b) -> { throw new IllegalStateException("boom"); });

        assertEquals(List.of("a", "b"), bodies.despawned, "an aborted run still tears down");
        assertTrue(r.report().aborted());
        assertTrue(r.report().summary().contains("boom"), r.report().summary());
    }

    @Test void aRosterThatCannotBeCompletedSpawnsNobodyAndLeavesNobody() {
        Fake bodies = new Fake(false);
        bodies.failOn = "b";
        var r = new ScenarioRun(load(TWO_PLAYERS), bodies, (c, b) -> null);
        r.start();

        assertEquals(List.of("a"), bodies.spawned);
        assertEquals(List.of("a"), bodies.despawned, "the one that did spawn is returned");
        assertTrue(r.report().aborted());
        assertFalse(r.report().passed(), "a half-populated scenario proves nothing");
    }

    @Test void teardownIsIdempotent() {
        Fake bodies = new Fake(false);
        var r = new ScenarioRun(load(TWO_PLAYERS), bodies, (c, b) -> null);
        r.start();
        r.teardown("world unloaded");
        r.teardown("world unloaded again");
        r.finish();
        assertEquals(List.of("a", "b"), bodies.despawned, "exactly once each, however many doors are used");
    }

    @Test void aDespawnThatThrowsDoesNotStrandTheRest() {
        // One body refusing to leave must not keep the others in a dead world.
        Fake bodies = new Fake(false) {
            @Override public void despawn(Body body) {
                super.despawn(body);
                if (body.id().equals("a")) throw new IllegalStateException("stuck");
            }
        };
        var r = new ScenarioRun(load(TWO_PLAYERS), bodies, (c, b) -> null);
        r.start();
        r.teardown("done");
        assertEquals(List.of("a", "b"), bodies.despawned);
    }

    // ---- the false pass ----------------------------------------------------

    @Test void aScenarioNeedingRealInputRefusesConnectionlessBodies() {
        // The whole reason connected() is on the interface. Movement denial
        // runs in PlayerMoveEvent, which a connectionless body never fires, so
        // every check would pass without the rule being exercised.
        Scenario s = load("""
            t:
              requiresConnectedBodies: true
              roster:
                - { id: a, team: NORTH, class: mole, level: 1, at: [0, 70, 0] }
              timeline:
                - { tick: 1, assert: position_unchanged, of: a, because: "Root denies movement" }
            """);
        Fake bodies = new Fake(false);
        var r = run(s, bodies, (c, b) -> null);

        assertTrue(r.report().aborted(), "it must refuse rather than pass vacuously");
        assertEquals(0, r.report().checksRun(), "no check was run, and none is claimed");
        assertTrue(bodies.spawned.isEmpty(), "nothing was spawned for a scenario that cannot run");
        assertTrue(r.report().summary().contains("connectionless"), r.report().summary());
    }

    @Test void theSameScenarioRunsOnceBodiesAreConnected() {
        Scenario s = load("""
            t:
              requiresConnectedBodies: true
              roster:
                - { id: a, team: NORTH, class: mole, level: 1, at: [0, 70, 0] }
              timeline:
                - { tick: 1, assert: position_unchanged, of: a }
            """);
        var r = run(s, new Fake(true), (c, b) -> null);
        assertTrue(r.report().passed(), r.report().summary());
        assertEquals(1, r.report().checksRun());
    }

    @Test void aRunThatEndsEarlyIsNotAPassEvenWithNoFailedCheck() {
        var r = new ScenarioRun(load(TWO_PLAYERS), new Fake(false), (c, b) -> null);
        r.start();
        r.advance();          // tick 0: the act only
        r.abort("lab ended");
        assertFalse(r.report().passed(),
                "checks never reached are unknowns, not successes");
        assertEquals(0, r.report().checksRun());
        assertEquals(1, r.report().checksExpected());
    }
}
