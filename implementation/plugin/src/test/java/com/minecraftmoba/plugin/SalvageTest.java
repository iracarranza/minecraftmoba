package com.minecraftmoba.plugin;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class SalvageTest {
    @Test void progressionUsesItsOwnAuthoredThresholds(){
        assertEquals(6,Salvage.threshold(7));assertEquals(5,Salvage.threshold(8));assertEquals(4,Salvage.threshold(16));assertEquals(3,Salvage.threshold(24));
    }
    @Test void newerCopperCanonWinsAndConstructionComponentsAreNotEquipment(){
        assertTrue(Salvage.PRIMARY.contains(Material.COPPER_INGOT));assertFalse(Salvage.PRIMARY.contains(Material.REDSTONE));
        assertTrue(Salvage.equipment(Material.COPPER_PICKAXE));assertFalse(Salvage.equipment(Material.IRON_BLOCK));assertFalse(Salvage.equipment(Material.PISTON));
    }
}
