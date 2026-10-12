package com.minecraftmoba.plugin;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class TwoPitonsTest {
 @Test void exactlyTwoAnchorsAndDestinationDependsOnCurrentPosition(){var p=new TwoPitons();var a=new TwoPitons.Point(0,0,0);var b=new TwoPitons.Point(10,0,0);assertTrue(p.place(a));assertTrue(p.place(b));assertFalse(p.place(new TwoPitons.Point(20,0,0)));assertEquals(2,p.size());assertEquals(b,p.farther(new TwoPitons.Point(1,0,0)));assertEquals(a,p.farther(new TwoPitons.Point(9,0,0)));}
}
