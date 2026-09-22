package com.minecraftmoba.plugin;

import org.junit.jupiter.api.Test;

import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Hunger Capacity is a RATE, not a ceiling, and the two must not be confused.
 *
 * Under scaling the bar is always twenty points and Capacity decides how fast
 * it empties. But four call sites kept using the Capacity as a maximum -- eating
 * was blocked at it, spawning set food to it, and the Fountain restored only up
 * to it -- so the bar visibly stopped at 9 even though nothing was capping it
 * on paper. Every one of those reads looked correct in isolation.
 *
 * That is the two-units hazard the Vitals seam exists for, arriving anyway
 * through a method whose name does not say which unit it returns. So the rule
 * is enforced by name: outside the two classes that own the distinction,
 * `effectiveHunger` is not the thing you want.
 */
class HungerUnitsTest {

    /** Owns the definition, or legitimately uses the Capacity as a rate. */
    private static final List<String> ALLOWED =
            List.of("MobaPlugin.java", "Capacity.java", "VitalsScaling.java");

    @Test void nothingOutsideTheSeamTreatsHungerCapacityAsACeiling() throws Exception {
        var offenders = new ArrayList<String>();
        try (Stream<Path> sources = Files.walk(Path.of("src/main/java/com/minecraftmoba/plugin"))) {
            for (Path src : sources.filter(f -> f.toString().endsWith(".java")).toList()) {
                String name = src.getFileName().toString();
                if (ALLOWED.contains(name)) continue;
                String body = Files.readString(src);
                for (String line : body.split("\n"))
                    if (line.contains("effectiveHunger(") && !line.trim().startsWith("*")
                            && !line.trim().startsWith("//"))
                        offenders.add(name + ": " + line.trim());
            }
        }
        assertTrue(offenders.isEmpty(),
                "these read hunger Capacity where they mean the bar's ceiling; use foodCeiling: "
                        + offenders);
    }

    @Test void theCeilingIsTheBarAndTheCapacityIsTheRate() throws Exception {
        String src = Files.readString(Path.of("src/main/java/com/minecraftmoba/plugin/MobaPlugin.java"));
        int i = src.indexOf("public int foodCeiling(Player p)");
        assertTrue(i > 0, "foodCeiling has been renamed or removed");
        String body = src.substring(i, src.indexOf("\n    }", i));
        assertTrue(body.contains("Vitals.DISPLAY_MAX"),
                "under scaling the ceiling is the bar, not the Capacity");
        assertTrue(body.contains("effectiveHunger(p)"),
                "without scaling the old ceiling must still apply");
    }
}
