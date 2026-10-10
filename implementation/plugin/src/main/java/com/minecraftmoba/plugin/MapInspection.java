package com.minecraftmoba.plugin;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * What the compiler measured about a scoop, read from its {@code inspection.json} sidecar.
 *
 * The overlay draws these; it never re-derives them. Costs, relations and bands were computed
 * at compile time by the same code that certified the map ({@code cell_grid},
 * {@code reach_fields}), so what a tester sees is what the certification saw, and a second
 * implementation of "near" cannot drift from the first.
 *
 * Pure. Parsed from a string so it is tested without files or a server.
 */
public record MapInspection(int cellSize, double openingCost, List<Cell> cells, Map<String, int[]> fountains,
                            List<Landmark> objectives, int[] lair, List<Landmark> worksites, List<Point> points) {

    /** One analysis cell. Costs are null where the cell is unreachable for that team. */
    public record Cell(int ci, int cj, int originX, int originZ, int centroidX, int centroidZ,
                       Double north, Double south, String relation, String bandNorth, String bandSouth, Integer surfaceY) {
        public Double cost(String team) { return team.equals("north") ? north : south; }
        public String band(String team) { return team.equals("north") ? bandNorth : bandSouth; }
    }

    public record Landmark(String label, int x, int z) {}

    /** A field point: where the compiler placed a regenerative source. */
    public record Point(String id, String kind, String type, String band, String nearTeam, int x, int y, int z,
                        int radius, int capacity) {}

    public static MapInspection parse(String json) {
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        if (!"moba_map_inspection/1".equals(str(root, "schema")))
            throw new IllegalArgumentException("Not a map inspection file (schema " + str(root, "schema") + ").");
        int size = root.get("cell_size").getAsInt();
        double opening = root.get("opening_cost").getAsDouble();
        var cells = new ArrayList<Cell>();
        for (JsonElement e : root.getAsJsonArray("cells")) {
            JsonObject c = e.getAsJsonObject();
            var cell = c.getAsJsonArray("cell"); var origin = c.getAsJsonArray("origin"); var cen = c.getAsJsonArray("centroid");
            JsonObject cost = c.getAsJsonObject("cost"); JsonObject band = c.getAsJsonObject("band");
            cells.add(new Cell(cell.get(0).getAsInt(), cell.get(1).getAsInt(), origin.get(0).getAsInt(), origin.get(1).getAsInt(),
                    cen.get(0).getAsInt(), cen.get(1).getAsInt(), num(cost, "north"), num(cost, "south"),
                    str(c, "relation"), str(band, "north"), str(band, "south"),
                    c.has("surface_y") && !c.get("surface_y").isJsonNull() ? (int) Math.round(c.get("surface_y").getAsDouble()) : null));
        }
        var fountains = new LinkedHashMap<String, int[]>();
        if (root.has("fountains")) for (var e : root.getAsJsonObject("fountains").entrySet()) {
            JsonArray a = e.getValue().getAsJsonArray();
            fountains.put(e.getKey(), new int[]{a.get(0).getAsInt(), a.get(1).getAsInt(), a.get(2).getAsInt()});
        }
        var objectives = new ArrayList<Landmark>();
        if (root.has("objectives")) for (JsonElement e : root.getAsJsonArray("objectives")) {
            JsonObject o = e.getAsJsonObject();
            objectives.add(new Landmark(str(o, "team") + " " + str(o, "kind"), o.get("x").getAsInt(), o.get("z").getAsInt()));
        }
        int[] lair = null;
        if (root.has("lair") && root.get("lair").isJsonArray()) {
            JsonArray a = root.getAsJsonArray("lair"); lair = new int[]{a.get(0).getAsInt(), a.get(1).getAsInt(), a.get(2).getAsInt()};
        }
        var worksites = new ArrayList<Landmark>();
        if (root.has("worksites")) for (JsonElement e : root.getAsJsonArray("worksites")) {
            JsonObject o = e.getAsJsonObject();
            worksites.add(new Landmark(str(o, "id"), o.get("x").getAsInt(), o.get("z").getAsInt()));
        }
        var points = new ArrayList<Point>();
        if (root.has("points")) for (JsonElement e : root.getAsJsonArray("points")) {
            JsonObject p = e.getAsJsonObject();
            points.add(new Point(str(p, "id"), str(p, "kind"), str(p, "type"), str(p, "band"), str(p, "near_team"),
                    p.get("x").getAsInt(), p.has("y") && !p.get("y").isJsonNull() ? p.get("y").getAsInt() : 0, p.get("z").getAsInt(),
                    p.has("radius") && !p.get("radius").isJsonNull() ? p.get("radius").getAsInt() : 0,
                    p.has("capacity") && !p.get("capacity").isJsonNull() ? p.get("capacity").getAsInt() : 0));
        }
        return new MapInspection(size, opening, List.copyOf(cells), Map.copyOf(fountains), List.copyOf(objectives), lair,
                List.copyOf(worksites), List.copyOf(points));
    }

    private static String str(JsonObject o, String k) { return o != null && o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsString() : null; }
    private static Double num(JsonObject o, String k) { return o != null && o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsDouble() : null; }

    /** Cells whose centroid lies within a horizontal distance of a point. */
    public List<Cell> cellsWithin(double x, double z, double radius) {
        var out = new ArrayList<Cell>();
        for (Cell c : cells) if (Math.hypot(c.centroidX() - x, c.centroidZ() - z) <= radius) out.add(c);
        return out;
    }

    public List<Point> pointsWithin(double x, double z, double radius) {
        var out = new ArrayList<Point>();
        for (Point p : points) if (Math.hypot(p.x() - x, p.z() - z) <= radius) out.add(p);
        return out;
    }

    /** The largest finite cost for a team, for scaling a gradient. Zero when nothing is reachable. */
    public double maxCost(String team) {
        double m = 0;
        for (Cell c : cells) { Double v = c.cost(team); if (v != null && v > m) m = v; }
        return m;
    }
}
