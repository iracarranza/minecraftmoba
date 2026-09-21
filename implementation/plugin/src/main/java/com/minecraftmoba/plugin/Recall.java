package com.minecraftmoba.plugin;

import org.bukkit.*;
import org.bukkit.boss.BarColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;
import java.util.*;

/**
 * Recall, begun by lifting the tome out of the offhand slot.
 *
 * The gesture is the point: opening the inventory and picking up the tome is
 * deliberate, slow, and already dangerous in a fight, so it cannot be fired by
 * accident the way a keybind can. The tome is never actually removed — the
 * click is cancelled and the channel starts instead.
 *
 * The channel is interruptible by damage and by moving beyond a small radius,
 * so it is a commitment rather than an escape button. Whether it should also
 * break on attacking, and where a team's destination actually is, are open.
 *
 * Disable with features.recall.enabled.
 */
public final class Recall implements Listener {
    private record Channel(Location origin, int ticksRemaining) {}

    private final MobaPlugin plugin;
    private final Map<UUID, Channel> active = new HashMap<>();

    public Recall(MobaPlugin plugin) { this.plugin = plugin; }

    public boolean enabled() { return plugin.getConfig().getBoolean("features.recall.enabled"); }
    public boolean channeling(Player p) { return active.containsKey(p.getUniqueId()); }

    /** Returns true when the click was consumed as a recall gesture. */
    public boolean beginFromOffhandClick(Player p) {
        if (!enabled()) return false;
        if (channeling(p)) { cancel(p, "cancelled"); return true; }
        int ticks = plugin.getConfig().getInt("features.recall.channelTicks", 100);
        active.put(p.getUniqueId(), new Channel(p.getLocation().clone(), ticks));
        p.closeInventory();
        if (plugin.hud() != null) plugin.hud().showMode(p, "Recalling", BarColor.PURPLE);
        p.sendActionBar(ChatColor.LIGHT_PURPLE + "Recalling — hold still");
        run(p, ticks);
        return true;
    }

    private void run(Player p, int total) {
        new BukkitRunnable() {
            int left = total;
            @Override public void run() {
                Channel c = active.get(p.getUniqueId());
                if (c == null || !p.isOnline()) { cancel(); return; }
                double moved = plugin.getConfig().getDouble("features.recall.moveTolerance", 1.5);
                if (!p.getWorld().equals(c.origin().getWorld())
                        || p.getLocation().distance(c.origin()) > moved) {
                    Recall.this.cancel(p, "moved"); cancel(); return;
                }
                if (--left <= 0) { complete(p); cancel(); return; }
                if (left % 10 == 0)
                    p.sendActionBar(ChatColor.LIGHT_PURPLE + "Recalling " + (left / 20 + 1) + "s");
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    private void complete(Player p) {
        active.remove(p.getUniqueId());
        if (plugin.hud() != null) plugin.hud().hideMode(p);
        Location target = destination(p);
        if (target == null) {
            p.sendMessage(ChatColor.RED + "Nowhere to recall to: your Fountain is gone.");
            return;
        }
        p.teleport(target);
        p.sendActionBar(ChatColor.LIGHT_PURPLE + "Recalled");
    }

    /**
     * Where a recall lands: the player's own Aether Fountain.
     *
     * This was left unresolved when recall was written, on the grounds that the
     * Fountain's position was a map-selection decision. It no longer is. The
     * frozen Alpha map fixes both homelands, ALPHA-D3 makes the Fountain the
     * thing that decides whether a player comes back at all, and ALPHA-D4 makes
     * it where a player reconstructs. Recalling to it is the same fact stated
     * once more, so a recall now lands somewhere that then rebuilds the player
     * rather than merely being a safe corner of the map.
     *
     * An explicit config destination still wins, and the world spawn remains
     * the only fallback outside a match. Nothing is invented for a player with
     * no team.
     */
    private Location destination(Player p) {
        String base = "features.recall.destination.";
        if (plugin.getConfig().isSet(base + "x")) {
            World w = Bukkit.getWorld(plugin.getConfig().getString(base + "world", p.getWorld().getName()));
            if (w != null) return new Location(w,
                    plugin.getConfig().getDouble(base + "x"), plugin.getConfig().getDouble(base + "y"),
                    plugin.getConfig().getDouble(base + "z"));
        }
        Match match = plugin.match();
        if (match != null && match.running()) {
            var part = match.participant(p.getUniqueId());
            if (part != null) {
                Location home = match.homeland(part.team);
                // A disabled Fountain is not a destination. It has stopped
                // being the team's infrastructure, and recalling to a ruin
                // would be the one Fountain interaction that still worked.
                if (home != null && !match.fountainDisabled(part.team)) return home;
                return null;
            }
        }
        return plugin.getConfig().getBoolean("features.recall.fallbackToWorldSpawn", true)
                ? p.getWorld().getSpawnLocation() : null;
    }

    public void cancel(Player p, String why) {
        if (active.remove(p.getUniqueId()) == null) return;
        if (plugin.hud() != null) plugin.hud().hideMode(p);
        p.sendActionBar(ChatColor.GRAY + "Recall " + why);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent e) {
        if (e.getEntity() instanceof Player p && channeling(p)) cancel(p, "interrupted");
    }

    @EventHandler public void onQuit(PlayerQuitEvent e) { active.remove(e.getPlayer().getUniqueId()); }

    public String report(Player p) {
        return "RECALL enabled=" + enabled() + " channeling=" + channeling(p)
                + " channelTicks=" + plugin.getConfig().getInt("features.recall.channelTicks", 100)
                + " destination=" + (destination(p) == null ? "none"
                    : destination(p).getBlockX() + "," + destination(p).getBlockY() + "," + destination(p).getBlockZ());
    }
}
