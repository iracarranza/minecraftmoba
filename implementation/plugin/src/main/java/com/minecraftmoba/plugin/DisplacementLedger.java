package com.minecraftmoba.plugin;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Which world positions are reserved, by whom, and what is owed back to them.
 *
 * The bookkeeping half of {@link Displacement}, with no Bukkit in it, because
 * the rule it enforces is arithmetic that wants testing and the block handling
 * around it is not.
 *
 * <h2>The rule, from classes.md section 27</h2>
 *
 * A temporarily displaced block's original position stays <b>reserved</b> until
 * it is restored, and nothing may occupy a reserved position in a way that
 * would prevent the block returning. Permanent displacement reserves nothing.
 *
 * <h2>A position may be reserved once</h2>
 *
 * Two abilities cannot both borrow the same block. The second attempt is
 * refused rather than overwriting the first, because overwriting loses the
 * first block entirely: its payload is the only record that it existed, and
 * the position it came from is already empty.
 *
 * <h2>Insertion order is preserved</h2>
 *
 * Restoration walks a token's positions in the order they were taken. Nothing
 * requires that today -- restoring without physics is order-independent -- but
 * a deterministic order makes a partial restore reproducible, and a random one
 * would make the first ordering-sensitive caller mysteriously flaky.
 *
 * @param <T> whatever the caller needs to put the block back, opaque here.
 */
public final class DisplacementLedger<T> {

    /** A world position. A record so equality and hashing are the identity. */
    public record Key(UUID world, int x, int y, int z) {}

    private record Entry<T>(UUID token, T payload) {}

    private final Map<Key, Entry<T>> reserved = new LinkedHashMap<>();
    private final Map<UUID, Set<Key>> byToken = new LinkedHashMap<>();

    /**
     * Reserve a position for a token.
     *
     * @return false if it is already reserved, by this token or any other.
     */
    public boolean reserve(Key key, UUID token, T payload) {
        if (key == null || token == null) return false;
        if (reserved.containsKey(key)) return false;
        reserved.put(key, new Entry<>(token, payload));
        byToken.computeIfAbsent(token, k -> new LinkedHashSet<>()).add(key);
        return true;
    }

    public boolean isReserved(Key key) { return key != null && reserved.containsKey(key); }

    /** Nothing is reserved at all -- the fast path for the hot event guards. */
    public boolean isEmpty() { return reserved.isEmpty(); }

    public int size() { return reserved.size(); }

    /** The token holding a position, or null. */
    public UUID holderOf(Key key) {
        Entry<T> e = reserved.get(key);
        return e == null ? null : e.token();
    }

    /**
     * Release everything a token holds, in the order it was taken, and return
     * it for restoration.
     *
     * Released as it is returned rather than after the caller restores, so a
     * caller that throws mid-restore cannot leave positions reserved forever
     * against blocks nobody is going to put back. Losing a block is bad; a
     * permanently unbuildable hole in the map is worse, because it outlives
     * the ability and nothing in the world explains it.
     */
    public List<Map.Entry<Key, T>> release(UUID token) {
        Set<Key> keys = byToken.remove(token);
        if (keys == null) return List.of();
        var out = new java.util.ArrayList<Map.Entry<Key, T>>(keys.size());
        for (Key k : keys) {
            Entry<T> e = reserved.remove(k);
            if (e != null) out.add(Map.entry(k, e.payload()));
        }
        return out;
    }

    /** Release one position, returning its payload, or null if it was not reserved. */
    public T release(Key key) {
        Entry<T> e = reserved.remove(key);
        if (e == null) return null;
        Set<Key> keys = byToken.get(e.token());
        if (keys != null) {
            keys.remove(key);
            if (keys.isEmpty()) byToken.remove(e.token());
        }
        return e.payload();
    }

    /** Every reservation, oldest first, for a match-end sweep. */
    public List<Map.Entry<Key, T>> releaseAll() {
        var out = new java.util.ArrayList<Map.Entry<Key, T>>(reserved.size());
        for (var e : reserved.entrySet()) out.add(Map.entry(e.getKey(), e.getValue().payload()));
        reserved.clear();
        byToken.clear();
        return out;
    }

    /** How many positions a token holds. */
    public int heldBy(UUID token) {
        Set<Key> keys = byToken.get(token);
        return keys == null ? 0 : keys.size();
    }
}
