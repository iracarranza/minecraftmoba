package com.minecraftmoba.plugin;

import java.util.Map;
import java.util.Objects;

/** Data-driven class seam. Progression consumers may use these hooks without changing the class registry. */
public record ClassDefinition(String id, String displayName, String passiveHook,
                              String statGrowthProfile, Map<String, String> infrastructureProgression,
                              Map<String, String> abilityBranches, Map<String, String> persistentStateDefaults,
                              Map<Integer, GrowthPacket> growth) {

    /**
     * Seven-argument form, for a class configured only with the superseded
     * {@code infrastructureProgression} map.
     *
     * The legacy map is translated rather than ignored, so an unmigrated class
     * keeps the progression it already had.
     */
    public ClassDefinition(String id, String displayName, String passiveHook,
                           String statGrowthProfile, Map<String, String> infrastructureProgression,
                           Map<String, String> abilityBranches, Map<String, String> persistentStateDefaults) {
        this(id, displayName, passiveHook, statGrowthProfile, infrastructureProgression,
             abilityBranches, persistentStateDefaults,
             GrowthPacket.fromLegacy(infrastructureProgression));
    }

    public ClassDefinition {
        Objects.requireNonNull(id);
        if (id.isBlank()) throw new IllegalArgumentException("Class ID cannot be blank");
        displayName = displayName == null ? id : displayName;
        passiveHook = passiveHook == null ? "" : passiveHook;
        statGrowthProfile = statGrowthProfile == null ? "" : statGrowthProfile;
        infrastructureProgression = immutable(infrastructureProgression);
        abilityBranches = immutable(abilityBranches);
        persistentStateDefaults = immutable(persistentStateDefaults);
        growth = growth == null ? Map.of() : Map.copyOf(growth);
    }
    private static Map<String, String> immutable(Map<String, String> value) {
        return value == null ? Map.of() : Map.copyOf(value);
    }

    public String branchFor(String abilityId) { return abilityBranches.get(abilityId); }

    /**
     * What this class gains at a level, or an empty packet.
     *
     * Empty is legitimate rather than a configuration error: budgets need not
     * be equal at every event, and a level that already grants an Ultimate or a
     * Task tier can reasonably spend nothing here.
     */
    public GrowthPacket growthAt(int level) {
        return growth.getOrDefault(level, new GrowthPacket(level, java.util.List.of()));
    }

    /** The levels at which this class's curve actually spends Growth budget. */
    public java.util.SortedSet<Integer> growthLevels() {
        return new java.util.TreeSet<>(growth.keySet());
    }
}
