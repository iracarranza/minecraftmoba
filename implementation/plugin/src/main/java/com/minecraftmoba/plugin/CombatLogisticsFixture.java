package com.minecraftmoba.plugin;
import org.bukkit.*;
/** Empty, attackable endpoints for demonstrating real worker transport in the existing chamber. */
final class CombatLogisticsFixture {
 private CombatLogisticsFixture(){}
 static void build(World world){
  for(int x:new int[]{0,8})world.getBlockAt(x,CombatSlab.FLOOR_Y+1,8).setType(Material.COPPER_CHEST,false);
 }
}
