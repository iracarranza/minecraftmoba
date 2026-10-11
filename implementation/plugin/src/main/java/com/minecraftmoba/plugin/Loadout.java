package com.minecraftmoba.plugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

/**
 * A set of items that can be given to a body or a tester: the 36 inventory slots and four armor pieces.
 * Pure and generic over the item type, so the rules are tests and the Bukkit layer
 * is only the translation to {@code ItemStack}.
 *
 * <h2>Slot numbering is Bukkit's own</h2>
 *
 * 0-8 the hotbar, 9-35 the rest of the inventory, then helmet, chestplate, leggings and boots at
 * 36-39. That numbering is the same as the editor's slot numbering
 * ({@link LoadoutEditorLayout}), so the two never need a translation table.
 *
 * <h2>The offhand is not part of a loadout</h2>
 *
 * An enrolled player or body carries the plugin's own map in the offhand, and the plugin PAUSES their
 * enrolment (class, abilities, progression) the moment the offhand holds anything else. A loadout
 * that put an item there would silently disable the class it was meant to test, so it cannot.
 *
 * <h2>One slot is reserved</h2>
 *
 * Slot {@link #EXIT_SLOT} holds the "Exit testing" item while a tester is in testing mode, so a
 * loadout cannot use it. It is the last hotbar slot, so the exit item can be held and used with a
 * click, and it is where the chamber menu's Back sits, so the habit is the same in both.
 *
 * Immutable: every change returns a new loadout.
 */
public final class Loadout<T> {
    public static final int INVENTORY = 36;
    public static final int HELMET = 36, CHESTPLATE = 37, LEGGINGS = 38, BOOTS = 39;
    public static final int SIZE = 40;
    /** The inventory slot the "Exit testing" item occupies, and so a loadout cannot: the hotbar's last, where the chamber menu's Back also lives. */
    public static final int EXIT_SLOT = 8;

    private final List<T> items;

    private Loadout(List<T> items) { this.items = Collections.unmodifiableList(items); }

    public static <T> Loadout<T> empty() { return new Loadout<>(new ArrayList<>(Collections.nCopies(SIZE, null))); }

    public static boolean valid(int slot) { return slot >= 0 && slot < SIZE; }

    public static boolean reserved(int slot) { return slot == EXIT_SLOT; }

    public T at(int slot) {
        check(slot);
        return items.get(slot);
    }

    /** A copy with one slot changed. A reserved slot refuses, so a loadout can never hold the exit slot. */
    public Loadout<T> with(int slot, T item) {
        check(slot);
        if (reserved(slot) && item != null)
            throw new IllegalArgumentException("Slot " + slot + " is reserved for the Exit testing item.");
        var copy = new ArrayList<>(items);
        copy.set(slot, item);
        return new Loadout<>(copy);
    }

    /** A copy built from another set of items, dropping what the reserved slot held. */
    public static <T> Loadout<T> of(List<T> source) {
        if (source.size() != SIZE) throw new IllegalArgumentException("A loadout has " + SIZE + " slots, not " + source.size() + ".");
        var copy = new ArrayList<>(source);
        copy.set(EXIT_SLOT, null);
        return new Loadout<>(copy);
    }

    public <R> Loadout<R> map(Function<T, R> f) {
        var out = new ArrayList<R>(SIZE);
        for (T t : items) out.add(t == null ? null : f.apply(t));
        return new Loadout<>(out);
    }

    public List<T> items() { return items; }

    public int count() {
        int n = 0;
        for (T t : items) if (t != null) n++;
        return n;
    }

    public boolean isEmpty() { return count() == 0; }

    private static void check(int slot) {
        if (!valid(slot)) throw new IllegalArgumentException("No loadout slot " + slot + " (0-" + (SIZE - 1) + ").");
    }

    @Override public boolean equals(Object o) { return o instanceof Loadout<?> l && items.equals(l.items); }
    @Override public int hashCode() { return items.hashCode(); }
}
