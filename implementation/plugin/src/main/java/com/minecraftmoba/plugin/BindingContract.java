package com.minecraftmoba.plugin;

import org.bukkit.World;

import java.util.ArrayList;
import java.util.List;

/**
 * Can a match actually be played on what was just bound?
 *
 * This is the runtime half of `terrain_harvest/readiness.py`, and it exists
 * because the compiler-side half was not enough. `readiness.certify` makes a
 * map unable to reach READY with an unmanifested Lair -- but a match does not
 * have to come from the pool. When the pool is empty, or a claim fails, the
 * selection falls back to the configured template, and the Alpha template's
 * `alpha.lair.site` is EMPTY BY DESIGN. So the one state the whole pool
 * contract exists to prevent was reachable in one step by the path that
 * bypasses the pool:
 *
 *   pool enabled but empty -> fall back to template -> lair=UNCONFIGURED
 *                          -> six-night cadence with nothing at night 2
 *
 * Certifying the realization instead of the pool entry closes that, because
 * the check is on what got bound rather than on where it came from.
 *
 * The codes deliberately match readiness.py's, so a failure reads the same on
 * both sides of the pipeline.
 */
public final class BindingContract {

    /** The three Worksite nights consume this many sites; activation is permanent. */
    public static final int MIN_WORKSITES = 7;

    public record Problem(String code, String detail) {
        @Override public String toString() { return code + ": " + detail; }
    }

    private BindingContract() {}

    /**
     * @param bindings the claimed realization's manifest, or null for the template
     */
    public static List<Problem> certify(MobaPlugin plugin, World world, MapBindings bindings) {
        List<Problem> problems = new ArrayList<>();
        if (world == null) {
            problems.add(new Problem("NO_WORLD_IDENTITY", "no world was bound"));
            return problems;
        }
        for (Team t : Team.values()) {
            if (bindings == null || bindings.fountain(world, t) == null)
                problems.add(new Problem("NO_FOUNTAIN",
                        t.lower() + " has no Aether Fountain position, so respawn and "
                        + "reconstruction cannot bind"));
        }
        var objectives = plugin.teamObjectives();
        if (objectives != null)
            for (Team t : Team.values())
                for (TeamObjectives.Kind k : TeamObjectives.Kind.values())
                    if (objectives.site(t, k) == null)
                        problems.add(new Problem("NO_OBJECTIVE_BINDING",
                                t.lower() + "/" + k + " has no position"));
        var lair = plugin.lair();
        if (lair == null || lair.report().contains("UNCONFIGURED"))
            problems.add(new Problem("LAIR_UNCONFIGURED",
                    "the Lair has no site, so the cadence reaches UNCONFIGURED on night 2 "
                    + "and three of the six opportunity nights do nothing"));
        var worksites = plugin.worksites();
        int sites = worksites == null ? 0 : worksites.all().size();
        if (sites < MIN_WORKSITES)
            problems.add(new Problem("WORKSITE_PORTFOLIO_TOO_SMALL",
                    "activation is permanent, so the eligible pool only shrinks; the three "
                    + "Worksite nights consume " + MIN_WORKSITES + " sites and this "
                    + "realization offers " + sites));
        return problems;
    }

    /**
     * Whether an operator has explicitly allowed playing on an incomplete
     * realization.
     *
     * Off by default. It exists so deliberate partial testing on the template
     * stays possible without reinstating a SILENT fallback -- the difference
     * that matters is that someone had to write it down.
     */
    public static boolean incompleteAllowed(MobaPlugin plugin) {
        return plugin.getConfig().getBoolean("alpha.pool.allowIncompleteBinding", false);
    }

    public static String describe(List<Problem> problems) {
        var out = new StringBuilder();
        for (Problem p : problems) out.append("\n  - ").append(p);
        return out.toString();
    }
}
