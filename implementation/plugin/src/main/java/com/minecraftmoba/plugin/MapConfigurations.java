package com.minecraftmoba.plugin;

import org.bukkit.World;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * The catalogue of map configurations, and which one a match gets.
 *
 * Selection is a menu, not a search. Every configuration here was authored and
 * measured offline, so a match plays a map somebody has looked at -- which is
 * also what an eventual vote or ban needs, since players cannot choose between
 * maps that are assembled on the spot.
 *
 * A configuration is chosen at random per match today. That gives a different
 * map each time from a set that is entirely known, rather than a different map
 * each time from a set that is not.
 */
public final class MapConfigurations {
    private final MobaPlugin plugin;
    private final List<MapDiff> catalogue = new ArrayList<>();
    private final Random random = new Random();
    private String baseFingerprint;
    private MapDiff applied;

    public MapConfigurations(MobaPlugin plugin) { this.plugin = plugin; }

    public boolean enabled() { return plugin.getConfig().getBoolean("alpha.configurations.enabled"); }

    public List<MapDiff> catalogue() { return List.copyOf(catalogue); }
    public MapDiff applied() { return applied; }

    /** Load every configured diff. A broken one is fatal, not skipped. */
    public int reload() {
        catalogue.clear();
        if (!enabled()) return 0;
        Path dir = resolve(plugin.getConfig().getString("alpha.configurations.directory", "maps"));
        for (String name : plugin.getConfig().getStringList("alpha.configurations.include")) {
            Path file = dir.resolve(name + ".json.gz");
            try {
                catalogue.add(MapDiff.load(file));
            } catch (Exception ex) {
                throw new IllegalStateException("could not load map configuration " + file
                        + ": " + ex.getMessage(), ex);
            }
        }
        return catalogue.size();
    }

    /** Search like WorldInstance does, so a server outside the repo still finds them. */
    private Path resolve(String configured) {
        Path given = Path.of(configured);
        if (given.isAbsolute()) return given;
        List<Path> roots = new ArrayList<>();
        roots.add(org.bukkit.Bukkit.getWorldContainer().toPath().toAbsolutePath().normalize());
        roots.add(plugin.getDataFolder().toPath().toAbsolutePath().normalize());
        for (Path up = roots.get(0).getParent(); up != null; up = up.getParent()) roots.add(up);
        for (Path root : roots) {
            Path candidate = root.resolve(given);
            if (Files.isDirectory(candidate)) return candidate.normalize();
        }
        return given.toAbsolutePath().normalize();
    }

    /** Pick one. Named if the config names one, otherwise a fresh draw per match. */
    public MapDiff choose() {
        if (catalogue.isEmpty()) return null;
        String forced = plugin.getConfig().getString("alpha.configurations.force", "");
        if (forced != null && !forced.isBlank())
            return catalogue.stream().filter(d -> d.name.equals(forced)).findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(
                            "alpha.configurations.force names an unknown map: " + forced));
        return catalogue.get(random.nextInt(catalogue.size()));
    }

    /**
     * Apply a configuration to a freshly materialized base world.
     *
     * The fingerprint check is the whole of verification, and it refuses rather
     * than warning: a diff applied to the wrong terrain produces a world that
     * looks plausible and is wrong everywhere, which is far more expensive to
     * discover later than a failed match open is now.
     */
    public MapDiff applyTo(World world, Path baseDirectory) {
        applied = null;
        MapDiff diff = choose();
        if (diff == null) return null;
        String actual = fingerprint(baseDirectory);
        if (!diff.baseFingerprint.equals(actual))
            throw new IllegalStateException("map '" + diff.name + "' was built against base "
                    + diff.baseFingerprint.substring(0, 12) + " but this server has "
                    + actual.substring(0, 12) + "; applying it would produce a wrong world");
        long t0 = System.nanoTime();
        int blocks = diff.applyTo(world);
        long ms = (System.nanoTime() - t0) / 1_000_000;
        plugin.getLogger().info("applied map '" + diff.name + "': " + blocks + " blocks in " + ms + " ms");
        applied = diff;
        return diff;
    }

    /** Must match terrain_harvest.map_diff.fingerprint exactly. */
    public String fingerprint(Path world) {
        if (baseFingerprint != null) return baseFingerprint;
        try {
            var outer = MessageDigest.getInstance("SHA-256");
            Path region = world.resolve("region");
            try (var files = Files.list(region)) {
                for (Path f : files.filter(f -> f.toString().endsWith(".mca")).sorted().toList()) {
                    outer.update(f.getFileName().toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
                    var inner = MessageDigest.getInstance("SHA-256");
                    inner.update(Files.readAllBytes(f));
                    outer.update(inner.digest());
                }
            }
            return baseFingerprint = hex(outer.digest());
        } catch (Exception ex) {
            throw new IllegalStateException("could not fingerprint the base world at " + world, ex);
        }
    }

    private static String hex(byte[] bytes) {
        var sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) sb.append(Character.forDigit((b >> 4) & 0xF, 16))
                               .append(Character.forDigit(b & 0xF, 16));
        return sb.toString();
    }

    public List<String> report() {
        var out = new ArrayList<String>();
        out.add("MAPS enabled=" + enabled() + " catalogue=" + catalogue.size()
                + " applied=" + (applied == null ? "none" : applied.name));
        for (MapDiff d : catalogue) out.add("  " + d + (d == applied ? "  <- this match" : ""));
        return out;
    }
}
