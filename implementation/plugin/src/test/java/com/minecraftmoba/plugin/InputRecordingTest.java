package com.minecraftmoba.plugin;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class InputRecordingTest {
    private static InputRecording.Take take() {
        var r = new InputRecording.Recorder(100, 90f);
        r.record(110, "a1", 90f, 5f);      // looking the same way as at the start
        r.record(130, "a2", 135f, -10f);   // 45 degrees to the side
        return r.finish(160, "mole", 12);
    }

    @Test void offsetsAndFacingAreRelativeToTheStart() {
        var t = take();
        assertEquals(10, t.events().get(0).offset()); assertEquals(0f, t.events().get(0).relYaw());
        assertEquals(30, t.events().get(1).offset()); assertEquals(45f, t.events().get(1).relYaw());
        assertEquals(60, t.length()); assertEquals("mole", t.classId()); assertEquals(12, t.level());
    }

    @Test void yawWrapsAroundTheSeam() {
        var r = new InputRecording.Recorder(0, 170f);
        r.record(1, "a1", -170f, 0f);      // 20 degrees clockwise across the +-180 seam
        assertEquals(20f, r.finish(2, "x", 1).events().get(0).relYaw(), 1e-4);
    }

    @Test void replayYawIsRelativeToTheGhostsPost() {
        assertEquals(-135f, InputRecording.replayYaw(180f, 45f), 1e-4);
        assertEquals(180f, Math.abs(InputRecording.replayYaw(180f, 0f)), 1e-4);
    }

    @Test void replayEmitsEventsAtTheirOffsetsOnce() {
        var rp = new InputRecording.Replayer(take(), false);
        assertTrue(rp.advance(9).isEmpty());
        assertEquals(1, rp.advance(10).size());
        assertTrue(rp.advance(29).isEmpty());
        assertEquals("a2", rp.advance(30).get(0).slot());
        assertTrue(rp.done());
        assertTrue(rp.advance(1000).isEmpty());
    }

    @Test void aLateAdvanceEmitsEverythingDueInOrder() {
        var due = new InputRecording.Replayer(take(), false).advance(500);
        assertEquals(2, due.size()); assertEquals("a1", due.get(0).slot()); assertEquals("a2", due.get(1).slot());
    }

    @Test void loopingRepeatsEachCycleAtTheTakeLength() {
        var rp = new InputRecording.Replayer(take(), true);
        assertEquals(2, rp.advance(59).size());
        assertTrue(rp.advance(69).isEmpty());
        assertEquals("a1", rp.advance(70).get(0).slot());    // 60 + 10
        assertFalse(rp.done());
        assertEquals("a2", rp.advance(90).get(0).slot());    // 60 + 30
    }

    @Test void anEmptyTakeReplaysNothingAndIsDone() {
        var empty = new InputRecording.Recorder(0, 0f).finish(50, "x", 1);
        var rp = new InputRecording.Replayer(empty, true);
        assertTrue(empty.empty()); assertTrue(rp.done()); assertTrue(rp.advance(100).isEmpty());
    }
}
