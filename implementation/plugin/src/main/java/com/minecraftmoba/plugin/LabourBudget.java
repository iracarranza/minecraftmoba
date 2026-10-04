package com.minecraftmoba.plugin;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * What a team's attention buys, and what spending it somewhere costs
 * everywhere else.
 *
 * <h2>The scarce resource is attention, not furnaces</h2>
 *
 * A team of seven has seven player-minutes per minute and no more. Every
 * minute spent gathering sand, feeding a furnace or ferrying a load is a
 * minute not spent extracting, developing, contesting an objective or
 * fighting. Modelling construction as "blocks per minute" hides that; modelling
 * it as a claim on a finite budget does not.
 *
 * <h2>Capital and labour scale differently, which is the whole point</h2>
 *
 * A furnace runs <b>unattended and in parallel</b>. Twenty furnaces cost no
 * more attention than one once they are lit, so smelting throughput is bought
 * with capital and fuel rather than with time. Gathering, ferrying and placing
 * are <b>attended and serial</b>: they consume a player for their duration and
 * cannot be parallelised except by assigning another player.
 *
 * So a team scales construction by building more furnaces -- cheap, and almost
 * free in attention -- until the attended stages bind. After that the only way
 * forward is to wayside something else.
 *
 * <h2>The comparison that matters</h2>
 *
 * {@link #workPerLabourMinute} puts every activity in the same currency: Work
 * Points earned per minute of player attention spent. That is the number a team
 * is implicitly comparing when it decides whether to build, and it is the
 * number that says whether a rational team would ever choose to.
 *
 * **This is a design instrument, not a test.** Its inputs are claims about how
 * long things take, and nobody has measured them.
 */
public final class LabourBudget {

    /**
     * One kind of work, costed in the two resources it actually consumes.
     *
     * @param labourSecondsPerUnit     attended time: a player is occupied for this long
     * @param unattendedSecondsPerUnit elapsed time in a parallel machine, costing no attention
     * @param parallelCapital          how many such machines are running
     * @param workPerUnit              Work Points the unit awards
     */
    public record Activity(String name,
                           double labourSecondsPerUnit,
                           double unattendedSecondsPerUnit,
                           double parallelCapital,
                           double workPerUnit) {

        /** Purely attended work -- mining, placing, walking. */
        public static Activity attended(String name, double labourSeconds, double work) {
            return new Activity(name, labourSeconds, 0, 0, work);
        }
    }

    /** What limits an activity at a given staffing, and the rate that implies. */
    public enum Binding { ATTENTION, CAPITAL }
    public record Limit(Binding binding, double unitsPerMinute, double attention, double capital) {}

    /** Units a given number of players can sustain, and which stage stops them. */
    public static Limit sustainable(Activity a, double players) {
        double attention = a.labourSecondsPerUnit() <= 0
                ? Double.MAX_VALUE
                : players * 60.0 / a.labourSecondsPerUnit();
        double capital = a.unattendedSecondsPerUnit() <= 0
                ? Double.MAX_VALUE
                : a.parallelCapital() * 60.0 / a.unattendedSecondsPerUnit();
        return attention <= capital
                ? new Limit(Binding.ATTENTION, attention, attention, capital)
                : new Limit(Binding.CAPITAL, capital, attention, capital);
    }

    /**
     * Work Points per minute of player attention.
     *
     * Deliberately ignores capital. Two activities with the same attention cost
     * are worth the same to a team's time budget even if one waits on a
     * machine, because the waiting is free -- the player is elsewhere.
     */
    public static double workPerLabourMinute(Activity a) {
        if (a.labourSecondsPerUnit() <= 0) return Double.MAX_VALUE;
        return a.workPerUnit() * 60.0 / a.labourSecondsPerUnit();
    }

    /** Players that must be waysided to sustain a rate, ignoring capital. */
    public static double playersFor(Activity a, double unitsPerMinute) {
        return unitsPerMinute * a.labourSecondsPerUnit() / 60.0;
    }

    /** Capital units needed to keep up, so capital never becomes the excuse. */
    public static double capitalFor(Activity a, double unitsPerMinute) {
        if (a.unattendedSecondsPerUnit() <= 0) return 0;
        return unitsPerMinute * a.unattendedSecondsPerUnit() / 60.0;
    }

    /**
     * What a team gives up by moving attention onto {@code onto}.
     *
     * The honest form of "waysiding other tasks": the same minutes, valued at
     * what they would have earned doing the thing they were already doing.
     */
    public record Tradeoff(String from, String to, double playersMoved,
                           double workGained, double workLost) {
        public double net() { return workGained - workLost; }
        public boolean worthwhile() { return net() > 0; }
    }

    public static Tradeoff shift(Activity from, Activity to, double players) {
        return new Tradeoff(from.name(), to.name(), players,
                players * workPerLabourMinute(to),
                players * workPerLabourMinute(from));
    }

    /** Activities ranked by what a minute of attention earns, best first. */
    public static List<Activity> byReturn(List<Activity> activities) {
        var out = new ArrayList<>(activities);
        out.sort(Comparator.comparingDouble(LabourBudget::workPerLabourMinute).reversed());
        return out;
    }

    private LabourBudget() {}
}
