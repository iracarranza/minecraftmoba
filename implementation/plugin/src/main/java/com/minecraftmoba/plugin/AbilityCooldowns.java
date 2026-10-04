package com.minecraftmoba.plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Per-player, per-ability cooldown and charge state, in ticks.
 *
 * Ported from {@code codex/lightfooted-from-phase1}, where it was extracted
 * from the inline map {@link AbilityInputs} used to carry. The extraction is
 * the point: the same map was being read in four places with four different
 * expressions of "is it ready" -- a {@code getOrDefault(...) > tick} here, a
 * {@code computeIfAbsent} there -- and one of them had to be the one that was
 * eventually written backwards.
 *
 * <h2>One stored number, two readings</h2>
 *
 * The stored {@code long} is <b>charge time</b>: the tick from which charges
 * accrue. {@code TOOLBOX_CIRCUIT_GRAMMAR.md} section 10D observes that charges
 * need no new storage, because a single-charge ability is the same arithmetic
 * with N = 1:
 *
 * <pre>
 *   available = min(N, (now - chargeTime) / R)
 *   on use:     chargeTime = max(chargeTime, now - N*R) + R
 * </pre>
 *
 * The {@code max} is the clamp that stops charges banking while already full.
 * Without it an ability unused for a minute would hold a minute's worth.
 *
 * <h2>Absolute, not counted down</h2>
 *
 * Nothing is decremented and no tick may be missed, so a lagging or skipped
 * server tick shortens no cooldown and grants no charge.
 *
 * <h2>Zero recharge is always ready</h2>
 *
 * An ability declaring no cooldown is limited by {@code lastFire} alone, which
 * is a different rule with a different purpose: one activation per tick, not
 * one per cooldown.
 */
public final class AbilityCooldowns {

    /**
     * Charge time, plus the terms needed to read it back.
     *
     * {@code TOOLBOX_CIRCUIT_GRAMMAR.md} 10D observes that charges need no new
     * storage, and for the ACCRUAL that is exactly right. It is not right for
     * the readback: the old single-charge field stored a deadline, which
     * describes itself, while charge time is an origin that means nothing
     * without the recharge it accrues at. {@link #remaining} is what the HUD
     * asks, so the recharge is stored rather than guessed.
     */
    private record Entry(long chargeTime, long recharge, int maxCharges) {}

    private final Map<UUID, Map<String, Entry>> state = new HashMap<>();

    // ---- single charge: the N = 1 case, kept as its own vocabulary ---------

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
        spend(player, ability, now, 1, effective);
    }

    /** Ticks until {@code ability} is ready, or 0 if it already is. */
    public long remaining(UUID player, String ability, long now) {
        Entry e = entry(player, ability);
        if (e == null) return 0;
        return untilNextCharge(player, ability, now, e.maxCharges(), e.recharge());
    }

    // ---- charges ----------------------------------------------------------

    /**
     * How many charges are available now.
     *
     * Capped at {@code maxCharges} by the formula itself rather than by a
     * separate clamp, so an ability left unused cannot accumulate more than it
     * declares.
     */
    public int available(UUID player, String ability, long now, int maxCharges, long recharge) {
        if (recharge <= 0) return Math.max(1, maxCharges);
        long since = now - charge(player, ability, now, maxCharges, recharge);
        if (since < 0) return 0;
        return (int) Math.min(maxCharges, since / recharge);
    }

    /** Charges available, using the terms the last activation was made with. */
    public int available(UUID player, String ability, long now) {
        Entry e = entry(player, ability);
        return e == null ? 1 : available(player, ability, now, e.maxCharges(), e.recharge());
    }

    /**
     * Spend one charge.
     *
     * Advancing charge time by one recharge rather than setting it to
     * {@code now} is what makes charges accrue independently: spending the
     * second of three does not restart the first one's timer.
     */
    public void spend(UUID player, String ability, long now, int maxCharges, long recharge) {
        long r = Math.max(0, recharge);
        long current = charge(player, ability, now, maxCharges, r);
        long clamped = Math.max(current, now - (long) maxCharges * r);
        state.computeIfAbsent(player, k -> new HashMap<>())
             .put(ability, new Entry(clamped + r, r, Math.max(1, maxCharges)));
    }

    /** Ticks until the next charge arrives, or 0 if one is already available. */
    public long untilNextCharge(UUID player, String ability, long now, int maxCharges, long recharge) {
        if (recharge <= 0) return 0;
        if (available(player, ability, now, maxCharges, recharge) > 0) return 0;
        long elapsed = now - charge(player, ability, now, maxCharges, recharge);
        return Math.max(0, recharge - Math.max(0, elapsed));
    }

    /**
     * An ability never used is full, not empty.
     *
     * Expressed as a charge time far enough back to have accrued its maximum,
     * so the absent case needs no branch anywhere else.
     */
    private long charge(UUID player, String ability, long now, int maxCharges, long recharge) {
        Entry e = entry(player, ability);
        return e != null ? e.chargeTime() : now - (long) maxCharges * Math.max(0, recharge);
    }

    private Entry entry(UUID player, String ability) {
        return state.getOrDefault(player, Map.of()).get(ability);
    }

    // ---- clearing ---------------------------------------------------------

    /** Forget every cooldown for a player -- a match reset, or a class change. */
    public void clear(UUID player) {
        state.remove(player);
    }

    /** Forget one ability's cooldown, leaving the player's others running. */
    public void clear(UUID player, String ability) {
        var perAbility = state.get(player);
        if (perAbility == null) return;
        perAbility.remove(ability);
        if (perAbility.isEmpty()) state.remove(player);
    }
}
