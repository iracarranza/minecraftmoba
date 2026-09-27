package com.minecraftmoba.plugin;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.projectiles.ProjectileSource;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The shared definition of "in combat".
 *
 * Five designed systems assumed this concept and none defined it: level-up
 * selection, Recall, Health Mastery's out-of-combat Absorption, Shovel
 * Pathfinding and Skeleton Crew's Deadline. Each would otherwise have grown its
 * own threshold, and five private windows cannot be displayed as one state --
 * which §4 of the design makes a requirement, not a preference.
 *
 * See docs/design/COMBAT_STATE.md. The rules that are design rather than
 * implementation detail are restated at the code that enforces them.
 *
 * <h2>Why a stored tick rather than a flag</h2>
 *
 * Pathfinding polls this continuously while a player moves, rather than
 * reacting to an event. A long compared against a deadline is the cheapest
 * thing to ask; a flag with a scheduled reset would mean a task per entity per
 * hit, and would answer more slowly besides.
 *
 * <h2>Why keyed by entity</h2>
 *
 * Deadline's threshold is about a Crew Member being in combat, not its
 * commander. A player-only implementation would be rebuilt the moment Skeleton
 * Crew is implemented, so the primitive is keyed by entity UUID from the start.
 *
 * The arithmetic takes ticks as arguments and holds no reference to the server,
 * so it is testable without one. The Bukkit listener sits on top.
 */
public final class CombatState implements Listener {

    private final MobaPlugin plugin;
    private final Map<UUID, Long> lastCombatTick = new HashMap<>();
    private final long durationTicks;

    /** Server-free constructor, for tests and for callers supplying their own duration. */
    CombatState(MobaPlugin plugin, long durationTicks) {
        this.plugin = plugin;
        this.durationTicks = durationTicks;
    }

    public CombatState(MobaPlugin plugin) {
        this(plugin, plugin.getConfig().getLong("combat.durationTicks"));
    }

    /** How long a qualifying action holds an entity in combat. [PROTOTYPE] */
    public long durationTicks() { return durationTicks; }

    // ---- arithmetic: no server, no Bukkit ---------------------------------

    /** Record a qualifying action at {@code now}. */
    public void mark(UUID entity, long now) { lastCombatTick.put(entity, now); }

    /** Ticks of combat left, or 0 when out of combat. */
    public long remaining(UUID entity, long now) {
        Long last = lastCombatTick.get(entity);
        if (last == null) return 0;
        return Math.max(0, last + durationTicks - now);
    }

    public boolean inCombat(UUID entity, long now) { return remaining(entity, now) > 0; }

    public void clear(UUID entity) { lastCombatTick.remove(entity); }

    /** Drop entries that can no longer be in combat. Keeps the map from growing with the world. */
    public void sweep(long now) {
        lastCombatTick.entrySet().removeIf(e -> e.getValue() + durationTicks <= now);
    }

    int tracked() { return lastCombatTick.size(); }

    // ---- live wrappers ----------------------------------------------------

    private long now() { return plugin.getServer().getCurrentTick(); }

    public boolean inCombat(Entity entity) { return inCombat(entity.getUniqueId(), now()); }

    public long remaining(Entity entity) { return remaining(entity.getUniqueId(), now()); }

    /**
     * Put an entity in combat for activating a combat ability.
     *
     * Called by the ability layer once the activation is classified, so that a
     * context-sensitive ability can report what it actually did rather than
     * carry a fixed answer.
     */
    public void markAbilityActivation(Entity entity) { mark(entity.getUniqueId(), now()); }

    // ---- what counts ------------------------------------------------------

    /**
     * Taking and dealing attack damage, the only two events that set the state.
     *
     * Listening to EntityDamageByEntityEvent rather than EntityDamageEvent is
     * what excludes environmental damage: fall, fire, lava, drowning,
     * suffocation, starvation, cactus and the void never arrive here. The
     * concept is combat, not harm -- burning in lava should not block a Recall,
     * because the lava is already the problem.
     *
     * MONITOR priority and ignoreCancelled: a hit another plugin or a guard
     * cancelled did not land, and did not happen for this purpose either.
     *
     * Note what this deliberately does NOT do: it never walks from a summon to
     * its owner. A summon's combat state does not propagate, and the omission
     * IS the rule -- Skeleton Crew's crew fights more or less continuously, so
     * propagation would mean that commander could never level up, never Recall
     * and never hold an out-of-combat effect during the exact play the class
     * exists to perform.
     *
     * [OPEN] Damage dealt to a passive animal counts here. Punching a cow for
     * Looting is technically an attack, so a Yield build farming animals sits
     * in near-continuous combat and is gated out of its own progression. This
     * is the likeliest exception in the design and the one most worth testing;
     * it is left counting rather than silently excepted.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent e) {
        long now = now();
        if (e.getEntity() instanceof LivingEntity victim) mark(victim.getUniqueId(), now);
        LivingEntity attacker = attacker(e.getDamager());
        if (attacker != null) mark(attacker.getUniqueId(), now);
    }

    /** The living entity responsible for a hit, resolving a projectile to its shooter. */
    private static LivingEntity attacker(Entity damager) {
        if (damager instanceof LivingEntity living) return living;
        if (damager instanceof Projectile projectile) {
            ProjectileSource source = projectile.getShooter();
            if (source instanceof LivingEntity shooter) return shooter;
        }
        return null;
    }

    @EventHandler public void onDeath(EntityDeathEvent e) { clear(e.getEntity().getUniqueId()); }

    @EventHandler public void onQuit(PlayerQuitEvent e) { clear(e.getPlayer().getUniqueId()); }

    public String report(Player p) {
        return "COMBAT durationTicks=" + durationTicks
                + " inCombat=" + inCombat(p)
                + " remainingTicks=" + remaining(p)
                + " tracked=" + tracked();
    }
}
