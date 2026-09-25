package com.minecraftmoba.plugin;

import java.nio.file.*;
import java.io.IOException;
import java.util.*;
import org.bukkit.*;
import org.bukkit.entity.Player;

/** Inspects a disposable copy; never loads or claims a pristine pool world. */
final class PoolPreview {
    private final MobaPlugin plugin;
    private final Set<UUID> loading = new HashSet<>();
    PoolPreview(MobaPlugin plugin) { this.plugin = plugin; }
    void visit(Player player, String selection) {
        List<MapPool.Entry> entries = plugin.mapPool().entries();
        MapPool.Entry chosen = entries.stream().filter(e -> e.mapId().equals(selection)).findFirst().orElse(null);
        if (chosen == null) try { chosen = entries.get(Integer.parseInt(selection)-1); } catch (RuntimeException ignored) {}
        if (chosen == null) { player.sendMessage("Map number/id not found. Pool contains " + entries.size() + " entries."); return; }
        if (!loading.add(player.getUniqueId())) { player.sendMessage("Your map preview is still loading."); return; }
        MapPool.Entry entry = chosen;
        Path container = Bukkit.getWorldContainer().toPath().toAbsolutePath();
        player.sendMessage("Copying a disposable map preview: " + entry.mapId());
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                Path copy = Files.createTempDirectory(container, "moba_preview_");
                try (var files = Files.walk(entry.world())) {
                    for (Path source : files.toList()) {
                        Path relative = entry.world().relativize(source);
                        if (Set.of("uid.dat", "session.lock").contains(source.getFileName().toString())) continue;
                        Path dest = copy.resolve(relative);
                        if (Files.isDirectory(source)) Files.createDirectories(dest); else Files.copy(source, dest);
                    }
                }
                Bukkit.getScheduler().runTask(plugin, () -> {
                    try {
                        World world = new WorldCreator(copy.getFileName().toString()).createWorld();
                        if (world == null) throw new IllegalStateException("Preview load failed");
                        MapBindings binding = MapBindings.of(entry);
                        Location at = binding == null ? null : binding.fountain(world, Team.NORTH);
                        if (at == null) at = world.getSpawnLocation();
                        player.setGameMode(GameMode.SPECTATOR);
                        player.teleport(at.clone().add(0, 8, 0));
                        player.sendMessage("Preview copy only; no pool claim consumed. /moba debug go colosseum to return.");
                    } finally { loading.remove(player.getUniqueId()); }
                });
            } catch (IOException failure) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    loading.remove(player.getUniqueId()); player.sendMessage("Preview failed: " + failure.getMessage());
                });
            }
        });
    }
}
