package com.minecraftmoba.plugin;

import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.*;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.*;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

/** An admin preparation room and reusable scoop sessions, outside production drafting/pool claims. */
public final class Lab implements Listener {
    private final MobaPlugin plugin;
    private final LabMaps maps;
    private final Map<UUID, Selection> selections = new HashMap<>();
    private UUID owner;
    private String activeMap;
    private World room;
    private final LabAuthoring authoring;
    private ChamberUi chamberUi;
    private final CombatChamber combat;

    LabMaps maps() { return maps; }
    LabAuthoring authoring() { return authoring; }

    private static final class Selection {
        String mapId, classId;
        int level = 1;
        Team team = Team.NORTH;
    }
    private static final class Menu implements InventoryHolder {
        final UUID player;
        final Map<Integer, String> actions = new HashMap<>();
        Inventory inventory;
        Menu(UUID player) { this.player = player; }
        @Override public Inventory getInventory() { return inventory; }
    }

    private final LabRules rules = new LabRules();
    private ScenarioBench scenario;
    public ScenarioBench scenario() { return scenario; }
    private NightBench night;
    public NightBench night() { return night; }
    private MapOverlay overlay;
    public MapOverlay overlay() { return overlay; }
    /** The launched scoop's id, or null. */
    String activeMapId() { return activeMap; }
    /** The launched scoop's world, or null when no lab scoop is running. */
    World scoopWorld() { return plugin.worldInstance().labActive() ? plugin.worldInstance().world() : null; }
    private OpportunityBench opportunity;
    public OpportunityBench opportunity() { return opportunity; }
    private LegibilityBench legibility;
    public LegibilityBench legibility() { return legibility; }
    public LabRules rules() { return rules; }

    public Lab(MobaPlugin plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(rules, plugin);
        authoring = new LabAuthoring(plugin, this);
        Bukkit.getPluginManager().registerEvents(authoring, plugin);
        combat = new CombatChamber(plugin, this);
        Bukkit.getPluginManager().registerEvents(combat, plugin);
        legibility = new LegibilityBench(plugin, this);
        opportunity = new OpportunityBench(plugin, this);
        overlay = new MapOverlay(plugin, this);
        night = new NightBench(plugin, this);
        scenario = new ScenarioBench(plugin, this);
        Path configured = Path.of(plugin.getConfig().getString("alpha.lab.mapsDirectory", "lab-maps"));
        maps = new LabMaps(configured.isAbsolute() ? configured
                : Bukkit.getWorldContainer().toPath().resolve(configured));
    }

    /**
     * The chamber workspace, which exists only inside a launched lab scoop.
     *
     * Gated on the disposable world rather than on the room, because a bay is
     * terrain a tester edits and the room is Adventure-mode and protected.
     * Offering a chamber in the room would be offering somewhere to build that
     * refuses every block.
     */
    private void chamber(Player p, String[] args) throws IOException {
        var workspace = plugin.chamberWorkspace();
        if (workspace == null || !plugin.worldInstance().labActive())
            throw new IllegalStateException(
                    "Chambers need a launched scoop. /moba lab start, choose and launch first.");
        String verb = args.length > 2 ? args[2].toLowerCase(Locale.ROOT) : "take";
        switch (verb) {
            case "take", "new" -> chamberUi.take(p);
            case "enter" -> chamberUi.enterBay(p);
            case "leave", "release" -> {
                chamberUi.leave(p);
                p.sendMessage("Chamber released. Its bay is free for the next tester.");
            }
            // Placement goes through LabAuthoring, which judges it against the
            // bay and does the write. Resolving the session here instead would
            // clear the verdict without placing anything.
            case "cancel" -> authoring.command(p, new String[]{"lab", "author", "cancel"});
            case "confirm" -> authoring.command(p, new String[]{"lab", "author", "apply"});
            case "status" -> p.sendMessage(workspace.report(p));
            default -> p.sendMessage("/moba lab chamber take | enter | leave | confirm | cancel | status");
        }
    }

    static boolean mayPrepare(Match.State state, boolean labActive) {
        return state == Match.State.IDLE && !labActive;
    }

