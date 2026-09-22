package com.minecraftmoba.plugin;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * A lobby fails in three ways, and none of them is visible by reading the loop
 * that places the blocks: a gap you can walk out of, an unlit cell, or a spawn
 * point inside a wall.
 *
 * So the geometry is pure and these check the properties rather than the code.
 */
class LobbyHallTest {

    private static final int R = 16, FLOOR = 64, HEIGHT = 8, SPACING = 6;

    private static Set<String> at(List<LobbyHall.Piece> hall, LobbyHall.Role role) {
        var out = new HashSet<String>();
        for (var p : hall) if (p.role() == role) out.add(p.x() + ":" + p.y() + ":" + p.z());
        return out;
    }

    @Test void theHallIsFullyEnclosed() {
        // Every interior cell must be walled on all four sides at every height.
        var hall = LobbyHall.hall(R, FLOOR, HEIGHT, SPACING);
        var walls = at(hall, LobbyHall.Role.WALL);
        int edge = R + 1;
        for (int y = FLOOR + 1; y <= FLOOR + HEIGHT; y++)
            for (int i = -edge; i <= edge; i++) {
                assertTrue(walls.contains((-edge) + ":" + y + ":" + i), "gap in the -X wall");
                assertTrue(walls.contains(edge + ":" + y + ":" + i), "gap in the +X wall");
                assertTrue(walls.contains(i + ":" + y + ":" + (-edge)), "gap in the -Z wall");
                assertTrue(walls.contains(i + ":" + y + ":" + edge), "gap in the +Z wall");
            }
    }

    @Test void theHallHasAFloorAndACeiling() {
        var hall = LobbyHall.hall(R, FLOOR, HEIGHT, SPACING);
        var floor = at(hall, LobbyHall.Role.FLOOR);
        var ceiling = at(hall, LobbyHall.Role.CEILING);
        int edge = R + 1;
        for (int x = -edge; x <= edge; x++)
            for (int z = -edge; z <= edge; z++) {
                assertTrue(floor.contains(x + ":" + FLOOR + ":" + z), "hole in the floor at " + x + "," + z);
                assertTrue(ceiling.contains(x + ":" + (FLOOR + HEIGHT + 1) + ":" + z),
                        "hole in the ceiling at " + x + "," + z);
            }
    }

    @Test void everyInteriorCellIsNearALight() {
        // Not "lights were placed" -- that a dark corner cannot exist. An unlit
        // cell in a lobby is where something spawns in a world where mob
        // spawning is ever re-enabled by accident.
        var lights = at(LobbyHall.hall(R, FLOOR, HEIGHT, SPACING), LobbyHall.Role.LIGHT);
        assertFalse(lights.isEmpty());
        for (int x = -R; x <= R; x++)
            for (int z = -R; z <= R; z++) {
                boolean near = false;
                for (String l : lights) {
                    String[] parts = l.split(":");
                    if (Math.abs(Integer.parseInt(parts[0]) - x) <= SPACING
                            && Math.abs(Integer.parseInt(parts[2]) - z) <= SPACING) { near = true; break; }
                }
                assertTrue(near, "no light within " + SPACING + " of " + x + "," + z);
            }
    }

    @Test void theSpawnPointIsStandingRoomInsideTheHall() {
        int[] spawn = LobbyHall.spawn(FLOOR);
        assertTrue(LobbyHall.inside(spawn[0], spawn[1], spawn[2], R, FLOOR, HEIGHT),
                "spawn must be interior air, not a wall or the floor");
        // And it must be on top of the platform rather than inside it.
        var platform = at(LobbyHall.hall(R, FLOOR, HEIGHT, SPACING), LobbyHall.Role.PLATFORM);
        assertTrue(platform.contains("0:" + (FLOOR + 1) + ":0"));
        assertEquals(FLOOR + 2, spawn[1], "a player stands on the platform, not in it");
    }

    @Test void noBlockIsClaimedTwiceForDifferentRoles() {
        // Two roles on one coordinate means whichever is placed last silently
        // wins -- a light overwritten by ceiling is exactly the invisible bug.
        var hall = LobbyHall.hall(R, FLOOR, HEIGHT, SPACING);
        var seen = new HashSet<String>();
        var collisions = new HashSet<String>();
        for (var p : hall)
            if (!seen.add(p.x() + ":" + p.y() + ":" + p.z())) collisions.add(p.x() + "," + p.y() + "," + p.z());
        // Lights DO sit in ceiling positions, by design: they replace ceiling
        // blocks. Assert that is the only collision, and that lights come last.
        var lights = at(hall, LobbyHall.Role.LIGHT);
        assertEquals(lights.size(), collisions.size(),
                "the only overlapping placements should be lights replacing ceiling");
        int lastCeiling = -1, firstLight = Integer.MAX_VALUE;
        for (int i = 0; i < hall.size(); i++) {
            if (hall.get(i).role() == LobbyHall.Role.CEILING) lastCeiling = i;
            if (hall.get(i).role() == LobbyHall.Role.LIGHT) firstLight = Math.min(firstLight, i);
        }
        assertTrue(firstLight > lastCeiling, "ceiling would overwrite the lights");
    }

    @Test void aHallTooSmallToStandInIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> LobbyHall.hall(1, 64, 8, 6));
        assertThrows(IllegalArgumentException.class, () -> LobbyHall.hall(16, 64, 2, 6));
        assertThrows(IllegalArgumentException.class, () -> LobbyHall.hall(16, 64, 8, 0));
    }
}
