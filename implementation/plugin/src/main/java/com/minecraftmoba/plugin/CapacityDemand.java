package com.minecraftmoba.plugin;

import java.util.ArrayList;
import java.util.List;

/**
 * Whether a team can keep up with its own progression.
 *
 * <h2>A capacity grant is an obligation, not only a ceiling</h2>
 *
 * The earlier models treated capacity as pure upside: more slots, more
 * footprint, more Constructs. But raised capacity is only worth what is put
 * into it, and putting something into it costs attention. <b>Every Growth step
 * that widens what a player may build also widens the attention bill for
 * operating at that width.</b>
 *
 * At low levels this is comfortable, and that comfort is itself a consequence:
 * few slots and a small permitted footprint mean the commitment required to sit
 * at the ceiling is small, so a team easily operates at full capacity. Each
 * grant then raises the bar. If grants arrive faster than the team can fill
 * them, <b>utilisation falls as the player levels</b> -- they are granted more
 * and use proportionally less, while the progression system reports success.
 *
 * <h2>This is not about inventory</h2>
 *
 * The same shape applies to every infrastructure type in
 * {@code infrastructure.md}: a Construct says these authored blocks are one
 * useful place; a Development Zone says these renewable resources are one
 * productive place; a Route says these two places are traversable; a Supply
 * Line says resources can move between these nodes. Each has a capacity that
 * grows with progression and each needs attention to fill. The unit differs and
 * the arithmetic does not.
 *
 * <h2>The question it answers</h2>
 *
 * Not "how big can this get" but <b>"does the backlog clear before the next
 * grant arrives"</b>. A grant that cannot be filled before it is superseded
 * means the player is permanently behind their own capacity, and every later
 * grant is decoration.
 */
public final class CapacityDemand {

    /** One Growth step's widening, in whatever unit the infrastructure counts. */
    public record Grant(int level, double before, double after) {
        public double increase() { return Math.max(0, after - before); }
    }

    /**
     * What a grant costs and whether it lands before the next one.
     *
     * @param minutesToFill       attention-minutes, at the allocated staffing
     * @param minutesUntilNext    how long until the capacity widens again
     * @param backlogAfter        unfilled units still owed when the next grant lands
     */
    public record Verdict(int level, double increase, double minutesToFill,
                          double minutesUntilNext, double backlogAfter) {
        public boolean keepsUp() { return minutesToFill <= minutesUntilNext; }
        public double utilisation() {
            double owed = increase() + backlogAfter();
            return owed <= 0 ? 1.0 : Math.min(1.0, increase() / owed);
        }
    }

    /** Read a stepped capacity curve into the grants it represents. */
    public static List<Grant> grants(List<Integer> growthLevels, List<Double> steps) {
        var out = new ArrayList<Grant>();
        for (int i = 1; i < steps.size() && i - 1 < growthLevels.size(); i++) {
            double before = steps.get(i - 1), after = steps.get(i);
            if (after > before) out.add(new Grant(growthLevels.get(i - 1), before, after));
        }
        return out;
    }

