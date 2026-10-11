package com.minecraftmoba.plugin;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

/**
 * Makes a body cast a real ability, by the same door a player's own input uses.
 *
 * The body has no keyboard or mouse, so it is given what those would have given:
 * a facing, the mode toggle, and the slot's input. Everything after that is
 * {@link AbilityInputs#input}, so the cast obeys cooldowns, Stun, unlock levels
 * and the ability's own rules exactly as a player's does. A refusal is reported
 * rather than hidden, because a dummy that silently did not cast reads as an
 * ability that did nothing.
 *
 * A held ability (charged or channelled with input kept down) is kept down by
 * repeating the input each tick while it is sustained, which is what a held key
 * does on the wire.
 */
public final class BodyCaster {
    /** What came of a command to cast, and what the click it sent was on (see {@link ClickResolver}). */
    public record Result(boolean cast, String note, ClickResolver.Click click) {
        public Result(boolean cast, String note) { this(cast, note, ClickResolver.Click.AIR); }
    }

    /** Whether a cast sends what a client's click would have carried, or a click on air. */
    public enum ClickMode { RESOLVE, AIR }

    private final MobaPlugin plugin;

    public BodyCaster(MobaPlugin plugin) { this.plugin = plugin; }

    /**
     * @param body     the caster
     * @param slot     "a1", "a2" or "ult"
     * @param facing   where to point first, or null to keep the current facing
     * @param holdTicks how long to keep a held input down; 0 for a tap
     */
    public Result cast(Player body, String slot, CastAim.Facing facing, int holdTicks) {
        return cast(body, slot, facing, holdTicks, ClickMode.RESOLVE);
    }

    /**
     * As {@link #cast(Player, String, CastAim.Facing, int)}, choosing what the click carries. RESOLVE is what a
     * player's own click would have sent: the block and face, or the creature, under the body's crosshair within
     * reach. AIR sends none of that, which is how every cast used to be sent.
     */
    public Result cast(Player body, String slot, CastAim.Facing facing, int holdTicks, ClickMode mode) {
        var inputs = plugin.inputs();
        var input = inputs.inputFor(slot);
        if (input == null) return new Result(false, "No input is bound to slot " + slot + ".");
        if (!plugin.enrolled(body)) return new Result(false, "The caster is not enrolled.");
        if (inputs.abilityFor(plugin.data(body).classId, slot) == null)
            return new Result(false, "That slot is not built for this class.");
        if (facing != null) body.setRotation(facing.yaw(), facing.pitch());
        // A running ability takes any input as its recast, before ability mode is
        // consulted, so opening the mode first would spend that input on the toggle.
        var ability = inputs.abilityFor(plugin.data(body).classId, slot);
        boolean wasActive = ability.active(body);
        if (!wasActive && !inputs.active(body)) inputs.input(body, inputs.modeInput());
        if (!wasActive && !inputs.active(body)) return new Result(false, "Ability mode would not open.");
        long before = inputs.executions(body, ability.id());
        var click = mode == ClickMode.RESOLVE ? ClickResolver.resolve(body) : ClickResolver.Click.AIR;
        var carries = ClickResolver.carries(input, click.kind());
        if (carries.block()) inputs.input(body, input, click.block(), click.face());
        else if (carries.entity()) inputs.input(body, input, null, null, click.entity());
        else inputs.input(body, input);
        // A refused cast (cooldown, stun, level) returns true from input() just
        // the same, so what actually ran is read from the execution count.
        // A second press on a running ability is its recast or cancel, which is a
        // state change rather than a new execution.
        boolean ran = inputs.executions(body, ability.id()) > before
                || inputs.aiming(body) != null || inputs.sustained(body) || ability.active(body) != wasActive;
        if (!ran) return new Result(false, "The cast was refused (cooldown, stun, level or the ability's own rule): level "
                + plugin.data(body).level + ", " + inputs.debug(body), click);
        if (holdTicks > 0) hold(body, input, holdTicks);
        return new Result(true, holdTicks > 0 ? "cast, held " + holdTicks + " ticks" : "cast", click);
    }

    private void hold(Player body, AbilityInputs.Input input, int ticks) {
        var self = new BukkitTask[1];
        int[] left = { ticks };
        self[0] = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!body.isOnline() || left[0]-- <= 0 || !plugin.inputs().sustained(body)) { self[0].cancel(); return; }
            plugin.inputs().input(body, input);
        }, 1L, 1L);
    }
}
