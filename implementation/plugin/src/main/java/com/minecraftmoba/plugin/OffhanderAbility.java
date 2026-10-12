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

/**
 * Offhander: the offhand's map, replaced by the branch's item, and put back by pressing it again.
 *
 * The item is issued (see {@link OffhandLoan}), not exchanged with gear in the inventory. The first version needed
 * the Kitfighter to carry the item already, swapped it with whatever was in the offhand, and left the map in an
 * inventory slot with the class paused. This one needs nothing carried, takes nothing, moves nothing.
 *
 * Pressing A1 with the same branch's item already equipped puts the map back; with another kind equipped it
 * replaces it. Eating a Golden Head, or anything else that empties the hand, brings the map back by itself.
 */
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
        var loan = plugin.offhandLoan();
        var kind = OffhandLoan.Kind.of(ctx.branchFor(id()));
        if (loan.held(p) == kind) { loan.unequip(p); return true; }
        if (!loan.equip(p, kind)) { p.sendMessage("Your offhand is holding something else; empty it first."); return false; }
        return true;
    }
    /** An Offhander item is not an equipped ability: leaving, resetting or a match ending puts the map back. */
    public void forget(Player p) { plugin.offhandLoan().unequip(p); }
    public void close() { for (Player p : plugin.getServer().getOnlinePlayers()) plugin.offhandLoan().unequip(p); }
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
