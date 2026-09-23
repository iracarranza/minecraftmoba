package com.minecraftmoba.plugin;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * One siege, several kinds of act, one shared state.
 *
 * The property worth defending is that the three Minecraft-native approaches
 * are not three separate completion buttons. A team that fights a wave,
 * demolishes a wall and takes the signature target has conducted one siege, and
 * nothing here may let a route become its own parallel objective-health model.
 */
class DefensiveCapacityTest {

    private static final Team A = Team.values()[0];
    private static final Team B = Team.values()[1];

    private DefensiveCapacity withOutpost() {
        var c = new DefensiveCapacity();
        c.register(A, TeamObjectives.Kind.PILLAGER_OUTPOST,
                TeamObjectives.authoredBlocks(TeamObjectives.Kind.PILLAGER_OUTPOST));
        return c;
    }

    @Test void capacityComesFromWhatWasActuallyBuilt() {
        var c = new DefensiveCapacity();
        var tower = c.register(A, TeamObjectives.Kind.PILLAGER_OUTPOST,
                TeamObjectives.authoredBlocks(TeamObjectives.Kind.PILLAGER_OUTPOST));
        var bastion = c.register(A, TeamObjectives.Kind.NETHER_BASTION,
                TeamObjectives.authoredBlocks(TeamObjectives.Kind.NETHER_BASTION));
        // A Bastion is harder to exhaust than a watchtower because it is
        // physically bigger, not because a table says so.
        assertTrue(bastion.initial > tower.initial * 10);
    }

    @Test void everyRouteReducesTheSameState() {
        var c = withOutpost();
        var o = c.get(A, TeamObjectives.Kind.PILLAGER_OUTPOST);
        double start = o.remaining();
        c.defenderKilled(A, TeamObjectives.Kind.PILLAGER_OUTPOST);
        c.structureDestroyed(A, TeamObjectives.Kind.PILLAGER_OUTPOST, 100);
        c.signature(A, TeamObjectives.Kind.PILLAGER_OUTPOST);
        assertTrue(o.remaining() < start);
        var by = o.contributions();
        assertTrue(by.get(DefensiveCapacity.Source.COMBAT) > 0);
        assertTrue(by.get(DefensiveCapacity.Source.STRUCTURAL) > 0);
        assertTrue(by.get(DefensiveCapacity.Source.SIGNATURE) > 0);
        // One state, not three: the parts account for the whole reduction.
        double spent = by.values().stream().mapToDouble(Double::doubleValue).sum();
        assertEquals(start - o.remaining(), spent, 1e-9);
    }

    @Test void structuralDamageIsAShareOfTheStructureNotAFlatRate() {
        // A flat per-block rate multiplied by a capacity derived from the block
        // count squared the structure's size: 2000 blocks removed from a
        // 1156-block watchtower exhausted it twice over.
        var c = withOutpost();
        var o = c.get(A, TeamObjectives.Kind.PILLAGER_OUTPOST);
        int blocks = TeamObjectives.authoredBlocks(TeamObjectives.Kind.PILLAGER_OUTPOST);
        c.structureDestroyed(A, TeamObjectives.Kind.PILLAGER_OUTPOST, blocks / 2);
        assertEquals(o.initial / 2, o.remaining(), o.initial * 0.01);
    }

    @Test void destroyingTheWholeStructureExhaustsItExactlyOnce() {
        var c = withOutpost();
        int blocks = TeamObjectives.authoredBlocks(TeamObjectives.Kind.PILLAGER_OUTPOST);
        assertTrue(c.structureDestroyed(A, TeamObjectives.Kind.PILLAGER_OUTPOST, blocks * 10));
        assertEquals(0.0, c.get(A, TeamObjectives.Kind.PILLAGER_OUTPOST).remaining());
    }

    @Test void theSignatureIsAMassiveHitNotAWinButton() {
        var c = withOutpost();
        var o = c.get(A, TeamObjectives.Kind.PILLAGER_OUTPOST);
        assertFalse(c.signature(A, TeamObjectives.Kind.PILLAGER_OUTPOST));
        assertTrue(o.remaining() > 0, "the signature alone must not topple");
        assertTrue(o.fraction() < 0.6);
    }

    @Test void aLairAssaultIsMajorProgressButNotAScriptedTopple() {
        var c = withOutpost();
        var o = c.get(A, TeamObjectives.Kind.PILLAGER_OUTPOST);
        assertFalse(c.lairAssault(A, TeamObjectives.Kind.PILLAGER_OUTPOST));
        assertTrue(o.remaining() > 0);
    }

    @Test void anAssaultCanFinishAnObjectivePlayersAlreadyWeakened() {
        // The setup doctrine explicitly permits: damage it before a Lair night,
        // then let the monster capitalise. It works because the monster acts on
        // the objective's real current state rather than on a fresh one.
        var c = withOutpost();
        int blocks = TeamObjectives.authoredBlocks(TeamObjectives.Kind.PILLAGER_OUTPOST);
        c.structureDestroyed(A, TeamObjectives.Kind.PILLAGER_OUTPOST, (int) (blocks * 0.5));
        assertTrue(c.lairAssault(A, TeamObjectives.Kind.PILLAGER_OUTPOST),
                "half a structure plus a monster siege should topple it");
    }

    @Test void mixedRoutesCombineToTopple() {
        var c = withOutpost();
        var kind = TeamObjectives.Kind.PILLAGER_OUTPOST;
        int blocks = TeamObjectives.authoredBlocks(kind);
        c.structureDestroyed(A, kind, (int) (blocks * 0.3));
        c.signature(A, kind);
        boolean toppled = false;
        for (int i = 0; i < 100 && !toppled; i++) toppled = c.defenderKilled(A, kind);
        assertTrue(toppled, "no single route finished it; combined, they did");
        assertEquals(DefensiveCapacity.State.TOPPLED, c.get(A, kind).state());
    }

    @Test void aToppledObjectiveAbsorbsNoFurtherSiege() {
        var c = withOutpost();
        var kind = TeamObjectives.Kind.PILLAGER_OUTPOST;
        c.structureDestroyed(A, kind, TeamObjectives.authoredBlocks(kind) * 5);
        double spentBefore = c.get(A, kind).contributions()
                .get(DefensiveCapacity.Source.COMBAT);
        assertFalse(c.defenderKilled(A, kind));
        assertEquals(spentBefore, c.get(A, kind).contributions()
                .get(DefensiveCapacity.Source.COMBAT));
    }

    @Test void teamsAndObjectivesAreIndependent() {
        var c = new DefensiveCapacity();
        for (Team t : Team.values())
            for (TeamObjectives.Kind k : TeamObjectives.Kind.values())
                c.register(t, k, TeamObjectives.authoredBlocks(k));
        c.signature(A, TeamObjectives.Kind.PILLAGER_OUTPOST);
        assertEquals(1.0, c.get(B, TeamObjectives.Kind.PILLAGER_OUTPOST).fraction());
        assertEquals(1.0, c.get(A, TeamObjectives.Kind.NETHER_BASTION).fraction());
    }

    @Test void unregisteredObjectivesAreNotSilentlyAccepted() {
        var c = new DefensiveCapacity();
        assertFalse(c.signature(A, TeamObjectives.Kind.END_SPIKE));
        assertNull(c.get(A, TeamObjectives.Kind.END_SPIKE));
    }
}
