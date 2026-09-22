package com.minecraftmoba.plugin;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;

/**
 * One map configuration: every block it changes about the base terrain.
 *
 * A match's world is a copy of the base with one of these applied, which is what
 * lets a configuration be a 300KB file instead of a 28MB world, lets a second
 * one cost nothing to add, and keeps every structure defined once -- in the
 * Python authoring tools, where the terrain analysis already lives.
 *
 * Verification is a fingerprint rather than a search. A diff records the base it
 * was computed against, so the only question at apply time is whether this
 * server holds that base. Applying a diff to the wrong terrain would produce a
 * plausible-looking world that is wrong everywhere, so the check refuses rather
 * than warns.
 */
public final class MapDiff {
    public final String name;
    public final String note;
    public final String baseFingerprint;
    private final List<BlockData> palette = new ArrayList<>();
    /** Flat x,y,z,paletteIndex quadruples, as exported. */
    private final int[] entries;

    private MapDiff(String name, String note, String baseFingerprint,
                    List<String> states, int[] entries) {
        this.name = name;
        this.note = note;
        this.baseFingerprint = baseFingerprint;
        this.entries = entries;
        for (String state : states) palette.add(Bukkit.createBlockData(state));
    }

    public int blocks() { return entries.length / 4; }

    public static MapDiff load(Path file) throws Exception {
        try (Reader r = new InputStreamReader(
                new GZIPInputStream(Files.newInputStream(file)), StandardCharsets.UTF_8)) {
            JsonObject root = JsonParser.parseReader(r).getAsJsonObject();
            String schema = root.get("schema").getAsString();
            if (!schema.startsWith("moba_map_diff/"))
                throw new IllegalArgumentException(file + " is not a map diff: " + schema);

            var states = new ArrayList<String>();
            root.getAsJsonArray("palette").forEach(e -> states.add(e.getAsString()));
            var array = root.getAsJsonArray("entries");
            int[] flat = new int[array.size()];
            for (int i = 0; i < flat.length; i++) flat[i] = array.get(i).getAsInt();

            return new MapDiff(root.get("name").getAsString(),
                    root.has("note") ? root.get("note").getAsString() : "",
                    root.get("base_fingerprint").getAsString(), states, flat);
        }
    }

    /**
     * Write every changed block into the world.
     *
     * Physics is suppressed: this is authored terrain arriving all at once, and
     * letting gravity and water run mid-application would rearrange it. Measured
     * at roughly 200,000 blocks in 2.8 seconds against a 4.4 second world copy,
     * so it happens inside a phase that already blocks.
     */
    public int applyTo(World world) {
        int applied = 0;
        for (int i = 0; i < entries.length; i += 4) {
            world.getBlockAt(entries[i], entries[i + 1], entries[i + 2])
                 .setBlockData(palette.get(entries[i + 3]), false);
            applied++;
        }
        return applied;
    }

    @Override public String toString() {
        return name + " (" + blocks() + " blocks, base " + baseFingerprint.substring(0, 8) + ")";
    }
}
