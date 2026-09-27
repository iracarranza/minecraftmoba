package com.minecraftmoba.plugin;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The bar's fill arithmetic, and the contract between plugin and resource pack.
 *
 * {@link #theRegistryAndThePluginAgreeOnCodepoints} is the important one. Every
 * other glyph in this pack is assigned a codepoint by its position in
 * registry.json and then hard-coded as a Java literal, so inserting one glyph
 * silently shifts the rest. The bars use an explicit base precisely to escape
 * that, and this test is what keeps the two declarations in step.
 */
class VitalsBarTest {

    // ---- fill arithmetic --------------------------------------------------

    @Test void emptyAndFullAreExact() {
        assertEquals(0, VitalsBar.level(0.0));
        assertEquals(VitalsBar.FILL_WIDTH, VitalsBar.level(1.0));
    }

    @Test void theFillIsProportional() {
        assertEquals(32, VitalsBar.level(0.5));
        assertEquals(16, VitalsBar.level(0.25));
        assertEquals(48, VitalsBar.level(0.75));
    }

    /**
     * Sixty-five levels, against the native row's twenty.
     *
     * The point of the whole exercise: a one-pixel change in the bar is a
     * distinguishable state, so the fill is as smooth as the bar is wide.
     */
    @Test void granularityIsOnePixel() {
        assertEquals(65, VitalsBar.LEVELS);
        assertNotEquals(VitalsBar.level(0.50), VitalsBar.level(0.51));
        assertNotEquals(VitalsBar.level(0.20), VitalsBar.level(0.22));
    }

    /**
     * A player who is alive never renders as an empty bar.
     *
     * Rounding alone would show a sliver of health as empty, which tells the
     * player they are dead and gets played as though they are.
     */
    @Test void aliveIsNeverEmpty() {
        assertEquals(1, VitalsBar.level(0.0001));
        assertEquals(1, VitalsBar.level(1, 10000));
        assertTrue(VitalsBar.level(0.001) > 0);
    }

    /**
     * Anything short of maximum gives up its last pixel.
     *
     * "Nearly full" versus "full" is the distinction a player checks before
     * deciding whether to go back, so it must survive rounding.
     */
    @Test void shortOfMaximumIsNeverFull() {
        assertEquals(VitalsBar.FILL_WIDTH - 1, VitalsBar.level(0.9999));
        assertTrue(VitalsBar.level(0.999) < VitalsBar.FILL_WIDTH);
        assertEquals(VitalsBar.FILL_WIDTH, VitalsBar.level(1.0), "only actually full reads full");
    }

    @Test void currentOverMaximumTracksCapacityRatherThanVanillasTwenty() {
        // The point of VitalsScaling: half health reads the same at any Capacity.
        assertEquals(VitalsBar.level(10, 20), VitalsBar.level(4.5, 9));
        assertEquals(VitalsBar.level(10, 20), VitalsBar.level(16, 32));
    }

    @Test void degenerateInputsDoNotThrow() {
        assertEquals(0, VitalsBar.level(5, 0));
        assertEquals(0, VitalsBar.level(5, -1));
        assertEquals(0, VitalsBar.level(Double.NaN));
        assertEquals(0, VitalsBar.level(-1.0));
        assertEquals(VitalsBar.FILL_WIDTH, VitalsBar.level(2.0), "over-full clamps");
    }

    // ---- the bar is one unit, repeated ------------------------------------

    /**
     * The invariant repetition buys: the bar is always the same width.
     *
     * With one image per fill level this could only have been asserted image
     * by image. Here it is structural, and this test says so.
     */
    @Test void theBarIsAlwaysFullWidthWhateverTheFill() {
        for (int level = 0; level <= VitalsBar.FILL_WIDTH; level++) {
            int lit = units(VitalsBar.filled(level), VitalsBar.UNIT_ON);
            int unlit = units(VitalsBar.unfilled(level), VitalsBar.UNIT_OFF);
            assertEquals(level, lit, "lit units at level " + level);
            assertEquals(VitalsBar.FILL_WIDTH, lit + unlit, "total width at level " + level);
        }
    }

    /** Every unit carries its -1 correction, or the bar is twice as wide as it should be. */
    @Test void everyUnitIsFollowedByTheAdvanceCorrection() {
        String filled = VitalsBar.filled(10);
        assertEquals(20, filled.length(), "ten units, each a glyph plus a back-one");
        for (int i = 0; i < filled.length(); i += 2) {
            assertEquals(VitalsBar.UNIT_ON, filled.substring(i, i + 1));
            assertEquals(VitalsBar.BACK_ONE, filled.substring(i + 1, i + 2));
        }
    }

    /** U+F001 is what the pack's space font maps to an advance of -1. */
    @Test void theCorrectionIsTheNegativeSpaceCharacterThePackDefines() {
        assertEquals(0xF001, VitalsBar.BACK_ONE.charAt(0));
    }

    @Test void emptyAndFullBarsAreStillFullWidth() {
        assertEquals(0, units(VitalsBar.filled(0), VitalsBar.UNIT_ON));
        assertEquals(VitalsBar.FILL_WIDTH, units(VitalsBar.unfilled(0), VitalsBar.UNIT_OFF));
        assertEquals(VitalsBar.FILL_WIDTH, units(VitalsBar.filled(VitalsBar.FILL_WIDTH), VitalsBar.UNIT_ON));
        assertEquals(0, units(VitalsBar.unfilled(VitalsBar.FILL_WIDTH), VitalsBar.UNIT_OFF));
    }

    @Test void theFourUnitsAreDistinctConsecutiveCodepoints() {
        assertEquals(VitalsBar.BASE,     VitalsBar.CAP_LEFT.charAt(0));
        assertEquals(VitalsBar.BASE + 1, VitalsBar.UNIT_ON.charAt(0));
        assertEquals(VitalsBar.BASE + 2, VitalsBar.UNIT_OFF.charAt(0));
        assertEquals(VitalsBar.BASE + 3, VitalsBar.CAP_RIGHT.charAt(0));
    }

    @Test void anOutOfRangeLevelIsRefusedRatherThanDrawingSomethingElse() {
        assertThrows(IllegalArgumentException.class, () -> VitalsBar.filled(-1));
        assertThrows(IllegalArgumentException.class, () -> VitalsBar.filled(VitalsBar.LEVELS));
        assertThrows(IllegalArgumentException.class, () -> VitalsBar.unfilled(VitalsBar.LEVELS));
    }

    private static int units(String run, String unit) {
        int n = 0;
        for (int i = 0; i < run.length(); i++) if (run.substring(i, i + 1).equals(unit)) n++;
        return n;
    }

    // ---- the contract with the pack ---------------------------------------

    @Test void theRegistryAndThePluginAgreeOnCodepoints() throws Exception {
        String registry = Files.readString(Path.of("../resourcepack/registry.json"));
        assertTrue(registry.contains("\"fill_width\": 64"),
                "VitalsBar.FILL_WIDTH is 64; the pack must generate that many levels");
        assertTrue(registry.contains(String.format("U+%04X", VitalsBar.BASE)),
                "registry.json must declare the base the plugin computes its units from");
        assertTrue(registry.contains("vitals_unit_on") && registry.contains("vitals_unit_off"),
                "the pack must generate the two units the plugin repeats");
    }

    /**
     * The bar glyphs must not be assigned sequentially.
     *
     * If they ever are, adding a single glyph anywhere above them shifts a
     * hundred and thirty codepoints at once and the bar silently draws
     * something else.
     */
    @Test void theBarsDeclareTheirOwnBaseRatherThanTakingTheNextIndex() throws Exception {
        String registry = Files.readString(Path.of("../resourcepack/registry.json"));
        assertTrue(registry.contains("codepoints_are_explicit"),
                "the reason these bases exist must stay written down beside them");
    }

    // ---- shipped config ---------------------------------------------------

    /**
     * Both switches ship off, and the ordering between them is the lesson.
     *
     * Hiding the native rows before the replacement is confirmed to render is
     * exactly how the previous glyph readout presented as "hunger is fully
     * invisible".
     */
    @Test void theBarAndTheRowHidingBothShipOff() throws Exception {
        var cfg = YamlConfiguration.loadConfiguration(new InputStreamReader(
                Objects.requireNonNull(getClass().getResourceAsStream("/config.yml"))));
        assertFalse(cfg.getBoolean("features.vitalsBar.enabled"),
                "unproven against a real client; tofu without the pack");
        assertFalse(cfg.getBoolean("features.vitalsBar.hideNativeRows"),
                "never hide the native rows before the replacement is confirmed");
        assertTrue(cfg.getLong("features.vitalsBar.refreshTicks") > 0);
        assertNotNull(cfg.getString("features.vitalsBar.colours.health"));
        assertNotNull(cfg.getString("features.vitalsBar.colours.hunger"));
    }
}
