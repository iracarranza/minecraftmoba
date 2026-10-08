package com.minecraftmoba.plugin;

import java.util.*;
import java.util.function.Consumer;

/** Bounded LIFO journal; an unsuccessful restore leaves the operation retryable. */
final class LabUndo<T> {
    /** Entries kept, and blocks across all of them. Named so callers can size against them. */
    static final int MAX_ENTRIES = 8;
    static final int MAX_BLOCKS = 120_000;

    record Entry<T>(String description, List<T> before, List<UUID> entities) {}
    private final Deque<Entry<T>> history = new ArrayDeque<>();
    private int size;
    void push(String description, List<T> before, List<UUID> entities) {
        history.push(new Entry<>(description,List.copyOf(before),List.copyOf(entities)));
        size += before.size();
        while (history.size() > MAX_ENTRIES || size > MAX_BLOCKS)
            size -= history.removeLast().before.size();
    }
    String undo(Consumer<T> restore, Consumer<UUID> removeEntity) {
        if (history.isEmpty()) throw new IllegalStateException("Nothing to undo in this lab session.");
        var entry = history.peek();
        entry.entities.forEach(removeEntity);
        // Reverse the write order, with physics disabled by the caller.
        for (int i = entry.before.size()-1; i >= 0; i--) restore.accept(entry.before.get(i));
        history.pop(); size -= entry.before.size(); return entry.description;
    }
    int depth() { return history.size(); }
    void clear() { history.clear(); size = 0; }
}
