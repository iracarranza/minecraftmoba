package com.minecraftmoba.plugin;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import java.util.*;

/** Sustained, velocity-driven Mole tunneling. The player moves through space; the ability only clears ahead. */
final class TunnelingAbility implements Ability {
    private final MobaPlugin plugin;
    private final ConfigurationSection config;
    private final Map<UUID, State> active = new HashMap<>();
    TunnelingAbility(MobaPlugin plugin, ConfigurationSection config) {
        this.plugin = plugin; this.config = config;
        new BukkitRunnable() { public void run() { tick(); } }.runTaskTimer(plugin, 1, 1);
    }
    public String id() { return "tunneling"; }
    public String displayName() { return Objects.requireNonNull(config.getString("displayName")); }
    public long cooldownTicks() { return config.getLong("cooldownTicks"); }
    public Map<String,String> branches() { return Map.of("bore", "Bore", "gallery", "Gallery", "dig_in", "Dig In"); }
    public List<String> branchIds() { return List.of("bore", "gallery", "dig_in"); }
    public boolean active(Player p) { return active.containsKey(p.getUniqueId()); }
    public boolean execute(Player p, AbilityContext ctx) {
        if (active(p)) return true;
        var hit = p.rayTraceBlocks(config.getDouble("targetDistance"), FluidCollisionMode.NEVER);
        Vector direction = hit == null || hit.getHitBlockFace() == null
            ? quantize(p.getLocation().getDirection()) : hit.getHitBlockFace().getDirection();
        if (direction.lengthSquared() == 0) return false;
        direction.normalize();
        String branch = ctx.branchFor(id);
        State state = new State(p.getLocation().clone(), direction, branch, p.getLocation().clone(), 0);
        active.put(p.getUniqueId(), state);
        p.sendMessage("Tunneling committed. Move to cancel.");
        return true;
    }
    public void cancel(Player p) { State s = active.remove(p.getUniqueId()); if (s != null) p.setVelocity(new Vector()); }
    private void tick() {
        for (UUID id : new ArrayList<>(active.keySet())) {
            Player p = plugin.getServer().getPlayer(id); State s = active.get(id);
            if (p == null || p.isDead() || s == null) { if (p != null) cancel(p); continue; }
            double max = config.getDouble("maxDistance");
            if (s.progress >= max) { cancel(p); continue; }
            if (!"dig_in".equals(s.branch) && externallyDisplaced(p, s)) { p.sendMessage("Tunneling interrupted."); cancel(p); continue; }
            if (!clearAhead(p, s)) { p.sendMessage("Tunneling stopped by impassable terrain."); cancel(p); continue; }
            double speed = config.getDouble("speed") * ("bore".equals(s.branch) ? config.getDouble("branches.bore.speedMultiplier") : 1.0);
            p.setVelocity(s.direction.clone().multiply(speed));
            s.progress += speed;
            s.lastLocation = p.getLocation().clone();
        }
    }
    private boolean externallyDisplaced(Player p, State s) {
        Vector moved = p.getLocation().toVector().subtract(s.lastLocation.toVector());
        return moved.lengthSquared() > config.getDouble("interruptDisplacementSquared")
            && moved.clone().subtract(s.direction.clone().multiply(moved.dot(s.direction))).lengthSquared()
                > config.getDouble("interruptOffPathSquared");
    }
    private boolean clearAhead(Player p, State s) {
        int width = "gallery".equals(s.branch) ? config.getInt("branches.gallery.width") : config.getInt("width");
        int height = config.getInt("height");
        Location center = p.getLocation().clone().add(s.direction.clone().multiply(config.getDouble("clearAhead")));
        Vector side = Math.abs(s.direction.getY()) > 0.5 ? new Vector(1,0,0) : new Vector(-s.direction.getZ(),0,s.direction.getX()).normalize();
        ItemStack tool = p.getInventory().getItemInMainHand();
        List<Block> blocks = new ArrayList<>();
        for (int w = -(width/2); w <= width/2; w++) for (int h = 0; h < height; h++) {
            Location at = center.clone().add(side.clone().multiply(w)).add(0, h, 0);
            Block b = at.getBlock();
            if (b.getType().isAir()) continue;
            if (b.getType().getHardness() < 0 || plugin.provenance().isPlayerPlaced(b)) return false;
            blocks.add(b);
        }
        for (Block b : blocks) if (!b.breakNaturally(tool)) return false;
        return true;
    }
    private Vector quantize(Vector v) {
        if (Math.abs(v.getY()) > Math.max(Math.abs(v.getX()), Math.abs(v.getZ()))) return new Vector(0, Math.signum(v.getY()), 0);
        return new Vector(Math.signum(v.getX()), 0, Math.signum(v.getZ())).normalize();
    }
    private static final class State {
        final Location origin; final Vector direction; final String branch; Location lastLocation; double progress;
        State(Location origin, Vector direction, String branch, Location lastLocation, double progress) { this.origin=origin; this.direction=direction; this.branch=branch; this.lastLocation=lastLocation; this.progress=progress; }
    }
}
