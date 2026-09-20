package com.minecraftmoba.plugin;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.*;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

/** The only item creation path: issue a fresh map into an EMPTY offhand. */
public final class OffhandMap implements Listener {
    private final MobaPlugin plugin;
    private final NamespacedKey key;
    public OffhandMap(MobaPlugin plugin) { this.plugin = plugin; key = new NamespacedKey(plugin, "offhand_map"); }
    public boolean isMap(ItemStack item) {
        // A sentinel skull is also "the offhand item" for every caller that asks.
        if (plugin.sentinel() != null && plugin.sentinel().isSentinel(item)) return true;
        return item != null && item.getType() == Material.FILLED_MAP && item.hasItemMeta()
            && item.getItemMeta().getPersistentDataContainer().has(key, PersistentDataType.BYTE);
    }
    public boolean ensure(Player p) {
        ItemStack held = p.getInventory().getItemInOffHand();
        if (isMap(held)) {
            MapMeta meta = (MapMeta) held.getItemMeta();
            if (meta.hasMapView() && meta.getMapView() != null) stub(meta.getMapView());
            return true;
        }
        if (!held.getType().isAir()) return false;
        // Tome mode: the map item also carries the sentinel identity and the
        // class blurb, so one offhand item is the map surface, the swap
        // sentinel and the class text. The swap input is cancelled elsewhere,
        // so the tome never reaches the main hand and the corner map stays
        // visible even while an ability mode is active.
        if (!plugin.getConfig().getBoolean("features.tome.enabled")
                && plugin.sentinel() != null && plugin.sentinel().enabled()) {
            var d = plugin.data(p);
            String classId = (d == null || d.classId == null) ? "test" : d.classId;
            p.getInventory().setItemInOffHand(plugin.sentinel().create(classId));
            return true;
        }
        var view = Bukkit.createMap(p.getWorld());
        stub(view);
        var item = new ItemStack(Material.FILLED_MAP);
        var meta = (MapMeta)item.getItemMeta();
        meta.setMapView(view);
        meta.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte)1);
        if (plugin.getConfig().getBoolean("features.tome.enabled") && plugin.sentinel() != null) {
            var d = plugin.data(p);
            plugin.sentinel().brand(meta, (d == null || d.classId == null) ? "test" : d.classId);
        }
        item.setItemMeta(meta);
        p.getInventory().setItemInOffHand(item); // fresh issuance only; never replaces a player item
        return true;
    }
    private void stub(MapView view) {
        boolean live = plugin.getConfig().getBoolean("features.minimap.enabled");
        Class<?> wanted = live ? Minimap.class : StubRenderer.class;
        if (view.getRenderers().stream().anyMatch(wanted::isInstance)) return;
        view.getRenderers().forEach(view::removeRenderer);
        // Vanilla terrain must not draw underneath: every pixel is ours.
        view.setTrackingPosition(false);
        view.setUnlimitedTracking(false);
        view.addRenderer(live ? new Minimap(plugin) : new StubRenderer(plugin));
    }
    private static final class StubRenderer extends MapRenderer {
        private final JavaPlugin plugin;
        private boolean drawn;
        StubRenderer(JavaPlugin plugin) { this.plugin = plugin; }
        @Override public void render(MapView view, MapCanvas canvas, Player player) {
            if (drawn) return;
            canvas.drawText(plugin.getConfig().getInt("mapStub.textX"), plugin.getConfig().getInt("mapStub.textY"),
                MinecraftFont.Font, plugin.getConfig().getString("mapStub.text", "MOBA"));
            drawn = true;
        }
    }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void drop(PlayerDropItemEvent e) { if (isMap(e.getItemDrop().getItemStack())) e.setCancelled(true); }
    @EventHandler(priority = EventPriority.HIGHEST)
    public void interact(PlayerInteractEvent e) {
        if (e.getHand() == EquipmentSlot.OFF_HAND && isMap(e.getItem())) e.setCancelled(true);
    }
    @EventHandler(priority = EventPriority.HIGHEST)
    public void entityInteract(PlayerInteractEntityEvent e) {
        if (e.getHand() == EquipmentSlot.OFF_HAND && isMap(e.getPlayer().getInventory().getItemInOffHand()))
            e.setCancelled(true);
    }
    @EventHandler(priority = EventPriority.HIGHEST)
    public void entityInteractAt(PlayerInteractAtEntityEvent e) { entityInteract(e); }
    @EventHandler(priority = EventPriority.HIGHEST)
    public void death(PlayerDeathEvent e) {
        if (!plugin.enrolled(e.getPlayer())) return;
        // Vanilla retention keeps the SAME slots. No itemsToKeep/addItem restoration path.
        e.setKeepInventory(true);
        e.getDrops().clear();
    }
}
