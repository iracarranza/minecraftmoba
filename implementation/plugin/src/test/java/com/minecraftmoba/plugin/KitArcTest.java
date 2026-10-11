package com.minecraftmoba.plugin;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class KitArcTest {
    @Test void arcStartsAndLandsExactlyAndOwnsExtremeHeight() {
        Vector a = new Vector(1, 64, 2), b = new Vector(20, 64, 5);
        assertEquals(a, RabbitsLuckyFootAbility.arc(a,b,0,12));
        assertEquals(b, RabbitsLuckyFootAbility.arc(a,b,1,12));
        assertEquals(76, RabbitsLuckyFootAbility.arc(a,b,.5,12).getY());
        assertEquals(new Vector(1,64,2), a); assertEquals(new Vector(20,64,5), b);
    }
}
