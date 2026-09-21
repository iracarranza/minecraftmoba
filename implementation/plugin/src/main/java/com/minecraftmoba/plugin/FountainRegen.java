package com.minecraftmoba.plugin;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRespawnEvent;

/**
 * Reconstruction at the Aether Fountain.
 *
 * The established mechanic is not a heal pad and not a respawn that hands back
 * a whole player. A player who dies with a functioning friendly Fountain
 * reappears there immediately, keeping their inventory, but reappears at
 * roughly 1 Health and 1 Hunger and must then *reconstruct*: Health and Hunger
 * return at fixed absolute rates for as long as they stay. They may leave at
 * any point, including half-reconstructed. A living player who walks back to
 * their own functioning Fountain uses the same free reconstruction.
 *
 * Two consequences of the rates being absolute rather than proportional:
 *
 *  - raising a player's Health/Hunger maxima lengthens full reconstruction
 *    rather than leaving it constant, so Capacity has a cost as well as a
 *    benefit;
 *  - reconstruction fills toward the player's own maxima and never past them,
 *    so it is not a way around the Hunger cap HungerRegen enforces on eating.
 *
 * When the Fountain stops functioning, reconstruction stops immediately. A
 * player mid-reconstruction stays alive with whatever partial Health and Hunger
 * they had reached and with their normal maxima — the Fountain falling does not
 * kill or diminish anyone. It only means the *next* death is permanent, which
 * is the mechanism ALPHA-D3 resolves victory through.
 *
 * Only a team's own Fountain reconstructs that team, so it is a place you
 * return to rather than sustain contested on the spot.
 *
 * Saturation is deliberately untouched: current authority specifies Health and
 * Hunger, so reconstructed Hunger behaves like ordinary unsaturated Hunger and
 * drains normally once the player leaves.
 *
 * Alpha treats Fountain disablement as irreversible. The broader design
 * distinguishes temporary obstruction/repair from permanent physical
 * destruction; that distinction survives in the documentation and is simplified
 * here, not overwritten.
 *
 * Radius and the per-interval rates are NON-CANON ALPHA FIXTURES: canon fixes
 * that the rates are absolute, not what they are.
 */
public final class FountainRegen implements Listener {
    private final MobaPlugin plugin;

    public FountainRegen(MobaPlugin plugin) {
        this.plugin = plugin;
        long period = plugin.getConfig().getLong("features.fountainRegen.intervalTicks", 20L);
        if (period > 0) Bukkit.getScheduler().runTaskTimer(plugin, this::tick, period, period);
    }

    public boolean enabled() { return plugin.getConfig().getBoolean("features.fountainRegen.enabled"); }

    private double radius() { return plugin.getConfig().getDouble("features.fountainRegen.radius", 8.0); }

    /**
     * A player reappears reconstructing, not restored. Applied a tick later
     * because the respawn is still setting game mode and position when the
     * event fires.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onRespawn(PlayerRespawnEvent e) {
        if (!enabled()) return;
        Match match = plugin.match();
        if (match == null || !match.running()) return;
        Match.Participant part = match.participant(e.getPlayer().getUniqueId());
        // An eliminated player is on their way to spectator; there is nothing
        // to reconstruct, and their Fountain is by definition not functioning.
        if (part == null || !part.alive || match.fountainDisabled(part.team)) return;
        Player p = e.getPlayer();
        double health = plugin.getConfig().getDouble("features.fountainRegen.respawnHealth", 1.0);
        int food = plugin.getConfig().getInt("features.fountainRegen.respawnFood", 1);
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!p.isOnline()) return;
            p.setHealth(Math.max(1.0, Math.min(maxHealth(p), health)));
            p.setFoodLevel(Math.min(plugin.effectiveHunger(p), food));
            p.setSaturation(0f);
        });
    }

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
            reconstruct(p, health, food);
        }
    }

    private double maxHealth(Player p) {
        var attr = p.getAttribute(Attribute.MAX_HEALTH);
        return attr == null ? 20.0 : attr.getValue();
    }

    private void reconstruct(Player p, double health, int food) {
        double max = maxHealth(p);
        if (p.getHealth() < max) p.setHealth(Math.min(max, p.getHealth() + health));
        int cap = plugin.effectiveHunger(p);
        if (p.getFoodLevel() < cap) p.setFoodLevel(Math.min(cap, p.getFoodLevel() + food));
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
                + " health=" + String.format("%.1f", p.getHealth()) + "/" + String.format("%.1f", maxHealth(p))
                + " food=" + p.getFoodLevel() + "/" + plugin.effectiveHunger(p);
    }
}
