package com.minecraftmoba.plugin;

import java.util.*;

/**
 * The Task allocation ledger: which of the seven allocations a player has spent,
 * and what tier each tree has reached.
 *
 * Task is the shared player-choice specialization system. At each Task level the
 * player chooses Yield, Efficiency or Slaying, or advances one already chosen.
 * Seven allocations exist in total, so at completion Y + E + S = 7.
 *
 * The arithmetic carries the design rule on its own: 4 + 4 > 7, so reaching
 * Tier IV in one tree permanently prevents another tree from reaching IV. Tier
 * IV is therefore the commitment threshold without needing a separate
 * exclusivity rule, and this class asserts that property rather than enforcing
 * it.
 *
 * This is the LEDGER only. Tier effects live in TaskEffects, and the advanced
 * IV-VII magnitudes are uncalibrated and deliberately absent.
 *
 * Allocations are stored as PlayerData.ChoiceRecord(level, treeId), which is
 * already persisted by PlayerDataCodec, so tiers survive a reload by being
 * recomputed from the record rather than saved separately.
 */
public final class TaskLedger {
    /** Tree identifiers as they appear in a ChoiceRecord and in configuration. */
    public static final String YIELD = "yield";
    public static final String EFFICIENCY = "efficiency";
    public static final String SLAYING = "slaying";
    public static final List<String> TREES = List.of(YIELD, EFFICIENCY, SLAYING);

    /** The commitment threshold. Two trees cannot both reach it out of seven allocations. */
    public static final int COMMITMENT_TIER = 4;

    private final List<Integer> levels;

    /** @param levels the Task allocation levels, in ascending order. */
    public TaskLedger(Collection<Integer> levels) {
        var sorted = new ArrayList<>(new TreeSet<>(levels));
        if (sorted.size() != levels.size())
            throw new IllegalArgumentException("Task levels must be unique");
        if (sorted.stream().anyMatch(l -> l < 0))
            throw new IllegalArgumentException("Task levels must be nonnegative");
        this.levels = List.copyOf(sorted);
    }

    public List<Integer> levels() { return levels; }

    /** Total allocations the ledger can ever grant. Seven under current canon. */
    public int budget() { return levels.size(); }

    public static boolean isTree(String id) { return TREES.contains(id); }

    /** Task allocation levels at or below `level` that have no record yet. */
    public List<Integer> pending(int level, List<PlayerData.ChoiceRecord> records) {
        var spent = new HashSet<Integer>();
        for (var r : records) if (isTree(r.choiceId())) spent.add(r.level());
        return levels.stream().filter(l -> l <= level && !spent.contains(l)).toList();
    }

    /** Tier reached in each tree: the number of allocations spent on it. */
    public Map<String, Integer> tiers(List<PlayerData.ChoiceRecord> records) {
        var tiers = new LinkedHashMap<String, Integer>();
        for (String tree : TREES) tiers.put(tree, 0);
        for (var r : records) {
            if (!isTree(r.choiceId())) continue;
            if (!levels.contains(r.level())) continue;
            tiers.merge(r.choiceId(), 1, Integer::sum);
        }
        return tiers;
    }

    public int tier(List<PlayerData.ChoiceRecord> records, String tree) {
        return tiers(records).getOrDefault(tree, 0);
    }

    public int spent(List<PlayerData.ChoiceRecord> records) {
        return tiers(records).values().stream().mapToInt(Integer::intValue).sum();
    }

    /**
     * Whether an allocation may be recorded for `tree` at `level`.
     *
     * One allocation per Task opportunity, only at a Task level, only at or
     * below the player's level, and never beyond the budget.
     */
    public boolean canAllocate(int playerLevel, List<PlayerData.ChoiceRecord> records,
                               int level, String tree) {
        if (!isTree(tree)) return false;
        if (!levels.contains(level)) return false;
        if (level > playerLevel) return false;
        if (spent(records) >= budget()) return false;
        return pending(playerLevel, records).contains(level);
    }

    public PlayerData.ChoiceRecord allocate(int playerLevel, List<PlayerData.ChoiceRecord> records,
                                            int level, String tree) {
        if (!canAllocate(playerLevel, records, level, tree))
            throw new IllegalArgumentException("Task allocation not available: " + tree + " at " + level);
        return new PlayerData.ChoiceRecord(level, tree);
    }

    /**
     * Whether the budget could still let `tree` reach the commitment tier,
     * given what is already spent. Reporting only: nothing here forbids a
     * distribution that the budget makes unreachable, because the budget
     * already does.
     */
    public boolean commitmentReachable(List<PlayerData.ChoiceRecord> records, String tree) {
        var tiers = tiers(records);
        int have = tiers.getOrDefault(tree, 0);
        int remaining = budget() - spent(records);
        return have + remaining >= COMMITMENT_TIER;
    }

    /** Trees that have crossed the commitment threshold. At most one can, out of seven. */
    public List<String> committed(List<PlayerData.ChoiceRecord> records) {
        var tiers = tiers(records);
        return TREES.stream().filter(t -> tiers.getOrDefault(t, 0) >= COMMITMENT_TIER).toList();
    }
}
