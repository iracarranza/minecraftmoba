package com.minecraftmoba.plugin;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/** The six statuses' definitions, as the owner stated them. */
class StatusRulesTest {
    private static boolean refuses(Statuses.Kind k, StatusRules.Capability c) { return StatusRules.refused(k).contains(c); }

    @Test void aStunCannotMoveOrActButARootCanActButNotMove() {
        for (var c : new StatusRules.Capability[]{StatusRules.Capability.MOVE, StatusRules.Capability.ACT, StatusRules.Capability.ABILITIES})
            assertTrue(refuses(Statuses.Kind.STUNNED, c), c.name());
        assertTrue(refuses(Statuses.Kind.ROOTED, StatusRules.Capability.MOVE));
        assertFalse(refuses(Statuses.Kind.ROOTED, StatusRules.Capability.ACT));
        assertFalse(refuses(Statuses.Kind.ROOTED, StatusRules.Capability.ABILITIES));
        assertFalse(refuses(Statuses.Kind.ROOTED, StatusRules.Capability.ATTACK), "a Root can still attack");
    }

    @Test void aStunnedPlayerKeepsTheirCamera() {
        assertFalse(refuses(Statuses.Kind.STUNNED, StatusRules.Capability.LOOK), "the camera cannot be taken by a Stun (see Stun)");
    }

    @Test void luredAndFrightenedAreTheSameRuleSet() {
        assertEquals(StatusRules.refused(Statuses.Kind.LURED), StatusRules.refused(Statuses.Kind.FRIGHTENED));
        assertEquals(StatusRules.allowed(Statuses.Kind.LURED), StatusRules.allowed(Statuses.Kind.FRIGHTENED));
    }

    @Test void luredForbidsSprintCrouchAndAbilitiesAndTakesMovementAndCamera() {
        for (var c : new StatusRules.Capability[]{StatusRules.Capability.SPRINT, StatusRules.Capability.CROUCH,
                StatusRules.Capability.ABILITIES, StatusRules.Capability.MOVE, StatusRules.Capability.LOOK})
            assertTrue(refuses(Statuses.Kind.LURED, c), c.name());
    }

    @Test void luredMayJumpAndAttack() {
        assertTrue(StatusRules.allowed(Statuses.Kind.LURED).contains(StatusRules.Capability.JUMP));
        assertTrue(StatusRules.allowed(Statuses.Kind.LURED).contains(StatusRules.Capability.ATTACK));
    }

    @Test void blindedAndWeakenedRefuseNothingOfTheirOwn() {
        assertTrue(StatusRules.refused(Statuses.Kind.BLINDED).isEmpty());
        assertTrue(StatusRules.refused(Statuses.Kind.WEAKENED).isEmpty());
    }

    @Test void everyCapabilityIsEitherRefusedOrAllowedForEveryStatus() {
        for (var k : Statuses.Kind.values())
            assertEquals(StatusRules.Capability.values().length, StatusRules.refused(k).size() + StatusRules.allowed(k).size(), k.name());
    }

    @Test void onlyLuredAndFrightenedForceMovementAndNeedASource() {
        for (var k : Statuses.Kind.values()) {
            boolean forced = k == Statuses.Kind.LURED || k == Statuses.Kind.FRIGHTENED;
            assertEquals(forced, StatusRules.forcesMovement(k), k.name());
            assertEquals(forced, k.needsSource(), k.name());
        }
    }

    @Test void aChecklistFlagsOnlyRealDisagreements() {
        var agree = StatusRules.checklist(Statuses.Kind.ROOTED, c -> StatusRules.refused(Statuses.Kind.ROOTED).contains(c));
        assertTrue(StatusRules.mismatches(agree).isEmpty());
        var wrong = StatusRules.checklist(Statuses.Kind.ROOTED, c -> false);       // server refuses nothing
        assertEquals(3, StatusRules.mismatches(wrong).size(), "move, jump and sprint should have been refused");
        var unknown = StatusRules.checklist(Statuses.Kind.LURED, c -> null);       // nothing could be asked
        assertTrue(StatusRules.mismatches(unknown).isEmpty(), "a capability that cannot be asked is not a mismatch");
    }

    @Test void statusNamesParseInAnyCase() {
        assertEquals(Statuses.Kind.FRIGHTENED, Statuses.Kind.of("frightened"));
        assertEquals(Statuses.Kind.LURED, Statuses.Kind.of("Lured"));
        assertNull(Statuses.Kind.of("confused"));
        assertNull(Statuses.Kind.of(null));
    }

    @Test void theKindsCycleAndCloseTheLoop() {
        var k = Statuses.Kind.STUNNED;
        for (int i = 0; i < Statuses.Kind.values().length; i++) k = k.next();
        assertEquals(Statuses.Kind.STUNNED, k);
    }
}
