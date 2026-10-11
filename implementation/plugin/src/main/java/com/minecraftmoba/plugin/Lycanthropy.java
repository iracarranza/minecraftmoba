package com.minecraftmoba.plugin;

/** Server-free state machine; Unleashed has one fixed deadline even after a second Howl. */
final class Lycanthropy {
    enum Form { HUMAN, TRANSFORMED, UNLEASHED }
    private Form form=Form.HUMAN;
    private int lowTicks,transformedTicks,unleashedUntil,lockUntil;
    Form form(){return form;}
    int remaining(int now){return form==Form.UNLEASHED?Math.max(0,unleashedUntil-now):0;}
    boolean tick(int now,boolean combat,boolean belowThreshold,int requirement,int trigger,int duration,int lockout) {
        boolean exhausted=false;
        if(form==Form.UNLEASHED) {
            if(now>=unleashedUntil){form=Form.HUMAN;lowTicks=0;transformedTicks=0;lockUntil=now+lockout;exhausted=true;}
            return exhausted;
        }
        if(!combat){form=Form.HUMAN;lowTicks=0;transformedTicks=0;return false;}
        if(form==Form.HUMAN){lowTicks=belowThreshold?lowTicks+1:0;if(lowTicks>trigger){form=Form.TRANSFORMED;transformedTicks=0;}}
        else if(++transformedTicks>=requirement && now>=lockUntil)unleash(now,duration);
        return false;
    }
    void unleash(int now,int duration){if(form!=Form.UNLEASHED){form=Form.UNLEASHED;unleashedUntil=now+duration;lowTicks=0;transformedTicks=0;}}
    static double bonus(int level){return level>=25?.25:level>=15?.15:.08;}
    static int nightRequirement(long time,int fastest,int slowest){
        double darkness=(1+Math.cos((Math.floorMod(time,24000)-18000)*2*Math.PI/24000))/2;
        return (int)Math.round(slowest-(slowest-fastest)*darkness);
    }
}
