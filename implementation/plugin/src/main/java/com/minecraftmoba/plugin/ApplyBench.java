package com.minecraftmoba.plugin;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.CommandSender;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * How long would applying the authored additions at match open actually take?
 *
 * The proposed model replaces "copy a baked template" with "copy the base world
 * and apply a verified set of additions". Everything else about that model is
 * sound on paper; this is the one part that could invalidate it, because the
 * writes happen on the main thread while a player is waiting.
 *
 * So it is measured rather than estimated, and measured the way the real thing
 * would run: the authored footprint is about 200,000 blocks spread across 32
 * sites, which means the dominant cost is very likely chunk loading rather than
 * the writes themselves. A dense benchmark in one corner of the world would
 * flatter the design by missing exactly that.
 *
 * Three numbers matter:
 *   - the CURRENT cost, copying a 28MB template, which is what this must beat
 *     or match;
 *   - scattered writes across real site coordinates, which is the honest case;
 *   - dense writes in one volume, as a floor showing what the writes alone cost.
 */
public final class ApplyBench {
    private final MobaPlugin plugin;

    public ApplyBench(MobaPlugin plugin) { this.plugin = plugin; }

    public boolean handle(CommandSender sender, String[] args) {
        String mode = args.length > 1 ? args[1].toLowerCase(java.util.Locale.ROOT) : "help";
        try {
            switch (mode) {
                case "copy" -> copy(sender);
                case "dense" -> write(sender, count(args, 2, 200_000), false);
                case "scattered" -> write(sender, count(args, 2, 200_000), true);
                default -> sender.sendMessage(
                        "/moba bench <copy|dense [n]|scattered [n]>  -- destructive to the instance");
            }
        } catch (Exception ex) {
            sender.sendMessage("bench: " + ex);
        }
        return true;
    }

    private static int count(String[] args, int i, int fallback) {
        return args.length > i ? Integer.parseInt(args[i]) : fallback;
    }

    /** The baseline the new model has to beat: unload, copy the template, load. */
    private void copy(CommandSender sender) throws IOException {
        long t0 = System.nanoTime();
        plugin.worldInstance().restore();
        long ms = (System.nanoTime() - t0) / 1_000_000;
        sender.sendMessage("BENCH copy-template: " + ms + " ms (28MB unload+copy+load)");
        plugin.getLogger().info("BENCH copy-template " + ms + " ms");
    }

    /**
     * Write n blocks and report throughput, chunk loads and the worst pause.
     *
     * Scattered mode uses the real authored coordinates -- worksites, renewable
     * sources and both homelands -- because the cost of this design is where
     * the writes land, not how many there are.
     */
    private void write(CommandSender sender, int n, boolean scattered) {
        World w = plugin.worldInstance().world();
        if (w == null) { sender.sendMessage("No instance loaded; /moba match open first."); return; }

        List<int[]> anchors = scattered ? anchors() : List.of(new int[]{0, 70, 0});
        if (anchors.isEmpty()) { sender.sendMessage("No authored coordinates in config."); return; }

        Set<Long> chunks = new HashSet<>();
        int perAnchor = Math.max(1, n / anchors.size());
        long worstChunkNanos = 0;
        long t0 = System.nanoTime();
        int written = 0;

        for (int[] a : anchors) {
            long c0 = System.nanoTime();
            // A square pad around the anchor, which is the shape authored
            // additions actually have: clear_and_foundation discs dominate the
            // real footprint far more than the massing does.
            int side = (int) Math.ceil(Math.sqrt(perAnchor));
            for (int dx = 0; dx < side && written < n; dx++)
                for (int dz = 0; dz < side && written < n; dz++) {
                    int x = a[0] + dx - side / 2, z = a[2] + dz - side / 2;
                    chunks.add(((long) (x >> 4) << 32) | ((z >> 4) & 0xFFFFFFFFL));
                    // physics off: the authored path never wants block updates
                    w.getBlockAt(x, a[1], z).setType(Material.POLISHED_DEEPSLATE, false);
                    written++;
                }
            worstChunkNanos = Math.max(worstChunkNanos, System.nanoTime() - c0);
        }

        long ms = (System.nanoTime() - t0) / 1_000_000;
        long perSite = worstChunkNanos / 1_000_000;
        String line = String.format(
                "BENCH %s: %d blocks in %d ms (%,d blocks/s) across %d chunk(s), worst site %d ms",
                scattered ? "scattered" : "dense", written, ms,
                ms == 0 ? written : written * 1000L / ms, chunks.size(), perSite);
        sender.sendMessage(line);
        plugin.getLogger().info(line);
    }

    /** Real authored coordinates: the sites additions would actually be applied at. */
    private List<int[]> anchors() {
        var out = new ArrayList<int[]>();
        var cfg = plugin.getConfig();
        var sites = cfg.getMapList("alpha.worksites.sites");
        for (var site : sites) {
            Object xyz = site.get("xyz");
            if (xyz instanceof List<?> l && l.size() == 3)
                out.add(new int[]{num(l.get(0)), num(l.get(1)), num(l.get(2))});
        }
        var section = cfg.getConfigurationSection("renewables.sources");
        if (section != null)
            for (String id : section.getKeys(false)) {
                String base = "renewables.sources." + id + ".";
                out.add(new int[]{cfg.getInt(base + "x"), cfg.getInt(base + "y"), cfg.getInt(base + "z")});
            }
        for (String team : List.of("north", "south")) {
            var home = cfg.getIntegerList("alpha.homelands." + team);
            if (home.size() == 3) out.add(new int[]{home.get(0), home.get(1), home.get(2)});
        }
        return out;
    }

    private static int num(Object o) { return o instanceof Number n ? n.intValue() : 0; }
}
