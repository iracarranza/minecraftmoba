package com.minecraftmoba.plugin;

import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * How an ability finds what it hits: a ray, or everything near a point.
 *
 * Every ability used to write its own {@code rayTrace} or {@code getNearbyEntities}, with its own
 * width and its own idea of who counts, and the differences were accidents: a ray width of .2, .25,
 * .3 or .35 depending on who wrote it, a ray that an ally standing in front would absorb, an area
 * check that passed through walls. This is the shape the cleanest abilities (Food Fight, Brawling
 * Slash) already had in common, written once.
 *
 * <h2>Who counts</h2>
 *
 * {@link Relation} says which side of {@link Targetability} an ability wants. An ENEMY ray passes
 * through allies, because an ally between you and an enemy should not block your attack; an ALLY ray
 * passes through enemies; EITHER stops at the first of both. The caster is never a target.
 *
 * <h2>What it does not do</h2>
 *
 * It does not decide damage, knockback or what happens next, and it does not change what any ability
 * hits today: widths and extents are passed in, and the abilities moved onto it pass the values they
 * had. The point is that there is one place to look, and one place a width lives (config).
 */
public final class AbilityTargeting {
    private AbilityTargeting() {}

    /** Which targets an ability wants. */
    public enum Relation { ENEMY, ALLY, EITHER }

    /** What a ray found: a creature, or the block that stopped it, and where. */
    public record Hit(LivingEntity entity, Block block, Vector point) {
        public boolean entityHit() { return entity != null; }
    }

    /** Pure: whether a target that is (or is not) an enemy and (or is not) an ally satisfies a relation. */
    public static boolean accepts(Relation relation, boolean enemy, boolean ally) {
        return switch (relation) {
            case ENEMY -> enemy;
            case ALLY -> ally;
            case EITHER -> enemy || ally;
        };
    }

    /** The predicate for a relation, for a given caster. Living things only; never the caster. */
    public static Predicate<Entity> filter(Player caster, Relation relation) {
        return e -> e instanceof LivingEntity living && !e.equals(caster) && !living.isDead()
                && accepts(relation, Targetability.status(e, caster), Targetability.allied(e, caster));
    }

    /**
     * The first creature (or block) along a ray, stopped by walls.
     *
     * @param width the ray's thickness in blocks: how far off the line a creature can be and still be hit
     * @return what stopped the ray, or null when nothing did within {@code range}
     */
    public static Hit ray(Player caster, Location from, Vector direction, double range, double width, Relation relation) {
        if (direction.lengthSquared() < 1e-12 || range <= 0) return null;
        var hit = from.getWorld().rayTrace(from, direction.clone().normalize(), range, FluidCollisionMode.NEVER, true,
                width, filter(caster, relation));
        if (hit == null) return null;
        return new Hit(hit.getHitEntity() instanceof LivingEntity living ? living : null, hit.getHitBlock(), hit.getHitPosition());
    }

    /**
     * Every creature whose box overlaps a box around {@code centre}.
     *
     * @param lineOfSight when true, a creature with a block between it and the centre is left out
     */
    public static List<LivingEntity> within(Player caster, Location centre, double rx, double ry, double rz,
                                            Relation relation, boolean lineOfSight) {
        var out = new ArrayList<LivingEntity>();
        var test = filter(caster, relation);
        for (Entity e : centre.getWorld().getNearbyEntities(centre, rx, ry, rz)) {
            if (!test.test(e)) continue;
            if (lineOfSight && !visible(centre, ((LivingEntity) e).getEyeLocation())) continue;
            out.add((LivingEntity) e);
        }
        return out;
    }

    /** Whether no block lies on the straight line between two points. */
    public static boolean visible(Location from, Location to) {
        Vector line = to.toVector().subtract(from.toVector());
        double length = line.length();
        if (length < 1e-6) return true;
        return from.getWorld().rayTraceBlocks(from, line.normalize(), length, FluidCollisionMode.NEVER, true) == null;
    }

    /**
     * Pure: whether a creature offset {@code (dx, dz)} from a centre is inside a flat radius and, optionally, a cone.
     *
     * {@code coneDot} is the cosine of the half-angle: 1 is straight ahead only, 0 is a half circle, and -1 is the
     * whole circle. A creature almost on top of the centre is always inside, because it has no direction.
     */
    public static boolean inCone(double dx, double dz, double facingX, double facingZ, double radius, double coneDot) {
        double d2 = dx * dx + dz * dz;
        if (d2 > radius * radius) return false;
        if (d2 <= .01) return true;
        double len = Math.sqrt(d2);
        return (dx / len) * facingX + (dz / len) * facingZ >= coneDot;
    }
}
