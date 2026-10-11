package com.minecraftmoba.plugin;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class CrewBurnTest {
 @Test void combatDisengagementResetsDeadlineButRetainsBurnDuration(){
  var b=new CrewBurn();for(int i=1;i<=59;i++)assertEquals(i,b.tick(true,true,false).sustainedCombat());
  assertEquals(0,b.tick(true,false,false).sustainedCombat());assertEquals(1,b.tick(true,true,false).sustainedCombat());
  assertEquals(61,b.tick(false,false,true).extinguishedAfter());assertEquals(0,b.tick(false,false,true).extinguishedAfter());
 }
 @Test void naturalTimeoutOrShadeDoesNotGrantWaterRecovery(){
  var b=new CrewBurn();b.tick(true,true,false);assertEquals(0,b.tick(false,false,false).extinguishedAfter());
  assertEquals(0,b.tick(false,false,true).extinguishedAfter());
 }
 @Test void wetWorkerStillBurningHasNotCompletedExtinguishing(){
  var b=new CrewBurn();assertEquals(0,b.tick(true,false,true).extinguishedAfter());assertEquals(1,b.tick(false,false,true).extinguishedAfter());
 }
}
