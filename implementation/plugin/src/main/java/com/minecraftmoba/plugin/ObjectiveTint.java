package com.minecraftmoba.plugin;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Biome;

import java.util.*;

/**
 * Tint the ground a team controls, by borrowing biomes the map does not use.
 *
 * The glowing box proved the legibility idea and then failed its own test in
 * play: a box cannot be read from inside it, and the ground around an objective
 * is exactly where a defender stands. Shrinking the box was the wrong fix --
 * that ground IS the controlled area and the signature should say so. So the
 * volume stays and the representation changes: no glass, and the terrain inside
 * carries the team's colour.
 *
 * Minecraft already tints terrain per biome -- it is why a swamp does not look
 * like a jungle -- so this recolours by swapping the biome rather than by
 * inventing a rendering path.
 *
 * WHY BIOMES THE MAP DOES NOT CONTAIN. Contrast has to be a property of the
 * map, not a guess. A tint that happens to match the surrounding terrain
 * communicates nothing, and which colours are available depends on what the
 * generated map already looks like. So the map is surveyed first and the two
 * tints are drawn from what is ABSENT. That also bounds the side effects: a
 * biome the map never had cannot be confused with one a player learned to read.
 *
 * WHAT THIS CANNOT DO. Only biome-tinted blocks change colour: grass, tall
 * grass and ferns, leaves, vines, sugar cane, water. Dirt, stone, wood, logs
 * and planks have fixed textures and will never take a tint, so an objective's
 * own masonry keeps its colour -- which is wanted, but is not a choice we made.
 * Biome cells are 4x4x4, so the edge snaps to a four-block grid and the
 * boundary is chunky rather than clean.
 *
 * [OPEN] Biome is not only colour. It carries mob spawn lists, weather, ambient
 * sound and fog, so tinting silently changes what spawns on the tinted ground.
 * Choosing absent biomes confines the change rather than removing it. The
 * resolution is a datapack biome that copies the local one and overrides only
 * the colours, or sending biome data per viewer so the server's world never
 * changes at all -- which would also allow showing an ally and an enemy
 * different colours. Neither is done here.
 */
public final class ObjectiveTint {

    /**
     * Candidate tints, most visually distinct first.
     *
     * NON-CANON FIXTURE: an appearance palette, not a design decision. The
     * runtime takes the first two that the map does not already contain, so
     * order is a preference and not a guarantee.
     */
    private static final List<String> CANDIDATES = List.of(
            "CHERRY_GROVE",      // pink foliage
            "SWAMP",             // grey-green, verified legible in play
            "MANGROVE_SWAMP",    // teal-green
            "BADLANDS",          // orange-brown
            "SAVANNA",           // pale gold
            "DARK_FOREST",       // deep green
            "OLD_GROWTH_PINE_TAIGA",
            "MEADOW");

    /** How far apart to sample when surveying which biomes a map uses. */
    private static final int SURVEY_STEP = 32;

    private record Cell(int x, int y, int z) {}

    private final MobaPlugin plugin;
    private final Map<Cell, Biome> original = new LinkedHashMap<>();
    private final EnumMap<Team, Biome> chosen = new EnumMap<>(Team.class);
    private World world;

    public ObjectiveTint(MobaPlugin plugin) { this.plugin = plugin; }

    public boolean enabled() {
        return plugin.getConfig().getBoolean("features.objectiveTint.enabled", false);
    }

    /**
     * Which biomes a player would see NEAR the ground being tinted.
     *
     * Surveyed around each volume rather than across the whole map, for two
     * reasons. It is correct: the tint has to contrast with the terrain beside
     * it, and a biome five hundred blocks away has no bearing on whether an
     * objective's ground reads as claimed. And it is affordable: the first
     * version swept the map on a 16-block grid calling getHighestBlockYAt,
     * which forced a synchronous chunk load per sample -- four thousand of
     * them -- and stalled the main thread hard enough for Paper to dump every
     * thread. These chunks are the ones the tint is about to write to anyway.
     *
     * It reads the volume's own y rather than the surface height, because the
     * heightmap is the expensive half and biome varies little over the few
     * blocks between them.
     */
    public Set<Biome> survey(World w, Collection<Location> around, int radius) {
        Set<Biome> present = new HashSet<>();
        for (Location centre : around)
            for (int dx = -radius; dx <= radius; dx += SURVEY_STEP)
                for (int dz = -radius; dz <= radius; dz += SURVEY_STEP)
                    present.add(w.getBiome(centre.getBlockX() + dx,
                            centre.getBlockY(), centre.getBlockZ() + dz));
        return present;
    }

