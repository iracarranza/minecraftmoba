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

/**
 * Everything a tester does INSIDE the combat chamber, without typing: hotbar items
 * and world buttons, as in the terrain chamber ({@link ChamberUi}).
 *
 * <ul>
 * <li>The hotbar becomes {@link CombatMenu}: use an item to run a verb or open a
 *     page, the last slot goes back. The tester's own hotbar is snapshotted and
 *     restored by {@link ChamberHotbar}.</li>
 * <li>A control deck beside the slab ({@link CombatConsole}) carries one button per
 *     verb, so the same actions are reachable in the world.</li>
 * <li>Leaving is an inventory item, not a hotbar one, for the terrain chamber's
 *     reason: a hotbar is scrolled past by accident.</li>
 * </ul>
 *
 * <h2>Right-click is A2 while ability mode is on</h2>
 *
 * Ability mode is entered with F and times out on its own. While it is on, a
 * right-click is the ability's, so the menu and the buttons stand back and say so;
 * otherwise a menu press would also cast. The typed commands still work, as before.
 *
 * NOT VERIFIED beyond the live acceptance run at the time of writing.
 */
final class CombatUi implements Listener {
    static final String BUTTON_TAG = "combat_button";
    private static final String VERB_PREFIX = "verb:";

    private final MobaPlugin plugin;
    private final CombatChamber chamber;
    private final ChamberHotbar hotbar;
    private final CombatConsole console = CombatConsole.of();

    CombatUi(MobaPlugin plugin, CombatChamber chamber) {
        this.plugin = plugin;
        this.chamber = chamber;
        this.hotbar = new ChamberHotbar(new NamespacedKey(plugin, "combat_item"));
    }

    boolean engaged(Player p) { return hotbar.engaged(p); }

    // ---- lifecycle --------------------------------------------------------------

    void engage(Player p, World w) {
        hotbar.engage(p, CombatMenu.menu(), "Leave chamber", "Leaves the combat chamber and returns to the lab room.", false);
        buildConsole(w);
        sync(p);
    }

    void release(Player p) {
        boolean wasEngaged = hotbar.engaged(p);
        hotbar.release(p);
        if (wasEngaged && hotbar.engagedIds().isEmpty()) {
            var w = Bukkit.getWorld(CombatChamber.WORLD);
            if (w != null) removeConsole(w);
        }
    }

    void close() {
        for (UUID id : hotbar.engagedIds()) {
            Player p = Bukkit.getPlayer(id);
            if (p != null) hotbar.release(p);
        }
        var w = Bukkit.getWorld(CombatChamber.WORLD);
        if (w != null) removeConsole(w);
        org.bukkit.event.HandlerList.unregisterAll(this);
    }

    /** Keep the items greyed or lit to match the chamber's actual state. */
    void sync(Player p) {
        if (hotbar.engaged(p)) hotbar.render(p, CombatMenu.gate(chamber.uiState(p)));
    }

    void syncAll() {
        for (UUID id : hotbar.engagedIds()) {
            Player p = Bukkit.getPlayer(id);
            if (p != null) sync(p);
        }
    }

    private void note(Player p, String text) {
        if (plugin.hudNotice() != null) plugin.hudNotice().show(p, text);
        p.sendMessage(text);
    }

    // ---- the console ------------------------------------------------------------

