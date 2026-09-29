package com.minecraftmoba.plugin;
import org.bukkit.util.Vector;import org.junit.jupiter.api.Test;import static org.junit.jupiter.api.Assertions.*;
class DaredevilMechanicsTest {
 @Test void negligibleMomentumDoesNotQualify(){assertFalse(DaredevilMechanics.qualifies(.1,.35));assertTrue(DaredevilMechanics.qualifies(.5,.35));}
 @Test void runwayLaunchPreservesFacingAndAddsUpwardVelocity(){var out=DaredevilMechanics.launch(new Vector(0,0,2),new Vector(1,0,0),1,.8);assertEquals(2,out.getX(),1e-9);assertEquals(.8,out.getY(),1e-9);assertEquals(0,out.getZ(),1e-9);}
}
