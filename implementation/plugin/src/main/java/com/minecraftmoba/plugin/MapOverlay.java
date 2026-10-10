package com.minecraftmoba.plugin;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * The map inspection overlay: the compiler's measurements of a launched scoop, drawn in the
 * world around the tester.
 *
 * <h2>What it draws</h2>
 *
 * <ul>
 * <li><b>Cell grid</b>: the analysis cells' borders, as particles at the ground.</li>
 * <li><b>Cost field</b>: a coloured column over each cell, tinted by one team's traversal
 *     cost, with a label giving both. Switch team to see the other field.</li>
 * <li><b>Relation</b>: the same columns tinted by who the cell is cheap for.</li>
 * <li><b>Band edges</b>: the borders where the near / farther / deeper band changes for the
 *     chosen team, coloured by the band on the cheaper side. This is where the compiler's
 *     boundary actually runs.</li>
 * <li><b>Field points</b>: each regenerative source the compiler placed, as a beam and a
 *     label, coloured by type.</li>
 * <li><b>Landmarks</b>: Fountains, Objectives, the Lair and Worksites.</li>
 * </ul>
 *
 * <h2>It draws what was measured, and only near you</h2>
 *
 * Every value comes from the scoop's {@code inspection.json}: nothing is recomputed here. A
 * scoop is large and entities are not free, so columns and markers are drawn only within
 * {@link #DRAW_RADIUS} of the tester and refreshed as they move, adding and removing only what
 * entered or left. Border particles are drawn within {@link #LINE_RADIUS}.
 *
 * <h2>Whose</h2>
 *
 * Entities are tagged with their owner and non-persistent; they are visible to everyone in the
 * world, which in a lab scoop is the tester.
 *
 * NOT VERIFIED beyond the live acceptance run at the time of writing; how legible it is to a
 * person is exactly what it exists to find out.
 */
public final class MapOverlay {
    static final String TAG = "map_overlay";
    /** Cells, points and landmarks within this many blocks of the tester get entities. */
    public static final double DRAW_RADIUS = 192;
    /** Border particles within this many blocks. */
    static final double LINE_RADIUS = 80;
    private static final int MAX_LINE_PARTICLES = 700;
    private static final double COLUMN_HEIGHT = 40;

    private static final class Active {
        final Set<InspectionMenu.Layer> layers = EnumSet.noneOf(InspectionMenu.Layer.class);
        String team = "north";
        MapInspection data;
        /** Entities per cell column, keyed by cell. */
        final Map<Long, List<Entity>> cellEntities = new HashMap<>();
        final List<Entity> markers = new ArrayList<>();
        /** Where each marker stands, so its chunk can be released with it. */
        final List<int[]> markerSpots = new ArrayList<>();
        /** What the cell columns currently show, so a switch redraws them. */
        String columnMode = "";
        /** Chunks held loaded, with how many entities stand in each. */
        final Map<Long, Integer> held = new HashMap<>();
        BukkitTask refresh;
        BukkitTask lines;
    }

    private final MobaPlugin plugin;
    private final Lab lab;
    private final BenchUi ui;
    private final Map<UUID, Active> active = new HashMap<>();

    public MapOverlay(MobaPlugin plugin, Lab lab) {
        this.plugin = plugin;
        this.lab = lab;
        this.ui = new BenchUi(plugin, new BenchUi.Spec(
                "overlay", InspectionMenu::menu,
                p -> InspectionMenu.gate(state(p)),
                (p, verb) -> InspectionMenu.refusal(verb, state(p)),
                // No deck: this is a view onto a real scoop, not a venue.
                new DeckLayout(new DeckLayout.Box(0, 0, 0, -1, -1, -1), List.of()),
                plugin.getConfig().getString("alpha.lab.instanceWorldName", "moba_lab_match"),
                this::runVerb, this::stop, "Leave overlay", "Switches the map overlay off and gives you your own hotbar back.",
                // A scoop tester has a class, and right-click is A2 while ability mode is on.
                p -> plugin.enrolled(p) && plugin.inputs().active(p)));
    }

    private InspectionMenu.State state(Player p) {
        var a = active.get(p.getUniqueId());
        return new InspectionMenu.State(a != null, a != null || sidecar(p) != null);
    }

    /** The active scoop's inspection file, or null when there is none. */
    private java.nio.file.Path sidecar(Player p) {
        var id = lab.activeMapId();
        if (id == null) return null;
        try {
            var entry = lab.maps().select(id);
            var path = entry.directory().resolve("inspection.json");
            return Files.isRegularFile(path) ? path : null;
        } catch (RuntimeException ex) { return null; }
    }

    // ---- commands ---------------------------------------------------------------------

    /** Handle {@code /moba lab overlay ...}. */
    public void command(Player p, String[] args) {
        String verb = args.length > 2 ? args[2].toLowerCase(Locale.ROOT) : "start";
        if (!active.containsKey(p.getUniqueId())) {
            if (verb.equals("start") || verb.equals("on")) start(p);
            else p.sendMessage("/moba lab overlay");
            return;
        }
        runVerb(p, verb);
    }

    void runVerb(Player p, String verb) {
        var a = active.get(p.getUniqueId());
        if (a == null) return;
        String refusal = InspectionMenu.refusal(verb, state(p));
        if (refusal != null) { p.sendMessage(ChatColor.RED + refusal); return; }
        var layer = InspectionMenu.layer(verb);
        if (layer != null) {
            if (!a.layers.add(layer)) a.layers.remove(layer);
            p.sendMessage(ChatColor.GREEN + layer.name().toLowerCase(Locale.ROOT).replace('_', ' ') + (a.layers.contains(layer) ? " on" : " off")
                    + (needsTeam(layer) ? " (" + a.team + ")" : "") + ".");
            redraw(p, a);
            return;
        }
        switch (verb) {
            case "team" -> {
                a.team = a.team.equals("north") ? "south" : "north";
                p.sendMessage(ChatColor.GREEN + "Showing the " + a.team + " team's cost field and bands.");
                redraw(p, a);
            }
            case "report", "legend", "status" -> reportLines(p).forEach(p::sendMessage);
            case "off", "leave", "stop" -> stop(p);
            default -> p.sendMessage("In the overlay: /moba lab overlay cells | cost | relation | bands | points | landmarks | team | report | off");
        }
    }

    private static boolean needsTeam(InspectionMenu.Layer l) { return l == InspectionMenu.Layer.COST || l == InspectionMenu.Layer.BANDS; }

    private void start(Player p) {
        var world = lab.scoopWorld();
        if (world == null || !p.getWorld().equals(world))
            throw new IllegalStateException("The overlay draws on a launched scoop. /moba lab start, choose a class and a scoop, then Launch test.");
        var path = sidecar(p);
        if (path == null) throw new IllegalStateException(InspectionMenu.refusal("cells", new InspectionMenu.State(false, false)));
        var a = new Active();
        try { a.data = MapInspection.parse(Files.readString(path)); }
        catch (IOException | RuntimeException ex) { throw new IllegalStateException("Could not read " + path + ": " + ex.getMessage()); }
        active.put(p.getUniqueId(), a);
        ui.engage(p, world);
        a.refresh = Bukkit.getScheduler().runTaskTimer(plugin, () -> refreshTick(p, a), 20L, 20L);
        a.lines = Bukkit.getScheduler().runTaskTimer(plugin, () -> lineTick(p, a), 10L, 10L);
        p.sendMessage(ChatColor.GREEN + "Map overlay on: " + a.data.cells().size() + " cells, " + a.data.points().size()
                + " field points. Use the hotbar to switch layers; Leave is in your inventory.");
        p.sendMessage(ChatColor.GRAY + "Costs are terrain-weighted traversal cost from each Fountain; bands are an analytical grouping, not an authored polygon. "
                + "Only what is near you is drawn.");
    }

    public void stop(Player p) {
        var a = active.remove(p.getUniqueId());
        ui.release(p);
        if (a == null) return;
        if (a.refresh != null) a.refresh.cancel();
        if (a.lines != null) a.lines.cancel();
        clear(a);
        if (p.isOnline()) p.sendMessage("Map overlay off.");
    }

    public void close() {
        for (var id : new ArrayList<>(active.keySet())) { Player p = Bukkit.getPlayer(id); if (p != null) stop(p); }
        active.clear();
        ui.close();
    }

    public boolean occupies(Player p) { return active.containsKey(p.getUniqueId()); }

    /** Entities currently drawn for a tester, by kind, for acceptance runs. */
    public Map<String, Integer> counts(Player p) {
        var a = active.get(p.getUniqueId());
        var out = new HashMap<String, Integer>();
        if (a == null) return out;
        int columns = 0; for (var l : a.cellEntities.values()) columns += l.size();
        out.put("cellEntities", columns); out.put("cells", a.cellEntities.size()); out.put("markers", a.markers.size());
        return out;
    }

    public MapInspection data(Player p) { var a = active.get(p.getUniqueId()); return a == null ? null : a.data; }

    // ---- drawing ---------------------------------------------------------------------------

    private void refreshTick(Player p, Active a) {
        if (!p.isOnline()) { stop(p); return; }
        redraw(p, a);
    }

    /** Bring the entities in line with the layers, the team, and where the tester is now. */
    private void redraw(Player p, Active a) {
        World w = p.getWorld();
        var here = p.getLocation();
        String mode = columnMode(a);
        if (!mode.equals(a.columnMode)) { clearCells(a); a.columnMode = mode; }
        // Cell columns: only cells in range, adding and removing just what changed.
        var wanted = new HashMap<Long, MapInspection.Cell>();
        if (!mode.isEmpty())
            for (var c : a.data.cellsWithin(here.getX(), here.getZ(), DRAW_RADIUS)) wanted.put(InspectionDraw.keyOf(c), c);
        for (var it = a.cellEntities.entrySet().iterator(); it.hasNext(); ) {
            var e = it.next();
            if (!wanted.containsKey(e.getKey())) {
                e.getValue().forEach(Entity::remove); it.remove();
                var gone = cellByKey(a, e.getKey());
                if (gone != null) release(w, a, gone.centroidX(), gone.centroidZ());
            }
        }
        for (var e : wanted.entrySet()) if (!a.cellEntities.containsKey(e.getKey())) {
            hold(w, a, e.getValue().centroidX(), e.getValue().centroidZ());
            a.cellEntities.put(e.getKey(), column(w, a, e.getValue()));
        }
        // Markers are few: rebuild them, releasing and re-taking their chunks.
        a.markers.forEach(Entity::remove); a.markers.clear();
        a.markerSpots.forEach(s -> release(w, a, s[0], s[1])); a.markerSpots.clear();
        if (a.layers.contains(InspectionMenu.Layer.POINTS))
            for (var pt : a.data.pointsWithin(here.getX(), here.getZ(), DRAW_RADIUS)) marker(w, a, pt.x(), pt.z(), InspectionDraw.pointGlass(pt.type()),
                    pt.kind() + " (" + pt.band() + ", " + pt.nearTeam() + ")");
        if (a.layers.contains(InspectionMenu.Layer.LANDMARKS)) landmarks(w, a, here);
    }

    private static MapInspection.Cell cellByKey(Active a, long key) {
        for (var c : a.data.cells()) if (InspectionDraw.keyOf(c) == key) return c;
        return null;
    }

    private String columnMode(Active a) {
        if (a.layers.contains(InspectionMenu.Layer.COST)) return "cost:" + a.team;
        if (a.layers.contains(InspectionMenu.Layer.RELATION)) return "relation";
        return "";
    }

    private List<Entity> column(World w, Active a, MapInspection.Cell c) {
        boolean cost = a.columnMode.startsWith("cost");
        String glass = cost ? InspectionDraw.costGlass(c.cost(a.team), a.data.openingCost()) : InspectionDraw.relationGlass(c.relation());
        int y = groundY(w, c.centroidX(), c.centroidZ(), c.surfaceY());
        var out = new ArrayList<Entity>();
        out.add(block(w, c.centroidX() + 0.5 - 1.5, y, c.centroidZ() + 0.5 - 1.5, 3f, (float) COLUMN_HEIGHT, 3f, Material.valueOf(glass)));
        String n = c.north() == null ? "-" : String.valueOf(Math.round(c.north())), s = c.south() == null ? "-" : String.valueOf(Math.round(c.south()));
        out.add(text(w, c.centroidX() + 0.5, y + COLUMN_HEIGHT + 1.2, c.centroidZ() + 0.5,
                "cell " + c.ci() + "," + c.cj() + "\nN " + n + " | S " + s + "\n" + c.relation(), NamedTextColor.WHITE));
        return out;
    }

    private void landmarks(World w, Active a, Location here) {
        for (var e : a.data.fountains().entrySet())
            if (near(here, e.getValue()[0], e.getValue()[2])) marker(w, a, e.getValue()[0], e.getValue()[2], "LIGHT_BLUE_STAINED_GLASS", e.getKey() + " Fountain");
        for (var o : a.data.objectives()) if (near(here, o.x(), o.z())) marker(w, a, o.x(), o.z(), "MAGENTA_STAINED_GLASS", o.label());
        if (a.data.lair() != null && near(here, a.data.lair()[0], a.data.lair()[2])) marker(w, a, a.data.lair()[0], a.data.lair()[2], "BLACK_STAINED_GLASS", "Lair");
        for (var s : a.data.worksites()) if (near(here, s.x(), s.z())) marker(w, a, s.x(), s.z(), "WHITE_STAINED_GLASS", "Worksite " + s.label());
    }

    private static boolean near(Location here, int x, int z) { return Math.hypot(here.getX() - x, here.getZ() - z) <= DRAW_RADIUS; }

    private void marker(World w, Active a, int x, int z, String glass, String label) {
        hold(w, a, x, z);
        a.markerSpots.add(new int[]{x, z});
        int y = groundY(w, x, z, null);
        a.markers.add(block(w, x + 0.5 - 0.25, y, z + 0.5 - 0.25, 0.5f, 16f, 0.5f, Material.valueOf(glass)));
        a.markers.add(text(w, x + 0.5, y + 17.2, z + 0.5, label, NamedTextColor.YELLOW));
    }

    /** Ground height at a column: the loaded world's own answer, else the cell's measured mean. */
    private int groundY(World w, int x, int z, Integer measured) {
        if (w.isChunkLoaded(x >> 4, z >> 4)) return w.getHighestBlockYAt(x, z) + 1;
        return measured != null ? measured + 1 : w.getHighestBlockYAt(x, z) + 1;
    }

    private BlockDisplay block(World w, double x, double y, double z, float sx, float sy, float sz, Material m) {
        return w.spawn(new Location(w, x, y, z), BlockDisplay.class, d -> {
            d.setBlock(m.createBlockData());
            d.setTransformation(new Transformation(new Vector3f(), new AxisAngle4f(), new Vector3f(sx, sy, sz), new AxisAngle4f()));
            d.setViewRange(4f);
            d.setPersistent(false);
            d.addScoreboardTag(TAG);
        });
    }

    private TextDisplay text(World w, double x, double y, double z, String s, NamedTextColor colour) {
        return w.spawn(new Location(w, x, y, z), TextDisplay.class, t -> {
            t.text(Component.text(s, colour));
            t.setBillboard(Display.Billboard.CENTER);
            t.setViewRange(4f);
            t.setPersistent(false);
            t.addScoreboardTag(TAG);
        });
    }

    /** Border particles near the tester, for the grid and the band edges. */
    private void lineTick(Player p, Active a) {
        if (!p.isOnline()) return;
        boolean grid = a.layers.contains(InspectionMenu.Layer.CELLS), bands = a.layers.contains(InspectionMenu.Layer.BANDS);
        if (!grid && !bands) return;
        var here = p.getLocation();
        World w = p.getWorld();
        var nearCells = a.data.cellsWithin(here.getX(), here.getZ(), LINE_RADIUS + a.data.cellSize());
        int budget = MAX_LINE_PARTICLES;
        if (grid)
            for (var e : InspectionDraw.gridEdges(nearCells, a.data.cellSize()))
                budget = draw(p, w, e, here, Color.WHITE, budget);
        if (bands)
            for (var e : InspectionDraw.bandEdges(nearCells, a.data.cellSize(), a.team))
                budget = draw(p, w, e, here, colourOf(edgeBand(a, e)), budget);
    }

    /** The band on the cheaper side of an edge, so the colour says which band the border leaves. */
    private String edgeBand(Active a, InspectionDraw.Edge e) {
        String best = null; double bestCost = Double.MAX_VALUE;
        for (var c : a.data.cells()) {
            boolean touches = (e.x1() == e.x2()) ? (c.originX() == e.x1() || c.originX() + a.data.cellSize() == e.x1()) && c.originZ() == e.z1()
                    : (c.originZ() == e.z1() || c.originZ() + a.data.cellSize() == e.z1()) && c.originX() == e.x1();
            Double cost = c.cost(a.team);
            if (touches && cost != null && cost < bestCost) { bestCost = cost; best = c.band(a.team); }
        }
        return best;
    }

    private static Color colourOf(String band) {
        return switch (InspectionDraw.bandColour(band)) { case "LIME" -> Color.LIME; case "YELLOW" -> Color.YELLOW; case "RED" -> Color.RED; default -> Color.WHITE; };
    }

    private int draw(Player p, World w, InspectionDraw.Edge e, Location here, Color colour, int budget) {
        var dust = new Particle.DustOptions(colour, 1.6f);
        for (int[] pt : InspectionDraw.along(e, 4)) {
            if (budget <= 0) return 0;
            if (Math.hypot(pt[0] - here.getX(), pt[1] - here.getZ()) > LINE_RADIUS) continue;
            int y = w.isChunkLoaded(pt[0] >> 4, pt[1] >> 4) ? w.getHighestBlockYAt(pt[0], pt[1]) + 1 : (int) here.getY();
            p.spawnParticle(Particle.DUST, pt[0] + 0.5, y + 0.3, pt[1] + 0.5, 1, 0, 0, 0, 0, dust);
            budget--;
        }
        return budget;
    }

    private void clearCells(Active a) {
        var w = lab.scoopWorld();
        for (var e : a.cellEntities.entrySet()) {
            e.getValue().forEach(Entity::remove);
            var cell = cellByKey(a, e.getKey());
            if (cell != null && w != null) release(w, a, cell.centroidX(), cell.centroidZ());
        }
        a.cellEntities.clear();
    }

    private void clear(Active a) {
        clearCells(a);
        a.markers.forEach(Entity::remove); a.markers.clear();
        a.held.clear();
        var w = lab.scoopWorld();
        if (w != null) w.removePluginChunkTickets(plugin);
    }

    /**
     * Keep the chunks the overlay's entities stand in loaded.
     *
     * The entities are non-persistent, and a non-persistent entity is discarded when its chunk
     * unloads. A column 150 blocks from the tester sits in a chunk nobody is near, so without
     * a ticket it vanished (the first live run drew 58 entities and found 18).
     */
    private void hold(World w, Active a, int x, int z) {
        long key = ((long) (x >> 4) << 32) ^ ((z >> 4) & 0xffffffffL);
        if (a.held.merge(key, 1, Integer::sum) == 1) w.addPluginChunkTicket(x >> 4, z >> 4, plugin);
    }

    private void release(World w, Active a, int x, int z) {
        long key = ((long) (x >> 4) << 32) ^ ((z >> 4) & 0xffffffffL);
        Integer n = a.held.get(key);
        if (n == null) return;
        if (n <= 1) { a.held.remove(key); w.removePluginChunkTicket(x >> 4, z >> 4, plugin); }
        else a.held.put(key, n - 1);
    }


    // ---- the report ---------------------------------------------------------------------------

    public List<String> reportLines(Player p) {
        var a = active.get(p.getUniqueId());
        var out = new ArrayList<String>();
        if (a == null) return out;
        var d = a.data;
        out.add(ChatColor.GOLD + "Map overlay: " + d.cells().size() + " cells of " + d.cellSize() + " blocks, "
                + d.points().size() + " field points, team shown: " + a.team + ", layers: " + a.layers);
        long reachableN = d.cells().stream().filter(c -> c.north() != null).count(), reachableS = d.cells().stream().filter(c -> c.south() != null).count();
        out.add("Reachable cells: north " + reachableN + ", south " + reachableS + " of " + d.cells().size()
                + ". Opening cost " + (int) d.openingCost() + " (within it is 'near').");
        var rel = new java.util.TreeMap<String, Integer>();
        for (var c : d.cells()) rel.merge(String.valueOf(c.relation()), 1, Integer::sum);
        out.add("Cells by relation: " + rel);
        var byBand = new java.util.TreeMap<String, Integer>();
        for (var c : d.cells()) byBand.merge(String.valueOf(c.band(a.team)), 1, Integer::sum);
        out.add("Cells by " + a.team + " band: " + byBand + "; " + InspectionDraw.bandEdges(d.cells(), d.cellSize(), a.team).size() + " band-edge borders.");
        var kinds = new java.util.TreeMap<String, Integer>();
        for (var pt : d.points()) kinds.merge(pt.kind() + "/" + pt.band(), 1, Integer::sum);
        out.add("Field points by kind and band: " + kinds);
        out.add("Cost colours (" + a.team + "): " + String.join("; ", InspectionDraw.costLegend(d.openingCost())));
        out.add("Relation colours: " + String.join("; ", InspectionDraw.relationLegend()));
        out.add(ChatColor.GRAY + "Bands are an analytical grouping, not an authored polygon; an absent point is a real answer.");
        return out;
    }
}
