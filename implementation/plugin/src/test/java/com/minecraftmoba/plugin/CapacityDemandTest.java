package com.minecraftmoba.plugin;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Whether a team can keep up with its own progression.
 *
 * The shape under examination: a capacity grant raises the attention bill for
 * operating at the new ceiling, so grants arriving faster than a team can fill
 * them make utilisation FALL as the player levels -- while the progression
 * system reports nothing but success.
 *
 * Timings here are claims nobody has measured. The structure is what is being
 * checked.
 */
class CapacityDemandTest {

    /** config.yml: Growth lands every three levels from 3 to 30. */
    private static final List<Integer> GROWTH = List.of(3, 6, 9, 12, 15, 18, 21, 24, 27, 30);

    /** config.yml's fallback slot curve. */
    private static final List<Double> DEFAULT_SLOTS =
            List.of(6.0, 9.0, 9.0, 12.0, 12.0, 15.0, 18.0, 18.0, 21.0, 21.0, 24.0);

    /** Toolbox: front-loaded and far steeper, which is the interesting contrast. */
    private static final List<Double> TOOLBOX_SLOTS =
            List.of(6.0, 12.0, 18.0, 21.0, 24.0, 27.0, 36.0, 36.0, 36.0, 36.0, 36.0);

    @Test void aFlatStepIsNotAGrantAndCostsNothing() {
        // The default curve holds flat at several Growths -- the config calls
        // that spending the budget elsewhere. Those must not appear as demand.
        var grants = CapacityDemand.grants(GROWTH, DEFAULT_SLOTS);
        assertEquals(6, grants.size(), "ten Growths, four of them flat");
        for (var g : grants) assertTrue(g.increase() > 0);
    }

    @Test void theEarlyCommitmentIsSmallWhichIsWhyItFeelsFine() {
        var grants = CapacityDemand.grants(GROWTH, DEFAULT_SLOTS);
        assertEquals(3, grants.get(0).increase(), 1e-9, "Lv3 asks for three more slots' worth");
        // A small ask at a point where a team has least to give is the right
        // shape, and it is also why the later asks go unnoticed.
        assertTrue(grants.get(0).increase() <= grants.get(grants.size() - 1).increase());
    }

    @Test void aSteepCurveAsksForMuchMoreAttentionThanAFlatOne() {
        double flat = CapacityDemand.playersToKeepUp(
                CapacityDemand.grants(GROWTH, DEFAULT_SLOTS), 2.0, 20);
        double steep = CapacityDemand.playersToKeepUp(
                CapacityDemand.grants(GROWTH, TOOLBOX_SLOTS), 2.0, 20);
        assertTrue(steep > flat,
                "Toolbox's +9 at one step costs more to fill than any default step: "
                        + steep + " vs " + flat);
    }

    @Test void aTeamThatKeepsUpCarriesNoBacklogAndStaysFullyUtilised() {
        var grants = CapacityDemand.grants(GROWTH, DEFAULT_SLOTS);
        var audit = CapacityDemand.audit(grants, 2.0, 1.0, 20);
        for (var v : audit) {
            assertTrue(v.keepsUp(), "level " + v.level() + " took " + v.minutesToFill() + " min");
            assertEquals(0.0, v.backlogAfter(), 1e-9);
            assertEquals(1.0, v.utilisation(), 1e-9);
        }
    }

    @Test void anUnderstaffedTeamFallsFurtherBehindRatherThanEquallyBehind() {
        // The compounding the carried backlog exists to show. Resetting per
        // grant would report each one as a fresh, survivable problem.
        var grants = CapacityDemand.grants(GROWTH, TOOLBOX_SLOTS);
        var audit = CapacityDemand.audit(grants, 6.0, 0.25, 15);

        double previous = -1;
        for (var v : audit) {
            assertTrue(v.backlogAfter() >= previous,
                    "backlog must never shrink for a team that never keeps up");
            previous = v.backlogAfter();
        }
        assertTrue(audit.get(audit.size() - 1).backlogAfter() > audit.get(0).backlogAfter() * 2,
                "the shortfall compounds rather than plateauing");
    }

    @Test void utilisationFallsAsCapacityOutrunsAttention() {
        // The finding this instrument exists for: granted more, using
        // proportionally less, with nothing in the progression system saying so.
        var grants = CapacityDemand.grants(GROWTH, TOOLBOX_SLOTS);
        var audit = CapacityDemand.audit(grants, 6.0, 0.25, 15);
        assertTrue(audit.get(audit.size() - 1).utilisation() < audit.get(0).utilisation(),
                "utilisation should decline: " + audit.get(0).utilisation()
                        + " -> " + audit.get(audit.size() - 1).utilisation());
    }

    @Test void theLastGrantHasNoDeadlineBecauseNothingSupersedesIt() {
        var grants = CapacityDemand.grants(GROWTH, DEFAULT_SLOTS);
        var audit = CapacityDemand.audit(grants, 100.0, 0.1, 5);
        var last = audit.get(audit.size() - 1);
        assertTrue(Double.isInfinite(last.minutesUntilNext()));
        assertTrue(last.keepsUp(), "the final grant can always be filled eventually");
    }

    @Test void noAttentionMeansTheCapacityIsNeverUsedAndTheModelSaysSo() {
        var grants = CapacityDemand.grants(GROWTH, DEFAULT_SLOTS);
        var audit = CapacityDemand.audit(grants, 2.0, 0, 20);
        assertTrue(Double.isInfinite(audit.get(0).minutesToFill()));
        assertFalse(audit.get(0).keepsUp());
        assertTrue(audit.get(audit.size() - 1).backlogAfter() > 0,
                "an unstaffed infrastructure accrues its whole curve as debt");
    }

    @Test void theSameArithmeticServesEveryInfrastructureType() {
        // Constructs, Development Zones, Routes and Supply Lines differ in unit
        // and not in shape. A Route curve measured in connections behaves
        // exactly as a slot curve measured in slots.
        var routes = List.of(0.0, 1.0, 2.0, 2.0, 3.0, 4.0);
        var grants = CapacityDemand.grants(List.of(6, 12, 18, 24, 30), routes);
        assertEquals(4, grants.size());
        assertEquals(6, grants.get(0).level());

        double players = CapacityDemand.playersToKeepUp(grants, 12.0, 30);
        assertEquals(0.4, players, 1e-9,
                "one Route per 12 attention-minutes, 30 minutes apart, is 0.4 of a player");
    }

    @Test void keepingUpIsAboutTheWorstStepNotTheAverage() {
        // A curve that is gentle except for one jump is as demanding as its
        // jump, because the deadline is per grant.
        var spiky = List.of(0.0, 1.0, 2.0, 12.0, 13.0);
        var grants = CapacityDemand.grants(List.of(3, 6, 9, 12), spiky);
        assertEquals(10.0 * 2.0 / 20.0, CapacityDemand.playersToKeepUp(grants, 2.0, 20), 1e-9,
                "the +10 step sets the staffing, not the +1s around it");
    }
}
