package com.minecraftmoba.plugin;

import org.bukkit.entity.Player;
import java.util.Map;

/** Extensible ability contract. A selected branch is resolved by the class definition. */
public interface Ability {
    String id();
    String displayName();
    long cooldownTicks();
    boolean execute(Player player, AbilityContext context);
    default Map<String, String> branches() { return Map.of(); }
    record AbilityContext(MobaPlugin plugin, Provenance provenance, AbilityInputs inputs,
                           ClassDefinition classDefinition) {
        public AbilityContext(MobaPlugin plugin, Provenance provenance, AbilityInputs inputs) {
            this(plugin, provenance, inputs, null);
        }
    }
}
