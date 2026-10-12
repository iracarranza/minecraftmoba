package com.minecraftmoba.plugin;
import org.bukkit.*;
import org.bukkit.block.data.Ageable;

/** Renewable source plants on the combat lab's grass band; reset rebuilds both maturity controls. */
final class CombatEcologyFixture {
    private CombatEcologyFixture(){}
    static void build(World world){
        for(int x=12;x<=16;x++)for(int z=-8;z<=-4;z++){
            int floor=CombatSlab.FLOOR_Y;world.getBlockAt(x,floor,z).setType(Material.FARMLAND,false);
            var soil=(org.bukkit.block.data.type.Farmland)world.getBlockAt(x,floor,z).getBlockData();soil.setMoisture(soil.getMaximumMoisture());world.getBlockAt(x,floor,z).setBlockData(soil,false);
            Material type=x<=13?Material.WHEAT:x==14?Material.CARROTS:x==15?Material.BEETROOTS:Material.POTATOES;
            var age=(Ageable)type.createBlockData();age.setAge(z%2==0?age.getMaximumAge():0);
            world.getBlockAt(x,floor+1,z).setBlockData(age,false);
        }
        // Separate live Mycelium controls, leaving the original crop maturity plot intact.
        for(int x=12;x<=16;x++)for(int z=4;z<=8;z++)world.getBlockAt(x,CombatSlab.FLOOR_Y,z).setType(Material.MYCELIUM,false);
        world.getBlockAt(12,CombatSlab.FLOOR_Y+1,5).setType(Material.BROWN_MUSHROOM,false);
        world.getBlockAt(16,CombatSlab.FLOOR_Y+1,5).setType(Material.RED_MUSHROOM,false);
    }
}
