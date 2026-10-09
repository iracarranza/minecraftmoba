package com.minecraftmoba.plugin;

import org.bukkit.Material;

import java.util.List;

/**
 * What the opportunity bench's hotbar and deck buttons mean, and when each is refused.
 * Pure, and mirroring the other benches.
 *
 * The refusals are the lifecycle's own rules, said to the tester at the moment they would
 * otherwise wonder why nothing happened:
 * <ul>
 * <li>a STANDING manifestation does not recover, and a partly harvested one is not
 *     topped up, so skipping recovery on one is refused with that reason;</li>
 * <li>a manifestation is only attempted when the opportunity is ready.</li>
 * </ul>
 */
public final class OpportunityBenchMenu {
    private OpportunityBenchMenu() {}

    public static final String LEAVE = "leave";

    /**
     * @param lifecycle the selected plot's opportunity state, or null when nothing is spawned there
     * @param remaining members left in the standing manifestation
     */
    public record State(boolean inBench, Opportunity.State lifecycle, int remaining) {
        boolean hasSource() { return lifecycle != null; }
    }

    public record Intent(List<String> command) {}

    public static ChamberMenu menu() {
        return new ChamberMenu("root")
                .page("root", "Opportunity bench",
                        ChamberMenu.Item.action("plot", "Next plot"),
                        ChamberMenu.Item.action("spawn", "Spawn the opportunity"),
                        ChamberMenu.Item.action("skip", "Skip to recovered"),
                        ChamberMenu.Item.action("manifest", "Force a manifestation"),
                        ChamberMenu.Item.submenu("harvest", "Harvest"),
                        ChamberMenu.Item.action("base", "Build over or clear the region"),
                        ChamberMenu.Item.action("time", "Day or night"),
                        ChamberMenu.Item.submenu("view", "Look"))
                .page("harvest", "Harvest",
                        ChamberMenu.Item.action("harvest.one", "Take one"),
                        ChamberMenu.Item.action("harvest.all", "Take everything"))
                .page("view", "Look",
                        ChamberMenu.Item.action("show", "Show eligible sites"),
                        ChamberMenu.Item.action("report", "Show the report"));
    }

    public static final List<String> VERBS = List.of(
            "plot", "spawn", "skip", "manifest", "harvest.one", "harvest.all", "base", "time", "show", "report", LEAVE);

    public static Intent intent(String id) {
        return switch (id) {
            case "harvest.one" -> new Intent(List.of("harvest", "one"));
            case "harvest.all" -> new Intent(List.of("harvest", "all"));
            default -> VERBS.contains(id) ? new Intent(List.of(id)) : null;
        };
    }

    public static String refusal(String id, State s) {
        if (!s.inBench()) return "Enter the opportunity bench first.";
        return switch (id) {
            case "skip" -> !s.hasSource() ? "Spawn the opportunity first."
                    : s.lifecycle() == Opportunity.State.RECOVERING ? null
                    : s.lifecycle() == Opportunity.State.MANIFESTED
                        ? "A standing manifestation does not recover, and a partly harvested one is not topped up. Harvest it all first."
                        : "It is already recovered and waiting for a place.";
            case "manifest" -> !s.hasSource() ? "Spawn the opportunity first."
                    : s.lifecycle() == Opportunity.State.READY_AWAITING_LOCUS ? null
                    : s.lifecycle() == Opportunity.State.RECOVERING ? "Still recovering: skip to recovered first."
                    : "A manifestation is already standing.";
            case "harvest.one", "harvest.all" -> !s.hasSource() ? "Spawn the opportunity first."
                    : s.lifecycle() == Opportunity.State.MANIFESTED && s.remaining() > 0 ? null
                    : "Nothing is standing to harvest.";
            case "base", "show" -> s.hasSource() ? null : "Spawn the opportunity first.";
            default -> null;
        };
    }

    public static ChamberMenu.Gate gate(State s) { return id -> refusal(id, s); }

    /** The deck stands west of the platform's viewing station, flush with the floor. */
    public static DeckLayout deck() {
        return DeckLayout.row(-5, OpportunityPlots.FLOOR_Y, -42, 4, VERBS, OpportunityBenchMenu::look, -7);
    }

    static DeckLayout.Look look(String verb) {
        return switch (verb) {
            case "plot" -> new DeckLayout.Look(Material.LIGHT_BLUE_CONCRETE, "NEXT PLOT");
            case "spawn" -> new DeckLayout.Look(Material.GREEN_CONCRETE, "SPAWN");
            case "skip" -> new DeckLayout.Look(Material.CYAN_CONCRETE, "SKIP RECOVERY");
            case "manifest" -> new DeckLayout.Look(Material.LIME_CONCRETE, "FORCE MANIFEST");
            case "harvest.one" -> new DeckLayout.Look(Material.ORANGE_CONCRETE, "TAKE ONE");
            case "harvest.all" -> new DeckLayout.Look(Material.RED_CONCRETE, "TAKE ALL");
            case "base" -> new DeckLayout.Look(Material.BROWN_CONCRETE, "BUILD OVER");
            case "time" -> new DeckLayout.Look(Material.YELLOW_CONCRETE, "DAY / NIGHT");
            case "show" -> new DeckLayout.Look(Material.MAGENTA_CONCRETE, "SHOW SITES");
            case "report" -> new DeckLayout.Look(Material.WHITE_CONCRETE, "SHOW REPORT");
            default -> new DeckLayout.Look(Material.BLACK_CONCRETE, "LEAVE BENCH");
        };
    }
}
