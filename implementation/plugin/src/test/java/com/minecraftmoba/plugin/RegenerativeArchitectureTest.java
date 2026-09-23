package com.minecraftmoba.plugin;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The architecture: Region -> current-terrain eligibility -> locus -> finite
 * manifestation -> resolution -> recovery -> fresh query -> next manifestation.
 *
 * These test the boundaries rather than the fixtures. The predicates and weights
 * are Alpha values and are expected to change; what must not change is that a
 * region is not a locus, that the query runs against the world as it currently
 * is, and that "nowhere is eligible" is an answer the system states rather than
 * one it works around.
 */
class RegenerativeArchitectureTest {

    private static final Eligibility.Rules RULES = new Eligibility.Rules(
            2, Eligibility.NATURAL_GROUND, 4, 12.0, 16.0, true);

    private static OpportunityRegion meadow() {
        return new OpportunityRegion(List.of(new OpportunityRegion.Cell(0, 0, 63, 63)));
    }

    private static FakeTerrain flatMeadow() {
        return new FakeTerrain().grass(0, 0, 63, 63, 64);
    }

    // ---- region vs locus -------------------------------------------------

    @Test void aRegionIsNotALocus() {
        // The collapse this whole model exists to undo: origin+radius made the
        // region, the eligible area and the site one number.
        var region = meadow();
        var loci = Eligibility.loci(region, flatMeadow(), RULES);
        assertTrue(loci.size() > 100, "a coarse region offers many loci, not one point");
        assertTrue(region.contains(loci.get(0).x(), loci.get(0).z()));
        assertTrue(region.area() > loci.size(), "the region is larger than the sampled loci");
    }

    @Test void aRegionMaySpanSeveralAuthoredCells() {
        var region = new OpportunityRegion(List.of(
                new OpportunityRegion.Cell(0, 0, 15, 15),
                new OpportunityRegion.Cell(16, 0, 31, 15)));
        assertTrue(region.contains(2, 2));
        assertTrue(region.contains(20, 2), "the ecology is not required to be one cell");
        assertFalse(region.contains(40, 2));
    }

    // ---- the query runs against the CURRENT world ------------------------

    @Test void aLaterGenerationCanManifestAtADifferentLocus() {
        // Locus(t) == Locus(t+1) is explicitly not required. The recurring
        // thing is the opportunity, not a pile at fixed coordinates.
        var world = flatMeadow();
        var loci = Eligibility.loci(meadow(), world, RULES);
        var first = Eligibility.select(loci, RULES, null, world, new Random(1));
        assertNotNull(first);
        var second = Eligibility.select(loci, RULES, first, world, new Random(2));
        assertNotNull(second);
        assertTrue(second.distanceTo(first) >= RULES.minDisplacement(),
                "a new manifestation is displaced from the last, or the region is a camp");
    }

    @Test void buildingOverTheEcologyMovesTheManifestationWithinIt() {
        // Development changing where the wild thing appears, without changing
        // the Opportunity at all. This is the property the query exists for.
        var pristine = flatMeadow();
        int before = Eligibility.loci(meadow(), pristine, RULES).size();

        var built = flatMeadow().built(0, 0, 31, 63, Material.STONE_BRICKS);
        var after = Eligibility.loci(meadow(), built, RULES);
        assertTrue(after.size() < before, "player work removes loci");
        for (var l : after)
            assertTrue(l.x() > 31, "every surviving locus is outside the built area");
    }

    @Test void obstructedAndSubmergedGroundIsNotEligible() {
        var drowned = flatMeadow().water(0, 0, 63, 31, 64);
        for (var l : Eligibility.loci(meadow(), drowned, RULES))
            assertTrue(l.z() > 31, "nothing manifests in the lake");

        var roofed = flatMeadow().roofed(0, 0, 63, 31, Material.STONE);
        for (var l : Eligibility.loci(meadow(), roofed, RULES))
            assertTrue(l.z() > 31, "nothing manifests where there is no headroom");
    }

    @Test void aLocusIsNotChosenOnTopOfAPlayer() {
        // An opportunity should be discovered, not materialise around someone.
        var world = flatMeadow().playerAt(32, 32, 64);
        var loci = Eligibility.loci(meadow(), world, RULES);
        var chosen = Eligibility.select(loci, RULES, null, world, new Random(7));
        assertNotNull(chosen);
        assertTrue(Math.hypot(chosen.x() - 32, chosen.z() - 32) >= RULES.playerExclusion());
    }

    // ---- the no-eligible-locus case --------------------------------------

    @Test void noEligibleLocusYieldsNothingRatherThanAFallback() {
        var covered = flatMeadow().built(0, 0, 63, 63, Material.STONE_BRICKS);
        var loci = Eligibility.loci(meadow(), covered, RULES);
        assertTrue(loci.isEmpty(), "an ecology entirely under player work offers nothing");
        assertNull(Eligibility.select(loci, RULES, null, covered, new Random()),
                "no authored origin, no forced spawn, no widened region");
    }

    @Test void theOpportunityStaysReadyAndSaysSo() {
        var o = Opportunity.fresh();
        assertTrue(o.readyToManifest());
        o.noEligibleLocus();
        o.noEligibleLocus();
        assertEquals(Opportunity.State.READY_AWAITING_LOCUS, o.state(),
                "ready but unmanifested is a state, not an error");
        assertEquals(2, o.blockedAttempts(), "the refusal is measurable");
    }

