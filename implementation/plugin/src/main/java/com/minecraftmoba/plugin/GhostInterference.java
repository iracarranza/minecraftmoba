package com.minecraftmoba.plugin;

/**
 * Whether the ghost is still following its script, as a small state machine.
 *
 * <pre>
 * SCRIPTING  --struck / displaced / stunned / rooted-->  INTERFERED (passive)
 * INTERFERED --leaves combat, and is no longer held-->    SCRIPTING
 * </pre>
 *
 * A ghost that is hit does not carry on regardless, and does not stay broken: it
 * stands down until the fight has paused, then starts over. That is what makes a
 * recorded take counterplay-able, which an open-loop replay otherwise is not.
 *
 * <h2>Decisions that are proposals, not stated</h2>
 * <ul>
 * <li>On interference the caller cancels any channel or charge the ghost held.</li>
 * <li>On resuming, the script restarts from the beginning, so each presentation of
 *     the telegraph is clean.</li>
 * <li>A stun or root still on the ghost stops it resuming; it would only be
 *     interfered with again.</li>
 * <li>"Displaced" means moved by something that is not its own ability. See
 *     {@link #displaced}.</li>
 * <li>While interfered it is still measurable; nothing here touches the logs.</li>
 * <li>An {@code ignoring} ghost is never interfered with, for Observer, whose point
 *     is to see the cast, not to disrupt it.</li>
 * </ul>
 *
 * Pure: the caller supplies facts and time. Tested without a server.
 */
public final class GhostInterference {
    public enum State { SCRIPTING, INTERFERED }
    public enum Cause { STRUCK, DISPLACED, STUNNED, ROOTED }

    /** A step along the ghost's own movement, per tick, past which it counts as pushed. */
    public static final double DISPLACED_SQ = 0.09;
    /** How long after the ghost's own cast its own movement is assumed to explain any motion. */
    public static final long OWN_CAST_GRACE_TICKS = 40;

    private State state = State.SCRIPTING;
    private Cause cause;
    private long since;
    private int times;
    private boolean ignoring;

    public State state() { return state; }
    public boolean scripting() { return state == State.SCRIPTING; }
    public Cause cause() { return cause; }
    public long since() { return since; }
    /** How many times the ghost has been interfered with. */
    public int times() { return times; }
    public boolean ignoring() { return ignoring; }

    /** Turning interference off also resumes a ghost that was standing down. */
    public void ignoring(boolean ignore) {
        this.ignoring = ignore;
        if (ignore) force();
    }

    /**
     * The ghost was interfered with.
     *
     * @return true when this CHANGED the state, so the caller cancels what the
     *         ghost was holding exactly once rather than on every further hit
     */
    public boolean interfere(Cause why, long tick) {
        if (ignoring || state == State.INTERFERED) return false;
        state = State.INTERFERED;
        cause = why;
        since = tick;
        times++;
        return true;
    }

    /**
     * Whether the ghost may start again.
     *
     * @param inCombat whether it is still in combat ({@code CombatState})
     * @param held whether a stun or root is still on it
     * @return true when this CHANGED the state, so the caller restarts the script
     */
    public boolean tryResume(boolean inCombat, boolean held) {
        if (state != State.INTERFERED || inCombat || held) return false;
        state = State.SCRIPTING;
        cause = null;
        return true;
    }

    /** A manual reset: back to scripting regardless of combat. Returns true if it changed anything. */
    public boolean force() {
        boolean changed = state == State.INTERFERED;
        state = State.SCRIPTING;
        cause = null;
        return changed;
    }

    /**
     * Whether motion this tick was something done TO the ghost.
     *
     * Its own movement abilities (Lunge, Bounding, Runway) move it legitimately, so
     * motion is only external when no ability of its own is running and it has not
     * cast recently enough to be mid-flight. The grace is approximate by design.
     */
    public static boolean displaced(double movedSquared, boolean ownAbilityActive, long ticksSinceOwnCast) {
        return movedSquared > DISPLACED_SQ && !ownAbilityActive && ticksSinceOwnCast > OWN_CAST_GRACE_TICKS;
    }
}
