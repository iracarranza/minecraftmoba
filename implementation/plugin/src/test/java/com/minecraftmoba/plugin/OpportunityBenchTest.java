package com.minecraftmoba.plugin;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

class OpportunityBenchTest {
    /** A flat world with controllable ground, headroom, placement, players and unloaded columns. */
    private static final class Fake implements TerrainView {
        Material ground = Material.GRASS_BLOCK;
        final Set<Long> unloaded = new HashSet<>(), placed = new HashSet<>(), blocked = new HashSet<>(), odd = new HashSet<>();
        double playerDistance = Double.MAX_VALUE;
        static long k(int x, int z) { return ((long) x << 32) ^ (z & 0xffffffffL); }
        @Override public int surfaceY(int x, int z) { return unloaded.contains(k(x, z)) ? Integer.MIN_VALUE : 64; }
        @Override public Material blockAt(int x, int y, int z) {
            if (y == 64) return odd.contains(k(x, z)) ? Material.OAK_PLANKS : ground;
            if (y > 64 && blocked.contains(k(x, z))) return Material.STONE_BRICKS;
            return Material.AIR;
        }
        @Override public boolean isPlayerPlaced(int x, int y, int z) { return placed.contains(k(x, z)); }
        @Override public double distanceToNearestPlayer(int x, int y, int z) { return playerDistance; }
    }

    private static final Eligibility.Rules RULES = new Eligibility.Rules(2, Eligibility.NATURAL_GROUND, 4, 12.0, 24.0, true);
    private static final OpportunityRegion REGION = OpportunityRegion.square(0, 0, 12);

    // ---- the explainer agrees with the rule it explains ---------------------------

    @Test void anOpenRegionIsEntirelyEligibleAndViable() {
        var r = EligibilityReport.explain(REGION, new Fake(), RULES, null);
        assertEquals(r.columns(), r.eligible()); assertEquals(r.columns(), r.viable());
        assertFalse(r.blocked());
    }

    @Test void everyColumnLandsInExactlyOneBucket() {
        var w = new Fake(); var rng = new Random(5);
        for (int[] c : REGION.columns(4)) {
            switch (rng.nextInt(5)) {
                case 0 -> w.unloaded.add(Fake.k(c[0], c[1]));
                case 1 -> w.odd.add(Fake.k(c[0], c[1]));
                case 2 -> w.placed.add(Fake.k(c[0], c[1]));
                case 3 -> w.blocked.add(Fake.k(c[0], c[1]));
                default -> { }
            }
        }
        var r = EligibilityReport.explain(REGION, w, RULES, null);
        assertEquals(r.columns(), r.unknown() + r.wrongGround() + r.playerPlaced() + r.noHeadroom() + r.eligible());
    }

    @Test void countsAgreeWithEligibilityLociAndSelectAcrossVariedTerrain() {
        for (long seed = 0; seed < 25; seed++) {
            var w = new Fake(); var rng = new Random(seed);
            for (int[] c : REGION.columns(4)) {
                int roll = rng.nextInt(8);
                if (roll == 0) w.unloaded.add(Fake.k(c[0], c[1]));
                else if (roll == 1) w.odd.add(Fake.k(c[0], c[1]));
                else if (roll == 2) w.placed.add(Fake.k(c[0], c[1]));
                else if (roll == 3) w.blocked.add(Fake.k(c[0], c[1]));
            }
            w.playerDistance = rng.nextBoolean() ? 10 : 100;
            var previous = rng.nextBoolean() ? null : new Eligibility.Locus(0, 65, 0);
            var r = EligibilityReport.explain(REGION, w, RULES, previous);
            var loci = Eligibility.loci(REGION, w, RULES);
            assertEquals(loci, r.eligibleLoci(), "seed " + seed);
            // select() draws from exactly the viable set: it returns one of them, or null iff none.
            var chosen = Eligibility.select(loci, RULES, previous, w, new Random(1));
            if (r.viable() == 0) assertNull(chosen, "seed " + seed);
            else assertTrue(r.viableLoci().contains(chosen), "seed " + seed);
        }
    }

