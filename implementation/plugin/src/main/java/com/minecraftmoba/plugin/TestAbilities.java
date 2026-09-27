package com.minecraftmoba.plugin;

import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.scheduler.BukkitRunnable;
import java.util.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Animals;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

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
                case "lunge" -> lunge(p, ctx);
                case "sinkhole_lite" -> sinkhole(p, ctx);
                case "channel_ult" -> { ctx.inputs().channel(p, config.getLong("channelTicks"), config.getDouble("movementThreshold")); yield true; }
                default -> throw new IllegalStateException("Unknown test ability");
            };
        }
        public Map<String, String> branches() {
            return id.equals("lunge") ? Map.of("swarming_bite", "Swarming Bite", "thieving_swipe", "Thieving Swipe", "stalking_pounce", "Stalking Pounce") : Map.of();
        }
        public List<String> branchIds() {
            return id.equals("lunge") ? List.of("swarming_bite", "thieving_swipe", "stalking_pounce") : List.of();
        }
        private boolean lunge(Player p, AbilityContext ctx) {
            String branch = ctx.branchFor(id);
            if ("stalking_pounce".equals(branch)) {
                int windup = config.getInt("branch" + "es.stalkingPounce.windupTicks");
                p.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, windup + 2, 4, false, false, true));
                p.sendActionBar(Component.text("Stalking Pounce — coiling..."));
                new BukkitRunnable() {
                    @Override public void run() {
                        if (p.isOnline() && !p.isDead()) {
                            p.removePotionEffect(PotionEffectType.SLOWNESS);
                            leap(p, branchDouble("stalkingPounce", "power"));
                        }
                    }
                }.runTaskLater(ctx.plugin(), windup);
                return true;
            }
            double power = config.getDouble("power");
            if ("swarming_bite".equals(branch)) power += Math.min(nearby(p, "WOLF"), 4) * branchDouble("swarmingBite", "perWolfPower");
            if ("thieving_swipe".equals(branch)) power = branchDouble("thievingSwipe", "power");
            if ("stalking_pounce".equals(branch)) power = branchDouble("stalkingPounce", "power");
            leap(p, power);
            sweep(p, ctx, branch);
            return true;
        }
        private void leap(Player p, double power) {
            p.setVelocity(p.getVelocity().add(p.getLocation().getDirection().multiply(power)));
        }
        /**
         * Damage what the leap passes through, each target once.
         *
         * Ported from codex/lightfooted-from-phase1. The previous version
         * ray-traced ONCE, at cast time, before the leap had moved the player
         * anywhere -- so it could only hit something already standing in front
         * of them, and the leap itself did nothing. The ability was a jump with
         * an unrelated poke attached.
         *
         * Following the player for the leap's duration makes the travel the
         * attack, which is what a lunge is. That branch did it by teleporting
         * the player along a computed path; this follows the velocity instead,
         * so knockback, blocks and the player's own momentum still apply and
         * the leap does not fight the physics it is made of.
         *
         * The per-target dedup is the load-bearing part. Without it a target
         * standing in the path is damaged once per tick of the sweep -- the
         * ability's damage would be a function of how long someone stayed
         * inside it, which is a multi-second stunlock rather than a lunge.
         */
        private void sweep(Player p, AbilityContext ctx, String branch) {
            double radius = config.getDouble("damageRadius");
            double damage = config.getDouble("damage");
            int ticks = config.getInt("sweepTicks");
            var struck = new HashSet<UUID>();
            new BukkitRunnable() {
                int elapsed;
                @Override public void run() {
                    if (elapsed++ >= ticks || !p.isOnline() || p.isDead()) { cancel(); return; }
                    for (Entity e : p.getNearbyEntities(radius, radius, radius)) {
                        if (!(e instanceof LivingEntity target) || target.equals(p) || target.isDead()) continue;
                        if (friendly(p, target) || !struck.add(target.getUniqueId())) continue;
                        target.damage(damage, p);
                        if ("thieving_swipe".equals(branch) && target instanceof Player victim) {
                            var item = victim.getInventory().getItemInMainHand();
                            if (!item.getType().isAir())
                                victim.setCooldown(item.getType(), branchInt("thievingSwipe", "disableTicks"));
                        }
                    }
                }
            }.runTaskTimer(ctx.plugin(), 0L, 1L);
        }
        /** Same-team players are never swept. Everything else, including mobs, is fair. */
        private boolean friendly(Player source, LivingEntity target) {
            if (!(target instanceof Player other)) return false;
            var board = Bukkit.getScoreboardManager() == null ? null : Bukkit.getScoreboardManager().getMainScoreboard();
            if (board == null) return false;
            var team = board.getEntryTeam(source.getName());
            return team != null && team.equals(board.getEntryTeam(other.getName()));
        }
        private double branchDouble(String branch, String key) { return config.getDouble("branch" + "es." + branch + "." + key); }
        private int branchInt(String branch, String key) { return config.getInt("branch" + "es." + branch + "." + key); }
        private int nearby(Player p, String type) {
            int count = 0;
            for (Entity e : p.getNearbyEntities(config.getDouble("animalRadius"), config.getDouble("animalRadius"), config.getDouble("animalRadius")))
                if (e.getType().name().equals(type) && e instanceof Animals) count++;
            return count;
        }
        private boolean sinkhole(Player p, AbilityContext ctx) {
            var hit = p.rayTraceBlocks(config.getDouble("rayDistance"), FluidCollisionMode.NEVER);
            if (hit == null || hit.getHitBlock() == null) return false;
            Block center = hit.getHitBlock();
            double radius = config.getDouble("radius");
            int reach = (int)Math.ceil(radius);
            var pending = new ArrayDeque<Block>();
            int excludedAtCollection = 0;
            for (int dy = -reach; dy <= reach; dy++) for (int dx = -reach; dx <= reach; dx++) for (int dz = -reach; dz <= reach; dz++) {
                if ((double)dx*dx + (double)dy*dy + (double)dz*dz > radius*radius) continue;
                int x=center.getX()+dx, y=center.getY()+dy, z=center.getZ()+dz;
                World w = center.getWorld();
                if (y < w.getMinHeight() || y >= w.getMaxHeight() || !w.isChunkLoaded(x >> 4, z >> 4)) continue;
                Block b = w.getBlockAt(x,y,z);
                if (b.getType().isAir()) continue;
                if (ctx.provenance().isPlayerPlaced(b)) { excludedAtCollection++; continue; }
                pending.add(b);
            }
            if (pending.isEmpty()) return false;
            final int playerPlacedExcluded = excludedAtCollection;
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
                        // playerPlacedExcluded: player-placed blocks skipped when the volume was collected.
                        // newlyPlacedSpared: blocks that became player-placed during the staged collapse.
                        ctx.plugin().getLogger().info("SinkholeLite removed=" + removed
                            + " playerPlacedExcluded=" + playerPlacedExcluded
                            + " newlyPlacedSpared=" + spared);
                        cancel();
                    }
                }
            }.runTaskTimer(ctx.plugin(), config.getLong("stageTicks"), config.getLong("stageTicks"));
            return true;
        }
    }
}
