package com.minecraftmoba.plugin;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** The Task allocation ledger: seven allocations, three trees, one commitment. */
class TaskLedgerTest {
    private static final List<Integer> LEVELS = List.of(4,8,12,16,20,24,28);
    private final TaskLedger ledger = new TaskLedger(LEVELS);

    private List<PlayerData.ChoiceRecord> spend(String... trees) {
        var records = new ArrayList<PlayerData.ChoiceRecord>();
        for (int i = 0; i < trees.length; i++) {
            int level = LEVELS.get(i);
            records.add(ledger.allocate(level, records, level, trees[i]));
        }
        return records;
    }

    @Test void sevenAllocationsExist() { assertEquals(7, ledger.budget()); }

    @Test void allocationIsOfferedOnlyAtTaskLevels() {
        assertEquals(List.of(), ledger.pending(3, List.of()));
        assertEquals(List.of(4), ledger.pending(4, List.of()));
        assertEquals(List.of(4, 8), ledger.pending(8, List.of()));
        assertEquals(7, ledger.pending(30, List.of()).size());
        assertFalse(ledger.canAllocate(5, List.of(), 5, TaskLedger.EFFICIENCY));
        assertFalse(ledger.canAllocate(4, List.of(), 8, TaskLedger.EFFICIENCY));
        assertFalse(ledger.canAllocate(4, List.of(), 4, "cooking"));
    }

    @Test void oneAllocationPerOpportunity() {
        var records = spend(TaskLedger.EFFICIENCY);
        assertFalse(ledger.canAllocate(8, records, 4, TaskLedger.YIELD));
        assertTrue(ledger.canAllocate(8, records, 8, TaskLedger.YIELD));
        assertThrows(IllegalArgumentException.class,
            () -> ledger.allocate(8, records, 4, TaskLedger.YIELD));
    }

    @Test void repeatedAllocationAdvancesTheSameTree() {
        var records = spend(TaskLedger.EFFICIENCY, TaskLedger.EFFICIENCY, TaskLedger.EFFICIENCY);
        assertEquals(3, ledger.tier(records, TaskLedger.EFFICIENCY));
        assertEquals(0, ledger.tier(records, TaskLedger.YIELD));
    }

    @Test void exclusiveSpecialistSpendsEverythingOnOneTree() {
        var records = spend(TaskLedger.EFFICIENCY, TaskLedger.EFFICIENCY, TaskLedger.EFFICIENCY,
            TaskLedger.EFFICIENCY, TaskLedger.EFFICIENCY, TaskLedger.EFFICIENCY, TaskLedger.EFFICIENCY);
        assertEquals(7, ledger.tier(records, TaskLedger.EFFICIENCY));
        assertEquals(7, ledger.spent(records));
        assertEquals(List.of(TaskLedger.EFFICIENCY), ledger.committed(records));
    }

    @Test void advancedHybridCommitsToOneTreeOnly() {
        var records = spend(TaskLedger.YIELD, TaskLedger.YIELD, TaskLedger.YIELD, TaskLedger.YIELD,
            TaskLedger.SLAYING, TaskLedger.SLAYING, TaskLedger.SLAYING);
        assertEquals(4, ledger.tier(records, TaskLedger.YIELD));
        assertEquals(3, ledger.tier(records, TaskLedger.SLAYING));
        assertEquals(List.of(TaskLedger.YIELD), ledger.committed(records));
        assertFalse(ledger.commitmentReachable(records, TaskLedger.SLAYING));
    }

    @Test void spreadingAcrossThreeTreesCommitsToNone() {
        var records = spend(TaskLedger.YIELD, TaskLedger.YIELD, TaskLedger.YIELD,
            TaskLedger.EFFICIENCY, TaskLedger.EFFICIENCY, TaskLedger.SLAYING, TaskLedger.SLAYING);
        assertEquals(7, ledger.spent(records));
        assertEquals(List.of(), ledger.committed(records));
    }

    @Test void theBudgetIsExhaustedAfterSeven() {
        var records = spend(TaskLedger.YIELD, TaskLedger.YIELD, TaskLedger.YIELD, TaskLedger.YIELD,
            TaskLedger.SLAYING, TaskLedger.SLAYING, TaskLedger.SLAYING);
        assertEquals(List.of(), ledger.pending(30, records));
        assertEquals(7, ledger.spent(records));
    }

    /**
     * 4 + 4 > 7, so no distribution of seven allocations reaches the commitment
     * tier twice. The budget enforces it; no exclusivity rule is encoded.
     */
    @Test void noDistributionCommitsTwice() {
        int distributions = 0;
        for (int y = 0; y <= 7; y++) for (int e = 0; e <= 7 - y; e++) {
            int s = 7 - y - e;
            distributions++;
            int committed = (y >= TaskLedger.COMMITMENT_TIER ? 1 : 0)
                + (e >= TaskLedger.COMMITMENT_TIER ? 1 : 0)
                + (s >= TaskLedger.COMMITMENT_TIER ? 1 : 0);
            assertTrue(committed <= 1, "two commitments at " + y + "/" + e + "/" + s);
        }
        assertEquals(36, distributions);
    }

    @Test void tiersSurviveTheSaveFormat() throws Exception {
        var uuid = UUID.randomUUID();
        var data = new PlayerData(uuid);
        data.level = 12;
        data.choices.addAll(spend(TaskLedger.YIELD, TaskLedger.EFFICIENCY, TaskLedger.YIELD));
        var back = PlayerDataCodec.decode(uuid, PlayerDataCodec.encode(data), 30);
        assertEquals(data.choices, back.choices);
        assertEquals(2, ledger.tier(back.choices, TaskLedger.YIELD));
        assertEquals(1, ledger.tier(back.choices, TaskLedger.EFFICIENCY));
    }
}
