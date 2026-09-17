package com.minecraftmoba.plugin;

import org.bukkit.configuration.file.FileConfiguration;
import java.util.HashSet;
import java.util.Map;

public record Settings(int maxLevel, int xpPerLevel, Capacity.Settings capacity) {
    public static Settings load(FileConfiguration c) {
        int max = positiveInt(c, "progression.maxLevel");
        int xp = positiveInt(c, "progression.xpPerLevel");
        var health = curve(c, "health");
        var hunger = curve(c, "hunger");
        var slots = curve(c, "slots");
        // These limits describe the vanilla protocol, not balance parameters.
        if (slots.cap() > 36 || slots.start() < 1 || slots.start() % 1 != 0
                || slots.growthUnit() % 1 != 0 || slots.cap() % 1 != 0)
            throw new IllegalArgumentException("Slot capacity must be integral within 1..36");
        if (hunger.start() % 1 != 0 || hunger.growthUnit() % 1 != 0 || hunger.cap() % 1 != 0
                || hunger.cap() > Integer.MAX_VALUE)
            throw new IllegalArgumentException("Hunger capacity must be a representable integer");
        if (!c.isList("capacity.growthLevels"))
            throw new IllegalArgumentException("capacity.growthLevels must be a list");
        var levels = c.getIntegerList("capacity.growthLevels");
        if (levels.stream().anyMatch(l -> l <= 1 || l > max) || new HashSet<>(levels).size() != levels.size())
            throw new IllegalArgumentException("Growth levels must be unique and within 2..maxLevel");
        return new Settings(max, xp, new Capacity.Settings(health, hunger, slots,
            new HashSet<>(levels), Map.of()));
    }
    private static int positiveInt(FileConfiguration c, String path) {
        if (!c.isInt(path) || c.getInt(path) <= 0)
            throw new IllegalArgumentException(path + " must be a positive integer");
        return c.getInt(path);
    }
    private static Capacity.Curve curve(FileConfiguration c, String name) {
        String p = "capacity." + name + ".";
        for (String key : new String[] {"start", "growthUnit", "cap"}) {
            Object value = c.get(p + key);
            if (!(value instanceof Number n) || !Double.isFinite(n.doubleValue()) || n.doubleValue() < 0)
                throw new IllegalArgumentException(p + key + " must be finite and nonnegative");
        }
        var result = new Capacity.Curve(c.getDouble(p + "start"), c.getDouble(p + "growthUnit"), c.getDouble(p + "cap"));
        if (result.start() <= 0 || result.cap() < result.start())
            throw new IllegalArgumentException(p + "requires 0 < start <= cap");
        return result;
    }
}
