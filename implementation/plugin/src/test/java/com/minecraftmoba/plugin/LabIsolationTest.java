package com.minecraftmoba.plugin;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.io.IOException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;

class LabIsolationTest {
    @TempDir Path root;

    private LabMaps catalog() throws IOException {
        Path entry = root.resolve("scoop");
        Files.createDirectories(entry.resolve("world/region"));
        Files.writeString(entry.resolve("world/level.dat"), "fixture");
        Files.writeString(entry.resolve("world/region/r.0.0.mca"), "pristine terrain");
        String fingerprint = LabMaps.fingerprint(entry.resolve("world"));
        Files.writeString(entry.resolve("map.json"), "{\"map_id\":\"scoop\",\"seed\":42,\"state\":\"READY\","
                + "\"world_fingerprint\":\"" + fingerprint + "\",\"runtime_bindings\":{}}");
        return new LabMaps(root);
    }

    @Test void repeatedSelectionAndVerificationNeverConsumesTemplates() throws IOException {
        LabMaps maps = catalog();
        for (int run = 0; run < 3; run++) {
            var entry = maps.select("1");
            assertEquals("scoop", entry.mapId());
            maps.verify(entry);
            assertFalse(Files.exists(entry.directory().resolve("claim")));
            assertFalse(Files.exists(entry.directory().resolve("used")));
        }
        assertEquals(MapPool.READY, new MapPool(root).entries().getFirst().state());
    }

    @Test void modifiedTerrainIsRefused() throws IOException {
        LabMaps maps = catalog();
        var entry = maps.select("scoop");
        Files.writeString(entry.world().resolve("region/r.0.0.mca"), "changed terrain");
        assertThrows(IOException.class, () -> maps.verify(entry));
    }

    @Test void quarantineInvalidatesAnAlreadySelectedScoop() throws IOException {
        LabMaps maps = catalog();
        var entry = maps.select("scoop");
        Files.writeString(entry.directory().resolve("quarantined"), "recheck required");
        assertTrue(maps.entries().isEmpty());
        assertThrows(IOException.class, () -> maps.verify(entry));
    }

    @Test void labPreparationCannotInterruptAnyMatchOrDraftState() {
        for (Match.State state : Match.State.values()) {
            assertEquals(state == Match.State.IDLE, Lab.mayPrepare(state, false));
            assertFalse(Lab.mayPrepare(state, true));
        }
    }

    @Test void labMaterializationCopiesItsOwnWorldAndLeavesProductionUntouched() throws IOException {
        LabMaps maps = catalog();
        var entry = maps.select("scoop");
        var plugin = mock(MobaPlugin.class);
        var primary = mock(World.class);
        when(primary.getName()).thenReturn("server_boot");
        var config = new YamlConfiguration();
        config.set("alpha.templatePath", root.resolve("normal-template").toString());
        config.set("alpha.instanceWorldName", "normal-match");
        when(plugin.getConfig()).thenReturn(config);
        Files.createDirectories(root.resolve("normal-match"));
        Files.writeString(root.resolve("normal-match/sentinel"), "production terrain");
        try (var bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getWorldContainer).thenReturn(root.toFile());
            bukkit.when(Bukkit::getWorlds).thenReturn(java.util.List.of(primary));
            WorldInstance instance = new WorldInstance(plugin);
            instance.enterLab(entry);
            assertNull(instance.claimed());
            assertEquals("moba_lab_match", instance.instanceName());
            instance.materialize();
            assertEquals("pristine terrain", Files.readString(root.resolve("moba_lab_match/region/r.0.0.mca")));
            Files.writeString(root.resolve("moba_lab_match/region/r.0.0.mca"), "tester mined terrain");
            instance.materialize();
            maps.verify(entry);
            assertEquals("pristine terrain", Files.readString(root.resolve("moba_lab_match/region/r.0.0.mca")));
            assertEquals("production terrain", Files.readString(root.resolve("normal-match/sentinel")));
            instance.exitLab();
            assertEquals("normal-match", instance.instanceName());
            assertNull(instance.claimed());
        }
    }
}