    // ---- lifecycle -------------------------------------------------------

    private static final Recovery.Rates RATES = new Recovery.Rates(12000, 0.33, 0.73);

    @Test void partialHarvestNeitherRefillsNorBeginsRecovery() {
        var o = Opportunity.fresh();
        o.manifested(new Eligibility.Locus(10, 65, 10), 5);
        o.memberRemoved();
        o.memberRemoved();
        assertEquals(Opportunity.State.MANIFESTED, o.state(), "still the current manifestation");
        assertEquals(3, o.remaining(), "three sheep, not a countdown and not five again");
        o.tickRecovery(100_000, false, RATES);
        assertEquals(0.0, o.recoveryProgress(), "an unresolved manifestation does not recover");
    }

    @Test void anIgnoredManifestationKeepsItsStockIndefinitely() {
        // Turnover pressure, not decay: leaving it alone costs tempo, not stock.
        var o = Opportunity.fresh();
        o.manifested(new Eligibility.Locus(10, 65, 10), 5);
        o.tickRecovery(1_000_000, false, RATES);
        assertEquals(5, o.remaining());
        assertEquals(Opportunity.State.MANIFESTED, o.state());
    }

    @Test void fullResolutionBeginsRecovery() {
        var o = Opportunity.fresh();
        o.manifested(new Eligibility.Locus(10, 65, 10), 2);
        o.memberRemoved();
        o.memberRemoved();
        assertEquals(Opportunity.State.RECOVERING, o.state());
        assertNull(o.locus(), "the locus belonged to the manifestation, not the opportunity");
    }

    @Test void recoveryCompletionDoesNotItselfManifest() {
        var o = Opportunity.fresh();
        o.manifested(new Eligibility.Locus(10, 65, 10), 1);
        o.memberRemoved();
        o.tickRecovery((long) RATES.dayTicks() + 1, false, RATES);
        assertEquals(Opportunity.State.READY_AWAITING_LOCUS, o.state(),
                "recovery authorizes an attempt; the world decides whether it succeeds");
    }

    @Test void noMissedGenerationBacklogAccumulates() {
        var o = Opportunity.fresh();
        o.manifested(new Eligibility.Locus(10, 65, 10), 1);
        o.memberRemoved();
        o.tickRecovery(1_000_000, false, RATES);          // a very long wait
        assertEquals(1.0, o.recoveryProgress(), 1e-9, "progress caps at one recovery");
        o.manifested(new Eligibility.Locus(40, 65, 40), 5);
        assertEquals(5, o.remaining(), "one manifestation, not four banked ones");
    }

    // ---- temporal model --------------------------------------------------

    @Test void recoveryIsExpressedRelativeToTheTemporalPhase() {
        // The phase length has already changed once (6m -> vanilla 10m). Nothing
        // here may be written in absolute minutes.
        var shortPhase = new Recovery.Rates(7200, 0.33, 0.73);
        var longPhase = new Recovery.Rates(12000, 0.33, 0.73);
        assertEquals(7200 * 0.33, shortPhase.dayTicks(), 1e-9);
        assertEquals(12000 * 0.33, longPhase.dayTicks(), 1e-9);
        assertTrue(Recovery.ratePerTick(shortPhase, false) > Recovery.ratePerTick(longPhase, false),
                "the same fraction of a shorter phase recovers faster in absolute time");
    }

    @Test void nightRecoversMoreSlowlyThanDay() {
        assertTrue(Recovery.ratePerTick(RATES, true) < Recovery.ratePerTick(RATES, false));
        assertEquals(0.73, Recovery.ratePerTick(RATES, true) / Recovery.ratePerTick(RATES, false), 1e-9);
    }

    @Test void recoveryCrossingSunsetKeepsTheProgressItAlreadyEarned() {
        // Not "pick a timer at the moment of resolution": clearing a herd at
        // 5:59 and at 6:01 should not recover on entirely different terms.
        long half = (long) (RATES.dayTicks() / 2);
        double justBeforeSunset = Recovery.advance(0, half, false, RATES);
        assertEquals(0.5, justBeforeSunset, 1e-6);

        double afterSunset = Recovery.advance(justBeforeSunset, half, true, RATES);
        assertTrue(afterSunset > 0.5 && afterSunset < 1.0,
                "the daylight half counts, and the rest continues more slowly");

        double allNight = Recovery.advance(0, half * 2, true, RATES);
        assertTrue(afterSunset > allNight, "starting in daylight really is worth more");
    }

    @Test void swarmAndPeacefulRatesAreConfiguredSeparately() {
        // Swarms are not assumed to follow the peaceful night slowdown; the
        // direction is that they change at night rather than slow down.
        var peaceful = new Recovery.Rates(12000, 0.33, 0.73);
        var swarm = new Recovery.Rates(12000, 0.33, 1.0);
        assertNotEquals(Recovery.ratePerTick(peaceful, true), Recovery.ratePerTick(swarm, true));
    }

    // ---- reset -----------------------------------------------------------

    @Test void matchResetDiscardsManifestationsAndKeepsTheOpportunity() {
        var o = Opportunity.fresh();
        o.manifested(new Eligibility.Locus(10, 65, 10), 5);
        o.memberRemoved();
        o.discardManifestation();
        assertEquals(Opportunity.State.READY_AWAITING_LOCUS, o.state());
        assertEquals(0, o.remaining());
        assertNull(o.locus());
    }
}
