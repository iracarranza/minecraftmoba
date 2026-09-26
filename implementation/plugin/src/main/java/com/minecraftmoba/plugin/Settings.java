package com.minecraftmoba.plugin;

import org.bukkit.configuration.file.FileConfiguration;
import java.util.HashSet;
import java.util.Map;

public record Settings(int maxLevel, int xpPerLevel, Capacity.Settings capacity, RewardCatalog rewards,
                       TaskLedger taskLedger) {
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
        // Growth levels are read before the curves because a stepped curve is
        // validated against how many Growth events exist.
        if (!c.isList("capacity.growthLevels"))
            throw new IllegalArgumentException("capacity.growthLevels must be a list");
        var levels = c.getIntegerList("capacity.growthLevels");
        if (levels.stream().anyMatch(l -> l <= 1 || l > max) || new HashSet<>(levels).size() != levels.size())
            throw new IllegalArgumentException("Growth levels must be unique and within 2..maxLevel");
        int growths = levels.size();
        var fallback = profile(c, "capacity", growths);
        // Classes share Growth levels, not Growth contents. A class without an
        // authored profile uses the fallback so it still loads; the fallback is
        // a bootstrap, not a roster-wide default curve.
        var profiles = new java.util.HashMap<String, Capacity.Profile>();
        var section = c.getConfigurationSection("capacityProfiles");
        if (section != null) for (String id : section.getKeys(false))
            profiles.put(id, profile(c, "capacityProfiles." + id, growths));
        var rewards = RewardCatalog.load(c.getConfigurationSection("rewards.levels"),max);
        var ledger = new TaskLedger(rewards.taskLevels());
        return new Settings(max, xp, new Capacity.Settings(fallback, profiles,
            new HashSet<>(levels), rewards.bonuses()), rewards, ledger);
    }
    private static Capacity.Profile profile(FileConfiguration c, String root, int growths) {
        var health = curve(c, root, "health", growths);
        var hunger = curve(c, root, "hunger", growths);
        var slots = curve(c, root, "slots", growths);
        // These limits describe the vanilla protocol, not balance parameters.
        if (slots.cap() > 36 || slots.start() < 1 || slots.start() % 1 != 0
                || slots.growthUnit() % 1 != 0 || slots.cap() % 1 != 0
                || slots.steps().stream().anyMatch(v -> v % 1 != 0 || v < 1 || v > 36))
            throw new IllegalArgumentException(root + " slot capacity must be integral within 1..36");
        if (hunger.start() % 1 != 0 || hunger.growthUnit() % 1 != 0 || hunger.cap() % 1 != 0
                || hunger.cap() > Integer.MAX_VALUE
                || hunger.steps().stream().anyMatch(v -> v % 1 != 0))
            throw new IllegalArgumentException(root + " hunger capacity must be a representable integer");
        return new Capacity.Profile(health, hunger, slots);
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
    /**
     * A stat curve. `steps` is the irregular form: absolute values at Lv0
     * followed by one per Growth event, authoritative when present, so `start`
     * is not also required and there is one representation of the Lv0 value.
     */
    private static Capacity.Curve curve(FileConfiguration c, String root, String name, int growths) {
        String p = root + "." + name + ".";
        Object cap = c.get(p + "cap");
        if (!(cap instanceof Number capNumber) || !Double.isFinite(capNumber.doubleValue()) || capNumber.doubleValue() < 0)
            throw new IllegalArgumentException(p + "cap must be finite and nonnegative");
        if (c.isList(p + "steps")) {
            var raw = c.getList(p + "steps");
            if (c.get(p + "start") != null || c.get(p + "growthUnit") != null)
                throw new IllegalArgumentException(p + "steps is authoritative; remove start and growthUnit");
            if (raw.size() != growths + 1)
                throw new IllegalArgumentException(p + "steps must hold " + (growths + 1)
                    + " values: Lv0 followed by one per Growth level, but held " + raw.size());
            var steps = new java.util.ArrayList<Double>(raw.size());
            for (Object value : raw) {
                if (!(value instanceof Number n) || !Double.isFinite(n.doubleValue()) || n.doubleValue() < 0)
                    throw new IllegalArgumentException(p + "steps must be finite and nonnegative");
                steps.add(n.doubleValue());
            }
            if (steps.getFirst() <= 0)
                throw new IllegalArgumentException(p + "steps must start above zero");
            for (int i = 1; i < steps.size(); i++)
                if (steps.get(i) < steps.get(i - 1))
                    throw new IllegalArgumentException(p + "steps must not decrease");
            return new Capacity.Curve(steps.getFirst(), 0, capNumber.doubleValue(), steps);
        }
        for (String key : new String[] {"start", "growthUnit"}) {
            Object value = c.get(p + key);
            if (!(value instanceof Number n) || !Double.isFinite(n.doubleValue()) || n.doubleValue() < 0)
                throw new IllegalArgumentException(p + key + " must be finite and nonnegative");
        }
        var result = new Capacity.Curve(c.getDouble(p + "start"), c.getDouble(p + "growthUnit"), capNumber.doubleValue());
        if (result.start() <= 0 || result.cap() < result.start())
            throw new IllegalArgumentException(p + "requires 0 < start <= cap");
        return result;
    }
}
