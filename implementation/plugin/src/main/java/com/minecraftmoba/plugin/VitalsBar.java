package com.minecraftmoba.plugin;

/**
 * Fixed-size health and hunger bars with a proportional internal fill.
 *
 * <h2>Why this is not the native row</h2>
 *
 * The client owns the native health and hunger rows and its fill quantum is
 * the half-sprite, so those rows show twenty steps and will never show more,
 * however they are repainted. The resource pack's {@code bar_segment} already
 * repaints them into something that <i>looks</i> like one continuous bar --
 * that buys the appearance and none of the resolution.
 *
 * {@code VitalsScaling} separately fixed the other half: the bar is always
 * twenty points for every player, with Capacity deciding what a point is worth
 * rather than how many there are. So the length was already right. What
 * remained was the fill.
 *
 * <h2>The bar is one unit, repeated</h2>
 *
 * A bitmap glyph cannot be stretched -- Minecraft renders it at its texture
 * size -- so a bar that grows horizontally is a bar built from a repeated
 * unit, and its length is the repeat count.
 *
 * The bar is <b>always {@link #FILL_WIDTH} units wide</b>; only how many are
 * lit changes. That makes the fixed size structural rather than a property
 * separate images have to agree about, and an unfilled bar cannot render
 * short. Four glyphs do the work that one image per fill level would have
 * needed a hundred and thirty for.
 *
 * <h2>The advance correction</h2>
 *
 * Each unit is one lit pixel wide, and Minecraft advances a bitmap glyph by
 * its bounding box plus one pixel of standard spacing -- so a unit advances
 * two. Each is therefore followed by {@link #BACK_ONE}, the negative-space
 * character that advances -1, for a net advance of exactly one pixel.
 *
 * If that assumption is ever wrong the bar collapses into a sliver rather than
 * drifting a pixel at a time, which is the failure mode to want: it is visible
 * immediately and cannot be mistaken for a tuning problem.
 *
 * <h2>Codepoints are computed, not hard-coded</h2>
 *
 * The rest of this pack's glyphs take codepoints by their order in {@code
 * registry.json} and are then hard-coded as Java literals, so inserting one
 * shifts every later one silently. These declare an explicit base instead.
 * {@code VitalsBarTest} pins the two declarations together.
 *
 * <h2>What this class does</h2>
 *
 * Arithmetic and string building, no Bukkit, tested without a server. The lit
 * and unlit runs are returned separately because they are tinted differently:
 * the pack draws them white and assigns no palette.
 */
public final class VitalsBar {

    /** Must match registry.json `bars.base`. */
    public static final int BASE = 0xE100;
    public static final String CAP_LEFT  = String.valueOf((char) (BASE));
    public static final String UNIT_ON   = String.valueOf((char) (BASE + 1));
    public static final String UNIT_OFF  = String.valueOf((char) (BASE + 2));
    public static final String CAP_RIGHT = String.valueOf((char) (BASE + 3));

    /**
     * The negative-space character advancing -1 pixel.
     *
     * The pack's space font maps U+F000+n to an advance of -n, so U+F001 is -1.
     */
    public static final String BACK_ONE = String.valueOf((char) 0xF001);

    /** Must match registry.json `bars.fill_width`. One unit per pixel. */
    public static final int FILL_WIDTH = 64;
    /** Levels are 0 (empty) through FILL_WIDTH (full), inclusive. */
    public static final int LEVELS = FILL_WIDTH + 1;

    private VitalsBar() {}

    /**
     * The fill level for a fraction of a bar.
     *
     * Rounds to nearest, so the bar reads as the honest nearest pixel -- but
     * with two exceptions at the ends, which are the reason this is not a bare
     * {@code Math.round}:
     *
     * <ul>
     *   <li><b>Only an empty bar reads empty.</b> Any health above zero keeps
     *       at least one lit unit. A player on a sliver who renders as an empty
     *       bar has been told they are dead, and will play as though they
     *       are.</li>
     *   <li><b>Only a full bar reads full.</b> Anything short of the maximum
     *       gives up its last unit, so "nearly full" stays visibly distinct
     *       from "full" -- exactly the distinction a player checks before
     *       deciding whether to go back.</li>
     * </ul>
     *
     * Both only matter once the bar is fine enough to round across them, which
     * is to say they exist because of the resolution, not in spite of it.
     */
    public static int level(double fraction) {
        if (Double.isNaN(fraction) || fraction <= 0) return 0;
        if (fraction >= 1) return FILL_WIDTH;
        int rounded = (int) Math.round(fraction * FILL_WIDTH);
        if (rounded <= 0) return 1;                        // alive is never empty
        if (rounded >= FILL_WIDTH) return FILL_WIDTH - 1;  // short is never full
        return rounded;
    }

    /** The level for a current/maximum pair, tolerating a zero or negative maximum. */
    public static int level(double current, double maximum) {
        if (maximum <= 0) return 0;
        return level(current / maximum);
    }

    /** The lit run: {@code level} units, each advancing exactly one pixel. */
    public static String filled(int level) { return run(UNIT_ON, check(level)); }

    /** The unlit run, which is whatever is left of the bar's fixed width. */
    public static String unfilled(int level) { return run(UNIT_OFF, FILL_WIDTH - check(level)); }

    private static int check(int level) {
        if (level < 0 || level >= LEVELS)
            throw new IllegalArgumentException("Fill level out of range: " + level);
        return level;
    }

    private static String run(String unit, int count) {
        var out = new StringBuilder(count * 2);
        for (int i = 0; i < count; i++) out.append(unit).append(BACK_ONE);
        return out.toString();
    }
}
