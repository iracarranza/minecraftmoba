package com.minecraftmoba.plugin;

import org.bukkit.World;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.WorldInfo;

import java.util.Random;

/**
 * A world that generates nothing.
 *
 * The lobby is built, not generated. Anything the terrain generator produced
 * would be scenery nobody authored -- the current lobby is a vanilla superflat,
 * and the village in it is there for no reason anybody chose.
 *
 * Empty chunks also make the lobby cheap: no structures, no ore, no caves, and
 * nothing to tick beyond the room itself.
 */
public final class VoidGenerator extends ChunkGenerator {
    @Override public void generateNoise(WorldInfo info, Random random, int cx, int cz, ChunkData data) { }
    @Override public void generateSurface(WorldInfo info, Random random, int cx, int cz, ChunkData data) { }
    @Override public void generateBedrock(WorldInfo info, Random random, int cx, int cz, ChunkData data) { }
    @Override public void generateCaves(WorldInfo info, Random random, int cx, int cz, ChunkData data) { }
    @Override public boolean shouldGenerateNoise() { return false; }
    @Override public boolean shouldGenerateSurface() { return false; }
    @Override public boolean shouldGenerateCaves() { return false; }
    @Override public boolean shouldGenerateDecorations() { return false; }
    @Override public boolean shouldGenerateMobs() { return false; }
    @Override public boolean shouldGenerateStructures() { return false; }
    @Override public BiomeProvider getDefaultBiomeProvider(WorldInfo info) { return null; }
}
