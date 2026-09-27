package com.minecraftmoba.plugin;

import org.bukkit.entity.Animals;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

/**
 * Lightfooted's passive: the animals around you make you harder to put down.
 *
 * Ported from {@code codex/lightfooted-from-phase1}, which is the only place a
 * passive hook was ever implemented. That branch is not merged -- it reads its
 * ability branch from a single global config string rather than per player,
 * which predates and contradicts the draft and the Task ledger -- but its
 * mechanics are real and had no equivalent here.
 *
 * <h2>Three species, three unrelated benefits</h2>
 *
 * Wolves mitigate incoming damage, cats soften falls, foxes add speed. They are
 * deliberately not one stacking "animal bonus": the class's fantasy is
 * companionship with particular animals, and a player choosing which to travel
 * with is a decision with a position and a logistics cost attached.
 *
 * <h2>Counts are capped</h2>
 *
 * The source branch clamped only at zero, so enough wolves reached literal
 * damage immunity -- it computed {@code 1 - wolves * 0.02} and floored the
 * result. Animals are free, renewable and breedable, and this project's map
 * compiler deliberately stocks them, so an uncapped count is an invitation
 * rather than an edge case. The cap is applied to the COUNT rather than to the
 * resulting multiplier, so the ceiling is legible as "past four wolves, more
 * wolves do nothing" instead of an opaque floor on a product.
 *
 * The existing Swarming Bite branch already caps wolves at four for exactly
 * this reason; this matches it rather than inventing a second ceiling.
 *
 * <h2>Passives do not set combat state</h2>
 *
 * A passive is not an activation. Animal Senses is always on, so if it marked
 * combat the class would never leave it and could never level up or Recall.
 * Taking damage still sets combat state -- but that is the damage doing it, not
 * this. See docs/design/COMBAT_STATE.md §3.
 *
 * The arithmetic is static and takes plain numbers, so it is testable without a
 * server. {@link Passives} supplies the counts.
 */
public final class AnimalSenses {

    private AnimalSenses() {}

    /** How many of each species are near enough to count. */
    public record Bonus(int wolves, int foxes, int cats) {
        public Bonus {
            if (wolves < 0 || foxes < 0 || cats < 0)
                throw new IllegalArgumentException("An animal count cannot be negative.");
        }
        public boolean none() { return wolves == 0 && foxes == 0 && cats == 0; }
    }

    /**
     * Incoming damage multiplier from nearby wolves.
     *
     * Never returns zero: even at the cap the class mitigates rather than
     * negates, because a damage multiplier that reaches 0 is not a strong
     * passive but a broken one, and nothing else in the kit would be able to
     * tell the difference between "very tanky" and "unkillable".
     */
    public static double damageMultiplier(int wolves, double perWolf, int cap) {
        return 1 - Math.min(Math.max(0, wolves), cap) * Math.max(0, perWolf);
    }

    /** Fall-damage multiplier from nearby cats. Cats are why a Lightfooted drop is survivable. */
    public static double fallMultiplier(int cats, double perCat, int cap) {
        return 1 - Math.min(Math.max(0, cats), cap) * Math.max(0, perCat);
    }

    /** Movement-speed bonus from nearby foxes, as a fraction added to base speed. */
    public static double speedBonus(int foxes, double perFox, int cap) {
        return Math.min(Math.max(0, foxes), cap) * Math.max(0, perFox);
    }

    /** Which species an entity counts as, or null. Ocelots count as cats. */
    public static EntityType species(Entity entity) {
        if (!(entity instanceof Animals)) return null;
        return switch (entity.getType()) {
            case WOLF -> EntityType.WOLF;
            case FOX -> EntityType.FOX;
            case CAT, OCELOT -> EntityType.CAT;
            default -> null;
        };
    }

    /** Count the animals around a player, within {@code radius} on every axis. */
    public static Bonus nearby(Player player, double radius) {
        int wolves = 0, foxes = 0, cats = 0;
        for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
            EntityType species = species(entity);
            if (species == null) continue;
            switch (species) {
                case WOLF -> wolves++;
                case FOX -> foxes++;
                default -> cats++;
            }
        }
        return new Bonus(wolves, foxes, cats);
    }
}
