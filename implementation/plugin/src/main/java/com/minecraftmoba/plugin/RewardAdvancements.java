package com.minecraftmoba.plugin;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.advancement.Advancement;
import org.bukkit.entity.Player;
import java.util.*;

/**
 * Grants the reward advancement tree as a *display* of what was chosen.
 *
 * The authority is ChoiceRecord in PlayerData and the task tiers in
 * TaskEffects. This class only mirrors them, so a missing, corrupt or
 * uninstalled datapack removes the picture and never changes a player's actual
 * capacity.
 *
 * Disable with features.rewardAdvancements.enabled.
 */
public final class RewardAdvancements {
    private final MobaPlugin plugin;

    public RewardAdvancements(MobaPlugin plugin) { this.plugin = plugin; }

    public boolean enabled() { return plugin.getConfig().getBoolean("features.rewardAdvancements.enabled"); }

    private Advancement find(String node) {
        return Bukkit.getAdvancement(new NamespacedKey("moba", "rewards/" + node));
    }

    public void grant(Player p, String node) {
        if (!enabled()) return;
        Advancement adv = find(node);
        // An absent advancement means the datapack is not installed. That is a
        // missing display, not an error worth interrupting progression for.
        if (adv == null) return;
        var progress = p.getAdvancementProgress(adv);
        for (String criterion : progress.getRemainingCriteria()) progress.awardCriteria(criterion);
    }

    public void revoke(Player p, String node) {
        Advancement adv = find(node);
        if (adv == null) return;
        var progress = p.getAdvancementProgress(adv);
        for (String criterion : progress.getAwardedCriteria()) progress.revokeCriteria(criterion);
    }

    /** Re-mirrors everything the player has actually earned. */
    public void sync(Player p) {
        if (!enabled()) return;
        grant(p, "root");
        var d = plugin.data(p);
        if (d == null) return;
        var task = plugin.taskEffects();
        if (task != null) {
            for (var domain : TaskEffects.Domain.values()) {
                int tier = task.tier(d, domain);
                for (int t = 1; t <= tier; t++) grant(p, domain.name().toLowerCase(Locale.ROOT) + "_" + t);
            }
        }
        for (var choice : d.choices) {
            String id = choice.choiceId();
            if (id != null && find(id) != null) grant(p, id);
        }
    }

    public void clearAll(Player p) {
        for (String node : List.of("root", "efficiency_1", "efficiency_2", "efficiency_3",
                "yield_1", "yield_2", "yield_3", "damage_1", "damage_2", "damage_3",
                "health_spec", "hunger_spec", "inventory_spec")) revoke(p, node);
    }
}
