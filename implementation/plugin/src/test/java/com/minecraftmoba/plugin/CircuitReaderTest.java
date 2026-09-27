package com.minecraftmoba.plugin;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.minecraftmoba.plugin.CircuitComponent.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Toolbox's program, read off the inventory.
 *
 * The decisive tests are {@link #flowBeginsAtTheTopLeftAndEndsAtTheHotbar} and
 * {@link #parityFlipsAtEveryWrapSoAGreedyBoardGetsNoParallelism}: both encode
 * geometry that was got wrong in design before it was got right, and both are
 * invisible from Bukkit's slot numbering alone.
 */
class CircuitReaderTest {

    private Material[] inventory() { return new Material[36]; }

    private Material[] put(Material[] inv, int slot, Material m) { inv[slot] = m; return inv; }

    /** Lay a row out from its leftmost slot, left to right on screen. */
    private Material[] row(Material[] inv, int from, Material... items) {
        for (int i = 0; i < items.length; i++) inv[from + i] = items[i];
        return inv;
    }

    private static final Material D = Material.REDSTONE;
    private static final Material P = Material.PISTON;
    private static final Material O = Material.OBSERVER;
    private static final Material T = Material.TRIPWIRE_HOOK;
    private static final Material R = Material.REPEATER;
    private static final Material SP = Material.STICKY_PISTON;

    // ---- flow order -------------------------------------------------------

    /**
     * Counting begins at the top-left -- slot 9 -- and the hotbar is LAST.
     *
     * Assuming the hotbar came first is the error that killed an entire
     * "loopback" finding, because it made flow appear to run backwards.
     */
    @Test void flowBeginsAtTheTopLeftAndEndsAtTheHotbar() {
        assertEquals(9, CircuitReader.FLOW[0], "row A, top-left on screen");
        assertEquals(35, CircuitReader.FLOW[26], "row C ends the storage rows");
        assertEquals(0, CircuitReader.FLOW[27], "the hotbar follows, as the magazine");
        assertEquals(8, CircuitReader.FLOW[35]);
        assertEquals(36, CircuitReader.FLOW.length);
    }

    /** Row C sits directly above the hotbar, which is a wrap in index space only. */
    @Test void beneathFollowsTheScreenNotTheIndex() {
        assertEquals(18, CircuitReader.below(9),  "A -> B");
        assertEquals(27, CircuitReader.below(18), "B -> C");
        assertEquals(0,  CircuitReader.below(27), "C -> hotbar, across the index wrap");
        assertEquals(-1, CircuitReader.below(0),  "nothing is beneath the hotbar");
    }

    // ---- the magazine boundary -------------------------------------------

    @Test void flowStopsAtTheFirstItemThatIsNotProgram() {
        var inv = row(inventory(), 9, P, D, O);
        inv[12] = Material.DIAMOND_SWORD;
        var circuit = CircuitReader.read(inv, 36);

        assertEquals(2, circuit.components(), "the sword is magazine, not program");
        assertEquals(12, circuit.magazine());
    }

    /**
     * Flow STEPS OVER a locked cell rather than ending there.
     *
     * It has to: slots unlock in raw index order, so the hotbar frees first
     * while row A -- where flow begins -- is still locked. Ending at the first
     * locked cell would leave a Lv0 Toolbox unable to run anything, when the
     * design says its program and magazine are forced to share the hotbar.
     *
     * This was caught by the test rather than by reading, and the reader was
     * wrong first.
     */
    @Test void flowStepsOverLockedCellsRatherThanEndingAtThem() {
        var inv = row(inventory(), 0, P, D, O, D, P, D, P);   // hotbar only
        var circuit = CircuitReader.read(inv, 6);

        assertEquals(3, circuit.components(), "P O P -- the fourth Piston is past the lock");
        // And there is NO magazine, which is the Lv0 problem stated exactly:
        // every unlocked cell holds program, so the machine has nothing to
        // fire. Dispenser and Dropper take "the first item that is not
        // program", and at six slots a full program leaves no such item.
        assertEquals(-1, circuit.magazine(),
                "a fully programmed six-slot board has no ammunition at all");
    }

    /**
     * At six slots the program and the magazine are forced to share the hotbar.
     *
     * This is the Lv0 problem exactly, and it is why the jump at Lv3 is bigger
     * than "+6 slots" suggests: the program leaves the hotbar entirely.
     */
    @Test void atSixSlotsARowAProgramIsUnreachable() {
        var inv = row(inventory(), 9, P, D, O);  // row A holds a program
        assertTrue(CircuitReader.read(inv, 6).isEmpty(),
                "row A is locked at Lv0, and flow finds nothing on the way past it");
    }

    // ---- timing -----------------------------------------------------------

    /** Dust costs an item and no time, so duration is component count, not cell count. */
    @Test void wiringCostsItemsButNoTime() {
        var wired   = CircuitReader.read(row(inventory(), 9, P, D, O, D, P), 36);
        var packed  = CircuitReader.read(row(inventory(), 9, P, O, P), 36);

        assertEquals(3, wired.components());
        assertEquals(List.of(10, 12), wired.wiring());
        assertEquals(packed.duration(), wired.duration(),
                "two extra wiring cells must cost zero ticks");
        assertEquals(10, wired.duration(), "three components on a five-tick grid");
    }

    @Test void componentsFireOnAFiveTickGrid() {
        var circuit = CircuitReader.read(row(inventory(), 9, P, D, O, D, SP), 36);
        assertEquals(List.of(0, 5, 10), circuit.steps().stream().map(CircuitReader.Step::tick).toList());
    }

    /** A Repeater delays the NEXT component by one interval, not itself. */
    @Test void aRepeaterDelaysTheComponentAfterIt() {
        var circuit = CircuitReader.read(row(inventory(), 9, P, D, R, O), 36);
        var ticks = circuit.steps().stream().map(CircuitReader.Step::tick).toList();
        assertEquals(List.of(0, 5, 15), ticks,
                "Piston at 0, Repeater at 5, Observer one extra interval later");
    }

    /** A Repeater needs dust before it but transmits without dust after it. */
    @Test void aRepeaterCarriesFlowWithNoWiringAfterIt() {
        var circuit = CircuitReader.read(row(inventory(), 9, P, D, R, O), 36);
        assertEquals(3, circuit.components());
        assertEquals(List.of(10), circuit.wiring(), "only the dust BEFORE the repeater is wiring");
    }

    // ---- Tripwire ---------------------------------------------------------

    /**
     * The horizontal next component resolves first; the hooked one follows it
     * by a tick. A secondary activation can never affect the primary, which is
     * what makes Tripwire parallel execution rather than a branch.
     */
    @Test void theHookedComponentResolvesAfterTheHorizontalOne() {
        var inv = row(inventory(), 9, T, O);     // hook at 9, Observer at 10
        put(inv, 18, P);                          // Piston directly beneath the hook
        var circuit = CircuitReader.read(inv, 36);

        var steps = circuit.steps();
        assertEquals(new CircuitReader.Step(9, TRIPWIRE_HOOK, 0, false), steps.get(0));
        assertEquals(new CircuitReader.Step(10, OBSERVER, 5, false), steps.get(1));
        assertEquals(new CircuitReader.Step(18, PISTON, 6, true), steps.get(2),
                "the hooked Piston resolves one tick after the Observer, never before");
    }

    /** A hook over a locked, empty or off-board cell simply does nothing. */
    @Test void aHookOverNothingIsLegalAndSilent() {
        var overEmpty  = CircuitReader.read(row(inventory(), 9, T, Material.REDSTONE, P), 36);
        assertTrue(overEmpty.steps().stream().noneMatch(CircuitReader.Step::hooked));

        var inv = row(inventory(), 0, T);
        assertTrue(CircuitReader.read(inv, 36).steps().stream().noneMatch(CircuitReader.Step::hooked),
                "the hotbar has nothing beneath it");
    }

    /**
     * At eighteen slots rows A and B are not vertically adjacent, so NO hook
     * can land -- the board is two full 9-wide runs and nothing else.
     *
     * This is the geometry an earlier draft got wrong by claiming 18 gave
     * "full vertical correspondence". It gives none.
     */
    @Test void eighteenSlotsIsAHorizontalMilestoneOnly() {
        var inv = row(inventory(), 9, T, D, T, D, T);
        put(inv, 18, P);
        var circuit = CircuitReader.read(inv, 18);
        assertTrue(circuit.steps().stream().noneMatch(CircuitReader.Step::hooked),
                "row B is locked at 18 unlocked slots, so every hook targets a locked cell");
    }

    /**
     * Parity: one wiring cell preserves it, two flip it.
     *
     * A row packed greedily as `C d C d C d C d C` ends on a component, so the
     * next row must begin with dust and every hook misses. The shim is the
     * whole reason Tripwire is a layout investment rather than free.
     */
    @Test void parityFlipsAtEveryWrapSoAGreedyBoardGetsNoParallelism() {
        var greedy = inventory();
        row(greedy, 9,  T, D, T, D, T, D, T, D, T);   // components on even positions
        row(greedy, 18, D, P, D, P, D, P, D, P, D);   // components on ODD positions
        assertTrue(CircuitReader.read(greedy, 36).steps().stream().noneMatch(CircuitReader.Step::hooked),
                "every hook lands on dust");

        var shimmed = inventory();
        row(shimmed, 9,  T, D, T, D, T, D, T, D, D);  // double wiring flips parity back
        row(shimmed, 18, P, D, P, D, P, D, P, D, D);
        assertEquals(4, CircuitReader.read(shimmed, 36).steps().stream()
                        .filter(CircuitReader.Step::hooked).count(),
                "with the shim, all four hooks land");
    }

    // ---- Dust's two roles -------------------------------------------------

    /** Dust at the head is an instruction, not "skip me". */
    @Test void leadingDustIsAnInstruction() {
        var circuit = CircuitReader.read(row(inventory(), 9, D, P), 36);
        assertEquals(DUST, circuit.steps().getFirst().component());
        assertEquals(0, circuit.steps().getFirst().tick());
        assertTrue(circuit.wiring().isEmpty(), "nothing precedes it, so it wires nothing");
    }

    /** Dust with no component after it was never wiring either. */
    @Test void trailingDustDischargesRatherThanWiringNothing() {
        var circuit = CircuitReader.read(row(inventory(), 9, P, D), 36);
        assertEquals(2, circuit.steps().size());
        assertEquals(DUST, circuit.steps().get(1).component());
        assertTrue(circuit.wiring().isEmpty());
    }

    @Test void dustBetweenTwoComponentsIsWiringAndFiresNothing() {
        var circuit = CircuitReader.read(row(inventory(), 9, P, D, O), 36);
        assertEquals(2, circuit.steps().size(), "the dust resolves nothing");
        assertEquals(List.of(10), circuit.wiring());
    }

    // ---- the dictionary ---------------------------------------------------

    @Test void nonProgramItemsAreNotComponents() {
        assertNull(CircuitComponent.of(Material.DIAMOND_SWORD));
        assertNull(CircuitComponent.of(Material.AIR));
        assertNull(CircuitComponent.of(null));
        assertFalse(CircuitComponent.isProgram(Material.COOKED_BEEF));
    }

    /** Effectless components are still program: a Lamp mid-board is a pause. */
    @Test void anEffectlessComponentStillCostsAnInterval() {
        var circuit = CircuitReader.read(row(inventory(), 9, P, D, Material.REDSTONE_LAMP, D, O), 36);
        assertEquals(3, circuit.components());
        assertEquals(10, circuit.duration(), "the Lamp delays the Observer by a full interval");
        assertEquals(INERT, circuit.steps().get(1).component());
    }

    @Test void anEmptyInventoryReadsAsAnEmptyCircuit() {
        var circuit = CircuitReader.read(inventory(), 36);
        assertTrue(circuit.isEmpty());
        assertEquals(0, circuit.duration());
        assertEquals(9, circuit.magazine(), "the board stops at the very first cell");
    }
}
