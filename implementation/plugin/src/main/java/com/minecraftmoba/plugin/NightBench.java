package com.minecraftmoba.plugin;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * The night and objective bench: jump a launched scoop to night N and watch what the match does,
 * then besiege an objective by each of the four routes.
 *
 * It exists because the six-night cadence (which Worksite tier opens, when the Lair installs which
 * boss, what is left standing) is the part of a match a single person cannot otherwise see without
 * sitting through 110 minutes, and the siege model (four routes reducing one capacity) had only
 * ever been exercised through unit tests of its arithmetic.
 *
 * <h2>It drives the real match</h2>
 *
 * A night jump is {@link Match#skipTicks}, the same path the ticker takes, so a skipped night is a
 * real night: {@code onSunset} fires, Worksites activate, the Lair is told the ordinal. A siege act
 * is {@link Match#siege}, the admin and integration entry point the player-facing paths route to.
 * Nothing is simulated beside them, and the report reads the runtime's own state next to the
 * cadence's plan so a disagreement between the two is visible.
 *
 * <h2>What it does not do</h2>
 *
 * It does not disable a Fountain: that is irreversible for the match and can end it. The amounts of
 * each siege route are bench conveniences ({@link SiegeTarget#ROUTES}), not tuning.
 *
 * NOT VERIFIED beyond the live acceptance run at the time of writing.
 */
public final class NightBench {
    private static final class Active {
        SiegeTarget.Target target = SiegeTarget.next(null);
        String last = "nothing yet";
    }

    private final MobaPlugin plugin;
    private final Lab lab;
    private final BenchUi ui;
    private final Map<UUID, Active> active = new HashMap<>();

    public NightBench(MobaPlugin plugin, Lab lab) {
        this.plugin = plugin;
        this.lab = lab;
        this.ui = new BenchUi(plugin, new BenchUi.Spec(
                "night", NightBenchMenu::menu,
                p -> NightBenchMenu.gate(state(p)),
                (p, verb) -> NightBenchMenu.refusal(verb, state(p)),
                new DeckLayout(new DeckLayout.Box(0, 0, 0, -1, -1, -1), List.of()),
                plugin.getConfig().getString("alpha.lab.instanceWorldName", "moba_lab_match"),
                this::runVerb, this::stop, "Leave bench", "Switches the night bench off and gives you your own hotbar back.",
                p -> plugin.enrolled(p) && plugin.inputs().active(p)));
    }

    private NightBenchMenu.State state(Player p) {
        var a = active.get(p.getUniqueId());
        if (a == null) return new NightBenchMenu.State(false, 0, false, false);
        var o = objective(a.target);
        return new NightBenchMenu.State(true, NightTimeline.current(plugin.match().elapsedTicks()), o != null,
                o != null && o.state() == DefensiveCapacity.State.TOPPLED);
    }

    private DefensiveCapacity.Objective objective(SiegeTarget.Target t) {
        var capacity = plugin.defensiveCapacity();
        return capacity == null ? null : capacity.get(t.team(), t.kind());
    }

    // ---- commands -----------------------------------------------------------------------------

    /** Handle {@code /moba lab night ...}. */
    public void command(Player p, String[] args) {
        String verb = args.length > 2 ? args[2].toLowerCase(Locale.ROOT) : "start";
        if (!active.containsKey(p.getUniqueId())) {
            if (verb.equals("start") || verb.equals("on")) start(p);
            else p.sendMessage("/moba lab night");
            return;
        }
        if (verb.equals("night") && args.length > 3) verb = "night." + args[3];
        else if (verb.equals("siege") && args.length > 3) verb = "siege." + args[3].toLowerCase(Locale.ROOT);
        else if (verb.matches("[1-6]")) verb = "night." + verb;
        runVerb(p, verb);
    }

    void runVerb(Player p, String verb) {
        var a = active.get(p.getUniqueId());
        if (a == null) return;
        String refusal = NightBenchMenu.refusal(verb, state(p));
        if (refusal != null && !verb.equals("off")) { p.sendMessage(ChatColor.RED + refusal); return; }
        try {
            if (verb.startsWith("night.")) jump(p, a, Integer.parseInt(verb.substring(6)));
            else if (verb.startsWith("siege.")) siege(p, a, verb.substring(6));
            else switch (verb) {
                case "target" -> {
                    a.target = SiegeTarget.next(a.target);
                    var o = objective(a.target);
                    p.sendMessage(ChatColor.GREEN + "Target: " + a.target.label() + (o == null ? " (not bound on this map)" : " " + o));
                }
                case "report", "status" -> reportLines(p).forEach(p::sendMessage);
                case "off", "leave", "stop" -> stop(p);
                default -> p.sendMessage("In the bench: /moba lab night <1-6> | target | siege combat|structural|signature|lair | report | off");
            }
        } catch (IllegalStateException | IllegalArgumentException ex) {
            p.sendMessage(ChatColor.RED + ex.getMessage());
        }
    }

    private void jump(Player p, Active a, int night) {
        var match = plugin.match();
        int worksitesBefore = plugin.worksites() == null ? 0 : plugin.worksites().inState(Worksites.State.ACTIVATED).size();
        long ticks = LabTime.ticksUntilNight(match.elapsedTicks(), night);
        String result = match.skipTicks(ticks);
        int worksitesAfter = plugin.worksites() == null ? 0 : plugin.worksites().inState(Worksites.State.ACTIVATED).size();
        var row = NightTimeline.rows(night).get(night - 1);
        p.sendMessage(ChatColor.GREEN + result + " Night " + night + " plan: " + row.what() + ".");
        p.sendMessage("  Worksites activated: " + worksitesBefore + " -> " + worksitesAfter
                + (plugin.lair() == null ? "" : "; " + plugin.lair().report()));
    }

    private void siege(Player p, Active a, String routeId) {
        var route = SiegeTarget.route(routeId);
        if (route == null) throw new IllegalArgumentException("Unknown route " + routeId + ". Routes: combat, structural, signature, lair.");
        var result = plugin.match().siege(a.target.team(), a.target.objectiveName(), route.match(), route.amount());
        a.last = routeId + " on " + a.target.label() + ": " + result;
        p.sendMessage(ChatColor.GREEN + "Siege (" + route.describes() + "): " + result);
    }

    private void start(Player p) {
        var world = lab.scoopWorld();
        if (world == null || !p.getWorld().equals(world) || !plugin.match().running())
            throw new IllegalStateException("The night bench drives a launched scoop. /moba lab start, choose a class and a scoop, then Launch test.");
        active.put(p.getUniqueId(), new Active());
        ui.engage(p, world);
        p.sendMessage(ChatColor.GREEN + "Night bench on. The clock is at " + MatchClock.describe(plugin.match().elapsedTicks())
                + ". Jump to a night, pick an objective, and besiege it; Leave is in your inventory.");
        p.sendMessage(ChatColor.GRAY + "A night jump is the match's own clock, so Worksites and the Lair react for real. "
                + "It only runs forward. Fountains are not touched: disabling one is irreversible and can end the match.");
    }

    public void stop(Player p) {
        var a = active.remove(p.getUniqueId());
        ui.release(p);
        if (a != null && p.isOnline()) p.sendMessage("Night bench off.");
    }

    public void close() {
        for (var id : new ArrayList<>(active.keySet())) { Player p = Bukkit.getPlayer(id); if (p != null) stop(p); }
        active.clear();
        ui.close();
    }

    public boolean occupies(Player p) { return active.containsKey(p.getUniqueId()); }
    public SiegeTarget.Target target(Player p) { var a = active.get(p.getUniqueId()); return a == null ? null : a.target; }

    // ---- the report ---------------------------------------------------------------------------

    /** The state as lines: the plan beside what the runtime actually holds. */
    public List<String> reportLines(Player p) {
        var a = active.get(p.getUniqueId());
        var out = new ArrayList<String>();
        if (a == null) return out;
        var match = plugin.match();
        out.add(ChatColor.GOLD + "Night bench: clock " + MatchClock.describe(match.elapsedTicks())
                + (match.labPaused() ? " [PAUSED]" : " [running]"));
        out.add("The plan (OpportunityCadence), '>' marks the night the clock is in:");
        NightTimeline.describe(match.elapsedTicks()).forEach(l -> out.add(l));
        out.add("Worksites, as the runtime holds them:");
        if (plugin.worksites() == null) out.add("  none bound");
        else plugin.worksites().report().forEach(l -> out.add("  " + l));
        out.add("Lair: " + (plugin.lair() == null ? "none bound" : plugin.lair().report()));
        out.add("Objectives (defensive capacity):");
        var capacity = plugin.defensiveCapacity();
        if (capacity == null || capacity.all().isEmpty()) out.add("  none registered on this map");
        else for (var o : capacity.all()) out.add("  " + o + " by " + o.contributions());
        out.add("Selected target: " + a.target.label() + "; last siege: " + a.last);
        out.add(ChatColor.GRAY + "Routes (bench amounts, not tuning): " + String.join("; ",
                SiegeTarget.ROUTES.stream().map(r -> r.id() + " = " + r.describes()).toList()));
        return out;
    }
}
