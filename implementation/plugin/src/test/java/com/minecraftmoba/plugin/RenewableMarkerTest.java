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

    @Test void registeringASourceStartsItsLifecycleRatherThanSpawningAtOnce() throws Exception {
        // The phantom-herd bug was that manifestation ran ONLY on recovery,
        // which needs available < capacity, so a fresh source never manifested.
        // The fix is no longer "manifest on register": an opportunity now begins
        // an initial delay and the lifecycle tick manifests it when it is due
        // and a locus is eligible. Manifesting on register would put the herd in
        // the world on the first tick of the match, which is the opening the
        // regenerative economy is meant to follow rather than race.
        String src = renewables();
        int register = src.indexOf("public void register(Source s)");
        assertTrue(register > 0, "register has been renamed");
        String body = src.substring(register, src.indexOf("\n    }", register));
        assertTrue(body.contains("beginInitialDelay"),
                "a fresh source must enter the lifecycle, not sit at full availability forever");
        assertFalse(body.contains("manifest(s)"),
                "manifesting on register bypasses the initial delay and the eligibility query");
    }

    @Test void theLifecycleRetriesRatherThanDeferringOnce() throws Exception {
        // Sources bind before their chunks load and a query cannot answer for an
        // unloaded column. The old fix was a pendingManifest set drained on
        // ChunkLoadEvent; the lifecycle tick now retries on its own cadence,
        // which also covers a region that becomes eligible again later.
        String src = renewables();
        assertTrue(src.contains("tickOpportunities"), "no lifecycle tick");
        assertTrue(src.contains("readyToManifest()"),
                "attempts must be gated on the lifecycle state, not on a one-shot deferral");
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
        Object view = at("features.renewableMarkers.viewRadius");
        assertNotNull(view, "without a view radius this runs world-wide every tick");
        // A gate wider than the client's entity tracking range is not a gate:
        // beyond it the entity is never sent, so the glow cannot render however
        // the marker is configured. spigot.yml ships animals at 96.
        assertTrue(((Number) view).doubleValue() <= 96.0,
                "viewRadius must stay inside entity-tracking-range.animals");
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
