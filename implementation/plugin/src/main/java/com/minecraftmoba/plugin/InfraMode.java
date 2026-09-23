package com.minecraftmoba.plugin;

import org.bukkit.ChatColor;
import org.bukkit.boss.BarColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import java.util.*;

/**
 * Infrastructure Mode: a player state that changes what interacting with the
 * world means, entered and left from the Quick Actions dialog.
 *
 * Holding no item and granting no benefit by itself. infrastructure.md is clear
 * that ordinary roads, caches and farms keep their value independently of
 * recognition, so entering this mode must not confer anything — it only changes
 * what a Banner interaction does.
 *
 * Mode must not survive death or disconnect in a stuck form. The Phase 1
 * deathInMode scenario established that for ability mode and the same applies
 * here, so both events clear it.
 *
 * Disable with features.infraMode.enabled.
 */
public final class InfraMode implements Listener {
    private final MobaPlugin plugin;
    private final Set<UUID> active = new HashSet<>();

    public InfraMode(MobaPlugin plugin) { this.plugin = plugin; }

    public boolean enabled() { return plugin.getConfig().getBoolean("features.infraMode.enabled"); }
    public boolean isActive(Player p) { return enabled() && active.contains(p.getUniqueId()); }

    public void enter(Player p) {
        if (!enabled()) { p.sendMessage("Infrastructure Mode is disabled."); return; }
        if (!active.add(p.getUniqueId())) { p.sendMessage("Already in Infrastructure Mode."); return; }
        if (plugin.hud() != null) plugin.hud().showMode(p, "Infrastructure Mode", BarColor.BLUE);
        p.sendMessage(ChatColor.AQUA + "Infrastructure Mode entered.");
        p.sendActionBar(ChatColor.AQUA + "Interact with a Banner to designate a Route endpoint");
    }

    public void exit(Player p) {
        if (!active.remove(p.getUniqueId())) { p.sendMessage("Not in Infrastructure Mode."); return; }
        // Leaving abandons an unfinished designation rather than keeping it
        // alive invisibly.
        if (plugin.routes() != null) plugin.routes().discardPending(p);
        if (plugin.hud() != null) plugin.hud().hideMode(p);
        p.sendMessage(ChatColor.GRAY + "Infrastructure Mode exited.");
    }

    public void toggle(Player p) { if (isActive(p)) exit(p); else enter(p); }

    /**
     * Clear Infrastructure Mode for everyone at match reset.
     *
     * Routed through forceClear so a player's bossbar and pending designation
     * are released the same way they are on death, rather than by dropping the
     * set and leaving the HUD showing a mode nobody is in.
     */
    public int activeCount() { return active.size(); }

    public int reset() {
        int cleared = active.size();
        for (java.util.UUID id : new java.util.HashSet<>(active)) {
            Player p = org.bukkit.Bukkit.getPlayer(id);
            if (p != null) forceClear(p); else active.remove(id);
        }
        active.clear();
        return cleared;
    }

    private void forceClear(Player p) {
        if (!active.remove(p.getUniqueId())) return;
        if (plugin.routes() != null) plugin.routes().discardPending(p);
        if (plugin.hud() != null) plugin.hud().hideMode(p);
    }

    @EventHandler public void onDeath(PlayerDeathEvent e) { forceClear(e.getEntity()); }
    @EventHandler public void onQuit(PlayerQuitEvent e) { forceClear(e.getPlayer()); }

    public String report(Player p) {
        return "INFRA player=" + p.getName() + " active=" + isActive(p)
             + " pendingRoute=" + (plugin.routes() != null && plugin.routes().hasPending(p));
    }
}
