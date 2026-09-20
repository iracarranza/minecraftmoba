package com.minecraftmoba.plugin;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.boss.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scoreboard.*;
import java.util.*;

/**
 * Progression readout.
 *
 * This is the plain-text baseline, deliberately usable with no resource pack.
 *
 * A plugin alone cannot add HUD elements, but a resource pack turns a bossbar
 * title into an arbitrary drawing surface: private-use codepoints mapped to
 * image glyphs, positioned with negative-space glyphs, render icons, panels and
 * meters inside what is nominally text. Large servers do exactly this. Position
 * stays fixed to the bossbar strip and the sidebar column, which is the limit
 * that remains real.
 *
 * The drawn layer belongs behind its own flag so a missing or mismatched pack
 * degrades to legible text rather than tofu boxes. See PHASE2-DRAFT.md §1.
 *
 * Disable with features.hud.enabled.
 */
public final class Hud implements Listener {
    private final MobaPlugin plugin;
    private final Map<UUID, BossBar> bars = new HashMap<>();

    public Hud(MobaPlugin plugin) {
        this.plugin = plugin;
        long period = plugin.getConfig().getLong("features.hud.refreshTicks", 20L);
        if (period <= 0) throw new IllegalArgumentException("features.hud.refreshTicks must be positive");
        Bukkit.getScheduler().runTaskTimer(plugin, this::refreshAll, period, period);
    }

    public boolean enabled() { return plugin.getConfig().getBoolean("features.hud.enabled"); }

    private void refreshAll() { if (enabled()) for (Player p : Bukkit.getOnlinePlayers()) refresh(p); }

    public void refresh(Player p) {
        if (!enabled()) { clear(p); return; }
        var d = plugin.data(p);
        if (d == null) return;
        Scoreboard board = p.getScoreboard();
        if (board == Bukkit.getScoreboardManager().getMainScoreboard())
            board = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective obj = board.getObjective("moba_hud");
        if (obj == null) obj = board.registerNewObjective("moba_hud", Criteria.DUMMY,
                ChatColor.AQUA + "" + ChatColor.BOLD + "MOBA");
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);
        for (String entry : new ArrayList<>(board.getEntries())) board.resetScores(entry);

        int line = 10;
        var task = plugin.taskEffects();
        var wp = plugin.workPoints();
        String progress = wp == null ? String.valueOf(d.xp)
                : d.xp + "" + ChatColor.DARK_GRAY + "/" + ChatColor.WHITE + wp.costOf(d.level);
        set(obj, line--, ChatColor.GRAY + "Level " + ChatColor.WHITE + d.level
                + ChatColor.GRAY + "  WP " + ChatColor.WHITE + progress);
        set(obj, line--, ChatColor.GRAY + "Class " + ChatColor.WHITE + (d.classId == null ? "none" : d.classId));
        set(obj, line--, " ");
        set(obj, line--, ChatColor.GRAY + "Health  " + ChatColor.WHITE + fmt(p.getMaxHealth()));
        set(obj, line--, ChatColor.GRAY + "Slots   " + ChatColor.WHITE + plugin.unlockedSlots(p));
        if (task != null && task.enabled())
            set(obj, line--, ChatColor.GRAY + "Eff " + ChatColor.WHITE + task.tier(d, TaskEffects.Domain.EFFICIENCY)
                    + ChatColor.GRAY + " Yld " + ChatColor.WHITE + task.tier(d, TaskEffects.Domain.YIELD)
                    + ChatColor.GRAY + " Dmg " + ChatColor.WHITE + task.tier(d, TaskEffects.Domain.DAMAGE));
        int pending = plugin.pendingRewardCount(p);
        if (pending > 0) set(obj, line--, ChatColor.YELLOW + "! " + pending + " reward(s) — press G");
        p.setScoreboard(board);
    }

    private void set(Objective obj, int score, String text) { obj.getScore(text).setScore(score); }
    private static String fmt(double v) { return v == Math.rint(v) ? String.valueOf((int) v) : String.valueOf(v); }

    /** Boss bar is reserved for an active mode, so it means something when present. */
    public void showMode(Player p, String label, BarColor colour) {
        if (!enabled()) return;
        var bar = bars.computeIfAbsent(p.getUniqueId(),
                k -> Bukkit.createBossBar(label, colour, BarStyle.SOLID));
        bar.setTitle(label);
        bar.setColor(colour);
        if (!bar.getPlayers().contains(p)) bar.addPlayer(p);
        bar.setVisible(true);
    }

    public void hideMode(Player p) {
        var bar = bars.get(p.getUniqueId());
        if (bar != null) bar.setVisible(false);
    }

    public void clear(Player p) {
        hideMode(p);
        Scoreboard board = p.getScoreboard();
        Objective obj = board.getObjective("moba_hud");
        if (obj != null) obj.unregister();
    }

    @EventHandler public void onJoin(PlayerJoinEvent e) { refresh(e.getPlayer()); }
    @EventHandler public void onQuit(PlayerQuitEvent e) {
        var bar = bars.remove(e.getPlayer().getUniqueId());
        if (bar != null) bar.removeAll();
    }
}
