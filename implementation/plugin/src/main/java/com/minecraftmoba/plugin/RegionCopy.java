package com.minecraftmoba.plugin;

/**
 * How a bay's columns map onto a source world's, decided without a server.
 *
 * The bay is a fixed slab of the lab world; a source is somewhere else entirely
 * and sits at whatever height its terrain happens to. Copying the same Y range
 * would show a mountain as a cut slab, so the source is shifted vertically so
 * its surface at the sampling centre lands a fixed distance above the bay's
 * floor. Horizontal alignment is a plain offset from the bay's centre column.
 */
public record RegionCopy(int bayCentreX, int bayCentreZ, int bayFloorY,
                         int sourceCentreX, int sourceCentreZ, int sourceSurfaceY) {

    /** Surface lands this far above the bay floor, leaving some ground below it. */
    public static final int SURFACE_ABOVE_FLOOR = 8;

    public static RegionCopy of(Chamber bay, int sourceCentreX, int sourceCentreZ, int sourceSurfaceY) {
        return new RegionCopy((bay.minX() + bay.maxX()) / 2, (bay.minZ() + bay.maxZ()) / 2,
                bay.minY(), sourceCentreX, sourceCentreZ, sourceSurfaceY);
    }

    public int sourceX(int bayX) { return sourceCentreX + (bayX - bayCentreX); }
    public int sourceZ(int bayZ) { return sourceCentreZ + (bayZ - bayCentreZ); }

    /** The source Y that supplies a given bay Y. */
    public int sourceY(int bayY) {
        return sourceSurfaceY - SURFACE_ABOVE_FLOOR + (bayY - bayFloorY);
    }
}
