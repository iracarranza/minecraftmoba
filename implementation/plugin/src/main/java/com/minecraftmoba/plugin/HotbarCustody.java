package com.minecraftmoba.plugin;

import java.util.*;

/** A single-owner loan. Taking twice is refused; returning twice pays nothing. */
final class HotbarCustody<T> {
    private final Map<UUID, List<T>> held = new HashMap<>();
    boolean hold(UUID player, List<T> slots) {
        if (slots.size() != 9) throw new IllegalArgumentException("A hotbar has nine slots");
        if (held.containsKey(player)) return false;
        held.put(player, Collections.unmodifiableList(new ArrayList<>(slots)));
        return true;
    }
    boolean contains(UUID player) { return held.containsKey(player); }
    List<T> release(UUID player) { return held.remove(player); }
}