    public boolean command(CommandSender sender, String[] args) {
        if (!(sender instanceof Player p)) { sender.sendMessage("Lab needs an in-game tester."); return true; }
        if (!p.hasPermission("moba.admin")) { p.sendMessage("An administrator is required."); return true; }
        String verb = args.length > 1 ? args[1].toLowerCase(Locale.ROOT) : "start";
        try {
            switch (verb) {
                case "start", "menu" -> enter(p, args.length > 2 ? args[2] : null);
                case "maps" -> { requireSetup(p); menu(p, "maps", 0); }
                case "classes" -> { requireSetup(p); menu(p, "classes", 0); }
                case "map" -> {
                    Selection s = requireSetup(p);
                    s.mapId = maps.select(value(args)).mapId(); menu(p, "setup", 0);
                }
                case "class" -> {
                    Selection s = requireSetup(p); String id = value(args);
                    if (!plugin.inputs().ids().contains(id)) throw new IllegalArgumentException("Unknown registered class: " + id);
                    s.classId = id; menu(p, "setup", 0);
                }
                case "level" -> {
                    Selection s = requireSetup(p); int level = Integer.parseInt(value(args));
                    if (level < 1 || level > plugin.settings().maxLevel()) throw new IllegalArgumentException("Level outside configured range.");
                    s.level = level; menu(p, "setup", 0);
                }
                case "team" -> {
                    Selection s = requireSetup(p); s.team = Team.valueOf(value(args).toUpperCase(Locale.ROOT)); menu(p, "setup", 0);
                }
                case "play", "launch" -> play(p);
                case "chamber" -> chamber(p, args);
                case "benches" -> { requireSetup(p); menu(p, "benches", 0); }
                case "bench" -> bench(p, value(args));
                case "combat" -> combat.command(p, args);
                case "legibility" -> legibility.command(p, args);
                case "opportunity" -> opportunity.command(p, args);
                case "overlay" -> overlay.command(p, args);
                case "night" -> night.command(p, args);
                case "scenario", "scenarios" -> scenario.command(p, args);
                case "time" -> time(p, args);
                case "rules" -> rules(p, args);
                case "author" -> authoring.command(p, args);
                case "undo" -> authoring.command(p, new String[]{"lab","author","undo"});
                case "end", "stop" -> end(p);
                case "leave" -> {
                    requireSetup(p);
                    if (plugin.worldInstance().labActive()) throw new IllegalStateException("End the lab session first with /moba lab end.");
                    selections.remove(p.getUniqueId()); p.closeInventory(); plugin.lobbyWorld().send(p);
                }
                case "status" -> {
                    p.sendMessage("Lab scoops: " + maps.entries().size() + " at " + maps.directory());
                    p.sendMessage("Lab session: " + (activeMap == null ? "not running" : activeMap));
                    var s = selections.get(p.getUniqueId());
                    if (s != null) p.sendMessage("Selected map=" + s.mapId + " class=" + s.classId + " level=" + s.level + " team=" + s.team);
                }
                default -> p.sendMessage("/moba lab start | benches | bench <id> | combat | legibility | opportunity | overlay | night | scenario | maps | classes | map <id/number> | class <id> | level <n> | team <north/south> | play | chamber | end | leave | status");
            }
        } catch (IOException | RuntimeException ex) {
            p.sendMessage("Lab refused: " + ex.getMessage());
            plugin.getLogger().warning("[lab] " + ex);
        }
        return true;
    }

    private static String value(String[] args) {
        if (args.length != 3) throw new IllegalArgumentException("This lab command needs one value.");
        return args[2];
    }

    private void enter(Player p, String mapId) {
        if (!mayPrepare(plugin.match().state(), plugin.worldInstance().labActive()))
            throw new IllegalStateException("A match or draft is active. End/reset it through its own commands first.");
        if (!plugin.enrolled(p)) {
            p.performCommand("moba join");
            if (!plugin.enrolled(p)) throw new IllegalStateException("Empty your offhand and cursor, then retry enrollment.");
        }
        Selection s = selections.computeIfAbsent(p.getUniqueId(), ignored -> new Selection());
        if (mapId != null) s.mapId = maps.select(mapId).mapId();
        sendToRoom(p);
        p.sendMessage("Lab preparation: choose a class and scoop, then Launch. /moba lab end returns here after a test.");
        menu(p, "setup", 0);
    }

    public CombatChamber combat() { return combat; }

    /** For the combat chamber, which starts from the lab room like a scoop launch does. */
    void requireSetupFor(Player p) { requireSetup(p); }

    /** Send a tester back to the lab room. */
    void toRoom(Player p) { sendToRoom(p); }

