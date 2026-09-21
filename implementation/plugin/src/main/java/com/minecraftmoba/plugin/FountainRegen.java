package com.minecraftmoba.plugin;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;

/**
 * The Aether Fountain restores Health and Hunger to the team that owns it.
 *
 * objectives.md ties the Fountain to respawning and cautions that it "should
 * function as Minecraft infrastructure rather than primarily as another
 * conventional combat health bar". This is written to stay on the infrastructure
 * side of that line rather than becoming a heal pad:
 *
 *  - only a team's **own** Fountain restores that team, so it is a place you
 *    return to rather than a resource you can contest for sustain;
 *  - a **disabled** Fountain restores nothing. It has stopped being the team's
 *    respawn infrastructure, and recovery stopping with it follows from the
 *    same fact rather than being a second rule;
 *  - restoration respects Capacity. It fills toward the player's own maximum
 *    Health and effective Hunger, and never past them, so it cannot be used to
 *    sidestep the Hunger cap that HungerRegen enforces elsewhere.
 *
 * Radius and rates are NON-CANON ALPHA FIXTURES: canon establishes the
 * Fountain's relationship to respawning, not how quickly standing in one
 * restores anything.
 */
public final class FountainRegen {
    private final MobaPlugin plugin;

    public FountainRegen(MobaPlugin plugin) {
        this.plugin = plugin;
        long period = plugin.getConfig().getLong("features.fountainRegen.intervalTicks", 20L);
        if (period > 0) Bukkit.getScheduler().runTaskTimer(plugin, this::tick, period, period);
    }

    public boolean enabled() { return plugin.getConfig().getBoolean("features.fountainRegen.enabled"); }

    private double radius() { return plugin.getConfig().getDouble("features.fountainRegen.radius", 8.0); }

    private void tick() {
        if (!enabled()) return;
        Match match = plugin.match();
        if (match == null || !match.running()) return;
        double r2 = radius() * radius();
        double health = plugin.getConfig().getDouble("features.fountainRegen.healthPerInterval", 1.0);
        int food = plugin.getConfig().getInt("features.fountainRegen.foodPerInterval", 1);

        for (Match.Participant part : match.participants()) {
            if (!part.alive) continue;
            Player p = Bukkit.getPlayer(part.uuid);
            if (p == null || !plugin.enrolled(p)) continue;
            if (match.fountainDisabled(part.team)) continue;
            Location fountain = match.homeland(part.team);
            if (fountain == null || !p.getWorld().equals(fountain.getWorld())) continue;
            if (p.getLocation().distanceSquared(fountain) > r2) continue;
            restore(p, health, food);
        }
    }

    private void restore(Player p, double health, int food) {
        var attr = p.getAttribute(Attribute.MAX_HEALTH);
        double max = attr == null ? 20.0 : attr.getValue();
        if (p.getHealth() < max) p.setHealth(Math.min(max, p.getHealth() + health));
        int cap = plugin.effectiveHunger(p);
        if (p.getFoodLevel() < cap) {
            p.setFoodLevel(Math.min(cap, p.getFoodLevel() + food));
            // Saturation is what makes the restored Hunger actually hold; without
            // it the next exhaustion tick spends the point straight back.
            p.setSaturation(Math.min(p.getFoodLevel(), p.getSaturation() + food));
        }
    }

    public String report(Player p) {
        Match match = plugin.match();
        if (match == null) return "FOUNTAIN_REGEN no match";
        var part = match.participant(p.getUniqueId());
        if (part == null) return "FOUNTAIN_REGEN enabled=" + enabled() + " (not a participant)";
        Location f = match.homeland(part.team);
        double d = f != null && p.getWorld().equals(f.getWorld()) ? p.getLocation().distance(f) : -1;
        return "FOUNTAIN_REGEN enabled=" + enabled() + " team=" + part.team.lower()
                + " fountainDisabled=" + match.fountainDisabled(part.team)
                + " distance=" + (d < 0 ? "other world" : String.format("%.1f", d))
                + " radius=" + radius()
                + " health=" + String.format("%.1f", p.getHealth())
                + " food=" + p.getFoodLevel() + "/" + plugin.effectiveHunger(p);
    }
}
