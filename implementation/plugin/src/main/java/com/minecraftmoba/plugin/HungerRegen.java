package com.minecraftmoba.plugin;

import io.papermc.paper.datacomponent.DataComponentTypes;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemConsumeEvent;

/**
 * Health regeneration expressed relative to maximum Hunger.
 *
 * **This fixes a live defect.** Vanilla natural regeneration requires a food
 * level of at least 18. Capacity caps food at effectiveHunger, which starts at
 * 9 on the canonical curve, so vanilla regeneration could never fire and a
 * player on the intended progression never healed naturally at all. The cap and
 * the vanilla threshold are both absolute, and they are incompatible.
 *
 * The rule is therefore relative: **two empty drumsticks block regeneration**.
 * A drumstick is two food points, so regeneration stops once four or more
 * points are missing. At vanilla's 20 that blocks at 16, which is eight
 * drumsticks or fewer, and permits it from 17 up. At a capped 9 it blocks at 5
 * and permits from 6.
 *
 * The config is named for the blocking condition rather than the permitting
 * one, because "four points below maximum" and "eight or fewer drumsticks"
 * differ by one point depending on which side the comparison sits, and that
 * ambiguity is easy to encode backwards.
 *
 * At a maximum of 9 the regeneration floor lands on 6, the same value as the
 * vanilla sprint cutoff, so an early player who cannot sprint also cannot heal.
 * That coincidence is a property of the curve, not something imposed here.
 *
 * The sprint cutoff is NOT implemented here. Vanilla already refuses to sprint
 * at 6 food or below, which is three drumsticks, and that threshold is absolute
 * by design: it is the fixed floor the Hunger reserve is measured against.
 *
 * Custom healing only runs where vanilla's cannot, so the two never stack.
 *
 * Disable with features.hungerRegen.enabled.
 */
public final class HungerRegen implements Listener {
    /** Vanilla's own natural-regeneration food threshold. */
    public static final int VANILLA_REGEN_FOOD = 18;

    private final MobaPlugin plugin;
    private long healed;

    /**
     * Refuse to eat at the Hunger cap, the way vanilla refuses at full.
     *
     * Vanilla blocks eating at 20 food. Capacity caps a player far below that
     * -- 9 on the canonical curve -- so vanilla kept permitting the meal and
     * `enforceHunger` clamped the result away a tick later. The food was spent
     * for nothing, which reads as a bug in a survival game.
     *
     * The exception is vanilla's own: an item whose food component says
     * canAlwaysEat is edible at full hunger, so it stays edible at the cap.
     * That keeps golden apples and chorus fruit working as they do in vanilla
     * rather than inventing a new rule about them. Items with no food component
     * at all -- milk, potions -- are not hunger items and are never refused.
     */
    @EventHandler
    public void consume(PlayerItemConsumeEvent e) {
        Player p = e.getPlayer();
        if (!plugin.enrolled(p)) return;
        var food = e.getItem().getData(DataComponentTypes.FOOD);
        if (food == null || food.canAlwaysEat()) return;
        int cap = plugin.foodCeiling(p);
        if (p.getFoodLevel() < cap) return;
        e.setCancelled(true);
        p.sendActionBar(net.kyori.adventure.text.Component.text(
                ChatColor.GRAY + "Hunger is full at " + cap + "."));
    }

    public HungerRegen(MobaPlugin plugin) {
        this.plugin = plugin;
        long period = plugin.getConfig().getLong("features.hungerRegen.intervalTicks", 80L);
        if (period > 0) Bukkit.getScheduler().runTaskTimer(plugin, this::tick, period, period);
    }

    public boolean enabled() { return plugin.getConfig().getBoolean("features.hungerRegen.enabled"); }

    /** Missing food points at which regeneration stops. Two drumsticks = 4. */
    public int blockedWhenPointsMissing() {
        return plugin.getConfig().getInt("features.hungerRegen.blockedWhenPointsMissing", 4);
    }

    /** The lowest food level that still regenerates. */
    public int thresholdFor(Player p) {
        return Math.max(1, plugin.foodCeiling(p) - blockedWhenPointsMissing() + 1);
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
        double configured = plugin.getConfig().getDouble("features.hungerRegen.healAmount", 1.0);
        float exhaustion = (float) plugin.getConfig().getDouble("features.hungerRegen.exhaustionPerHeal", 6.0);
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!eligible(p)) continue;
            var attr = p.getAttribute(Attribute.MAX_HEALTH);
            // Stated in effective points, like every other configured rate, so
            // it means the same fraction of a player whatever their Capacity --
            // which is why it is converted per player rather than once.
            double amount = Vitals.scaleHealing(configured, plugin.effectiveMaxHealth(p));
            p.setHealth(Math.min(attr.getValue(), p.getHealth() + amount));
            // Vanilla charges exhaustion for healing; without it, regeneration
            // would be free and Hunger would stop mattering once nearly full.
            p.setExhaustion(p.getExhaustion() + exhaustion);
            healed++;
        }
    }

    public String report(Player p) {
        int max = plugin.foodCeiling(p);
        return "HUNGER_REGEN enabled=" + enabled()
                + " food=" + p.getFoodLevel() + "/" + max
                + " missing=" + (max - p.getFoodLevel())
                + " blockedAtMissing=" + blockedWhenPointsMissing()
                + " lowestRegenFood=" + thresholdFor(p)
                + " vanillaWouldRegen=" + (p.getFoodLevel() >= VANILLA_REGEN_FOOD)
                + " eligibleNow=" + eligible(p)
                + " totalHeals=" + healed
                + " (vanilla sprint cutoff at 6 food is unchanged and not reimplemented)";
    }
}