    /**
     * Walk the grants, carrying any unfilled backlog forward.
     *
     * Carried rather than reset, because that is what actually happens: a team
     * that could not fill the last grant does not start the next one even. The
     * difference between carrying and resetting is the difference between
     * noticing a compounding shortfall and reporting each grant as a fresh,
     * survivable problem.
     *
     * @param labourMinutesPerUnit attention to establish one unit of capacity
     * @param players              attention allocated to this infrastructure
     * @param minutesBetweenGrants wall-clock between Growth steps
     */
    public static List<Verdict> audit(List<Grant> grants, double labourMinutesPerUnit,
                                      double players, double minutesBetweenGrants) {
        var out = new ArrayList<Verdict>();
        double backlog = 0;
        double fillPerMinute = players <= 0 || labourMinutesPerUnit <= 0
                ? 0 : players / labourMinutesPerUnit;

        for (int i = 0; i < grants.size(); i++) {
            Grant g = grants.get(i);
            double owed = backlog + g.increase();
            double minutesToFill = fillPerMinute <= 0 ? Double.POSITIVE_INFINITY : owed / fillPerMinute;

            // The DEADLINE and the DRAIN WINDOW are different quantities, and
            // conflating them erased the finding. The last grant has no
            // successor pressuring it, so its deadline is unbounded -- but the
            // team still only gets one Growth interval of attention in the
            // period being measured, so the window it actually drains in is
            // the same as every other step's. Giving the last grant an
            // infinite window let it clear an entire match's accumulated
            // backlog for free, and reported a team that never kept up as
            // finishing fully utilised.
            double deadline = i < grants.size() - 1 ? minutesBetweenGrants : Double.POSITIVE_INFINITY;
            double filled = fillPerMinute * Math.min(minutesToFill, minutesBetweenGrants);
            backlog = Math.max(0, owed - filled);
            out.add(new Verdict(g.level(), g.increase(), minutesToFill, deadline, backlog));
        }
        return out;
    }

    /**
     * Attention needed to stay level with the grants, rather than fall behind.
     *
     * The number a team would want before committing to an infrastructure
     * strategy: not "can we build this" but "can we keep building it as fast as
     * we are allowed to".
     */
    public static double playersToKeepUp(List<Grant> grants, double labourMinutesPerUnit,
                                         double minutesBetweenGrants) {
        double worst = 0;
        for (Grant g : grants)
            worst = Math.max(worst, g.increase() * labourMinutesPerUnit / minutesBetweenGrants);
        return worst;
    }

    /**
     * The most attention one unit may cost before this curve outruns its
     * staffing.
     *
     * <b>This is the output that survives not having measured anything.</b>
     * Absolute minutes depend entirely on a labour cost nobody has measured,
     * so quoting them dresses a guess as a finding. A break-even inverts the
     * unknown instead: it says "this class keeps up if and only if
     * establishing one unit costs less than X attention-minutes", which is a
     * claim the scenario harness could go and check.
     *
     * Set by the WORST step rather than the average, because each grant has
     * its own deadline and a curve that is gentle except for one jump is as
     * demanding as its jump.
     */
    public static double breakEvenLabourPerUnit(List<Grant> grants, double players,
                                                double minutesBetweenGrants) {
        double worstIncrease = 0;
        for (Grant g : grants) worstIncrease = Math.max(worstIncrease, g.increase());
        if (worstIncrease <= 0) return Double.POSITIVE_INFINITY;
        return players * minutesBetweenGrants / worstIncrease;
    }

    /** The grant that sets the staffing, which is the one worth designing around. */
    public static Grant bindingGrant(List<Grant> grants) {
        Grant worst = null;
        for (Grant g : grants)
            if (worst == null || g.increase() > worst.increase()) worst = g;
        return worst;
    }

    /**
     * The attention-weighted average level at which a curve's demand arrives.
     *
     * A centroid rather than a first-half share. Splitting the grants in two by
     * INDEX ignores when they land: the default curve and Toolbox's both have
     * six grants, so both scored exactly 0.5 and the metric said nothing. The
     * difference between them is entirely in the levels -- Toolbox finishes
     * asking at 18, the default curve is still asking at 24 -- so weighting by
     * level is the measurement, and splitting by count was a proxy for it that
     * happened to be constant.
     *
     * A low centroid asks for commitment when a team has least to give. A high
     * one asks when the match may already be decided. Neither is wrong; they
     * are different strategic objects, and the curve says which without any
     * timing claim.
     */
    public static double demandCentroidLevel(List<Grant> grants) {
        double weighted = 0, total = 0;
        for (Grant g : grants) { weighted += g.increase() * g.level(); total += g.increase(); }
        return total <= 0 ? 0 : weighted / total;
    }

    private CapacityDemand() {}
}
