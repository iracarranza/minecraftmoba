package com.minecraftmoba.plugin;
/** Pure two-anchor selector. No expanding list and no distance from an old origin. */
final class TwoPitons {
 record Point(double x,double y,double z){double distance(Point p){return Math.pow(x-p.x,2)+Math.pow(y-p.y,2)+Math.pow(z-p.z,2);}}
 private Point first,second;
 boolean place(Point p){if(first==null){first=p;return true;}if(second==null){second=p;return true;}return false;}
 Point farther(Point from){if(second==null)return first;return first.distance(from)>=second.distance(from)?first:second;}
 int size(){return first==null?0:second==null?1:2;}
}
