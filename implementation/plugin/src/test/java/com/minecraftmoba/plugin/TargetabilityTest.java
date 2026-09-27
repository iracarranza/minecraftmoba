package com.minecraftmoba.plugin;

import org.bukkit.entity.*;
import org.junit.jupiter.api.Test;

import static com.minecraftmoba.plugin.Targetability.Category.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

/**
 * Who counts as a target, and for which of the two axes.
 *
 * The classification is tested directly because it is the part that carries
 * the design decision; `impulse` and `status` need a live scoreboard for their
 * ally check and are exercised in the lab rather than here.
 */
class TargetabilityTest {

    @Test void hostileAndNeutralMobsAreFullTargets() {
        assertEquals(MOB, Targetability.category(mock(Zombie.class)));
        assertEquals(MOB, Targetability.category(mock(Enderman.class)), "neutral still counts");
        assertEquals(MOB, Targetability.category(mock(Ravager.class)));
        assertEquals(MOB, Targetability.category(mock(Villager.class)),
                "not breedable livestock, so not excluded by the economy argument");
    }

    @Test void playersAreTargets() {
        assertEquals(ENEMY, Targetability.category(mock(Player.class)));
    }

    /**
     * Livestock are excluded for an economic reason, not a thematic one.
     *
     * Animals are this project's renewable resource. If an Observer could lock
     * a cow, the best way to hold a circuit's target would be to stand in a
     * pen, and "first enemy struck" would fire on a breeding accident.
     */
    @Test void passiveLivestockAreNotTargets() {
        assertEquals(LIVESTOCK, Targetability.category(mock(Cow.class)));
        assertEquals(LIVESTOCK, Targetability.category(mock(Chicken.class)));
        assertEquals(LIVESTOCK, Targetability.category(mock(Sheep.class)));
    }

    /** Tameable fighters are still somebody's property, so the farming argument holds. */
    @Test void wolvesAreLivestockDespiteFighting() {
        assertEquals(LIVESTOCK, Targetability.category(mock(Wolf.class)));
    }

    /**
     * Classification follows vanilla's own interface rather than a type list.
     *
     * A list would need editing every time Minecraft adds a mob and would be
     * wrong in the interval; `Animals` is what vanilla already uses for
     * breedable passive creatures, so the rule tracks the game.
     */
    @Test void classificationIsByBehaviourNotByAList() {
        assertInstanceOf(Animals.class, mock(Cow.class));
        assertFalse(mock(Zombie.class) instanceof Animals);
    }
}
