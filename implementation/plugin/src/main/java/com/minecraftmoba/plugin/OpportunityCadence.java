package com.minecraftmoba.plugin;

/** Exceptional opportunities derived from the one match clock; not an independent timer. */
public final class OpportunityCadence {
    /**
     * The three Worksite tiers and their declared economic identities.
     *
     * objectives.md 17C, reconciled 23 September 2026:
     *
     *   I   Iron + Coal              / Blast Furnace + Smoker   bulk material / processing
     *   II  Diamond + Lapis          / Enchanting Table         strategic capital / enhancement
     *   III Ancient Debris + Diamond / Smithing Table           apex material / conversion
     *
     * These strings are the DECLARED IDENTITY, not a reward spec. The runtime
     * knows what each Worksite night is economically about; it does not know
     * how much of anything manifests, because every quantity in that table is
     * still OPEN and inventing one here would turn a calibration hypothesis
     * into a production constant.
     *
     * [HISTORICAL] The four-tier ladder Copper -> Iron -> Diamond ->
     * Ancient Debris, paired with Blast/Smoker -> TBD intermediate factory ->
     * Enchanting -> Smithing, is superseded: the cadence has three Worksite
     * nights, Copper needs no exceptional injection, and the intermediate
     * factory was never defined and is removed rather than filled.
     */
    public enum WorksiteTier {
        I("Iron + Coal", "Blast Furnace + Smoker", "bulk material / processing"),
        II("Diamond + Lapis", "Enchanting Table", "strategic capital / enhancement"),
        III("Ancient Debris + Diamond", "Smithing Table", "apex material / conversion");

        private final String miningSite, factory, role;
        WorksiteTier(String miningSite, String factory, String role) {
            this.miningSite = miningSite; this.factory = factory; this.role = role;
        }
        /** The Mining Site's material backbone. Construction enables, Extraction pays off. */
        public String miningSite() { return miningSite; }
        /** The Industrial Factory's workstation. Logistics enables, Production pays off. */
        public String factory() { return factory; }
        public String economicRole() { return role; }
        public String identity() { return miningSite + " / " + factory; }
    }
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
