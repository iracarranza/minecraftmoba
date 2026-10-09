package com.minecraftmoba.plugin;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Daredevil A2 -- Crash Landing. At sufficient velocity, deliberately Crash
 * into terrain: movement ends, Daredevil takes fixed fall damage, nearby
 * enemies take damage scaled by the velocity lost, and Daredevil's next
 * instance of fall damage is negated.
 *
 * Ported from {@code codex/lightfooted-from-phase1} onto the {@link Ability}
 * contract.
 *
 * <h2>Impact damage comes from what was spent, not from what remains</h2>
 *
 * The scaling reads the descent speed at the moment of the Crash, and the
 * Crash then zeroes it. Measuring afterwards would always read zero, which is
 * the kind of ordering mistake that produces an ability that "does nothing"
 * and looks correct in the source.
 *
 * <h2>The self-damage is fixed and the enemy damage is not</h2>
 *
 * That asymmetry is the design's, and it is what keeps Crash from being a
 * bigger fall the faster you were going: the cost is knowable before you
 * commit, the payoff is not.
 *
 * [WORKING] Every value is provisional calibration carried from the source
 * branch. None has been played.
 */
public final class CrashLandingAbility implements Ability {

    /**
     * Ends the movement the caster already has. Nearby enemies take damage from
     * the impact, which is an area EFFECT and not an area target: nothing is aimed.
     */
    @Override public java.util.List<AbilityOutput> outputs() {
        return AbilityOutput.single("crash_landing", TargetForm.SELF, InputForm.INSTANT, Recipients.ENEMIES);
    }

    private final MobaPlugin plugin;
    private final ConfigurationSection config;
    private final DaredevilState state;

    public CrashLandingAbility(MobaPlugin plugin, ConfigurationSection config, DaredevilState state) {
        this.plugin = plugin;
        this.config = Objects.requireNonNull(config);
        this.state = state;
        if (config.getLong("cooldownTicks") < 0)
            throw new IllegalArgumentException("Negative cooldown: crash_landing");
    }

    @Override public String id() { return "crash_landing"; }
    @Override public String displayName() { return Objects.requireNonNull(config.getString("displayName")); }
    @Override public long cooldownTicks() { return config.getLong("cooldownTicks"); }

    @Override public Map<String, String> branches() {
        return Map.of("crater", "Crater", "combat_roll", "Combat Roll", "superhero", "Superhero");
    }
    @Override public List<String> branchIds() { return List.of("crater", "combat_roll", "superhero"); }

    @Override
    public boolean execute(Player p, AbilityContext ctx) {
        if (!state.isDaredevil(p)) return false;

        double descent = state.descent(p);
        if (descent < config.getDouble("crashVelocity")) {
            if (plugin.hudNotice() != null) plugin.hudNotice().unavailable(p, "TOO SLOW");
            return false;
        }

        String branch = ctx.branchFor(id());
        ConfigurationSection b = branchConfig(branch);

        p.setVelocity(new Vector());
        state.negateNextFall(p);

        double selfDamage = config.getDouble("selfDamage")
                * (b == null ? 1.0 : b.getDouble("selfDamageMultiplier", 1.0));
        if (selfDamage > 0) p.damage(selfDamage);

        double impact = descent * config.getDouble("impactDamageScale")
                * (b == null ? 1.0 : b.getDouble("impactDamageMultiplier", 1.0));
        if (impact > 0) {
            double radius = config.getDouble("impactRadius");
            for (Entity e : p.getNearbyEntities(radius, radius, radius))
                if (e instanceof LivingEntity target && !target.equals(p)
                        && Targetability.impulse(target, p))
                    target.damage(impact, p);
        }

        // Crater trades impact damage for control, scaled by the velocity it
        // spent -- the same descent the damage reads, so a bigger Crash is a
        // longer Stun as well as a smaller hit.
        if ("crater".equals(branch) && plugin.stun() != null) {
            long stunTicks = Math.round(descent * config.getDouble("craterStunTicksPerVelocity"));
            double radius = config.getDouble("impactRadius");
            for (Entity e : p.getNearbyEntities(radius, radius, radius))
                if (e instanceof LivingEntity target && !target.equals(p)
                        && Targetability.status(target, p))
                    plugin.stun().stun(target, stunTicks);
        }

        // [OPEN] Superhero's "invincible and unable to act" pose is a Stun
        // applied to Daredevil plus invulnerability, and needs a duration
        // classes.md does not yet give. Left unimplemented rather than
        // guessed: its damage profile above is the part that is settled.
        return true;
    }

    private ConfigurationSection branchConfig(String branch) {
        if (branch == null) return null;
        ConfigurationSection all = config.getConfigurationSection("branches");
        return all == null ? null : all.getConfigurationSection(branch);
    }
}
