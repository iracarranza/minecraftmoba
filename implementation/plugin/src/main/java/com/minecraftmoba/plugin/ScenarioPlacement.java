package com.minecraftmoba.plugin;

import java.util.function.IntBinaryOperator;

/**
 * Where a roster member stands: an offset from an anchor on the launched map, at ground level.
 *
 * A scoop's own coordinates differ from one map to the next (the first three sit around x -2100
 * and z 1500), so a scenario that said "at 10, 70, 0" would work on exactly one map. Offsets from
 * a named anchor, with a vertical offset from the GROUND there, work on any of them.
 *
 * Pure. The ground lookup is injected so this is tested without a world.
 */
public final class ScenarioPlacement {
    private ScenarioPlacement() {}

    /** The anchors a scenario may name. */
    public static final java.util.Set<String> ANCHORS = java.util.Set.of("fountain_north", "fountain_south", "lair", Scenario.ABSOLUTE);

    /**
     * @param anchorX the anchor's block x (ignored when the scenario is absolute)
     * @param groundY the highest block y at an x and z
     * @return {x, y, z} centred in its block, one block above the ground plus the member's vertical offset
     */
    public static double[] resolve(Scenario.Member m, boolean absolute, int anchorX, int anchorZ, IntBinaryOperator groundY) {
        if (absolute) return new double[]{m.x(), m.y(), m.z()};
        int bx = (int) Math.floor(anchorX + m.x()), bz = (int) Math.floor(anchorZ + m.z());
        return new double[]{bx + 0.5, groundY.applyAsInt(bx, bz) + 1 + m.y(), bz + 0.5};
    }

    public static String refusal(String anchor) {
        return ANCHORS.contains(anchor) ? null : "Unknown anchor '" + anchor + "'. Anchors: " + new java.util.TreeSet<>(ANCHORS) + ".";
    }
}
