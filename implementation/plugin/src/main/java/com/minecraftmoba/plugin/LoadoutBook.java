package com.minecraftmoba.plugin;

import java.util.ArrayList;
import java.util.List;

/**
 * A tester's saved loadouts: a few named slots, one of them selected. Pure and generic.
 *
 * The selection is what "Start testing" and "Give to the dummy" use, so choosing a loadout and
 * applying it are separate steps and a loadout is never applied by accident while it is being
 * edited.
 */
public final class LoadoutBook<T> {
    public static final int SLOTS = 5;
    private static final String NAMES = "ABCDE";

    private final List<Loadout<T>> loadouts = new ArrayList<>();
    private int selected;

    public LoadoutBook() { for (int i = 0; i < SLOTS; i++) loadouts.add(Loadout.empty()); }

    public static String name(int index) {
        check(index);
        return "Loadout " + NAMES.charAt(index);
    }

    public int selected() { return selected; }

    public Loadout<T> current() { return loadouts.get(selected); }

    public Loadout<T> get(int index) {
        check(index);
        return loadouts.get(index);
    }

    public void set(int index, Loadout<T> loadout) {
        check(index);
        loadouts.set(index, loadout == null ? Loadout.empty() : loadout);
    }

    public void select(int index) {
        check(index);
        selected = index;
    }

    /** Move to the next loadout, wrapping, and return its index. */
    public int next() {
        selected = (selected + 1) % SLOTS;
        return selected;
    }

    public String selectedName() { return name(selected); }

    private static void check(int index) {
        if (index < 0 || index >= SLOTS) throw new IllegalArgumentException("No loadout " + index + " (0-" + (SLOTS - 1) + ").");
    }
}
