package com.minecraftmoba.plugin;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class DiamondProjectionTest {
    @Test void projectionFillsOnlyTheDeficitAndPreservesAboveDiamond(){
        assertEquals(3,CoveredWithDiamondsAbility.missing(5,8));assertEquals(0,CoveredWithDiamondsAbility.missing(9,8));assertEquals(8,CoveredWithDiamondsAbility.missing(0,8));
    }
}
