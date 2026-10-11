package com.minecraftmoba.plugin;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.inventory.ItemStack;
import java.util.*;

/** Actual ingredient debit, not click count. Matrix reconciliation also handles shift-crafting. */
final class Salvage implements Listener {
    private final MobaPlugin plugin;
    private final Set<UUID> pending = new HashSet<>();
    static final Set<Material> PRIMARY = Set.of(Material.IRON_INGOT, Material.GOLD_INGOT,
            Material.COPPER_INGOT, Material.DIAMOND, Material.NETHERITE_INGOT, Material.LEATHER);
    Salvage(MobaPlugin plugin) { this.plugin = plugin; }
    static int threshold(int level) { return level >= 24 ? 3 : level >= 16 ? 4 : level >= 8 ? 5 : 6; }
    static boolean equipment(Material m) {
        String n = m.name();
        return n.endsWith("_SWORD") || n.endsWith("_AXE") || n.endsWith("_PICKAXE") || n.endsWith("_SHOVEL")
                || n.endsWith("_HOE") || n.endsWith("_HELMET") || n.endsWith("_CHESTPLATE")
                || n.endsWith("_LEGGINGS") || n.endsWith("_BOOTS") || m == Material.BUCKET || m == Material.SHEARS || m == Material.SHIELD;
    }
    static Map<Material,Integer> counts(ItemStack[] matrix) {
        Map<Material,Integer> out = new EnumMap<>(Material.class);
        for (var item : matrix) if (item != null && PRIMARY.contains(item.getType())) out.merge(item.getType(), item.getAmount(), Integer::sum);
        return out;
    }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void craft(CraftItemEvent e) {
        if (!(e.getWhoClicked() instanceof Player p) || !plugin.passives().has(p, "salvage")
                || !equipment(e.getRecipe().getResult().getType())) return;
        // One matrix settlement per tick prevents a second click from claiming the same debit.
        if (!pending.add(p.getUniqueId())) { e.setCancelled(true); return; }
        var inventory = e.getInventory(); var before = counts(inventory.getMatrix());
        Map<Material,Integer> units = new EnumMap<>(Material.class);
        for (var item : inventory.getMatrix()) if (item != null && PRIMARY.contains(item.getType())) units.merge(item.getType(), 1, Integer::sum);
        var data = plugin.data(p);
        ItemStack result = e.getRecipe().getResult();
        int producedBefore = held(p, result);
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            pending.remove(p.getUniqueId());
            if (e.isCancelled() || !p.isOnline() || plugin.data(p) != data || !plugin.passives().has(p, "salvage")) return;
            var after = counts(inventory.getMatrix());
            for (var entry : units.entrySet()) if (entry.getValue() >= 3) {
                int crafts = Math.min(Math.max(0, held(p, result) - producedBefore) / result.getAmount(),
                        Math.max(0, before.getOrDefault(entry.getKey(), 0) - after.getOrDefault(entry.getKey(), 0)) / entry.getValue());
                reward(p, entry.getKey(), crafts);
            }
        });
    }
    private static int held(Player p, ItemStack result) {
        int count = 0;
        for (var item : p.getInventory().getContents()) if (item != null && item.isSimilar(result)) count += item.getAmount();
        var cursor = p.getItemOnCursor(); if (cursor.isSimilar(result)) count += cursor.getAmount();
        return count;
    }
    void reward(Player p, Material material, int crafts) {
        if (crafts <= 0 || !PRIMARY.contains(material)) return;
        var data = plugin.data(p); String key = "salvage." + material.name();
        int total = Integer.parseInt(data.classState.getOrDefault(key, "0")) + crafts;
        int threshold = threshold(data.level); int recovered = total / threshold * 2;
        data.classState.put(key, Integer.toString(total % threshold));
        if (recovered > 0) for (var leftover : p.getInventory().addItem(new ItemStack(material, recovered)).values())
            p.getWorld().dropItemNaturally(p.getLocation(), leftover);
    }
}
