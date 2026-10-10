package com.minecraftmoba.plugin;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.util.ArrayList;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

/** The scenarios the plugin ships, loaded exactly as the lab loads them. */
class ScenarioShippedTest {
    private static YamlConfiguration config() { return YamlConfiguration.loadConfiguration(new File("src/main/resources/config.yml")); }

    private static java.util.List<Scenario> shipped() {
        var section = config().getConfigurationSection("alpha.lab.scenarios");
        assertNotNull(section, "no scenarios shipped");
        var out = new ArrayList<Scenario>();
        for (String id : section.getKeys(false)) out.add(Scenario.load(id, section.getConfigurationSection(id)));
        return out;
    }

    @Test void everyShippedScenarioLoadsAndNamesOnlyRosterMembers() { assertTrue(shipped().size() >= 3); }

    @Test void everyActAndAssertionIsOneTheHarnessKnows() {
        for (var s : shipped()) assertNull(ScenarioActs.problem(s), s.id());
    }

    @Test void theThreeFirstScenariosAreThereWithTheirRules() {
        var byId = new java.util.HashMap<String, Scenario>();
        for (var s : shipped()) byId.put(s.id(), s);
        for (String id : new String[]{"friendly_fire", "stun_and_root", "skirmish_2v2"}) assertTrue(byId.containsKey(id), id);
        assertEquals(4, byId.get("skirmish_2v2").roster().size());
        assertEquals(2, byId.get("skirmish_2v2").roster().stream().filter(m -> m.team() == Team.NORTH).count());
    }

    @Test void everyCheckStatesWhyItBelievesTheRule() {
        // 'because' is part of the assertion, so a failure reports the rule. The ones that are only
        // bookkeeping (the Stun landed, the body is alive) may omit it; the rule-bearing ones may not.
        for (var s : shipped())
            for (var step : s.timeline())
                if (step instanceof Scenario.Check c && (c.assertion().contains("unchanged") || c.assertion().contains("not_lower") || c.assertion().contains("moved")))
                    assertFalse(c.because().isBlank(), s.id() + " tick " + c.tick() + " " + c.assertion());
    }

    @Test void theStunAndRootScenarioHasAControlSoItsRootCheckIsNotVacuous() {
        var s = shipped().stream().filter(x -> x.id().equals("stun_and_root")).findFirst().orElseThrow();
        boolean control = s.timeline().stream().anyMatch(st -> st instanceof Scenario.Check c && c.assertion().equals("moved_more_than"));
        boolean rootCheck = s.timeline().stream().anyMatch(st -> st instanceof Scenario.Check c && c.assertion().equals("moved_less_than"));
        assertTrue(control && rootCheck, "a root check without a control passes when walking is broken");
    }

    @Test void friendlyFireSpacesItsHitsPastTheDamageImmunityWindow() {
        // Two hits on one victim inside ten ticks would apply only the excess of the second, which
        // would read as a failing rule that is actually vanilla immunity frames.
        var s = shipped().stream().filter(x -> x.id().equals("friendly_fire")).findFirst().orElseThrow();
        var hitsOn = new java.util.HashMap<String, java.util.List<Long>>();
        for (var st : s.timeline()) if (st instanceof Scenario.Act a && (a.verb().equals("hit") || a.verb().equals("explode")))
            hitsOn.computeIfAbsent(a.args().containsKey("target") ? a.args().get("target") : a.args().get("at"), k -> new ArrayList<>()).add(a.tick());
        for (var e : hitsOn.entrySet()) for (int i = 1; i < e.getValue().size(); i++)
            assertTrue(e.getValue().get(i) - e.getValue().get(i - 1) >= 10, e.getKey() + " is hit twice inside the immunity window");
    }

    // ---- the act vocabulary --------------------------------------------------------------------

    private static Scenario with(String act) {
        var cfg = YamlConfiguration.loadConfiguration(new java.io.StringReader("""
            t:
              roster:
                - { id: a, team: NORTH, class: mole, level: 1, at: [0, 0, 0] }
                - { id: b, team: SOUTH, class: mole, level: 1, at: [2, 0, 0] }
              timeline:
                - %s
            """.formatted(act)));
        return Scenario.load("t", cfg.getConfigurationSection("t"));
    }

    @Test void aWellFormedActPasses() {
        assertNull(ScenarioActs.problem(with("{ tick: 1, actor: a, do: hit, target: b, amount: 3 }")));
        assertNull(ScenarioActs.problem(with("{ tick: 1, actor: a, do: walk, direction: east, ticks: 5 }")));
        assertNull(ScenarioActs.problem(with("{ tick: 1, actor: a, do: heal }")));
    }

    @Test void aMissingArgumentIsRefusedAtLoadWithTheArgumentNamed() {
        assertTrue(ScenarioActs.problem(with("{ tick: 1, actor: a, do: hit, target: b }")).contains("needs 'amount'"));
        assertTrue(ScenarioActs.problem(with("{ tick: 1, actor: a, do: stun, target: b }")).contains("needs 'ticks'"));
    }

    @Test void anUnknownActOrTargetOrDirectionOrSlotIsRefused() {
        assertTrue(ScenarioActs.problem(with("{ tick: 1, actor: a, do: teleport, target: b }")).contains("unknown act"));
        assertTrue(ScenarioActs.problem(with("{ tick: 1, actor: a, do: hit, target: ghost, amount: 1 }")).contains("not on the roster"));
        assertTrue(ScenarioActs.problem(with("{ tick: 1, actor: a, do: walk, direction: up, ticks: 3 }")).contains("not one of"));
        assertTrue(ScenarioActs.problem(with("{ tick: 1, actor: a, do: cast, slot: a9, at: b }")).contains("not one of"));
        assertTrue(ScenarioActs.problem(with("{ tick: 1, actor: a, do: hit, target: b, amount: lots }")).contains("not a number"));
    }

    @Test void anUnknownAssertionIsRefusedAtLoad() {
        var cfg = YamlConfiguration.loadConfiguration(new java.io.StringReader("""
            t:
              roster:
                - { id: a, team: NORTH, class: mole, level: 1, at: [0, 0, 0] }
              timeline:
                - { tick: 1, assert: health_unchnaged, of: a }
            """));
        assertTrue(ScenarioActs.problem(Scenario.load("t", cfg.getConfigurationSection("t"))).contains("unknown assertion"));
    }

    @Test void directionsAreInMinecraftsConventionAndStepAlongTheirAxis() {
        assertEquals(0f, ScenarioActs.yaw("south")); assertEquals(-90f, ScenarioActs.yaw("east"));
        assertEquals(180f, ScenarioActs.yaw("north")); assertEquals(90f, ScenarioActs.yaw("west"));
        assertArrayEquals(new double[]{1, 0}, ScenarioActs.step("east")); assertArrayEquals(new double[]{0, -1}, ScenarioActs.step("north"));
    }
}
