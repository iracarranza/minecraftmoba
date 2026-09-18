package com.minecraftmoba.fixture;

import java.util.Arrays;
import java.util.Objects;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

/** TEST ONLY: dispatches real server events; never transports or repairs any item. */
public final class AcceptanceFixture extends JavaPlugin {
    @Override public void onEnable() {
        Objects.requireNonNull(getCommand("mobafixture")).setExecutor(this);
    }
    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player p) || !sender.hasPermission("moba.fixture")) return false;
        try {
            if (args.length != 1) return false;
            switch (args[0]) {
                case "transfer-partial" -> transfers(p, true);
                case "transfer-full" -> transfers(p, false);
                case "denied-place" -> deniedPlace(p);
                default -> { return false; }
            }
            sender.sendMessage("FIXTURE_PASS " + args[0]);
            getLogger().info("FIXTURE_PASS " + args[0] + " player=" + p.getName());
        } catch (RuntimeException failure) {
            sender.sendMessage("FIXTURE_FAIL " + failure);
            getLogger().severe("FIXTURE_FAIL " + failure);
        }
        return true;
    }
    private void transfers(Player p, boolean partial) {
        var plugin = Objects.requireNonNull(Bukkit.getPluginManager().getPlugin("MinecraftMoba"));
        int expectedLevel = partial ? 1 : plugin.getConfig().getInt("progression.maxLevel");
        if (p.getLevel() != expectedLevel) throw new IllegalStateException("Set fixture level to " + expectedLevel + " first");
        Inventory external = Bukkit.createInventory(null, 9);
        ItemStack map = p.getInventory().getItemInOffHand().clone();
        if (map.getType() != Material.FILLED_MAP) throw new IllegalStateException("Missing offhand map");
        transfer(partial ? "partial destination" : "full capacity destination with room",
            external, new ItemStack(Material.STONE), p.getInventory(), partial);
        transfer("map inbound", external, map, p.getInventory(), true);
        transfer("map outbound", p.getInventory(), map, external, true);
    }
    private void transfer(String label, Inventory source, ItemStack item, Inventory destination, boolean cancelled) {
        var beforeSource = snapshot(source); var beforeDestination = snapshot(destination);
        var event = new InventoryMoveItemEvent(source, item.clone(), destination, true);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled() != cancelled) throw new IllegalStateException(label + " cancellation=" + event.isCancelled());
        if (!Arrays.equals(beforeSource, snapshot(source)) || !Arrays.equals(beforeDestination, snapshot(destination)))
            throw new IllegalStateException(label + " mutated inventory during event");
        getLogger().info("TRANSFER_PASS " + label + " cancelled=" + cancelled + " inventoriesUnchanged=true");
    }
    private ItemStack[] snapshot(Inventory inventory) {
        return Arrays.stream(inventory.getContents()).map(i -> i == null ? null : i.clone()).toArray(ItemStack[]::new);
    }
    private void deniedPlace(Player p) {
        var plugin = Objects.requireNonNull(Bukkit.getPluginManager().getPlugin("MinecraftMoba"));
        var key = new NamespacedKey(plugin, "placed_blocks_v1");
        var block = p.getLocation().getBlock();
        var pdc = block.getChunk().getPersistentDataContainer();
        byte[] before = pdc.get(key, PersistentDataType.BYTE_ARRAY);
        var event = new BlockPlaceEvent(block, block.getState(), block.getRelative(0, -1, 0),
            new ItemStack(Material.STONE), p, false, EquipmentSlot.HAND);
        Bukkit.getPluginManager().callEvent(event);
        if (!Arrays.equals(before, pdc.get(key, PersistentDataType.BYTE_ARRAY)))
            throw new IllegalStateException("Denied placement changed chunk provenance");
    }
}
