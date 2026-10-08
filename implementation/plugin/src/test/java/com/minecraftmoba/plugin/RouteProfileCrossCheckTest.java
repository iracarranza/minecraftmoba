package com.minecraftmoba.plugin;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The Java half of a predicate that exists twice.
 *
 * The walkable height profile is implemented here, in {@link LabGeometry}, and
 * in the offline compiler's {@code terrain_harvest/routes.py}. Two copies are
 * necessary rather than careless: the compiler authors Routes into a world file
 * without a server, and the lab fits paths at runtime against live blocks.
 * Neither can call the other.
 *
 * <h2>Two copies of a rule drift, and these had</h2>
 *
 * Measured over 2,000 random terrain profiles the two disagreed on <b>86%</b>
 * of them, and the whole difference was anchors: this copy pins the path to
 * them, the Python copy did not.
 *
 * <b>This copy was right.</b> The Python one was always walkable only because
 * it was free to ignore the anchor, and a path that does not reach its door is
 * not a walkable path but a different path. On every FEASIBLE input, neither
 * produced a step a walker could not climb.
 *
 * What was missing from both was saying when an input is infeasible -- two
 * anchors further apart than the columns between them allow. That was absorbed
 * silently into whichever end lost, which reads as a cliff at a doorway. The
 * compiler reports it now.
 *
 * This fixture is what keeps the two together. If they disagree, the lab is
 * previewing a path the compiler would not build.
 *
 * Parsed by hand rather than by adding a JSON dependency, following
 * {@link EligibilityCrossCheckTest}: the fixture is small, and the point of the
 * file is that both sides read the SAME bytes.
 */
class RouteProfileCrossCheckTest {

    private static final Path FIXTURE =
            Path.of("../worldgen/fixtures/route-profile-crosscheck.json");

    private record Case(String note, int[] raw, int[] profile, boolean faulted) {}

    private static final List<Case> CASES = new ArrayList<>();

    @BeforeAll
    static void load() throws Exception {
        String json = Files.readString(FIXTURE);
        // Each case is a flat object; the arrays are the only bracketed lists
        // inside it, in declaration order: raw, profile, faults.
        Matcher m = Pattern.compile(
                "\\{\\s*\"note\":\\s*\"([^\"]*)\",\\s*"
                        + "\"raw\":\\s*\\[([^\\]]*)\\],\\s*"
                        + "\"profile\":\\s*\\[([^\\]]*)\\],\\s*"
                        + "\"faults\":\\s*\\[([^\\]]*)\\]", Pattern.DOTALL)
                .matcher(json);
        while (m.find())
            CASES.add(new Case(m.group(1), ints(m.group(2)), ints(m.group(3)),
                    !m.group(4).isBlank()));
    }

    private static int[] ints(String body) {
        return java.util.Arrays.stream(body.split(","))
                .map(String::trim).filter(s -> !s.isEmpty())
                .mapToInt(Integer::parseInt).toArray();
    }

    @Test void theFixtureWasActuallyRead() {
        // A regex that matched nothing would make every assertion below vacuous.
        assertFalse(CASES.isEmpty(), "no cases parsed from " + FIXTURE.toAbsolutePath());
        assertTrue(CASES.size() >= 10, "parsed only " + CASES.size());
    }

    @Test void everyCaseReproducesItsRecordedProfile() {
        for (Case c : CASES)
            assertArrayEquals(c.profile(), LabGeometry.profile(c.raw()),
                    c.note() + "\n  raw      " + java.util.Arrays.toString(c.raw())
                            + "\n  expected " + java.util.Arrays.toString(c.profile())
                            + "\n  got      " + java.util.Arrays.toString(LabGeometry.profile(c.raw())));
    }

    @Test void theAnchorsAreAlwaysMetExactly() {
        for (Case c : CASES) {
            int[] got = LabGeometry.profile(c.raw());
            assertEquals(c.raw()[0], got[0], c.note());
            assertEquals(c.raw()[c.raw().length - 1], got[got.length - 1], c.note());
        }
    }

    @Test void aFeasibleCaseIsWalkableEverywhere() {
        // The defect this test exists for. The clamp loops ran from 1 to
        // length-2, so a pinned end was never compared with its neighbour.
        for (Case c : CASES) {
            if (c.faulted()) continue;          // infeasible by the fixture's own reckoning
            int[] got = LabGeometry.profile(c.raw());
            for (int i = 1; i < got.length; i++)
                assertTrue(Math.abs(got[i] - got[i - 1]) <= 1,
                        c.note() + ": step of " + Math.abs(got[i] - got[i - 1])
                                + " at column " + i);
        }
    }

    @Test void theFixtureCarriesBothOutcomes() {
        // A fixture of only clean cases would pass against an implementation
        // that never fitted anything difficult.
        assertTrue(CASES.stream().anyMatch(Case::faulted), "no infeasible cases");
        assertTrue(CASES.stream().anyMatch(c -> !c.faulted()), "no clean cases");
    }
}
