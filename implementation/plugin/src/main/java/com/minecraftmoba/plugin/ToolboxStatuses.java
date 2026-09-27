package com.minecraftmoba.plugin;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Observed, Illuminated and Root: short real-time world statuses.
 *
 * <h2>Real time, not circuit memory</h2>
 *
 * Both Observed and Illuminated are measured in ticks of the world, never in
 * "until the next component" or "until the circuit ends". That is the whole
 * reason a long machine has to <b>refresh</b> its target lock, and why a
 * second Observer is a deliberate reacquisition point rather than redundancy.
 *
 * A gap between Observers is fine, provided no target-dependent component
 * falls inside it -- which makes the gap a thing the player schedules, and
 * makes a Repeater at a row boundary a decision about whether the lock
 * survives the wrap.
 *
 * <h2>Root refreshes; it does not stack</h2>
 *
 * Rooting an already-Rooted target sets the remaining duration back to full
 * rather than adding to it. Stacking would let a machine that can apply Root
 * <i>n</i> times hold a player for <i>n</i> x 10 ticks, which is the shape of
 * a chain nobody escapes. Refreshing keeps the ceiling at the rate a circuit
 * can re-apply it.
 *
 * <h2>Expiry is read, never swept</h2>
 *
 * A status is a deadline tick compared against the current one. Nothing runs a
 * timer per afflicted entity, and an entity that dies or unloads simply stops
 * being asked about -- so there is no listener whose absence would leak state.
 */
public final class ToolboxStatuses {

    /** Working duration for Observed: roughly four component intervals. */
    public static final int OBSERVED_TICKS = 20;
    /** Root's duration. Refreshes rather than stacking. */
    public static final int ROOT_TICKS = 10;

    /** Applied to a target until consumed by the next relevant hit, not by a clock. */
    private final Map<UUID, Long> observed = new HashMap<>();
    private final Map<UUID, Long> rooted = new HashMap<>();
    private final Map<UUID, Long> illuminated = new HashMap<>();

    private final MobaPlugin plugin;

    public ToolboxStatuses(MobaPlugin plugin) { this.plugin = plugin; }

    private long now() { return plugin.getServer().getCurrentTick(); }

    // ---- Observed ---------------------------------------------------------

    public void observe(Entity target) { observed.put(target.getUniqueId(), now() + OBSERVED_TICKS); }

    public boolean isObserved(Entity target) { return held(observed, target); }

    // ---- Illuminated ------------------------------------------------------

    /**
     * Illuminated has a duration AND is consumed by use.
     *
     * The design says the next relevant hit receives greatly increased
     * knockback and consumes the status. A duration as well, because an
     * Illuminated target that is never hit should not carry the mark for the
     * rest of the match -- the status is a window the machine opened, and a
     * window that never closes is not a window.
     */
    public void illuminate(Entity target) { illuminated.put(target.getUniqueId(), now() + OBSERVED_TICKS); }

    public boolean isIlluminated(Entity target) { return held(illuminated, target); }

    /** Read and clear in one step, because every consumer must consume it. */
    public boolean consumeIlluminated(Entity target) {
        boolean lit = isIlluminated(target);
        illuminated.remove(target.getUniqueId());
        return lit;
    }

    // ---- Root -------------------------------------------------------------

    /**
     * Prevent movement input, refreshing rather than stacking.
     *
     * Root negates movement input only -- WASD, sneak and jump. Interaction,
     * attacking, item use and ender pearls all remain available, which is what
     * separates Root from a Stun and is why chaining it is not the lockout it
     * first appears to be. Every escape an interaction can buy is still open.
     */
    public void root(LivingEntity target) { rooted.put(target.getUniqueId(), now() + ROOT_TICKS); }

    public boolean isRooted(Entity target) { return held(rooted, target); }

    /** Ticks of Root left, for the movement listener that enforces it. */
    public long rootRemaining(Entity target) {
        Long until = rooted.get(target.getUniqueId());
        return until == null ? 0 : Math.max(0, until - now());
    }

    // ---- shared -----------------------------------------------------------

    private boolean held(Map<UUID, Long> statuses, Entity target) {
        if (target == null) return false;
        Long until = statuses.get(target.getUniqueId());
        if (until == null) return false;
        if (until > now()) return true;
        statuses.remove(target.getUniqueId());   // read is when expiry is noticed
        return false;
    }

    /** Drop everything, for a match reset. Statuses are match-scoped like the rest. */
    public void reset() { observed.clear(); rooted.clear(); illuminated.clear(); }
}
