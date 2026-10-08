package com.minecraftmoba.plugin;

import java.util.*;

/** Pure, bounded prototype geometry planner; shared by preview and application. */
public final class LabGeometry {
    public record Pos(int x, int y, int z) {}
    public record XZ(int x, int z) {}
    public record TemplateBlock(int x, int y, int z, String data) {}
    public enum Module { LANDING, STRAIGHT, CORNER, STAIR, BRIDGE }
    public interface Terrain {
        int height(int x, int z);
        boolean water(int x, int z);
        String block(Pos pos);
        boolean protectedAt(Pos pos);
    }
    public record Plan(Map<Pos, String> edits, List<String> problems, int cut, int fill,
                       int maxCut, int maxFill, int relief, int steps, int bridges,
                       List<XZ> centreline, Map<Module,Integer> modules) {
        public Plan {
            edits = Collections.unmodifiableMap(new LinkedHashMap<>(edits));
            problems = List.copyOf(problems); centreline = List.copyOf(centreline); modules=Map.copyOf(modules);
        }
        public boolean accepted() { return problems.isEmpty() && !edits.isEmpty(); }
    }
    private static final String AIR = "minecraft:air";
    private LabGeometry() {}

    public static Plan structure(Terrain terrain, XZ anchor, List<TemplateBlock> blocks, boolean follow) {
        var edits = new LinkedHashMap<Pos, String>();
        var problems = new LinkedHashSet<String>();
        // Only actual template columns form the footprint: no guessed circular pad.
        var bottom = new LinkedHashMap<XZ, Integer>();
        int top = 0;
        for (var b : blocks) {
            bottom.merge(new XZ(b.x, b.z), b.y, Math::min); top = Math.max(top, b.y);
        }
        int base = terrain.height(anchor.x, anchor.z) + 1;
        int cut = 0, fill = 0, maxCut = 0, maxFill = 0, lo = Integer.MAX_VALUE, hi = Integer.MIN_VALUE;
        for (var entry : bottom.entrySet()) {
            int x = anchor.x + entry.getKey().x, z = anchor.z + entry.getKey().z;
            int ground = terrain.height(x, z);
            lo = Math.min(lo, ground); hi = Math.max(hi, ground);
            int level = follow ? ground + 1 : base;
            int supportY = level + entry.getValue() - 1;
            int c = Math.max(0, ground - supportY), f = Math.max(0, supportY - ground);
            cut += c; fill += f; maxCut = Math.max(maxCut, c); maxFill = Math.max(maxFill, f);
            if (terrain.water(x, z)) problems.add("Structure footprint crosses water.");
            for (int y = ground + 1; y <= supportY; y++)
                put(terrain, edits, problems, new Pos(x, y, z), "minecraft:stone_bricks");
            // Clear the entire occupied column to preserve interiors of sparse NBT templates.
            for (int y = level + entry.getValue(); y <= Math.max(ground, level + top); y++)
                put(terrain, edits, problems, new Pos(x, y, z), AIR);
        }
        for (var b : blocks) {
            int x = anchor.x + b.x, z = anchor.z + b.z;
            int y = (follow ? terrain.height(x, z) + 1 : base) + b.y;
            put(terrain, edits, problems, new Pos(x, y, z), b.data);
        }
        double moved = (cut + fill) / (double) Math.max(1, bottom.size());
        if (moved > 10) problems.add("Cut/fill exceeds prototype limit 10 blocks per footprint column.");
        if (maxCut > 12 || maxFill > 12) problems.add("Local cut/fill exceeds prototype limit 12 blocks.");
        // Useful to SEE why column projection breaks rigid buildings, but never certify it.
        if (follow && hi != lo) problems.add("Terrain-following deforms this rigid structure; preview only.");
        if (edits.size() > 60000) problems.add("Operation exceeds 60000-block lab limit.");
        return new Plan(edits, new ArrayList<>(problems), cut, fill, maxCut, maxFill,
                hi - lo, 0, 0, List.of(anchor),Map.of());
    }

    private record QueueNode(XZ pos, double cost, double priority) {}

