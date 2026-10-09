package com.minecraftmoba.plugin;

import org.bukkit.entity.EntityType;
import org.junit.jupiter.api.Test;

import static com.minecraftmoba.plugin.UndeadAffinity.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Who gets pulled, and -- more importantly -- who does not.
 */
class UndeadAffinityTest {

    private static final double NORMAL = VANILLA_FOLLOW;
    private static final double EXTENDED = DEFAULT_EXTENDED;

    // ---- the band ----------------------------------------------------------

    @Test void aMobInTheExtendedBandIsPulled() {
        assertTrue(shouldPursue(24, NORMAL, EXTENDED, false));
    }

    @Test void aMobBeyondTheExtendedRangeIsNot() {
        assertFalse(shouldPursue(EXTENDED + 0.01, NORMAL, EXTENDED, false));
        assertTrue(shouldPursue(EXTENDED, NORMAL, EXTENDED, false), "the edge is inside");
    }

    @Test void aMobVanillaAlreadyCoversIsLeftToVanilla() {
        // Re-targeting inside the normal range would mean the passive silently
        // overriding ordinary mob behaviour -- and it would make "how much does
        // this passive do" unanswerable, because its effect and vanilla's would
        // be the same blocks.
        assertFalse(shouldPursue(NORMAL, NORMAL, EXTENDED, false));
        assertFalse(shouldPursue(4, NORMAL, EXTENDED, false));
    }

    // ---- what it must not become -------------------------------------------

    @Test void aMobAlreadyFightingSomebodyIsNotTakenFromThem() {
        // The load-bearing refusal. Converting night-time danger into
        // opportunity means drawing mobs that were not fighting anyone. Pulling
        // one off an ally mid-fight is a TAUNT: a different and much stronger
        // ability, and not one the design asked for.
        assertFalse(shouldPursue(24, NORMAL, EXTENDED, true));
    }

    @Test void anExtendedRangeThatDoesNotExceedTheNormalOneDoesNothing() {
        // Rather than inverting the band and pulling only mobs that are
        // already close, which is the shape a bad config would otherwise take.
        assertFalse(shouldPursue(10, 16, 16, false));
        assertFalse(shouldPursue(10, 16, 8, false));
    }

    // ---- eligibility -------------------------------------------------------

    @Test void theOrdinaryNightTimeUndeadAreEligible() {
        for (EntityType type : new EntityType[]{EntityType.ZOMBIE, EntityType.SKELETON,
                EntityType.HUSK, EntityType.DROWNED, EntityType.STRAY, EntityType.PHANTOM})
            assertTrue(eligible(type), type + " is what the passive is for");
    }

    @Test void zombifiedPiglinsAreExcludedBecauseTheyAreNeutral() {
        // They are undead, and they retaliate as a group. Pulling them would
        // not extend a danger Skeleton Crew already lives with; it would
        // manufacture a new and lethal one out of mobs ignoring everybody.
        assertFalse(eligible(EntityType.ZOMBIFIED_PIGLIN));
    }

    @Test void theWitherIsExcludedBecauseItIsSummonedNotEncountered() {
        assertFalse(eligible(EntityType.WITHER));
    }

    @Test void theLivingAreNotUndead() {
        for (EntityType type : new EntityType[]{EntityType.CREEPER, EntityType.SPIDER,
                EntityType.PLAYER, EntityType.VILLAGER, EntityType.COW})
            assertFalse(eligible(type), type + " is not undead");
    }

    @Test void nothingIsEligibleWithoutAType() {
        assertFalse(eligible(null));
    }

    // ---- the fixture is substantially farther -------------------------------

    @Test void theExtendedRangeIsSubstantiallyFartherAsTheDesignRequires() {
        // "Substantially farther than normal" is the only constraint classes.md
        // gives. A fixture that failed it would make the passive technically
        // present and practically absent, which is the outcome the dead hook
        // already produced.
        assertTrue(DEFAULT_EXTENDED >= VANILLA_FOLLOW * 1.5,
                DEFAULT_EXTENDED + " must be substantially beyond " + VANILLA_FOLLOW);
    }

    @Test void crewIsNotMintedHere() {
        // The other half of the passive needs Crew, which does not exist. If a
        // Crew counter ever appears on this class, it should arrive with the
        // ability that spends it -- not as a number that only goes up.
        for (var m : UndeadAffinity.class.getDeclaredMethods())
            assertFalse(m.getName().toLowerCase().contains("crew"),
                    "UndeadAffinity grew " + m.getName() + " before Crew existed");
    }
}
