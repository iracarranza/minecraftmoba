package com.minecraftmoba.plugin;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class LabRulesTest {
    @Test void onlyALossIsStoppedByAFreeze() {
        assertTrue(LabRules.isLoss(18, 17));
        assertFalse(LabRules.isLoss(10, 10));
        assertFalse(LabRules.isLoss(10, 14));    // eating still works
    }
}
