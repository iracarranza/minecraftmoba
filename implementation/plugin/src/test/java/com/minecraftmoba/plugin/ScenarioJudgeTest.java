package com.minecraftmoba.plugin;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ScenarioJudgeTest {
    /** Facts a test controls. */
    private static final class Fake implements ScenarioFacts {
        double health = 20, max = 20; boolean alive = true, stunned, rooted; long stunLeft;
        double[] pos = {0, 70, 0};
        final Map<Long, Double> healthAt = new HashMap<>(); final Map<Long, double[]> posAt = new HashMap<>();
        public double health(String id) { return health; }
        public double healthAt(String id, long t) { return healthAt.getOrDefault(t, Double.NaN); }
        public double maxHealth(String id) { return max; }
        public boolean alive(String id) { return alive; }
        public double[] position(String id) { return pos; }
        public double[] positionAt(String id, long t) { return posAt.get(t); }
        public boolean stunned(String id) { return stunned; }
        public long stunRemaining(String id) { return stunLeft; }
        public boolean rooted(String id) { return rooted; }
        int ran = 0;
        public int executions(String id, String ability) { return ran; }
    }

    private static Scenario.Check check(String assertion, String... kv) {
        var args = new HashMap<String, String>();
        for (int i = 0; i < kv.length; i += 2) args.put(kv[i], kv[i + 1]);
        return new Scenario.Check(10, 0, assertion, "b", "because", args);
    }

    @Test void healthUnchangedComparesWithAnEarlierTick() {
        var f = new Fake(); f.healthAt.put(5L, 20.0);
        assertNull(ScenarioJudge.judge(check("health_unchanged", "since", "5"), f));
        f.health = 16;
        assertTrue(ScenarioJudge.judge(check("health_unchanged", "since", "5"), f).contains("changed from 20.00 to 16.00"));
    }

    @Test void aTickThatWasNeverRecordedIsAFailureNotAPass() {
        assertTrue(ScenarioJudge.judge(check("health_unchanged", "since", "99"), new Fake()).contains("no health was recorded"));
        assertTrue(ScenarioJudge.judge(check("moved_less_than", "blocks", "1", "since", "99"), new Fake()).contains("no position was recorded"));
    }

    @Test void healthNotLowerTolerantOfRegenerationButNotOfDamage() {
        var f = new Fake(); f.healthAt.put(5L, 18.0);
        f.health = 18.0; assertNull(ScenarioJudge.judge(check("health_not_lower", "since", "5"), f));
        f.health = 19.1; assertNull(ScenarioJudge.judge(check("health_not_lower", "since", "5"), f), "regeneration raised it, nothing hurt it");
        f.health = 14.0; assertTrue(ScenarioJudge.judge(check("health_not_lower", "since", "5"), f).contains("fell from 18.00 to 14.00"));
        assertTrue(ScenarioJudge.judge(check("health_not_lower", "since", "99"), f).contains("no health was recorded"));
    }

    @Test void healthBelowIsStrictAndComparesWithFullByDefault() {
        var f = new Fake();
        assertNotNull(ScenarioJudge.judge(check("health_below"), f), "full health is not below full");
        f.health = 19.9; assertNull(ScenarioJudge.judge(check("health_below"), f));
        f.healthAt.put(3L, 19.9); assertNotNull(ScenarioJudge.judge(check("health_below", "than", "since", "since", "3"), f));
        f.health = 15; assertNull(ScenarioJudge.judge(check("health_below", "than", "since", "since", "3"), f));
    }

    @Test void healthAtLeastAFractionOfMax() {
        var f = new Fake(); f.health = 5;
        assertNull(ScenarioJudge.judge(check("health_at_least", "fraction", "0.25"), f));
        assertNotNull(ScenarioJudge.judge(check("health_at_least", "fraction", "0.5"), f));
        f.health = 0; assertNotNull(ScenarioJudge.judge(check("health_at_least"), f));
    }

    @Test void movementIsHorizontalAndJudgedBothWays() {
        var f = new Fake(); f.posAt.put(0L, new double[]{0, 70, 0}); f.pos = new double[]{3, 90, 4};   // 5 horizontal, 20 vertical
        assertNull(ScenarioJudge.judge(check("moved_more_than", "blocks", "4.9"), f));
        assertNotNull(ScenarioJudge.judge(check("moved_less_than", "blocks", "5"), f));
        f.pos = new double[]{0.1, 70, 0}; assertNull(ScenarioJudge.judge(check("moved_less_than", "blocks", "0.5"), f));
        assertTrue(ScenarioJudge.judge(check("moved_more_than", "blocks", "0.5"), f).contains("moved only"));
        assertTrue(ScenarioJudge.judge(check("moved_less_than"), f).contains("needs a 'blocks'"));
    }

    @Test void stunAndRootAssertions() {
        var f = new Fake();
        assertNotNull(ScenarioJudge.judge(check("stunned"), f)); assertNull(ScenarioJudge.judge(check("not_stunned"), f));
        f.stunned = true; f.stunLeft = 30;
        assertNull(ScenarioJudge.judge(check("stunned"), f));
        assertTrue(ScenarioJudge.judge(check("not_stunned"), f).contains("30 ticks left"));
        assertNull(ScenarioJudge.judge(check("stunned_for_at_least", "ticks", "30"), f));
        assertTrue(ScenarioJudge.judge(check("stunned_for_at_least", "ticks", "31"), f).contains("only 30"));
        assertNotNull(ScenarioJudge.judge(check("rooted"), f)); f.rooted = true;
        assertNull(ScenarioJudge.judge(check("rooted"), f)); assertNotNull(ScenarioJudge.judge(check("not_rooted"), f));
    }

    @Test void anAbilityMustHaveReallyRun() {
        var f = new Fake();
        assertTrue(ScenarioJudge.judge(check("ability_executed", "ability", "drill_rush"), f).contains("ran 0 time"));
        f.ran = 2;
        assertNull(ScenarioJudge.judge(check("ability_executed", "ability", "drill_rush"), f));
        assertNull(ScenarioJudge.judge(check("ability_executed", "ability", "drill_rush", "times", "2"), f));
        assertNotNull(ScenarioJudge.judge(check("ability_executed", "ability", "drill_rush", "times", "3"), f));
        assertTrue(ScenarioJudge.judge(check("ability_executed"), f).contains("needs an 'ability'"));
    }

    @Test void aliveFailsForTheDead() {
        var f = new Fake(); assertNull(ScenarioJudge.judge(check("alive"), f));
        f.alive = false; assertNotNull(ScenarioJudge.judge(check("alive"), f));
    }

    @Test void anUnknownAssertionFailsRatherThanPassing() {
        assertTrue(ScenarioJudge.judge(check("health_unchnaged"), new Fake()).contains("unknown assertion"));
    }

    @Test void everyKnownAssertionIsHandledByTheJudge() {
        for (String name : ScenarioJudge.KNOWN) {
            String why = ScenarioJudge.judge(check(name, "blocks", "1", "ticks", "1"), new Fake());
            assertTrue(why == null || !why.startsWith("unknown assertion"), name);
        }
    }
}
