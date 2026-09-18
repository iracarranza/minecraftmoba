package com.minecraftmoba.plugin;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class NativePacketQueueTest {
    public interface Listener {}
    public interface Packet { void handle(Listener listener); String type(); }
    public static final class Processor {
        private final ArrayDeque<Runnable> queue = new ArrayDeque<>();
        public void scheduleIfPossible(Listener listener, Packet packet) { queue.add(() -> packet.handle(listener)); }
        void drain() { while (!queue.isEmpty()) queue.remove().run(); }
    }
    public static final class InputPacket implements Packet {
        private final Runnable action;
        InputPacket(Runnable action) { this.action = action; }
        public void handle(Listener listener) { action.run(); }
        public String type() { return "player_action"; }
    }
    @Test void consumedSwapRunsBeforeFollowingVanillaClick() throws Exception {
        var processor = new Processor(); var listener = new Listener() {};
        var queue = new NativePacketQueue(processor, listener, Packet.class, Listener.class);
        var observed = new ArrayList<String>();
        queue.submit(new InputPacket(() -> fail("Consumed F reached vanilla")), () -> { observed.add("mode"); return true; });
        processor.scheduleIfPossible(listener, new InputPacket(() -> observed.add("click")));
        assertTrue(observed.isEmpty(), "No game state is touched on the submitting thread");
        processor.drain();
        assertEquals(List.of("mode", "click"), observed);
    }
    @Test void unconsumedDropStaysInOrderAndRunsOnlyOnce() throws Exception {
        var processor = new Processor(); var listener = new Listener() {};
        var queue = new NativePacketQueue(processor, listener, Packet.class, Listener.class);
        var observed = new ArrayList<String>();
        queue.submit(new InputPacket(() -> observed.add("vanilla drop")), () -> false);
        processor.scheduleIfPossible(listener, new InputPacket(() -> observed.add("next packet")));
        processor.drain();
        assertEquals(List.of("vanilla drop", "next packet"), observed);
    }
}
