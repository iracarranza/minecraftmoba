package com.minecraftmoba.plugin;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * A1 Reconfiguratron: swap the first components of the circuit for the last
 * components in the inventory.
 *
 * Pure index arithmetic over flow order, so the rule can be settled without a
 * server and without an inventory.
 *
 * <h2>Why this is A2's loadout editor</h2>
 *
 * A1 swaps the first K components; A2 fires the first two. So A1 does not
 * reconfigure the circuit in general -- it rewrites the part A2 uses, which is
 * the answer to "why reconfigure mid-fight". The code should not need to know
 * that, but the choice of "first" over "any" is the whole design and is worth
 * not losing.
 *
 * <h2>The pairing, which classes.md does not specify</h2>
 *
 * "The first two for the last two" fixes the two groups and not which member
 * of one meets which member of the other. This pairs them <b>outermost
 * first</b>: the first component swaps with the last, the second with the
 * second-to-last.
 *
 * That choice is not arbitrary. It is the only pairing under which the groups
 * overlapping behaves sensibly. With three components and K = 2 the groups are
 * {c0, c1} and {c1, c2}; pairing outermost-first gives c0 with c2 and leaves
 * c1 matched with itself, so the middle component is <b>unchanged</b> -- which
 * is exactly the state branch III (Spare Parts) pays for. Pairing in the same
 * order would ask c1 to swap with both c0 and c2 at once, which has no meaning.
 *
 * A short circuit therefore degrades into healing rather than into an error,
 * and does so by the arithmetic rather than by a special case.
 */
public final class Reconfiguration {

    /**
     * One exchange, by inventory slot.
     *
     * Only real exchanges are planned. A component matched with itself is not
     * a swap that does nothing -- it is a component the reconfiguration left
     * alone, which is a different fact and is counted as such.
     */
    public record Swap(int from, int to) {}

    /** What a reconfiguration would do: the swaps, and how many components it leaves alone. */
    public record Plan(List<Swap> swaps, int unchanged) {
        public boolean isEmpty() { return swaps.isEmpty(); }
        public int moved() { return swaps.size() * 2; }
    }

    /**
     * Plan a reconfiguration over the component slots, in flow order.
     *
     * @param componentSlots slots holding components, in the order the circuit runs them
     * @param depth          how many from each end: two at base, three under branch II
     */
    public static Plan plan(List<Integer> componentSlots, int depth) {
        var swaps = new ArrayList<Swap>();
        var seen = new LinkedHashSet<Long>();
        int n = componentSlots.size();
        if (n == 0 || depth <= 0) return new Plan(List.of(), n);

        int k = Math.min(depth, n);
        Set<Integer> touched = new LinkedHashSet<>();

        for (int i = 0; i < k; i++) {
            int from = componentSlots.get(i);
            int to = componentSlots.get(n - 1 - i);
            // Each unordered pair once. With exactly 2K components and K = n,
            // the two groups are the same set read from both ends, so every
            // pair would otherwise be planned twice and cancel itself out.
            if (from == to) continue;                      // matched with itself: unchanged
            long key = Math.min(from, to) * 100L + Math.max(from, to);
            if (!seen.add(key)) continue;
            swaps.add(new Swap(from, to));
            touched.add(from); touched.add(to);
        }
        return new Plan(List.copyOf(swaps), n - touched.size());
    }

    /**
     * Apply a plan to a slot array.
     *
     * Takes the array rather than an inventory so the rule is testable; the
     * caller is responsible for writing the result back.
     */
    public static <T> void apply(T[] contents, Plan plan) {
        for (Swap s : plan.swaps()) {
            T held = contents[s.from()];
            contents[s.from()] = contents[s.to()];
            contents[s.to()] = held;
        }
    }

    private Reconfiguration() {}
}
