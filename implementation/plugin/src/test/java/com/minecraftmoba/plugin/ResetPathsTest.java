package com.minecraftmoba.plugin;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * There is one reset, and every command that resets must go through it.
 *
 * There were two. `/moba match reset` cleared inventory, vanilla XP, task
 * modifiers and re-issued the tome; `/moba reset <player>` swapped PlayerData
 * and stopped. Both were called reset, only one of them was, and the partial
 * one is the one an admin types -- so resetting visibly did nothing to items or
 * levels while the code that did the work sat in the other path looking correct.
 *
 * A second implementation of an idea is the recurring defect in this codebase,
 * so this pins the property rather than the behaviour: no command may rebuild
 * PlayerData by hand.
 */
class ResetPathsTest {

    private static String plugin() throws Exception {
        return Files.readString(Path.of("src/main/java/com/minecraftmoba/plugin/MobaPlugin.java"));
    }

    @Test void theAdminResetGoesThroughTheSharedPath() throws Exception {
        String src = plugin();
        // Anchored on the admin branch's own message: there are two `case
        // "reset"` labels and the first belongs to /moba match, which is
        // exactly the confusion this test exists about.
        int marker = src.indexOf("Empty cursor and temporary menu slots before reset.");
        assertTrue(marker > 0, "the admin reset subcommand has been renamed");
        int reset = src.lastIndexOf("case \"reset\" ->", marker);
        String body = src.substring(reset, src.indexOf("\n                }", reset));
        assertTrue(body.contains("clearMatchScopedState(p)"),
                "admin reset must use the same clearing as match reset");
        assertFalse(body.contains("new PlayerData("),
                "rebuilding PlayerData here forks reset into two meanings");
    }

    @Test void onlyTheSharedPathConstructsFreshPlayerData() throws Exception {
        // clearMatchScopedState is the one place allowed to mint a fresh
        // PlayerData for a reset. A join loading saved data is a different
        // thing and is allowed to construct one too, so this counts rather
        // than forbids.
        String src = plugin();
        int occurrences = src.split("new PlayerData\\(", -1).length - 1;
        assertTrue(occurrences <= 2,
                "found " + occurrences + " PlayerData constructions; each extra one is "
                        + "another place that means something slightly different by reset");
    }

    @Test void matchResetAndAdminResetClearTheSameThings() throws Exception {
        // Both routes end in clearMatchScopedState, so the list of what reset
        // means lives in exactly one method.
        String src = plugin();
        int clear = src.indexOf("public void clearMatchScopedState");
        String body = src.substring(clear, src.indexOf("\n    }", clear));
        for (String required : new String[]{"inv.clear()", "setTotalExperience(0)",
                "taskEffects.reapply", "sync(p, fresh)", "offhandMap.ensure"})
            assertTrue(body.contains(required), "clearMatchScopedState no longer does: " + required);
    }
}
