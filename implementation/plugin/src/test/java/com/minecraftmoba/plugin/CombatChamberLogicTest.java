package com.minecraftmoba.plugin;

import org.junit.jupiter.api.Test;

import java.util.List;

import static com.minecraftmoba.plugin.CombatAvailability.*;
import static org.junit.jupiter.api.Assertions.*;

/** The pure rules of the combat chamber: availability, behavior choices, pre-entry flow, damage log. */
class CombatChamberLogicTest {

    // ---- fixtures: real abilities' declarations ---------------------------------

    private static AbilityOutput out(String id, TargetForm t, InputForm i, Recipients r) {
        return new AbilityOutput(id, t, i, r);
    }

    /** Crash Landing: aimed at SELF, damages nearby enemies. */
    private static final SlotInfo CRASH = new SlotInfo(Slot.A2, "crash_landing", "Crash Landing", true, false, false,
            List.of(out("crash_landing", TargetForm.SELF, InputForm.INSTANT, Recipients.ENEMIES)));
    /** Deathly Clutches: combat-relevant, touches nobody else. */
    private static final SlotInfo CLUTCHES = new SlotInfo(Slot.ULT, "deathly_clutches", "Deathly Clutches", true, false, true,
            List.of(out("deathly_clutches", TargetForm.SELF, InputForm.INSTANT, Recipients.NONE)));
    /** Tunneling: movement only. */
    private static final SlotInfo TUNNELING = new SlotInfo(Slot.A1, "tunneling", "Tunneling", true, false, false,
            List.of(out("tunneling", TargetForm.DIRECTION, InputForm.INSTANT, Recipients.NONE)));
    private static final SlotInfo PASSIVE = new SlotInfo(Slot.PASSIVE, "sifth_sense", "Sifth Sense", false, true, false, List.of());
    private static final SlotInfo DESIGNED = new SlotInfo(Slot.ULT, "sinkhole", "Sinkhole", false, false, false, List.of());
    /** A support ability that heals allies with a held gesture. */
    private static final SlotInfo HEAL = new SlotInfo(Slot.A1, "mend", "Mend", true, false, false,
            List.of(out("mend", TargetForm.UNIT_ENTITY, InputForm.CHANNELED, Recipients.ALLIES)));
    /** One ability, two outputs: a tap that hits enemies and a hold that affects everyone. */
    private static final SlotInfo TWO = new SlotInfo(Slot.A1, "pantheon_q", "Two-Way", true, false, false,
            List.of(out("stab", TargetForm.UNIT_ENTITY, InputForm.INSTANT, Recipients.ENEMIES),
                    out("throw", TargetForm.AREA_ENTITIES, InputForm.CHARGED, Recipients.BOTH)));

    // ---- availability -----------------------------------------------------------

    @Test void operatorIsAlwaysAvailableForAnySlot() {
        for (var s : List.of(CRASH, CLUTCHES, TUNNELING, PASSIVE, DESIGNED))
            assertTrue(slot(Role.OPERATOR, s).available(), s.name());
        assertTrue(role(Role.OPERATOR, List.of()).available(), "even a class with nothing built can be operated");
    }

    @Test void anAbilityThatActsOnOthersIsReceivedEvenWhenAimedAtSelf() {
        var o = slot(Role.RECIPIENT, CRASH);
        assertTrue(o.available(), "target form must not decide this: Crash Landing is SELF yet hurts enemies");
        assertEquals(Mode.RECEIVE, o.mode());
        assertEquals(Relation.ENEMY, o.relation());
    }

    @Test void aSelfBuffThatChangesFightingIsCounteredNotReceived() {
        var o = slot(Role.RECIPIENT, CLUTCHES);
        assertTrue(o.available());
        assertEquals(Mode.COUNTER, o.mode(), "the tester strikes the dummy before and after it casts");
    }

    @Test void aPureMovementAbilityHasNothingToReceiveOrCounter() {
        var o = slot(Role.RECIPIENT, TUNNELING);
        assertFalse(o.available());
        assertTrue(o.reason().toLowerCase().contains("nothing to receive"), o.reason());
    }

