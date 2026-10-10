package com.minecraftmoba.plugin;

import java.util.List;

/**
 * The map inspection overlay's hotbar and typed verbs, and when each is refused. Pure.
 *
 * Every verb is a single press: layers TOGGLE and the team CYCLES, so there is no page to
 * descend into and one root page of eight holds them all. The overlay is a view onto a
 * launched scoop, not a venue, so there is no deck of world buttons: standing in a real map
 * with a row of concrete buttons would be wrong. Leaving is an inventory item, as in the
 * other chambers.
 */
public final class InspectionMenu {
    private InspectionMenu() {}

    public enum Layer { CELLS, COST, RELATION, BANDS, POINTS, LANDMARKS }

    /** @param active the overlay is on for this tester; @param hasData the scoop carries an inspection sidecar */
    public record State(boolean active, boolean hasData) {}

    public record Intent(List<String> command) {}

    public static ChamberMenu menu() {
        return new ChamberMenu("root")
                .page("root", "Map overlay",
                        ChamberMenu.Item.action("cells", "Cell grid"),
                        ChamberMenu.Item.action("cost", "Cost field"),
                        ChamberMenu.Item.action("relation", "Who each cell favours"),
                        ChamberMenu.Item.action("bands", "Band edges"),
                        ChamberMenu.Item.action("points", "Field points"),
                        ChamberMenu.Item.action("landmarks", "Landmarks"),
                        ChamberMenu.Item.action("team", "Switch team"),
                        ChamberMenu.Item.action("report", "Legend and counts"));
    }

    public static final List<String> VERBS = List.of("cells", "cost", "relation", "bands", "points", "landmarks", "team", "report", "off");

    public static Intent intent(String id) { return VERBS.contains(id) ? new Intent(List.of(id)) : null; }

    public static Layer layer(String verb) {
        return switch (verb) {
            case "cells" -> Layer.CELLS; case "cost" -> Layer.COST; case "relation" -> Layer.RELATION;
            case "bands" -> Layer.BANDS; case "points" -> Layer.POINTS; case "landmarks" -> Layer.LANDMARKS;
            default -> null;
        };
    }

    public static String refusal(String id, State s) {
        if (!s.hasData())
            return "This scoop has no inspection data: it was published before the overlay existed. Regenerate its sidecar with terrain_harvest.inspection_sidecar.";
        if (!s.active()) return "Start the overlay first: /moba lab overlay.";
        return null;
    }

    public static ChamberMenu.Gate gate(State s) { return id -> refusal(id, s); }
}
