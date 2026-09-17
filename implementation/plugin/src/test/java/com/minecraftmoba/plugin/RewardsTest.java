package com.minecraftmoba.plugin;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class RewardsTest {
    @Test void pendingSurvivesRoundTripAndChoiceSpendsExactlyOneLevel() throws Exception {
        var option=new RewardCatalog.Option("fixture","Test","PAPER",new Capacity.Bonus(1,0,0));
        var catalog=new RewardCatalog(Map.of(2,List.of(option),4,List.of(option)),Map.of("fixture",option.bonus()));
        var d=new PlayerData(UUID.randomUUID()); d.level=4;
        assertEquals(List.of(2,4),catalog.pending(d));
        d.choices.add(new PlayerData.ChoiceRecord(2,"fixture"));
        var restored=PlayerDataCodec.decode(d.uuid,PlayerDataCodec.encode(d),30);
        assertEquals(List.of(4),catalog.pending(restored));
        restored.level=1; assertTrue(catalog.pending(restored).isEmpty());
        restored.level=4; assertEquals(List.of(4),catalog.pending(restored));
        assertTrue(new RewardCatalog(Map.of(),Map.of()).pending(restored).isEmpty());
    }
}
