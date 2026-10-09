package com.minecraftmoba.plugin;

import java.util.ArrayList;
import java.util.List;

/**
 * The pre-entry phase of the combat chamber, as a state machine.
 *
 * <pre>
 * class -> role -> (Recipient / Observer) slot -> output -> aim -> script -> modes -> ready
 *               \-> (Operator)                                           modes -> ready
 * </pre>
 *
 * Pure: every option comes from {@link CombatAvailability} and
 * {@link CombatBehavior}, so what is offered follows what abilities declare and is
 * tested without a server. Nothing is offered until a class is chosen, and an
 * unavailable option is carried with its reason and refuses to be chosen with that
 * reason, never silently skipped.
 *
 * Modes (cooldown waiving, time of day, the dummy's side, level) are set here and
 * not mid-session, because they change what is being measured and every report
 * has to say so.
 */
public final class CombatPreEntry {

    public interface Catalog {
        List<String> classes();
        String label(String classId);
        List<CombatAvailability.SlotInfo> slots(String classId);
        int defaultLevel();
    }

    public enum Step { CLASS, ROLE, SLOT, OUTPUT, AIM, SCRIPT, MODES, READY }
    public enum TimeOfDay { DEFAULT, DAY, NIGHT }

    public record Choice(String id, String label, boolean available, String reason) {
        static Choice yes(String id, String label) { return new Choice(id, label, true, null); }
        static Choice no(String id, String label, String reason) { return new Choice(id, label, false, reason); }
    }

    /** What the tester decided about how to measure. */
    public record Modes(boolean cooldownWaiver, TimeOfDay timeOfDay,
                        CombatAvailability.Relation dummy, int level) {}

    /** The finished choice, handed to whatever builds the chamber. */
    public record Session(String classId, CombatAvailability.Role role, CombatAvailability.Slot slot,
                          String outputId, CombatBehavior.Aim aim, CombatBehavior.Script script,
                          Modes modes, CombatAvailability.Mode mode) {}

    private final Catalog catalog;
    private final List<Step> history = new ArrayList<>();
    private Step step = Step.CLASS;

    private String classId;
    private CombatAvailability.Role role;
    private CombatAvailability.SlotInfo slot;
    private AbilityOutput output;
    private CombatBehavior.Aim aim;
    private CombatBehavior.Script script;
    private boolean cooldownWaiver;
    private TimeOfDay timeOfDay = TimeOfDay.DEFAULT;
    private CombatAvailability.Relation dummy;
    private int level;
    private CombatAvailability.Mode mode;

    public CombatPreEntry(Catalog catalog) {
        this.catalog = catalog;
        this.level = catalog.defaultLevel();
    }

    public Step step() { return step; }
    public boolean ready() { return step == Step.READY; }

    // ---- what is offered -------------------------------------------------------

    public List<Choice> options() {
        var out = new ArrayList<Choice>();
        switch (step) {
            case CLASS -> { for (String id : catalog.classes()) out.add(Choice.yes(id, catalog.label(id))); }
            case ROLE -> {
                var slots = catalog.slots(classId);
                for (var r : CombatAvailability.Role.values()) {
                    var o = CombatAvailability.role(r, slots);
                    out.add(o.available() ? Choice.yes(r.name(), label(r)) : Choice.no(r.name(), label(r), o.reason()));
                }
            }
            case SLOT -> {
                for (var s : catalog.slots(classId)) {
                    var o = CombatAvailability.slot(role, s);
                    String label = s.slot() + " - " + s.name();
                    out.add(o.available() ? Choice.yes(s.slot().name(), label) : Choice.no(s.slot().name(), label, o.reason()));
                }
            }
            case OUTPUT -> {
                for (var o : slot.outputs()) {
                    var a = CombatAvailability.output(role, slot, o);
                    out.add(a.available() ? Choice.yes(o.id(), o.id()) : Choice.no(o.id(), o.id(), a.reason()));
                }
            }
            case AIM -> {
                for (var c : CombatBehavior.aims(output))
                    out.add(c.available() ? Choice.yes(c.value().name(), c.value().label)
                            : Choice.no(c.value() == null ? "unsupported" : c.value().name(),
                                        c.value() == null ? "Unsupported" : c.value().label, c.reason()));
            }
            case SCRIPT -> {
                for (var c : CombatBehavior.scripts(output))
                    out.add(c.available() ? Choice.yes(c.value().name(), c.value().label)
                            : Choice.no("none", "No script", c.reason()));
            }
            case MODES -> out.add(Choice.yes("enter", "Enter the chamber"));
            case READY -> { }
        }
        return out;
    }

    private static String label(CombatAvailability.Role r) {
        return switch (r) { case OPERATOR -> "Operate"; case RECIPIENT -> "Receive or counter"; case OBSERVER -> "Observe"; };
    }

