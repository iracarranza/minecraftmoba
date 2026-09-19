package com.minecraftmoba.plugin;

import org.bukkit.entity.Player;

/** Flat registry seam; variants/branch trees are deliberately absent. */
public interface Ability {
    String id();
    String displayName();
    long cooldownTicks();
    boolean execute(Player player, AbilityContext context);
    record AbilityContext(MobaPlugin plugin, Provenance provenance, AbilityInputs inputs) {}
}
