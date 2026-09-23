package com.minecraftmoba.plugin;

import org.bukkit.Bukkit;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/**
 * The READY map pool: claim a previously compiled map, do not search for one.
 *
 * `/moba match start` must never go looking for geography. Screening rejects
 * roughly four seeds in five at recognition alone and authoring a world takes
 * minutes, so a search inside match start is a player watching a loading screen
 * while the server brute-forces terrain. The foundry does that offline and
 * writes verified maps here; this only claims one.
 *
 * State moves one way: READY -> IN_USE -> USED. A map is never returned to
 * READY, because it has been played in -- players changed it, and the whole
 * point of a pool entry is that it is pristine when claimed.
 *
 * Claiming has to be atomic, because two matches starting together must not
 * take the same map. The claim is a file created with CREATE_NEW, which the
 * filesystem makes exclusive: the first writer wins and the loser sees
 * FileAlreadyExistsException and moves to the next candidate. Writing "state":
 * "IN_USE" into the manifest and hoping would be a read-modify-write race.
 */
public final class MapPool {
    public static final String READY = "READY";
    public static final String IN_USE = "IN_USE";
    public static final String USED = "USED";

    /** One pool entry: its directory, its id, and the provenance it carries. */
    public record Entry(String mapId, Path directory, long seed, String state) {
        public Path world() { return directory.resolve("world"); }
    }

    private final MobaPlugin plugin;
    private final Path fixed;

    public MapPool(MobaPlugin plugin) { this.plugin = plugin; this.fixed = null; }

    /**
     * A pool at a known directory, with no plugin behind it.
     *
     * Exists so the claim can be tested against a real filesystem, which is
     * where its only interesting property lives: two matches must not take the
     * same map, and that is a question about file creation rather than about
     * Bukkit.
     */
    public MapPool(Path directory) { this.plugin = null; this.fixed = directory; }

    public boolean enabled() {
        return fixed != null || plugin.getConfig().getBoolean("alpha.pool.enabled", false);
    }

    /** Where the foundry writes. Resolved like the template path, for the same reason. */
    public Path directory() {
        if (fixed != null) return fixed;
        String configured = plugin.getConfig().getString("alpha.pool.directory", "map-pool");
        Path given = Paths.get(configured);
        if (given.isAbsolute()) return given;
        return Bukkit.getWorldContainer().toPath().toAbsolutePath().normalize().resolve(given);
    }

    private void warn(String message) {
        if (plugin != null) plugin.getLogger().warning(message);
    }

    private void info(String message) {
        if (plugin != null) plugin.getLogger().info(message);
    }

    private static String field(String json, String key) {
        // The manifest is written by the foundry and read here; a full JSON
        // parser is not worth a dependency for three flat fields, but a sloppy
        // scan would be. This matches "key": value at the top level only.
        String needle = '"' + key + "\":";
        int at = json.indexOf(needle);
        if (at < 0) return null;
        int i = at + needle.length();
        while (i < json.length() && Character.isWhitespace(json.charAt(i))) i++;
        if (i >= json.length()) return null;
        if (json.charAt(i) == '"') {
            int end = json.indexOf('"', i + 1);
            return end < 0 ? null : json.substring(i + 1, end);
        }
        int end = i;
        while (end < json.length() && "-0123456789".indexOf(json.charAt(end)) >= 0) end++;
        return end == i ? null : json.substring(i, end);
    }

    /** Every entry the pool holds, in a stable order. */
    public List<Entry> entries() {
        Path dir = directory();
        if (!Files.isDirectory(dir)) return List.of();
        List<Entry> out = new ArrayList<>();
        try (var stream = Files.list(dir)) {
            for (Path d : stream.sorted().toList()) {
                Path manifest = d.resolve("map.json");
                if (!Files.isRegularFile(manifest)) continue;
                String json = Files.readString(manifest, StandardCharsets.UTF_8);
                String id = field(json, "map_id");
                String seed = field(json, "seed");
                if (id == null) continue;
                // The claim and used markers, not the manifest, are the
                // authority on whether a map is taken: they are what gets
                // written atomically. USED is checked FIRST because it is
                // terminal -- a retired map still has its claim file, and
                // checking claim first reported a played map as IN_USE
                // forever, which would have leaked the pool one map per match.
                String state = Files.exists(d.resolve("used")) ? USED
                        : Files.exists(d.resolve("claim")) ? IN_USE
                        : Objects.requireNonNullElse(field(json, "state"), READY);
                out.add(new Entry(id, d, seed == null ? 0L : Long.parseLong(seed), state));
            }
        } catch (IOException e) {
            warn("[pool] cannot read " + dir + ": " + e.getMessage());
            return List.of();
        }
        return out;
    }

    /**
     * Take an unused map, or return null if the pool is empty.
     *
     * Null rather than an exception: an empty pool is an operational state -- the
     * foundry has not run recently enough -- not a bug, and the caller can fall
     * back to the configured template and say so.
     */
    public Entry claim(String matchId) {
        for (Entry e : entries()) {
            if (!READY.equals(e.state())) continue;
            try {
                Files.writeString(e.directory().resolve("claim"),
                        matchId + "\n" + System.currentTimeMillis() + "\n",
                        StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
            } catch (FileAlreadyExistsException race) {
                continue;               // another match got there first
            } catch (IOException io) {
                warn("[pool] could not claim " + e.mapId() + ": " + io);
                continue;
            }
            info("[pool] claimed " + e.mapId() + " (seed " + e.seed() + ")");
            return new Entry(e.mapId(), e.directory(), e.seed(), IN_USE);
        }
        return null;
    }

    /** Retire a claimed map. Terminal: a played map is never READY again. */
    public void retire(Entry entry, String result) {
        if (entry == null) return;
        try {
            Files.writeString(entry.directory().resolve("used"),
                    result + "\n" + System.currentTimeMillis() + "\n",
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException io) {
            warn("[pool] could not retire " + entry.mapId() + ": " + io);
        }
    }

    public String report() {
        if (!enabled()) return "pool=disabled (alpha.pool.enabled)";
        var all = entries();
        long ready = all.stream().filter(e -> READY.equals(e.state())).count();
        long inUse = all.stream().filter(e -> IN_USE.equals(e.state())).count();
        long used = all.stream().filter(e -> USED.equals(e.state())).count();
        return "pool=" + directory() + " entries=" + all.size()
                + " READY=" + ready + " IN_USE=" + inUse + " USED=" + used
                + (ready == 0 ? " (EMPTY: run the foundry, or matches fall back to the template)" : "");
    }
}
