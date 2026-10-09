package com.minecraftmoba.plugin;

import org.bukkit.Material;

import java.util.List;

/**
 * A control deck: a floor with one world button per verb, as pure geometry.
 *
 * The combat chamber has its own ({@link CombatConsole}); this is the generic shape
 * for benches built since, so a new bench supplies verbs and a position instead of
 * writing its own deck builder and button handler. {@link BenchUi} consumes it.
 */
public record DeckLayout(Box deck, List<Button> buttons) {

    public record Pos(int x, int y, int z) {}

    public record Box(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        public boolean contains(Pos p) {
            return p.x() >= minX && p.x() <= maxX && p.y() >= minY && p.y() <= maxY && p.z() >= minZ && p.z() <= maxZ;
        }
        public boolean overlaps(Box o) {
            return minX <= o.maxX && maxX >= o.minX && minY <= o.maxY && maxY >= o.minY && minZ <= o.maxZ && maxZ >= o.minZ;
        }
    }

    public record Button(String verb, Pos at, Material block, String label) {}

    /** What a verb looks like on the deck. */
    public record Look(Material block, String label) {}

    /**
     * A single row of buttons along z, two blocks apart so labels never overlap, with the
     * last verb set apart from the rest (the one that ends the session).
     *
     * @param x       the column the buttons stand in
     * @param floorY  the deck's floor; buttons stand one block above it
     * @param firstZ  z of the first button
     * @param apartGap blocks between the last working button and the final verb
     */
    public static DeckLayout row(int x, int floorY, int firstZ, int apartGap, List<String> verbs,
                                 java.util.function.Function<String, Look> look, int deckWest) {
        var buttons = new java.util.ArrayList<Button>();
        int z = firstZ;
        for (int i = 0; i < verbs.size(); i++) {
            if (i == verbs.size() - 1 && i > 0) z += apartGap - 2;
            var l = look.apply(verbs.get(i));
            buttons.add(new Button(verbs.get(i), new Pos(x, floorY + 1, z), l.block(), l.label()));
            z += 2;
        }
        int lastZ = buttons.get(buttons.size() - 1).at().z();
        var deck = new Box(deckWest, floorY, firstZ - 1, x + 2, floorY, lastZ + 1);
        return new DeckLayout(deck, List.copyOf(buttons));
    }

    public Button button(String verb) {
        for (Button b : buttons) if (b.verb().equals(verb)) return b;
        return null;
    }
}
