package com.minecraftmoba.plugin;

import java.util.*;

/**
 * Ecological categories over vanilla biome ids.
 *
 * A swarm names the ecology it belongs to rather than a list of ids, so its
 * definition survives a change in which vanilla biomes count as "mountain".
 * A spec entry is either a group name from this table or a literal biome id.
 *
 * WORKING FIXTURE, not canon: which biomes belong to which category is content.
 * The membership is mirrored in implementation/worldgen/fixtures/
 * swarm-biome-groups.json, which the compiler reads and BiomeGroupsTest checks,
 * so the two sides cannot drift silently.
 *
 * Vanilla has one mushroom biome, minecraft:mushroom_fields; there is no
 * separate "mycelium biome".
 */
public final class BiomeGroups {
    public static final Map<String, Set<String>> GROUPS;
    static {
        var g = new LinkedHashMap<String, Set<String>>();
        g.put("mountain", Set.of("minecraft:jagged_peaks", "minecraft:frozen_peaks",
                "minecraft:stony_peaks", "minecraft:snowy_slopes", "minecraft:grove",
                "minecraft:windswept_hills", "minecraft:windswept_gravelly_hills",
                "minecraft:windswept_forest"));
        g.put("desert", Set.of("minecraft:desert"));
        g.put("beach", Set.of("minecraft:beach", "minecraft:snowy_beach"));
        g.put("jungle", Set.of("minecraft:jungle", "minecraft:sparse_jungle",
                "minecraft:bamboo_jungle"));
        g.put("mushroom", Set.of("minecraft:mushroom_fields"));
        GROUPS = Collections.unmodifiableMap(g);
    }

    private BiomeGroups() {}

    /** Whether a spec (group names and/or literal ids) admits this biome. Unknown biome never matches. */
    public static boolean matches(Set<String> spec, String biomeKey) {
        if (biomeKey == null) return false;
        for (String entry : spec) {
            var group = GROUPS.get(entry);
            if (group != null ? group.contains(biomeKey) : entry.equals(biomeKey)) return true;
        }
        return false;
    }

    /** A spec entry must be a known group or look like a namespaced biome id. */
    public static void validate(Set<String> spec) {
        for (String entry : spec)
            if (!GROUPS.containsKey(entry) && !entry.contains(":"))
                throw new IllegalArgumentException("not a biome group or id: " + entry
                        + " (groups: " + String.join(", ", GROUPS.keySet()) + ")");
    }
}
