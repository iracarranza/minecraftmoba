package com.minecraftmoba.plugin;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Daredevil A1 -- Runway. Convert meaningful forward momentum into a
 * forward-and-upward launch.
 *
 * Ported from {@code codex/lightfooted-from-phase1} onto the {@link Ability}
 * contract, which is what gives it a per-player branch, a cooldown and a
 * refusal the player can see.
 *
 * <h2>It refuses rather than doing nothing</h2>
 *
 * Below the momentum threshold the ability reports unavailable instead of
 * returning quietly. A standing Daredevil pressing Runway and getting silence
 * cannot tell an unmet requirement from a dropped input, and "you were not
 * moving fast enough" is the single most common thing this ability has to say.
 *
 * [WORKING] Every value is provisional calibration carried from the source
 * branch. None has been played.
 */
public final class RunwayAbility implements Ability {

    private final MobaPlugin plugin;
    private final ConfigurationSection config;
    private final DaredevilState state;

    public RunwayAbility(MobaPlugin plugin, ConfigurationSection config, DaredevilState state) {
        this.plugin = plugin;
        this.config = Objects.requireNonNull(config);
        this.state = state;
        if (config.getLong("cooldownTicks") < 0)
            throw new IllegalArgumentException("Negative cooldown: runway");
    }

    @Override public String id() { return "runway"; }
    @Override public String displayName() { return Objects.requireNonNull(config.getString("displayName")); }
    @Override public long cooldownTicks() { return config.getLong("cooldownTicks"); }

    @Override public Map<String, String> branches() {
        return Map.of("pop_rocket", "Pop Rocket", "trampoline", "Trampoline", "suplex", "Suplex");
    }
    @Override public List<String> branchIds() { return List.of("pop_rocket", "trampoline", "suplex"); }

    @Override
    public boolean execute(Player p, AbilityContext ctx) {
        if (!state.isDaredevil(p)) return false;

        double momentum = state.momentum(p);
        if (!DaredevilState.qualifies(momentum, config.getDouble("momentumThreshold"))) {
            if (plugin.hudNotice() != null) plugin.hudNotice().unavailable(p, "TOO SLOW");
            return false;
        }

        String branch = ctx.branchFor(id());
        ConfigurationSection b = branchConfig(branch);
        double horizontal = b == null ? 1.0 : b.getDouble("horizontalMultiplier", 1.0);
        double upward = config.getDouble("upward") * (b == null ? 1.0 : b.getDouble("upwardMultiplier", 1.0));

        Vector travel = new Vector(momentum, 0, 0);
        Vector heading = p.getLocation().getDirection();
        Vector leap = DaredevilState.launch(travel, heading, horizontal, upward);
        p.setVelocity(leap);

        // Suplex takes whoever Daredevil is touching along for the whole
        // trajectory -- up AND back down -- so it is applied as the same
        // vector rather than as a separate knock-up.
        if ("suplex".equals(branch)) {
            double reach = config.getDouble("suplexReach");
            // Targetability.impulse is the permissive axis: anything physics
            // can move, allies excluded. Suplex displaces rather than
            // afflicts, so it is the correct one of the two.
            for (Entity e : p.getNearbyEntities(reach, reach, reach))
                if (!e.equals(p) && Targetability.impulse(e, p)) e.setVelocity(leap.clone());
        }
        return true;
    }

    private ConfigurationSection branchConfig(String branch) {
        if (branch == null) return null;
        ConfigurationSection all = config.getConfigurationSection("branches");
        return all == null ? null : all.getConfigurationSection(branch);
    }
}
