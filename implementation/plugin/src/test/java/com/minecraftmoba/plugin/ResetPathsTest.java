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

    private static String resetCommand(String src) {
        int i = src.indexOf("private boolean resetCommand");
        assertTrue(i > 0, "resetCommand has been renamed or removed");
        return src.substring(i, src.indexOf("\n    }", i));
    }

    @Test void oneCommandResetsAndItsScopeIsNamed() throws Exception {
        // The bug was grammar, not logic: one verb meaning two scopes, and
        // which one you got depended on where you typed it.
        String body = resetCommand(plugin());
        assertTrue(body.contains("case \"match\""), "no match scope");
        assertTrue(body.contains("case \"player\""), "no player scope");
        assertTrue(body.contains("clearMatchScopedState(target)"),
                "the player scope must use the shared clearing");
        assertFalse(body.contains("new PlayerData("),
                "rebuilding PlayerData here forks reset into two meanings again");
    }

    @Test void bareResetMeansTheMatch() throws Exception {
        // Almost always what is wanted, and the scope that used to require
        // knowing it lived under a different command.
        String body = resetCommand(plugin());
        assertTrue(body.contains("args.length >= 2 ? args[1]") && body.contains("\"match\";"),
                "bare /moba reset must default to the match scope");
    }

    @Test void resettingOnePlayerHasToBeAskedFor() throws Exception {
        // The narrower, more surprising scope is the one you have to spell out.
        String body = resetCommand(plugin());
        int player = body.indexOf("case \"player\"");
        String branch = body.substring(player, body.indexOf("}", body.indexOf("{", player)));
        assertTrue(branch.contains("args.length != 3"),
                "a player reset must name its player explicitly");
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

    @Test void bothScopesClearTheSameThings() throws Exception {
        // A match reset IS the player clear applied to every participant, plus
        // the world and the other match-scoped systems. Containment, not two
        // implementations -- so the list of what clearing means lives in
        // exactly one method.
        String src = plugin();
        int clear = src.indexOf("public void clearMatchScopedState");
        String body = src.substring(clear, src.indexOf("\n    }", clear));
        for (String required : new String[]{"inv.clear()", "setTotalExperience(0)",
                "taskEffects.reapply", "sync(p, fresh)", "offhandMap.ensure"})
            assertTrue(body.contains(required), "clearMatchScopedState no longer does: " + required);
    }
}
