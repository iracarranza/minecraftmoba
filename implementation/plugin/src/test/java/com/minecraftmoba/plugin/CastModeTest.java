package com.minecraftmoba.plugin;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * How a player commits an ability, and what that must not touch.
 */
class CastModeTest {

    @Test void quickIsTheDefaultAndTheOnlyOneWithoutAPreview() {
        assertEquals(CastMode.QUICK, CastMode.DEFAULT);
        assertFalse(CastMode.QUICK.previews());
        assertTrue(CastMode.HOLD.previews());
        assertTrue(CastMode.DOUBLE.previews());
    }

    /** The toggle cycles, so one click is always a change and never a dead end. */
    @Test void theCycleReturnsToWhereItStarted() {
        CastMode mode = CastMode.DEFAULT;
        for (int i = 0; i < CastMode.values().length; i++) mode = mode.next();
        assertEquals(CastMode.DEFAULT, mode);
        assertNotEquals(CastMode.DEFAULT, CastMode.DEFAULT.next());
    }

    /**
     * Parsing never throws.
     *
     * This is read from persisted player data and from a clickable item. An
     * unreadable cast mode should leave a player able to cast rather than
     * unable to log in.
     */
    @Test void anUnreadableValueFallsBackRatherThanFailing() {
        assertEquals(CastMode.DEFAULT, CastMode.of(null));
        assertEquals(CastMode.DEFAULT, CastMode.of(""));
        assertEquals(CastMode.DEFAULT, CastMode.of("nonsense"));
        assertEquals(CastMode.HOLD, CastMode.of("hold"));
        assertEquals(CastMode.HOLD, CastMode.of("  HoLd "));
    }

    /** Every mode is nameable, since the setting item shows both. */
    @Test void everyModeSaysWhatItIsAndWhatItDoes() {
        for (CastMode mode : CastMode.values()) {
            assertFalse(mode.label().isBlank(), mode.name());
            assertFalse(mode.description().isBlank(), mode.name());
            assertTrue(HudLabel.CHARACTERS.indexOf(
                            mode.label().toUpperCase(java.util.Locale.ROOT).charAt(0)) >= 0,
                    "a mode's name must be drawable if it ever reaches the HUD");
        }
    }

    /**
     * A preference outlives a match; class state does not.
     *
     * Cast mode lives in PlayerData.settings rather than classState, which a
     * reset clears. Clearing it would make every match begin by re-choosing an
     * input scheme.
     */
    @Test void theSettingSurvivesAMatchScopedClear() {
        var data = new PlayerData(java.util.UUID.randomUUID());
        data.settings.put("castMode", CastMode.DOUBLE.name());
        data.classState.put("branch.lunge", "stalking_pounce");

        data.classState.clear();   // what a reset does

        assertEquals(CastMode.DOUBLE, data.castMode());
        assertTrue(data.classState.isEmpty());
    }

    @Test void aFreshPlayerHasTheDefault() {
        assertEquals(CastMode.DEFAULT, new PlayerData(java.util.UUID.randomUUID()).castMode());
    }

    /** Settings survive a save and load, which is the point of being settings. */
    @Test void settingsRoundTripThroughTheCodec() throws Exception {
        var id = java.util.UUID.randomUUID();
        var data = new PlayerData(id);
        data.level = 7;
        data.settings.put("castMode", CastMode.HOLD.name());

        var decoded = PlayerDataCodec.decode(id, PlayerDataCodec.encode(data), 30);
        assertEquals(CastMode.HOLD, decoded.castMode());
        assertEquals(7, decoded.level, "and progression is untouched by the new block");
    }
}
