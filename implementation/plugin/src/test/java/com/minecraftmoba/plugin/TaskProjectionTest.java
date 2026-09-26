package com.minecraftmoba.plugin;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * PlayerData.task is a projection of the persisted ChoiceRecord list, not a
 * second authority.
 *
 * It had one writer, on the load path, so a Task allocation made during a
 * session was persisted correctly and had no effect until the player
 * reconnected. applyAndSave now projects too, which is what these tests stand
 * in for: the Bukkit menu path itself is not unit-testable, but the projection
 * it depends on is.
 */
class TaskProjectionTest {
    private final TaskLedger ledger = new TaskLedger(List.of(4,8,12,16,20,24,28));

    private PlayerData player() {
        var d = new PlayerData(UUID.randomUUID());
        d.level = 12;
        return d;
    }

    @Test void anAllocationIsVisibleWithoutReconnecting() {
        var d = player();
        PlayerDataCodec.projectTask(d, ledger);
        assertEquals(0, d.task.get(TaskEffects.Domain.EFFICIENCY.name()));

        d.choices.add(new PlayerData.ChoiceRecord(4, TaskLedger.EFFICIENCY));
        PlayerDataCodec.projectTask(d, ledger);
        assertEquals(1, d.task.get(TaskEffects.Domain.EFFICIENCY.name()));

        d.choices.add(new PlayerData.ChoiceRecord(8, TaskLedger.EFFICIENCY));
        PlayerDataCodec.projectTask(d, ledger);
        assertEquals(2, d.task.get(TaskEffects.Domain.EFFICIENCY.name()));
    }

    /** applyAndSave may run many times per session; projecting must not accumulate. */
    @Test void projectionIsIdempotent() {
        var d = player();
        d.choices.add(new PlayerData.ChoiceRecord(4, TaskLedger.YIELD));
        for (int i = 0; i < 5; i++) PlayerDataCodec.projectTask(d, ledger);
        assertEquals(1, d.task.get(TaskEffects.Domain.YIELD.name()));
    }

    /** Slaying is the design name; DAMAGE is the persisted key kept for old saves. */
    @Test void slayingProjectsOntoTheLegacyDamageKey() {
        var d = player();
        d.choices.add(new PlayerData.ChoiceRecord(4, TaskLedger.SLAYING));
        PlayerDataCodec.projectTask(d, ledger);
        assertEquals(1, d.task.get(TaskEffects.Domain.DAMAGE.name()));
        assertEquals(0, d.task.get(TaskEffects.Domain.YIELD.name()));
        assertEquals(0, d.task.get(TaskEffects.Domain.EFFICIENCY.name()));
    }

    @Test void aReloadAgreesWithTheLiveProjection() throws Exception {
        var d = player();
        d.choices.add(new PlayerData.ChoiceRecord(4, TaskLedger.EFFICIENCY));
        d.choices.add(new PlayerData.ChoiceRecord(8, TaskLedger.SLAYING));
        PlayerDataCodec.projectTask(d, ledger);
        var back = PlayerDataCodec.decode(d.uuid, PlayerDataCodec.encode(d), 30, ledger);
        assertEquals(d.task, back.task);
    }
}
