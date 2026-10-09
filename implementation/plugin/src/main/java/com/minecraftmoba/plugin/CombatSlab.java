package com.minecraftmoba.plugin;

import org.bukkit.Material;

import java.util.List;

/**
 * The blank-slate combat venue as pure geometry: a deep slab of measured ground in
 * side-by-side bands, so an ability can be tried on stone, sand, dirt, water, ice
 * or a slope without changing anything else.
 *
 * The first band is the CONTROL: plain stone, flat, with the tester and the dummy
 * on it. That follows {@code TestBed}'s rule of a control followed by variations
 * along one axis, so a result that differs on another band was changed by the
 * ground and nothing else.
 *
 * The ground is deep ({@link #DEPTH} blocks) because terrain-reading classes
 * (Mole, Quarryman, Paver, Gardener) need something to dig, and a one-block floor
 * would be an empty room.
 *
 * Everything here is a function of coordinates, so the layout is tested without a
 * server and the builder only loops over {@link #materialAt}.
 */
public final class CombatSlab {
    private CombatSlab() {}

    public enum Feature { FLAT, POOL, RAMP }

    public record Band(String name, Material surface, Material fill, Feature feature, int minX, int maxX) {
        public boolean contains(int x) { return x >= minX && x <= maxX; }
    }

    public static final int FLOOR_Y = 64;
    public static final int DEPTH = 12;
    public static final int HEADROOM = 14;
    public static final int BAND_WIDTH = 7;
    public static final int MIN_Z = -20, MAX_Z = 20;
    /** Water depth of the pool band, in blocks. */
    public static final int POOL_DEPTH = 3;
    /** The ramp rises one block every this many blocks of z. */
    public static final int RAMP_RUN = 8;

    public static final List<Band> BANDS = List.of(
            new Band("control", Material.STONE, Material.STONE, Feature.FLAT, -3, 3),
            new Band("sand", Material.SAND, Material.SANDSTONE, Feature.FLAT, 4, 10),
            new Band("grass", Material.GRASS_BLOCK, Material.DIRT, Feature.FLAT, 11, 17),
            new Band("water", Material.WATER, Material.STONE, Feature.POOL, 18, 24),
            new Band("ice", Material.PACKED_ICE, Material.STONE, Feature.FLAT, 25, 31),
            new Band("ramp", Material.STONE_BRICKS, Material.STONE, Feature.RAMP, 32, 38));

    public static int minX() { return BANDS.get(0).minX(); }
    public static int maxX() { return BANDS.get(BANDS.size() - 1).maxX(); }
    public static int bottomY() { return FLOOR_Y - DEPTH; }
    public static int topY() { return FLOOR_Y + HEADROOM; }

    public static boolean contains(int x, int z) { return x >= minX() && x <= maxX() && z >= MIN_Z && z <= MAX_Z; }

    public static Band bandAt(int x) {
        for (Band b : BANDS) if (b.contains(x)) return b;
        return null;
    }

    public static Band band(String name) {
        for (Band b : BANDS) if (b.name().equals(name)) return b;
        throw new IllegalArgumentException("No band named " + name);
    }

    /** Height of the top block of the column at (x, z), or {@code Integer.MIN_VALUE} outside the slab. */
    public static int surfaceY(int x, int z) {
        if (!contains(x, z)) return Integer.MIN_VALUE;
        Band b = bandAt(x);
        if (b.feature() == Feature.RAMP) return FLOOR_Y + 1 + (z - MIN_Z) / RAMP_RUN;
        return FLOOR_Y;
    }

    /** The block at a position. Air anywhere outside the ground, so a rebuild also clears what abilities left. */
    public static Material materialAt(int x, int y, int z) {
        if (!contains(x, z) || y < bottomY() || y > topY()) return Material.AIR;
        Band b = bandAt(x);
        int top = surfaceY(x, z);
        if (y > top) return Material.AIR;
        if (b.feature() == Feature.POOL) {
            if (y > FLOOR_Y - POOL_DEPTH) return Material.WATER;
            return y == FLOOR_Y - POOL_DEPTH ? Material.STONE_BRICKS : b.fill();
        }
        return y == top ? b.surface() : b.fill();
    }

    /** Where the tester stands: on the control band, near one end, looking along +z. */
    public static double[] testerSpawn() { return new double[]{0.5, FLOOR_Y + 1, MIN_Z + 7.5}; }

    /** Where the dummy stands, {@code distance} blocks in front of the tester, on the control band. */
    public static double[] dummySpawn(int distance) {
        if (distance < 2) throw new IllegalArgumentException("a dummy closer than two blocks overlaps the tester");
        double z = testerSpawn()[2] + distance;
        if (z > MAX_Z - 1) throw new IllegalArgumentException("a distance of " + distance + " leaves the slab");
        return new double[]{0.5, FLOOR_Y + 1, z};
    }

    /** Standing position in the middle of a band, for sending a tester to try another ground. */
    public static double[] bandCentre(String name) {
        Band b = band(name);
        double x = (b.minX() + b.maxX()) / 2.0 + 0.5;
        int z = 0;
        return new double[]{x, surfaceY((int) Math.floor(x), z) + 1, z + 0.5};
    }

    public static double centreX() { return (minX() + maxX() + 1) / 2.0; }
    public static double borderSize() { return Math.max(maxX() - minX() + 1, MAX_Z - MIN_Z + 1) + 16; }
}