    /** A cardinal corridor: its steps have an unambiguous stair facing and no diagonal gaps. */
    public static List<XZ> corridor(Terrain terrain, XZ from, XZ to) {
        if (Math.abs(from.x - to.x) + Math.abs(from.z - to.z) > 192)
            throw new IllegalArgumentException("Path endpoints must be within 192 Manhattan blocks.");
        int minX = Math.min(from.x, to.x) - 12, maxX = Math.max(from.x, to.x) + 12;
        int minZ = Math.min(from.z, to.z) - 12, maxZ = Math.max(from.z, to.z) + 12;
        var queue = new PriorityQueue<QueueNode>(Comparator.comparingDouble(QueueNode::priority));
        var cost = new HashMap<XZ, Double>(); var previous = new HashMap<XZ, XZ>();
        cost.put(from, 0.0); queue.add(new QueueNode(from, 0, 0));
        int visited = 0;
        while (!queue.isEmpty() && visited++ < 40000) {
            var node = queue.remove();
            if (node.cost > cost.getOrDefault(node.pos, Double.POSITIVE_INFINITY)) continue;
            if (node.pos.equals(to)) {
                var path = new ArrayList<XZ>(); XZ at = to;
                while (at != null) { path.add(at); at = previous.get(at); }
                Collections.reverse(path); return path;
            }
            for (var next : neighbors(node.pos)) {
                if (next.x < minX || next.x > maxX || next.z < minZ || next.z > maxZ) continue;
                int rise = Math.abs(terrain.height(next.x, next.z) - terrain.height(node.pos.x, node.pos.z));
                if (rise > 12) continue;
                double candidate = node.cost + 1 + rise * 3 + (terrain.water(next.x, next.z) ? 5 : 0);
                if (candidate >= cost.getOrDefault(next, Double.POSITIVE_INFINITY)) continue;
                cost.put(next, candidate); previous.put(next, node.pos);
                double heuristic = Math.abs(next.x - to.x) + Math.abs(next.z - to.z);
                queue.add(new QueueNode(next, candidate, candidate + heuristic));
            }
        }
        throw new IllegalArgumentException("No bounded corridor found; choose closer waypoints around the obstacle.");
    }

    static List<XZ> neighbors(XZ p) {
        return List.of(new XZ(p.x+1,p.z), new XZ(p.x-1,p.z), new XZ(p.x,p.z+1), new XZ(p.x,p.z-1));
    }

    /**
     * A height profile that meets its anchors and that a walker can climb.
     *
     * This exists twice: here, and in the offline compiler's
     * {@code terrain_harvest/routes.py}. Neither can call the other -- the
     * compiler authors Routes into a world file without a server, and the lab
     * fits paths at runtime against live blocks -- so both run
     * {@code fixtures/route-profile-crosscheck.json} and must agree. See
     * {@code RouteProfileCrossCheckTest}.
     *
     * <h2>This copy was the correct one</h2>
     *
     * The two disagreed on 86% of profiles, and the difference was anchors:
     * this copy pins them, the Python copy did not. Pinning is right -- a
     * median takes an endpoint's height from its neighbours, so an unpinned
     * profile can begin three blocks off the thing the path is meant to meet.
     *
     * Measured over 2,000 random profiles, this implementation produced an
     * unclimbable step on <b>no feasible input</b>. The cases where it did
     * were exactly the cases where two anchors stand further apart than the
     * columns between them allow, and no walkable profile exists at all.
     *
     * What was missing from BOTH copies was saying so. Infeasibility was
     * absorbed silently into whichever end happened to lose, which reads as a
     * cliff at a doorway. The compiler now reports it; see
     * {@code routes.anchors_reachable}.
     */
    public static int[] profile(int[] raw) {
        int[] out = raw.clone();
        for (int i = 0; i < raw.length; i++) {
            int[] window = Arrays.copyOfRange(raw, Math.max(0, i-3), Math.min(raw.length, i+4));
            Arrays.sort(window); out[i] = window[window.length / 2];
        }
        // Endpoint heights are fixed: do not produce a path disconnected from its anchors.
        if (raw.length > 0) { out[0] = raw[0]; out[out.length-1] = raw[raw.length-1]; }
        for (int pass = 0; pass < 3; pass++) {
            // From 1, and TO length-1: the interior is clamped against the
            // pinned ends rather than around them.
            for (int i = 1; i < out.length-1; i++) out[i] = clamp(out[i], out[i-1]);
            for (int i = out.length-2; i > 0; i--) out[i] = clamp(out[i], out[i+1]);
        }
        return out;
    }
    private static int clamp(int value, int neighbor) { return Math.max(neighbor-1, Math.min(neighbor+1, value)); }

