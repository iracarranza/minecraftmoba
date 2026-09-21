package com.minecraftmoba.plugin;

import net.kyori.adventure.text.Component;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityBreedEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.FurnaceExtractEvent;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

import java.util.*;

/**
 * Work Points: the fine-grained accounting unit for progression-worthy work.
 *
 * The manuscript's doctrine is that progression measures *work*, not wealth:
 * 256 WP is one Useful Action Unit, small legitimate actions produce individual
 * points, and repetition is not inherently invalid -- increasingly expensive
 * bands, not diminishing returns, are what make primitive activity
 * competitively insufficient later.
 *
 * Two consequences are load-bearing here.
 *
 * **Extraction is opportunity plus harvest**, WP = A(O) + qH. A(O) credits
 * exploiting one physical opportunity and is paid once per ore block broken.
 * H is the qualifying harvest actually obtained and q converts it to points.
 *
 * Fortune moves H and never A(O). One ore block does not become 2.2 ore blocks
 * because a pick is enchanted -- the opportunity was singular -- but the extra
 * material really was extracted, and that is additional Extraction work. This
 * is what makes Yield a progression specialization rather than only an item
 * enchantment, alongside Efficiency buying opportunities per unit time and
 * Unbreaking buying sustained exploitation.
 *
 * **Placing a block you just mined is not new work.** Provenance already tracks
 * player-placed blocks, so a block a player put down earns no Extraction credit
 * when broken again, and the loop cannot be farmed.
 *
 * Only domains whose contribution rule is specified enough to implement are
 * wired. The rest are declared and deliberately award nothing; see
 * docs/analysis/alpha-0.1-progression.md for which and why.
 */
public final class WorkPoints implements Listener {

    /** The seven work domains. Progression credit is always attributed to one. */
    public enum Domain {
        CONSTRUCTION, EXTRACTION, EXPLORATION, PRODUCTION,
        DEVELOPMENT, LOGISTICS, COMBAT
    }

    /** Working accounting resolution from the manuscript. */
    public static final int WP_PER_UAU = 256;

    private final MobaPlugin plugin;
    private final Map<UUID, Map<Domain, Long>> earned = new HashMap<>();
    /** Ore blocks broken this tick, awaiting their drops for the harvest half. */
    private final Map<org.bukkit.Location, String> harvestable = new HashMap<>();
    /**
     * Positions each player has already been paid Construction for.
     *
     * Placement is credited for occupying a position that was not already
     * built, so rebuilding the same position is not a second unit of work.
     * This is what stops place/break/place from being an unbounded loop, and
     * it is not a cooldown: placing somewhere new pays immediately and as often
     * as the player likes.
     */
    private final Map<UUID, Set<Long>> built = new HashMap<>();
    /** Geography each player has already resolved this match. */
    private final Map<UUID, Set<String>> discovered = new HashMap<>();

    public WorkPoints(MobaPlugin plugin) {
        this.plugin = plugin;
        long period = plugin.getConfig().getLong("progression.work.exploration.intervalTicks", 20L);
        if (period > 0) Bukkit.getScheduler().runTaskTimer(plugin, this::surveyExploration, period, period);
    }

    public boolean enabled() { return plugin.getConfig().getBoolean("features.workPoints.enabled"); }

    // ---- the band curve --------------------------------------------------

    /**
     * WP required to advance out of a level.
     *
     * The curve is a per-level sawtooth rather than five flat bands:
     *
     *     cost(b, i) = 300 * bandMultiplier[b] * 1.15^i
     *
     * with i counting advancements within an economic band from 0. Requirements
     * compound by about 15% inside a phase and then *drop* on entering the next
     * one, because each phase restarts from its own multiplier. That drop is
     * the design, not a rounding artefact: reaching a new economic phase should
     * feel like a widened economy, while the phase itself gets steadily more
     * expensive than the one before it ever became.
     *
     * The table is held per level in config so the curve is inspectable and a
     * recalibration is a config edit. The 300 baseline and the 1.15 factor are
     * WORKING ALPHA CALIBRATION and not canon.
     */
    public int costOf(int level) {
        var cfg = plugin.getConfig();
        if (cfg.isSet("progression.levelCosts." + level))
            return cfg.getInt("progression.levelCosts." + level);
        return cfg.getInt("progression.xpPerLevel", 100);
    }

    // ---- awarding --------------------------------------------------------

    /**
     * Credit work and apply any level-ups it completes.
     *
     * Levelling consumes the band cost of the level being left, so a player
     * crossing several bands in one award pays each band's own price.
     */
    /**
     * Whether a player's actions can currently constitute work.
     *
     * Creative and Spectator hand a player material without labour, so neither
     * produces progression. Adventure is excluded too: it cannot break the
     * blocks these sources credit.
     */
    public static boolean counts(Player p) {
        return p.getGameMode() == org.bukkit.GameMode.SURVIVAL;
    }

