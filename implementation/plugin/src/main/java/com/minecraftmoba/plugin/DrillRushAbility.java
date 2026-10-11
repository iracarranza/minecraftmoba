package com.minecraftmoba.plugin;

import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Mole A2 -- Drill Rush. Walk into walls or the floor; on reactivation,
 * emerge and deal damage in the direction faced.
 *
 * Ported from {@code codex/lightfooted-from-phase1} onto the {@link Ability}
 * contract, which supplies the thing the source most needed: {@link
 * Ability#recast} already means "a second activation while already active".
 * The source tracked burrowed players in a set and branched on
 * {@code !set.add(id)}, which works but makes the two halves of the ability
 * one method with an invisible mode switch.
 *
 * <h2>The source's emergence test was inverted</h2>
 *
 * It required the destination block to be SOLID before surfacing, so Mole
 * could only emerge into rock. Emergence needs somewhere to stand: the test is
 * for passable space, and failing it refuses visibly rather than teleporting
 * Mole inside a wall.
 *
 * <h2>Burrowed Mole is invulnerable, and that must end on every path</h2>
 *
 * Not only on emergence and quit. A Mole who dies burrowed, or whose match
 * ends burrowed, would otherwise respawn invulnerable -- and the cheapest way
 * to find that bug is for a player to find it first. {@link #cancel} is the
 * single place invulnerability is released, and every path goes through it.
 *
 * [WORKING] Every value is provisional calibration carried from the source
 * branch. None has been played.
 */
public final class DrillRushAbility implements Ability, org.bukkit.event.Listener {

    /**
     * Emerges along the facing. INSTANT despite being active for a while: the
     * press is the whole input, and the recast is a second press, not a hold.
     */
    @Override public java.util.List<AbilityOutput> outputs() {
        return AbilityOutput.single("drill_rush", TargetForm.DIRECTION, InputForm.INSTANT, Recipients.ENEMIES);
    }

    private final MobaPlugin plugin;
    private final ConfigurationSection config;
    private final Map<UUID, Location> entries = new HashMap<>();
    private final Map<UUID, Long> burrowStarted = new HashMap<>();
    private final Map<UUID, Boolean> invulnerability = new HashMap<>();
    private final Map<UUID, Long> armored = new HashMap<>();
    private record Chain(Location wall, long until) {}
    private final Map<UUID, Chain> chain = new HashMap<>();
    private long clock;
    private org.bukkit.NamespacedKey speedKey() { return new org.bukkit.NamespacedKey(plugin, "burrow_chain"); }

    public DrillRushAbility(MobaPlugin plugin, ConfigurationSection config) {
        this.plugin = plugin;
        this.config = Objects.requireNonNull(config);
        if (config.getLong("cooldownTicks") < 0)
            throw new IllegalArgumentException("Negative cooldown: drill_rush");
    }

    @Override public String id() { return "drill_rush"; }
    @Override public String displayName() { return Objects.requireNonNull(config.getString("displayName")); }
    @Override public long cooldownTicks() { return config.getLong("cooldownTicks"); }

    @Override public Map<String, String> branches() {
        return Map.of("armored_emergence", "Armored Emergence",
                      "undermine", "Undermine",
                      "burrow_chain", "Burrow Chain");
    }
    @Override public List<String> branchIds() {
        return List.of("armored_emergence", "undermine", "burrow_chain");
    }

    @Override public boolean active(Player p) { return entries.containsKey(p.getUniqueId()); }

    @Override
    public void cancel(Player p) {
        if (entries.remove(p.getUniqueId()) != null) p.setInvulnerable(invulnerability.getOrDefault(p.getUniqueId(), false));
        burrowStarted.remove(p.getUniqueId());
        invulnerability.remove(p.getUniqueId()); armored.remove(p.getUniqueId());
        if (chain.remove(p.getUniqueId()) != null) removeSpeed(p);
    }

    /** Burrow. */
    @Override
    public boolean execute(Player p, AbilityContext ctx) {
        if (active(p)) return false;
        entries.put(p.getUniqueId(), p.getLocation().clone());
        invulnerability.put(p.getUniqueId(), p.isInvulnerable());
        burrowStarted.put(p.getUniqueId(), clock);
        p.setInvulnerable(true);
        return true;
    }

    /** Emerge, and damage what is in front. */
    @Override
    public boolean recast(Player p, AbilityContext ctx) {
        if (!active(p)) return false;

        Vector facing = p.getLocation().getDirection().normalize();
        Location out = p.getLocation().clone().add(facing.clone().multiply(config.getDouble("emergeDistance")));

        // Somewhere to stand, not somewhere to be buried.
        if (!out.getBlock().isPassable() || !out.clone().add(0, 1, 0).getBlock().isPassable()) {
            if (plugin.hudNotice() != null) plugin.hudNotice().unavailable(p, "NO EXIT");
            return true;
        }

        Location entry = entries.get(p.getUniqueId());
        cancel(p);
        p.teleport(out);

        double radius = config.getDouble("emergeRadius");
        double damage = config.getDouble("emergeDamage");
        for (Entity e : out.getWorld().getNearbyEntities(out, radius, radius, radius))
            if (e instanceof LivingEntity target && !target.equals(p)
                    && Targetability.status(target, p))
                target.damage(damage, p);

        // Undermine stuns only on a FLOOR emergence -- coming up from below,
        // not out of a wall. The branch's whole distinction is the direction
        // Mole arrives from, so the pitch of the facing is the condition.
        if ("undermine".equals(ctx.branchFor(id())) && plugin.stun() != null
                && facing.getY() >= config.getDouble("undermineUpwardFacing")) {
            long stunTicks = config.getLong("undermineStunTicks");
            for (Entity e : out.getWorld().getNearbyEntities(out, radius, radius, radius))
                if (e instanceof LivingEntity target && !target.equals(p)
                        && Targetability.status(target, p))
                    plugin.stun().stun(target, stunTicks);
        }

        if ("armored_emergence".equals(ctx.branchFor(id())))
            armored.put(p.getUniqueId(), clock + config.getLong("armorTicks"));
        if ("burrow_chain".equals(ctx.branchFor(id())) && Math.abs(facing.getY()) < .6) {
            chain.put(p.getUniqueId(), new Chain(entry, clock + config.getLong("chainTicks")));
            var speed = p.getAttribute(org.bukkit.attribute.Attribute.MOVEMENT_SPEED);
            if (speed != null) speed.addModifier(new org.bukkit.attribute.AttributeModifier(speedKey(),
                    config.getDouble("chainSpeed"), org.bukkit.attribute.AttributeModifier.Operation.MULTIPLY_SCALAR_1,
                    org.bukkit.inventory.EquipmentSlotGroup.ANY));
        }
        return true;
    }
    private void removeSpeed(Player p) {
        var speed = p.getAttribute(org.bukkit.attribute.Attribute.MOVEMENT_SPEED);
        if (speed != null) speed.getModifiers().stream().filter(m -> m.getKey().equals(speedKey())).toList().forEach(speed::removeModifier);
    }
    @org.bukkit.event.EventHandler(priority = org.bukkit.event.EventPriority.HIGH, ignoreCancelled = true)
    public void damage(org.bukkit.event.entity.EntityDamageEvent e) {
        if (e.getEntity() instanceof Player p && armored.getOrDefault(p.getUniqueId(), 0L) > clock)
            e.setDamage(e.getDamage() * (1 - config.getDouble("armorReduction")));
    }
    @Override public void tick() {
        clock++;
        for (var item : List.copyOf(entries.entrySet())) {
            Player p = plugin.getServer().getPlayer(item.getKey());
            if (p == null) { entries.remove(item.getKey()); invulnerability.remove(item.getKey()); burrowStarted.remove(item.getKey()); continue; }
            if (!p.isOnline() || p.isDead() || !p.getWorld().equals(item.getValue().getWorld())) { cancel(p); continue; }
            if (clock - burrowStarted.getOrDefault(item.getKey(), clock) >= config.getLong("maxBurrowTicks")) {
                p.teleport(item.getValue()); cancel(p); continue;
            }
            if (plugin.stun() != null && plugin.stun().movementRefused(p)) continue;
            var input = p.getCurrentInput(); Vector motion = p.getLocation().getDirection().setY(0);
            if (motion.lengthSquared() > 1e-6) motion.normalize();
            Vector lateral = new Vector(-motion.getZ(), 0, motion.getX());
            Vector step = new Vector();
            if (input.isForward()) step.add(motion); if (input.isBackward()) step.subtract(motion);
            if (input.isRight()) step.add(lateral); if (input.isLeft()) step.subtract(lateral);
            if (input.isJump()) step.setY(1); if (input.isSneak()) step.setY(-1);
            if (step.lengthSquared() > 1e-6) {
                Location next = p.getLocation().add(step.normalize().multiply(config.getDouble("burrowSpeed")));
                if (next.getY() >= p.getWorld().getMinHeight() && next.getY() < p.getWorld().getMaxHeight() - 2
                        && next.getBlock().getType().getHardness() >= 0) p.teleport(next);
            }
            p.setVelocity(new Vector());
        }
        armored.values().removeIf(expiry -> expiry <= clock);
        for (var item : List.copyOf(chain.entrySet())) {
            Player p = plugin.getServer().getPlayer(item.getKey()); Chain c = item.getValue();
            if (p == null || !p.isOnline() || p.isDead() || clock >= c.until()
                    || !p.getWorld().equals(c.wall().getWorld())) {
                if (p != null) removeSpeed(p); chain.remove(item.getKey()); continue;
            }
            Location ahead = p.getLocation().add(p.getLocation().getDirection().setY(0).multiply(.8));
            if (!ahead.getBlock().isPassable() && ahead.distanceSquared(c.wall()) >= 4) {
                plugin.inputs().cooldowns().clear(p.getUniqueId(), id());
                removeSpeed(p); chain.remove(item.getKey());
            }
        }
    }

}
