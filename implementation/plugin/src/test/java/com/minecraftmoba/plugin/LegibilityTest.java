package com.minecraftmoba.plugin;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashSet;
import org.junit.jupiter.api.Test;

class LegibilityTest {
    // ---- how large it looks -------------------------------------------------

    @Test void aTwoBlockSubjectAtTwoBlocksSubtendsNinetyDegrees() {
        assertEquals(90.0, Legibility.angularHeightDegrees(2, 1), 1e-6);   // 2*atan(2/2)
    }

    @Test void fartherAndSmallerLookSmaller() {
        assertTrue(Legibility.angularHeightDegrees(1.8, 10) > Legibility.angularHeightDegrees(1.8, 20));
        assertTrue(Legibility.angularHeightDegrees(3.6, 20) > Legibility.angularHeightDegrees(1.8, 20));
    }

    @Test void doublingTheScaleAtDoubleTheDistanceLooksAboutTheSame() {
        assertEquals(Legibility.pixelHeight(1.8, 10, 70, 1080), Legibility.pixelHeight(3.6, 20, 70, 1080), 1.0);
    }

    @Test void aSubjectFillingTheFovIsTheScreenHeight() {
        // Height and distance chosen so the subtended angle equals the FOV.
        double fov = 70, d = 5, h = 2 * d * Math.tan(Math.toRadians(fov / 2));
        assertEquals(1080, Legibility.pixelHeight(h, d, fov, 1080), 1e-6);
    }

    @Test void distanceForPixelsInvertsPixelHeight() {
        double d = Legibility.distanceForPixels(1.8, 20, 70, 1080);
        assertEquals(20, Legibility.pixelHeight(1.8, d, 70, 1080), 0.05);
    }

    @Test void guardsAgainstNonsense() {
        assertThrows(IllegalArgumentException.class, () -> Legibility.angularHeightDegrees(1.8, 0));
        assertThrows(IllegalArgumentException.class, () -> Legibility.distanceForPixels(1.8, 0, 70, 1080));
    }

    // ---- who is shown a glow ------------------------------------------------

    private static Legibility.Observer enemy(boolean combat, double d) { return new Legibility.Observer(Legibility.Relation.ENEMY, combat, d); }
    private static final Legibility.Observer ALLY = new Legibility.Observer(Legibility.Relation.ALLY, false, 500);

    @Test void ultimateGlowIsShownToEveryoneAlways() {
        assertTrue(Legibility.glowVisible(Legibility.Tier.ULTIMATE, enemy(false, 900), 24, true));
        assertTrue(Legibility.glowVisible(Legibility.Tier.ULTIMATE, ALLY, 24, false));
    }

    @Test void anAllySeesEmpowermentWhateverTheDistanceCombatOrInvisibility() {
        assertTrue(Legibility.glowVisible(Legibility.Tier.EMPOWERMENT, ALLY, 24, true));
    }

    @Test void anEnemyNeedsCombatAndRadiusAndAVisibleSubject() {
        var t = Legibility.Tier.EMPOWERMENT;
        assertTrue(Legibility.glowVisible(t, enemy(true, 10), 24, false));
        assertFalse(Legibility.glowVisible(t, enemy(false, 10), 24, false));   // not in combat: no leakage at 80 blocks or 10
        assertFalse(Legibility.glowVisible(t, enemy(true, 30), 24, false));    // beyond the radius
        assertFalse(Legibility.glowVisible(t, enemy(true, 10), 24, true));     // invisibility wins
        assertTrue(Legibility.glowVisible(t, enemy(true, 24), 24, false));     // the radius is inclusive
    }

    @Test void explanationsNameTheReasonForHiding() {
        var t = Legibility.Tier.EMPOWERMENT;
        assertTrue(Legibility.explain(t, enemy(false, 10), 24, false).contains("not in combat"));
        assertTrue(Legibility.explain(t, enemy(true, 30), 24, false).contains("beyond 24"));
        assertTrue(Legibility.explain(t, enemy(true, 10), 24, true).contains("invisible"));
        assertTrue(Legibility.explain(t, enemy(true, 10), 24, false).contains("shown"));
    }

    // ---- variants -----------------------------------------------------------

    @Test void theControlAppliesNothing() {
        var c = LegibilityVariant.control();
        assertEquals(1.0, c.scale()); assertFalse(c.glow()); assertFalse(c.clutter());
        assertEquals(LegibilityVariant.Armor.NONE, c.armor());
    }

