package com.minecraftmoba.plugin;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.map.*;
import java.awt.Color;
import java.util.*;

/**
 * A persistent corner visualization: terrain, players and infrastructure.
 *
 * A MapRenderer is the only arbitrary per-player pixel surface a vanilla client
 * offers, and a filled map held in the offhand renders continuously in the
 * screen corner. The item is only the surface: nothing here draws vanilla map
 * terrain, and every pixel is computed by this class.
 *
 * What it draws, from the outside in:
 *   terrain   sampled block colours around the player, cached and refreshed slowly
 *   infra     Route endpoints and corridors, renewable sources
 *   players   self, then everyone else in range
 *
 * Constraints that are real and not worked around:
 *   - 128x128 pixels, so scale trades coverage against detail
 *   - Minecraft's map palette, not RGB; colours are matched, not exact
 *   - it renders only while the map is in the offhand, which costs that slot
 *
 * Disable with features.minimap.enabled, which falls back to the stub.
 */
public final class Minimap extends MapRenderer {
    private static final int SIZE = 128;

    private final MobaPlugin plugin;
    /** Cached terrain per player: redrawing 16k world samples every frame is not affordable. */
    private final Map<UUID, byte[]> terrain = new HashMap<>();
    private final Map<UUID, long[]> lastTerrain = new HashMap<>();   // [tick, cx, cz]

    public Minimap(MobaPlugin plugin) { super(true); this.plugin = plugin; }

    private int scale() { return Math.max(1, plugin.getConfig().getInt("features.minimap.blocksPerPixel", 4)); }
    private long terrainTicks() { return plugin.getConfig().getLong("features.minimap.terrainRefreshTicks", 40L); }

    @Override
    public void render(MapView view, MapCanvas canvas, Player player) {
        if (!plugin.getConfig().getBoolean("features.minimap.enabled")) return;
        drawTerrain(canvas, player);
        drawInfrastructure(canvas, player);
        drawRenewables(canvas, player);
        drawPlayers(canvas, player);
        drawSelf(canvas);
    }

    // ---- terrain -------------------------------------------------------------

    private void drawTerrain(MapCanvas canvas, Player player) {
        var id = player.getUniqueId();
        long now = player.getWorld().getFullTime();
        int cx = player.getLocation().getBlockX(), cz = player.getLocation().getBlockZ();
        long[] last = lastTerrain.get(id);
        boolean stale = last == null || now - last[0] >= terrainTicks()
                || Math.abs(cx - last[1]) >= scale() || Math.abs(cz - last[2]) >= scale();
        if (stale) {
            terrain.put(id, sampleTerrain(player, cx, cz));
            lastTerrain.put(id, new long[]{now, cx, cz});
        }
        byte[] cached = terrain.get(id);
        if (cached == null) return;
        for (int x = 0; x < SIZE; x++)
            for (int y = 0; y < SIZE; y++)
                canvas.setPixel(x, y, cached[y * SIZE + x]);
    }

    private byte[] sampleTerrain(Player player, int cx, int cz) {
        var world = player.getWorld();
        byte[] out = new byte[SIZE * SIZE];
        int s = scale();
        for (int px = 0; px < SIZE; px++) {
            for (int py = 0; py < SIZE; py++) {
                int wx = cx + (px - SIZE / 2) * s;
                int wz = cz + (py - SIZE / 2) * s;
                // Unloaded chunks are left blank rather than force-loaded: a
                // minimap must not drag the world in around every player.
                if (!world.isChunkLoaded(wx >> 4, wz >> 4)) { out[py * SIZE + px] = 0; continue; }
                Block top = world.getHighestBlockAt(wx, wz);
                out[py * SIZE + px] = match(colourOf(top), shadeFor(world, wx, wz, top.getY(), s));
            }
        }
        return out;
    }

    /** Relief shading from the height step north of the sample, as vanilla maps do. */
    private double shadeFor(World world, int wx, int wz, int height, int s) {
        if (!world.isChunkLoaded(wx >> 4, (wz - s) >> 4)) return 1.0;
        int north = world.getHighestBlockYAt(wx, wz - s);
        if (height > north) return 1.15;
        if (height < north) return 0.85;
        return 1.0;
    }

