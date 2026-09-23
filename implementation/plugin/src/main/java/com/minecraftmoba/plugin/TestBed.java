package com.minecraftmoba.plugin;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.block.data.type.Bed;
import org.bukkit.block.sign.Side;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import java.util.*;

/**
 * Builds labelled test areas for designation playability and legibility.
 *
 * Each infrastructure type gets its own strip, and each strip runs a **control
 * followed by variations** in one axis, so a designation that works at the
 * control and fails at 4x tells you which variable broke it. Without a control
 * a failure is just a failure.
 *
 * Varying one thing at a time is the point. The Route strip varies distance
 * only; the Construct strip varies footprint only. Mixing them would make a
 * result uninterpretable, which is the same discipline the terrain probes used.
 *
 * Provenance matters here and is a deliberate choice, not a default.
 * Server-placed blocks do not fire BlockPlaceEvent, so they read as
 * world-generated. A Construct is supposed to be player-built, so the builder
 * can mark its blocks as player-placed; a wild crop patch must not be. The
 * flag is explicit because getting it backwards would silently invalidate
 * every Construct test.
 *
 * Disable with features.testbed.enabled.
 */
public final class TestBed {
    private record Station(String label, int offset, String detail) {}

    private final MobaPlugin plugin;

    public TestBed(MobaPlugin plugin) { this.plugin = plugin; }

    private boolean enabled() { return plugin.getConfig().getBoolean("features.testbed.enabled"); }

    /** Distances chosen around the Route minimum so the control is a real boundary. */
    private static final int[] DISTANCES = {16, 32, 64, 128};
    private static final int[] FOOTPRINTS = {5, 9, 17};

    public boolean build(CommandSender sender, Player at, String which) {
        if (!enabled()) { sender.sendMessage("features.testbed.enabled is false"); return true; }
        World w = at.getWorld();
        Location origin = at.getLocation().getBlock().getLocation();
        int y = origin.getBlockY();
        int baseX = origin.getBlockX();
        int baseZ = origin.getBlockZ();
        int built = 0;

        switch (which.toLowerCase(Locale.ROOT)) {
            case "routes" -> built = routes(w, baseX, y, baseZ);
            case "supply" -> built = supply(w, baseX, y, baseZ + 40);
            case "development" -> built = development(w, baseX, y, baseZ + 80);
            case "constructs" -> built = constructs(w, baseX, y, baseZ + 120);
            case "all" -> {
                built = routes(w, baseX, y, baseZ) + supply(w, baseX, y, baseZ + 40)
                      + development(w, baseX, y, baseZ + 80) + constructs(w, baseX, y, baseZ + 120);
            }
            default -> { sender.sendMessage("/moba testbed <routes|supply|development|constructs|all>"); return true; }
        }
        sender.sendMessage("Built " + which + " testbed: " + built + " stations at "
                + baseX + "," + y + "," + baseZ);
        sender.sendMessage(ChatColor.GRAY + "Each strip runs control first, then variations in one axis only.");
        return true;
    }

    // ---- strips ---------------------------------------------------------------

    /** Banner pairs at increasing separation. Control is the configured minimum. */
    private int routes(World w, int x, int y, int z) {
        int n = 0;
        int lane = z;
        for (int i = 0; i < DISTANCES.length; i++) {
            int d = DISTANCES[i];
            pad(w, x, y, lane, 3);
            place(w, x, y + 1, lane, Material.WHITE_BANNER);
            pad(w, x + d, y, lane, 3);
            place(w, x + d, y + 1, lane, Material.RED_BANNER);
            sign(w, x, y + 1, lane - 2, "ROUTE", i == 0 ? "CONTROL" : "x" + (d / DISTANCES[0]),
                 d + " blocks", "white -> red");
            lane += 8;
            n += 2;
        }
        return n;
    }

    /** Copper chest pairs at the same separations, for Supply Line endpoints. */
    private int supply(World w, int x, int y, int z) {
        int n = 0;
        int lane = z;
        Material chest = Material.matchMaterial("COPPER_CHEST");
        if (chest == null) chest = Material.CHEST;          // pre-copper-chest fallback
        for (int i = 0; i < DISTANCES.length; i++) {
            int d = DISTANCES[i];
            pad(w, x, y, lane, 3);
            place(w, x, y + 1, lane, chest);
            pad(w, x + d, y, lane, 3);
            place(w, x + d, y + 1, lane, chest);
            sign(w, x, y + 1, lane - 2, "SUPPLY", i == 0 ? "CONTROL" : "x" + (d / DISTANCES[0]),
                 d + " blocks", chest.name().toLowerCase(Locale.ROOT));
            lane += 8;
            n += 2;
        }
        return n;
    }