    @Test void aPlayerStandingNearbyBlocksEverySiteAndTheReportSaysSo() {
        var w = new Fake(); w.playerDistance = 5;
        var r = EligibilityReport.explain(REGION, w, RULES, null);
        assertTrue(r.blocked()); assertEquals(r.eligible(), r.tooCloseToPlayer());
        assertTrue(EligibilityReport.describe(r).get(0).contains("too close to a player"));
    }

    @Test void theWholeRegionBuiltOverIsReportedAsWrongGround() {
        var w = new Fake();
        for (int[] c : REGION.columns(4)) w.odd.add(Fake.k(c[0], c[1]));
        var r = EligibilityReport.explain(REGION, w, RULES, null);
        assertTrue(r.blocked());
        assertTrue(EligibilityReport.describe(r).get(0).contains("not natural ground"));
    }

    @Test void playerPlacedGroundAndMissingHeadroomAreNamed() {
        var placed = new Fake(); for (int[] c : REGION.columns(4)) placed.placed.add(Fake.k(c[0], c[1]));
        assertTrue(EligibilityReport.describe(EligibilityReport.explain(REGION, placed, RULES, null)).get(0).contains("player-placed"));
        var low = new Fake(); for (int[] c : REGION.columns(4)) low.blocked.add(Fake.k(c[0], c[1]));
        assertTrue(EligibilityReport.describe(EligibilityReport.explain(REGION, low, RULES, null)).get(0).contains("headroom"));
    }

    @Test void theSitePreviouslyUsedExcludesItsNeighbourhood() {
        var r = EligibilityReport.explain(REGION, new Fake(), RULES, new Eligibility.Locus(0, 65, 0));
        assertTrue(r.tooCloseToPrevious() > 0); assertFalse(r.blocked());
    }

    @Test void unloadedColumnsAreCountedNotGuessed() {
        var w = new Fake(); for (int[] c : REGION.columns(4)) w.unloaded.add(Fake.k(c[0], c[1]));
        var r = EligibilityReport.explain(REGION, w, RULES, null);
        assertEquals(r.columns(), r.unknown()); assertTrue(r.blocked());
    }

    @Test void theRadiusCubeCanLeaveMostOfARegionOutside() {
        // The Alpha configuration: radius 20 against a migrated region half-span of 48.
        var alpha = OpportunityRegion.square(0, 0, 48);
        assertEquals(1 - (41.0 * 41.0) / (97.0 * 97.0), EligibilityReport.fractionOutsideRadius(alpha, 0, 0, 20), 1e-9);
        assertEquals(0.0, EligibilityReport.fractionOutsideRadius(OpportunityRegion.square(0, 0, 12), 0, 0, 16), 1e-9);
        assertEquals(1.0, EligibilityReport.fractionOutsideRadius(OpportunityRegion.square(100, 100, 3), 0, 0, 20), 1e-9);
    }

    @Test void theSiteOfAResolvedManifestationBecomesThePreviousSite() {
        // Found by the opportunity bench: resolving cleared the locus without remembering it,
        // so the 'keep a new manifestation away from the last one' rule never had a previous site.
        var op = Opportunity.fresh();
        var a = new Eligibility.Locus(10, 65, 10);
        op.manifested(a, 2);
        op.memberRemoved(); op.memberRemoved();
        assertEquals(Opportunity.State.RECOVERING, op.state());
        assertNull(op.locus());
        assertEquals(a, op.previousLocus());
        var b = new Eligibility.Locus(40, 65, 40);
        op.beginInitialDelay(); op.tickRecovery(1_000_000, false, new Recovery.Rates(12000, 0.33, 0.73));
        op.manifested(b, 1);
        assertEquals(a, op.previousLocus()); assertEquals(b, op.locus());
    }

    @Test void observedEmptinessAlsoRemembersTheSite() {
        var op = Opportunity.fresh();
        var a = new Eligibility.Locus(1, 65, 1);
        op.manifested(a, 3);
        op.observeRemaining(0);
        assertEquals(a, op.previousLocus());
    }

