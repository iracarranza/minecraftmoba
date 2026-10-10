package com.minecraftmoba.plugin;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
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
import org.bukkit.inventory.EquipmentSlot;

import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * The in-bench UI every bench shares: hotbar items, world buttons on a deck, and Leave
 * in the inventory. A bench supplies its menu, its deck, and what a verb does; this
 * supplies the events, the snapshot and restore of the tester's own hotbar, and the
 * deck's blocks and entities.
 *
 * It is the generic form of {@link CombatUi}, which predates it and has not yet been
 * moved onto it.
 */
final class BenchUi implements Listener {
    /** What a bench tells the UI. */
    record Spec(String id, java.util.function.Supplier<ChamberMenu> menu, Function<Player, ChamberMenu.Gate> gate,
                Refuser refusal, DeckLayout deck, String worldName,
                BiConsumer<Player, String> run, Consumer<Player> leave, String leaveName, String leaveLore,
                java.util.function.Predicate<Player> standBack) {
        /** The common case: nothing competes with right-click, so the menu always takes it. */
        Spec(String id, java.util.function.Supplier<ChamberMenu> menu, Function<Player, ChamberMenu.Gate> gate,
             Refuser refusal, DeckLayout deck, String worldName,
             BiConsumer<Player, String> run, Consumer<Player> leave, String leaveName, String leaveLore) {
            this(id, menu, gate, refusal, deck, worldName, run, leave, leaveName, leaveLore, p -> false);
        }
    }

    /** A verb's refusal for a tester, or null. */
    interface Refuser { String refuse(Player p, String verb); }

    private final MobaPlugin plugin;
    private final Spec spec;
    private final String buttonTag;
    private static final String VERB = "verb:";
    private final ChamberHotbar hotbar;

    BenchUi(MobaPlugin plugin, Spec spec) {
        this.plugin = plugin;
        this.spec = spec;
        this.buttonTag = spec.id() + "_button";
        this.hotbar = new ChamberHotbar(new NamespacedKey(plugin, spec.id() + "_item"));
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    boolean engaged(Player p) { return hotbar.engaged(p); }

    void engage(Player p, World w) {
        hotbar.engage(p, spec.menu().get(), spec.leaveName(), spec.leaveLore(), false);
        buildDeck(w);
        sync(p);
    }

    void release(Player p) {
        boolean was = hotbar.engaged(p);
        hotbar.release(p);
        if (was && hotbar.engagedIds().isEmpty()) {
            var w = Bukkit.getWorld(spec.worldName());
            if (w != null) removeDeck(w);
        }
    }

    void close() {
        for (UUID id : hotbar.engagedIds()) {
            Player p = Bukkit.getPlayer(id);
            if (p != null) hotbar.release(p);
        }
        var w = Bukkit.getWorld(spec.worldName());
        if (w != null) removeDeck(w);
        HandlerList.unregisterAll(this);
    }

    void sync(Player p) {
        if (hotbar.engaged(p)) hotbar.render(p, spec.gate().apply(p));
    }

    private void note(Player p, String text) {
        if (plugin.hudNotice() != null) plugin.hudNotice().show(p, text);
        p.sendMessage(text);
    }

    // ---- the deck -----------------------------------------------------------------

    private void buildDeck(World w) {
        for (var e : w.getEntities()) if (e.getScoreboardTags().contains(buttonTag)) return;
        var d = spec.deck().deck();
        for (int x = d.minX(); x <= d.maxX(); x++)
            for (int z = d.minZ(); z <= d.maxZ(); z++)
                w.getBlockAt(x, d.minY(), z).setType(Material.POLISHED_ANDESITE, false);
        for (var b : spec.deck().buttons()) {
            w.getBlockAt(b.at().x(), b.at().y(), b.at().z()).setType(b.block(), false);
            var colour = b.verb().equals("leave") ? NamedTextColor.RED : NamedTextColor.WHITE;
            w.spawn(new Location(w, b.at().x() + .5, b.at().y() + 1.3, b.at().z() + .5), TextDisplay.class, t -> {
                t.text(Component.text(b.label(), colour));
                t.setBillboard(Display.Billboard.CENTER);
                t.setPersistent(false);
                t.addScoreboardTag(buttonTag);
            });
            w.spawn(new Location(w, b.at().x() + .5, b.at().y(), b.at().z() + .5), Interaction.class, i -> {
                i.setInteractionWidth(1.0f);
                i.setInteractionHeight(1.0f);
                i.setResponsive(true);
                i.setPersistent(false);
                i.addScoreboardTag(buttonTag);
                i.addScoreboardTag(VERB + b.verb());
            });
        }
    }

    private void removeDeck(World w) {
        for (var e : w.getEntities()) if (e.getScoreboardTags().contains(buttonTag)) e.remove();
        var d = spec.deck().deck();
        for (int x = d.minX(); x <= d.maxX(); x++)
            for (int y = d.minY(); y <= d.minY() + 1; y++)
                for (int z = d.minZ(); z <= d.maxZ(); z++)
                    w.getBlockAt(x, y, z).setType(Material.AIR, false);
    }

    @EventHandler
    public void button(PlayerInteractEntityEvent e) {
        if (!(e.getRightClicked() instanceof Interaction i) || !i.getScoreboardTags().contains(buttonTag)) return;
        Player p = e.getPlayer();
        e.setCancelled(true);
        if (!hotbar.engaged(p)) return;
        String verb = i.getScoreboardTags().stream().filter(t -> t.startsWith(VERB))
                .map(t -> t.substring(VERB.length())).findFirst().orElse(null);
        if (verb != null) run(p, verb);
    }

    private void run(Player p, String verb) {
        String refusal = spec.refusal().refuse(p, verb);
        if (refusal != null) { note(p, refusal); return; }
        spec.run().accept(p, verb);
        sync(p);
    }

    // ---- the hotbar ---------------------------------------------------------------

    @EventHandler(priority = EventPriority.HIGH)
    public void use(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        if (e.getHand() != EquipmentSlot.HAND || !hotbar.engaged(p)) return;
        if (e.getAction() != Action.RIGHT_CLICK_AIR && e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (hotbar.idOf(p.getInventory().getItemInMainHand()) == null) return;
        // A bench whose tester has a class (right-click is A2 while ability mode is on) stands back.
        if (spec.standBack().test(p)) return;
        e.setCancelled(true);
        var menu = hotbar.menu(p);
        var choice = menu.choose(p.getInventory().getHeldItemSlot(), spec.gate().apply(p));
        if (choice.kind() == null) return;
        if (choice.refused()) { note(p, choice.refusal()); return; }
        if (choice.kind() == ChamberMenu.Kind.ACTION) { spec.run().accept(p, choice.itemId()); menu.reset(); }
        sync(p);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void click(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p) || !hotbar.engaged(p)) return;
        String current = hotbar.idOf(e.getCurrentItem());
        String cursor = hotbar.idOf(e.getCursor());
        if (current == null && cursor == null) return;
        e.setCancelled(true);
        if ("return".equals(current))
            Bukkit.getScheduler().runTask(plugin, () -> { p.closeInventory(); spec.leave().accept(p); });
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

    @EventHandler
    public void death(PlayerDeathEvent e) {
        if (!hotbar.engaged(e.getEntity())) return;
        e.setKeepInventory(true);
        e.getDrops().clear();
    }
}
