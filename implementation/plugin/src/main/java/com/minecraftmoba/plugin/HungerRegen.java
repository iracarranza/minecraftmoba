package com.minecraftmoba.plugin;

import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;

/**
 * Health regeneration expressed relative to maximum Hunger.
 *
 * **This fixes a live defect.** Vanilla natural regeneration requires a food
 * level of at least 18. Capacity caps food at effectiveHunger, which starts at
 * 9 on the canonical curve, so vanilla regeneration could never fire and a
 * player on the intended progression never healed naturally at all. The cap and
 * the vanilla threshold are both absolute, and they are incompatible.
 *
 * The rule is therefore relative: regeneration requires food within
 * `pointsBelowMax` of the player's own maximum. At vanilla's 20 that reproduces
 * the familiar 18 threshold; at a capped 9 it becomes 7, so the same "nearly
 * full keeps you healing" relationship holds at every stage of progression.
 *
 * The sprint cutoff is NOT implemented here. Vanilla already refuses to sprint
 * at 6 food or below, which is three drumsticks, and that threshold is absolute
 * by design: it is the fixed floor the Hunger reserve is measured against.
 *
 * Custom healing only runs where vanilla's cannot, so the two never stack.
 *
 * Disable with features.hungerRegen.enabled.
 */
public final class HungerRegen {
    /** Vanilla's own natural-regeneration food threshold. */
    public static final int VANILLA_REGEN_FOOD = 18;

    private final MobaPlugin plugin;
    private long healed;

    public HungerRegen(MobaPlugin plugin) {
        this.plugin = plugin;
        long period = plugin.getConfig().getLong("features.hungerRegen.intervalTicks", 80L);
        if (period > 0) Bukkit.getScheduler().runTaskTimer(plugin, this::tick, period, period);
    }

    public boolean enabled() { return plugin.getConfig().getBoolean("features.hungerRegen.enabled"); }

    public int pointsBelowMax() {
        return plugin.getConfig().getInt("features.hungerRegen.pointsBelowMax", 2);
    }

    /** The food level at or above which this player regenerates. */
    public int thresholdFor(Player p) {
        return Math.max(1, plugin.effectiveHunger(p) - pointsBelowMax());
    }

    public boolean eligible(Player p) {
        if (!enabled() || !plugin.enrolled(p)) return false;
        // Where vanilla can regenerate, leave it to vanilla rather than stacking.
        if (p.getFoodLevel() >= VANILLA_REGEN_FOOD) return false;
        if (p.getFoodLevel() < thresholdFor(p)) return false;
        var attr = p.getAttribute(Attribute.MAX_HEALTH);
        return attr != null && p.getHealth() < attr.getValue();
    }

    private void tick() {
        if (!enabled()) return;
        double amount = plugin.getConfig().getDouble("features.hungerRegen.healAmount", 1.0);
        float exhaustion = (float) plugin.getConfig().getDouble("features.hungerRegen.exhaustionPerHeal", 6.0);
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!eligible(p)) continue;
            var attr = p.getAttribute(Attribute.MAX_HEALTH);
            p.setHealth(Math.min(attr.getValue(), p.getHealth() + amount));
            // Vanilla charges exhaustion for healing; without it, regeneration
            // would be free and Hunger would stop mattering once nearly full.
            p.setExhaustion(p.getExhaustion() + exhaustion);
            healed++;
        }
    }

    public String report(Player p) {
        int max = plugin.effectiveHunger(p);
        return "HUNGER_REGEN enabled=" + enabled()
                + " food=" + p.getFoodLevel() + "/" + max
                + " threshold=" + thresholdFor(p)
                + " vanillaWouldRegen=" + (p.getFoodLevel() >= VANILLA_REGEN_FOOD)
                + " eligibleNow=" + eligible(p)
                + " totalHeals=" + healed
                + " (vanilla sprint cutoff at 6 food is unchanged and not reimplemented)";
    }
}
