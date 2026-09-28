package com.minecraftmoba.plugin;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * HUD placement, by arithmetic.
 *
 * The pin to registry.json is the point: the plugin emits codepoints the pack
 * has to have mapped, and a mismatch renders as tofu rather than as a wrong
 * icon -- so nothing about it looks like a bug until somebody says the HUD is
 * blank.
 */
class HudLayerTest {

    @Test void theBaseAndTheLayerOrderMatchTheRegistry() throws Exception {
        String registry = Files.readString(Path.of("../resourcepack/registry.json"));
        assertTrue(registry.contains("\"base\": \"U+E200\""),
                "HudLayer.BASE is hard-coded and must match what the pack emits");
        for (String layer : HudLayer.LAYERS)
            assertTrue(registry.contains("\"" + layer + "\""),
                    "layer '" + layer + "' is referenced by the plugin and absent from the pack");
    }

    /**
     * Codepoints follow the layer list's order, and the build sorts by name.
     *
     * That is why the names carry an ordering prefix: a layer added later with
     * a name sorting into the middle would shift every codepoint after it
     * silently. The vitals glyphs took explicit codepoints for exactly this.
     */
    @Test void layerCodepointsAreAssignedInDeclaredOrder() {
        assertEquals("", HudLayer.glyph("a_baseline"));
        assertEquals("", HudLayer.glyph("d_abilities_over"));
        assertEquals(HudLayer.LAYERS, HudLayer.LAYERS.stream().sorted().toList(),
                "the build sorts layers by name, so the declared order must already be sorted");
    }

    @Test void anUnknownLayerIsRefusedAndNamesTheDeclaredOnes() {
        var thrown = assertThrows(IllegalArgumentException.class, () -> HudLayer.glyph("nope"));
        assertTrue(thrown.getMessage().contains("a_baseline"), thrown.getMessage());
    }

    // ---- horizontal placement --------------------------------------------

    /** Negative space maps U+F000+n to an advance of -n. */
    @Test void aSmallLeftShiftIsOneCharacter() {
        assertEquals("", HudLayer.left(10));
    }

    /** Beyond the font's range it is several characters, never one wrong one. */
    @Test void aLargeLeftShiftIsSplitAcrossTheMaximumStep() {
        String shift = HudLayer.left(300);
        assertEquals(2, shift.length());
        assertEquals((char) (0xF000 + HudLayer.MAX_STEP), shift.charAt(0));
        assertEquals((char) (0xF000 + 300 - HudLayer.MAX_STEP), shift.charAt(1));
    }

    /**
     * Rightward movement overshoots in spaces and comes back.
     *
     * There is no positive-advance glyph but the space itself, so this is the
     * shape the pack's own fonts force. It needs no extra provider, and
     * providers are the scarce thing.
     */
    @Test void aRightShiftOvershootsInSpacesAndReturns() {
        String shift = HudLayer.right(10);
        assertEquals(3, shift.chars().filter(c -> c == ' ').count(), "3 spaces is 12px");
        assertTrue(shift.endsWith(""), "and 2px back, for a net 10");
    }

    /** An exact multiple of a space needs no correction at all. */
    @Test void anAlignedRightShiftAddsNoNegativeSpace() {
        assertEquals("    ", HudLayer.right(16));
    }

    @Test void zeroIsNoMovement() {
        assertEquals("", HudLayer.left(0));
        assertEquals("", HudLayer.right(0));
    }

    @Test void placementPutsTheOffsetBeforeTheGlyph() {
        assertEquals(HudLayer.left(8) + HudLayer.glyph("b_hotbar_above"),
                HudLayer.at("b_hotbar_above", -8));
    }

    // ---- the pack must actually map what the plugin emits -----------------

    /**
     * Negative space must live in the SAME font as the glyphs.
     *
     * A text component carries exactly one font. The plugin interleaves unit
     * glyphs with U+F001 to correct their advance, so if negative space is
     * only in `moba:space`, every correction character is unmapped inside
     * `moba:glyphs` and the bar draws one tofu box per unit -- 128 of them per
     * vitals bar. That is what the first live look at the bar actually showed.
     */
    @Test void theGlyphFontCarriesItsOwnNegativeSpace() throws Exception {
        String build = Files.readString(Path.of("../resourcepack/build_pack.py"));
        assertTrue(build.contains("providers + [space_provider]"),
                "moba:glyphs must include the space provider, or every positioned "
                        + "glyph renders beside a tofu box");
    }

    /**
     * The font is written after every provider has been appended.
     *
     * It used to be written partway through, before the vitals units and these
     * layers were added, so their providers were computed and discarded. The
     * textures shipped; nothing mapped them.
     */
    @Test void theFontIsWrittenAfterEveryProviderIsAppended() throws Exception {
        String build = Files.readString(Path.of("../resourcepack/build_pack.py"));
        int lastAppend = build.lastIndexOf("providers.append(");
        int write = build.indexOf("write_json(assets / \"font\" / \"glyphs.json\"");
        assertTrue(write > lastAppend,
                "glyphs.json is written before the last providers.append, so those "
                        + "providers never reach the pack");
    }
}
