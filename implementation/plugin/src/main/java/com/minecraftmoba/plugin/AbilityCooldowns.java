package com.minecraftmoba.plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Per-player, per-ability cooldown state, in ticks.
 *
 * Ported from {@code codex/lightfooted-from-phase1}, where it was extracted
 * from the inline map {@link AbilityInputs} used to carry. The extraction is
 * the point: the same map was being read in four places with four different
 * expressions of "is it ready" -- a {@code getOrDefault(...) > tick} here, a
 * {@code computeIfAbsent} there -- and one of them had to be the one that was
 * eventually written backwards.
 *
 * <h2>Absolute deadlines, not counters</h2>
 *
 * A cooldown is stored as the tick it expires on, so nothing has to be
 * decremented and no tick may be missed. {@link #remaining} derives the
 * countdown from the current tick, which means a lagging or skipped server
 * tick shortens nothing.
 *
 * <h2>Zero is ready, immediately</h2>
 *
 * {@code start(..., 0)} leaves the ability ready on the same tick rather than
 * holding it for one. An ability that declares no cooldown should be limited
 * by {@code lastFire} alone, which is a different rule with a different
 * purpose: one activation per tick, not one per cooldown.
 */
public final class AbilityCooldowns {

    private final Map<UUID, Map<String, Long>> readyAt = new HashMap<>();

    /** Whether {@code ability} may be activated by {@code player} at {@code now}. */
    public boolean ready(UUID player, String ability, long now) {
        return remaining(player, ability, now) == 0;
    }

    /**
     * Put {@code ability} on cooldown for {@code effective} ticks from {@code now}.
     *
     * The duration is the EFFECTIVE one, already reduced by whatever the
     * ability or its branch reduces it by, so this class never needs to know
     * which abilities scale their cooldown and which do not.
     */
    public void start(UUID player, String ability, long now, long effective) {
        readyAt.computeIfAbsent(player, k -> new HashMap<>())
               .put(ability, now + Math.max(0, effective));
    }

    /** Ticks until {@code ability} is ready, or 0 if it already is. */
    public long remaining(UUID player, String ability, long now) {
        return Math.max(0, readyAt.getOrDefault(player, Map.of()).getOrDefault(ability, 0L) - now);
    }

    /** Forget every cooldown for a player -- a match reset, or a class change. */
    public void clear(UUID player) {
        readyAt.remove(player);
    }

    /** Forget one ability's cooldown, leaving the player's others running. */
    public void clear(UUID player, String ability) {
        var perAbility = readyAt.get(player);
        if (perAbility == null) return;
        perAbility.remove(ability);
        if (perAbility.isEmpty()) readyAt.remove(player);
    }
}
