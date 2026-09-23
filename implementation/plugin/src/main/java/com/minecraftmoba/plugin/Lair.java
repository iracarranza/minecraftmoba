package com.minecraftmoba.plugin;

import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.persistence.PersistentDataType;
import java.util.*;

/** Bukkit binding for one explicitly supplied Lair socket. Existing maps do not imply a valid socket. */
public final class Lair implements Listener {
    private final MobaPlugin plugin;
    private final NamespacedKey memberKey;
    private Location site;
    /** Where the occupant was last seen. A Ghast does not stay over its socket. */
    private Location lastKnown;
    /** Occupants the runtime could not reach to remove; cleaned on chunk load. */
    private final Set<UUID> stale = new HashSet<>();
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
    public void bind(World world) { bind(world, null); }

    /**
     * Bind to the claimed realization's Lair, or to the configured socket.
     *
     * The manifest answers first. A generated map carries its own anchor, and
     * `alpha.lair.site` describes the Alpha template -- which is why a
     * generated map used to reach UNCONFIGURED: it was being asked about a
     * socket belonging to a different world.
     */
    public void bind(World world, MapBindings bindings) {
        lifecycle.reset();
        site = null;
        sweepTagged();
        if (bindings != null) {
            Location anchor = bindings.lairAnchor(world);
            if (anchor != null) { site = anchor; return; }
        }
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
        // Then where it actually was, not where it was spawned. A disposable-world
        // smoke test on 2026-09-23 caught this: a Ghast moved 300 blocks from its
        // socket, its chunk unloaded, and `Bukkit.getEntity` returned null while
        // the lifecycle still reported ALIVE. Loading the SOCKET chunk -- the only
        // fallback there used to be -- looks in the one place the occupant is
        // least likely to be once it can fly.
        if (entity == null && id != null && lastKnown != null) {
            lastKnown.getChunk().load();
            entity = Bukkit.getEntity(id);
        }
        if (entity != null) { entity.remove(); return; }
        if (sweepTagged() > 0) return;
        // Unreachable is not gone. An entity in an unloaded chunk is still there,
        // and pretending otherwise is how a replaced Ghast ends up sharing the
        // Lair with the Dragon that replaced it. Remember it, and remove it the
        // moment its chunk comes back.
        if (id != null) {
            stale.add(id);
            plugin.getLogger().info("[lair] occupant " + id + " is in an unloaded chunk; "
                    + "queued for removal on chunk load");
        }
    }

    /**
     * Sweep stale occupants as their chunks come back.
     *
     * This is the half that makes removal durable. The entity is persistent by
     * design -- it must survive a night -- so it cannot be left to vanish on its
     * own, and match state is memory-only, so nothing else would ever look for
     * it again.
     */
    @EventHandler
    public void chunkLoad(ChunkLoadEvent e) {
        if (stale.isEmpty()) return;
        for (Entity entity : e.getChunk().getEntities())
            if (stale.remove(entity.getUniqueId())) {
                entity.remove();
                plugin.getLogger().info("[lair] removed stale occupant on chunk load");
            }
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
            lastKnown = e.getLocation();
            return e.getUniqueId();
        } catch (RuntimeException failure) {
            plugin.getLogger().warning("Lair manifestation blocked: " + failure.getMessage());
            return null;
        }
    }
    public void onNight(int ordinal) {
        // Refresh the occupant's position before any replacement decision, so a
        // boss that has moved is removed where it is rather than where it began.
        UUID occupant = lifecycle.occupant();
        if (occupant != null) {
            Entity entity = Bukkit.getEntity(occupant);
            if (entity != null) lastKnown = entity.getLocation();
        }
        lifecycle.onNight(ordinal);
    }

    /** Whether the runtime can currently see its own occupant. */
    public boolean occupantReachable() {
        UUID id = lifecycle.occupant();
        return id != null && Bukkit.getEntity(id) != null;
    }

    public int pendingRemovals() { return stale.size(); }
    @EventHandler(priority = EventPriority.MONITOR)
    public void death(EntityDeathEvent e) {
        Player killer = e.getEntity().getKiller();
        Match.Participant p = killer == null ? null : plugin.match().participant(killer.getUniqueId());
        if (lifecycle.killed(e.getEntity().getUniqueId(), p != null && p.alive ? p.team : null)) {
            resolveVictory();
        }
    }
    /**
     * The monster is dead: perform its siege on the paired enemy objective.
     *
     * At the moment of the kill, not at the next Lair turnover. An earlier
     * version compared `lastVictory` across `onNight` and never fired, because
     * replacing an occupant does not change the recorded victory -- the reward
     * was recorded and then silently dropped. Doctrine also puts it here: the
     * monster performs the siege after being defeated.
     *
     * Public so a test harness can drive a kill it staged itself.
     */
    public String resolveVictory() {
        var victory = lifecycle.lastVictory();
        if (victory == null || victory.victoriousTeam() == null) {
            plugin.getLogger().info("[lair] occupant defeated, attribution unknown; "
                    + "no siege performed rather than guessing a team");
            return null;
        }
        if (plugin.match() == null || !plugin.match().running()) return null;
        String result = plugin.match().lairAssault(victory.boss(), victory.victoriousTeam());
        plugin.getLogger().info("[lair] " + victory + " -> " + result);
        return result;
    }

    /** Drop the occupant and the socket. Called before the world is replaced. */
    public void reset() {
        lifecycle.reset(); sweepTagged(); site = null; lastKnown = null;
        // Queued removals do not survive a match: the world itself is replaced.
        stale.clear();
    }
    public String report() {
        return "lair=" + lifecycle.state()
                + (lifecycle.state() == LairLifecycle.State.ALIVE && !occupantReachable()
                   ? " (occupant unloaded)" : "")
                + " pendingRemovals=" + stale.size()
                + " site=" + (site == null ? "UNCONFIGURED" : site.toVector())
                + " scheduled=" + lifecycle.scheduled() + " occupant=" + lifecycle.occupant()
                + " lastVictory=" + lifecycle.lastVictory()
                + " pairedSiege=" + (lifecycle.lastVictory() == null ? "none"
                    : lifecycle.lastVictory().pairedObjective() + " (performed on the kill)");
    }
}
