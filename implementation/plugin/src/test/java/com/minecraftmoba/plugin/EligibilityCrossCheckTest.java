package com.minecraftmoba.plugin;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The eligibility predicate exists twice, and both copies must agree.
 *
 * The plugin's copy is authoritative but can only run against a loaded Bukkit
 * world. The offline simulation in implementation/worldgen has to answer "given
 * the frozen template as it stands, where could each opportunity manifest?" --
 * a question that must be answerable before any re-freeze and that no running
 * server can answer about a world it has not loaded.
 *
 * So there are two implementations, which is a real cost, and this is what makes
 * it survivable: both run fixtures/eligibility-crosscheck.json and must produce
 * the same loci. If they diverge, the simulation is lying about the map, and
 * this test says so rather than the map review saying it later.
 *
 * Deliberately parsed by hand rather than by adding a JSON dependency: the
 * fixture is small, and the point of the file is that both sides read the SAME
 * bytes.
 */
class EligibilityCrossCheckTest {

    private static final Path FIXTURE =
            Path.of("../worldgen/fixtures/eligibility-crosscheck.json");

    /** The fixture's terrain, as a TerrainView. */
    private static final class FixtureTerrain implements TerrainView {
        final Map<Long, Material> ground = new HashMap<>();
        final Map<Long, Integer> height = new HashMap<>();
        final Set<Long> obstructed = new HashSet<>();
        final Set<Long> built = new HashSet<>();

        static long key(int x, int z) { return ((long) x << 32) | (z & 0xFFFFFFFFL); }

        @Override public int surfaceY(int x, int z) {
            return height.getOrDefault(key(x, z), Integer.MIN_VALUE);
        }
        @Override public Material blockAt(int x, int y, int z) {
            Integer top = height.get(key(x, z));
            if (top == null) return Material.AIR;
            if (y == top) return ground.get(key(x, z));
            if (y == top + 1 && obstructed.contains(key(x, z))) return Material.STONE;
            return y > top ? Material.AIR : Material.STONE;
        }
        @Override public boolean isPlayerPlaced(int x, int y, int z) {
            Integer top = height.get(key(x, z));
            return built.contains(key(x, z)) && top != null && y == top;
        }
        @Override public double distanceToNearestPlayer(int x, int y, int z) {
            return Double.MAX_VALUE;
        }
    }

    private static Material material(String name) {
        Material m = Material.matchMaterial(name.startsWith("minecraft:") ? name : "minecraft:" + name);
        assertNotNull(m, "fixture names a material that does not exist: " + name);
        return m;
    }

    @Test void theJavaPredicateMatchesTheSharedFixture() throws Exception {
        assertTrue(Files.exists(FIXTURE), "shared fixture missing at " + FIXTURE.toAbsolutePath());
        String json = Files.readString(FIXTURE);

        var terrain = new FixtureTerrain();
        Matcher surface = Pattern.compile(
                "\"(-?\\d+),(-?\\d+)\"\\s*:\\s*\\[\"([a-z_]+)\",\\s*(-?\\d+)\\]").matcher(json);
        while (surface.find()) {
            int x = Integer.parseInt(surface.group(1)), z = Integer.parseInt(surface.group(2));
            terrain.ground.put(FixtureTerrain.key(x, z), material(surface.group(3)));
            terrain.height.put(FixtureTerrain.key(x, z), Integer.parseInt(surface.group(4)));
        }
        assertFalse(terrain.height.isEmpty(), "parsed no terrain from the fixture");
        parsePairs(json, "obstructed").forEach(p -> terrain.obstructed.add(FixtureTerrain.key(p[0], p[1])));
        parsePairs(json, "playerPlaced").forEach(p -> terrain.built.add(FixtureTerrain.key(p[0], p[1])));

        var rules = new Eligibility.Rules(
                intField(json, "headroom"),
                groundSet(json),
                intField(json, "sampleStride"),
                0.0, 0.0, json.contains("\"rejectPlayerPlaced\": true"));

        var region = new OpportunityRegion(List.of(new OpportunityRegion.Cell(
                intField(json, "minX"), intField(json, "minZ"),
                intField(json, "maxX"), intField(json, "maxZ"))));

        var got = new TreeSet<String>();
        for (var l : Eligibility.loci(region, terrain, rules))
            got.add(l.x() + "," + l.y() + "," + l.z());

        var want = new TreeSet<String>();
        Matcher expected = Pattern.compile("\\[(-?\\d+),\\s*(-?\\d+),\\s*(-?\\d+)\\]")
                .matcher(json.substring(json.indexOf("\"expected\"")));
        while (expected.find())
            want.add(expected.group(1) + "," + expected.group(2) + "," + expected.group(3));

        assertEquals(want.size(), 17, "the fixture's expected set changed shape");
        assertEquals(want, got,
                "the plugin and the offline simulation disagree about the same terrain; "
                        + "the simulation is reporting a map that the game would not produce");
    }

    private static List<int[]> parsePairs(String json, String field) {
        int start = json.indexOf("\"" + field + "\"");
        if (start < 0) return List.of();
        String body = json.substring(start, json.indexOf(']', start));
        var out = new ArrayList<int[]>();
        Matcher m = Pattern.compile("\"(-?\\d+),(-?\\d+)\"").matcher(body);
        while (m.find()) out.add(new int[]{Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2))});
        return out;
    }

    private static int intField(String json, String name) {
        Matcher m = Pattern.compile("\"" + name + "\"\\s*:\\s*(-?\\d+)").matcher(json);
        assertTrue(m.find(), "fixture has no " + name);
        return Integer.parseInt(m.group(1));
    }

    private static Set<Material> groundSet(String json) {
        int start = json.indexOf("\"naturalGround\"");
        String body = json.substring(start, json.indexOf(']', start));
        var set = new HashSet<Material>();
        Matcher m = Pattern.compile("\"([a-z_]+)\"").matcher(body.substring(body.indexOf('[')));
        while (m.find()) set.add(material(m.group(1)));
        return set;
    }
}
