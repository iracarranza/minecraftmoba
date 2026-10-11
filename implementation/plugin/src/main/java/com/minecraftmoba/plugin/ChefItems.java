package com.minecraftmoba.plugin;

import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import java.util.List;

final class ChefItems {
    static final String HOOK = "trusty_spatula";
    private ChefItems() {}
    static int foodSlot(Player p) {
        for (int i = 0; i < 36; i++) {
            var s = p.getInventory().getItem(i);
            if (s != null && !s.isEmpty() && s.getType().isEdible()) return i;
        }
        return -1;
    }
    static int shovelTier(Material m) {
        return switch (m) {
            case WOODEN_SHOVEL -> 1; case GOLDEN_SHOVEL -> 2; case STONE_SHOVEL -> 3;
            case COPPER_SHOVEL -> 4; case IRON_SHOVEL -> 5; case DIAMOND_SHOVEL -> 6;
            case NETHERITE_SHOVEL -> 7; default -> 0;
        };
    }
    static boolean spatula(Player p, MobaPlugin plugin) {
        if (!plugin.passives().has(p, HOOK)) return false;
        int held = shovelTier(p.getInventory().getItemInMainHand().getType());
        if (held == 0) return false;
        for (var s : p.getInventory().getStorageContents())
            if (s != null && shovelTier(s.getType()) > held) return false;
        return true;
    }
    static boolean tagged(ItemStack s, NamespacedKey key) {
        return s != null && s.hasItemMeta()
                && s.getItemMeta().getPersistentDataContainer().has(key, PersistentDataType.BYTE);
    }
    static ItemStack ration(ItemStack input, NamespacedKey key) {
        var copy = input.asOne(); var meta = copy.getItemMeta();
        meta.displayName(Component.text("Recklessly Rationed " + input.getType().name().toLowerCase().replace('_', ' ')));
        meta.lore(List.of(Component.text("Cannot itself be Recklessly Rationed.")));
        meta.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);
        copy.setItemMeta(meta); return copy;
    }
    static void heal(LivingEntity target, double hp) {
        var max = target.getAttribute(Attribute.MAX_HEALTH);
        if (!target.isDead() && max != null) target.setHealth(Math.min(max.getValue(), target.getHealth() + hp));
    }
}
