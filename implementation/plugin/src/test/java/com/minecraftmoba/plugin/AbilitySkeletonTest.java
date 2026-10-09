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

    /** One row per SLOT: id, name, status, and its outputs map. */
    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> slots() {
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

    /**
     * One row per OUTPUT, which is the unit everything else is asked about.
     *
     * An ability is a set of one or more outputs, so a test that asked a slot
     * "what do you target" would be asking the wrong thing of Graveyard Shift.
     */
    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> entries() {
        var out = new ArrayList<Map<String, Object>>();
        for (var slot : slots()) {
            var outputs = (Map<String, Map<String, Object>>) slot.get("outputs");
            assertNotNull(outputs, slot.get("id") + " declares no outputs map");
            outputs.forEach((name, o) -> {
                var copy = new LinkedHashMap<String, Object>(o);
                copy.put("class", slot.get("class"));
                copy.put("slot", slot.get("slot"));
                copy.put("id", slot.get("id"));
                copy.put("output", name);
                copy.put("status", slot.get("status"));
                out.add(copy);
            });
        }
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
        for (var e : slots()) if (!seen.add((String) e.get("id"))) clashes.add((String) e.get("id"));
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
                // NOT asserted NONE. Fungal Assassin's Creeping Colony applies
                // Fungal Growth to enemies Fungal Assassin attacks, so the
                // roster's first enemy-facing passive exists -- and the earlier
                // version of this test would have forced it to lie.
                assertNotEquals("?", e.get("affects"),
                        e.get("class") + "'s passive has not said who it reaches");
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
        for (var e : slots()) {
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
            if (!BUILT.equals(e.get("status")) || !e.get("id").equals(e.get("output"))) continue;
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

    /** Every branch's every output, flattened. */
    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> branches() {
        var out = new ArrayList<Map<String, Object>>();
        for (var slot : slots()) {
            var declared = (Map<String, Map<String, Object>>) slot.get("branches");
            if (declared == null) continue;
            declared.forEach((branch, body) -> {
                var outputs = (Map<String, Map<String, Object>>) body.get("outputs");
                assertNotNull(outputs, slot.get("id") + "." + branch + " declares no outputs");
                outputs.forEach((name, o) -> {
                    var copy = new LinkedHashMap<String, Object>(o);
                    copy.put("ability", slot.get("id"));
                    copy.put("branch", branch);
                    copy.put("output", name);
                    copy.put("status", slot.get("status"));
                    out.add(copy);
                });
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
        for (var e : slots()) {
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
        // single-output abilities only, which all of the ones named here are

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

    @Test void aPassiveMayReachSomebodyAndExactlyOneDoes() {
        // Worth pinning, because the shape of a passive is otherwise assumed:
        // every other one in the roster acts on its own holder.
        var enemyFacing = new TreeSet<String>();
        for (var e : entries())
            if ("passive".equals(e.get("slot")) && !"NONE".equals(e.get("affects")))
                enemyFacing.add(e.get("class") + " (" + e.get("affects") + ")");
        assertEquals(Set.of("fungal_assassin (ENEMIES)"), enemyFacing,
                "Creeping Colony applies Fungal Growth to enemies attacked; a second "
                + "such passive is a design change worth noticing here");
    }

    @SuppressWarnings("unchecked")
    @Test void anAbilityIsASetOfOutputsAndFourOfThemAreMoreThanOne() {
        // classes.md settled that the unit of description is an OUTPUT. The
        // manifest used to put one output's fields on the ability, which
        // quietly contradicted that and could not hold the ones that need it.
        //
        // Two kinds appear. Graveyard Shift and Flip and Press hold outputs
        // selected by what the cast FOUND. Capillary Flow and Viscerwall hold
        // the held channels Bloodmason's Ultimate re-expresses them as, which
        // a third thing selects: the Anatomb being up.
        var multi = new TreeSet<String>();
        for (var slot : slots()) {
            var outputs = (Map<String, Object>) slot.get("outputs");
            if (outputs.size() > 1) multi.add(slot.get("id") + "=" + new TreeSet<>(outputs.keySet()));
        }
        assertEquals(Set.of("graveyard_shift=[raise, strike]", "flip_and_press=[flip, press]",
                            "capillary_flow=[capillary_flow, hemorrhage]",
                            "viscerwall=[teratoma, viscerwall]"), multi, "found: " + multi);
    }

    @Test void twoOutputsOfOneAbilityAreToldApartByTargetOrByGesture() {
        // Not by gesture alone, which is what AbilityOutput.of used to demand.
        // Graveyard Shift strikes a targeted enemy and otherwise raises a Crew
        // Member: one gesture, two outputs, chosen by what the cast found.
        var byOutput = new LinkedHashMap<String, Map<String, Object>>();
        for (var e : entries())
            if ("graveyard_shift".equals(e.get("id"))) byOutput.put((String) e.get("output"), e);

        assertEquals("UNIT_ENTITY", byOutput.get("strike").get("target"));
        assertEquals("SELF", byOutput.get("raise").get("target"));
        assertEquals(byOutput.get("strike").get("input"), byOutput.get("raise").get("input"),
                "the gesture is the same, so the target is what selects");
    }

    @Test void aBranchDeclaresEveryOutputAndNotOnlyTheOneItChanges() {
        // Field Work turns Graveyard Shift into a projectile, moving BOTH
        // outputs to DIRECTION. A branch recording only what differs would
        // have to say which output it meant.
        var fieldWork = branches().stream()
                .filter(b -> "field_work".equals(b.get("branch"))).toList();
        assertEquals(2, fieldWork.size(), "both outputs: " + fieldWork);
        for (var o : fieldWork)
            assertEquals("DIRECTION", o.get("target"), o.get("output") + " becomes a projectile too");
    }

    @Test void theOnlyHeldOutputsAreTheOnesTheDesignSaysToHold() {
        // INSTANT is not a default applied for want of evidence. classes.md
        // records the preference -- "prefer press to a deterministic state
        // sequence over hold and release, unless continuous charging is
        // indispensable" -- and says so explicitly where it could be misread,
        // as in Stalking Pounce: "There is no hold-and-release input".
        //
        // So a held output is a claim, and these three make it in as many words.
        var held = new TreeSet<String>();
        for (var e : entries())
            if (!"INSTANT".equals(e.get("input")) && !"PASSIVE".equals(e.get("input")))
                held.add(e.get("input") + " " + e.get("output"));
        assertEquals(Set.of("CHANNELED hemorrhage", "CHANNELED teratoma",
                            "CHANNELED eternity_mountain"), held, "found: " + held);
    }

    @Test void nothingInTheRosterIsCharged() {
        // CHARGED means the output fires ONCE at whatever a held aim reached --
        // a beam that widens while aimed. Bloodmason's channels are not that:
        // they repeat an effect every half second held. Worth pinning, because
        // CastMode.effectiveFor and InputForm.resolve both exist to handle
        // CHARGED and neither has ever been exercised against a real ability.
        for (var e : entries())
            assertNotEquals("CHARGED", e.get("input"),
                    e.get("output") + " would be the first; both Quick-to-Hold upgrade "
                    + "paths become live the moment it is");
    }

    @Test void anUltimateMayReExpressAnAbilityAsAnotherOutput() {
        // Bloodmason's Anatomb turns Capillary Flow into Hemorrhage and
        // Viscerwall into Teratoma, both held channels. They are outputs of
        // their own abilities rather than of the Ultimate, because that is
        // where the gesture lands -- and the pair (target, input) differs from
        // the base, so AbilityOutput.of admits them.
        var capillary = entries().stream()
                .filter(e -> "capillary_flow".equals(e.get("id"))).toList();
        assertEquals(2, capillary.size(), "base and its Anatomb re-expression");
        assertEquals(Set.of("INSTANT", "CHANNELED"),
                capillary.stream().map(e -> e.get("input")).collect(java.util.stream.Collectors.toSet()));
    }

    @Test void theRostersOnlyAllyFacingOutputsAreChefsAndTheTalismaniacs() {
        // The combat chamber's ally dummy has exactly these to receive, and
        // neither is built. Asserted so a third arrives deliberately.
        var ally = new TreeSet<String>();
        for (var e : entries())
            if ("ALLIES".equals(e.get("affects")) || "BOTH".equals(e.get("affects")))
                ally.add((String) e.get("id"));
        for (var b : branches())
            if ("ALLIES".equals(b.get("affects")) || "BOTH".equals(b.get("affects")))
                ally.add(b.get("ability") + "." + b.get("branch"));
        assertEquals(Set.of("talisman_of_undying", "food_fight.super_nutritious"), ally,
                "found: " + ally);
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
