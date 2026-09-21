package com.minecraftmoba.plugin;

import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Every config key the progression code reads must exist in config.yml.
 *
 * This exists because it did not. WorkPoints read
 * `progression.work.extraction.*` while config.yml defined
 * `progression.extraction.*`, so every Extraction value in a live match came
 * from the Java fallback argument instead of the documented Alpha fixture --
 * silently, because `getInt(path, default)` cannot tell a missing key from a
 * key that happens to equal the default. Nothing crashed and nothing warned;
 * the file simply was not the authority it looked like.
 *
 * A key-by-key scan of the source is the only check that catches this class of
 * bug without a running server, so the scan is the test.
 */
class ProgressionConfigTest {

    private static final Path SOURCE =
            Path.of("src/main/java/com/minecraftmoba/plugin/WorkPoints.java");

    @SuppressWarnings("unchecked")
    private Map<String, Object> config() throws Exception {
        try (InputStream in = Files.newInputStream(Path.of("src/main/resources/config.yml"))) {
            return (Map<String, Object>) new Yaml().load(in);
        }
    }

    @SuppressWarnings("unchecked")
    private Object at(Map<String, Object> root, String dotted) {
        Object node = root;
        for (String part : dotted.split("\\.")) {
            if (!(node instanceof Map)) return null;
            node = ((Map<String, Object>) node).get(part);
            if (node == null) return null;
        }
        return node;
    }

    /** Config paths WorkPoints reads, including the prefix of a computed one. */
    private List<String> pathsRead() throws Exception {
        String src = Files.readString(SOURCE);
        Pattern p = Pattern.compile("get(?:Int|Long|Double|Boolean|String|ConfigurationSection)\\(\\s*\"(progression\\.[^\"]+)\"");
        Matcher m = p.matcher(src);
        List<String> out = new ArrayList<>();
        while (m.find()) out.add(m.group(1));
        assertFalse(out.isEmpty(), "found no progression config reads; has the source moved?");
        return out;
    }

    @Test void everyProgressionKeyTheCodeReadsIsDefined() throws Exception {
        var cfg = config();
        List<String> missing = new ArrayList<>();
        for (String path : pathsRead()) {
            // A path ending in '.' is a computed lookup such as
            // "...opportunity." + kind; the section must exist even though the
            // leaf is chosen at runtime.
            String check = path.endsWith(".") ? path.substring(0, path.length() - 1) : path;
            if (at(cfg, check) == null) missing.add(path);
        }
        assertTrue(missing.isEmpty(), "WorkPoints reads keys config.yml does not define: " + missing);
    }

    @Test void configuredValuesDifferFromTheJavaFallbacks() throws Exception {
        // If the file only ever repeated the fallbacks, the scan above could
        // pass while the file was still doing no work. These are values that
        // exist only because config.yml says so.
        var cfg = config();
        assertEquals(2, at(cfg, "progression.work.extraction.opportunity.iron"),
                "iron opportunity is 2 in config against a Java fallback of 0");
        assertEquals(4, at(cfg, "progression.work.extraction.opportunity.diamond"));
        assertEquals(6, at(cfg, "progression.work.extraction.opportunity.ancient_debris"));
        assertEquals(0, at(cfg, "progression.work.extraction.opportunity.default"));
    }

    @Test void ordinaryPlacementIsFlaggedUnresolvedRatherThanAssumedUseful() throws Exception {
        // Paying every BlockPlaceEvent made logs, torches and dirt into
        // Construction progression. The fixture is preserved in the file; what
        // is not asserted is that placement is useful.
        assertEquals(0, at(config(), "progression.work.ordinaryPlacement"));
        assertEquals(2, at(config(), "progression.work.constructionBlockPlacement"));
    }

    @Test void theBandCurveInstantiatesTheEstablishedRequirementIndices() throws Exception {
        // 1.000 / 1.350 / 1.875 / 2.575 / 3.250 against a 40 WP Bootstrap
        // baseline. 40 is the Bootstrap per-level cost, NOT a universal one.
        var cfg = config();
        int base = (Integer) at(cfg, "progression.bands.1-6");
        assertEquals(40, base, "ALPHA CALIBRATION baseline");
        double[] indices = {1.000, 1.350, 1.875, 2.575, 3.250};
        String[] bands = {"1-6", "7-12", "13-19", "20-24", "25-30"};
        for (int i = 0; i < bands.length; i++) {
            int actual = (Integer) at(cfg, "progression.bands." + bands[i]);
            assertEquals(Math.round(base * indices[i]), actual,
                    "band " + bands[i] + " must instantiate index " + indices[i]);
        }
    }

    @Test void everyProductionCategoryHasAFixture() throws Exception {
        var cfg = config();
        for (ProductionRecipes.Output out : ProductionRecipes.Output.values()) {
            if (out == ProductionRecipes.Output.UNRESOLVED) continue;
            String key = "progression.work.production." + out.name().toLowerCase();
            assertNotNull(at(cfg, key), "a recognized category with no fixture pays nothing: " + key);
        }
    }
}
