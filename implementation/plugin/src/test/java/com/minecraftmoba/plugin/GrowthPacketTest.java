package com.minecraftmoba.plugin;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.minecraftmoba.plugin.GrowthPacket.Family.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * The Growth packet, and the blocker it exists to clear.
 *
 * The decisive test is {@link #skeletonCrewsLevelSixHoldsTwoEffectsFromTwoFamilies}:
 * under the superseded one-effect-per-level map, that class could not be
 * configured at all.
 */
class GrowthPacketTest {

    private YamlConfiguration yaml(String body) {
        var c = new YamlConfiguration();
        try { c.loadFromString(body); } catch (Exception e) { throw new AssertionError(e); }
        return c;
    }

    // ---- the blocker ------------------------------------------------------

    @Test void skeletonCrewsLevelSixHoldsTwoEffectsFromTwoFamilies() {
        var cfg = yaml("""
                growth:
                  level6:
                    infrastructure:
                      - { dimension: eligibility, effect: supply_line }
                    methodology:
                      - { effect: signal_flares, tier: 1 }
                """);
        var packets = GrowthPacket.load(cfg.getConfigurationSection("growth"));
        var six = packets.get(6);
        assertEquals(2, six.effects().size());

        var infrastructure = six.of(INFRASTRUCTURE);
        assertEquals(1, infrastructure.size());
        assertEquals("supply_line", infrastructure.getFirst().effect());
        assertEquals("eligibility", infrastructure.getFirst().dimension());

        var methodology = six.of(METHODOLOGY);
        assertEquals(1, methodology.size());
        assertEquals("signal_flares", methodology.getFirst().effect());
        assertEquals(1, methodology.getFirst().tier());
    }

    /**
     * The point of families rather than a longer list.
     *
     * Night Efficiency is METHODOLOGY, not INFRASTRUCTURE. A
     * Map&lt;level, List&lt;String&gt;&gt; of infrastructure strings could only
     * have recorded it by lying about which family it belongs to -- rebuilding
     * the Growth-is-Infrastructure confusion one level down.
     */
    @Test void twoEffectsAtALevelNeedNotShareAFamily() {
        var cfg = yaml("""
                growth:
                  level18:
                    methodology:
                      - { effect: signal_flares, tier: 2 }
                      - { effect: crew_combat_development, tier: 1 }
                """);
        var eighteen = GrowthPacket.load(cfg.getConfigurationSection("growth")).get(18);
        assertEquals(2, eighteen.of(METHODOLOGY).size());
        assertTrue(eighteen.of(INFRASTRUCTURE).isEmpty());
        assertTrue(eighteen.of(PERSONAL).isEmpty());
    }

    // ---- loading ----------------------------------------------------------

    @Test void allThreeFamiliesLoad() {
        var cfg = yaml("""
                growth:
                  level9:
                    personal:
                      - { dimension: class_resource, effect: crew_capacity, tier: 2 }
                    infrastructure:
                      - { dimension: reach, effect: supply_line_reach, tier: 1 }
                    methodology:
                      - { effect: worker_scheduling }
                """);
        var nine = GrowthPacket.load(cfg.getConfigurationSection("growth")).get(9);
        assertEquals(1, nine.of(PERSONAL).size());
        assertEquals(1, nine.of(INFRASTRUCTURE).size());
        assertEquals(1, nine.of(METHODOLOGY).size());
        assertEquals(0, nine.of(METHODOLOGY).getFirst().tier(), "an ungraded effect is tier 0");
    }

    @Test void anEffectThatNamesNothingIsRefused() {
        var cfg = yaml("""
                growth:
                  level6:
                    methodology:
                      - { dimension: reach, tier: 1 }
                """);
        var thrown = assertThrows(IllegalStateException.class,
                () -> GrowthPacket.load(cfg.getConfigurationSection("growth")));
        assertTrue(thrown.getMessage().contains("level6"), thrown.getMessage());
    }

    @Test void aMalformedLevelKeyIsRefused() {
        var cfg = yaml("""
                growth:
                  sixth:
                    methodology:
                      - { effect: signal_flares }
                """);
        assertThrows(IllegalStateException.class,
                () -> GrowthPacket.load(cfg.getConfigurationSection("growth")));
    }

    @Test void noGrowthSectionIsNotAnError() {
        assertTrue(GrowthPacket.load(null).isEmpty());
    }

    // ---- the superseded shape --------------------------------------------

    /** Translated as what it always meant: one infrastructure effect, dimension unstated. */
    @Test void legacyProgressionTranslatesToOneInfrastructureEffect() {
        var packets = GrowthPacket.fromLegacy(Map.of("level18", "routes_establish"));
        var eighteen = packets.get(18);
        assertEquals(1, eighteen.effects().size());
        assertEquals(INFRASTRUCTURE, eighteen.effects().getFirst().family());
        assertEquals("routes_establish", eighteen.effects().getFirst().effect());
        assertEquals("", eighteen.effects().getFirst().dimension());
    }

    /** A migrated level must not end up holding both its old effect and its replacement. */
    @Test void authoredPacketsWinOutrightOverLegacyAtTheSameLevel() {
        var legacy = GrowthPacket.fromLegacy(Map.of("level6", "routes", "level12", "traversal"));
        var authored = Map.of(6, new GrowthPacket(6,
                List.of(new GrowthPacket.Effect(METHODOLOGY, "", "signal_flares", 1))));
        var merged = GrowthPacket.merge(legacy, authored);

        assertEquals(1, merged.get(6).effects().size());
        assertEquals(METHODOLOGY, merged.get(6).effects().getFirst().family());
        assertEquals("traversal", merged.get(12).effects().getFirst().effect(), "untouched levels survive");
        assertEquals(INFRASTRUCTURE, merged.get(12).effects().getFirst().family());
    }

    // ---- ClassDefinition --------------------------------------------------

    @Test void theSevenArgumentFormStillCarriesLegacyProgression() {
        var definition = new ClassDefinition("mole", "Mole", "mole_digging", "mole",
                Map.of("level18", "routes_establish"), Map.of(), Map.of());
        assertEquals("routes_establish",
                definition.growthAt(18).of(INFRASTRUCTURE).getFirst().effect());
        assertEquals(java.util.Set.of(18), definition.growthLevels());
    }

    /** Budgets need not be equal, so a level with no packet is legitimate. */
    @Test void aLevelWithNoPacketIsEmptyRatherThanNull() {
        var definition = new ClassDefinition("x", null, null, null, Map.of(), Map.of(), Map.of());
        assertTrue(definition.growthAt(15).isEmpty());
        assertNotNull(definition.growthAt(15).effects());
    }

    @Test void degenerateEffectsAreRefused() {
        assertThrows(NullPointerException.class,
                () -> new GrowthPacket.Effect(null, "", "x"));
        assertThrows(IllegalArgumentException.class,
                () -> new GrowthPacket.Effect(METHODOLOGY, "", "  "));
        assertThrows(IllegalArgumentException.class,
                () -> new GrowthPacket.Effect(METHODOLOGY, "", "x", -1));
    }

    // ---- the shipped file -------------------------------------------------

    @Test void theShippedConfigHoldsSkeletonCrewsTwoEffectLevels() throws Exception {
        var cfg = YamlConfiguration.loadConfiguration(new InputStreamReader(
                Objects.requireNonNull(getClass().getResourceAsStream("/config.yml"))));
        var crew = cfg.getConfigurationSection("abilities.classes.skeleton_crew");
        assertNotNull(crew, "skeleton_crew is the class the old model could not express");
        var packets = GrowthPacket.load(crew.getConfigurationSection("growth"));

        assertEquals(2, packets.get(6).effects().size(), "Lv6 is Supply Line access AND Signal Flares I");
        assertEquals(1, packets.get(6).of(INFRASTRUCTURE).size());
        assertEquals(1, packets.get(6).of(METHODOLOGY).size());
        // Lv18 carries one effect, not two. `crew_combat_development` was
        // removed: Growth does not touch abilities, and developing the crew's
        // combat develops an ability's output. Lv6 is the two-effect proof and
        // is the level the blocker was actually recorded against.
        assertEquals(1, packets.get(18).effects().size(), "Lv18 is Signal Flares II alone");
        assertTrue(packets.get(18).of(METHODOLOGY).stream()
                        .noneMatch(e -> e.effect().contains("combat")),
                "Growth must not carry an ability's development");
    }

    /** Mole is unmigrated and must keep the progression it already had. */
    @Test void theShippedConfigStillReadsMolesLegacyProgression() throws Exception {
        var cfg = YamlConfiguration.loadConfiguration(new InputStreamReader(
                Objects.requireNonNull(getClass().getResourceAsStream("/config.yml"))));
        var mole = cfg.getConfigurationSection("abilities.classes.mole");
        assertNotNull(mole);
        var legacy = mole.getConfigurationSection("infrastructureProgression");
        assertNotNull(legacy, "Mole is deliberately not migrated yet");
        var packets = GrowthPacket.fromLegacy(
                legacy.getKeys(false).stream().collect(
                        java.util.stream.Collectors.toMap(k -> k, legacy::getString)));
        assertEquals("routes_establish", packets.get(18).of(INFRASTRUCTURE).getFirst().effect());
        assertEquals(5, packets.size(), "all five of Mole's authored levels survive translation");
    }
}
