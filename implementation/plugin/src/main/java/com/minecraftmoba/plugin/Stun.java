package com.minecraftmoba.plugin;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Stun, and the enforcement Root never had.
 *
 * <h2>Root was declared and enforced nowhere</h2>
 *
 * {@link ToolboxStatuses} has owned Root since the Utility Belt shipped, and
 * nothing in the plugin read {@code isRooted} or {@code rootRemaining}. A
 * status with no enforcement is not a weak status, it is an absent one, and it
 * is invisible in exactly the way that survives review: the code that sets it
 * is correct, tested, and pointless. Both statuses are enforced here so that
 * cannot happen separately to each of them again.
 *
 * <h2>What a Stun is, precisely</h2>
 *
 * Root plus the actions. A stunned entity cannot move, attack, use, place,
 * break, consume or drop, and a stunned player's ability inputs are refused by
 * {@link AbilityInputs}.
 *
 * <h2>What a Stun is not: camera control</h2>
 *
 * [TECHNICAL RISK] <b>A stunned player can still look around.</b> {@code
 * org.bukkit.Input} carries movement keys only; there is no attack or use
 * signal to suppress, and the client always sends the click. Cancelling the
 * resulting events recovers every consequence except the camera, which cannot
 * be taken. This is the same wall the Waxer capability audit hit with Amber,
 * recorded in classes.md section 18.
 *
 * Retained look is a property of Stun in this project, not a defect to be
 * worked around later. It is also arguably better for long Stuns -- Bloodmason's
 * Anatomb completion Stun scales with missing Health and can be very long, and
 * a player who can watch what is happening to them is being told more than one
 * whose camera is frozen.
 *
 * <h2>Movement is refused by rewinding position, not by cancelling the event</h2>
 *
 * Cancelling {@link PlayerMoveEvent} outright would also refuse the look
 * change carried in the same event, since Minecraft sends both together. The
 * destination is instead rewritten to the origin's POSITION with the event's
 * own yaw and pitch -- so the head turns and the feet do not.
 */
public final class Stun implements Listener {

    private final MobaPlugin plugin;
    private final Map<UUID, Long> stunned = new HashMap<>();

    public Stun(MobaPlugin plugin) { this.plugin = plugin; }

    private long now() { return plugin.getServer().getCurrentTick(); }

    /**
     * Apply a Stun, taking the LONGER of the existing and the new expiry.
     *
     * Not refreshing, and not stacking. Refreshing would let a short Stun
     * shorten a long one applied a tick earlier -- Bloodmason's Anatomb Stun
     * cut short by an incidental Crater is a strictly wrong outcome, and it is
     * the one a naive {@code put} produces.
     */
    public void stun(LivingEntity target, long ticks) {
        if (target == null || ticks <= 0) return;
        long until = now() + ticks;
        stunned.merge(target.getUniqueId(), until, Stun::extend);
        // A mob has no input to refuse, so its AI is what has to stop.
        if (target instanceof Mob mob) mob.setAI(false);
    }

    /**
     * Combine an existing expiry with a new one: the later wins.
     *
     * Deliberately neither refresh nor stack. Refreshing lets a short Stun
     * SHORTEN a long one applied a tick earlier, which is how an incidental
     * Crater would cut Bloodmason's Anatomb Stun down to half a second.
     * Stacking would let two ordinary Stuns produce a lockout neither was
     * balanced to give.
     */
    static long extend(long existingUntil, long candidateUntil) {
        return Math.max(existingUntil, candidateUntil);
    }

    public boolean isStunned(Entity target) {
        if (target == null) return false;
        Long until = stunned.get(target.getUniqueId());
        if (until == null) return false;
        if (until > now()) return true;
        release(target);
        return false;
    }

    public long remaining(Entity target) {
        if (target == null) return 0;
        Long until = stunned.get(target.getUniqueId());
        return until == null ? 0 : Math.max(0, until - now());
    }

    private void release(Entity target) {
        stunned.remove(target.getUniqueId());
        if (target instanceof Mob mob) mob.setAI(true);
    }

    /** Whether this entity may not act at all -- a Stun, not a Root. */
    public boolean actionsRefused(Entity target) { return isStunned(target); }

    /**
     * Whether this entity's movement is refused, by either status.
     *
     * The one place the two statuses agree, and the reason they share a
     * listener: Root negates movement input, and a Stun is a Root that also
     * takes the actions.
     */
    public boolean movementRefused(Entity target) {
        if (isStunned(target)) return true;
        var statuses = plugin.toolboxStatuses();
        return statuses != null && statuses.isRooted(target);
    }

    // ---- enforcement ------------------------------------------------------

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void move(PlayerMoveEvent e) {
        if (!movementRefused(e.getPlayer())) return;
        Location from = e.getFrom(), to = e.getTo();
        if (from.getX() == to.getX() && from.getY() == to.getY() && from.getZ() == to.getZ()) return;
        // Position rewound, look preserved: turning the head is not moving.
        Location held = from.clone();
        held.setYaw(to.getYaw());
        held.setPitch(to.getPitch());
        e.setTo(held);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void interact(PlayerInteractEvent e) {
        if (actionsRefused(e.getPlayer())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void attack(EntityDamageByEntityEvent e) {
        // The DAMAGER is refused. A stunned entity being hit is the point of
        // stunning it, so the victim is deliberately not checked.
        if (actionsRefused(e.getDamager())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void breakBlock(BlockBreakEvent e) {
        if (actionsRefused(e.getPlayer())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void placeBlock(BlockPlaceEvent e) {
        if (actionsRefused(e.getPlayer())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void consume(PlayerItemConsumeEvent e) {
        if (actionsRefused(e.getPlayer())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void drop(PlayerDropItemEvent e) {
        if (actionsRefused(e.getPlayer())) e.setCancelled(true);
    }

    @EventHandler public void quit(PlayerQuitEvent e) { release(e.getPlayer()); }

    /**
     * Drop everything, for a match reset.
     *
     * Mobs have their AI restored on the way out rather than being left frozen
     * for the next match in a reused world.
     */
    public void reset() {
        for (UUID id : Map.copyOf(stunned).keySet()) {
            Entity e = plugin.getServer().getEntity(id);
            if (e instanceof Mob mob) mob.setAI(true);
        }
        stunned.clear();
    }

    /** For a status readout, alongside the Toolbox statuses. */
    public String report(Player p) {
        return "STUN stunned=" + isStunned(p) + " remaining=" + remaining(p)
             + " movementRefused=" + movementRefused(p)
             + " (camera look is NOT taken; see the class doc)";
    }
}
