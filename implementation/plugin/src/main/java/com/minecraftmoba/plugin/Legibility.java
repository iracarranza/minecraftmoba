package com.minecraftmoba.plugin;

/**
 * The arithmetic of how large a subject looks, and the doctrine for who is shown a glow.
 *
 * <h2>How large it looks</h2>
 *
 * A subject {@code h} blocks tall at distance {@code d} subtends
 * {@code 2 atan(h / 2d)}. On screen that is a fraction of the vertical field of view,
 * which Minecraft's FOV slider sets (default 70). These are computed so a tester's
 * "it still reads at 40 blocks" can be set beside "it was 31 pixels tall", and a
 * scale step judged by what it buys rather than by what it feels like.
 *
 * <h2>Who is shown a glow (classes.md, "Global Effect Legibility", settled 1 October 2026)</h2>
 *
 * <ul>
 * <li><b>Tier 1, Ultimate state</b>: unconditional public information. Everyone,
 *     always, with no Combat, proximity or team condition.</li>
 * <li><b>Tier 2, self-empowerment</b>: an ALLY always sees it; an ENEMY sees it only
 *     while the enemy is in Combat AND within the Empowerment Legibility Radius AND the
 *     empowered player is not Invisible. Invisibility wins.</li>
 * </ul>
 *
 * The radius is not set in canon, so it is a parameter here and a prototype value on
 * the bench. This function is the RULE; whether the game can deliver it per viewer is
 * a separate, open technical question (the flag is all-or-nothing).
 *
 * Pure. Tested without a server.
 */
public final class Legibility {
    private Legibility() {}

    public static final double DEFAULT_FOV = 70.0;
    public static final int DEFAULT_SCREEN_HEIGHT = 1080;
    /** [PROTOTYPE] The Empowerment Legibility Radius is not settled; this is a bench value, not a decision. */
    public static final double PROTOTYPE_RADIUS = 24.0;

    /** The vertical angle a subject of this height subtends at this distance, in degrees. */
    public static double angularHeightDegrees(double heightBlocks, double distance) {
        if (distance <= 0) throw new IllegalArgumentException("Distance must be positive.");
        return Math.toDegrees(2 * Math.atan(heightBlocks / (2 * distance)));
    }

    /** How tall the subject is on screen, in pixels, for a vertical FOV and screen height. */
    public static double pixelHeight(double heightBlocks, double distance, double verticalFovDegrees, int screenHeight) {
        double subtended = Math.toRadians(angularHeightDegrees(heightBlocks, distance));
        return screenHeight * Math.tan(subtended / 2) / Math.tan(Math.toRadians(verticalFovDegrees) / 2);
    }

    /** The distance at which a subject shrinks to a given pixel height: where it stops being a figure. */
    public static double distanceForPixels(double heightBlocks, double pixels, double verticalFovDegrees, int screenHeight) {
        if (pixels <= 0) throw new IllegalArgumentException("Pixels must be positive.");
        double halfFov = Math.tan(Math.toRadians(verticalFovDegrees) / 2);
        return heightBlocks * screenHeight / (2 * pixels * halfFov);
    }

    public enum Tier { ULTIMATE, EMPOWERMENT }
    public enum Relation { ALLY, ENEMY }

    /** An observer's situation, as the doctrine asks it. */
    public record Observer(Relation relation, boolean inCombat, double distance) {}

    /** Whether this observer is shown the glow, by the settled rule. */
    public static boolean glowVisible(Tier tier, Observer observer, double radius, boolean subjectInvisible) {
        if (tier == Tier.ULTIMATE) return true;
        if (observer.relation() == Relation.ALLY) return true;
        return observer.inCombat() && observer.distance() <= radius && !subjectInvisible;
    }

    /** The rule in words for one observer, for the bench to show beside what is actually displayed. */
    public static String explain(Tier tier, Observer o, double radius, boolean invisible) {
        boolean shown = glowVisible(tier, o, radius, invisible);
        if (tier == Tier.ULTIMATE) return "Ultimate glow: shown to everyone, always.";
        if (o.relation() == Relation.ALLY) return "Empowerment glow: an ally always sees it.";
        if (shown) return "Empowerment glow: shown (enemy, in combat, within " + (int) radius + " blocks, not invisible).";
        String why = !o.inCombat() ? "the enemy is not in combat"
                : o.distance() > radius ? "the enemy is beyond " + (int) radius + " blocks" : "the subject is invisible";
        return "Empowerment glow: hidden (" + why + ").";
    }
}