    private void buildConsole(World w) {
        for (var e : w.getEntities()) if (e.getScoreboardTags().contains(BUTTON_TAG)) return;   // already built
        var d = console.deck();
        for (int x = d.minX(); x <= d.maxX(); x++)
            for (int z = d.minZ(); z <= d.maxZ(); z++)
                w.getBlockAt(x, d.minY(), z).setType(Material.POLISHED_ANDESITE, false);
        for (var b : console.buttons()) {
            w.getBlockAt(b.at().x(), b.at().y(), b.at().z()).setType(b.block(), false);
            var colour = b.verb().equals(CombatMenu.LEAVE) ? NamedTextColor.RED : NamedTextColor.WHITE;
            w.spawn(new Location(w, b.at().x() + .5, b.at().y() + 1.3, b.at().z() + .5), TextDisplay.class, t -> {
                t.text(Component.text(b.label(), colour));
                t.setBillboard(Display.Billboard.CENTER);
                t.setPersistent(false);
                t.addScoreboardTag(BUTTON_TAG);
            });
            w.spawn(new Location(w, b.at().x() + .5, b.at().y(), b.at().z() + .5), Interaction.class, i -> {
                i.setInteractionWidth(1.0f);
                i.setInteractionHeight(1.0f);
                i.setResponsive(true);
                i.setPersistent(false);
                i.addScoreboardTag(BUTTON_TAG);
                i.addScoreboardTag(VERB_PREFIX + b.verb());
            });
        }
    }

    private void removeConsole(World w) {
        for (var e : w.getEntities()) if (e.getScoreboardTags().contains(BUTTON_TAG)) e.remove();
        var d = console.deck();
        for (int x = d.minX(); x <= d.maxX(); x++)
            for (int y = d.minY(); y <= d.minY() + 1; y++)
                for (int z = d.minZ(); z <= d.maxZ(); z++)
                    w.getBlockAt(x, y, z).setType(Material.AIR, false);
    }

    @EventHandler
    public void button(PlayerInteractEntityEvent e) {
        if (!(e.getRightClicked() instanceof Interaction i) || !i.getScoreboardTags().contains(BUTTON_TAG)) return;
        Player p = e.getPlayer();
        e.setCancelled(true);
        if (!hotbar.engaged(p)) return;
        String verb = i.getScoreboardTags().stream().filter(t -> t.startsWith(VERB_PREFIX))
                .map(t -> t.substring(VERB_PREFIX.length())).findFirst().orElse(null);
        if (verb == null) return;
        if (plugin.inputs().active(p)) { note(p, "Ability mode is on (F toggles it); the click would cast. Press F, then the button."); return; }
        run(p, verb);
    }

    // ---- the hotbar -------------------------------------------------------------

    @EventHandler(priority = EventPriority.HIGH)
    public void use(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        if (e.getHand() != EquipmentSlot.HAND || !hotbar.engaged(p)) return;
        if (e.getAction() != Action.RIGHT_CLICK_AIR && e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (hotbar.idOf(p.getInventory().getItemInMainHand()) == null) return;
        // Right-click is A2 while ability mode is on: stand back rather than also cast.
        if (plugin.inputs().active(p)) return;
        e.setCancelled(true);
        var menu = hotbar.menu(p);
        var choice = menu.choose(p.getInventory().getHeldItemSlot(), CombatMenu.gate(chamber.uiState(p)));
        if (choice.kind() == null) return;
        if (choice.refused()) { note(p, choice.refusal()); return; }
        if (choice.kind() == ChamberMenu.Kind.ACTION) {
            chamber.runVerb(p, choice.itemId());
            menu.reset();
        }
        sync(p);
    }

    private void run(Player p, String verb) {
        String refusal = CombatMenu.refusal(verb, chamber.uiState(p));
        if (refusal != null) { note(p, refusal); return; }
        chamber.runVerb(p, verb);
        sync(p);
    }

    // ---- keeping the menu intact -------------------------------------------------

    @EventHandler(priority = EventPriority.HIGH)
    public void click(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p) || !hotbar.engaged(p)) return;
        String current = hotbar.idOf(e.getCurrentItem());
        String cursor = hotbar.idOf(e.getCursor());
        if (current == null && cursor == null) return;
        e.setCancelled(true);
        if ("return".equals(current))
            Bukkit.getScheduler().runTask(plugin, () -> { p.closeInventory(); chamber.leave(p); });
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

    /** A tester who dies keeps the menu rather than dropping it. */
    @EventHandler
    public void death(PlayerDeathEvent e) {
        if (!hotbar.engaged(e.getEntity())) return;
        e.setKeepInventory(true);
        e.getDrops().clear();
    }
}
