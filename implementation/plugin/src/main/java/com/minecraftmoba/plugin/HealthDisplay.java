package com.minecraftmoba.plugin;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scoreboard.*;

/**
 * Visible health, using the two scoreboard display slots the readout is not using.
 *
 * Vanilla gives three slots and the sidebar is already the progression readout,
 * so health goes to the other two:
 *
 *   BELOW_NAME   under the nameplate above a player's head, visible in world
 *   PLAYER_LIST  beside the name in the tab menu
 *
 * Both accept RenderType.HEARTS, which draws a heart icon and a number rather
 * than a bare integer, and both are plain vanilla: no resource pack, no entity
 * per player, no client mod.
 *
 * The limits are real. BELOW_NAME shows one number, not a bar, and vanilla
 * hides it beyond about 32 blocks; it also shows for players only. A richer
 * drawn bar above a head needs a text display entity ridden by the player,
 * which costs an entity each and is not implemented here.
 *
 * Values are scaled so a changed maximum still reads correctly: a player with
 * 9 max health shows 9, not a fraction of 20.
 *
 * Disable with features.healthDisplay.enabled.
 */
public final class HealthDisplay implements Listener {
    private static final String BELOW = "moba_health_below";
    private static final String TAB = "moba_health_tab";

    private final MobaPlugin plugin;

    public HealthDisplay(MobaPlugin plugin) {
        this.plugin = plugin;
        long period = plugin.getConfig().getLong("features.healthDisplay.refreshTicks", 20L);
        if (period > 0) Bukkit.getScheduler().runTaskTimer(plugin, this::refreshAll, period, period);
    }

    public boolean enabled() { return plugin.getConfig().getBoolean("features.healthDisplay.enabled"); }

    private Objective ensure(Scoreboard board, String name, DisplaySlot slot, String title) {
        Objective obj = board.getObjective(name);
        if (obj == null) {
            obj = board.registerNewObjective(name, Criteria.DUMMY, title, RenderType.HEARTS);
        }
        // Another objective may already hold the slot; last writer wins in vanilla too.
        if (obj.getDisplaySlot() != slot) obj.setDisplaySlot(slot);
        return obj;
    }

    /** Health rounded up, so a player on a sliver still reads as 1 rather than 0. */
    private int shown(Player p) { return (int) Math.ceil(p.getHealth()); }

    public void refresh(Player viewer) {
        if (!enabled()) { clear(viewer); return; }
        Scoreboard board = viewer.getScoreboard();
        if (board == Bukkit.getScoreboardManager().getMainScoreboard()) return;  // Hud owns creation
        Objective below = ensure(board, BELOW, DisplaySlot.BELOW_NAME, ChatColor.RED + "❤");
        Objective tab = ensure(board, TAB, DisplaySlot.PLAYER_LIST, ChatColor.RED + "❤");
        for (Player subject : Bukkit.getOnlinePlayers()) {
            int value = shown(subject);
            below.getScore(subject.getName()).setScore(value);
            tab.getScore(subject.getName()).setScore(value);
        }
    }

    private void refreshAll() { if (enabled()) for (Player p : Bukkit.getOnlinePlayers()) refresh(p); }

    public void clear(Player viewer) {
        Scoreboard board = viewer.getScoreboard();
        for (String name : new String[]{BELOW, TAB}) {
            Objective obj = board.getObjective(name);
            if (obj != null) obj.unregister();
        }
    }

    // Immediate updates on the events that actually change health, so the
    // number is not a second stale during a fight.
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent e) {
        if (e.getEntity() instanceof Player) Bukkit.getScheduler().runTask(plugin, this::refreshAll);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHeal(EntityRegainHealthEvent e) {
        if (e.getEntity() instanceof Player) Bukkit.getScheduler().runTask(plugin, this::refreshAll);
    }

    @EventHandler public void onJoin(PlayerJoinEvent e) {
        Bukkit.getScheduler().runTask(plugin, () -> refresh(e.getPlayer()));
    }

    public String report(Player p) {
        var attr = p.getAttribute(Attribute.MAX_HEALTH);
        return "HEALTH_DISPLAY enabled=" + enabled()
                + " shown=" + shown(p) + "/" + (attr == null ? "?" : (int) attr.getValue())
                + " slots=BELOW_NAME,PLAYER_LIST renderType=HEARTS"
                + " (sidebar is the progression readout; a drawn bar above the head "
                + "would need a text display entity per player)";
    }
}
