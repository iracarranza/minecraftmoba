package com.minecraftmoba.plugin;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The Bukkit half of the chamber UI: allot a bay, show a placement, commit it.
 *
 * Deliberately thin. Every rule it enforces lives in {@link Chamber},
 * {@link Chambers} or {@link ChamberSession}, all of which are tested without
 * a server; this class converts between those and Bukkit, and does the two
 * things that genuinely need a world -- drawing particles and writing blocks.
 *
 * <h2>Confirmation reuses the ability gesture</h2>
 *
 * {@link AimState} under {@link CastMode#DOUBLE} already means "first press
 * previews, same input confirms, another cancels". A chamber placement asks
 * the identical question, so it asks it the same way rather than teaching the
 * tester a second grammar for the same decision.
 *
 * [OPEN] Scoop SOURCE. Regenerating a chamber's terrain has two meanings that
 * must not be confused: copying a pre-staged certified scoop, which the lab
 * already does through {@link LabMaps}, and generating rough vanilla terrain
 * for severity testing, which no certified pipeline produces and which the
 * offline compiler cannot be asked for at runtime. Both are legitimate and
 * they answer different questions, so they want different names and a
 * measurement taken in one must never be reported as the other.
 */
public final class ChamberWorkspace implements Listener {

    private final MobaPlugin plugin;
    private final Chambers chambers;
    private final Map<UUID, ChamberSession> sessions = new HashMap<>();

    public ChamberWorkspace(MobaPlugin plugin, World world, int radius, int height, int floor) {
        this.plugin = plugin;
        this.chambers = new Chambers(world.getUID(), radius, height, floor);
    }

    public Chambers chambers() { return chambers; }

    public ChamberSession session(Player p) {
        return sessions.computeIfAbsent(p.getUniqueId(), k -> new ChamberSession());
    }

    // ---- conversion --------------------------------------------------------

    public static Chamber.At at(Block block) {
        return new Chamber.At(block.getWorld().getUID(), block.getX(), block.getY(), block.getZ());
    }

    public static Chamber.At at(Location where) {
        return new Chamber.At(where.getWorld().getUID(),
                where.getBlockX(), where.getBlockY(), where.getBlockZ());
    }

    public Location location(Chamber.At at) {
        World w = plugin.getServer().getWorld(at.world());
        return w == null ? null : new Location(w, at.x() + 0.5, at.y() + 0.5, at.z() + 0.5);
    }

    // ---- the workspace -----------------------------------------------------

    /** Give a tester a bay and put them in it. */
    public Chamber enter(Player p) {
        Chamber c = chambers.allot(p.getUniqueId());
        Location centre = location(new Chamber.At(c.world(),
                (c.minX() + c.maxX()) / 2, c.minY(), (c.minZ() + c.maxZ()) / 2));
        if (centre != null) p.teleport(centre);
        return c;
    }

    /** Hand the bay back, discarding anything pending. */
    public void leave(Player p) {
        sessions.remove(p.getUniqueId());
        chambers.release(p.getUniqueId());
    }

    /**
     * Offer a placement for confirmation, with its verdict already decided.
     *
     * The containment check happens HERE rather than at commit, because the
     * preview is the thing the tester is reading: a placement that leaves the
     * chamber should say so while it is still a proposal.
     */
    public ChamberSession.Pending propose(Player p, String description,
                                          List<Chamber.At> affected, List<String> faults) {
        Chamber c = chambers.of(p.getUniqueId());
        boolean escapes = c == null || !c.encloses(affected);
        var pending = new ChamberSession.Pending(description, affected, faults, escapes);
        session(p).offer(pending, AimState.begin("chamber." + description,
                AbilityInputs.Input.RIGHT_CLICK, CastMode.DOUBLE,
                plugin.getServer().getCurrentTick()));
        notice(p, pending);
        return pending;
    }

    /**
     * Draw the pending placement to its tester alone.
     *
     * Reduced before it reaches {@link TargetPreview}, because that sends one
     * particle per block per cadence to one player and a long route would be a
     * fog rather than a preview. See {@link ChamberPreview}.
     */
    public void show(Player p, List<String> characters) {
        var pending = session(p).pending();
        if (pending == null) return;
        List<Chamber.At> drawn = characters == null || characters.isEmpty()
                ? ChamberPreview.outline(pending.affected())
                : ChamberPreview.route(pending.affected(), characters);
        drawn = ChamberPreview.withinBudget(drawn, ChamberPreview.DEFAULT_BUDGET);

        var blocks = new ArrayList<Block>(drawn.size());
        for (Chamber.At a : drawn) {
            Location where = location(a);
            if (where != null) blocks.add(where.getBlock());
        }
        if (plugin.targetPreview() != null) plugin.targetPreview().draw(p, blocks);
    }

    /**
     * Resolve the tester's input into an outcome, and act on it.
     *
     * The write itself is the caller's: a chamber does not know how to build a
     * route or a structure, only whether this one may be built. Returning the
     * outcome rather than performing the edit keeps that boundary.
     */
    public ChamberSession.Outcome resolve(Player p, AimState.Decision decision) {
        var session = session(p);
        var pending = session.pending();
        var outcome = session.resolve(decision);
        if (outcome == ChamberSession.Outcome.REFUSED && pending != null)
            notice(p, pending);
        if (outcome == ChamberSession.Outcome.CANCELLED && plugin.hudNotice() != null)
            plugin.hudNotice().show(p, "CANCELLED");
        return outcome;
    }

    private void notice(Player p, ChamberSession.Pending pending) {
        if (plugin.hudNotice() != null) plugin.hudNotice().show(p, pending.verdict());
    }

    @EventHandler public void quit(PlayerQuitEvent e) { leave(e.getPlayer()); }

    /** Drop every bay, for a lab reset. */
    public void reset() { sessions.clear(); chambers.clear(); }

    public String report(Player p) {
        Chamber c = chambers.of(p.getUniqueId());
        var pending = session(p).pending();
        return "CHAMBER bay=" + (c == null ? "none" : c.minX() + "," + c.minZ())
                + " allotted=" + chambers.size()
                + " pending=" + (pending == null ? "none" : pending.description()
                                                 + " (" + pending.verdict() + ")");
    }
}
