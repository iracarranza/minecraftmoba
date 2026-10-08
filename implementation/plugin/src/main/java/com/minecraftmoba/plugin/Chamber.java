package com.minecraftmoba.plugin;

import java.util.Collection;
import java.util.Objects;
import java.util.UUID;

/**
 * A bounded region of a lab world that one tester owns.
 *
 * The lab is already a disposable world; a chamber is smaller and sharper. It
 * exists so that a placement experiment has <b>somewhere it cannot escape
 * from</b>, which is what makes "generate new terrain here" and "undo
 * everything here" answerable at all.
 *
 * <h2>Containment is checked before placement, not after</h2>
 *
 * A route that wanders out of its chamber and a structure whose footprint
 * overhangs the edge are the same defect, and both are cheap to refuse and
 * expensive to clean up. {@link #encloses} is the gate, and it asks about the
 * WHOLE placement rather than its anchor: an anchor inside a chamber says
 * nothing about where a 40-block corridor ends up.
 */
public record Chamber(UUID id, UUID world, int minX, int minY, int minZ,
                      int maxX, int maxY, int maxZ, UUID owner) {

    public Chamber {
        Objects.requireNonNull(id);
        Objects.requireNonNull(world);
        if (maxX < minX || maxY < minY || maxZ < minZ)
            throw new IllegalArgumentException("A chamber's maxima must not precede its minima.");
    }

    /** A position, independent of Bukkit so the rules can be tested. */
    public record At(UUID world, int x, int y, int z) {}

    public static Chamber around(UUID world, int x, int y, int z, int radius, int height, UUID owner) {
        return new Chamber(UUID.randomUUID(), world,
                x - radius, y, z - radius, x + radius, y + height, z + radius, owner);
    }

    public boolean contains(At at) {
        return at != null && world.equals(at.world())
                && at.x() >= minX && at.x() <= maxX
                && at.y() >= minY && at.y() <= maxY
                && at.z() >= minZ && at.z() <= maxZ;
    }

    /**
     * Whether every position of a placement is inside.
     *
     * Empty is <b>not</b> enclosed. A placement that affects nothing is not a
     * placement that fits; it is one that failed to produce anything, and
     * answering "yes, it fits" would let it through the one gate that would
     * have noticed.
     */
    public boolean encloses(Collection<At> positions) {
        if (positions == null || positions.isEmpty()) return false;
        for (At at : positions) if (!contains(at)) return false;
        return true;
    }

    /** Positions of a placement that fall outside, for a refusal that says where. */
    public java.util.List<At> escapes(Collection<At> positions) {
        var out = new java.util.ArrayList<At>();
        if (positions == null) return out;
        for (At at : positions) if (!contains(at)) out.add(at);
        return out;
    }

    public long volume() {
        return (long) (maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1);
    }

    /**
     * Whether two chambers share any space.
     *
     * Two testers editing overlapping ground would see each other's terrain
     * change under them, and an undo in one would revert blocks the other
     * placed. Chambers are allotted rather than claimed freely, and this is
     * the check that keeps them apart.
     */
    public boolean overlaps(Chamber other) {
        return other != null && world.equals(other.world())
                && minX <= other.maxX && maxX >= other.minX
                && minY <= other.maxY && maxY >= other.minY
                && minZ <= other.maxZ && maxZ >= other.minZ;
    }

    public boolean ownedBy(UUID player) { return owner != null && owner.equals(player); }
}
