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
    @Test void toolboxShipsAPartialKitAndSaysWhichPartsAreMissing() {
        // This asserted that NOTHING but the passive was built. A1 now is, so
        // the tripwire moves rather than being deleted: it still fails the
        // moment A2 or the Ultimate is wired without this being updated, which
        // is the whole point of having it.
        var toolbox = shipped().getConfigurationSection("abilities.classes.toolbox");
        assertNotNull(toolbox, "the class must exist, or setclass refuses it");
        assertEquals("utility_belt", toolbox.getString("passiveHook"));
        assertEquals("reconfiguratron", toolbox.getString("a1"), "A1 is built");
        assertNull(toolbox.getString("a2"), "Jumpstartinator! is designed, not built");
        assertNull(toolbox.getString("ult"), "Gizmo of Absurdity is designed, not built");
    }

    // ---- the Growth clock -------------------------------------------------

    /**
     * The shipped curve must match classes.md, which is canonical for it.
     *
     * An earlier build of this file carried 8/1.6 and a slots curve reaching
     * 36 at Lv24, both written from memory and both contradicting a document
     * authored the same day. Nothing catches that except a comparison.
     */
    @Test void theCurveMatchesTheCanonicalDocument() throws Exception {
        var cfg = shipped();
        assertEquals(10.0, cfg.getDouble("capacityProfiles.toolbox.health.start"));
        assertEquals(1.4, cfg.getDouble("capacityProfiles.toolbox.health.growthUnit"));
        assertEquals(24, cfg.getInt("capacityProfiles.toolbox.health.cap"),
                "2,400 is the most health obtainable while still dying in three "
                        + "netherite hits; 2,500 flips that band");

        assertEquals(List.of(6, 12, 18, 21, 24, 27, 36, 36, 36, 36, 36),
                cfg.getIntegerList("capacityProfiles.toolbox.slots.steps"),
                "36 arrives at Lv18, not Lv24 -- 27 is the requirement for "
                        + "full-width Tripwire, so the specialist band is core function");
    }

    /**
     * Mostly Constructs, otherwise Buildable Scale, and nothing else.
     *
     * Toolbox builds machines and wants them bigger; it does not want a
     * logistics network. So the infrastructure spend touches the Construct
     * type only, on its Count axis first and its Spatial axis second -- never
     * Operational Scale, which governs what may CONNECT to a Construct rather
     * than what one may be.
     */
    @Test void growthSpendsOnConstructsAndBuildableScaleOnly() {
        var packets = GrowthPacket.load(shipped()
                .getConfigurationSection("abilities.classes.toolbox.growth"));

        var infrastructure = packets.values().stream()
                .flatMap(packet -> packet.of(GrowthPacket.Family.INFRASTRUCTURE).stream())
                .toList();
        assertEquals(5, infrastructure.size(), "four Constructs and one Buildable Scale");

        long constructs = infrastructure.stream().filter(e -> e.effect().equals("constructs")).count();
        assertEquals(4, constructs, "mostly Constructs");
        assertTrue(infrastructure.stream().anyMatch(e -> e.effect().equals("buildable_scale")),
                "and otherwise Buildable Scale");
        assertTrue(infrastructure.stream().noneMatch(e ->
                        e.effect().contains("route") || e.effect().contains("line")
                                || e.effect().contains("zone") || e.effect().contains("operational")),
                "no logistics network, and never Operational Scale");
    }

    /** Constructs arrive one tier at a time, at the levels the document names. */
    @Test void constructsTierUpAtSixTwelveTwentyOneAndTwentyFour() {
        var packets = GrowthPacket.load(shipped()
                .getConfigurationSection("abilities.classes.toolbox.growth"));
        assertEquals(1, packets.get(6).of(GrowthPacket.Family.INFRASTRUCTURE).getFirst().tier());
        assertEquals(2, packets.get(12).of(GrowthPacket.Family.INFRASTRUCTURE).getFirst().tier());
        assertEquals(3, packets.get(21).of(GrowthPacket.Family.INFRASTRUCTURE).getFirst().tier());
        assertEquals(4, packets.get(24).of(GrowthPacket.Family.INFRASTRUCTURE).getFirst().tier());
    }

    /** Placement Reach is PERSONAL: it changes geometry, not what is recognized. */
    @Test void placementReachIsPersonalRatherThanInfrastructure() {
        var packets = GrowthPacket.load(shipped()
                .getConfigurationSection("abilities.classes.toolbox.growth"));
        for (int level : List.of(9, 21, 27)) {
            var personal = packets.get(level).of(GrowthPacket.Family.PERSONAL);
            assertEquals(1, personal.size(), "Placement Reach at Lv" + level);
            assertEquals("placement_reach", personal.getFirst().dimension());
        }
    }

    /**
     * The Lv30 capstone is DEFERRED, and its slot stays empty.
     *
     * An empty packet at a Growth level is legitimate rather than a
     * configuration error, and leaving it empty is what stops placeholder
     * content reading as a decision later.
     */
    @Test void theCapstoneSlotIsLeftEmpty() {
        var packets = GrowthPacket.load(shipped()
                .getConfigurationSection("abilities.classes.toolbox.growth"));
        assertNull(packets.get(30), "nothing authored at Lv30 yet");
        assertFalse(packets.isEmpty(), "while the rest of the curve is authored");
    }
}
