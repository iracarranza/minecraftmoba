package com.minecraftmoba.plugin;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Who has which chamber, and where the next one goes.
 *
 * <h2>Allotted on a grid, not placed on request</h2>
 *
 * Chambers could be claimed wherever a tester stands, with an overlap check to
 * refuse the bad cases. A grid is better for the same reason a car park has
 * bays: non-overlap stops being a rule that is enforced and becomes a
 * property of the layout. {@link Chamber#overlaps} survives as the assertion
 * that the arithmetic is right, rather than as the thing standing between two
 * testers and each other's terrain.
 *
 * <h2>One chamber per tester</h2>
 *
 * Allotting a second releases the first. A tester with two chambers has two
 * undo histories and no way to say which {@code /moba lab undo} meant, and the
 * question has no good answer -- so it is not asked.
 */
public final class Chambers {

    /** Blocks of untouched ground between neighbouring chambers. */
    public static final int GUTTER = 8;

    private final UUID world;
    private final int radius, height, floor;
    private final Map<UUID, Chamber> byOwner = new LinkedHashMap<>();

    public Chambers(UUID world, int radius, int height, int floor) {
        if (radius <= 0) throw new IllegalArgumentException("A chamber needs a positive radius.");
        this.world = world; this.radius = radius; this.height = height; this.floor = floor;
    }

    /** Centre-to-centre spacing. Two radii plus a gutter, so bays never touch. */
    public int pitch() { return radius * 2 + 1 + GUTTER; }

    /**
     * The centre of bay {@code n}, laid out in a square spiral from the origin.
     *
     * A row would put the twentieth tester a long walk from the first. A spiral
     * keeps every bay within a comparable distance of the lab's centre, which
     * matters because a tester walks to their chamber and back on every
     * session.
     */
    public int[] bay(int n) {
        int ring = (int) Math.ceil((Math.sqrt(Math.max(1, n + 1)) - 1) / 2.0);
        int side = ring * 2 + 1;
        int first = (side - 2) * (side - 2);
        int offset = n - first;
        int x = ring, z = -ring;
        if (offset < side - 1) { z = -ring + offset; }
        else if (offset < 2 * (side - 1)) { x = ring - (offset - (side - 1)); z = ring; }
        else if (offset < 3 * (side - 1)) { x = -ring; z = ring - (offset - 2 * (side - 1)); }
        else { x = -ring + (offset - 3 * (side - 1)); z = -ring; }
        return new int[]{x * pitch(), z * pitch()};
    }

    /** The chamber a tester holds, or null. */
    public Chamber of(UUID owner) { return byOwner.get(owner); }

    /** The chamber containing a position, or null. */
    public Chamber at(Chamber.At at) {
        for (Chamber c : byOwner.values()) if (c.contains(at)) return c;
        return null;
    }

    public List<Chamber> all() { return new ArrayList<>(byOwner.values()); }

    /**
     * Give a tester a chamber, replacing any they already hold.
     *
     * The returned chamber is in the lowest free bay, so a lab that has been
     * used and released does not sprawl: the next tester gets the gap rather
     * than the next ring out.
     */
    public Chamber allot(UUID owner) {
        byOwner.remove(owner);
        for (int n = 0; ; n++) {
            int[] centre = bay(n);
            Chamber candidate = Chamber.around(world, centre[0], floor, centre[1],
                    radius, height, owner);
            if (byOwner.values().stream().noneMatch(candidate::overlaps)) {
                byOwner.put(owner, candidate);
                return candidate;
            }
        }
    }

    /** Hand a chamber back. Returns what was released, or null. */
    public Chamber release(UUID owner) { return byOwner.remove(owner); }

    public void clear() { byOwner.clear(); }

    public int size() { return byOwner.size(); }
}