    /** Release what the lab holds at shutdown. */
    public void close() { combat.close(); legibility.close(); opportunity.close(); overlay.close(); night.close(); scenario.close(); rules.clearAll(); }

    private Selection requireSetup(Player p) {
        if (!mayPrepare(plugin.match().state(), plugin.worldInstance().labActive()))
            throw new IllegalStateException("Finish the active session before changing lab setup.");
        Selection s = selections.get(p.getUniqueId());
        if (s == null || room == null || !p.getWorld().equals(room))
            throw new IllegalStateException("Enter the debug room with /moba lab start first.");
        return s;
    }

    private void play(Player p) throws IOException {
        Selection s = requireSetup(p);
        if (s.mapId == null || s.classId == null) throw new IllegalStateException("Choose both a scoop and a class before launching.");
        if (!plugin.inputs().ids().contains(s.classId)) throw new IllegalStateException("Selected class is no longer registered.");
        MapPool.Entry entry = maps.select(s.mapId);
        maps.verify(entry);
        p.closeInventory();
        plugin.match().open();
        owner = p.getUniqueId(); activeMap = entry.mapId();
        try {
            plugin.match().resolveLabSelection(entry);
            plugin.clearMatchScopedState(p);
            plugin.applyDraftedClass(p, s.classId);
            p.performCommand("moba setlevel " + p.getName() + " " + s.level);
            plugin.match().startForTest();
            plugin.match().add(p, s.team);
            // The workspace is built per launch and against the disposable
            // world, so bays never outlive the terrain they were cut from.
            World scoop = p.getWorld();
            var cfg = plugin.getConfig();
            plugin.chamberWorkspace(new ChamberWorkspace(plugin, scoop,
                    cfg.getInt("alpha.lab.chamber.radius", 16),
                    cfg.getInt("alpha.lab.chamber.height", 20),
                    cfg.getInt("alpha.lab.chamber.floor", 60)));
            chamberUi = new ChamberUi(plugin, this, plugin.chamberWorkspace());
            p.setGameMode(GameMode.SURVIVAL);
            p.sendMessage("Lab running on " + maps.label(entry) + " as " + s.classId
                    + ". /moba lab end returns to setup. This scoop remains reusable.");
        } catch (IOException | RuntimeException ex) {
            try {
                teardown();
                plugin.clearMatchScopedState(p);
                sendToRoom(p);
            } catch (Exception cleanup) { ex.addSuppressed(cleanup); }
            throw ex;
        }
    }

    private void teardown() throws IOException {
        // Bodies first: a fake player alive in a world that no longer exists is the failure this exists to prevent.
        scenario.abortAll("the lab session ended");
        rules.clearAll();
        authoring.clear();
        // Dropped before the world is unloaded, not after: a bay in a world
        // that no longer exists is the same defect the scenario harness is
        // built around avoiding.
        if (chamberUi != null) { chamberUi.close(); chamberUi = null; }
        plugin.chamberWorkspace(null);
        if (plugin.worldInstance().labActive()) {
            plugin.match().resetForLab();
            plugin.worldInstance().exitLab();
        } else if (owner != null && plugin.match().state() == Match.State.PRE_MATCH) {
            // Selection failed before opening a lab world; no production world needs restoring.
            plugin.match().discardEmptyLabPreparation();
        }
        owner = null; activeMap = null;
    }

    // ---- time and world rules ---------------------------------------------------

    /** Where this tester's clock lives: their launched scoop, or the combat chamber's world. */
    private enum Where { SCOOP, COMBAT }

    private Where whereClock(Player p) {
        if (combat.occupies(p)) return Where.COMBAT;
        if (owner != null && owner.equals(p.getUniqueId()) && plugin.worldInstance().labActive()
                && p.getWorld().equals(plugin.worldInstance().world())) return Where.SCOOP;
        throw new IllegalStateException("Time and rules need your launched scoop or the combat chamber.");
    }

