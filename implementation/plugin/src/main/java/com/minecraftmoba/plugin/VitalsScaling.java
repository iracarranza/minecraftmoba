package com.minecraftmoba.plugin;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Applies the vitals scaling to the two things the world does to a player:
 * hurting them, and making them hungry.
 *
 * Damage is straightforward -- one event, one multiplication.
 *
 * Exhaustion is not, because vanilla has no event for it. Sprinting, jumping and
 * mining raise a hidden float and hunger drops when it crosses four. So the
 * accumulated value is sampled and its INCREASES are amplified, which multiplies
 * the rate without touching how vanilla spends it. Decreases are left alone:
 * those are vanilla consuming exhaustion to take a hunger point, and amplifying
 * them would double-count.
 */
public final class VitalsScaling implements Listener {
    private final MobaPlugin plugin;
    /** Last exhaustion seen per player, so only the increase is amplified. */
    private final Map<UUID, Float> lastExhaustion = new HashMap<>();

    public VitalsScaling(MobaPlugin plugin) {
        this.plugin = plugin;
        long period = plugin.getConfig().getLong("features.vitalsScaling.sampleTicks", 2L);
        if (period > 0) plugin.getServer().getScheduler()
                .runTaskTimer(plugin, this::sampleExhaustion, period, period);
    }

    public boolean enabled() { return plugin.getConfig().getBoolean("features.vitalsScaling.enabled"); }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void damage(EntityDamageEvent e) {
        if (!enabled() || !(e.getEntity() instanceof Player p) || !plugin.enrolled(p)) return;
        double capacity = plugin.effectiveMaxHealth(p);
        if (capacity <= 0) return;
        e.setDamage(Vitals.scaleDamage(e.getDamage(), capacity));
    }

    private void sampleExhaustion() {
        if (!enabled()) return;
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            if (!plugin.enrolled(p)) { lastExhaustion.remove(p.getUniqueId()); continue; }
            float now = p.getExhaustion();
            Float before = lastExhaustion.get(p.getUniqueId());
            if (before != null && now > before) {
                double extra = (now - before)
                        * (Vitals.exhaustionMultiplier(plugin.effectiveHunger(p)) - 1.0);
                if (extra > 0) {
                    now = (float) Math.min(40.0, now + extra);
                    p.setExhaustion(now);
                }
            }
            lastExhaustion.put(p.getUniqueId(), now);
        }
    }

    @EventHandler public void quit(PlayerQuitEvent e) { lastExhaustion.remove(e.getPlayer().getUniqueId()); }

    public String report(Player p) {
        double c = plugin.effectiveMaxHealth(p);
        return "VITALS enabled=" + enabled()
                + String.format(" health %.1f/%.1f effective (bar %.1f/20)",
                    Vitals.toEffective(p.getHealth(), c), c, p.getHealth())
                + " hungerCapacity=" + plugin.effectiveHunger(p)
                + String.format(" exhaustion x%.2f", Vitals.exhaustionMultiplier(plugin.effectiveHunger(p)));
    }
}
