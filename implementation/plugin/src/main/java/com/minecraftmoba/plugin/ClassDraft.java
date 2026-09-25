package com.minecraftmoba.plugin;

import java.util.*;

/**
 * The class selection draft: bans, then pick windows, in a shared physical hall.
 *
 * Specified by docs/design/PRE_MATCH_SELECTION_FLOW.md, which decides five
 * things and deliberately leaves a dozen open. This implements the five and
 * takes the rest as PARAMETERS rather than inventing them, the same discipline
 * the regenerative portfolio uses: machinery now, calibration when it is
 * decided.
 *
 * WHAT IS DECIDED, and is therefore structural here:
 *
 *   1. Classes are drafted FIRST and BLIND; maps follow. This class knows
 *      nothing about maps, which is the point -- a composition has to be
 *      defensible across the rotation.
 *   2. Every player may ban, and BANS ARE GLOBAL. A banned class is gone for
 *      both teams, because that is what the physical model already says: a
 *      station that is removed is removed from the room.
 *   3. THE TURN IS THE ABILITY TO MOVE. Off-turn players are ghosts; on-turn
 *      players are solid. That is expressed here as `onTurn`, and the ghost
 *      state is applied by the caller.
 *   4. The unit of selection is the CLASS ALONE. Branches and upgrades arrive
 *      at authored levels during play, so drafting them would pre-empt the
 *      progression curve.
 *   5. A phase must terminate regardless of player behaviour, and the stated
 *      direction is a DEFAULT ASSIGNMENT RATHER THAN A STALL.
 *
 * WHAT IS OPEN, and is a parameter here rather than a decision:
 * bans per team; whether bans are simultaneous or sequential; reveal timing;
 * collision handling; pick window size, alternation and duration; whether
 * class exclusivity is global or per-team; lock and re-pick rules. Each is
 * named in {@link Rules} so a reader sees what has not been settled.
 */
public final class ClassDraft {

    /** Every value the spec leaves open, gathered so the gaps are visible. */
    public record Rules(
            /** Bans available to each team. OPEN: one per player is the obvious
             *  reading of "all players may ban", but the total may want to be
             *  smaller than the roster. */
            int bansPerTeam,
            /** Window sizes in order, alternating from the team that won the
             *  coinflip. DECIDED 25 September: 2-3-2-3-3-1 for a 7v7.
             *
             *  The first proposal was 2-3-3-3-2-1 and it is not balanced.
             *  Counting enemy picks visible at each choice, A sees 21 and B
             *  sees 28 -- B picks all seven informed while A picks two blind.
             *  Moving one pick from A's second window to its third closes the
             *  gap to 1 (24 against 25) WITHOUT adding a window, so the
             *  six-window pacing a physical hall wants is kept. */
            int[] windows,
            /** OPEN: whether a class taken by one team is unavailable to the
             *  other (each pick is also a denial) or only within a team
             *  (mirror matchups allowed). */
            boolean exclusiveAcrossTeams,
            /** OPEN: whether a claimed station can be released within its own
             *  window. */
            boolean allowRepickInWindow) {

        /** The decided 7v7 order. */
        public static final int[] SNAKE_7V7 = {2, 3, 2, 3, 3, 1};

        public static Rules provisional(int rosterSize) {
            return new Rules(rosterSize, SNAKE_7V7, true, true);
        }

        /** Total picks this window sequence allows each team. */
        public int picksFor(boolean first) {
            int n = 0;
            for (int i = 0; i < windows.length; i++)
                if ((i % 2 == 0) == first) n += windows[i];
            return n;
        }
    }

    public enum Phase { BAN, PICK, COMPLETE }

    private final Rules rules;
    private final List<String> roster;              // class ids, in hall order
    private final Map<Team, List<UUID>> players;
    private final Set<String> banned = new LinkedHashSet<>();
    private final Map<UUID, String> personalBans = new LinkedHashMap<>();
    public Map<UUID, String> personalBans() { return Map.copyOf(personalBans); }
    private final Map<UUID, String> picks = new LinkedHashMap<>();
    /**
     * What each player's own stand is wearing, committed or not.
     *
     * `hover` works OFF-TURN by design: it is the pre-commitment channel the
     * physical hall exists for, and a ghost hovering a class is broadcasting
     * intent to both teams. See the 25 September amendment.
     */
    private final Map<UUID, String> hovering = new LinkedHashMap<>();
    /** Players the roster could not serve; reported rather than retried. */
    private final Set<UUID> unassignable = new LinkedHashSet<>();
    private final Map<Team, Integer> bansUsed = new EnumMap<>(Team.class);
    private final List<UUID> pickOrder = new ArrayList<>();
    private final Team firstPick;
    private final List<int[]> windowBounds;
    private int cursor;
    private Phase phase = Phase.BAN;

    public ClassDraft(Rules rules, List<String> roster, Map<Team, List<UUID>> players) {
        this(rules, roster, players, new Random().nextBoolean() ? Team.NORTH : Team.SOUTH);
    }

