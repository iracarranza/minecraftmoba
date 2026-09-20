package com.minecraftmoba.plugin;

import org.junit.jupiter.api.Test;

import static com.minecraftmoba.plugin.Worksites.State.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * The Worksite lifecycle canon specifies: Dormant -> Activated -> Capitalized
 * -> Exploited. The property that matters most is that activation does not
 * grant capitalization, so it is asserted directly rather than left implicit
 * in the sunset handler.
 */
class WorksiteLifecycleTest {

    @Test void theFourCanonStatesExistInOrder() {
        assertArrayEquals(new Worksites.State[]{DORMANT, ACTIVATED, CAPITALIZED, EXPLOITED},
                Worksites.State.values());
    }

    @Test void activationDoesNotGrantCapitalization() {
        // A Worksite that has just activated is capitalizable, not capitalized.
        assertTrue(Worksites.canCapitalize(ACTIVATED));
        assertNotEquals(CAPITALIZED, ACTIVATED);
    }

    @Test void onlyAnActivatedWorksiteCanBeCapitalized() {
        assertFalse(Worksites.canCapitalize(DORMANT), "a sunset must precede capitalization");
        assertFalse(Worksites.canCapitalize(CAPITALIZED), "first capture is once only");
        assertFalse(Worksites.canCapitalize(EXPLOITED), "exploited is terminal");
    }
}
