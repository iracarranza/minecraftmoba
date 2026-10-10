package com.minecraftmoba.plugin;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashSet;
import org.junit.jupiter.api.Test;

class NightBenchTest {
    // ---- the timeline is the cadence, not a copy of it -----------------------------------

    @Test void sixNightsMatchTheCadenceStageForStage() {
        var rows = NightTimeline.scheduled();
        assertEquals(6, rows.size());
        for (var r : rows) {
            var stage = OpportunityCadence.atNight(r.night());
            assertSame(stage, r.stage()); assertEquals(stage.tier(), r.tier()); assertEquals(stage.boss(), r.boss());
            assertTrue(MatchClock.isSunsetBoundary(r.startsAtTick()), "night " + r.night());
        }
    }

    @Test void worksiteAndLairNightsAlternateAndEveryNightOpensExactlyOneThing() {
        for (var r : NightTimeline.scheduled()) assertTrue((r.tier() != null) ^ (r.boss() != null), "night " + r.night());
        assertNotNull(NightTimeline.rows(1).get(0).tier());          // night 1 is a worksite
        assertNotNull(NightTimeline.rows(2).get(1).boss());          // night 2 is the Giant
        assertEquals(OpportunityCadence.Boss.ENDER_DRAGON, NightTimeline.rows(6).get(5).boss());
    }

    @Test void theCurrentNightIsMarkedInTheDescription() {
        var before = NightTimeline.describe(0);
        assertTrue(before.stream().noneMatch(l -> l.startsWith(">")));
        var lines = NightTimeline.describe(LabTime.sunsetTick(3) + 5);
        assertTrue(lines.get(2).startsWith("> night 3"), lines.toString());
        assertEquals(1, lines.stream().filter(l -> l.startsWith(">")).count());
        assertTrue(lines.get(0).contains("Iron + Coal"));
        assertTrue(lines.get(1).contains("GIANT"));
    }

    @Test void currentNightFollowsTheMatchClock() {
        assertEquals(0, NightTimeline.current(0)); assertEquals(1, NightTimeline.current(12000));
        assertEquals(2, NightTimeline.current(36000));
    }

    // ---- targets ---------------------------------------------------------------------------

    @Test void thereAreSixTargetsAndCyclingVisitsAllOfThemThenWraps() {
        assertEquals(6, SiegeTarget.all().size());
        var seen = new HashSet<SiegeTarget.Target>(); SiegeTarget.Target t = null;
        for (int i = 0; i < 6; i++) { t = SiegeTarget.next(t); assertTrue(seen.add(t)); }
        assertEquals(SiegeTarget.all().get(0), SiegeTarget.next(t));
    }

    @Test void everyTargetNameIsOneMatchSiegeAccepts() {
        for (var t : SiegeTarget.all()) assertTrue(java.util.Set.of("outpost", "bastion", "spike").contains(t.objectiveName()), t.label());
        assertEquals("north pillager outpost", SiegeTarget.all().get(0).label());
    }

    @Test void routesAreTheFourTheCapacityKnows() {
        assertEquals(java.util.List.of("combat", "structural", "signature", "lair"), SiegeTarget.ROUTES.stream().map(SiegeTarget.Route::id).toList());
        assertNull(SiegeTarget.route("nonsense")); assertEquals(5, SiegeTarget.route("combat").amount());
    }

    // ---- the menu ----------------------------------------------------------------------------

    @Test void theMenuFitsAndEveryActionHasAnIntent() {
        var m = NightBenchMenu.menu();
        for (String page : new String[]{"root", "night", "siege"}) {
            m.reset(); if (!page.equals("root")) m.open(page);
            for (var item : m.view(ChamberMenu.Gate.OPEN).slots())
                if (item != null && item.kind() == ChamberMenu.Kind.ACTION) assertNotNull(NightBenchMenu.intent(item.id()), item.id());
        }
        assertEquals(java.util.List.of("night", "3"), NightBenchMenu.intent("night.3").command());
        assertEquals(java.util.List.of("target"), NightBenchMenu.intent("target").command());
        assertNull(NightBenchMenu.intent("night.9"));
    }

    @Test void theClockOnlyRunsForwardSoPastNightsAreRefusedWithTheReason() {
        var s = new NightBenchMenu.State(true, 2, true, false);
        assertTrue(NightBenchMenu.refusal("night.1", s).contains("only runs forward"));
        assertTrue(NightBenchMenu.refusal("night.2", s).contains("already begun"));
        assertNull(NightBenchMenu.refusal("night.3", s));
        assertNull(NightBenchMenu.refusal("night.1", new NightBenchMenu.State(true, 0, true, false)));
    }

    @Test void aSiegeIsRefusedOnAnUnboundOrToppledObjective() {
        assertTrue(NightBenchMenu.refusal("siege.combat", new NightBenchMenu.State(true, 0, false, false)).contains("not bound"));
        assertTrue(NightBenchMenu.refusal("siege.lair", new NightBenchMenu.State(true, 0, true, true)).contains("already toppled"));
        assertNull(NightBenchMenu.refusal("siege.signature", new NightBenchMenu.State(true, 0, true, false)));
    }

    @Test void nothingWorksUntilTheBenchIsStartedAndReportsAlwaysDo() {
        var off = new NightBenchMenu.State(false, 0, true, false);
        for (String v : NightBenchMenu.VERBS) assertNotNull(NightBenchMenu.refusal(v, off), v);
        var on = new NightBenchMenu.State(true, 6, false, true);
        assertNull(NightBenchMenu.refusal("report", on)); assertNull(NightBenchMenu.refusal("target", on)); assertNull(NightBenchMenu.refusal("off", on));
    }
}
