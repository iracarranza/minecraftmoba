package com.minecraftmoba.plugin;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Mole's passive: breaking sand or gravel sifts the material connected to it
 * rather than letting it collapse.
 *
 * <h2>What this class is</h2>
 *
 * The reach: given the block Mole broke, which connected blocks go with it.
 * That is ordinary graph work and is settled here without a server. {@link
 * Passives} does the two things that need a world -- reading block types and
 * clearing them.
 *
 * <h2>Breadth-first, because the cap has to drop the right blocks</h2>
 *
 * A volume cap is reached constantly: a desert dune is thousands of connected
 * sand blocks. Which blocks a cap discards is therefore the whole behaviour,
 * not an edge case.
 *
 * Breadth-first means the set grows outward from the break, so a cap truncates
 * the <b>far edge</b> and what Mole gets is a contiguous pocket around where
 * they dug. A depth-first or arbitrary fill would hand back a tendril reaching
 * across the dune, which is both unreadable and a different ability.
 *
 * <h2>Player-placed material is not sifted</h2>
 *
 * Tunneling already refuses to dig blocks {@link Provenance} knows a player
 * put there, and the same reason applies harder here: sifting is faster than
 * mining and reaches through a whole connected mass, so without the check a
 * sand or gravel wall would come apart in one hit. That is a siege ability,
 * and nobody designed it.
 *
 * <h2>[OPEN] Two readings of "while actively digging"</h2>
 *
 * classes.md says the sense operates "while actively digging". That is
 * implemented here as <b>breaking a siftable block at all</b>, rather than as
 * requiring Tunneling to be running, because a passive gated on an ability is
 * not a passive -- it is a rider on A1, and the design document lists it as
 * the class's passive.
 *
 * The other reading is defensible and the gate is one predicate at the call
 * site, so switching is a line. It is recorded rather than decided.
 *
 * <h2>[OPEN] Sift versus clear</h2>
 *
 * "Sift or clear ... substantially more effectively" permits either a yield
 * bonus or mere removal, and classes.md does not choose. This returns the
 * reach and takes no position; what the caller does with it is where that
 * decision will land.
 */
public final class SifthSense {

    /**
     * How far from the break the sense reaches, as a cube half-width.
     *
     * [FIXTURE -- expect to tune] classes.md records sense radius as
     * unresolved. This is a working value, not a balance decision.
     */
    public static final int DEFAULT_RADIUS = 5;

    /**
     * The most blocks one break may sift.
     *
     * [FIXTURE -- expect to tune] Also unresolved in classes.md. It exists at
     * all because a dune is effectively unbounded and an uncapped fill is a
     * server stall, not a strong ability.
     */
    public static final int DEFAULT_VOLUME = 64;

    /** A position, so the reach can be computed with no world. */
    public record At(int x, int y, int z) {
        At offset(int dx, int dy, int dz) { return new At(x + dx, y + dy, z + dz); }

        /** Cube distance from another position: the shape a radius means here. */
        int chebyshev(At other) {
            return Math.max(Math.abs(x - other.x()),
                   Math.max(Math.abs(y - other.y()), Math.abs(z - other.z())));
        }
    }

    /** The six face neighbours. Material connects through faces, not corners. */
    private static final int[][] FACES = {
            {1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}
    };

    /**
     * The blocks one break sifts, nearest first.
     *
     * @param origin    the block Mole broke; not included in the result, because
     *                  breaking it is what already happened
     * @param siftable  whether a position holds material this sense acts on,
     *                  which the caller answers from the world INCLUDING
     *                  provenance
     * @param radius    cube half-width around the origin
     * @param volume    the most blocks to return
     */
    public static List<At> reach(At origin, Predicate<At> siftable, int radius, int volume) {
        var found = new ArrayList<At>();
        if (origin == null || volume <= 0 || radius < 0 || !siftable.test(origin)) return found;

        Set<At> seen = new LinkedHashSet<>();
        var queue = new ArrayDeque<At>();
        seen.add(origin);
        queue.add(origin);

        while (!queue.isEmpty() && found.size() < volume) {
            At at = queue.poll();
            for (int[] face : FACES) {
                At next = at.offset(face[0], face[1], face[2]);
                if (next.chebyshev(origin) > radius) continue;
                if (!seen.add(next)) continue;
                if (!siftable.test(next)) continue;
                found.add(next);
                if (found.size() >= volume) break;
                queue.add(next);
            }
        }
        return found;
    }

    public static List<At> reach(At origin, Predicate<At> siftable) {
        return reach(origin, siftable, DEFAULT_RADIUS, DEFAULT_VOLUME);
    }

    private SifthSense() {}
}
