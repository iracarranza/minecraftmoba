package com.minecraftmoba.plugin;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Reset must actually return a player to a fresh state.
 *
 * ALPHA-D2 says reset clears all match-scoped plugin and player state, and the
 * implementation cleared everything a player CANNOT see -- PlayerData, Routes,
 * Worksites, contributions -- while leaving the inventory untouched. Every item
 * in it was extracted, crafted or picked up in the world being discarded, so a
 * second match began with the first one's work already banked.
 *
 * The reset path is Bukkit-bound and cannot be instantiated off-server, so what
 * is checked here is the source-level contract: that the clear happens, and
 * that it happens in the order the re-issuance depends on. Both are invariants
 * a reorder would break silently -- the tome is only issued into an EMPTY
 * offhand, so ensure() running first simply does nothing and the player spends
 * the next match with no map, no sentinel and no recall.
 */
class MatchResetTest {

    private static String source(String name) throws Exception {
        return Files.readString(Path.of("src/main/java/com/minecraftmoba/plugin/" + name));
    }

    private static int at(String haystack, String needle) {
        int i = haystack.indexOf(needle);
        assertTrue(i >= 0, "clearMatchScopedState no longer contains: " + needle);
        return i;
    }

    /** The body of clearMatchScopedState, where the ordering has to hold. */
    private String clearBody() throws Exception {
        String src = source("MobaPlugin.java");
        int start = src.indexOf("public void clearMatchScopedState");
        assertTrue(start > 0, "clearMatchScopedState has been renamed or removed");
        int end = src.indexOf("\n    }", start);
        return src.substring(start, end);
    }

    @Test void resetClearsTheInventory() throws Exception {
        String body = clearBody();
        assertTrue(body.contains("inv.clear()"), "the reported bug: items survived reset");
        assertTrue(body.contains("setArmorContents(null)"), "worn armour is inventory too");
        assertTrue(body.contains("setItemInOffHand(null)"));
        assertTrue(body.contains("setItemOnCursor(null)"),
                "an item held on the cursor is dropped back into the next match");
    }

    @Test void theClearHappensBeforeAnythingIsReissued() throws Exception {
        String body = clearBody();
        assertTrue(at(body, "inv.clear()") < at(body, "lockedSlots.refresh"),
                "markers would be placed into slots the clear then empties");
        assertTrue(at(body, "setItemInOffHand(null)") < at(body, "offhandMap.ensure"),
                "ensure() only issues a tome into an EMPTY offhand, so clearing after it "
                        + "leaves the player with no tome for the whole match");
    }

    @Test void resetRestoresTheLivePlayerAndNotJustTheStoredData() throws Exception {
        String body = clearBody();
        assertTrue(body.contains("sync(p, fresh)"),
                "save() alone persists the reset while the live player keeps the old "
                        + "match's maximum health and level display");
        assertTrue(body.contains("taskEffects.reapply"),
                "applyAutomaticGrants only grants upward, so a previous match's "
                        + "Efficiency and Damage modifiers would stay attached at level 1");
    }

    @Test void spawnUsesTheDerivedMaximaRatherThanVanillaTwenty() throws Exception {
        // A level 1 player's maxima are 9 and 9. setFoodLevel(20) handed out
        // eleven hunger points the Capacity model says they do not have, which
        // the custom readout cannot even display.
        String spawn = source("Match.java");
        int start = spawn.indexOf("private void spawn(Player p, Team team)");
        assertTrue(start > 0);
        String body = spawn.substring(start, spawn.indexOf("\n    }", start));
        assertFalse(body.contains("setFoodLevel(20)"), "vanilla hunger cap, not the player's");
        assertTrue(body.contains("effectiveHunger(p)"));
        assertTrue(body.contains("MAX_HEALTH"));
    }
}
