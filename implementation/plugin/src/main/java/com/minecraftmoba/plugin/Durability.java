package com.minecraftmoba.plugin;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemDamageEvent;
import java.util.*;

/**
 * Equipment wear, following the family-wide durability doctrine in manuscript
 * 15.1.2 and the sensitivity candidate in 15.1.1.
 *
 * The doctrine's balancing model is
 *
 *     L_e = D_e * U_e / R_e
 *
 * expected useful lifetime = base durability, times the effective-life
 * multiplier from Unbreaking, divided by the consumption rate under that
 * category's workload. Two consequences shape this class:
 *
 *  - Identity belongs to the **material family**, not to one item. Tools,
 *    weapons and armor should communicate the same ordering, so a target is
 *    configured per material *and* per equipment category rather than as one
 *    number per material.
 *  - Unbreaking is part of the model, not an extra. It is carried by the
 *    Efficiency task domain, so Efficiency covers working faster and wearing
 *    slower.
 *
 * The ladder is Wood temporary, Stone disposable, Copper replaceable legitimate
 * equipment, Iron reliable, Diamond capital, Netherite premier.
 *
 * Targets ship **empty**, which is vanilla. 15.1.1 is labelled a NON-CANON
 * BALANCE-TEST CANDIDATE and 796cade says exact values remain OPEN, so its
 * numbers are in config as a commented block to switch on deliberately.
 *
 * Disable with features.durability.enabled.
 */
public final class Durability implements Listener {
    public enum Category { TOOL, WEAPON, ARMOR, OTHER }

    private final MobaPlugin plugin;

    public Durability(MobaPlugin plugin) { this.plugin = plugin; }

    public boolean enabled() { return plugin.getConfig().getBoolean("features.durability.enabled"); }

    /** Material family. Copper is a Primary Material as of 19 September 2026. */
    static String tierOf(Material m) {
        String n = m.name();
        if (n.startsWith("WOODEN_")) return "wood";
        if (n.startsWith("STONE_")) return "stone";
        if (n.startsWith("COPPER_")) return "copper";
        if (n.startsWith("IRON_")) return "iron";
        if (n.startsWith("GOLDEN_")) return "gold";
        if (n.startsWith("DIAMOND_")) return "diamond";
        if (n.startsWith("NETHERITE_")) return "netherite";
        if (n.startsWith("LEATHER_")) return "leather";
        if (n.startsWith("CHAINMAIL_")) return "chainmail";
        if (n.startsWith("TURTLE_")) return "turtle";
        return "other";
    }

    /** Workload category: R_e differs between mining, fighting and being hit. */
    static Category categoryOf(Material m) {
        String n = m.name();
        if (n.endsWith("_PICKAXE") || n.endsWith("_AXE") || n.endsWith("_SHOVEL") || n.endsWith("_HOE")
                || n.equals("SHEARS") || n.equals("FLINT_AND_STEEL")) return Category.TOOL;
        if (n.endsWith("_SWORD") || n.equals("BOW") || n.equals("CROSSBOW") || n.equals("TRIDENT")
                || n.equals("MACE")) return Category.WEAPON;
        if (n.endsWith("_HELMET") || n.endsWith("_CHESTPLATE") || n.endsWith("_LEGGINGS")
                || n.endsWith("_BOOTS") || n.equals("SHIELD") || n.equals("ELYTRA")) return Category.ARMOR;
        return Category.OTHER;
    }

    /**
     * Vanilla Unbreaking: a tool takes damage with probability 1/(level+1), so
     * expected uses scale by (level+1). That reproduces the 15.1.1 column
     * exactly — Iron 96 base reaching ~192, ~288 and ~384 at Unbreaking I, II
     * and III — which is why U_e needs no separate coefficient.
     */
    private boolean skipsDamage(int level) {
        return level > 0 && Math.random() >= 1.0 / (level + 1.0);
    }

    public int unbreakingLevel(org.bukkit.entity.Player p) {
        var task = plugin.taskEffects();
        if (task == null || !task.enabled()) return 0;
        if (!plugin.getConfig().getBoolean("progression.task.efficiencyGrantsUnbreaking", true)) return 0;
        return task.tier(plugin.data(p), TaskEffects.Domain.EFFICIENCY);
    }

    /** Target base uses, or 0 when unset, which means leave vanilla alone. */
    private int targetUses(Material m) {
        String path = "durability.targetBaseUses." + tierOf(m) + "."
                + categoryOf(m).name().toLowerCase(Locale.ROOT);
        return plugin.getConfig().getInt(path, 0);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onItemDamage(PlayerItemDamageEvent e) {
        if (!enabled()) return;
        if (skipsDamage(unbreakingLevel(e.getPlayer()))) { e.setCancelled(true); return; }

        Material m = e.getItem().getType();
        int target = targetUses(m);
        if (target <= 0) return;                       // vanilla; touch nothing
        short vanillaMax = m.getMaxDurability();
        if (vanillaMax <= 0) return;

        // Compress the item's life to the target by wearing it proportionally
        // faster. D_e is expressed as uses rather than as an opaque multiplier
        // so config reads the way the doctrine's table does.
        double multiplier = (double) vanillaMax / target;
        double scaled = e.getDamage() * multiplier;
        int whole = (int) Math.floor(scaled);
        if (Math.random() < scaled - whole) whole++;   // keep wear integral
        e.setDamage(Math.max(1, whole));
    }

    public String report() {
        var out = new ArrayList<String>();
        out.add("DURABILITY enabled=" + enabled() + " efficiencyGrantsUnbreaking="
                + plugin.getConfig().getBoolean("progression.task.efficiencyGrantsUnbreaking", true));
        out.add("  model L_e = D_e * U_e / R_e  (manuscript 15.1.2)");
        for (String tier : List.of("wood", "stone", "copper", "iron", "gold", "diamond", "netherite")) {
            var parts = new ArrayList<String>();
            for (Category c : List.of(Category.TOOL, Category.WEAPON, Category.ARMOR)) {
                int uses = plugin.getConfig().getInt(
                        "durability.targetBaseUses." + tier + "." + c.name().toLowerCase(Locale.ROOT), 0);
                parts.add(c.name().toLowerCase(Locale.ROOT) + "=" + (uses > 0 ? uses : "vanilla"));
            }
            out.add("  " + tier + " " + String.join(" ", parts));
        }
        return String.join("\n", out);
    }
}
