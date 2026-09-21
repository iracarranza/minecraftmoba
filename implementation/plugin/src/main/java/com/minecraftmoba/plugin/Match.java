package com.minecraftmoba.plugin;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.scheduler.BukkitTask;

import java.io.IOException;
import java.util.*;

/**
 * Authoritative match state: participation, teams, clock, Fountains, victory.
 *
 * This is the piece the audit found missing. Everything else in the plugin was
 * a system without a contest to take part in; this owns the contest.
 *
 * Victory (ALPHA-D3): a team wins when the opposing team has no remaining
 * living participating players *while that team's Fountain is disabled*.
 * Disabling a Fountain is not victory — it stops respawning, and the survivors
 * must still be eliminated. The predicate below is the real rule; debug
 * commands force the state that feeds it rather than short-circuiting it.
 */
public final class Match implements Listener {
    public enum State { IDLE, RUNNING, ENDED }

    /** A participant is alive until eliminated; eliminated is terminal in a match. */
    public static final class Participant {
        public final UUID uuid;
        public final Team team;
        public boolean alive = true;
        public Participant(UUID uuid, Team team) { this.uuid = uuid; this.team = team; }
    }

    private final MobaPlugin plugin;
    private final WorldInstance worldInstance;
    private final Map<UUID, Participant> participants = new LinkedHashMap<>();
    private final EnumMap<Team, Boolean> fountainDisabled = new EnumMap<>(Team.class);
    private final EnumMap<Team, Location> homelands = new EnumMap<>(Team.class);

    private State state = State.IDLE;
    private long elapsed;
    private Team winner;
    private BukkitTask ticker;

    public Match(MobaPlugin plugin, WorldInstance worldInstance) {
        this.plugin = plugin;
        this.worldInstance = worldInstance;
        resetFields();
    }

    private void resetFields() {
        participants.clear();
        for (Team t : Team.values()) fountainDisabled.put(t, false);
        state = State.IDLE; elapsed = 0; winner = null;
    }

    public State state() { return state; }
    public long elapsedTicks() { return elapsed; }
    public Team winner() { return winner; }
    public boolean running() { return state == State.RUNNING; }
    public Participant participant(UUID id) { return participants.get(id); }
    public Collection<Participant> participants() { return participants.values(); }
    public boolean fountainDisabled(Team t) { return fountainDisabled.get(t); }
    public Location homeland(Team t) { return homelands.get(t); }

    public List<Participant> living(Team t) {
        return participants.values().stream().filter(p -> p.team == t && p.alive).toList();
    }

    // ---- lifecycle -------------------------------------------------------

    /** Load the Alpha instance and open enrolment. Does not start the clock. */
    public String open() throws IOException {
        if (state == State.RUNNING) throw new IllegalStateException("A match is already running.");
        resetFields();
        World w = worldInstance.load();
        w.setGameRule(GameRule.DO_IMMEDIATE_RESPAWN, true);
        loadHomelands(w);
        int renewables = plugin.resetRenewables();
        plugin.getLogger().info("[match] bound " + renewables + " renewable source(s)");
        state = State.IDLE;
        return "Alpha instance '" + w.getName() + "' loaded. Homelands: "
                + homelands.keySet() + ". Add players, then /moba match start.";
    }

    private void loadHomelands(World w) {
        homelands.clear();
        var cfg = plugin.getConfig();
        for (Team t : Team.values()) {
            String key = "alpha.homelands." + t.lower();
            List<Integer> xz = cfg.getIntegerList(key);
            if (xz.size() < 2) continue;
            int x = xz.get(0), z = xz.get(1);
            int y = xz.size() > 2 ? xz.get(2) : w.getHighestBlockYAt(x, z) + 1;
            homelands.put(t, new Location(w, x + 0.5, y, z + 0.5));
        }
    }

    public String add(Player p, Team team) {
        if (state == State.ENDED) throw new IllegalStateException("Match has ended; reset first.");
        participants.put(p.getUniqueId(), new Participant(p.getUniqueId(), team));
        if (state == State.RUNNING) spawn(p, team);
        return p.getName() + " joined " + team.lower() + ".";
    }

