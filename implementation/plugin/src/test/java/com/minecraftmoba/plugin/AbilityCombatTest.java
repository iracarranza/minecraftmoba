package com.minecraftmoba.plugin;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Combat classification is declared, required, and read per branch.
 *
 * The last test is the important one: it holds the real config.yml to the rule,
 * the same way ConfigKeysDefinedTest does. A unit test of the loader alone
 * would pass happily while the shipped file classified an ability by accident.
 */
class AbilityCombatTest {

    private YamlConfiguration yaml(String body) {
        var c = new YamlConfiguration();
        try { c.loadFromString(body); } catch (Exception e) { throw new AssertionError(e); }
        return c;
    }

    @Test void aMissingAbilityFlagFailsAtLoad() {
        var cfg = yaml("""
                definitions:
                  tunneling:
                    cooldownTicks: 10
                """);
        var thrown = assertThrows(IllegalStateException.class,
                () -> AbilityCombat.load(cfg.getConfigurationSection("definitions")));
        assertTrue(thrown.getMessage().contains("tunneling.combat"), thrown.getMessage());
    }

    @Test void aMissingBranchFlagFailsAtLoad() {
        var cfg = yaml("""
                definitions:
                  runway:
                    combat: false
                    branches:
                      suplex: { power: 1.0 }
                """);
        var thrown = assertThrows(IllegalStateException.class,
                () -> AbilityCombat.load(cfg.getConfigurationSection("definitions")));
        assertTrue(thrown.getMessage().contains("runway.branches.suplex.combat"), thrown.getMessage());
    }

    /** All of them at once: one restart per missing key would be its own defect. */
    @Test void everyMissingFlagIsReportedTogether() {
        var cfg = yaml("""
                definitions:
                  a: { cooldownTicks: 1 }
                  b: { cooldownTicks: 1 }
                """);
        var thrown = assertThrows(IllegalStateException.class,
                () -> AbilityCombat.load(cfg.getConfigurationSection("definitions")));
        assertTrue(thrown.getMessage().contains("a.combat"), thrown.getMessage());
        assertTrue(thrown.getMessage().contains("b.combat"), thrown.getMessage());
    }

    /**
     * Daredevil's case: a non-combat ability whose branch subjects an enemy to
     * the launch. The branch overrides, which is the whole point of per-branch.
     */
    @Test void aBranchOverridesTheAbilitysOwnAnswer() {
        var cfg = yaml("""
                definitions:
                  runway:
                    combat: false
                    branches:
                      suplex: { combat: true }
                      sprint: { combat: false }
                """);
        var rules = AbilityCombat.load(cfg.getConfigurationSection("definitions"));
        assertFalse(rules.combat("runway", null));
        assertTrue(rules.combat("runway", "suplex"));
        assertFalse(rules.combat("runway", "sprint"));
    }

    @Test void anUnknownBranchFallsBackToTheAbility() {
        var cfg = yaml("""
                definitions:
                  lunge:
                    combat: true
                    branches:
                      swarmingBite: { combat: true }
                """);
        var rules = AbilityCombat.load(cfg.getConfigurationSection("definitions"));
        assertTrue(rules.combat("lunge", "not_a_branch"));
        assertFalse(rules.combat("no_such_ability", null));
    }

    /**
     * Branch ids are snake_case in PlayerData.classState and camelCase in
     * config.yml. If these two spellings stop being reconciled, every branch
     * silently falls back to its ability's answer -- which is exactly the kind
     * of silent wrong default this whole mechanism exists to prevent.
     */
    @Test void snakeCaseBranchIdsMatchCamelCaseConfigKeys() {
        var cfg = yaml("""
                definitions:
                  runway:
                    combat: false
                    branches:
                      suplexSlam: { combat: true }
                """);
        var rules = AbilityCombat.load(cfg.getConfigurationSection("definitions"));
        assertTrue(rules.combat("runway", "suplex_slam"));
    }

    @Test void theShippedConfigDeclaresEveryAbilityAndBranch() throws Exception {
        var cfg = YamlConfiguration.loadConfiguration(new InputStreamReader(
                Objects.requireNonNull(getClass().getResourceAsStream("/config.yml"))));
        var definitions = cfg.getConfigurationSection("abilities.definitions");
        assertNotNull(definitions, "config.yml defines no abilities");
        assertDoesNotThrow(() -> AbilityCombat.load(definitions));

        var rules = AbilityCombat.load(definitions);
        // The two the design names explicitly, in opposite directions, over the
        // same medium: Sinkhole moves blocks and is combat because it is crowd
        // control; Tunneling moves blocks and is not, because it does nothing
        // to anyone's capacity to fight.
        assertTrue(rules.combat("sinkhole_lite", null), "Sinkhole is settled as combat");
        assertFalse(rules.combat("tunneling", null), "Tunneling is the case that proves the rule");
        assertFalse(rules.combat("tunneling", "bore"));
        assertFalse(rules.combat("tunneling", "gallery"));
        assertTrue(rules.combat("lunge", "swarming_bite"));
        assertTrue(rules.combat("lunge", "thieving_swipe"));
        assertTrue(rules.combat("lunge", "stalking_pounce"));
    }

    /**
     * Recall must not silently regain a movement tolerance.
     *
     * "Hold still means hold still" was a decision, and the replacement is a
     * block-position comparison. A reintroduced `moveTolerance` key would be
     * read by nothing and would quietly document a rule the code no longer has.
     */
    @Test void recallHasNoMovementTolerance() throws Exception {
        var body = Files.readString(Path.of("src/main/resources/config.yml"));
        assertFalse(body.contains("\n    moveTolerance:"),
                "features.recall.moveTolerance was removed by design decision");
    }
}
