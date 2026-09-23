package com.minecraftmoba.plugin;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * Mark the renewable resources themselves, not the region they sit in.
 *
 * The previous marker drew a ring of particles at the source's radius -- a
 * forty-block circle at a single fixed height. It answered "a renewable source
 * is somewhere around here", which is the wrong question twice over. Animals
 * move, so a fixed ring stops describing the herd the moment it wanders; and
 * what a player needs to recognise is the sheep, not the pen.
 *
 * So:
 *
 *   ANIMAL / SWARM   the member entities glow. Vanilla's glow is a silhouette
 *                    that tracks the entity, which is exactly the property a
 *                    moving resource needs and one no particle emitter has.
 *   CROP             a particle at each qualifying BLOCK. Blocks cannot glow,
 *                    and marking the individual plants is still much closer to
 *                    the resource than a boundary ring is.
 *
 * Depleted sources mark nothing, so the marking means "this is available", not
 * merely "this exists". That is the same rule the ring had and the only part of
 * it worth keeping.
 *
 * Two costs are managed rather than ignored. Crop membership is found by
 * scanning the source volume, which is O(radius^3) and far too expensive per
 * tick, so it is cached and rescanned slowly. And nothing is marked at all
 * unless a player is near enough to see it.
 *
 * OPEN DESIGN QUESTION, flagged rather than decided: vanilla glow renders
 * through terrain, for every player, on both teams. For Alpha legibility that
 * is a feature; for a competitive match it means a herd is public information
 * the moment it exists. Making it per-viewer needs packet-level work, which
 * this does not attempt.
 *
 * Appearance is deliberately plain. Particle and glow colour are a design
 * decision, and this file picks the least opinionated thing that reads.
 */
public final class RenewableMarkers {
    private final MobaPlugin plugin;
    /** Entities this class made glow, so it never clears someone else's glow. */
    private final Set<UUID> marked = new HashSet<>();
    /** Cached crop block positions per source; rescanning every tick is not affordable. */
    private final Map<String, List<Block>> crops = new HashMap<>();
    private long lastScan;

    public RenewableMarkers(MobaPlugin plugin) {
        this.plugin = plugin;
        long period = plugin.getConfig().getLong("features.renewableMarkers.ticks", 20L);
        if (period > 0) Bukkit.getScheduler().runTaskTimer(plugin, this::tick, period, period);
    }

    public boolean enabled() {
        return plugin.getConfig().getBoolean("features.renewableMarkers.enabled");
    }

    private double viewRadius() {
        return plugin.getConfig().getDouble("features.renewableMarkers.viewRadius", 64.0);
    }

    private void tick() {
        var renewables = plugin.renewables();
        if (!enabled() || renewables == null) { clearAll(); return; }

        long rescan = plugin.getConfig().getLong("features.renewableMarkers.rescanTicks", 200L);
        boolean doScan = rescan > 0 && (System.currentTimeMillis() - lastScan) / 50 >= rescan;
        if (doScan) { crops.clear(); lastScan = System.currentTimeMillis(); }

        Set<UUID> stillMarked = new HashSet<>();
        for (var source : renewables.sources()) {
            World w = Bukkit.getWorld(source.world());
            if (w == null || !w.isChunkLoaded(source.x() >> 4, source.z() >> 4)) continue;
            Location centre = new Location(w, source.x() + 0.5, source.y() + 0.5, source.z() + 0.5);
            if (!anyoneNear(w, centre, viewRadius() + source.radius())) continue;
            // A depleted source marks nothing: the mark means "available".
            if (renewables.available(source) <= 0) continue;

            var kind = source.kind() == null ? null : RenewableKinds.require(source.kind());
            if (kind == null) continue;
            if (kind.type() == Renewables.Type.CROP) markCrops(w, source, kind, doScan);
            else markFauna(w, source, kind, stillMarked);
        }
        // Anything that was glowing and is no longer a member -- harvested,
        // wandered out, or its source depleted -- stops glowing.
        for (UUID id : new ArrayList<>(marked)) {
            if (stillMarked.contains(id)) continue;
            Entity e = Bukkit.getEntity(id);
            if (e != null) e.setGlowing(false);
            marked.remove(id);
        }
    }

    private boolean anyoneNear(World w, Location centre, double radius) {
        double r2 = radius * radius;
        for (Player p : w.getPlayers())
            if (p.getLocation().distanceSquared(centre) <= r2) return true;
        return false;
    }

    private void markFauna(World w, Renewables.Source s, RenewableKinds.Kind kind, Set<UUID> keep) {
        Location centre = new Location(w, s.x() + 0.5, s.y() + 0.5, s.z() + 0.5);
        // Membership, not type-in-radius: a player's own livestock standing in
        // the region is their economy and must not be marked as a wild herd.
        for (Entity e : w.getNearbyEntities(centre, s.radius(), s.radius(), s.radius())) {
            if (!plugin.renewables().isMember(e, s) || !(e instanceof LivingEntity)) continue;
            keep.add(e.getUniqueId());
            if (marked.add(e.getUniqueId()) || !e.isGlowing()) e.setGlowing(true);
        }
    }

    private void markCrops(World w, Renewables.Source s, RenewableKinds.Kind kind, boolean rescan) {
        List<Block> cached = crops.get(s.id());
        if (cached == null) {
            if (!rescan && crops.containsKey(s.id())) return;
            cached = scanCrops(w, s, kind);
            crops.put(s.id(), cached);
        }
        for (Block b : cached) {
            if (!kind.blocks().contains(b.getType())) continue;   // harvested since the scan
            w.spawnParticle(Particle.HAPPY_VILLAGER,
                    b.getX() + 0.5, b.getY() + 0.6, b.getZ() + 0.5, 1, 0.15, 0.15, 0.15, 0);
        }
    }

    /** O(radius^3) and therefore run on the slow rescan, never per tick. */
    private List<Block> scanCrops(World w, Renewables.Source s, RenewableKinds.Kind kind) {
        var found = new ArrayList<Block>();
        for (int x = s.x() - s.radius(); x <= s.x() + s.radius(); x++)
            for (int y = s.y() - s.radius(); y <= s.y() + s.radius(); y++)
                for (int z = s.z() - s.radius(); z <= s.z() + s.radius(); z++) {
                    Block b = w.getBlockAt(x, y, z);
                    if (kind.blocks().contains(b.getType())) found.add(b);
                }
        return found;
    }

    /** Used when the feature is switched off, so nothing is left glowing. */
    public void clearAll() {
        for (UUID id : marked) {
            Entity e = Bukkit.getEntity(id);
            if (e != null) e.setGlowing(false);
        }
        marked.clear();
        crops.clear();
    }

    public void reset() { clearAll(); lastScan = 0; }

    public String report() {
        return "RENEWABLE_MARKERS enabled=" + enabled() + " glowing=" + marked.size()
                + " cachedCropPatches=" + crops.size() + " viewRadius=" + viewRadius();
    }
}