    @Test void aSupportAbilityReceivesFromAnAllyDummy() {
        var o = slot(Role.RECIPIENT, HEAL);
        assertEquals(Mode.RECEIVE, o.mode());
        assertEquals(Relation.ALLY, o.relation(), "an ally dummy casts heals and buffs on the tester");
    }

    @Test void unbuiltAbilitiesAreUnavailableForRecipientAndObserverWithAReason() {
        for (var r : List.of(Role.RECIPIENT, Role.OBSERVER)) {
            var o = slot(r, DESIGNED);
            assertFalse(o.available(), r.name());
            assertEquals("Not built yet.", o.reason());
        }
    }

    @Test void aPassiveCannotBeReceivedOrObserved() {
        for (var r : List.of(Role.RECIPIENT, Role.OBSERVER)) {
            var o = slot(r, PASSIVE);
            assertFalse(o.available(), r.name());
            assertNotNull(o.reason());
        }
    }

    @Test void observerNeedsOnlyABuiltOutput() {
        for (var s : List.of(CRASH, CLUTCHES, TUNNELING, HEAL))
            assertEquals(Mode.WATCH, slot(Role.OBSERVER, s).mode(), s.name());
    }

    @Test void theRoleIsAvailableIfAnySlotSupportsIt() {
        assertTrue(role(Role.RECIPIENT, List.of(TUNNELING, CRASH)).available());
        var none = role(Role.RECIPIENT, List.of(TUNNELING, PASSIVE, DESIGNED));
        assertFalse(none.available());
        assertNotNull(none.reason());
    }

    @Test void eachOutputOfAnAbilityIsJudgedOnItsOwn() {
        var stab = output(Role.RECIPIENT, TWO, TWO.outputs().get(0));
        var thrown = output(Role.RECIPIENT, TWO, TWO.outputs().get(1));
        assertEquals(Relation.ENEMY, stab.relation());
        assertEquals(Mode.RECEIVE, thrown.mode());
        assertEquals(2, usableOutputs(Role.RECIPIENT, TWO).size());
    }

    // ---- behavior ---------------------------------------------------------------

    @Test void aSelfTargetedOutputIsNeverAskedWhereToAim() {
        assertTrue(CombatBehavior.aims(CRASH.outputs().get(0)).isEmpty());
        assertFalse(CombatBehavior.needsAim(CRASH.outputs().get(0)));
    }

    @Test void aMarkedPointIsOfferedForAreasAndDirectionsButNotUnits() {
        var area = CombatBehavior.aims(TWO.outputs().get(1));
        var unit = CombatBehavior.aims(TWO.outputs().get(0));
        assertTrue(area.stream().anyMatch(c -> c.value() == CombatBehavior.Aim.MARKED_POINT && c.available()));
        var marked = unit.stream().filter(c -> c.value() == CombatBehavior.Aim.MARKED_POINT).findFirst().orElseThrow();
        assertFalse(marked.available());
        assertNotNull(marked.reason());
    }

    @Test void asRecordedIsUnavailableUntilARecordingExists() {
        var aims = CombatBehavior.aims(TUNNELING.outputs().get(0));
        var rec = aims.stream().filter(c -> c.value() == CombatBehavior.Aim.AS_RECORDED).findFirst().orElseThrow();
        assertFalse(rec.available(), "a button that does nothing is worse than a greyed one with a reason");
    }

    @Test void heldOutputsAskForADuration() {
        assertTrue(CombatBehavior.needsHoldDuration(HEAL.outputs().get(0)));
        assertFalse(CombatBehavior.needsHoldDuration(CRASH.outputs().get(0)));
    }

    @Test void aPassiveHasNoScript() {
        var passive = out("p", TargetForm.SELF, InputForm.PASSIVE, Recipients.NONE);
        assertFalse(CombatBehavior.scripts(passive).get(0).available());
    }

    // ---- pre-entry --------------------------------------------------------------

    private static CombatPreEntry.Catalog catalog() {
        return new CombatPreEntry.Catalog() {
            public List<String> classes() { return List.of("daredevil", "toolbox"); }
            public String label(String id) { return id; }
            public List<SlotInfo> slots(String id) {
                return id.equals("daredevil") ? List.of(PASSIVE, TUNNELING, CRASH, CLUTCHES)
                        : List.of(PASSIVE, DESIGNED);
            }
            public int defaultLevel() { return 15; }
        };
    }

