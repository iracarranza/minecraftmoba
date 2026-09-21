package com.minecraftmoba.plugin;

import java.util.ArrayList;
import java.util.List;

/**
 * The coarse authored ecological search region of a Regenerative Opportunity.
 *
 * A Region is the persistent strategic fact -- "this part of the map can
 * repeatedly produce sheep" -- and it is deliberately NOT origin plus radius.
 * That collapse is what made a fenced pen the natural reading: if the region,
 * the eligible area and the manifestation site are one number, the only thing
 * left to decide is what to build at it.
 *
 * A Region is a set of cells, because the worldgen analysis that justified an
 * opportunity works in cells, and because an ecology worth calling a region
 * often spans several adjacent ones. One cell is a case, not the rule.
 *
 * A Region enumerates nothing. It does not hold spawn nodes, and it is not a
 * visible gameplay zone. It answers exactly one question -- "is this column
 * inside the ecology?" -- and leaves every question about where a resource can
 * actually appear to a query against the CURRENT world.
 */
public final class OpportunityRegion {

    /** An authored analysis cell, inclusive on both bounds, unbounded in Y. */
    public record Cell(int minX, int minZ, int maxX, int maxZ) {
        public Cell {
            if (minX > maxX || minZ > maxZ)
                throw new IllegalArgumentException("cell bounds inverted: " + minX + ".." + maxX);
        }
        public boolean contains(int x, int z) {
            return x >= minX && x <= maxX && z >= minZ && z <= maxZ;
        }
        public int centreX() { return Math.floorDiv(minX + maxX, 2); }
        public int centreZ() { return Math.floorDiv(minZ + maxZ, 2); }
    }

    private final List<Cell> cells;

    public OpportunityRegion(List<Cell> cells) {
        if (cells.isEmpty()) throw new IllegalArgumentException("a region needs at least one cell");
        this.cells = List.copyOf(cells);
    }

    /** A square region around a point, for migrating authored origin+radius data. */
    public static OpportunityRegion square(int x, int z, int half) {
        return new OpportunityRegion(List.of(new Cell(x - half, z - half, x + half, z + half)));
    }

    public List<Cell> cells() { return cells; }

    public boolean contains(int x, int z) {
        for (Cell c : cells) if (c.contains(x, z)) return true;
        return false;
    }

    /**
     * Columns to test for eligibility, at a sampling stride.
     *
     * Sampled rather than exhaustive: a region is coarse by design and testing
     * every column of a large ecology per manifestation would cost far more
     * than the answer is worth. The stride is a fixture, not a claim about
     * geometry -- it trades the chance of missing a small eligible pocket
     * against the cost of the query.
     */
    public List<int[]> columns(int stride) {
        if (stride < 1) throw new IllegalArgumentException("stride must be positive");
        var out = new ArrayList<int[]>();
        for (Cell c : cells)
            for (int x = c.minX(); x <= c.maxX(); x += stride)
                for (int z = c.minZ(); z <= c.maxZ(); z += stride)
                    out.add(new int[]{x, z});
        return out;
    }

    /** Area in columns, for diagnostics and for sanity-checking authored data. */
    public long area() {
        long n = 0;
        for (Cell c : cells) n += (long) (c.maxX() - c.minX() + 1) * (c.maxZ() - c.minZ() + 1);
        return n;
    }

    @Override public String toString() {
        return "Region[" + cells.size() + " cell(s), " + area() + " columns]";
    }
}
