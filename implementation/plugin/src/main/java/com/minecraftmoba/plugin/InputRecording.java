package com.minecraftmoba.plugin;

import java.util.ArrayList;
import java.util.List;

/**
 * A take: the ability inputs a tester performed, replayable by a ghost.
 *
 * <h2>Inputs, not positions</h2>
 *
 * The ghost is a real player, so replaying INPUTS keeps every game rule in force:
 * its casts obey cooldowns, Stun and Root. Replaying positions would teleport past
 * all of that. What is stored per event is the slot pressed, when (ticks from the
 * start of the take), and where the tester was looking.
 *
 * <h2>Facing is relative</h2>
 *
 * The ghost stands somewhere else, facing another way, so an absolute yaw would
 * point it wrongly. Yaw is stored relative to the recorder's facing at the start
 * and replayed relative to the ghost's own post. Pitch is absolute.
 *
 * <h2>What it does not capture</h2>
 *
 * Movement, hold durations, and anything the tester did that was not a slot press.
 * A replay is approximate: terrain, timing and randomness differ per run, and the
 * report says so. It is open-loop, so it does not react to the tester.
 *
 * Pure and tested without a server.
 */
public final class InputRecording {
    private InputRecording() {}

    /** One slot press: {@code slot} is "a1", "a2" or "ult". */
    public record Event(long offset, String slot, float relYaw, float pitch) {}

    /** A finished recording and the class it was made as, which the ghost must be. */
    public record Take(String classId, int level, List<Event> events, long length) {
        public Take { events = List.copyOf(events); }
        public boolean empty() { return events.isEmpty(); }
    }

    public static final class Recorder {
        private final long startTick;
        private final float startYaw;
        private final List<Event> events = new ArrayList<>();

        public Recorder(long startTick, float startYaw) { this.startTick = startTick; this.startYaw = startYaw; }

        public void record(long tick, String slot, float yaw, float pitch) {
            events.add(new Event(Math.max(0, tick - startTick), slot, normalize(yaw - startYaw), pitch));
        }

        public int count() { return events.size(); }

        public Take finish(long endTick, String classId, int level) {
            long end = Math.max(endTick - startTick, events.isEmpty() ? 0 : events.get(events.size() - 1).offset());
            return new Take(classId, level, events, end);
        }
    }

    /** Walks a take forward in time, once or looping. */
    public static final class Replayer {
        private final Take take;
        private final boolean loop;
        private int next;
        private long cycle;

        public Replayer(Take take, boolean loop) { this.take = take; this.loop = loop; }

        /** The events now due, given ticks elapsed since the replay began. */
        public List<Event> advance(long elapsed) {
            var due = new ArrayList<Event>();
            if (take.empty()) return due;
            while (!done()) {
                var e = take.events().get(next);
                if (cycle * Math.max(1, take.length()) + e.offset() > elapsed) break;
                due.add(e);
                if (++next >= take.events().size()) {
                    if (loop) { next = 0; cycle++; } else next = take.events().size();
                }
            }
            return due;
        }

        public boolean done() { return take.empty() || (!loop && next >= take.events().size()); }
    }

    /** The yaw a ghost with this post facing should adopt for a recorded relative yaw. */
    public static float replayYaw(float postYaw, float relYaw) { return normalize(postYaw + relYaw); }

    static float normalize(float yaw) {
        float y = yaw % 360f;
        if (y >= 180f) y -= 360f;
        if (y < -180f) y += 360f;
        return y;
    }
}
