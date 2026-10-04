package com.minecraftmoba.plugin;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Construction costed against the thing it actually competes with: the rest of
 * what the team could be doing.
 *
 * **Every number below is a claim nobody has measured.** They are deliberately
 * round, and the point of the exercise is the SHAPE of the answer and which
 * inputs it is sensitive to -- not the figures.
 */
class LabourBudgetTest {

    /** 20 WP per construction placement; from config.yml. */
    private static final int CONSTRUCTION_WP = 20;

    /**
     * Placing a construction block: the attended cost is gathering the raw
     * material, collecting the output, ferrying it and placing it. The ten
     * seconds in the furnace is NOT here, because the player is elsewhere.
     */
    private static final LabourBudget.Activity GLASS =
            new LabourBudget.Activity("glass placement", 4.0, ConstructionEconomy.SMELT_SECONDS, 4, CONSTRUCTION_WP);

    /** Bricks: the same attended work, but four smelts of capital per block. */
    private static final LabourBudget.Activity BRICKS =
            new LabourBudget.Activity("bricks placement", 4.0, ConstructionEconomy.SMELT_SECONDS * 4, 4, CONSTRUCTION_WP);

    /** Concrete: no machine at all, so attention is the only cost. */
    private static final LabourBudget.Activity CONCRETE =
            LabourBudget.Activity.attended("concrete placement", 3.5, CONSTRUCTION_WP);

    /** One iron ore: A(O) 20 + qH 10, from config.yml. Roughly six seconds of attention. */
    private static final LabourBudget.Activity IRON =
            LabourBudget.Activity.attended("iron extraction", 6.0, 30);

    /** Ordinary placement, the atomic Work Point. Fast, and worth almost nothing. */
    private static final LabourBudget.Activity ORDINARY =
            LabourBudget.Activity.attended("ordinary placement", 2.0, 1);

    @Test void attentionIsTheCurrencyAndEveryActivityPricesInIt() {
        assertEquals(300.0, LabourBudget.workPerLabourMinute(GLASS), 1e-9);
        assertEquals(300.0, LabourBudget.workPerLabourMinute(IRON), 1e-9);
        assertEquals(30.0, LabourBudget.workPerLabourMinute(ORDINARY), 1e-9);
    }

    @Test void theFurnaceCostsCapitalButNotAttention() {
        // Glass and bricks differ by a factor of four in machine time and not
        // at all in attention, so they are worth the SAME to a team's minute.
        // That is the modelling claim worth checking: waiting is free when the
        // player is elsewhere.
        assertEquals(LabourBudget.workPerLabourMinute(GLASS),
                     LabourBudget.workPerLabourMinute(BRICKS), 1e-9);
        assertTrue(LabourBudget.capitalFor(BRICKS, 12) > LabourBudget.capitalFor(GLASS, 12));
    }

    @Test void capitalBindsBeforeAttentionDoesWhenFurnacesAreFew() {
        // Four furnaces of bricks: the machines stop the team long before its
        // attention does. The fix is more furnaces, which cost almost nothing.
        var limit = LabourBudget.sustainable(BRICKS, 2);
        assertEquals(LabourBudget.Binding.CAPITAL, limit.binding());
        assertEquals(6.0, limit.unitsPerMinute(), 1e-9);

        // Enough furnaces and the same two players are attention-bound instead.
        var many = new LabourBudget.Activity("bricks, well equipped", 4.0,
                ConstructionEconomy.SMELT_SECONDS * 4, 40, CONSTRUCTION_WP);
        assertEquals(LabourBudget.Binding.ATTENTION, LabourBudget.sustainable(many, 2).binding());
        assertEquals(30.0, LabourBudget.sustainable(many, 2).unitsPerMinute(), 1e-9);
    }

    @Test void concreteNeverWaitsOnCapitalAtAnyRate() {
        assertEquals(0.0, LabourBudget.capitalFor(CONCRETE, 1000), 1e-9);
        assertEquals(LabourBudget.Binding.ATTENTION,
                LabourBudget.sustainable(CONCRETE, 1).binding());
    }

    @Test void twelveBlocksAMinuteIsLessThanOnePlayerOfAttention() {
        // The figure the first model invented, priced in the currency that
        // actually matters. It is not a lot of ATTENTION -- it is 0.8 of a
        // player -- which is why the furnace count was the wrong worry.
        assertEquals(0.8, LabourBudget.playersFor(GLASS, 12), 1e-9);
        assertEquals(0.7, LabourBudget.playersFor(CONCRETE, 12), 1e-9);
        // But it is eight furnaces on the bricks route, which is the real ask.
        assertEquals(8.0, LabourBudget.capitalFor(BRICKS, 12), 1e-9);
    }

    @Test void aTeamOfSevenCanSustainAGreatDealIfItChoosesTo() {
        // Two of seven players, fully committed, on concrete.
        var limit = LabourBudget.sustainable(CONCRETE, 2);
        assertTrue(limit.unitsPerMinute() > 30,
                "two waysided players make over thirty blocks a minute: " + limit.unitsPerMinute());
    }

    // ---- the decision the team is actually making --------------------------

    @Test void movingAPlayerFromExtractionToConstructionIsNearlyFree() {
        // At these claims the two activities are worth the same per minute of
        // attention, so the shift costs no Work Points. The team is choosing
        // WHAT it builds up, not whether to progress.
        var move = LabourBudget.shift(IRON, GLASS, 1);
        assertEquals(0.0, move.net(), 1e-9);
        assertFalse(move.worthwhile(), "equal return is not an improvement");
    }

    @Test void concreteIsAPayCutAndTheModelSaysSoRatherThanHidingIt() {
        // Concrete is faster per block but the same award, so at 3.5s against
        // iron's 6s it is actually BETTER per minute. The sign of this is the
        // kind of thing the instrument exists to surface.
        var move = LabourBudget.shift(IRON, CONCRETE, 1);
        assertTrue(move.worthwhile(), "net " + move.net());
        assertEquals(342.857, LabourBudget.workPerLabourMinute(CONCRETE), 1e-3);
    }

    @Test void mundaneWorkIsInsufficientRatherThanWorthless() {
        // config.yml's stated intent for the 1 WP placement, checked. It earns
        // a tenth of real work per minute of attention -- a real but poor
        // return, which is the claim the comment makes.
        var move = LabourBudget.shift(ORDINARY, IRON, 1);
        assertTrue(move.worthwhile());
        assertEquals(10.0, LabourBudget.workPerLabourMinute(IRON)
                         / LabourBudget.workPerLabourMinute(ORDINARY), 1e-9);
    }

    @Test void activitiesRankByWhatAMinuteOfAttentionEarns() {
        var ranked = LabourBudget.byReturn(List.of(ORDINARY, IRON, CONCRETE, GLASS));
        assertEquals("concrete placement", ranked.get(0).name(), "best return per attended minute");
        assertEquals("ordinary placement", ranked.get(ranked.size() - 1).name());
    }

    @Test void anActivityCostingNoAttentionIsNotAnInfiniteFreeLunch() {
        // Guard on the model rather than the game: a zero attended cost means
        // "capital only", and sustainable() must fall through to capital
        // instead of reporting an unbounded rate.
        var passive = new LabourBudget.Activity("passive", 0, 10, 2, 5);
        var limit = LabourBudget.sustainable(passive, 1);
        assertEquals(LabourBudget.Binding.CAPITAL, limit.binding());
        assertEquals(12.0, limit.unitsPerMinute(), 1e-9);
    }
}
