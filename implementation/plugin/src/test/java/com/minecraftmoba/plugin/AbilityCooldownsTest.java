package com.minecraftmoba.plugin;
import org.junit.jupiter.api.Test;import java.util.UUID;import static org.junit.jupiter.api.Assertions.*;

class AbilityCooldownsTest {
 private final UUID a=UUID.randomUUID(),b=UUID.randomUUID();
 @Test void readyActivationStartsCooldownAndBlocksUntilExpiry(){var c=new AbilityCooldowns();assertTrue(c.ready(a,"a1",0));c.start(a,"a1",0,5);assertFalse(c.ready(a,"a1",0));assertEquals(5,c.remaining(a,"a1",0));assertEquals(3,c.remaining(a,"a1",2));assertTrue(c.ready(a,"a1",5));}
 @Test void clearResetsOnlyRequestedPlayerAndAbilities(){var c=new AbilityCooldowns();c.start(a,"a1",0,8);c.start(a,"a2",0,4);c.start(b,"a1",0,8);c.clear(a,"a1");assertTrue(c.ready(a,"a1",0));assertFalse(c.ready(a,"a2",0));assertFalse(c.ready(b,"a1",0));c.clear(a);assertTrue(c.ready(a,"a2",0));assertFalse(c.ready(b,"a1",0));}
 @Test void effectiveDurationsAreIndependentAndClampAtZero(){var c=new AbilityCooldowns();c.start(a,"a1",10,2);c.start(a,"a2",10,7);assertEquals(2,c.remaining(a,"a1",10));assertEquals(7,c.remaining(a,"a2",10));c.start(b,"a1",10,0);assertTrue(c.ready(b,"a1",10));}
}
