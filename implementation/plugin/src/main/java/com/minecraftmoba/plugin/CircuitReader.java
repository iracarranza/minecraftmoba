package com.minecraftmoba.plugin;

import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;

/**
 * Reads Toolbox's inventory as a circuit: what fires, in what order, and when.
 *
 * <h2>The core rule</h2>
 *
 * Inventory arrangement <i>is</i> circuit topology. This class is the half of
 * that which can be decided without a world -- the program's shape and its
 * schedule. What each component then <i>does</i> samples live world state at
 * its own resolution tick and is deliberately not here.
 *
 * <h2>Flow order follows the screen, not Bukkit</h2>
 *
 * Bukkit's slot indices do not match what a player sees, and flow follows the
 * screen. This is the single most common way to get Toolbox wrong, and it
 * invalidated several worked examples before it was noticed, so the order is a
 * named constant rather than arithmetic at a call site:
 *
 * <pre>
 *   flow order        screen position        Bukkit
 *     1st   row A     top                     9-17
 *     2nd   row B                            18-26
 *     3rd   row C     directly above hotbar  27-35
 *     4th   hotbar    bottom                  0- 8
 * </pre>
 *
 * Counting begins at the <b>top-left</b>. The hotbar is last, which is what
 * makes it the magazine.
 *
 * <h2>The magazine boundary is decided by absence</h2>
 *
 * Flow runs until it reaches something that is not program, and that item is
 * what Dispenser and Dropper take. There is no rule separating circuit from
 * ammunition: the boundary is wherever the program stops, the player chooses
 * where that is, and whatever they are carrying is what the machine fires.
 *
 * <h2>Locked cells keep their geometry</h2>
 *
 * An unavailable slot is not skipped and does not compress the board. It ends
 * the program like any other non-program cell, so inventory capacity decides
 * the <b>dimensions of a programmable board</b> rather than merely how much
 * Toolbox can carry. That is why the unlock order matters at all.
 */
public final class CircuitReader {

    /** Screen reading order: row A, row B, row C, then the hotbar. */
    public static final int[] FLOW = flow();

    private static int[] flow() {
        int[] order = new int[36];
        int at = 0;
        for (int slot = 9; slot <= 35; slot++) order[at++] = slot;  // rows A, B, C
        for (int slot = 0; slot <= 8; slot++) order[at++] = slot;   // hotbar last
        return order;
    }

    /** Ticks between consecutive components. [PROTOTYPE] */
    public static final int INTERVAL = 5;

    /**
     * How much later than the interval a Tripwire's vertical target resolves.
     *
     * The horizontal next component resolves first and the hooked one follows
     * it by a tick, so a secondary activation can never retroactively affect
     * the primary. That ordering is the whole reason Tripwire is parallel
     * execution rather than a branch.
     */
    public static final int HOOK_LAG = 1;

    private CircuitReader() {}

    /** Fewest unlocked slots at which any storage cell exists at all. */
    public static final int STORAGE_BEGINS = 9;

    /**
     * Where flow starts, which is the only thing capacity changes about it.
     *
     * Below {@link #STORAGE_BEGINS} there is no storage to begin in, so the
     * hotbar is the whole board rather than the magazine. At every other
     * capacity flow starts at the top-left as usual and stops at the first
     * locked cell, which is what keeps a partial row A from leaping the locked
     * remainder of the board to continue in the hotbar.
     */
    static int[] flowFor(int unlockedSlots) {
        if (unlockedSlots > STORAGE_BEGINS) return FLOW;
        int[] hotbarOnly = new int[9];
        System.arraycopy(FLOW, 27, hotbarOnly, 0, 9);
        return hotbarOnly;
    }

    /**
     * The slot directly beneath a given one <i>on screen</i>, or -1 at the bottom.
     *
     * Row C sits directly above the hotbar, so 27+k is above hotbar slot k --
     * a wrap in Bukkit's index space that is plain adjacency on screen. Again
     * the screen is the authority.
     */
    public static int below(int slot) {
        if (slot >= 9 && slot <= 26) return slot + 9;   // A -> B, B -> C
        if (slot >= 27 && slot <= 35) return slot - 27; // C -> hotbar
        return -1;                                       // hotbar has nothing beneath
    }

    /** One resolution: a component, where it sits, and the tick it fires on. */
    public record Step(int slot, CircuitComponent component, int tick, boolean hooked) {
        public Step {
            if (tick < 0) throw new IllegalArgumentException("A step cannot fire before the circuit starts.");
            java.util.Objects.requireNonNull(component, "A step must name its component.");
        }
    }

