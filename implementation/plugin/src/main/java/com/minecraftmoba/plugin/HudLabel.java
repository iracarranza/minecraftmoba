package com.minecraftmoba.plugin;

import java.util.List;
import java.util.Locale;

/**
 * Notice text, drawn at a chosen height.
 *
 * A second face beside {@link HudText}, at the same mechanism and a different
 * size. The numeral is 3x5 because it sits inside a seven-pixel bar; a notice
 * is scanned at a glance and keeps vanilla's 5x7 proportions. Glyph size is a
 * property of a FAMILY rather than of the pack, which is what lets both exist.
 *
 * <h2>Uppercase only</h2>
 *
 * Fifty-two shapes drawn and maintained by hand is a font project; twenty-six
 * is an afternoon. Caps also read better at HUD size, where a notice is
 * glanced at rather than read. Callers write ordinary strings and
 * {@link #at} uppercases them.
 *
 * <h2>What this is for</h2>
 *
 * The three refusals that currently have no legible channel -- not unlocked,
 * still on cooldown, cannot be used right now. They went to the action bar,
 * which has twenty-six writers across thirteen files and no arbiter, so
 * {@code AbilityInputs.bar()} overwrote each one within a tick or two. That is
 * the "flashes illegibly" report, exactly, and it is not a formatting problem.
 *
 * Drawing here does not arbitrate that surface. It stops using it.
 */
public final class HudLabel {

    /** Must match registry.json `label.base`. */
    public static final int BASE = 0xE600;
    /** Must match registry.json `label.stride`. */
    public static final int STRIDE = 64;
    /** Must match registry.json `label.characters`, in order. */
    public static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789 .,!?:-'";
    /** Must match registry.json `label.ascents`. */
    public static final List<Integer> ASCENTS =
            List.of(66, 73, 80);
    /** Five pixels of glyph plus vanilla's one of spacing. */
    public static final int ADVANCE = 6;

    /**
     * A space is the SPACE FONT's own, not a blank bitmap.
     *
     * Minecraft measures a bitmap glyph's advance from the bounding box of its
     * non-transparent pixels, so an all-transparent glyph advances about one
     * pixel rather than its declared width -- and a notice rendered as
     * ABILITYUNLOCKSATLEVEL2.
     *
     * That also moved the bars. The title is built to a net advance of zero so
     * its centre is the screen's centre; every collapsed space made the real
     * total five pixels shorter than the arithmetic assumed, the net went
     * NEGATIVE, and a centred component of negative width starts right of
     * centre. One missing advance, two unrelated-looking symptoms.
     */
    public static final int SPACE_ADVANCE = 4;

    private HudLabel() {}

    /**
     * The pixel width, counting exactly what {@link #at} will emit.
     *
     * An unknown character degrades to a space, so it must be MEASURED as a
     * space too. Counting it as a glyph is the same class of error that pushed
     * the bars right: the arithmetic and the emission have to agree, or the
     * net-zero title stops being net zero.
     */
    public static int width(String text) {
        int total = 0;
        for (char c : text.toUpperCase(Locale.ROOT).toCharArray())
            total += drawable(c) ? ADVANCE : SPACE_ADVANCE;
        return total;
    }

    private static boolean drawable(char c) {
        return c != ' ' && CHARACTERS.indexOf(c) >= 0;
    }

    /**
     * Translate into the glyph block for an ascent, uppercasing on the way.
     *
     * An unknown character becomes a space rather than throwing. A notice is
     * feedback, and a refusal that itself fails because someone wrote an
     * apostrophe the face lacks is worse than a notice with a gap in it --
     * which is not the trade the numeral makes, where a wrong digit would be a
     * wrong number.
     */
    public static String at(int ascent, String text) {
        int block = ASCENTS.indexOf(ascent);
        if (block < 0) throw new IllegalArgumentException(
                "No label block at ascent " + ascent + ". The pack carries: " + ASCENTS);
        String upper = text.toUpperCase(Locale.ROOT);
        var out = new StringBuilder(upper.length());
        for (char c : upper.toCharArray()) {
            if (c == ' ') { out.append(' '); continue; }   // the space font's own
            int index = CHARACTERS.indexOf(c);
            if (index < 0) { out.append(' '); continue; }
            out.append((char) (BASE + block * STRIDE + index));
        }
        return out.toString();
    }
}
