package com.minecraftmoba.plugin;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The Java half of the per-column path classification.
 *
 * The compiler's half is {@code path_pieces.column_character}. Two copies exist
 * for the same reason the height profile's do -- the compiler authors without a
 * server, the lab classifies at runtime against live blocks -- and they had
 * drifted into answering different questions:
 *
 * <ul>
 *   <li><b>BRIDGE</b> meant <i>water</i> here and <i>a void beneath</i> there.
 *       Both are true, and a path over a ravine needs the same piece as a path
 *       over a river, so a bridge now has two reasons and one name.</li>
 *   <li><b>LANDING</b> meant <i>the first or last column</i> here and <i>a
 *       slope reversal</i> there. The first of those is an ENTRANCE; the second
 *       is a grouping artefact and is not a column character at all.</li>
 *   <li>This copy tested <b>slope before water</b>, so a sloping column over a
 *       river classified as a staircase.</li>
 * </ul>
 *
 * Parsed by hand, following {@link EligibilityCrossCheckTest}: the fixture is
 * small, and the point is that both sides read the SAME bytes.
 */
class PathCharacterCrossCheckTest {

    private static final Path FIXTURE =
            Path.of("../worldgen/fixtures/path-character-crosscheck.json");

    private record Case(String note, int[] profile, int[] raw, boolean[] water,
                        Set<Integer> turns, boolean termini, List<String> characters) {}

    private static final List<Case> CASES = new ArrayList<>();

    @BeforeAll
    static void load() throws Exception {
        String json = Files.readString(FIXTURE);
        Matcher m = Pattern.compile(
                "\\{\\s*\"note\":\\s*\"([^\"]*)\",\\s*"
                        + "\"profile\":\\s*\\[([^\\]]*)\\],\\s*"
                        + "\"raw\":\\s*\\[([^\\]]*)\\],\\s*"
                        + "\"water\":\\s*\\[([^\\]]*)\\],\\s*"
                        + "\"turns\":\\s*\\[([^\\]]*)\\],\\s*"
                        + "\"termini\":\\s*(true|false),\\s*"
                        + "\"characters\":\\s*\\[([^\\]]*)\\]", Pattern.DOTALL)
                .matcher(json);
        while (m.find())
            CASES.add(new Case(m.group(1), ints(m.group(2)), ints(m.group(3)),
                    bools(m.group(4)), intSet(m.group(5)),
                    Boolean.parseBoolean(m.group(6)), strings(m.group(7))));
    }

    private static int[] ints(String body) {
        return Arrays.stream(body.split(",")).map(String::trim).filter(s -> !s.isEmpty())
                .mapToInt(Integer::parseInt).toArray();
    }

    private static Set<Integer> intSet(String body) {
        return Arrays.stream(body.split(",")).map(String::trim).filter(s -> !s.isEmpty())
                .map(Integer::parseInt).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static boolean[] bools(String body) {
        String[] parts = Arrays.stream(body.split(",")).map(String::trim)
                .filter(s -> !s.isEmpty()).toArray(String[]::new);
        boolean[] out = new boolean[parts.length];
        for (int i = 0; i < parts.length; i++) out[i] = Boolean.parseBoolean(parts[i]);
        return out;
    }

    private static List<String> strings(String body) {
        return Arrays.stream(body.split(",")).map(s -> s.trim().replace("\"", ""))
                .filter(s -> !s.isEmpty()).toList();
    }

    @Test void theFixtureWasActuallyRead() {
        // A regex that matched nothing would make every assertion below vacuous.
        assertFalse(CASES.isEmpty(), "no cases parsed from " + FIXTURE.toAbsolutePath());
        assertTrue(CASES.size() >= 10, "parsed only " + CASES.size());
    }

    @Test void everyCaseReproducesItsRecordedCharacters() {
        for (Case c : CASES) {
            var got = new ArrayList<String>();
            for (int i = 0; i < c.profile().length; i++)
                got.add(LabGeometry.character(c.profile(), c.raw(), i, c.water(),
                        c.turns(), LabGeometry.SPAN_DEVIATION, c.termini()).name());
            assertEquals(c.characters(), got, c.note());
        }
    }

    @Test void aBridgeHasTwoReasonsAndOneName() {
        // Water was this copy's definition and a void was the compiler's. A
        // path over a ravine needs the same piece as a path over a river.
        int[] flat = {70, 70, 70};
        assertEquals(LabGeometry.Module.BRIDGE,
                LabGeometry.character(flat, flat, 1, new boolean[]{false, true, false},
                        Set.of(), LabGeometry.SPAN_DEVIATION, true));
        assertEquals(LabGeometry.Module.BRIDGE,
                LabGeometry.character(flat, new int[]{70, 55, 70}, 1, new boolean[3],
                        Set.of(), LabGeometry.SPAN_DEVIATION, true));
    }

    @Test void waterOutranksSlopeBecauseAStaircaseIntoARiverIsNotAStaircase() {
        // The defect in this copy's old order, named.
        int[] climb = {70, 71, 72};
        assertEquals(LabGeometry.Module.BRIDGE,
                LabGeometry.character(climb, climb, 1, new boolean[]{false, true, false},
                        Set.of(), LabGeometry.SPAN_DEVIATION, true));
    }

    @Test void aTerminusOutranksTheGroundUnderIt() {
        int[] flat = {70, 70};
        assertEquals(LabGeometry.Module.ENTRANCE,
                LabGeometry.character(flat, flat, 0, new boolean[]{true, true},
                        Set.of(), LabGeometry.SPAN_DEVIATION, true));
    }

    @Test void landingIsNotAColumnCharacter() {
        // It is synthesised between two reversing stair runs, which is a
        // grouping decision the compiler owns. Cross-checking it would force
        // this side to implement run-grouping it has no use for.
        for (Case c : CASES)
            assertFalse(c.characters().contains("LANDING"), c.note());
    }

    @Test void theFixtureExercisesEveryCharacter() {
        var produced = CASES.stream().flatMap(c -> c.characters().stream())
                .collect(Collectors.toSet());
        for (var m : List.of("ENTRANCE", "BRIDGE", "STAIR", "BEND", "STRAIGHT"))
            assertTrue(produced.contains(m), "no case produces " + m);
    }
}