    public static Plan path(Terrain terrain, XZ from, XZ to) {
        var line = corridor(terrain, from, to);
        int[] raw = line.stream().mapToInt(p -> terrain.height(p.x, p.z)).toArray(), height = profile(raw);
        var edits = new LinkedHashMap<Pos, String>(); var problems = new LinkedHashSet<String>();
        var modules = new EnumMap<Module,Integer>(Module.class);
        int cut = 0, fill = 0, maxCut = 0, maxFill = 0, steps = 0, bridges = 0;
        for (int i = 0; i < line.size(); i++) {
            var here = line.get(i);
            int difference = height[i] - raw[i];
            cut += Math.max(0,-difference); fill += Math.max(0,difference);
            maxCut = Math.max(maxCut, Math.max(0,-difference)); maxFill = Math.max(maxFill, Math.max(0,difference));
            String surface = terrain.water(here.x,here.z) ? "minecraft:oak_planks" : "minecraft:dirt_path";
            if (terrain.water(here.x,here.z)) bridges++;
            // Place a stair on the lower column toward its higher neighbor.
            XZ up = null;
            if (i > 0 && height[i-1] > height[i]) up = line.get(i-1);
            if (i+1 < line.size() && height[i+1] > height[i]) {
                if (up != null && !up.equals(line.get(i+1))) problems.add("One-column valley needs a landing; choose another corridor.");
                up = line.get(i+1);
            }
            int rise = i == 0 ? 0 : Math.abs(height[i] - height[i-1]);
            if (rise > 1) problems.add("Endpoint transition exceeds a one-block step.");
            if (up != null) {
                surface = "minecraft:stone_brick_stairs[facing=" + facing(here,up) + ",half=bottom,shape=straight,waterlogged=false]";
                steps++;
            }
            int surfaceY = height[i];
            Module module = up!=null ? Module.STAIR : terrain.water(here.x,here.z) ? Module.BRIDGE
                    : i==0||i==line.size()-1 ? Module.LANDING
                    : line.get(i-1).x!=line.get(i+1).x && line.get(i-1).z!=line.get(i+1).z ? Module.CORNER : Module.STRAIGHT;
            modules.merge(module,1,Integer::sum);
            for (int y = raw[i]+1; y < surfaceY; y++)
                put(terrain,edits,problems,new Pos(here.x,y,here.z),"minecraft:coarse_dirt");
            put(terrain,edits,problems,new Pos(here.x,surfaceY,here.z),surface);
            for (int y = surfaceY+1; y <= Math.max(raw[i],surfaceY+3); y++)
                put(terrain,edits,problems,new Pos(here.x,y,here.z),AIR);
        }
        if (maxCut > 2 || maxFill > 2) problems.add("Path adjustment exceeds prototype limit 2 blocks; add waypoints or reroute.");
        int lo = Arrays.stream(raw).min().orElse(0), hi = Arrays.stream(raw).max().orElse(0);
        return new Plan(edits,new ArrayList<>(problems),cut,fill,maxCut,maxFill,hi-lo,steps,bridges,line,modules);
    }

    static String facing(XZ lower, XZ higher) {
        if (higher.x > lower.x) return "east"; if (higher.x < lower.x) return "west";
        return higher.z > lower.z ? "south" : "north";
    }
    private static void put(Terrain terrain, Map<Pos,String> edits, Set<String> problems, Pos p, String data) {
        if (terrain.protectedAt(p)) problems.add("Footprint intersects protected or existing authored blocks.");
        edits.put(p,data);
    }
}
