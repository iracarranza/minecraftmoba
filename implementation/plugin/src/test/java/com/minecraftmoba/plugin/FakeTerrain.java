package com.minecraftmoba.plugin;

import org.bukkit.Material;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * A hand-built world for the eligibility tests.
 *
 * The architectural claims worth testing are all about the world CHANGING --
 * that a locus can move, that a player can invalidate one, that building over an
 * ecology pushes the manifestation elsewhere. None of those can be tested
 * against a server without scripting a live match, so the query was built
 * against an interface and this answers it.
 */
final class FakeTerrain implements TerrainView {
    private final Map<Long, Integer> surface = new HashMap<>();
    private final Map<Long, Material> ground = new HashMap<>();
    private final Map<String, Material> blocks = new HashMap<>();
    private final Set<Long> playerPlaced = new HashSet<>();
    private final Map<Long, Double> playerDistance = new HashMap<>();
    private double defaultPlayerDistance = Double.MAX_VALUE;

    private static long col(int x, int z) { return ((long) x << 32) | (z & 0xFFFFFFFFL); }

    /** Flat grass across a rectangle at a given surface height. */
    FakeTerrain grass(int minX, int minZ, int maxX, int maxZ, int y) {
        for (int x = minX; x <= maxX; x++)
            for (int z = minZ; z <= maxZ; z++) { surface.put(col(x, z), y); ground.put(col(x, z), Material.GRASS_BLOCK); }
        return this;
    }

    FakeTerrain water(int minX, int minZ, int maxX, int maxZ, int y) {
        for (int x = minX; x <= maxX; x++)
            for (int z = minZ; z <= maxZ; z++) { surface.put(col(x, z), y); ground.put(col(x, z), Material.WATER); }
        return this;
    }

    /** A player has built here: the ground block is theirs. */
    FakeTerrain built(int minX, int minZ, int maxX, int maxZ, Material material) {
        for (int x = minX; x <= maxX; x++)
            for (int z = minZ; z <= maxZ; z++) {
                ground.put(col(x, z), material);
                playerPlaced.add(col(x, z));
            }
        return this;
    }

    /** An obstruction sitting on the surface, so the column has no headroom. */
    FakeTerrain roofed(int minX, int minZ, int maxX, int maxZ, Material material) {
        for (int x = minX; x <= maxX; x++)
            for (int z = minZ; z <= maxZ; z++)
                blocks.put(x + ":" + (surface.get(col(x, z)) + 1) + ":" + z, material);
        return this;
    }

    FakeTerrain playerAt(int px, int pz, double radiusReported) {
        defaultPlayerDistance = Double.MAX_VALUE;
        for (var e : surface.keySet()) {
            int x = (int) (e >> 32), z = (int) (e & 0xFFFFFFFFL);
            playerDistance.put(e, Math.hypot(x - px, z - pz) < radiusReported
                    ? Math.hypot(x - px, z - pz) : Double.MAX_VALUE);
        }
        return this;
    }

    @Override public int surfaceY(int x, int z) {
        return surface.getOrDefault(col(x, z), Integer.MIN_VALUE);
    }

    @Override public Material blockAt(int x, int y, int z) {
        Material explicit = blocks.get(x + ":" + y + ":" + z);
        if (explicit != null) return explicit;
        Integer s = surface.get(col(x, z));
        if (s == null) return Material.AIR;
        if (y == s) return ground.getOrDefault(col(x, z), Material.GRASS_BLOCK);
        return y > s ? Material.AIR : Material.STONE;
    }

    @Override public boolean isPlayerPlaced(int x, int y, int z) {
        return playerPlaced.contains(col(x, z)) && y == surfaceY(x, z);
    }

    @Override public double distanceToNearestPlayer(int x, int y, int z) {
        return playerDistance.getOrDefault(col(x, z), defaultPlayerDistance);
    }
}
