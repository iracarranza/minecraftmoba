package com.minecraftmoba.plugin;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Toolbox as shipped: the curve and the cooldown ladder.
 *
 * Both are numbers the design argued its way to, and both are the kind that
 * get "tidied" later by someone who reads them as arbitrary. The tests say
 * what each one is for.
 */
class ToolboxConfigTest {

    private YamlConfiguration shipped() {
        return YamlConfiguration.loadConfiguration(new InputStreamReader(
                Objects.requireNonNull(getClass().getResourceAsStream("/config.yml"))));
    }

    /**
     * The inventory curve carries the class, so its breakpoints are load-bearing.
     *
     * Steps are indexed by Growth level: index n is Lv 3n.
     */
    @Test void theInventoryCurveHitsTheBreakpointsTheGrammarNeeds() {
        List<Integer> steps = shipped().getIntegerList("capacityProfiles.toolbox.slots.steps");
        assertEquals(11, steps.size(), "one entry per Growth level, Lv0 through Lv30");

        assertEquals(6,  steps.get(0), "Lv0: hotbar only, program and magazine collide");
        assertEquals(12, steps.get(1), "Lv3: the program leaves the hotbar");
        assertEquals(18, steps.get(2), "Lv6: two full rows that are NOT vertically adjacent");
        assertEquals(21, steps.get(3), "Lv9: first vertical adjacency, three Tripwire columns");
        assertEquals(27, steps.get(5), "Lv15: row A fully above row B, nine columns");
        assertEquals(36, steps.get(8), "Lv24: the whole board, and the reason this class is 36");
    }

    /**
     * 36 by Lv24 is structural, not generous.
     *
     * Inventory capacity is the DIMENSIONS OF A PROGRAMMABLE BOARD for this
     * class, so a curve that stops at the usual 24 stops the class.
     */
    @Test void toolboxIsTheClassThatReachesThirtySix() {
        var cfg = shipped();
        var toolbox = cfg.getIntegerList("capacityProfiles.toolbox.slots.steps");
        var mole = cfg.getIntegerList("capacityProfiles.mole.slots.steps");

        assertEquals(36, toolbox.get(8), "Toolbox has the whole board at Lv24");
        assertEquals(21, mole.get(8), "Mole is still at 21 there");
        assertEquals(24, mole.get(10),
                "and reaches the ordinary 24 only at maximum level -- deliberately "
                        + "stretched across the whole match");
        assertEquals(36, toolbox.get(10), "while Toolbox has had the full board for six levels");
    }

    /** The squishiest class on the roster, and deliberately so. */
    @Test void healthGrowsFarMoreSlowlyThanTheComparisonClass() {
        var cfg = shipped();
        assertTrue(cfg.getDouble("capacityProfiles.toolbox.health.growthUnit")
                        < cfg.getDouble("capacityProfiles.mole.health.growthUnit"),
                "the machine is the defence; a Toolbox that could also take hits "
                        + "would not have to build one");
        assertEquals(24, cfg.getInt("capacityProfiles.toolbox.health.cap"));
    }

    /**
     * The ability clock: 16 / 14 / 12 / 10 / 6 seconds.
     *
     * The fourth step is at Lv20, NOT Lv15. Lv15 already grants the Ultimate,
     * and budgets need not be equal at every event -- stacking an improvement
     * there would make one level enormous and leave Lv20 empty. This is the
     * line most likely to be "corrected" to a regular five-level cadence.
     */
    @Test void theCooldownLadderSkipsFifteenOnPurpose() {
        var ladder = shipped().getConfigurationSection(
                "abilities.definitions.utility_belt.cooldownByLevel");
        assertNotNull(ladder);
        assertEquals(List.of("level0", "level10", "level20", "level25", "level5"),
                ladder.getKeys(false).stream().sorted().toList());
        assertEquals(320, ladder.getInt("level0"),  "16s");
        assertEquals(280, ladder.getInt("level5"),  "14s");
        assertEquals(240, ladder.getInt("level10"), "12s");
        assertEquals(200, ladder.getInt("level20"), "10s");
        assertEquals(120, ladder.getInt("level25"), "6s");
        assertNull(ladder.get("level15"), "Lv15 is the Ultimate's level, not the belt's");
    }

    /**
     * The class ships with a PARTIAL kit, and that is the honest state.
     *
     * A1, A2 and the Ultimate are designed and not implemented, so their slots
     * are absent rather than pointed at a placeholder. An unbound input does
     * nothing; a placeholder would be a lie that looked like a feature.
     */
    @Test void toolboxShipsWithOnlyItsPassive() {
        var toolbox = shipped().getConfigurationSection("abilities.classes.toolbox");
        assertNotNull(toolbox, "the class must exist, or setclass refuses it");
        assertEquals("utility_belt", toolbox.getString("passiveHook"));
        assertNull(toolbox.getString("a1"), "Reconfiguratron! is designed, not built");
        assertNull(toolbox.getString("a2"));
        assertNull(toolbox.getString("ult"));
    }
}
