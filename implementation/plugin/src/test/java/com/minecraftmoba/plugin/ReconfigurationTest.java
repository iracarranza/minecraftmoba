package com.minecraftmoba.plugin;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * A1 Reconfiguratron's exchange rule.
 *
 * classes.md fixes the two groups -- first K components, last K components --
 * and not which member of one meets which member of the other. These pin the
 * pairing, and in particular pin the overlap cases, which are where branch III
 * (Spare Parts) gets its meaning.
 */
class ReconfigurationTest {

    private static List<Integer> slots(int... s) {
        var out = new java.util.ArrayList<Integer>();
        for (int x : s) out.add(x);
        return out;
    }

    @Test void disjointGroupsSwapOutermostFirst() {
        // Six components, depth two: c0 with c5, c1 with c4. The inner pair is
        // untouched, which is what makes "first two for last two" a swap
        // rather than a rotation.
        var plan = Reconfiguration.plan(slots(10, 11, 12, 13, 14, 15), 2);
        assertEquals(List.of(new Reconfiguration.Swap(10, 15),
                             new Reconfiguration.Swap(11, 14)), plan.swaps());
        assertEquals(2, plan.unchanged(), "the middle two are not involved");
        assertEquals(4, plan.moved());
    }

    @Test void theMiddleComponentOfThreeIsLeftAloneRatherThanAskedToBeInTwoPlaces() {
        // The case that decides the pairing. Groups are {c0,c1} and {c1,c2};
        // outermost-first gives c0 with c2 and leaves c1 matched with itself.
        // Same-order pairing would ask c1 to swap with both c0 and c2 at once.
        var plan = Reconfiguration.plan(slots(10, 11, 12), 2);
        assertEquals(1, plan.swaps().size());
        assertEquals(new Reconfiguration.Swap(10, 12), plan.swaps().get(0));
        assertEquals(1, plan.unchanged(), "the middle component is Spare Parts' payout");
    }

    @Test void twoComponentsSwapOnceRatherThanTwiceAndCancelling() {
        // Both groups are the same set read from each end, so planning every
        // pair would produce (c0,c1) and (c1,c0) -- applied in sequence that
        // is the identity, and the ability would silently do nothing.
        var plan = Reconfiguration.plan(slots(10, 11), 2);
        assertEquals(1, plan.swaps().size());
        assertEquals(0, plan.unchanged(), "both moved");

        var board = new String[]{"PISTON", "DUST"};
        Reconfiguration.apply(board, Reconfiguration.plan(slots(0, 1), 2));
        assertArrayEquals(new String[]{"DUST", "PISTON"}, board, "exchanged, not restored");
    }

    @Test void aLoneComponentIsEntirelyUnchanged() {
        var plan = Reconfiguration.plan(slots(10), 2);
        assertTrue(plan.isEmpty(), "a component matched with itself is not an exchange");
        assertEquals(1, plan.unchanged(), "a short circuit heals rather than erroring");
        assertEquals(0, plan.moved());
    }

    @Test void anEmptyCircuitPlansNothing() {
        var plan = Reconfiguration.plan(List.of(), 2);
        assertTrue(plan.isEmpty());
        assertEquals(0, plan.unchanged());
    }

    @Test void moreConfigReachesDeeperWithoutChangingTheRule() {
        // Branch II: three from each end instead of two.
        var plan = Reconfiguration.plan(slots(10, 11, 12, 13, 14, 15), 3);
        assertEquals(List.of(new Reconfiguration.Swap(10, 15),
                             new Reconfiguration.Swap(11, 14),
                             new Reconfiguration.Swap(12, 13)), plan.swaps());
        assertEquals(0, plan.unchanged(), "six components, three swaps, nothing left over");
    }

    @Test void depthBeyondTheCircuitIsClampedRatherThanRefused() {
        // A deep swap on a small circuit is a legitimate state, not an error:
        // it simply overlaps more and therefore heals more.
        // The ends meet in the middle and stop, so a deeper swap on an odd
        // circuit CONVERGES rather than reaching further: three components
        // have exactly one exchange at any depth, and the middle is always
        // the one left alone.
        var deep = Reconfiguration.plan(slots(10, 11, 12), 5);
        var shallow = Reconfiguration.plan(slots(10, 11, 12), 2);
        assertEquals(shallow.swaps(), deep.swaps(), "depth past the circuit adds nothing");
        assertEquals(1, deep.swaps().size());
        assertEquals(1, deep.unchanged());
    }

    @Test void applyPerformsExactlyThePlannedExchanges() {
        var board = new String[]{"A", "B", "C", "D"};
        Reconfiguration.apply(board, Reconfiguration.plan(slots(0, 1, 2, 3), 2));
        assertArrayEquals(new String[]{"D", "C", "B", "A"}, board);
    }

    @Test void applyIsAnInvolutionSoTwiceIsIdentity() {
        // Swapping is its own inverse, which means a player can undo a
        // reconfiguration by spending another one -- a real tactical option
        // and worth being a property rather than an accident.
        var board = new String[]{"A", "B", "C", "D", "E"};
        var plan = Reconfiguration.plan(slots(0, 1, 2, 3, 4), 2);
        Reconfiguration.apply(board, plan);
        Reconfiguration.apply(board, plan);
        assertArrayEquals(new String[]{"A", "B", "C", "D", "E"}, board);
    }

    @Test void aNonPositiveDepthChangesNothingAndHealsEverything() {
        var plan = Reconfiguration.plan(slots(10, 11, 12), 0);
        assertTrue(plan.isEmpty());
        assertEquals(3, plan.unchanged());
    }
}
