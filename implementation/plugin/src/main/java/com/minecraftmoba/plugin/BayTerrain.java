package com.minecraftmoba.plugin;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * Replacing the terrain inside a tester's bay.
 *
 * <h2>Two sources, and they are never one control</h2>
 *
 * {@link Source.Certified} copies a region out of a pre-staged certified scoop.
 * {@link Source.Random} copies a region of rough vanilla terrain nobody vetted.
 * They answer different questions -- <i>does this work on ground we ship</i>
 * versus <i>does this survive ground nobody vetted</i> -- so a measurement from
 * one must never be reportable as the other. That is why {@link Result} carries
 * which one it was.
 *
 * <h2>Certified means certified</h2>
 *
 * Loading a scoop world and reading a region that was never generated would make
 * Minecraft invent terrain, which would then be reported as certified ground.
 * So the certified source refuses a sampling centre unless every chunk it needs
 * is already generated on disk, and gives up rather than fall back.
 *
 * <h2>Undoable</h2>
 *
 * Every changed block is journalled through {@link LabAuthoring#recordEdit}, so
 * a regeneration is undone like any other authored edit. The bay is sized
 * against that journal's budget (ChambersTest).
 *
 * NOT VERIFIED AGAINST A LIVE SERVER at the time of writing. The mapping
 * arithmetic is tested ({@link RegionCopy}); creating, reading and deleting the
 * temporary source world is not.
 */
final class BayTerrain {
    sealed interface Source {
        String label();
        record Certified(MapPool.Entry entry, String label) implements Source {}
        record Random(long seed) implements Source {
            @Override public String label() { return "random seed " + seed; }
        }
    }

    /** What was done, naming the source kind so reports cannot blur the two. */
    record Result(String kind, String label, int changed, int sourceX, int sourceZ) {}

    private static final int ATTEMPTS = 8;

    private final MobaPlugin plugin;
    private final LabAuthoring authoring;
    private final Random random = new Random();

    BayTerrain(MobaPlugin plugin, LabAuthoring authoring) {
        this.plugin = plugin;
        this.authoring = authoring;
    }

    Result regenerate(Player p, Chamber bay, Source source) throws IOException {
        World target = Bukkit.getWorld(bay.world());
        if (target == null) throw new IllegalStateException("The bay's world is not loaded.");
        authoring.discardPreview(p);
        String name = "chamber_src_" + Long.toHexString(System.nanoTime());
        Path folder = Bukkit.getWorldContainer().toPath().resolve(name);
        World src = null;
        try {
            if (source instanceof Source.Certified c) {
                WorldInstance.copyTree(c.entry().world(), folder);
                Files.deleteIfExists(folder.resolve("session.lock"));
                Files.deleteIfExists(folder.resolve("uid.dat"));
                src = Bukkit.createWorld(new WorldCreator(name));
            } else {
                long seed = ((Source.Random) source).seed();
                src = Bukkit.createWorld(new WorldCreator(name).seed(seed).generateStructures(false));
            }
            if (src == null) throw new IOException("Could not load the source world.");
            return copy(p, bay, target, src, source);
        } finally {
            if (src != null) Bukkit.unloadWorld(src, false);
            try { WorldInstance.deleteTree(folder); }
            catch (IOException ex) { plugin.getLogger().warning("[chamber] could not delete " + folder + ": " + ex); }
        }
    }

    private Result copy(Player p, Chamber bay, World target, World src, Source source) throws IOException {
        int radius = (bay.maxX() - bay.minX()) / 2;
        boolean certified = source instanceof Source.Certified;
        int cx = 0, cz = 0, surface = 0;
        boolean found = false;
        for (int attempt = 0; attempt < ATTEMPTS && !found; attempt++) {
            cx = src.getSpawnLocation().getBlockX() + random.nextInt(certified ? 401 : 4001) - (certified ? 200 : 2000);
            cz = src.getSpawnLocation().getBlockZ() + random.nextInt(certified ? 401 : 4001) - (certified ? 200 : 2000);
            if (certified && !generated(src, cx, cz, radius)) continue;
            surface = src.getHighestBlockYAt(cx, cz);
            // Rough terrain is for severity testing, not for testing a sea: skip
            // a centre that is open water.
            Material top = src.getBlockAt(cx, surface, cz).getType();
            if (top == Material.WATER) continue;
            found = true;
        }
        if (!found)
            throw new IOException(certified
                    ? "No fully generated stretch of this certified scoop was found near its spawn. "
                      + "Not substituting generated terrain: that would not be certified ground."
                    : "Could not find dry ground in this random seed. Try again.");
        var map = RegionCopy.of(bay, cx, cz, surface);

        // Mobs and drops from earlier experiments do not belong in fresh terrain.
        for (Entity e : target.getNearbyEntities(new org.bukkit.util.BoundingBox(
                bay.minX(), bay.minY(), bay.minZ(), bay.maxX() + 1, bay.maxY() + 1, bay.maxZ() + 1)))
            if (!(e instanceof Player)) e.remove();

        var before = new ArrayList<BlockState>();
        int minY = Math.max(bay.minY(), target.getMinHeight());
        int maxY = Math.min(bay.maxY(), target.getMaxHeight() - 1);
        for (int x = bay.minX(); x <= bay.maxX(); x++) {
            for (int z = bay.minZ(); z <= bay.maxZ(); z++) {
                for (int y = minY; y <= maxY; y++) {
                    int sy = map.sourceY(y);
                    var data = (sy < src.getMinHeight() || sy >= src.getMaxHeight())
                            ? Material.AIR.createBlockData()
                            : src.getBlockAt(map.sourceX(x), sy, map.sourceZ(z)).getBlockData();
                    var block = target.getBlockAt(x, y, z);
                    if (block.getBlockData().equals(data)) continue;
                    before.add(block.getState());
                    block.setBlockData(data, false);
                }
            }
        }
        authoring.recordEdit("regenerate (" + source.label() + ")", before);
        String kind = certified ? "CERTIFIED" : "RANDOM";
        p.sendMessage(kind + ": bay regenerated from " + source.label() + " at "
                + cx + "," + cz + ". " + before.size() + " blocks changed; /moba lab undo reverts it.");
        if (!certified)
            p.sendMessage("Rough terrain nobody vetted. A result here is not a result on shipped ground.");
        return new Result(kind, source.label(), before.size(), cx, cz);
    }

    private static boolean generated(World src, int cx, int cz, int radius) {
        for (int x = (cx - radius) >> 4; x <= (cx + radius) >> 4; x++)
            for (int z = (cz - radius) >> 4; z <= (cz + radius) >> 4; z++)
                if (!src.isChunkGenerated(x, z)) return false;
        return true;
    }

    static long newSeed() { return UUID.randomUUID().getMostSignificantBits(); }
}
