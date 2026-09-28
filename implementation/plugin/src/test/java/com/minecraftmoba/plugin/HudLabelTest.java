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
}
