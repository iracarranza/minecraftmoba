package com.minecraftmoba.plugin;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The terrain families each Bounding branch takes off from.
 *
 * Pinned by name rather than by an enumerated list, because the failure this
 * guards against is a branch that silently works on half its own terrain: a
 * snow LAYER is the block a player stands INSIDE, while a snow BLOCK is the
 * one beneath them, and checking only one of the two looks correct in a flat
 * test world and wrong on a mountain.
 */
class BoundingBranchesTest {

    private static boolean family(Material at, Material below, String f) {
        return BoundingAbility.matchesFamily(at, below, f);
    }

    @Test void snowCountsWhetherItIsALayerOrABlock() {
        assertTrue(family(Material.SNOW, Material.STONE, "snow"), "standing in a snow layer");
        assertTrue(family(Material.AIR, Material.SNOW_BLOCK, "snow"), "standing on a snow block");
        assertTrue(family(Material.AIR, Material.POWDER_SNOW, "snow"));
        assertFalse(family(Material.AIR, Material.STONE, "snow"));
    }

    @Test void leavesCoverEveryWoodType() {
        for (Material m : new Material[]{Material.OAK_LEAVES, Material.AZALEA_LEAVES,
                                         Material.CHERRY_LEAVES, Material.MANGROVE_LEAVES})
            assertTrue(family(Material.AIR, m, "leaves"), m + " is leaves");
        assertFalse(family(Material.AIR, Material.OAK_LOG, "leaves"), "a log is not its leaves");
    }

    @Test void theDirtFamilyIsTheOneTheDesignNames() {
        for (Material m : new Material[]{Material.DIRT, Material.COARSE_DIRT, Material.ROOTED_DIRT,
                                         Material.GRASS_BLOCK, Material.PODZOL, Material.MYCELIUM,
                                         Material.DIRT_PATH, Material.MUD})
            assertTrue(family(Material.AIR, m, "dirt"), m + " is dirt-family terrain");

        assertFalse(family(Material.AIR, Material.STONE, "dirt"));
        assertFalse(family(Material.AIR, Material.SAND, "dirt"), "sand is not dirt-family");
    }

    @Test void familiesDoNotOverlap() {
        assertFalse(family(Material.AIR, Material.SNOW_BLOCK, "dirt"));
        assertFalse(family(Material.AIR, Material.GRASS_BLOCK, "leaves"));
        assertFalse(family(Material.AIR, Material.OAK_LEAVES, "snow"));
    }

    @Test void anUnknownFamilyMatchesNothing() {
        assertFalse(family(Material.GRASS_BLOCK, Material.GRASS_BLOCK, "gravel"),
                "a typo in a branch name must refuse, not match everything");
    }
}
