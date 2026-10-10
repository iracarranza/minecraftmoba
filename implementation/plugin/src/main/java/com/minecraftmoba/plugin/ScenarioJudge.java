package com.minecraftmoba.plugin;

import java.util.Locale;

/**
 * The assertion vocabulary: what a scenario check can say, and how each is judged.
 *
 * Returns null for a pass and the reason for a failure, so a failure reads "health changed from
 * 20.0 to 16.0" and not "expected false". An assertion this does not know is a FAILURE, never a
 * pass: a typo in an assertion name must not turn into a green check over a rule that never ran.
 *
 * <pre>
 * health_unchanged   [since]        no change since a tick (default: tick 0)
 * health_below       [than: max|since]  strictly lower than full, or than a tick
 * health_not_lower   [since]        no LOWER than at a tick (regeneration may raise it)
 * health_at_least    [fraction]     at least this fraction of max (default 0.01)
 * alive
 * moved_less_than    blocks [since] horizontal movement
 * moved_more_than    blocks [since]
 * stunned / not_stunned
 * stunned_for_at_least  ticks
 * rooted / not_rooted
 * ability_executed  ability [times]  a real ability ran at least this many times (default 1)
 * </pre>
 *
 * Pure. Tested without a server.
 */
public final class ScenarioJudge {
    private ScenarioJudge() {}

    private static final double EPS = 1e-6;

    public static String judge(Scenario.Check c, ScenarioFacts f) {
        String id = c.subject();
        var a = c.args();
        long since = a.containsKey("since") ? Long.parseLong(a.get("since")) : 0;
        switch (c.assertion().toLowerCase(Locale.ROOT)) {
            case "health_unchanged" -> {
                double then = f.healthAt(id, since), now = f.health(id);
                if (Double.isNaN(then)) return "no health was recorded at tick " + since;
                return Math.abs(now - then) < EPS ? null : "health changed from " + fmt(then) + " to " + fmt(now);
            }
            case "health_not_lower" -> {
                double then = f.healthAt(id, since), now = f.health(id);
                if (Double.isNaN(then)) return "no health was recorded at tick " + since;
                return now >= then - EPS ? null : "health fell from " + fmt(then) + " to " + fmt(now);
            }
            case "health_below" -> {
                double now = f.health(id);
                boolean vsMax = !"since".equals(a.get("than"));
                double then = vsMax ? f.maxHealth(id) : f.healthAt(id, since);
                if (Double.isNaN(then)) return "no health was recorded at tick " + since;
                return now < then - EPS ? null : "health is " + fmt(now) + ", not below " + fmt(then) + (vsMax ? " (full)" : "");
            }
            case "health_at_least" -> {
                double frac = a.containsKey("fraction") ? Double.parseDouble(a.get("fraction")) : 0.01;
                double now = f.health(id), need = f.maxHealth(id) * frac;
                return now >= need ? null : "health " + fmt(now) + " is below " + fmt(need);
            }
            case "alive" -> { return f.alive(id) ? null : "is not alive"; }
            case "moved_less_than", "moved_more_than" -> {
                if (!a.containsKey("blocks")) return "needs a 'blocks' argument";
                double limit = Double.parseDouble(a.get("blocks"));
                double[] then = f.positionAt(id, since), now = f.position(id);
                if (then == null) return "no position was recorded at tick " + since;
                double moved = Math.hypot(now[0] - then[0], now[2] - then[2]);
                boolean less = c.assertion().equalsIgnoreCase("moved_less_than");
                if (less) return moved < limit ? null : "moved " + fmt(moved) + " blocks, not less than " + fmt(limit);
                return moved > limit ? null : "moved only " + fmt(moved) + " blocks, not more than " + fmt(limit);
            }
            case "stunned" -> { return f.stunned(id) ? null : "is not stunned"; }
            case "not_stunned" -> { return !f.stunned(id) ? null : "is still stunned (" + f.stunRemaining(id) + " ticks left)"; }
            case "stunned_for_at_least" -> {
                if (!a.containsKey("ticks")) return "needs a 'ticks' argument";
                long need = Long.parseLong(a.get("ticks")), left = f.stunRemaining(id);
                return left >= need ? null : "is stunned for only " + left + " more ticks, not " + need;
            }
            case "ability_executed" -> {
                if (!a.containsKey("ability")) return "needs an 'ability' argument";
                int need = a.containsKey("times") ? Integer.parseInt(a.get("times")) : 1, ran = f.executions(id, a.get("ability"));
                return ran >= need ? null : a.get("ability") + " ran " + ran + " time(s), not " + need
                        + " (a refused cast, a cooldown or a stun does not count)";
            }
            case "rooted" -> { return f.rooted(id) ? null : "is not rooted"; }
            case "not_rooted" -> { return !f.rooted(id) ? null : "is still rooted"; }
            default -> { return "unknown assertion '" + c.assertion() + "'"; }
        }
    }

    /** Every assertion name the judge understands, for validating scenarios at load. */
    public static final java.util.Set<String> KNOWN = java.util.Set.of(
            "health_unchanged", "health_not_lower", "health_below", "health_at_least", "alive", "moved_less_than", "moved_more_than",
            "stunned", "not_stunned", "stunned_for_at_least", "rooted", "not_rooted", "ability_executed");

    private static String fmt(double d) { return String.format(Locale.ROOT, "%.2f", d); }
}