    public void award(Player p, Domain domain, int wp, String source) {
        if (!enabled() || wp <= 0 || !plugin.enrolled(p) || !counts(p)) return;
        var d = plugin.data(p);
        if (d == null) return;
        earned.computeIfAbsent(p.getUniqueId(), k -> new EnumMap<>(Domain.class))
              .merge(domain, (long) wp, Long::sum);

        int max = plugin.settings().maxLevel();
        if (d.level >= max) {
            // At the cap there is nothing to progress toward, so WP is still
            // attributed for diagnostics but the counter does not grow without
            // bound behind a denominator that no longer means anything.
            d.xp = 0;
            feedback(p, domain, wp, source + " (max level)", d);
            return;
        }
        long total = (long) d.xp + wp;
        int before = d.level;
        while (d.level < max && total >= costOf(d.level)) {
            total -= costOf(d.level);
            d.level++;
        }
        d.xp = d.level >= max ? 0 : (int) Math.min(total, Integer.MAX_VALUE);
        plugin.applyProgression(p, d, before);
        feedback(p, domain, wp, source, d);
    }

    /**
     * Alpha/debug feedback. Names the domain and the source so a tester can see
     * which rule fired, not merely that a number moved. Not permanent UX.
     */
    private void feedback(Player p, Domain domain, int wp, String source, PlayerData d) {
        if (!plugin.getConfig().getBoolean("features.workPoints.announce", true)) return;
        int cost = costOf(d.level);
        p.sendActionBar(Component.text(ChatColor.AQUA + "+" + wp + " WP "
                + ChatColor.GRAY + domain.name().toLowerCase() + " · " + source
                + ChatColor.DARK_GRAY + "   " + ChatColor.WHITE + d.xp + ChatColor.DARK_GRAY + "/"
                + cost + ChatColor.GRAY + " to Lv" + (d.level + 1)));
    }

    public Map<Domain, Long> earnedBy(Player p) {
        return earned.getOrDefault(p.getUniqueId(), Map.of());
    }

    public void reset() {
        earned.clear(); harvestable.clear(); built.clear(); discovered.clear();
    }

    // ---- live sources ----------------------------------------------------

    /**
     * Construction: building at a position the player has not built at before.
     *
     * Ordinary placement is worth 1 WP and a Construction Block 20. The 1 is the
     * atomic Work Point -- the smallest legible unit of progression -- and
     * placing a block is exactly the kind of mundane legitimate activity that
     * unit exists for.
     *
     * It is deliberately not 0. Mundane work should become *insufficient*, not
     * worthless, and the level-cost curve is what makes it so: the same
     * placement is 1/300th of a Bootstrap level and 1/1705th of an Endgame one,
     * so primitive activity prices itself out of competitiveness without ever
     * being declared not to be work.
     *
     * What prevents farming is the position rule rather than the magnitude. A
     * player is paid for a block position they have not built at before, so
     * place/break/place pays once and building somewhere new always pays in
     * full. Construction Block membership is classes.md's and is untouched;
     * what counts as Construction *work* is the separate question answered
     * here.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void place(BlockPlaceEvent e) {
        if (!enabled()) return;
        Material m = e.getBlockPlaced().getType();
        boolean construction = MaterialCategories.isConstructionBlock(m);
        int wp = construction
                ? plugin.getConfig().getInt("progression.work.constructionBlockPlacement", 2)
                : plugin.getConfig().getInt("progression.work.ordinaryPlacement", 0);
        if (wp <= 0) return;
        if (!firstTime(built, e.getPlayer().getUniqueId(), key(e.getBlockPlaced().getLocation()))) return;
        award(e.getPlayer(), Domain.CONSTRUCTION, wp, m.name().toLowerCase());
    }

    /**
     * Whether this player has built at this position before.
     *
     * Not a cooldown and not a diminishing return: a position pays in full the
     * first time and never again, so building somewhere new is always worth the
     * same and rebuilding the same spot is worth nothing.
     */
    static boolean firstTime(Map<UUID, Set<Long>> memory, UUID who, long position) {
        return memory.computeIfAbsent(who, k -> new HashSet<>()).add(position);
    }

    static long key(Location l) {
        return ((long) l.getBlockX() & 0x3FFFFFFL) << 38
                | ((long) l.getBlockZ() & 0x3FFFFFFL) << 12
                | ((long) l.getBlockY() & 0xFFFL);
    }

