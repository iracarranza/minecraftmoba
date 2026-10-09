package com.minecraftmoba.plugin;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Every {@code passiveHook} in config.yml must reach code that reads it.
 *
 * <h2>The failure this exists for</h2>
 *
 * A hook travels from config.yml, through {@link AbilityInputs}, into {@link
 * ClassDefinition}, to {@link Passives#hook}, where it is compared against the
 * hooks that have constants. A hook with no constant matches nothing, and the
 * comparison simply returns false.
 *
 * Nothing fails. Nothing logs. The key is present, the parse succeeds, the
 * design document agrees -- and the class has no passive. That is {@link
 * ConfigKeysDefinedTest}'s failure in a place a missing-key scan cannot see,
 * because the key is not missing.
 *
 * <h2>Why a source scan</h2>
 *
 * Same reason as {@code ConfigKeysDefinedTest}: dispatch happens at runtime
 * against a live player with a class, and that needs a server. What can be
 * checked without one is whether any production source mentions the hook at
 * all -- which is a weaker claim, and still catches every case here, because a
 * hook nothing names cannot possibly be dispatched.
 */
class PassiveHookDispatchTest {

    private static final Path CONFIG = Path.of("src/main/resources/config.yml");
    private static final Path SOURCES = Path.of("src/main/java/com/minecraftmoba/plugin");

    /**
     * The files that merely CARRY a hook rather than acting on it.
     *
     * Excluded because a parser naming the field would make every hook look
     * dispatched, which is the exact confusion this test exists to remove.
     */
    private static final Set<String> PLUMBING =
            Set.of("AbilityInputs.java", "ClassDefinition.java", "MobaPlugin.java");

    /**
     * Hooks known to reach nothing, with what that costs.
     *
     * A list rather than a silent pass: the test fails if a NEW dead hook
     * appears, and fails just as loudly if one of these is fixed and not
     * removed from here, so the record cannot quietly go stale.
     */
    private static final Map<String, String> UNDISPATCHED = new LinkedHashMap<>(Map.of(
            // Skeleton Crew. Half of it is ordinary -- undead pursuing from
            // farther away is a follow-range change -- and half needs Crew,
            // which is the class's whole resource system and does not exist.
            "undead_affinity", "Skeleton Crew has no passive in game.",
            // Toolbox. The only one here whose passive actually RUNS: UtilityBelt
            // matches classId == "toolbox" directly. So the cost is not a missing
            // feature, it is a config key that describes a mechanism it does not
            // use -- which is what made the other two hard to see.
            "utility_belt", "Toolbox's passive runs, but not through this."));

    private static String config() {
        try {
            return Files.readString(CONFIG);
        } catch (IOException e) {
            throw new AssertionError("config.yml is the source of hooks: " + e);
        }
    }

    /** Every hook config declares, in the order it declares them. */
    private static Set<String> declaredHooks() {
        var hooks = new TreeSet<String>();
        Matcher m = Pattern.compile("(?m)^\\s*passiveHook:\\s*\"?([A-Za-z0-9_]+)\"?\\s*$")
                .matcher(config());
        while (m.find()) hooks.add(m.group(1));
        return hooks;
    }

    /** Whether any production source names this hook as a bare string literal. */
    private static boolean named(String hook) {
        String literal = '"' + hook + '"';
        try (Stream<Path> files = Files.walk(SOURCES)) {
            return files.filter(p -> p.toString().endsWith(".java"))
                    .filter(p -> !PLUMBING.contains(p.getFileName().toString()))
                    .anyMatch(p -> {
                        try {
                            return Files.readString(p).contains(literal);
                        } catch (IOException e) {
                            return false;
                        }
                    });
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    // ---- the checks --------------------------------------------------------

    @Test void thereAreHooksToCheck() {
        // A regex that matched nothing would make every other test here pass
        // by vacancy, which is the way a scan like this usually dies.
        assertFalse(declaredHooks().isEmpty(), "config.yml declares passiveHooks");
    }

    @Test void everyDeclaredHookReachesCodeOrIsAKnownGap() {
        var dead = new TreeSet<String>();
        for (String hook : declaredHooks())
            if (!named(hook) && !UNDISPATCHED.containsKey(hook)) dead.add(hook);

        assertTrue(dead.isEmpty(),
                "These passiveHooks are declared on a class and read by nothing, so the class "
                + "silently has no passive: " + dead + ". Dispatch them, or record them in "
                + "UNDISPATCHED with what it costs.");
    }

    @Test void aFixedHookIsRemovedFromTheKnownGaps() {
        // Without this the list is write-only, and a gap closed in 2026 still
        // reads as open in 2027.
        var stale = new TreeSet<String>();
        for (String hook : UNDISPATCHED.keySet()) if (named(hook)) stale.add(hook);

        assertTrue(stale.isEmpty(),
                "These are dispatched now and should leave UNDISPATCHED: " + stale);
    }

    @Test void everyKnownGapIsStillDeclaredSomewhere() {
        // The other direction: a hook removed from config.yml should leave the
        // list too, rather than sitting here describing a class that is gone.
        var orphaned = new TreeSet<>(UNDISPATCHED.keySet());
        orphaned.removeAll(declaredHooks());
        assertTrue(orphaned.isEmpty(), "No longer in config.yml: " + orphaned);
    }

    @Test void toolboxesHookIsDeadEvenThoughItsPassiveRuns() {
        // The case that makes "the passive works" and "the hook dispatches it"
        // worth separating. UtilityBelt matches on classId == "toolbox"
        // directly, so the declared hook is read by nothing -- and the passive
        // runs anyway, through a second path the config does not describe.
        assertTrue(declaredHooks().contains("utility_belt"));
        assertFalse(named("utility_belt"),
                "If UtilityBelt now dispatches by hook, this test should say so instead");
        assertTrue(named("toolbox"), "it matches the class id instead");
    }

    @Test void theOneWorkingHookProvesTheMechanismIsReal() {
        // Otherwise every assertion above could be satisfied by dispatch not
        // existing at all.
        assertTrue(named("animal_senses"), "Lightfooted's passive goes through the hook");
    }
}
