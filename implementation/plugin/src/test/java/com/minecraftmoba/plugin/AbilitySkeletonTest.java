package com.minecraftmoba.plugin;

import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The skeleton: every designed class has four named slots, and every built
 * ability has said what it is.
 *
 * <h2>Why a manifest and not config.yml</h2>
 *
 * config.yml already records the decision: Toolbox's unbuilt slots are
 * <i>absent rather than pointed at a placeholder</i>, because "an unbound input
 * does nothing, which is honest; a placeholder would be a lie that looked like
 * a feature". Stubbing sixty unbuilt abilities into the live config would
 * break exactly that -- and {@link AbilityCombat} would then demand a
 * {@code combat} answer for each, which is a design decision being forced by a
 * scaffolding exercise.
 *
 * So the skeleton lives under test resources, where it cannot be loaded, and
 * this test is what makes it worth having.
 */
class AbilitySkeletonTest {

    private static final Set<String> SLOTS = Set.of("passive", "a1", "a2", "ult");
    private static final String BUILT = "built";

    @SuppressWarnings("unchecked")
    private static Map<String, Map<String, Object>> classes() {
        try (InputStream in = AbilitySkeletonTest.class.getResourceAsStream("/ability-skeleton.yml")) {
            assertNotNull(in, "ability-skeleton.yml is the manifest; without it there is no skeleton");
            Map<String, Object> root = new Yaml().load(in);
            return (Map<String, Map<String, Object>>) root.get("classes");
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> entries() {
        var out = new ArrayList<Map<String, Object>>();
        classes().forEach((cls, body) -> {
            var abilities = (Map<String, Map<String, Object>>) body.get("abilities");
            abilities.forEach((slot, a) -> {
                var copy = new LinkedHashMap<String, Object>(a);
                copy.put("class", cls);
                copy.put("slot", slot);
                out.add(copy);
            });
        });
        return out;
    }

    // ---- the shape ---------------------------------------------------------

    @Test void everyClassDeclaresAllFourSlots() {
        // A class missing a slot is not a class with three abilities; it is a
        // class whose fourth was forgotten, and the roster is where that shows.
        var incomplete = new TreeSet<String>();
        classes().forEach((cls, body) -> {
            @SuppressWarnings("unchecked")
            var abilities = (Map<String, Object>) body.get("abilities");
            if (!abilities.keySet().equals(SLOTS)) incomplete.add(cls + " has " + abilities.keySet());
        });
        assertTrue(incomplete.isEmpty(), "Every class has passive, a1, a2, ult: " + incomplete);
    }

    @Test void abilityIdsAreUniqueAcrossTheWholeRoster() {
        // Ids are the wiring. Two classes sharing one would mean a cooldown,
        // a branch selection and a combat flag shared between them.
        var seen = new TreeSet<String>();
        var clashes = new TreeSet<String>();
        for (var e : entries()) if (!seen.add((String) e.get("id"))) clashes.add((String) e.get("id"));
        assertTrue(clashes.isEmpty(), "Duplicate ability ids: " + clashes);
    }

    @Test void theRosterCoversEveryClassDesignedInClassesMd() {
        assertEquals(18, classes().size(),
                "classes.md sections 4-29 design 18 classes; the skeleton covers " + classes().keySet());
    }

    // ---- what a declaration must say ---------------------------------------

    @Test void aBuiltAbilityHasAnsweredBothForms() {
        // "?" is for a question classes.md has not answered. Something that is
        // already implemented has answered it by existing, so leaving the cell
        // open would be the manifest disagreeing with the code.
        var unanswered = new TreeSet<String>();
        for (var e : entries())
            if (BUILT.equals(e.get("status")) && ("?".equals(e.get("target"))
                    || "?".equals(e.get("input")) || "?".equals(e.get("affects"))))
                unanswered.add(e.get("class") + "." + e.get("slot"));
        assertTrue(unanswered.isEmpty(), "Built but undeclared: " + unanswered);
    }

    @Test void everyDeclaredFormIsOneTheCodeKnows() {
        var unknown = new TreeSet<String>();
        for (var e : entries()) {
            String t = (String) e.get("target"), i = (String) e.get("input");
            if (!"?".equals(t) && !named(TargetForm.values(), t)) unknown.add("target " + t);
            if (!"?".equals(i) && !named(InputForm.values(), i)) unknown.add("input " + i);
            String a = (String) e.get("affects");
            if (!"?".equals(a) && !named(Recipients.values(), a)) unknown.add("affects " + a);
        }
        assertTrue(unknown.isEmpty(), "Not a form in the vocabulary: " + unknown);
    }

    @Test void everyPassiveSlotIsPassiveAndSelf() {
        // The one form determined without reading further: a passive has no
        // gesture, so it permits NO cast mode -- which is a different statement
        // from "every mode applies", and the one leaving it blank would make.
        for (var e : entries())
            if ("passive".equals(e.get("slot"))) {
                assertEquals("PASSIVE", e.get("input"), e.get("class") + "'s passive");
                assertEquals("SELF", e.get("target"), e.get("class") + "'s passive");
                assertEquals("NONE", e.get("affects"),
                        e.get("class") + "'s passive: no passive in the roster reaches another "
                        + "player, and the first that does should say so rather than inherit this");
            }
    }

    @Test void nothingBuiltUsesAFormTheInputLayerCannotServe() {
        // VECTOR is named so it is not reinvented badly, and is explicitly not
        // supported. An ability shipping on it would be shipping on nothing.
        for (var e : entries())
            if (BUILT.equals(e.get("status")))
                assertNotEquals("VECTOR", e.get("target"),
                        e.get("class") + "." + e.get("slot") + " is built on an unsupported target form");
    }

    // ---- the manifest against the code -------------------------------------

    @Test void everyAbilityMarkedBuiltDeclaresItsOutputsInJava() throws Exception {
        // The manifest is a claim about the code. This is the half that checks
        // it: Ability.outputs() defaults to EMPTY rather than to a guess, so an
        // implementation that never declared is found here rather than by a
        // cast mode silently doing nothing.
        var missing = new TreeSet<String>();
        for (var e : entries()) {
            if (!BUILT.equals(e.get("status"))) continue;
            Class<?> impl = implementationOf((String) e.get("id"));
            assertNotNull(impl, e.get("id") + " is marked built but has no implementation class");
            try {
                impl.getDeclaredMethod("outputs");
            } catch (NoSuchMethodException notDeclared) {
                missing.add(impl.getSimpleName());
            }
        }
        assertTrue(missing.isEmpty(),
                "Built abilities that never declared a target and input form: " + missing);
    }

    @Test void aBuiltAbilitysJavaAgreesWithTheManifest() throws Exception {
        // Two records of the same fact drift. This is the cross-check that
        // stops them, in the pattern the route and path vocabularies already
        // use: one shared declaration, read from both sides.
        var disagreements = new TreeSet<String>();
        for (var e : entries()) {
            if (!BUILT.equals(e.get("status"))) continue;
            Class<?> impl = implementationOf((String) e.get("id"));
            var method = impl.getDeclaredMethod("outputs");
            var src = new String(java.nio.file.Files.readAllBytes(
                    java.nio.file.Path.of("src/main/java/com/minecraftmoba/plugin/"
                            + impl.getSimpleName() + ".java")));
            String want = "AbilityOutput.single(\"" + e.get("id") + "\", TargetForm."
                    + e.get("target") + ", InputForm." + e.get("input")
                    + ", Recipients." + e.get("affects") + ")";
            if (!src.contains(want)) disagreements.add(impl.getSimpleName() + " should declare " + want);
            assertNotNull(method);
        }
        assertTrue(disagreements.isEmpty(), String.join("; ", disagreements));
    }

    @SuppressWarnings("unchecked")
    @Test void everyBranchAffectsValueIsAFormTheCodeKnows() {
        var unknown = new TreeSet<String>();
        for (var e : entries()) {
            var branches = (Map<String, Object>) e.get("branchAffects");
            if (branches == null) continue;
            branches.forEach((branch, value) -> {
                if (!named(Recipients.values(), String.valueOf(value)))
                    unknown.add(e.get("id") + "." + branch + " = " + value);
            });
        }
        assertTrue(unknown.isEmpty(), "Not a recipient the code knows: " + unknown);
    }

    @SuppressWarnings("unchecked")
    @Test void aBranchIsOnlyRecordedWhenItChangesTheAnswer() {
        // A branch repeating the base is noise that reads as a decision, and
        // the next person has to check the design to find out it was not one.
        var redundant = new TreeSet<String>();
        for (var e : entries()) {
            var branches = (Map<String, Object>) e.get("branchAffects");
            if (branches == null) continue;
            branches.forEach((branch, value) -> {
                if (String.valueOf(value).equals(e.get("affects")))
                    redundant.add(e.get("id") + "." + branch);
            });
        }
        assertTrue(redundant.isEmpty(), "Same as the base, so says nothing: " + redundant);
    }

    @SuppressWarnings("unchecked")
    @Test void worstCaseCoverageIsTheRowUnionedWithItsBranches() {
        // The reading a combat chamber needs. Asserted on the two cases that
        // exist so the union is a documented operation rather than something
        // each reader works out again.
        var runway = entries().stream().filter(e -> "runway".equals(e.get("id"))).findFirst().orElseThrow();
        assertEquals("NONE", runway.get("affects"));
        assertEquals("ENEMIES", ((Map<String, Object>) runway.get("branchAffects")).get("suplex"),
                "a chamber reading the row alone would stand up no enemy dummy for Suplex");

        var food = entries().stream().filter(e -> "food_fight".equals(e.get("id"))).findFirst().orElseThrow();
        assertEquals("BOTH", ((Map<String, Object>) food.get("branchAffects")).get("super_nutritious"),
                "Super Nutritious heals allies it hits, and the base does not");
    }

    @Test void anAbilityWhoseBranchChangesWhoItReachesSaysSoInJava() {
        // The manifest has one row per slot and cannot express branch variance.
        // Runway is NONE on Pop Rocket and Trampoline and ENEMIES on Suplex,
        // which config.yml already records as combat false and combat true.
        // A reader taking the row as the whole answer would under-provision a
        // combat chamber for Suplex, so the override must exist.
        try {
            String src = java.nio.file.Files.readString(java.nio.file.Path.of(
                    "src/main/java/com/minecraftmoba/plugin/RunwayAbility.java"));
            assertTrue(src.contains("Recipients.ENEMIES"),
                    "Suplex reaches an enemy and the base declaration does not say so");
            assertTrue(src.contains("outputs(org.bukkit.entity.Player"),
                    "the per-branch declaration is where that lives");
        } catch (java.io.IOException e) {
            throw new AssertionError(e);
        }
    }

    @Test void noBuiltAbilityClaimsToReachAlliesYet() {
        // Not a rule -- a fact worth asserting, so that the first ally-facing
        // ability is a deliberate change here rather than a quiet one. The
        // combat chamber's ally dummy currently has nothing to receive.
        for (var e : entries())
            if (BUILT.equals(e.get("status")))
                assertNotEquals("ALLIES", e.get("affects"),
                        e.get("class") + "." + e.get("slot") + " is the first; update this test");
    }

    private static Class<?> implementationOf(String id) {
        var camel = new StringBuilder();
        for (String part : id.split("_"))
            camel.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        try {
            return Class.forName("com.minecraftmoba.plugin." + camel + "Ability");
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    private static boolean named(Enum<?>[] values, String name) {
        for (Enum<?> v : values) if (v.name().equals(name)) return true;
        return false;
    }
}
