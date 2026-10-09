package com.minecraftmoba.plugin;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Difficulty;
import org.bukkit.GameMode;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.block.Biome;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.generator.WorldInfo;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * The opportunity bench: watch a regenerative opportunity manifest, resolve and recover,
 * see WHY a manifestation can or cannot happen, and force one.
 *
 * It exists because the regenerative systems (Herd, Crop Patch, Swarm) are lifecycles
 * whose interesting properties are transitions, and the part that touches the world --
 * choosing a locus, spawning, harvest detection, recovery on the world's own clock -- had
 * only ever been run through unit tests of its pure core. This puts the real
 * {@link Renewables} runtime in front of a tester, on a platform with plains and
 * mountain biomes so a time- and biome-gated swarm can both manifest and be refused.
 *
 * <h2>What it drives, and what it does not</h2>
 *
 * It registers real sources with {@link Renewables#createRuntime} and uses the runtime's
 * own {@code manifest}, harvest listeners and lifecycle ticker: nothing is simulated
 * beside them. The two things it does for the tester are shortcuts the real game cannot
 * offer: {@code skip} advances the opportunity's recovery progress to complete (via
 * {@link Opportunity#tickRecovery}, at the current rate, so the world's day or night still
 * decides how long that WOULD have taken, which the report states), and {@code base}
 * builds over the region to show Development displacing a manifestation.
 *
 * NOT VERIFIED beyond the live acceptance run at the time of writing.
 */
public final class OpportunityBench {
    static final String WORLD = "moba_opportunity";
    /** Particle sites shown at most. */
    private static final int MAX_SITES = 120;

    /** Plains in the west, a mountain biome from {@link OpportunityPlots#MOUNTAIN_FROM_X} east. */
    private static final class Biomes extends BiomeProvider {
        @Override public Biome getBiome(WorldInfo info, int x, int y, int z) {
            return x >= OpportunityPlots.MOUNTAIN_FROM_X ? Biome.WINDSWEPT_HILLS : Biome.PLAINS;
        }
        @Override public List<Biome> getBiomes(WorldInfo info) { return List.of(Biome.PLAINS, Biome.WINDSWEPT_HILLS); }
    }

    private static final class Active {
        int selected;
        /** Plot id to the source registered for it. */
        final Map<String, Renewables.Source> sources = new HashMap<>();
        final Map<String, Boolean> built = new HashMap<>();
        BukkitTask sites;
    }

    private final MobaPlugin plugin;
    private final Lab lab;
    private final BenchUi ui;
    private final Map<UUID, Active> active = new HashMap<>();
    private World world;
    private boolean chunksHeld;
    /** A typed {@code spawn <radius>} for one spawn only: lets a tester reproduce the radius/region mismatch. */
    private int radiusOverride;

    public OpportunityBench(MobaPlugin plugin, Lab lab) {
        this.plugin = plugin;
        this.lab = lab;
        this.ui = new BenchUi(plugin, new BenchUi.Spec(
                "opportunity", OpportunityBenchMenu::menu,
                p -> OpportunityBenchMenu.gate(state(p)),
                (p, verb) -> OpportunityBenchMenu.refusal(verb, state(p)),
                OpportunityBenchMenu.deck(), WORLD, this::runVerb, this::leave,
                "Leave bench", "Leaves the opportunity bench and returns to the lab room."));
    }

    private OpportunityBenchMenu.State state(Player p) {
        var a = active.get(p.getUniqueId());
        if (a == null) return new OpportunityBenchMenu.State(false, null, 0);
        var s = a.sources.get(plot(a).id());
        return new OpportunityBenchMenu.State(true, s == null ? null : s.opportunity().state(),
                s == null ? 0 : s.opportunity().remaining());
    }

    private OpportunityPlots.Plot plot(Active a) { return OpportunityPlots.PLOTS.get(a.selected); }

    /**
     * Every member of a manifestation, wherever it has wandered.
     *
     * {@link Renewables#membersOf} only scans the source's radius cube, so a member standing
     * outside it (placed at a region edge, or wandered) is invisible to it; the bench must see
     * all of them to harvest, count and clean up honestly.
     */
    private List<org.bukkit.entity.Entity> members(Renewables.Source s) {
        var out = new ArrayList<org.bukkit.entity.Entity>();
        for (var e : world.getEntities()) if (plugin.renewables().isMember(e, s)) out.add(e);
        return out;
    }

    // ---- commands -----------------------------------------------------------------

    /** Handle {@code /moba lab opportunity ...}; {@code args} is the full argument array. */
    public void command(Player p, String[] args) {
        String verb = args.length > 2 ? args[2].toLowerCase(Locale.ROOT) : "start";
        if (!active.containsKey(p.getUniqueId())) {
            if (verb.equals("start") || verb.equals("enter")) enter(p);
            else p.sendMessage("/moba lab opportunity start");
            return;
        }
        if (verb.equals("harvest") && args.length > 3) verb = "harvest." + args[3].toLowerCase(Locale.ROOT);
        if (verb.equals("spawn") && args.length > 3) { radiusOverride = Integer.parseInt(args[3]); }
        runVerb(p, verb);
        radiusOverride = 0;
    }

    void runVerb(Player p, String verb) {
        var a = active.get(p.getUniqueId());
        if (a == null) return;
        String refusal = OpportunityBenchMenu.refusal(verb, state(p));
        if (refusal != null && !verb.equals("leave")) { p.sendMessage(ChatColor.RED + refusal); return; }
        switch (verb) {
            case "plot" -> { a.selected = (a.selected + 1) % OpportunityPlots.PLOTS.size(); p.sendMessage(ChatColor.GREEN + "Selected: " + plot(a).label()); }
            case "spawn" -> spawn(p, a);
            case "skip" -> skip(p, a);
            case "manifest" -> manifest(p, a);
            case "harvest.one" -> harvest(p, a, false);
            case "harvest.all" -> harvest(p, a, true);
            case "base" -> base(p, a);
            case "time" -> time(p);
            case "show" -> showSites(p, a);
            case "report", "status", "log" -> reportLines(p).forEach(p::sendMessage);
            case "leave", "end" -> leave(p);
            default -> p.sendMessage("In the bench: /moba lab opportunity plot | spawn | skip | manifest | harvest one|all | base | time | show | report | leave");
        }
    }

    // ---- verbs ------------------------------------------------------------------------

    private void spawn(Player p, Active a) {
        var plot = plot(a);
        discard(a, plot);
        String id = "bench_" + plot.id() + "_" + Long.toHexString(System.nanoTime() & 0xFFFFFFL);
        var s = plugin.renewables().createRuntime(id, plot.kind(), world, plot.x(), OpportunityPlots.FLOOR_Y, plot.z(),
                radius(), plot.capacity(), 200L);
        // The migrated default region is 48 blocks each way, which would swallow the platform.
        s.region = plot.region();
        if (plot.swarm()) s.table = SwarmDefinitions.table(List.of("mountain_ravager"));
        a.sources.put(plot.id(), s);
        a.built.put(plot.id(), false);
        p.sendMessage(ChatColor.GREEN + "Spawned " + plot.label() + ". It starts RECOVERING (the first manifestation waits longer than later ones): "
                + "skip to recovered, or wait.");
    }

    /**
     * The source radius: the region's half-span plus room for a herd's spread, so membership
     * (judged by this cube) covers every site eligibility (judged by the region) can choose.
     * The Alpha sources do not have this: radius 20 against a migrated region half-span of 48.
     */
    private int radius() { return radiusOverride > 0 ? radiusOverride : OpportunityPlots.HALF + 4; }

    private void skip(Player p, Active a) {
        var s = a.sources.get(plot(a).id());
        var rates = plugin.renewables().ratesFor(s);
        boolean night = WorldTerrain.isNight(world);
        long ticks = Recovery.remainingTicks(s.opportunity().recoveryProgress(), night, rates);
        s.opportunity().tickRecovery(ticks + 1, night, rates);
        p.sendMessage(ChatColor.GREEN + String.format(Locale.ROOT,
                "Recovery advanced by %d ticks (about %.0f s at the current %s rate). The lifecycle ticker manifests it within a second if a site is viable.",
                ticks, ticks / 20.0, night ? "night" : "day"));
    }

    private void manifest(Player p, Active a) {
        var s = a.sources.get(plot(a).id());
        int made = plugin.renewables().manifest(s);
        if (made > 0) p.sendMessage(ChatColor.GREEN + "Manifested " + made + " at " + s.opportunity().locus() + ".");
        else p.sendMessage(ChatColor.YELLOW + "Nothing manifested: " + EligibilityReport.describe(eligibility(s)).get(0));
    }

    private void harvest(Player p, Active a, boolean all) {
        var plot = plot(a);
        var s = a.sources.get(plot.id());
        int taken = 0;
        int guard = 64;
        do {
            if (s.opportunity().state() != Opportunity.State.MANIFESTED || s.opportunity().remaining() <= 0 || guard-- <= 0) break;
            if (!takeOne(p, s, plot)) break;
            taken++;
        } while (all);
        p.sendMessage((taken > 0 ? ChatColor.GREEN : ChatColor.YELLOW) + "Took " + taken + "; now " + s.opportunity());
    }

    /** One real harvest: a kill credited to the tester, or a broken crop block, through the runtime's own listeners. */
    private boolean takeOne(Player p, Renewables.Source s, OpportunityPlots.Plot plot) {
        var kind = RenewableKinds.require(plot.kind());
        if (kind.type() == Renewables.Type.CROP) {
            // A patch spreads past its locus, so crops can stand outside the region: scan the source's cube.
            for (int x = s.x() - s.radius(); x <= s.x() + s.radius(); x++) for (int z = s.z() - s.radius(); z <= s.z() + s.radius(); z++) {
                var b = world.getBlockAt(x, OpportunityPlots.FLOOR_Y + 1, z);
                if (!kind.blocks().contains(b.getType())) continue;
                var event = new BlockBreakEvent(b, p);
                Bukkit.getPluginManager().callEvent(event);
                if (!event.isCancelled()) b.setType(Material.AIR, false);
                return true;
            }
            return false;
        }
        for (var e : members(s)) {
            if (e instanceof LivingEntity living && !living.isDead()) {
                living.damage(10000.0, p);     // lethal, credited to the tester: only player-caused harvests count
                return true;
            }
        }
        return false;
    }

    /** Build a plank floor over the region, marked player-placed, or take it away again. */
    private void base(Player p, Active a) {
        var plot = plot(a);
        boolean on = !a.built.getOrDefault(plot.id(), false);
        for (int[] c : plot.region().columns(1)) {
            var b = world.getBlockAt(c[0], OpportunityPlots.FLOOR_Y + 1, c[1]);
            if (on) { b.setType(Material.OAK_PLANKS, false); plugin.provenance().mark(b, true); }
            else if (b.getType() == Material.OAK_PLANKS) { plugin.provenance().mark(b, false); b.setType(Material.AIR, false); }
        }
        a.built.put(plot.id(), on);
        p.sendMessage(ChatColor.GREEN + (on ? "Built over the region: no ground is eligible now. A ready opportunity will report it and wait; a standing one is unaffected."
                : "Cleared. A ready opportunity can manifest again (force it, or wait for the next lifecycle tick)."));
    }

    private void time(Player p) {
        boolean toNight = !WorldTerrain.isNight(world);
        world.setTime(toNight ? 18000L : 6000L);
        p.sendMessage(ChatColor.GREEN + "It is now " + (toNight ? "night" : "day")
                + ". Recovery runs at the night rate and a day-only swarm is outside its window; day reverses both.");
    }

    private void showSites(Player p, Active a) {
        if (a.sites != null) a.sites.cancel();
        var s = a.sources.get(plot(a).id());
        var r = eligibility(s);
        var eligible = r.eligibleLoci();
        var viable = new java.util.HashSet<>(r.viableLoci());
        p.sendMessage(ChatColor.GREEN + "Showing " + Math.min(eligible.size(), MAX_SITES) + " eligible site(s) for 10 seconds: "
                + "green is viable, grey is eligible but excluded (a player or the previous site is too near).");
        int[] left = {20};
        a.sites = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!p.isOnline() || left[0]-- <= 0) { a.sites.cancel(); a.sites = null; return; }
            int n = 0;
            for (var l : eligible) {
                if (n++ >= MAX_SITES) break;
                p.spawnParticle(viable.contains(l) ? Particle.HAPPY_VILLAGER : Particle.SMOKE,
                        l.x() + 0.5, l.y() + 0.6, l.z() + 0.5, 1, 0.05, 0.4, 0.05, 0);
            }
        }, 0L, 10L);
    }

    // ---- enter, leave, the world --------------------------------------------------------

    private void enter(Player p) {
        lab.requireSetupFor(p);
        world();
        holdChunks();
        buildPlatform();
        var a = new Active();
        active.put(p.getUniqueId(), a);
        double[] st = OpportunityPlots.station();
        p.teleport(new Location(world, st[0], st[1], st[2], 0f, 0f));
        p.setGameMode(GameMode.SURVIVAL);
        ui.engage(p, world);
        p.sendMessage(ChatColor.GREEN + "Opportunity bench. Three plots lie ahead: a Herd (west), a Crop Patch (centre) and a Swarm "
                + "(east, mountain biome, daytime only). Pick a plot, spawn it, and watch.");
        p.sendMessage(ChatColor.GRAY + "You stand at least " + (int) plugin.renewables().eligibilityRules().playerExclusion()
                + " blocks from every plot, because a manifestation is never placed that close to a player. Walk closer to see it refuse.");
    }

    public void leave(Player p) {
        var a = active.remove(p.getUniqueId());
        ui.release(p);
        if (a == null) return;
        if (a.sites != null) a.sites.cancel();
        for (var plot : OpportunityPlots.PLOTS) discard(a, plot);
        // Members swept as 'left the region' stay alive by design, so cleanup removes everything that is not a player.
        if (active.isEmpty())
            for (var e : world.getEntities()) if (!(e instanceof Player) && !NmsBodies.isBody(e.getUniqueId())) e.remove();
        if (active.isEmpty()) releaseChunks();
        lab.toRoom(p);
        p.sendMessage("Left the opportunity bench.");
    }

    public void close() {
        for (var id : new ArrayList<>(active.keySet())) {
            Player p = Bukkit.getPlayer(id);
            if (p != null) leave(p);
        }
        active.clear();
        ui.close();
    }

    public boolean occupies(Player p) { return active.containsKey(p.getUniqueId()); }
    public Renewables.Source source(Player p, String plotId) { var a = active.get(p.getUniqueId()); return a == null ? null : a.sources.get(plotId); }
    public int selected(Player p) { var a = active.get(p.getUniqueId()); return a == null ? -1 : a.selected; }
    public World world() {
        if (world != null) return world;
        world = Bukkit.getWorld(WORLD);
        if (world == null) world = new WorldCreator(WORLD).generator(new VoidGenerator()).biomeProvider(new Biomes())
                .generateStructures(false).createWorld();
        if (world == null) throw new IllegalStateException("Could not create the opportunity bench world.");
        world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
        world.setGameRule(GameRule.DO_MOB_SPAWNING, false);
        world.setDifficulty(Difficulty.NORMAL);   // peaceful would refuse a hostile swarm for a reason that is not the question
        world.setTime(6000L);
        return world;
    }

    private void holdChunks() {
        if (chunksHeld) return;
        for (int cx = OpportunityPlots.MIN_X >> 4; cx <= OpportunityPlots.MAX_X >> 4; cx++)
            for (int cz = OpportunityPlots.MIN_Z >> 4; cz <= OpportunityPlots.MAX_Z >> 4; cz++)
                world.addPluginChunkTicket(cx, cz, plugin);
        chunksHeld = true;
    }

    private void releaseChunks() {
        if (!chunksHeld || world == null) return;
        world.removePluginChunkTickets(plugin);
        chunksHeld = false;
    }

    /** Grass over dirt, rebuilt on entry, with everything above the floor in the plots cleared. */
    private void buildPlatform() {
        int y = OpportunityPlots.FLOOR_Y;
        for (int x = OpportunityPlots.MIN_X; x <= OpportunityPlots.MAX_X; x++)
            for (int z = OpportunityPlots.MIN_Z; z <= OpportunityPlots.MAX_Z; z++) {
                world.getBlockAt(x, y, z).setType(Material.GRASS_BLOCK, false);
                world.getBlockAt(x, y - 1, z).setType(Material.DIRT, false);
                world.getBlockAt(x, y - 2, z).setType(Material.DIRT, false);
            }
        for (var plot : OpportunityPlots.PLOTS)
            for (int[] c : plot.region().columns(1))
                for (int h = 1; h <= 4; h++) {
                    var b = world.getBlockAt(c[0], y + h, c[1]);
                    if (b.getType() != Material.AIR) { plugin.provenance().mark(b, false); b.setType(Material.AIR, false); }
                }
        for (var e : world.getEntities()) if (!(e instanceof Player) && !NmsBodies.isBody(e.getUniqueId())) e.remove();
    }

    /** Remove a plot's source, its members, and anything it or the tester built there. */
    private void discard(Active a, OpportunityPlots.Plot plot) {
        var s = a.sources.remove(plot.id());
        if (s != null) {
            for (var e : members(s)) e.remove();
            plugin.renewables().remove(s.id());
        }
        a.built.remove(plot.id());
        if (world == null) return;
        var kind = RenewableKinds.require(plot.kind());
        int reach = OpportunityPlots.HALF + 8;     // the region plus a patch's spread
        for (int x = plot.x() - reach; x <= plot.x() + reach; x++) for (int z = plot.z() - reach; z <= plot.z() + reach; z++) {
            var b = world.getBlockAt(x, OpportunityPlots.FLOOR_Y + 1, z);
            if (b.getType() == Material.OAK_PLANKS || kind.blocks().contains(b.getType())) {
                plugin.provenance().mark(b, false);
                b.setType(Material.AIR, false);
            }
        }
    }

    // ---- the report ------------------------------------------------------------------------

    private EligibilityReport.Report eligibility(Renewables.Source s) {
        return EligibilityReport.explain(s.region(), new WorldTerrain(world, plugin.provenance()),
                plugin.renewables().eligibilityRules(), s.opportunity().previousLocus());
    }

    /** The report as lines, so it can be shown, logged or asserted on. */
    public List<String> reportLines(Player tester) {
        var a = active.get(tester.getUniqueId());
        if (a == null) return List.of();
        var plot = plot(a);
        var s = a.sources.get(plot.id());
        var out = new ArrayList<String>();
        boolean night = WorldTerrain.isNight(world);
        out.add(ChatColor.GOLD + "Opportunity report: " + plot.label() + " at (" + plot.x() + ", " + plot.z() + "), "
                + (night ? "NIGHT" : "DAY") + ", biome " + (plot.inMountain() ? "mountain" : "plains"));
        if (s == null) { out.add("Not spawned. Spawn it to register a real source."); return out; }
        var op = s.opportunity();
        var rates = plugin.renewables().ratesFor(s);
        out.add("Lifecycle: " + op + "; remaining " + op.remaining() + "; members in the world "
                + members(s).size() + "; blocked attempts " + op.blockedAttempts());
        out.add(String.format(Locale.ROOT, "Recovery %.0f%%: about %.0f s to complete at the current %s rate (day would take %.0f s, night %.0f s).",
                op.recoveryProgress() * 100,
                Recovery.remainingTicks(op.recoveryProgress(), night, rates) / 20.0, night ? "night" : "day",
                Recovery.remainingTicks(op.recoveryProgress(), false, rates) / 20.0,
                Recovery.remainingTicks(op.recoveryProgress(), true, rates) / 20.0));
        out.add("Previous site " + op.previousLocus() + ", current site " + op.locus());
        double outside = EligibilityReport.fractionOutsideRadius(s.region(), s.x(), s.z(), s.radius());
        out.add(String.format(Locale.ROOT, "Source radius %d around its origin; region half-span %d. %.0f%% of the region lies OUTSIDE that cube%s",
                s.radius(), OpportunityPlots.HALF, outside * 100,
                outside > 0 ? ": members placed there are swept as 'left the region' (the Alpha sources have this mismatch)." : "."));
        out.add("Eligibility query, as the world is now:");
        EligibilityReport.describe(eligibility(s)).forEach(l -> out.add(l));
        if (plot.swarm()) {
            var def = SwarmDefinitions.require("mountain_ravager");
            String biome = new WorldTerrain(world, plugin.provenance()).biomeAt(plot.x(), plot.z());
            out.add("Swarm table: " + s.table().ids() + "; here the biome is " + biome + " and it is "
                    + (night ? "night" : "day") + ": " + (def.eligibleAt(biome, night) ? "ELIGIBLE" : "NOT eligible")
                    + " (needs " + def.biomes() + ", " + def.time() + ").");
        }
        out.add("Granted by renewal: " + plugin.renewables().grantedByRenewal() + " (must stay 0: renewal restores availability, it pays nobody).");
        return out;
    }
}
