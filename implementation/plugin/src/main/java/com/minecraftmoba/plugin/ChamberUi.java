package com.minecraftmoba.plugin;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.scheduler.BukkitTask;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * The chamber as a place: a platform to look from, a door to walk through, and a
 * hotbar that is a menu once you are inside.
 *
 * <h2>What lives where</h2>
 *
 * Every rule is in a class tested without a server: {@link ChamberMenu} (pages),
 * {@link ChamberController} (what an item means, and why it may not be used),
 * {@link ChamberPlatform} (geometry) and {@link RegionCopy} (terrain mapping).
 * This class owns events and the lifecycle, and nothing else.
 *
 * <h2>Routing</h2>
 *
 * Placement verbs are not reimplemented. A hotbar item runs the same
 * {@code /moba lab author} command a typed one does, so the preview a tester
 * sees, the plan it was judged against and the undo it enters are
 * {@link LabAuthoring}'s. The chamber adds a verdict on top: LabAuthoring
 * offers its plan to {@link ChamberWorkspace#propose} and consults the
 * workspace before committing, so a placement that leaves the bay is refused
 * while it is still a proposal.
 *
 * <h2>Not built</h2>
 *
 * <b>Redo</b> has no journal ({@link LabUndo} is one-way), so it is shown
 * greyed with that reason. The <b>holographic footprint</b> is not built:
 * LabAuthoring's client-side block preview already shows the real blocks, and a
 * BLOCK_DISPLAY ghost is a different mechanism with its own entity cost.
 *
 * <b>Test as player</b> returns the tester's own hotbar and gamemode but cannot
 * resume the match clock LabAuthoring paused; Match has no resume.
 *
 * NOT VERIFIED AGAINST A LIVE SERVER at the time of writing: the events, the
 * platform build and the terrain copy have compiled but not been exercised by a
 * client.
 */
final class ChamberUi implements Listener {
    private final MobaPlugin plugin;
    private final Lab lab;
    private final ChamberWorkspace workspace;
    private final ChamberHotbar hotbar;
    private final BayTerrain terrain;
    private final BukkitTask sync;

    private final Map<UUID, Integer> certifiedIndex = new HashMap<>();
    private final Map<UUID, BayTerrain.Source> lastSource = new HashMap<>();
    private final Map<UUID, ChamberPlatform> platforms = new HashMap<>();
    /** The pending placement a tester backed out of, so it is not reopened under them. */
    private final Map<UUID, Object> dismissed = new HashMap<>();
    private final Set<UUID> busy = new HashSet<>();
    private final Set<String> verified = new HashSet<>();

    ChamberUi(MobaPlugin plugin, Lab lab, ChamberWorkspace workspace) {
        this.plugin = plugin;
        this.lab = lab;
        this.workspace = workspace;
        this.hotbar = new ChamberHotbar(new NamespacedKey(plugin, "chamber_item"));
        this.terrain = new BayTerrain(plugin, lab.authoring());
        Bukkit.getPluginManager().registerEvents(this, plugin);
        // Keeps the hotbar honest when state changes by a typed command rather
        // than a click. Cheap: it only rewrites a hotbar whose view changed.
        this.sync = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (UUID id : hotbar.engagedIds()) {
                Player p = Bukkit.getPlayer(id);
                if (p != null) sync(p);
            }
        }, 10L, 10L);
    }

    boolean inBay(Player p) { return hotbar.engaged(p); }

    // ---- lifecycle --------------------------------------------------------------

    /** Allot a bay, build its platform, and stand the tester on it. */
    void take(Player p) {
        leaveInternal(p, false);
        Chamber bay = workspace.allot(p);
        World w = Bukkit.getWorld(bay.world());
        var plat = ChamberPlatform.of(bay, Chambers.GUTTER);
        platforms.put(p.getUniqueId(), plat);
        if (w != null) ChamberPlatformWorld.build(w, plat, p.getUniqueId());
        Location stand = new Location(w, plat.doorX() + .5, plat.deckY() + 1,
                plat.deck().minZ() + 2.5, 0f, 10f);
        p.teleport(stand);
        p.sendMessage("Chamber allotted. Look down into the bay through the glass. "
                + "Click CERTIFIED SCOOP or RANDOM SEED to fill it, then step on the pressure plate to go in.");
    }

    /** The door: into the bay, with the hotbar becoming the menu. */
    void enterBay(Player p) {
        Chamber bay = workspace.chambers().of(p.getUniqueId());
        if (bay == null) return;
        Location where = workspace.bayEntry(bay);
        if (where == null) return;
        hotbar.engage(p);
        p.teleport(where);
        sync(p);
        p.sendMessage("Inside your bay. Hold and use an item to open its page; the last slot goes back. "
                + "Return to lab and Test as player are in your inventory.");
    }

    /** Hand the bay and the tester's own hotbar back. */
    void leave(Player p) { leaveInternal(p, true); }

    private void leaveInternal(Player p, boolean releaseBay) {
        UUID id = p.getUniqueId();
        hotbar.release(p);
        dismissed.remove(id);
        var plat = platforms.remove(id);
        Chamber bay = workspace.chambers().of(id);
        if (bay != null) {
            var w = Bukkit.getWorld(bay.world());
            if (w != null) ChamberPlatformWorld.remove(w, id);
        }
        if (releaseBay || plat != null) workspace.leave(p);
        lastSource.remove(id);
        certifiedIndex.remove(id);
    }

    void close() {
        sync.cancel();
        for (UUID id : hotbar.engagedIds()) {
            Player p = Bukkit.getPlayer(id);
            if (p != null) hotbar.release(p);
        }
        HandlerList.unregisterAll(this);
    }

    // ---- state ------------------------------------------------------------------

    private ChamberController.State state(Player p) {
        return new ChamberController.State(hotbar.engaged(p), lab.authoring().undoDepth(),
                !workspace.session(p).idle(), lastSource.containsKey(p.getUniqueId()),
                plugin.match() != null && plugin.match().labPaused(), lab.rules().hungerFrozen(p), lab.rules().regenOff(p));
    }

    /** Keep the page and the items matching what is actually pending. */
    private void sync(Player p) {
        var menu = hotbar.menu(p);
        if (menu == null) return;
        var pending = workspace.session(p).pending();
        UUID id = p.getUniqueId();
        if (pending != null && !menu.pageId().equals("pending") && dismissed.get(id) != pending)
            menu.open("pending");
        else if (pending == null && menu.pageId().equals("pending")) menu.reset();
        hotbar.render(p, ChamberController.gate(state(p)));
    }

    private void note(Player p, String text) {
        if (plugin.hudNotice() != null) plugin.hudNotice().show(p, text);
        p.sendMessage(text);
    }

    // ---- the hotbar --------------------------------------------------------------

    @EventHandler(priority = EventPriority.HIGH)
    public void use(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        if (e.getHand() != EquipmentSlot.HAND || !hotbar.engaged(p)) return;
        if (e.getAction() != Action.RIGHT_CLICK_AIR && e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        String held = hotbar.idOf(p.getInventory().getItemInMainHand());
        if (held == null) return;
        e.setCancelled(true);

        var menu = hotbar.menu(p);
        var wasPending = menu.pageId().equals("pending");
        var choice = menu.choose(p.getInventory().getHeldItemSlot(), ChamberController.gate(state(p)));
        if (choice.kind() == null) return;
        if (choice.refused()) { note(p, choice.refusal()); return; }
        switch (choice.kind()) {
            case BACK -> {
                // Backing out of a pending placement leaves it pending but stops
                // the menu reopening it, until a new proposal replaces it.
                if (wasPending && !workspace.session(p).idle())
                    dismissed.put(p.getUniqueId(), workspace.session(p).pending());
            }
            case SUBMENU -> { }
            case ACTION -> perform(p, choice.itemId());
        }
        sync(p);
    }

    private void perform(Player p, String itemId) {
        var intent = ChamberController.intent(itemId);
        switch (intent.kind()) {
            case AUTHOR -> author(p, intent.args());
            case SHOW_AGAIN -> workspace.show(p, null);
            case CONFIRM -> author(p, List.of("apply"));
            case CANCEL -> { author(p, List.of("cancel")); hotbar.menu(p).reset(); }
            case REGENERATE_LAST -> {
                var source = lastSource.get(p.getUniqueId());
                if (source != null) regenerate(p, source);
            }
            case LAB -> {
                var args = new java.util.ArrayList<String>(List.of("lab"));
                args.addAll(intent.args());
                // The rules toggle: the argument is the opposite of the current setting.
                if (itemId.equals("rules.hunger")) { args.add("hunger"); args.add(lab.rules().hungerFrozen(p) ? "normal" : "freeze"); }
                if (itemId.equals("rules.regen")) { args.add("regen"); args.add(lab.rules().regenOff(p) ? "normal" : "off"); }
                lab.command(p, args.toArray(new String[0]));
            }
            case UNAVAILABLE -> note(p, ChamberController.refusal(itemId, state(p)) == null
                    ? "That has no effect yet." : ChamberController.refusal(itemId, state(p)));
        }
        if (intent.kind() == ChamberController.Kind.CONFIRM && workspace.session(p).idle())
            hotbar.menu(p).reset();
    }

    /** The same command a typed one is, so there is one authoring path. */
    private void author(Player p, List<String> args) {
        var full = new java.util.ArrayList<String>(List.of("lab", "author"));
        full.addAll(args);
        lab.command(p, full.toArray(new String[0]));
    }

    // ---- platform ---------------------------------------------------------------

    @EventHandler
    public void walkOnPlate(PlayerInteractEvent e) {
        if (e.getAction() != Action.PHYSICAL || e.getClickedBlock() == null) return;
        Player p = e.getPlayer();
        var plat = platforms.get(p.getUniqueId());
        if (plat == null || hotbar.engaged(p)) return;
        var at = e.getClickedBlock().getLocation();
        if (at.getBlockX() == plat.plate().x() && at.getBlockY() == plat.plate().y()
                && at.getBlockZ() == plat.plate().z()) {
            e.setCancelled(true);
            enterBay(p);
        }
    }

    @EventHandler
    public void button(PlayerInteractEntityEvent e) {
        if (!(e.getRightClicked() instanceof Interaction i)) return;
        var tags = i.getScoreboardTags();
        Player p = e.getPlayer();
        if (!tags.contains(ChamberPlatformWorld.BUTTON_TAG) || !tags.contains(p.getUniqueId().toString())) return;
        e.setCancelled(true);
        if (hotbar.engaged(p)) { note(p, "Leave the bay to use the platform."); return; }
        if (tags.contains(ChamberPlatformWorld.CERTIFIED)) pressCertified(p);
        else if (tags.contains(ChamberPlatformWorld.RANDOM)) pressRandom(p);
    }

    private void pressCertified(Player p) {
        var entries = lab.maps().entries();
        if (entries.isEmpty()) { note(p, "No certified scoops are installed in " + lab.maps().directory()); return; }
        UUID id = p.getUniqueId();
        int next = (certifiedIndex.getOrDefault(id, -1) + 1) % entries.size();
        certifiedIndex.put(id, next);
        var entry = entries.get(next);
        String label = lab.maps().label(entry);
        regenerate(p, new BayTerrain.Source.Certified(entry, label));
        relabel(p, "certified", "CERTIFIED: " + label + " (" + (next + 1) + "/" + entries.size() + ")");
    }

    private void pressRandom(Player p) {
        long seed = BayTerrain.newSeed();
        regenerate(p, new BayTerrain.Source.Random(seed));
        relabel(p, "random", "RANDOM SEED: " + Long.toHexString(seed & 0xFFFFFFL));
    }

    private void relabel(Player p, String which, String line) {
        Chamber bay = workspace.chambers().of(p.getUniqueId());
        var w = bay == null ? null : Bukkit.getWorld(bay.world());
        if (w != null) ChamberPlatformWorld.relabel(w, p.getUniqueId(), which, line);
    }

    private void regenerate(Player p, BayTerrain.Source source) {
        UUID id = p.getUniqueId();
        Chamber bay = workspace.chambers().of(id);
        if (bay == null) { note(p, "No bay allotted."); return; }
        if (!busy.add(id)) { note(p, "Still regenerating."); return; }
        try {
            if (source instanceof BayTerrain.Source.Certified c && verified.add(c.entry().mapId()))
                lab.maps().verify(c.entry());
            terrain.regenerate(p, bay, source);
            lastSource.put(id, source);
        } catch (IOException | RuntimeException ex) {
            verified.remove(source instanceof BayTerrain.Source.Certified c ? c.entry().mapId() : "");
            note(p, "Regeneration refused: " + ex.getMessage());
            plugin.getLogger().warning("[chamber] " + ex);
        } finally { busy.remove(id); }
    }

    // ---- keeping the menu intact -------------------------------------------------

    @EventHandler(priority = EventPriority.HIGH)
    public void click(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p) || !hotbar.engaged(p)) return;
        String current = hotbar.idOf(e.getCurrentItem());
        String cursor = hotbar.idOf(e.getCursor());
        if (current == null && cursor == null) return;
        e.setCancelled(true);
        if ("return".equals(current)) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                p.closeInventory();
                leave(p);
                lab.command(p, new String[]{"lab", "end"});
            });
        } else if ("playtest".equals(current)) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                p.closeInventory();
                leave(p);
                p.setGameMode(GameMode.SURVIVAL);
                p.sendMessage("Your own hotbar is back. The match clock stays paused; "
                        + "/moba lab end returns to setup.");
            });
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void drag(InventoryDragEvent e) {
        if (e.getWhoClicked() instanceof Player p && hotbar.engaged(p)
                && (hotbar.idOf(e.getOldCursor()) != null
                || e.getNewItems().values().stream().anyMatch(s -> hotbar.idOf(s) != null)))
            e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void drop(PlayerDropItemEvent e) {
        if (hotbar.engaged(e.getPlayer()) && hotbar.idOf(e.getItemDrop().getItemStack()) != null)
            e.setCancelled(true);
    }

    /** A tester who dies keeps what they had, menu included, rather than dropping it. */
    @EventHandler
    public void death(PlayerDeathEvent e) {
        if (!hotbar.engaged(e.getEntity())) return;
        e.setKeepInventory(true);
        e.getDrops().clear();
    }

    @EventHandler
    public void quit(PlayerQuitEvent e) { leaveInternal(e.getPlayer(), true); }
}
