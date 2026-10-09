package com.minecraftmoba.plugin;

import java.util.ArrayList;
import java.util.List;

/**
 * How the dummy is told to act, and which of those choices make sense for an output.
 *
 * Two independent layers, so neither has to carry the other: a SCRIPT (what the
 * dummy does and when) and an AIM policy (where it points). Which are offered
 * depends on the output's declared forms, so a self-targeted ability is never asked
 * where to aim and a held ability is asked how long to hold.
 */
public final class CombatBehavior {
    private CombatBehavior() {}

    public enum Aim {
        AT_PLAYER("At the player"),
        STRAIGHT_AHEAD("Straight ahead"),
        RANDOM("Random direction"),
        MARKED_POINT("At a marked point"),
        AS_RECORDED("As recorded");
        public final String label;
        Aim(String label) { this.label = label; }
    }

    public enum Script {
        ONCE("Cast once"),
        REPEAT("Cast every few seconds");
        public final String label;
        Script(String label) { this.label = label; }
    }

    public record Choice<T>(T value, boolean available, String reason) {}

    /**
     * Whether any recorded take exists to replay. False until the ghost does:
     * offering AS_RECORDED with nothing behind it would be a button that does
     * nothing.
     */
    public static final boolean RECORDINGS_EXIST = false;

    /** Aim policies for an output: none at all for SELF, which aims at nothing. */
    public static List<Choice<Aim>> aims(AbilityOutput out) {
        var result = new ArrayList<Choice<Aim>>();
        if (out.target() == TargetForm.SELF) return result;
        if (!out.target().supported())
            return List.of(new Choice<>(null, false, out.target() + " targeting is not supported."));
        boolean pointable = out.target() == TargetForm.AREA || out.target() == TargetForm.DIRECTION;
        result.add(new Choice<>(Aim.AT_PLAYER, true, null));
        result.add(new Choice<>(Aim.STRAIGHT_AHEAD, true, null));
        result.add(new Choice<>(Aim.RANDOM, true, null));
        result.add(pointable ? new Choice<>(Aim.MARKED_POINT, true, null)
                : new Choice<>(Aim.MARKED_POINT, false, "A unit target is a creature, not a point."));
        result.add(RECORDINGS_EXIST ? new Choice<>(Aim.AS_RECORDED, true, null)
                : new Choice<>(Aim.AS_RECORDED, false, "No take has been recorded."));
        return result;
    }

    public static List<Choice<Script>> scripts(AbilityOutput out) {
        if (out.input() == InputForm.PASSIVE)
            return List.of(new Choice<>(null, false, "A passive has no gesture to script."));
        return List.of(new Choice<>(Script.ONCE, true, null), new Choice<>(Script.REPEAT, true, null));
    }

    /** Whether the output is held, so the tester should be asked for a duration. */
    public static boolean needsHoldDuration(AbilityOutput out) {
        return out.input() == InputForm.CHANNELED || out.input() == InputForm.CHARGED;
    }

    /** Whether any aim choice is needed for this output. */
    public static boolean needsAim(AbilityOutput out) { return out.target() != TargetForm.SELF; }
}
