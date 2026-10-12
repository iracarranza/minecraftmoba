package com.minecraftmoba.plugin;

/**
 * The loadout editor's 54-slot window, as data. Pure.
 *
 * <pre>
 * rows 0-3   the 36 inventory slots (the hotbar first, then the rest); slot 8 is locked
 * row 4      helmet, chestplate, leggings, boots, then spacers
 * row 5      select, fill from me, fill from the dummy, clear, the item shelf, spacers, done
 * </pre>
 *
 * Because the loadout's own slot numbering (see {@link Loadout}) runs 0-39 in exactly this order, an
 * editor slot below 40 IS the loadout slot, with no translation between them.
 */
public final class LoadoutEditorLayout {
    private LoadoutEditorLayout() {}

    public static final int SIZE = 54;
    public static final int SELECT = 45, FILL_ME = 46, FILL_DUMMY = 47, CLEAR = 48, ITEMS = 49, DONE = 53;

    public enum Kind { ITEM, RESERVED, CONTROL, SPACER }

    public static Kind kind(int slot) {
        if (slot < 0 || slot >= SIZE) throw new IllegalArgumentException("No editor slot " + slot);
        if (Loadout.reserved(slot)) return Kind.RESERVED;
        if (slot < Loadout.SIZE) return Kind.ITEM;
        return switch (slot) {
            case SELECT, FILL_ME, FILL_DUMMY, CLEAR, ITEMS, DONE -> Kind.CONTROL;
            default -> Kind.SPACER;
        };
    }

    /** Whether the tester may put an item in, or take one out of, this slot. */
    public static boolean editable(int slot) { return kind(slot) == Kind.ITEM; }

    /** The label of a slot that holds armor, for the empty-slot hint. */
    public static String armorLabel(int slot) {
        return switch (slot) {
            case Loadout.HELMET -> "Helmet";
            case Loadout.CHESTPLATE -> "Chestplate";
            case Loadout.LEGGINGS -> "Leggings";
            case Loadout.BOOTS -> "Boots";
            default -> null;
        };
    }
}
