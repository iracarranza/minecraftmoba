package com.minecraftmoba.plugin;

import org.bukkit.entity.EntityType;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Swarm eligibility: biome, time, and one opportunity owning both tables.
 *
 * These pin the 2026-09-29 doctrine rather than any balance: an opportunity is
 * spatial and persists, and what may occupy it depends on the biome under the
 * locus and the current time of day.
 */
class SwarmTableTest {
    private static final Eligibility.Rules RULES = new Eligibility.Rules(
            2, Eligibility.NATURAL_GROUND, 4, 12.0, 16.0, true);
    private static final String PEAKS = "minecraft:jagged_peaks";
    private static final String PLAINS = "minecraft:plains";

    private static SwarmDefinition def(String id, EntityType type, Set<String> biomes,
                                       SwarmDefinition.Time time) {
        return new SwarmDefinition(id, List.of(new SwarmDefinition.Member(type, 1)), 2,
                biomes, time, null, null, 1);
    }

    /** West half mountain, east half plains, all flat and open. */
    private static FakeTerrain split() {
        return new FakeTerrain().grass(0, 0, 63, 63, 64)
                .biome(0, 0, 31, 63, PEAKS).biome(32, 0, 63, 63, PLAINS);
    }

    private static OpportunityRegion region() {
        return new OpportunityRegion(List.of(new OpportunityRegion.Cell(0, 0, 63, 63)));
    }

    @Test void aMountainSwarmOnlyManifestsUnderAMountainBiome() {
        var table = SwarmDefinitions.table(List.of("mountain_ravager"));
        var random = new Random(1);
        for (int i = 0; i < 40; i++) {
            var pick = table.pick(region(), split(), RULES, null, false, random);
            assertNotNull(pick);
            assertTrue(pick.locus().x() <= 31, "chosen under the mountain half, not the plains");
        }
    }

    @Test void aDaytimeSwarmHasNothingToManifestAtNight() {
        var table = SwarmDefinitions.table(List.of("mountain_ravager"));
        assertNull(table.pick(region(), split(), RULES, null, true, new Random(1)),
                "an out-of-window opportunity stays ready rather than forcing a spawn");
    }

    @Test void anUnreportableBiomeNeverQualifies() {
        var table = SwarmDefinitions.table(List.of("mountain_ravager"));
        var blind = new FakeTerrain().grass(0, 0, 63, 63, 64);   // no biome painted
        assertNull(table.pick(region(), blind, RULES, null, false, new Random(1)),
                "an unknown biome is not guessed into an eligible one");
    }

    @Test void oneOpportunityOwnsBothTimesOfDay() {
        var day = def("day_pack", EntityType.SPIDER, Set.of("mountain"), SwarmDefinition.Time.DAY);
        var night = def("night_pack", EntityType.STRAY, Set.of("mountain"), SwarmDefinition.Time.NIGHT);
        var table = new SwarmTable(List.of(day, night));
        var random = new Random(3);
        assertEquals("day_pack", table.pick(region(), split(), RULES, null, false, random).definition().id());
        assertEquals("night_pack", table.pick(region(), split(), RULES, null, true, random).definition().id(),
                "the same opportunity, a different definition, chosen at the next manifestation");
    }

    @Test void anAllDayDefinitionServesBothTables() {
        var d = def("always", EntityType.SLIME, Set.of("mushroom"), SwarmDefinition.Time.ALL);
        assertTrue(d.eligibleAt("minecraft:mushroom_fields", false));
        assertTrue(d.eligibleAt("minecraft:mushroom_fields", true));
        assertFalse(d.eligibleAt(PLAINS, true));
    }

    @Test void biomeMatchingAcceptsGroupsAndLiteralIds() {
        var d = def("mixed", EntityType.CREEPER, Set.of("desert", "minecraft:beach"),
                SwarmDefinition.Time.DAY);
        assertTrue(d.eligibleAt("minecraft:desert", false));
        assertTrue(d.eligibleAt("minecraft:beach", false));
        assertFalse(d.eligibleAt("minecraft:snowy_beach", false), "literal id is exact, group is not implied");
    }

    @Test void definitionsThatWouldBeInertAreRefused() {
        assertThrows(IllegalArgumentException.class, () -> new SwarmDefinition("x",
                List.of(new SwarmDefinition.Member(EntityType.WARDEN, 1)), 1, Set.of("mountain"),
                SwarmDefinition.Time.DAY, null, null, 1), "boss-class is not an ordinary swarm");
        assertThrows(IllegalArgumentException.class, () -> new SwarmDefinition("x",
                List.of(new SwarmDefinition.Member(EntityType.RAVAGER, 1)), 1, Set.of("mountain"),
                SwarmDefinition.Time.DAY, "sand_to_quartz", null, 1), "behavior registry is empty");
        assertThrows(IllegalArgumentException.class, () -> new SwarmDefinition("x",
                List.of(new SwarmDefinition.Member(EntityType.RAVAGER, 1)), 1, Set.of("volcano"),
                SwarmDefinition.Time.DAY, null, null, 1), "not a group or biome id");
        assertThrows(IllegalArgumentException.class, () -> new SwarmDefinition("x",
                List.of(), 1, Set.of("mountain"), SwarmDefinition.Time.DAY, null, null, 1));
    }

    @Test void compositionFollowsWeights() {
        var d = new SwarmDefinition("w", List.of(
                new SwarmDefinition.Member(EntityType.ZOMBIE, 9),
                new SwarmDefinition.Member(EntityType.WITCH, 1)), 500,
                Set.of("mountain"), SwarmDefinition.Time.DAY, null, null, 1);
        var drawn = d.compose(new Random(7));
        assertEquals(500, drawn.size());
        long zombies = drawn.stream().filter(t -> t == EntityType.ZOMBIE).count();
        assertTrue(zombies > 400 && zombies < 490, "about 90% zombies, got " + zombies);
    }

    @Test void theWorldgenFixtureAndBiomeGroupsAgree() throws Exception {
        String json = Files.readString(Path.of("../worldgen/fixtures/swarm-biome-groups.json"));
        var found = new TreeMap<String, Set<String>>();
        var m = Pattern.compile("\"(\\w+)\":\\s*\\[([^\\]]*)\\]").matcher(json);
        while (m.find()) {
            var ids = new TreeSet<String>();
            var id = Pattern.compile("\"([a-z_]+:[a-z_]+)\"").matcher(m.group(2));
            while (id.find()) ids.add(id.group(1));
            found.put(m.group(1), ids);
        }
        var expected = new TreeMap<String, Set<String>>();
        BiomeGroups.GROUPS.forEach((k, v) -> expected.put(k, new TreeSet<>(v)));
        assertEquals(expected, found, "compiler and plugin must read the same ecological categories");
    }
}
