package com.minecraftmoba.plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The level-up beat: a recurring evaluation, deliberately not a channel.
 *
 * While a player holds unspent choices, a countdown runs. At the end of each
 * countdown their state is evaluated; if the conditions hold, the options are
 * summoned, and if not the countdown simply restarts.
 *
 * See docs/design/IN_WORLD_SELECTION_AND_CHANNEL_CONDITIONS.md.
 *
 * <h2>Why a beat and not a channel</h2>
 *
 * <b>What matters is state at the end, not conduct across the interval.</b>
 * Spending the first four seconds escaping danger, digging a chamber and then
 * crouching on the final beat is correct play rather than an exploit -- that is
 * where the skill lives.
 *
 * So there is no progress to defend, no interruption, no cancel reason, and
 * none of a channel's failure modes: no penalty for being nudged by a mob or
 * clipped by knockback. It is less machinery than {@link Recall}, not more.
 *
 * The asymmetry against Recall is intended. Recall stays a channel that
 * movement and combat interrupt, because leaving a fight is meant to be
 * defensible. This only asks where you are standing when the clock strikes.
 *
 * <h2>Why two conditions need a trailing window</h2>
 *
 * Read instantaneously, two of the four conditions stop meaning anything. A
 * player mid-fight is not being damaged at most individual instants -- the gap
 * between two sword swings would qualify -- and instantaneous velocity is zero
 * at odd moments. Combat is given its window by {@link CombatState}; stillness
 * gets one here, by remembering when the player last changed block.
 *
 * Crouching and aim are genuinely fine as instantaneous reads.
 *
 * <h2>What this class does not do</h2>
 *
 * No Bukkit, no entities, no world. It takes ticks and booleans and answers
 * questions about them, so it is testable without a server. {@link
 * InWorldSelection} supplies the real conditions and builds the options.
 */
public final class SelectionBeat {

    /**
     * The four conditions, evaluated together at the end of a beat.
     *
     * In practice crouching, stillness and steady aim already exclude most real
     * fighting -- a player trading hits is moving, taking knockback, and cannot
     * hold a crouch while aiming at a fixed piece of the world. {@code inCombat}
     * is retained anyway, because that overlap is incidental rather than
     * guaranteed, and because it is the condition that states the intent.
     */
    public record Conditions(boolean crouching, boolean onSolidGround, boolean stationary,
                             boolean inCombat, boolean aimEligible) {

        public boolean satisfied() {
            return crouching && onSolidGround && stationary && !inCombat && aimEligible;
        }

        /**
         * Why the beat failed, for telling the player.
         *
         * A gate the player cannot observe fails silently, so a beat that does
         * nothing and says nothing reads as a broken feature. Combat is
         * reported by the bossbar swap; this covers the three the player
         * chooses.
         */
        public String unmetReason() {
            if (inCombat) return "in combat";
            if (!crouching) return "crouch to choose";
            if (!onSolidGround) return "stand on solid ground";
            if (!stationary) return "hold still";
            if (!aimEligible) return "look at open space";
            return null;
        }
    }

    private final long beatTicks;
    private final long stillnessTicks;
    /** When each player last changed block position. */
    private final Map<UUID, Long> lastMoved = new HashMap<>();
    /** When each player's current countdown began. */
    private final Map<UUID, Long> beatStarted = new HashMap<>();

    public SelectionBeat(long beatTicks, long stillnessTicks) {
        this.beatTicks = beatTicks;
        this.stillnessTicks = stillnessTicks;
    }

    public long beatTicks() { return beatTicks; }
    public long stillnessTicks() { return stillnessTicks; }

    /** Note that the player changed block. Stillness is measured from here. */
    public void moved(UUID player, long now) { lastMoved.put(player, now); }

    /**
     * Whether the player has held still long enough.
     *
     * A player never seen moving counts as still: they have been standing
     * there since before anyone was watching, which is the honest reading and
     * avoids a first-join player being told to hold still while holding still.
     */
    public boolean stationary(UUID player, long now) {
        Long moved = lastMoved.get(player);
        return moved == null || now - moved >= stillnessTicks;
    }

    /** Begin or restart the countdown. */
    public void restart(UUID player, long now) { beatStarted.put(player, now); }

    /** Ticks until the next evaluation. */
    public long remaining(UUID player, long now) {
        Long started = beatStarted.get(player);
        if (started == null) return beatTicks;
        return Math.max(0, started + beatTicks - now);
    }

    /** Progress through the current beat, 0 at its start and 1 at its end. */
    public double progress(UUID player, long now) {
        if (beatTicks <= 0) return 1;
        return 1 - (double) remaining(player, now) / beatTicks;
    }

    /**
     * Whether the countdown has elapsed. Starts one if none is running, so the
     * first call after gaining a choice begins the clock rather than firing.
     */
    public boolean due(UUID player, long now) {
        Long started = beatStarted.get(player);
        if (started == null) { restart(player, now); return false; }
        return now - started >= beatTicks;
    }

    public void clear(UUID player) { lastMoved.remove(player); beatStarted.remove(player); }

    int tracked() { return beatStarted.size(); }
}
