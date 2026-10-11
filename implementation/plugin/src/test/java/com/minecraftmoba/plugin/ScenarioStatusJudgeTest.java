package com.minecraftmoba.plugin;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** The control-status assertions and the status act, judged and validated without a server. */
class ScenarioStatusJudgeTest {
    private static final class Facts implements ScenarioFacts {
        final Map<String, double[]> now = new HashMap<>();
        final Map<String, Map<Long, double[]>> then = new HashMap<>();
        final Map<String, Float> yaws = new HashMap<>();
        final Map<String, Set<String>> kinds = new HashMap<>();
        final Map<String, Integer> casts = new HashMap<>();
        public double health(String id) { return 20; }
        public double healthAt(String id, long t) { return 20; }
        public double maxHealth(String id) { return 20; }
        public boolean alive(String id) { return true; }
        public double[] position(String id) { return now.get(id); }
        public double[] positionAt(String id, long t) { var m = then.get(id); return m == null ? null : m.get(t); }
        public boolean stunned(String id) { return false; }
        public long stunRemaining(String id) { return 0; }
        public boolean rooted(String id) { return false; }
        public boolean statusActive(String id, String kind) { return kinds.getOrDefault(id, Set.of()).contains(kind.toLowerCase()); }
        public float yaw(String id) { return yaws.getOrDefault(id, 0f); }
        public int executions(String id, String ability) { return casts.getOrDefault(id + ":" + ability, 0); }

        Facts place(String id, long tick, double x, double z, double nowX, double nowZ) {
            then.computeIfAbsent(id, k -> new HashMap<>()).put(tick, new double[]{x, 64, z});
            now.put(id, new double[]{nowX, 64, nowZ});
            return this;
        }
    }

    private static Scenario.Check check(String assertion, String of, Map<String, String> args) {
        return new Scenario.Check(10, 0, assertion, of, "because", args);
    }

    @Test void movedTowardNeedsTheDistanceToShrink() {
        var f = new Facts().place("a", 0, 10, 0, 6, 0).place("src", 0, 0, 0, 0, 0);
        assertNull(ScenarioJudge.judge(check("moved_toward", "a", Map.of("source", "src", "blocks", "3")), f));
        assertNotNull(ScenarioJudge.judge(check("moved_toward", "a", Map.of("source", "src", "blocks", "5")), f), "closed 4, not 5");
        assertNotNull(ScenarioJudge.judge(check("moved_away", "a", Map.of("source", "src", "blocks", "0.1")), f), "it moved closer, not away");
    }

    @Test void movedAwayNeedsTheDistanceToGrow() {
        var f = new Facts().place("a", 0, 4, 0, 9, 0).place("src", 0, 0, 0, 0, 0);
        assertNull(ScenarioJudge.judge(check("moved_away", "a", Map.of("source", "src", "blocks", "4")), f));
        assertNotNull(ScenarioJudge.judge(check("moved_toward", "a", Map.of("source", "src", "blocks", "0.1")), f));
    }

    @Test void sidewaysMovementIsNeitherTowardNorAway() {
        var f = new Facts().place("a", 0, 10, 0, 10, 0.2).place("src", 0, 0, 0, 0, 0);
        assertNotNull(ScenarioJudge.judge(check("moved_toward", "a", Map.of("source", "src", "blocks", "1")), f));
        assertNotNull(ScenarioJudge.judge(check("moved_away", "a", Map.of("source", "src", "blocks", "1")), f));
    }

    @Test void aMovingSourceIsJudgedAtBothTimes() {
        // the subject follows a source that walked away: distance unchanged, so it did not close in
        var f = new Facts().place("a", 0, 10, 0, 20, 0).place("src", 0, 0, 0, 10, 0);
        assertNotNull(ScenarioJudge.judge(check("moved_toward", "a", Map.of("source", "src", "blocks", "1")), f));
    }

    @Test void aMissingSnapshotFailsRatherThanPassing() {
        var f = new Facts().place("a", 0, 10, 0, 6, 0).place("src", 0, 0, 0, 0, 0);
        var late = check("moved_toward", "a", Map.of("source", "src", "blocks", "1", "since", "99"));
        assertTrue(ScenarioJudge.judge(late, f).contains("no position was recorded"));
    }

