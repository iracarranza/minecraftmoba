package com.minecraftmoba.plugin;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.Objects;

/**
 * Toolbox's Ultimate -- Gizmo of Absurdity and Untold Destruction!!!
 *
 * Arms a window in which world Redstone components also fire the Utility Belt
 * at Toolbox. The mechanism is {@link GizmoWindow}; this is the activation.
 *
 * <h2>The window is deliberate, not coincidental</h2>
 *
 * Six seconds is also the Lv25 passive cooldown, and A2 holds three charges --
 * so the window is a burst window for the whole kit: a machine circuit, a
 * passive proc, and up to three A2s all landing inside it, all originating at
 * Toolbox. That crescendo is the intent, and it is why the duration is not a
 * free number.
 */
public final class GizmoAbility implements Ability {

    /**
     * Arms a window on the caster. Everything that then fires originates at
     * Toolbox, so the ability aims at nobody -- the window does.
     */
    @Override public java.util.List<AbilityOutput> outputs() {
        return AbilityOutput.single("gizmo", TargetForm.SELF, InputForm.INSTANT);
    }

    private final MobaPlugin plugin;
    private final ConfigurationSection config;

    public GizmoAbility(MobaPlugin plugin, ConfigurationSection config) {
        this.plugin = plugin;
        this.config = Objects.requireNonNull(config);
        if (config.getLong("cooldownTicks") < 0)
            throw new IllegalArgumentException("Negative cooldown: gizmo");
        if (config.getInt("activations") <= 0)
            throw new IllegalArgumentException("The Gizmo must permit at least one activation.");
    }

    @Override public String id() { return "gizmo"; }
    @Override public String displayName() { return Objects.requireNonNull(config.getString("displayName")); }
    @Override public long cooldownTicks() { return config.getLong("cooldownTicks"); }

    @Override
    public boolean execute(Player p, AbilityContext ctx) {
        if (plugin.gizmoWindow() == null) return false;
        plugin.gizmoWindow().arm(p, config.getLong("durationTicks"), config.getInt("activations"));
        if (plugin.hudNotice() != null) plugin.hudNotice().show(p, "GIZMO ARMED");
        return true;
    }
}
