package com.minecraftmoba.plugin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * What to draw so a tester can see where a placement will land.
 *
 * <h2>Drawing every affected block does not work</h2>
 *
 * {@link TargetPreview} sends particles to one player, which is right -- a
 * preview is the caster's business -- but it means the cost is per block per
 * cadence. A 200-column route touching three columns wide is 600 particles
 * every preview tick, for one viewer, for as long as they are deciding. That
 * is not a budget problem to tune later; it is the difference between a
 * usable preview and a white fog where the terrain used to be.
 *
 * <h2>So draw the shape, not the volume</h2>
 *
 * A placement's meaning is in its <b>boundary and its decisions</b>: where a
 * route turns, where it changes piece, where a footprint's corners are. The
 * interior is implied by them, and a viewer reads an outline faster than a
 * solid anyway.
 *
 * Reduction happens before any Bukkit call, so what the budget costs is
 * measurable without a server.
 */
public final class ChamberPreview {

    /** Particles one viewer is asked to render per preview tick. */
    public static final int DEFAULT_BUDGET = 120;

    /**
     * The outline of a route: its ends, and every column where the path
     * changes character.
     *
     * Those are exactly the points a tester is deciding about. A straight run
     * of forty identical columns tells them nothing the first and last do not,
     * and costs forty particles to say it.
     */
    public static List<Chamber.At> route(List<Chamber.At> centreline, List<String> characters) {
        var out = new ArrayList<Chamber.At>();
        if (centreline == null || centreline.isEmpty()) return out;
        out.add(centreline.get(0));
        for (int i = 1; i < centreline.size(); i++) {
            boolean changed = characters != null && i < characters.size()
                    && !characters.get(i).equals(characters.get(i - 1));
            if (changed) out.add(centreline.get(i));
        }
        Chamber.At last = centreline.get(centreline.size() - 1);
        if (!out.get(out.size() - 1).equals(last)) out.add(last);
        return out;
    }

    /**
     * The outline of a volume: its edges only.
     *
     * A filled 9x5x9 footprint is 405 particles and reads as a block of fog.
     * Its twelve edges are legible and cost a fraction, and a viewer judging
     * "does this fit here" is judging the extent.
     */
    public static List<Chamber.At> outline(Collection<Chamber.At> volume) {
        var out = new LinkedHashSet<Chamber.At>();
        if (volume == null || volume.isEmpty()) return List.of();
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        java.util.UUID world = null;
        for (Chamber.At at : volume) {
            world = at.world();
            minX = Math.min(minX, at.x()); maxX = Math.max(maxX, at.x());
            minY = Math.min(minY, at.y()); maxY = Math.max(maxY, at.y());
            minZ = Math.min(minZ, at.z()); maxZ = Math.max(maxZ, at.z());
        }
        for (int x : new int[]{minX, maxX})
            for (int y : new int[]{minY, maxY})
                for (int z = minZ; z <= maxZ; z++) out.add(new Chamber.At(world, x, y, z));
        for (int z : new int[]{minZ, maxZ})
            for (int y : new int[]{minY, maxY})
                for (int x = minX; x <= maxX; x++) out.add(new Chamber.At(world, x, y, z));
        for (int x : new int[]{minX, maxX})
            for (int z : new int[]{minZ, maxZ})
                for (int y = minY; y <= maxY; y++) out.add(new Chamber.At(world, x, y, z));
        return new ArrayList<>(out);
    }

    /**
     * Thin a list to a budget, keeping the ends.
     *
     * Even an outline can exceed what one viewer should render, and a preview
     * that silently drops its tail would mislead about where a placement
     * stops. Keeping the first and last and sampling between them degrades the
     * DETAIL rather than the EXTENT, which is the property a tester is
     * actually reading.
     */
    public static List<Chamber.At> withinBudget(List<Chamber.At> points, int budget) {
        if (points == null || points.isEmpty() || budget <= 0) return List.of();
        if (points.size() <= budget) return points;
        var out = new ArrayList<Chamber.At>(budget);
        double stride = (points.size() - 1) / (double) (budget - 1);
        for (int i = 0; i < budget; i++)
            out.add(points.get((int) Math.round(i * stride)));
        return out;
    }

    private ChamberPreview() {}
}
