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
     * The ORDERING is the invariant, not either switch's value.
     *
     * The bar is now on -- step one of the procedure the config describes.
     * Row hiding stays off until it is confirmed to render against a real
     * client, because hiding the native rows before the replacement is
     * confirmed is exactly how the previous glyph readout presented as
     * "hunger is fully invisible".
     *
     * So this no longer asserts that both ship off. It asserts the thing that
     * must hold at EVERY step: the native rows are never hidden unless a
     * replacement is drawing. That survives turning the bar on, and it is the
     * clause the earlier failure actually violated.
     */
    @Test void theNativeRowsAreNeverHiddenWithoutAReplacement() throws Exception {
        var cfg = YamlConfiguration.loadConfiguration(new InputStreamReader(
                Objects.requireNonNull(getClass().getResourceAsStream("/config.yml"))));
        if (cfg.getBoolean("features.vitalsBar.hideNativeRows"))
            assertTrue(cfg.getBoolean("features.vitalsBar.enabled"),
                    "hiding the native rows with no replacement drawing is the "
                            + "'hunger is fully invisible' defect, exactly");
        // Step five now: the bars are confirmed drawing, so the rows they
        // duplicate are hidden. The glyph bars are drawn INTO the slot those
        // rows occupied, which is why both halves had to move together.
        assertTrue(cfg.getBoolean("features.vitalsBar.hideNativeRows"),
                "the glyph bars now occupy the native rows' own position");
        assertTrue(cfg.getLong("features.vitalsBar.refreshTicks") > 0);
        assertNotNull(cfg.getString("features.vitalsBar.colours.health"));
        assertNotNull(cfg.getString("features.vitalsBar.colours.hunger"));
        assertEquals(100, cfg.getInt("features.vitalsBar.displayScale"),
                "x100 is the decided player-facing scale");
        // Ticks are OFF. They were a way to read magnitude off a bar that
        // carried no number; the numeral now sits on the bar itself, which
        // does that job exactly rather than by density. Zero is the documented
        // way to disable them -- tickUnits already returns nothing for a
        // non-positive interval -- so this is a setting, not a missing key.
        assertEquals(0, cfg.getInt("features.vitalsBar.tickInterval"),
                "the numeral replaced what the ticks were for");
        assertTrue(VitalsBar.INLINE.tickUnits(1900, 0).isEmpty(),
                "and a zero interval really does draw none");
        assertTrue(cfg.getBoolean("features.vitalsBar.showNumerals"),
                "nothing reads an exact total off a bar; the numeral is what x100 was for");
    }

    // ---- the two scales must not be mixed ---------------------------------

    /**
     * A full bar reads as full, never as more than full.
     *
     * Under scaling every player's attribute is DISPLAY_MAX, and Capacity
     * decides what a point is worth. So health arrives on the 0..20 display
     * scale while Capacity is in effective points, and dividing one by the
     * other reported a level 30 Lightfooted at 2000/1900 -- 105% of their own
     * maximum, at full health.
     *
     * The fill is measured against DISPLAY_MAX; the numeral converts back
     * through Vitals. Both ends then agree.
     */
    @Test void fullHealthIsNeverMoreThanTheMaximum() {
        double capacity = 19.0;                       // Lightfooted at Lv30
        double displayed = Vitals.DISPLAY_MAX;        // what the attribute allows

        assertEquals(VitalsBar.FILL_WIDTH, VitalsBar.level(displayed, Vitals.DISPLAY_MAX));
        assertEquals(Math.round(capacity * 100),
                Math.round(Vitals.toEffective(displayed, capacity) * 100),
                "1900/1900, not 2000/1900");
    }

    /** And the conversion is not a special case for the full bar alone. */
    @Test void halfABarReadsAsHalfTheCapacity() {
        double capacity = 19.0;
        double displayed = Vitals.DISPLAY_MAX / 2;
        assertEquals(950, Math.round(Vitals.toEffective(displayed, capacity) * 100));
        assertEquals(VitalsBar.FILL_WIDTH / 2, VitalsBar.level(displayed, Vitals.DISPLAY_MAX));
    }

    /**
     * The two layouts, and the cost of the one that fits.
     *
     * 81 is what the native row occupies; 128 is what the ticks were sized
     * for. At 81 the guard in tickUnits refuses marks past 27 of them, so a
     * character above 2,700 displayed health silently loses its scale --
     * Mole's Lv30 3,200 being exactly that character.
     */
    @Test void theInlineLayoutTradesTicksForFittingTheNativeRow() {
        assertEquals(81, VitalsBar.INLINE.fillWidth());
        assertEquals(VitalsBar.FILL_WIDTH, VitalsBar.WIDE.fillWidth());

        assertFalse(VitalsBar.INLINE.tickUnits(3200, 100).isEmpty()
                        == VitalsBar.WIDE.tickUnits(3200, 100).isEmpty(),
                "the two layouts must disagree at 3,200, or the trade is not real");
        assertTrue(VitalsBar.INLINE.tickUnits(3200, 100).isEmpty(),
                "81/3 is 27 marks, and 3,200 wants 32");
        assertFalse(VitalsBar.WIDE.tickUnits(3200, 100).isEmpty(),
                "128/3 is 42, so the wide bar keeps them");
    }

    /** Below the threshold both layouts still carry a scale. */
    @Test void bothLayoutsTickForAnOrdinaryCharacter() {
        assertFalse(VitalsBar.INLINE.tickUnits(1900, 100).isEmpty());
        assertFalse(VitalsBar.WIDE.tickUnits(1900, 100).isEmpty());
    }

    /** A full bar is full at either width, and short is never full. */
    @Test void theFillRulesHoldAtBothWidths() {
        for (var style : java.util.List.of(VitalsBar.INLINE, VitalsBar.WIDE)) {
            assertEquals(style.fillWidth(), style.level(20, 20));
            assertEquals(style.fillWidth() - 1, style.level(19.99, 20), "short is never full");
            assertEquals(1, style.level(0.0001, 20), "alive is never empty");
            assertEquals(0, style.level(0, 20));
        }
    }

    /**
     * The pack and the plugin must agree about the native rows.
     *
     * Two switches, one decision. publish.py used to call build() with the
     * default and never hid anything, so the plugin could be configured to
     * replace the rows while the pack kept drawing them -- and the symptom of
     * that is a native row, which looks exactly like a native row.
     */
    @Test void theRegistryAndTheConfigAgreeAboutHidingTheNativeRows() throws Exception {
        var cfg = YamlConfiguration.loadConfiguration(new InputStreamReader(
                Objects.requireNonNull(getClass().getResourceAsStream("/config.yml"))));
        String registry = Files.readString(Path.of("../resourcepack/registry.json"));
        boolean packHides = registry.contains("\"hide_native_rows\": true");
        assertEquals(cfg.getBoolean("features.vitalsBar.hideNativeRows"), packHides,
                "the pack blanks the rows and the plugin replaces them; one without "
                        + "the other is either a doubled readout or a missing one");
    }

    /**
     * Every ascent the plugin offers must be a set the pack emitted.
     *
     * A bar at an ascent with no provider renders as tofu, which looks like
     * nothing rather than like a misconfiguration -- the failure this HUD has
     * now produced four separate times.
     */
    @Test void everyOfferedAscentExistsInThePack() throws Exception {
        String registry = Files.readString(Path.of("../resourcepack/registry.json"));
        for (int ascent : VitalsBar.ASCENTS)
            assertTrue(registry.contains(String.valueOf(ascent)),
                    "no unit set at ascent " + ascent);
        assertTrue(registry.contains("\"stride\": " + VitalsBar.STRIDE));
    }

    /** Sets are STRIDE apart, so no two ascents can claim the same codepoint. */
    @Test void unitSetsDoNotCollide() {
        assertTrue(VitalsBar.STRIDE > 6, "six units per set, so the stride must clear them");
        for (int i = 1; i < VitalsBar.ASCENTS.size(); i++)
            assertEquals(VitalsBar.STRIDE,
                    VitalsBar.setFor(VitalsBar.ASCENTS.get(i))
                            - VitalsBar.setFor(VitalsBar.ASCENTS.get(i - 1)));
    }

    /** An ascent the pack does not carry is refused, not drawn as tofu. */
    @Test void anUnknownAscentIsRefusedAtConstruction() {
        var thrown = assertThrows(IllegalArgumentException.class,
                () -> VitalsBar.INLINE.withAscent(999));
        assertTrue(thrown.getMessage().contains("20"), thrown.getMessage());
    }

    /** Changing ascent changes which glyphs are emitted, and nothing else. */
    @Test void ascentShiftsTheCodepointsAndKeepsTheGeometry() {
        var low = VitalsBar.INLINE.withAscent(20);
        var high = VitalsBar.INLINE.withAscent(73);
        assertEquals(low.fillWidth(), high.fillWidth());
        assertEquals(low.filled(10, java.util.Set.of()).length(),
                     high.filled(10, java.util.Set.of()).length());
        assertNotEquals(low.filled(10, java.util.Set.of()),
                        high.filled(10, java.util.Set.of()));
    }
}