    /**
     * Pick a tint per team from what the map does not contain.
     *
     * Returns false when the map is varied enough that fewer than two
     * candidates are free. That is a real outcome, not an error to paper over:
     * a tint indistinguishable from nearby terrain would be worse than none,
     * so the feature declines rather than picking a colliding colour.
     */
    private boolean choose(World w, Collection<Location> around) {
        Set<Biome> present = survey(w, around, plugin.getConfig()
                .getInt("features.objectiveTint.surveyRadius", 64));
        List<Biome> free = new ArrayList<>();
        for (String name : CANDIDATES) {
            Biome candidate = org.bukkit.Registry.BIOME.get(
                    org.bukkit.NamespacedKey.minecraft(name.toLowerCase(Locale.ROOT)));
            if (candidate != null && !present.contains(candidate)) free.add(candidate);
        }
        if (free.size() < 2) {
            plugin.getLogger().warning("[tint] this map already uses all but "
                    + free.size() + " candidate tint biome(s); declining to tint rather "
                    + "than pick a colour the surrounding terrain already wears");
            return false;
        }
        chosen.put(Team.NORTH, free.get(0));
        chosen.put(Team.SOUTH, free.get(1));
        return true;
    }

    public Biome tintFor(Team team) { return chosen.get(team); }

    /** Tint the ground inside every objective and Fountain volume. */
    public int apply(World w, Map<Location, Team> volumes, int radius, int height) {
        revert();
        if (!enabled() || w == null || volumes.isEmpty()) return 0;
        this.world = w;
        if (!choose(w, volumes.keySet())) return 0;
        Set<Long> touched = new HashSet<>();
        for (var entry : volumes.entrySet()) {
            Location at = entry.getKey();
            Biome tint = chosen.get(entry.getValue());
            if (tint == null) continue;
            for (int dx = -radius; dx <= radius; dx += 4)
                for (int dz = -radius; dz <= radius; dz += 4)
                    for (int dy = -4; dy <= height; dy += 4) {
                        int x = at.getBlockX() + dx, y = at.getBlockY() + dy,
                                z = at.getBlockZ() + dz;
                        Cell cell = new Cell(x, y, z);
                        if (original.containsKey(cell)) continue;
                        original.put(cell, w.getBiome(x, y, z));
                        w.setBiome(x, y, z, tint);
                        touched.add((((long) (x >> 4)) << 32) | ((z >> 4) & 0xffffffffL));
                    }
        }
        // Clients cache biomes with the chunk, so the recolour is invisible
        // until the chunk is sent again.
        for (long key : touched) w.refreshChunk((int) (key >> 32), (int) key);
        plugin.getLogger().info("[tint] " + report());
        return original.size();
    }

    /** Put every tinted cell back. */
    public void revert() {
        if (world == null || original.isEmpty()) { original.clear(); chosen.clear(); return; }
        Set<Long> touched = new HashSet<>();
        for (var entry : original.entrySet()) {
            Cell c = entry.getKey();
            world.setBiome(c.x(), c.y(), c.z(), entry.getValue());
            touched.add((((long) (c.x() >> 4)) << 32) | ((c.z() >> 4) & 0xffffffffL));
        }
        for (long key : touched) world.refreshChunk((int) (key >> 32), (int) key);
        original.clear();
        chosen.clear();
    }

    public String report() {
        return "OBJECTIVE_TINT enabled=" + enabled() + " cells=" + original.size()
                + " north=" + (chosen.get(Team.NORTH) == null ? "none"
                        : chosen.get(Team.NORTH).getKey().getKey())
                + " south=" + (chosen.get(Team.SOUTH) == null ? "none"
                        : chosen.get(Team.SOUTH).getKey().getKey());
    }
}
