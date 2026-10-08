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
    @Test void toolboxShipsItsWholeKit() {
        // This began as "ships with ONLY its passive" and moved twice as A1
        // and A2 landed. The kit is now complete, so what it guards changes
        // shape: it stops tracking what is missing and starts pinning what is
        // present, including that every slot names the ability classes.md
        // names and not a stub.
        var toolbox = shipped().getConfigurationSection("abilities.classes.toolbox");
        assertNotNull(toolbox, "the class must exist, or setclass refuses it");
        assertEquals("utility_belt", toolbox.getString("passiveHook"));
        assertEquals("reconfiguratron", toolbox.getString("a1"));
        assertEquals("jumpstartinator", toolbox.getString("a2"));
        assertEquals("gizmo", toolbox.getString("ult"));

        // The Lv30 capstone remains DEFERRED rather than absent: its slot is
        // left empty on purpose, and filling it is a design decision.
        var growth = toolbox.getConfigurationSection("growth");
        assertNull(growth.getConfigurationSection("level30"),
                "the capstone is deferred, and a placeholder would hide that");
    }

    // ---- the Ultimate ------------------------------------------------------

    @Test void theTwoCapsArePairedRatherThanIndependent() {
        // 120 / 32 is 3.75 ticks per activation, which is what makes both caps
        // bite: a clock faster than about four ticks is bounded by activations,
        // a broad slow machine by time. Drifting either number alone would
        // quietly leave one cap doing nothing.
        var ult = shipped().getConfigurationSection("abilities.definitions.gizmo");
        assertEquals(120, ult.getLong("durationTicks"), "six seconds");
        assertEquals(32, ult.getInt("activations"));
        assertEquals(3.75, ult.getLong("durationTicks") / (double) ult.getInt("activations"), 1e-9);
    }

    @Test void theWindowMatchesThePassivesFastestCooldown() {
        // Not a coincidence: the window is a burst window for the whole kit,
        // so it is the Lv25 passive cooldown, with A2's three charges fitting
        // inside it.
        var cfg = shipped();
        long window = cfg.getLong("abilities.definitions.gizmo.durationTicks");
        long fastestPassive = cfg.getLong("abilities.definitions.utility_belt.cooldownByLevel.level25");
        assertEquals(fastestPassive, window, "six seconds, both");
    }

    @Test void onlyComponentsIndependentOfInventoryAndTheTriggerAreEligible() {
        // The eligible list is produced by a rule -- a component qualifies only
        // if its effect depends on neither inventory geometry nor the damage
        // trigger -- but a world block cannot be asked about inventory
        // geometry, so the code encodes the rule's OUTPUT. This is where the
        // two are checked against each other.
        for (var m : new org.bukkit.Material[]{
                org.bukkit.Material.PISTON, org.bukkit.Material.STICKY_PISTON,
                org.bukkit.Material.HOPPER, org.bukkit.Material.DISPENSER,
                org.bukkit.Material.DROPPER, org.bukkit.Material.OBSERVER})
            assertNotNull(GizmoWindow.eligible(m), m + " qualifies under the rule");

        // Each excluded for its own reason, all from the same rule.
        assertNull(GizmoWindow.eligible(org.bukkit.Material.COMPARATOR), "needs the damage trigger");
        assertNull(GizmoWindow.eligible(org.bukkit.Material.TRIPWIRE_HOOK), "needs the slot below");
        assertNull(GizmoWindow.eligible(org.bukkit.Material.DAYLIGHT_DETECTOR), "needs the slot above");
        assertNull(GizmoWindow.eligible(org.bukkit.Material.REDSTONE), "needs being first, and the trigger");
        assertNull(GizmoWindow.eligible(org.bukkit.Material.REPEATER), "needs inventory flow order");
        assertNull(GizmoWindow.eligible(org.bukkit.Material.REDSTONE_TORCH), "a source, not a sink");
        assertNull(GizmoWindow.eligible(org.bukkit.Material.LEVER), "sources drive a circuit");
        assertNull(GizmoWindow.eligible(org.bukkit.Material.STONE_BUTTON));
        assertNull(GizmoWindow.eligible(org.bukkit.Material.STONE_PRESSURE_PLATE));
    }

    @Test void anActivationIsARisingEdgeAndNotASustainedSignal() {
        // "Activated" is read as RECEIVES a signal. A block held powered is one
        // activation, not one per tick -- otherwise a single lever would spend
        // the whole cap instantly.
        assertTrue(GizmoWindow.isRisingEdge(0, 15));
        assertFalse(GizmoWindow.isRisingEdge(15, 15), "already powered is not a new activation");
        assertFalse(GizmoWindow.isRisingEdge(15, 0), "falling is not an activation");
        assertFalse(GizmoWindow.isRisingEdge(0, 0));
        assertTrue(GizmoWindow.isRisingEdge(0, 1), "any strength counts");
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

    // ---- A2's numbers against classes.md -----------------------------------

    @Test void jumpstartinatorIsThreeChargesOnAnEightSecondRecharge() {
        // classes.md is explicit that this is NOT a flat cooldown, and why: a
        // flat 2s would be thirty uses a minute, while three charges on 8s is
        // a three-use burst and 7.5 a minute sustained.
        var a2 = shipped().getConfigurationSection("abilities.definitions.jumpstartinator");
        assertNotNull(a2);
        assertEquals(3, a2.getInt("charges"));
        assertEquals(160, a2.getLong("rechargeTicks"), "eight seconds");
        assertEquals(0, a2.getLong("cooldownTicks"),
                "charge-limited, so a cooldown would be a number that merely looks like one");
    }

    @Test void a2FiresTwoComponentsAndSuperCircuitFiresThree() {
        var a2 = shipped().getConfigurationSection("abilities.definitions.jumpstartinator");
        assertEquals(2, a2.getInt("components"));
        assertEquals(3, a2.getConfigurationSection("branches")
                         .getConfigurationSection("super_circuit").getInt("components"));
    }

    @Test void shortCircuitTriggersBelowTheBaseComponentCount() {
        // "If fewer than two components activate" -- so the threshold and the
        // base count must stay the same number, or the branch fires on a
        // perfectly healthy board or never fires at all.
        var a2 = shipped().getConfigurationSection("abilities.definitions.jumpstartinator");
        assertEquals(a2.getInt("components"), a2.getInt("shortCircuitBelow"));
    }

    @Test void a1AndA2OperateOnTheSameEndOfTheBoard() {
        // The loop classes.md describes: A1 swaps the first components, A2
        // fires the first components. If these diverged, A1 would stop being
        // A2's loadout editor and the design's answer to "why reconfigure
        // mid-fight" would quietly stop being true.
        var cfg = shipped();
        int swapped = cfg.getConfigurationSection("abilities.definitions.reconfiguratron").getInt("depth");
        int fired = cfg.getConfigurationSection("abilities.definitions.jumpstartinator").getInt("components");
        assertEquals(swapped, fired, "A1 swaps exactly the components A2 fires");
    }

    @Test void twoCircuitRunsDoNotShareOriginOrAmplification() {
        // The refactor A2 required. These were maps keyed by PLAYER, which
        // worked only because the passive cannot overlap itself; A2 fires the
        // same components on its own charges, so two runs can now be live at
        // once for one player.
        var first = new UtilityBelt.Run();
        var second = new UtilityBelt.Run();

        first.amplification = 2;
        first.projectedOrigin = java.util.UUID.randomUUID();

        assertEquals(0, second.amplification, "a second run starts unamplified");
        assertNull(second.projectedOrigin, "and aimed at nobody");

        second.amplification = 0;
        assertEquals(2, first.amplification, "and clearing it does not clear the first run's");
    }
}
