package com.minecraftmoba.plugin;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * The level-cost curve, as a thing that can be reasoned about rather than
 * looked up.
 *
 * Takes the shipped numbers rather than a copy of them. A model carrying its
 * own transcription of the curve validates the transcription, not the game, and
 * the transcription is the easier of the two to get right.
 *
 * <h2>The band boundary drop is a feature, and this can see it</h2>
 *
 * The config states that cost drops at each band boundary on purpose: a phase
 * restarts from its own multiplier and then compounds within itself, so
 * entering a new phase is a step down in per-level cost and a step up in the
 * cost that phase will reach.
 *
 * That makes the drops <b>structural</b>, which means bands can be recovered
 * from the curve itself instead of from the comment that describes them. The
 * comment is then checkable, which it currently is not: nothing in the config
 * declares which levels belong to which band.
 */
public final class ProgressionCurve {

    /**
     * A contiguous run of levels sharing one multiplier.
     *
     * {@code multiplier} is the DECLARED value this band was identified as, not
     * the ratio computed from its first cost. Deriving it from a cost that has
     * already been rounded to 5 loses precision -- level 13's 560 implies
     * 1.8667 rather than the declared 1.875 -- and that error then compounds
     * through {@code 1.15^i} and reports every later level in the band as
     * wrong. {@code rawRatio} is kept for diagnosing a band that matches
     * nothing.
     */
    public record Band(int firstLevel, int lastLevel, double multiplier, double rawRatio) {
        public int length() { return lastLevel - firstLevel + 1; }
        public boolean contains(int level) { return level >= firstLevel && level <= lastLevel; }
    }

    private final TreeMap<Integer, Integer> costs = new TreeMap<>();
    private final int maxLevel;
    private final double baseline, inBandFactor;

    public ProgressionCurve(Map<Integer, Integer> costToLeave, int maxLevel,
                            double baseline, double inBandFactor) {
        this.costs.putAll(costToLeave);
        this.maxLevel = maxLevel;
        this.baseline = baseline;
        this.inBandFactor = inBandFactor;
    }

    public int maxLevel() { return maxLevel; }

    /** Work Points required to leave {@code level}. Zero at the cap. */
    public int costToLeave(int level) { return costs.getOrDefault(level, 0); }

    /** Total Work Points to reach {@code level} from level 1. */
    public long cumulativeTo(int level) {
        long total = 0;
        for (int l = 1; l < level; l++) total += costToLeave(l);
        return total;
    }

    /** Every level that has a cost, in order. */
    public List<Integer> levelsWithCost() { return new ArrayList<>(costs.keySet()); }

    /**
     * Recover the bands from the curve, by splitting wherever cost falls.
     *
     * Within a band cost compounds upward, so a fall can only be a boundary.
     * The multiplier is read back from the band's own first cost rather than
     * assumed, which is what lets a caller check it against what the config
     * says the multipliers are.
     */
    /** Band ranges with their raw, rounding-contaminated ratios. */
    public List<Band> bands() { return bands(List.of()); }

    /**
     * Band ranges, with each identified against the declared multipliers.
     *
     * Passing the declared set is what makes the formula check exact. Passing
     * none leaves the raw ratio in place, which is only useful for diagnosis.
     */
    public List<Band> bands(java.util.Collection<Double> declared) {
        var out = new ArrayList<Band>();
        List<Integer> levels = levelsWithCost();
        if (levels.isEmpty()) return out;

        int first = levels.get(0);
        for (int i = 1; i <= levels.size(); i++) {
            boolean boundary = i == levels.size()
                    || costToLeave(levels.get(i)) < costToLeave(levels.get(i - 1));
            if (boundary) {
                int last = levels.get(i - 1);
                double raw = costToLeave(first) / baseline;
                out.add(new Band(first, last, snap(raw, declared), raw));
                if (i < levels.size()) first = levels.get(i);
            }
        }
        return out;
    }

    /**
     * The declared multiplier nearest a raw ratio, or the raw ratio when
     * nothing was declared.
     *
     * Nearest rather than exact, because the raw ratio comes from a rounded
     * cost and will essentially never equal a declared value. A band matching
     * nothing within a sensible distance is left raw so the caller sees it.
     */
    private static double snap(double raw, java.util.Collection<Double> declared) {
        double best = raw, bestGap = Double.MAX_VALUE;
        for (double d : declared) {
            double gap = Math.abs(d - raw);
            if (gap < bestGap) { bestGap = gap; best = d; }
        }
        return bestGap <= 0.05 ? best : raw;
    }

    /**
     * What the documented formula says a cost should be:
     * {@code 300 * multiplier * 1.15^i}, rounded to the nearest 5.
     *
     * The rounding is part of the published rule, not a tolerance -- the config
     * says values are rounded to convenient 5-WP steps -- so a check against
     * this is exact rather than approximate.
     */
    public int expected(double multiplier, int indexWithinBand) {
        double raw = baseline * multiplier * Math.pow(inBandFactor, indexWithinBand);
        // Half-to-EVEN, not half-up. Two costs land exactly on a tie -- 562.5
        // at the Developed band's start and 772.5 at Advanced's -- and the
        // shipped curve rounds both down, to 560 and 770. Math.round would
        // take both up and report two false disagreements.
        return (int) (Math.rint(raw / 5.0) * 5);
    }

    /** Every level's cost against the formula, for the levels that disagree. */
    public Map<Integer, String> disagreementsWithFormula(java.util.Collection<Double> declared) {
        var out = new LinkedHashMap<Integer, String>();
        for (Band band : bands(declared)) {
            int i = 0;
            for (int level = band.firstLevel(); level <= band.lastLevel(); level++, i++) {
                int want = expected(band.multiplier(), i);
                int got = costToLeave(level);
                if (want != got) out.put(level, "formula " + want + ", config " + got);
            }
        }
        return out;
    }
}
