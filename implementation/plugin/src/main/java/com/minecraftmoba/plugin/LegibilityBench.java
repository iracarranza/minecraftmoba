package com.minecraftmoba.plugin;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * The legibility bench: how a state READS to someone looking at it from a distance.
 *
 * It exists because the settled doctrine ("Global Effect Legibility", classes.md) makes
 * glow the universal signal and class presentation the specific one, and neither has
 * been looked at. The earlier Werewolf presentation investigation found the reachable
 * mechanisms (scale, glow, worn armor, particles) but could not test them. This is the
 * place to: a long marked track, a subject at one end wearing a chosen variant, and the
 * tester walking away from it judging at what distance it still reads.
 *
 * <h2>What is measured, and what is judged</h2>
 *
 * Distance and apparent size are computed ({@link Legibility}). "Still reads" is the
 * TESTER'S judgement, recorded as a mark at the distance they stand at, keyed by the
 * variant. The report gives each variant's marks, their median, and how many pixels tall
 * the subject was at that median, so a scale step is judged by what it buys.
 *
 * <h2>What the glow here is NOT</h2>
 *
 * The doctrine's Tier 2 glow is observer-relative; the platform's glow flag is not (it is
 * the same for every viewer), and per-viewer delivery is an open technical risk. The glow
 * on this bench is the universal flag, which is Tier 1's behavior. The report states what
 * the RULE would show a given observer, beside what is actually displayed, and does not
 * claim they are the same.
 *
 * <h2>Not a class</h2>
 *
 * The subject is a plain body wearing a variant, not a Werewolf: no Werewolf kit exists in
 * the plugin, and forms are not the question yet. The bench tests the mechanisms a form
 * would be built from.
 *
 * NOT VERIFIED beyond the live acceptance run at the time of writing.
 */
public final class LegibilityBench {
    static final String WORLD = "moba_legibility";
    private static final long CLUTTER_SEED = 7L;
    /** [PROTOTYPE] pillars per ten blocks of track. */
    private static final double CLUTTER_DENSITY = 2.0;

    private static final class Active {
        LegibilityVariant variant = LegibilityVariant.control();
        final LegibilityLog log = new LegibilityLog();
        Bodies.Body subject;
        List<LegibilityTrack.Pillar> pillars = List.of();
        BukkitTask particles;
    }

    private final MobaPlugin plugin;
    private final Lab lab;
    private final BenchUi ui;
    private final Map<UUID, Active> active = new HashMap<>();
    private World world;
    private boolean chunksHeld;

    public LegibilityBench(MobaPlugin plugin, Lab lab) {
        this.plugin = plugin;
        this.lab = lab;
        this.ui = new BenchUi(plugin, new BenchUi.Spec(
                "legibility", LegibilityMenu::menu,
                p -> LegibilityMenu.gate(new LegibilityMenu.State(active.containsKey(p.getUniqueId()))),
                (p, verb) -> LegibilityMenu.refusal(verb, new LegibilityMenu.State(active.containsKey(p.getUniqueId()))),
                LegibilityMenu.deck(), WORLD, this::runVerb, this::leave,
                "Leave bench", "Leaves the legibility bench and returns to the lab room."));
    }

    // ---- commands -----------------------------------------------------------------

    /** Handle {@code /moba lab legibility ...}; {@code args} is the full argument array. */
    public void command(Player p, String[] args) {
        String verb = args.length > 2 ? args[2].toLowerCase(Locale.ROOT) : "start";
        if (!active.containsKey(p.getUniqueId())) {
            if (verb.equals("start") || verb.equals("enter")) enter(p);
            else p.sendMessage("/moba lab legibility start");
            return;
        }
        runVerb(p, verb);
    }

