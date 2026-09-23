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

    /**
     * Activation is now scoped to a Worksite TIER, not to a sunset ordinal.
     *
     * The old entry point took the sunset number and indexed a list with it,
     * which is exactly the shape that made every sunset a Worksite sunset and
     * repeated the last entry forever after the list ran out. Removing the
     * ordinal from the signature is what makes a Lair night unable to open
     * Worksites at all -- there is no arithmetic left to get wrong.
     */
    @Test void worksitesOpenByTierAndCannotBeOpenedByASunsetOrdinal() throws Exception {
        var open = java.util.Arrays.stream(Worksites.class.getDeclaredMethods())
                .filter(m -> m.getName().equals("onOpportunityNight")).findFirst().orElseThrow();
        assertArrayEquals(new Class<?>[]{OpportunityCadence.WorksiteTier.class},
                open.getParameterTypes());
        assertTrue(java.util.Arrays.stream(Worksites.class.getDeclaredMethods())
                .noneMatch(m -> m.getName().equals("onSunset")),
                "the per-sunset entry point is superseded, not kept alongside");
    }

    @Test void onlyTheThreeWorksiteNightsCarryATier() {
        // Whatever calls into Worksites can only do so with a tier, and only
        // three of the six nights have one.
        int tiered = 0;
        for (int n = 1; n <= 6; n++)
            if (OpportunityCadence.atNight(n).tier() != null) tiered++;
        assertEquals(3, tiered);
        assertNull(OpportunityCadence.atNight(2).tier(), "a Lair night opens no Worksite");
    }

    @Test void theTierPackageIsReportedUnresolvedRatherThanGranted() throws Exception {
        // Worksite I's Blast Furnace and Smoker, II's Enchanting Table and
        // Anvil, and III's open package are design anchors. Nothing here hands
        // them out, so nothing here may look like it does.
        String source = java.nio.file.Files.readString(java.nio.file.Path.of(
                "src/main/java/com/minecraftmoba/plugin/Worksites.java"));
        assertTrue(source.contains("UNRESOLVED"), "an activated tier must say its package is open");
        for (String granted : new String[]{"BLAST_FURNACE", "SMOKER", "ENCHANTING_TABLE", "ANVIL"})
            assertFalse(source.contains(granted),
                    "Worksites must not place " + granted + ": the package design is unresolved");
    }
}
