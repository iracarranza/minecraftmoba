package com.minecraftmoba.plugin;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * The combat chamber: a place in the lab to exercise abilities against a passive,
 * measurable player dummy.
 *
 * <h2>Scope of this slice</h2>
 *
 * The pre-entry flow ({@link CombatPreEntry}) offers every role. Operator, Recipient
 * and Observer can be ENTERED. For the latter two the dummy is a body of the chosen
 * class that casts on command through {@link BodyCaster}. Still refused, with a
 * reason: a passive slot, an ally dummy (no team assignment), and marked-point or
 * recorded aim (no marker, no recording).
 *
 * <h2>Why it needs no scoop and no match</h2>
 *
 * A standalone void world holds the {@link CombatSlab}. {@link TeamDamage} only
 * cancels hits between players on the SAME team, and neither the tester nor the
 * dummy is a match participant, so they are enemies by default and damage flows.
 *
 * <h2>What is measured, in which units</h2>
 *
 * A player's Bukkit health is a 0 to 20 bar, and {@link VitalsScaling} rescales
 * incoming damage at HIGH priority. So RAW damage is read at LOWEST, before that
 * scaling, in effective points; the FINAL damage is read after it and converted
 * back to effective points with {@link Vitals#toEffective}. The pair is how armor
 * and Damage Reduction are checked. Numbers are effective points; the project's
 * displayed values are these times 100.
 *
 * <h2>Final can be below raw with no armor: damage immunity</h2>
 *
 * Vanilla gives a hurt entity a short immunity window. A second hit inside it
 * applies only the amount by which it exceeds the first, and none at all if it is
 * smaller. So two 8-point hits in quick succession record ONE hit, and a 100000-point
 * hit right after an 8-point one has a final of 99992. That is the game's own
 * behavior and it matters when judging an ability's damage, so it is stated here
 * rather than corrected away.
 *
 * <h2>The dummy cannot die</h2>
 *
 * A lethal hit is clamped to leave it a sliver and the hit is recorded at its true
 * size first, so a very large hit is measured rather than ending the session.
 *
 * NOT VERIFIED against a live server at the time of writing.
 */
public final class CombatChamber implements Listener {
    static final String WORLD = "moba_combat";
    private static final int DUMMY_DISTANCE = 8;
    /** How many ticks after a hit a displaced dummy is walked back to its post. */
    private static final int RETURN_AFTER = 40;

    private static final class Active {
        final CombatPreEntry.Session spec;
        final DamageLog dealt = new DamageLog();
        final DamageLog taken = new DamageLog();
        Bodies.Body dummy;
        Location post;
        long nextCast = Long.MAX_VALUE;
        int casts, refused;
        String lastCast = "none";
        long lastHit;
        int refills;
        Active(CombatPreEntry.Session spec) { this.spec = spec; }
    }

    private final MobaPlugin plugin;
    private final Lab lab;
    private final NmsBodies bodies;
    private final CombatCatalog catalog;
    private final BodyCaster caster;
    private final java.util.Random rng = new java.util.Random();
    /** Ticks between casts under the repeat script. */
    private static final int REPEAT_TICKS = 100;
    private final Map<UUID, CombatPreEntry> pre = new HashMap<>();
    private final Map<UUID, Active> active = new HashMap<>();
    private final Map<EntityDamageEvent, Double> rawSeen = new java.util.WeakHashMap<>();
    private World world;
    private BukkitTask ticker;

    public CombatChamber(MobaPlugin plugin, Lab lab) {
        this.plugin = plugin;
        this.lab = lab;
        this.bodies = new NmsBodies(plugin);
        this.catalog = new CombatCatalog(plugin);
        this.caster = new BodyCaster(plugin);
    }

    /** The shared body factory, so one set of dummies exists however the chamber is reached. */
    NmsBodies bodies() { return bodies; }

    // ---- commands -------------------------------------------------------------

    /** Handle {@code /moba lab combat ...}; {@code args} is the full argument array. */
    public void command(Player p, String[] args) {
        String verb = args.length > 2 ? args[2].toLowerCase(Locale.ROOT) : "start";
        if (active.containsKey(p.getUniqueId())) { inChamber(p, verb, args); return; }
        switch (verb) {
            case "start", "begin" -> begin(p);
            case "pick" -> pick(p, args);
            case "back" -> back(p);
            case "set" -> set(p, args);
            case "status" -> status(p);
            case "leave", "cancel" -> { pre.remove(p.getUniqueId()); p.sendMessage("Combat chamber setup cancelled."); }
            default -> p.sendMessage("/moba lab combat start | pick <id> | back | set <cooldown|time|level|dummy> <value> | status | leave");
        }
    }

    private void begin(Player p) {
        lab.requireSetupFor(p);
        var flow = new CombatPreEntry(catalog);
        pre.put(p.getUniqueId(), flow);
        p.sendMessage(ChatColor.GOLD + "Combat chamber. Choose the class to examine.");
        show(p, flow);
    }

    private CombatPreEntry flow(Player p) {
        var flow = pre.get(p.getUniqueId());
        if (flow == null) throw new IllegalStateException("Start with /moba lab combat start.");
        return flow;
    }

    private void pick(Player p, String[] args) {
        var flow = flow(p);
        if (args.length < 4) { show(p, flow); return; }
        try {
            flow.choose(args[3]);
        } catch (IllegalStateException | IllegalArgumentException ex) {
            p.sendMessage(ChatColor.RED + "Not available: " + ex.getMessage());
            show(p, flow);
            return;
        }
        if (flow.ready()) { enter(p, flow.session()); pre.remove(p.getUniqueId()); }
        else show(p, flow);
    }

    private void back(Player p) {
        var flow = flow(p);
        try { flow.back(); } catch (IllegalStateException ex) { p.sendMessage(ex.getMessage()); }
        show(p, flow);
    }

    private void set(Player p, String[] args) {
        var flow = flow(p);
        if (args.length < 5) { p.sendMessage("/moba lab combat set cooldown on|off | time default|day|night | level <n> | dummy enemy|ally"); return; }
        try {
            String v = args[4].toLowerCase(Locale.ROOT);
            switch (args[3].toLowerCase(Locale.ROOT)) {
                case "cooldown" -> flow.cooldownWaiver(v.equals("off") || v.equals("waive") || v.equals("true"));
                case "time" -> flow.timeOfDay(CombatPreEntry.TimeOfDay.valueOf(v.toUpperCase(Locale.ROOT)));
                case "level" -> flow.level(Integer.parseInt(v));
                case "dummy" -> flow.dummyTeam(CombatAvailability.Relation.valueOf(v.toUpperCase(Locale.ROOT)));
                default -> throw new IllegalArgumentException("Unknown mode " + args[3]);
            }
        } catch (RuntimeException ex) {
            p.sendMessage(ChatColor.RED + ex.getMessage());
            return;
        }
        var m = flow.modes();
        p.sendMessage("Modes: cooldowns " + (m.cooldownWaiver() ? "WAIVED" : "normal") + ", time " + m.timeOfDay()
                + ", level " + m.level() + (m.dummy() == null ? "" : ", dummy " + m.dummy()));
    }

    private void status(Player p) {
        var flow = pre.get(p.getUniqueId());
        if (flow == null) { p.sendMessage("No combat chamber setup in progress."); return; }
        show(p, flow);
    }

    private void show(Player p, CombatPreEntry flow) {
        p.sendMessage(ChatColor.GRAY + "Step: " + flow.step());
        for (var c : flow.options()) {
            if (c.available()) p.sendMessage("  " + ChatColor.GREEN + "pick " + c.id() + ChatColor.WHITE + "  " + c.label());
            else p.sendMessage("  " + ChatColor.DARK_GRAY + c.id() + "  " + c.label() + " - " + c.reason());
        }
        if (flow.step() == CombatPreEntry.Step.MODES) {
            var m = flow.modes();
            p.sendMessage(ChatColor.GRAY + "Modes: cooldowns " + (m.cooldownWaiver() ? "WAIVED" : "normal")
                    + ", time " + m.timeOfDay() + ", level " + m.level() + ". Change with /moba lab combat set ...");
        }
    }

    // ---- entering ---------------------------------------------------------------

    private void enter(Player p, CombatPreEntry.Session s) {
        boolean operator = s.role() == CombatAvailability.Role.OPERATOR;
        if (!operator) {
            String refusal = refusal(s);
            if (refusal != null) {
                p.sendMessage(ChatColor.YELLOW + refusal);
                pre.put(p.getUniqueId(), flowToRole(s));
                return;
            }
        }
        world();
        CombatSlabWorld.build(world);
        applyTime(s.modes().timeOfDay());
        if (!plugin.enrolled(p)) p.performCommand("moba join");
        if (!plugin.enrolled(p)) throw new IllegalStateException("Could not enrol; empty your offhand and cursor, then retry.");
        plugin.applyDraftedClass(p, s.classId());
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "moba setlevel " + p.getName() + " " + s.modes().level());

        double[] at = CombatSlab.testerSpawn();
        p.teleport(new Location(world, at[0], at[1], at[2], 0f, 0f));
        p.setGameMode(GameMode.SURVIVAL);
        var a = new Active(s);
        double[] d = CombatSlab.dummySpawn(DUMMY_DISTANCE);
        a.post = new Location(world, d[0], d[1], d[2], 180f, 0f);
        a.dummy = spawnDummy(a);
        if (!operator && s.script() == CombatBehavior.Script.REPEAT) a.nextCast = Bukkit.getCurrentTick() + REPEAT_TICKS;
        active.put(p.getUniqueId(), a);
        startTicker();
        p.sendMessage(ChatColor.GREEN + "In the combat chamber as " + s.classId() + " (level " + s.modes().level() + "). "
                + "Cooldowns " + (s.modes().cooldownWaiver() ? "WAIVED (every report says so). " : "normal. ")
                + (operator ? "A passive dummy stands " + DUMMY_DISTANCE + " blocks ahead. /moba lab combat log | clear | reset | leave"
                        : "The dummy is a " + s.classId() + " and casts " + s.slot() + " on command: /moba lab combat cast "
                                + (s.script() == CombatBehavior.Script.REPEAT ? "(it also casts every " + REPEAT_TICKS / 20 + "s)" : "")
                                + " | log | clear | reset | leave"));
    }

    /** Why this Recipient or Observer session cannot be entered yet, or null when it can. */
    private String refusal(CombatPreEntry.Session s) {
        if (s.slot() == null || s.slot() == CombatAvailability.Slot.PASSIVE)
            return "A passive has no gesture to command; it can be operated, not received. Pick Operate or another slot.";
        if (s.modes().dummy() == CombatAvailability.Relation.ALLY)
            return "An ally dummy needs team assignment, which is not built. Choose an enemy dummy.";
        if (s.aim() == CombatBehavior.Aim.MARKED_POINT || s.aim() == CombatBehavior.Aim.AS_RECORDED)
            return "Marked-point and recorded aim are not built. Choose at-player, straight-ahead or random.";
        return null;
    }

    /** The dummy: classless when the tester operates, otherwise the class being examined. */
    private Bodies.Body spawnDummy(Active a) {
        boolean operator = a.spec.role() == CombatAvailability.Role.OPERATOR;
        return bodies.spawn("dummy", null, operator ? null : a.spec.classId(),
                operator ? 0 : a.spec.modes().level(), a.post);
    }

    /** Make the dummy cast the session's slot, aimed by the session's policy. Returns the outcome for the log. */
    private BodyCaster.Result castNow(Player tester, Active a) {
        if (a.dummy == null) return new BodyCaster.Result(false, "No dummy.");
        Player d = a.dummy.player();
        var aim = a.spec.aim();
        CastAim.Facing facing = null;
        if (aim == CombatBehavior.Aim.AT_PLAYER) {
            var from = d.getEyeLocation();
            var to = tester.getLocation().add(0, tester.getHeight() / 2, 0);
            facing = CastAim.toward(from.getX(), from.getY(), from.getZ(), to.getX(), to.getY(), to.getZ());
        } else if (aim == CombatBehavior.Aim.RANDOM) {
            facing = CastAim.random(rng);
        } else if (aim == CombatBehavior.Aim.STRAIGHT_AHEAD) {
            facing = new CastAim.Facing(a.post.getYaw(), 0f);
        }
        var r = caster.cast(d, a.spec.slot().name().toLowerCase(Locale.ROOT), facing, 0);
        if (r.cast()) a.casts++; else a.refused++;
        a.lastCast = r.note();
        return r;
    }

    /** A flow parked at the role step so the tester can choose Operate after a refused entry. */
    private CombatPreEntry flowToRole(CombatPreEntry.Session s) {
        var flow = new CombatPreEntry(catalog);
        flow.choose(s.classId());
        return flow;
    }

    private World world() {
        if (world != null) return world;
        world = Bukkit.getWorld(WORLD);
        if (world == null) world = new WorldCreator(WORLD).generator(new VoidGenerator()).generateStructures(false).createWorld();
        if (world == null) throw new IllegalStateException("Could not create the combat chamber world.");
        world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
        world.setGameRule(GameRule.DO_MOB_SPAWNING, false);
        world.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
        return world;
    }

    private void applyTime(CombatPreEntry.TimeOfDay t) {
        world.setTime(switch (t) { case NIGHT -> 18000L; case DAY, DEFAULT -> 6000L; });
    }

    // ---- inside -----------------------------------------------------------------

    private void inChamber(Player p, String verb, String[] args) {
        var a = active.get(p.getUniqueId());
        switch (verb) {
            case "log", "status" -> report(p, a);
            case "clear" -> { a.dealt.clear(); a.taken.clear(); a.refills = 0; a.casts = 0; a.refused = 0; p.sendMessage("Logs cleared."); }
            case "reset" -> reset(p, a);
            case "cast" -> {
                if (a.spec.role() == CombatAvailability.Role.OPERATOR) { p.sendMessage("Operator casts for themselves; the dummy is passive."); break; }
                var r = castNow(p, a);
                p.sendMessage((r.cast() ? ChatColor.GREEN : ChatColor.RED) + "Dummy: " + r.note());
            }
            case "leave", "end" -> leave(p);
            default -> p.sendMessage("In the chamber: /moba lab combat cast | log | clear | reset | leave");
        }
    }

    private void report(Player p, Active a) { reportLines(a).forEach(p::sendMessage); }

    /** The report as lines, so it can be shown, logged or asserted on. */
    public java.util.List<String> reportLines(Player tester) {
        var a = active.get(tester.getUniqueId());
        return a == null ? java.util.List.of() : reportLines(a);
    }

    private java.util.List<String> reportLines(Active a) {
        var out = new java.util.ArrayList<String>();
        var m = a.spec.modes();
        out.add(ChatColor.GOLD + "Combat report: " + a.spec.classId() + " L" + m.level() + ", cooldowns "
                + (m.cooldownWaiver() ? "WAIVED" : "normal") + ", time " + m.timeOfDay());
        out.add(ChatColor.GRAY + "Units are effective points (the displayed value is these x100).");
        long now = Bukkit.getCurrentTick();
        var log = a.dealt;
        out.add("Dealt to the dummy: " + log.count() + " hits, raw " + String.format("%.2f", log.totalRaw())
                + " -> final " + String.format("%.2f", log.totalFinal())
                + String.format(" (%.1f%% mitigated)", log.totalRaw() == 0 ? 0 : 100 * (1 - log.totalFinal() / log.totalRaw())));
        out.add(String.format("  last 5s: %.2f per second", log.perSecond(now, 100)));
        log.finalBySource().forEach((src, dmg) -> out.add(String.format("  %s: %.2f", src, dmg)));
        out.add("Taken by you: " + a.taken.count() + " hits, final " + String.format("%.2f", a.taken.totalFinal()));
        if (a.spec.role() != CombatAvailability.Role.OPERATOR)
            out.add("Dummy casts: " + a.casts + " ran, " + a.refused + " refused; last: " + a.lastCast
                    + ". Replay is approximate: terrain, timing and randomness differ per cast.");
        if (a.refills > 0) out.add(ChatColor.GRAY + "The dummy was refilled " + a.refills + " time(s); it cannot die.");
        return out;
    }

    /** Where a tester is in the chamber's flow, for diagnostics: "in chamber", "setup step X" or "idle". */
    public String state(Player p) {
        if (active.containsKey(p.getUniqueId())) return "in chamber";
        var flow = pre.get(p.getUniqueId());
        return flow == null ? "idle" : "setup step " + flow.step();
    }

    /** Whether this player is a chamber tester or a tester's dummy. */
    public boolean occupies(Player p) {
        if (active.containsKey(p.getUniqueId())) return true;
        for (Active a : active.values())
            if (a.dummy != null && a.dummy.player().getUniqueId().equals(p.getUniqueId())) return true;
        return false;
    }

    /** The tester's dummy, or null. */
    public Player dummy(Player tester) {
        var a = active.get(tester.getUniqueId());
        return a == null || a.dummy == null ? null : a.dummy.player();
    }

    /** How many hits the dummy has taken, for assertions. */
    public int hitsDealt(Player tester) {
        var a = active.get(tester.getUniqueId());
        return a == null ? 0 : a.dealt.count();
    }

    private void reset(Player p, Active a) {
        CombatSlabWorld.build(world);
        if (a.dummy != null) bodies.despawn(a.dummy);
        a.dummy = spawnDummy(a);
        a.dealt.clear(); a.taken.clear(); a.refills = 0; a.casts = 0; a.refused = 0;
        double[] at = CombatSlab.testerSpawn();
        p.teleport(new Location(world, at[0], at[1], at[2], 0f, 0f));
        p.sendMessage("Chamber rebuilt, dummy restored, logs cleared.");
    }

    /** Leave the chamber: remove the dummy and go back to the lab room. */
    public void leave(Player p) {
        var a = active.remove(p.getUniqueId());
        if (a != null && a.dummy != null) bodies.despawn(a.dummy);
        if (active.isEmpty()) stopTicker();
        if (a != null) { lab.toRoom(p); p.sendMessage("Left the combat chamber."); }
    }

    /** Remove everything, for plugin shutdown. */
    public void close() {
        for (var e : new java.util.ArrayList<>(active.entrySet())) {
            Player p = Bukkit.getPlayer(e.getKey());
            if (e.getValue().dummy != null) bodies.despawn(e.getValue().dummy);
            if (p != null) lab.toRoom(p);
        }
        active.clear(); pre.clear(); stopTicker();
    }

    // ---- measurement ------------------------------------------------------------

    /** Before VitalsScaling (HIGH): the raw hit, in effective points. */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void rawDamage(EntityDamageEvent e) {
        if (active.isEmpty() || !(e.getEntity() instanceof Player)) return;
        rawSeen.put(e, e.getDamage());
    }

    /** After VitalsScaling: the final hit, converted back to effective points. */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void finalDamage(EntityDamageEvent e) {
        if (active.isEmpty() || !(e.getEntity() instanceof Player victim)) return;
        Double raw = rawSeen.remove(e);
        for (Active a : active.values()) {
            boolean dummyHit = a.dummy != null && a.dummy.player().equals(victim);
            boolean testerHit = active.get(victim.getUniqueId()) == a;
            if (!dummyHit && !testerHit) continue;
            double capacity = plugin.effectiveMaxHealth(victim);
            double finalEffective = capacity > 0 ? Vitals.toEffective(e.getFinalDamage(), capacity) : e.getFinalDamage();
            double rawEffective = raw == null ? finalEffective : raw;
            String source = source(e);
            if (dummyHit) {
                a.dealt.record(Bukkit.getCurrentTick(), rawEffective, finalEffective, source);
                a.lastHit = Bukkit.getCurrentTick();
                // A lethal hit is recorded at its true size, then clamped so the session survives it.
                if (victim.getHealth() - e.getFinalDamage() <= 0.5) e.setDamage(Math.max(0, victim.getHealth() - 1.0));
            } else {
                a.taken.record(Bukkit.getCurrentTick(), rawEffective, finalEffective, source);
            }
        }
    }

    private static String source(EntityDamageEvent e) {
        String cause = e.getCause().name().toLowerCase(Locale.ROOT);
        if (e instanceof EntityDamageByEntityEvent by) return by.getDamager().getName() + " " + cause;
        return cause;
    }

    // ---- upkeep -----------------------------------------------------------------

    private void startTicker() {
        if (ticker == null) ticker = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 1L, 1L);
    }

    private void stopTicker() {
        if (ticker != null) { ticker.cancel(); ticker = null; }
    }

    private void tick() {
        long now = Bukkit.getCurrentTick();
        for (var e : active.entrySet()) {
            Active a = e.getValue();
            Player tester = Bukkit.getPlayer(e.getKey());
            if (tester == null) continue;
            if (a.spec.modes().cooldownWaiver()) {
                plugin.inputs().cooldowns().clear(e.getKey());
                if (a.dummy != null) plugin.inputs().cooldowns().clear(a.dummy.player().getUniqueId());
            }
            if (a.dummy == null) continue;
            if (now >= a.nextCast) { castNow(tester, a); a.nextCast = now + REPEAT_TICKS; }
            Player dummy = a.dummy.player();
            var max = dummy.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH);
            double full = max == null ? 20.0 : max.getValue();
            if (dummy.getHealth() < full * 0.5) { dummy.setHealth(full); a.refills++; }
            // A knocked-back dummy is walked home once the fight has paused.
            if (now - a.lastHit > RETURN_AFTER && dummy.getLocation().distance(a.post) > 2.0) {
                dummy.setVelocity(new org.bukkit.util.Vector());
                dummy.teleport(a.post);
            }
        }
    }

    @EventHandler public void quit(PlayerQuitEvent e) {
        pre.remove(e.getPlayer().getUniqueId());
        if (active.containsKey(e.getPlayer().getUniqueId())) {
            var a = active.remove(e.getPlayer().getUniqueId());
            if (a.dummy != null) bodies.despawn(a.dummy);
            if (active.isEmpty()) stopTicker();
        }
    }
}
