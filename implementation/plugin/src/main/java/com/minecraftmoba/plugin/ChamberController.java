package com.minecraftmoba.plugin;

import java.util.List;

/**
 * What a hotbar item means, and whether it may be used right now.
 *
 * Pure: the Bukkit layer asks {@link #intent} what an item is for and carries
 * it out, so the mapping from menu ids to authoring verbs is tested rather than
 * discovered by pressing buttons.
 *
 * <h2>Why undo and redo stay visible</h2>
 *
 * Items that appear and disappear as history changes break the muscle memory a
 * fixed layout exists to give, and a silent dead button's obvious response is to
 * press it again. So {@link #refusal} greys them with a reason.
 */
public final class ChamberController {
    private ChamberController() {}

    /** The facts a refusal depends on. */
    public record State(boolean inBay, int undoDepth, boolean pending, boolean sourceChosen,
                        boolean clockPaused, boolean hungerFrozen, boolean regenOff) {}

    public enum Kind { AUTHOR, SHOW_AGAIN, CONFIRM, CANCEL, REGENERATE_LAST, LAB, UNAVAILABLE }

    /** What to do. {@code args} are LabAuthoring arguments after "lab author". */
    public record Intent(Kind kind, List<String> args) {
        static Intent author(String... args) { return new Intent(Kind.AUTHOR, List.of(args)); }
        static Intent of(Kind kind) { return new Intent(kind, List.of()); }
        /** A {@code /moba lab ...} command, for the clock and rules. */
        static Intent lab(String... args) { return new Intent(Kind.LAB, List.of(args)); }
    }

    /**
     * The menu says Rampart; the authoring templates call it bastion. The
     * template is what exists, so the id is translated here and the mismatch is
     * stated in one place.
     */
    public static final String RAMPART_TEMPLATE = "bastion";

    public static Intent intent(String itemId) {
        return switch (itemId) {
            case "fountain" -> Intent.author("preview", "fountain");
            case "outpost" -> Intent.author("preview", "outpost");
            case "rampart" -> Intent.author("preview", RAMPART_TEMPLATE);
            case "spike" -> Intent.author("preview", "spike");
            case "route.from" -> Intent.author("path", "from");
            case "route.to" -> Intent.author("path", "to");
            case "undo" -> Intent.author("undo");
            case "clock.dusk" -> Intent.lab("time", "dusk");
            case "clock.midnight" -> Intent.lab("time", "midnight");
            case "clock.dawn" -> Intent.lab("time", "dawn");
            case "clock.skip10" -> Intent.lab("time", "skip", "10");
            case "clock.pause" -> Intent.lab("time", "pause");
            case "clock.resume" -> Intent.lab("time", "resume");
            // The ARGUMENT depends on the current setting, so it is filled in by the caller.
            case "rules.hunger", "rules.regen" -> Intent.lab("rules");
            case "redo" -> Intent.of(Kind.UNAVAILABLE);
            case "preview" -> Intent.of(Kind.SHOW_AGAIN);
            case "confirm" -> Intent.of(Kind.CONFIRM);
            case "cancel" -> Intent.of(Kind.CANCEL);
            case "regenerate" -> Intent.of(Kind.REGENERATE_LAST);
            default -> {
                if (itemId.startsWith("fauna.")) yield Intent.author("fauna", itemId.substring(6));
                if (itemId.startsWith("swarm.")) yield Intent.author("swarm", itemId.substring(6));
                yield Intent.of(Kind.UNAVAILABLE);
            }
        };
    }

    /** Null when the item may be used; otherwise why not. */
    public static String refusal(String itemId, State s) {
        if (!s.inBay()) return "Walk through the door into your bay first.";
        return switch (itemId) {
            case "undo" -> s.undoDepth() == 0 ? "Nothing to undo in this bay." : null;
            // LabUndo is one-way: undoing discards. Saying so is more useful than
            // a button that promises a verb nothing implements.
            case "redo" -> "No forward journal exists yet; undo discards.";
            case "regenerate" -> s.sourceChosen() ? null
                    : "Choose a certified scoop or a random seed at the platform first.";
            case "confirm", "preview", "cancel" -> s.pending() ? null : "Nothing is pending.";
            case "clock.pause" -> s.clockPaused() ? "The clock is already paused." : null;
            case "clock.resume" -> s.clockPaused() ? null : "The clock is already running.";
            default -> null;
        };
    }

    public static ChamberMenu.Gate gate(State s) { return id -> refusal(id, s); }
}
