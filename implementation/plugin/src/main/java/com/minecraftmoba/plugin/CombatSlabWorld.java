package com.minecraftmoba.plugin;

import org.bukkit.Material;
import org.bukkit.World;

/** Builds {@link CombatSlab} into a world. Free of decisions: every block is {@link CombatSlab#materialAt}. */
final class CombatSlabWorld {
    private CombatSlabWorld() {}

    /**
     * Rebuild the whole slab, including the air above it, so a rebuild removes
     * what an ability dug, built or left behind.
     */
    static int build(World w) {
        int changed = 0;
        for (int x = CombatSlab.minX(); x <= CombatSlab.maxX(); x++)
            for (int z = CombatSlab.MIN_Z; z <= CombatSlab.MAX_Z; z++)
                for (int y = CombatSlab.bottomY(); y <= CombatSlab.topY(); y++) {
                    Material want = CombatSlab.materialAt(x, y, z);
                    var block = w.getBlockAt(x, y, z);
                    if (block.getType() != want) { block.setType(want, false); changed++; }
                }
        var border = w.getWorldBorder();
        border.setCenter(CombatSlab.centreX(), 0.5);
        border.setSize(CombatSlab.borderSize());
        return changed;
    }
}