    void runVerb(Player p, String verb) {
        var a = active.get(p.getUniqueId());
        if (a == null) return;
        switch (verb) {
            case "scale" -> change(p, a, a.variant.nextScale());
            case "glow" -> change(p, a, a.variant.toggleGlow());
            case "armor" -> change(p, a, a.variant.nextArmor());
            case "particles" -> change(p, a, a.variant.nextParticles());
            case "clutter" -> change(p, a, a.variant.toggleClutter());
            case "mark" -> mark(p, a);
            case "report", "log", "status" -> reportLines(a, p).forEach(p::sendMessage);
            case "clear" -> { a.log.clear(); p.sendMessage("Marks cleared."); }
            case "leave", "end" -> leave(p);
            default -> p.sendMessage("In the bench: /moba lab legibility scale | glow | armor | particles | clutter | mark | report | clear | leave");
        }
    }

    private void change(Player p, Active a, LegibilityVariant next) {
        a.variant = next;
        apply(a);
        p.sendMessage(ChatColor.GREEN + next.signature());
    }

    private void mark(Player p, Active a) {
        double d = horizontal(p.getLocation(), a.subject.player().getLocation());
        if (d < 1.0) { p.sendMessage(ChatColor.RED + "Stand away from the subject, at the distance where it still reads, then mark."); return; }
        a.log.mark(a.variant.signature(), d);
        var s = a.log.summary(a.variant.signature());
        p.sendMessage(ChatColor.GREEN + String.format(Locale.ROOT, "Marked %.1f blocks (%d for this variant, median %.1f).", d, s.count(), s.median()));
    }

    private static double horizontal(Location a, Location b) { return Math.hypot(a.getX() - b.getX(), a.getZ() - b.getZ()); }

    // ---- entering and leaving -----------------------------------------------------

    private void enter(Player p) {
        lab.requireSetupFor(p);
        world();
        holdChunks();
        buildTrack();
        var a = new Active();
        Location at = new Location(world, 0.5, LegibilityTrack.FLOOR_Y + 1, LegibilityTrack.SUBJECT_Z, 0f, 0f);
        a.subject = lab.combat().bodies().spawn("subject", null, null, 0, at);
        active.put(p.getUniqueId(), a);
        apply(a);
        p.teleport(new Location(world, 0.5, LegibilityTrack.FLOOR_Y + 1, LegibilityTrack.OBSERVER_Z, 180f, 0f));
        p.setGameMode(GameMode.SURVIVAL);
        ui.engage(p, world);
        p.sendMessage(ChatColor.GREEN + "Legibility bench. The subject is 16 blocks ahead; stripes mark every "
                + LegibilityTrack.MARKER_EVERY + ". Change the variant, walk away to where it STOPS reading, and mark it.");
        p.sendMessage(ChatColor.GRAY + "Hotbar items, the deck to the west, or /moba lab legibility ... Leave is in your inventory. "
                + "The glow here is the universal flag; the doctrine's per-viewer rule is shown in the report, not delivered.");
    }

