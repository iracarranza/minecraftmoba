package com.minecraftmoba.plugin;

/** Consecutive burning combat, and a separate burn-to-water recovery lifecycle. */
final class CrewBurn {
    private int burningTicks,combatTicks;private boolean wasBurning;
    record Step(int sustainedCombat,int extinguishedAfter) {}
    Step tick(boolean burning,boolean engaged,boolean wet){
        int recovered=!burning && wasBurning && wet?burningTicks:0;
        if(burning){burningTicks++;combatTicks=engaged?combatTicks+1:0;}else{burningTicks=0;combatTicks=0;}
        wasBurning=burning;return new Step(combatTicks,recovered);
    }
}
