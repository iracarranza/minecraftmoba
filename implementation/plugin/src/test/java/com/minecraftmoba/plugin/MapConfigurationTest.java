package com.minecraftmoba.plugin;

import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.GZIPInputStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * A match's map is the base terrain plus one authored configuration.
 *
 * The properties worth pinning are the ones whose failure is silent: a diff
 * applied to the wrong base produces a world that looks plausible and is wrong
 * everywhere, and a catalogue that quietly falls back to one entry produces
 * "different every match" that never differs.
 */
class MapConfigurationTest {

    private static final Path MAPS =
            Path.of("../../artifacts/worldgen/alpha-0.1/maps");

    private static String read(Path file) throws Exception {
        try (var r = new InputStreamReader(
                new GZIPInputStream(Files.newInputStream(file)), StandardCharsets.UTF_8)) {
            var sb = new StringBuilder();
            char[] buf = new char[8192];
            for (int n; (n = r.read(buf)) > 0; ) sb.append(buf, 0, n);
            return sb.toString();
        }
    }

    private static String field(String json, String key) {
        int i = json.indexOf("\"" + key + "\"");
        assertTrue(i > 0, "diff has no " + key);
        int start = json.indexOf('"', json.indexOf(':', i)) + 1;
        return json.substring(start, json.indexOf('"', start));
    }

    @Test void everyConfiguredMapExistsAndDeclaresItsBase() throws Exception {
        if (!Files.isDirectory(MAPS)) return;   // artifacts are not in git
        for (String name : List.of("consolidative", "resource_light")) {
            Path file = MAPS.resolve(name + ".json.gz");
            assertTrue(Files.exists(file), "missing configuration " + file);
            String json = read(file);
            assertTrue(field(json, "schema").startsWith("moba_map_diff/"));
            assertEquals(name, field(json, "name"));
            assertEquals(64, field(json, "base_fingerprint").length(),
                    "a diff must name the base it was computed against");
        }
    }

    @Test void configurationsInTheSameCatalogueShareABase() throws Exception {
        if (!Files.isDirectory(MAPS)) return;
        String a = field(read(MAPS.resolve("consolidative.json.gz")), "base_fingerprint");
        String b = field(read(MAPS.resolve("resource_light.json.gz")), "base_fingerprint");
        assertEquals(a, b,
                "two maps drawn for the same match series must apply to the same terrain");
    }

    @Test void everyPlayableConfigurationPlacesTheFountains() throws Exception {
        // A map without Fountains is unplayable: alpha.homelands is pinned in
        // config, ALPHA-D3 decides victory by Fountain state and ALPHA-D4 makes
        // it where a player reconstructs. Re-authoring once shipped without
        // them, because building the team structures is a separate step from
        // authoring the portfolio, and the symptom was spawning in a field.
        if (!Files.isDirectory(MAPS)) return;
        for (String name : List.of("consolidative_v2", "resource_light_v2")) {
            String json = read(MAPS.resolve(name + ".json.gz"));
            assertTrue(json.contains("chiseled_quartz_block"),
                    name + " has no Fountain material; the team structures step was skipped");
        }
    }

    @Test void theFingerprintCheckRefusesRatherThanWarns() throws Exception {
        // The silent-failure case: a diff on the wrong terrain is a world that
        // looks fine and is wrong everywhere.
        String src = Files.readString(
                Path.of("src/main/java/com/minecraftmoba/plugin/MapConfigurations.java"));
        int i = src.indexOf("public MapDiff applyTo(");
        String body = src.substring(i, src.indexOf("\n    }", i));
        assertTrue(body.contains("throw new IllegalStateException"),
                "a base mismatch must stop the match open");
        assertTrue(body.contains("baseFingerprint.equals"), "the check must actually compare");
    }

    @Test void aBrokenConfigurationIsFatalNotSkipped() throws Exception {
        // Skipping one would silently shrink the catalogue, and a catalogue of
        // one is "different every match" that never differs.
        String src = Files.readString(
                Path.of("src/main/java/com/minecraftmoba/plugin/MapConfigurations.java"));
        int i = src.indexOf("public int reload()");
        String body = src.substring(i, src.indexOf("\n    }", i));
        assertTrue(body.contains("throw new IllegalStateException"),
                "an unreadable configuration must fail loudly");
    }

    @Test void selectionIsDrawnPerMatchUnlessPinned() throws Exception {
        String src = Files.readString(
                Path.of("src/main/java/com/minecraftmoba/plugin/MapConfigurations.java"));
        int i = src.indexOf("public MapDiff choose()");
        String body = src.substring(i, src.indexOf("\n    }", i));
        assertTrue(body.contains("random.nextInt"), "a match should draw its map");
        assertTrue(body.contains("force"), "a controlled test must be able to pin one");
        assertTrue(body.contains("orElseThrow"),
                "forcing an unknown map must fail rather than silently drawing at random");
    }

    @Test void applyingHappensOnLoadSoResetRedrawsToo() throws Exception {
        // Reset IS load, so a reset must get a fresh draw rather than repeating
        // the map the previous match happened to get.
        String src = Files.readString(
                Path.of("src/main/java/com/minecraftmoba/plugin/WorldInstance.java"));
        int i = src.indexOf("public World load()");
        String body = src.substring(i, src.indexOf("\n    }", i));
        assertTrue(body.contains("applyTo"), "configurations must be applied where the world loads");
    }
}