    // ---- the radius is authoritative (decided 10 October 2026) ----------------------

    @Test void aSourcesRegionIsItsRadiusLessThePlacementMarginAndNeverLeavesTheCube() {
        for (int radius : new int[]{8, 12, 20, 32, 48}) {
            var region = Renewables.migratedRegion(100, -50, radius, 6);
            assertEquals(0.0, EligibilityReport.fractionOutsideRadius(region, 100, -50, radius), 1e-12, "radius " + radius);
            assertEquals(Math.max(1, radius - 6), (region.cells().get(0).maxX() - region.cells().get(0).minX()) / 2);
        }
    }

    @Test void aSiteAtTheEdgeOfTheRegionStillKeepsItsMembersInsideTheCube() {
        // The worst case: a site on the region's edge, members spread by the margin beyond it.
        int radius = 20, margin = Renewables.placementMargin(6, 5);
        var c = Renewables.migratedRegion(0, 0, radius, margin).cells().get(0);
        assertTrue(c.maxX() + margin <= radius && c.minX() - margin >= -radius);
        assertTrue(c.maxZ() + margin <= radius && c.minZ() - margin >= -radius);
    }

    @Test void theMarginIsTheLargerOfAPatchsSpreadAndHalfAHerdsCluster() {
        assertEquals(6, Renewables.placementMargin(6, 5));
        assertEquals(6, Renewables.placementMargin(2, 11));      // half of 11, rounded up
        assertEquals(3, Renewables.placementMargin(1, 5));
    }

    @Test void aTinyRadiusStillHasARegion() {
        var region = Renewables.migratedRegion(0, 0, 4, 6);
        assertEquals(9, region.area(), "a 3x3 region, never empty");
    }

    @Test void theBenchPlotsRegionIsExactlyWhatTheRuntimeDerivesFromItsRadius() {
        int margin = Renewables.placementMargin(6, 5);
        for (var plot : OpportunityPlots.PLOTS)
            assertEquals(plot.region().cells(), Renewables.migratedRegion(plot.x(), plot.z(), OpportunityPlots.HALF + margin, margin).cells(), plot.id());
    }

    @Test void theAlphaSourcesNoLongerHaveMostOfTheirRegionOutsideTheirRadius() {
        // Radius 20, as every Alpha source has. Before: a 48-block square, 82% outside the cube.
        var region = Renewables.migratedRegion(0, 0, 20, Renewables.placementMargin(6, 5));
        assertEquals(0.0, EligibilityReport.fractionOutsideRadius(region, 0, 0, 20), 1e-12);
    }

    // ---- the venue ----------------------------------------------------------------

    @Test void plotsAreOnThePlatformAndDoNotOverlap() {
        for (var p : OpportunityPlots.PLOTS) {
            for (var c : p.region().cells()) {
                assertTrue(OpportunityPlots.onPlatform(c.minX(), c.minZ()), p.id());
                assertTrue(OpportunityPlots.onPlatform(c.maxX(), c.maxZ()), p.id());
            }
        }
        var ps = OpportunityPlots.PLOTS;
        for (int i = 0; i < ps.size(); i++) for (int j = i + 1; j < ps.size(); j++) {
            var a = ps.get(i).region().cells().get(0); var b = ps.get(j).region().cells().get(0);
            boolean overlap = a.minX() <= b.maxX() && a.maxX() >= b.minX() && a.minZ() <= b.maxZ() && a.maxZ() >= b.minZ();
            assertFalse(overlap, ps.get(i).id() + " overlaps " + ps.get(j).id());
        }
    }

    @Test void theStationIsOutsideThePlayerExclusionOfEveryPlot() {
        for (var p : OpportunityPlots.PLOTS)
            assertTrue(OpportunityPlots.stationDistance(p) >= RULES.playerExclusion(), p.id() + " at " + OpportunityPlots.stationDistance(p));
    }