    public void leave(Player p) {
        var a = active.remove(p.getUniqueId());
        ui.release(p);
        if (a == null) return;
        stopParticles(a);
        clearPillars(a);
        if (a.subject != null) lab.combat().bodies().despawn(a.subject);
        if (active.isEmpty()) releaseChunks();
        lab.toRoom(p);
        p.sendMessage("Left the legibility bench.");
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
    public Player subject(Player tester) { var a = active.get(tester.getUniqueId()); return a == null ? null : a.subject.player(); }
    public LegibilityVariant variant(Player tester) { var a = active.get(tester.getUniqueId()); return a == null ? null : a.variant; }
    public int marks(Player tester) { var a = active.get(tester.getUniqueId()); return a == null ? 0 : a.log.total(); }

    // ---- the venue ----------------------------------------------------------------

    /**
     * Keep the bench's chunks loaded while anyone is in it.
     *
     * The deck's buttons and the subject are non-persistent, and a non-persistent entity
     * is discarded when its chunk unloads. A tester standing at the far end of the track
     * is exactly the case where the subject's and the deck's chunks are no longer near
     * anyone, so without a ticket the bench loses its own controls and subject mid-test.
     */
    private void holdChunks() {
        if (chunksHeld) return;
        var deck = LegibilityMenu.deck().deck();
        int minX = Math.min(LegibilityTrack.MIN_X, deck.minX()) >> 4, maxX = Math.max(LegibilityTrack.MAX_X, deck.maxX()) >> 4;
        int minZ = Math.min(LegibilityTrack.MIN_Z, deck.minZ()) >> 4, maxZ = Math.max(LegibilityTrack.MAX_Z, deck.maxZ()) >> 4;
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
        if (world == null) throw new IllegalStateException("Could not create the legibility bench world.");
        world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
        world.setGameRule(GameRule.DO_MOB_SPAWNING, false);
        world.setTime(6000L);
        return world;
    }

    /** The floor and its distance stripes. Rebuilt on entry so a stale pillar or stripe cannot survive. */
    private void buildTrack() {
        int y = LegibilityTrack.FLOOR_Y;
        var stripes = new java.util.HashSet<Integer>();
        for (int d : LegibilityTrack.markerDistances()) stripes.add(LegibilityTrack.markerZ(d));
        Material[] colours = {Material.RED_CONCRETE, Material.YELLOW_CONCRETE};
        for (int x = LegibilityTrack.MIN_X; x <= LegibilityTrack.MAX_X; x++)
            for (int z = LegibilityTrack.MIN_Z; z <= LegibilityTrack.MAX_Z; z++) {
                Material m = Material.STONE;
                if (stripes.contains(z)) m = colours[(z / LegibilityTrack.MARKER_EVERY) % 2];
                world.getBlockAt(x, y, z).setType(m, false);
                for (int h = 1; h <= LegibilityTrack.PILLAR_HEIGHT + 1; h++)
                    if (world.getBlockAt(x, y + h, z).getType() != Material.AIR) world.getBlockAt(x, y + h, z).setType(Material.AIR, false);
            }
        for (var e : world.getEntitiesByClass(org.bukkit.entity.TextDisplay.class))
            if (e.getScoreboardTags().contains("legibility_marker")) e.remove();
        for (int d : LegibilityTrack.markerDistances()) {
            int z = LegibilityTrack.markerZ(d);
            world.spawn(new Location(world, LegibilityTrack.MIN_X - 0.5, y + 1.2, z + .5), org.bukkit.entity.TextDisplay.class, t -> {
                t.text(net.kyori.adventure.text.Component.text(d + " blocks"));
                t.setBillboard(org.bukkit.entity.Display.Billboard.CENTER);
                t.setPersistent(false);
                t.addScoreboardTag("legibility_marker");
            });
        }
    }

    // ---- applying a variant --------------------------------------------------------

    private void apply(Active a) {
        Player s = a.subject.player();
        var scale = s.getAttribute(Attribute.SCALE);
        if (scale != null) scale.setBaseValue(a.variant.scale());
        s.setGlowing(a.variant.glow());
        var eq = s.getEquipment();
        if (eq != null) {
            String prefix = switch (a.variant.armor()) {
                case LEATHER -> "LEATHER"; case IRON -> "IRON"; case DIAMOND -> "DIAMOND"; case NETHERITE -> "NETHERITE"; default -> null;
            };
            eq.setHelmet(prefix == null ? null : new ItemStack(Material.valueOf(prefix + "_HELMET")));
            eq.setChestplate(prefix == null ? null : new ItemStack(Material.valueOf(prefix + "_CHESTPLATE")));
            eq.setLeggings(prefix == null ? null : new ItemStack(Material.valueOf(prefix + "_LEGGINGS")));
            eq.setBoots(prefix == null ? null : new ItemStack(Material.valueOf(prefix + "_BOOTS")));
        }
        stopParticles(a);
        Particle type = switch (a.variant.particles()) {
            case FLAME -> Particle.FLAME; case SOUL_FIRE_FLAME -> Particle.SOUL_FIRE_FLAME; case END_ROD -> Particle.END_ROD; default -> null;
        };
        if (type != null) {
            a.particles = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
                if (!s.isOnline()) return;
                double h = a.variant.heightBlocks(), w = 0.3 * a.variant.scale();
                s.getWorld().spawnParticle(type, s.getLocation().add(0, h / 2, 0), 6, w, h * 0.25, w, 0.01);
            }, 2L, 4L);
        }
        clearPillars(a);
        if (a.variant.clutter()) {
            a.pillars = LegibilityTrack.clutter(CLUTTER_SEED, CLUTTER_DENSITY);
            for (var p : a.pillars)
                for (int h = 1; h <= p.height(); h++)
                    world.getBlockAt(p.x(), LegibilityTrack.FLOOR_Y + h, p.z()).setType(Material.STONE_BRICKS, false);
        }
    }

    private void stopParticles(Active a) { if (a.particles != null) { a.particles.cancel(); a.particles = null; } }

    private void clearPillars(Active a) {
        for (var p : a.pillars)
            for (int h = 1; h <= p.height(); h++)
                world.getBlockAt(p.x(), LegibilityTrack.FLOOR_Y + h, p.z()).setType(Material.AIR, false);
        a.pillars = List.of();
    }

    // ---- the report ------------------------------------------------------------------

    /** The report as lines, so it can be shown, logged or asserted on. */
    public List<String> reportLines(Player tester) {
        var a = active.get(tester.getUniqueId());
        return a == null ? List.of() : reportLines(a, tester);
    }

    private List<String> reportLines(Active a, Player p) {
        var out = new ArrayList<String>();
        out.add(ChatColor.GOLD + "Legibility report. Now showing: " + a.variant.signature());
        out.add(ChatColor.GRAY + "Marks are YOUR judgement of where it still reads. Pixels assume FOV "
                + (int) Legibility.DEFAULT_FOV + ", " + Legibility.DEFAULT_SCREEN_HEIGHT + "p; set them to yours before trusting them.");
        if (a.log.signatures().isEmpty()) out.add("No marks yet.");
        for (String sig : a.log.signatures()) {
            var s = a.log.summary(sig);
            double height = heightOf(sig);
            out.add(String.format(Locale.ROOT, "%s: %d mark(s), %.1f / %.1f / %.1f blocks (min / median / max); %.0f px tall at the median",
                    sig, s.count(), s.min(), s.median(), s.max(), Legibility.pixelHeight(height, s.median(), Legibility.DEFAULT_FOV, Legibility.DEFAULT_SCREEN_HEIGHT)));
        }
        double d = Math.max(horizontal(p.getLocation(), a.subject.player().getLocation()), 0.1);
        out.add(String.format(Locale.ROOT, "You are %.1f blocks away: the subject subtends %.1f degrees, about %.0f px tall.",
                d, Legibility.angularHeightDegrees(a.variant.heightBlocks(), d),
                Legibility.pixelHeight(a.variant.heightBlocks(), d, Legibility.DEFAULT_FOV, Legibility.DEFAULT_SCREEN_HEIGHT)));
        if (a.variant.glow()) {
            double r = Legibility.PROTOTYPE_RADIUS;
            out.add("The settled RULE for this glow, as a self-empowerment (radius " + (int) r + " is a PROTOTYPE value):");
            out.add("  ally: " + Legibility.explain(Legibility.Tier.EMPOWERMENT, new Legibility.Observer(Legibility.Relation.ALLY, false, d), r, false));
            out.add("  enemy in combat at your distance: " + Legibility.explain(Legibility.Tier.EMPOWERMENT, new Legibility.Observer(Legibility.Relation.ENEMY, true, d), r, false));
            out.add("  enemy out of combat: " + Legibility.explain(Legibility.Tier.EMPOWERMENT, new Legibility.Observer(Legibility.Relation.ENEMY, false, d), r, false));
            out.add(ChatColor.YELLOW + "What you SEE is the universal glow flag, the same for every viewer (Tier 1's behavior). Observer-relative glow is not delivered.");
        }
        return out;
    }

    /** The height a signature's variant stands, recovered from its scale. */
    private static double heightOf(String signature) {
        try {
            String s = signature.substring("scale ".length(), signature.indexOf(' ', "scale ".length()));
            return LegibilityVariant.PLAYER_HEIGHT * Double.parseDouble(s);
        } catch (RuntimeException ex) { return LegibilityVariant.PLAYER_HEIGHT; }
    }
}