    /**
     * @param firstPick the coinflip winner, who picks first and -- per
     *                  PRE_MATCH_SELECTION_FLOW -- concedes the final MAP
     *                  choice to the other team as compensation.
     */
    public ClassDraft(Rules rules, List<String> roster,
                      Map<Team, List<UUID>> players, Team firstPick) {
        this.rules = rules;
        this.roster = List.copyOf(roster);
        this.players = Map.copyOf(players);
        for (Team t : players.keySet()) bansUsed.put(t, 0);
        // OPEN: alternation. Alternating one player at a time is the most
        // readable order and is not a decision -- it is the simplest thing
        // that satisfies "only some players may act at a time".
        // Lay the snake out once, so `onTurn` reads a list instead of
        // recomputing which window it is in. Plain alternation was here
        // before and contradicted the decided order outright.
        this.firstPick = firstPick;
        Team second = firstPick == Team.NORTH ? Team.SOUTH : Team.NORTH;
        var queue = new EnumMap<Team, ArrayDeque<UUID>>(Team.class);
        queue.put(firstPick, new ArrayDeque<>(players.getOrDefault(firstPick, List.of())));
        queue.put(second, new ArrayDeque<>(players.getOrDefault(second, List.of())));
        for (int w = 0; w < rules.windows().length; w++) {
            Team turn = (w % 2 == 0) ? firstPick : second;
            for (int i = 0; i < rules.windows()[w] && !queue.get(turn).isEmpty(); i++)
                pickOrder.add(queue.get(turn).poll());
        }
        // Anyone the sequence did not reach still picks, so a roster larger
        // than the window sizes cannot silently lose a player.
        for (var remaining : queue.values()) pickOrder.addAll(remaining);
        windowBounds = new ArrayList<>();
        int at = 0;
        for (int w = 0; w < rules.windows().length && at < pickOrder.size(); w++) {
            int size = Math.min(rules.windows()[w], pickOrder.size() - at);
            windowBounds.add(new int[]{at, at + size});
            at += size;
        }
        if (at < pickOrder.size()) windowBounds.add(new int[]{at, pickOrder.size()});
        // A draft with no bans configured starts in PICK. `maybeAdvance` is
        // only reached from a verb, so with zero bans nothing ever called it
        // and the phase sat in BAN with no way out -- a draft that could not
        // begin.
        if (rules.bansPerTeam() <= 0) phase = Phase.PICK;
    }

    public Phase phase() { return phase; }

    /** Who won the coinflip. They pick classes first and concede the map choice. */
    public Team firstPick() { return firstPick; }

    public Set<String> banned() { return Set.copyOf(banned); }

    public Map<UUID, String> picks() { return Map.copyOf(picks); }
    public Map<UUID, String> hovering() { return Map.copyOf(hovering); }

    /** Players no class could be assigned to. Empty unless the roster is too small. */
    public Set<UUID> unassignable() { return Set.copyOf(unassignable); }

    /** What every stand is showing: a locked pick, or a hover. */
    public Map<UUID, String> stands() {
        Map<UUID, String> out = new LinkedHashMap<>(hovering);
        out.putAll(picks);
        return out;
    }

    /**
     * Show a class on your own stand without committing to it.
     *
     * Allowed off-turn and in either phase. Refused for a banned class,
     * because the banned section already shows those and a stand wearing one
     * would say a thing that is not true.
     */
    public String hover(UUID player, String classId) {
        if (phase == Phase.COMPLETE) return "the draft is over";
        if (teamOf(player) == null) return "not in the draft";
        if (!roster.contains(classId)) return "no such class";
        if (banned.contains(classId)) return "banned";
        if (picks.containsKey(player)) return "already locked in";
        hovering.put(player, classId);
        return null;
    }

    /** Classes still standing in the hall. */
    public List<String> available() {
        List<String> out = new ArrayList<>();
        for (String id : roster) if (!banned.contains(id)) out.add(id);
        return out;
    }

    /**
     * Whose turn it is: the players who may walk and claim.
     *
     * In the BAN phase every player may act, because the spec says every
     * player may ban. In the PICK phase it is the current window.
     */
    public Set<UUID> onTurn() {
        if (phase == Phase.COMPLETE) return Set.of();
        if (phase == Phase.BAN) {
            Set<UUID> all = new LinkedHashSet<>();
            players.values().forEach(all::addAll);
            all.removeAll(personalBans.keySet());
            return all;
        }
        // The window the cursor sits in, not a fixed count: the snake's
        // windows are 2, 3, 2, 3, 3, 1 and a constant would flatten them.
        Set<UUID> window = new LinkedHashSet<>();
        for (int[] bound : windowBounds) {
            if (cursor >= bound[1]) continue;
            for (int i = bound[0]; i < bound[1]; i++)
                if (!picks.containsKey(pickOrder.get(i))) window.add(pickOrder.get(i));
            // A snake window belonging to an empty team is a no-op in a
            // small-lobby test. Skip it rather than presenting an empty turn.
            if (!window.isEmpty()) break;
            cursor = bound[1];
        }
        return window;
    }

    /** A player not on turn is a ghost: present, visible, unable to act. */
    public boolean isGhost(UUID player) {
        return phase != Phase.COMPLETE && !onTurn().contains(player);
    }

