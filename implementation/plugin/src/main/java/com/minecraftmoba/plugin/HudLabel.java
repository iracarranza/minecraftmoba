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
            List.of(50, 60, 66, 70, 73, 76, 80, 86, 92, 100);
    /** Five pixels of glyph plus vanilla's one of spacing. */
    public static final int ADVANCE = 6;

    private HudLabel() {}

    public static int width(String text) { return text.length() * ADVANCE; }

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
            int index = CHARACTERS.indexOf(c);
            if (index < 0) index = CHARACTERS.indexOf(' ');
            out.append((char) (BASE + block * STRIDE + index));
        }
        return out.toString();
    }
}
