package com.minecraftmoba.plugin;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockRedstoneEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The Ultimate's armed window: world Redstone components also fire the Utility
 * Belt, at Toolbox.
 *
 * <h2>The machine is a sequencer; the effects land on Toolbox</h2>
 *
 * The physical machine does its ordinary Minecraft job -- a world dispenser
 * fires its own arrows where it points. The belt effect is separate and
 * additional, and resolves at Toolbox's location, facing and inventory,
 * exactly as a belt component would. The world circuit supplies <b>order and
 * timing</b>; Toolbox still supplies origin, aim and ammunition.
 *
 * <h2>The eligible list comes from one rule, not a list</h2>
 *
 * A component produces its belt effect only if that effect depends on neither
 * <b>inventory geometry</b> nor the <b>damage trigger</b>. That excludes
 * Comparator (needs the trigger), Tripwire and Daylight Sensor (need the slot
 * below and above), Dust (needs being first, plus the trigger), Repeater
 * (needs flow order), and Torch -- which is a power SOURCE, not something a
 * circuit activates. Levers, buttons and pressure plates drop out with it.
 *
 * {@link #eligible} encodes the rule's OUTPUT rather than the rule, because a
 * rule about inventory geometry cannot be evaluated against a world block.
 * Where the two could drift, the test pins the list against the table.
 *
 * <h2>Two caps, deliberately paired</h2>
 *
 * 120 ticks and 32 activations. 120 / 32 is 3.75 ticks each, so a clock faster
 * than about four ticks is bounded by the activation cap and a broad slow
 * machine is bounded by the time cap. Both bite, in different builds, which
 * removes the unbounded-clock problem without a rule about clocks.
 *
 * <h2>No 5-tick grid, and concurrency is real</h2>
 *
 * Activations resolve at the machine's own timing, so machine WIDTH matters as
 * much as speed: a single pulse into a bank of components fires all of them in
 * the same instant, and the 32 can be spent in one tick.
 */
public final class GizmoWindow implements Listener {

    /** One armed window. */
    private static final class Armed {
        final long expiresAt;
        int activationsLeft;
        Armed(long expiresAt, int activations) {
            this.expiresAt = expiresAt; this.activationsLeft = activations;
        }
    }

    private final MobaPlugin plugin;
    private final UtilityBelt belt;
    private final Map<UUID, Armed> armed = new HashMap<>();

    public GizmoWindow(MobaPlugin plugin, UtilityBelt belt) {
        this.plugin = plugin; this.belt = belt;
    }

    /**
     * Which world blocks carry a belt effect, and as which component.
     *
     * Observer is in the list but is counted differently -- see
     * {@link #isRisingEdge}.
     */
    public static CircuitComponent eligible(Material material) {
        return switch (material) {
            case PISTON -> CircuitComponent.PISTON;
            case STICKY_PISTON -> CircuitComponent.STICKY_PISTON;
            case HOPPER -> CircuitComponent.HOPPER;
            case DISPENSER -> CircuitComponent.DISPENSER;
            case DROPPER -> CircuitComponent.DROPPER;
            case OBSERVER -> CircuitComponent.OBSERVER;
            default -> null;
        };
    }

    /**
     * An activation is a rising edge: unpowered to powered.
     *
     * "Activated" is read as <b>receives a signal</b>, regardless of what the
     * signal does to the block's own behaviour. A powered hopper stops
     * transferring, and it is kept deliberately on that reading. Observer
     * counts when it FIRES, which is also when its own output rises, so one
     * test serves both cases.
     */
    public static boolean isRisingEdge(int oldCurrent, int newCurrent) {
        return oldCurrent <= 0 && newCurrent > 0;
    }

    /** Arm the window. Re-arming replaces rather than extends. */
    public void arm(Player p, long durationTicks, int activations) {
        armed.put(p.getUniqueId(),
                new Armed(plugin.getServer().getCurrentTick() + durationTicks, activations));
    }

    public boolean isArmed(Player p) { return remaining(p) != null; }

    /** Activations left, or null when no window is running. */
    public Integer activationsLeft(Player p) {
        Armed a = remaining(p);
        return a == null ? null : a.activationsLeft;
    }

    private Armed remaining(Player p) {
        Armed a = armed.get(p.getUniqueId());
        if (a == null) return null;
        if (a.activationsLeft <= 0 || plugin.getServer().getCurrentTick() >= a.expiresAt) {
            armed.remove(p.getUniqueId());          // expiry is noticed on read
            return null;
        }
        return a;
    }

    /**
     * Charge one activation against the window.
     *
     * Separated from the event handler so the cap arithmetic is testable: the
     * two caps ending a window is the whole of the ability's bound, and it is
     * not something to discover on a server.
     */
    public boolean consume(Player p) {
        Armed a = remaining(p);
        if (a == null) return false;
        a.activationsLeft--;
        return true;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void redstone(BlockRedstoneEvent e) {
        if (armed.isEmpty()) return;                // the fast path: almost always
        if (!isRisingEdge(e.getOldCurrent(), e.getNewCurrent())) return;
        CircuitComponent component = eligible(e.getBlock().getType());
        if (component == null) return;

        double radius = plugin.getConfig().getDouble(
                "abilities.definitions.gizmo.machineRadius", 32.0);
        for (UUID id : Map.copyOf(armed).keySet()) {
            Player p = plugin.getServer().getPlayer(id);
            if (p == null || !p.isOnline()) { armed.remove(id); continue; }
            if (!p.getWorld().equals(e.getBlock().getWorld())) continue;
            if (p.getLocation().distanceSquared(e.getBlock().getLocation()) > radius * radius) continue;
            if (!consume(p)) continue;
            // Each activation is its own run: the machine supplies order, and
            // nothing carries between two world components the way it does
            // between two steps of one belt circuit.
            belt.effect(p, component, new UtilityBelt.Run());
        }
    }

    /** Drop every window, for a match reset. */
    public void reset() { armed.clear(); }

    public String report(Player p) {
        Integer left = activationsLeft(p);
        return "GIZMO armed=" + (left != null) + " activationsLeft=" + (left == null ? 0 : left);
    }
}
