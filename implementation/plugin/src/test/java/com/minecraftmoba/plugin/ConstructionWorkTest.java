package com.minecraftmoba.plugin;

import org.bukkit.Location;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Construction pays for building somewhere, not for firing BlockPlaceEvent.
 *
 * The handler previously paid 1 WP for every placement of anything, which made
 *
 *     place block -> break it -> place it again
 *
 * an unbounded Construction generator that needed no material, no time and no
 * structure. Crediting a POSITION rather than an EVENT closes it without
 * resorting to a cooldown or a diminishing return, both of which would also
 * have punished someone legitimately building a long wall.
 */
class ConstructionWorkTest {

    private final Map<UUID, Set<Long>> memory = new HashMap<>();
    private final UUID alice = UUID.randomUUID();
    private final UUID bob = UUID.randomUUID();

    private static long at(int x, int y, int z) {
        return WorkPoints.key(new Location(null, x, y, z));
    }

    @Test void placeBreakPlaceCannotLoop() {
        assertTrue(WorkPoints.firstTime(memory, alice, at(10, 64, 10)), "first build pays");
        // ...player breaks it and places it back...
        assertFalse(WorkPoints.firstTime(memory, alice, at(10, 64, 10)), "rebuild pays nothing");
        for (int i = 0; i < 100; i++)
            assertFalse(WorkPoints.firstTime(memory, alice, at(10, 64, 10)),
                    "and still nothing on the hundredth cycle");
    }

    @Test void buildingSomewhereNewAlwaysPaysInFull() {
        // The rule must not read as a rate limit: a wall is many positions and
        // every one of them is a position that was not built before.
        for (int x = 0; x < 50; x++)
            assertTrue(WorkPoints.firstTime(memory, alice, at(x, 64, 0)), "wall segment " + x);
    }

    @Test void oneBuildersHistoryDoesNotConsumeAnothers() {
        assertTrue(WorkPoints.firstTime(memory, alice, at(3, 70, 3)));
        assertTrue(WorkPoints.firstTime(memory, bob, at(3, 70, 3)),
                "a teammate rebuilding a lost position is doing that work themselves");
    }

    @Test void positionsAreDistinguishedOnEveryAxis() {
        // A packed key that collided would silently refuse to pay for real
        // building, which is a quieter failure than the loop it prevents.
        assertNotEquals(at(0, 0, 0), at(1, 0, 0));
        assertNotEquals(at(0, 0, 0), at(0, 1, 0));
        assertNotEquals(at(0, 0, 0), at(0, 0, 1));
        assertNotEquals(at(-2400, 74, -460), at(-2400, 74, -461));
        assertNotEquals(at(-2400, 74, -460), at(-2401, 74, -460));
        assertEquals(at(-2400, 74, -460), at(-2400, 74, -460));
    }
}
