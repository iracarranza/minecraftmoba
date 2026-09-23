package com.minecraftmoba.plugin;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The READY map pool.
 *
 * The property worth defending is that two matches starting at the same moment
 * cannot claim the same map. That is why the claim is an exclusive file create
 * rather than a read of the manifest followed by a write: the manifest version
 * is a read-modify-write and races by construction, and it would fail in
 * production rather than here.
 */
class MapPoolTest {

    /** A pool on disk, with the manifests the foundry writes. */
    private static Path pool(String... seeds) throws IOException {
        Path dir = Files.createTempDirectory("moba-pool");
        for (String seed : seeds) {
            Path entry = dir.resolve(seed + "-abcdef0123");
            Files.createDirectories(entry.resolve("world/region"));
            Files.writeString(entry.resolve("map.json"),
                    "{\n \"schema\": \"moba_map_pool_entry/1\",\n"
                    + " \"map_id\": \"" + seed + "-abcdef0123\",\n"
                    + " \"state\": \"READY\",\n"
                    + " \"provenance\": {\n  \"seed\": " + seed + ",\n"
                    + "  \"map_type\": \"default\"\n }\n}\n");
        }
        return dir;
    }

    private static MapPool at(Path dir) { return new MapPool(dir); }

    @Test void itReadsWhatTheFoundryWrote() throws IOException {
        var entries = at(pool("99887766", "2718281")).entries();
        assertEquals(2, entries.size());
        assertEquals(Set.of(2718281L, 99887766L),
                entries.stream().map(MapPool.Entry::seed).collect(java.util.stream.Collectors.toSet()));
        assertTrue(entries.stream().allMatch(e -> MapPool.READY.equals(e.state())));
    }

    @Test void claimingTakesAMapAndMarksItInUse() throws IOException {
        Path dir = pool("99887766");
        MapPool p = at(dir);
        var got = p.claim("match-1");
        assertNotNull(got);
        assertEquals(MapPool.IN_USE, got.state());
        assertEquals(MapPool.IN_USE, p.entries().get(0).state());
    }

    @Test void aClaimedMapIsNotClaimedTwice() throws IOException {
        MapPool p = at(pool("99887766"));
        assertNotNull(p.claim("match-1"));
        assertNull(p.claim("match-2"), "the only map was already taken");
    }

    @Test void concurrentClaimsNeverCollide() throws Exception {
        // The real requirement. Twelve maps, twelve threads, all starting at
        // once: every thread must get a different map or none.
        int n = 12;
        String[] seeds = new String[n];
        for (int i = 0; i < n; i++) seeds[i] = String.valueOf(1000 + i);
        MapPool p = at(pool(seeds));
        var start = new CountDownLatch(1);
        var pool = Executors.newFixedThreadPool(n);
        List<Future<MapPool.Entry>> futures = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            int id = i;
            futures.add(pool.submit(() -> { start.await(); return p.claim("match-" + id); }));
        }
        start.countDown();
        Set<String> claimed = new HashSet<>();
        int nulls = 0;
        for (var f : futures) {
            var e = f.get(10, TimeUnit.SECONDS);
            if (e == null) nulls++;
            else assertTrue(claimed.add(e.mapId()), "two matches claimed " + e.mapId());
        }
        pool.shutdownNow();
        assertEquals(n, claimed.size() + nulls);
        assertTrue(claimed.size() > 0);
    }

    @Test void anEmptyPoolIsNullNotAnException() throws IOException {
        // An empty pool means the foundry has not run recently enough. That is
        // operational, and the caller falls back to the template and says so.
        assertNull(at(pool()).claim("match-1"));
        assertNull(at(Files.createTempDirectory("absent").resolve("nope")).claim("match-1"));
    }

    @Test void aPlayedMapNeverReturnsToReady() throws IOException {
        Path dir = pool("99887766");
        MapPool p = at(dir);
        var got = p.claim("match-1");
        p.retire(got, "north-wins");
        assertEquals(MapPool.USED, p.entries().get(0).state());
        assertNull(p.claim("match-2"), "a used map is not reissued");
    }

    @Test void anEntryWithoutAManifestIsIgnoredNotGuessedAt() throws IOException {
        Path dir = pool("99887766");
        Files.createDirectories(dir.resolve("junk/world"));
        assertEquals(1, at(dir).entries().size());
    }

    @Test void theReportSaysWhenThePoolIsEmpty() throws IOException {
        MapPool p = at(pool("99887766"));
        assertFalse(p.report().contains("EMPTY"));
        p.claim("match-1");
        assertTrue(p.report().contains("EMPTY"),
                "an empty pool must be visible in status, not discovered at match start");
    }
}
