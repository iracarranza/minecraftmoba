package com.minecraftmoba.plugin;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.player.PlayerToggleSprintEvent;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Lured and Frightened, enforced.
 *
 * <b>[PROTOTYPE]</b> The statuses come from the unofficial Wayfinder and Hollow classes, which do
 * not exist in the roster; their behaviour here is the owner's description, not a settled design:
 *
 * <ul>
 *   <li><b>Lured</b>: looks at the source and moves toward it, uncontrollably. Cannot sprint,
 *       crouch or use abilities; may jump and attack.
 *   <li><b>Frightened</b>: the same, but walking away from the source (still looking at it).
 * </ul>
 *
 * <h2>How a step is delivered</h2>
 *
 * A step is taken three ways, because a real client, a scripted body and a mob are driven
 * differently:
 *
 * <ul>
 *   <li>The entity is turned to face the source every tick ({@link Entity#setRotation}).
 *   <li>A real player is pushed with a velocity every tick, and any step the client reports is
 *       rewritten in {@link PlayerMoveEvent} so held keys cannot argue with it. Vertical movement
 *       is left alone, which is what lets a lured player jump.
 *   <li>A scripted body is moved by reporting a position, as a client would, and a mob is given a
 *       path.
 * </ul>
 *
 * <h2>Known limits</h2>
 *
 * Sprint and crouch are suppressed, not taken: the client believes it is sprinting until the
 * server says otherwise. A wall stops a step; a lured entity may jump a one-block rise itself
 * but is not made to. Rewriting a client's reported step will rubber-band it, so this is likely
 * to feel jerky. None of that can be judged by a scripted body, only by someone playing.
 *
 * <h2>One forced direction at a time</h2>
 *
 * Applying Lured to a Frightened entity (or the reverse) replaces it; applying the same kind
 * again replaces its source and restarts the clock. That is an assumption, recorded here so it
 * can be overruled, not a decision.
 *
 * NOT VERIFIED beyond the live acceptance run at the time of writing.
 */
public final class ForcedMovement implements Listener {

    /** Delivers a step to something that is not a real client. Returns false when it could not. */
    public interface BodyMover { boolean push(Player body, double dx, double dz, float yaw, float pitch); }

    private record State(ForcedMotion.Kind kind, Supplier<Location> source, long until) {}

    /** [PROTOTYPE] Blocks per tick: about a walking pace. */
    public static final double DEFAULT_SPEED = 0.2158;
    /** [PROTOTYPE] A lured entity stops this far from its source. */
    public static final double DEFAULT_STOP = 1.5;

    private final MobaPlugin plugin;
    private final Map<UUID, State> states = new HashMap<>();
    private BodyMover bodyMover = (p, dx, dz, yaw, pitch) -> false;

    public ForcedMovement(MobaPlugin plugin) { this.plugin = plugin; }

    public void bodyMover(BodyMover mover) { this.bodyMover = mover; }

    /** Start the per-tick driver. */
    public void start() { Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 1L, 1L); }

    private long now() { return plugin.getServer().getCurrentTick(); }
    private double speed() { return plugin.getConfig().getDouble("statuses.forced.speed", DEFAULT_SPEED); }
    private double stop() { return plugin.getConfig().getDouble("statuses.forced.stopDistance", DEFAULT_STOP); }

    // ---- state --------------------------------------------------------------------------------

    /** Apply a forced direction, replacing any existing one. */
    public void apply(ForcedMotion.Kind kind, LivingEntity target, Supplier<Location> source, long ticks) {
        if (target == null || source == null || ticks <= 0) return;
        states.put(target.getUniqueId(), new State(kind, source, now() + ticks));
    }

    public ForcedMotion.Kind kind(Entity e) {
        var s = live(e);
        return s == null ? null : s.kind();
    }

    public long remaining(Entity e) {
        var s = live(e);
        return s == null ? 0 : Math.max(0, s.until() - now());
    }

    public void clear(Entity e) { if (e != null) states.remove(e.getUniqueId()); }

    public void reset() { states.clear(); }

    /** A forced entity may not use abilities, sprint or crouch. */
    public boolean refusesAbilities(Entity e) { return live(e) != null; }

    private State live(Entity e) {
        if (e == null) return null;
        var s = states.get(e.getUniqueId());
        if (s == null) return null;
        if (s.until() <= now()) { states.remove(e.getUniqueId()); return null; }
        return s;
    }

    // ---- the step -----------------------------------------------------------------------------

    private ForcedMotion.Step stepFor(State s, LivingEntity e, Location feet) {
        Location source = s.source().get();
        if (source == null || source.getWorld() == null || !source.getWorld().equals(feet.getWorld())) return null;
        Location eye = feet.clone().add(0, e.getEyeHeight(), 0);
        return ForcedMotion.step(s.kind(),
                new double[]{eye.getX(), eye.getY(), eye.getZ()},
                new double[]{feet.getX(), feet.getY(), feet.getZ()},
                new double[]{source.getX(), source.getY(), source.getZ()}, speed(), stop());
    }

    /** Whether a solid block stands where the step would take the entity's feet or head. */
    static boolean blocked(LivingEntity e, Location feet, ForcedMotion.Step step) {
        double len = Math.hypot(step.dx(), step.dz());
        if (len < 1e-9) return false;
        double reach = 0.45 / len + 1;          // a step plus the entity's half-width
        Location ahead = feet.clone().add(step.dx() * reach, 0, step.dz() * reach);
        return !ahead.getBlock().isPassable() || !ahead.clone().add(0, 1, 0).getBlock().isPassable();
    }

    private void tick() {
        for (UUID id : Map.copyOf(states).keySet()) {
            Entity raw = Bukkit.getEntity(id);
            var s = states.get(id);
            if (!(raw instanceof LivingEntity e) || raw.isDead() || !raw.isValid() || s == null || s.until() <= now()) {
                states.remove(id);
                continue;
            }
            Location feet = e.getLocation();
            var step = stepFor(s, e, feet);
            if (step == null) { states.remove(id); continue; }
            if (!Float.isNaN(step.yaw())) e.setRotation(step.yaw(), step.pitch());
            if (e instanceof Player p) {
                if (p.isSprinting()) p.setSprinting(false);
                if (p.isSneaking()) p.setSneaking(false);
            }
            if (!step.moves() || movementRefused(e) || blocked(e, feet, step)) continue;
            push(e, step, feet);
        }
    }

    private boolean movementRefused(Entity e) {
        return plugin.stun() != null && plugin.stun().movementRefused(e);
    }

    private void push(LivingEntity e, ForcedMotion.Step step, Location feet) {
        if (e instanceof Mob mob) {
            mob.getPathfinder().moveTo(feet.clone().add(step.dx() * 8, 0, step.dz() * 8));
        } else if (e instanceof Player p && NmsBodies.isBody(p.getUniqueId())) {
            try { bodyMover.push(p, step.dx(), step.dz(), step.yaw(), step.pitch()); }
            catch (IllegalStateException unsettled) { /* a body that has not settled cannot be moved yet */ }
        } else {
            Vector v = e.getVelocity();
            e.setVelocity(new Vector(step.dx(), v.getY(), step.dz()));
        }
    }

    // ---- enforcement against a real client's own input ------------------------------------------

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void move(PlayerMoveEvent e) {
        Player p = e.getPlayer();
        var s = live(p);
        if (s == null || movementRefused(p)) return;
        Location from = e.getFrom(), to = e.getTo();
        if (to == null) return;
        var step = stepFor(s, p, from);
        if (step == null) return;
        Location forced = to.clone();
        if (!Float.isNaN(step.yaw())) { forced.setYaw(step.yaw()); forced.setPitch(step.pitch()); }
        if (step.moves() && !blocked(p, from, step)) {
            forced.setX(from.getX() + step.dx());
            forced.setZ(from.getZ() + step.dz());
        } else {
            forced.setX(from.getX());
            forced.setZ(from.getZ());
        }
        e.setTo(forced);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void sprint(PlayerToggleSprintEvent e) {
        if (e.isSprinting() && live(e.getPlayer()) != null) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void sneak(PlayerToggleSneakEvent e) {
        if (e.isSneaking() && live(e.getPlayer()) != null) e.setCancelled(true);
    }

    @EventHandler public void quit(PlayerQuitEvent e) { clear(e.getPlayer()); }

    /** For a status readout. */
    public String report(Entity e) {
        var s = live(e);
        return s == null ? "FORCED none" : "FORCED " + s.kind() + " remaining=" + remaining(e);
    }
}
