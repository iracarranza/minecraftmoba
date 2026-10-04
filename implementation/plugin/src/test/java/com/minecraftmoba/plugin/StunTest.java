package com.minecraftmoba.plugin;

import org.bukkit.Location;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * How two Stuns combine, which is the only part of Stun that is arithmetic
 * rather than event plumbing -- and the part where a plausible wrong answer
 * would be invisible until a long Stun mattered.
 */
class StunTest {

    @Test void theLaterExpiryWins() {
        assertEquals(200, Stun.extend(200, 50),
                "a short Stun must not shorten a long one already running");
        assertEquals(200, Stun.extend(50, 200),
                "a long Stun applied over a short one extends it");
    }

    @Test void stunsDoNotStack() {
        // Two 40-tick Stuns applied at tick 0 both expire at 40, not 80. A
        // chain of ordinary Stuns is therefore a chain of ordinary Stuns, not
        // a lockout nobody balanced.
        assertEquals(40, Stun.extend(40, 40));
    }

    @Test void equalExpiriesAreStable() {
        assertEquals(123, Stun.extend(123, 123));
    }

    @Test void anExpiredStunIsOverwrittenByANewOne() {
        // "Later wins" is about absolute expiry ticks, so a Stun applied now
        // over one that expired long ago simply wins on its own merits.
        assertEquals(500, Stun.extend(10, 500));
    }

    // ---- movement denial ---------------------------------------------------

    private static Location at(double x, double y, double z, float yaw, float pitch) {
        return new Location(null, x, y, z, yaw, pitch);
    }

    @Test void aLookOnlyChangePassesThrough() {
        // Minecraft sends a turn and a step in the same event. Holding a pure
        // turn would re-assert position every time the player looked around,
        // fighting the client for nothing.
        assertNull(Stun.hold(at(10, 64, 10, 0, 0), at(10, 64, 10, 90, -30)),
                "turning the head is not moving");
    }

    @Test void aStepIsRewoundToTheOrigin() {
        Location held = Stun.hold(at(10, 64, 10, 0, 0), at(12, 64, 10, 0, 0));
        assertNotNull(held);
        assertEquals(10, held.getX(), 1e-9);
        assertEquals(64, held.getY(), 1e-9);
        assertEquals(10, held.getZ(), 1e-9);
    }

    @Test void theHeldLocationKeepsTheNewLook() {
        // Taking the origin's look as well would snap the camera back on every
        // movement attempt. A Stun here deliberately does not take the camera,
        // and a Root never did.
        Location held = Stun.hold(at(10, 64, 10, 0, 0), at(12, 64, 10, 135, -20));
        assertEquals(135f, held.getYaw(), 1e-6);
        assertEquals(-20f, held.getPitch(), 1e-6);
    }

    @Test void everyAxisCountsAsMovement() {
        assertNotNull(Stun.hold(at(10, 64, 10, 0, 0), at(10.01, 64, 10, 0, 0)), "x");
        assertNotNull(Stun.hold(at(10, 64, 10, 0, 0), at(10, 64.01, 10, 0, 0)), "y -- a jump is movement");
        assertNotNull(Stun.hold(at(10, 64, 10, 0, 0), at(10, 64, 10.01, 0, 0)), "z");
    }

    @Test void theOriginIsNotMutated() {
        // The handler passes the event's own From; returning it rather than a
        // copy would let a later consumer see a rewritten origin.
        Location from = at(10, 64, 10, 0, 0);
        Location held = Stun.hold(from, at(12, 64, 10, 90, 0));
        assertNotSame(from, held);
        assertEquals(0f, from.getYaw(), 1e-6, "the caller's From keeps its own look");
    }

    @Test void nullsAreRefusedRatherThanThrowing() {
        assertNull(Stun.hold(null, at(1, 1, 1, 0, 0)));
        assertNull(Stun.hold(at(1, 1, 1, 0, 0), null));
    }
}