    // ---- choosing ---------------------------------------------------------------

    /** Choose an option. An unavailable one throws, saying why. */
    public void choose(String id) {
        Choice picked = options().stream().filter(c -> c.id().equals(id)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No option '" + id + "' at step " + step));
        if (!picked.available()) throw new IllegalStateException(picked.reason());
        history.add(step);
        switch (step) {
            case CLASS -> { classId = id; step = Step.ROLE; }
            case ROLE -> {
                role = CombatAvailability.Role.valueOf(id);
                if (role == CombatAvailability.Role.OPERATOR) { mode = CombatAvailability.Mode.OPERATE; step = Step.MODES; }
                else step = Step.SLOT;
            }
            case SLOT -> {
                slot = catalog.slots(classId).stream().filter(s -> s.slot().name().equals(id)).findFirst().orElseThrow();
                var usable = CombatAvailability.usableOutputs(role, slot);
                if (usable.size() == 1) { output = usable.get(0); settleOutput(); }
                else step = Step.OUTPUT;
            }
            case OUTPUT -> {
                output = slot.outputs().stream().filter(o -> o.id().equals(id)).findFirst().orElseThrow();
                settleOutput();
            }
            case AIM -> { aim = CombatBehavior.Aim.valueOf(id); step = Step.SCRIPT; }
            case SCRIPT -> { script = CombatBehavior.Script.valueOf(id); step = Step.MODES; }
            case MODES -> step = Step.READY;
            case READY -> throw new IllegalStateException("Already ready.");
        }
    }

    /** After an output is known: derive the mode and relation, then go on to aim or script. */
    private void settleOutput() {
        var o = CombatAvailability.output(role, slot, output);
        mode = o.mode();
        dummy = o.relation();
        step = CombatBehavior.needsAim(output) ? Step.AIM : Step.SCRIPT;
    }

    /** Step back, forgetting what the abandoned step decided and everything after it. */
    public void back() {
        if (history.isEmpty()) throw new IllegalStateException("Nothing to go back to.");
        Step previous = history.remove(history.size() - 1);
        clearFrom(previous);
        step = previous;
    }

    /** Forget the decision made at this step and every later one. */
    private void clearFrom(Step s) {
        int from = s.ordinal();
        if (from <= Step.CLASS.ordinal()) classId = null;
        if (from <= Step.ROLE.ordinal()) { role = null; mode = null; }
        if (from <= Step.SLOT.ordinal()) slot = null;
        if (from <= Step.OUTPUT.ordinal()) { output = null; dummy = null; }
        if (from <= Step.AIM.ordinal()) aim = null;
        if (from <= Step.SCRIPT.ordinal()) script = null;
    }

    // ---- modes ------------------------------------------------------------------

    private void requireModes() {
        if (step != Step.MODES) throw new IllegalStateException("Modes are set at the modes step, not " + step + ".");
    }

    public void cooldownWaiver(boolean on) { requireModes(); cooldownWaiver = on; }
    public void timeOfDay(TimeOfDay t) { requireModes(); timeOfDay = t; }

    public void level(int l) {
        requireModes();
        if (l < 1) throw new IllegalArgumentException("A level starts at 1.");
        level = l;
    }

    /** Which side the dummy is on. Only the sides the chosen output actually acts on are allowed. */
    public void dummyTeam(CombatAvailability.Relation r) {
        requireModes();
        if (role == CombatAvailability.Role.OPERATOR) return;
        if (!allowedSides().contains(r))
            throw new IllegalStateException("This output acts on " + describeSides() + ", so a dummy on the "
                    + r.name().toLowerCase() + " side would show nothing.");
        dummy = r;
    }

    public List<CombatAvailability.Relation> allowedSides() {
        if (output == null) return List.of();
        return switch (output.affects()) {
            case ENEMIES -> List.of(CombatAvailability.Relation.ENEMY);
            case ALLIES -> List.of(CombatAvailability.Relation.ALLY);
            case BOTH -> List.of(CombatAvailability.Relation.ENEMY, CombatAvailability.Relation.ALLY);
            // Counter mode: the tester strikes the dummy, so it stands opposite.
            case NONE -> List.of(CombatAvailability.Relation.ENEMY);
        };
    }

    private String describeSides() {
        return switch (output.affects()) {
            case ENEMIES -> "enemies only"; case ALLIES -> "allies only"; case BOTH -> "either side";
            case NONE -> "its caster only";
        };
    }

    public Modes modes() { return new Modes(cooldownWaiver, timeOfDay, dummy, level); }

    public Session session() {
        if (!ready()) throw new IllegalStateException("Not ready: at step " + step + ".");
        return new Session(classId, role, slot == null ? null : slot.slot(), output == null ? null : output.id(),
                aim, script, modes(), mode);
    }
}
