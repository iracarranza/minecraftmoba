package com.minecraftmoba.plugin;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class HemostasisTest {
 @Test void endpointsCastThenResetAndRepeatedPathologyHurts(){var h=new Hemostasis(1);var cast=h.cast(-1,0,100,200);assertEquals(0,cast.manifestation());assertTrue(cast.endpoint());assertFalse(cast.relapse());assertEquals(1,h.value());assertTrue(h.cast(-1,1,100,200).relapse());}
 @Test void stabilizationHasTwoSeparateStableStates(){var low=new Hemostasis(1);low.tick(0,100);low.tick(100,100);low.tick(200,100);assertEquals(3,low.value());var high=new Hemostasis(5);high.tick(0,100);high.tick(100,100);assertEquals(4,high.value());}
 @Test void castsResetStabilizationAndCannotOriginateAtEndpoint(){var h=new Hemostasis(5);assertEquals(4,h.cast(-1,30,100,200).manifestation());h.tick(129,100);assertEquals(4,h.value());assertEquals(5,h.cast(1,130,100,200).manifestation());assertEquals(6,h.cast(1,131,100,200).manifestation());assertEquals(4,h.cast(-1,132,100,200).manifestation());}
}
