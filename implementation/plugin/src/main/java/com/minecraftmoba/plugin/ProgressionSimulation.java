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
    public List<Arrival> run(Policy policy) {
        var out = new ArrayList<Arrival>();
        double perMinute = policy.workPerMinute(awards);
        if (perMinute <= 0) return out;

        double elapsed = 0;
        for (int level = 1; level < curve.maxLevel(); level++) {
            int cost = curve.costToLeave(level);
            double minutes = cost / perMinute;
            elapsed += minutes;
            out.add(new Arrival(level + 1, elapsed, minutes));
        }
        return out;
    }

    /** Minutes for a policy to reach the cap, or -1 if it never does. */
    public double minutesToCap(Policy policy) {
        List<Arrival> arrivals = run(policy);
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
