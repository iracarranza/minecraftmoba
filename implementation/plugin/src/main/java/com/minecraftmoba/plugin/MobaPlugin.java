package com.minecraftmoba.plugin;

import net.kyori.adventure.text.Component;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.*;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.IOException;
import java.util.*;

public final class MobaPlugin extends JavaPlugin implements Listener, CommandExecutor {
    private final Map<UUID, PlayerData> players = new HashMap<>();
    private Settings settings;
    private OffhandMap offhandMap;
    private NamespacedKey dataKey;
    @Override public void onEnable() {
        saveDefaultConfig();
        settings = Settings.load(getConfig());
        dataKey = new NamespacedKey(this, "player_data");
        offhandMap = new OffhandMap(this);
        getServer().getPluginManager().registerEvents(offhandMap, this);
        getServer().getPluginManager().registerEvents(new InventoryGuard(this), this);
        Objects.requireNonNull(getCommand("moba")).setExecutor(this);
        getServer().getPluginManager().registerEvents(this, this);
        getServer().getOnlinePlayers().forEach(this::load);
        getServer().getScheduler().runTaskTimer(this, () -> {
            for (Player p : getServer().getOnlinePlayers()) {
                if (enrolled(p) && !offhandMap.ensure(p)) {
                    PlayerData d = players.remove(p.getUniqueId());
                    d.modeState.clear(); save(p, d);
                    p.sendMessage("MOBA enrollment paused: empty your offhand, then /moba join.");
                }
            }
        }, getConfig().getLong("mapStub.checkTicks"), getConfig().getLong("mapStub.checkTicks"));
    }
    @Override public void onDisable() {
        for (Player p : getServer().getOnlinePlayers()) {
            PlayerData d = players.get(p.getUniqueId());
            if (d != null) { d.modeState.clear(); save(p, d); }
        }
        players.clear();
    }
    public boolean enrolled(Player p) { return players.containsKey(p.getUniqueId()); }
    public boolean isMap(org.bukkit.inventory.ItemStack item) { return offhandMap.isMap(item); }
    public int unlockedSlots(Player p) { return capacity(players.get(p.getUniqueId())).unlockedSlots(); }
    private void load(Player p) {
        if (!offhandMap.ensure(p)) {
            p.sendMessage("Empty your offhand yourself, then /moba join to enroll. No item was replaced.");
            return;
        }
        try {
            byte[] bytes = p.getPersistentDataContainer().get(dataKey, PersistentDataType.BYTE_ARRAY);
            var data = bytes == null ? new PlayerData(p.getUniqueId())
                : PlayerDataCodec.decode(p.getUniqueId(), bytes, settings.maxLevel());
            players.put(p.getUniqueId(), data);
            sync(p, data);
        } catch (IOException ex) {
            getLogger().severe("Refusing to overwrite invalid player data for " + p.getUniqueId() + ": " + ex.getMessage());
            p.kick(Component.text("MOBA data could not be loaded; contact an administrator."));
        }
    }
    private void save(Player p, PlayerData data) {
        try {
            p.getPersistentDataContainer().set(dataKey, PersistentDataType.BYTE_ARRAY, PlayerDataCodec.encode(data));
        } catch (IOException ex) { throw new IllegalStateException("Cannot encode player data", ex); }
    }
    private Capacity.DerivedCapacity capacity(PlayerData d) {
        return Capacity.recompute(d.level, d.choices, settings.capacity());
    }
    private void sync(Player p, PlayerData d) {
        var c = capacity(d);
        var attribute = Objects.requireNonNull(p.getAttribute(Attribute.MAX_HEALTH));
        attribute.setBaseValue(c.maxHealth());
        if (p.getHealth() > attribute.getValue()) p.setHealth(attribute.getValue());
        p.setFoodLevel(Math.min(p.getFoodLevel(), Math.min(20, c.effectiveHunger())));
        p.setSaturation(Math.min(p.getSaturation(), p.getFoodLevel()));
        p.setLevel(d.level);
        p.setExp(d.level == settings.maxLevel() ? 0 : Math.min(1f, (float)d.xp / settings.xpPerLevel()));
        save(p, d);
    }
    @EventHandler public void join(PlayerJoinEvent e) { load(e.getPlayer()); }
    @EventHandler public void quit(PlayerQuitEvent e) {
        var d = players.remove(e.getPlayer().getUniqueId());
        if (d != null) { d.modeState.clear(); save(e.getPlayer(), d); }
    }
    @EventHandler public void death(PlayerDeathEvent e) {
        var d = players.get(e.getPlayer().getUniqueId());
        if (d != null) d.modeState.clear();
        e.setDroppedExp(0);
        e.setKeepLevel(true);
    }
    @EventHandler public void respawn(PlayerRespawnEvent e) {
        getServer().getScheduler().runTask(this, () -> {
            var d = players.get(e.getPlayer().getUniqueId());
            if (d != null) sync(e.getPlayer(), d);
        });
    }
    @EventHandler(ignoreCancelled = true) public void hunger(FoodLevelChangeEvent e) {
        if (e.getEntity() instanceof Player p && players.containsKey(p.getUniqueId()))
            e.setFoodLevel(Math.min(e.getFoodLevel(), Math.min(20, capacity(players.get(p.getUniqueId())).effectiveHunger())));
    }
    @EventHandler public void vanillaXp(PlayerExpChangeEvent e) { e.setAmount(0); }
    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1 && args[0].equalsIgnoreCase("join") && sender instanceof Player player) {
            if (!enrolled(player)) load(player);
            return true;
        }
        if (!sender.hasPermission("moba.admin")) { sender.sendMessage("Missing moba.admin permission."); return true; }
        if (args.length < 2) return false;
        Player p = getServer().getPlayerExact(args[1]);
        if (p == null || !players.containsKey(p.getUniqueId())) { sender.sendMessage("Player must be online with valid MOBA data."); return true; }
        var d = players.get(p.getUniqueId());
        try {
            switch (args[0].toLowerCase(Locale.ROOT)) {
                case "debug" -> {
                    if (args.length != 2) return false;
                    sender.sendMessage("uuid=" + d.uuid + " class=" + d.classId + " level=" + d.level + " xp=" + d.xp
                        + " choices=" + d.choices + " capacity=" + capacity(d) + " mode=" + d.modeState.active);
                    return true;
                }
                case "reset" -> {
                    if (args.length != 2) return false;
                    d = new PlayerData(p.getUniqueId()); players.put(d.uuid, d);
                }
                case "setclass" -> {
                    if (args.length != 3) return false;
                    if (args[2].isBlank()) throw new IllegalArgumentException("Class ID cannot be blank.");
                    d.classId = args[2].equals("none") ? null : args[2];
                    d.modeState.clear();
                }
                case "setlevel" -> {
                    if (args.length != 3) return false;
                    int level = Integer.parseInt(args[2]);
                    if (level < 1 || level > settings.maxLevel()) throw new IllegalArgumentException("Level outside configured range.");
                    d.level = level; d.xp = 0; d.modeState.clear();
                }
                case "xp" -> {
                    if (args.length != 3) return false;
                    int amount = Integer.parseInt(args[2]);
                    if (amount < 0) throw new IllegalArgumentException("XP amount must be nonnegative.");
                    long total = (long)d.xp + amount;
                    while (total >= settings.xpPerLevel() && d.level < settings.maxLevel()) {
                        total -= settings.xpPerLevel(); d.level++;
                        // Step 6 replaces this hook with the configurable reward queue.
                        p.sendMessage(Component.text("Level " + d.level));
                    }
                    d.xp = (int)Math.min(total, Integer.MAX_VALUE);
                }
                default -> { return false; }
            }
            sync(p, d);
            sender.sendMessage("Updated " + p.getName() + ": level=" + d.level + " xp=" + d.xp + " class=" + d.classId);
        } catch (IllegalArgumentException ex) { sender.sendMessage("Invalid input: " + ex.getMessage()); }
        return true;
    }
}
