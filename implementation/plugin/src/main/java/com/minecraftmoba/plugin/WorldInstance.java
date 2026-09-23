package com.minecraftmoba.plugin;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

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
    /** The claimed pool map for the current match, or null when using the template. */
    private MapPool.Entry claimed;

    public WorldInstance(MobaPlugin plugin) {
        this.plugin = plugin;
        var cfg = plugin.getConfig();
        this.template = resolveTemplate(plugin,
                cfg.getString("alpha.templatePath",
                        "artifacts/worldgen/alpha-0.1/consolidative-alpha"));
        this.instanceName = cfg.getString("alpha.instanceWorldName", "alpha_match");
    }

    /**
     * Find the frozen template without moving or copying it.
     *
     * A server is normally run from its own directory, not the repository root,
     * so a repo-relative path in config does not resolve. Rather than
     * duplicating the template next to the server -- which would create a
     * second authority for a deliberately frozen artefact -- search the places
     * it legitimately is: an absolute path as given, then relative to the server
     * directory, the plugin's data folder, and each ancestor of the server
     * directory. The first existing match wins; if none exists the configured
     * path is returned unchanged so the error names what was configured.
     */
    static Path resolveTemplate(MobaPlugin plugin, String configured) {
        Path given = Paths.get(configured);
        if (given.isAbsolute()) return given;
        List<Path> roots = new ArrayList<>();
        Path serverDir = Bukkit.getWorldContainer().toPath().toAbsolutePath().normalize();
        roots.add(serverDir);
        roots.add(plugin.getDataFolder().toPath().toAbsolutePath().normalize());
        for (Path up = serverDir.getParent(); up != null; up = up.getParent()) roots.add(up);
        for (Path root : roots) {
            Path candidate = root.resolve(given);
            if (Files.isDirectory(candidate)) return candidate.normalize();
        }
        // A relative path can only be found if the server sits inside the
        // repository. When it does not -- the normal deployment -- there is no
        // way to locate an arbitrary directory, so fail with the search path
        // rather than pretending a default location exists.
        plugin.getLogger().warning("alpha.templatePath '" + configured
                + "' was not found relative to " + roots.size()
                + " candidate root(s); use an absolute path when the server is"
                + " outside the repository.");
        return given.toAbsolutePath().normalize();
    }

    public String instanceName() { return instanceName; }
    public Path templatePath() { return template; }
    public boolean templateAvailable() { return Files.isDirectory(template); }
    public World world() { return Bukkit.getWorld(instanceName); }

    private Path instancePath() {
        return Bukkit.getWorldContainer().toPath().resolve(instanceName);
    }

    /**
     * The world this match should be built from: a claimed pool map, or the
     * configured template.
     *
     * The template is the fallback rather than the default. A pool map is a
     * verified, previously unused map with provenance; the template is one
     * frozen world that every match has shared, which is what the foundry
     * exists to replace.
     */
    private Path source() {
        return claimed != null ? claimed.world() : template;
    }

    /** Claim a pool map for this match, if the pool is enabled and has one. */
    public MapPool.Entry claim(String matchId) { return claim(matchId, null); }

    public MapPool.Entry claim(String matchId, String mapId) {
        MapPool pool = plugin.mapPool();
        claimed = (pool != null && pool.enabled()) ? pool.claim(matchId, mapId) : null;
        return claimed;
    }

    public MapPool.Entry claimed() { return claimed; }

    /**
     * Give back a claim for a match that never started. Not a retirement.
     *
     * Returns true when a map was actually released, so a caller can say so
     * rather than guess.
     */
    public boolean abandon() {
        if (claimed == null || plugin.mapPool() == null) { claimed = null; return false; }
        boolean released = plugin.mapPool().unclaim(claimed);
        claimed = null;
        return released;
    }

    /** Retire the claimed map. A played map never returns to READY. */
    public void release(String result) {
        if (claimed != null && plugin.mapPool() != null) plugin.mapPool().retire(claimed, result);
        claimed = null;
    }

    /** Copy the template into place, replacing any existing instance. */
    public void materialize() throws IOException {
        Path from = source();
        if (!Files.isDirectory(from))
            throw new IOException(claimed != null
                    ? "claimed pool map " + claimed.mapId() + " has no world at " + from
                    : "Alpha template missing at " + template.toAbsolutePath()
                      + ". Set alpha.templatePath to an absolute path to the frozen"
                      + " Consolidative world (artifacts/worldgen/alpha-0.1/"
                      + "consolidative-alpha in the repository).");
        if (world() != null)
            throw new IllegalStateException("instance '" + instanceName
                    + "' is loaded; unload before materializing");
        Path dest = instancePath();
        deleteTree(dest);
        copyTree(from, dest);
        // A copied world must not inherit the template's session lock or its
        // player data; the lock makes Bukkit refuse the load.
        Files.deleteIfExists(dest.resolve("session.lock"));
        deleteTree(dest.resolve("playerdata"));
        deleteTree(dest.resolve("stats"));
        deleteTree(dest.resolve("advancements"));
    }

    /**
     * Whether loading must replace what is on disk.
     *
     * An instance directory sitting on disk while nothing is loaded is not a
     * world to resume: it is the LEFTOVER of a previous match. Treating its
     * presence as "already materialized" is what made `match open` after a
     * server restart hand back the last session's world, holes and all, while
     * every piece of match state around it had been cleared.
     *
     * So presence on disk is deliberately not consulted. The only question is
     * whether a world is currently loaded.
     */
    static boolean mustMaterialize(boolean loaded, boolean presentOnDisk) {
        return !loaded;
    }

    /**
     * The base the instance is copied from, before any configuration is applied.
     * Exposed so the fingerprint check can see what it is verifying against.
     */
    public java.nio.file.Path basePath() { return template; }

    /**
     * Load a FRESH instance.
     *
     * ALPHA-D2 says loading a match and resetting one are the same mechanism --
     * unload, copy the template, load -- and this is the half that was not.
     * Anything already on disk is discarded, because a match never resumes: its
     * plugin and player state are match-scoped and gone, so resuming the terrain
     * alone would produce a world that agrees with nothing.
     */
    public World load() throws IOException {
        World existing = world();
        if (existing != null) return existing;
        if (mustMaterialize(false, Files.isDirectory(instancePath()))) materialize();
        World w = Bukkit.createWorld(new WorldCreator(instanceName));
        if (w == null) throw new IOException("Bukkit refused to load " + instanceName);
        // A match's map is the base plus one authored configuration, chosen
        // fresh each time. Applying here rather than in Match means a reset gets
        // a new draw too, because reset IS load.
        var maps = plugin.mapConfigurations();
        if (maps != null && maps.enabled()) maps.applyTo(w, template);
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
            // The lobby is where a player belongs when there is no match. Before
            // this they were dropped at "the first other world's spawn", which
            // on this server is the superflat's origin -- an arbitrary place
            // nobody chose, and the reason a match reset stranded people.
            var lobby = plugin.lobbyWorld();
            if (lobby != null && lobby.spawn() != null) { lobby.send(p); continue; }
            var fallback = Bukkit.getWorlds().stream().filter(o -> o != w).findFirst();
            fallback.ifPresent(o -> p.teleport(o.getSpawnLocation()));
        }
        return Bukkit.unloadWorld(w, false);
    }

    /**
     * Unload and load again, which now IS the replacement: load() materializes.
     * Keeping a separate materialize() call here would copy the template twice.
     */
    public World restore() throws IOException {
        if (!unload())
            throw new IOException("could not unload '" + instanceName
                    + "'; world not restored");
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
        return (claimed != null
                ? "map=" + claimed.mapId() + " (pool, seed " + claimed.seed() + ") "
                : "map=template (no pool map claimed) ")
                + "template=" + template + (templateAvailable() ? " (present)" : " (MISSING)")
                + " instance=" + instanceName
                + (world() != null ? " (loaded)" : Files.isDirectory(instancePath())
                    ? " (on disk, unloaded)" : " (absent)");
    }
}
