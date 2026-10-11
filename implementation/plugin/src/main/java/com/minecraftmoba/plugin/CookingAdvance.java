package com.minecraftmoba.plugin;

/** Productive ticks, with sequential carryover and fuel payment. No Minecraft state. */
final class CookingAdvance {
    record Result(int completed, int progress, int productiveTicks, int fuelSpent) {}
    static Result advance(int count, int progress, int perItem, int ticks, int fuel, double fuelFraction) {
        if (count < 0 || progress < 0 || perItem <= 0 || ticks < 0 || fuel < 0
                || fuelFraction <= 0 || fuelFraction > 1) throw new IllegalArgumentException("Invalid cooking budget");
        int room = Math.max(0, count * perItem - progress);
        int productive = Math.min(Math.min(room, ticks), (int) Math.floor(fuel / fuelFraction));
        int total = progress + productive;
        int done = Math.min(count, total / perItem);
        return new Result(done, done == count ? 0 : total % perItem, productive,
                (int) Math.ceil(productive * fuelFraction));
    }
}
