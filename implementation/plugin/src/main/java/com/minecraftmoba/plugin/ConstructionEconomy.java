package com.minecraftmoba.plugin;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * What actually limits a Construction player: not how fast they can place
 * blocks, but how fast anyone can make the blocks and get them there.
 *
 * <h2>Every construction material is manufactured</h2>
 *
 * {@link MaterialCategories} admits bricks, mud bricks, terracotta and its
 * dyed variants, glass and stained glass, and concrete. <b>None of them is
 * mined.</b> Each is the output of a production chain, so the 20 WP award for
 * a construction placement is paid for work that happened upstream, minutes
 * earlier, usually by somebody else.
 *
 * A model that treats placements-per-minute as a free parameter is therefore
 * asking the wrong question. It answers "if a builder could place twelve a
 * minute forever, how long to the cap" when the real question is whether
 * anyone can supply twelve a minute at all.
 *
 * <h2>The furnace is the gate, and it is not uniform</h2>
 *
 * A furnace smelts one item per ten seconds, so one furnace is six items a
 * minute. But the number of smelts <i>per placed block</i> differs by a factor
 * of four across the category:
 *
 * <ul>
 *   <li><b>Glass</b> -- one sand, one smelt, one block.</li>
 *   <li><b>Terracotta</b> -- one clay block, one smelt, one block.</li>
 *   <li><b>Bricks</b> -- four clay balls, four smelts, then a craft. One block
 *       costs forty seconds of furnace time.</li>
 *   <li><b>Concrete</b> -- <b>no furnace at all.</b> Four sand, four gravel and
 *       one dye craft eight powder, which hardens on contact with water.</li>
 * </ul>
 *
 * That spread is the most consequential fact in the category, and it is why a
 * Construction exemplar built on concrete is the sensible one: concrete is the
 * only construction material whose supply is not rationed by furnace time.
 *
 * <h2>Carry is the second gate, and it moves with level</h2>
 *
 * Even a well-supplied builder brings blocks from where they were made to
 * where they go, and a trip carries {@code unlockedSlots * 64}. Slots are a
 * Growth stat, so the logistics ceiling <b>rises as the player levels</b> --
 * which makes construction progression a compounding loop rather than a
 * constant rate, and is precisely the shape a flat policy cannot express.
 */
public final class ConstructionEconomy {

    /** Vanilla: one item per ten seconds in an ordinary furnace. */
    public static final double SMELT_SECONDS = 10.0;
    public static final int STACK = 64;

    /** How a material reaches a placeable block. */
    public enum Route {
        /** One smelt per block: glass, terracotta. */
        GLASS(1), TERRACOTTA(1),
        /** Four smelts per block: four clay balls to one bricks block. */
        BRICKS(4),
        /** No furnace. Rationed by dye rather than by heat. */
        CONCRETE(0);

        public final int smeltsPerBlock;
        Route(int smeltsPerBlock) { this.smeltsPerBlock = smeltsPerBlock; }
    }

    /** Which stage is actually limiting, which is the only interesting output. */
    public enum Binding { SUPPLY, CARRY }

    public record Limit(Binding binding, double blocksPerMinute, double supply, double carry) {}

    private final Route route;
    private final double furnaces;
    private final double concreteBatchesPerMinute;
    private final double tripMinutes;

    /**
     * @param furnaces                 furnaces kept continuously fed
     * @param concreteBatchesPerMinute crafts per minute on the concrete route,
     *                                 each yielding eight blocks; this is a
     *                                 claim about dye supply, not about crafting speed
     * @param tripMinutes              out, back, and placing a load
     */
    public ConstructionEconomy(Route route, double furnaces,
                               double concreteBatchesPerMinute, double tripMinutes) {
        this.route = route;
        this.furnaces = furnaces;
        this.concreteBatchesPerMinute = concreteBatchesPerMinute;
        this.tripMinutes = tripMinutes;
    }

    /** Placeable blocks per minute the production chain can emit. */
    public double supplyPerMinute() {
        if (route == Route.CONCRETE) return concreteBatchesPerMinute * 8;
        double smeltsPerMinute = furnaces * (60.0 / SMELT_SECONDS);
        return smeltsPerMinute / route.smeltsPerBlock;
    }

    /** Placeable blocks per minute the player can physically ferry at this level. */
    public double carryPerMinute(int unlockedSlots) {
        if (tripMinutes <= 0) return Double.MAX_VALUE;
        return (unlockedSlots * (double) STACK) / tripMinutes;
    }

    /**
     * The binding constraint and the rate it implies.
     *
     * Reported rather than assumed, because "which stage is limiting" is the
     * question a Construction player is actually asking, and a single
     * placements-per-minute figure hides it.
     */
    public Limit limit(int unlockedSlots) {
        double supply = supplyPerMinute(), carry = carryPerMinute(unlockedSlots);
        return supply <= carry
                ? new Limit(Binding.SUPPLY, supply, supply, carry)
                : new Limit(Binding.CARRY, carry, supply, carry);
    }

    /** Furnaces needed to keep a builder supplied at a target rate. */
    public static double furnacesFor(Route route, double blocksPerMinute) {
        if (route == Route.CONCRETE) return 0;
        return blocksPerMinute * route.smeltsPerBlock * SMELT_SECONDS / 60.0;
    }

    /** Furnace-seconds embodied in one placed block, which is the comparison that matters. */
    public static Map<Route, Double> furnaceSecondsPerBlock() {
        var out = new LinkedHashMap<Route, Double>();
        for (Route r : Route.values()) out.put(r, r.smeltsPerBlock * SMELT_SECONDS);
        return out;
    }
}
