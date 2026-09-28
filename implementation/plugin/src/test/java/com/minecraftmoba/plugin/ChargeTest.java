package com.minecraftmoba.plugin;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Charging: how far along, and what that currently means.
 *
 * Arithmetic only, like CombatState and SelectionBeat -- the tick is passed
 * in, so a charge curve can be checked against a table without a server.
 */
class ChargeTest {

    private static final List<Charge.Band> BANDS = List.of(
            new Charge.Band(0.0, "#888888", "weak"),
            new Charge.Band(0.35, "#FFAA55", "armed"),
            new Charge.Band(0.8, "#FF5555", "pierce"));

    // ---- rate --------------------------------------------------------------

    /** Rate is the full duration, so "full" means the same thing everywhere. */
    @Test void differentAbilitiesChargeAtDifferentRates() {
        var quick = new Charge(0, 20, List.of());
        var slow = new Charge(0, 60, List.of());
        assertEquals(1.0, quick.progress(20));
        assertEquals(1.0 / 3, slow.progress(20), 1e-9);
        assertTrue(quick.complete(20));
        assertFalse(slow.complete(20));
    }

    /** Held past full simply reads full rather than overflowing. */
    @Test void aChargeHeldPastFullIsClamped() {
        var charge = new Charge(0, 20, List.of());
        assertEquals(1.0, charge.progress(500));
        assertEquals(100, Charge.percent(charge.progress(500)));
    }

    @Test void beforeItStartsItIsZero() {
        var charge = new Charge(100, 20, List.of());
        assertEquals(0, charge.progress(50));
        assertEquals(0, charge.progress(100));
    }

    // ---- bands -------------------------------------------------------------

    /** Breakpoints are per ability, and the band is what the colour comes from. */
    @Test void theBandChangesAtItsOwnBreakpoint() {
        var charge = new Charge(0, 100, BANDS);
        assertEquals("weak", charge.band(0.0).label());
        assertEquals("weak", charge.band(0.34).label());
        assertEquals("armed", charge.band(0.35).label(), "the band begins AT its threshold");
        assertEquals("armed", charge.band(0.79).label());
        assertEquals("pierce", charge.band(0.8).label());
        assertEquals("pierce", charge.band(1.0).label());
    }

    @Test void noBandsIsLegalAndReadsAsNull() {
        assertNull(new Charge(0, 20, List.of()).band(0.5));
        assertNull(new Charge(0, 20, null).band(0.5));
    }

    /**
     * Bands must ascend and start at zero.
     *
     * Unsorted, band() would silently return whichever happened to be last
     * below the fraction -- a wrong colour rather than an error, which is the
     * failure mode this HUD has spent a night removing elsewhere.
     */
    @Test void unorderedBandsAreRefused() {
        assertThrows(IllegalArgumentException.class, () -> new Charge(0, 20, List.of(
                new Charge.Band(0.0, "#FFFFFF", "a"),
                new Charge.Band(0.8, "#FFFFFF", "c"),
                new Charge.Band(0.35, "#FFFFFF", "b"))));
        assertThrows(IllegalArgumentException.class, () -> new Charge(0, 20, List.of(
                new Charge.Band(0.2, "#FFFFFF", "late"))), "a fresh charge would have no colour");
    }

    @Test void aChargeThatTakesNoTimeIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> new Charge(0, 0, List.of()));
        assertThrows(IllegalArgumentException.class, () -> new Charge(0, -1, List.of()));
    }

    // ---- the readout -------------------------------------------------------

    /**
     * Percent FLOORS, so the meter never reads 100% before the charge is done.
     *
     * That is the one reading a player would act on and be wrong about --
     * releasing on a rounded 100 that was actually 99.5 and getting the
     * lower band.
     */
    @Test void percentNeverRoundsUpToFull() {
        assertEquals(99, Charge.percent(0.999));
        assertEquals(100, Charge.percent(1.0));
        assertEquals(0, Charge.percent(0.009));
        assertEquals(0, Charge.percent(-5), "and cannot go negative");
        assertEquals(100, Charge.percent(5));
    }

    /** The meter is drawn at the same width whatever the charge. */
    @Test void theMeterFillsProportionally() {
        var style = new VitalsBar.Style(48, 73);
        assertEquals(0, style.level(0, 1));
        assertEquals(24, style.level(0.5, 1));
        assertEquals(48, style.level(1, 1));
        assertEquals(47, style.level(0.999, 1), "short is never full, here too");
    }

    /** The meter's height must be one the pack actually carries. */
    @Test void theMeterHeightExistsInTheBarFamily() {
        assertTrue(VitalsBar.ASCENTS.contains(73),
                "the subtext line needs unit glyphs at its own ascent");
    }

    // ---- target-conditional activation -------------------------------------

    /**
     * The context can say it was aimed at nothing, which is a real answer.
     *
     * An activation from air, from Swap Offhand, or from a damage event has no
     * block -- and an ability that states "used ON block a" needs to
     * distinguish that from "aimed at something I do not want", rather than
     * silently acting on whatever a fresh raycast happens to find.
     */
    @Test void anActivationCanReportThatItHitNoBlock() {
        var context = new Ability.AbilityContext(null, null, null);
        assertFalse(context.onBlock());
        assertNull(context.block());
        assertNull(context.face());
    }

    /** The five-argument form is still air, so existing abilities are unchanged. */
    @Test void theOlderContextFormsStillMeanNoBlock() {
        var context = new Ability.AbilityContext(null, null, null, null, null);
        assertFalse(context.onBlock());
    }
}
