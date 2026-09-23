package com.minecraftmoba.plugin;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Loading a match and resetting one are the same mechanism (ALPHA-D2).
 *
 * They were not. `restore()` unloaded, copied the template and loaded, but
 * `load()` skipped the copy whenever an instance directory already existed:
 *
 *     if (!Files.isDirectory(instancePath())) materialize();
 *
 * That reads as an optimization and is a correctness bug. After a server
 * restart, `match open` found last session's directory, called it materialized,
 * and handed the player back a world they had already mined -- while every
 * piece of match state around it had been cleared. The terrain agreed with
 * nothing: hay bales missing, trees missing, progression at zero.
 */
class WorldInstanceTest {

    @Test void anUnloadedInstanceOnDiskIsAPreviousMatchAndIsReplaced() {
        // The case that was broken. Presence on disk must NOT suppress the copy.
        assertTrue(WorldInstance.mustMaterialize(false, true),
                "a leftover directory is the last match, not a world to resume");
    }

    @Test void anAbsentInstanceIsMaterialized() {
        assertTrue(WorldInstance.mustMaterialize(false, false));
    }

    @Test void aLoadedWorldIsNeverReplacedUnderneathItself() {
        // materialize() refuses while a world is loaded, and a mid-match copy
        // would be a far worse failure than a stale one.
        assertFalse(WorldInstance.mustMaterialize(true, true));
        assertFalse(WorldInstance.mustMaterialize(true, false));
    }

    @Test void presenceOnDiskIsNotConsultedAtAll() {
        // Stated directly, because the bug was precisely that it was.
        for (boolean loaded : new boolean[]{true, false})
            assertEquals(WorldInstance.mustMaterialize(loaded, true),
                    WorldInstance.mustMaterialize(loaded, false),
                    "the decision must not depend on what is on disk");
    }
}
