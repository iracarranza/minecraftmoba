package com.minecraftmoba.plugin;

import java.util.ArrayList;
import java.util.List;

/**
 * The six-night cadence as a table a tester can read, derived from {@link OpportunityCadence}
 * rather than restated.
 *
 * What opens on each night, and what the Lair does with it, is the part of the match a single
 * person cannot otherwise see without sitting through 110 minutes. This is the plan; the live
 * state (what actually activated, what actually stands) comes from the runtime, so the bench can
 * show both and a disagreement between the two is visible.
 *
 * Pure. Tested without a server.
 */
public final class NightTimeline {
    private NightTimeline() {}

    /** One night. {@code tier} and {@code boss} are null when the night opens neither. */
    public record Row(int night, long startsAtTick, OpportunityCadence.Stage stage,
                      OpportunityCadence.WorksiteTier tier, OpportunityCadence.Boss boss) {
        public String what() {
            if (tier != null) return "Worksite " + tier + ": " + tier.identity() + " (" + tier.economicRole() + ")";
            if (boss != null) return "Lair: " + boss + " installed (a surviving occupant is replaced)";
            return "nothing scheduled (the post-Dragon cadence is open)";
        }
    }

    /** Nights 1 to {@code through}, in order. */
    public static List<Row> rows(int through) {
        var out = new ArrayList<Row>();
        for (int n = 1; n <= through; n++) {
            var stage = OpportunityCadence.atNight(n);
            out.add(new Row(n, LabTime.sunsetTick(n), stage, stage.tier(), stage.boss()));
        }
        return out;
    }

    /** The cadence's six scheduled nights. */
    public static List<Row> scheduled() { return rows(6); }

    /** The night whose sunset most recently began, or 0 before the first. */
    public static int current(long elapsedTicks) { return MatchClock.sunsetOrdinal(elapsedTicks); }

    /** The report lines, marking the night the clock is in. */
    public static List<String> describe(long elapsedTicks) {
        int now = current(elapsedTicks);
        var out = new ArrayList<String>();
        for (var r : scheduled())
            out.add((r.night() == now ? "> " : "  ") + "night " + r.night() + " (" + MatchClock.minutes(r.startsAtTick()) + " min): " + r.what());
        return out;
    }
}
