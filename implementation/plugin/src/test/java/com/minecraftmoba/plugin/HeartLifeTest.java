package com.minecraftmoba.plugin;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class HeartLifeTest {
 @Test void heartIsFiveSeventhsAndBreakingDamageCannotOverflow(){var h=new HeartLife(21);assertEquals(15,h.maximum());assertTrue(h.absorb(100,320));assertEquals(0,h.health());assertFalse(h.absorb(1,320));assertEquals(320,h.remaining());}
 @Test void fullBodyDoublesRecoveryWithoutResettingProgress(){var h=new HeartLife(10);h.absorb(20,800);for(int i=0;i<100;i++)h.advance(false);assertEquals(700,h.remaining());for(int i=0;i<349;i++)assertFalse(h.advance(true));assertTrue(h.advance(true));assertEquals(h.maximum(),h.health());}
 @Test void healingDoesNotResurrectBrokenHeartAndGrowthPreservesFraction(){var h=new HeartLife(14);h.absorb(5,100);h.capacity(21);assertEquals(7.5,h.health());h.heal(100);assertEquals(15,h.health());h.absorb(15,100);h.heal(10);assertEquals(0,h.health());}
}
