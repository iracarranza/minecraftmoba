package com.minecraftmoba.plugin;

import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Renewable sources must hold what they claim to hold, and the marking must
 * follow the resource rather than the region.
 *
 * Both halves were broken in the same way: they described a LOCATION. `manifest`
 * ran only on recovery, which needs `available < capacity`, so a fresh source
 * constructed at full availability never manifested -- every animal pen in the
 * Alpha map reported 6/6 while standing completely empty. And the marker drew a
 * ring at the source's radius, which stops describing a herd the moment it
 * wanders out of the middle.
 *
 * Both are Bukkit-bound, so what is checked here is the decision rule and the
 * source-level contract.
 */
class RenewableMarkerTest {

    @SuppressWarnings("unchecked")
    private Map<String, Object> config() throws Exception {
        try (InputStream in = Files.newInputStream(Path.of("src/main/resources/config.yml"))) {
            return (Map<String, Object>) new Yaml().load(in);
        }
    }

    @SuppressWarnings("unchecked")
    private Object at(String dotted) throws Exception {
        Object node = config();
        for (String part : dotted.split("\\.")) {
            if (!(node instanceof Map)) return null;
            node = ((Map<String, Object>) node).get(part);
            if (node == null) return null;
        }
        return node;
    }

    private static String renewables() throws Exception {
        return Files.readString(Path.of("src/main/java/com/minecraftmoba/plugin/Renewables.java"));
    }

    @Test void registeringASourceManifestsIt() throws Exception {
        // The phantom-herd bug. A source is a claim about the world, not a
        // counter, and before this the claim was never made true.
        String src = renewables();
        int register = src.indexOf("public void register(Source s)");
        assertTrue(register > 0, "register has been renamed");
        String body = src.substring(register, src.indexOf("\n    }", register));
        assertTrue(body.contains("manifest(s)"),
                "a fresh source must put its animals or crops in the world; "
                        + "recovery alone never fires at full availability");
    }

    @Test void aChunkLoadingLateStillFillsThePen() throws Exception {
        // Sources bind before their chunks load, and manifest cannot spawn into
        // an unloaded chunk, so a deferred path has to exist or the fix only
        // works for whichever pens happen to be near spawn.
        String src = renewables();
        assertTrue(src.contains("pendingManifest"), "no deferral for unloaded chunks");
        int onChunk = src.indexOf("public void onChunkLoad");
        String body = src.substring(onChunk, src.indexOf("\n    }", onChunk));
        assertTrue(body.contains("pendingManifest"), "the deferral is never drained");
    }

    @Test void theBoundaryRingIsGone() throws Exception {
        // It marked where a source was, which is the wrong claim for anything
        // that moves. Retired rather than left switched off.
        String src = renewables();
        assertFalse(src.contains("pointsPerRing"), "the ring renderer survives");
        assertNull(at("features.renewableParticles"), "its config survives");
        assertNotNull(at("features.renewableMarkers.enabled"));
    }

    @Test void markingIsCulledAndCached() throws Exception {
        // Marking every source in the world every tick, and rescanning an
        // O(radius^3) volume to find crops, are both affordable only because
        // they do not happen.
        assertNotNull(at("features.renewableMarkers.viewRadius"),
                "without a view radius this runs world-wide every tick");
        assertNotNull(at("features.renewableMarkers.rescanTicks"),
                "without a rescan cadence the crop scan runs per tick");
        String src = Files.readString(
                Path.of("src/main/java/com/minecraftmoba/plugin/RenewableMarkers.java"));
        assertTrue(src.contains("anyoneNear"), "no distance cull");
        assertTrue(src.indexOf("scanCrops") > 0 && src.contains("crops.put"), "no crop cache");
    }

    @Test void onlyGlowThisClassAppliedIsEverCleared() throws Exception {
        // Clearing glow indiscriminately would strip it from anything else that
        // uses it -- a future spectator effect, an ability, an admin tool.
        String src = Files.readString(
                Path.of("src/main/java/com/minecraftmoba/plugin/RenewableMarkers.java"));
        assertTrue(src.contains("marked.remove") && src.contains("marked.add"),
                "the set of entities this class marked must be tracked");
    }

    @Test void aDepletedSourceIsNotMarked() throws Exception {
        String src = Files.readString(
                Path.of("src/main/java/com/minecraftmoba/plugin/RenewableMarkers.java"));
        assertTrue(src.contains("available(source) <= 0"),
                "a mark means available, not merely present");
    }
}
