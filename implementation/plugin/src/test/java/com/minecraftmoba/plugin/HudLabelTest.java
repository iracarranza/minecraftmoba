package com.minecraftmoba.plugin;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The notice face, and the two ways it differs from the numeral face.
 *
 * Size and error behaviour both differ, and both differences are decisions
 * rather than oversights.
 */
class HudLabelTest {

    @Test void theBlockLayoutMatchesThePack() throws Exception {
        String registry = Files.readString(Path.of("../resourcepack/registry.json"));
        assertTrue(registry.contains("\"base\": \"U+E600\""), "HudLabel.BASE is hard-coded");
        assertTrue(registry.contains("\"stride\": " + HudLabel.STRIDE));
    }

    /** Forty-four characters need a block wider than the numeral face's sixteen. */
    @Test void theStrideClearsTheCharacterSet() {
        assertTrue(HudLabel.CHARACTERS.length() <= HudLabel.STRIDE,
                "a block must hold every character, or the next ascent overlaps it");
        assertTrue(HudLabel.STRIDE > HudText.STRIDE,
                "the label face is much larger, so it cannot share the numeral's stride");
    }

    /** Callers write ordinary strings; the face is caps. */
    @Test void textIsUppercasedOnTheWayIn() {
        assertEquals(HudLabel.at(73, "ABILITY"), HudLabel.at(73, "ability"));
    }

    /**
     * An unknown character becomes a space rather than throwing.
     *
     * Deliberately the opposite of HudText, and the reason is what the two are
     * for: a refusal that itself fails because the face lacks an apostrophe is
     * worse than a notice with a gap in it, whereas a numeral with a missing
     * digit would be a wrong number.
     */
    @Test void anUnknownCharacterDegradesRatherThanThrowing() {
        assertDoesNotThrow(() -> HudLabel.at(73, "can't"));
        assertEquals(5, HudLabel.at(73, "can't").length(), "the gap keeps its width");
        assertThrows(IllegalArgumentException.class, () -> HudText.at(50, "x"),
                "the numeral face refuses, because a gap there would be a wrong number");
    }

