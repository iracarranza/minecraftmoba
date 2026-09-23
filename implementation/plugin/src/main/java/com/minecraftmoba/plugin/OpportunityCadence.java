package com.minecraftmoba.plugin;

/** Exceptional opportunities derived from the one match clock; not an independent timer. */
public final class OpportunityCadence {
    public enum WorksiteTier { I, II, III }
    public enum Boss { GIANT, GHAST, ENDER_DRAGON }
    public enum Stage {
        OPENING(null, null), WORKSITE_I(WorksiteTier.I, null), GIANT(null, Boss.GIANT),
        WORKSITE_II(WorksiteTier.II, null), GHAST(null, Boss.GHAST),
        WORKSITE_III(WorksiteTier.III, null), DRAGON(null, Boss.ENDER_DRAGON),
        UNSCHEDULED(null, null);
        private final WorksiteTier tier;
        private final Boss boss;
        Stage(WorksiteTier tier, Boss boss) { this.tier = tier; this.boss = boss; }
        public WorksiteTier tier() { return tier; }
        public Boss boss() { return boss; }
    }
    private OpportunityCadence() {}
    /** The existing first sunset is opportunity night 1 (tick 12000). */
    public static Stage atNight(int ordinal) {
        return switch (ordinal) {
            case 0 -> Stage.OPENING;
            case 1 -> Stage.WORKSITE_I;
            case 2 -> Stage.GIANT;
            case 3 -> Stage.WORKSITE_II;
            case 4 -> Stage.GHAST;
            case 5 -> Stage.WORKSITE_III;
            case 6 -> Stage.DRAGON;
            default -> Stage.UNSCHEDULED; // post-Dragon schedule is OPEN; do not invent repeats
        };
    }
    public static Stage at(long elapsedTicks) { return atNight(MatchClock.sunsetOrdinal(elapsedTicks)); }
}
