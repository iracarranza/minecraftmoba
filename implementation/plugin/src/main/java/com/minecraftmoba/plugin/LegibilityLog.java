package com.minecraftmoba.plugin;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * What the tester judged, per variant: the distances at which they said "it still reads".
 *
 * A mark is the tester's judgement, not a measurement of the screen, so the log keeps
 * every mark and summarises with the median rather than the mean: one distracted mark
 * should not move the answer. Marks are keyed by variant signature, so the same
 * presentation tried on two occasions accumulates.
 *
 * Pure. Tested without a server.
 */
public final class LegibilityLog {
    public record Summary(int count, double min, double median, double max) {}

    private final Map<String, List<Double>> marks = new LinkedHashMap<>();

    public void mark(String signature, double distance) {
        if (distance <= 0) throw new IllegalArgumentException("A mark needs a positive distance.");
        marks.computeIfAbsent(signature, k -> new ArrayList<>()).add(distance);
    }

    public Summary summary(String signature) {
        var list = marks.get(signature);
        if (list == null || list.isEmpty()) return null;
        var sorted = new ArrayList<>(list);
        java.util.Collections.sort(sorted);
        int n = sorted.size();
        double median = n % 2 == 1 ? sorted.get(n / 2) : (sorted.get(n / 2 - 1) + sorted.get(n / 2)) / 2.0;
        return new Summary(n, sorted.get(0), median, sorted.get(n - 1));
    }

    public List<String> signatures() { return List.copyOf(marks.keySet()); }
    public int total() { return marks.values().stream().mapToInt(List::size).sum(); }
    public void clear() { marks.clear(); }
}
