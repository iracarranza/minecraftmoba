package com.minecraftmoba.plugin;

import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.EquipmentSlotGroup;
import java.util.*;

/**
 * Task progression as player state: Efficiency, Yield and Damage.
 *
 * classes.md names the vanilla families — Efficiency, Fortune plus Looting, and
 * Sharpness plus Power — and caps generic progression at Tier III so class
 * amplification such as Mole's Tunneling has room and cannot multiply
 * uncontrollably with a high generic tier.
 *
 * The Efficiency domain also carries **Unbreaking**, so it covers both working
 * faster and wearing slower. That extends the family classes.md lists; the
 * durability half lives in Durability.java and is switchable separately via
 * progression.task.efficiencyGrantsUnbreaking.
 *
 * These are applied to the player, not enchanted onto an item, so progression
 * is not lost with a tool and cannot be transferred by dropping one.
 *
 * Every coefficient is config. Canon fixes the families and the ceiling, not
 * the numbers, so nothing here is balance.
 *
 * Disable with features.taskProgression.enabled.
 */
public final class TaskEffects implements Listener {
    public enum Domain { EFFICIENCY, YIELD, DAMAGE }

    private final MobaPlugin plugin;
    private final NamespacedKey speedKey, damageKey;

    public TaskEffects(MobaPlugin plugin) {
        this.plugin = plugin;
        this.speedKey = new NamespacedKey(plugin, "moba_task_efficiency");
        this.damageKey = new NamespacedKey(plugin, "moba_task_damage");
    }

    public boolean enabled() { return plugin.getConfig().getBoolean("features.taskProgression.enabled"); }
    public int tierMax() { return plugin.getConfig().getInt("progression.task.tierMax", 3); }

    public int tier(PlayerData d, Domain domain) {
        return d == null ? 0 : d.task.getOrDefault(domain.name(), 0);
    }

    public boolean has(PlayerData d) { return d != null; }

    /** Grants are capped at the generic ceiling; a config asking for more is clamped. */
    public void grant(Player p, PlayerData d, Domain domain, int tier) {
        // An unenrolled player has no data. Granting silently created a null
        // dereference that surfaced only as "an unexpected error occurred".
        if (d == null) throw new IllegalStateException(
                p.getName() + " is not enrolled; run /moba join first");
        int capped = Math.max(0, Math.min(tier, tierMax()));
        int current = tier(d, domain);
        if (capped <= current) return;
        d.task.put(domain.name(), capped);
        reapply(p, d);
        if (plugin.rewardAdvancements() != null)
            for (int t = 1; t <= capped; t++)
                plugin.rewardAdvancements().grant(p, domain.name().toLowerCase(Locale.ROOT) + "_" + t);
    }

    public void reapply(Player p, PlayerData d) {
        if (!enabled()) { clear(p); return; }
        applyModifier(p, Attribute.BLOCK_BREAK_SPEED, speedKey,
                tier(d, Domain.EFFICIENCY) * plugin.getConfig().getDouble("progression.task.efficiencyPerTier"),
                AttributeModifier.Operation.MULTIPLY_SCALAR_1);
        applyModifier(p, Attribute.ATTACK_DAMAGE, damageKey,
                tier(d, Domain.DAMAGE) * plugin.getConfig().getDouble("progression.task.damagePerTier"),
                AttributeModifier.Operation.ADD_NUMBER);
    }

    private void applyModifier(Player p, Attribute attribute, NamespacedKey key, double amount,
                               AttributeModifier.Operation op) {
        var inst = p.getAttribute(attribute);
        if (inst == null) return;
        inst.getModifiers().stream().filter(m -> key.equals(m.getKey())).toList().forEach(inst::removeModifier);
        if (amount == 0) return;
        inst.addModifier(new AttributeModifier(key, amount, op, EquipmentSlotGroup.ANY));
    }

    public void clear(Player p) {
        for (var pair : List.of(Map.entry(Attribute.BLOCK_BREAK_SPEED, speedKey),
                                Map.entry(Attribute.ATTACK_DAMAGE, damageKey))) {
            var inst = p.getAttribute(pair.getKey());
            if (inst == null) continue;
            inst.getModifiers().stream().filter(m -> pair.getValue().equals(m.getKey())).toList()
                .forEach(inst::removeModifier);
        }
    }

    // Yield has no vanilla attribute, so it is applied at the drop.

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockDrop(BlockDropItemEvent e) {
        if (!enabled()) return;
        var d = plugin.data(e.getPlayer());
        int tier = tier(d, Domain.YIELD);
        if (tier <= 0) return;
        // A block the player placed is not a resource opportunity; see SPEC 7.1.
        if (plugin.provenance().isPlayerPlaced(e.getBlock())) return;
        for (var item : e.getItems()) multiply(item.getItemStack(), tier);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityDeath(EntityDeathEvent e) {
        if (!enabled()) return;
        var killer = e.getEntity().getKiller();
        if (killer == null) return;
        int tier = tier(plugin.data(killer), Domain.YIELD);
        if (tier <= 0) return;
        for (var stack : e.getDrops()) multiply(stack, tier);
    }

    /** Fortune-like: a chance of one extra unit per tier, never a flat multiplier. */
    private void multiply(org.bukkit.inventory.ItemStack stack, int tier) {
        double chance = tier * plugin.getConfig().getDouble("progression.task.yieldExtraChancePerTier");
        int extra = 0;
        for (int i = 0; i < tier; i++) if (Math.random() < chance) extra++;
        if (extra > 0) stack.setAmount(Math.min(stack.getMaxStackSize(), stack.getAmount() + extra));
    }

    public String report(PlayerData d) {
        return "TASK efficiency=" + tier(d, Domain.EFFICIENCY)
             + " yield=" + tier(d, Domain.YIELD)
             + " damage=" + tier(d, Domain.DAMAGE)
             + " tierMax=" + tierMax();
    }
}
