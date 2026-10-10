package com.minecraftmoba.plugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * What the overlay draws and in which colour, as pure decisions.
 *
 * Colours are chosen by NAME (a stained-glass material, a particle colour) and not by
 * rendering, so the choices are tests: a cell's cost bucket, a relation's colour, which cell
 * borders are band edges. The Bukkit layer maps a name to an entity or a particle and decides
 * nothing.
 *
 * <h2>Cost buckets are anchored on the map's own opening cost</h2>
 *
 * Within the opening cost is the cheapest bucket, whatever a map's spread. The compiler's
 * "near" is exactly that, so the overlay and the certification agree on where near ends.
 * Beyond it the buckets are multiples of it. They are a display choice, not a band: the
 * authoritative bands are drawn separately from the data.
 */
public final class InspectionDraw {
    private InspectionDraw() {}

    public static final String GREY = "GRAY_STAINED_GLASS";

    /** The glass colour for a cost, or grey when the cell is unreachable for that team. */
    public static String costGlass(Double cost, double openingCost) {
        if (cost == null) return GREY;
        if (cost <= openingCost) return "LIME_STAINED_GLASS";
        if (cost <= openingCost * 2) return "YELLOW_STAINED_GLASS";
        if (cost <= openingCost * 4) return "ORANGE_STAINED_GLASS";
        if (cost <= openingCost * 6) return "RED_STAINED_GLASS";
        return "PURPLE_STAINED_GLASS";
    }

    /** The legend for the cost colours, in order, for the report. */
    public static List<String> costLegend(double openingCost) {
        return List.of(
                "lime: within the opening (cost <= " + (int) openingCost + ")",
                "yellow: up to " + (int) (openingCost * 2), "orange: up to " + (int) (openingCost * 4),
                "red: up to " + (int) (openingCost * 6), "purple: beyond that", "grey: unreachable for this team");
    }

    /** The glass colour for a relation label from {@code reach_fields.relation}. */
    public static String relationGlass(String relation) {
        if (relation == null) return GREY;
        return switch (relation) {
            case "contested_opening" -> "PURPLE_STAINED_GLASS";
            case "cheap_north" -> "BLUE_STAINED_GLASS";
            case "cheap_south" -> "ORANGE_STAINED_GLASS";
            case "equal" -> "WHITE_STAINED_GLASS";
            case "behind_north" -> "LIGHT_BLUE_STAINED_GLASS";
            case "behind_south" -> "YELLOW_STAINED_GLASS";
            case "only_north" -> "CYAN_STAINED_GLASS";
            case "only_south" -> "RED_STAINED_GLASS";
            default -> GREY;      // unreachable, or a label this does not know
        };
    }

    public static List<String> relationLegend() {
        return List.of("blue: cheap for north", "orange: cheap for south", "purple: contested opening", "white: equally reachable",
                "light blue: behind north", "yellow: behind south", "cyan: only north reaches it", "red: only south reaches it",
                "grey: unreachable");
    }

    /** A particle colour (as a name) for a band. */
    public static String bandColour(String band) {
        if (band == null) return "WHITE";
        return switch (band) { case "near" -> "LIME"; case "farther" -> "YELLOW"; case "deeper" -> "RED"; default -> "WHITE"; };
    }

    /** The glass for a field point by its type: crops, animals and swarms read differently at a glance. */
    public static String pointGlass(String type) {
        if (type == null) return GREY;
        return switch (type) { case "CROP" -> "YELLOW_STAINED_GLASS"; case "ANIMAL" -> "LIME_STAINED_GLASS"; case "SWARM" -> "RED_STAINED_GLASS"; default -> GREY; };
    }

    /** One border segment in world blocks. */
    public record Edge(int x1, int z1, int x2, int z2) {
        public double length() { return Math.hypot(x2 - x1, z2 - z1); }
    }

    private static Edge[] borders(MapInspection.Cell c, int size) {
        int x0 = c.originX(), z0 = c.originZ(), x1 = x0 + size, z1 = z0 + size;
        return new Edge[]{new Edge(x0, z0, x1, z0), new Edge(x1, z0, x1, z1), new Edge(x0, z1, x1, z1), new Edge(x0, z0, x0, z1)};
    }

    /** Every cell border once, however many cells share it. */
    public static List<Edge> gridEdges(List<MapInspection.Cell> cells, int size) {
        var seen = new LinkedHashSet<Edge>();
        for (var c : cells) for (var e : borders(c, size)) seen.add(e);
        return new ArrayList<>(seen);
    }

    /**
     * The borders between two ADJACENT cells whose band differs for a team: where the
     * compiler's near/farther/deeper boundary actually runs. A border with a cell on only one
     * side is not an edge (there is nothing to differ from), and a cell with no band for that
     * team (unreachable) is treated as its own band so the reachable frontier shows.
     */
    public static List<Edge> bandEdges(List<MapInspection.Cell> cells, int size, String team) {
        Map<Long, MapInspection.Cell> byIndex = new HashMap<>();
        for (var c : cells) byIndex.put(key(c.ci(), c.cj()), c);
        var out = new ArrayList<Edge>();
        for (var c : cells) {
            var east = byIndex.get(key(c.ci() + 1, c.cj()));
            if (east != null && differs(c, east, team)) out.add(new Edge(c.originX() + size, c.originZ(), c.originX() + size, c.originZ() + size));
            var south = byIndex.get(key(c.ci(), c.cj() + 1));
            if (south != null && differs(c, south, team)) out.add(new Edge(c.originX(), c.originZ() + size, c.originX() + size, c.originZ() + size));
        }
        return out;
    }

    private static boolean differs(MapInspection.Cell a, MapInspection.Cell b, String team) {
        return !java.util.Objects.equals(a.band(team), b.band(team));
    }

    private static long key(int i, int j) { return ((long) i << 32) ^ (j & 0xffffffffL); }

    /** The cell keys, for diffing a redraw: only what entered or left the draw radius changes. */
    public static Set<Long> keys(List<MapInspection.Cell> cells) {
        var out = new LinkedHashSet<Long>();
        for (var c : cells) out.add(key(c.ci(), c.cj()));
        return out;
    }

    public static long keyOf(MapInspection.Cell c) { return key(c.ci(), c.cj()); }

    /** Sample points along an edge at a spacing, ends included, for particles. */
    public static List<int[]> along(Edge e, int spacing) {
        var out = new ArrayList<int[]>();
        double len = e.length();
        int n = Math.max(1, (int) Math.ceil(len / spacing));
        for (int i = 0; i <= n; i++) {
            double t = (double) i / n;
            out.add(new int[]{(int) Math.round(e.x1() + (e.x2() - e.x1()) * t), (int) Math.round(e.z1() + (e.z2() - e.z1()) * t)});
        }
        return out;
    }
}
