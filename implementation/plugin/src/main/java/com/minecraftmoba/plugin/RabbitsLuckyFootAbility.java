package com.minecraftmoba.plugin;

import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.*;
import org.bukkit.util.Vector;
import java.util.*;

/** Protected, deliberately forced animal arcs followed by the caster's heavier landing. */
final class RabbitsLuckyFootAbility implements Ability {
    private record Leap(Player caster, LivingEntity entity, Location start, Location target,
                        long begins, long ends, boolean wasInvulnerable, boolean casterLeap) {}
    private final MobaPlugin plugin; private final ConfigurationSection config;
    private final List<Leap> leaps = new ArrayList<>(); private long clock;
    RabbitsLuckyFootAbility(MobaPlugin plugin, ConfigurationSection config) { this.plugin = plugin; this.config = config; }
    public String id() { return "rabbits_lucky_foot"; }
    public String displayName() { return "Rabbit's Lucky Foot"; }
    public long cooldownTicks() { return config.getLong("cooldownTicks"); }
    public List<AbilityOutput> outputs() {
        return AbilityOutput.single("rabbits_lucky_foot", TargetForm.AREA_BLOCKS, InputForm.INSTANT, Recipients.ENEMIES);
    }
    public Collection<org.bukkit.block.Block> preview(Player p, AbilityContext ctx) {
        var target = p.getTargetBlockExact(config.getInt("range")); return target == null ? List.of() : List.of(target);
    }
    public List<Need> needs(Player p, AbilityContext ctx) {
        return List.of(new Need("a block to land beside, within " + config.getInt("range") + " blocks, with room above it",
                (pl, c) -> {
                    var t = pl.getTargetBlockExact(config.getInt("range"));
                    return t != null && t.getRelative(0, 1, 0).isPassable() && t.getRelative(0, 2, 0).isPassable();
                }, null));
    }
    public boolean execute(Player p, AbilityContext ctx) {
        var target = p.getTargetBlockExact(config.getInt("range")); if (target == null) return false;
        Location landing = target.getLocation().add(.5, 1, .5);
        if (!landing.getBlock().isPassable() || !landing.clone().add(0, 1, 0).getBlock().isPassable()) return false;
        cancel(p);
        double radius = config.getDouble("animalRadius"); int index = 0;
        for (var e : p.getNearbyEntities(radius, radius, radius)) if (AnimalSenses.species(e) != null && e instanceof LivingEntity animal) {
            double angle = index++ * 2.4;
            Location destination = landing.clone().add(Math.cos(angle), 0, Math.sin(angle));
            if (!destination.getBlock().isPassable() || !destination.clone().add(0, 1, 0).getBlock().isPassable()) destination = landing.clone();
            boolean previous = animal.isInvulnerable(); animal.setInvulnerable(true);
            leaps.add(new Leap(p, animal, animal.getLocation(), destination, clock,
                    clock + config.getInt("flightTicks"), previous, false));
        }
        long begins = clock + config.getInt("casterDelayTicks");
        leaps.add(new Leap(p, p, p.getLocation(), landing, begins,
                begins + config.getInt("flightTicks"), p.isInvulnerable(), true));
        return true;
    }
    static Vector arc(Vector start, Vector end, double fraction, double height) {
        return start.clone().multiply(1 - fraction).add(end.clone().multiply(fraction))
                .add(new Vector(0, 4 * height * fraction * (1 - fraction), 0));
    }
    public void tick() {
        clock++;
        for (var leap : List.copyOf(leaps)) {
            var e = leap.entity();
            if (!leap.caster().isOnline() || leap.caster().isDead() || !e.isValid() || e.isDead()
                    || !e.getWorld().equals(leap.target().getWorld()) || !leap.caster().getWorld().equals(e.getWorld())) { finish(leap, false); continue; }
            if (clock < leap.begins()) continue;
            double f = Math.min(1, (clock - leap.begins()) / (double) (leap.ends() - leap.begins()));
            Location next = arc(leap.start().toVector(), leap.target().toVector(), f, config.getDouble("arcHeight")).toLocation(e.getWorld());
            if (!next.getChunk().isLoaded() || next.getY() >= e.getWorld().getMaxHeight() - 2
                    || !next.getBlock().isPassable() || !next.clone().add(0, e.getHeight(), 0).getBlock().isPassable()) { finish(leap, false); continue; }
            next.setYaw(e.getLocation().getYaw()); next.setPitch(e.getLocation().getPitch());
            e.teleport(next); e.setFallDistance(0);
            if (f >= 1) finish(leap, true);
        }
    }
    private void finish(Leap leap, boolean impact) {
        leaps.remove(leap); var e = leap.entity(); e.setFallDistance(0);
        if (impact) {
            double radius = config.getDouble("impactRadius");
            for (var enemy : AbilityTargeting.around(leap.caster(), e, radius, radius, radius, AbilityTargeting.Relation.ENEMY, false))
                AbilityDamage.deal(enemy, config.getDouble(leap.casterLeap() ? "casterDamage" : "animalDamage"), leap.caster());
            e.getWorld().spawnParticle(Particle.CLOUD, e.getLocation(), 12, 1, .1, 1, .05);
        }
        if (!leap.casterLeap()) e.setInvulnerable(leap.wasInvulnerable());
    }
    public void cancel(Player p) { for (var l : List.copyOf(leaps)) if (l.caster().equals(p) || l.entity().equals(p)) finish(l, false); }
}
