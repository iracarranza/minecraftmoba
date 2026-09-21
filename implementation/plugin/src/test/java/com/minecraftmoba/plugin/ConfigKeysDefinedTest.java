package com.minecraftmoba.plugin;

import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Every config key any plugin class reads must exist in config.yml.
 *
 * `getBoolean(path)` returns false for a key that is absent, and
 * `getInt(path, default)` cannot distinguish a missing key from one that
 * happens to equal the default. A whole feature can therefore be written,
 * committed, deployed and never run, with nothing in the log to say so.
 *
 * That is not hypothetical. It had happened to six features at once. The tome,
 * the minimap and recall were fully implemented and shipped permanently
 * disabled because `features.tome.enabled`, `features.minimap.*` and
 * `features.recall.*` were never added to the file; `progression.work.*` had
 * the same defect and was silently serving Java fallbacks in place of the
 * documented Alpha fixtures.
 *
 * A source scan is the only check that catches this without a running server,
 * so the scan is the test. It is deliberately whole-package: the earlier
 * version of this test scanned WorkPoints alone and would have passed happily
 * against a dead tome.
 */
class ConfigKeysDefinedTest {

    private static final Pattern READ = Pattern.compile(
            "get(?:Int|Long|Double|Boolean|String|StringList|ConfigurationSection)\\(\\s*\"([a-zA-Z][A-Za-z0-9_.]*)\"");

    @SuppressWarnings("unchecked")
    private Map<String, Object> config() throws Exception {
        try (InputStream in = Files.newInputStream(Path.of("src/main/resources/config.yml"))) {
            return (Map<String, Object>) new Yaml().load(in);
        }
    }

    @SuppressWarnings("unchecked")
    private boolean defined(Map<String, Object> root, String dotted) {
        Object node = root;
        for (String part : dotted.split("\\.")) {
            if (!(node instanceof Map)) return false;
            node = ((Map<String, Object>) node).get(part);
            if (node == null) return false;
        }
        return true;
    }

    @Test void noClassReadsAKeyTheFileDoesNotDefine() throws Exception {
        var cfg = config();
        var missing = new TreeMap<String, Set<String>>();
        try (Stream<Path> sources = Files.walk(Path.of("src/main/java/com/minecraftmoba/plugin"))) {
            for (Path src : sources.filter(f -> f.toString().endsWith(".java")).toList()) {
                Matcher m = READ.matcher(Files.readString(src));
                while (m.find()) {
                    String path = m.group(1);
                    // A single segment is not a config path, and a trailing dot
                    // is a computed lookup such as "...opportunity." + kind --
                    // there the SECTION must exist, not the runtime leaf.
                    if (!path.contains(".")) continue;
                    String check = path.endsWith(".") ? path.substring(0, path.length() - 1) : path;
                    if (!defined(cfg, check))
                        missing.computeIfAbsent(path, k -> new TreeSet<>())
                               .add(src.getFileName().toString());
                }
            }
        }
        assertTrue(missing.isEmpty(),
                "config.yml does not define keys the plugin reads, so these run on Java "
                + "fallbacks or silently disabled: " + missing);
    }

    @Test void theTomeAndItsTwoDependentsAreActuallyEnabled() throws Exception {
        // The tome is one item doing three jobs; two of the three are only
        // reachable if their own feature flag is on, and both were absent.
        var cfg = config();
        assertEquals(Boolean.TRUE, at(cfg, "features.tome.enabled"));
        assertEquals(Boolean.TRUE, at(cfg, "features.minimap.enabled"), "the map surface");
        assertEquals(Boolean.TRUE, at(cfg, "features.recall.enabled"), "the lift gesture");
    }

    @SuppressWarnings("unchecked")
    private Object at(Map<String, Object> root, String dotted) {
        Object node = root;
        for (String part : dotted.split("\\.")) node = ((Map<String, Object>) node).get(part);
        return node;
    }
}
