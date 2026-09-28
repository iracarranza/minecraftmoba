package com.minecraftmoba.plugin;

import java.util.List;
import java.util.Objects;

/**
 * A charge: how far along it is, and what that currently means.
 *
 * <h2>Why bands rather than a curve</h2>
 *
 * A charging ability is not usually a continuous scale. It has <b>thresholds</b>
 * -- a minimum worth releasing at, a point where it gains a second effect, a
 * full charge -- and the player's real question is which side of the next one
 * they are on. So a charge carries an ordered list of bands, each with the
 * fraction it begins at and the colour it reads as, and the meter changes
 * colour when one is crossed.
 *
 * The bands are per ABILITY. A dash that is only worth releasing past a third
 * and a projectile that gains pierce at four fifths are different shapes, and
 * neither is served by a shared ladder.
 *
 * <h2>Rate is the full duration, not a speed</h2>
 *
 * Abilities charge at different rates, expressed as how many ticks a full
 * charge takes. That keeps "full" meaning the same thing everywhere and puts
 * the difference in one number an author can read off a table -- a speed would
 * make the endpoint depend on arithmetic.
 *
 * <h2>Pure</h2>
 *
 * No Bukkit, no world. The tick is passed in, exactly as {@link CombatState}
 * and {@link SelectionBeat} do, so the arithmetic is testable without a server
 * and the same numbers can be checked against a table.
 */
public record Charge(long startTick, long fullTicks, List<Band> bands) {

    /**
     * One threshold and what it looks like.
     *
     * @param from   the fraction this band begins at, 0..1
     * @param colour a hex colour the HUD tints the meter with
     * @param label  what the band is called, for the line beside the meter
     */
    public record Band(double from, String colour, String label) {
        public Band {
            if (from < 0 || from > 1) throw new IllegalArgumentException(
                    "A band begins somewhere in 0..1, not at " + from);
            Objects.requireNonNull(colour, "A band must say what colour it reads as.");
            Objects.requireNonNull(label, "A band must be nameable.");
        }
    }

    public Charge {
        if (fullTicks <= 0) throw new IllegalArgumentException(
                "A full charge takes some time; " + fullTicks + " ticks is not a charge.");
        bands = bands == null ? List.of() : List.copyOf(bands);
        // Sorted and starting at zero, so band() is a scan rather than a
        // search with a fallback. An unsorted list would silently return
        // whichever band happened to be last below the fraction.
        for (int i = 1; i < bands.size(); i++)
            if (bands.get(i).from() <= bands.get(i - 1).from())
                throw new IllegalArgumentException(
                        "Bands must ascend; " + bands.get(i).from()
                                + " does not follow " + bands.get(i - 1).from());
        if (!bands.isEmpty() && bands.getFirst().from() != 0)
            throw new IllegalArgumentException(
                    "The first band must begin at 0, or a fresh charge has no colour.");
    }

    /** How far along, 0..1. Clamped, so a charge held past full simply reads full. */
    public double progress(long now) {
        if (now <= startTick) return 0;
        return Math.min(1.0, (double) (now - startTick) / fullTicks);
    }

    public boolean complete(long now) { return progress(now) >= 1.0; }

    /** The band a fraction falls in, or null when none are authored. */
    public Band band(double progress) {
        Band found = null;
        for (Band band : bands) {
            if (band.from() > progress) break;
            found = band;
        }
        return found;
    }

    public Band band(long now) { return band(progress(now)); }

    /**
     * Whole percent, for the readout.
     *
     * Floored rather than rounded, so a meter never shows 100% before the
     * charge is actually complete -- which is the one reading a player would
     * act on and be wrong about.
     */
    public static int percent(double progress) {
        return (int) Math.floor(Math.max(0, Math.min(1, progress)) * 100);
    }
}
