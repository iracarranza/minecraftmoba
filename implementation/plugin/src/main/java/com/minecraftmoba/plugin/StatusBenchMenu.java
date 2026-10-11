package com.minecraftmoba.plugin;

import org.bukkit.Material;

import java.util.List;

/**
 * What the status bench's hotbar and console buttons mean. Pure.
 *
 * One page: every verb is a single press. The status and the duration CYCLE, so the tester sets
 * what to apply and then applies it, to the subject body or to themselves.
 */
public final class StatusBenchMenu {
    private StatusBenchMenu() {}

    public static final String LEAVE = "leave";

    public record State(boolean inBench) {}
    public record Intent(List<String> command) {}

    public static ChamberMenu menu() {
        return new ChamberMenu("root")
                .page("root", "Status bench",
                        ChamberMenu.Item.action("status", "Next status"),
                        ChamberMenu.Item.action("time", "Next duration"),
                        ChamberMenu.Item.action("apply", "Apply to the subject"),
                        ChamberMenu.Item.action("self", "Apply to me (feel it)"),
                        ChamberMenu.Item.action("source", "Put the source here"),
                        ChamberMenu.Item.action("check", "Check the rules"),
                        ChamberMenu.Item.action("clear", "Clear every status"),
                        ChamberMenu.Item.action("reset", "Reset subject and source"));
    }

    public static final List<String> VERBS = List.of(
            "status", "time", "apply", "self", "source", "check", "clear", "reset", LEAVE);

    public static Intent intent(String id) {
        return VERBS.contains(id) ? new Intent(List.of(id)) : null;
    }

    public static String refusal(String id, State s) {
        return s.inBench() ? null : "Enter the status bench first.";
    }

    public static ChamberMenu.Gate gate(State s) { return id -> refusal(id, s); }

    public static DeckLayout deck() {
        return DeckLayout.row(-27, StatusArena.FLOOR_Y, 4, 4, VERBS, StatusBenchMenu::look, -29);
    }

    static DeckLayout.Look look(String verb) {
        return switch (verb) {
            case "status" -> new DeckLayout.Look(Material.PURPLE_CONCRETE, "NEXT STATUS");
            case "time" -> new DeckLayout.Look(Material.LIGHT_BLUE_CONCRETE, "NEXT DURATION");
            case "apply" -> new DeckLayout.Look(Material.RED_CONCRETE, "APPLY TO SUBJECT");
            case "self" -> new DeckLayout.Look(Material.ORANGE_CONCRETE, "APPLY TO ME");
            case "source" -> new DeckLayout.Look(Material.YELLOW_CONCRETE, "SOURCE HERE");
            case "check" -> new DeckLayout.Look(Material.WHITE_CONCRETE, "CHECK THE RULES");
            case "clear" -> new DeckLayout.Look(Material.LIME_CONCRETE, "CLEAR ALL");
            case "reset" -> new DeckLayout.Look(Material.CYAN_CONCRETE, "RESET");
            default -> new DeckLayout.Look(Material.BLACK_CONCRETE, "LEAVE BENCH");
        };
    }
}