    /**
     * {@code /moba lab time status | dawn | noon | dusk | midnight | night <n> | skip <minutes> | pause | resume}.
     *
     * In a scoop this drives the real match clock, so a skipped night is a real night:
     * its boundaries fire. It only runs FORWARD ({@link LabTime}). The combat chamber
     * has no match clock, only a world time, so there only the time of day is settable.
     */
    private void time(Player p, String[] args) {
        Where where = whereClock(p);
        String verb = args.length > 2 ? args[2].toLowerCase(Locale.ROOT) : "status";
        if (where == Where.COMBAT) {
            var w = p.getWorld();
            switch (verb) {
                case "status" -> p.sendMessage("Combat chamber time " + w.getTime() + " (fixed; no match clock here).");
                case "dawn", "noon", "dusk", "midnight", "day", "night", "sunset", "sunrise" -> {
                    w.setTime(LabTime.TimeOfDay.parse(verb).tick);
                    p.sendMessage("Combat chamber time set to " + verb + ".");
                }
                default -> p.sendMessage("The combat chamber has no match clock: /moba lab time dawn | noon | dusk | midnight");
            }
            return;
        }
        var match = plugin.match();
        switch (verb) {
            case "status" -> p.sendMessage("Clock " + MatchClock.describe(match.elapsedTicks())
                    + (match.labPaused() ? " [PAUSED]" : " [running]") + "; " + rules.describe(p));
            case "pause" -> { match.pauseForLabAuthoring(); p.sendMessage("Clock paused."); }
            case "resume" -> { match.resumeFromLabPause(); p.sendMessage("Clock running."); }
            case "skip" -> {
                if (args.length < 4) throw new IllegalArgumentException("/moba lab time skip <minutes>");
                p.sendMessage(match.skipTicks(LabTime.ticksForMinutes(Integer.parseInt(args[3]))));
            }
            case "night" -> {
                if (args.length < 4) throw new IllegalArgumentException("/moba lab time night <n>");
                p.sendMessage(match.skipTicks(LabTime.ticksUntilNight(match.elapsedTicks(), Integer.parseInt(args[3]))));
            }
            default -> p.sendMessage(match.skipTicks(LabTime.ticksUntil(match.elapsedTicks(), LabTime.TimeOfDay.parse(verb))));
        }
    }

    /** {@code /moba lab rules status | hunger freeze|normal | regen off|normal}. */
    private void rules(Player p, String[] args) {
        whereClock(p);
        String verb = args.length > 2 ? args[2].toLowerCase(Locale.ROOT) : "status";
        String value = args.length > 3 ? args[3].toLowerCase(Locale.ROOT) : "";
        switch (verb) {
            case "hunger" -> rules.hungerFrozen(p, value.equals("freeze") || value.equals("frozen") || value.equals("on"));
            case "regen" -> rules.regenOff(p, value.equals("off") || value.equals("stop"));
            case "status" -> { }
            default -> throw new IllegalArgumentException("/moba lab rules hunger freeze|normal | regen off|normal");
        }
        p.sendMessage("Lab rules: " + rules.describe(p));
    }

    private void end(Player p) throws IOException {
        if (owner == null) throw new IllegalStateException("No lab session is active; standard matches are not ended here.");
        if (!owner.equals(p.getUniqueId())) throw new IllegalStateException("Only the tester who launched this session can end it.");
        var testers = new ArrayList<>(plugin.match().participantsByTeam().keySet());
        teardown();
        for (UUID id : testers) {
            Player tester = Bukkit.getPlayer(id);
            if (tester != null) sendToRoom(tester);
        }
        sendToRoom(p); menu(p, "setup", 0);
        p.sendMessage("Lab ended. The next launch copies the pristine scoop again.");
    }

    void requireAuthor(Player p) {
        if (owner == null || !owner.equals(p.getUniqueId()) || !plugin.worldInstance().labActive()
                || !p.getWorld().equals(plugin.worldInstance().world()))
            throw new IllegalStateException("Authoring requires your own active disposable lab scoop. /moba lab start, choose and launch first.");
        plugin.match().pauseForLabAuthoring();
        p.setGameMode(GameMode.CREATIVE);
    }

    /** Enter a bench from the hub, by id. The bench's own command does the work, so there is one way in. */
    private void bench(Player p, String id) {
        var b = LabHub.bench(id);
        if (b == null) throw new IllegalArgumentException("No bench '" + id + "'. Benches: "
                + LabHub.BENCHES.stream().map(LabHub.Bench::id).toList());
        requireSetup(p);
        String why = LabHub.refusal(b, new LabHub.State(plugin.worldInstance().labActive()));
        if (why != null) { p.sendMessage(why); return; }
        p.closeInventory();
        switch (b.id()) {
            case "combat" -> combat.command(p, new String[]{"lab", "combat", "start"});
            case "legibility" -> legibility.command(p, new String[]{"lab", "legibility", "start"});
            case "opportunity" -> opportunity.command(p, new String[]{"lab", "opportunity", "start"});
            case "overlay" -> overlay.command(p, new String[]{"lab", "overlay", "start"});
            case "night" -> night.command(p, new String[]{"lab", "night", "start"});
            case "scenario" -> scenario.command(p, new String[]{"lab", "scenario", "start"});
            case "terrain" -> { try { chamber(p, new String[]{"lab", "chamber", "take"}); } catch (IOException ex) { throw new IllegalStateException(ex); } }
            default -> throw new IllegalStateException("Unwired bench " + id);
        }
    }

