package com.minecraftmoba.plugin;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * The state Daredevil's three abilities share: momentum, airtime, and the two
 * damage exemptions the kit grants.
 *
 * Ported from {@code codex/lightfooted-from-phase1}, where it lived inside a
 * single {@code DaredevilMechanics} listener that also implemented the
 * abilities and read a server-wide branch key. Split because the abilities
 * belong on the {@link Ability} contract -- cooldowns, branches per player,
 * combat declarations, the notice on refusal -- and none of that is available
 * to a bare listener.
 *
 * <h2>Momentum is measured, not inferred</h2>
 *
 * {@code getVelocity()} on a walking player is not what a player means by
 * momentum: the server's value for ordinary ground movement is close to zero
 * because the client moves itself. The distance actually covered between two
 * move events is the honest measure, so that is what Runway's threshold reads.
 *
 * <h2>Two exemptions, deliberately different</h2>
 *
 * A pending fall-damage negation is <b>one instance of one cause</b>, granted
 * by Crash Landing and consumed by the next fall. Deathly Clutches negates the
 * next N damage instances of <b>any</b> cause, which is what classes.md
 * specifies -- not "the next N lethal hits". Chip damage spending a clutch is
 * the ability working.
 */
public final class DaredevilState implements Listener {

    private final MobaPlugin plugin;

    private final Map<UUID, Vector> lastStep = new HashMap<>();
    private final Map<UUID, Integer> airborne = new HashMap<>();
    private final Set<UUID> pendingFallNegation = new HashSet<>();
    private final Map<UUID, Integer> clutches = new HashMap<>();
    private final Map<UUID, Integer> clutchRestoresLeft = new HashMap<>();
    private final Map<UUID, Integer> clutchMaximum = new HashMap<>();

    public DaredevilState(MobaPlugin plugin) { this.plugin = plugin; }

    public boolean isDaredevil(Player p) {
        if (!plugin.enrolled(p)) return false;
        var d = plugin.data(p);
        return d != null && "daredevil".equals(d.classId);
    }

    /** Horizontal distance covered in the last movement tick. */
    public double momentum(Player p) {
        Vector step = lastStep.get(p.getUniqueId());
        return step == null ? 0 : step.clone().setY(0).length();
    }

    public int airborneTicks(Player p) { return airborne.getOrDefault(p.getUniqueId(), 0); }

    /** Vertical speed downward, which is what Crash Landing spends. */
    public double descent(Player p) { return Math.max(0, -p.getVelocity().getY()); }

    public static boolean qualifies(double momentum, double threshold) { return momentum >= threshold; }

    /**
     * Runway's launch: keep the speed already earned, redirect it to where the
     * player is looking, and add lift.
     *
     * Facing decides the heading rather than travel, so a Runway can turn --
     * but the SPEED comes from the travel, so a standing Daredevil cannot
     * manufacture one. That is the whole of "convert momentum", and it is why
     * the threshold is on measured travel rather than on the look vector.
     */
    public static Vector launch(Vector travel, Vector facing, double horizontal, double upward) {
        Vector heading = facing.clone().setY(0);
        if (heading.lengthSquared() < 1.0e-6) heading = travel.clone().setY(0);
        if (heading.lengthSquared() < 1.0e-6) return new Vector(0, upward, 0);
        return heading.normalize().multiply(travel.clone().setY(0).length() * horizontal).setY(upward);
    }

    public void negateNextFall(Player p) { pendingFallNegation.add(p.getUniqueId()); }

    public void grantClutches(Player p, int instances, int restores) {
        clutches.put(p.getUniqueId(), instances);
        clutchMaximum.put(p.getUniqueId(), instances);
        clutchRestoresLeft.put(p.getUniqueId(), restores);
    }

    public int clutchesLeft(Player p) { return clutches.getOrDefault(p.getUniqueId(), 0); }

    @EventHandler(ignoreCancelled = true)
    public void move(PlayerMoveEvent e) {
        Player p = e.getPlayer();
        if (!isDaredevil(p)) return;
        UUID id = p.getUniqueId();
        lastStep.put(id, e.getTo().toVector().subtract(e.getFrom().toVector()));
        if (p.isOnGround()) airborne.put(id, 0);
        else airborne.merge(id, 1, Integer::sum);
    }

    /**
     * Clutches first, then the fall negation.
     *
     * Order matters and is not arbitrary: a clutch negates any instance, so if
     * it ran second a landing fall would spend the single-use fall exemption
     * while a clutch was available to cover it for free. Spending the narrower
     * exemption on damage the broader one would have eaten is strictly worse
     * for the player, and invisible.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void damage(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player p) || !isDaredevil(p)) return;
        UUID id = p.getUniqueId();

        int left = clutches.getOrDefault(id, 0);
        if (left > 0) {
            e.setCancelled(true);
            clutches.put(id, left - 1);
            return;
        }
        if (e.getCause() == EntityDamageEvent.DamageCause.FALL && pendingFallNegation.remove(id))
            e.setCancelled(true);
    }

    /** A successful attack restores a spent clutch, never past the initial maximum. */
    @EventHandler(ignoreCancelled = true)
    public void attacked(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player p) || !isDaredevil(p)) return;
        UUID id = p.getUniqueId();
        int max = clutchMaximum.getOrDefault(id, 0);
        if (max == 0) return;
        int restores = clutchRestoresLeft.getOrDefault(id, 0);
        if (restores <= 0) return;
        int have = clutches.getOrDefault(id, 0);
        if (have >= max) return;
        clutches.put(id, have + 1);
        clutchRestoresLeft.put(id, restores - 1);
    }

    @EventHandler public void quit(PlayerQuitEvent e) { clear(e.getPlayer()); }

    public void clear(Player p) {
        UUID id = p.getUniqueId();
        lastStep.remove(id); airborne.remove(id); pendingFallNegation.remove(id);
        clutches.remove(id); clutchRestoresLeft.remove(id); clutchMaximum.remove(id);
    }
}
