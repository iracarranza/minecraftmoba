package com.minecraftmoba.plugin;

/**
 * The arithmetic of Lured and Frightened. Pure: no server, so the geometry is a test and not a
 * thing found by being dragged around a test arena.
 *
 * <h2>The two statuses are one rule with a sign</h2>
 *
 * Both turn the entity to face the source and move it along the line between them, uncontrollably.
 * Lured walks toward the source; Frightened walks away from it, still facing it, so it backs off.
 * Nothing else differs, and the difference is a single sign here, not two code paths.
 *
 * <h2>What this does not decide</h2>
 *
 * Whether a step is blocked by a wall, whether the entity is also stunned or rooted, and how a
 * real client is made to take the step are runtime concerns in {@link ForcedMovement}.
 */
public final class ForcedMotion {
    private ForcedMotion() {}

    public enum Kind { LURED, FRIGHTENED }

    /**
     * One tick's forced movement.
     *
     * @param dx horizontal step along x, in blocks (zero when arrived or on top of the source)
     * @param dz horizontal step along z
     * @param yaw the yaw that faces the source, or NaN when the source is directly above or below
     * @param pitch the pitch that faces the source
     * @param arrived a Lured entity close enough to stop
     */
    public record Step(double dx, double dz, float yaw, float pitch, boolean arrived) {
        public static final Step NONE = new Step(0, 0, Float.NaN, 0f, false);
        public boolean moves() { return dx != 0 || dz != 0; }
    }

    /**
     * @param eye the entity's eye position {x, y, z}
     * @param feet the entity's feet position {x, y, z}
     * @param source the point it is lured to or frightened from {x, y, z}
     * @param speed blocks per tick (a walk is about 0.2158)
     * @param stopDistance a Lured entity stops this far from the source
     */
    public static Step step(Kind kind, double[] eye, double[] feet, double[] source, double speed, double stopDistance) {
        double hx = source[0] - feet[0], hz = source[2] - feet[2];
        double horizontal = Math.hypot(hx, hz);
        float yaw = horizontal < 1e-9 ? Float.NaN : yawOf(hx, hz);
        float pitch = pitchOf(source[0] - eye[0], source[1] - eye[1], source[2] - eye[2]);
        if (horizontal < 1e-9) return new Step(0, 0, yaw, pitch, kind == Kind.LURED);
        double ux = hx / horizontal, uz = hz / horizontal;
        if (kind == Kind.FRIGHTENED) return new Step(-ux * speed, -uz * speed, yaw, pitch, false);
        double room = horizontal - stopDistance;
        if (room <= 1e-9) return new Step(0, 0, yaw, pitch, true);
        double len = Math.min(speed, room);
        return new Step(ux * len, uz * len, yaw, pitch, len >= room - 1e-9);
    }

    /** Minecraft's yaw for a horizontal direction: south (+z) is 0, west (-x) is 90. */
    public static float yawOf(double dx, double dz) {
        return (float) Math.toDegrees(Math.atan2(-dx, dz));
    }

    /** Minecraft's pitch for a direction: positive looks down. */
    public static float pitchOf(double dx, double dy, double dz) {
        double h = Math.hypot(dx, dz);
        if (h < 1e-9 && Math.abs(dy) < 1e-9) return 0f;
        return (float) -Math.toDegrees(Math.atan2(dy, h));
    }

    /** The smallest angle between two yaws, in degrees, 0 to 180. */
    public static double yawDifference(double a, double b) {
        double d = Math.abs(((a - b) % 360 + 540) % 360 - 180);
        return d;
    }
}
