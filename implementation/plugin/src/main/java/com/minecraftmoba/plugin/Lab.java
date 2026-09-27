package com.minecraftmoba.plugin;

import org.bukkit.GameMode;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Locale;

/**
 * One command that puts a single administrator on a map, playing, immediately.
 *
 * <h2>The problem this exists for</h2>
 *
 * Every piece of a debug session already existed, and the sequence that
 * assembles them is neither discoverable nor forgiving. Running
 * {@code match open}, {@code select}, {@code add}, {@code start} -- the
 * obvious order, and the wrong one -- does this:
 *
 * <ul>
 *   <li>{@code add} at PRE_MATCH records a participant and does <b>not</b>
 *       spawn them, because {@link Match#add} only spawns when the match is
 *       already RUNNING;</li>
 *   <li>{@code start} then calls {@code beginPreMatch}, which opens the class
 *       draft and sends everyone into the colosseum via
 *       {@code DraftHallView.enter};</li>
 *   <li>and {@code DraftHallView.release} clears the ghost state but
 *       <b>never teleports anybody back</b> -- the hall is left by walking out
 *       of it, which a solo tester with no draft to finish cannot do.</li>
 * </ul>
 *
 * The result is a stranded administrator in a hall waiting on picks from
 * thirteen people who do not exist. That is not a bug in any one of those
 * three; each is right for a real match. It is the absence of a path that
 * does not go through a real match.
 *
 * <h2>What it does, and the one ordering that matters</h2>
 *
 * The draft is not "skipped" by suppressing anything inside it -- it is never
 * entered, because {@link Match#startForTest} goes from IDLE or PRE_MATCH
 * straight to RUNNING without passing through CLASS_SELECT.
 *
 * <b>Start first, then add.</b> {@code add} spawns a player only when the
 * state is already RUNNING, so adding before starting is exactly the silent
 * failure described above. Written down because the natural reading of the two
 * verbs is the opposite order.
 *
 * <h2>Why it adds a participant at all</h2>
 *
 * {@code start-test} alone would run the match. But {@link Match#reset} clears
 * player state by iterating <i>participants</i>, so a tester who is not one
 * gets the world reset underneath them while keeping the level, class,
 * inventory and Task modifiers the session produced. Being a participant is
 * what makes {@code end} total.
 *
 * A single participant cannot end the match by accident: {@code victorOf}
 * skips any team with no participants, so the empty opposing side can never
 * satisfy the victory predicate.
 *
 * <h2>Deliberately not a separate mode</h2>
 *
 * Nothing here relaxes a rule. The slot guard, Capacity scaling, combat
 * gating, Work Points and the three clocks all behave exactly as they do in a
 * match, because the session <i>is</i> a match -- one with a roster of one and
 * no pre-match process. A debug path that disabled any of those would be
 * testing a different game.
 */
public final class Lab {

    private final MobaPlugin plugin;

    public Lab(MobaPlugin plugin) { this.plugin = plugin; }

    public boolean command(CommandSender sender, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("/moba lab needs an in-game player.");
            return true;
        }
        String verb = args.length > 1 ? args[1].toLowerCase(Locale.ROOT) : "start";
        try {
            switch (verb) {
                case "start" -> start(p, args.length > 2 ? args[2] : null);
                case "end", "stop" -> end(p);
                case "status" -> plugin.match().report().forEach(p::sendMessage);
                default -> {
                    p.sendMessage("/moba lab [start [<map>] | end | status]");
                    return true;
                }
            }
        } catch (RuntimeException ex) {
            p.sendMessage("Lab refused: " + ex.getMessage());
        } catch (java.io.IOException ex) {
            // The world copy is the one step that touches the filesystem, and
            // a half-replaced world is worse than a refused command --
            // WorldInstance already fails loudly rather than leaving one. Say
            // so here too instead of reporting a generic refusal.
            p.sendMessage("Lab failed during world restore: " + ex.getMessage());
            plugin.getLogger().warning("lab world restore failed: " + ex);
        }
        return true;
    }

    /**
     * Open a map, start playing on it, and stand on it -- in one call.
     *
     * Idempotent where it can be: a lab session already running just re-adds
     * and re-spawns the caller, which is also how someone who wandered off,
     * died oddly or logged back in gets put back.
     */
    private void start(Player p, String mapId) throws java.io.IOException {
        Match match = plugin.match();

        // Someone re-running `lab start` after being stranded is the expected
        // case, not an edge one. Leave the hall before touching match state,
        // since the ghost state outlives the draft that set it.
        plugin.draftHall().release();

        if (!plugin.enrolled(p)) {
            p.performCommand("moba join");
            if (!plugin.enrolled(p))
                throw new IllegalStateException(
                        "could not enroll -- empty your offhand and cursor, then retry.");
        }

        if (match.state() != Match.State.RUNNING) {
            // ENDED refuses `open`, and a lab session that ended on a
            // victory or a manual end has to clear before it can run again.
            if (match.state() == Match.State.ENDED) match.reset();
            if (match.state() == Match.State.IDLE) match.open();
            match.resolveSelectionForTest(mapId);
            match.startForTest();
        }

        // AFTER the start, never before: `add` spawns only at RUNNING.
        match.add(p, Team.NORTH);
        p.setGameMode(GameMode.SURVIVAL);

        var claimed = plugin.worldInstance().claimed();
        p.sendMessage("Lab running on " + (claimed == null ? "the template" : claimed.mapId())
                + " as north. Class and level are yours: /moba setclass <id>, "
                + "/moba setlevel <n>, /moba xp <n>. Finish with /moba lab end.");
        if (claimed != null) p.sendMessage("NOTE: this consumes a pool entry on end. "
                + "Set pool.enabled false to run the lab on the reusable template.");
    }

    /**
     * End the session and put everything back.
     *
     * Delegates entirely to {@link Match#reset}, which already replaces the
     * world from the template rather than rolling blocks back, and clears
     * progression, inventory, vanilla XP, potion effects, Task modifiers,
     * worksites, routes, Infra Mode, contributions and renewable bindings.
     * Nothing about a lab session needs a second, weaker teardown beside it.
     */
    private void end(Player p) throws java.io.IOException {
        plugin.draftHall().release();
        p.sendMessage(plugin.match().reset());
        p.sendMessage("Lab ended. World, progression and inventory are back to the template.");
    }
}
