package com.minecraftmoba.plugin;
import org.bukkit.*;
import org.bukkit.block.Block;
import java.util.*;
/** Stable spatial identity; reading an unavailable world/chunk never loads it. */
record BlockAddress(UUID world,int x,int y,int z) {
 static BlockAddress of(Block b){return new BlockAddress(b.getWorld().getUID(),b.getX(),b.getY(),b.getZ());}
 Block block(){World w=Bukkit.getWorld(world);return w!=null&&w.isChunkLoaded(x>>4,z>>4)?w.getBlockAt(x,y,z):null;}
 String encode(){return world+":"+x+":"+y+":"+z;}
 static BlockAddress decode(String value){String[] p=value.split(":");return new BlockAddress(UUID.fromString(p[0]),Integer.parseInt(p[1]),Integer.parseInt(p[2]),Integer.parseInt(p[3]));}
 List<BlockAddress> adjacent(){return List.of(new BlockAddress(world,x+1,y,z),new BlockAddress(world,x-1,y,z),new BlockAddress(world,x,y+1,z),new BlockAddress(world,x,y-1,z),new BlockAddress(world,x,y,z+1),new BlockAddress(world,x,y,z-1));}
}
