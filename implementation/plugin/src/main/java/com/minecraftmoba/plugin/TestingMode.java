package com.minecraftmoba.plugin;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Combat testing mode: the tester's inventory holds a loadout and one item, "Exit testing".
 *
 * Entering takes the real inventory away (parked on disk first, see {@link LoadoutStore}), gives the
 * selected loadout and puts the exit item in the reserved slot. Nothing else is on the tester: no
 * menu, no debugging items. Everything is still reachable by command, which is the point of having
 * both. Leaving gives the real inventory back exactly.
 *
 * <h2>The real inventory is never only in memory</h2>
 *
 * It is parked in a file before it is cleared, restored on exit, on quit, on death-free teardown and,
 * if the server died in between, at the next join.
 *
 * NOT VERIFIED beyond the live acceptance run at the time of writing.
 */
final class TestingMode implements Listener {
    static final String EXIT_ID = "exit_testing";

    private final MobaPlugin plugin;
    private final LoadoutStore store;
    private final NamespacedKey key;
    private final Consumer<Player> onExit;
    /** Testers in testing mode, with the real inventory held in memory as well as on disk. */
    private final Map<UUID, List<ItemStack>> parked = new HashMap<>();

    private final TestingHud hud;

    TestingMode(MobaPlugin plugin, LoadoutStore store, Consumer<Player> onExit, java.util.function.Function<Player, Player> dummyOf) {
        this.plugin = plugin;
        this.store = store;
        this.hud = new TestingHud(plugin, store, dummyOf);
        this.key = new NamespacedKey(plugin, "testing_item");
        this.onExit = onExit;
    }

    boolean active(Player p) { return parked.containsKey(p.getUniqueId()); }

    // ---- entering and leaving ---------------------------------------------------------------

    /** Park the real inventory, then give the loadout and the exit item. */
    void enter(Player p, Loadout<ItemStack> loadout) {
        if (active(p)) throw new IllegalStateException("Already testing.");
        var real = LoadoutGear.captureAll(p);
        store.park(p.getUniqueId(), real);            // throws before anything is cleared if it cannot
        parked.put(p.getUniqueId(), real);
        give(p, loadout);
        hud.start(p);
    }

    /** The sidebar's lines as drawn now (loadout, health, armor, the dummy), for assertions. Null when not testing. */
    java.util.List<String> hudLines(Player p) { return active(p) ? hud.linesFor(p) : null; }

    /** Give the loadout again (consumed gear, a dropped item), keeping the exit item. */
    void refill(Player p, Loadout<ItemStack> loadout) {
        if (!active(p)) throw new IllegalStateException("Refilling is for testing mode.");
        give(p, loadout);
    }

    private void give(Player p, Loadout<ItemStack> loadout) {
        LoadoutGear.give(p, loadout);
        p.getInventory().setItem(Loadout.EXIT_SLOT, exitItem());
        p.updateInventory();
    }

    /** Give the real inventory back and forget it. Returns false when the tester was not testing. */
    boolean exit(Player p) {
        var real = parked.remove(p.getUniqueId());
        if (real == null) return false;
        hud.stop(p);
        LoadoutGear.give(p, real);
        store.discardParked(p.getUniqueId());
        p.updateInventory();
        return true;
    }

    void exitAll() {
        for (UUID id : List.copyOf(parked.keySet())) {
            Player p = plugin.getServer().getPlayer(id);
            if (p != null) exit(p);
        }
    }

    /** At join: an inventory parked by a session that never finished goes back now. */
    void recover(Player p) {
        if (active(p) || !store.hasParked(p.getUniqueId())) return;
        var real = store.parked(p.getUniqueId());
        if (real == null) return;                      // unreadable: leave the file for a person to look at
        LoadoutGear.give(p, real);
        store.discardParked(p.getUniqueId());
        p.sendMessage(org.bukkit.ChatColor.GREEN + "Your inventory from an unfinished testing session was restored.");
    }

    // ---- the exit item ----------------------------------------------------------------------

    private ItemStack exitItem() {
        var item = new ItemStack(Material.BARRIER);
        item.editMeta(meta -> {
            meta.displayName(Component.text("Exit testing", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(Component.text("Gives you your own inventory back.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)));
            meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, EXIT_ID);
        });
        return item;
    }

    private boolean isExit(ItemStack s) {
        if (s == null || !s.hasItemMeta()) return false;
        return EXIT_ID.equals(s.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.STRING));
    }

    // ---- guarding it ------------------------------------------------------------------------

    @EventHandler(priority = EventPriority.HIGH)
    public void use(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        if (!active(p) || e.getHand() != EquipmentSlot.HAND) return;
        if (e.getAction() != Action.RIGHT_CLICK_AIR && e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (!isExit(p.getInventory().getItemInMainHand())) return;
        e.setCancelled(true);
        onExit.accept(p);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void click(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p) || !active(p)) return;
        boolean exitInvolved = isExit(e.getCurrentItem()) || isExit(e.getCursor());
        if (e.getClick() == org.bukkit.event.inventory.ClickType.NUMBER_KEY && e.getHotbarButton() >= 0
                && isExit(p.getInventory().getItem(e.getHotbarButton()))) exitInvolved = true;
        if (!exitInvolved) return;
        e.setCancelled(true);
        if (isExit(e.getCurrentItem()))
            plugin.getServer().getScheduler().runTask(plugin, () -> { p.closeInventory(); onExit.accept(p); });
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void drag(InventoryDragEvent e) {
        if (e.getWhoClicked() instanceof Player p && active(p)
                && (isExit(e.getOldCursor()) || e.getNewItems().values().stream().anyMatch(this::isExit)))
            e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void drop(PlayerDropItemEvent e) {
        if (active(e.getPlayer()) && isExit(e.getItemDrop().getItemStack())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void swap(PlayerSwapHandItemsEvent e) {
        if (active(e.getPlayer()) && (isExit(e.getMainHandItem()) || isExit(e.getOffHandItem()))) e.setCancelled(true);
    }

    /** A tester who dies keeps the loadout rather than dropping it, so the exit item survives too. */
    @EventHandler
    public void death(PlayerDeathEvent e) {
        if (!active(e.getEntity())) return;
        e.setKeepInventory(true);
        e.getDrops().clear();
        e.setKeepLevel(true);
        e.setDroppedExp(0);
    }

    @EventHandler public void quit(PlayerQuitEvent e) { exit(e.getPlayer()); }

    @EventHandler public void join(PlayerJoinEvent e) { recover(e.getPlayer()); }
}
