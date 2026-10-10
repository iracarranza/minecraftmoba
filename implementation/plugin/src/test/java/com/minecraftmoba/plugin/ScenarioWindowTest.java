package com.minecraftmoba.plugin;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

class ScenarioWindowTest {
    private static final class FakeBodies implements Bodies {
        final List<String> spawned = new ArrayList<>(); final List<Location> at = new ArrayList<>();
        public Body spawn(String id, Team team, String classId, int level, Location where) { spawned.add(id); at.add(where); return new Body(id, null, team, classId, level); }
        public void despawn(Body body) { }
        public boolean connected() { return true; }
        public String describe() { return "fake"; }
    }

    private static Scenario load(String yaml) {
        var cfg = YamlConfiguration.loadConfiguration(new java.io.StringReader(yaml));
        return Scenario.load("t", cfg.getConfigurationSection("t"));
    }

    private static final String WINDOWED = """
        t:
          anchor: lair
          roster:
            - { id: a, team: NORTH, class: mole, level: 5, at: [1, 0, 2] }
          timeline:
            - { tick: 2, assert: health_below, of: a, window: 4, because: "it lands within four ticks" }
        """;

    private static ScenarioRun run(Scenario s, int passOnTick) {
        var r = new ScenarioRun(s, new FakeBodies(), (c, b) -> r2tick[0] >= passOnTick ? null : "not yet");
        r.start();
        for (r2tick[0] = 0; r.running() && r2tick[0] < 30; r2tick[0]++) r.advance();
        return r;
    }
    private static final long[] r2tick = {0};

    @Test void aWindowedCheckPassesAsSoonAsItHoldsAndIsCountedOnce() {
        var rep = run(load(WINDOWED), 4).report();
        assertTrue(rep.passed(), rep.summary()); assertEquals(1, rep.checksRun());
    }

    @Test void aWindowedCheckFailsOnlyWhenItNeverHolds() {
        var rep = run(load(WINDOWED), 99).report();
        assertFalse(rep.passed()); assertEquals(1, rep.checksRun());
        assertTrue(rep.failures().get(0).contains("never held between ticks 2 and 6"), rep.failures().toString());
        assertTrue(rep.failures().get(0).contains("it lands within four ticks"));
    }

    @Test void aWindowedCheckThatHoldsAfterTheWindowStillFails() {
        assertFalse(run(load(WINDOWED), 7).report().passed());
    }

    @Test void theRunLastsUntilTheWindowCloses() {
        assertEquals(6, load(WINDOWED).lastTick());
    }

    @Test void theAnchorIsReadAndDefaultsToTheNorthFountain() {
        assertEquals("lair", load(WINDOWED).anchor());
        assertEquals(Scenario.DEFAULT_ANCHOR, load(WINDOWED.replace("  anchor: lair\n", "")).anchor());
    }

    @Test void aPlacementResolverSeesEveryMemberAtSpawn() {
        var bodies = new FakeBodies();
        var seen = new ArrayList<String>();
        var r = new ScenarioRun(load(WINDOWED), bodies, (c, b) -> null, m -> { seen.add(m.id() + "@" + m.x() + "," + m.z()); return null; });
        r.start();
        assertEquals(List.of("a@1.0,2.0"), seen);
    }
}
