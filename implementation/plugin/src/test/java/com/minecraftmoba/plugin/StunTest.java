package com.minecraftmoba.plugin;

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
}
