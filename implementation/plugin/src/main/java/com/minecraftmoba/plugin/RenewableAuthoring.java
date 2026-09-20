package com.minecraftmoba.plugin;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import java.util.*;

/**
 * Two ways to author a renewable source, with an important difference.
 *
 * `capture` recognizes an opportunity the world already contains: it counts the
 * qualifying blocks or entities actually present in a region and sizes the
 * source to what is there. That is what maps.md asks for — identify real
 * candidate opportunities and observe the eligible subset.
 *
 * `spawn` creates a minimal source and, for block kinds, lays the minimum
 * viable patch. It is a fixture tool. Using it to populate a map would be
 * exactly the node-sprinkling canon forbids, so it says so when used.
 *
 * Disable with features.renewableAuthoring.enabled.
 */
public final class RenewableAuthoring {
    private final MobaPlugin plugin;

    public RenewableAuthoring(MobaPlugin plugin) { this.plugin = plugin; }

    private boolean enabled() { return plugin.getConfig().getBoolean("features.renewableAuthoring.enabled"); }

    public boolean handle(CommandSender sender, String[] args) {
        if (!enabled()) { sender.sendMessage("features.renewableAuthoring.enabled is false"); return true; }
        if (!(sender instanceof Player p)) { sender.sendMessage("Player only"); return true; }
        if (args.length < 2) { usage(sender); return true; }

        switch (args[1].toLowerCase(Locale.ROOT)) {
            case "kinds" -> sender.sendMessage("Kinds: " + String.join(", ", RenewableKinds.ids()));
            case "spawn" -> spawn(p, args);
            case "capture" -> capture(p, args);
            case "remove" -> {
                if (args.length < 3) { sender.sendMessage("/moba renew remove <id>"); return true; }
                sender.sendMessage(plugin.renewables().remove(args[2]) ? "Removed " + args[2] : "No such source");
            }
            default -> usage(sender);
        }
        return true;
    }

    private void usage(CommandSender s) {
        s.sendMessage("/moba renew kinds");
        s.sendMessage("/moba renew spawn <kind> [radius] [capacity]   (fixture tool)");
        s.sendMessage("/moba renew capture <kind> [radius]            (recognize what is there)");
        s.sendMessage("/moba renew remove <id>");
    }

    private Block target(Player p) {
        Block b = p.getTargetBlockExact(plugin.getConfig().getInt("features.renewableAuthoring.reach", 48));
        return b == null ? p.getLocation().getBlock() : b;
    }

    private void spawn(Player p, String[] args) {
        if (args.length < 3) { p.sendMessage("/moba renew spawn <kind> [radius] [capacity]"); return; }
        var kind = RenewableKinds.require(args[2]);
        int radius = args.length > 3 ? Integer.parseInt(args[3])
                : plugin.getConfig().getInt("features.renewableAuthoring.defaultRadius", 3);
        int capacity = args.length > 4 ? Integer.parseInt(args[4])
                : plugin.getConfig().getInt("features.renewableAuthoring.defaultCapacity", 5);
        Block b = target(p);
        String id = "authored_" + kind.id() + "_" + b.getX() + "_" + b.getY() + "_" + b.getZ();

        int laid = 0;
        if (kind.type() == Renewables.Type.CROP) {
            Material material = kind.blocks().iterator().next();
            for (int dx = -radius; dx <= radius; dx++)
                for (int dz = -radius; dz <= radius; dz++) {
                    Block cell = b.getWorld().getBlockAt(b.getX() + dx, b.getY() + 1, b.getZ() + dz);
                    if (!cell.getType().isAir()) continue;
                    cell.setType(material, false);
                    laid++;
                    if (laid >= capacity) break;
                }
        }
        var s = plugin.renewables().createRuntime(id, kind.id(), b.getWorld(),
                b.getX(), b.getY(), b.getZ(), radius,
                Math.max(1, kind.type() == Renewables.Type.CROP ? Math.max(1, laid) : capacity),
                plugin.getConfig().getLong("features.renewableAuthoring.recoverTicks", 200L));
        // Herds and swarms are authored content too: a registered volume with
        // no animals in it is not an opportunity.
        int manifested = plugin.renewables().manifest(s);
        p.sendMessage("Spawned " + s.id() + " kind=" + kind.id() + " type=" + s.type()
                + " capacity=" + s.capacity()
                + (laid > 0 ? " (laid " + laid + " blocks)" : "")
                + (manifested > 0 ? " (manifested " + manifested + ")" : ""));
        p.sendMessage(ChatColor.GRAY + "Fixture tool. Populating a map this way is the node-sprinkling "
                + "maps.md forbids; use capture on real geography.");
    }

    private void capture(Player p, String[] args) {
        if (args.length < 3) { p.sendMessage("/moba renew capture <kind> [radius]"); return; }
        var kind = RenewableKinds.require(args[2]);
        int radius = args.length > 3 ? Integer.parseInt(args[3])
                : plugin.getConfig().getInt("features.renewableAuthoring.defaultRadius", 3);
        Block b = target(p);
        int found = 0;

        if (kind.type() == Renewables.Type.CROP) {
            for (int dx = -radius; dx <= radius; dx++)
                for (int dy = -radius; dy <= radius; dy++)
                    for (int dz = -radius; dz <= radius; dz++) {
                        Block cell = b.getWorld().getBlockAt(b.getX() + dx, b.getY() + dy, b.getZ() + dz);
                        if (!kind.blocks().contains(cell.getType())) continue;
                        // A player's own farm is not a wild patch; see SPEC 7.1.
                        if (plugin.provenance().isPlayerPlaced(cell)) continue;
                        found++;
                    }
        } else {
            for (var e : b.getWorld().getNearbyEntities(b.getLocation(), radius, radius, radius))
                if (kind.entities().contains(e.getType())) found++;
        }

        if (found == 0) {
            p.sendMessage(ChatColor.YELLOW + "Nothing of kind " + kind.id() + " found within " + radius
                    + ". Not registering an empty source — an absent opportunity is a real answer.");
            return;
        }
        String id = "captured_" + kind.id() + "_" + b.getX() + "_" + b.getY() + "_" + b.getZ();
        var s = plugin.renewables().createRuntime(id, kind.id(), b.getWorld(),
                b.getX(), b.getY(), b.getZ(), radius, found,
                plugin.getConfig().getLong("features.renewableAuthoring.recoverTicks", 200L));
        p.sendMessage("Captured " + s.id() + " kind=" + kind.id() + " type=" + s.type()
                + " capacity=" + found + " (sized to what is actually present)");
    }
}