    private void sendToRoom(Player p) {
        World w = ensureRoom();
        p.teleport(new Location(w, .5, 66, .5));
        p.setGameMode(GameMode.ADVENTURE);
    }

    private World ensureRoom() {
        if (room != null) return room;
        String name = plugin.getConfig().getString("alpha.lab.roomWorldName", "moba_lab");
        if (name.equals(plugin.worldInstance().instanceName())
                || name.equals(plugin.getConfig().getString("alpha.lab.instanceWorldName", "moba_lab_match"))
                || name.equals(plugin.getConfig().getString("features.lobbyWorld.name", "moba_lobby"))
                || name.equals(Bukkit.getWorlds().getFirst().getName()))
            throw new IllegalStateException("The lab room needs a separate, non-primary world name.");
        room = new WorldCreator(name).generator(new VoidGenerator()).generateStructures(false).createWorld();
        if (room == null) throw new IllegalStateException("Could not load the debug room.");
        room.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
        room.setGameRule(GameRule.DO_MOB_SPAWNING, false);
        room.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
        room.setTime(6000);
        room.setSpawnLocation(0, 66, 0);
        for (var piece : LobbyHall.hall(10, 64, 6, 3)) {
            Material material = switch (piece.role()) {
                case FLOOR -> Material.SMOOTH_STONE;
                case WALL, CEILING -> Material.STONE_BRICKS;
                case LIGHT -> Material.SEA_LANTERN;
                case PLATFORM -> Material.POLISHED_ANDESITE;
            };
            room.getBlockAt(piece.x(), piece.y(), piece.z()).setType(material, false);
        }
        room.getEntities().stream().filter(e -> e instanceof TextDisplay).forEach(org.bukkit.entity.Entity::remove);
        pedestal(-5, 0, Material.BOOKSHELF, "Classes");
        pedestal(5, 0, Material.CARTOGRAPHY_TABLE, "Map scoops");
        pedestal(0, 6, Material.EMERALD_BLOCK, "Launch test");
        for (var b : LabHub.BENCHES) pedestal(b.x(), b.z(), b.pedestal(), b.label());
        return room;
    }

    private void pedestal(int x, int z, Material material, String label) {
        room.getBlockAt(x, 65, z).setType(material, false);
        room.spawn(new Location(room, x+.5, 67, z+.5), TextDisplay.class, text -> {
            text.text(Component.text(label));
            text.setBillboard(org.bukkit.entity.Display.Billboard.CENTER);
        });
    }

    private void item(Menu holder, int slot, Material material, String title, String action, String... lore) {
        var stack = new ItemStack(material);
        stack.editMeta(meta -> {
            meta.displayName(Component.text(title));
            meta.lore(Arrays.stream(lore).map(Component::text).toList());
        });
        holder.inventory.setItem(slot, stack);
        if (action != null) holder.actions.put(slot, action);
    }

