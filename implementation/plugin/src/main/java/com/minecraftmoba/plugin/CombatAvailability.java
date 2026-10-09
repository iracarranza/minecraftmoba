package com.minecraftmoba.plugin;

import java.util.ArrayList;
import java.util.List;

/**
 * Which roles, slots and outputs the combat chamber can offer for a class, decided
 * from what abilities DECLARE rather than from a hand-kept table.
 *
 * Pure: nothing here touches a server, so the rules are tested for every class in
 * the skeleton manifest. An option that cannot be used is carried with its reason,
 * never dropped, for the same reason the hotbar menu does it: a missing option
 * cannot be told from one that was never offered.
 *
 * <h2>The rules</h2>
 *
 * <b>Operator</b> is always available: you take the class and use it.
 *
 * <b>Recipient</b> needs a built ability whose output reaches someone other than
 * its caster. Two shapes, because the ability decides which:
 * <ul>
 * <li><b>Receive</b>: it acts on others ({@code affects} ENEMIES, ALLIES or BOTH),
 *     so the dummy casts and the tester is the target.</li>
 * <li><b>Counter</b>: it touches only its caster but changes how that caster
 *     fights ({@code affects} NONE and {@code combat} true), so the tester strikes
 *     the dummy before and after it casts. Bloodhide is the model case.</li>
 * </ul>
 * An output that does neither (a pure movement ability) has nothing to receive or
 * counter, and says so.
 *
 * <b>Observer</b> needs a built output. A passive has no gesture to observe, so it
 * needs a scripted usage that does not exist yet.
 *
 * {@code affects} and {@code combat} are separate questions and neither derives
 * from the other: Crash Landing is aimed at SELF yet damages enemies, and Deathly
 * Clutches is combat-relevant yet touches nobody else.
 */
public final class CombatAvailability {
    private CombatAvailability() {}

    public enum Role { OPERATOR, RECIPIENT, OBSERVER }
    public enum Slot { PASSIVE, A1, A2, ULT }

    /** How the tester and the dummy relate for this use. */
    public enum Mode { OPERATE, RECEIVE, COUNTER, WATCH }

    /** Which side the dummy stands on, relative to the tester. */
    public enum Relation { ENEMY, ALLY }

    /**
     * One slot of a class as the chamber needs to see it.
     *
     * @param built       an Ability exists and declares its outputs
     * @param passive     the slot is a passive (it has no cast)
     * @param combat      the ability changes how its caster fights (from AbilityCombat)
     */
    public record SlotInfo(Slot slot, String abilityId, String name, boolean built, boolean passive,
                           boolean combat, List<AbilityOutput> outputs) {
        public SlotInfo {
            outputs = outputs == null ? List.of() : List.copyOf(outputs);
        }
    }

    /** One usable or unusable option, with the reason when it cannot be used. */
    public record Option(boolean available, String reason, Mode mode, Relation relation) {
        static Option yes(Mode mode, Relation relation) { return new Option(true, null, mode, relation); }
        static Option no(String reason) { return new Option(false, reason, null, null); }
    }

    /** The role as a whole: available if any slot supports it. */
    public static Option role(Role role, List<SlotInfo> slots) {
        if (role == Role.OPERATOR) return Option.yes(Mode.OPERATE, null);
        for (SlotInfo s : slots)
            for (AbilityOutput o : s.outputs())
                if (output(role, s, o).available()) return Option.yes(null, null);
        return Option.no(role == Role.RECIPIENT
                ? "None of this class's built abilities acts on, or changes how it fights against, another player."
                : "This class has no built ability to observe yet.");
    }

    /** A slot: available if any of its outputs is. */
    public static Option slot(Role role, SlotInfo slot) {
        if (role == Role.OPERATOR) return Option.yes(Mode.OPERATE, null);
        String refusal = notUsable(slot);
        if (refusal != null) return Option.no(refusal);
        Option first = null;
        for (AbilityOutput o : slot.outputs()) {
            Option opt = output(role, slot, o);
            if (opt.available()) return opt;
            if (first == null) first = opt;
        }
        return first != null ? first : Option.no("This slot declares no outputs.");
    }

    /** One output of a slot. */
    public static Option output(Role role, SlotInfo slot, AbilityOutput out) {
        if (role == Role.OPERATOR) return Option.yes(Mode.OPERATE, null);
        String refusal = notUsable(slot);
        if (refusal != null) return Option.no(refusal);
        if (role == Role.OBSERVER) return Option.yes(Mode.WATCH, null);
        // RECIPIENT
        if (out.affects() == Recipients.NONE) {
            if (slot.combat()) return Option.yes(Mode.COUNTER, Relation.ENEMY);
            return Option.no("It acts only on its caster and does not change how its caster fights, "
                    + "so there is nothing to receive and nothing to counter.");
        }
        Relation relation = out.affects() == Recipients.ALLIES ? Relation.ALLY : Relation.ENEMY;
        return Option.yes(Mode.RECEIVE, relation);
    }

    /** Why this slot cannot be used by Recipient or Observer at all, or null. */
    private static String notUsable(SlotInfo s) {
        if (!s.built() && !s.passive()) return "Not built yet.";
        if (s.passive()) return "A passive has no cast to receive, and no scripted usage exists to observe.";
        if (s.outputs().isEmpty()) return "It declares no outputs.";
        return null;
    }

    /** The outputs of a slot a role can use, in declaration order. */
    public static List<AbilityOutput> usableOutputs(Role role, SlotInfo slot) {
        var out = new ArrayList<AbilityOutput>();
        for (AbilityOutput o : slot.outputs()) if (output(role, slot, o).available()) out.add(o);
        return out;
    }
}
