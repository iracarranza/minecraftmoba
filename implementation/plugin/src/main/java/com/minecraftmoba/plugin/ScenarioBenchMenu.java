package com.minecraftmoba.plugin;

import java.util.List;

/**
 * The scenario bench's hotbar and typed verbs, and when each is refused. Pure.
 *
 * Four verbs: cycle the selected scenario, run it, stop it, show the report. A scenario cannot be
 * started while one is running (two would share the lab's bodies and teams), and there is nothing
 * to stop when none is.
 */
public final class ScenarioBenchMenu {
    private ScenarioBenchMenu() {}

    public record State(boolean active, boolean running) {}

    public static ChamberMenu menu() {
        return new ChamberMenu("root")
                .page("root", "Scenarios",
                        ChamberMenu.Item.action("scenario", "Next scenario"),
                        ChamberMenu.Item.action("run", "Run it"),
                        ChamberMenu.Item.action("stop", "Stop it"),
                        ChamberMenu.Item.action("report", "Show the results"));
    }

    public static final List<String> VERBS = List.of("scenario", "run", "stop", "report", "off");

    public static String refusal(String id, State s) {
        if (!s.active()) return "Start the bench first: /moba lab scenario.";
        return switch (id) {
            case "run" -> s.running() ? "A scenario is already running. Stop it first." : null;
            case "scenario" -> s.running() ? "A scenario is running. Stop it before choosing another." : null;
            case "stop" -> s.running() ? null : "No scenario is running.";
            default -> null;
        };
    }

    public static ChamberMenu.Gate gate(State s) { return id -> refusal(id, s); }
}
