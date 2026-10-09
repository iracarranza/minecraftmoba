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

    public Lab(MobaPlugin plugin) {
        this.plugin = plugin;
        authoring = new LabAuthoring(plugin, this);
        Bukkit.getPluginManager().registerEvents(authoring, plugin);
        combat = new CombatChamber(plugin, this);
        Bukkit.getPluginManager().registerEvents(combat, plugin);
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
                case "combat" -> combat.command(p, args);
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
                default -> p.sendMessage("/moba lab start | combat | maps | classes | map <id/number> | class <id> | level <n> | team <north/south> | play | chamber | end | leave | status");
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
    public void close() { combat.close(); }

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
        holder.inventory = Bukkit.createInventory(holder, kind.equals("setup") ? 27 : 54,
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
            item(holder, 26, Material.OAK_DOOR, "Return to normal lobby", "leave");
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
            default -> "menu";
        };
        if (event.getHand() == org.bukkit.inventory.EquipmentSlot.HAND) command(event.getPlayer(), new String[]{"lab", action});
    }
    @EventHandler public void breakBlock(BlockBreakEvent event) {
        if (room != null && event.getBlock().getWorld().equals(room)) event.setCancelled(true);
    }
    @EventHandler public void placeBlock(BlockPlaceEvent event) {
        if (room != null && event.getBlock().getWorld().equals(room)) event.setCancelled(true);
    }
}
