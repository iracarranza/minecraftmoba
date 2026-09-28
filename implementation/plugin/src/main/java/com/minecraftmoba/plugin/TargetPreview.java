package com.minecraftmoba.plugin;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.Collection;

/**
 * Showing a player what their ability will hit, and showing it to nobody else.
 *
 * <h2>The caster only</h2>
 *
 * {@code Player.spawnParticle} sends to that player alone, where
 * {@code World.spawnParticle} sends to everyone in range. That distinction is
 * the entire implementation of "opponents do not see the indicator", and it is
 * worth naming because the two calls differ by a receiver and nothing else --
 * using the wrong one broadcasts a pre-commitment tell to the people it would
 * most help.
 *
 * <b>A preview is pre-commitment information.</b> A cast has happened and is
 * fair to read; an aim has not, and under Double cast it can be feinted for
 * free. Showing it to an opponent would make aiming a liability rather than a
 * convenience, which is the opposite of what it is for.
 *
 * <h2>Occluded blocks are drawn anyway</h2>
 *
 * A tunnelling preview marks blocks inside stone that the player cannot see,
 * and that is fine. The preview's job is to say WHICH BLOCKS, not to be a
 * picture -- a run that disappears where it enters the hill would answer the
 * question least well exactly where the ability is most opaque.
 *
 * <h2>Drawn per tick, remembered nowhere</h2>
 *
 * Particles are fire-and-forget: there is nothing to clean up when an aim ends,
 * so a cancelled or fired aim simply stops being drawn. State that needs no
 * teardown cannot leak.
 */
public final class TargetPreview {

    private final MobaPlugin plugin;

    public TargetPreview(MobaPlugin plugin) { this.plugin = plugin; }

    private boolean enabled() {
        return plugin.getConfig().getBoolean("features.targetPreview.enabled", true);
    }

    /** How often the marks are redrawn. Fewer, larger draws read as a shape. */
    public int cadenceTicks() {
        return Math.max(1, plugin.getConfig().getInt("features.targetPreview.cadenceTicks", 4));
    }

    private Particle particle() {
        String name = plugin.getConfig().getString("features.targetPreview.particle", "END_ROD");
        try {
            return Particle.valueOf(name.toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            // A bad particle name must not stop a player aiming. Draw something
            // and say which key is wrong.
            plugin.getLogger().warning("features.targetPreview.particle is not a particle: " + name);
            return Particle.END_ROD;
        }
    }

    /**
     * Mark each block, for this player only.
     *
     * One particle at the block's centre rather than an outline: an outline is
     * twelve edges per block and a run of twenty blocks becomes a haze, while
     * centres stay countable.
     */
    public void draw(Player p, Collection<Block> blocks) {
        if (!enabled() || blocks == null || blocks.isEmpty()) return;
        int count = plugin.getConfig().getInt("features.targetPreview.perBlock", 1);
        for (Block block : blocks) {
            if (block == null) continue;
            Location at = block.getLocation().add(0.5, 0.5, 0.5);
            // Player, not World: the caster sees it and nobody else does.
            p.spawnParticle(particle(), at, count, 0, 0, 0, 0);
        }
    }
}