    /** Fenced animals and crop patches at increasing footprint. */
    private int development(World w, int x, int y, int z) {
        int n = 0;
        int cursor = x;
        EntityType[] stock = {EntityType.SHEEP, EntityType.COW, EntityType.CHICKEN};
        for (int i = 0; i < FOOTPRINTS.length; i++) {
            int size = FOOTPRINTS[i];
            pen(w, cursor, y, z, size);
            for (int a = 0; a < Math.max(2, size / 3); a++)
                w.spawnEntity(new Location(w, cursor + size / 2.0, y + 1, z + size / 2.0),
                        stock[a % stock.length]);
            // Crops sit beside each pen so one station tests both.
            int cropZ = z + size + 2;
            for (int dx = 0; dx < size; dx++)
                for (int dz = 0; dz < size; dz++) {
                    place(w, cursor + dx, y, cropZ + dz, Material.FARMLAND);
                    place(w, cursor + dx, y + 1, cropZ + dz,
                          (dx + dz) % 2 == 0 ? Material.WHEAT : Material.POTATOES);
                }
            sign(w, cursor, y + 1, z - 2, "DEVELOP", i == 0 ? "CONTROL" : "x" + (size / FOOTPRINTS[0]),
                 size + "x" + size, "pen + crops");
            cursor += size + 6;
            n += 2;
        }
        return n;
    }

    /** Construction-block platforms at increasing footprint, one material each. */
    private int constructs(World w, int x, int y, int z) {
        boolean asPlayerBuilt = plugin.getConfig().getBoolean("features.testbed.constructsArePlayerPlaced", true);
        Material[] palette = {Material.BRICKS, Material.TERRACOTTA, Material.WHITE_CONCRETE};
        int n = 0;
        int cursor = x;
        for (int i = 0; i < FOOTPRINTS.length; i++) {
            int size = FOOTPRINTS[i];
            Material m = palette[i % palette.length];
            for (int dx = 0; dx < size; dx++)
                for (int dz = 0; dz < size; dz++)
                    for (int dy = 0; dy < 3; dy++) {
                        boolean wall = dx == 0 || dz == 0 || dx == size - 1 || dz == size - 1;
                        if (!wall && dy > 0) continue;
                        Block b = w.getBlockAt(cursor + dx, y + dy, z + dz);
                        b.setType(m, false);
                        // A Construct is player-built by definition; server
                        // placement would read as world-generated.
                        if (asPlayerBuilt && plugin.provenance() != null) plugin.provenance().mark(b, true);
                    }
            sign(w, cursor, y + 3, z - 2, "CONSTRUCT", i == 0 ? "CONTROL" : "x" + (size / FOOTPRINTS[0]),
                 size + "x" + size, m.name().toLowerCase(Locale.ROOT));
            cursor += size + 6;
            n++;
        }
        return n;
    }

    // ---- helpers --------------------------------------------------------------

    private void pad(World w, int x, int y, int z, int radius) {
        for (int dx = -radius; dx <= radius; dx++)
            for (int dz = -radius; dz <= radius; dz++) {
                w.getBlockAt(x + dx, y, z + dz).setType(Material.SMOOTH_STONE, false);
                for (int dy = 1; dy <= 3; dy++) w.getBlockAt(x + dx, y + dy, z + dz).setType(Material.AIR, false);
            }
    }

    /** A fenced enclosure with a grass floor, so animals stay in their station. */
    private void pen(World w, int x, int y, int z, int size) {
        for (int dx = 0; dx < size; dx++)
            for (int dz = 0; dz < size; dz++) {
                w.getBlockAt(x + dx, y, z + dz).setType(Material.GRASS_BLOCK, false);
                boolean edge = dx == 0 || dz == 0 || dx == size - 1 || dz == size - 1;
                w.getBlockAt(x + dx, y + 1, z + dz).setType(edge ? Material.OAK_FENCE : Material.AIR, false);
                w.getBlockAt(x + dx, y + 2, z + dz).setType(Material.AIR, false);
            }
    }

    private void place(World w, int x, int y, int z, Material m) {
        w.getBlockAt(x, y, z).setType(m, false);
    }

    private void sign(World w, int x, int y, int z, String a, String b, String c, String d) {
        Block block = w.getBlockAt(x, y, z);
        block.setType(Material.OAK_SIGN, false);
        if (block.getState() instanceof Sign s) {
            s.getSide(Side.FRONT).setLine(0, ChatColor.AQUA + a);
            s.getSide(Side.FRONT).setLine(1, ChatColor.WHITE + b);
            s.getSide(Side.FRONT).setLine(2, ChatColor.GRAY + c);
            s.getSide(Side.FRONT).setLine(3, ChatColor.DARK_GRAY + d);
            s.update();
        }
    }
}