    @Test void theSwarmPlotIsWhollyMountainAndTheOthersAreNot() {
        assertTrue(OpportunityPlots.plot("swarm").inMountain());
        assertFalse(OpportunityPlots.plot("herd").inMountain());
        assertFalse(OpportunityPlots.plot("patch").inMountain());
    }

    @Test void everyPlotKindIsARealRenewableKindOfTheRightType() {
        for (var p : OpportunityPlots.PLOTS) {
            var kind = RenewableKinds.require(p.kind());
            assertEquals(p.swarm(), kind.type() == Renewables.Type.SWARM, p.id());
        }
    }

    // ---- the menu -----------------------------------------------------------------

    private static OpportunityBenchMenu.State s(Opportunity.State st, int remaining) { return new OpportunityBenchMenu.State(true, st, remaining); }

    @Test void theMenuFitsAndLeaveIsNotOnTheHotbar() {
        var m = OpportunityBenchMenu.menu();
        for (String page : new String[]{"root", "harvest", "view"}) {
            m.reset(); if (!page.equals("root")) m.open(page);
            for (var item : m.view(ChamberMenu.Gate.OPEN).slots()) {
                if (item == null) continue;
                assertNotEquals("leave", item.id());
                if (item.kind() == ChamberMenu.Kind.ACTION) {
                    assertTrue(OpportunityBenchMenu.VERBS.contains(item.id()), item.id());
                    assertNotNull(OpportunityBenchMenu.intent(item.id()), item.id());
                }
            }
        }
        assertEquals(java.util.List.of("harvest", "one"), OpportunityBenchMenu.intent("harvest.one").command());
    }

    @Test void nothingWorksOutsideTheBench() {
        var out = new OpportunityBenchMenu.State(false, null, 0);
        for (String v : OpportunityBenchMenu.VERBS) assertNotNull(OpportunityBenchMenu.refusal(v, out), v);
    }

    @Test void withoutASourceOnlyTheNonSourceVerbsWork() {
        var none = s(null, 0);
        for (String v : new String[]{"skip", "manifest", "harvest.one", "harvest.all", "base", "show"})
            assertNotNull(OpportunityBenchMenu.refusal(v, none), v);
        for (String v : new String[]{"plot", "spawn", "time", "report", "leave"})
            assertNull(OpportunityBenchMenu.refusal(v, none), v);
    }

    @Test void theLifecycleRulesAreEnforcedWithTheirReasons() {
        var rec = s(Opportunity.State.RECOVERING, 0);
        var man = s(Opportunity.State.MANIFESTED, 3);
        var ready = s(Opportunity.State.READY_AWAITING_LOCUS, 0);
        assertNull(OpportunityBenchMenu.refusal("skip", rec));
        assertTrue(OpportunityBenchMenu.refusal("skip", man).contains("not topped up"));
        assertTrue(OpportunityBenchMenu.refusal("skip", ready).contains("already recovered"));
        assertNull(OpportunityBenchMenu.refusal("manifest", ready));
        assertTrue(OpportunityBenchMenu.refusal("manifest", rec).contains("recovering"));
        assertTrue(OpportunityBenchMenu.refusal("manifest", man).contains("already standing"));
        assertNull(OpportunityBenchMenu.refusal("harvest.one", man));
        assertNotNull(OpportunityBenchMenu.refusal("harvest.all", rec));
        assertNotNull(OpportunityBenchMenu.refusal("harvest.one", s(Opportunity.State.MANIFESTED, 0)));
    }

    @Test void theDeckIsOnThePlatformAndLeaveIsSetApart() {
        var d = OpportunityBenchMenu.deck();
        assertEquals(OpportunityBenchMenu.VERBS.size(), d.buttons().size());
        for (var b : d.buttons()) assertTrue(OpportunityPlots.onPlatform(b.at().x(), b.at().z()), b.verb());
        var b = d.buttons();
        assertEquals("leave", b.get(b.size() - 1).verb());
        assertTrue(b.get(b.size() - 1).at().z() - b.get(b.size() - 2).at().z() >= 4);
    }
}
