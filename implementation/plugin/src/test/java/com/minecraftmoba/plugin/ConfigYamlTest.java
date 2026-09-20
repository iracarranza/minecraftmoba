package com.minecraftmoba.plugin;

import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

/**
 * config.yml must parse, and must still contain the keys the plugin reads.
 *
 * This exists because config.yml has been broken twice by textual edits that
 * moved a key into the wrong block. Both times it compiled, shipped, and only
 * failed at server start with "Cannot load configuration from stream" — after
 * a full deploy and restart cycle. A parse failure should cost a second, not a
 * deploy.
 */
class ConfigYamlTest {
    private Map<String, Object> load() throws Exception {
        Path path = Path.of("src/main/resources/config.yml");
        assertTrue(Files.exists(path), "config.yml missing at " + path.toAbsolutePath());
        try (InputStream in = Files.newInputStream(path)) {
            Object parsed = new Yaml().load(in);
            assertInstanceOf(Map.class, parsed, "config.yml must be a mapping");
            @SuppressWarnings("unchecked") Map<String, Object> map = (Map<String, Object>) parsed;
            return map;
        }
    }

    @SuppressWarnings("unchecked")
    private Object at(Map<String, Object> root, String dotted) {
        Object node = root;
        for (String part : dotted.split("\\.")) {
            assertInstanceOf(Map.class, node, "not a mapping while resolving " + dotted);
            node = ((Map<String, Object>) node).get(part);
            assertNotNull(node, "missing config key: " + dotted);
        }
        return node;
    }

    @Test void parses() throws Exception { assertFalse(load().isEmpty()); }

    /**
     * SnakeYAML silently keeps the last of two duplicate top-level keys, so a
     * second `progression:` block discarded an entire section of config while
     * parsing cleanly and leaving the code to fall back on defaults. Nothing
     * reported it; Efficiency simply had no effect.
     */
    @Test void hasNoDuplicateTopLevelKeys() throws Exception {
        var seen = new java.util.HashSet<String>();
        var duplicates = new java.util.ArrayList<String>();
        for (String line : Files.readAllLines(Path.of("src/main/resources/config.yml"))) {
            if (line.isEmpty() || line.startsWith(" ") || line.startsWith("#")) continue;
            int colon = line.indexOf(':');
            if (colon <= 0) continue;
            String key = line.substring(0, colon);
            if (!seen.add(key)) duplicates.add(key);
        }
        assertTrue(duplicates.isEmpty(), "duplicate top-level keys silently discard config: " + duplicates);
    }

    @Test void carriesEveryFeatureFlag() throws Exception {
        var root = load();
        for (String flag : List.of("sentinelSkull", "taskProgression", "hud", "renewableParticles",
                "renewableAuthoring", "infraMode", "routes", "rewardAdvancements", "hubLobby",
                "durability", "contributions", "lockedSlots", "hungerRegen")) {
            assertInstanceOf(Boolean.class, at(root, "features." + flag + ".enabled"),
                    "features." + flag + ".enabled must be a boolean");
        }
    }

    @Test void taskProgressionKeepsItsNesting() throws Exception {
        var root = load();
        // The exact shape that was broken: automatic must sit under task, and
        // contributionForkLevel must not be nested inside it.
        assertInstanceOf(Map.class, at(root, "progression.task.automatic"));
        assertInstanceOf(Integer.class, at(root, "progression.task.tierMax"));
        assertInstanceOf(Integer.class, at(root, "progression.contributionForkLevel"));
    }

    @Test void durabilityShipsVanilla() throws Exception {
        Object targets = at(load(), "durability.targetBaseUses");
        assertTrue(targets instanceof Map && ((Map<?, ?>) targets).isEmpty(),
                "durability targets must ship empty; 15.1.1 is a NON-CANON candidate");
    }

    @Test void hubDestinationsShipEmpty() throws Exception {
        Object dest = at(load(), "features.hubLobby.destinations");
        assertTrue(dest instanceof Map && ((Map<?, ?>) dest).isEmpty(),
                "which maps are offered is a selection decision, not a plugin default");
    }

    @Test void regenBlockIsExpressedAsMissingPoints() throws Exception {
        assertEquals(4, at(load(), "features.hungerRegen.blockedWhenPointsMissing"),
                "two empty drumsticks block regeneration");
    }
}
