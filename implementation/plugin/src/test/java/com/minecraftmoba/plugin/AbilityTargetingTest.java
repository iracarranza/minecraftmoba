package com.minecraftmoba.plugin;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/** The pure parts of ability targeting: who a relation accepts, and what a cone contains. */
class AbilityTargetingTest {
    private static final AbilityTargeting.Relation ENEMY = AbilityTargeting.Relation.ENEMY,
            ALLY = AbilityTargeting.Relation.ALLY, EITHER = AbilityTargeting.Relation.EITHER;

    @Test void anEnemyRelationAcceptsEnemiesAndNotAllies() {
        assertTrue(AbilityTargeting.accepts(ENEMY, true, false));
        assertFalse(AbilityTargeting.accepts(ENEMY, false, true));
        assertFalse(AbilityTargeting.accepts(ENEMY, false, false), "neither (the caster, livestock) is no one");
    }

    @Test void anAllyRelationAcceptsAlliesAndNotEnemies() {
        assertTrue(AbilityTargeting.accepts(ALLY, false, true));
        assertFalse(AbilityTargeting.accepts(ALLY, true, false));
        assertFalse(AbilityTargeting.accepts(ALLY, false, false));
    }

    @Test void eitherAcceptsBothAndStillNotNeither() {
        assertTrue(AbilityTargeting.accepts(EITHER, true, false));
        assertTrue(AbilityTargeting.accepts(EITHER, false, true));
        assertFalse(AbilityTargeting.accepts(EITHER, false, false));
    }

    @Test void aFullCircleTakesEverythingInRadiusAndNothingBeyond() {
        assertTrue(AbilityTargeting.inCone(3, 0, 0, 1, 4, -1));
        assertTrue(AbilityTargeting.inCone(0, -3, 0, 1, 4, -1), "behind is inside a full circle");
        assertFalse(AbilityTargeting.inCone(3, 3, 0, 1, 4, -1), "4.24 away is outside a radius of 4");
    }

    @Test void aConeKeepsWhatIsAheadAndDropsWhatIsBehind() {
        // facing south (+z); dot threshold .4 is about a 66 degree half-angle
        assertTrue(AbilityTargeting.inCone(0, 3, 0, 1, 4, .4));
        assertTrue(AbilityTargeting.inCone(1.5, 3, 0, 1, 4, .4));
        assertFalse(AbilityTargeting.inCone(3, 0.5, 0, 1, 4, .4), "off to the side, outside the cone");
        assertFalse(AbilityTargeting.inCone(0, -3, 0, 1, 4, .4));
    }

    @Test void aNarrowConeIsNarrowerThanAWideOne() {
        double dx = 2, dz = 3;      // about 34 degrees off the facing
        assertTrue(AbilityTargeting.inCone(dx, dz, 0, 1, 5, .4));
        assertFalse(AbilityTargeting.inCone(dx, dz, 0, 1, 5, .9));
    }

    @Test void aCreatureOnTopOfTheCentreIsAlwaysInside() {
        assertTrue(AbilityTargeting.inCone(0.05, -0.05, 0, 1, 4, .9), "it has no direction, so the cone cannot exclude it");
        assertTrue(AbilityTargeting.inCone(0, 0, 0, 0, 4, .9));
    }

    @Test void aZeroFacingKeepsOnlyTheNoDirectionAndFullCircleCases() {
        // straight-up casts have a zero flat facing: the dot is 0, so any positive threshold drops everything off-centre
        assertFalse(AbilityTargeting.inCone(2, 0, 0, 0, 4, .4));
        assertTrue(AbilityTargeting.inCone(2, 0, 0, 0, 4, -1));
        assertTrue(AbilityTargeting.inCone(2, 0, 0, 0, 4, 0), "the threshold is inclusive");
    }

    @Test void theRadiusBoundaryIsInclusive() { assertTrue(AbilityTargeting.inCone(4, 0, 1, 0, 4, -1)); }

    @Test void theMovedAbilitiesKeepTheValuesTheyHad() {
        var config = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(new java.io.File("src/main/resources/config.yml"));
        assertEquals(0.35, config.getDouble("abilities.definitions.brawling_slash.rayWidth"), "Brawling's ray was .35 in code");
        assertEquals(0.25, config.getDouble("abilities.definitions.flip_and_press.rayWidth"), "Flip's ray was .25 in code");
        assertEquals(0.25, config.getDouble("abilities.definitions.food_fight.hitRadius"), "Food Fight already read config");
    }
}