    private Team teamOf(UUID player) {
        for (var e : players.entrySet()) if (e.getValue().contains(player)) return e.getKey();
        return null;
    }

    /**
     * Ban a class by interacting with its station.
     *
     * A ban is GLOBAL: the station leaves the hall for both teams. Rejected
     * reasons are returned rather than thrown, because a player walking into
     * a taken station is ordinary and not exceptional.
     */
    public String ban(UUID player, String classId) {
        if (phase != Phase.BAN) return "the ban phase is over";
        Team team = teamOf(player);
        if (team == null) return "not in the draft";
        if (personalBans.containsKey(player)) return "you have already banned";
        if (!roster.contains(classId)) return "no such class";
        // OPEN: collision handling. Refusing the second ban is the only option
        // that does not silently spend it, so a caller can still choose to
        // return or waste it without this having decided.
        if (banned.contains(classId)) return "already banned";
        if (bansUsed.get(team) >= rules.bansPerTeam()) return "no bans left";
        banned.add(classId);
        personalBans.put(player, classId);
        bansUsed.merge(team, 1, Integer::sum);
        maybeAdvance();
        return null;
    }

    /** Claim a class by interacting with its station. */
    public String pick(UUID player, String classId) {
        if (phase != Phase.PICK) return "not the pick phase";
        if (!onTurn().contains(player)) return "not your turn";
        if (banned.contains(classId)) return "banned";
        if (!roster.contains(classId)) return "no such class";
        String taken = takenBy(classId, teamOf(player));
        if (taken != null) return taken;
        String previous = picks.put(player, classId);
        if (previous != null && !rules.allowRepickInWindow()) {
            picks.put(player, previous);
            return "already locked";
        }
        hovering.remove(player);
        maybeAdvance();
        return null;
    }

    private String takenBy(String classId, Team team) {
        for (var e : picks.entrySet()) {
            if (!e.getValue().equals(classId)) continue;
            if (rules.exclusiveAcrossTeams()) return "taken";
            if (teamOf(e.getKey()) == team) return "taken by a teammate";
        }
        return null;
    }

    /**
     * End the current window, assigning defaults to anyone who did not act.
     *
     * A DEFAULT ASSIGNMENT RATHER THAN A STALL, which is the spec's stated
     * direction for every timeout case. A single absent player cannot hold a
     * match. The assignment is returned so the caller can say in the hall that
     * one happened, which the spec also asks for: the rest of the match should
     * know an assignment occurred rather than a choice.
     */
    public Map<UUID, String> timeout() {
        Map<UUID, String> assigned = new LinkedHashMap<>();
        if (phase == Phase.BAN) {
            // OPEN: whether an unspent ban lapses or is auto-assigned. Lapsing
            // is chosen as the reversible one: an auto-ban removes a class from
            // everyone on nobody's decision.
            phase = Phase.PICK;
            return assigned;
        }
        if (phase != Phase.PICK) return assigned;
        for (UUID player : onTurn()) {
            // THE HOVER IS A DECLARED PREFERENCE, so an assignment follows the
            // player's own stated intent where one exists. An earlier version
            // took the first available class, which ignored a choice the
            // player had already broadcast to the room.
            String wanted = hovering.get(player);
            String choice = (wanted != null && !banned.contains(wanted)
                    && takenBy(wanted, teamOf(player)) == null)
                    ? wanted : firstAvailableFor(player);
            if (choice != null) {
                picks.put(player, choice);
                hovering.remove(player);
                assigned.put(player, choice);
            } else {
                // NOTHING LEFT TO ASSIGN, and the draft must still end. A
                // roster smaller than the player count -- or exhausted by
                // global exclusivity -- otherwise leaves the cursor parked on
                // a player who can never pick, and a timeout loop spins for
                // ever. Better an unassigned player than a hung draft.
                unassignable.add(player);
            }
        }
        maybeAdvance();
        return assigned;
    }

    private String firstAvailableFor(UUID player) {
        Team team = teamOf(player);
        for (String id : available()) if (takenBy(id, team) == null) return id;
        return null;
    }

    private void maybeAdvance() {
        if (phase == Phase.BAN) {
            int total = 0, allowed = 0;
            for (Team t : players.keySet()) {
                if (!players.getOrDefault(t, List.of()).isEmpty()) {
                    total += bansUsed.getOrDefault(t, 0);
                    allowed += Math.min(rules.bansPerTeam(), players.get(t).size());
                }
            }
            if (total >= allowed) phase = Phase.PICK;
            return;
        }
        while (cursor < pickOrder.size()
                && (picks.containsKey(pickOrder.get(cursor))
                    || unassignable.contains(pickOrder.get(cursor)))) cursor++;
        if (cursor >= pickOrder.size()) phase = Phase.COMPLETE;
    }

    /** What the hall should show, for status output. */
    public String report() {
        return "draft phase=" + phase + " banned=" + banned.size()
                + " picked=" + picks.size() + "/" + pickOrder.size()
                + " onTurn=" + onTurn().size();
    }
}
