package com.minecraftmoba.plugin;

import java.util.Random;

/**
 * Where a commanded body points, as plain arithmetic.
 *
 * A body has no mouse, so "aim" is a yaw and a pitch set on it before the cast. The
 * math is Minecraft's: yaw 0 faces +Z and increases clockwise seen from above (so
 * +X is -90), pitch is positive looking DOWN. Kept apart from Bukkit so it is
 * tested without a server.
 */
public final class CastAim {
    private CastAim() {}

    /** A facing, in degrees. */
    public record Facing(float yaw, float pitch) {}

    /** The facing that looks from one point toward another. Equal points give straight ahead (0, 0). */
    public static Facing toward(double fromX, double fromY, double fromZ, double toX, double toY, double toZ) {
        double dx = toX - fromX, dy = toY - fromY, dz = toZ - fromZ;
        double flat = Math.hypot(dx, dz);
        if (flat < 1e-9 && Math.abs(dy) < 1e-9) return new Facing(0f, 0f);
        float yaw = flat < 1e-9 ? 0f : (float) Math.toDegrees(Math.atan2(-dx, dz));
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, flat));
        return new Facing(yaw, pitch);
    }

    /** A fresh horizontal direction, level pitch. Seeded by the caller so a take can be reproduced. */
    public static Facing random(Random rng) {
        return new Facing(rng.nextFloat() * 360f - 180f, 0f);
    }

    /** The unit direction a facing looks along, as {x, y, z}. */
    public static double[] direction(Facing f) {
        double yaw = Math.toRadians(f.yaw()), pitch = Math.toRadians(f.pitch());
        double h = Math.cos(pitch);
        return new double[] { -Math.sin(yaw) * h, -Math.sin(pitch), Math.cos(yaw) * h };
    }
}
