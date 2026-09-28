package com.minecraftmoba.plugin;

import java.util.List;

/**
 * Text that can be drawn at a chosen height.
 *
 * <h2>Why ordinary text will not do</h2>
 *
 * A bossbar title is the only persistent surface a plugin has, and negative
 * space can move anything along it. Nothing moves ordinary text DOWN.
 *
 * Vertical placement is {@code ascent}, and {@code ascent} is a property of a
 * font provider. Default-font text carries vanilla's, so it renders on the
 * bossbar's own line and stays there however it is nudged -- which is why the
 * health numeral sat at the top of the screen while its bar sat above the
 * hotbar, and why that was never a positioning bug to fix.
 *
 * So text that must sit somewhere specific has to be <b>glyphs in a pack font
 * declared at that ascent</b>. This translates a string into them.
 *
 * <h2>Why the pack draws its own shapes</h2>
 *
 * Vanilla's {@code ascii.png} cannot be borrowed. A provider's ascent may not
 * exceed its height, and reaching the hotbar needs an ascent near 50; vanilla's
 * sheet is 8px cells, which caps ascent at 8. The cell has to be tall with the
 * glyph in its foot, so the shapes are drawn in {@code build_pack.py}.
 *
 * <h2>Scope</h2>
 *
 * Digits and a slash: what the health numeral needs. The mechanism is
 * identical for letters, and the ability-refusal line is the next consumer --
 * at which point this becomes the answer to the action bar's twenty-six
 * competing writers rather than an arbiter for them.
 */
public final class HudText {

    /** Must match registry.json `text.base`. */
    public static final int BASE = 0xE400;
    /** Must match registry.json `text.stride`. */
    public static final int STRIDE = 16;
    /** Must match registry.json `text.characters`, in order. */
    public static final String CHARACTERS = "0123456789/";
    /** Must match registry.json `text.ascents`. */
    public static final List<Integer> ASCENTS =
            List.of(20, 30, 40, 50, 52, 54, 56, 58, 60, 66, 73);

    /**
     * Pixels one character advances: its width plus vanilla's one of spacing.
     *
     * Three pixels of glyph plus vanilla's one of spacing.
     *
     * Fixed rather than per-character, because the digits are drawn to one
     * width. A proportional face would need a table and would buy nothing for
     * a numeral, where columns lining up matters more than tight spacing.
     */
    public static final int ADVANCE = 4;

    private HudText() {}

    /** The pixel width of a string, for centring it on something. */
    public static int width(String text) { return text.length() * ADVANCE; }

    /**
     * Translate a string into the glyph block for one ascent.
     *
     * Refuses a character the pack has no shape for, rather than emitting a
     * codepoint that renders as tofu. Tofu looks like nothing rather than like
     * a mistake, and this HUD has produced it four separate ways already.
     */
    public static String at(int ascent, String text) {
        int block = ASCENTS.indexOf(ascent);
        if (block < 0) throw new IllegalArgumentException(
                "No text block at ascent " + ascent + ". The pack carries: " + ASCENTS);
        var out = new StringBuilder(text.length());
        for (char c : text.toCharArray()) {
            int index = CHARACTERS.indexOf(c);
            if (index < 0) throw new IllegalArgumentException(
                    "No glyph for '" + c + "'. The pack carries: " + CHARACTERS);
            out.append((char) (BASE + block * STRIDE + index));
        }
        return out.toString();
    }

    /** Whether every character of a string can be drawn. */
    public static boolean canDraw(String text) {
        return text.chars().allMatch(c -> CHARACTERS.indexOf(c) >= 0);
    }
}
