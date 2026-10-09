package com.minecraftmoba.plugin;

import org.bukkit.Material;

import java.util.List;
import java.util.Map;

/**
 * What a menu item looks like as an item stack, decided without a server.
 *
 * The hotbar is a view; this is the lookup from a {@link ChamberMenu.Item} and
 * its refusal to the stack that represents it. Kept pure so the properties that
 * matter -- the way back is always an arrow, an unavailable item is visibly
 * greyed and says why -- are tests rather than things to notice in play.
 *
 * Materials are placeholders chosen to be distinguishable at a glance. They are
 * not a visual decision.
 */
public final class ChamberItems {
    private ChamberItems() {}

    public record Spec(String itemId, Material material, String name, List<String> lore,
                       boolean available) {}

    /** Stand-in for an item whose reason it cannot be used is shown in its lore. */
    public static final Material GREYED = Material.GRAY_DYE;

    private static final Map<String, Material> MATERIAL = Map.ofEntries(
            Map.entry("objective", Material.RED_BANNER),
            Map.entry("renewable", Material.WHEAT),
            Map.entry("route", Material.RAIL),
            Map.entry("regenerate", Material.GRASS_BLOCK),
            Map.entry("undo", Material.ORANGE_DYE),
            Map.entry("redo", Material.LIME_DYE),
            Map.entry("fountain", Material.BEACON),
            Map.entry("outpost", Material.BELL),
            Map.entry("rampart", Material.STONE_BRICKS),
            Map.entry("spike", Material.POINTED_DRIPSTONE),
            Map.entry("route.from", Material.GREEN_CONCRETE),
            Map.entry("route.to", Material.RED_CONCRETE),
            Map.entry("preview", Material.ENDER_EYE),
            Map.entry("confirm", Material.EMERALD),
            Map.entry("cancel", Material.BARRIER),
            // The combat chamber's verbs.
            Map.entry("cast", Material.BOW),
            Map.entry("record", Material.REDSTONE_TORCH),
            Map.entry("replay", Material.CLOCK),
            Map.entry("log", Material.WRITABLE_BOOK),
            Map.entry("clear", Material.SPONGE),
            Map.entry("reset", Material.TNT),
            Map.entry("record.start", Material.RED_CONCRETE),
            Map.entry("record.stop", Material.ORANGE_CONCRETE),
            Map.entry("replay.once", Material.SPECTRAL_ARROW),
            Map.entry("replay.loop", Material.REPEATER),
            Map.entry("replay.stop", Material.BARRIER));

    public static Spec spec(ChamberMenu.Item item, String refusal) {
        if (item.kind() == ChamberMenu.Kind.BACK)
            return new Spec(item.id(), Material.ARROW, item.label(), List.of("Back one page"), true);
        boolean available = refusal == null;
        Material material = available ? MATERIAL.getOrDefault(item.id(), fallback(item)) : GREYED;
        List<String> lore = available
                ? List.of(item.kind() == ChamberMenu.Kind.SUBMENU ? "Opens a page" : "Use")
                : List.of("Unavailable: " + refusal);
        return new Spec(item.id(), material, item.label(), lore, available);
    }

    private static Material fallback(ChamberMenu.Item item) {
        if (item.id().startsWith("fauna.")) return Material.WHEAT_SEEDS;
        if (item.id().startsWith("swarm.")) return Material.ZOMBIE_HEAD;
        return item.kind() == ChamberMenu.Kind.SUBMENU ? Material.CHEST : Material.PAPER;
    }
}
