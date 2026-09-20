package com.minecraftmoba.plugin;

import net.kyori.adventure.text.Component;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

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
 * **Yield does not multiply Extraction.** "WHAT YOU GOT" is the material
 * economy; "WHAT IT TOOK TO GET IT" is Extraction progression. A Fortune pick
 * produces more ore from the same acquisition, so the WP is per block broken
 * and never per item dropped.
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

    public WorkPoints(MobaPlugin plugin) { this.plugin = plugin; }

    public boolean enabled() { return plugin.getConfig().getBoolean("features.workPoints.enabled"); }

    // ---- the band curve --------------------------------------------------

    /**
     * WP required to advance out of a level.
     *
     * ALPHA CALIBRATION, supplied for this pass rather than derived here:
     * Lv1-6 40, Lv7-12 54, Lv13-19 75, Lv20-24 103, Lv25-30 130. Held in config
     * so a recalibration is a config edit and not a code change.
     */
    public int costOf(int level) {
        var cfg = plugin.getConfig();
        for (String band : cfg.getConfigurationSection("progression.bands").getKeys(false)) {
            String[] range = band.split("-");
            int lo = Integer.parseInt(range[0]);
            int hi = Integer.parseInt(range[range.length - 1]);
            if (level >= lo && level <= hi) return cfg.getInt("progression.bands." + band);
        }
        return plugin.getConfig().getInt("progression.xpPerLevel", 100);
    }

    // ---- awarding --------------------------------------------------------

    /**
     * Credit work and apply any level-ups it completes.
     *
     * Levelling consumes the band cost of the level being left, so a player
     * crossing several bands in one award pays each band's own price.
     */
    public void award(Player p, Domain domain, int wp, String source) {
        if (!enabled() || wp <= 0 || !plugin.enrolled(p)) return;
        var d = plugin.data(p);
        if (d == null) return;
        earned.computeIfAbsent(p.getUniqueId(), k -> new EnumMap<>(Domain.class))
              .merge(domain, (long) wp, Long::sum);

        long total = (long) d.xp + wp;
        int before = d.level;
        while (d.level < plugin.settings().maxLevel() && total >= costOf(d.level)) {
            total -= costOf(d.level);
            d.level++;
        }
        d.xp = (int) Math.min(total, Integer.MAX_VALUE);
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

    public void reset() { earned.clear(); }

    // ---- live sources ----------------------------------------------------

    /**
     * Construction: ordinary useful placement is 1 WP, a Construction Block is
     * 2 WP total. Both are the manuscript's own initial sensitivity fixtures.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void place(BlockPlaceEvent e) {
        if (!enabled()) return;
        Material m = e.getBlockPlaced().getType();
        int wp = MaterialCategories.isConstructionBlock(m)
                ? plugin.getConfig().getInt("progression.work.constructionBlockPlacement", 2)
                : plugin.getConfig().getInt("progression.work.ordinaryPlacement", 1);
        award(e.getPlayer(), Domain.CONSTRUCTION, wp, m.name().toLowerCase());
    }

    /**
     * Extraction: per ore block broken, never per item dropped, so Fortune
     * changes the material economy and not the progression.
     *
     * A block the player placed earns nothing: Provenance knows it is not new
     * acquisition, which is what stops a place-and-break loop from farming.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void breakBlock(BlockBreakEvent e) {
        if (!enabled()) return;
        var provenance = plugin.provenance();
        if (provenance != null && provenance.isPlayerPlaced(e.getBlock())) return;
        String kind = oreKind(e.getBlock().getType());
        if (kind == null) return;
        int wp = plugin.getConfig().getInt("progression.work.oreExtraction." + kind,
                 plugin.getConfig().getInt("progression.work.oreExtraction.default", 0));
        award(e.getPlayer(), Domain.EXTRACTION, wp, kind);
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
