package com.minecraftmoba.plugin;

import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.*;
import java.util.*;

/** Reusable certified templates. Reading/testing these never writes claim/used markers. */
public final class LabMaps {
    private final Path directory;
    public LabMaps(Path directory) { this.directory = directory; }
    public Path directory() { return directory; }

    public List<MapPool.Entry> entries() {
        return new MapPool(directory).entries().stream()
                .filter(e -> MapPool.READY.equals(e.state()))
                .filter(e -> Files.isRegularFile(e.world().resolve("level.dat")))
                .filter(e -> MapBindings.of(e) != null).toList();
    }

    public MapPool.Entry select(String id) {
        var entries = entries();
        if (id != null && id.matches("[1-9][0-9]*")) {
            int i = Integer.parseInt(id) - 1;
            if (i < entries.size()) return entries.get(i);
        }
        return entries.stream().filter(e -> e.mapId().equals(id)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown lab scoop: " + id));
    }

    public String label(MapPool.Entry entry) {
        try {
            var doc = JsonParser.parseString(Files.readString(entry.directory().resolve("map.json"))).getAsJsonObject();
            return doc.has("lab_label") ? doc.get("lab_label").getAsString() : "Seed " + entry.seed();
        } catch (Exception ex) { return "Seed " + entry.seed(); }
    }

    public void verify(MapPool.Entry entry) throws IOException {
        if (!entries().stream().anyMatch(e -> e.mapId().equals(entry.mapId())))
            throw new IOException("Lab scoop is missing or quarantined: " + entry.mapId());
        var doc = JsonParser.parseString(Files.readString(entry.directory().resolve("map.json"))).getAsJsonObject();
        if (!doc.has("world_fingerprint")) throw new IOException("Lab scoop has no fingerprint.");
        if (!fingerprint(entry.world()).equals(doc.get("world_fingerprint").getAsString()))
            throw new IOException("Lab scoop differs from its certified template: " + entry.mapId());
    }

    static String fingerprint(Path world) throws IOException {
        try {
            MessageDigest combined = MessageDigest.getInstance("SHA-256");
            try (var files = Files.list(world.resolve("region"))) {
                for (Path file : files.filter(p -> p.getFileName().toString().endsWith(".mca")).sorted().toList()) {
                    combined.update(file.getFileName().toString().getBytes(StandardCharsets.UTF_8));
                    combined.update(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(file)));
                }
            }
            return HexFormat.of().formatHex(combined.digest());
        } catch (NoSuchAlgorithmException ex) { throw new IllegalStateException(ex); }
    }
}