    @Test void nothingIsOfferedBeforeAClassIsChosen() {
        var p = new CombatPreEntry(catalog());
        assertEquals(CombatPreEntry.Step.CLASS, p.step());
        assertEquals(List.of("daredevil", "toolbox"), p.options().stream().map(CombatPreEntry.Choice::id).toList());
    }

    @Test void afterAClassRecipientAndObserverAppearWithTheirAvailability() {
        var p = new CombatPreEntry(catalog());
        p.choose("toolbox");
        var roles = p.options();
        assertTrue(roles.stream().filter(c -> c.id().equals("OPERATOR")).findFirst().orElseThrow().available());
        var rec = roles.stream().filter(c -> c.id().equals("RECIPIENT")).findFirst().orElseThrow();
        assertFalse(rec.available(), "this class has nothing built");
        assertNotNull(rec.reason());
    }

    @Test void operatorSkipsStraightToModes() {
        var p = new CombatPreEntry(catalog());
        p.choose("daredevil"); p.choose("OPERATOR");
        assertEquals(CombatPreEntry.Step.MODES, p.step());
        p.choose("enter");
        assertTrue(p.ready());
        var s = p.session();
        assertEquals(Mode.OPERATE, s.mode());
        assertNull(s.slot());
    }

    @Test void recipientWalksSlotAimScriptModes() {
        var p = new CombatPreEntry(catalog());
        p.choose("daredevil"); p.choose("RECIPIENT");
        assertEquals(CombatPreEntry.Step.SLOT, p.step());
        p.choose("A2");                                     // Crash Landing: one usable output, so no OUTPUT step
        assertEquals(CombatPreEntry.Step.SCRIPT, p.step(), "SELF-aimed: no aim question");
        p.choose("ONCE");
        assertEquals(CombatPreEntry.Step.MODES, p.step());
        p.cooldownWaiver(true);
        p.timeOfDay(CombatPreEntry.TimeOfDay.NIGHT);
        p.choose("enter");
        var s = p.session();
        assertEquals(Mode.RECEIVE, s.mode());
        assertEquals("crash_landing", s.outputId());
        assertTrue(s.modes().cooldownWaiver());
        assertEquals(CombatPreEntry.TimeOfDay.NIGHT, s.modes().timeOfDay());
        assertEquals(Relation.ENEMY, s.modes().dummy());
    }

    @Test void anUnavailableChoiceRefusesWithItsReason() {
        var p = new CombatPreEntry(catalog());
        p.choose("daredevil"); p.choose("RECIPIENT");
        var ex = assertThrows(IllegalStateException.class, () -> p.choose("A1"));   // Tunneling: nothing to receive
        assertTrue(ex.getMessage().toLowerCase().contains("nothing to receive"));
        assertEquals(CombatPreEntry.Step.SLOT, p.step(), "a refused choice must not advance");
    }

    @Test void aimIsAskedForAimedOutputs() {
        var p = new CombatPreEntry(catalog());
        p.choose("daredevil"); p.choose("OBSERVER");
        p.choose("A1");                                     // Tunneling: DIRECTION
        assertEquals(CombatPreEntry.Step.AIM, p.step());
        p.choose("STRAIGHT_AHEAD");
        assertEquals(CombatPreEntry.Step.SCRIPT, p.step());
    }

    @Test void backForgetsTheAbandonedStepAndEverythingAfter() {
        var p = new CombatPreEntry(catalog());
        p.choose("daredevil"); p.choose("RECIPIENT"); p.choose("A2");
        p.back();                                           // out of the slot choice
        assertEquals(CombatPreEntry.Step.SLOT, p.step());
        p.back();
        assertEquals(CombatPreEntry.Step.ROLE, p.step());
        p.choose("OBSERVER");
        p.choose("A2");
        assertEquals(CombatPreEntry.Step.SCRIPT, p.step());
        p.choose("ONCE"); p.choose("enter");
        assertEquals(Mode.WATCH, p.session().mode(), "the earlier Recipient choice must not leak through");
    }

