package com.minecraftmoba.plugin;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;

/**
 * The live answer to {@link TerrainView}: the loaded world, as it is right now.
 *
 * Unloaded columns answer "unknown" rather than being force-loaded. A
 * regenerative query must not drag the world in around itself, and an
 * opportunity whose ecology nobody is near is one whose manifestation nobody
 * would see anyway.
 */
public final class WorldTerrain implements TerrainView {
    private final World world;
    private final Provenance provenance;

    public WorldTerrain(World world, Provenance provenance) {
        this.world = world;
        this.provenance = provenance;
    }

    @Override public int surfaceY(int x, int z) {
        if (!world.isChunkLoaded(x >> 4, z >> 4)) return Integer.MIN_VALUE;
        return world.getHighestBlockYAt(x, z);
    }

    @Override public Material blockAt(int x, int y, int z) {
        if (!world.isChunkLoaded(x >> 4, z >> 4)) return Material.AIR;
        return world.getBlockAt(x, y, z).getType();
    }

    @Override public boolean isPlayerPlaced(int x, int y, int z) {
        if (provenance == null || !world.isChunkLoaded(x >> 4, z >> 4)) return false;
        return provenance.isPlayerPlaced(world.getBlockAt(x, y, z));
    }

    @Override public double distanceToNearestPlayer(int x, int y, int z) {
        double best = Double.MAX_VALUE;
        Location at = new Location(world, x + 0.5, y, z + 0.5);
        for (Player p : world.getPlayers())
            best = Math.min(best, p.getLocation().distance(at));
        return best;
    }

    public World world() { return world; }

    /** The live world's own phase, so recovery follows what players can see. */
    public static boolean isNight(World w) {
        long time = w.getTime();
        return time >= MatchClock.SUNSET_TICK && time < MatchClock.CYCLE_TICKS;
    }

    static World of(java.util.UUID id) { return Bukkit.getWorld(id); }
}
