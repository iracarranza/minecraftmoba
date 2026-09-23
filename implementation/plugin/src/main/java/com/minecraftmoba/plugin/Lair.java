package com.minecraftmoba.plugin;

import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.persistence.PersistentDataType;
import java.util.*;

/** Bukkit binding for one explicitly supplied Lair socket. Existing maps do not imply a valid socket. */
public final class Lair implements Listener {
    private final MobaPlugin plugin;
    private final NamespacedKey memberKey;
    private Location site;
    private final LairLifecycle lifecycle;
    public Lair(MobaPlugin plugin) {
        this.plugin = plugin;
        this.memberKey = new NamespacedKey(plugin, "lair_occupant");
        lifecycle = new LairLifecycle(new LairLifecycle.Encounters() {
            public boolean bound() { return site != null; }
            public UUID spawn(OpportunityCadence.Boss boss) { return spawnEncounter(boss); }
            public void remove(UUID id) { removeOccupant(id); }
        });
    }
    public LairLifecycle lifecycle() { return lifecycle; }
    /**
     * Bind the Lair to a freshly loaded match world.
     *
     * The lifecycle is reset -- which removes any occupant -- while {@code site}
     * still points at the OLD world, so cleanup happens before the reference is
     * dropped rather than after. The tagged sweep then catches anything a
     * previous run left behind, because match state is memory-only and a
     * restart mid-match does not resume: an untracked boss from before the
     * restart would otherwise still be standing in the world.
     */
    public void bind(World world) {
        lifecycle.reset();
        site = null;
        sweepTagged();
        var xyz = plugin.getConfig().getDoubleList("alpha.lair.site");
        if (xyz.isEmpty()) return; // explicit OPEN socket; never default to the map centre
        if (xyz.size() != 3 || xyz.stream().anyMatch(v -> !Double.isFinite(v)))
            throw new IllegalArgumentException("alpha.lair.site must be empty or one [x,y,z] socket");
        site = new Location(world, xyz.get(0), xyz.get(1), xyz.get(2));
    }
    /**
     * Remove the recorded occupant, wherever it has got to.
     *
     * Chunk unloading is not death, and a Ghast or a Dragon does not stay over
     * its socket, so loading the socket chunk and asking for the entity is not
     * enough on its own -- a boss that drifted two hundred blocks away would
     * survive its own replacement and the Lair would then hold two occupants.
     * The id lookup is tried first, then the tagged sweep of every loaded world
     * catches anything the id could not resolve, including an entity that
     * outlived a reload.
     */
    private void removeOccupant(UUID id) {
        Entity entity = id == null ? null : Bukkit.getEntity(id);
        if (entity == null && site != null) {
            site.getChunk().load();
            if (id != null) entity = Bukkit.getEntity(id);
        }
        if (entity != null) { entity.remove(); return; }
        sweepTagged();
    }

    /** Remove every entity still carrying this plugin's Lair tag. */
    public int sweepTagged() {
        int removed = 0;
        for (World world : Bukkit.getWorlds())
            for (Entity e : world.getEntities())
                if (e.getPersistentDataContainer().has(memberKey, PersistentDataType.STRING)) {
                    e.remove(); removed++;
                }
        return removed;
    }

    private UUID spawnEncounter(OpportunityCadence.Boss boss) {
        try {
            site.getChunk().load();
            EntityType type = switch (boss) {
                case GIANT -> EntityType.GIANT;
                case GHAST -> EntityType.GHAST;
                case ENDER_DRAGON -> EntityType.ENDER_DRAGON;
            };
            Entity e = site.getWorld().spawnEntity(site, type);
            e.setPersistent(true);
            if (e instanceof LivingEntity living) living.setRemoveWhenFarAway(false);
            e.getPersistentDataContainer().set(memberKey, PersistentDataType.STRING, boss.name());
            return e.getUniqueId();
        } catch (RuntimeException failure) {
            plugin.getLogger().warning("Lair manifestation blocked: " + failure.getMessage());
            return null;
        }
    }
    public void onNight(int ordinal) { lifecycle.onNight(ordinal); }
    @EventHandler(priority = EventPriority.MONITOR)
    public void death(EntityDeathEvent e) {
        Player killer = e.getEntity().getKiller();
        Match.Participant p = killer == null ? null : plugin.match().participant(killer.getUniqueId());
        if (lifecycle.killed(e.getEntity().getUniqueId(), p != null && p.alive ? p.team : null)) {
            plugin.getLogger().info("[lair] " + lifecycle.lastVictory()
                    + "; paired siege advantage and boss XP unresolved (no custom award)");
        }
    }
    /** Drop the occupant and the socket. Called before the world is replaced. */
    public void reset() { lifecycle.reset(); sweepTagged(); site = null; }
    public String report() {
        return "lair=" + lifecycle.state() + " site=" + (site == null ? "UNCONFIGURED" : site.toVector())
                + " scheduled=" + lifecycle.scheduled() + " occupant=" + lifecycle.occupant()
                + " lastVictory=" + lifecycle.lastVictory() + " siegeAdvantage=UNRESOLVED";
    }
}
