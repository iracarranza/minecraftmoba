package com.minecraftmoba.plugin;

import java.util.List;

/**
 * What the combat chamber's hotbar and console buttons mean, and whether each may
 * be used right now. Pure, so the mapping and every refusal are tests rather than
 * things found by pressing buttons.
 *
 * It mirrors the terrain chamber: the hotbar is a {@link ChamberMenu} the tester
 * descends, an item that cannot be used is SHOWN with its reason rather than hidden,
 * and the verbs that end the session are not hotbar verbs. The same ids drive the
 * world-object buttons of {@link CombatConsole}, so there is one set of verbs.
 *
 * <h2>Right-click belongs to the ability when ability mode is on</h2>
 *
 * A2 is bound to right-click. The menu therefore only reacts while ability mode is
 * OFF, which is a rule of the Bukkit layer ({@link CombatUi}), stated here because
 * it shapes what the items can promise.
 */
public final class CombatMenu {
    private CombatMenu() {}

    /** Verbs that appear on the console but never on the hotbar. */
    public static final String LEAVE = "leave";

    /** The facts a refusal depends on. */
    public record State(boolean inChamber, boolean operator, boolean recording, boolean hasTake, boolean replaying) {}

    /** The in-chamber command a verb runs: the words after {@code /moba lab combat}. */
    public record Intent(List<String> command) {}

    public static ChamberMenu menu() {
        return new ChamberMenu("root")
                .page("root", "Combat chamber",
                        ChamberMenu.Item.action("cast", "Dummy casts"),
                        ChamberMenu.Item.submenu("record", "Record a take"),
                        ChamberMenu.Item.submenu("replay", "Ghost replay"),
                        ChamberMenu.Item.action("interference", "Toggle interference"),
                        ChamberMenu.Item.action("log", "Show the report"),
                        ChamberMenu.Item.action("clear", "Clear the logs"),
                        ChamberMenu.Item.action("reset", "Reset the chamber"))
                .page("record", "Record",
                        ChamberMenu.Item.action("record.start", "Start recording"),
                        ChamberMenu.Item.action("record.stop", "Stop and save"))
                .page("replay", "Replay",
                        ChamberMenu.Item.action("replay.once", "Replay once"),
                        ChamberMenu.Item.action("replay.loop", "Replay looping"),
                        ChamberMenu.Item.action("replay.stop", "Stop the ghost"));
    }

    /** Every verb the console offers, in button order. {@link #LEAVE} is last and set apart. */
    public static final List<String> CONSOLE_VERBS = List.of(
            "cast", "record.start", "record.stop", "replay.once", "replay.loop", "replay.stop",
            "interference", "log", "clear", "reset", LEAVE);

    public static Intent intent(String id) {
        return switch (id) {
            case "cast" -> new Intent(List.of("cast"));
            case "record.start" -> new Intent(List.of("record", "start"));
            case "record.stop" -> new Intent(List.of("record", "stop"));
            case "replay.once" -> new Intent(List.of("replay"));
            case "replay.loop" -> new Intent(List.of("replay", "loop"));
            case "replay.stop" -> new Intent(List.of("replay", "stop"));
            case "interference" -> new Intent(List.of("interference"));
            case "log" -> new Intent(List.of("log"));
            case "clear" -> new Intent(List.of("clear"));
            case "reset" -> new Intent(List.of("reset"));
            case LEAVE -> new Intent(List.of("leave"));
            default -> null;
        };
    }

    /** Null when the verb may be used; otherwise why not. Submenu ids are always usable. */
    public static String refusal(String id, State s) {
        if (!s.inChamber()) return "Enter the combat chamber first.";
        return switch (id) {
            case "cast" -> s.operator() ? "You are the caster; the dummy is passive. Use your own abilities, or replay a take." : null;
            case "record.start" -> !s.operator() ? "Record as Operator: a take is made by acting."
                    : s.recording() ? "Already recording." : s.replaying() ? "Stop the ghost first." : null;
            case "record.stop" -> s.recording() ? null : "Not recording.";
            case "replay.once", "replay.loop" -> s.recording() ? "Stop recording first."
                    : !s.hasTake() ? "No take yet: record one as Operator." : s.replaying() ? "The ghost is already replaying." : null;
            case "replay.stop" -> s.replaying() ? null : "The ghost is not replaying.";
            default -> null;
        };
    }

    public static ChamberMenu.Gate gate(State s) { return id -> refusal(id, s); }
}
