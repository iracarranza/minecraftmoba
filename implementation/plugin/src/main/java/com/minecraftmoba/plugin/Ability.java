package com.minecraftmoba.plugin;

import org.bukkit.entity.Player;
import java.util.Map;
import java.util.List;

/** Extensible ability contract. A selected branch is resolved by the class definition. */
public interface Ability {
    String id();
    String displayName();
    long cooldownTicks();
    boolean execute(Player player, AbilityContext context);
    default Map<String, String> branches() { return Map.of(); }
    default List<String> branchIds() { return List.copyOf(branches().keySet()); }
    default boolean active(Player player) { return false; }
    default void cancel(Player player) {}
    record AbilityContext(MobaPlugin plugin, Provenance provenance, AbilityInputs inputs,
                           ClassDefinition classDefinition, PlayerData playerData) {
        public AbilityContext(MobaPlugin plugin, Provenance provenance, AbilityInputs inputs) {
            this(plugin, provenance, inputs, null, null);
        }
        public String branchFor(String abilityId) {
            if (playerData != null) {
                String selected = playerData.classState.get("branch." + abilityId);
                if (selected != null) return selected;
            }
            return classDefinition == null ? null : classDefinition.branchFor(abilityId);
        }
    }
}
