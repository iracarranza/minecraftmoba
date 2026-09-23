package com.minecraftmoba.fixture;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityExhaustionEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

/** Disposable acceptance-server diagnostics, never included in the gameplay plugin. */
final class HungerProbe implements Listener {
    private final JavaPlugin plugin;
    HungerProbe(JavaPlugin plugin) { this.plugin = plugin; }
    static String state(Player p) {
        return " player=" + p.getName() + " food=" + p.getFoodLevel() + " saturation=" + p.getSaturation()
            + " exhaustion=" + p.getExhaustion() + " health=" + p.getHealth() + " mode=" + p.getGameMode();
    }
    static void watch(JavaPlugin plugin, Player p) {
        new BukkitRunnable() {
            int samples;
            public void run() {
                if (!p.isOnline() || samples++ >= 240) { cancel(); return; }
                plugin.getLogger().info("HUNGER_SAMPLE" + state(p));
            }
        }.runTaskTimer(plugin, 0, 20);
    }
    @EventHandler(priority = EventPriority.MONITOR)
    public void exhaustion(EntityExhaustionEvent e) {
        if (!(e.getEntity() instanceof Player p)) return;
        plugin.getLogger().info("HUNGER_EXHAUST reason=" + e.getExhaustionReason() + " amount=" + e.getExhaustion()
            + " cancelled=" + e.isCancelled() + state(p));
    }
    @EventHandler(priority = EventPriority.LOWEST)
    public void foodBefore(FoodLevelChangeEvent e) { food("BEFORE", e); }
    @EventHandler(priority = EventPriority.MONITOR)
    public void foodAfter(FoodLevelChangeEvent e) { food("AFTER", e); }
    private void food(String phase, FoodLevelChangeEvent e) {
        if (e.getEntity() instanceof Player p) plugin.getLogger().info("HUNGER_FOOD_" + phase
            + " target=" + e.getFoodLevel() + " cancelled=" + e.isCancelled() + state(p));
    }
    @EventHandler(priority = EventPriority.MONITOR)
    public void consume(PlayerItemConsumeEvent e) {
        plugin.getLogger().info("HUNGER_CONSUME item=" + e.getItem().getType() + " cancelled=" + e.isCancelled() + state(e.getPlayer()));
    }
}
