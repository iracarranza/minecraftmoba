package com.minecraftmoba.plugin;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import java.util.ArrayList;
import java.util.List;

/**
 * The offhand sentinel item, and the only place its appearance is decided.
 *
 * Identity lives in persistent data, never in the material. A player holding an
 * ordinary skeleton skull must not be mistaken for holding a sentinel, and
 * changing the material later must stay a config edit.
 *
 * Disable with features.sentinelSkull.enabled to fall back to the filled map.
 */
public final class Sentinel {
    private final MobaPlugin plugin;
    private final NamespacedKey markerKey, classKey;

    public Sentinel(MobaPlugin plugin) {
        this.plugin = plugin;
        this.markerKey = new NamespacedKey(plugin, "moba_sentinel");
        this.classKey = new NamespacedKey(plugin, "moba_class");
    }

    public boolean enabled() { return plugin.getConfig().getBoolean("features.sentinelSkull.enabled"); }

    private Material material() {
        String name = plugin.getConfig().getString("features.sentinelSkull.material", "SKELETON_SKULL");
        Material m = Material.matchMaterial(name);
        if (m == null) throw new IllegalArgumentException("features.sentinelSkull.material is not a material: " + name);
        return m;
    }

    public ItemStack create(String classId) {
        String base = "classes." + classId + ".";
        String display = plugin.getConfig().getString(base + "displayName", classId);
        List<String> blurb = plugin.getConfig().getStringList(base + "blurb");
        var item = new ItemStack(material());
        var meta = item.getItemMeta();
        meta.setDisplayName(display);
        var lore = new ArrayList<String>(blurb);
        if (lore.isEmpty()) lore.add("No blurb configured for " + classId);
        meta.setLore(lore);
        var pdc = meta.getPersistentDataContainer();
        pdc.set(markerKey, PersistentDataType.BYTE, (byte) 1);
        pdc.set(classKey, PersistentDataType.STRING, classId);
        meta.setUnbreakable(true);
        item.setItemMeta(meta);
        item.setAmount(1);
        return item;
    }

    /** Persistent data only: material is cosmetic and must not confer identity. */
    public boolean isSentinel(ItemStack item) {
        return item != null && item.hasItemMeta()
                && item.getItemMeta().getPersistentDataContainer().has(markerKey, PersistentDataType.BYTE);
    }

    public String classOf(ItemStack item) {
        if (!isSentinel(item)) return null;
        return item.getItemMeta().getPersistentDataContainer().get(classKey, PersistentDataType.STRING);
    }
}
