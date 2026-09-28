package com.minecraftmoba.plugin;

import java.util.Locale;

/**
 * How an ability is committed: immediately, on release, or on a second press.
 *
 * <h2>Why this is a player setting rather than a per-ability decision</h2>
 *
 * Block- and area-targeting abilities have a problem the rest do not: until
 * they fire, <b>nothing shows what they will affect</b>. Mole's tunnelling is
 * the live example -- direction and the blocks it will attempt are undefined
 * until it starts moving and blocks start breaking, which is to say until it
 * is too late to have aimed.
 *
 * A targeting preview fixes that, and the question of when to commit is a
 * question about the PLAYER, not the ability. The same person wants the same
 * answer for every ability they own, and MOBAs have converged on exactly these
 * three because they trade the same way everywhere.
 *
 * <h2>The three</h2>
 *
 * <ul>
 *   <li>{@link #QUICK} -- the press fires it. Fastest, no preview, and what
 *       every ability does today.</li>
 *   <li>{@link #HOLD} -- the press shows the preview and the RELEASE fires it.
 *       One gesture, and the commit point is unambiguous.</li>
 *   <li>{@link #DOUBLE} -- the press shows the preview and a second press of
 *       the SAME input fires it. Any other ability input cancels. Slowest and
 *       the only one that can be aborted without firing.</li>
 * </ul>
 *
 * <h2>What it does not change</h2>
 *
 * The ability. A mode decides when the commit happens and whether a preview is
 * drawn in between; it never changes what is cast, where, or for how much. An
 * ability that behaved differently by cast mode would make the setting a power
 * choice rather than an input preference, and players would pick the strongest
 * one rather than the one they can use.
 *
 * Abilities that target nothing -- a self-buff, a channel with no aim -- ignore
 * the mode entirely rather than growing a preview with nothing in it.
 */
public enum CastMode {

    /** Press fires. No preview. The behaviour every ability has today. */
    QUICK,

    /** Press previews, release fires. */
    HOLD,

    /** Press previews, the same input again fires, any other input cancels. */
    DOUBLE;

    /** The setting a player has not chosen yet. */
    public static final CastMode DEFAULT = QUICK;

    /**
     * Parse a stored or typed value, falling back to the default.
     *
     * Never throws: this is read from persisted player data and from a
     * clickable item, and an unreadable cast mode should leave a player able to
     * cast rather than unable to log in.
     */
    public static CastMode of(String name) {
        if (name == null) return DEFAULT;
        try {
            return valueOf(name.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return DEFAULT;
        }
    }

    /** The next mode in the cycle, for a setting toggled by clicking it. */
    public CastMode next() {
        return values()[(ordinal() + 1) % values().length];
    }

    /** Whether this mode shows a targeting preview before committing. */
    public boolean previews() { return this != QUICK; }

    /** What the setting item says it is. */
    public String label() {
        return switch (this) {
            case QUICK -> "Quick cast";
            case HOLD -> "Hold to cast";
            case DOUBLE -> "Double cast";
        };
    }

    /** One line saying what the player is choosing. */
    public String description() {
        return switch (this) {
            case QUICK -> "Fires the moment you press. No preview.";
            case HOLD -> "Hold to aim, release to fire.";
            case DOUBLE -> "Press to aim, press again to fire, any other ability cancels.";
        };
    }
}
