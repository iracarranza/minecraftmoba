package com.minecraftmoba.plugin;

import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;

/**
 * Where the combat chamber's control deck and its buttons sit, as pure geometry.
 *
 * The deck is the combat chamber's answer to the terrain chamber's observation
 * platform: a floor beside the slab with one button per verb, each a block with a
 * label above it and an interaction box over it. It stands WEST of the control
 * band, flush with the slab's floor, so a tester steps off the stone onto it and
 * watches the dummy from there.
 *
 * Buttons are two blocks apart so labels never overlap, and Leave sits well clear
 * of the rest: a world button is pressed deliberately, but the one that ends the
 * session still should not be next to the one pressed most.
 */
public record CombatConsole(Box deck, List<Button> buttons) {

    public record Pos(int x, int y, int z) {}
    public record Box(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        public boolean contains(Pos p) {
            return p.x() >= minX && p.x() <= maxX && p.y() >= minY && p.y() <= maxY && p.z() >= minZ && p.z() <= maxZ;
        }
        public boolean overlapsSlab() {
            return minX <= CombatSlab.maxX() && maxX >= CombatSlab.minX()
                    && minZ <= CombatSlab.MAX_Z && maxZ >= CombatSlab.MIN_Z;
        }
    }

    /** One button: the verb it runs, where it stands, and what it looks like. */
    public record Button(String verb, Pos at, Material block, String label) {}

    private static final int BUTTON_X = -7;
    private static final int FIRST_Z = -19, SPACING = 2;
    /** Leave is set apart from the working buttons by this many blocks. */
    private static final int LEAVE_GAP = 4;

    public static CombatConsole of() {
        var buttons = new ArrayList<Button>();
        int y = CombatSlab.FLOOR_Y + 1;
        int z = FIRST_Z;
        for (String verb : CombatMenu.CONSOLE_VERBS) {
            if (verb.equals(CombatMenu.LEAVE)) { z += LEAVE_GAP - SPACING; }
            buttons.add(new Button(verb, new Pos(BUTTON_X, y, z), material(verb), label(verb)));
            z += SPACING;
        }
        int lastZ = buttons.get(buttons.size() - 1).at().z();
        // Flush against the control band, which starts at x = -3, so a tester walks straight on.
        var deck = new Box(-9, CombatSlab.FLOOR_Y, FIRST_Z - 1, CombatSlab.minX() - 1, CombatSlab.FLOOR_Y, lastZ + 1);
        return new CombatConsole(deck, List.copyOf(buttons));
    }

    private static Material material(String verb) {
        return switch (verb) {
            case "cast" -> Material.PURPLE_CONCRETE;
            case "record.start" -> Material.RED_CONCRETE;
            case "record.stop" -> Material.ORANGE_CONCRETE;
            case "replay.once" -> Material.LIGHT_BLUE_CONCRETE;
            case "replay.loop" -> Material.CYAN_CONCRETE;
            case "replay.stop" -> Material.GRAY_CONCRETE;
            case "log" -> Material.WHITE_CONCRETE;
            case "clear" -> Material.YELLOW_CONCRETE;
            case "reset" -> Material.BROWN_CONCRETE;
            default -> Material.BLACK_CONCRETE;      // leave
        };
    }

    private static String label(String verb) {
        return switch (verb) {
            case "cast" -> "DUMMY CASTS";
            case "record.start" -> "RECORD";
            case "record.stop" -> "STOP RECORDING";
            case "replay.once" -> "GHOST ONCE";
            case "replay.loop" -> "GHOST LOOP";
            case "replay.stop" -> "STOP GHOST";
            case "log" -> "SHOW REPORT";
            case "clear" -> "CLEAR LOGS";
            case "reset" -> "RESET";
            default -> "LEAVE CHAMBER";
        };
    }

    public Button button(String verb) {
        for (Button b : buttons) if (b.verb().equals(verb)) return b;
        return null;
    }
}
