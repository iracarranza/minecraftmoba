package com.minecraftmoba.plugin;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class LycanthropyTest {
    @Test void sixtyTicksBelowThresholdAreInsufficientAndRecoveryBreaksTheStreak(){
        var wolf=new Lycanthropy();for(int t=1;t<=60;t++)wolf.tick(t,true,true,400,60,200,200);
        assertEquals(Lycanthropy.Form.HUMAN,wolf.form());wolf.tick(61,true,false,400,60,200,200);
        for(int t=62;t<=122;t++)wolf.tick(t,true,true,400,60,200,200);
        assertEquals(Lycanthropy.Form.TRANSFORMED,wolf.form());
    }
    @Test void transformedEndsWithCombatButUnleashedFinishesItsFullTenSeconds(){
        var wolf=new Lycanthropy();for(int t=1;t<=61;t++)wolf.tick(t,true,true,400,60,200,200);
        wolf.tick(62,false,false,400,60,200,200);assertEquals(Lycanthropy.Form.HUMAN,wolf.form());
        wolf.unleash(100,200);wolf.tick(299,false,false,400,60,200,200);assertEquals(Lycanthropy.Form.UNLEASHED,wolf.form());
        assertTrue(wolf.tick(300,false,false,400,60,200,200));assertEquals(Lycanthropy.Form.HUMAN,wolf.form());
    }
    @Test void naturalEscalationHonorsLockoutAndHowlBypassesItWithoutExtending(){
        var wolf=new Lycanthropy();wolf.unleash(0,200);wolf.tick(200,true,true,1,60,200,200);
        for(int t=201;t<=300;t++)wolf.tick(t,true,true,1,60,200,200);
        assertEquals(Lycanthropy.Form.TRANSFORMED,wolf.form());
        wolf.unleash(301,200);assertEquals(200,wolf.remaining(301));wolf.unleash(350,200);assertEquals(151,wolf.remaining(350));
        assertTrue(wolf.tick(501,true,true,1,60,200,200));
    }
    @Test void naturalLockoutEndsExactlyAtItsDeadline(){
        var wolf=new Lycanthropy();wolf.unleash(0,200);wolf.tick(200,true,true,1,60,200,200);
        for(int t=201;t<400;t++)wolf.tick(t,true,true,1,60,200,200);
        assertEquals(Lycanthropy.Form.TRANSFORMED,wolf.form());wolf.tick(400,true,true,1,60,200,200);
        assertEquals(Lycanthropy.Form.UNLEASHED,wolf.form());
    }
    @Test void midnightIsFastestAndNoonSlowestWithWrappedTime(){
        assertEquals(100,Lycanthropy.nightRequirement(18000,100,400));assertEquals(400,Lycanthropy.nightRequirement(6000,100,400));
        assertEquals(Lycanthropy.nightRequirement(18000,100,400),Lycanthropy.nightRequirement(42000,100,400));
        assertEquals(.08,Lycanthropy.bonus(0));assertEquals(.15,Lycanthropy.bonus(15));assertEquals(.25,Lycanthropy.bonus(25));
    }
}
