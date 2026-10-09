package com.minecraftmoba.plugin;

import org.bukkit.Material;

import java.util.List;

/**
 * What the legibility bench's hotbar and console buttons mean. Pure: the verbs, their
 * command, their refusals and the deck's layout are tests, not things found by pressing.
 *
 * Mirrors the other chambers: the hotbar is a {@link ChamberMenu}, unusable items stay
 * in place with a reason, and leaving is an inventory item. One page, because every verb
 * is a single press: the axis verbs CYCLE (scale, armor, particles) or TOGGLE (glow,
 * clutter), so there is no list to descend into.
 */
public final class LegibilityMenu {
    private LegibilityMenu() {}

    public static final String LEAVE = "leave";

    public record State(boolean inBench) {}
    public record Intent(List<String> command) {}

    public static ChamberMenu menu() {
        return new ChamberMenu("root")
                .page("root", "Legibility bench",
                        ChamberMenu.Item.action("scale", "Next scale"),
                        ChamberMenu.Item.action("glow", "Glow on or off"),
                        ChamberMenu.Item.action("armor", "Next armor"),
                        ChamberMenu.Item.action("particles", "Next particles"),
                        ChamberMenu.Item.action("clutter", "Clutter on or off"),
                        ChamberMenu.Item.action("mark", "Mark: it still reads here"),
                        ChamberMenu.Item.action("report", "Show the report"),
                        ChamberMenu.Item.action("clear", "Clear the marks"));
    }

    public static final List<String> VERBS = List.of(
            "scale", "glow", "armor", "particles", "clutter", "mark", "report", "clear", LEAVE);

    public static Intent intent(String id) {
        return VERBS.contains(id) ? new Intent(List.of(id)) : null;
    }

    public static String refusal(String id, State s) {
        return s.inBench() ? null : "Enter the legibility bench first.";
    }

    public static ChamberMenu.Gate gate(State s) { return id -> refusal(id, s); }

    /** The deck stands west of the track, flush with its floor, near the subject. */
    public static DeckLayout deck() {
        return DeckLayout.row(-7, LegibilityTrack.FLOOR_Y, -4, 4, VERBS, LegibilityMenu::look, -9);
    }

    static DeckLayout.Look look(String verb) {
        return switch (verb) {
            case "scale" -> new DeckLayout.Look(Material.SLIME_BLOCK, "NEXT SCALE");
            case "glow" -> new DeckLayout.Look(Material.YELLOW_CONCRETE, "GLOW");
            case "armor" -> new DeckLayout.Look(Material.LIGHT_GRAY_CONCRETE, "NEXT ARMOR");
            case "particles" -> new DeckLayout.Look(Material.ORANGE_CONCRETE, "NEXT PARTICLES");
            case "clutter" -> new DeckLayout.Look(Material.BROWN_CONCRETE, "CLUTTER");
            case "mark" -> new DeckLayout.Look(Material.LIME_CONCRETE, "MARK (stand at the distance)");
            case "report" -> new DeckLayout.Look(Material.WHITE_CONCRETE, "SHOW REPORT");
            case "clear" -> new DeckLayout.Look(Material.CYAN_CONCRETE, "CLEAR MARKS");
            default -> new DeckLayout.Look(Material.BLACK_CONCRETE, "LEAVE BENCH");
        };
    }
}
