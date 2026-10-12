package com.minecraftmoba.plugin;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class QuarryQuotaTest {
 @Test void partialFailureReplacesLastButNeverErasesCompleted(){var q=new QuarryQuota(6,99,3,1,100);q.refresh(100,100,1,12,100);assertEquals(5,q.stage);assertEquals(3,q.completed);assertEquals(.99,q.last);assertEquals(0,q.progress);}
 @Test void clearingNeverSandbagsAndDoesNotRestartTimer(){var q=new QuarryQuota(6,99,3,.8,100);q.toggle();q.toggle();q.mined();q.refresh(100,100,1,12,100);assertEquals(6,q.stage);assertEquals(3,q.completed);assertEquals(.8,q.last);assertFalse(q.cleared);assertEquals(200,q.expires);}
 @Test void onlyCompletedAndLastAffectPotency(){var a=new QuarryQuota(1,0,5,1,100);var b=new QuarryQuota(12,99,5,1,100);assertEquals(a.scanWidth(),b.scanWidth());assertEquals(5.5,a.scanWidth());assertEquals(.85,a.reduction(),1e-9);assertEquals(a.strikeDamage(6,.5),b.strikeDamage(6,.5));assertEquals(24,new QuarryQuota(1,0,20,1,100).strikeDamage(6,1));}
}
