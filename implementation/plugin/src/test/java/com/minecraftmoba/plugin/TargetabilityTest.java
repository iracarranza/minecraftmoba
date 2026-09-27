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
     * Livestock classify as livestock, and are STILL targets.
     *
     * An earlier draft excluded them on an economic argument. The decisive
     * objection was about statuses: if another class ever deals bonus damage
     * to stunned enemies, a population that cannot be Stunned is a silent
     * exception in THAT class's kit, visible nowhere near either design.
     * Category therefore describes and never permits.
     */
    @Test void livestockAreClassifiedButNotExcluded() {
        assertEquals(LIVESTOCK, Targetability.category(mock(Cow.class)));
        assertEquals(LIVESTOCK, Targetability.category(mock(Chicken.class)));
        assertEquals(LIVESTOCK, Targetability.category(mock(Sheep.class)));
    }

    @Test void tameableFightersClassifyAsLivestockToo() {
        assertEquals(LIVESTOCK, Targetability.category(mock(Wolf.class)));
    }

    /**
     * The point of keeping Category at all: it describes, it does not gate.
     *
     * An ability that genuinely wants to skip animals says so at its own call
     * site, where the exception is visible to whoever reads that ability --
     * rather than having one baked into the shared predicate, where no other
     * class would ever find it.
     */
    @Test void categoryDescribesRatherThanPermits() {
        for (var category : Targetability.Category.values())
            assertNotNull(category.name(), "every category is a full target; none is a veto");
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
