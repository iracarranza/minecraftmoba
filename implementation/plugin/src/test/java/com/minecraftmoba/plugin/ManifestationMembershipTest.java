package com.minecraftmoba.plugin;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Manifestation membership is a fact about the resource, not about where it is
 * standing.
 *
 * Membership used to be "an entity of the right type inside the radius", which
 * gets all three ownership cases wrong at once: a player's bred sheep became a
 * wild herd member, a wild sheep that wandered out stopped being one, and any
 * naturally spawned zombie standing in a Swarm region counted as a Swarm. None
 * of those is recoverable by tuning a radius -- the representation was the bug.
 *
 * Bukkit-bound, so these check the source-level contract.
 */
class ManifestationMembershipTest {

    private static String renewables() throws Exception {
        return Files.readString(Path.of("src/main/java/com/minecraftmoba/plugin/Renewables.java"));
    }

    private static String body(String src, String signature) {
        int i = src.indexOf(signature);
        assertTrue(i > 0, "missing: " + signature);
        return src.substring(i, src.indexOf("\n    }", i));
    }

    @Test void membersAreMarkedAtSpawn() throws Exception {
        String spawn = body(renewables(), "private int spawnFauna(");
        assertTrue(spawn.contains("memberKey"),
                "an entity must be marked as a member when the system creates it");
    }

    @Test void countingResolvesMembershipNotProximity() throws Exception {
        String count = body(renewables(), "private int count(Source s,");
        assertTrue(count.contains("isMember(e, s)"),
                "entity membership must be read from the mark");
        assertFalse(count.contains("kind.entities().contains"),
                "type-in-radius is the representation that was wrong");
    }

    @Test void aKillOnlyDepletesWhenTheVictimWasAMember() throws Exception {
        String death = body(renewables(), "public void onDeath(EntityDeathEvent");
        assertTrue(death.contains("memberKey"), "resolves the victim's owner from its mark");
        assertTrue(death.contains("if (owner == null) return;"),
                "an ordinary animal killed inside a region is not a harvest of that region");
    }

    @Test void cropMembershipExcludesPlayerPlacedBlocks() throws Exception {
        // Provenance already answers "did a player put this here", and count()
        // was the one place that did not ask -- so a player's farm inside a
        // region read as the wild patch being intact and blocked regeneration.
        String count = body(renewables(), "private int count(Source s,");
        assertTrue(count.contains("isPlayerPlaced(b)"),
                "player-planted crops are production, not the wild patch");
    }

    @Test void revocationNeverDeletesTheEntity() throws Exception {
        // The whole point: a captured animal leaves the wild population and
        // remains an ordinary animal in the team's economy.
        String revoke = body(renewables(), "public void revoke(");
        assertTrue(revoke.contains("remove(memberKey)"), "membership is removed");
        for (String destructive : new String[]{"remove()", "setHealth(0", "damage(", "kill"})
            assertFalse(revoke.contains(destructive),
                    "revocation must not touch the entity itself: " + destructive);
    }

    @Test void regenerationDoesNotStackOntoAStandingManifestation() throws Exception {
        // An ignored opportunity must not become an animal printer, and the
        // region must not become a camp coordinate.
        String manifest = body(renewables(), "public int manifest(Source s)");
        assertTrue(manifest.contains("if (present > 0)"),
                "a standing manifestation IS the current one; nothing is added to it");
        assertFalse(manifest.contains("available(s) - present"),
                "topping up to capacity is the stacking behaviour that was removed");
    }

    @Test void siteSelectionIsMarkedAsNotImplemented() throws Exception {
        // It waits on two architectural decisions. Shipping the authored origin
        // silently would leave the rejected fixed-pad behaviour looking
        // intentional.
        String manifest = body(renewables(), "public int manifest(Source s)");
        assertTrue(manifest.contains("SITE SELECTION IS NOT IMPLEMENTED"),
                "the placeholder must say so at the point it applies");
    }
}
