package com.minecraftmoba.plugin;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * The scenario bench: run a scripted situation with several real fake players on a launched scoop,
 * and report what held. The 2v2 skirmish and the engineered rule checks of
 * {@code docs/design/SCENARIO_HARNESS.md}.
 *
 * <h2>Real players, real match</h2>
 *
 * The roster are {@link NmsBodies}: real {@code Player} objects, enrolled, classed, levelled and put
 * on teams in the RUNNING match, so {@code TeamDamage}, {@code Stun}, {@code Capacity} and
 * {@code AbilityInputs} all run unchanged, with no branch in the game for "it is a bot". Casts go
 * through {@link BodyCaster}, the same door a player's keys use.
 *
 * <h2>The harness is tested before the scenarios are believed</h2>
 *
 * The timeline, the roster, windowed checks, the assertion judge, the act vocabulary and the
 * one-door teardown ({@link ScenarioRun}) are pure and unit-tested. A scenario is data
 * ({@code alpha.lab.scenarios}), validated when it is loaded. A check that would pass without its
 * rule running (the Root check without a walking control) is part of the shipped scenario.
 *
 * <h2>Teardown has one door</h2>
 *
 * A finished run, a thrown step, {@code /moba lab end} and the plugin stopping all end in
 * {@link ScenarioRun#teardown}, which despawns every body. {@link #abortAll} is called by the lab
 * BEFORE its world is unloaded.
 *
 * NOT VERIFIED beyond the live acceptance run at the time of writing.
 */
public final class ScenarioBench {
    private static final class Active {
        List<Scenario> library = List.of();
        int selected;
        ScenarioRun run;
        BukkitTask task;
        Snapshots snapshots;
        final Map<String, ScenarioRun.Report> reports = new LinkedHashMap<>();
        final List<BukkitTask> walkers = new ArrayList<>();
    }

    /** The start-of-tick state of every roster member, so a check can say "since tick N". */
    private static final class Snapshots {
        record Snap(double health, double[] pos) {}
        final Map<Long, Map<String, Snap>> byTick = new HashMap<>();
    }

    private final MobaPlugin plugin;
    private final Lab lab;
    private final BenchUi ui;
    private final BodyCaster caster;
    private final Map<UUID, Active> active = new HashMap<>();

    public ScenarioBench(MobaPlugin plugin, Lab lab) {
        this.plugin = plugin;
        this.lab = lab;
        this.caster = new BodyCaster(plugin);
        this.ui = new BenchUi(plugin, new BenchUi.Spec(
                "scenario", ScenarioBenchMenu::menu,
                p -> ScenarioBenchMenu.gate(state(p)),
                (p, verb) -> ScenarioBenchMenu.refusal(verb, state(p)),
                new DeckLayout(new DeckLayout.Box(0, 0, 0, -1, -1, -1), List.of()),
                plugin.getConfig().getString("alpha.lab.instanceWorldName", "moba_lab_match"),
                this::runVerb, this::stop, "Leave bench", "Stops any running scenario and switches the bench off.",
                p -> plugin.enrolled(p) && plugin.inputs().active(p)));
    }

    private ScenarioBenchMenu.State state(Player p) {
        var a = active.get(p.getUniqueId());
        return new ScenarioBenchMenu.State(a != null, a != null && a.run != null && a.run.running());
    }

    private NmsBodies bodies() { return lab.combat().bodies(); }

    // ---- commands -----------------------------------------------------------------------------

    /** Handle {@code /moba lab scenario ...}. */
    public void command(Player p, String[] args) {
        String verb = args.length > 2 ? args[2].toLowerCase(Locale.ROOT) : "list";
        var a = active.get(p.getUniqueId());
        if (a == null) {
            start(p);
            a = active.get(p.getUniqueId());
            if (verb.equals("start") || verb.equals("on") || verb.equals("list")) return;
        }
        // A scenario id runs that scenario.
        for (int i = 0; i < a.library.size(); i++)
            if (a.library.get(i).id().equals(verb)) { a.selected = i; runVerb(p, "run"); return; }
        runVerb(p, verb);
    }

    void runVerb(Player p, String verb) {
        var a = active.get(p.getUniqueId());
        if (a == null) return;
        String refusal = ScenarioBenchMenu.refusal(verb, state(p));
        if (refusal != null && !verb.equals("off")) { p.sendMessage(ChatColor.RED + refusal); return; }
        try {
            switch (verb) {
                case "scenario", "next" -> {
                    a.selected = (a.selected + 1) % a.library.size();
                    var s = a.library.get(a.selected);
                    p.sendMessage(ChatColor.GREEN + "Selected " + s.id() + ": " + s.description() + " (" + s.roster().size() + " bodies, "
                            + s.checkCount() + " checks)");
                }
                case "run" -> run(p, a);
                case "stop" -> { a.run.abort("stopped by the tester"); finish(p, a); }
                case "report", "status" -> reportLines(p).forEach(p::sendMessage);
                case "off", "leave" -> stop(p);
                default -> p.sendMessage("In the bench: /moba lab scenario <id> | next | run | stop | report | off. Scenarios: "
                        + a.library.stream().map(Scenario::id).toList());
            }
        } catch (IllegalStateException | IllegalArgumentException ex) {
            p.sendMessage(ChatColor.RED + ex.getMessage());
        }
    }

    // ---- lifecycle ------------------------------------------------------------------------------

    private void start(Player p) {
        var world = lab.scoopWorld();
        if (world == null || !p.getWorld().equals(world) || !plugin.match().running())
            throw new IllegalStateException("The scenario bench runs on a launched scoop. /moba lab start, choose a class and a scoop, then Launch test.");
        var a = new Active();
        a.library = library();
        if (a.library.isEmpty()) throw new IllegalStateException("No scenarios are configured (alpha.lab.scenarios).");
        active.put(p.getUniqueId(), a);
        ui.engage(p, world);
        // Verifying that bodies generate real movement is asynchronous; start it now so it is ready by the first run.
        // Verified where the tester stands, on the scoop's real ground (see NmsBodies.prepare).
        // Not at the Fountain: the diagnosis of an earlier failure showed a non-participant standing there had a
        // teleport pending on every check, so its movement was dropped and bodies read as "not connected".
        var here = p.getLocation();
        int px = here.getBlockX() + 6, pz = here.getBlockZ() + 10;
        bodies().prepare(new Location(world, px + 0.5, world.getHighestBlockYAt(px, pz) + 1, pz + 0.5), null);
        p.sendMessage(ChatColor.GREEN + "Scenario bench on. " + a.library.size() + " scenarios: " + a.library.stream().map(Scenario::id).toList()
                + ". Pick one and run it; Leave is in your inventory.");
        p.sendMessage(ChatColor.GRAY + "The roster are real fake players on teams in this running match. A scenario's coordinates are offsets from its anchor on this map.");
    }

    /** Load and VALIDATE every configured scenario, refusing rather than defaulting. */
    private List<Scenario> library() {
        var out = new ArrayList<Scenario>();
        var section = plugin.getConfig().getConfigurationSection("alpha.lab.scenarios");
        if (section == null) return out;
        for (String id : section.getKeys(false)) {
            var s = Scenario.load(id, section.getConfigurationSection(id));
            String problem = ScenarioActs.problem(s);
            if (problem != null) throw new IllegalStateException("Scenario " + id + " is malformed: " + problem);
            out.add(s);
        }
        return out;
    }

    private void run(Player p, Active a) {
        var s = a.library.get(a.selected);
        var nms = bodies();
        if (s.requiresConnectedBodies() && !nms.verified())
            throw new IllegalStateException("Still verifying that bodies generate real movement. Try again in a few seconds.");
        var anchor = anchor(s);
        var world = lab.scoopWorld();
        a.snapshots = new Snapshots();
        // Observe from the anchor's side, away from the action.
        p.teleport(new Location(world, anchor.getX(), anchor.getY() + 1, anchor.getZ() - 6, 0f, 10f));
        var run = new ScenarioRun(s, nms, (check, body) -> ScenarioJudge.judge(check, facts(a)),
                m -> place(s, m, anchor, world));
        run.onAct((actor, act) -> act(a, actor, act));
        a.run = run;
        run.start();
        if (run.report().aborted()) { finish(p, a); return; }
        // Joining a team in a running match teleports a body to its Fountain, which discards the place it
        // was spawned at, so each body is put back where the scenario says. (Its pending teleport is
        // accepted by NmsBodies on the next tick.)
        for (var m : s.roster()) {
            var body = run.body(m.id());
            if (body != null) body.player().teleport(place(s, m, anchor, world));
        }
        p.sendMessage(ChatColor.GREEN + "Running " + s.id() + ": " + s.description());
        a.task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!p.isOnline()) { run.abort("the tester left"); finish(p, a); return; }
            snapshot(a, run.tick() + 1);
            run.advance();
            if (!run.running()) finish(p, a);
        }, 1L, 1L);
    }

    private void finish(Player p, Active a) {
        if (a.task != null) { a.task.cancel(); a.task = null; }
        a.walkers.forEach(BukkitTask::cancel); a.walkers.clear();
        var report = a.run.report();
        a.reports.put(report.scenarioId(), report);
        plugin.getLogger().info("[scenario] " + report.summary().replace("\n", " | "));
        if (p.isOnline()) {
            p.sendMessage((report.passed() ? ChatColor.GREEN : ChatColor.RED) + report.summary());
        }
    }

    public void stop(Player p) {
        var a = active.remove(p.getUniqueId());
        ui.release(p);
        if (a == null) return;
        if (a.run != null && a.run.running()) { a.run.abort("the bench was switched off"); }
        if (a.task != null) a.task.cancel();
        a.walkers.forEach(BukkitTask::cancel);
        if (p.isOnline()) p.sendMessage("Scenario bench off.");
    }

    /** Despawn every running scenario's bodies. The lab calls this BEFORE it unloads its world. */
    public void abortAll(String because) {
        for (var a : active.values()) {
            if (a.run != null && a.run.running()) a.run.abort(because);
            if (a.task != null) { a.task.cancel(); a.task = null; }
            a.walkers.forEach(BukkitTask::cancel); a.walkers.clear();
        }
    }

    public void close() {
        abortAll("the plugin stopped");
        for (var id : new ArrayList<>(active.keySet())) { Player p = Bukkit.getPlayer(id); if (p != null) stop(p); }
        active.clear();
        ui.close();
    }

    public boolean occupies(Player p) { return active.containsKey(p.getUniqueId()); }
    public ScenarioRun.Report lastReport(Player p, String id) { var a = active.get(p.getUniqueId()); return a == null ? null : a.reports.get(id); }
    public ScenarioRun runOf(Player p) { var a = active.get(p.getUniqueId()); return a == null ? null : a.run; }
    public List<Scenario> scenarios(Player p) { var a = active.get(p.getUniqueId()); return a == null ? List.of() : a.library; }

    // ---- placement ---------------------------------------------------------------------------------

    private Location anchor(Scenario s) {
        var world = lab.scoopWorld();
        if (s.anchor().equals(Scenario.ABSOLUTE)) return new Location(world, 0, 0, 0);
        var entry = lab.maps().select(lab.activeMapId());
        var b = MapBindings.of(entry);
        if (b == null) throw new IllegalStateException("This scoop has no bindings to anchor a scenario to.");
        Location at = switch (s.anchor()) {
            case "fountain_north" -> b.fountain(world, Team.NORTH);
            case "fountain_south" -> b.fountain(world, Team.SOUTH);
            case "lair" -> b.lairAnchor(world);
            default -> null;
        };
        if (at == null) throw new IllegalStateException("The anchor '" + s.anchor() + "' is not bound on this map.");
        return at;
    }

    private Location place(Scenario s, Scenario.Member m, Location anchor, org.bukkit.World world) {
        boolean absolute = s.anchor().equals(Scenario.ABSOLUTE);
        double[] at = ScenarioPlacement.resolve(m, absolute, anchor.getBlockX(), anchor.getBlockZ(),
                (x, z) -> { world.getChunkAt(x >> 4, z >> 4).load(true); return world.getHighestBlockYAt(x, z); });
        return new Location(world, at[0], at[1], at[2], 0f, 0f);
    }

    // ---- facts ---------------------------------------------------------------------------------------

    private void snapshot(Active a, long tick) {
        var now = new HashMap<String, Snapshots.Snap>();
        for (var m : a.run.scenario().roster()) {
            var b = a.run.body(m.id());
            if (b == null || b.player() == null) continue;
            var l = b.player().getLocation();
            now.put(m.id(), new Snapshots.Snap(b.player().getHealth(), new double[]{l.getX(), l.getY(), l.getZ()}));
        }
        a.snapshots.byTick.put(tick, now);
    }

    private ScenarioFacts facts(Active a) {
        return new ScenarioFacts() {
            private Player p(String id) { return a.run.body(id).player(); }
            @Override public double health(String id) { return p(id).getHealth(); }
            @Override public double healthAt(String id, long tick) {
                var m = a.snapshots.byTick.get(tick); var s = m == null ? null : m.get(id);
                return s == null ? Double.NaN : s.health();
            }
            @Override public double maxHealth(String id) {
                var attr = p(id).getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH);
                return attr == null ? 20.0 : attr.getValue();
            }
            @Override public boolean alive(String id) { return !p(id).isDead() && p(id).isOnline(); }
            @Override public double[] position(String id) { var l = p(id).getLocation(); return new double[]{l.getX(), l.getY(), l.getZ()}; }
            @Override public double[] positionAt(String id, long tick) {
                var m = a.snapshots.byTick.get(tick); var s = m == null ? null : m.get(id);
                return s == null ? null : s.pos();
            }
            @Override public boolean stunned(String id) { return plugin.stun() != null && plugin.stun().isStunned(p(id)); }
            @Override public long stunRemaining(String id) { return plugin.stun() == null ? 0 : plugin.stun().remaining(p(id)); }
            @Override public boolean rooted(String id) { return plugin.toolboxStatuses() != null && plugin.toolboxStatuses().isRooted(p(id)); }
            @Override public int executions(String id, String abilityId) { return plugin.inputs().executions(p(id), abilityId); }
        };
    }

    // ---- acts -------------------------------------------------------------------------------------------

    private void act(Active a, Bodies.Body actor, Scenario.Act act) {
        var args = act.args();
        Player me = actor.player();
        switch (act.verb()) {
            case "hit" -> {
                Player victim = a.run.body(args.get("target")).player();
                double before = victim.getHealth();
                victim.damage(Double.parseDouble(args.get("amount")), me);
                // Logged, so a hit that did nothing can be told from a check that was wrong.
                plugin.getLogger().info("[scenario] " + actor.id() + " hit " + args.get("target") + ": health "
                        + String.format(Locale.ROOT, "%.2f -> %.2f", before, victim.getHealth())
                        + " (invulnerable=" + victim.isInvulnerable() + ", immunityTicks=" + victim.getNoDamageTicks() + ")");
            }
            case "explode" -> {
                Location at = a.run.body(args.get("at")).player().getLocation();
                at.getWorld().createExplosion(at, Float.parseFloat(args.get("power")), false, false, me);
            }
            case "stun" -> plugin.stun().stun(a.run.body(args.get("target")).player(), Long.parseLong(args.get("ticks")));
            case "root" -> plugin.toolboxStatuses().root(a.run.body(args.get("target")).player());
            case "heal" -> {
                var attr = me.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH);
                me.setHealth(attr == null ? 20.0 : attr.getValue());
            }
            case "look" -> { var f = facing(me, a.run.body(args.get("at")).player()); bodies().look(actor, f.yaw(), f.pitch()); }
            case "cast" -> {
                var aimed = facing(me, a.run.body(args.get("at")).player());
                // `level: true` aims along the ground: an ability that lands a fixed distance ahead (Drill
                // Rush's emerge) fails with NO EXIT when aimed slightly downward into the floor.
                var f = "true".equals(args.get("level")) ? new CastAim.Facing(aimed.yaw(), 0f) : aimed;
                var r = caster.cast(me, args.get("slot"), f, 0);
                plugin.getLogger().info("[scenario] " + actor.id() + " cast " + args.get("slot") + ": " + r.note());
            }
            case "walk" -> walk(a, actor, args.get("direction"), Integer.parseInt(args.get("ticks")));
            default -> throw new IllegalStateException("unhandled act " + act.verb());
        }
    }

    private static CastAim.Facing facing(Player from, Player to) {
        var f = from.getEyeLocation(); var t = to.getLocation().add(0, to.getHeight() / 2, 0);
        return CastAim.toward(f.getX(), f.getY(), f.getZ(), t.getX(), t.getY(), t.getZ());
    }

    /** The body reports walking, a step a tick, from wherever the server currently has it. */
    private void walk(Active a, Bodies.Body actor, String direction, int ticks) {
        double[] step = ScenarioActs.step(direction);
        bodies().look(actor, ScenarioActs.yaw(direction), 0f);
        int[] left = {ticks};
        var task = new BukkitTask[1];
        task[0] = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!actor.player().isOnline() || left[0]-- <= 0) { task[0].cancel(); return; }
            var l = actor.player().getLocation();
            bodies().moveTo(actor, l.getX() + step[0] * 0.25, l.getY(), l.getZ() + step[1] * 0.25);
        }, 1L, 1L);
        a.walkers.add(task[0]);
    }

    // ---- the report ---------------------------------------------------------------------------------------

    public List<String> reportLines(Player p) {
        var a = active.get(p.getUniqueId());
        var out = new ArrayList<String>();
        if (a == null) return out;
        out.add(ChatColor.GOLD + "Scenario bench: " + a.library.size() + " scenarios, selected " + a.library.get(a.selected).id()
                + (a.run != null && a.run.running() ? " (RUNNING, tick " + a.run.tick() + ")" : ""));
        for (var s : a.library) {
            var r = a.reports.get(s.id());
            out.add("  " + s.id() + ": " + s.description() + " [" + s.roster().size() + " bodies, " + s.checkCount() + " checks] "
                    + (r == null ? "not run" : r.passed() ? ChatColor.GREEN + "PASSED" : ChatColor.RED + (r.aborted() ? "ABORTED" : "FAILED")));
        }
        for (var r : a.reports.values()) if (!r.passed()) out.add(ChatColor.RED + r.summary());
        return out;
    }
}
