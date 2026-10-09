package com.minecraftmoba.plugin;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Toolbox A2 -- Jumpstartinator! Manually activate the first components of the
 * same Utility Belt.
 *
 * <h2>There is no separate A2 circuit, and that is the point</h2>
 *
 * A2 fires the first two components of the board the passive already runs, so
 * circuit design is front-weighted: the opening is not "what happens first",
 * it is "what happens constantly". A Piston in slot one is a dash on a charge
 * timer, paid for one Piston at a time.
 *
 * Together with A1, which swaps the first components, that closes the loop --
 * A1 is A2's loadout editor rather than a general circuit editor.
 *
 * <h2>Charges rather than a flat cooldown</h2>
 *
 * Three on an eight-second recharge, which classes.md distinguishes sharply
 * from a flat 2s: thirty uses a minute would be a rhythm to lean on, while a
 * three-use burst and 7.5 a minute sustained is something spent and then
 * absent. It alternates with the passive at 6-16s instead of drowning it.
 */
public final class JumpstartinatorAbility implements Ability {

    /**
     * Fires the first components of your own belt. The components may reach
     * outward; the ACTIVATION does not aim, which is what the form records.
     */
    @Override public java.util.List<AbilityOutput> outputs() {
        return AbilityOutput.single("jumpstartinator", TargetForm.SELF, InputForm.INSTANT, Recipients.ENEMIES);
    }

    private final MobaPlugin plugin;
    private final ConfigurationSection config;
    private final UtilityBelt belt;

    public JumpstartinatorAbility(MobaPlugin plugin, ConfigurationSection config, UtilityBelt belt) {
        this.plugin = plugin;
        this.config = Objects.requireNonNull(config);
        this.belt = belt;
        if (config.getInt("charges") <= 0)
            throw new IllegalArgumentException("Jumpstartinator must have at least one charge.");
        if (config.getLong("rechargeTicks") < 0)
            throw new IllegalArgumentException("Negative recharge: jumpstartinator");
    }

    @Override public String id() { return "jumpstartinator"; }
    @Override public String displayName() { return Objects.requireNonNull(config.getString("displayName")); }

    /**
     * Unused: A2 is charge-limited, and the two are different quantities.
     *
     * Zero rather than the recharge, so that anything reading a cooldown gets
     * the honest answer -- this ability does not have one -- instead of a
     * number that happens to look like one.
     */
    @Override public long cooldownTicks() { return 0; }

    @Override public int charges() { return config.getInt("charges"); }
    @Override public long rechargeTicks() { return config.getLong("rechargeTicks"); }

    @Override public Map<String, String> branches() {
        return Map.of("overcharging", "Overcharging",
                      "super_circuit", "Super Circuit",
                      "short_circuit", "Short Circuit");
    }
    @Override public List<String> branchIds() {
        return List.of("overcharging", "super_circuit", "short_circuit");
    }

    @Override
    public boolean execute(Player p, AbilityContext ctx) {
        if (belt == null) return false;
        String branch = ctx.branchFor(id());
        ConfigurationSection b = branchConfig(branch);

        int count = b == null ? config.getInt("components")
                              : b.getInt("components", config.getInt("components"));
        int fired = belt.jumpstart(p, count);

        // Short Circuit pays out when the board cannot supply what was asked
        // for -- reached by deliberately breaking the circuit down to one
        // component or none. It is a STATE, not a build: A1 cannot produce it,
        // since A1 only moves components, so it costs manual inventory work
        // mid-fight.
        if ("short_circuit".equals(branch) && fired < config.getInt("shortCircuitBelow")) {
            ConfigurationSection shorted = config.getConfigurationSection("shortCircuit");
            if (shorted != null) blast(p, shorted.getDouble("damage"), shorted.getDouble("radius"));
        }

        // Firing nothing still spends the charge. The alternative is an
        // ability that silently refuses on an empty board, and a player who
        // cannot tell a broken circuit from a dropped input -- which is
        // exactly the state Short Circuit is built around reaching on purpose.
        if (fired == 0 && plugin.hudNotice() != null) plugin.hudNotice().unavailable(p, "NO CIRCUIT");
        return true;
    }

    private void blast(Player p, double damage, double radius) {
        for (Entity e : p.getNearbyEntities(radius, radius, radius))
            if (e instanceof LivingEntity target && !target.equals(p)
                    && Targetability.status(target, p))
                target.damage(damage, p);
    }

    private ConfigurationSection branchConfig(String branch) {
        if (branch == null) return null;
        ConfigurationSection all = config.getConfigurationSection("branches");
        return all == null ? null : all.getConfigurationSection(branch);
    }
}
