package com.minecraftmoba.plugin;

import java.util.*;

/** Explicitly coarse terrain-cost diagnostic, not competitive certification. */
final class LabAccess {
    private record Node(int cell,double cost) {}
    static double asymmetry(double north,double south) {
        double mean=(north+south)/2;
        return mean==0 ? 0 : Math.abs(north-south)/mean;
    }
    static double cost(int[] height,boolean[] water,int width,int start,int goal) {
        if(start<0||goal<0||start>=height.length||goal>=height.length||height[start]==Integer.MIN_VALUE||height[goal]==Integer.MIN_VALUE) return Double.POSITIVE_INFINITY;
        double[] distance=new double[height.length];Arrays.fill(distance,Double.POSITIVE_INFINITY);
        distance[start]=0;
        var queue=new PriorityQueue<Node>(Comparator.comparingDouble(Node::cost));queue.add(new Node(start,0));
        while(!queue.isEmpty()) {
            var node=queue.remove();if(node.cost>distance[node.cell])continue;if(node.cell==goal)return node.cost;
            int x=node.cell%width,z=node.cell/width;
            for(int[] d:new int[][]{{1,0},{-1,0},{0,1},{0,-1}}) {
                int xx=x+d[0],zz=z+d[1];if(xx<0||xx>=width||zz<0||zz>=height.length/width)continue;
                int next=zz*width+xx;if(height[next]==Integer.MIN_VALUE)continue;
                int rise=Math.abs(height[next]-height[node.cell]);if(rise>8)continue;
                double c=node.cost+8+rise*2+(water[next]?16:0);
                if(c<distance[next]){distance[next]=c;queue.add(new Node(next,c));}
            }
        }
        return Double.POSITIVE_INFINITY;
    }
}