    @Test void anUnknownAscentIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> HudLabel.at(999, "A"));
    }

    /** The notice sits above the ability row, so it needs heights the bars do not. */
    @Test void theNoticeReachesHigherThanTheBars() {
        assertTrue(HudLabel.ASCENTS.stream().max(Integer::compare).orElseThrow()
                        > VitalsBar.ASCENTS.stream().max(Integer::compare).orElseThrow(),
                "the refusal line sits above the ability row, not beside the vitals");
    }

    /** The shipped default has to exist, or the notice silently never draws. */
    @Test void theShippedNoticeHeightExists() throws Exception {
        var cfg = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(
                new java.io.InputStreamReader(java.util.Objects.requireNonNull(
                        getClass().getResourceAsStream("/config.yml"))));
        assertTrue(HudLabel.ASCENTS.contains(cfg.getInt("features.vitalsBar.noticeAscent")));
        assertNotNull(cfg.getString("features.vitalsBar.colours.notice"));
        assertTrue(cfg.getInt("features.vitalsBar.noticeTicks") > 0);
    }

    /** The three refusals are one line, because they are one kind of answer. */
    @Test void everyRefusalFitsTheBarWidth() {
        for (String message : java.util.List.of(
                "Ability unlocks at level 30", "On cooldown 12.5s", "Cannot be used right now"))
            assertTrue(HudLabel.width(message) < 320,
                    message + " is too wide to sit on one HUD line");
    }

    /**
     * Candidate heights are cheap in bytes and expensive in ATLAS AREA.
     *
     * Every tall glyph occupies 256 rows of the font atlas whatever it draws.
     * A 44-character face at ten candidate heights is 440 of them, and with
     * the other families that came to 606 needing ~930,000 px against a
     * 1024x1024 atlas's 1,048,576 -- the font stopped loading and EVERY glyph
     * rendered as tofu, including bars that had been working for an hour.
     *
     * So the search space has a ceiling. Three heights to sweep with, not ten.
     */
    @Test void theFamiliesTogetherStayInsideTheAtlasBudget() throws Exception {
        String registry = Files.readString(Path.of("../resourcepack/registry.json"));
        assertTrue(registry.contains("\"atlas_budget\""),
                "the build must refuse an overflowing font rather than ship one");

        int tall = HudLabel.ASCENTS.size() * HudLabel.CHARACTERS.length()
                 + HudText.ASCENTS.size() * HudText.CHARACTERS.length()
                 + VitalsBar.ASCENTS.size() * 6;
        assertTrue(tall <= 256,
                tall + " tall glyphs is over the budget; the font will not load and "
                        + "every glyph goes tofu, not just the new ones");
    }

    /**
     * The build must refuse an uppercase resource path.
     *
     * Resource locations must match [a-z0-9/._-]. An uppercase letter makes
     * the pack invalid and the FONT FAILS TO LOAD ENTIRELY -- every glyph goes
     * tofu, including families that were working, and nothing about the pack
     * looks wrong. `label_A.png` did exactly that.
     *
     * It was mistaken for an atlas-size problem first, because the symptom is
     * identical: the whole font stops rather than the new part of it. Which is
     * why the check belongs in the build, where it can name the offending path,
     * rather than in a diagnosis after the fact.
     */
    @Test void theBuildRefusesAnUppercaseResourcePath() throws Exception {
        String build = Files.readString(Path.of("../resourcepack/build_pack.py"));
        assertTrue(build.contains("[^a-z0-9:/._-]"),
                "build_pack.py must reject a path the client will refuse");
        assertTrue(build.contains("resource paths must be lowercase"),
                "and say why, since the symptom points nowhere near the cause");

        String registry = Files.readString(Path.of("../resourcepack/registry.json"));
        assertTrue(registry.contains("\"lowercase_paths\""),
                "the reason stays written down beside the names it constrains");
    }

    /**
     * A space is the space font's own, never a blank bitmap.
     *
     * Minecraft measures a bitmap glyph's advance from the bounding box of its
     * non-transparent pixels, so an all-transparent glyph advances about one
     * pixel rather than its declared width. The notice rendered as
     * ABILITYUNLOCKSATLEVEL2 -- and the same missing advance moved the health
     * and hunger bars, because the title is built to a net advance of zero and
     * every collapsed space made the real total shorter than the arithmetic
     * assumed. The net went negative, and a centred component of negative
     * width starts RIGHT of centre.
     *
     * One missing advance, two symptoms that looked unrelated.
     */
    @Test void spacesUseTheSpaceFontRatherThanABlankGlyph() {
        String drawn = HudLabel.at(73, "A B");
        assertEquals(' ', drawn.charAt(1), "the middle character must be a real space");
        assertNotEquals(' ', drawn.charAt(0));
    }

    /**
     * Width must equal the sum of what {@link HudLabel#at} actually emits.
     *
     * This is the invariant the whole layout rests on: the title is built to a
     * net advance of zero, so any disagreement between the arithmetic and the
     * emission moves the centre and takes the health and hunger bars with it.
     *
     * Stated as a sum over the string rather than as a comparison between a
     * space and a glyph, because at 3x5 the two advances happen to coincide --
     * so the old form of this test would now pass whatever the code did. The
     * discriminating check lives in the emission test above.
     */
    @Test void widthEqualsWhatIsEmitted() {
        for (String text : java.util.List.of("A B", "ON COOLDOWN 12.5S", "@", "  ", "ABILITY")) {
            int expected = 0;
            for (char c : text.toUpperCase(java.util.Locale.ROOT).toCharArray())
                expected += (c != ' ' && HudLabel.CHARACTERS.indexOf(c) >= 0)
                        ? HudLabel.ADVANCE : HudLabel.SPACE_ADVANCE;
            assertEquals(expected, HudLabel.width(text), text);
            assertEquals(text.length(), HudLabel.at(73, text).length(),
                    "one emitted character per input character, or the sum is wrong");
        }
    }

    /**
     * An unknown character degrades to a real space, and is MEASURED as one.
     *
     * Counting it as a glyph would be the same class of error that pushed the
     * bars right -- the arithmetic and the emission have to agree, or the
     * net-zero title stops being net zero.
     */
    @Test void anUnknownCharacterDegradesToARealSpaceAndIsMeasuredAsOne() {
        assertEquals(' ', HudLabel.at(73, "@").charAt(0), "@ is not in the face");
        assertEquals(HudLabel.SPACE_ADVANCE, HudLabel.width("@"));
    }

    /**
     * Three lines on one canvas, at three distinct heights.
     *
     * Combat, the refusal line and the vitals all draw into the same bossbar
     * title, because bars stack and each has its own baseline -- one canvas is
     * what makes an ascent mean one thing. They must not land on each other.
     */
    @Test void theThreeLinesSitAtDistinctHeights() throws Exception {
        var cfg = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(
                new java.io.InputStreamReader(java.util.Objects.requireNonNull(
                        getClass().getResourceAsStream("/config.yml"))));
        int bar = cfg.getInt("features.vitalsBar.ascent");
        int numeral = cfg.getInt("features.vitalsBar.numeralAscent");
        int notice = cfg.getInt("features.vitalsBar.noticeAscent");

        int clock = cfg.getInt("features.vitalsBar.clockAscent");
        assertEquals(4, java.util.Set.of(bar, numeral, clock, notice).size(),
                "two lines at one ascent draw on top of each other");
        assertTrue(HudLabel.ASCENTS.contains(clock), "the clock label needs a block to draw in");
        assertNotNull(cfg.getString("features.vitalsBar.colours.clock"));
        assertTrue(HudLabel.ASCENTS.contains(notice), "the refusal line needs a block to draw in");
        // Only the numeral's relation to its bar is structural -- it annotates
        // it and must sit just above it. Where the clock label and the refusal
        // line go relative to each other is a LAYOUT choice, and it changed
        // once already: the clock moved from just above the bars to near the
        // ability row, which put it above the refusal line. Asserting a fixed
        // order would have made that a test failure rather than a decision.
        assertTrue(numeral > bar && numeral - bar < 20,
                "the numeral annotates its bar and must sit just above it");
        assertTrue(clock > bar && notice > bar, "nothing draws under the bars");
        assertNull(cfg.getString("features.vitalsBar.colours.combat"),
                "combat is a BOSSBAR, not a line here -- InWorldSelection owns it");
    }

    /** Every message the readouts can produce must be drawable. */
    @Test void everyReadoutStringIsInTheFace() {
        for (String text : java.util.List.of(
                "ABILITY UNLOCKS AT LEVEL 30", "IN COMBAT",
                "3 UNSPENT LEVEL POINTS AVAILABLE - CROUCH TO SUMMON, M2 TO SELECT",
                "ON COOLDOWN 12.5S", "CANNOT BE USED RIGHT NOW"))
            for (char c : text.toCharArray())
                assertTrue(c == ' ' || HudLabel.CHARACTERS.indexOf(c) >= 0,
                        "'" + c + "' in \"" + text + "\" would render as a gap");
    }

    /**
     * Every line the canvas can draw is actually reached by the code.
     *
     * The clock label shipped once with its ascent configured, its colour
     * configured, its block present in the pack -- and no code emitting it,
     * because the edit that added it aborted before the file was written. The
     * bar's colour and progress worked, so the feature looked half-built
     * rather than half-applied.
     *
     * Config and pack agreeing proves nothing on their own; this asserts the
     * renderer names them.
     */
    @Test void theRendererActuallyDrawsEveryConfiguredLine() throws Exception {
        String vitals = Files.readString(Path.of(
                "src/main/java/com/minecraftmoba/plugin/VitalsDisplay.java"));
        for (String call : java.util.List.of(
                "HudLabel.at(clockAscent()", "HudLabel.at(notice.ascent()",
                "HudText.at(numeralAscent"))
            assertTrue(vitals.contains(call),
                    call + " is configured but never emitted -- the line will not draw");
    }
}
