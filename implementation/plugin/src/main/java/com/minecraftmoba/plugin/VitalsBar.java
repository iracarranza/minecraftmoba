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
    public static final String TICK_ON   = String.valueOf((char) (BASE + 4));
    public static final String TICK_OFF  = String.valueOf((char) (BASE + 5));

    /**
     * The negative-space character advancing -1 pixel.
     *
     * The pack's space font maps U+F000+n to an advance of -n, so U+F001 is -1.
     */
    public static final String BACK_ONE = String.valueOf((char) 0xF001);

    /**
     * Must match registry.json `bars.fill_width`. One unit per pixel.
     *
     * 128 rather than 64 because of the tick interval, not because of the
     * fill: at 100 displayed health per tick, Mole's Lv30 3,200 is 32 ticks,
     * and across 64 units that is a tick every two pixels -- each one touching
     * its neighbour. 128 gives four, which is enough to read one.
     */
    public static final int FILL_WIDTH = 128;

    /**
     * A bar's width, which is a LAYOUT choice rather than a property of the bar.
     *
     * Two are wanted and they trade against each other:
     *
     * <ul>
     *   <li>{@link #WIDE} -- 128 units, the resolution the ticks were sized
     *       for. Too wide to sit inside the ~81px the native row occupies, so
     *       the two bars overhang the hotbar.</li>
     *   <li>{@link #INLINE} -- 81 units, exactly where vanilla draws the
     *       native row. Fits, and {@code tickUnits} then refuses to draw ticks
     *       above 2,700 displayed health, because 81/3 is 27 marks. Mole's
     *       Lv30 3,200 loses them -- the character they exist for.</li>
     * </ul>
     *
     * The loss is smaller than it looks now that the numeral is drawn over the
     * bar: ticks were a substitute for a number nobody could read, and the
     * number is now right there. But it is a real loss and it is silent, so it
     * is written down here rather than discovered.
     */
    public record Style(int fillWidth) {
        public Style {
            if (fillWidth < 1) throw new IllegalArgumentException("A bar needs width.");
        }
        public int levels() { return fillWidth + 1; }

        public int level(double current, double maximum) {
            if (maximum <= 0) return 0;
            double fraction = current / maximum;
            if (Double.isNaN(fraction) || fraction <= 0) return 0;
            if (fraction >= 1) return fillWidth;
            int rounded = (int) Math.round(fraction * fillWidth);
            if (rounded <= 0) return 1;
            if (rounded >= fillWidth) return fillWidth - 1;
            return rounded;
        }

        public java.util.Set<Integer> tickUnits(double maxDisplayed, int interval) {
            var ticks = new java.util.HashSet<Integer>();
            if (interval <= 0 || maxDisplayed <= 0) return ticks;
            int count = (int) Math.floor(maxDisplayed / interval);
            if (count <= 1) return ticks;
            if (count > fillWidth / MIN_TICK_SPACING) return ticks;
            for (int n = 1; n < count; n++)
                ticks.add((int) Math.round((double) n * fillWidth / count));
            return ticks;
        }

        public String filled(int level, java.util.Set<Integer> ticks) {
            return run(0, check(level), UNIT_ON, TICK_ON, ticks);
        }

        public String unfilled(int level, java.util.Set<Integer> ticks) {
            return run(check(level), fillWidth, UNIT_OFF, TICK_OFF, ticks);
        }

        private int check(int level) {
            if (level < 0 || level >= levels())
                throw new IllegalArgumentException("Fill level out of range: " + level);
            return level;
        }
    }

    /** The resolution the ticks were sized for; overhangs the native row. */
    public static final Style WIDE = new Style(FILL_WIDTH);
    /** Exactly the width vanilla's own row occupies. [FIXTURE -- confirm by probe] */
    public static final Style INLINE = new Style(81);
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

    /**
     * Which unit indices carry a reference tick.
     *
     * <h3>Why the bar needs these at all</h3>
     *
     * A proportional bar shows a FRACTION, and a fraction alone erases
     * magnitude: 1,000/1,000 and 3,200/3,200 are the same full bar. That is a
     * direct loss for this project, which adopted a x100 player-facing scale
     * specifically so damage would be legible -- a bar that cannot distinguish
     * a 1,000-health character from a 3,200-health one gives that back.
     *
     * Ticks restore it. They sit at a fixed interval of DISPLAYED health, so a
     * bigger character carries visibly more of them, and one tick is a
     * yardstick: a hit that eats two ticks took 200.
     *
     * <h3>Fixed width, denser ticks</h3>
     *
     * The bar stays the same length and the ticks compress as maximum health
     * grows, rather than the bar growing and the ticks staying put. Growing
     * the bar would hand a low-Capacity player a visibly shorter one, which is
     * the exact defect VitalsScaling was written to remove.
     *
     * Nobody counts thirty-two ticks; density is read, not counted. Reading an
     * exact total is what the numeral beside the bar is for.
     *
     * <h3>Rounding</h3>
     *
     * Tick n sits at {@code round(n * FILL_WIDTH / tickCount)}, so ticks stay
     * evenly spread when the spacing is not a whole number of pixels -- 1,000
     * health is ten ticks across 128 units, which is 12.8. Truncating instead
     * would bunch the error at one end and leave a visibly wider last gap.
     *
     * Returns an empty set when the ticks would be too dense to read, rather
     * than drawing a solid bar of marks and calling it a scale.
     */
    public static java.util.Set<Integer> tickUnits(double maxDisplayed, int interval) {
        var ticks = new java.util.HashSet<Integer>();
        if (interval <= 0 || maxDisplayed <= 0) return ticks;
        int count = (int) Math.floor(maxDisplayed / interval);
        if (count <= 1) return ticks;
        // Two pixels per tick is a mark touching its neighbour; below that the
        // row stops being a scale and becomes texture.
        if (count > FILL_WIDTH / MIN_TICK_SPACING) return ticks;
        for (int n = 1; n < count; n++)
            ticks.add((int) Math.round((double) n * FILL_WIDTH / count));
        return ticks;
    }

    /** Fewest pixels between ticks that still reads as two marks rather than one band. */
    public static final int MIN_TICK_SPACING = 3;

    /** The lit run: {@code level} units, each advancing exactly one pixel. */
    public static String filled(int level) { return run(UNIT_ON, check(level)); }

    /** The unlit run, which is whatever is left of the bar's fixed width. */
    public static String unfilled(int level) { return run(UNIT_OFF, FILL_WIDTH - check(level)); }

    /** The lit run with reference ticks marked. */
    public static String filled(int level, java.util.Set<Integer> ticks) {
        return run(0, check(level), UNIT_ON, TICK_ON, ticks);
    }

    /** The unlit run with reference ticks marked, continuing the same index space. */
    public static String unfilled(int level, java.util.Set<Integer> ticks) {
        return run(check(level), FILL_WIDTH, UNIT_OFF, TICK_OFF, ticks);
    }

    /**
     * Emit units {@code from} (inclusive) to {@code to} (exclusive).
     *
     * The index is the unit's position along the WHOLE bar, not within the
     * run, so a tick lands at the same pixel whether it falls in the lit part
     * or the unlit part. Indexing per run would make ticks jump as the player
     * took damage.
     */
    private static String run(int from, int to, String plain, String tick, java.util.Set<Integer> ticks) {
        var out = new StringBuilder((to - from) * 2);
        for (int i = from; i < to; i++)
            out.append(ticks.contains(i) ? tick : plain).append(BACK_ONE);
        return out.toString();
    }

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
