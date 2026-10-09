package com.minecraftmoba.plugin;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.minecraftmoba.plugin.SifthSense.At;
import static org.junit.jupiter.api.Assertions.*;

/**
 * What one break reaches, and -- the part that matters -- what a cap discards.
 *
 * A desert dune is thousands of connected sand blocks, so the volume cap is
 * reached constantly rather than rarely. Which blocks it drops is the ability.
 */
class SifthSenseTest {

    private static final At ORIGIN = new At(0, 64, 0);

    /** A world made of the positions named, and nothing else. */
    private static Set<At> world(At... positions) { return new HashSet<>(List.of(positions)); }

    /** A solid run of siftable material along +X, starting at the origin. */
    private static Set<At> run(int length) {
        var out = new HashSet<At>();
        for (int x = 0; x < length; x++) out.add(new At(x, 64, 0));
        return out;
    }

    // ---- what connects ------------------------------------------------------

    @Test void theBrokenBlockIsNotReturnedBecauseItIsAlreadyGone() {
        var found = SifthSense.reach(ORIGIN, run(3)::contains);
        assertFalse(found.contains(ORIGIN), "breaking it is what already happened");
        assertEquals(2, found.size());
    }

    @Test void theFillStopsAtMaterialItDoesNotActOn() {
        // Sand, sand, STONE, sand. The far sand is connected only through the
        // stone, so it is not connected at all.
        var sand = world(ORIGIN, new At(1, 64, 0), new At(3, 64, 0));
        var found = SifthSense.reach(ORIGIN, sand::contains);
        assertEquals(List.of(new At(1, 64, 0)), found);
    }

    @Test void materialConnectsThroughFacesAndNotThroughCorners() {
        // A diagonal touch is not a connection, or a sift would jump between
        // two dunes that merely graze each other.
        var diagonal = world(ORIGIN, new At(1, 65, 0));
        assertTrue(SifthSense.reach(ORIGIN, diagonal::contains).isEmpty());
    }

    @Test void breakingSomethingThatIsNotSiftableReachesNothing() {
        assertTrue(SifthSense.reach(ORIGIN, at -> false).isEmpty());
    }

    // ---- the bounds ---------------------------------------------------------

    @Test void materialOutsideTheRadiusIsLeftEvenThoughItIsConnected() {
        var found = SifthSense.reach(ORIGIN, run(100)::contains, 3, 1000);
        assertEquals(3, found.size(), "the origin plus three steps: " + found);
        assertEquals(new At(3, 64, 0), found.get(found.size() - 1));
    }

    @Test void aVolumeCapDiscardsTheFarEdgeAndNeverThePocket() {
        // The claim breadth-first exists to make. A cap must leave a contiguous
        // pocket around the break, not a tendril reaching across the dune.
        var found = SifthSense.reach(ORIGIN, run(100)::contains, 50, 5);
        assertEquals(5, found.size());
        for (int i = 0; i < 5; i++)
            assertEquals(new At(i + 1, 64, 0), found.get(i), "contiguous from the break outward");
    }

    @Test void theReachGrowsOutwardInEveryDirectionAtOnce() {
        // A depth-first fill would spend the whole cap going one way. With a
        // break in the middle of a solid mass, both sides must be represented.
        var mass = new HashSet<At>();
        for (int x = -10; x <= 10; x++) mass.add(new At(x, 64, 0));

        var found = SifthSense.reach(ORIGIN, mass::contains, 10, 6);
        assertEquals(6, found.size());
        assertTrue(found.stream().anyMatch(a -> a.x() < 0), "reached -X: " + found);
        assertTrue(found.stream().anyMatch(a -> a.x() > 0), "reached +X: " + found);
    }

    @Test void aZeroVolumeSiftsNothingRatherThanEverything() {
        assertTrue(SifthSense.reach(ORIGIN, run(10)::contains, 5, 0).isEmpty());
    }

    @Test void aZeroRadiusReachesNothingBeyondTheBreakItself() {
        assertTrue(SifthSense.reach(ORIGIN, run(10)::contains, 0, 64).isEmpty());
    }

    @Test void noBlockIsReturnedTwiceEvenInARing() {
        // A loop of material revisits positions; a fill that did not remember
        // them would both duplicate drops and spin.
        var ring = new HashSet<At>();
        for (int x = 0; x <= 3; x++) { ring.add(new At(x, 64, 0)); ring.add(new At(x, 64, 3)); }
        for (int z = 0; z <= 3; z++) { ring.add(new At(0, 64, z)); ring.add(new At(3, 64, z)); }

        var found = SifthSense.reach(ORIGIN, ring::contains, 5, 1000);
        assertEquals(new HashSet<>(found).size(), found.size(), "no duplicates: " + found);
        assertEquals(ring.size() - 1, found.size(), "the whole ring except the break");
    }

    // ---- the fixtures are declared as fixtures -------------------------------

    @Test void theDefaultsAreBoundedEnoughToRunOnATick() {
        // The cap exists because a dune is effectively unbounded, and an
        // uncapped flood fill is a server stall rather than a strong ability.
        assertTrue(SifthSense.DEFAULT_VOLUME > 0 && SifthSense.DEFAULT_VOLUME <= 256);
        int cube = (2 * SifthSense.DEFAULT_RADIUS + 1);
        assertTrue(SifthSense.DEFAULT_VOLUME < cube * cube * cube,
                "the volume cap must bite before the radius does, or it is decoration");
    }
}
