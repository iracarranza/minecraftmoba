package com.minecraftmoba.plugin;

import org.junit.jupiter.api.Test;

import static com.minecraftmoba.plugin.Worksites.State.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * The Worksite lifecycle canon specifies: Dormant -> Activated -> Capitalized
 * -> Depleted. The property that matters most is that activation does not
 * grant capitalization, so it is asserted directly rather than left implicit
 * in the sunset handler.
 */
class WorksiteLifecycleTest {

    @Test void theFourCanonStatesExistInOrder() {
        assertArrayEquals(new Worksites.State[]{DORMANT, ACTIVATED, CAPITALIZED, DEPLETED},
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
        assertFalse(Worksites.canCapitalize(DEPLETED), "depleted is terminal");
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
        // The tier identities are design anchors, not contents. Nothing here
        // hands out a facility, so nothing here may look like it does.
        String source = java.nio.file.Files.readString(java.nio.file.Path.of(
                "src/main/java/com/minecraftmoba/plugin/Worksites.java"));
        assertTrue(source.contains("UNRESOLVED"), "an activated tier must say its package is open");
        for (String granted : new String[]{"BLAST_FURNACE", "SMOKER", "ENCHANTING_TABLE", "ANVIL",
                "SMITHING_TABLE", "ANCIENT_DEBRIS", "DIAMOND_ORE", "LAPIS_ORE", "IRON_ORE"})
            assertFalse(source.contains(granted),
                    "Worksites must not place " + granted + ": the package design is unresolved");
    }

    /**
     * The reconciled three-tier economic identity.
     *
     * Asserted on the enum rather than on a doc string because the runtime is
     * supposed to KNOW what a Worksite night is about. The negative half
     * matters as much: there is no fourth tier, no Copper tier, and no
     * unnamed intermediate Factory to fill.
     */
    @Test void theThreeTiersCarryTheirReconciledEconomicIdentity() {
        var tiers = OpportunityCadence.WorksiteTier.values();
        assertEquals(3, tiers.length, "three Worksite nights, three tiers");
        assertEquals("Iron + Coal / Blast Furnace + Smoker", tiers[0].identity());
        assertEquals("Diamond + Lapis / Enchanting Table", tiers[1].identity());
        assertEquals("Ancient Debris + Diamond / Smithing Table", tiers[2].identity());
        for (var t : tiers) {
            assertFalse(t.miningSite().contains("Copper"),
                    "the Copper Worksite tier is removed: Copper needs no exceptional injection");
            assertFalse(t.factory().isBlank(), "no tier may carry a TBD Factory");
        }
    }

    /**
     * Sunset activates; sunrise does nothing.
     *
     * The absence of the method is the assertion. An empty onSunrise would be
     * an invitation to put closure back into it, so the test refuses the name
     * outright rather than checking that it returns an empty list.
     */
    @Test void activationIsPermanentAndSunriseClosesNothing() {
        assertTrue(java.util.Arrays.stream(Worksites.class.getDeclaredMethods())
                        .noneMatch(m -> m.getName().equals("onSunrise")),
                "sunrise deactivation is superseded: activation is permanent");
    }
}
