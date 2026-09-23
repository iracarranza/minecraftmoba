package com.minecraftmoba.plugin;

import java.util.Map;
import java.util.Objects;

/** Data-driven class seam. Progression consumers may use these hooks without changing the class registry. */
public record ClassDefinition(String id, String displayName, String passiveHook,
                              String statGrowthProfile, Map<String, String> infrastructureProgression,
                              Map<String, String> abilityBranches, Map<String, String> persistentStateDefaults) {
    public ClassDefinition {
        Objects.requireNonNull(id);
        if (id.isBlank()) throw new IllegalArgumentException("Class ID cannot be blank");
        displayName = displayName == null ? id : displayName;
        passiveHook = passiveHook == null ? "" : passiveHook;
        statGrowthProfile = statGrowthProfile == null ? "" : statGrowthProfile;
        infrastructureProgression = immutable(infrastructureProgression);
        abilityBranches = immutable(abilityBranches);
        persistentStateDefaults = immutable(persistentStateDefaults);
    }
    private static Map<String, String> immutable(Map<String, String> value) {
        return value == null ? Map.of() : Map.copyOf(value);
    }
    public String branchFor(String abilityId) { return abilityBranches.get(abilityId); }
}
