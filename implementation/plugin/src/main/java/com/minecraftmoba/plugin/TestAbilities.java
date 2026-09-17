package com.minecraftmoba.plugin;

import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.scheduler.BukkitRunnable;
import java.util.*;

final class TestAbilities {
    private TestAbilities() {}
    static Map<String, Ability> create(MobaPlugin p) {
        var result = new HashMap<String, Ability>();
        for (String id : List.of("lunge", "sinkhole_lite", "channel_ult")) {
            var c = Objects.requireNonNull(p.getConfig().getConfigurationSection("abilities.definitions." + id));
            result.put(id, new Configured(id, c));
        }
        return Map.copyOf(result);
    }
    private record Configured(String id, ConfigurationSection config) implements Ability {
        Configured {
            if (config.getLong("cooldownTicks") < 0) throw new IllegalArgumentException("Negative cooldown: " + id);
        }
        public String displayName() { return Objects.requireNonNull(config.getString("displayName")); }
        public long cooldownTicks() { return config.getLong("cooldownTicks"); }
        public boolean execute(Player p, AbilityContext ctx) {
            return switch (id) {
                case "lunge" -> {
                    p.setVelocity(p.getVelocity().add(p.getLocation().getDirection().multiply(config.getDouble("power"))));
                    yield true;
                }
                case "sinkhole_lite" -> sinkhole(p, ctx);
                case "channel_ult" -> { ctx.inputs().channel(p, config.getLong("channelTicks"), config.getDouble("movementThreshold")); yield true; }
                default -> throw new IllegalStateException("Unknown test ability");
            };
        }
        private boolean sinkhole(Player p, AbilityContext ctx) {
            var hit = p.rayTraceBlocks(config.getDouble("rayDistance"), FluidCollisionMode.NEVER);
            if (hit == null || hit.getHitBlock() == null) return false;
            Block center = hit.getHitBlock();
            double radius = config.getDouble("radius");
            int reach = (int)Math.ceil(radius);
            var pending = new ArrayDeque<Block>();
            for (int dy = -reach; dy <= reach; dy++) for (int dx = -reach; dx <= reach; dx++) for (int dz = -reach; dz <= reach; dz++) {
                if ((double)dx*dx + (double)dy*dy + (double)dz*dz > radius*radius) continue;
                int x=center.getX()+dx, y=center.getY()+dy, z=center.getZ()+dz;
                World w = center.getWorld();
                if (y < w.getMinHeight() || y >= w.getMaxHeight() || !w.isChunkLoaded(x >> 4, z >> 4)) continue;
                Block b = w.getBlockAt(x,y,z);
                if (!b.getType().isAir() && !ctx.provenance().isPlayerPlaced(b)) pending.add(b);
            }
            if (pending.isEmpty()) return false;
            new BukkitRunnable() {
                int spared, removed;
                @Override public void run() {
                    for (int i = 0; i < config.getInt("blocksPerStage") && !pending.isEmpty(); i++) {
                        Block b = pending.remove();
                        if (!b.getWorld().isChunkLoaded(b.getX() >> 4, b.getZ() >> 4)) continue;
                        if (ctx.provenance().isPlayerPlaced(b)) { spared++; continue; }
                        // Do not destroy container contents; never call breakNaturally/dropItem.
                        if (b.getState() instanceof InventoryHolder) continue;
                        if (b.getType().isAir()) continue;
                        b.setType(Material.AIR, false); removed++;
                    }
                    if (pending.isEmpty()) {
                        ctx.plugin().getLogger().info("SinkholeLite removed=" + removed + " newlyPlacedSpared=" + spared);
                        cancel();
                    }
                }
            }.runTaskTimer(ctx.plugin(), config.getLong("stageTicks"), config.getLong("stageTicks"));
            return true;
        }
    }
}
