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

    /**
     * Converts the momentum the caster already has. The Suplex branch subjects a
     * touched enemy to the launch, but it is not AIMED at them -- contact
     * decides it, so the target form is unchanged and only combat-ness differs.
     */
    @Override public java.util.List<AbilityOutput> outputs() {
        return AbilityOutput.single("runway", TargetForm.SELF, InputForm.INSTANT, Recipients.NONE);
    }

    /**
     * Suplex reaches an enemy; the other two branches reach nobody.
     *
     * The declaration has to be per branch, because config.yml already says so:
     * {@code runway.combat} is false and {@code branches.suplex.combat} is
     * true. A single answer for the ability would have to be wrong for one
     * branch or the other -- NONE under-describes Suplex, ENEMIES claims
     * Pop Rocket and Trampoline hit somebody.
     *
     * The manifest records the BASE, because a row has one cell. This method is
     * the authority when a branch is in hand.
     */
    @Override public java.util.List<AbilityOutput> outputs(org.bukkit.entity.Player player,
                                                          AbilityContext context) {
        boolean suplex = context != null && "suplex".equals(context.branchFor("runway"));
        return AbilityOutput.single("runway", TargetForm.SELF, InputForm.INSTANT,
                suplex ? Recipients.ENEMIES : Recipients.NONE);
    }

    private static final class Stunt {
        final Player caster; final String branch; final java.util.List<Entity> passengers;
        final long starts; boolean airborne; boolean pulled; double descent;
        Stunt(Player caster, String branch, java.util.List<Entity> passengers, long starts) {
            this.caster = caster; this.branch = branch; this.passengers = passengers; this.starts = starts;
        }
    }
    private final java.util.Map<java.util.UUID, Stunt> stunts = new java.util.HashMap<>();
    private long clock;
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
    public List<Need> needs(Player p, AbilityContext ctx) {
        double need = config.getDouble("momentumThreshold");
        return List.of(new Need("more speed: run (or sprint-jump) before you cast",
                (pl, c) -> DaredevilState.qualifies(state.momentum(pl), need),
                (pl, c) -> String.format(java.util.Locale.ROOT, "momentum %.2f of %.2f", state.momentum(pl), need)));
    }

    @Override
    public boolean execute(Player p, AbilityContext ctx) {
        if (!state.isDaredevil(p)) return false;

        double momentum = state.momentum(p);
        if (!DaredevilState.qualifies(momentum, config.getDouble("momentumThreshold"))) {
            return false;                                   // AbilityInputs explains it: see needs()
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
        var passengers = new java.util.ArrayList<Entity>();
        if ("pop_rocket".equals(branch)) {
            p.getWorld().spawnParticle(org.bukkit.Particle.GUST_EMITTER_SMALL, p.getLocation(), 1);
            p.setFallDistance(0);
            state.negateNextFall(p);
        }
        if ("suplex".equals(branch)) {
            double reach = config.getDouble("suplexReach");
            // Targetability.impulse is the permissive axis: anything physics
            // can move, allies excluded. Suplex displaces rather than
            // afflicts, so it is the correct one of the two.
            for (Entity e : p.getNearbyEntities(reach, reach, reach))
                if (!e.equals(p) && Targetability.impulse(e, p)) { e.setVelocity(leap.clone()); passengers.add(e); }
        }
        stunts.put(p.getUniqueId(), new Stunt(p, branch, passengers, clock));
        return true;
    }

    private ConfigurationSection branchConfig(String branch) {
        if (branch == null) return null;
        ConfigurationSection all = config.getConfigurationSection("branches");
        return all == null ? null : all.getConfigurationSection(branch);
    }
    @Override public void tick() {
        clock++;
        for (var stunt : java.util.List.copyOf(stunts.values())) {
            Player p = stunt.caster;
            if (!p.isOnline() || p.isDead() || !state.isDaredevil(p) || clock - stunt.starts > config.getLong("stuntTimeoutTicks")) { cancel(p); continue; }
            if (!p.isOnGround()) stunt.airborne = true;
            stunt.descent = Math.max(stunt.descent, Math.max(0, -p.getVelocity().getY()));
            if ("suplex".equals(stunt.branch) && stunt.airborne && !stunt.pulled && p.getVelocity().getY() < 0) {
                stunt.pulled = true;
                for (Entity target : stunt.passengers) if (target.isValid() && target.getWorld().equals(p.getWorld()) && Targetability.impulse(target, p))
                    target.setVelocity(target.getVelocity().setY(-config.getDouble("suplexDownward")));
            }
            if (stunt.airborne && p.isOnGround()) {
                if ("trampoline".equals(stunt.branch)) {
                    Vector heading = p.getLocation().getDirection().setY(0);
                    if (heading.lengthSquared() > 1e-6) heading.normalize().multiply(Math.max(.2, state.momentum(p)));
                    p.setVelocity(heading.setY(Math.max(config.getDouble("reboundMinimum"), stunt.descent * config.getDouble("reboundFraction"))));
                    p.setFallDistance(0);
                }
                cancel(p);
            }
        }
    }
    @Override public void cancel(Player p) { stunts.remove(p.getUniqueId()); }

}
