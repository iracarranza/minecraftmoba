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
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

/**
 * Kitfighter's Offhander item: the offhand's map, replaced.
 *
 * <h2>What it is</h2>
 *
 * An enrolled player carries the plugin's own map in the offhand. Offhander puts the branch's item
 * (a Bow, or the Crossbow, Fishing Rod or Golden Head branch) THERE INSTEAD, and pressing it again
 * takes it away and the map comes back. The item is issued, not found: nothing in the inventory is
 * needed, taken or moved, so there is nothing to duplicate and no exchange to get wrong.
 *
 * <h2>Why the map has to be explicitly exempted</h2>
 *
 * The plugin PAUSES a player's enrolment (class, abilities, progression) when the offhand holds
 * anything but its own map. The first Offhander put a real Crossbow there and, a second later, the
 * Kitfighter had no class: caught live, {@code enrolled=false paused=true}. {@link OffhandMap#ensure}
 * now treats this item as the offhand's rightful occupant.
 *
 * <h2>It cannot be lost, moved or kept</h2>
 *
 * It cannot be dropped or clicked out of the slot (a player who lifted it out would hold a free
 * Crossbow and a blank offhand); it is removed on death and on quit, so it never persists into a
 * session that does not know about it; and whenever it goes (toggled, eaten, broken) the map is
 * issued again by {@link OffhandMap#ensure}.
 */
public final class OffhandLoan implements Listener {

    /** What the branches put in the hand. The base kit's Bow is what Kitfighter has without a branch. */
    public enum Kind {
        BOW("base", Material.BOW, "Bow"),
        CROSSBOW("crossbow", Material.CROSSBOW, "Crossbow"),
        FISHING_ROD("fishing_rod", Material.FISHING_ROD, "Fishing Rod"),
        GOLDEN_HEAD("golden_head", Material.GOLDEN_APPLE, "Golden Head");

        private final String id;
        private final Material material;
        private final String label;
        Kind(String id, Material material, String label) { this.id = id; this.material = material; this.label = label; }
        public String id() { return id; }
        public Material material() { return material; }
        public String label() { return label; }

        /** The kind for a branch id; anything unrecognised, including none, is the base Bow. */
        public static Kind of(String branch) {
            for (Kind k : values()) if (k.id.equals(branch)) return k;
            return BOW;
        }
    }

    private final MobaPlugin plugin;
    private final NamespacedKey key, head;

    OffhandLoan(MobaPlugin plugin) {
        this.plugin = plugin;
        this.key = new NamespacedKey(plugin, "offhand_loan");
        this.head = new NamespacedKey(plugin, "golden_head");      // the Golden Head eat bonus reads this tag
    }

    // ---- identity ---------------------------------------------------------------------------

    public ItemStack create(Kind kind) {
        var item = new ItemStack(kind.material());
        item.editMeta(meta -> {
            meta.displayName(Component.text(kind.label(), NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(Component.text("Kitfighter's Offhander. Press A1 again to put the map back.", NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false)));
            meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, kind.id());
            if (kind == Kind.GOLDEN_HEAD) meta.getPersistentDataContainer().set(head, PersistentDataType.BYTE, (byte) 1);
        });
        return item;
    }

    public boolean isLoan(ItemStack item) { return kindOf(item) != null; }

    /** The kind an item is, or null when it is not an Offhander item. */
    public Kind kindOf(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        String id = item.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.STRING);
        if (id == null) return null;
        for (Kind k : Kind.values()) if (k.id.equals(id)) return k;
        return null;
    }

    public Kind held(Player p) { return kindOf(p.getInventory().getItemInOffHand()); }

    // ---- equipping --------------------------------------------------------------------------

    /**
     * Put this kind in the offhand in place of the map (or of another Offhander item).
     *
     * @return false when something that is neither the map nor an Offhander item is in the offhand, which is the
     *         player's own business and is not overwritten
     */
    public boolean equip(Player p, Kind kind) {
        ItemStack current = p.getInventory().getItemInOffHand();
        boolean replaceable = current.getType().isAir() || plugin.offhandMap().isMap(current) || isLoan(current);
        if (!replaceable) return false;
        p.getInventory().setItemInOffHand(create(kind));
        return true;
    }

    /** Take the Offhander item away and issue the map again. Returns whether there was one. */
    public boolean unequip(Player p) {
        if (!isLoan(p.getInventory().getItemInOffHand())) return false;
        p.getInventory().setItemInOffHand(null);
        plugin.offhandMap().ensure(p);
        return true;
    }

    // ---- guarding it ------------------------------------------------------------------------

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void drop(PlayerDropItemEvent e) { if (isLoan(e.getItemDrop().getItemStack())) e.setCancelled(true); }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void click(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        boolean involved = isLoan(e.getCurrentItem()) || isLoan(e.getCursor());
        if (e.getClick() == ClickType.SWAP_OFFHAND && held(p) != null) involved = true;
        if (e.getClick() == ClickType.NUMBER_KEY && e.getHotbarButton() >= 0 && isLoan(p.getInventory().getItem(e.getHotbarButton()))) involved = true;
        if (involved) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void drag(InventoryDragEvent e) {
        if (isLoan(e.getOldCursor()) || e.getNewItems().values().stream().anyMatch(this::isLoan)) e.setCancelled(true);
    }

    /** Never dropped and never carried through a death: the map is issued again after respawn. */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void death(PlayerDeathEvent e) {
        e.getDrops().removeIf(this::isLoan);
        if (held(e.getEntity()) != null) e.getEntity().getInventory().setItemInOffHand(null);
    }

    /** Never saved into a session that does not know about it. */
    @EventHandler(priority = EventPriority.LOWEST)
    public void quit(PlayerQuitEvent e) {
        if (held(e.getPlayer()) != null) e.getPlayer().getInventory().setItemInOffHand(null);
    }
}
