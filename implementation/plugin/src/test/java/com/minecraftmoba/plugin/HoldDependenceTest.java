package com.minecraftmoba.plugin;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Which abilities a cast-mode setting may reshape, and which it may not.
 *
 * The distinction is NOT the ability's form. A projectile that is merely
 * aimed, and a persistent projectile that behaves like a beam, are both
 * ordinary: they commit, and what happens afterwards is theirs. Either can be
 * cast all three ways.
 *
 * It is whether the ability's OUTPUT depends on how long the input is held.
 */
class HoldDependenceTest {

    /**
     * Quick cannot measure a hold, so it would resolve such an ability at zero
     * every time -- the minimum, silently. That is an ability broken by a
     * preference rather than a preference honoured.
     */
    @Test void quickIsUpgradedForAHoldDependentAbility() {
        assertEquals(CastMode.HOLD, CastMode.QUICK.effectiveFor(true));
        assertEquals(CastMode.QUICK, CastMode.QUICK.effectiveFor(false));
    }

    /**
     * Upgraded, not refused.
     *
     * Refusing would leave a player unable to cast because of a setting, and a
     * tap under Hold already behaves as Quick does -- so the upgrade costs the
     * quick-caster almost nothing and costs the ability nothing at all.
     */
    @Test void theUpgradeIsToTheNearestWorkingModeRatherThanARefusal() {
        assertNotNull(CastMode.QUICK.effectiveFor(true));
        assertTrue(CastMode.QUICK.effectiveFor(true).previews(),
                "and the upgraded mode is one that can measure a hold");
    }

    /** The other two modes are untouched: both already involve holding or waiting. */
    @Test void holdAndDoubleSurviveHoldDependenceUnchanged() {
        assertEquals(CastMode.HOLD, CastMode.HOLD.effectiveFor(true));
        assertEquals(CastMode.DOUBLE, CastMode.DOUBLE.effectiveFor(true),
                "Double stays available -- but its second press must itself be holdable");
    }

    /** An ordinary ability is never reshaped, whatever the player prefers. */
    @Test void anOrdinaryAbilityKeepsWhicheverModeThePlayerChose() {
        for (CastMode mode : CastMode.values())
            assertEquals(mode, mode.effectiveFor(false));
    }

    /**
     * Hold-dependence is declared PER ACTIVATION, with the branch in hand.
     *
     * A projectile branch is not hold-dependent; its widening-beam branch is.
     * So the same ability id answers differently depending on which branch is
     * selected, exactly as its preview does.
     */
    @Test void aBranchMayChangeWhetherAnAbilityIsHoldDependent() {
        class Forked implements Ability {
            final String branch;
            Forked(String branch) { this.branch = branch; }
            public String id() { return "arc"; }
            public String displayName() { return "Arc"; }
            public long cooldownTicks() { return 0; }
            public boolean execute(org.bukkit.entity.Player p, AbilityContext c) { return true; }
            @Override public boolean holdDependent(org.bukkit.entity.Player p, AbilityContext c) {
                return "widening_beam".equals(branch);
            }
        }
        var context = new Ability.AbilityContext(null, null, null);
        var player = org.mockito.Mockito.mock(org.bukkit.entity.Player.class);

        assertFalse(new Forked("projectile").holdDependent(player, context));
        assertTrue(new Forked("widening_beam").holdDependent(player, context));
    }

    /** Nothing is hold-dependent unless it says so. */
    @Test void theDefaultIsOrdinary() {
        Ability plain = new Ability() {
            public String id() { return "plain"; }
            public String displayName() { return "Plain"; }
            public long cooldownTicks() { return 0; }
            public boolean execute(org.bukkit.entity.Player p, AbilityContext c) { return true; }
        };
        assertFalse(plain.holdDependent(
                org.mockito.Mockito.mock(org.bukkit.entity.Player.class),
                new Ability.AbilityContext(null, null, null)));
    }

    /**
     * A sustain is ended by the same quiet window that ends an aim.
     *
     * It is the same question -- is the button still down -- so it gets the
     * same answer and the same per-input windows, rather than a second
     * heuristic that could disagree with the first.
     */
    @Test void aSustainEndsOnTheSameSilenceAnAimDoes() {
        var held = AimState.begin("beam", AbilityInputs.Input.LEFT_CLICK, CastMode.HOLD, 0);
        assertEquals(AimState.Decision.HOLD, held.onTick(2, 2, 200));
        assertEquals(AimState.Decision.FIRE, held.onTick(3, 2, 200),
                "for a sustain, this decision means 'let go' rather than 'commit'");
    }
}
