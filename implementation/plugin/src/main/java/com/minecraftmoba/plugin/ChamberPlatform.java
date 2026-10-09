package com.minecraftmoba.plugin;

/**
 * Where the observation platform and its door sit, relative to a bay.
 *
 * Pure geometry: nothing here touches a world, so that the platform can be
 * shown to stay inside the gutter between bays -- not over a neighbour's
 * terrain, not over the tester's own -- as arithmetic rather than hoped for.
 *
 * <h2>Shape</h2>
 *
 * The platform stands in the gutter on the bay's north side, raised above the
 * ground so the tester looks DOWN into the bay. A glass wall along the bay edge
 * is the observation window. A doorway in that wall, with a pressure plate
 * just inside it, is the way in. The two scoop-source buttons stand on the
 * deck's rear edge, facing the window.
 *
 * Positions are block coordinates. Y values are relative to the bay's floor.
 */
public record ChamberPlatform(Box deck, Box window, int doorX, int plateZ, int deckY,
                              Pos certifiedButton, Pos randomButton) {

    public record Pos(int x, int y, int z) {}

    /** Inclusive axis-aligned box. */
    public record Box(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        public boolean overlaps(Box o) {
            return minX <= o.maxX && maxX >= o.minX && minY <= o.maxY && maxY >= o.minY
                    && minZ <= o.maxZ && maxZ >= o.minZ;
        }
        public boolean contains(Pos p) {
            return p.x() >= minX && p.x() <= maxX && p.y() >= minY && p.y() <= maxY
                    && p.z() >= minZ && p.z() <= maxZ;
        }
    }

    /** How far above the bay floor the deck stands. */
    public static final int RISE = 8;
    /** Half the deck's width along X. */
    public static final int HALF_WIDTH = 5;

    /** Deck depth in blocks, leaving a one-block margin to each neighbour's bay. */
    public static int depth(int gutter) { return Math.max(1, gutter - 3); }

    public static ChamberPlatform of(Chamber bay, int gutter) {
        int cx = (bay.minX() + bay.maxX()) / 2;
        int deckY = bay.minY() + RISE;
        // z = minZ - 1 is the window wall; the deck runs outward from it.
        int wallZ = bay.minZ() - 1;
        int deckMaxZ = wallZ - 1;
        int deckMinZ = deckMaxZ - depth(gutter) + 1;
        Box deck = new Box(cx - HALF_WIDTH, deckY, deckMinZ, cx + HALF_WIDTH, deckY, deckMaxZ);
        Box window = new Box(cx - HALF_WIDTH, deckY + 1, wallZ, cx + HALF_WIDTH, deckY + 3, wallZ);
        int plateZ = deckMaxZ;
        Pos certified = new Pos(cx - 3, deckY + 1, deckMinZ);
        Pos random = new Pos(cx + 3, deckY + 1, deckMinZ);
        return new ChamberPlatform(deck, window, cx, plateZ, deckY, certified, random);
    }

    /** Every block the platform occupies, as boxes, for overlap checks. */
    public java.util.List<Box> footprint() { return java.util.List.of(deck, window); }

    /** The pressure plate stands on the deck, in the doorway. */
    public Pos plate() { return new Pos(doorX, deckY + 1, plateZ); }
}
