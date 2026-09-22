package com.minecraftmoba.plugin;

import java.util.ArrayList;
import java.util.List;

/**
 * The lobby's geometry, as pure arithmetic.
 *
 * Separated from the world so the properties that matter can be asserted
 * without a server: that the hall is actually ENCLOSED, that it is lit
 * throughout, and that the spawn point is inside it. Those are the three ways a
 * lobby fails in practice -- a gap you can walk out of, a dark corner that
 * spawns something, or a spawn point in a wall -- and none of them is visible
 * by reading the loop that places the blocks.
 *
 * This is a GREYBOX. It decides extent, enclosure and lighting, which are
 * function. It decides nothing about what the room looks like: materials come
 * from config, and the visual language is a design deliverable, the same way
 * massing.py and the resource pack ship deliberate placeholders.
 */
public final class LobbyHall {

    /** One block to place: offset from the hall's origin, and which role it plays. */
    public record Piece(int x, int y, int z, Role role) {}

    public enum Role { FLOOR, WALL, CEILING, LIGHT, PLATFORM }

    private LobbyHall() {}

    /**
     * Every block of a hall centred on (0, floorY, 0).
     *
     * `radius` is the interior half-span, so the room is (2r+1) across and the
     * walls sit at exactly ±(r+1). Height is interior: the ceiling is at
     * floorY + height + 1.
     */
    public static List<Piece> hall(int radius, int floorY, int height, int lightSpacing) {
        if (radius < 2) throw new IllegalArgumentException("a hall needs room to stand in");
        if (height < 3) throw new IllegalArgumentException("a hall needs headroom");
        if (lightSpacing < 1) throw new IllegalArgumentException("lightSpacing must be positive");
        var out = new ArrayList<Piece>();
        int wall = radius + 1;
        int ceiling = floorY + height + 1;

        for (int x = -wall; x <= wall; x++)
            for (int z = -wall; z <= wall; z++) {
                boolean perimeter = Math.abs(x) == wall || Math.abs(z) == wall;
                out.add(new Piece(x, floorY, z, Role.FLOOR));
                out.add(new Piece(x, ceiling, z, Role.CEILING));
                if (perimeter)
                    for (int y = floorY + 1; y < ceiling; y++)
                        out.add(new Piece(x, y, z, Role.WALL));
            }

        // Lights in the ceiling on a grid. Spacing is a fixture; what is not
        // negotiable is that the interior has no unlit cell, which the test
        // checks by distance rather than by trusting the loop.
        for (int x = -radius; x <= radius; x++)
            for (int z = -radius; z <= radius; z++)
                if (Math.floorMod(x + radius, lightSpacing) == 0
                        && Math.floorMod(z + radius, lightSpacing) == 0)
                    out.add(new Piece(x, ceiling, z, Role.LIGHT));

        // A small raised platform at the centre, so arriving somewhere is
        // legible rather than being dropped onto an empty floor.
        for (int x = -1; x <= 1; x++)
            for (int z = -1; z <= 1; z++)
                out.add(new Piece(x, floorY + 1, z, Role.PLATFORM));

        return out;
    }

    /** Where a player arrives: standing on the platform, at the centre. */
    public static int[] spawn(int floorY) { return new int[]{0, floorY + 2, 0}; }

    /** Whether a point is inside the hall's interior air. */
    public static boolean inside(int x, int y, int z, int radius, int floorY, int height) {
        return Math.abs(x) <= radius && Math.abs(z) <= radius
                && y > floorY && y <= floorY + height;
    }
}
