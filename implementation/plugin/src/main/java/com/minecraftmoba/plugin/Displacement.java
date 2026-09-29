package com.minecraftmoba.plugin;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockFormEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockGrowEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Temporary block displacement, and the reservation that makes it safe.
 *
 * Implements the rule in classes.md section 27:
 *
 * <blockquote>
 * Whenever an ability temporarily takes a block out of its original world
 * position <b>with the intention of restoring it</b>, that position remains
 * reserved until restoration. A reserved position cannot be occupied in a way
 * that would prevent the block returning.
 * </blockquote>
 *
 * <h2>Why this is shared rather than per ability</h2>
 *
 * Three abilities want it -- Paver's Concrete Shoes, Mole's Sinkhole and
 * Bloodmason's Athanor Anatomb. Built per class it would come out three
 * incompatible ways, and the failure mode is not a disagreement between them
 * but a hole in the map: whichever implementation forgets a restore path
 * leaves a position nothing can ever build on, outliving the ability with
 * nothing in the world to explain it.
 *
 * <h2>Taking a block drops nothing and updates nothing</h2>
 *
 * The block is cleared with physics suppressed, so neighbouring sand does not
 * fall into the hole and no item drops. Restoration is likewise
 * physics-suppressed, which is what makes "returns to its exact original state"
 * true and makes restore order irrelevant.
 *
 * <h2>Reservation is enforced against everything cheap to enforce it against</h2>
 *
 * Placement, fluid flow, falling blocks, pistons, and the form/spread/grow
 * family. Each guard takes an early exit when nothing is displaced at all,
 * which is almost the whole match -- so the cost of the system when unused is
 * one {@code isEmpty()} per event.
 *
 * <h2>[TECHNICAL RISK] Reservations live in memory</h2>
 *
 * A server stopped mid-displacement loses the held blocks and their
 * reservations both. That is survivable because displacement is
 * within-a-cast state and matches are ephemeral, but it is a real difference
 * from {@link Provenance}, which persists in the chunk PDC. Do not build a
 * displacement that is meant to outlast a cast on this.
 */
public final class Displacement implements Listener {

    private final MobaPlugin plugin;
    private final DisplacementLedger<BlockData> ledger = new DisplacementLedger<>();

    public Displacement(MobaPlugin plugin) { this.plugin = plugin; }

    private static DisplacementLedger.Key keyOf(Block b) {
        return new DisplacementLedger.Key(b.getWorld().getUID(), b.getX(), b.getY(), b.getZ());
    }

    /**
     * Take a block, reserving where it came from.
     *
     * @return the data taken, or null if the position was already reserved or
     *         there was nothing there to take.
     */
    public BlockData take(Block block, UUID token) {
        if (block == null || token == null) return null;
        if (block.getType().isAir()) return null;
        var key = keyOf(block);
        BlockData data = block.getBlockData().clone();
        // Provenance travels with the block: a player-placed block that is
        // borrowed and returned is still player-placed, and forgetting that
        // would quietly convert built material into a resource opportunity.
        boolean placed = plugin.provenance() != null && plugin.provenance().isPlayerPlaced(block);
        if (!ledger.reserve(key, token, data)) return null;
        block.setType(Material.AIR, false);
        if (placed) placedByPlayer.add(key);
        return data;
    }

    /** Positions whose block was player-placed before being taken. */
    private final java.util.Set<DisplacementLedger.Key> placedByPlayer = new java.util.HashSet<>();

    /** Put everything this token holds back, exactly. */
    public int restore(UUID token) {
        return put(ledger.release(token));
    }

    /** Put one position back early, leaving the token's others held. */
    public boolean restore(Block block) {
        var key = keyOf(block);
        BlockData data = ledger.release(key);
        if (data == null) return false;
        return put(List.of(Map.entry(key, data))) == 1;
    }

