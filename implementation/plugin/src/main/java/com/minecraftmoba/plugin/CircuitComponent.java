package com.minecraftmoba.plugin;

import org.bukkit.Material;

import java.util.Map;

/**
 * Toolbox's component dictionary: which items are program, and what each means.
 *
 * <h2>Why an enum rather than Materials everywhere</h2>
 *
 * The grammar distinguishes three populations that {@link Material} does not:
 * items that are <b>wiring</b>, items that are <b>instructions</b>, and items
 * that are <b>neither</b> and therefore end the program. Reading the circuit
 * with raw Materials would rebuild that distinction at every call site, and
 * the third case is the one that matters most -- it is the magazine boundary,
 * and it is decided by absence rather than by a rule.
 *
 * <h2>The dictionary is deliberately small</h2>
 *
 * A component does not need a Utility Belt effect to earn its place; several
 * are useful only through Dropper or through ordinary world Redstone. Those
 * are {@link #INERT}: still program, still consuming a cell and an interval,
 * with no effect of their own. Keeping them in the dictionary rather than
 * treating them as non-circuit items is deliberate -- a Lamp in the middle of
 * a board is a deliberate pause, not the end of the program.
 */
public enum CircuitComponent {

    /**
     * Wiring, or a defensive discharge.
     *
     * The only component with two roles, and the boundary between them is
     * positional rather than declared: Dust <i>traversed</i> between two
     * components is wiring and costs an item but no time; Dust <i>resolved</i>
     * as an instruction discharges the stack for mitigation. See
     * {@link CircuitReader} for where that decision is made -- it cannot be
     * made here, because it depends on what is on either side.
     */
    DUST(Material.REDSTONE),

    /** Illuminate the current origin; apply Illuminated. */
    TORCH(Material.REDSTONE_TORCH),

    /** Directional impulse in Toolbox's current facing, including dropped items. */
    PISTON(Material.PISTON),

    /** Pull opposite current facing, and Root for 10 ticks. */
    STICKY_PISTON(Material.STICKY_PISTON),

    /**
     * Delays the next component one interval, and transmits without dust after it.
     *
     * The only component besides Dust that can carry flow horizontally with no
     * wiring cell following, which is why it shifts parity as well as timing.
     */
    REPEATER(Material.REPEATER),

    /** Convert damage magnitude into amplification for the next component. */
    COMPARATOR(Material.COMPARATOR),

    /** Activate exactly one slot directly beneath, one tick after the interval. */
    TRIPWIRE_HOOK(Material.TRIPWIRE_HOOK),

    /** Acquire, apply Observed, move the projected origin. */
    OBSERVER(Material.OBSERVER),

    /** Take the first item; vanilla Dispenser behaviour, else eject. */
    DISPENSER(Material.DISPENSER),

    /** Take the first item and throw it; full blocks settle into terrain. */
    DROPPER(Material.DROPPER),

    /** Vacuum nearby dropped items into inventory. */
    HOPPER(Material.HOPPER),

    /** Play a note. */
    NOTE_BLOCK(Material.NOTE_BLOCK),

    /**
     * Program with no Utility Belt effect of its own.
     *
     * Lamp, Target, Redstone Block, Daylight Detector, plates, buttons and
     * levers. They still occupy a cell and still cost an interval, so they are
     * usable as deliberate delay -- which is why they are program rather than
     * magazine.
     */
    INERT(Material.REDSTONE_LAMP, Material.TARGET, Material.REDSTONE_BLOCK,
          Material.DAYLIGHT_DETECTOR, Material.LEVER, Material.STONE_BUTTON,
          Material.STONE_PRESSURE_PLATE);

    private final Material[] materials;

    CircuitComponent(Material... materials) { this.materials = materials; }

    private static final Map<Material, CircuitComponent> BY_MATERIAL = build();

    private static Map<Material, CircuitComponent> build() {
        var map = new java.util.EnumMap<Material, CircuitComponent>(Material.class);
        for (CircuitComponent c : values())
            for (Material m : c.materials) {
                var clash = map.put(m, c);
                if (clash != null)
                    throw new IllegalStateException(m + " is claimed by both " + clash + " and " + c);
            }
        return java.util.Collections.unmodifiableMap(map);
    }

    /** The component an item is, or null when it is not program at all. */
    public static CircuitComponent of(Material material) {
        return material == null ? null : BY_MATERIAL.get(material);
    }

    /** Whether this item is program. Air is not, which is what ends a circuit. */
    public static boolean isProgram(Material material) { return of(material) != null; }

    /**
     * Dust alone, and only as a question about the item rather than its role.
     *
     * Whether a particular Dust is wiring or an instruction is positional and
     * belongs to {@link CircuitReader}.
     */
    public boolean isWiring() { return this == DUST; }
}
