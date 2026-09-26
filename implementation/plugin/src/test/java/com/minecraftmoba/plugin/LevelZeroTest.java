package com.minecraftmoba.plugin;

import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Lv0 is a progression state, not bookkeeping: it holds the Passive alone, and
 * Lv1 earns Active 1. Enrolling at Lv1 would erase the first observable
 * progression event.
 */
class LevelZeroTest {
    @Test void freshPlayersEnrolAtLevelZero() {
        assertEquals(0, new PlayerData(UUID.randomUUID()).level);
    }

    @Test void levelZeroSurvivesTheSaveFormat() throws Exception {
        var uuid = UUID.randomUUID();
        var data = new PlayerData(uuid);
        data.classId = "mole";
        var back = PlayerDataCodec.decode(uuid, PlayerDataCodec.encode(data), 30);
        assertEquals(0, back.level);
        assertEquals("mole", back.classId);
    }

    /** Saves written before Lv0 existed carry level 1 and must still load. */
    @Test void earlierSavesStillDecode() throws Exception {
        var uuid = UUID.randomUUID();
        var data = new PlayerData(uuid);
        data.level = 1;
        assertEquals(1, PlayerDataCodec.decode(uuid, PlayerDataCodec.encode(data), 30).level);
    }

    @Test void negativeLevelsAreStillRejected() throws Exception {
        var uuid = UUID.randomUUID();
        var data = new PlayerData(uuid);
        data.level = -1;
        byte[] encoded = PlayerDataCodec.encode(data);
        assertThrows(IOException.class, () -> PlayerDataCodec.decode(uuid, encoded, 30));
    }
}