    public String start() {
        if (state == State.RUNNING) throw new IllegalStateException("Already running.");
        if (worldInstance.world() == null)
            throw new IllegalStateException("No Alpha instance loaded; run /moba match open.");
        if (participants.isEmpty())
            throw new IllegalStateException("No participants; add at least one player.");
        state = State.RUNNING; elapsed = 0; winner = null;
        World w = worldInstance.world();
        w.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
        // Respawning is instant: the match clock does not stop for a death
        // screen, and the Fountain -- not a button -- is what decides whether a
        // player comes back. Elimination still runs, so a player whose Fountain
        // is disabled respawns and is immediately put into spectator.
        w.setGameRule(GameRule.DO_IMMEDIATE_RESPAWN, true);
        for (Participant part : participants.values()) {
            Player p = Bukkit.getPlayer(part.uuid);
            if (p != null) spawn(p, part.team);
        }
        ticker = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 1L, 1L);
        announce("Match started. " + MatchClock.describe(0));
        return "Match started with " + participants.size() + " participant(s).";
    }

    /**
     * Put a player at their homeland at full strength.
     *
     * "Full" means the player's OWN derived maxima, not vanilla's 20. At level
     * one those are 9 and 9, and setting 20 handed every player eleven hunger
     * points that the Capacity model says they do not have -- invisible in the
     * custom readout, and slowly spent back down to the cap.
     */
    private void spawn(Player p, Team team) {
        Location home = homelands.get(team);
        if (home != null) p.teleport(home);
        p.setGameMode(GameMode.SURVIVAL);
        var max = p.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH);
        p.setHealth(max == null ? 20.0 : max.getValue());
        int hunger = plugin.effectiveHunger(p);
        p.setFoodLevel(hunger);
        p.setSaturation(hunger);
    }

    private void tick() {
        if (state != State.RUNNING) return;
        elapsed++;
        World w = worldInstance.world();
        if (w != null) w.setTime(MatchClock.worldTime(elapsed));
        if (MatchClock.isSunsetBoundary(elapsed)) onSunset(MatchClock.sunsetOrdinal(elapsed));
        else if (MatchClock.isSunriseBoundary(elapsed)) onSunrise();
        if (MatchClock.pastHorizon(elapsed)) {
            // The 48-minute figure is an analytical horizon, not a timed draw.
            // Nothing in canon ends a match on it, so the clock is announced and
            // the match continues until the victory predicate is satisfied.
            announce("48-minute analytical horizon reached; match continues.");
            elapsed++; // announce once
        }
    }

    private void onSunset(int ordinal) {
        var opened = plugin.worksites().onSunset(ordinal);
        announce("Sunset " + ordinal + " (" + MatchClock.minutes(elapsed) + "m): "
                + (opened.isEmpty() ? "no Worksites activated"
                   : opened.size() + " Worksite(s) activated: "
                     + opened.stream().map(w -> w.id).toList()));
        plugin.getLogger().info("[match] sunset " + ordinal + " at "
                + MatchClock.minutes(elapsed) + "m, activated " + opened.size());
    }

    private void onSunrise() {
        var closed = plugin.worksites().onSunrise();
        announce("Sunrise (" + MatchClock.minutes(elapsed) + "m): "
                + closed.size() + " Worksite(s) closed.");
    }

    /**
     * Advance the clock by whole minutes, firing every boundary crossed.
     *
     * This is how one person exercises a 48-minute lifecycle without waiting
     * out 48 minutes. It advances the same counter the ticker advances and
     * runs the same sunset/sunrise handlers, so a skipped sunset is a real
     * sunset rather than a simulated one.
     */
    public String skipMinutes(int minutes) {
        if (!running()) throw new IllegalStateException("No match running.");
        if (minutes <= 0) throw new IllegalArgumentException("Minutes must be positive.");
        long target = elapsed + minutes * 60L * 20L;
        while (elapsed < target && state == State.RUNNING) {
            elapsed++;
            if (MatchClock.isSunsetBoundary(elapsed)) onSunset(MatchClock.sunsetOrdinal(elapsed));
            else if (MatchClock.isSunriseBoundary(elapsed)) onSunrise();
        }
        World w = worldInstance.world();
        if (w != null) w.setTime(MatchClock.worldTime(elapsed));
        return "Advanced to " + MatchClock.describe(elapsed) + ".";
    }

    // ---- fountains and elimination ---------------------------------------

    /**
     * Disable a team's Fountain. Irreversible for Alpha 0.1 (ALPHA-D3); repair
     * and reactivation are unresolved and deliberately absent.
     */
    public String disableFountain(Team team) {
        if (!running()) throw new IllegalStateException("No match running.");
        if (fountainDisabled.get(team)) return team.lower() + " Fountain is already disabled.";
        fountainDisabled.put(team, true);
        announce(team.lower() + " Aether Fountain disabled: " + team.lower()
                + " can no longer respawn.");
        evaluateVictory();
        return team.lower() + " Fountain disabled.";
    }

    public String eliminate(UUID id, String cause) {
        Participant part = participants.get(id);
        if (part == null || !part.alive) return "not a living participant";
        part.alive = false;
        Player p = Bukkit.getPlayer(id);
        announce((p != null ? p.getName() : id.toString()) + " eliminated (" + cause + ").");
        if (p != null) p.setGameMode(GameMode.SPECTATOR);
        evaluateVictory();
        return "eliminated";
    }

    /**
     * The victory predicate (ALPHA-D3).
     *
     * A team wins when its opponent has no living participants AND that
     * opponent's Fountain is disabled. Both halves are required: survivors with
     * a disabled Fountain have not lost yet, and an empty field with a working
     * Fountain is not a win because respawning continues.
     */
    public Team victor() {
        if (state != State.RUNNING) return null;
        return victorOf(participants.values(), fountainDisabled);
    }

    /**
     * The predicate itself, as a pure function so it can be tested without a
     * server. This is the rule the whole Alpha lifecycle exists to reach, so it
     * should not be reachable only through a running match.
     */
    public static Team victorOf(Collection<Participant> parts,
                                Map<Team, Boolean> disabled) {
        for (Team t : Team.values()) {
            Team loser = t.other();
            boolean hasParticipants = parts.stream().anyMatch(p -> p.team == loser);
            if (!hasParticipants) continue;
            boolean survivors = parts.stream().anyMatch(p -> p.team == loser && p.alive);
            if (Boolean.TRUE.equals(disabled.get(loser)) && !survivors) return t;
        }
        return null;
    }

    private void evaluateVictory() {
        Team v = victor();
        if (v != null) end(v);
    }

    public String end(Team victor) {
        if (state != State.RUNNING) throw new IllegalStateException("No match running.");
        state = State.ENDED; winner = victor;
        if (ticker != null) { ticker.cancel(); ticker = null; }
        announce(victor == null ? "Match ended with no victor."
                : victor.lower() + " wins: " + victor.other().lower()
                  + " Fountain disabled and no survivors remain.");
        return victor == null ? "Match ended." : victor.lower() + " wins.";
    }

    /** Clear match-scoped state and restore the pristine world (ALPHA-D2). */
    public String reset() throws IOException {
        if (ticker != null) { ticker.cancel(); ticker = null; }
        for (Participant part : participants.values()) {
            Player p = Bukkit.getPlayer(part.uuid);
            if (p != null) {
                p.setGameMode(GameMode.SURVIVAL);
                // Progression is match-scoped for Alpha (ALPHA-D2 confirmation).
                plugin.clearMatchScopedState(p);
            }
        }
        resetFields();
        plugin.worksites().reset();
        // Routes, Infrastructure Mode and contributions are all match-scoped and
        // hold references into the instance world, so they are discarded before
        // that world is replaced.
        int routes = plugin.routes() != null ? plugin.routes().reset() : 0;
        int infra = plugin.infraMode() != null ? plugin.infraMode().reset() : 0;
        int contrib = plugin.contributions() != null ? plugin.contributions().reset() : 0;
        if (plugin.workPoints() != null) plugin.workPoints().reset();
        worldInstance.restore();
        // Renewables bind to a world UUID, and restore() produces a *new* world.
        // Rebuilding before the restore would rebind to the world about to be
        // discarded, which is the stale-binding bug this is meant to prevent.
        int renewables = plugin.resetRenewables();
        return "Match reset: world restored; cleared worksites, " + renewables
                + " renewable source(s), " + routes + " route(s)/pending, "
                + infra + " in infra mode, " + contrib + " capitalization(s).";
    }

    // ---- events ----------------------------------------------------------

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(PlayerDeathEvent e) {
        if (!running()) return;
        Participant part = participants.get(e.getEntity().getUniqueId());
        if (part == null || !part.alive) return;
        // A death only eliminates when the team cannot respawn. This is the
        // whole point of the Fountain rule.
        if (fountainDisabled.get(part.team)) eliminate(part.uuid, "no respawn available");
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent e) {
        if (!running()) return;
        Participant part = participants.get(e.getPlayer().getUniqueId());
        if (part == null) return;
        if (!part.alive) {
            // An eliminated player still respawns, because the respawn is
            // immediate and cannot be refused. Spectator is reapplied on the
            // next tick, after the respawn has finished setting game mode.
            Bukkit.getScheduler().runTask(plugin,
                    () -> e.getPlayer().setGameMode(GameMode.SPECTATOR));
            return;
        }
        Location home = homelands.get(part.team);
        if (home != null) e.setRespawnLocation(home);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        // Leaving is not elimination; the participant keeps its slot so a
        // reconnect rejoins the same team.
    }

    private void announce(String message) {
        String text = ChatColor.AQUA + "[match] " + ChatColor.RESET + message;
        for (Participant part : participants.values()) {
            Player p = Bukkit.getPlayer(part.uuid);
            if (p != null) p.sendMessage(text);
        }
        plugin.getLogger().info("[match] " + message);
    }

    public List<String> report() {
        List<String> out = new ArrayList<>();
        out.add("state=" + state + (winner != null ? " winner=" + winner.lower() : ""));
        out.add("clock=" + MatchClock.describe(elapsed)
                + " sunsets=" + MatchClock.sunsetOrdinal(elapsed));
        out.add("world: " + worldInstance.report());
        for (Team t : Team.values())
            out.add(t.lower() + ": " + living(t).size() + " living of "
                    + participants.values().stream().filter(p -> p.team == t).count()
                    + ", fountain " + (fountainDisabled.get(t) ? "DISABLED" : "active")
                    + ", homeland " + (homelands.containsKey(t)
                        ? homelands.get(t).getBlockX() + "," + homelands.get(t).getBlockZ()
                        : "UNSET"));
        return out;
    }
}
