package com.minecraftmoba.plugin;

import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.*;
import java.util.*;

/** Equips actual carried gear by exchanging storage and offhand; there is no item loan to duplicate. */
final class OffhanderAbility implements Ability, Listener {
    private final MobaPlugin plugin; private final ConfigurationSection config; private final NamespacedKey head;
    OffhanderAbility(MobaPlugin plugin, ConfigurationSection config) { this.plugin = plugin; this.config = config; head = new NamespacedKey(plugin,"golden_head"); }
    public String id() { return "offhander"; }
    public String displayName() { return "Offhander"; }
    public long cooldownTicks() { return config.getLong("cooldownTicks"); }
    public List<AbilityOutput> outputs() { return AbilityOutput.single("offhander", TargetForm.SELF, InputForm.INSTANT, Recipients.NONE, true); }
    public Map<String,String> branches() { return Map.of("crossbow","Crossbow","fishing_rod","Fishing Rod","golden_head","Golden Head"); }
    public List<String> branchIds() { return List.of("crossbow","fishing_rod","golden_head"); }
    public boolean execute(Player p, AbilityContext ctx) {
        String branch = ctx.branchFor(id());
        Material wanted = "crossbow".equals(branch) ? Material.CROSSBOW : "fishing_rod".equals(branch) ? Material.FISHING_ROD
                : "golden_head".equals(branch) ? Material.GOLDEN_APPLE : Material.BOW;
        if (p.getInventory().getItemInOffHand().getType() == wanted) return false;
        for (int i = 0; i < 36; i++) {
            ItemStack item = p.getInventory().getItem(i);
            if (item == null || item.getType() != wanted) continue;
            ItemStack old = p.getInventory().getItemInOffHand().clone();
            if (wanted == Material.GOLDEN_APPLE) {
                item = item.clone(); var meta = item.getItemMeta(); meta.displayName(Component.text("Golden Head"));
                meta.getPersistentDataContainer().set(head, PersistentDataType.BYTE, (byte)1); item.setItemMeta(meta);
            }
            p.getInventory().setItemInOffHand(item); p.getInventory().setItem(i, old); return true;
        }
        p.sendMessage("Carry a " + wanted.name().toLowerCase().replace('_',' ') + " to equip it."); return false;
    }
    @EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
    public void eat(PlayerItemConsumeEvent e) {
        var item = e.getItem(); Player p = e.getPlayer();
        if (!plugin.passives().has(p,"salvage") || !ChefItems.tagged(item,head)) return;
        // Native consumption spends the real apple. Bonuses follow after its native effects.
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (!p.isOnline() || p.isDead() || !plugin.passives().has(p,"salvage")) return;
            p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, config.getInt("headRegenTicks"), 1));
            if (plugin.inputs().abilityFor("kitfighter","ult") instanceof CoveredWithDiamondsAbility ult && ult.projecting(p))
                p.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION,config.getInt("headRegenTicks"),3));
        });
    }
}