    private int put(List<Map.Entry<DisplacementLedger.Key, BlockData>> entries) {
        int done = 0;
        for (var e : entries) {
            var world = plugin.getServer().getWorld(e.getKey().world());
            // A world unloaded under a live displacement takes its blocks with
            // it. Drop the reservation rather than retrying forever.
            if (world == null) { placedByPlayer.remove(e.getKey()); continue; }
            Block at = world.getBlockAt(e.getKey().x(), e.getKey().y(), e.getKey().z());
            at.setBlockData(e.getValue(), false);
            if (placedByPlayer.remove(e.getKey()) && plugin.provenance() != null)
                plugin.provenance().mark(at, true);
            done++;
        }
        return done;
    }

    public boolean isReserved(Block b) { return !ledger.isEmpty() && ledger.isReserved(keyOf(b)); }

    public int reservedCount() { return ledger.size(); }

    public int heldBy(UUID token) { return ledger.heldBy(token); }

    /**
     * Put everything back, for a match reset.
     *
     * Restoring rather than discarding, because the alternative is a map that
     * remembers an unfinished cast from the previous match.
     */
    public void reset() {
        put(ledger.releaseAll());
        placedByPlayer.clear();
    }

    // ---- reservation guards ------------------------------------------------

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void place(BlockPlaceEvent e) {
        if (ledger.isEmpty()) return;
        if (ledger.isReserved(keyOf(e.getBlock()))) {
            e.setCancelled(true);
            if (plugin.hudNotice() != null) plugin.hudNotice().unavailable(e.getPlayer(), "RESERVED");
        }
    }

    /** Fluid flowing into a reserved position would occupy it. */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void flow(BlockFromToEvent e) {
        if (ledger.isEmpty()) return;
        if (ledger.isReserved(keyOf(e.getToBlock()))) e.setCancelled(true);
    }

    /** A falling block landing, or any entity changing a block. */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void entityChange(EntityChangeBlockEvent e) {
        if (ledger.isEmpty()) return;
        if (ledger.isReserved(keyOf(e.getBlock()))) e.setCancelled(true);
    }

    /**
     * A piston push is refused whole, not trimmed.
     *
     * Bukkit gives no way to move some of a push's blocks and not others, and
     * a partial push is not a thing Minecraft can express. Refusing the whole
     * push is the only honest option.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void pistonExtend(BlockPistonExtendEvent e) {
        if (ledger.isEmpty()) return;
        for (Block b : e.getBlocks())
            if (ledger.isReserved(keyOf(b.getRelative(e.getDirection())))) { e.setCancelled(true); return; }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void pistonRetract(BlockPistonRetractEvent e) {
        if (ledger.isEmpty()) return;
        for (Block b : e.getBlocks())
            if (ledger.isReserved(keyOf(b.getRelative(e.getDirection())))) { e.setCancelled(true); return; }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void form(BlockFormEvent e) {
        if (ledger.isEmpty()) return;
        if (ledger.isReserved(keyOf(e.getBlock()))) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void spread(BlockSpreadEvent e) {
        if (ledger.isEmpty()) return;
        if (ledger.isReserved(keyOf(e.getBlock()))) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void grow(BlockGrowEvent e) {
        if (ledger.isEmpty()) return;
        if (ledger.isReserved(keyOf(e.getBlock()))) e.setCancelled(true);
    }

    /**
     * An explosion cannot destroy a reservation, because there is nothing
     * there to destroy -- but it must not be allowed to leave debris in one.
     * Reserved positions are simply removed from the block list.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void explode(EntityExplodeEvent e) {
        if (ledger.isEmpty()) return;
        e.blockList().removeIf(b -> ledger.isReserved(keyOf(b)));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void explode(BlockExplodeEvent e) {
        if (ledger.isEmpty()) return;
        e.blockList().removeIf(b -> ledger.isReserved(keyOf(b)));
    }

    public String report() {
        return "DISPLACEMENT reserved=" + reservedCount()
             + " (in memory only; a restart loses held blocks -- see the class doc)";
    }
}
