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
        assertEquals(VitalsBar.FILL_WIDTH / 2, VitalsBar.level(0.5));
        assertEquals(VitalsBar.FILL_WIDTH / 4, VitalsBar.level(0.25));
        assertEquals(VitalsBar.FILL_WIDTH * 3 / 4, VitalsBar.level(0.75));
    }

    /**
     * Sixty-five levels, against the native row's twenty.
     *
     * The point of the whole exercise: a one-pixel change in the bar is a
     * distinguishable state, so the fill is as smooth as the bar is wide.
     */
    @Test void granularityIsOnePixel() {
        assertEquals(VitalsBar.FILL_WIDTH + 1, VitalsBar.LEVELS);
        assertNotEquals(VitalsBar.level(0.50), VitalsBar.level(0.51));
        assertNotEquals(VitalsBar.level(0.20), VitalsBar.level(0.21));
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


    // ---- reference ticks ---------------------------------------------------

    /**
     * The defect ticks exist to repair.
     *
     * A proportional bar shows a fraction, and a fraction alone cannot tell a
     * 1,000-health character from a 3,200-health one -- both are a full bar.
     * That gives back exactly what the x100 player-facing scale was adopted to
     * buy, so the bar has to carry magnitude somehow.
     */
    @Test void aBiggerCharacterCarriesMoreTicks() {
        int small = VitalsBar.tickUnits(1000, 100).size();
        int large = VitalsBar.tickUnits(3200, 100).size();
        assertEquals(9, small, "1,000 health is ten intervals, so nine interior marks");
        assertEquals(31, large, "3,200 health is thirty-two intervals");
        assertTrue(large > small, "magnitude has to be visible or x100 is given back");
    }

    /** Fixed width, denser ticks -- never a shorter bar for a smaller character. */
    @Test void theBarWidthNeverDependsOnMaximumHealth() {
        for (double max : new double[]{1000, 2000, 2750, 3200}) {
            var ticks = VitalsBar.tickUnits(max, 100);
            int lit = units(VitalsBar.filled(40, ticks), VitalsBar.UNIT_ON)
                    + units(VitalsBar.filled(40, ticks), VitalsBar.TICK_ON);
            int unlit = units(VitalsBar.unfilled(40, ticks), VitalsBar.UNIT_OFF)
                      + units(VitalsBar.unfilled(40, ticks), VitalsBar.TICK_OFF);
            assertEquals(VitalsBar.FILL_WIDTH, lit + unlit, "width at max " + max);
        }
    }

    /**
     * Ticks are indexed along the whole bar, so they do not move as the fill does.
     *
     * Indexing per run would slide every tick each time the player took damage,
     * which is the opposite of a reference mark.
     */
    @Test void ticksStayPutAsTheFillMoves() {
        var ticks = VitalsBar.tickUnits(3200, 100);
        int atFull = units(VitalsBar.filled(VitalsBar.FILL_WIDTH, ticks), VitalsBar.TICK_ON);
        int atHalfLit = units(VitalsBar.filled(VitalsBar.FILL_WIDTH / 2, ticks), VitalsBar.TICK_ON);
        int atHalfUnlit = units(VitalsBar.unfilled(VitalsBar.FILL_WIDTH / 2, ticks), VitalsBar.TICK_OFF);
        assertEquals(ticks.size(), atFull);
        assertEquals(ticks.size(), atHalfLit + atHalfUnlit,
                "every tick appears exactly once, on whichever side of the fill edge it falls");
    }

    /** Evenly spread when the spacing is not a whole number: 1,000 is 12.8 units apart. */
    @Test void ticksAreEvenlySpreadWhenSpacingIsFractional() {
        var ticks = new java.util.TreeSet<>(VitalsBar.tickUnits(1000, 100));
        assertEquals(9, ticks.size());
        int previous = 0;
        for (int tick : ticks) {
            int gap = tick - previous;
            assertTrue(gap >= 12 && gap <= 13, "gap " + gap + " should be 12 or 13, not bunched");
            previous = tick;
        }
    }

    /** Too dense to read is drawn as no scale, rather than as a solid band of marks. */
    @Test void anUnreadableScaleIsNotDrawn() {
        assertTrue(VitalsBar.tickUnits(3200, 10).isEmpty(), "320 ticks across 128 units");
        assertFalse(VitalsBar.tickUnits(3200, 100).isEmpty(), "32 ticks is 4px apart and fine");
    }

    @Test void degenerateTickInputsGiveNoTicks() {
        assertTrue(VitalsBar.tickUnits(3200, 0).isEmpty());
        assertTrue(VitalsBar.tickUnits(3200, -100).isEmpty());
        assertTrue(VitalsBar.tickUnits(0, 100).isEmpty());
        assertTrue(VitalsBar.tickUnits(50, 100).isEmpty(), "below one interval there is nothing to mark");
    }

    // ---- the contract with the pack ---------------------------------------

    @Test void theRegistryAndThePluginAgreeOnCodepoints() throws Exception {
        String registry = Files.readString(Path.of("../resourcepack/registry.json"));
        assertTrue(registry.contains("\"fill_width\": " + VitalsBar.FILL_WIDTH),
                "registry.json and VitalsBar.FILL_WIDTH must state the same bar width");
        assertTrue(registry.contains(String.format("U+%04X", VitalsBar.BASE)),
                "registry.json must declare the base the plugin computes its units from");
        for (String unit : new String[]{"vitals_unit_on", "vitals_unit_off",
                                        "vitals_tick_on", "vitals_tick_off"})
            assertTrue(registry.contains(unit), "the pack must generate " + unit);
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
        assertEquals(100, cfg.getInt("features.vitalsBar.displayScale"),
                "x100 is the decided player-facing scale");
        assertTrue(cfg.getInt("features.vitalsBar.tickInterval") > 0);
        assertTrue(cfg.getBoolean("features.vitalsBar.showNumerals"),
                "nothing reads an exact total off a bar; the numeral is what x100 was for");
    }
}
