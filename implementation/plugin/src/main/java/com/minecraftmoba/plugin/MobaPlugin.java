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
    private Provenance provenance;
    private Renewables renewables;
    public Provenance provenance() { return provenance; }
    public Renewables renewables() { return renewables; }
    private Sentinel sentinel;
    public Sentinel sentinel() { return sentinel; }
    private TaskEffects taskEffects;
    public TaskEffects taskEffects() { return taskEffects; }
    private Hud hud;
    public Hud hud() { return hud; }
    private RenewableAuthoring renewAuthoring;
    private InfraMode infraMode;
    private Routes routes;
    public InfraMode infraMode() { return infraMode; }
    public Routes routes() { return routes; }
    private RewardAdvancements rewardAdvancements;
    public RewardAdvancements rewardAdvancements() { return rewardAdvancements; }
    private HubLobby hubLobby;
    public HubLobby hubLobby() { return hubLobby; }
    private Durability durability;
    public Durability durability() { return durability; }
    private Contributions contributions;
    public Contributions contributions() { return contributions; }
    private LockedSlots lockedSlots;
    public LockedSlots lockedSlots() { return lockedSlots; }
    private HungerRegen hungerRegen;
    public HungerRegen hungerRegen() { return hungerRegen; }
    private HealthDisplay healthDisplay;
    public HealthDisplay healthDisplay() { return healthDisplay; }
    private Recall recall;
    public Recall recall() { return recall; }
    private Pings pings;
    public Pings pings() { return pings; }
    public OffhandMap offhandMap() { return offhandMap; }
    /** Effective maximum Hunger for a player, for rules expressed relative to it. */
    public int effectiveHunger(Player p) {
        return Math.min(20, capacity(data(p)).effectiveHunger());
    }
    public int pendingRewardCount(Player p) { return settings.rewards().pending(data(p)).size(); }
    private AbilityInputs inputs;
    private PacketInputs packets;
    private Rewards rewards;
    private NamespacedKey dataKey;
    @Override public void onEnable() {
        saveDefaultConfig();
        settings = Settings.load(getConfig());
        saveConfig(); // persist merged missing defaults after validation
        dataKey = new NamespacedKey(this, "player_data");
        provenance = new Provenance(this);
        getServer().getPluginManager().registerEvents(provenance, this);
        sentinel = new Sentinel(this);
        taskEffects = new TaskEffects(this);
        hud = new Hud(this);
        renewAuthoring = new RenewableAuthoring(this);
        infraMode = new InfraMode(this);
        getServer().getPluginManager().registerEvents(infraMode, this);
        rewardAdvancements = new RewardAdvancements(this);
        contributions = new Contributions(this);
        lockedSlots = new LockedSlots(this);
        hungerRegen = new HungerRegen(this);
        healthDisplay = new HealthDisplay(this);
        pings = new Pings(this);
        getServer().getPluginManager().registerEvents(pings, this);
        recall = new Recall(this);
        getServer().getPluginManager().registerEvents(recall, this);
        getServer().getPluginManager().registerEvents(healthDisplay, this);
        getServer().getPluginManager().registerEvents(lockedSlots, this);
        durability = new Durability(this);
        getServer().getPluginManager().registerEvents(durability, this);
        hubLobby = new HubLobby(this);
        getServer().getPluginManager().registerEvents(hubLobby, this);
        routes = new Routes(this);
        getServer().getPluginManager().registerEvents(routes, this);
        getServer().getPluginManager().registerEvents(hud, this);
        getServer().getPluginManager().registerEvents(taskEffects, this);
        renewables = new Renewables(this);
        getServer().getPluginManager().registerEvents(renewables, this);
        offhandMap = new OffhandMap(this);
        getServer().getPluginManager().registerEvents(offhandMap, this);
        getServer().getPluginManager().registerEvents(new InventoryGuard(this), this);
        Objects.requireNonNull(getCommand("moba")).setExecutor(this);
        getServer().getPluginManager().registerEvents(this, this);
        rewards = new Rewards(this, settings.rewards());
        getServer().getPluginManager().registerEvents(rewards, this);
        inputs = new AbilityInputs(this, provenance);
        packets = new PacketInputs(this, inputs);
        getServer().getPluginManager().registerEvents(inputs, this);
        getServer().getPluginManager().registerEvents(packets, this);
        getServer().getOnlinePlayers().forEach(p -> { load(p); packets.attach(p); });
        getServer().getScheduler().runTaskTimer(this, () -> {
            for (Player p : getServer().getOnlinePlayers()) if (enrolled(p)) enforceHunger(p, capacity(data(p)));
        }, getConfig().getLong("capacity.enforceTicks"), getConfig().getLong("capacity.enforceTicks"));
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
        if (rewards != null) getServer().getOnlinePlayers().forEach(rewards::cleanup);
        if (packets != null) packets.close();
        if (inputs != null) getServer().getOnlinePlayers().forEach(p -> inputs.exit(p, true));
        if (provenance != null) provenance.sample();
        for (Player p : getServer().getOnlinePlayers()) {
            PlayerData d = players.get(p.getUniqueId());
            if (d != null) { d.modeState.clear(); save(p, d); }
        }
        players.clear();
    }
    public PlayerData data(Player p) { return players.get(p.getUniqueId()); }
    public boolean enrolled(Player p) { return players.containsKey(p.getUniqueId()); }
    public boolean isMap(org.bukkit.inventory.ItemStack item) { return offhandMap.isMap(item); }
    public int unlockedSlots(Player p) { return capacity(players.get(p.getUniqueId())).unlockedSlots(); }
    private void load(Player p) {
        if (!InventoryGuard.safeToReduce(p)) {
            p.sendMessage("Empty the cursor and temporary crafting/menu slots yourself, then /moba join.");
            return;
        }
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
    private void enforceHunger(Player p, Capacity.DerivedCapacity c) {
        int capped=Math.min(p.getFoodLevel(), Math.min(20,c.effectiveHunger()));
        if (p.getFoodLevel()!=capped) p.setFoodLevel(capped);
        if (p.getSaturation()>capped) p.setSaturation(capped);
    }
    public void applyAndSave(Player p) { sync(p, data(p)); }
    private void sync(Player p, PlayerData d) {
        var c = capacity(d);
        var attribute = Objects.requireNonNull(p.getAttribute(Attribute.MAX_HEALTH));
        attribute.setBaseValue(c.maxHealth());
        if (p.getHealth() > attribute.getValue()) p.setHealth(attribute.getValue());
        enforceHunger(p, c);
        if (taskEffects != null) taskEffects.applyAutomaticGrants(p, data(p));
        p.setLevel(d.level);
        p.setExp(d.level == settings.maxLevel() ? 0 : Math.min(1f, (float)d.xp / settings.xpPerLevel()));
        save(p, d);
        rewards.refresh(p);
    }
    @EventHandler public void join(PlayerJoinEvent e) { load(e.getPlayer()); }
    @EventHandler public void quit(PlayerQuitEvent e) {
        var d = players.remove(e.getPlayer().getUniqueId());
        if (d != null) { d.modeState.clear(); save(e.getPlayer(), d); }
    }
    @EventHandler public void death(PlayerDeathEvent e) {
        var d = players.get(e.getPlayer().getUniqueId());
        if (d == null) return;
        d.modeState.clear();
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
    @EventHandler public void vanillaXp(PlayerExpChangeEvent e) { if (enrolled(e.getPlayer())) e.setAmount(0); }
    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1 && args[0].equalsIgnoreCase("join") && sender instanceof Player player) {
            if (!enrolled(player)) load(player);
            return true;
        }
        if (args.length == 1 && args[0].equalsIgnoreCase("rewards") && sender instanceof Player player) {
            rewards.open(player); return true;
        }
        if (!sender.hasPermission("moba.admin")) { sender.sendMessage("Missing moba.admin permission."); return true; }
        if (args.length == 2 && args[0].equalsIgnoreCase("task")) {
            var target = org.bukkit.Bukkit.getPlayerExact(args[1]);
            if (target == null) { sender.sendMessage("No such player"); return true; }
            if (data(target) == null) { sender.sendMessage(args[1] + " is not enrolled."); return true; }
            sender.sendMessage(taskEffects.report(data(target))); return true;
        }
        if (args.length == 4 && args[0].equalsIgnoreCase("grant")) {
            var target = org.bukkit.Bukkit.getPlayerExact(args[1]);
            if (target == null) { sender.sendMessage("No such player"); return true; }
            TaskEffects.Domain domain;
            try { domain = TaskEffects.Domain.valueOf(args[2].toUpperCase(java.util.Locale.ROOT)); }
            catch (IllegalArgumentException ex) { sender.sendMessage("Domain must be EFFICIENCY, YIELD or DAMAGE"); return true; }
            if (data(target) == null) {
                sender.sendMessage(args[1] + " is not enrolled. Run: /moba join  (as that player)");
                return true;
            }
            taskEffects.grant(target, data(target), domain, Integer.parseInt(args[3]));
            sender.sendMessage(taskEffects.report(data(target))); return true;
        }
        if (args.length >= 1 && args[0].equalsIgnoreCase("renew")) return renewAuthoring.handle(sender, args);
        if (args.length >= 2 && args[0].equalsIgnoreCase("infra")) {
            if (!(sender instanceof Player ip)) { sender.sendMessage("Player only"); return true; }
            switch (args[1].toLowerCase(java.util.Locale.ROOT)) {
                case "enter" -> infraMode.enter(ip);
                case "exit" -> infraMode.exit(ip);
                case "toggle" -> infraMode.toggle(ip);
                case "status" -> sender.sendMessage(infraMode.report(ip));
                default -> sender.sendMessage("/moba infra <enter|exit|toggle|status>");
            }
            return true;
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("syncadv")) {
            var target = org.bukkit.Bukkit.getPlayerExact(args[1]);
            if (target == null) { sender.sendMessage("No such player"); return true; }
            rewardAdvancements.sync(target); sender.sendMessage("Synced advancements for " + args[1]); return true;
        }
        if (args.length >= 1 && args[0].equalsIgnoreCase("hub")) {
            if (args.length == 2 && args[1].equalsIgnoreCase("build")) {
                if (!(sender instanceof Player hp)) { sender.sendMessage("Player only"); return true; }
                hubLobby.build(sender, hp.getWorld()); return true;
            }
            hubLobby.report().forEach(sender::sendMessage); return true;
        }
        if (args.length >= 2 && args[0].equalsIgnoreCase("contrib")) {
            switch (args[1].toLowerCase(java.util.Locale.ROOT)) {
                case "options" -> {
                    if (!(sender instanceof Player cp)) { sender.sendMessage("Player only"); return true; }
                    var d = data(cp);
                    sender.sendMessage("Eligible for " + (d == null ? "?" : d.classId) + ": "
                            + contributions.eligible(d == null ? null : d.classId));
                }
                case "choose" -> {
                    if (!(sender instanceof Player cp)) { sender.sendMessage("Player only"); return true; }
                    if (args.length < 3) { sender.sendMessage("/moba contrib choose <form>"); return true; }
                    Contributions.Form form;
                    try { form = Contributions.Form.valueOf(args[2].toUpperCase(java.util.Locale.ROOT)); }
                    catch (IllegalArgumentException ex) { sender.sendMessage("Unknown form: " + args[2]); return true; }
                    String refusal = contributions.choose(cp, form);
                    if (refusal != null) sender.sendMessage("Refused: " + refusal);
                }
                case "capitalize" -> {
                    if (args.length < 4) { sender.sendMessage("/moba contrib capitalize <worksiteId> <team>"); return true; }
                    String refusal = contributions.capitalize(args[2], args[3]);
                    sender.sendMessage(refusal == null ? "Shared opportunity earned by " + args[3] : "Refused: " + refusal);
                }
                case "allocate" -> {
                    if (args.length < 4) { sender.sendMessage("/moba contrib allocate <team> <form>"); return true; }
                    Contributions.Form form;
                    try { form = Contributions.Form.valueOf(args[3].toUpperCase(java.util.Locale.ROOT)); }
                    catch (IllegalArgumentException ex) { sender.sendMessage("Unknown form: " + args[3]); return true; }
                    String refusal = contributions.allocate(args[2], form);
                    sender.sendMessage(refusal == null ? "Allocated " + form + " to " + args[2] : "Refused: " + refusal);
                }
                case "status" -> contributions.report().forEach(sender::sendMessage);
                default -> sender.sendMessage("/moba contrib <options|choose|capitalize|allocate|status>");
            }
            return true;
        }
        if (args.length == 1 && args[0].equalsIgnoreCase("pings")) {
            sender.sendMessage(pings.report()); return true;
        }
        if (args.length >= 1 && args[0].equalsIgnoreCase("curve")) {
            // Calibration aid: vanilla hardness and blast resistance, so a
            // material tier curve can be grounded in the game's own numbers
            // rather than invented multipliers.
            String[] probe = args.length > 1 ? java.util.Arrays.copyOfRange(args, 1, args.length)
                : new String[]{"GLASS","MUD_BRICKS","TERRACOTTA","WHITE_CONCRETE","BRICKS",
                    "STONE","COBBLESTONE","STONE_BRICKS","TUFF","POLISHED_TUFF","TUFF_BRICKS",
                    "DEEPSLATE","COBBLED_DEEPSLATE","POLISHED_DEEPSLATE","DEEPSLATE_BRICKS",
                    "BLACKSTONE","POLISHED_BLACKSTONE_BRICKS","OBSIDIAN","NETHERITE_BLOCK",
                    "IRON_BLOCK","COPPER_BLOCK","CALCITE","BASALT","END_STONE_BRICKS",
                    "PURPUR_BLOCK","DARK_PRISMARINE","QUARTZ_BLOCK","SANDSTONE","PACKED_MUD"};
            for (String n : probe) {
                var m = org.bukkit.Material.matchMaterial(n);
                sender.sendMessage(m == null ? "CURVE " + n + " ABSENT"
                    : String.format("CURVE %s hardness=%.2f blast=%.2f", n, m.getHardness(), m.getBlastResistance()));
            }
            return true;
        }
        if (args.length == 1 && args[0].equalsIgnoreCase("materials")) {
            sender.sendMessage(MaterialCategories.report()); return true;
        }
        if (args.length == 1 && args[0].equalsIgnoreCase("recall")) {
            if (!(sender instanceof Player rp)) { sender.sendMessage("Player only"); return true; }
            sender.sendMessage(recall.report(rp)); return true;
        }
        if (args.length == 1 && args[0].equalsIgnoreCase("health")) {
            if (!(sender instanceof Player hp)) { sender.sendMessage("Player only"); return true; }
            healthDisplay.refresh(hp); sender.sendMessage(healthDisplay.report(hp)); return true;
        }
        if (args.length == 1 && args[0].equalsIgnoreCase("regen")) {
            if (!(sender instanceof Player rp)) { sender.sendMessage("Player only"); return true; }
            sender.sendMessage(hungerRegen.report(rp)); return true;
        }
        if (args.length == 1 && args[0].equalsIgnoreCase("slots")) {
            if (!(sender instanceof Player sp)) { sender.sendMessage("Player only"); return true; }
            lockedSlots.refresh(sp); sender.sendMessage(lockedSlots.report(sp)); return true;
        }
        if (args.length == 1 && args[0].equalsIgnoreCase("durability")) {
            sender.sendMessage(durability.report()); return true;
        }
        if (args.length == 1 && args[0].equalsIgnoreCase("routes")) {
            routes.report().forEach(sender::sendMessage); return true;
        }
        if (args.length == 1 && args[0].equalsIgnoreCase("renewables")) {
            renewables.report().forEach(sender::sendMessage); return true;
        }
        if (args.length == 1 && args[0].equalsIgnoreCase("provenance")) {
            provenance.sample(); sender.sendMessage(provenance.summary()); return true;
        }
        if (args.length < 2) return false;
        Player p = getServer().getPlayerExact(args[1]);
        if (p == null || !players.containsKey(p.getUniqueId())) { sender.sendMessage("Player must be online with valid MOBA data."); return true; }
        var d = players.get(p.getUniqueId());
        try {
            switch (args[0].toLowerCase(Locale.ROOT)) {
                case "debug" -> {
                    if (args.length != 2) return false;
                    sender.sendMessage("uuid=" + d.uuid + " class=" + d.classId + " level=" + d.level + " xp=" + d.xp
                        + " choices=" + d.choices + " capacity=" + capacity(d) + " mode=" + d.modeState.active + " " + inputs.debug(p));
                    return true;
                }
                case "reset" -> {
                    if (args.length != 2) return false;
                    if (!InventoryGuard.safeToReduce(p)) throw new IllegalArgumentException("Empty cursor and temporary menu slots before reset.");
                    inputs.forget(p);
                    d = new PlayerData(p.getUniqueId()); players.put(d.uuid, d);
                }
                case "setclass" -> {
                    if (args.length != 3) return false;
                    if (args[2].isBlank()) throw new IllegalArgumentException("Class ID cannot be blank.");
                    inputs.exit(p, true);
                    d.classId = args[2].equals("none") ? null : args[2];
                    d.modeState.clear();
                }
                case "setlevel" -> {
                    if (args.length != 3) return false;
                    int level = Integer.parseInt(args[2]);
                    if (level < 1 || level > settings.maxLevel()) throw new IllegalArgumentException("Level outside configured range.");
                    if (level < d.level && !InventoryGuard.safeToReduce(p)) throw new IllegalArgumentException("Empty cursor and temporary menu slots before lowering level.");
                    inputs.exit(p, true);
                    d.level = level; d.xp = 0; d.modeState.clear();
                }
                case "xp" -> {
                    if (args.length != 3) return false;
                    int amount = Integer.parseInt(args[2]);
                    if (amount < 0) throw new IllegalArgumentException("XP amount must be nonnegative.");
                    long total = (long)d.xp + amount;
                    while (total >= settings.xpPerLevel() && d.level < settings.maxLevel()) {
                        total -= settings.xpPerLevel(); d.level++;
                        rewards.levelUp(p, d.level);
                    }
                    d.xp = (int)Math.min(total, Integer.MAX_VALUE);
                }
                default -> { return false; }
            }
            sync(p, d);
            rewards.invalidate(p);
            sender.sendMessage("Updated " + p.getName() + ": level=" + d.level + " xp=" + d.xp + " class=" + d.classId);
        } catch (IllegalArgumentException ex) { sender.sendMessage("Invalid input: " + ex.getMessage()); }
        return true;
    }
}
