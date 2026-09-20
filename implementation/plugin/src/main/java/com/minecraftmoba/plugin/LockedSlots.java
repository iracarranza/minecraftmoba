package com.minecraftmoba.plugin;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import java.util.List;

/**
 * Makes a locked inventory slot look different from an empty one.
 *
 * `InventoryGuard` already refuses interaction with slots above the player's
 * unlocked count, but nothing showed it: a locked slot and an empty slot were
 * pixel-identical, so the only feedback was a click that silently did nothing.
 *
 * A marker item fills each locked slot. Two useful consequences beyond looks:
 * vanilla's own "find an empty slot" never selects an occupied slot, so pickup
 * stops depending on the guard alone; and the default material is BARRIER,
 * which reads as blocked without any resource pack and can be given a subtle
 * slot-shaped texture with one.
 *
 * The material must be unobtainable in survival. If a player could hold the
 * same item, vanilla would merge a pickup into the marker stack.
 *
 * Markers are identified by persistent data, never by material, so changing
 * the material is a config edit and a player-held lookalike is not a marker.
 *
 * Disable with features.lockedSlots.enabled.
 */
public final class LockedSlots implements Listener {
    private final MobaPlugin plugin;
    private final NamespacedKey key;

    public LockedSlots(MobaPlugin plugin) {
        this.plugin = plugin;
        this.key = new NamespacedKey(plugin, "moba_locked_slot");
        long period = plugin.getConfig().getLong("features.lockedSlots.refreshTicks", 20L);
        if (period > 0) Bukkit.getScheduler().runTaskTimer(plugin, this::refreshAll, period, period);
    }

    public boolean enabled() { return plugin.getConfig().getBoolean("features.lockedSlots.enabled"); }

    public boolean isMarker(ItemStack item) {
        return item != null && item.hasItemMeta()
                && item.getItemMeta().getPersistentDataContainer().has(key, PersistentDataType.BYTE);
    }

    private ItemStack marker() {
        String name = plugin.getConfig().getString("features.lockedSlots.material", "BARRIER");
        Material m = Material.matchMaterial(name);
        if (m == null) throw new IllegalArgumentException("features.lockedSlots.material is not a material: " + name);
        var item = new ItemStack(m);
        var meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.DARK_GRAY + plugin.getConfig().getString(
                "features.lockedSlots.label", "Locked slot"));
        meta.setLore(List.of(ChatColor.DARK_GRAY + "Unlocked by Inventory progression"));
        meta.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    private void refreshAll() { if (enabled()) for (Player p : Bukkit.getOnlinePlayers()) refresh(p); }

    /** Adds markers to newly locked slots and clears them from newly unlocked ones. */
    public void refresh(Player p) {
        var inv = p.getInventory();
        if (!enabled()) { clear(p); return; }
        int unlocked = plugin.unlockedSlots(p);
        for (int slot = 0; slot < 36; slot++) {
            ItemStack current = inv.getItem(slot);
            if (slot >= unlocked) {
                if (current == null || current.getType().isAir()) inv.setItem(slot, marker());
            } else if (isMarker(current)) {
                inv.setItem(slot, null);
            }
        }
    }

    public void clear(Player p) {
        var inv = p.getInventory();
        for (int slot = 0; slot < 36; slot++) if (isMarker(inv.getItem(slot))) inv.setItem(slot, null);
    }

    /** A marker is furniture, not loot: it must never enter the world. */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDeath(PlayerDeathEvent e) {
        e.getDrops().removeIf(this::isMarker);
        clear(e.getEntity());
    }

    @EventHandler public void onJoin(PlayerJoinEvent e) { refresh(e.getPlayer()); }
    @EventHandler public void onRespawn(PlayerRespawnEvent e) {
        Bukkit.getScheduler().runTask(plugin, () -> refresh(e.getPlayer()));
    }
    @EventHandler public void onClose(InventoryCloseEvent e) {
        if (e.getPlayer() instanceof Player p) Bukkit.getScheduler().runTask(plugin, () -> refresh(p));
    }

    public String report(Player p) {
        int unlocked = plugin.unlockedSlots(p);
        int markers = 0;
        for (int slot = 0; slot < 36; slot++) if (isMarker(p.getInventory().getItem(slot))) markers++;
        return "LOCKED_SLOTS enabled=" + enabled() + " unlocked=" + unlocked + "/36 markers=" + markers
                + " expected=" + Math.max(0, 36 - unlocked);
    }
}
