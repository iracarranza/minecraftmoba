package com.minecraftmoba.plugin;

import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Telling a player what an ability is missing. The formatting is pure; the checks are the abilities' own
 * ({@link Ability#needs}).
 */
public final class AbilityNeeds {
    private AbilityNeeds() {}

    /** The needs that are not met, each as the text the player reads ("4 Sand (you have 0)"). */
    public static List<String> unmet(List<Ability.Need> needs, Player p, Ability.AbilityContext ctx) {
        var out = new ArrayList<String>();
        for (var n : needs) {
            boolean met;
            try { met = n.met().test(p, ctx); } catch (RuntimeException ex) { continue; }   // a need that cannot be checked is not reported
            if (met) continue;
            String have = n.have() == null ? null : n.have().apply(p, ctx);
            out.add(n.label() + (have == null || have.isBlank() ? "" : " (" + have + ")"));
        }
        return out;
    }

    /** "Food Fight needs food in your inventory." / "Concrete Shoes needs Concrete to carry, and a block to land on." */
    public static String sentence(String abilityName, List<String> unmet) {
        if (unmet.isEmpty()) return null;
        return abilityName + " needs " + join(unmet) + ".";
    }

    /** The short form for the HUD line: the first need only. */
    public static String hud(List<String> unmet) { return unmet.isEmpty() ? null : "Needs " + unmet.get(0); }

    static String join(List<String> parts) {
        if (parts.size() == 1) return parts.get(0);
        if (parts.size() == 2) return parts.get(0) + " and " + parts.get(1);
        return String.join(", ", parts.subList(0, parts.size() - 1)) + ", and " + parts.get(parts.size() - 1);
    }
}
