package com.minecraftmoba.plugin;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

/**
 * Settings you hold, in the one place holding things is free.
 *
 * <h2>Why the lobby, and why an item</h2>
 *
 * The lobby now carries <b>no MOBA state at all</b> -- no offhand map, no
 * locked slots, no vitals, no progression readout -- so the inventory there is
 * the player's own and has room for something that is not a menu.
 *
 * An immovable item is the same device the offhand map already uses, and it
 * has a property a chest GUI does not: it is <b>visible without being opened</b>.
 * A player who has never heard of cast modes still sees the item, and a player
 * who has can change it in one click without leaving where they are standing.
 *
 * <h2>The row is settings, not a menu</h2>
 *
 * Each slot is one setting, clicked to cycle. No pages, no confirm, no close.
 * Adding a second setting is adding a slot, which is the property that keeps
 * this from growing into the rewards chest it is deliberately not.
 *
 * <h2>Preferences outlive matches</h2>
 *
 * Stored in {@code PlayerData.settings} rather than {@code classState}, which
 * a match reset clears. A cast mode is a statement about how a person plays; it
 * must survive a reset, a class change and a new match, or every match begins
 * by re-choosing an input scheme.
 */
public final class LobbySettings implements Listener {

    private final MobaPlugin plugin;
    private final NamespacedKey key;

    public LobbySettings(MobaPlugin plugin) {
        this.plugin = plugin;
        this.key = new NamespacedKey(plugin, "lobby_setting");
    }

    private boolean enabled() {
        return plugin.getConfig().getBoolean("features.lobbySettings.enabled", true);
    }

    /** The slot the cast-mode toggle occupies. Config, not a constant. */
    private int slot() {
        return plugin.getConfig().getInt("features.lobbySettings.castModeSlot", 8);
    }

    /** Settings belong where MOBA state does not: the lobby, not the match. */
    private boolean applies(Player p) {
        return enabled() && plugin.enrolled(p) && !plugin.inMatchState(p);
    }

    // ---- the item ---------------------------------------------------------

    /** Put the row in place, or take it away when it does not belong. */
    public void refresh(Player p) {
        var inventory = p.getInventory();
        ItemStack present = inventory.getItem(slot());
        boolean ours = isSetting(present);

        if (!applies(p)) {
            if (ours) inventory.setItem(slot(), null);
            return;
        }
        // Never displace something the player is carrying. The lobby inventory
        // is theirs, and a settings item that ate an item would be a worse
        // trade than the setting is worth.
        if (present != null && !present.getType().isAir() && !ours) return;
        inventory.setItem(slot(), item(p));
    }

    private ItemStack item(Player p) {
        var data = plugin.data(p);
        CastMode mode = data == null ? CastMode.DEFAULT : data.castMode();
        ItemStack item = new ItemStack(Material.COMPARATOR);
        item.editMeta(meta -> {
            meta.displayName(net.kyori.adventure.text.Component.text(mode.label()));
            meta.lore(List.of(
                    net.kyori.adventure.text.Component.text(mode.description()),
                    net.kyori.adventure.text.Component.text("Click to change.")));
            meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, "castMode");
        });
        return item;
    }

    private boolean isSetting(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(key, PersistentDataType.STRING);
    }

    // ---- clicking it ------------------------------------------------------

    private void cycle(Player p) {
        var data = plugin.data(p);
        if (data == null) return;
        CastMode next = data.castMode().next();
        data.settings.put("castMode", next.name());
        plugin.applyAndSave(p);
        refresh(p);
        p.sendMessage(next.label() + " — " + next.description());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void use(PlayerInteractEvent e) {
        if (!applies(e.getPlayer()) || !isSetting(e.getItem())) return;
        e.setCancelled(true);
        cycle(e.getPlayer());
    }

    /**
     * Immovable, the same way the offhand map is.
     *
     * A setting that can be dropped is a setting a player loses by accident and
     * then cannot find, in the one place the inventory is otherwise entirely
     * theirs to rearrange.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void click(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p) || !applies(p)) return;
        if (isSetting(e.getCurrentItem()) || isSetting(e.getCursor())) {
            e.setCancelled(true);
            if (isSetting(e.getCurrentItem())) cycle(p);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void drop(PlayerDropItemEvent e) {
        if (isSetting(e.getItemDrop().getItemStack())) e.setCancelled(true);
    }
}
