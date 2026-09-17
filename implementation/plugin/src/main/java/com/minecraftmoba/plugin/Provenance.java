package com.minecraftmoba.plugin;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.event.*;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.persistence.PersistentDataType;
import java.io.*;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;

/** No cache of block sets: the chunk PDC is the authority. All access is on the server thread. */
public final class Provenance implements Listener {
    private record ChunkId(UUID world, int x, int z) {}
    private final MobaPlugin plugin;
    private final NamespacedKey key;
    private final Map<ChunkId, Integer> payloads = new HashMap<>();
    private final Cost place = new Cost(), clear = new Cost(), read = new Cost();
    private final long started = System.nanoTime();
    private long bytes, peakBytes, peakChunk, reclaimedBytes, pistonMarked, explosionMarked;
    private final Path csv;
    private static final class Cost {
        long calls, nanos, max;
        void add(long elapsed) { calls++; nanos += elapsed; max = Math.max(max, elapsed); }
        double mean() { return calls == 0 ? 0 : (double)nanos / calls; }
    }
    public Provenance(MobaPlugin plugin) {
        this.plugin = plugin; key = new NamespacedKey(plugin, "placed_blocks_v1");
        csv = plugin.getDataFolder().toPath().resolve("measurements").resolve("provenance-" + Instant.now().toEpochMilli() + ".csv");
        for (World w : Bukkit.getWorlds()) for (Chunk chunk : w.getLoadedChunks()) observe(chunk, payload(chunk));
        try {
            Files.createDirectories(csv.getParent());
            Files.writeString(csv, "timestamp,elapsedSeconds,observedNonemptyChunks,payloadBytes,peakPayloadBytes,maxChunkPayloadBytes,reclaimedPayloadBytes,placeCalls,placeMeanNs,placeMaxNs,breakCalls,breakMeanNs,breakMaxNs,readCalls,readMeanNs,readMaxNs,serverMeanTickMs,pistonMarked,explosionMarked\n");
        } catch (IOException ex) { throw new IllegalStateException("Cannot create provenance measurements", ex); }
        long period = plugin.getConfig().getLong("provenance.sampleTicks");
        if (period <= 0) throw new IllegalArgumentException("provenance.sampleTicks must be positive");
        Bukkit.getScheduler().runTaskTimer(plugin, this::sample, period, period);
    }
    private byte[] payload(Chunk c) { return c.getPersistentDataContainer().get(key, PersistentDataType.BYTE_ARRAY); }
    private int index(Block b) { return ProvenanceBits.index(b.getX(), b.getY(), b.getZ(), b.getWorld().getMinHeight(), b.getWorld().getMaxHeight()); }
    private void observe(Chunk c, byte[] payload) {
        var id = new ChunkId(c.getWorld().getUID(), c.getX(), c.getZ());
        int size = payload == null ? 0 : payload.length;
        int old = payloads.getOrDefault(id, 0);
        bytes += size - old;
        if (size == 0) payloads.remove(id); else payloads.put(id, size);
        peakBytes = Math.max(peakBytes, bytes); peakChunk = Math.max(peakChunk, size);
    }
    public void mark(Block b, boolean placed) {
        long start = System.nanoTime();
        Chunk chunk = b.getChunk();
        byte[] before = payload(chunk);
        byte[] after = ProvenanceBits.change(before, index(b), placed);
        if (after.length == 0) chunk.getPersistentDataContainer().remove(key);
        else chunk.getPersistentDataContainer().set(key, PersistentDataType.BYTE_ARRAY, after);
        if (!placed) reclaimedBytes += Math.max(0, (before == null ? 0 : before.length) - after.length);
        observe(chunk, after);
        (placed ? place : clear).add(System.nanoTime() - start);
    }
    public boolean isPlayerPlaced(Block b) {
        long start = System.nanoTime();
        boolean answer = ProvenanceBits.contains(payload(b.getChunk()), index(b));
        read.add(System.nanoTime() - start);
        return answer;
    }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void place(BlockPlaceEvent e) {
        if (e instanceof BlockMultiPlaceEvent multi) multi.getReplacedBlockStates().forEach(s -> mark(s.getBlock(), true));
        else mark(e.getBlockPlaced(), true);
    }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void breakBlock(BlockBreakEvent e) { mark(e.getBlock(), false); }
    @EventHandler(priority = EventPriority.MONITOR)
    public void load(ChunkLoadEvent e) { observe(e.getChunk(), payload(e.getChunk())); }
    // [OPEN]: measurement only. Do not invent transfer or explosion reclamation policy.
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void piston(BlockPistonExtendEvent e) { pistonMarked += e.getBlocks().stream().filter(this::isPlayerPlaced).count(); }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void piston(BlockPistonRetractEvent e) { pistonMarked += e.getBlocks().stream().filter(this::isPlayerPlaced).count(); }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void explosion(EntityExplodeEvent e) { explosionMarked += e.blockList().stream().filter(this::isPlayerPlaced).count(); }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void explosion(BlockExplodeEvent e) { explosionMarked += e.blockList().stream().filter(this::isPlayerPlaced).count(); }
    public String summary() {
        return "provenance observedNonemptyChunks=" + payloads.size() + " payloadBytes=" + bytes + " peakBytes=" + peakBytes
            + " maxChunkBytes=" + peakChunk + " reclaimedBytes=" + reclaimedBytes
            + " placeCount=" + place.calls + " placeMeanNs=" + place.mean() + " placeMaxNs=" + place.max
            + " breakCount=" + clear.calls + " breakMeanNs=" + clear.mean() + " breakMaxNs=" + clear.max
            + " openPistonMarked=" + pistonMarked + " openExplosionMarked=" + explosionMarked + " csv=" + csv;
    }
    public void sample() {
        String row = String.join(",", Instant.now().toString(), Double.toString((System.nanoTime()-started)/1e9),
            Integer.toString(payloads.size()), Long.toString(bytes), Long.toString(peakBytes), Long.toString(peakChunk), Long.toString(reclaimedBytes),
            Long.toString(place.calls), Double.toString(place.mean()), Long.toString(place.max),
            Long.toString(clear.calls), Double.toString(clear.mean()), Long.toString(clear.max),
            Long.toString(read.calls), Double.toString(read.mean()), Long.toString(read.max),
            Double.toString(Bukkit.getServer().getAverageTickTime()), Long.toString(pistonMarked), Long.toString(explosionMarked));
        try { Files.writeString(csv, row + "\n", StandardOpenOption.APPEND); }
        catch (IOException ex) { plugin.getLogger().severe("Cannot append provenance measurement: " + ex.getMessage()); }
    }
}
