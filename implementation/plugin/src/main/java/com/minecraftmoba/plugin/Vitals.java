package com.minecraftmoba.plugin;

/**
 * The one place health and hunger change units.
 *
 * A player's bar is always twenty points long -- ten hearts, ten drumsticks --
 * regardless of Capacity. Capacity instead decides what a point is WORTH: a
 * player with 9 effective health and one with 24 both look at a full bar, and
 * the same hit takes a bigger bite out of the smaller pool.
 *
 * This is a display normalization, not a balance change. At Capacity 9 today a
 * 3-damage hit removes 3 of 9, a third of the player. Scaled, it removes 6.67 of
 * 20 -- still a third. Fractions are preserved exactly, so nothing about combat
 * changes except that the bar stops shrinking.
 *
 * <h2>Why this is a class and not four multiplications</h2>
 *
 * It introduces two units for one quantity, which is the single most reliable
 * source of bugs in this codebase: two config schemas, two eligibility
 * predicates, two world-load paths and two resets all shipped broken because one
 * copy was fixed and the other was not. A quantity with two units is the same
 * hazard wearing different clothes.
 *
 * So every conversion goes through here, the units are in the method names, and
 * VitalsTest pins the round trip. A caller that multiplies by 20.0 itself is a
 * bug whatever the number says.
 */
public final class Vitals {

    /** The bar is always this long. Ten hearts, ten drumsticks, every player. */
    public static final double DISPLAY_MAX = 20.0;

    private Vitals() {}

    /** Effective points -> the 0..20 the client draws. */
    public static double toDisplay(double effective, double capacity) {
        if (capacity <= 0) return 0;
        return clamp(effective * DISPLAY_MAX / capacity);
    }

    /** The 0..20 the client draws -> effective points, which is what the design means. */
    public static double toEffective(double display, double capacity) {
        if (capacity <= 0) return 0;
        return display * capacity / DISPLAY_MAX;
    }

    /**
     * How much of the bar a hit takes.
     *
     * Damage arrives in effective points -- a zombie hits for 3 whatever your
     * Capacity -- and the bar is in display points, so it scales up for a small
     * pool and down for a large one.
     */
    public static double scaleDamage(double effectiveDamage, double capacity) {
        return capacity <= 0 ? effectiveDamage : effectiveDamage * DISPLAY_MAX / capacity;
    }

    /** Healing is the same conversion: a rate stated in effective points. */
    public static double scaleHealing(double effectiveHealing, double capacity) {
        return scaleDamage(effectiveHealing, capacity);
    }

    /**
     * Exhaustion multiplier for hunger.
     *
     * Hunger's bar was already a fixed ten drumsticks -- vanilla never shortens
     * it -- so Capacity could only ever be expressed by refusing to fill part of
     * it, which is what produced a row of drumsticks a player could never reach.
     * Expressed as a rate instead, a smaller Hunger Capacity simply empties the
     * same bar faster, and every position becomes reachable.
     */
    public static double exhaustionMultiplier(double hungerCapacity) {
        return hungerCapacity <= 0 ? 1.0 : DISPLAY_MAX / hungerCapacity;
    }

    private static double clamp(double display) {
        return Math.max(0, Math.min(DISPLAY_MAX, display));
    }
}
