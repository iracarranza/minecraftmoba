package com.minecraftmoba.plugin;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

/**
 * The one door ability damage goes through.
 *
 * <h2>Why abilities cannot call {@code target.damage(x, caster)} directly</h2>
 *
 * {@link AbilityInputs} reads every {@code EntityDamageByEntityEvent} with a player damager as a
 * LEFT CLICK, because a melee swing is the only left-click the server can see. A scripted hit looks
 * identical to it. So an ability's own damage, landing while its caster has ability mode on, was:
 *
 * <ul>
 *   <li>cancelled (the input layer cancels a click it consumes), and
 *   <li>read as a left-click, which fired another ability and closed the mode.
 * </ul>
 *
 * Caught live: with mode off a scripted hit of 3 landed twice (20 to 13.33 to 6.67); with mode on it
 * did nothing and the mode closed. A projectile that lands after the caster has pressed F for the next
 * ability is exactly this case, which makes it the normal rapid-fire rotation rather than an edge.
 *
 * <h2>What this does</h2>
 *
 * {@link #deal} marks the damage as an ability's for the duration of the call. Events are dispatched
 * synchronously inside {@code damage}, so a counter that is open across the call is seen by every
 * listener, on this thread, and by nothing else. {@link AbilityInputs#attack} stands down while it
 * is open. Nothing else changes: team protection, stuns, armor, combat state and every other listener
 * still see an ordinary player-caused hit, because it is one.
 *
 * A real melee swing is NOT marked, so it still reads as a left-click.
 */
public final class AbilityDamage {
    private AbilityDamage() {}

    private static final ThreadLocal<int[]> DEPTH = ThreadLocal.withInitial(() -> new int[1]);

    /** Whether the damage event being dispatched right now was dealt by an ability. */
    public static boolean active() { return DEPTH.get()[0] > 0; }

    /** Held open across a damage call. Closing it restores the previous state, so scopes nest. */
    public static final class Scope implements AutoCloseable {
        private boolean closed;
        private Scope() { DEPTH.get()[0]++; }
        @Override public void close() {
            if (closed) return;
            closed = true;
            DEPTH.get()[0]--;
        }
    }

    public static Scope open() { return new Scope(); }

    /** Deal ability damage, credited to {@code source}. The scope is closed even if a listener throws. */
    public static void deal(LivingEntity target, double amount, Entity source) {
        try (var ignored = open()) {
            target.damage(amount, source);
        }
    }
}
