package com.minecraftmoba.plugin;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * The six control statuses behind one door, so a bench or a scenario can apply, read and clear
 * any of them without knowing which class owns which.
 *
 * Stunned and Rooted are the existing {@link Stun} and {@link ToolboxStatuses}; Lured and
 * Frightened are {@link ForcedMovement}. Blinded and Weakened are Minecraft's own Blindness and
 * Weakness, at the strength config names (default level I): the design says their strengths are
 * the platform's, so there is nothing to invent here.
 *
 * <h2>What Weakened does and does not reach</h2>
 *
 * Native Weakness lowers MELEE damage. Ability damage dealt through {@code Entity#damage} is not
 * a melee hit and is not lowered by it, so a Weakened caster's abilities are not weakened. That
 * is a gap between "offensive effectiveness is reduced" and what the platform provides, reported
 * by the bench and not worked around.
 */
public final class Statuses {
    private Statuses() {}

    public enum Kind {
        STUNNED("Stunned"), ROOTED("Rooted"), BLINDED("Blinded"), WEAKENED("Weakened"),
        LURED("Lured"), FRIGHTENED("Frightened");

        private final String label;
        Kind(String label) { this.label = label; }
        public String label() { return label; }

        public Kind next() { var v = values(); return v[(ordinal() + 1) % v.length]; }

        /** Whether the kind needs a source to be lured to or frightened from. */
        public boolean needsSource() { return this == LURED || this == FRIGHTENED; }

        public static Kind of(String name) {
            if (name == null) return null;
            for (Kind k : values()) if (k.name().equalsIgnoreCase(name) || k.label.equalsIgnoreCase(name)) return k;
            return null;
        }
    }

    public record Active(Kind kind, long remaining) {}

    public static final int DEFAULT_AMPLIFIER = 0;

    public static void apply(MobaPlugin plugin, Kind kind, LivingEntity target, long ticks, Supplier<Location> source) {
        if (target == null || ticks <= 0) return;
        switch (kind) {
            case STUNNED -> plugin.stun().stun(target, ticks);
            case ROOTED -> plugin.toolboxStatuses().root(target, ticks);
            case BLINDED -> target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, (int) ticks,
                    plugin.getConfig().getInt("statuses.blinded.amplifier", DEFAULT_AMPLIFIER), false, true, true));
            case WEAKENED -> target.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, (int) ticks,
                    plugin.getConfig().getInt("statuses.weakened.amplifier", DEFAULT_AMPLIFIER), false, true, true));
            case LURED -> plugin.forced().apply(ForcedMotion.Kind.LURED, target, source, ticks);
            case FRIGHTENED -> plugin.forced().apply(ForcedMotion.Kind.FRIGHTENED, target, source, ticks);
        }
    }

    public static boolean has(MobaPlugin plugin, Kind kind, Entity e) { return remaining(plugin, kind, e) > 0; }

    public static long remaining(MobaPlugin plugin, Kind kind, Entity e) {
        if (e == null) return 0;
        return switch (kind) {
            case STUNNED -> plugin.stun().remaining(e);
            case ROOTED -> plugin.toolboxStatuses().rootRemaining(e);
            case BLINDED -> effectLeft(e, PotionEffectType.BLINDNESS);
            case WEAKENED -> effectLeft(e, PotionEffectType.WEAKNESS);
            case LURED -> plugin.forced().kind(e) == ForcedMotion.Kind.LURED ? plugin.forced().remaining(e) : 0;
            case FRIGHTENED -> plugin.forced().kind(e) == ForcedMotion.Kind.FRIGHTENED ? plugin.forced().remaining(e) : 0;
        };
    }

    private static long effectLeft(Entity e, PotionEffectType type) {
        if (!(e instanceof LivingEntity l)) return 0;
        var effect = l.getPotionEffect(type);
        return effect == null ? 0 : Math.max(0, effect.getDuration());
    }

    public static List<Active> active(MobaPlugin plugin, Entity e) {
        var out = new ArrayList<Active>();
        for (Kind k : Kind.values()) {
            long left = remaining(plugin, k, e);
            if (left > 0) out.add(new Active(k, left));
        }
        return out;
    }

    public static void clear(MobaPlugin plugin, Entity e) {
        if (e == null) return;
        plugin.stun().clear(e);
        plugin.toolboxStatuses().unroot(e);
        plugin.forced().clear(e);
        if (e instanceof LivingEntity l) {
            l.removePotionEffect(PotionEffectType.BLINDNESS);
            l.removePotionEffect(PotionEffectType.WEAKNESS);
        }
    }
}
