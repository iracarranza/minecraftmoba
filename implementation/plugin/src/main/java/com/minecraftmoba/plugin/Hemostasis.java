package com.minecraftmoba.plugin;

/** Endpoints manifest before immediately resetting; natural stabilization never crosses 3/4. */
final class Hemostasis {
    private int value = 3;
    private long stabilizeAt;
    private long pathologicalUntil;
    record Cast(int manifestation, boolean endpoint, boolean relapse) {}
    Hemostasis() {}
    Hemostasis(int value) { this.value = Math.max(1, Math.min(5, value)); }
    int value() { return value; }
    Cast cast(int delta, long tick, int interval, int pathologyTicks) {
        int next = Math.max(0, Math.min(6, value + delta));
        boolean endpoint = next == 0 || next == 6;
        boolean relapse = endpoint && tick < pathologicalUntil;
        value = next == 0 ? 1 : next == 6 ? 5 : next;
        stabilizeAt = tick + interval;
        if (endpoint) pathologicalUntil = tick + pathologyTicks;
        return new Cast(next, endpoint, relapse);
    }
    void tick(long tick, int interval) {
        if (tick < stabilizeAt) return;
        if (value < 3) value++; else if (value > 4) value--;
        stabilizeAt = tick + interval;
    }
}
