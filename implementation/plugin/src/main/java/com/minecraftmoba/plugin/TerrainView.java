package com.minecraftmoba.plugin;

import org.bukkit.Material;

/**
 * What the eligibility query is allowed to know about the world.
 *
 * The query runs against the CURRENT world, not against authored data, because
 * player modification has to be able to change which loci are eligible without
 * changing the underlying Opportunity. That means the query cannot be answered
 * from the map file, and it also means it has to be re-answered before every
 * manifestation rather than cached.
 *
 * This interface exists so that the answering is separable from the asking. A
 * live match answers it from the loaded world; a test answers it from a small
 * hand-built fixture. Without the seam, none of the architectural properties
 * worth testing -- that a locus can move, that a player can invalidate one --
 * could be tested without a server.
 */
public interface TerrainView {

    /** Highest non-air Y in this column, or Integer.MIN_VALUE if unknown/unloaded. */
    int surfaceY(int x, int z);

    Material blockAt(int x, int y, int z);

    /** Whether a player put this block here. Wild manifestations avoid player work. */
    boolean isPlayerPlaced(int x, int y, int z);

    /** Distance to the nearest player, or Double.MAX_VALUE if none. */
    double distanceToNearestPlayer(int x, int y, int z);

    /** Whether this column is loaded and therefore answerable at all. */
    default boolean known(int x, int z) { return surfaceY(x, z) != Integer.MIN_VALUE; }
}
