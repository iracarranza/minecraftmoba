package com.minecraftmoba.plugin;
/** Server-free external life: a breaking hit is fully absorbed, with no overflow. */
final class HeartLife {
 private double maximum,health;private int recovery;
 HeartLife(double bodyMaximum){maximum=bodyMaximum*5/7;health=maximum;}
 double maximum(){return maximum;} double health(){return health;} boolean intact(){return health>0;}
 void capacity(double bodyMaximum){double next=bodyMaximum*5/7;if(maximum>0&&intact())health*=next/maximum;maximum=next;}
 boolean absorb(double damage,int returnTicks){if(!intact()||damage<=0)return false;health=Math.max(0,health-damage);if(!intact())recovery=returnTicks;return true;}
 void heal(double amount){if(intact())health=Math.min(maximum,health+Math.max(0,amount));}
 boolean advance(boolean fullBody){if(intact())return false;recovery-=fullBody?2:1;if(recovery>0)return false;health=maximum;recovery=0;return true;}
 int remaining(){return recovery;}
}
