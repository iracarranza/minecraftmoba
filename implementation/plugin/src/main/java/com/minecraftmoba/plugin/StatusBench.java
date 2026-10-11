package com.minecraftmoba.plugin;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * The status bench: apply each control status to a body, or to yourself, and see what it does.
 *
 * It exists because Stunned and Rooted were only ever checked by scripted bodies, Lured and
 * Frightened did not exist, and a status is only as real as someone having watched it hold. The
 * arena is a flat floor with a source marker, a subject body and the tester.
 *
 * <h2>Two kinds of evidence, kept apart</h2>
 *
 * <b>Checked</b>: the rule table ({@link StatusRules}) against what the server answers, line by
 * line, with a mismatch flagged. <b>Measured</b>: what actually happened to the subject since the
 * status was applied: how far it moved, whether it got nearer to or further from the source, and
 * how far its camera is from facing the source. The first says the plugin agrees with the
 * definition; only the second says the entity behaved.
 *
 * <h2>Applying to yourself</h2>
 *
 * The only way to feel forced movement on a real client. A self-application is capped
 * ({@link StatusArena#SELF_MAX_TICKS}) because a Stunned tester cannot use the hotbar that would
 * clear it; {@code /moba lab effects clear} always works.
 *
 * NOT VERIFIED beyond the live acceptance run at the time of writing. Forced movement on a real
 * client has not been felt by anyone.
 */
public final class StatusBench {
    static final String WORLD = "moba_status";

    private static final class Active {
        Statuses.Kind kind = Statuses.Kind.LURED;
        int duration = StatusArena.DURATIONS.get(1);
        Bodies.Body subject;
        Location source = new Location(null, StatusArena.SOURCE_X, StatusArena.FLOOR_Y + 1, StatusArena.SOURCE_Z);
        Player lastTarget;
        Statuses.Kind lastKind;
        long appliedAt;
        Location appliedFrom;
        double distanceAtApply;
    }

    private final MobaPlugin plugin;
    private final Lab lab;
    private final BenchUi ui;
    private final Map<UUID, Active> active = new HashMap<>();
    private World world;
    private boolean chunksHeld;

    public StatusBench(MobaPlugin plugin, Lab lab) {
        this.plugin = plugin;
        this.lab = lab;
        this.ui = new BenchUi(plugin, new BenchUi.Spec(
                "status", StatusBenchMenu::menu,
                p -> StatusBenchMenu.gate(new StatusBenchMenu.State(active.containsKey(p.getUniqueId()))),
                (p, verb) -> StatusBenchMenu.refusal(verb, new StatusBenchMenu.State(active.containsKey(p.getUniqueId()))),
                StatusBenchMenu.deck(), WORLD, this::runVerb, this::leave,
                "Leave bench", "Leaves the status bench and returns to the lab room."));
    }

    // ---- commands -----------------------------------------------------------------

    /** Handle {@code /moba lab effects ...}; {@code args} is the full argument array. */
    public void command(Player p, String[] args) {
        String verb = args.length > 2 ? args[2].toLowerCase(Locale.ROOT) : "start";
        if (!active.containsKey(p.getUniqueId())) {
            if (verb.equals("start") || verb.equals("enter")) enter(p);
            else p.sendMessage("/moba lab effects start");
            return;
        }
        var a = active.get(p.getUniqueId());
        var named = Statuses.Kind.of(verb);
        if (named != null) {                     // /moba lab effects lured [seconds]
            a.kind = named;
            if (args.length > 3) try { a.duration = (int) Math.round(Double.parseDouble(args[3]) * 20); } catch (NumberFormatException ignored) { }
            p.sendMessage(ChatColor.GREEN + "Selected " + named.label() + " for " + StatusArena.describe(a.duration) + ".");
            return;
        }
        runVerb(p, verb);
    }

    void runVerb(Player p, String verb) {
        var a = active.get(p.getUniqueId());
        if (a == null) return;
        switch (verb) {
            case "status" -> {
                a.kind = a.kind.next();
                p.sendMessage(ChatColor.GREEN + "Selected: " + a.kind.label() + ". " + describe(a.kind));
            }
            case "time" -> {
                a.duration = StatusArena.nextDuration(a.duration);
                p.sendMessage(ChatColor.GREEN + "Duration: " + StatusArena.describe(a.duration) + ".");
            }
            case "apply" -> applyTo(p, a, subject(a), a.duration, "the subject");
            case "self" -> {
                int ticks = StatusArena.selfTicks(a.duration);
                applyTo(p, a, p, ticks, "you");
                if (ticks < a.duration)
                    p.sendMessage(ChatColor.GRAY + "Capped to " + StatusArena.describe(ticks) + " when applied to yourself. /moba lab effects clear ends it at once.");
            }
            case "source" -> moveSource(p, a);
            case "check", "report", "status?" -> checkLines(p, a).forEach(p::sendMessage);
            case "clear" -> { clear(a, p); p.sendMessage(ChatColor.GREEN + "Every status cleared on the subject and on you."); }
            case "reset" -> reset(p, a);
            case "leave", "end" -> leave(p);
            default -> p.sendMessage("In the bench: /moba lab effects [stunned|rooted|blinded|weakened|lured|frightened] [seconds] | status | time | apply | self | source | check | clear | reset | leave");
        }
    }

    private static String describe(Statuses.Kind k) {
        var refused = StatusRules.refused(k).stream().map(StatusRules.Capability::label).toList();
        var allowed = StatusRules.allowed(k).stream().map(StatusRules.Capability::label).toList();
        if (refused.isEmpty()) return "Refuses nothing of its own (a platform effect).";
        return "Refuses: " + String.join(", ", refused) + ". Allows: " + String.join(", ", allowed) + ".";
    }

    // ---- acting ---------------------------------------------------------------------

    private Player subject(Active a) {
        if (a.subject == null || a.subject.player() == null || !a.subject.player().isOnline() || a.subject.player().isDead()) {
            if (a.subject != null) lab.combat().bodies().despawn(a.subject);
            a.subject = lab.combat().bodies().spawn("subject", null, null, 0, subjectStart());
        }
        return a.subject.player();
    }

    private Location subjectStart() { return new Location(world, StatusArena.SUBJECT_X, StatusArena.FLOOR_Y + 1, StatusArena.SUBJECT_Z, 0f, 0f); }

    private void applyTo(Player tester, Active a, Player target, int ticks, String who) {
        try {
            a.source.setWorld(world);
            Statuses.apply(plugin, a.kind, target, ticks, () -> a.source.clone());
        } catch (IllegalStateException ex) {
            tester.sendMessage(ChatColor.RED + ex.getMessage());
            return;
        }
        a.lastTarget = target;
        a.lastKind = a.kind;
        a.appliedAt = plugin.getServer().getCurrentTick();
        a.appliedFrom = target.getLocation().clone();
        a.distanceAtApply = horizontal(target.getLocation(), a.source);
        tester.sendMessage(ChatColor.GREEN + a.kind.label() + " on " + who + " for " + StatusArena.describe(ticks)
                + (a.kind.needsSource() ? ", source " + String.format(Locale.ROOT, "%.1f", a.distanceAtApply) + " blocks away" : "") + ".");
        tester.sendMessage(ChatColor.GRAY + "Check the rules shows what it holds against the definition, and what the subject actually did.");
    }

    private void clear(Active a, Player tester) {
        if (a.subject != null && a.subject.player() != null) Statuses.clear(plugin, a.subject.player());
        Statuses.clear(plugin, tester);
    }

    private void moveSource(Player p, Active a) {
        Location at = p.getLocation();
        int x = Math.max(StatusArena.MIN_X, Math.min(StatusArena.MAX_X, at.getBlockX()));
        int z = Math.max(StatusArena.MIN_Z, Math.min(StatusArena.MAX_Z, at.getBlockZ()));
        placeMarker(a, x + 0.5, z + 0.5);
        p.sendMessage(ChatColor.GREEN + "Source at " + x + ", " + z + ".");
    }

    private void reset(Player p, Active a) {
        clear(a, p);
        placeMarker(a, StatusArena.SOURCE_X, StatusArena.SOURCE_Z);
        var s = subject(a);
        s.teleport(subjectStart());
        a.lastTarget = null;
        p.sendMessage(ChatColor.GREEN + "Subject and source back at their start. Statuses cleared.");
    }

    // ---- the check ------------------------------------------------------------------

    /** The check as lines, so it can be shown, logged or asserted on. */
    public List<String> checkLines(Player tester) {
        var a = active.get(tester.getUniqueId());
        return a == null ? List.of() : checkLines(tester, a);
    }

    private List<String> checkLines(Player p, Active a) {
        var out = new ArrayList<String>();
        Player subject = subject(a);
        out.add(ChatColor.GOLD + "Selected: " + a.kind.label() + " for " + StatusArena.describe(a.duration) + ". " + describe(a.kind));
        for (var who : new Player[]{subject, p}) {
            var now = Statuses.active(plugin, who);
            out.add((who == p ? "You" : "Subject") + ": " + (now.isEmpty() ? "no status"
                    : String.join(", ", now.stream().map(s -> s.kind().label() + " " + StatusArena.describe((int) s.remaining())).toList())));
        }
        Statuses.Kind shown = a.lastKind != null ? a.lastKind : a.kind;
        Player target = a.lastTarget != null && a.lastTarget.isOnline() ? a.lastTarget : subject;
        out.add(ChatColor.GOLD + "Checked: " + shown.label() + " on " + (target == p ? "you" : "the subject")
                + " - the definition against what the server answers");
        var lines = StatusRules.checklist(shown, c -> observe(c, target));
        for (var l : lines) {
            String defined = l.definedRefused() ? "refused" : "allowed";
            String seen = l.observedRefused() == null ? "cannot be asked without a client" : (l.observedRefused() ? "refused" : "allowed");
            out.add((l.mismatch() ? ChatColor.RED + "  MISMATCH " : ChatColor.GRAY + "  ") + l.capability().label() + ": defined " + defined + ", server says " + seen);
        }
        if (StatusRules.mismatches(lines).isEmpty()) out.add(ChatColor.GREEN + "  No mismatch where the server can answer.");
        if (a.lastTarget != null && a.lastKind != null) out.addAll(measured(a, target, shown));
        if (shown == Statuses.Kind.WEAKENED)
            out.add(ChatColor.YELLOW + "Weakened is native Weakness: it lowers MELEE damage. Ability damage is not lowered by it.");
        return out;
    }

    /** What the server answers for a capability (true = refused), or null when only a client could say. */
    private Boolean observe(StatusRules.Capability c, Player e) {
        boolean stunned = plugin.stun().actionsRefused(e), moveHeld = plugin.stun().movementRefused(e);
        boolean forced = plugin.forced().refusesAbilities(e);
        return switch (c) {
            case MOVE -> moveHeld || forced;
            case JUMP -> moveHeld ? Boolean.TRUE : (forced ? null : Boolean.FALSE);
            case SPRINT -> moveHeld || forced;
            case CROUCH -> forced;
            case ACT -> stunned;
            case ABILITIES -> stunned || forced;
            case ATTACK -> stunned ? Boolean.TRUE : (forced ? null : Boolean.FALSE);
            case LOOK -> forced ? Boolean.TRUE : (stunned ? Boolean.FALSE : null);
        };
    }

    private List<String> measured(Active a, Player target, Statuses.Kind kind) {
        var out = new ArrayList<String>();
        long ticks = plugin.getServer().getCurrentTick() - a.appliedAt;
        double moved = horizontal(target.getLocation(), a.appliedFrom);
        double now = horizontal(target.getLocation(), a.source);
        out.add(ChatColor.GOLD + "Measured since it was applied, " + ticks + " ticks ago:");
        out.add(String.format(Locale.ROOT, "  moved %.2f blocks horizontally", moved));
        if (kind.needsSource()) {
            double facing = ForcedMotion.yawDifference(target.getLocation().getYaw(),
                    ForcedMotion.yawOf(a.source.getX() - target.getLocation().getX(), a.source.getZ() - target.getLocation().getZ()));
            out.add(String.format(Locale.ROOT, "  distance to the source %.1f -> %.1f (%s)", a.distanceAtApply, now,
                    now < a.distanceAtApply - 0.05 ? "nearer" : now > a.distanceAtApply + 0.05 ? "further" : "unchanged"));
            out.add(String.format(Locale.ROOT, "  camera is %.0f degrees off the source", facing));
        }
        return out;
    }

    private static double horizontal(Location a, Location b) { return Math.hypot(a.getX() - b.getX(), a.getZ() - b.getZ()); }

    // ---- entering and leaving -------------------------------------------------------

    private void enter(Player p) {
        lab.requireSetupFor(p);
        world();
        holdChunks();
        buildArena();
        var a = new Active();
        a.source.setWorld(world);
        active.put(p.getUniqueId(), a);
        placeMarker(a, StatusArena.SOURCE_X, StatusArena.SOURCE_Z);
        subject(a);
        p.teleport(new Location(world, StatusArena.TESTER_X, StatusArena.FLOOR_Y + 1, StatusArena.TESTER_Z, 180f, 0f));
        p.setGameMode(GameMode.SURVIVAL);
        ui.engage(p, world);
        p.sendMessage(ChatColor.GREEN + "Status bench. The subject stands ahead, the gold block is the source for Lured and Frightened.");
        p.sendMessage(ChatColor.GRAY + "Hotbar items, the deck to the west, or /moba lab effects ... Pick a status, apply it, and Check the rules. "
                + "Apply to me to feel it on a real client; /moba lab effects clear ends it at once.");
    }

    public void leave(Player p) {
        var a = active.remove(p.getUniqueId());
        ui.release(p);
        if (a == null) return;
        clear(a, p);
        if (a.subject != null) lab.combat().bodies().despawn(a.subject);
        removeMarkerText();
        if (active.isEmpty()) releaseChunks();
        lab.toRoom(p);
        p.sendMessage("Left the status bench.");
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
    public Player subjectOf(Player tester) { var a = active.get(tester.getUniqueId()); return a == null ? null : subject(a); }
    public Location source(Player tester) { var a = active.get(tester.getUniqueId()); return a == null ? null : a.source.clone(); }
    public Statuses.Kind selected(Player tester) { var a = active.get(tester.getUniqueId()); return a == null ? null : a.kind; }

    // ---- the venue ----------------------------------------------------------------

    private void holdChunks() {
        if (chunksHeld) return;
        var deck = StatusBenchMenu.deck().deck();
        int minX = Math.min(StatusArena.MIN_X, deck.minX()) >> 4, maxX = Math.max(StatusArena.MAX_X, deck.maxX()) >> 4;
        int minZ = Math.min(StatusArena.MIN_Z, deck.minZ()) >> 4, maxZ = Math.max(StatusArena.MAX_Z, deck.maxZ()) >> 4;
        for (int cx = minX; cx <= maxX; cx++)
            for (int cz = minZ; cz <= maxZ; cz++) world.addPluginChunkTicket(cx, cz, plugin);
        chunksHeld = true;
    }

    private void releaseChunks() {
        if (!chunksHeld || world == null) return;
        world.removePluginChunkTickets(plugin);
        chunksHeld = false;
    }

    private World world() {
        if (world != null) return world;
        world = Bukkit.getWorld(WORLD);
        if (world == null) world = new WorldCreator(WORLD).generator(new VoidGenerator()).generateStructures(false).createWorld();
        if (world == null) throw new IllegalStateException("Could not create the status bench world.");
        world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
        world.setGameRule(GameRule.DO_MOB_SPAWNING, false);
        world.setTime(6000L);
        return world;
    }

    /** A flat floor with a faint grid, rebuilt on entry so a stale marker cannot survive. */
    private void buildArena() {
        int y = StatusArena.FLOOR_Y;
        for (int x = StatusArena.MIN_X; x <= StatusArena.MAX_X; x++)
            for (int z = StatusArena.MIN_Z; z <= StatusArena.MAX_Z; z++) {
                Material m = ((x & 3) == 0 && (z & 3) == 0) ? Material.LIGHT_GRAY_CONCRETE : Material.STONE;
                world.getBlockAt(x, y, z).setType(m, false);
                for (int h = 1; h <= 3; h++) world.getBlockAt(x, y + h, z).setType(Material.AIR, false);
            }
    }

    private void placeMarker(Active a, double x, double z) {
        int y = StatusArena.FLOOR_Y;
        removeMarkerText();
        world.getBlockAt((int) Math.floor(a.source.getX()), y, (int) Math.floor(a.source.getZ())).setType(Material.STONE, false);
        a.source = new Location(world, x, y + 1, z);
        world.getBlockAt((int) Math.floor(x), y, (int) Math.floor(z)).setType(Material.GOLD_BLOCK, false);
        world.spawn(new Location(world, x, y + 2.2, z), TextDisplay.class, t -> {
            t.text(net.kyori.adventure.text.Component.text("SOURCE"));
            t.setBillboard(Display.Billboard.CENTER);
            t.setPersistent(false);
            t.addScoreboardTag("status_marker");
        });
    }

    private void removeMarkerText() {
        if (world == null) return;
        for (var e : world.getEntitiesByClass(TextDisplay.class))
            if (e.getScoreboardTags().contains("status_marker")) e.remove();
    }
}
