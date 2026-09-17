package com.minecraftmoba.plugin;

import org.bukkit.configuration.file.FileConfiguration;
import java.util.HashSet;
import java.util.Map;

public record Settings(int maxLevel, int xpPerLevel, Capacity.Settings capacity, RewardCatalog rewards) {
    public static Settings load(FileConfiguration c) {
        // Bukkit scalar fallback is not enough: getKeys() omits default-only registry entries.
        // Include bundled missing keys while preserving every explicit administrator value.
        c.options().copyDefaults(true);
        positiveInt(c, "capacity.enforceTicks");
        positiveInt(c, "abilities.modeTimeoutTicks");
        positiveInt(c, "mapStub.checkTicks");
        positiveInt(c, "provenance.sampleTicks");
        for (String id : java.util.List.of("lunge", "sinkhole_lite", "channel_ult")) {
            String path = "abilities.definitions." + id + ".cooldownTicks";
            if (!c.isInt(path) || c.getInt(path) < 0) throw new IllegalArgumentException(path + " must be nonnegative");
        }
        finitePositive(c,"abilities.definitions.lunge.power");
        finitePositive(c,"abilities.definitions.sinkhole_lite.radius");
        finitePositive(c,"abilities.definitions.sinkhole_lite.rayDistance");
        positiveInt(c,"abilities.definitions.sinkhole_lite.blocksPerStage");
        positiveInt(c,"abilities.definitions.sinkhole_lite.stageTicks");
        int maxBlocks = positiveInt(c,"abilities.definitions.sinkhole_lite.maxSelectionBlocks");
        double reach = Math.ceil(c.getDouble("abilities.definitions.sinkhole_lite.radius"));
        if (Math.pow(2*reach+1,3)>maxBlocks) throw new IllegalArgumentException("Sinkhole selection exceeds configured budget");
        positiveInt(c,"abilities.definitions.channel_ult.channelTicks");
        finitePositive(c,"abilities.definitions.channel_ult.movementThreshold");
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
        var rewards = RewardCatalog.load(c.getConfigurationSection("rewards.levels"),max);
        return new Settings(max, xp, new Capacity.Settings(health, hunger, slots,
            new HashSet<>(levels), rewards.bonuses()), rewards);
    }
    private static void finitePositive(FileConfiguration c,String path) {
        if (!(c.get(path) instanceof Number n) || !Double.isFinite(n.doubleValue()) || n.doubleValue() <= 0)
            throw new IllegalArgumentException(path + " must be finite and positive");
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
