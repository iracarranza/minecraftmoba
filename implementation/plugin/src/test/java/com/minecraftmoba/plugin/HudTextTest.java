package com.minecraftmoba.plugin;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Text that can leave the bossbar's line.
 *
 * The thing being tested is a correspondence: the plugin emits codepoints and
 * the pack has to have drawn them. A mismatch renders as tofu, which looks
 * like nothing rather than like a mistake -- this HUD has produced tofu four
 * separate ways, and every one of them was found by looking at a screenshot.
 */
class HudTextTest {

    private String registry() throws Exception {
        return Files.readString(Path.of("../resourcepack/registry.json"));
    }

    @Test void theBlockLayoutMatchesThePack() throws Exception {
        String registry = registry();
        assertTrue(registry.contains("\"base\": \"U+E400\""), "HudText.BASE is hard-coded");
        assertTrue(registry.contains("\"stride\": " + HudText.STRIDE));
        for (char c : HudText.CHARACTERS.toCharArray())
            assertTrue(registry.contains("\"" + c + "\""),
                    "the pack draws no '" + c + "', which the plugin will emit");
    }

    /** Blocks are STRIDE apart, and eleven characters must fit inside one. */
    @Test void aBlockHoldsItsCharactersWithoutColliding() {
        assertTrue(HudText.CHARACTERS.length() <= HudText.STRIDE,
                "a block must hold every character, or the next ascent overlaps it");
        assertEquals((char) (0xE400 + HudText.STRIDE),
                HudText.at(HudText.ASCENTS.get(1), "0").charAt(0));
    }

    @Test void charactersTranslateInDeclaredOrder() {
        assertEquals((char) 0xE400, HudText.at(HudText.ASCENTS.getFirst(), "0").charAt(0));
        assertEquals((char) (0xE400 + 10), HudText.at(HudText.ASCENTS.getFirst(), "/").charAt(0),
                "the slash is last");
    }

    /** The whole point: the same string differs per ascent. */
    @Test void theSameTextDiffersByAscent() {
        int low = HudText.ASCENTS.getFirst(), high = HudText.ASCENTS.getLast();
        assertNotEquals(HudText.at(low, "1900"), HudText.at(high, "1900"));
        assertEquals(HudText.at(low, "1900").length(), HudText.at(high, "1900").length());
    }

    /**
     * A character with no shape is refused rather than emitted.
     *
     * Emitting it would produce tofu, and tofu is indistinguishable from
     * nothing being drawn at all.
     */
    @Test void anUndrawableCharacterIsRefused() {
        assertFalse(HudText.canDraw("1900 HP"));
        var thrown = assertThrows(IllegalArgumentException.class, () -> HudText.at(50, "HP"));
        assertTrue(thrown.getMessage().contains("0123456789"), thrown.getMessage());
    }

    @Test void anUnknownAscentIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> HudText.at(999, "0"));
    }

    /** Width is what centring a numeral on a bar depends on. */
    @Test void widthIsAdvanceTimesLength() {
        assertEquals(9 * HudText.ADVANCE, HudText.width("1900/1900"));
        assertTrue(HudText.width("1900/1900") < VitalsBar.INLINE.fillWidth(),
                "the numeral has to fit inside the narrower bar, or it cannot be centred on it");
    }

    /**
     * Text offers every height the bars do, and more besides.
     *
     * A superset rather than a match: the numeral sits a few pixels ABOVE its
     * bar rather than on the same line, so it needs finer steps than the bars
     * ever will. But every bar height must still be reachable, or a numeral
     * could not be put level with one if that were ever wanted.
     */
    @Test void everyBarHeightIsAnnotatableBySomeFace() {
        // Either face will do, and which one is the annotation's own choice:
        // the vitals bars carry a NUMERAL (HudText, 3x5 digits) while the
        // charge meter carries a percentage and a band word (HudLabel, which
        // has letters). Requiring one face to cover every bar height would
        // force both faces to carry every height, at 44 tall glyphs each.
        for (int ascent : VitalsBar.ASCENTS)
            assertTrue(HudText.ASCENTS.contains(ascent) || HudLabel.ASCENTS.contains(ascent),
                    "a bar at ascent " + ascent + " could never be annotated");
        assertTrue(HudText.ASCENTS.size() > VitalsBar.ASCENTS.size() - 1,
                "the numeral still needs finer steps than the bars, for the offset "
                        + "between a bar and the numeral sitting on it");
    }

    /** The shipped default pairing has to exist on both sides. */
    @Test void theShippedBarAndNumeralHeightsBothExist() throws Exception {
        var cfg = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(
                new java.io.InputStreamReader(java.util.Objects.requireNonNull(
                        getClass().getResourceAsStream("/config.yml"))));
        assertTrue(VitalsBar.ASCENTS.contains(cfg.getInt("features.vitalsBar.ascent")));
        assertTrue(HudText.ASCENTS.contains(cfg.getInt("features.vitalsBar.numeralAscent")));
    }
}
