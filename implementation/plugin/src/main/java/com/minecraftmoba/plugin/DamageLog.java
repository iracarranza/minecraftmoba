package com.minecraftmoba.plugin;

import java.util.ArrayList;
import java.util.List;

/**
 * What happened to something over time, and what it implies.
 *
 * Pure accumulation. The raw and after-armor damage are both kept because the PAIR
 * is how armor and Damage Reduction are checked: a number that already includes
 * armor cannot be compared with the number that does not.
 *
 * Ticks, not seconds: a tick is the server's own unit and 20 of them make a second.
 */
public final class DamageLog {

    /** One damaging event. {@code raw} is before armor and reduction, {@code finalDamage} after. */
    public record Hit(long tick, double raw, double finalDamage, String source) {
        public Hit {
            if (raw < 0 || finalDamage < 0) throw new IllegalArgumentException("damage cannot be negative");
        }
        /** How much armor and reduction removed, as a fraction of raw. Zero for zero raw. */
        public double mitigated() { return raw == 0 ? 0 : 1 - finalDamage / raw; }
    }

    public static final int TICKS_PER_SECOND = 20;

    private final List<Hit> hits = new ArrayList<>();

    public void record(Hit hit) {
        if (!hits.isEmpty() && hit.tick() < hits.get(hits.size() - 1).tick())
            throw new IllegalArgumentException("hits must be recorded in tick order");
        hits.add(hit);
    }

    public void record(long tick, double raw, double finalDamage, String source) {
        record(new Hit(tick, raw, finalDamage, source));
    }

    public int count() { return hits.size(); }
    public boolean isEmpty() { return hits.isEmpty(); }
    public List<Hit> hits() { return List.copyOf(hits); }
    public void clear() { hits.clear(); }

    public double totalRaw() { return hits.stream().mapToDouble(Hit::raw).sum(); }
    public double totalFinal() { return hits.stream().mapToDouble(Hit::finalDamage).sum(); }

    /** Final damage within the last {@code windowTicks} ticks up to and including {@code now}. */
    public double finalWithin(long now, int windowTicks) {
        if (windowTicks <= 0) throw new IllegalArgumentException("a window must be positive");
        double sum = 0;
        for (Hit h : hits) if (h.tick() > now - windowTicks && h.tick() <= now) sum += h.finalDamage();
        return sum;
    }

    /** Final damage per second over the window. */
    public double perSecond(long now, int windowTicks) {
        return finalWithin(now, windowTicks) * TICKS_PER_SECOND / windowTicks;
    }

    /**
     * How long, in seconds, until {@code health} runs out at the recent rate.
     * Infinity when nothing has landed in the window, because "never" is the honest answer.
     */
    public double secondsToKill(double health, long now, int windowTicks) {
        double rate = perSecond(now, windowTicks);
        return rate <= 0 ? Double.POSITIVE_INFINITY : health / rate;
    }

    /** Damage grouped by source, for "what did the most". Insertion-ordered. */
    public java.util.Map<String, Double> finalBySource() {
        var out = new java.util.LinkedHashMap<String, Double>();
        for (Hit h : hits) out.merge(h.source(), h.finalDamage(), Double::sum);
        return out;
    }

    /** One line per hit, for a report: tick, source, raw -> final. */
    public List<String> lines() {
        var out = new ArrayList<String>();
        for (Hit h : hits)
            out.add(String.format("t%d %s raw=%.2f final=%.2f", h.tick(), h.source(), h.raw(), h.finalDamage()));
        return out;
    }
}