    @Test void facingTheSourceIsWithinADegreeBudget() {
        var f = new Facts().place("a", 0, 0, 0, 0, 0).place("src", 0, 0, 10, 0, 10);       // source due south: yaw 0
        f.yaws.put("a", 12f);
        assertNull(ScenarioJudge.judge(check("facing_source", "a", Map.of("source", "src", "within", "15")), f));
        assertNotNull(ScenarioJudge.judge(check("facing_source", "a", Map.of("source", "src", "within", "10")), f));
        f.yaws.put("a", 170f);
        assertNotNull(ScenarioJudge.judge(check("facing_source", "a", Map.of("source", "src")), f), "facing away fails the default budget");
    }

    @Test void statusAssertionsReadTheKind() {
        var f = new Facts();
        f.kinds.put("a", Set.of("lured"));
        assertNull(ScenarioJudge.judge(check("status_active", "a", Map.of("kind", "Lured")), f));
        assertNotNull(ScenarioJudge.judge(check("status_active", "a", Map.of("kind", "frightened")), f));
        assertNull(ScenarioJudge.judge(check("status_inactive", "a", Map.of("kind", "frightened")), f));
        assertNotNull(ScenarioJudge.judge(check("status_inactive", "a", Map.of("kind", "lured")), f));
        assertTrue(ScenarioJudge.judge(check("status_active", "a", Map.of("kind", "confused")), f).contains("not a status"));
        assertTrue(ScenarioJudge.judge(check("status_active", "a", Map.of()), f).contains("kind"));
    }

    @Test void abilityNotExecutedPassesOnlyWhenNothingRan() {
        var f = new Facts();
        assertNull(ScenarioJudge.judge(check("ability_not_executed", "a", Map.of("ability", "food_fight")), f));
        f.casts.put("a:food_fight", 1);
        assertNotNull(ScenarioJudge.judge(check("ability_not_executed", "a", Map.of("ability", "food_fight")), f));
    }

    @Test void everyNewAssertionIsKnownToTheValidator() {
        for (String n : new String[]{"status_active", "status_inactive", "moved_toward", "moved_away", "facing_source", "ability_not_executed"})
            assertTrue(ScenarioJudge.KNOWN.contains(n), n);
    }

    // ---- the status act ---------------------------------------------------------

    private static Scenario scenarioWith(Map<String, String> args) {
        var roster = java.util.List.of(
                new Scenario.Member("a", Team.NORTH, "mole", 5, 0, 0, 0),
                new Scenario.Member("b", Team.SOUTH, "mole", 5, 4, 0, 0));
        var act = new Scenario.Act(10, "a", "status", args);
        return new Scenario("s", "d", roster, java.util.List.<Scenario.Step>of(act), false, "fountain_north");
    }

    @Test void aWellFormedStatusActPasses() {
        assertNull(ScenarioActs.problem(scenarioWith(Map.of("target", "b", "kind", "lured", "ticks", "40", "source", "a"))));
        assertNull(ScenarioActs.problem(scenarioWith(Map.of("target", "b", "kind", "blinded", "ticks", "40"))));
    }

    @Test void aLuredOrFrightenedActNeedsASourceOnTheRoster() {
        assertTrue(ScenarioActs.problem(scenarioWith(Map.of("target", "b", "kind", "lured", "ticks", "40"))).contains("source"));
        assertTrue(ScenarioActs.problem(scenarioWith(Map.of("target", "b", "kind", "frightened", "ticks", "40", "source", "ghost"))).contains("ghost"));
    }

    @Test void anUnknownKindOrMissingArgumentIsRefusedAtLoad() {
        assertTrue(ScenarioActs.problem(scenarioWith(Map.of("target", "b", "kind", "confused", "ticks", "40"))).contains("confused"));
        assertTrue(ScenarioActs.problem(scenarioWith(Map.of("target", "b", "ticks", "40"))).contains("kind"));
        assertTrue(ScenarioActs.problem(scenarioWith(Map.of("target", "b", "kind", "lured", "ticks", "soon", "source", "a"))).contains("not a number"));
    }
}
