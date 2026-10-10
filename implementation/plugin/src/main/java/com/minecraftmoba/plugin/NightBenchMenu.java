package com.minecraftmoba.plugin;

import java.util.List;

/**
 * The night and objective bench's hotbar and typed verbs, and when each is refused. Pure.
 *
 * Like the map overlay this is a view onto a launched scoop, so there is no deck. The refusals
 * are the match's own rules said at the moment they matter: the clock only runs forward, and an
 * objective that is not bound on the map or has already toppled cannot be besieged.
 */
public final class NightBenchMenu {
    private NightBenchMenu() {}

    /**
     * @param active the bench is on for this tester
     * @param night the night the clock is in (0 before the first sunset)
     * @param targetBound whether the selected objective exists on this map
     * @param targetToppled whether the selected objective has already been toppled
     */
    public record State(boolean active, int night, boolean targetBound, boolean targetToppled) {}

    public record Intent(List<String> command) {}

    public static ChamberMenu menu() {
        return new ChamberMenu("root")
                .page("root", "Night and objectives",
                        ChamberMenu.Item.submenu("night", "Jump to a night"),
                        ChamberMenu.Item.action("target", "Next objective"),
                        ChamberMenu.Item.submenu("siege", "Besiege it"),
                        ChamberMenu.Item.action("report", "Show the state"))
                .page("night", "Jump to night",
                        ChamberMenu.Item.action("night.1", "Night 1: Worksite I"),
                        ChamberMenu.Item.action("night.2", "Night 2: Giant"),
                        ChamberMenu.Item.action("night.3", "Night 3: Worksite II"),
                        ChamberMenu.Item.action("night.4", "Night 4: Ghast"),
                        ChamberMenu.Item.action("night.5", "Night 5: Worksite III"),
                        ChamberMenu.Item.action("night.6", "Night 6: Dragon"))
                .page("siege", "Besiege",
                        ChamberMenu.Item.action("siege.combat", "A wave of defenders"),
                        ChamberMenu.Item.action("siege.structural", "Break structure"),
                        ChamberMenu.Item.action("siege.signature", "Take the signature"),
                        ChamberMenu.Item.action("siege.lair", "Lair assault"));
    }

    public static final List<String> VERBS = List.of("night.1", "night.2", "night.3", "night.4", "night.5", "night.6",
            "target", "siege.combat", "siege.structural", "siege.signature", "siege.lair", "report", "off");

    public static Intent intent(String id) {
        if (!VERBS.contains(id)) return null;
        return new Intent(id.contains(".") ? List.of(id.split("\\.")) : List.of(id));
    }

    public static String refusal(String id, State s) {
        if (!s.active()) return "Start the bench first: /moba lab night.";
        if (id.startsWith("night.")) {
            int n = Integer.parseInt(id.substring(6));
            if (n <= s.night())
                return "Night " + n + " has already begun (the clock is in night " + s.night() + "); the clock only runs forward. Launch the scoop again to see it from the start.";
            return null;
        }
        if (id.startsWith("siege.")) {
            if (!s.targetBound()) return "That objective is not bound on this map. Pick another with Next objective.";
            if (s.targetToppled()) return "That objective has already toppled. Pick another with Next objective.";
        }
        return null;
    }

    public static ChamberMenu.Gate gate(State s) { return id -> refusal(id, s); }
}
