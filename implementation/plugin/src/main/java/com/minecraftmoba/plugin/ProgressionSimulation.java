package com.minecraftmoba.plugin;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * What a level curve costs in minutes, for a player who works a particular way.
 *
 * <h2>This is a design instrument, not a test</h2>
 *
 * Stated here because the distinction is easy to lose once the output looks
 * precise. A simulation's value depends on its model being right, and the model
 * is largely the thing under examination, so this is reliable for <b>internal
 * inconsistency</b> -- a level nobody can reach, a band that costs less than
 * the one before it for every policy, a phase that passes in seconds -- and
 * silent about whether any of it is any fun.
 *
 * Nothing here should ever be cited as evidence of balance. See
 * {@code docs/design/SCENARIO_HARNESS.md}.
 *
 * <h2>Deterministic on purpose</h2>
 *
 * A player who mines at a steady rate is not realistic, and introducing
 * variance would not make it more so -- it would add a distribution nobody has
 * measured on top of rates nobody has measured. A flat rate makes the output
 * exactly as trustworthy as its inputs, and visibly so.
 */
public final class ProgressionSimulation {

    /**
     * A way of playing, as actions per minute.
     *
     * Keys are economy action names; the rate is how many of that action the
     * policy performs in a minute. A policy is a claim about behaviour, and a
     * deliberately crude one.
     */
    public record Policy(String name, Map<String, Double> actionsPerMinute) {
        public double workPerMinute(Map<String, Integer> awards) {
            double wp = 0;
            for (var e : actionsPerMinute().entrySet())
                wp += e.getValue() * awards.getOrDefault(e.getKey(), 0);
            return wp;
        }
        /** This policy as a level-independent {@link Rate}. */
        public Rate asRate(Map<String, Integer> awards) {
            double flat = workPerMinute(awards);
            return new Rate() {
                @Override public double workPerMinute(int level) { return flat; }
                @Override public String name() { return name(); }
            };
        }
    }

    /**
     * A Construction player, limited by what the chain can supply and what they
     * can carry, rather than by how fast they can click.
     *
     * @param slotsAtLevel the Capacity curve, which is why this varies by level
     */
    public static Rate construction(String name, ConstructionEconomy economy,
                                    java.util.function.IntUnaryOperator slotsAtLevel,
                                    int wpPerPlacement) {
        return new Rate() {
            @Override public double workPerMinute(int level) {
                return economy.limit(slotsAtLevel.applyAsInt(level)).blocksPerMinute() * wpPerPlacement;
            }
            @Override public String name() { return name; }
        };
    }

    /**
     * A work rate that may depend on the level it is earned at.
     *
     * Added because a flat rate cannot express the Construction case at all.
     * Carry capacity is a Growth stat, so a builder's ceiling RISES as they
     * level, which makes construction a compounding loop rather than a
     * constant -- and a constant would have answered a question nobody asked.
     */
    public interface Rate {
        double workPerMinute(int level);
        String name();
    }

    /** When a policy arrives at each level, and how long each one took. */
    public record Arrival(int level, double minutesTotal, double minutesForThisLevel) {}

    private final ProgressionCurve curve;
    private final Map<String, Integer> awards;

    public ProgressionSimulation(ProgressionCurve curve, Map<String, Integer> awards) {
        this.curve = curve;
        this.awards = Map.copyOf(awards);
    }

    /**
     * Walk a policy up the curve.
     *
     * A policy earning nothing returns an empty list rather than an infinity or
     * a loop. "This player never levels" is a real and interesting answer, and
     * it should be returned rather than hung on.
     */
    public List<Arrival> run(Policy policy) { return run(policy.asRate(awards)); }

    /**
     * Walk a rate up the curve, re-reading it at every level.
     *
     * Re-read rather than sampled once, because a rate that rises with level is
     * the whole point of the Construction case: sampling at level 1 would
     * understate the cap by the entire Capacity curve.
     */
    public List<Arrival> run(Rate rate) {
        var out = new ArrayList<Arrival>();
        double elapsed = 0;
        for (int level = 1; level < curve.maxLevel(); level++) {
            double perMinute = rate.workPerMinute(level);
            if (perMinute <= 0) return List.of();
            double minutes = curve.costToLeave(level) / perMinute;
            elapsed += minutes;
            out.add(new Arrival(level + 1, elapsed, minutes));
        }
        return out;
    }

    /** Minutes for a policy to reach the cap, or -1 if it never does. */
    public double minutesToCap(Policy policy) { return minutesToCap(policy.asRate(awards)); }

    /** Minutes for a rate to reach the cap, or -1 if it never does. */
    public double minutesToCap(Rate rate) {
        List<Arrival> arrivals = run(rate);
        return arrivals.isEmpty() ? -1 : arrivals.get(arrivals.size() - 1).minutesTotal();
    }

    /** Minutes spent inside each band, which is where a phase that vanishes shows up. */
    public Map<String, Double> minutesPerBand(Policy policy) {
        var out = new LinkedHashMap<String, Double>();
        double perMinute = policy.workPerMinute(awards);
        if (perMinute <= 0) return out;
        for (ProgressionCurve.Band band : curve.bands()) {
            double minutes = 0;
            for (int level = band.firstLevel(); level <= band.lastLevel(); level++)
                minutes += curve.costToLeave(level) / perMinute;
            out.put(band.firstLevel() + "-" + band.lastLevel(), minutes);
        }
        return out;
    }

    /** A human-readable run, for the report this instrument exists to produce. */
    public String report(List<Policy> policies) {
        var sb = new StringBuilder("PROGRESSION SIMULATION (design instrument; not evidence of balance)\n");
        for (Policy p : policies) {
            double perMinute = p.workPerMinute(awards);
            sb.append(String.format("%n%-18s %8.1f WP/min", p.name(), perMinute));
            if (perMinute <= 0) { sb.append(System.lineSeparator()).append("   never levels")
                                    .append(System.lineSeparator()); continue; }
            sb.append(String.format("   cap at %.0f min (%.1f h)%n", minutesToCap(p), minutesToCap(p) / 60));
            for (var e : minutesPerBand(p).entrySet())
                sb.append(String.format("    band %-8s %7.1f min%n", e.getKey(), e.getValue()));
        }
        return sb.toString();
    }
}
