package com.minecraftmoba.plugin;

import java.util.Locale;

/**
 * One presentation under test: the combination of things a state can look like.
 *
 * These are the mechanisms the Werewolf presentation investigation found reachable
 * without art: scale ({@code Attribute.SCALE}), glow, worn armor, and particles; plus
 * clutter between observer and subject, which is a property of the bench, not of the
 * subject. A variant is a value, cycled one axis at a time, so a mark taken under one
 * is never reported as another.
 *
 * Pure. Tested without a server.
 */
public record LegibilityVariant(int scaleStep, boolean glow, Armor armor, Particles particles, boolean clutter) {

    /** The scale ladder. 1.0 is the control: an unmodified player. */
    public static final double[] SCALES = {1.0, 1.25, 1.5, 2.0, 3.0};
    /** A player's height in blocks at scale 1. */
    public static final double PLAYER_HEIGHT = 1.8;

    public enum Armor { NONE, LEATHER, IRON, DIAMOND, NETHERITE }
    public enum Particles { NONE, FLAME, SOUL_FIRE_FLAME, END_ROD }

    public LegibilityVariant {
        if (scaleStep < 0 || scaleStep >= SCALES.length) throw new IllegalArgumentException("No scale step " + scaleStep);
    }

    /** The control: nothing applied. */
    public static LegibilityVariant control() {
        return new LegibilityVariant(0, false, Armor.NONE, Particles.NONE, false);
    }

    public double scale() { return SCALES[scaleStep]; }
    public double heightBlocks() { return PLAYER_HEIGHT * scale(); }

    public LegibilityVariant nextScale() {
        return new LegibilityVariant((scaleStep + 1) % SCALES.length, glow, armor, particles, clutter);
    }
    public LegibilityVariant toggleGlow() { return new LegibilityVariant(scaleStep, !glow, armor, particles, clutter); }
    public LegibilityVariant nextArmor() {
        var all = Armor.values();
        return new LegibilityVariant(scaleStep, glow, all[(armor.ordinal() + 1) % all.length], particles, clutter);
    }
    public LegibilityVariant nextParticles() {
        var all = Particles.values();
        return new LegibilityVariant(scaleStep, glow, armor, all[(particles.ordinal() + 1) % all.length], clutter);
    }
    public LegibilityVariant toggleClutter() { return new LegibilityVariant(scaleStep, glow, armor, particles, !clutter); }

    /** The same variant with the clutter flag ignored: clutter belongs to the bench, so it is reported beside, not inside, the subject. */
    public String signature() {
        return String.format(Locale.ROOT, "scale %.2f | glow %s | armor %s | particles %s | clutter %s",
                scale(), glow ? "on" : "off", armor.name().toLowerCase(Locale.ROOT),
                particles.name().toLowerCase(Locale.ROOT), clutter ? "on" : "off");
    }
}