    /**
     * A read circuit.
     *
     * @param steps      every resolution in firing order, hooked ones included
     * @param wiring     cells traversed as wiring -- items spent, no time
     * @param magazine   the first non-program slot, or -1 if the board is full
     * @param components how many intervals the circuit costs
     */
    public record Circuit(List<Step> steps, List<Integer> wiring, int magazine, int components) {
        public Circuit {
            steps = List.copyOf(steps);
            wiring = List.copyOf(wiring);
        }
        public boolean isEmpty() { return steps.isEmpty(); }
        /** The tick the last thing resolves on; 0 for an empty circuit. */
        public int duration() {
            return steps.stream().mapToInt(Step::tick).max().orElse(0);
        }
    }

    /**
     * Read the program out of an inventory.
     *
     * @param contents      indexed by Bukkit slot, as {@code Inventory.getContents()} gives it
     * @param unlockedSlots the player's Capacity; slots at or above it are locked
     */
    public static Circuit read(Material[] contents, int unlockedSlots) {
        var steps = new ArrayList<Step>();
        var wiring = new ArrayList<Integer>();
        int magazine = -1;
        int tick = 0;
        int components = 0;
        boolean first = true;
        boolean repeaterPending = false;
        // Dust is only wiring once something has been emitted to wire FROM.
        // Before that it is an instruction, which is why a leading Dust
        // discharges rather than meaning "skip me".
        boolean anyComponent = false;

        for (int slot : flowFor(unlockedSlots)) {
            if (slot >= unlockedSlots) { magazine = slot; break; }
            Material material = slot < contents.length ? contents[slot] : null;
            CircuitComponent component = CircuitComponent.of(material);
            if (component == null) { magazine = slot; break; }

            if (component.isWiring() && anyComponent) {
                // Wiring costs an item and no time. Whether it is the LAST
                // wiring before the program ends -- which would make it an
                // instruction rather than wiring -- is settled after the walk,
                // because it cannot be known here.
                wiring.add(slot);
                continue;
            }

            if (first) { first = false; }
            else {
                tick += INTERVAL;
                if (repeaterPending) tick += INTERVAL;
            }
            repeaterPending = component == CircuitComponent.REPEATER;
            anyComponent = true;
            components++;
            steps.add(new Step(slot, component, tick, false));

            if (component == CircuitComponent.TRIPWIRE_HOOK) {
                hook(steps, contents, unlockedSlots, slot, tick);
            }
        }

        trailingWiringIsAnInstruction(steps, wiring, tick);
        steps.sort(java.util.Comparator.comparingInt(Step::tick));
        return new Circuit(steps, wiring, magazine, components);
    }

    /**
     * Fire the one slot directly beneath a hook.
     *
     * Exactly one cell: no downward search, no vertical wrap, no fork. A
     * vertically activated Repeater or Dust does not start a second circuit --
     * otherwise every Tripwire becomes an arbitrary branch and geometry stops
     * meaning anything.
     *
     * The hook simply does nothing when the cell beneath is locked, empty or
     * off the board. That is not a failure to report: a hook over nothing is a
     * legitimate, if wasteful, arrangement, and at 18 slots it is the ONLY
     * arrangement available because rows A and B are not yet vertically
     * adjacent.
     */
    private static void hook(List<Step> steps, Material[] contents, int unlockedSlots,
                             int slot, int tick) {
        int target = below(slot);
        if (target < 0 || target >= unlockedSlots || target >= contents.length) return;
        CircuitComponent beneath = CircuitComponent.of(contents[target]);
        if (beneath == null || beneath.isWiring()) return;
        steps.add(new Step(target, beneath, tick + INTERVAL + HOOK_LAG, true));
    }

    /**
     * Dust that nothing follows was never wiring.
     *
     * "Wiring" means sitting BETWEEN two components. A run of Dust at the tail
     * of a program has a component before it and nothing after, so it is an
     * instruction -- the defensive discharge -- and this is the only place that
     * can be known, because it depends on the program ending.
     *
     * Only the first such Dust discharges. The rest are ordinary items in a
     * cell, since a discharge consumes the stack it resolves, not every stack
     * behind it.
     */
    private static void trailingWiringIsAnInstruction(List<Step> steps, List<Integer> wiring, int tick) {
        if (wiring.isEmpty() || steps.isEmpty()) return;
        int lastComponentSlot = steps.getLast().slot();
        int lastWiringSlot = wiring.getLast();
        if (order(lastWiringSlot) < order(lastComponentSlot)) return;  // a component follows it
        wiring.removeLast();
        steps.add(new Step(lastWiringSlot, CircuitComponent.DUST, tick + INTERVAL, false));
    }

    /** A slot's position in flow order, for comparing "which comes first". */
    public static int order(int slot) {
        for (int i = 0; i < FLOW.length; i++) if (FLOW[i] == slot) return i;
        return -1;
    }
}
