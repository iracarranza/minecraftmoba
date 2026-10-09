package com.minecraftmoba.plugin;

import org.bukkit.Material;

import java.util.List;

/**
 * The lab room's way into every bench: where each pedestal stands, what it is, and when it
 * may be used. Pure, so that "every chamber is reachable, and nothing collides" is a test.
 *
 * Until this existed a tester could only reach a chamber by already knowing its command.
 * The room now has one pedestal per bench, and the setup menu has a matching Benches page,
 * so the same list serves a tester who walks and one who opens a menu.
 *
 * The terrain chamber is listed with its requirement rather than hidden: it needs a
 * launched scoop, and a bench that quietly disappears reads as missing.
 */
public final class LabHub {
    private LabHub() {}

    /** What a bench needs in order to be entered. */
    public enum Needs { NOTHING, LAUNCHED_SCOOP }

    public record Bench(String id, String label, Material pedestal, int x, int z, Needs needs, String blurb) {}

    public record State(boolean scoopLaunched) {}

    /** Row along the room's north side, clear of the three existing pedestals. */
    public static final List<Bench> BENCHES = List.of(
            new Bench("combat", "Combat chamber", Material.NETHERITE_BLOCK, -6, -7, Needs.NOTHING,
                    "Exercise abilities against a measurable player dummy."),
            new Bench("legibility", "Legibility bench", Material.LODESTONE, -2, -7, Needs.NOTHING,
                    "How a state reads at a distance: scale, glow, armor, particles."),
            new Bench("opportunity", "Opportunity bench", Material.HAY_BLOCK, 2, -7, Needs.NOTHING,
                    "Watch a Herd, Crop Patch or Swarm manifest, resolve and recover."),
            new Bench("terrain", "Terrain chamber", Material.GRASS_BLOCK, 6, -7, Needs.LAUNCHED_SCOOP,
                    "Place objectives, renewables and routes on certified or random terrain."));

    /** The pedestals the room already has, so a new one never lands on them. */
    public static final List<int[]> EXISTING_PEDESTALS = List.of(new int[]{-5, 0}, new int[]{5, 0}, new int[]{0, 6});

    public static Bench bench(String id) {
        for (Bench b : BENCHES) if (b.id().equals(id)) return b;
        return null;
    }

    public static Bench atBlock(Material m) {
        for (Bench b : BENCHES) if (b.pedestal() == m) return b;
        return null;
    }

    /** Null when the bench may be entered; otherwise why not. */
    public static String refusal(Bench b, State s) {
        if (b.needs() == Needs.LAUNCHED_SCOOP && !s.scoopLaunched())
            return b.label() + " needs a launched scoop: choose a class and a scoop, then Launch test.";
        return null;
    }
}
