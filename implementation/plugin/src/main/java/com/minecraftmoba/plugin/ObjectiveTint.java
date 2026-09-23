package com.minecraftmoba.plugin;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;

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
public final class ObjectiveTint implements Listener {

    /**
     * Candidate tints, most visually distinct first.
     *
     * NON-CANON FIXTURE: an appearance palette, not a design decision. The
     * runtime takes the first two that the map does not already contain, so
     * order is a preference and not a guarantee.
     */
    private static final List<String> CANDIDATES = List.of(
            "SWAMP",             // murky olive; verified legible in play
            "BADLANDS",          // tan/orange
            "DESERT",            // pale straw
            "SNOWY_PLAINS",      // washed-out grey-green
            "MANGROVE_SWAMP",    // dark teal, and a distinct water colour
            "SAVANNA",           // dry gold
            "DARK_FOREST",       // notably darker green
            "MUSHROOM_FIELDS");

    /**
     * Chosen for GRASS AND FOLIAGE TINT, not for how the biome looks overall.
     *
     * The first list led with CHERRY_GROVE because a cherry grove reads as
     * pink. That pink is cherry leaf and log BLOCKS; the biome's own grass
     * colour is close to plains, so it would have tinted almost nothing. Only
     * biomes whose grass/foliage colour genuinely differs belong here, and
     * SWAMP leads because it is the one confirmed by eye in play.
     */

    /** How far apart to sample when surveying which biomes a map uses. */
    private static final int SURVEY_STEP = 32;

    private record Cell(int x, int y, int z) {}

    private final MobaPlugin plugin;
    private final Map<Cell, Team> planned = new LinkedHashMap<>();
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

    /**
     * Plan the tint for every volume, and paint whatever is already loaded.
     *
     * PLANNED, then painted per chunk. The first version painted all 4840 cells
     * at selection, when almost none of the objectives' chunks were loaded:
     * `setBiome` pulled each chunk in, wrote, and the chunk unloaded again
     * without the write surviving, so the log reported 4840 cells and the
     * Fountain was still sparse_jungle minutes later. Same shape as the glow's
     * first failure -- a count of writes attempted reported as a result.
     */
    public int apply(World w, Map<Location, Team> volumes, int radius, int height) {
        revert();
        if (!enabled() || w == null || volumes.isEmpty()) return 0;
        this.world = w;
        if (!choose(w, volumes.keySet())) return 0;
        for (var entry : volumes.entrySet()) {
            Location at = entry.getKey();
            Team team = entry.getValue();
            if (chosen.get(team) == null) continue;
            for (int dx = -radius; dx <= radius; dx += 4)
                for (int dz = -radius; dz <= radius; dz += 4)
                    for (int dy = -4; dy <= height; dy += 4)
                        planned.put(new Cell(at.getBlockX() + dx, at.getBlockY() + dy,
                                at.getBlockZ() + dz), team);
        }
        for (org.bukkit.Chunk c : w.getLoadedChunks()) paint(c);
        plugin.getLogger().info("[tint] " + report());
        return planned.size();
    }

    @EventHandler
    public void chunkLoad(ChunkLoadEvent e) {
        if (!enabled() || world == null || !e.getWorld().equals(world)) return;
        paint(e.getChunk());
    }

    /** Write every planned cell that falls in this chunk, then re-send it. */
    private void paint(org.bukkit.Chunk chunk) {
        boolean wrote = false;
        for (var entry : planned.entrySet()) {
            Cell cell = entry.getKey();
            if (cell.x() >> 4 != chunk.getX() || cell.z() >> 4 != chunk.getZ()) continue;
            Biome tint = chosen.get(entry.getValue());
            if (tint == null) continue;
            Biome was = world.getBiome(cell.x(), cell.y(), cell.z());
            if (was.equals(tint)) continue;
            original.putIfAbsent(cell, was);
            world.setBiome(cell.x(), cell.y(), cell.z(), tint);
            wrote = true;
        }
        // Clients cache biomes with the chunk, so a write is invisible until
        // the chunk is sent again.
        if (wrote) world.refreshChunk(chunk.getX(), chunk.getZ());
    }

    /** Put every tinted cell back. */
    public void revert() {
        planned.clear();
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
        return "OBJECTIVE_TINT enabled=" + enabled() + " planned=" + planned.size()
                + " painted=" + original.size()
                + " north=" + (chosen.get(Team.NORTH) == null ? "none"
                        : chosen.get(Team.NORTH).getKey().getKey())
                + " south=" + (chosen.get(Team.SOUTH) == null ? "none"
                        : chosen.get(Team.SOUTH).getKey().getKey());
    }
}
