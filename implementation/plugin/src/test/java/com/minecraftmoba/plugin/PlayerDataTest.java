package com.minecraftmoba.plugin;
import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class PlayerDataTest {
    @Test void roundTripPersistsChoicesButNotMode() throws Exception {
        var d = new PlayerData(UUID.randomUUID());
        d.classId = "mole"; d.level = 8; d.xp = 41;
        d.choices.add(new PlayerData.ChoiceRecord(4, "opaque-choice"));
        d.modeState.active = true; d.modeState.expiresAt = 500;
        var loaded = PlayerDataCodec.decode(d.uuid, PlayerDataCodec.encode(d), 30);
        assertEquals(d.classId, loaded.classId); assertEquals(d.level, loaded.level);
        assertEquals(d.xp, loaded.xp); assertEquals(d.choices, loaded.choices);
        assertFalse(loaded.modeState.active); assertEquals(0, loaded.modeState.expiresAt);
        assertThrows(IOException.class, () -> PlayerDataCodec.decode(UUID.randomUUID(), PlayerDataCodec.encode(d), 30));
    }
    @Test void capacityIsPureAndResetReturnsBaseline() {
        var settings = new Capacity.Settings(new Capacity.Curve(9,1,20), new Capacity.Curve(9,1,20),
            new Capacity.Curve(6,3,36), Set.of(2,4,7,8,9,11,13,14,15,16,17),
            Map.of("test", new Capacity.Bonus(1,1,3)));
        var choices = List.of(new PlayerData.ChoiceRecord(4,"test"));
        assertEquals(new Capacity.DerivedCapacity(9,9,6), Capacity.recompute(1,choices,settings));
        assertEquals(new Capacity.DerivedCapacity(10,10,9), Capacity.recompute(2,choices,settings));
        assertEquals(new Capacity.DerivedCapacity(12,12,15), Capacity.recompute(4,choices,settings));
        assertEquals(new Capacity.DerivedCapacity(20,20,36), Capacity.recompute(30,choices,settings));
        assertEquals(new Capacity.DerivedCapacity(9,9,6), Capacity.recompute(1,List.of(),settings));
    }
}