    private void menu(Player p, String kind, int page) {
        Selection s = selections.get(p.getUniqueId());
        Menu holder = new Menu(p.getUniqueId());
        holder.inventory = Bukkit.createInventory(holder, kind.equals("setup") || kind.equals("benches") ? 27 : 54,
                Component.text(kind.equals("setup") ? "Lab · Choose and launch" : "Lab · " + kind));
        if (kind.equals("setup")) {
            item(holder, 10, Material.BOOK, "Class: " + Objects.toString(s.classId, "choose"), "classes");
            item(holder, 12, Material.MAP, "Scoop: " + Objects.toString(s.mapId, "choose"), "maps", "Reusable templates; no production pool claims.");
            item(holder, 14, Material.WHITE_BANNER, "Team: " + s.team.lower(), "team " + (s.team == Team.NORTH ? "south" : "north"));
            int next = s.level < 5 ? 5 : s.level < 10 ? 10 : s.level < 20 ? 20 : 1;
            next = Math.min(next, plugin.settings().maxLevel());
            item(holder, 16, Material.EXPERIENCE_BOTTLE, "Starting level: " + s.level, "level " + next, "/moba lab level <n> for a specific level.");
            boolean ready = s.mapId != null && s.classId != null;
            item(holder, 22, ready ? Material.EMERALD_BLOCK : Material.BARRIER, "Launch test", "play", "Choose class and scoop first.");
            item(holder, 18, Material.COMPASS, "Benches", "benches", "Combat chamber, legibility, opportunity, terrain.");
            item(holder, 26, Material.OAK_DOOR, "Return to normal lobby", "leave");
        } else if (kind.equals("benches")) {
            int index = 0;
            for (var b : LabHub.BENCHES) {
                String why = LabHub.refusal(b, new LabHub.State(plugin.worldInstance().labActive()));
                item(holder, LabHub.menuSlot(index++), why == null ? b.pedestal() : Material.GRAY_DYE, b.label(), "bench " + b.id(),
                        why == null ? b.blurb() : "Unavailable: " + why);
            }
            item(holder, LabHub.BACK_SLOT, Material.OAK_DOOR, "Back to setup", "menu");
        } else {
            List<String> ids = kind.equals("classes") ? plugin.inputs().ids()
                    : maps.entries().stream().map(MapPool.Entry::mapId).toList();
            int start = Math.max(0, page) * 45;
            for (int i = start; i < Math.min(start+45, ids.size()); i++) {
                String id = ids.get(i);
                String label = kind.equals("classes") ? plugin.inputs().definition(id).displayName() : maps.label(maps.select(id));
                if (label == null || label.isBlank()) label = id;
                item(holder, i-start, kind.equals("classes") ? Material.BOOK : Material.MAP, label,
                        (kind.equals("classes") ? "class " : "map ") + id, id);
            }
            if (ids.isEmpty()) item(holder, 22, Material.BARRIER, "No lab scoops installed", null, "Install the certified scoop templates into " + maps.directory());
            if (page > 0) item(holder, 45, Material.ARROW, "Previous", "page:"+kind+":"+(page-1));
            item(holder, 49, Material.OAK_DOOR, "Back to setup", "menu");
            if (start+45 < ids.size()) item(holder, 53, Material.ARROW, "Next", "page:"+kind+":"+(page+1));
        }
        p.openInventory(holder.inventory);
    }

    @EventHandler public void click(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof Menu holder)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player p) || !holder.player.equals(p.getUniqueId()) || !p.hasPermission("moba.admin")) return;
        String action = holder.actions.get(event.getRawSlot());
        if (action == null) return;
        Bukkit.getScheduler().runTask(plugin, () -> {
            try {
                requireSetup(p);
                if (action.startsWith("page:")) {
                    String[] parts = action.split(":"); menu(p, parts[1], Integer.parseInt(parts[2]));
                } else command(p, ("lab " + action).split(" "));
            } catch (RuntimeException ex) { p.sendMessage("Lab refused: " + ex.getMessage()); }
        });
    }
    @EventHandler public void drag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof Menu) event.setCancelled(true);
    }
    @EventHandler public void interact(PlayerInteractEvent event) {
        if (room == null || !event.getPlayer().getWorld().equals(room) || event.getClickedBlock() == null) return;
        event.setCancelled(true);
        if (event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) return;
        String action = switch (event.getClickedBlock().getType()) {
            case BOOKSHELF -> "classes";
            case CARTOGRAPHY_TABLE -> "maps";
            case EMERALD_BLOCK -> "play";
            default -> { var b = LabHub.atBlock(event.getClickedBlock().getType()); yield b == null ? "menu" : "bench " + b.id(); }
        };
        // "bench <id>" is two words: passing it as one argument made the pedestals do nothing.
        if (event.getHand() == org.bukkit.inventory.EquipmentSlot.HAND) command(event.getPlayer(), ("lab " + action).split(" "));
    }
    @EventHandler public void breakBlock(BlockBreakEvent event) {
        if (room != null && event.getBlock().getWorld().equals(room)) event.setCancelled(true);
    }
    @EventHandler public void placeBlock(BlockPlaceEvent event) {
        if (room != null && event.getBlock().getWorld().equals(room)) event.setCancelled(true);
    }
}
