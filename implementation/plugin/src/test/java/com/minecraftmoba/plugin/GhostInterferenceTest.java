package com.minecraftmoba.plugin;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class GhostInterferenceTest {
    @Test void startsScripting() {
        var g = new GhostInterference();
        assertTrue(g.scripting()); assertNull(g.cause()); assertEquals(0, g.times());
    }

    @Test void anyInterferenceStandsItDown() {
        for (var cause : GhostInterference.Cause.values()) {
            var g = new GhostInterference();
            assertTrue(g.interfere(cause, 100));
            assertFalse(g.scripting()); assertEquals(cause, g.cause()); assertEquals(100, g.since());
        }
    }

    @Test void furtherHitsWhileInterferedChangeNothingAndAreNotReportedAsNew() {
        var g = new GhostInterference();
        assertTrue(g.interfere(GhostInterference.Cause.STRUCK, 10));
        assertFalse(g.interfere(GhostInterference.Cause.STUNNED, 20));
        assertEquals(GhostInterference.Cause.STRUCK, g.cause()); assertEquals(10, g.since()); assertEquals(1, g.times());
    }

    @Test void resumesOnlyOnceOutOfCombatAndNotHeld() {
        var g = new GhostInterference();
        g.interfere(GhostInterference.Cause.STRUCK, 0);
        assertFalse(g.tryResume(true, false));      // still fighting
        assertFalse(g.tryResume(false, true));      // stunned or rooted still
        assertFalse(g.scripting());
        assertTrue(g.tryResume(false, false));
        assertTrue(g.scripting()); assertNull(g.cause());
        assertFalse(g.tryResume(false, false));     // nothing to resume
    }

    @Test void canBeInterferedAgainAfterResuming() {
        var g = new GhostInterference();
        g.interfere(GhostInterference.Cause.STRUCK, 0); g.tryResume(false, false);
        assertTrue(g.interfere(GhostInterference.Cause.DISPLACED, 50));
        assertEquals(2, g.times());
    }

    @Test void manualResetForcesResumeEvenInCombat() {
        var g = new GhostInterference();
        g.interfere(GhostInterference.Cause.ROOTED, 0);
        assertTrue(g.force()); assertTrue(g.scripting());
        assertFalse(g.force());
    }

    @Test void anIgnoringGhostIsNeverInterferedWith() {
        var g = new GhostInterference();
        g.ignoring(true);
        assertFalse(g.interfere(GhostInterference.Cause.STRUCK, 0));
        assertTrue(g.scripting()); assertEquals(0, g.times());
    }

    @Test void switchingToIgnoringResumesAStoodDownGhost() {
        var g = new GhostInterference();
        g.interfere(GhostInterference.Cause.STRUCK, 0);
        g.ignoring(true);
        assertTrue(g.scripting());
    }

    @Test void motionIsExternalOnlyWhenItsOwnAbilitiesDoNotExplainIt() {
        double pushed = 0.5;
        assertTrue(GhostInterference.displaced(pushed, false, 1000));
        assertFalse(GhostInterference.displaced(pushed, true, 1000));    // an ability of its own is running
        assertFalse(GhostInterference.displaced(pushed, false, 10));     // it just cast and is mid-flight
        assertFalse(GhostInterference.displaced(0.01, false, 1000));     // settling, not pushed
    }
}
