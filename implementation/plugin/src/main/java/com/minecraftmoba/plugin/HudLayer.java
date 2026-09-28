package com.minecraftmoba.plugin;

import java.util.List;

/**
 * Where a HUD glyph goes: a named vertical layer, and a free horizontal offset.
 *
 * <h2>The asymmetry is the design</h2>
 *
 * A bossbar title is the only persistent drawing surface a plugin has, and a
 * resource pack turns it into one we can place things on. But the two axes are
 * not alike, and pretending they are is what would make this collapse:
 *
 * <ul>
 *   <li><b>Horizontal is free.</b> The negative-space font advances {@code -n}
 *       for {@code U+F000+n}, and the prefix varies per emission, so {@code x}
 *       is an ordinary number computed at send time.</li>
 *   <li><b>Vertical is enumerated.</b> {@code ascent} and {@code height} belong
 *       to a font PROVIDER, not to an emission. Every distinct
 *       {@code (y, scale)} pair costs one provider entry pointing at the same
 *       texture, so a continuous {@code y} would mean a provider per pixel.</li>
 * </ul>
 *
 * Hence named layers rather than coordinates. {@code registry.json} declares
 * them and this reads the same names back, exactly as {@code VitalsBar} pins
 * itself to {@code bars.base}.
 *
 * <h2>Baseline drift</h2>
 *
 * Each bossbar is its own line with its own baseline, and they stack. The same
 * layer therefore lands somewhere different depending on WHICH bossbar drew
 * it, and this project already shows four. A layer name means one thing only
 * once one bossbar is nominated as the canvas.
 *
 * <h2>Arithmetic only</h2>
 *
 * No Bukkit, no world, tested without a server -- the same split that lets
 * {@link VitalsBar} be checked by arithmetic rather than by looking at it.
 */
public final class HudLayer {

    /** Must match registry.json `hud_layers.base`. */
    public static final int BASE = 0xE200;

    /**
     * Layers in declared order, which is how their codepoints are assigned.
     *
     * Sorted alphabetically by the build, which is why the names are
     * zero-padded: the offsets have to be stable against someone adding a
     * layer, and a name sorting into the middle would shift every later one
     * silently. The same defect the vitals glyphs took explicit codepoints to
     * avoid.
     *
     * Named for the ASCENT they carry rather than for a place on screen, and
     * they stay that way now that the placement IS known. The first live
     * reading killed the earlier semantic names -- `hotbar_above` at ascent 20
     * cleared the bossbar by a few pixels and came nowhere near the hotbar --
     * and a name is exactly the thing that survives a later change to what a
     * layer is used for.
     *
     * Where they land, measured: the foot of a height-256 glyph sits at
     * {@code ascent - 26} GUI pixels above the bottom of the screen. So
     * placing something is {@code ascent = height_wanted + 26}, and
     * {@code registry.json} records which layer is meant for what.
     */
    public static final List<String> LAYERS =
            List.of("asc008", "asc048", "asc050", "asc064", "asc072", "asc128", "asc224");

    /** The negative-space character advancing -1 pixel. */
    public static final String BACK_ONE = String.valueOf((char) 0xF001);

    private HudLayer() {}

    /** The probe codepoint for a layer. */
    public static String glyph(String layer) {
        int index = LAYERS.indexOf(layer);
        if (index < 0) throw new IllegalArgumentException(
                "No such HUD layer: " + layer + ". Declared: " + LAYERS);
        return String.valueOf((char) (BASE + index));
    }

    /**
     * A run of negative space advancing exactly {@code pixels} to the left.
     *
     * The space font maps {@code U+F000+n} to {@code -n} for n in 1..256, so a
     * larger shift is several characters rather than one. Emitted as repeats of
     * the largest available step so the string stays short.
     */
    public static String left(int pixels) {
        if (pixels < 0) throw new IllegalArgumentException("Use right() for a positive offset.");
        var out = new StringBuilder();
        int left = pixels;
        while (left > 0) {
            int step = Math.min(left, MAX_STEP);
            out.append((char) (0xF000 + step));
            left -= step;
        }
        return out.toString();
    }

    /** Must match build_pack.py NEGATIVE_SPACE_MAX. */
    public static final int MAX_STEP = 256;

    /**
     * Ordinary spaces, which advance 4 pixels each, plus a remainder in
     * negative space.
     *
     * There is no positive-advance glyph beyond the space itself, so moving
     * right by an arbitrary amount is "overshoot in spaces, come back in
     * negative space". Uglier than a symmetric pair of fonts and it needs no
     * extra provider, which matters because providers are the scarce thing.
     */
    public static String right(int pixels) {
        if (pixels < 0) throw new IllegalArgumentException("Use left() for a negative offset.");
        int spaces = (pixels + SPACE_ADVANCE - 1) / SPACE_ADVANCE;
        return " ".repeat(spaces) + left(spaces * SPACE_ADVANCE - pixels);
    }

    /** What one space advances, per the pack's own space font. */
    public static final int SPACE_ADVANCE = 4;

    /** Place a layer's glyph at an offset from the title's own origin. */
    public static String at(String layer, int x) {
        return (x >= 0 ? right(x) : left(-x)) + glyph(layer);
    }
}
