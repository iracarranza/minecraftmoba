package com.minecraftmoba.plugin;

import static org.junit.jupiter.api.Assertions.*;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

/** Which item each Offhander branch puts in the hand. */
class OffhandLoanTest {
    @Test void eachBranchHasItsItem() {
        assertEquals(Material.CROSSBOW, OffhandLoan.Kind.of("crossbow").material());
        assertEquals(Material.FISHING_ROD, OffhandLoan.Kind.of("fishing_rod").material());
        assertEquals(Material.GOLDEN_APPLE, OffhandLoan.Kind.of("golden_head").material());
    }

    @Test void noBranchOrAnUnknownOneIsTheBaseBow() {
        assertEquals(OffhandLoan.Kind.BOW, OffhandLoan.Kind.of(null));
        assertEquals(OffhandLoan.Kind.BOW, OffhandLoan.Kind.of("base"));
        assertEquals(OffhandLoan.Kind.BOW, OffhandLoan.Kind.of("trident"));
        assertEquals(Material.BOW, OffhandLoan.Kind.BOW.material());
    }

    @Test void everyKindRoundTripsThroughItsId() {
        for (var k : OffhandLoan.Kind.values()) assertEquals(k, OffhandLoan.Kind.of(k.id()));
    }

    @Test void theBranchIdsAreTheOnesTheAbilityDeclares() {
        var declared = java.util.Set.of("crossbow", "fishing_rod", "golden_head");
        for (var k : OffhandLoan.Kind.values()) if (k != OffhandLoan.Kind.BOW) assertTrue(declared.contains(k.id()), k.id());
    }
}
