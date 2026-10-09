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
                    || "?".equals(e.get("input")) || "?".equals(e.get("affects"))
                    || "?".equals(e.get("caster"))))
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
                    + ", Recipients." + e.get("affects")
                    + ("true".equals(e.get("caster")) ? ", true)" : ")");
            if (!src.contains(want)) disagreements.add(impl.getSimpleName() + " should declare " + want);
            assertNotNull(method);
        }
        assertTrue(disagreements.isEmpty(), String.join("; ", disagreements));
    }

    /** Every branch declaration in the manifest, flattened. */
    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> branches() {
        var out = new ArrayList<Map<String, Object>>();
        for (var e : entries()) {
            var declared = (Map<String, Map<String, Object>>) e.get("branches");
            if (declared == null) continue;
            declared.forEach((branch, body) -> {
                var copy = new LinkedHashMap<String, Object>(body);
                copy.put("ability", e.get("id"));
                copy.put("branch", branch);
                copy.put("status", e.get("status"));
                out.add(copy);
            });
        }
        return out;
    }

    @Test void aBranchDeclaresTheWholeTripleRatherThanADeltaOnTheAbility() {
        // A branch is its own thing, not a modifier. Any of the three can
        // differ -- classes.md records that a branch may change an ability's
        // input form -- so a branch carrying only the field that happens to
        // differ today would silently become wrong the first time another one
        // moves.
        var incomplete = new TreeSet<String>();
        for (var b : branches())
            for (String field : new String[]{"target", "input", "affects"})
                if (b.get(field) == null)
                    incomplete.add(b.get("ability") + "." + b.get("branch") + " omits " + field);
        assertTrue(incomplete.isEmpty(), String.valueOf(incomplete));
    }

    @Test void everyBranchDeclarationUsesFormsTheCodeKnows() {
        var unknown = new TreeSet<String>();
        for (var b : branches()) {
            String t = String.valueOf(b.get("target")), i = String.valueOf(b.get("input")),
                   a = String.valueOf(b.get("affects"));
            String where = b.get("ability") + "." + b.get("branch");
            if (!"?".equals(t) && !named(TargetForm.values(), t)) unknown.add(where + " target " + t);
            if (!"?".equals(i) && !named(InputForm.values(), i)) unknown.add(where + " input " + i);
            if (!"?".equals(a) && !named(Recipients.values(), a)) unknown.add(where + " affects " + a);
        }
        assertTrue(unknown.isEmpty(), String.valueOf(unknown));
    }

    @Test void aBuiltAbilitysBranchesHaveAnsweredToo() {
        // Same rule as the ability row: "?" is for a question classes.md has
        // not answered, and something implemented has answered by existing.
        var unanswered = new TreeSet<String>();
        for (var b : branches()) {
            if (!BUILT.equals(b.get("status"))) continue;
            if ("?".equals(b.get("target")) || "?".equals(b.get("input")) || "?".equals(b.get("affects")))
                unanswered.add(b.get("ability") + "." + b.get("branch"));
        }
        assertTrue(unanswered.isEmpty(), "Built but undeclared: " + unanswered);
    }

    @SuppressWarnings("unchecked")
    @Test void everyBuiltAbilityWithBranchesListsAllOfThem() {
        // Listing only the branches that differ would mean a reader could not
        // tell "this branch is the same" from "nobody has checked". Built
        // abilities list all three, so the file answers without a union.
        var missing = new TreeSet<String>();
        for (var e : entries()) {
            if (!BUILT.equals(e.get("status"))) continue;
            Class<?> impl = implementationOf((String) e.get("id"));
            if (impl == null) continue;
            var declared = (Map<String, Object>) e.get("branches");
            int listed = declared == null ? 0 : declared.size();
            if (listed != 0 && listed != 3)
                missing.add(e.get("id") + " lists " + listed + " of its branches");
        }
        assertTrue(missing.isEmpty(), String.valueOf(missing));
    }

    @Test void branchesMayAgreeWithEachOtherAndWithTheAbility() {
        // Deliberately asserted, because an earlier version refused a branch
        // that repeated the base as redundant. It is not redundant: three
        // branches that happen to reach the same people is a fact about the
        // kit, and suppressing it makes "same" indistinguishable from
        // "unchecked". Crash Landing is the case -- all three branches trade
        // self-damage against impact damage, and all three hit enemies.
        var crash = branches().stream()
                .filter(b -> "crash_landing".equals(b.get("ability"))).toList();
        assertEquals(3, crash.size());
        for (var b : crash) assertEquals("ENEMIES", b.get("affects"));
    }

    @Test void aBranchThatReachesSomebodyTheAbilityDoesNotIsVisibleHere() {
        // The reading the combat chamber needs, and the reason this is not a
        // base-plus-override: Suplex and Super Nutritious reach people their
        // own abilities do not, and the file says so directly.
        var suplex = branches().stream()
                .filter(b -> "suplex".equals(b.get("branch"))).findFirst().orElseThrow();
        assertEquals("ENEMIES", suplex.get("affects"));

        var nutritious = branches().stream()
                .filter(b -> "super_nutritious".equals(b.get("branch"))).findFirst().orElseThrow();
        assertEquals("BOTH", nutritious.get("affects"), "it heals allies and still throws food");
    }

    @Test void theCasterIsAskedSeparatelyFromWhoElseIsReached() {
        // Recipients.NONE used to mean "the caster, or nobody", which made
        // Tunneling -- which lands on no one -- indistinguishable from Deathly
        // Clutches, which drops its caster to near-death. Crash Landing is the
        // case an enum could not have held at all: ENEMIES *and* the caster.
        var byId = new LinkedHashMap<String, Map<String, Object>>();
        for (var e : entries()) byId.put((String) e.get("id"), e);

        assertEquals("false", byId.get("tunneling").get("caster"), "it lands on nobody");
        assertEquals("true", byId.get("deathly_clutches").get("caster"));
        assertEquals("NONE", byId.get("deathly_clutches").get("affects"),
                "and it still reaches nobody else, which is the pair that needed separating");

        assertEquals("ENEMIES", byId.get("crash_landing").get("affects"));
        assertEquals("true", byId.get("crash_landing").get("caster"),
                "both at once, which no single enum value could have said");

        assertEquals("false", byId.get("runway").get("caster"),
                "locomotion is the ability working, not an effect landing on you");
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
