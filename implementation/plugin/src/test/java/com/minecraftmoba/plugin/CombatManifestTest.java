package com.minecraftmoba.plugin;

import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.*;

import static com.minecraftmoba.plugin.CombatAvailability.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * The availability rules against the real roster: every class in the ability
 * skeleton manifest, so a rule that works on hand-built fixtures but not on the
 * actual declarations is found here.
 *
 * The manifest carries no `combat` flag (that lives in config.yml per ability), so
 * COUNTER is exercised with hand-built slots in CombatChamberLogicTest and not here.
 */
class CombatManifestTest {

    @SuppressWarnings("unchecked")
    private static Map<String, Object> manifest() throws Exception {
        try (InputStream in = CombatManifestTest.class.getResourceAsStream("/ability-skeleton.yml")) {
            return (Map<String, Object>) new Yaml().load(in);
        }
    }

    private static <E extends Enum<E>> E parse(Class<E> type, Object v) {
        String s = String.valueOf(v);
        return s.equals("?") ? null : Enum.valueOf(type, s);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, List<SlotInfo>> roster() throws Exception {
        var classes = (Map<String, Object>) manifest().get("classes");
        var out = new LinkedHashMap<String, List<SlotInfo>>();
        for (var e : classes.entrySet()) {
            var abilities = (Map<String, Map<String, Object>>) ((Map<String, Object>) e.getValue()).get("abilities");
            var slots = new ArrayList<SlotInfo>();
            for (var a : abilities.entrySet()) {
                Slot slot = Slot.valueOf(a.getKey().equals("ult") ? "ULT" : a.getKey().toUpperCase(Locale.ROOT));
                var m = a.getValue();
                boolean built = "built".equals(m.get("status"));
                boolean passive = slot == Slot.PASSIVE;
                // An ability is a SET of outputs: the manifest carries an
                // outputs map per slot, because Graveyard Shift and Flip and
                // Press each have two that one row could not hold.
                List<AbilityOutput> outputs = List.of();
                if (built) {
                    var declared = (Map<String, Map<String, Object>>) m.get("outputs");
                    var built0 = new ArrayList<AbilityOutput>();
                    declared.forEach((name, o) -> built0.add(new AbilityOutput(name,
                            parse(TargetForm.class, o.get("target")), parse(InputForm.class, o.get("input")),
                            parse(Recipients.class, o.get("affects")),
                            "true".equals(String.valueOf(o.get("caster"))))));
                    outputs = List.copyOf(built0);
                }
                slots.add(new SlotInfo(slot, String.valueOf(m.get("id")), String.valueOf(m.get("name")),
                        built, passive, false, outputs));
            }
            out.put(e.getKey(), slots);
        }
        return out;
    }

    @Test void everyClassCanBeOperated() throws Exception {
        var roster = roster();
        assertEquals(18, roster.size(), "the manifest holds eighteen designed classes");
        for (var e : roster.entrySet())
            assertTrue(role(Role.OPERATOR, e.getValue()).available(), e.getKey());
    }

    @Test void everyBuiltAbilityCanBeObserved() throws Exception {
        int built = 0;
        for (var e : roster().entrySet())
            for (SlotInfo s : e.getValue())
                if (s.built() && !s.passive()) {
                    built++;
                    assertEquals(Mode.WATCH, slot(Role.OBSERVER, s).mode(), e.getKey() + " " + s.name());
                }
        assertTrue(built >= 9, "nine abilities were built when this was written, saw " + built);
    }

    @Test void nothingUnbuiltIsOfferedToRecipientOrObserver() throws Exception {
        for (var e : roster().entrySet())
            for (SlotInfo s : e.getValue())
                if (!s.built())
                    for (var r : List.of(Role.RECIPIENT, Role.OBSERVER))
                        assertFalse(slot(r, s).available(), e.getKey() + " " + s.name() + " as " + r);
    }

    @Test void crashLandingIsReceivedFromAnEnemyDummyBecauseItActsOnEnemies() throws Exception {
        var daredevil = roster().get("daredevil");
        var crash = daredevil.stream().filter(s -> s.abilityId().equals("crash_landing")).findFirst().orElseThrow();
        var o = slot(Role.RECIPIENT, crash);
        assertTrue(o.available());
        assertEquals(Mode.RECEIVE, o.mode());
        assertEquals(Relation.ENEMY, o.relation());
    }

    @Test void aMovementOnlyAbilityOffersRecipientNothing() throws Exception {
        var mole = roster().get("mole");
        var tunneling = mole.stream().filter(s -> s.abilityId().equals("tunneling")).findFirst().orElseThrow();
        assertFalse(slot(Role.RECIPIENT, tunneling).available());
    }

    @Test void noBuiltAbilityReachesAlliesSoNoAllyDummyIsOfferedYet() throws Exception {
        // The affects axis's own test records that no built ability reaches ALLIES. This is
        // its consequence here: an ally dummy has nothing to receive, so the first support
        // ability is the first time that side of Recipient can be exercised.
        for (var e : roster().entrySet())
            for (SlotInfo s : e.getValue())
                for (AbilityOutput o : s.outputs())
                    if (output(Role.RECIPIENT, s, o).available())
                        assertEquals(Relation.ENEMY, output(Role.RECIPIENT, s, o).relation(), e.getKey() + " " + s.name());
    }

    @Test void atLeastOneClassCanBeReceivedSoTheRoleIsNotEmpty() throws Exception {
        long classes = roster().values().stream().filter(slots -> role(Role.RECIPIENT, slots).available()).count();
        assertTrue(classes >= 1, "Recipient must be reachable for some class");
    }
}
