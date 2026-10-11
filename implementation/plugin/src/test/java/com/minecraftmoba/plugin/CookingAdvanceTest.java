package com.minecraftmoba.plugin;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class CookingAdvanceTest {
    @Test void remainderAdvancesNextItemAndPaysAllFuel() {
        var r = CookingAdvance.advance(10, 140, 200, 160, 1000, 1);
        assertEquals(new CookingAdvance.Result(1, 100, 160, 160), r);
    }
    @Test void outputOrInputLimitDoesNotBurnUnusedBudget() {
        assertEquals(new CookingAdvance.Result(1, 0, 60, 60), CookingAdvance.advance(1, 140, 200, 160, 1000, 1));
    }
    @Test void coalsConserveFuelWithoutInfiniteCooking() {
        assertEquals(new CookingAdvance.Result(1, 0, 200, 100), CookingAdvance.advance(10, 0, 200, 400, 100, .5));
        assertEquals(0, CookingAdvance.advance(10, 0, 200, 400, 0, .5).productiveTicks());
    }
    @Test void eachCampfireSlotHasIndependentProgress() {
        assertEquals(1, CookingAdvance.advance(1, 550, 600, 100, 100, 1).completed());
        assertEquals(100, CookingAdvance.advance(1, 0, 600, 100, 100, 1).progress());
    }
}
