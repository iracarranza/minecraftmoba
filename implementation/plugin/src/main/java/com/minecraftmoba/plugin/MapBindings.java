package com.minecraftmoba.plugin;

import com.google.gson.*;
import org.bukkit.Location;
import org.bukkit.World;

import java.nio.file.*;
import java.util.*;

/**
 * Where every match system finds its positions on a generated map.
 *
 * The runtime used to read `alpha.homelands`, `alpha.worksites.sites` and
 * `alpha.lair.site` out of config. That is correct for the one frozen Alpha
 * template and wrong for every generated map, because those coordinates
 * describe a different world. A claimed realization carries its own bindings in
 * its manifest, and this reads them.
 *
 * Config remains the fallback, deliberately: the Alpha template is still a
 * useful reference fixture, and a server with no pool should keep working. What
 * changed is that config is no longer the only answer.
 *
 * Absent is absent. A binding this cannot resolve returns null rather than a
 * default, because a silently defaulted Fountain would put a team's respawn in
 * the wrong world.
 */
public final class MapBindings {
    private final JsonObject root;

    private MapBindings(JsonObject root) { this.root = root; }

    /** Read a claimed pool entry's manifest, or null if it has none. */
    public static MapBindings of(MapPool.Entry entry) {
        if (entry == null) return null;
        Path manifest = entry.directory().resolve("map.json");
        if (!Files.isRegularFile(manifest)) return null;
        try {
            JsonObject doc = JsonParser.parseString(Files.readString(manifest)).getAsJsonObject();
            JsonElement bindings = doc.get("runtime_bindings");
            if (bindings == null || !bindings.isJsonObject()) return null;
            return new MapBindings(bindings.getAsJsonObject());
        } catch (Exception failure) {
            return null;
        }
    }

    static MapBindings parse(String json) {
        return new MapBindings(JsonParser.parseString(json).getAsJsonObject());
    }

    private JsonObject obj(String... path) {
        JsonElement at = root;
        for (String key : path) {
            if (at == null || !at.isJsonObject()) return null;
            at = at.getAsJsonObject().get(key);
        }
        return at != null && at.isJsonObject() ? at.getAsJsonObject() : null;
    }

    private static Location location(World world, JsonElement xyz, boolean hasY) {
        if (xyz == null || !xyz.isJsonArray()) return null;
        JsonArray a = xyz.getAsJsonArray();
        if (a.size() < (hasY ? 3 : 2)) return null;
        double x = a.get(0).getAsDouble();
        double y = hasY ? a.get(1).getAsDouble() : 0;
        double z = a.get(hasY ? 2 : 1).getAsDouble();
        // The manifest stores block coordinates; centre them so an entity does
        // not spawn in the corner of a block.
        return new Location(world, x + 0.5, y, z + 0.5);
    }

    /** A team's Aether Fountain: spawn, respawn and reconstruction anchor. */
    public Location fountain(World world, Team team) {
        JsonObject f = obj("fountains");
        return f == null ? null : location(world, f.get(team.lower()), true);
    }

    /** A defensive objective's position, by team and kind. */
    public Location objective(World world, Team team, String kind) {
        JsonObject byTeam = obj("objectives", team.lower());
        if (byTeam == null) return null;
        Location at = location(world, byTeam.get(kind), false);
        if (at == null) return null;
        // Objectives are stored as [x, z]: the surface decides y, and baking one
        // in would break if the world were ever re-authored.
        at.setY(world.getHighestBlockYAt(at.getBlockX(), at.getBlockZ()) + 1);
        return at;
    }

    public List<String> objectiveKinds(Team team) {
        JsonObject byTeam = obj("objectives", team.lower());
        return byTeam == null ? List.of() : new ArrayList<>(byTeam.keySet());
    }

    /** The singular Lair's spawn/binding anchor. */
    public Location lairAnchor(World world) {
        JsonObject lair = obj("lair");
        if (lair == null) return null;
        JsonObject anchor = lair.getAsJsonObject("anchor");
        return anchor == null ? null : location(world, anchor.get("xyz"), true);
    }

    public int lairCount() {
        JsonObject lair = obj("lair");
        return lair == null || lair.get("count") == null ? 0 : lair.get("count").getAsInt();
    }

    /** The Worksite portfolio: id and position for each site. */
    public List<Map<String, Object>> worksites(World world) {
        JsonElement sites = root.get("worksites");
        if (sites == null || !sites.isJsonArray()) return List.of();
        List<Map<String, Object>> out = new ArrayList<>();
        for (JsonElement e : sites.getAsJsonArray()) {
            if (!e.isJsonObject()) continue;
            JsonObject site = e.getAsJsonObject();
            JsonElement xz = site.get("world_xz");
            if (xz == null || !xz.isJsonArray() || xz.getAsJsonArray().size() < 2) continue;
            int x = xz.getAsJsonArray().get(0).getAsInt();
            int z = xz.getAsJsonArray().get(1).getAsInt();
            out.add(Map.of("id", site.has("id") ? site.get("id").getAsString() : "ws_" + x + "_" + z,
                           "x", x,
                           "y", world == null ? 64 : world.getHighestBlockYAt(x, z) + 1,
                           "z", z));
        }
        return out;
    }

    public String worldName() {
        JsonObject w = obj("world");
        return w == null || w.get("name") == null ? null : w.get("name").getAsString();
    }

    /** Which systems this manifest can bind, for status output. */
    public String report() {
        JsonObject lair = obj("lair");
        return "bindings: fountains=" + (obj("fountains") == null ? 0 : obj("fountains").size())
                + " objectives=" + (obj("objectives") == null ? 0 : obj("objectives").size())
                + " lair=" + (lair != null && lair.getAsJsonObject("anchor") != null
                              ? "anchored" : "MISSING")
                + " worksites=" + (root.get("worksites") != null && root.get("worksites").isJsonArray()
                                   ? root.get("worksites").getAsJsonArray().size() : 0);
    }
}