    private Color colourOf(Block block) {
        Material m = block.getType();
        String n = m.name();
        if (block.isLiquid() || n.contains("WATER")) return new Color(64, 100, 180);
        if (n.contains("LAVA")) return new Color(200, 90, 40);
        if (n.contains("GRASS") || n.contains("MOSS")) return new Color(96, 140, 72);
        if (n.contains("LEAVES")) return new Color(60, 110, 55);
        if (n.contains("SAND")) return new Color(214, 200, 148);
        if (n.contains("SNOW") || n.contains("ICE")) return new Color(228, 236, 240);
        if (n.contains("STONE") || n.contains("DEEPSLATE") || n.contains("ANDESITE")) return new Color(122, 122, 126);
        if (n.contains("DIRT") || n.contains("PODZOL") || n.contains("MUD")) return new Color(128, 100, 72);
        if (n.contains("LOG") || n.contains("PLANKS") || n.contains("WOOD")) return new Color(126, 98, 62);
        return new Color(110, 110, 110);
    }

    @SuppressWarnings("deprecation")
    private byte match(Color base, double shade) {
        Color c = new Color(clamp(base.getRed() * shade), clamp(base.getGreen() * shade), clamp(base.getBlue() * shade));
        return MapPalette.matchColor(c);
    }

    private static int clamp(double v) { return Math.max(0, Math.min(255, (int) v)); }

    // ---- overlays ------------------------------------------------------------

    private int[] project(Player viewer, org.bukkit.Location target) {
        int s = scale();
        int px = SIZE / 2 + (target.getBlockX() - viewer.getLocation().getBlockX()) / s;
        int py = SIZE / 2 + (target.getBlockZ() - viewer.getLocation().getBlockZ()) / s;
        return (px < 1 || px >= SIZE - 1 || py < 1 || py >= SIZE - 1) ? null : new int[]{px, py};
    }

    @SuppressWarnings("deprecation")
    private void blob(MapCanvas canvas, int px, int py, Color colour, int radius) {
        byte b = MapPalette.matchColor(colour);
        for (int dx = -radius; dx <= radius; dx++)
            for (int dy = -radius; dy <= radius; dy++) {
                int x = px + dx, y = py + dy;
                if (x >= 0 && x < SIZE && y >= 0 && y < SIZE && dx * dx + dy * dy <= radius * radius)
                    canvas.setPixel(x, y, b);
            }
    }

    private void drawInfrastructure(MapCanvas canvas, Player viewer) {
        if (plugin.routes() == null || !plugin.routes().enabled()) return;
        for (var route : plugin.routes().routes()) {
            for (var sample : route.path()) {
                var at = project(viewer, sample);
                if (at != null) blob(canvas, at[0], at[1], new Color(210, 200, 120), 0);
            }
            for (var end : List.of(route.a(), route.b())) {
                var at = project(viewer, end);
                if (at != null) blob(canvas, at[0], at[1], new Color(240, 230, 140), 2);
            }
        }
    }

    private void drawRenewables(MapCanvas canvas, Player viewer) {
        if (plugin.renewables() == null) return;
        for (var source : plugin.renewables().sources()) {
            var world = Bukkit.getWorld(viewer.getWorld().getUID());
            var at = project(viewer, new org.bukkit.Location(world, source.x(), source.y(), source.z()));
            if (at == null) continue;
            // Depleted sources read dim; available ones read bright.
            boolean available = source.available() > 0;
            blob(canvas, at[0], at[1], available ? new Color(120, 220, 160) : new Color(80, 110, 95), 1);
        }
    }

    private void drawPlayers(MapCanvas canvas, Player viewer) {
        double range = plugin.getConfig().getDouble("features.minimap.playerRange", 256.0);
        for (Player other : viewer.getWorld().getPlayers()) {
            if (other.equals(viewer)) continue;
            if (other.getLocation().distance(viewer.getLocation()) > range) continue;
            var at = project(viewer, other.getLocation());
            if (at != null) blob(canvas, at[0], at[1], new Color(220, 90, 90), 1);
        }
    }

    private void drawSelf(MapCanvas canvas) {
        blob(canvas, SIZE / 2, SIZE / 2, new Color(255, 255, 255), 1);
    }
}
