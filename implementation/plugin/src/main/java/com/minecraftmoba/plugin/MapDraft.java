package com.minecraftmoba.plugin;

import java.util.*;

/** Pure state machine for the post-class map board. */
public final class MapDraft {
    public record Option(String id, String type, String scale, String density,
                         String symmetryBand, String thumbnail) {}

    public record Rules(int boardSize, int strikesByFirst, int strikesBySecond) {
        public Rules {
            if (boardSize < 4 || boardSize % 2 != 0)
                throw new IllegalArgumentException("board size must be 2n+2");
            int n = (boardSize - 2) / 2;
            if (strikesByFirst != n || strikesBySecond != n - 1)
                throw new IllegalArgumentException("strike counts must be n and n-1");
        }
        public static Rules sixBoard() { return new Rules(6, 2, 1); }
    }

    public enum Phase { STRIKE, PICK, COMPLETE }

    private final Rules rules;
    private final Team first;
    private final List<Option> board;
    private final List<Option> struck = new ArrayList<>();
    private Phase phase = Phase.STRIKE;
    private Team turn;
    private int firstStrikes;
    private int secondStrikes;
    private Option chosen;

    public MapDraft(Rules rules, Collection<Option> options, Team first) {
        this.rules = Objects.requireNonNull(rules);
        this.first = Objects.requireNonNull(first);
        var distinct = new LinkedHashMap<String, Option>();
        for (Option option : options) distinct.putIfAbsent(option.id(), option);
        if (distinct.size() != rules.boardSize())
            throw new IllegalArgumentException("board must contain distinct options");
        this.board = new ArrayList<>(distinct.values());
        this.turn = first;
    }

    public Phase phase() { return phase; }
    public Team firstPick() { return first; }
    public Team onTurn() { return phase == Phase.COMPLETE ? null : turn; }
    public boolean isGhost(Team team) { return phase != Phase.COMPLETE && team != turn; }
    public List<Option> board() { return List.copyOf(board); }
    public List<Option> struck() { return List.copyOf(struck); }
    public Option chosen() { return chosen; }

    /** The pre-draft information: types, and nothing else. */
    public Set<String> revealedTypes() {
        var out = new LinkedHashSet<String>();
        for (Option option : board) out.add(option.type());
        return Set.copyOf(out);
    }

    public String strike(Team team, String id) {
        if (phase != Phase.STRIKE) return "striking is over";
        if (team != turn) return "not your turn";
        Option option = find(id);
        if (option == null) return "no such map option";
        board.remove(option); struck.add(option);
        if (team == first) firstStrikes++; else secondStrikes++;
        if (firstStrikes == rules.strikesByFirst() && secondStrikes == rules.strikesBySecond()) {
            phase = Phase.PICK; turn = first.other();
        } else turn = turn.other();
        return null;
    }

    public String pick(Team team, String id) {
        if (phase != Phase.PICK) return "map picking is not open";
        if (team != turn) return "not your turn";
        Option option = find(id);
        if (option == null) return "no such map option";
        chosen = option; phase = Phase.COMPLETE; turn = null;
        return null;
    }

    private Option find(String id) {
        for (Option option : board) if (Objects.equals(option.id(), id)) return option;
        return null;
    }
}
