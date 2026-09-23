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
    /**
     * The match lifecycle, including the pre-match doctrine establishes.
     *
     * `/moba match start` begins the match and its PRE-MATCH process; it does
     * not teleport anyone. Between creating a match and playing one there is a
     * drafting step in which teams choose a map option and only then is a
     * compatible hidden READY realization claimed. Collapsing that into "claim
     * the first READY map and go" would make the draft impossible to add later
     * and would quietly become the production rule.
     *
     * So PRE_MATCH is a real state with a real exit, and the exit is a
     * selection. The production selection rules -- ban order, option counts,
     * whether class or map drafting comes first -- are explicitly open, so what
     * exists is the state machine and a clearly-marked test path through it.
     */
    public enum State { IDLE, PRE_MATCH, RUNNING, ENDED }

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
    /** Positions read from the claimed realization, or null on the template. */
    private MapBindings bindings;
    /** What this match did, for comparison against what the compiler predicted. */
    private MatchRecord record = new MatchRecord();

    public MatchRecord record() { return record; }

    public Match(MobaPlugin plugin, WorldInstance worldInstance) {
        this.plugin = plugin;
        this.worldInstance = worldInstance;
        resetFields();
    }

    private void resetFields() {
        participants.clear();
        for (Team t : Team.values()) fountainDisabled.put(t, false);
        state = State.IDLE; elapsed = 0; winner = null; bindings = null;
    }

    public State state() { return state; }
    public MapBindings bindings() { return bindings; }
    public boolean preMatch() { return state == State.PRE_MATCH; }
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

    /**
     * Create the match and enter PRE-MATCH. Claims nothing and loads nothing.
     *
     * This used to load a world immediately, which made the map decision
     * invisible: there was nowhere for a draft to happen because the map was
     * already chosen by the time anyone was asked.
     */
    public String open() {
        if (state == State.RUNNING) throw new IllegalStateException("A match is already running.");
        // A PRE_MATCH that already resolved its selection is holding a claim on
        // a map that was never played. Discarding it without giving the claim
        // back leaked one map per abandoned match: the entry stayed IN_USE for
        // the life of the server, and once the pool ran dry the next selection
        // silently fell back to the template.
        boolean released = state == State.PRE_MATCH && worldInstance.abandon();
        resetFields();
        state = State.PRE_MATCH;
        record = new MatchRecord();
        var pool = plugin.mapPool();
        return "Match created; PRE_MATCH. Add players and teams, then resolve the map"
                + " selection. "
                + (released ? "Released the previous PRE_MATCH's unplayed map. " : "")
                + (pool == null ? "" : pool.report());
    }

    /** The map options a draft could offer: broad classification only. */
    public List<String> options() {
        var pool = plugin.mapPool();
        if (pool == null || !pool.enabled()) return List.of();
        List<String> out = new ArrayList<>();
        for (var e : pool.entries())
            if (MapPool.READY.equals(e.state())) out.add(e.mapId());
        return out;
    }

    /**
     * TEST/DEBUG map resolution. Not the production draft rule.
     *
     * The real selection comes out of a ban/counterpick process over Map Type,
     * Scale and Resource Density that is explicitly undecided. This exists so
     * the architecture downstream of the decision -- claim, bind, play, retire --
     * can be exercised end to end before those rules exist, and it is named so
     * that "first READY map wins" cannot quietly become the answer.
     */
    public String resolveSelectionForTest(String mapId) throws IOException {
        if (state != State.PRE_MATCH)
            throw new IllegalStateException("Not in PRE_MATCH; run /moba match open first.");
        var pool = plugin.mapPool();
        if (pool != null && pool.enabled() && mapId == null && options().isEmpty())
            plugin.getLogger().warning("[match] pool enabled but empty; falling back to template");
        var claimed = worldInstance.claim(UUID.randomUUID().toString(), mapId);
        World w = worldInstance.load();
        w.setGameRule(GameRule.DO_IMMEDIATE_RESPAWN, true);
        bindings = MapBindings.of(claimed);
        loadHomelands(w);
        int renewables = plugin.resetRenewables();
        plugin.getLogger().info("[match] bound " + renewables + " renewable source(s)");
        bindObjectives(w);
        if (plugin.lair() != null) {
            plugin.lair().bind(w, bindings);
            plugin.getLogger().info("[match] " + plugin.lair().report());
        }
        if (plugin.worksites() != null) {
            int sites = plugin.worksites().bind(bindings, w);
            plugin.getLogger().info("[match] bound " + sites + " worksite(s)");
        }
        if (plugin.objectiveGlow() != null) {
            int boxes = plugin.objectiveGlow().rebuild(w);
            if (boxes > 0) plugin.getLogger().info("[match] " + plugin.objectiveGlow().report());
            // The tint marks the same volumes the glow plans, from the same
            // list, so the two cannot disagree about where an objective is.
            if (plugin.objectiveTint() != null)
                plugin.objectiveTint().apply(w, plugin.objectiveGlow().volumes(),
                        plugin.objectiveGlow().widestHalfWidth(), 12);
        }
        record.matchBound(claimed == null ? null : claimed.mapId(),
                claimed == null ? 0 : claimed.seed(), bindings);
        if (bindings != null) {
            record.prediction("lair_anchor", String.valueOf(bindings.lairAnchor(w)));
            record.prediction("worksites", String.valueOf(bindings.worksites(w).size()));
        }
        for (Team t : Team.values())
            record.prediction("fountain_" + t.lower(), String.valueOf(homelands.get(t)));
        // Certify what was BOUND, not where it came from.
        //
        // readiness.certify makes a map unable to reach READY with an
        // unmanifested Lair, and that was reported as closing the defect. It
        // did not: a match does not have to come from the pool. An empty pool
        // fell back to the configured template, whose `alpha.lair.site` is
        // EMPTY BY DESIGN, and produced exactly the UNCONFIGURED cadence the
        // pool contract exists to prevent -- in one step, by the path that
        // bypasses the contract.
        var problems = BindingContract.certify(plugin, w, bindings);
        if (!problems.isEmpty()) {
            for (var problem : problems) record.problem(problem.toString());
            if (!BindingContract.incompleteAllowed(plugin)) {
                worldInstance.abandon();   // never played; give the map back
                bindings = null;
                state = State.PRE_MATCH;
                throw new IllegalStateException(
                        "This realization cannot be played:"
                        + BindingContract.describe(problems)
                        + "\nNothing was bound. Run the foundry to stock the pool, or set "
                        + "alpha.pool.allowIncompleteBinding: true to test on an incomplete "
                        + "realization deliberately.");
            }
            plugin.getLogger().warning("[match] PLAYING AN INCOMPLETE REALIZATION because "
                    + "alpha.pool.allowIncompleteBinding is set:"
                    + BindingContract.describe(problems));
        }
        return "Selected " + (claimed != null ? "pool map " + claimed.mapId()
                                                + " (seed " + claimed.seed() + ")"
                                              : "the configured template")
                + "; world '" + w.getName() + "' bound. Homelands: " + homelands.keySet()
                + (bindings != null ? ". " + bindings.report() : "")
                + ". Then /moba match start.";
    }

    /**
     * Register all six defensive objectives against the generated map.
     *
     * Capacity comes from the structure that was actually authored, so nothing
     * maintains a second table of numbers that can drift away from the meshes.
     */
    private void bindObjectives(World w) {
        var objectives = plugin.teamObjectives();
        var capacity = plugin.defensiveCapacity();
        if (objectives != null) objectives.reset();
        if (capacity == null) return;
        capacity.reset();
        for (Team team : Team.values()) {
            for (TeamObjectives.Kind kind : TeamObjectives.Kind.values()) {
                Location at = bindings == null ? null : bindings.objective(w, team, kind.name().toLowerCase(java.util.Locale.ROOT));
                if (at != null && objectives != null) objectives.place(team, kind, at);
                capacity.register(team, kind, TeamObjectives.authoredBlocks(kind));
            }
        }
    }

    /**
     * Where each team spawns and respawns.
     *
     * The claimed realization answers first. Config is the fallback for the
     * Alpha template, not the default -- reading `alpha.homelands` on a
     * generated map would put both teams at coordinates from a different world.
     */
    private void loadHomelands(World w) {
        homelands.clear();
        for (Team t : Team.values()) {
            Location fountain = bindings == null ? null : bindings.fountain(w, t);
            if (fountain != null) { homelands.put(t, fountain); continue; }
            List<Integer> xz = plugin.getConfig().getIntegerList("alpha.homelands." + t.lower());
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

    /**
     * TEST/DEBUG start with no participants.
     *
     * The participant requirement is a real production rule -- a match without
     * players is not a match -- so this bypasses that ONE check and nothing
     * else, and is named so it cannot drift into the production path. It exists
     * because the cadence, the objectives and the Lair are all server-side, and
     * proving they run on a generated map should not require simulating humans.
     */
    public String startForTest() {
        testStart = true;
        try { return start(); } finally { testStart = false; }
    }

    private boolean testStart;

    public String start() {
        if (state == State.RUNNING) throw new IllegalStateException("Already running.");
        if (worldInstance.world() == null)
            throw new IllegalStateException("No world bound; create the match and resolve "
                    + "the map selection first.");
        if (participants.isEmpty() && !testStart)
            throw new IllegalStateException("No participants; add at least one player.");
        state = State.RUNNING; elapsed = 0; winner = null;   // ACTIVE PLAY
        World w = worldInstance.world();
        w.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
        // Vanilla regeneration needs food >= 18, which a Hunger Capacity of 9
        // made unreachable -- so it has never once fired, and HungerRegen was
        // written to be the regeneration rule instead. Uncapping hunger for the
        // fixed-length bar would switch vanilla's on for the first time, as a
        // side effect of a display change. Held off deliberately: the Fountain
        // and HungerRegen stay the ways health comes back until that is decided
        // on its own terms. Flip features.vitalsScaling.naturalRegeneration to
        // hand it to vanilla.
        w.setGameRule(GameRule.NATURAL_REGENERATION,
                plugin.getConfig().getBoolean("features.vitalsScaling.naturalRegeneration", false));
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
    /**
     * Where in the Fountain a player appears.
     *
     * The Fountain's bound coordinate is its CENTRE, and its centre is a solid
     * chiseled-quartz plinth four blocks tall. Spawning on the coordinate
     * therefore put players inside the plinth, where they suffocated, respawned
     * at the same coordinate, and suffocated again -- the Fountain is the
     * respawn anchor, so the loop had no exit.
     *
     * Players belong in the POOL, not on the plinth and not merely beside the
     * structure: standing in the Fountain's water is the reading canon wants of
     * reconstruction, and it is inside the thing that defines the spawn.
     *
     * The ring is searched outward from the plinth rather than at one fixed
     * offset, because terrain, later authoring or player construction can fill
     * any single cell. Falls back to the bound coordinate only if no passable
     * cell exists at all, which is a broken Fountain and should look like one
     * rather than be silently papered over somewhere else.
     */
    static Location fountainSpawn(Location centre) {
        if (centre == null) return null;
        World w = centre.getWorld();
        if (w == null) return centre;
        // Outside the radius-2 plinth, inside the radius-5 pool.
        for (int r = 3; r <= 5; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (dx * dx + dz * dz > r * r) continue;
                    if (dx * dx + dz * dz <= 4) continue;              // the plinth
                    Location cell = centre.clone().add(dx, 0, dz);
                    if (!cell.getBlock().isPassable()) continue;
                    if (!cell.clone().add(0, 1, 0).getBlock().isPassable()) continue;
                    Location at = cell.toCenterLocation();
                    at.setY(cell.getBlockY());
                    return at;
                }
            }
        }
        return centre;
    }

    private void spawn(Player p, Team team) {
        Location home = fountainSpawn(homelands.get(team));
        if (home != null) p.teleport(home);
        p.setGameMode(GameMode.SURVIVAL);
        var max = p.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH);
        p.setHealth(max == null ? 20.0 : max.getValue());
        int hunger = plugin.foodCeiling(p);
        p.setFoodLevel(hunger);
        p.setSaturation(hunger);
    }

    private void tick() { advance(1); }

    /**
     * Advance the counter, firing every boundary crossed exactly once.
     *
     * The ticker and {@link #skipMinutes} both come through here. They used to
     * be two copies of the same loop, which is the repeated defect in this
     * codebase -- two implementations of one idea -- and it had already drifted:
     * only the ticker carried the obsolete horizon branch, and that branch
     * incremented {@code elapsed} a second time, so a boundary landing on the
     * skipped tick was silently lost.
     */
    private void advance(long ticks) {
        for (long i = 0; i < ticks && state == State.RUNNING; i++) {
            elapsed++;
            if (MatchClock.isSunsetBoundary(elapsed)) onSunset(MatchClock.sunsetOrdinal(elapsed));
            else if (MatchClock.isSunriseBoundary(elapsed)) onSunrise();
        }
        World w = worldInstance.world();
        if (w != null) w.setTime(MatchClock.worldTime(elapsed));
    }

    /**
     * A sunset is an opportunity night, and which one it is decides what opens.
     *
     * The cadence alternates Worksite and Lair (OpportunityCadence): a Worksite
     * night activates sites of that tier and leaves the Lair's occupant alone,
     * and a Lair night installs the next boss and activates no Worksites. The
     * Lair is told the ordinal on every night regardless, because "leave the
     * occupant alone" is a decision its lifecycle has to make rather than one
     * made by not calling it -- that is what lets a surviving Giant persist
     * through Worksite II and still be replaced when Ghast's night arrives.
     */
    private void onSunset(int ordinal) {
        var stage = OpportunityCadence.atNight(ordinal);
        List<String> notes = new ArrayList<>();
        if (stage.tier() != null) {
            var opened = plugin.worksites().onOpportunityNight(stage.tier());
            notes.add(opened.isEmpty()
                    ? "Worksite " + stage.tier() + " (" + stage.tier().identity()
                      + "): no eligible site activated"
                    : "Worksite " + stage.tier() + " (" + stage.tier().identity() + ", "
                      + stage.tier().economicRole() + "): " + opened.size() + " activated "
                      + opened.stream().map(w -> w.id).toList());
        }
        if (plugin.lair() != null) {
            plugin.lair().onNight(ordinal);
            if (stage.boss() != null) notes.add("Lair: " + stage.boss() + " "
                    + plugin.lair().lifecycle().state());
            // The paired siege fires on the kill, in Lair.resolveVictory, not
            // here: comparing lastVictory across nights never fired, because
            // replacing an occupant does not change the recorded victory.
        }
        if (notes.isEmpty()) notes.add("no scheduled opportunity (post-Dragon cadence is OPEN)");
        record.night(ordinal, String.valueOf(stage),
                plugin.worksites() == null ? 0
                        : plugin.worksites().inState(Worksites.State.ACTIVATED).size(),
                plugin.lair() == null ? "none" : plugin.lair().lifecycle().state().name());
        announce("Night " + ordinal + " (" + MatchClock.minutes(elapsed) + "m, " + stage + "): "
                + String.join("; ", notes));
        plugin.getLogger().info("[match] night " + ordinal + " " + stage + " at "
                + MatchClock.minutes(elapsed) + "m: " + String.join("; ", notes));
    }

    /**
     * Sunrise.
     *
     * [HISTORICAL] This used to close the night's Worksites and announce how
     * many. Sunrise closure is superseded (objectives.md 17C): activation is
     * permanent, so sunrise takes nothing away and the announcement now says
     * so rather than reporting a zero it would once have reported as news.
     * Swarm reversion and infrastructure efficiency, which canon does put at
     * sunrise, are not implemented, so nothing is asserted about them here.
     */
    private void onSunrise() {
        announce("Sunrise (" + MatchClock.minutes(elapsed) + "m): "
                + plugin.worksites().inState(Worksites.State.ACTIVATED).size()
                + " Worksite(s) remain active — activation is permanent.");
    }

    /**
     * Advance the clock by whole minutes, firing every boundary crossed.
     *
     * This is how one person exercises the whole six-night cadence without
     * sitting through 110 minutes of it. It advances the same counter the
     * ticker advances through the same {@link #advance} path, so a skipped
     * night is a real night rather than a simulated one.
     */
    public String skipMinutes(int minutes) {
        if (!running()) throw new IllegalStateException("No match running.");
        if (minutes <= 0) throw new IllegalArgumentException("Minutes must be positive.");
        advance(minutes * 60L * 20L);
        return "Advanced to " + MatchClock.describe(elapsed) + ".";
    }

    /**
     * Apply one siege act to a generated objective.
     *
     * The routes are deliberately not separate systems: combat, structural and
     * signature all reduce the same Defensive Capacity, so a team that fights a
     * wave, breaks a wall and takes the signature target is conducting one
     * siege rather than choosing between three minigames.
     *
     * This is the admin/integration entry point. The player-facing paths --
     * killing a defender, breaking usable structure, stealing the Allay --
     * route to the same calls, which is the point: there is one state.
     */
    public String siege(Team team, String objectiveName, String route, int amount) {
        if (!running()) throw new IllegalStateException("No match running.");
        var capacity = plugin.defensiveCapacity();
        if (capacity == null) throw new IllegalStateException("No defensive capacity state.");
        TeamObjectives.Kind kind = switch (objectiveName.toLowerCase(java.util.Locale.ROOT)) {
            case "outpost", "pillager_outpost" -> TeamObjectives.Kind.PILLAGER_OUTPOST;
            case "bastion", "nether_bastion" -> TeamObjectives.Kind.NETHER_BASTION;
            case "spike", "end_spike" -> TeamObjectives.Kind.END_SPIKE;
            default -> throw new IllegalArgumentException("unknown objective " + objectiveName);
        };
        var objective = capacity.get(team, kind);
        if (objective == null)
            throw new IllegalStateException(team.lower() + " " + kind
                    + " is not bound on this map");
        boolean toppled = switch (route.toLowerCase(java.util.Locale.ROOT)) {
            case "combat" -> {
                boolean last = false;
                for (int i = 0; i < amount; i++) last = capacity.defenderKilled(team, kind);
                yield last;
            }
            case "structural" -> capacity.structureDestroyed(team, kind, amount);
            case "signature" -> capacity.signature(team, kind);
            case "lair" -> capacity.lairAssault(team, kind);
            default -> throw new IllegalArgumentException("unknown siege route " + route);
        };
        if (toppled && plugin.teamObjectives() != null)
            plugin.teamObjectives().recordValidatedToppling(team, kind);
        record.siege(team, kind, switch (route.toLowerCase(java.util.Locale.ROOT)) {
            case "combat" -> DefensiveCapacity.Source.COMBAT;
            case "structural" -> DefensiveCapacity.Source.STRUCTURAL;
            case "signature" -> DefensiveCapacity.Source.SIGNATURE;
            default -> DefensiveCapacity.Source.LAIR_ASSAULT;
        }, objective.remaining(), objective.initial, toppled);
        Location at = plugin.teamObjectives() == null ? null
                : plugin.teamObjectives().site(team, kind);
        String where = at == null ? "UNBOUND"
                : at.getBlockX() + "," + at.getBlockZ();
        announce("Siege (" + route + ") on " + team.lower() + " " + kind + " at " + where
                + ": " + String.format("%.0f", objective.remaining()) + "/"
                + String.format("%.0f", objective.initial)
                + (toppled ? " -- TOPPLED" : ""));
        return objective.toString();
    }

    /**
     * A defeated Lair monster performs its siege on the paired enemy objective.
     *
     * Not an abstract buff and not a separate objective-health model: the
     * monster does, at scale, what players already do, and it reduces the same
     * capacity. Because it acts on the objective's real current state, it can
     * finish one players have already weakened -- which is the setup the design
     * explicitly permits.
     */
    public String lairAssault(OpportunityCadence.Boss boss, Team victor) {
        if (victor == null) return "Lair victory unattributed; no siege performed.";
        var kind = TeamObjectives.pairedObjective(boss);
        Team defender = victor.other();
        String result = siege(defender, kind.name(), "lair", 1);
        record.lairContest(boss, victor, "assault on " + defender.lower() + " " + kind);
        announce(victor.lower() + " defeated the " + boss + "; it assaults "
                 + defender.lower() + " " + kind + ".");
        return result;
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
        record.ended(victor);
        var claimed = worldInstance.claimed();
        if (claimed != null) record.write(claimed.directory().resolve("matches"));
        // A played map never returns to READY: players changed it, and a pool
        // entry is only worth anything while it is pristine.
        worldInstance.release(victor == null ? "no-victor" : victor.lower() + "-wins");
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
        // The Lair holds a live entity in the world about to be discarded, so
        // its occupant is removed before the restore rather than orphaned in a
        // world nobody will load again.
        if (plugin.lair() != null) plugin.lair().reset();
        worldInstance.release("reset");
        if (plugin.teamObjectives() != null) plugin.teamObjectives().reset();
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
        // The same pool cell the initial spawn uses. This is the half that made
        // the plinth fatal rather than merely wrong: respawn is immediate and
        // the Fountain is the anchor, so a player who suffocated in the plinth
        // respawned inside it and suffocated again, with no way out.
        Location home = fountainSpawn(homelands.get(part.team));
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
        int night = MatchClock.sunsetOrdinal(elapsed);
        out.add("clock=" + MatchClock.describe(elapsed) + " night=" + night
                + " stage=" + OpportunityCadence.atNight(night)
                + " next=" + OpportunityCadence.atNight(night + 1));
        out.add("world: " + worldInstance.report());
        if (plugin.mapPool() != null) out.add(plugin.mapPool().report());
        if (plugin.lair() != null) out.add(plugin.lair().report());
        if (plugin.teamObjectives() != null) out.addAll(plugin.teamObjectives().report());
        if (plugin.defensiveCapacity() != null) out.addAll(plugin.defensiveCapacity().report());
        if (bindings != null) out.add(bindings.report());
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