    /**
     * Extraction, opportunity half: A(O), once per ore block exploited.
     *
     * Read at HIGHEST because Provenance clears its mark at MONITOR (SPEC
     * section 7.1). A consumer listening at MONITOR asks after the answer has
     * been erased and sees every block as natural, which would have paid for
     * ore a player placed themselves.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void breakBlock(BlockBreakEvent e) {
        if (!enabled()) return;
        var provenance = plugin.provenance();
        if (provenance != null && provenance.isPlayerPlaced(e.getBlock())) return;
        String kind = oreKind(e.getBlock().getType());
        if (kind == null) return;
        int opportunity = plugin.getConfig().getInt("progression.work.extraction.opportunity." + kind,
                plugin.getConfig().getInt("progression.work.extraction.opportunity.default", 0));
        // Remember the block so the harvest half can be attributed to it once
        // the drops are known. Cleared by the drop handler, or by the next
        // break of the same position.
        harvestable.put(e.getBlock().getLocation(), kind);
        award(e.getPlayer(), Domain.EXTRACTION, opportunity, kind + " opportunity");
    }

    /**
     * Extraction, harvest half: qH, from the material actually obtained.
     *
     * Using the drops rather than a predicted yield is what makes Fortune count
     * without being modelled: whatever the break really produced is what the
     * player really extracted. Silk touch yields the ore block itself, which is
     * one qualifying item, because the opportunity was exploited but the
     * material was deferred rather than multiplied.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void drops(BlockDropItemEvent e) {
        if (!enabled()) return;
        String kind = harvestable.remove(e.getBlock().getLocation());
        if (kind == null) return;
        boolean silk = e.getItems().stream().anyMatch(
                item -> oreKind(item.getItemStack().getType()) != null);
        if (silk) {
            // UNRESOLVED IN AUTHORITY. Nothing in canon says how Silk Touch maps
            // to H: the opportunity was exploited but the material was deferred
            // rather than obtained, and whether deferral is harvest is a design
            // question. The default credits no harvest, and the choice is
            // configurable and labelled rather than decided here.
            int h = plugin.getConfig().getInt("progression.work.extraction.silkTouchHarvest", 0);
            if (h > 0) award(e.getPlayer(), Domain.EXTRACTION, h, kind + " silk-touch (UNRESOLVED)");
            return;
        }
        int harvested = e.getItems().stream()
                .mapToInt(item -> item.getItemStack().getAmount()).sum();
        if (harvested <= 0) return;
        int q = plugin.getConfig().getInt("progression.work.extraction.harvestCoefficient", 1);
        award(e.getPlayer(), Domain.EXTRACTION, q * harvested,
                kind + " harvest x" + harvested);
    }

    // ---- production ------------------------------------------------------

    /**
     * Production: a completed transformation into a strategically useful output.
     *
     * The classification and the reason compression cannot loop both live in
     * ProductionRecipes. What this adds is the amount: a shift-click craft
     * fires one event but may complete many transformations, so the real count
     * is worked out from the grid rather than assumed to be one.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void craft(CraftItemEvent e) {
        if (!enabled() || !(e.getWhoClicked() instanceof Player p)) return;
        ItemStack result = e.getRecipe().getResult();
        var output = ProductionRecipes.craftedOutput(result.getType());
        if (output == ProductionRecipes.Output.UNRESOLVED) return;
        int made = result.getAmount() * craftMultiplier(e);
        if (made <= 0) return;
        awardProduction(p, output, made, result.getType());
    }

    /**
     * Production, furnace half. Smelting is irreversible and costs fuel, so a
     * furnace result needs none of the crafting-grid exclusions.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void smelt(FurnaceExtractEvent e) {
        if (!enabled()) return;
        var output = ProductionRecipes.smeltedOutput(e.getItemType());
        if (output == ProductionRecipes.Output.UNRESOLVED) return;
        awardProduction(e.getPlayer(), output, e.getItemAmount(), e.getItemType());
    }

    private void awardProduction(Player p, ProductionRecipes.Output output, int made, Material m) {
        int per = plugin.getConfig().getInt(
                "progression.work.production." + output.name().toLowerCase(), 0);
        if (per <= 0) return;
        award(p, Domain.PRODUCTION, per * made,
                m.name().toLowerCase() + (made > 1 ? " x" + made : ""));
    }

    /**
     * How many times a craft actually completed.
     *
     * A normal click completes once. A shift-click completes as many times as
     * the scarcest ingredient allows, and paying for one would systematically
     * understate batch production while paying for a full stack would invent
     * work that never happened.
     */
    static int craftMultiplier(CraftItemEvent e) {
        if (!e.isShiftClick()) return 1;
        int limit = Integer.MAX_VALUE;
        for (ItemStack ingredient : e.getInventory().getMatrix())
            if (ingredient != null && ingredient.getType() != Material.AIR)
                limit = Math.min(limit, ingredient.getAmount());
        return limit == Integer.MAX_VALUE ? 1 : limit;
    }

    // ---- development -----------------------------------------------------

