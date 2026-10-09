package com.minecraftmoba.plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The combat chamber's pre-entry flow laid out as an inventory screen, as pure data.
 *
 * The flow itself ({@link CombatPreEntry}) is a tested state machine; this only decides
 * what goes in which slot and what pressing it MEANS, so the screen is as testable as the
 * flow and the Bukkit layer does nothing but draw slots and run the action string.
 *
 * <h2>Actions are the typed commands</h2>
 *
 * Every slot's action is the words after {@code /moba lab combat}: {@code pick <id>},
 * {@code back}, {@code set cooldown waive}. The screen therefore adds no second grammar:
 * what it can do is exactly what the commands can do, and the commands remain.
 *
 * <h2>Modes are cycled, not typed</h2>
 *
 * At the modes step each setting is one button that advances to its next value, and its
 * label shows the CURRENT value, so a tester reads what will be measured before entering.
 *
 * Pure. Tested without a server.
 */
public final class CombatPreEntryView {
    private CombatPreEntryView() {}

    public enum Kind { OPTION, BACK, MODE, ENTER, INFO }

    /** One slot. {@code action} is null for a slot that cannot be pressed. */
    public record Slot(int index, Kind kind, String id, String label, boolean available, String reason, String action) {}

    public record Screen(String title, int size, List<Slot> slots) {
        public Slot at(int index) {
            for (Slot s : slots) if (s.index() == index) return s;
            return null;
        }
    }

    public static final int OPTION_SLOTS = 45, BACK_SLOT_BIG = 49, INFO_SLOT_BIG = 53;
    public static final int COOLDOWN = 10, TIME = 12, LEVEL = 14, DUMMY = 16, ENTER = 22, BACK_SLOT_SMALL = 18;

    private static final int[] LEVELS = {1, 5, 10, 15, 20, 25, 30};

    public static Screen of(CombatPreEntry flow, int maxLevel) {
        var slots = new ArrayList<Slot>();
        String title = "Combat chamber · " + stepName(flow.step());
        if (flow.step() == CombatPreEntry.Step.MODES) {
            var m = flow.modes();
            slots.add(new Slot(COOLDOWN, Kind.MODE, "cooldown", "Cooldowns: " + (m.cooldownWaiver() ? "WAIVED" : "normal"), true, null,
                    "set cooldown " + (m.cooldownWaiver() ? "normal" : "waive")));
            slots.add(new Slot(TIME, Kind.MODE, "time", "Time of day: " + m.timeOfDay().name().toLowerCase(Locale.ROOT), true, null,
                    "set time " + nextTime(m.timeOfDay()).name().toLowerCase(Locale.ROOT)));
            slots.add(new Slot(LEVEL, Kind.MODE, "level", "Level: " + m.level(), true, null,
                    "set level " + nextLevel(m.level(), maxLevel)));
            var sides = flow.allowedSides();
            if (!sides.isEmpty()) {
                var cur = m.dummy() == null ? sides.get(0) : m.dummy();
                boolean cycle = sides.size() > 1;
                var next = sides.get((sides.indexOf(cur) + 1) % sides.size());
                slots.add(new Slot(DUMMY, Kind.MODE, "dummy", "Dummy side: " + cur.name().toLowerCase(Locale.ROOT), cycle,
                        cycle ? null : "This output only makes sense against a " + cur.name().toLowerCase(Locale.ROOT) + " dummy.",
                        cycle ? "set dummy " + next.name().toLowerCase(Locale.ROOT) : null));
            }
            slots.add(new Slot(ENTER, Kind.ENTER, "enter", "Enter the chamber", true, null, "pick enter"));
            slots.add(new Slot(BACK_SLOT_SMALL, Kind.BACK, "back", "Go back", true, null, "back"));
            return new Screen(title, 27, slots);
        }
        int i = 0;
        for (var c : flow.options()) {
            if (i >= OPTION_SLOTS) break;
            slots.add(new Slot(i++, Kind.OPTION, c.id(), c.label(), c.available(), c.reason(), c.available() ? "pick " + c.id() : null));
        }
        if (flow.step() != CombatPreEntry.Step.CLASS)
            slots.add(new Slot(BACK_SLOT_BIG, Kind.BACK, "back", "Go back", true, null, "back"));
        slots.add(new Slot(INFO_SLOT_BIG, Kind.INFO, "info", help(flow.step()), true, null, null));
        return new Screen(title, 54, slots);
    }

    static String stepName(CombatPreEntry.Step s) {
        return switch (s) {
            case CLASS -> "choose a class"; case ROLE -> "choose a role"; case SLOT -> "choose an ability";
            case OUTPUT -> "choose an output"; case AIM -> "choose where the dummy aims"; case SCRIPT -> "choose how it acts";
            case MODES -> "set the modes"; case READY -> "ready";
        };
    }

    static String help(CombatPreEntry.Step s) {
        return switch (s) {
            case CLASS -> "Nothing else is offered until a class is chosen.";
            case ROLE -> "Operate is always available; the others depend on what the class's abilities do.";
            case SLOT -> "Greyed slots say why they cannot be used in this role.";
            case OUTPUT -> "An ability can have several outputs.";
            case AIM -> "Where the dummy points when it casts.";
            case SCRIPT -> "Once, or repeating.";
            default -> "";
        };
    }

    /** The next time-of-day setting, wrapping. */
    public static CombatPreEntry.TimeOfDay nextTime(CombatPreEntry.TimeOfDay t) {
        var all = CombatPreEntry.TimeOfDay.values();
        return all[(t.ordinal() + 1) % all.length];
    }

    /** The next preset level above the current one, wrapping to 1; never above the configured maximum. */
    public static int nextLevel(int current, int maxLevel) {
        for (int l : LEVELS) if (l > current && l <= maxLevel) return l;
        return 1;
    }
}
