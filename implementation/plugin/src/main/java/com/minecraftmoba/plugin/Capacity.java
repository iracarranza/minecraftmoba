package com.minecraftmoba.plugin;

import java.util.List;
import java.util.Map;
import java.util.Set;

public final class Capacity {
    private Capacity() {}
    public record Curve(double start, double growthUnit, double cap) {}
    public record Bonus(double health, int hunger, int slots) {}
    public record Settings(Curve health, Curve hunger, Curve slots,
                           Set<Integer> growthLevels, Map<String, Bonus> choices) {
        public Settings {
            growthLevels = Set.copyOf(growthLevels);
            choices = Map.copyOf(choices);
        }
    }
    public record DerivedCapacity(double maxHealth, int effectiveHunger, int unlockedSlots) {}
    public static DerivedCapacity recompute(int level, List<PlayerData.ChoiceRecord> choices,
                                             Settings config) {
        long growth = config.growthLevels.stream().filter(l -> l > 1 && l <= level).count();
        double health = config.health.start + growth * config.health.growthUnit;
        double hunger = config.hunger.start + growth * config.hunger.growthUnit;
        double slots = config.slots.start + growth * config.slots.growthUnit;
        for (var choice : choices) {
            var bonus = config.choices.get(choice.choiceId());
            if (choice.level() <= level && bonus != null) {
                health += bonus.health;
                hunger += bonus.hunger;
                slots += bonus.slots;
            }
        }
        return new DerivedCapacity(Math.min(health, config.health.cap),
            (int) Math.min(hunger, config.hunger.cap), (int) Math.min(slots, config.slots.cap));
    }
}