    /**
     * Development: successful animal breeding.
     *
     * The event fires only on an actual successful pairing, which already
     * carries the established meaning -- founder stock becoming a reproducing
     * renewable source -- and cannot be produced by interaction spam, because a
     * failed or cooling-down attempt never reaches it.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void breed(EntityBreedEvent e) {
        if (!enabled() || !(e.getBreeder() instanceof Player p)) return;
        award(p, Domain.DEVELOPMENT,
                plugin.getConfig().getInt("progression.work.development.breeding", 8),
                e.getEntityType().name().toLowerCase() + " bred");
    }

    /**
     * Development: harvesting a crop that actually reached maturity.
     *
     * Credit deliberately attaches to the harvest of a MATURE crop rather than
     * to planting a seed. Planting is the interaction a player can repeat as
     * fast as they can click -- plant, break, plant -- whereas maturity is
     * bought with real growth time that no amount of clicking shortens. The
     * mature crop, not the seed in the ground, is the improved productive
     * state.
     *
     * Player-placed provenance is deliberately NOT consulted here. For ore it
     * marks a block whose opportunity was already counted; for a crop the
     * player planting it is the entire point.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void harvest(BlockBreakEvent e) {
        if (!enabled() || !isMatureCrop(e.getBlock())) return;
        award(e.getPlayer(), Domain.DEVELOPMENT,
                plugin.getConfig().getInt("progression.work.development.matureCropHarvest", 2),
                e.getBlock().getType().name().toLowerCase() + " harvested");
    }

    /** Crops whose maturity is unambiguous. Stem and stalk plants are excluded. */
    private static final Set<Material> CROPS = Set.of(
            Material.WHEAT, Material.CARROTS, Material.POTATOES, Material.BEETROOTS,
            Material.NETHER_WART, Material.COCOA, Material.TORCHFLOWER_CROP);

    static boolean isMatureCrop(Block b) {
        if (!CROPS.contains(b.getType())) return false;
        return b.getBlockData() instanceof Ageable age && age.getAge() >= age.getMaximumAge();
    }

    // ---- exploration -----------------------------------------------------

    /**
     * Exploration: first resolution of a registered site.
     *
     * Reach is not distance walked. Walking, entering a chunk, re-crossing
     * known ground and running an already-resolved Route are all excluded,
     * because none of them resolves anything that was not already resolved.
     * What is credited is the first time a player brings a site in the Alpha
     * map's own Worksite registry from unknown to known -- a discrete fact
     * about the map that a player either has or does not have.
     *
     * Resolution is remembered for the match, so leaving and returning pays
     * nothing. It is remembered per player rather than per team, because Reach
     * is something a player has.
     */
    private void surveyExploration() {
        if (!enabled()) return;
        Match match = plugin.match();
        var worksites = plugin.worksites();
        if (match == null || !match.running() || worksites == null) return;
        int wp = plugin.getConfig().getInt("progression.work.exploration.siteResolved", 6);
        if (wp <= 0) return;
        double radius = plugin.getConfig().getDouble("progression.work.exploration.radius", 24.0);
        double r2 = radius * radius;
        for (Match.Participant part : match.participants()) {
            if (!part.alive) continue;
            Player p = Bukkit.getPlayer(part.uuid);
            if (p == null || !plugin.enrolled(p) || !counts(p)) continue;
            Set<String> mine = discovered.computeIfAbsent(part.uuid, k -> new HashSet<>());
            for (var site : worksites.all()) {
                if (mine.contains(site.id)) continue;
                Location at = site.location(p.getWorld());
                if (p.getLocation().distanceSquared(at) > r2) continue;
                mine.add(site.id);
                award(p, Domain.EXPLORATION, wp, site.id + " resolved");
            }
        }
    }

    /** The resource an ore block yields, or null if the block is not ore. */
    static String oreKind(Material m) {
        String n = m.name().toLowerCase().replace("deepslate_", "");
        if (!n.endsWith("_ore") && !n.equals("ancient_debris")) return null;
        return n.equals("ancient_debris") ? "ancient_debris" : n.substring(0, n.length() - 4);
    }

    public List<String> report(Player p) {
        var d = plugin.data(p);
        List<String> out = new ArrayList<>();
        if (d == null) { out.add("not enrolled"); return out; }
        out.add("WORK enabled=" + enabled() + " level=" + d.level
                + " wp=" + d.xp + "/" + costOf(d.level) + " to Lv" + (d.level + 1));
        var mine = earnedBy(p);
        long total = mine.values().stream().mapToLong(Long::longValue).sum();
        out.add("  earned this match=" + total + " WP (" + String.format("%.2f", total / (double) WP_PER_UAU) + " UAU)");
        for (Domain dom : Domain.values())
            out.add("  " + dom.name().toLowerCase() + " = " + mine.getOrDefault(dom, 0L));
        return out;
    }
}
