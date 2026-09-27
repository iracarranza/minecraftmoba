package com.minecraftmoba.plugin;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Lightfooted's passive arithmetic.
 *
 * The cap tests are the ones that matter. The source branch clamped only at
 * zero, so enough wolves reached literal damage immunity -- and animals are
 * free, renewable, breedable and deliberately stocked by the map compiler, so
 * that is an invitation rather than an edge case.
 */
class AnimalSensesTest {

    private static final int CAP = 4;

    @Test void noAnimalsChangesNothing() {
        assertEquals(1.0, AnimalSenses.damageMultiplier(0, 0.02, CAP), 1e-9);
        assertEquals(1.0, AnimalSenses.fallMultiplier(0, 0.10, CAP), 1e-9);
        assertEquals(0.0, AnimalSenses.speedBonus(0, 0.03, CAP), 1e-9);
        assertTrue(new AnimalSenses.Bonus(0, 0, 0).none());
    }

    @Test void eachSpeciesScalesItsOwnBenefit() {
        assertEquals(0.96, AnimalSenses.damageMultiplier(2, 0.02, CAP), 1e-9);
        assertEquals(0.80, AnimalSenses.fallMultiplier(2, 0.10, CAP), 1e-9);
        assertEquals(0.06, AnimalSenses.speedBonus(2, 0.03, CAP), 1e-9);
    }

    /** The cap applies to the COUNT, so the ceiling reads as "past four, more do nothing". */
    @Test void countsAreCappedSoBenefitsPlateau() {
        assertEquals(AnimalSenses.damageMultiplier(CAP, 0.02, CAP),
                     AnimalSenses.damageMultiplier(40, 0.02, CAP), 1e-9);
        assertEquals(AnimalSenses.fallMultiplier(CAP, 0.10, CAP),
                     AnimalSenses.fallMultiplier(99, 0.10, CAP), 1e-9);
        assertEquals(AnimalSenses.speedBonus(CAP, 0.03, CAP),
                     AnimalSenses.speedBonus(99, 0.03, CAP), 1e-9);
    }

    /**
     * The defect this cap exists to prevent.
     *
     * Uncapped, fifty wolves at 0.02 each is a multiplier of zero: literal
     * damage immunity, from an entity a player can breed.
     */
    @Test void damageIsMitigatedNeverNegated() {
        assertTrue(AnimalSenses.damageMultiplier(50, 0.02, CAP) > 0,
                "a passive that reaches zero damage is broken, not strong");
        assertEquals(0.92, AnimalSenses.damageMultiplier(50, 0.02, CAP), 1e-9);
    }

    @Test void negativeCountsAndRatesAreTreatedAsZero() {
        assertEquals(1.0, AnimalSenses.damageMultiplier(-5, 0.02, CAP), 1e-9);
        assertEquals(1.0, AnimalSenses.damageMultiplier(3, -0.02, CAP), 1e-9);
        assertEquals(0.0, AnimalSenses.speedBonus(-5, 0.03, CAP), 1e-9);
    }

    @Test void aNegativeAnimalCountIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> new AnimalSenses.Bonus(-1, 0, 0));
    }

    /** The cap must not silently disappear from config; it is load-bearing, not tuning. */
    @Test void theShippedConfigCapsSpeciesCountsAndDispatchesTheHook() throws Exception {
        var cfg = YamlConfiguration.loadConfiguration(new InputStreamReader(
                Objects.requireNonNull(getClass().getResourceAsStream("/config.yml"))));
        assertTrue(cfg.isInt("passives.animalSenses.speciesCap"),
                "the species cap is what stops breedable animals reaching damage immunity");
        assertTrue(cfg.getInt("passives.animalSenses.speciesCap") > 0);
        assertEquals(Passives.ANIMAL_SENSES,
                cfg.getString("abilities.classes.lightfooted.passiveHook"),
                "Lightfooted's hook must match the constant Passives dispatches on");
    }

    /** Lightfooted's second ability was Mole's prototype stub; it is its own now. */
    @Test void lightfootedHasItsOwnSecondAbility() throws Exception {
        var cfg = YamlConfiguration.loadConfiguration(new InputStreamReader(
                Objects.requireNonNull(getClass().getResourceAsStream("/config.yml"))));
        assertEquals("bounding", cfg.getString("abilities.classes.lightfooted.a2"));
        assertTrue(cfg.getInt("abilities.definitions.bounding.bounds") > 0);
        assertFalse(cfg.getBoolean("abilities.definitions.bounding.combat"),
                "Bounding is travel; attacking is refused for its duration");
    }
}
