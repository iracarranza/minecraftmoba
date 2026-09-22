package com.minecraftmoba.plugin;

import net.kyori.adventure.text.Component;
import org.bukkit.ChatColor;
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
    private RenewableMarkers renewableMarkers;
    public RenewableMarkers renewableMarkers() { return renewableMarkers; }
    private LobbySafety lobbySafety;
    public LobbySafety lobbySafety() { return lobbySafety; }
    private LobbyWorld lobbyWorld;
    public LobbyWorld lobbyWorld() { return lobbyWorld; }
    private VitalsScaling vitalsScaling;
    public VitalsScaling vitalsScaling() { return vitalsScaling; }
    private ApplyBench applyBench;
    private ResourcePackPush resourcePackPush;
    public ResourcePackPush resourcePackPush() { return resourcePackPush; }
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
    private WorldInstance worldInstance;
    private Worksites worksites;
    private Match match;
    public Contributions contributions() { return contributions; }
    private LockedSlots lockedSlots;
    public LockedSlots lockedSlots() { return lockedSlots; }
    private HungerRegen hungerRegen;
    private HungerDisplay hungerDisplay;
    private WorkPoints workPoints;
    private FountainRegen fountainRegen;
    public FountainRegen fountainRegen() { return fountainRegen; }
    public WorkPoints workPoints() { return workPoints; }
    public Settings settings() { return settings; }

    /**
     * Apply everything a level change implies: rewards, automatic grants, HUD.
     * Shared so gameplay progression and the admin grant take the same path.
     */
    public void applyProgression(Player p, PlayerData d, int levelBefore) {
        for (int lvl = levelBefore + 1; lvl <= d.level; lvl++) rewards.levelUp(p, lvl);
        sync(p, d);
        rewards.invalidate(p);
    }
    public HungerDisplay hungerDisplay() { return hungerDisplay; }
    public HungerRegen hungerRegen() { return hungerRegen; }
    private HealthDisplay healthDisplay;
    public HealthDisplay healthDisplay() { return healthDisplay; }
    private Recall recall;
    public Recall recall() { return recall; }
    private Pings pings;
    public Pings pings() { return pings; }
    private TestBed testBed;
    public OffhandMap offhandMap() { return offhandMap; }
    /** Effective maximum Hunger for a player, for rules expressed relative to it. */
    /** The player's Capacity health: what a full bar is WORTH, not how long it is. */
    public double effectiveMaxHealth(Player p) {
        var d = data(p);
        return d == null ? Vitals.DISPLAY_MAX : capacity(d).maxHealth();
    }

    /**
     * Hunger Capacity: a RATE parameter under scaling, not a ceiling.
     *
     * Callers that mean "how full can this bar get" want {@link #foodCeiling},
     * which is the distinction that matters and the one four call sites got
     * wrong -- the bar visibly stopped at 9 because eating, spawning and
     * Fountain restoration each used the Capacity as a maximum.
     */
    public int effectiveHunger(Player p) {
        return Math.min(20, capacity(data(p)).effectiveHunger());
    }

    /** How full the hunger bar can get. Twenty under scaling; the Capacity otherwise. */
    public int foodCeiling(Player p) {
        return getConfig().getBoolean("features.vitalsScaling.enabled")
                ? (int) Vitals.DISPLAY_MAX : effectiveHunger(p);
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
        worldInstance = new WorldInstance(this);
        worksites = new Worksites(this);
        match = new Match(this, worldInstance);
        getServer().getPluginManager().registerEvents(match, this);
        infraMode = new InfraMode(this);
        getServer().getPluginManager().registerEvents(infraMode, this);
        rewardAdvancements = new RewardAdvancements(this);
        contributions = new Contributions(this);
        lockedSlots = new LockedSlots(this);
        hungerRegen = new HungerRegen(this);
        getServer().getPluginManager().registerEvents(hungerRegen, this);
        renewableMarkers = new RenewableMarkers(this);
        vitalsScaling = new VitalsScaling(this);
        getServer().getPluginManager().registerEvents(vitalsScaling, this);
        applyBench = new ApplyBench(this);
        resourcePackPush = new ResourcePackPush(this);
        getServer().getPluginManager().registerEvents(resourcePackPush, this);
        lobbyWorld = new LobbyWorld(this);
        getServer().getPluginManager().registerEvents(lobbyWorld, this);
        lobbyWorld.ensure();
        lobbySafety = new LobbySafety(this);
        getServer().getPluginManager().registerEvents(lobbySafety, this);
        fountainRegen = new FountainRegen(this);
        getServer().getPluginManager().registerEvents(fountainRegen, this);
        workPoints = new WorkPoints(this);
        getServer().getPluginManager().registerEvents(workPoints, this);
        hungerDisplay = new HungerDisplay(this);
        getServer().getPluginManager().registerEvents(hungerDisplay, this);
        healthDisplay = new HealthDisplay(this);
        pings = new Pings(this);
        testBed = new TestBed(this);
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
        // Enrollment pauses when the offhand is blocked, and RESUMES when it is
        // free again. It used to only pause: the player was dropped, told once
        // in chat, and then never checked again, because the check itself was
        // behind `enrolled(p)`. Miss that one message and the tome, the map,
        // abilities and progression are all simply gone with nothing to say so
        // -- which is exactly how it was reported: "map seems to not be working
        // anymore".
        getServer().getScheduler().runTaskTimer(this, () -> {
            for (Player p : getServer().getOnlinePlayers()) {
                if (enrolled(p)) {
                    if (offhandMap.ensure(p)) continue;
                    PlayerData d = players.remove(p.getUniqueId());
                    d.modeState.clear(); save(p, d);
                    paused.add(p.getUniqueId());
                    p.sendMessage(ChatColor.YELLOW
                            + "MOBA paused: your offhand is occupied. Empty it and this resumes by itself.");
                    getLogger().info("enrollment paused for " + p.getName() + ": offhand occupied");
                } else if (paused.contains(p.getUniqueId()) && InventoryGuard.safeToReduce(p)
                        && p.getInventory().getItemInOffHand().getType().isAir()) {
                    load(p);
                    if (enrolled(p)) {
                        paused.remove(p.getUniqueId());
                        p.sendMessage(ChatColor.GREEN + "MOBA resumed.");
                    }
                }
            }
        }, getConfig().getLong("mapStub.checkTicks"), getConfig().getLong("mapStub.checkTicks"));
    }
    /** Players whose enrollment is paused, so it can be resumed automatically. */
    private final java.util.Set<java.util.UUID> paused = new java.util.HashSet<>();
    public boolean enrollmentPaused(Player p) { return paused.contains(p.getUniqueId()); }

    /**
     * One reset, with its scope named.
     *
     * There were two commands and one verb. `/moba match reset` rebuilt the
     * world and cleared every participant; `/moba reset <player>` cleared one
     * player -- and for a while cleared rather less than that, which is how a
     * reset that visibly did nothing shipped.
     *
     * They were never two implementations of one operation. A match reset IS
     * the player clear applied to every participant, plus the world and the
     * other match-scoped systems, so the relationship is containment. What was
     * wrong was the grammar: the same word meant two different scopes and which
     * one you got depended on where you typed it.
     *
     * So the scope is now said out loud. Bare `/moba reset` is the match, since
     * that is what is almost always meant, and a single player has to be asked
     * for. `/moba match reset` still works, because it is the form in the
     * runbook and in muscle memory, and it runs the same code.
     */
    private boolean resetCommand(CommandSender sender, String[] args) {
        String scope = args.length >= 2 ? args[1].toLowerCase(Locale.ROOT) : "match";
        try {
            switch (scope) {
                case "match" -> {
                    if (match == null) { sender.sendMessage("No match to reset."); return true; }
                    sender.sendMessage(match.reset());
                }
                case "player" -> {
                    if (args.length != 3) {
                        sender.sendMessage("/moba reset player <name>"); return true;
                    }
                    Player target = org.bukkit.Bukkit.getPlayerExact(args[2]);
                    if (target == null) {
                        sender.sendMessage("No such player online: " + args[2]); return true;
                    }
                    if (!InventoryGuard.safeToReduce(target)) {
                        sender.sendMessage(target.getName()
                                + " must empty their cursor and temporary menu slots first.");
                        return true;
                    }
                    clearMatchScopedState(target);
                    var d = data(target);
                    sender.sendMessage("Reset " + target.getName() + ": level=" + d.level
                            + " xp=" + d.xp + " class=" + d.classId + ", inventory cleared.");
                }
                default -> sender.sendMessage(
                        "/moba reset [match] | /moba reset player <name>"
                        + "  -- bare 'reset' resets the match");
            }
        } catch (Exception ex) {
            sender.sendMessage("reset: " + ex.getMessage());
        }
        return true;
    }

    public Match match() { return match; }
    public Worksites worksites() { return worksites; }

    /** Rebuild renewable state for a new match; returns the source count. */
    public int resetRenewables() {
        return renewables == null ? 0 : renewables.resetForNewMatch();
    }

    public WorldInstance worldInstance() { return worldInstance; }

    /**
     * Clear a player's match-scoped state (ALPHA-D2 confirmation: progression
     * and levels are match-scoped for Alpha and reset between matches).
     */
    public void clearMatchScopedState(org.bukkit.entity.Player p) {
        inputs.forget(p);
        var fresh = new PlayerData(p.getUniqueId());
        players.put(fresh.uuid, fresh);

        // The INVENTORY is match-scoped player state, and this is the part that
        // was missing. Every item in it was extracted, crafted or picked up in
        // the world that reset is discarding, so carrying it forward starts the
        // next match with the previous one's work already banked -- which makes
        // progression meaningless from match two onward. ALPHA-D2 already says
        // reset clears match-scoped player state; this is that clause applied
        // to the state a player can actually see.
        var inv = p.getInventory();
        inv.clear();
        inv.setArmorContents(null);
        inv.setItemInOffHand(null);
        p.setItemOnCursor(null);
        // Vanilla XP is not our progression, but it is still state this match
        // produced, and a stale bar reads as a level the player no longer has.
        p.setTotalExperience(0);
        p.setExp(0f);
        p.setLevel(0);
        for (var effect : p.getActivePotionEffects()) p.removePotionEffect(effect.getType());
        p.setFireTicks(0);

        // Task modifiers are removed and re-added from the FRESH data rather
        // than left to applyAutomaticGrants, which only ever grants upward and
        // so would leave a previous match's Efficiency and Damage modifiers
        // attached to a level 1 player.
        if (taskEffects != null) taskEffects.reapply(p, fresh);
        // sync() rather than save(): saving alone persisted the reset data while
        // leaving the live player carrying the old match's maximum health and
        // level display.
        sync(p, fresh);

        // Re-issue what a player is supposed to start with. Both have to run
        // after the clear: locked-slot markers go into slots the clear emptied,
        // and the tome is only issued into an EMPTY offhand.
        if (lockedSlots != null) lockedSlots.refresh(p);
        if (offhandMap != null) offhandMap.ensure(p);
    }

    /**
     * Worksite administration. Capitalization is explicit because canon defers
     * the qualifying-Construct rule; this forces the real state transition and
     * the real shared-opportunity award rather than a parallel mechanism.
     */
    private boolean worksiteCommand(CommandSender sender, String[] args) {
        String sub = args.length > 1 ? args[1].toLowerCase(java.util.Locale.ROOT) : "status";
        try {
            switch (sub) {
                case "status" -> worksites.report().forEach(sender::sendMessage);
                case "list" -> worksites.all().forEach(w -> sender.sendMessage("  " + w));
                case "capitalize" -> {
                    if (args.length != 4) {
                        sender.sendMessage("/moba worksite capitalize <id> <north|south>"); return true;
                    }
                    sender.sendMessage(worksites.capitalize(args[2], Team.parse(args[3]), contributions));
                }
                case "exploit" -> {
                    if (args.length != 3) { sender.sendMessage("/moba worksite exploit <id>"); return true; }
                    sender.sendMessage(worksites.exploit(args[2]));
                }
                default -> sender.sendMessage("/moba worksite <status|list|capitalize|exploit>");
            }
        } catch (Exception ex) {
            sender.sendMessage("worksite: " + ex.getMessage());
        }
        return true;
    }

    /**
     * Match lifecycle administration.
     *
     * These force legitimate state transitions rather than implementing a
     * parallel debug game: `fountain disable` sets the same flag a breach
     * would, and `kill` eliminates through the same path a death does, so the
     * real victory predicate decides the outcome either way.
     */
    private boolean matchCommand(CommandSender sender, String[] args) {
        String sub = args.length > 1 ? args[1].toLowerCase(java.util.Locale.ROOT) : "status";
        try {
            switch (sub) {
                case "open" -> sender.sendMessage(match.open());
                case "start" -> sender.sendMessage(match.start());
                case "status" -> match.report().forEach(sender::sendMessage);
                case "add" -> {
                    if (args.length != 4) { sender.sendMessage("/moba match add <player> <north|south>"); return true; }
                    var target = org.bukkit.Bukkit.getPlayerExact(args[2]);
                    if (target == null) { sender.sendMessage("No such player online: " + args[2]); return true; }
                    sender.sendMessage(match.add(target, Team.parse(args[3])));
                }
                case "fountain" -> {
                    if (args.length != 4 || !args[3].equalsIgnoreCase("disable")) {
                        sender.sendMessage("/moba match fountain <north|south> disable"); return true;
                    }
                    sender.sendMessage(match.disableFountain(Team.parse(args[2])));
                }
                case "kill" -> {
                    if (args.length != 3) { sender.sendMessage("/moba match kill <player>"); return true; }
                    var target = org.bukkit.Bukkit.getPlayerExact(args[2]);
                    if (target == null) { sender.sendMessage("No such player online: " + args[2]); return true; }
                    sender.sendMessage(match.eliminate(target.getUniqueId(), "admin"));
                }
                case "skip" -> {
                    if (args.length != 3) { sender.sendMessage("/moba match skip <minutes>"); return true; }
                    sender.sendMessage(match.skipMinutes(Integer.parseInt(args[2])));
                }
                case "reset" -> sender.sendMessage(match.reset());   // alias of /moba reset
                default -> sender.sendMessage(
                    "/moba match <open|add|start|status|skip|fountain|kill|reset>");
            }
        } catch (Exception ex) {
            sender.sendMessage("match: " + ex.getMessage());
        }
        return true;
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
    /** Unlocked slots, or the full inventory for an unenrolled lobby player. */
    public int unlockedSlots(Player p) {
        var d = players.get(p.getUniqueId());
        return d == null ? 36 : capacity(d).unlockedSlots();
    }
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
        // Under scaling there is no ceiling to enforce: Hunger Capacity is a
        // rate, so the bar fills to twenty and empties faster.
        if (getConfig().getBoolean("features.vitalsScaling.enabled")) return;
        int capped=Math.min(p.getFoodLevel(), Math.min(20,c.effectiveHunger()));
        if (p.getFoodLevel()!=capped) p.setFoodLevel(capped);
        if (p.getSaturation()>capped) p.setSaturation(capped);
    }
    public void applyAndSave(Player p) { sync(p, data(p)); }
    private void sync(Player p, PlayerData d) {
        var c = capacity(d);
        var attribute = Objects.requireNonNull(p.getAttribute(Attribute.MAX_HEALTH));
        // The bar is always twenty points long. Capacity decides what a point
        // is worth, applied as damage and healing scaling, so a player with 9
        // effective health and one with 24 read the same full bar and the same
        // hit takes a bigger bite out of the smaller pool. Setting the maximum
        // from Capacity instead is what made the bar shrink -- and made a
        // half-length health bar the normal sight at level one.
        boolean scaled = getConfig().getBoolean("features.vitalsScaling.enabled");
        double barLength = scaled ? Vitals.DISPLAY_MAX : c.maxHealth();
        attribute.setBaseValue(barLength);
        if (p.getHealth() > attribute.getValue()) p.setHealth(attribute.getValue());
        enforceHunger(p, c);
        if (taskEffects != null) taskEffects.applyAutomaticGrants(p, data(p));
        p.setLevel(d.level);
        // The vanilla bar must read against the band cost, not the flat
        // fallback, or it fills at the wrong rate for every level past 6.
        int cost = workPoints != null ? workPoints.costOf(d.level) : settings.xpPerLevel();
        p.setExp(d.level == settings.maxLevel() ? 0 : Math.min(1f, (float)d.xp / Math.max(1, cost)));
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
    /**
     * Hunger's ceiling, or its absence.
     *
     * Vanilla always draws ten drumsticks, so a Hunger Capacity below twenty
     * could only ever be expressed by refusing to fill part of the row -- which
     * is where "drumsticks you can never reach" came from, and the whole reason
     * a parallel glyph readout was ever built. Under scaling the Capacity is a
     * RATE instead: the bar fills completely and empties faster, and every
     * position is reachable.
     */
    @EventHandler(ignoreCancelled = true) public void hunger(FoodLevelChangeEvent e) {
        if (getConfig().getBoolean("features.vitalsScaling.enabled")) return;
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
        if (args.length >= 1 && args[0].equalsIgnoreCase("bench")) return applyBench.handle(sender, args);
        if (args.length >= 1 && args[0].equalsIgnoreCase("reset")) return resetCommand(sender, args);
        if (args.length >= 1 && args[0].equalsIgnoreCase("match")) return matchCommand(sender, args);
        if (args.length >= 1 && args[0].equalsIgnoreCase("worksite")) return worksiteCommand(sender, args);
        if (args.length >= 1 && args[0].equalsIgnoreCase("work")) {
            Player target = args.length > 1 ? org.bukkit.Bukkit.getPlayerExact(args[1])
                          : (sender instanceof Player sp ? sp : null);
            if (target == null) { sender.sendMessage("/moba work [player]"); return true; }
            workPoints.report(target).forEach(sender::sendMessage);
            return true;
        }
        if (args.length >= 1 && args[0].equalsIgnoreCase("route")) {
            sender.sendMessage("routes=" + routes.routes().size()
                    + " pendingDesignations=" + routes.pendingCount()
                    + " inInfraMode=" + infraMode.activeCount());
            routes.routes().forEach(r -> sender.sendMessage(
                    "  " + r.id() + " world=" + r.world() + " points=" + r.path().size()));
            return true;
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
        if (args.length >= 1 && args[0].equalsIgnoreCase("testbed")) {
            if (!(sender instanceof Player tp)) { sender.sendMessage("Player only"); return true; }
            return testBed.build(sender, tp, args.length > 1 ? args[1] : "all");
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("ping")) {
            if (!(sender instanceof Player pp)) { sender.sendMessage("Player only"); return true; }
            Pings.Kind kind;
            try { kind = Pings.Kind.valueOf(args[1].toUpperCase(java.util.Locale.ROOT)); }
            catch (IllegalArgumentException ex) { sender.sendMessage("Unknown ping: " + args[1]); return true; }
            if (!kind.targetless()) {
                sender.sendMessage(kind + " is a targeted ping; aim and middle-click instead.");
                return true;
            }
            if (!pings.raise(pp, kind)) sender.sendMessage("Ping not sent (disabled, unenrolled, or on cooldown).");
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
                    int before = d.level;
                    while (d.level < settings.maxLevel() && total >= workPoints.costOf(d.level)) {
                        total -= workPoints.costOf(d.level); d.level++;
                    }
                    d.xp = (int)Math.min(total, Integer.MAX_VALUE);
                    for (int lvl = before + 1; lvl <= d.level; lvl++) rewards.levelUp(p, lvl);
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
