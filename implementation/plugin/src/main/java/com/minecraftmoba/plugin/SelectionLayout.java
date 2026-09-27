package com.minecraftmoba.plugin;

/**
 * Where the summoned options sit, derived from how many there are.
 *
 * Three slots, because N is always three -- three Task trees, three ability
 * branches. But the width requirement is <b>derived from the slot count</b>
 * rather than fixed as a block span, so a future two- or four-option choice
 * does not silently break the space rule by asking for a gap sized for three.
 *
 * Pure arithmetic over doubles: no world, no entities, testable without a
 * server. {@link InWorldSelection} turns these offsets into locations.
 */
public final class SelectionLayout {

    private SelectionLayout() {}

    /**
     * Offsets across the row, centred on zero.
     *
     * Three slots at spacing 1.0 give {-1, 0, +1}; two give {-0.5, +0.5}. The
     * centre option therefore sits exactly at the aim point for any odd count,
     * which is what makes "look at open space" mean the space you are looking
     * at rather than one beside it.
     */
    public static double[] offsets(int slots, double spacing) {
        if (slots <= 0) throw new IllegalArgumentException("A choice needs at least one option.");
        if (spacing <= 0) throw new IllegalArgumentException("Option spacing must be positive.");
        double[] out = new double[slots];
        double centre = (slots - 1) / 2.0;
        for (int i = 0; i < slots; i++) out[i] = (i - centre) * spacing;
        return out;
    }

    /**
     * How wide a clear space the summon needs, in blocks.
     *
     * The span between the outermost options, plus one block so each end option
     * has its own block rather than sitting on the boundary of the last clear
     * one. A single option still needs a block to occupy, which is why this
     * never returns zero.
     */
    public static double requiredWidth(int slots, double spacing) {
        if (slots <= 0) throw new IllegalArgumentException("A choice needs at least one option.");
        if (spacing <= 0) throw new IllegalArgumentException("Option spacing must be positive.");
        return (slots - 1) * spacing + 1;
    }

    /**
     * How many blocks either side of the aim point must be clear.
     *
     * Ceiling rather than exact half-width: block checks are integers, and
     * rounding down would approve a space one block short at every even count.
     */
    public static int clearRadiusBlocks(int slots, double spacing) {
        return (int) Math.ceil(requiredWidth(slots, spacing) / 2.0);
    }
}
