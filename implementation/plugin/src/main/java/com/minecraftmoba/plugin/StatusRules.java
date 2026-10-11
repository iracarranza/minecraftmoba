package com.minecraftmoba.plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * What each control status is DEFINED to do, as data, and a comparison with what the plugin does.
 *
 * The definitions are the owner's: Stunned cannot move or act; Rooted cannot move but can act;
 * Lured and Frightened force movement (toward or away from a source) and the camera, forbid sprint,
 * crouch and abilities, and allow jump and attack. Blinded and Weakened are the platform's own effects and
 * constrain nothing here, so they appear with no rule beyond "the effect is present".
 *
 * Keeping the definitions in one table lets the bench print, for the status a tester has just
 * applied, each rule next to what the server actually answers, and lets a test fail the day the
 * two disagree.
 *
 * Pure: the observations come in as a function.
 */
public final class StatusRules {
    private StatusRules() {}

    /** The things a status can allow or refuse. */
    public enum Capability {
        MOVE("move on its own"), ACT("use, place, break, drop, consume"), ABILITIES("use abilities"),
        SPRINT("sprint"), CROUCH("crouch"), JUMP("jump"), ATTACK("attack"), LOOK("turn its camera");

        private final String label;
        Capability(String label) { this.label = label; }
        public String label() { return label; }
    }

    /**
     * What a status is defined to refuse; every other capability stays allowed.
     *
     * Jump and sprint go with movement for Stunned and Rooted because the plugin holds the
     * position, vertical included (see {@link Stun}); a Rooted player cannot jump. Lured and
     * Frightened leave jump and attack alone, which is the owner's definition.
     */
    private static final Map<Statuses.Kind, List<Capability>> REFUSES = Map.of(
            Statuses.Kind.STUNNED, List.of(Capability.MOVE, Capability.JUMP, Capability.SPRINT, Capability.ACT,
                    Capability.ABILITIES, Capability.ATTACK),
            Statuses.Kind.ROOTED, List.of(Capability.MOVE, Capability.JUMP, Capability.SPRINT),
            Statuses.Kind.BLINDED, List.of(),
            Statuses.Kind.WEAKENED, List.of(),
            Statuses.Kind.LURED, List.of(Capability.MOVE, Capability.ABILITIES, Capability.SPRINT, Capability.CROUCH, Capability.LOOK),
            Statuses.Kind.FRIGHTENED, List.of(Capability.MOVE, Capability.ABILITIES, Capability.SPRINT, Capability.CROUCH, Capability.LOOK));

    public static List<Capability> refused(Statuses.Kind k) { return REFUSES.get(k); }

    /** What the definition lets the entity still do. */
    public static List<Capability> allowed(Statuses.Kind k) {
        var out = new ArrayList<Capability>();
        for (Capability c : Capability.values()) if (!refused(k).contains(c)) out.add(c);
        return out;
    }

    /** Whether the definition says a status moves its subject (toward or away from a source). */
    public static boolean forcesMovement(Statuses.Kind k) { return k == Statuses.Kind.LURED || k == Statuses.Kind.FRIGHTENED; }

    /** One line of the checklist. */
    public record Line(Capability capability, boolean definedRefused, Boolean observedRefused) {
        /** True when the server answers differently from the definition. A null observation is not a mismatch. */
        public boolean mismatch() { return observedRefused != null && observedRefused != definedRefused; }
    }

    /**
     * The checklist for a status: every capability, what the definition says, what is observed.
     *
     * @param observe the server's answer for a capability (true = refused), or null when it cannot
     *                be asked without a client (jump, attack and look are not queryable)
     */
    public static List<Line> checklist(Statuses.Kind k, Function<Capability, Boolean> observe) {
        var out = new ArrayList<Line>();
        for (Capability c : Capability.values())
            out.add(new Line(c, refused(k).contains(c), observe.apply(c)));
        return out;
    }

    public static List<Line> mismatches(List<Line> lines) { return lines.stream().filter(Line::mismatch).toList(); }
}
