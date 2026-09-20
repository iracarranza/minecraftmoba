package com.minecraftmoba.plugin;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import java.util.*;

/**
 * The vocabulary of renewable kinds, and nothing about where they belong.
 *
 * maps.md forbids sprinkling regenerative nodes to satisfy a density target and
 * records the depth gradient, regional tables, species assignment and cadence as
 * [OPEN]. This therefore lists what a kind *is* — its type and what counts as
 * harvesting it — and assigns no depth, no value and no region.
 *
 * Editable in config under renewables.kinds; these are defaults, not balance.
 */
public final class RenewableKinds {
    public record Kind(String id, Renewables.Type type, Set<Material> blocks, Set<EntityType> entities) {}

    private static Kind blocks(String id, Material... m) {
        return new Kind(id, Renewables.Type.CROP, Set.of(m), Set.of());
    }
    private static Kind animals(String id, EntityType... e) {
        return new Kind(id, Renewables.Type.ANIMAL, Set.of(), Set.of(e));
    }
    private static Kind swarm(String id, EntityType... e) {
        return new Kind(id, Renewables.Type.SWARM, Set.of(), Set.of(e));
    }

    /** Built-in vocabulary. Config may add to or override any entry. */
    public static final Map<String, Kind> DEFAULTS = new LinkedHashMap<>();
    static {
        // Plant and crop resources.
        for (Kind k : List.of(
                blocks("wheat", Material.WHEAT),
                blocks("potatoes", Material.POTATOES),
                blocks("carrots", Material.CARROTS),
                blocks("beetroot", Material.BEETROOTS),
                blocks("melon", Material.MELON),
                blocks("pumpkin", Material.PUMPKIN),
                blocks("sugar_cane", Material.SUGAR_CANE),
                blocks("bamboo", Material.BAMBOO),
                blocks("kelp", Material.KELP, Material.KELP_PLANT),
                blocks("sweet_berries", Material.SWEET_BERRY_BUSH),
                blocks("cocoa", Material.COCOA),
                blocks("mushrooms", Material.RED_MUSHROOM, Material.BROWN_MUSHROOM),
                // Mineral and surface materials. Finite in vanilla; regenerating
                // here only because a Source says so, which is a design claim
                // this class does not make on its own.
                blocks("copper", Material.COPPER_ORE, Material.DEEPSLATE_COPPER_ORE),
                blocks("iron", Material.IRON_ORE, Material.DEEPSLATE_IRON_ORE),
                blocks("coal", Material.COAL_ORE, Material.DEEPSLATE_COAL_ORE),
                blocks("redstone", Material.REDSTONE_ORE, Material.DEEPSLATE_REDSTONE_ORE),
                blocks("lapis", Material.LAPIS_ORE, Material.DEEPSLATE_LAPIS_ORE),
                blocks("diamond", Material.DIAMOND_ORE, Material.DEEPSLATE_DIAMOND_ORE),
                blocks("emerald", Material.EMERALD_ORE, Material.DEEPSLATE_EMERALD_ORE),
                blocks("gravel", Material.GRAVEL),
                blocks("sand", Material.SAND, Material.RED_SAND),
                blocks("clay", Material.CLAY),
                // Animal populations.
                animals("sheep", EntityType.SHEEP),
                animals("rabbit", EntityType.RABBIT),
                animals("chicken", EntityType.CHICKEN),
                animals("cow", EntityType.COW),
                animals("pig", EntityType.PIG),
                animals("horse", EntityType.HORSE),
                animals("cod", EntityType.COD),
                animals("salmon", EntityType.SALMON),
                animals("squid", EntityType.SQUID),
                animals("bee", EntityType.BEE),
                animals("turtle", EntityType.TURTLE),
                // Hostile swarms. objectives.md sketches near, intermediate and
                // deep tiers; the tiers themselves are [OPEN] and not encoded.
                swarm("spiders", EntityType.SPIDER, EntityType.CAVE_SPIDER),
                swarm("creepers", EntityType.CREEPER),
                swarm("undead", EntityType.ZOMBIE, EntityType.SKELETON, EntityType.HUSK,
                        EntityType.STRAY, EntityType.DROWNED, EntityType.ZOMBIE_VILLAGER),
                swarm("slimes", EntityType.SLIME),
                swarm("witches", EntityType.WITCH),
                swarm("pillagers", EntityType.PILLAGER, EntityType.VINDICATOR),
                swarm("guardians", EntityType.GUARDIAN, EntityType.ELDER_GUARDIAN),
                swarm("ravagers", EntityType.RAVAGER))) {
            DEFAULTS.put(k.id(), k);
        }
    }

    /**
     * Canon forbids Warden-class bosses as ordinary regenerative Swarms, so the
     * vocabulary refuses them rather than leaving it to an author to remember.
     */
    public static final Set<EntityType> FORBIDDEN_SWARM = Set.of(
            EntityType.WARDEN, EntityType.ENDER_DRAGON, EntityType.WITHER);

    public static Kind require(String id) {
        Kind k = DEFAULTS.get(id);
        if (k == null) throw new IllegalArgumentException("Unknown renewable kind: " + id
                + " (known: " + String.join(", ", DEFAULTS.keySet()) + ")");
        for (EntityType e : k.entities())
            if (FORBIDDEN_SWARM.contains(e))
                throw new IllegalArgumentException("Boss-class entity is not an ordinary regenerative Swarm: " + e);
        return k;
    }

    public static Set<String> ids() { return Collections.unmodifiableSet(DEFAULTS.keySet()); }

    private RenewableKinds() {}
}
