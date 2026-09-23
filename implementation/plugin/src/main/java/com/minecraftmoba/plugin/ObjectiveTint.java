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
    /**
     * The two tint biomes, defined by the `moba_tint` datapack.
     *
     * [SUPERSEDED] This was a list of vanilla candidates, and the map was
     * surveyed to find two it did not already contain. Absence turned out to be
     * the wrong test, twice: CHERRY_GROVE was picked because a cherry grove
     * READS as pink, when that pink is leaf and log BLOCKS and its grass colour
     * is close to plains; then BADLANDS was picked because the map had none,
     * when the south Fountain sits in savanna whose grass is already dry tan.
     * Two biomes can be different biomes and the same colour.
     *
     * Defining the colours outright replaces a search with a decision. These
     * biomes exist only to carry a team's colour: they are never generated,
     * only painted onto ground that already exists, and the datapack gives them
     * empty spawners and no features precisely so that painting them changes
     * what the ground LOOKS like and nothing else. That also removes the open
     * question the vanilla approach could not answer -- a borrowed biome
     * brought its spawn table with it.
     *
     * Their colours match the glow's ChatColor pair, so the two signatures
     * agree rather than being two different aquas.
     */
    private static final Map<Team, org.bukkit.NamespacedKey> TINT = Map.of(
            Team.NORTH, org.bukkit.NamespacedKey.fromString("moba:team_north"),
            Team.SOUTH, org.bukkit.NamespacedKey.fromString("moba:team_south"));

    /** How far apart to sample when surveying which biomes a map uses. */
    private static final int SURVEY_STEP = 32;

    /**
     * Blocks a biome colour can actually reach.
     *
     * MEASURED, not chosen: these are the block models in the 1.21.11 client
     * jar that carry a `tintindex`, which is the only way a block takes a
     * biome's colour. Thirty-three models in the whole game. Everything else --
     * stone, deepslate, MYCELIUM, sand, dirt, wood, terracotta -- renders a
     * fixed texture and cannot be tinted by any means available to a datapack
     * or a resource pack, because the colour-provider registry is Java-side.
     *
     * So the tint is terrain-DEPENDENT, and on a map built into a cave or onto
     * a mushroom island it would mark nothing at all while reporting success.
     * The set exists so coverage can be measured and that case reported rather
     * than shipped.
     */
    private static final Set<String> TINTABLE_SUFFIX = Set.of(
            "GRASS_BLOCK", "SHORT_GRASS", "TALL_GRASS", "FERN", "LARGE_FERN",
            "VINE", "LILY_PAD", "SUGAR_CANE", "BAMBOO", "MELON_STEM",
            "PUMPKIN_STEM", "ATTACHED_MELON_STEM", "ATTACHED_PUMPKIN_STEM",
            "WATER", "BUBBLE_COLUMN");

    private static boolean tintable(org.bukkit.Material m) {
        String n = m.name();
        return TINTABLE_SUFFIX.contains(n) || n.endsWith("_LEAVES");
    }

    private record Cell(int x, int y, int z) {}

    private final MobaPlugin plugin;
    private final Map<Cell, Team> planned = new LinkedHashMap<>();
    private final Map<Cell, Biome> original = new LinkedHashMap<>();
    private final EnumMap<Team, Biome> chosen = new EnumMap<>(Team.class);
    private Set<Biome> surveyed = Set.of();
    private double coverage = -1.0;

    /**
     * What fraction of the sampled surface in these volumes can take a colour.
     *
     * The number the feature was missing. Without it a cave map reports the same
     * "painted N cells" as a meadow, and the log looks identical whether the
     * ground changed colour or not.
     */
    private double measureCoverage(World w, Collection<Location> volumes, int radius) {
        int sampled = 0, colourable = 0;
        for (Location at : volumes)
            for (int dx = -radius; dx <= radius; dx += 4)
                for (int dz = -radius; dz <= radius; dz += 4) {
                    int x = at.getBlockX() + dx, z = at.getBlockZ() + dz;
                    var block = w.getHighestBlockAt(x, z);
                    sampled++;
                    if (tintable(block.getType())) colourable++;
                }
        return sampled == 0 ? 0.0 : (double) colourable / sampled;
    }
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
        chosen.clear();
        var missing = new ArrayList<String>();
        for (var entry : TINT.entrySet()) {
            Biome biome = entry.getValue() == null ? null
                    : org.bukkit.Registry.BIOME.get(entry.getValue());
            if (biome == null) missing.add(String.valueOf(entry.getValue()));
            else chosen.put(entry.getKey(), biome);
        }
        if (!missing.isEmpty()) {
            // Fail loudly rather than falling back to a vanilla colour. A
            // silent fallback is how the wrong tint shipped twice.
            plugin.getLogger().warning("[tint] the moba_tint datapack is not loaded: "
                    + missing + " are not in the biome registry. Install "
                    + "implementation/datapacks/moba_tint into the world's datapacks "
                    + "directory. No tint is applied.");
            chosen.clear();
            return false;
        }
        // The survey no longer chooses anything; it only reports what the
        // ground already looks like, so a tint that fails to read can be
        // diagnosed instead of guessed at.
        surveyed = survey(w, around, plugin.getConfig()
                .getInt("features.objectiveTint.surveyRadius", 64));
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
        coverage = measureCoverage(w, volumes.keySet(), radius);
        if (coverage == 0.0)
            plugin.getLogger().warning("[tint] NOTHING IN THESE VOLUMES CAN TAKE A BIOME "
                    + "COLOUR. Only vegetation and water carry a tintindex; stone, "
                    + "deepslate, mycelium, sand and wood cannot be tinted by any means "
                    + "a datapack has. On this map the tint marks nothing, and the glow "
                    + "is the only signature. Do not read a clean log as a working tint.");
        else if (coverage < 0.15)
            plugin.getLogger().warning(String.format("[tint] only %.0f%% of sampled surface "
                    + "in these volumes can take a biome colour; the tint will read weakly",
                    coverage * 100));
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
                        : chosen.get(Team.SOUTH).getKey().getKey())
                + " surrounding=" + surveyed.size() + " biome(s)"
                + (coverage < 0 ? "" : String.format(" tintable_surface=%.0f%%", coverage * 100));
    }
}
