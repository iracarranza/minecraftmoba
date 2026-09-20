package com.minecraftmoba.plugin;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Comparator;

/**
 * The disposable Alpha world instance (ALPHA-D2).
 *
 * A match runs on a copy of the frozen Consolidative template, never on the
 * template itself. Loading a match and resetting one are therefore the same
 * operation: unload the instance, copy the template over it, load it again.
 * That is why reset is not block rollback — nothing is undone, the world is
 * simply replaced.
 *
 * Paper cannot replace a loaded world in place, so {@link #restore()} unloads
 * first and fails loudly rather than leaving a half-replaced world on disk.
 */
public final class WorldInstance {
    private final MobaPlugin plugin;
    private final Path template;
    private final String instanceName;

    public WorldInstance(MobaPlugin plugin) {
        this.plugin = plugin;
        var cfg = plugin.getConfig();
        this.template = Paths.get(cfg.getString("alpha.templatePath",
                "artifacts/worldgen/alpha-0.1/consolidative-alpha"));
        this.instanceName = cfg.getString("alpha.instanceWorldName", "alpha_match");
    }

    public String instanceName() { return instanceName; }
    public Path templatePath() { return template; }
    public boolean templateAvailable() { return Files.isDirectory(template); }
    public World world() { return Bukkit.getWorld(instanceName); }

    private Path instancePath() {
        return Bukkit.getWorldContainer().toPath().resolve(instanceName);
    }

    /** Copy the template into place, replacing any existing instance. */
    public void materialize() throws IOException {
        if (!templateAvailable())
            throw new IOException("Alpha template missing at " + template.toAbsolutePath()
                    + "; set alpha.templatePath");
        if (world() != null)
            throw new IllegalStateException("instance '" + instanceName
                    + "' is loaded; unload before materializing");
        Path dest = instancePath();
        deleteTree(dest);
        copyTree(template, dest);
        // A copied world must not inherit the template's session lock or its
        // player data; the lock makes Bukkit refuse the load.
        Files.deleteIfExists(dest.resolve("session.lock"));
        deleteTree(dest.resolve("playerdata"));
        deleteTree(dest.resolve("stats"));
        deleteTree(dest.resolve("advancements"));
    }

    /** Load the instance, materializing it first when absent. */
    public World load() throws IOException {
        World existing = world();
        if (existing != null) return existing;
        if (!Files.isDirectory(instancePath())) materialize();
        World w = Bukkit.createWorld(new WorldCreator(instanceName));
        if (w == null) throw new IOException("Bukkit refused to load " + instanceName);
        return w;
    }

    /**
     * Unload without saving. The instance is disposable, so saving it would
     * only persist the match that is being discarded.
     */
    public boolean unload() {
        World w = world();
        if (w == null) return true;
        for (var p : w.getPlayers()) {
            var fallback = Bukkit.getWorlds().stream().filter(o -> o != w).findFirst();
            fallback.ifPresent(o -> p.teleport(o.getSpawnLocation()));
        }
        return Bukkit.unloadWorld(w, false);
    }

    /** Unload, replace from the template, and load again. */
    public World restore() throws IOException {
        if (!unload())
            throw new IOException("could not unload '" + instanceName
                    + "'; world not restored");
        materialize();
        return load();
    }

    private static void copyTree(Path from, Path to) throws IOException {
        Files.walkFileTree(from, new SimpleFileVisitor<>() {
            @Override public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes a)
                    throws IOException {
                Files.createDirectories(to.resolve(from.relativize(dir).toString()));
                return FileVisitResult.CONTINUE;
            }
            @Override public FileVisitResult visitFile(Path f, BasicFileAttributes a)
                    throws IOException {
                Files.copy(f, to.resolve(from.relativize(f).toString()),
                        StandardCopyOption.REPLACE_EXISTING);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private static void deleteTree(Path root) throws IOException {
        if (!Files.exists(root)) return;
        try (var walk = Files.walk(root)) {
            for (Path p : walk.sorted(Comparator.reverseOrder()).toList()) Files.delete(p);
        }
    }

    public String report() {
        return "template=" + template + (templateAvailable() ? " (present)" : " (MISSING)")
                + " instance=" + instanceName
                + (world() != null ? " (loaded)" : Files.isDirectory(instancePath())
                    ? " (on disk, unloaded)" : " (absent)");
    }
}