    @Test void backFromTheClassStepIsRefused() {
        assertThrows(IllegalStateException.class, () -> new CombatPreEntry(catalog()).back());
    }

    @Test void modesAreOnlySettableAtTheModesStep() {
        var p = new CombatPreEntry(catalog());
        assertThrows(IllegalStateException.class, () -> p.cooldownWaiver(true));
    }

    @Test void theDummyCanOnlyStandOnASideTheOutputActsOn() {
        var p = new CombatPreEntry(catalog());
        p.choose("daredevil"); p.choose("RECIPIENT"); p.choose("A2"); p.choose("ONCE");
        var ex = assertThrows(IllegalStateException.class, () -> p.dummyTeam(Relation.ALLY));
        assertTrue(ex.getMessage().contains("enemies only"), ex.getMessage());
        p.dummyTeam(Relation.ENEMY);
    }

    @Test void theSessionIsUnavailableUntilReady() {
        var p = new CombatPreEntry(catalog());
        assertThrows(IllegalStateException.class, p::session);
    }

    @Test void theDefaultLevelComesFromTheCatalog() {
        var p = new CombatPreEntry(catalog());
        p.choose("daredevil"); p.choose("OPERATOR");
        assertEquals(15, p.modes().level());
        p.level(25);
        assertEquals(25, p.modes().level());
        assertThrows(IllegalArgumentException.class, () -> p.level(0));
    }

    // ---- damage log -------------------------------------------------------------

    @Test void rawAndFinalDamageAreKeptTogetherSoMitigationIsVisible() {
        var h = new DamageLog.Hit(5, 24.0, 21.12, "miners_strike");
        assertEquals(0.12, h.mitigated(), 1e-9, "full Iron armor against a capped strike removes 12%");
    }

    @Test void totalsAndPerSourceTotals() {
        var log = new DamageLog();
        log.record(1, 10, 8, "a1"); log.record(5, 10, 8, "a1"); log.record(9, 4, 3, "a2");
        assertEquals(24, log.totalRaw(), 1e-9);
        assertEquals(19, log.totalFinal(), 1e-9);
        assertEquals(16.0, log.finalBySource().get("a1"), 1e-9);
        assertEquals(3, log.count());
    }

    @Test void theWindowIncludesNowAndExcludesTheEdgeBehind() {
        var log = new DamageLog();
        log.record(0, 1, 1, "x"); log.record(10, 1, 2, "x"); log.record(20, 1, 4, "x");
        assertEquals(4, log.finalWithin(20, 10), 1e-9, "(10, 20]: the hit at tick 10 is outside a 10-tick window ending at 20");
        assertEquals(6, log.finalWithin(20, 11), 1e-9, "(9, 20] takes in tick 10 as well");
        assertEquals(4, log.finalWithin(20, 5), 1e-9);
    }

    @Test void perSecondScalesTheWindowToTwentyTicks() {
        var log = new DamageLog();
        log.record(11, 5, 5, "x"); log.record(15, 5, 5, "x");
        assertEquals(20.0, log.perSecond(20, 10), 1e-9, "10 damage in half a second is 20 per second");
    }

    @Test void timeToKillIsInfinityWhenNothingLanded() {
        assertEquals(Double.POSITIVE_INFINITY, new DamageLog().secondsToKill(100, 40, 20));
    }

    @Test void timeToKillUsesTheRecentRate() {
        var log = new DamageLog();
        for (int t = 1; t <= 20; t++) log.record(t, 2, 2, "x");     // 40 damage in one second
        assertEquals(2.5, log.secondsToKill(100, 20, 20), 1e-9);
    }

    @Test void hitsMustBeRecordedInOrderAndNeverNegative() {
        var log = new DamageLog();
        log.record(5, 1, 1, "x");
        assertThrows(IllegalArgumentException.class, () -> log.record(4, 1, 1, "x"));
        assertThrows(IllegalArgumentException.class, () -> new DamageLog.Hit(1, -1, 0, "x"));
    }

    @Test void clearEmptiesTheLog() {
        var log = new DamageLog(); log.record(1, 1, 1, "x"); log.clear();
        assertTrue(log.isEmpty());
        assertEquals(0, log.totalFinal(), 1e-9);
    }
}
