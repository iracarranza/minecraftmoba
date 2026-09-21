package com.minecraftmoba.plugin;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The tome's renderer must be reattached, not assumed.
 *
 * A MapView persists in world data; its RENDERERS do not. After every server
 * restart the tome came back as a plain vanilla map, which draws exactly what a
 * vanilla map of unexplored ground draws -- nothing. The item was still there,
 * the log was clean, and the map had simply stopped working.
 *
 * The cause was ordering. `ensure` checked "is this a sentinel?" first and
 * returned early, which was correct while the sentinel was a separate skull and
 * wrong the moment the tome became both a sentinel and a map. Ordering bugs
 * survive refactors unless something pins the order, so this pins it.
 */
class OffhandMapRendererTest {

    private String ensureBody() throws Exception {
        String src = Files.readString(
                Path.of("src/main/java/com/minecraftmoba/plugin/OffhandMap.java"));
        int start = src.indexOf("public boolean ensure(Player p)");
        assertTrue(start > 0, "ensure has been renamed");
        return src.substring(start, src.indexOf("\n    }", start));
    }

    @Test void anythingWithAMapViewIsRestubbedBeforeTheSentinelCheck() throws Exception {
        String body = ensureBody();
        int stub = body.indexOf("stub(meta.getMapView())");
        int sentinel = body.indexOf("isSentinel(held)");
        assertTrue(stub > 0, "the held map's renderer is never refreshed");
        assertTrue(sentinel > 0, "the skull representation is no longer handled");
        assertTrue(stub < sentinel,
                "the tome is BOTH a sentinel and a map; checking sentinel first "
                        + "returns early and leaves it with vanilla renderers");
    }

    @Test void theSkullIsStillNotCastToMapMeta() throws Exception {
        // The reason the sentinel check existed at all: a skull has no MapView
        // and casting its meta to MapMeta crashed with CraftMetaSkull.
        String body = ensureBody();
        assertTrue(body.contains("instanceof MapMeta"),
                "the MapView path must stay guarded by an instanceof, not a cast");
    }

    @Test void restubbingIsIdempotent() throws Exception {
        // ensure() runs on a timer, so it re-stubs constantly. stub() must not
        // pile up renderers or clear a live one every tick.
        String src = Files.readString(
                Path.of("src/main/java/com/minecraftmoba/plugin/OffhandMap.java"));
        int start = src.indexOf("private void stub(MapView view)");
        String body = src.substring(start, src.indexOf("\n    }", start));
        assertTrue(body.contains("anyMatch(wanted::isInstance)") && body.contains("return"),
                "stub must return early when the wanted renderer is already attached");
    }
}
