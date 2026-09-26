package com.minecraftmoba.plugin;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Derived player capacity: maximum Health, effective Hunger and unlocked slots.
 *
 * Capacity is one RECIPIENT of Growth, not the owner of it. A Growth event is a
 * class-authored packet that may touch capacity, infrastructure, methodology or
 * several at once; this class only knows about the capacity share. The shared
 * Growth clock currently lives in config under `capacity.growthLevels` because
 * that is where the existing seam is, and that placement is an implementation
 * accident rather than a statement about ownership.
 *
 * Classes share Growth LEVELS. They do not share Growth CONTENTS, so the curves
 * are per class, selected by ClassDefinition.statGrowthProfile. The `fallback`
 * profile exists so an unconfigured class still loads; it is a bootstrap, not a
 * roster-wide default curve.
 *
 * Engine units throughout. Health here is Minecraft HP; the x100 player-facing
 * scale is presentation and belongs to the HUD, not to this arithmetic.
 */
public final class Capacity {
    private Capacity() {}

    /**
     * A stat curve across Growth events.
     *
     * Continuous curves use start + growthUnit per Growth, clamped at cap.
     * Irregular curves supply `steps` instead: absolute values at Lv0 followed
     * by one value per Growth event, so a class can hold a stat flat across a
     * Growth and spend that budget elsewhere. When steps are present they are
     * authoritative and `start`/`growthUnit` are unused.
     */
    public record Curve(double start, double growthUnit, double cap, List<Double> steps) {
        public Curve {
            steps = steps == null ? List.of() : List.copyOf(steps);
        }
        public Curve(double start, double growthUnit, double cap) { this(start, growthUnit, cap, List.of()); }
        public boolean stepped() { return !steps.isEmpty(); }
        /** Value after `growths` completed Growth events, clamped at cap. */
        public double at(int growths) {
            int n = Math.max(0, growths);
            double raw = stepped() ? steps.get(Math.min(n, steps.size() - 1)) : start + n * growthUnit;
            return Math.min(raw, cap);
        }
    }

    public record Bonus(double health, int hunger, int slots) {}

    /** One class's authored stat curves. */
    public record Profile(Curve health, Curve hunger, Curve slots) {}

    public record Settings(Profile fallback, Map<String, Profile> profiles,
                           Set<Integer> growthLevels, Map<String, Bonus> choices) {
        public Settings {
            growthLevels = Set.copyOf(growthLevels);
            choices = Map.copyOf(choices);
            profiles = Map.copyOf(profiles);
        }
        /** Compact form for a roster with no authored profiles yet. */
        public Settings(Curve health, Curve hunger, Curve slots,
                        Set<Integer> growthLevels, Map<String, Bonus> choices) {
            this(new Profile(health, hunger, slots), Map.of(), growthLevels, choices);
        }
        public Profile profile(String id) {
            return id == null ? fallback : profiles.getOrDefault(id, fallback);
        }
    }

    public record DerivedCapacity(double maxHealth, int effectiveHunger, int unlockedSlots) {}

    public static DerivedCapacity recompute(int level, List<PlayerData.ChoiceRecord> choices,
                                             Settings config) {
        return recompute(level, choices, config, null);
    }

    /** `profileId` is ClassDefinition.statGrowthProfile; null or unknown uses the fallback. */
    public static DerivedCapacity recompute(int level, List<PlayerData.ChoiceRecord> choices,
                                             Settings config, String profileId) {
        // Growth events strictly at or below the player's level. Level 0 is the
        // enrolled starting state and completes no Growth, so it reads `start`.
        int growth = (int) config.growthLevels.stream().filter(l -> l <= level).count();
        var profile = config.profile(profileId);
        double health = profile.health().at(growth);
        double hunger = profile.hunger().at(growth);
        double slots = profile.slots().at(growth);
        // Capacity specialization is superseded by Growth. Records are still read
        // so an existing save does not change meaning underfoot, but nothing in
        // current configuration registers a capacity bonus.
        for (var choice : choices) {
            var bonus = config.choices.get(choice.choiceId());
            if (choice.level() <= level && bonus != null) {
                health += bonus.health;
                hunger += bonus.hunger;
                slots += bonus.slots;
            }
        }
        return new DerivedCapacity(Math.min(health, profile.health().cap()),
            (int) Math.min(hunger, profile.hunger().cap()),
            (int) Math.min(slots, profile.slots().cap()));
    }
}
