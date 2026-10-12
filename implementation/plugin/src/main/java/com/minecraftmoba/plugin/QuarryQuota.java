package com.minecraftmoba.plugin;

/** Expectation and performance are independent. Clearing a quota never lowers its stage. */
final class QuarryQuota {
    int stage, progress, completed;
    double last;
    long expires;
    boolean cleared, strike;
    QuarryQuota(int stage,int progress,int completed,double last,long expires) {
        this.stage=stage;this.progress=progress;this.completed=completed;this.last=last;this.expires=expires;
    }
    void mined() { if(!strike&&!cleared)progress++; }
    void toggle() {strike=!strike;cleared=true;progress=0;}
    void refresh(long tick,int duration,int floor,int ceiling,int demand) {
        if(tick<expires)return;
        if(!cleared&&!strike){last=Math.min(1,progress/(double)Math.max(1,demand));if(last>=1){completed++;stage=Math.min(ceiling,stage+1);}else stage=Math.max(floor,stage-1);}
        progress=0;cleared=strike;expires=tick+duration;
    }
    double scanWidth(){return completed/2.0+3;}
    double reduction(){return .3+.05*last+.1*completed;}
    double strikeDamage(double base,double charge){return Math.min(24,base*Math.pow(1.15,completed))*(.2+.8*charge*charge);}
}
