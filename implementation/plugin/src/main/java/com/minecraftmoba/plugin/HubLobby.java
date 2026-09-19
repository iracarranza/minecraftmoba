package com.minecraftmoba.plugin;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import java.util.*;

/**
 * A lobby: a floor, and one framed portal per map option with a sign above it.
 *
 * Presentation only. Walking into a portal runs the gallery's existing
 * harvest:visit/<id> function rather than duplicating teleport logic, so
 * navigation authority stays in the terrain-harvest datapack and this cannot
 * drift away from it.
 *
 * Disable with features.hubLobby.enabled.
 */
public final class HubLobby implements Listener {
    public record Portal(String label, String command, Location min, Location max) {}

    private final MobaPlugin plugin;
    private final List<Portal> portals = new ArrayList<>();
    private final Map<UUID, Long> lastUse = new HashMap<>();

    public HubLobby(MobaPlugin plugin) { this.plugin = plugin; }

    public boolean enabled() { return plugin.getConfig().getBoolean("features.hubLobby.enabled"); }

    /** Builds the lobby from config: one entry per destination. */
    public void build(CommandSender sender, World world) {
        if (!enabled()) { sender.sendMessage("features.hubLobby.enabled is false"); return; }
        portals.clear();
        var section = plugin.getConfig().getConfigurationSection("features.hubLobby.destinations");
        if (section == null || section.getKeys(false).isEmpty()) {
            sender.sendMessage("No destinations configured. This ships empty: destinations are map"
                    + " options, which is a selection decision, not a plugin default.");
            return;
        }
        int originX = plugin.getConfig().getInt("features.hubLobby.originX", 0);
        int originY = plugin.getConfig().getInt("features.hubLobby.originY", 65);
        int originZ = plugin.getConfig().getInt("features.hubLobby.originZ", 0);
        int spacing = plugin.getConfig().getInt("features.hubLobby.spacing", 8);
        Material floor = Material.matchMaterial(
                plugin.getConfig().getString("features.hubLobby.floorMaterial", "POLISHED_DEEPSLATE"));
        Material frame = Material.matchMaterial(
                plugin.getConfig().getString("features.hubLobby.frameMaterial", "POLISHED_BLACKSTONE_BRICKS"));

        int index = 0;
        for (String key : section.getKeys(false)) {
            String label = section.getString(key + ".label", key);
            String command = section.getString(key + ".command");
            if (command == null) { sender.sendMessage("Destination " + key + " has no command; skipped"); continue; }
            int x = originX + index * spacing;

            // Floor pad.
            for (int dx = -2; dx <= 2; dx++)
                for (int dz = -2; dz <= 2; dz++)
                    world.getBlockAt(x + dx, originY - 1, originZ + dz).setType(floor, false);

            // Portal frame: a 3 wide, 4 tall arch with an open interior.
            for (int dy = 0; dy <= 4; dy++)
                for (int dx = -1; dx <= 1; dx++) {
                    boolean edge = (dy == 0 || dy == 4 || dx == -1 || dx == 1);
                    Block b = world.getBlockAt(x + dx, originY + dy, originZ);
                    b.setType(edge ? frame : Material.AIR, false);
                }

            Block signBlock = world.getBlockAt(x, originY + 5, originZ);
            signBlock.setType(Material.OAK_SIGN, false);
            if (signBlock.getState() instanceof Sign sign) {
                sign.getSide(Side.FRONT).setLine(0, ChatColor.AQUA + "[ MAP ]");
                sign.getSide(Side.FRONT).setLine(1, label.length() > 15 ? label.substring(0, 15) : label);
                sign.getSide(Side.FRONT).setLine(2, ChatColor.GRAY + "walk through");
                sign.update();
            }

            portals.add(new Portal(label, command,
                    new Location(world, x - 0.5, originY, originZ - 0.5),
                    new Location(world, x + 0.5, originY + 3, originZ + 0.5)));
            index++;
        }
        sender.sendMessage("Built " + portals.size() + " portal(s) at " + originX + "," + originY + "," + originZ);
    }

    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent e) {
        if (!enabled() || portals.isEmpty() || e.getTo() == null) return;
        Player p = e.getPlayer();
        long now = System.currentTimeMillis();
        long cooldown = plugin.getConfig().getLong("features.hubLobby.cooldownMillis", 3000L);
        if (now - lastUse.getOrDefault(p.getUniqueId(), 0L) < cooldown) return;
        for (Portal portal : portals) {
            if (!inside(e.getTo(), portal)) continue;
            lastUse.put(p.getUniqueId(), now);
            p.sendActionBar(ChatColor.AQUA + portal.label());
            // Reuse the gallery's own navigation rather than teleporting here.
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                    "execute as " + p.getName() + " run " + portal.command());
            return;
        }
    }

    private boolean inside(Location l, Portal portal) {
        return l.getWorld().equals(portal.min().getWorld())
                && l.getX() >= portal.min().getX() && l.getX() <= portal.max().getX()
                && l.getY() >= portal.min().getY() && l.getY() <= portal.max().getY()
                && l.getZ() >= portal.min().getZ() && l.getZ() <= portal.max().getZ();
    }

    public List<String> report() {
        var out = new ArrayList<String>();
        for (Portal p : portals) out.add("PORTAL " + p.label() + " -> " + p.command());
        out.add("PORTAL_TOTALS portals=" + portals.size());
        return out;
    }
}
