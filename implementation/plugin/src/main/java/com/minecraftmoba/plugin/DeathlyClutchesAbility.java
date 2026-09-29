package com.minecraftmoba.plugin;

import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.Objects;

/**
 * Daredevil's ultimate -- Deathly Clutches. Drop to near-death Health and
 * negate the next N damage instances; a limited number of successful attacks
 * restore already-consumed instances, never exceeding the initial maximum.
 *
 * Ported from {@code codex/lightfooted-from-phase1} onto the {@link Ability}
 * contract. The negation and restoration live in {@link DaredevilState},
 * because they are event-driven and outlive the activation.
 *
 * <h2>"Damage instances" means instances, not lethal hits</h2>
 *
 * A clutch is spent by any damage, including chip damage and fall damage.
 * That is what classes.md specifies, and it is the whole tension of the
 * ultimate: at near-death Health, every trivial source is now competing for
 * the same finite protection.
 *
 * <h2>Near-death is set in EFFECTIVE points</h2>
 *
 * Like every other configured health quantity here, via {@link Vitals}, so it
 * means the same fraction of a player whatever their Capacity. A literal 1.0
 * would be a scratch at Capacity 20 and nearly the whole bar at 9.
 *
 * [OPEN] N, the restore count and the near-death value are all unresolved in
 * classes.md; the config carries provisional figures.
 */
public final class DeathlyClutchesAbility implements Ability {

    private final MobaPlugin plugin;
    private final ConfigurationSection config;
    private final DaredevilState state;

    public DeathlyClutchesAbility(MobaPlugin plugin, ConfigurationSection config, DaredevilState state) {
        this.plugin = plugin;
        this.config = Objects.requireNonNull(config);
        this.state = state;
        if (config.getLong("cooldownTicks") < 0)
            throw new IllegalArgumentException("Negative cooldown: deathly_clutches");
        if (config.getInt("instances") <= 0)
            throw new IllegalArgumentException("Deathly Clutches must negate at least one instance.");
    }

    @Override public String id() { return "deathly_clutches"; }
    @Override public String displayName() { return Objects.requireNonNull(config.getString("displayName")); }
    @Override public long cooldownTicks() { return config.getLong("cooldownTicks"); }

    @Override
    public boolean execute(Player p, AbilityContext ctx) {
        if (!state.isDaredevil(p)) return false;
        // Refuse rather than heal. Casting at 1 HP should not be punished by a
        // silent no-op, but casting must never RAISE Health -- the ultimate's
        // cost is the whole reason its payoff is allowed to be large.
        double target = Vitals.scaleHealing(config.getDouble("nearDeathHealth"),
                                            plugin.effectiveMaxHealth(p));
        var attr = p.getAttribute(Attribute.MAX_HEALTH);
        double floor = Math.max(0.5, Math.min(target, attr == null ? target : attr.getValue()));
        if (p.getHealth() > floor) p.setHealth(floor);

        state.grantClutches(p, config.getInt("instances"), config.getInt("restores"));
        return true;
    }
}