    @Test void everyAxisCyclesBackToWhereItStarted() {
        var v = LegibilityVariant.control();
        var s = v; for (int i = 0; i < LegibilityVariant.SCALES.length; i++) s = s.nextScale();
        var a = v; for (int i = 0; i < LegibilityVariant.Armor.values().length; i++) a = a.nextArmor();
        var p = v; for (int i = 0; i < LegibilityVariant.Particles.values().length; i++) p = p.nextParticles();
        assertEquals(v, s); assertEquals(v, a); assertEquals(v, p);
        assertEquals(v, v.toggleGlow().toggleGlow());
        assertEquals(v, v.toggleClutter().toggleClutter());
    }

    @Test void scaleHeightFollowsTheLadder() {
        assertEquals(1.8, LegibilityVariant.control().heightBlocks(), 1e-9);
        assertEquals(1.8 * 3.0, new LegibilityVariant(4, false, LegibilityVariant.Armor.NONE, LegibilityVariant.Particles.NONE, false).heightBlocks(), 1e-9);
    }

    @Test void signaturesDistinguishEveryAxis() {
        var v = LegibilityVariant.control();
        var all = new HashSet<String>();
        for (var x : new LegibilityVariant[]{v, v.nextScale(), v.toggleGlow(), v.nextArmor(), v.nextParticles(), v.toggleClutter()})
            assertTrue(all.add(x.signature()), x.signature());
    }

    @Test void aBadScaleStepIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> new LegibilityVariant(99, false, LegibilityVariant.Armor.NONE, LegibilityVariant.Particles.NONE, false));
    }

    // ---- marks --------------------------------------------------------------

    @Test void medianResistsAStrayMark() {
        var log = new LegibilityLog();
        for (double d : new double[]{30, 32, 31, 5, 33}) log.mark("a", d);
        var s = log.summary("a");
        assertEquals(5, s.count()); assertEquals(5, s.min()); assertEquals(33, s.max()); assertEquals(31, s.median());
    }

    @Test void anEvenCountAveragesTheMiddleTwo() {
        var log = new LegibilityLog(); log.mark("a", 10); log.mark("a", 20);
        assertEquals(15, log.summary("a").median());
    }

    @Test void marksAccumulatePerVariantAndClear() {
        var log = new LegibilityLog();
        log.mark("a", 10); log.mark("b", 20); log.mark("a", 12);
        assertEquals(2, log.summary("a").count()); assertEquals(3, log.total());
        assertNull(log.summary("c"));
        log.clear(); assertEquals(0, log.total()); assertTrue(log.signatures().isEmpty());
    }

    @Test void aMarkNeedsADistance() {
        assertThrows(IllegalArgumentException.class, () -> new LegibilityLog().mark("a", 0));
    }

    // ---- the track ----------------------------------------------------------

    @Test void markersAreEvenlySpacedAndOnTheTrack() {
        var d = LegibilityTrack.markerDistances();
        assertFalse(d.isEmpty());
        for (int i = 1; i < d.size(); i++) assertEquals(LegibilityTrack.MARKER_EVERY, d.get(i) - d.get(i - 1));
        for (int dist : d) assertTrue(LegibilityTrack.onTrack(0, LegibilityTrack.markerZ(dist)));
    }

    @Test void theObserverStartsSixteenBlocksOut() {
        assertEquals(16.0, LegibilityTrack.distanceFromSubject(LegibilityTrack.OBSERVER_Z), 1e-9);
    }

    @Test void clutterIsDeterministicInBoundsAndKeepsTheEndsClear() {
        var a = LegibilityTrack.clutter(42, 2.0);
        assertEquals(a, LegibilityTrack.clutter(42, 2.0));
        assertNotEquals(a, LegibilityTrack.clutter(43, 2.0));
        assertFalse(a.isEmpty());
        for (var p : a) {
            assertTrue(LegibilityTrack.onTrack(p.x(), p.z()));
            assertTrue(p.z() - LegibilityTrack.SUBJECT_Z >= LegibilityTrack.CLEAR_RADIUS);
            assertTrue(Math.abs(p.z() - LegibilityTrack.OBSERVER_Z) >= LegibilityTrack.CLEAR_RADIUS);
        }
        assertEquals(a.size(), new HashSet<>(a).size(), "no two pillars on one column");
    }

    @Test void moreDensityMeansMorePillarsAndZeroMeansNone() {
        assertTrue(LegibilityTrack.clutter(1, 4.0).size() > LegibilityTrack.clutter(1, 1.0).size());
        assertTrue(LegibilityTrack.clutter(1, 0).isEmpty());
    }
}
