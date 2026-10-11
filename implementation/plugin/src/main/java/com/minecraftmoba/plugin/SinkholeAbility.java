package com.minecraftmoba.plugin;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import java.util.*;

/** Revised temporary Sinkhole: telegraph, vanilla mining delay, zero-drop loan, exact restore. */
final class SinkholeAbility implements Ability {
    private record Selected(Block block, BlockData original) {}
    private record Hole(Player caster, UUID token, List<Selected> blocks, Set<Chunk> chunks, long opens, long closes) {}
    private final MobaPlugin plugin;
    private final ConfigurationSection config;
    private final List<Hole> holes = new ArrayList<>();
    private final Map<Chunk,Integer> tickets = new HashMap<>();
    private long clock;
    SinkholeAbility(MobaPlugin plugin, ConfigurationSection config) { this.plugin = plugin; this.config = config; }
    public String id() { return "sinkhole"; }
    public String displayName() { return "Sinkhole"; }
    public long cooldownTicks() { return config.getLong("cooldownTicks"); }
    public List<AbilityOutput> outputs() {
        return AbilityOutput.single("sinkhole", TargetForm.AREA_BLOCKS, InputForm.INSTANT, Recipients.ENEMIES);
    }
    private List<Block> selection(Player p) {
        Block center = p.getTargetBlockExact(config.getInt("range"));
        if (center == null) return List.of();
        var selected = new ArrayList<Block>(); int radius = config.getInt("radius");
        for (int depth = 0; depth < config.getInt("depth"); depth++)
            for (int x = -radius; x <= radius; x++) for (int z = -radius; z <= radius; z++) {
                int rough = Math.floorMod(x * 7349 + z * 9157 + center.getX() * 31 + center.getZ(), 3);
                if (x*x + z*z > radius*radius - rough) continue;
                Block b = center.getRelative(x, -depth, z);
                if (b.getY() < b.getWorld().getMinHeight() || !b.getChunk().isLoaded() || b.isEmpty()
                        || b.isLiquid() || b.getType().getHardness() < 0 || plugin.displacement().isReserved(b)) continue;
                // Objectives and portal frames remain protected pending the canonical Open rule.
                if (Set.of(Material.END_PORTAL, Material.END_PORTAL_FRAME, Material.END_GATEWAY,
                        Material.SPAWNER, Material.TRIAL_SPAWNER, Material.VAULT).contains(b.getType())) continue;
                selected.add(b);
                if (selected.size() >= config.getInt("blockBudget")) return selected;
            }
        return selected;
    }
    public Collection<Block> preview(Player p, AbilityContext ctx) { return selection(p); }
    public boolean execute(Player p, AbilityContext ctx) {
        var blocks = selection(p); if (blocks.isEmpty()) return false;
        double slowest = 1;
        for (var b : blocks) {
            float progress = b.getBreakSpeed(p);
            if (!Float.isFinite(progress) || progress <= 0) return false;
            slowest = Math.max(slowest, Math.ceil(1.0 / progress));
        }
        var chunks = new HashSet<Chunk>(); var selected = new ArrayList<Selected>();
        for (var b : blocks) { selected.add(new Selected(b, b.getBlockData().clone())); chunks.add(b.getChunk()); }
        for (var c : chunks) { if (tickets.merge(c, 1, Integer::sum) == 1) c.addPluginChunkTicket(plugin); }
        long opens = clock + (long) slowest;
        holes.add(new Hole(p, UUID.randomUUID(), selected, chunks, opens, opens + config.getLong("openTicks")));
        highlight(selected); return true;
    }
    private void highlight(List<Selected> blocks) {
        for (var b : blocks) b.block().getWorld().spawnParticle(Particle.END_ROD,
                b.block().getLocation().add(.5, 1.05, .5), 1, 0, 0, 0, 0);
    }
    public void tick() {
        clock++;
        for (var hole : List.copyOf(holes)) {
            if (!hole.caster().isOnline() || hole.caster().isDead()
                    || !hole.caster().getWorld().equals(hole.blocks().getFirst().block().getWorld())) { restore(hole); continue; }
            if (clock < hole.opens()) { if (clock % 10 == 0) highlight(hole.blocks()); continue; }
            if (clock == hole.opens())
                for (var b : hole.blocks()) if (b.block().getBlockData().equals(b.original())) plugin.displacement().take(b.block(), hole.token());
            if (clock >= hole.closes()) restore(hole);
        }
    }
    private void restore(Hole h) {
        plugin.displacement().restore(h.token()); holes.remove(h);
        for (var c : h.chunks()) if (tickets.computeIfPresent(c, (k,v) -> v == 1 ? null : v - 1) == null) c.removePluginChunkTicket(plugin);
    }
    public void cancel(Player p) { for (var h : List.copyOf(holes)) if (h.caster().equals(p)) restore(h); }
}
