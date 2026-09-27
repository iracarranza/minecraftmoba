package com.minecraftmoba.plugin;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The server misconfiguration that silently kills every match.
 *
 * `level-name=alpha_match` was the state of the alpha server on 27 September
 * 2026, and it made `match select`, `reset`, `restore` and the lab all fail
 * with "could not unload preloaded instance" -- a message that points at the
 * world copy rather than at the cause, because the copy is where it is
 * noticed. Nothing else about the server looked wrong: Paper preloaded the
 * world and spawned players into it perfectly happily.
 */
class WorldInstancePrimaryTest {

    @Test void theInstanceBeingThePrimaryWorldIsReported() {
        String message = WorldInstance.primaryWorldConflict("alpha_match", "alpha_match");
        assertNotNull(message, "this is the configuration that makes every match impossible");
        assertTrue(message.contains("level-name"), message);
        assertTrue(message.contains("alpha.instanceWorldName"),
                "the operator has to be told BOTH keys; knowing one does not locate the other");
    }

    @Test void aDistinctPrimaryWorldIsFine() {
        assertNull(WorldInstance.primaryWorldConflict("alpha_match", "moba_boot"));
    }

    /** A server with no worlds yet is not a conflict, and must not report one. */
    @Test void absentNamesAreNotAConflict() {
        assertNull(WorldInstance.primaryWorldConflict("alpha_match", null));
        assertNull(WorldInstance.primaryWorldConflict(null, "alpha_match"));
    }
}
