package com.minecraftmoba.plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * What the testing-mode readout says. Pure: formatting and arithmetic, no server.
 *
 * Testing mode empties the hotbar of the chamber's own menu, which is also where a tester reads the chamber from, so the
 * numbers that matter to a combat test go on a sidebar instead: loadout, the tester's health and armor, and the dummy's.
 * Health is EFFECTIVE health (what Capacity scaling makes it) and not the twenty-point vanilla bar, which under scaling
 * is always twenty and says nothing.
 */
public final class TestingHudLines {
    private TestingHudLines() {}

    /** One combatant's numbers. {@code armor} and {@code toughness} are the attribute values, so projected or modified armor counts. */
    public record Vitals(String label, double health, double maxHealth, double armor, double toughness) {}

    public static final int BAR_WIDTH = 10;

    /** A bar of {@code BAR_WIDTH} cells: filled cells proportional to current/max, never empty while alive and never more than full. */
    public static String bar(double current, double max) {
        if (max <= 0) return "░".repeat(BAR_WIDTH);
        double fraction = Math.max(0, Math.min(1, current / max));
        int filled = (int) Math.round(fraction * BAR_WIDTH);
        if (current > 0 && filled == 0) filled = 1;
        return "█".repeat(filled) + "░".repeat(BAR_WIDTH - filled);
    }

    static String number(double v) { return v == Math.rint(v) ? String.valueOf((int) v) : String.format(Locale.ROOT, "%.1f", v); }

    /** The lines, top to bottom. Every line is distinct (a sidebar keys on its text). */
    public static List<String> lines(String loadoutName, int loadoutItems, Vitals you, Vitals dummy) {
        var out = new ArrayList<String>();
        out.add("Testing: " + loadoutName + " (" + loadoutItems + " item" + (loadoutItems == 1 ? "" : "s") + ")");
        out.add(" ");
        out.addAll(block(you, "\u00a71"));
        if (dummy != null) {
            out.add("  ");
            out.addAll(block(dummy, "\u00a72"));
        }
        return out;
    }

    /** {@code tag} is an invisible colour code so the two combatants' lines never collide in the sidebar, which keys on text. */
    private static List<String> block(Vitals v, String tag) {
        return List.of(
                v.label() + "  " + number(v.health()) + "/" + number(v.maxHealth()),
                bar(v.health(), v.maxHealth()) + tag,
                "Armor " + number(v.armor()) + "  Tough " + number(v.toughness()) + tag);
    }
}
