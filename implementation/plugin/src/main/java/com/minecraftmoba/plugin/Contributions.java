package com.minecraftmoba.plugin;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import java.util.*;

/**
 * The Level 6 contribution fork, and team-owned Worksite opportunities.
 *
 * infrastructure.md, 19 September 2026: at Level 6 a player's authored
 * progression forks between contributing **one class-compatible Infrastructure
 * form** and **Monster Combat**. Monster Combat is the alternative to
 * contribution, not a fifth Infrastructure type, so it is modelled as a branch
 * of the same fork rather than as a form.
 *
 * Eligibility is authored per class and may be asymmetric: a class may have
 * several coherent options, one, or none. It is **not** inherited from
 * archetype tags, so this class reads an explicit per-class list and refuses
 * anything absent from it rather than deriving eligibility from anything else.
 *
 * Duplicate choices across teammates are legal and meaningful — concentrated
 * team depth, not a wasted unlock — so nothing here deduplicates.
 *
 * A shared Worksite opportunity is team-owned and distinct from any player's
 * class-bounded personal contribution. First capitalization earns it
 * permanently; later control of the Worksite can change hands without taking it
 * back.
 *
 * Unresolved in canon and therefore unimplemented: the full class x
 * infrastructure matrix, Monster Combat branch content, and how a shared
 * opportunity receives later quality or quantity advancement.
 *
 * Disable with features.contributions.enabled.
 */
public final class Contributions {
    /** The four established Infrastructure forms, plus the fork's other branch. */
    public enum Form { CONSTRUCT, DEVELOPMENT_ZONE, ROUTE, SUPPLY_LINE, MONSTER_COMBAT }

    public record Shared(String team, Form form, String earnedFromWorksite) {}

    private final MobaPlugin plugin;
    /** Team-owned opportunities, keyed by team, in the order they were earned. */
    private final Map<String, List<Shared>> shared = new LinkedHashMap<>();
    /** Worksites already capitalized; first capitalization is once only. */
    private final Set<String> capitalized = new HashSet<>();

    public Contributions(MobaPlugin plugin) { this.plugin = plugin; }

    public boolean enabled() { return plugin.getConfig().getBoolean("features.contributions.enabled"); }

    public int forkLevel() { return plugin.getConfig().getInt("progression.contributionForkLevel", 6); }

    /** Authored per class. An unlisted class has no options, which is legal. */
    public List<Form> eligible(String classId) {
        if (classId == null) return List.of();
        var raw = plugin.getConfig().getStringList("classes." + classId + ".infrastructureEligibility");
        var out = new ArrayList<Form>();
        for (String s : raw) {
            try { out.add(Form.valueOf(s.toUpperCase(Locale.ROOT))); }
            catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException("classes." + classId
                        + ".infrastructureEligibility has unknown form: " + s);
            }
        }
        return out;
    }

    public Form contributionOf(PlayerData d) {
        if (d == null || d.contribution == null) return null;
        return Form.valueOf(d.contribution);
    }

    /** Returns a refusal reason, or null on success. */
    public String choose(Player p, Form form) {
        if (!enabled()) return "features.contributions.enabled is false";
        var d = plugin.data(p);
        if (d == null) return "no player data";
        if (d.level < forkLevel()) return "the fork opens at level " + forkLevel();
        if (d.contribution != null) return "already chose " + d.contribution;
        var options = eligible(d.classId);
        if (options.isEmpty())
            return "class " + d.classId + " has no authored contribution options; eligibility is authored per class";
        if (!options.contains(form))
            return form + " is not eligible for " + d.classId + " (eligible: " + options + ")";
        d.contribution = form.name();
        p.sendMessage(ChatColor.AQUA + "Contribution chosen: " + ChatColor.WHITE + form);
        if (form == Form.MONSTER_COMBAT)
            p.sendMessage(ChatColor.GRAY + "Monster Combat is the alternative to Infrastructure contribution, "
                    + "not a fifth Infrastructure form.");
        return null;
    }

    /**
     * First capitalization of an activated Worksite earns the team one shared
     * opportunity, permanently. Later control of that Worksite can change
     * hands; the opportunity does not.
     */
    public String capitalize(String worksiteId, String team) {
        if (!enabled()) return "features.contributions.enabled is false";
        if (!capitalized.add(worksiteId))
            return "worksite " + worksiteId + " was already capitalized; first capture is once only";
        shared.computeIfAbsent(team, k -> new ArrayList<>()).add(new Shared(team, null, worksiteId));
        return null;
    }

    /**
     * Discard match-scoped contribution state (ALPHA-D2).
     *
     * Infrastructure is a recognized persistent contribution *within* a match,
     * not cross-match progression, so shared opportunities and the
     * first-capitalization record both end with the match. Without this, a
     * Worksite capitalized in match 1 is refused in match 2 as "already
     * capitalized" even though the world has been restored.
     *
     * A player's own Level 6 contribution choice lives in PlayerData and is
     * cleared with the rest of that player's match-scoped state.
     */
    public int reset() {
        int discarded = capitalized.size();
        shared.clear();
        capitalized.clear();
        return discarded;
    }

    /** Allocates an unassigned shared opportunity to any Infrastructure form. */
    public String allocate(String team, Form form) {
        if (form == Form.MONSTER_COMBAT) return "Monster Combat is not an Infrastructure form";
        var list = shared.get(team);
        if (list == null) return "team " + team + " has no shared opportunities";
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).form() == null) {
                list.set(i, new Shared(team, form, list.get(i).earnedFromWorksite()));
                return null;
            }
        }
        return "no unallocated shared opportunity for " + team;
    }

    public List<String> report() {
        var out = new ArrayList<String>();
        out.add("CONTRIB forkLevel=" + forkLevel() + " enabled=" + enabled());
        for (var team : shared.entrySet())
            for (var s : team.getValue())
                out.add("  SHARED team=" + s.team() + " form=" + (s.form() == null ? "UNALLOCATED" : s.form())
                        + " from=" + s.earnedFromWorksite());
        out.add("  worksitesCapitalized=" + capitalized.size());
        return out;
    }
}
