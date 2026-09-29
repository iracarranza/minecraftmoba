package com.minecraftmoba.plugin;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Bounding: a fixed series of leaps along one heading, with the animals coming too.
 *
 * Ported from {@code codex/lightfooted-from-phase1}. Lightfooted previously had
 * {@code a2: sinkhole_lite} on main -- the shared prototype stub -- which is
 * Mole's ability and says nothing about this class.
 *
 * <h2>The heading is locked at the first bound</h2>
 *
 * Each leap reuses the heading taken at launch rather than re-reading where the
 * player is looking. That makes Bounding a <b>committed line</b>, not steerable
 * flight: it crosses ground fast and cannot chase. A version that re-aimed each
 * bound would be a better pursuit tool than anything else in the game and would
 * make the mobility class also the best duellist, which is the opposite of what
 * a travel ability should do.
 *
 * <h2>Nearby animals are carried along</h2>
 *
 * Wolves, foxes and cats near the player are launched on the same heading. The
 * class's passive depends on having animals with you, so a travel ability that
 * left them behind would spend the passive every time it was used -- the
 * ability and the passive would actively fight each other.
 *
 * <h2>Attacking is refused mid-bound</h2>
 *
 * A player cannot swing while bounding. Without this the ability is a free
 * gap-closer with an attack on the end, and its combat declaration would be a
 * formality; with it, Bounding commits you to travelling.
 *
 * <h2>The branches read the terrain you LEAVE</h2>
 *
 * All three are about takeoff, not landing, so each is sampled at the moment a
 * bound launches. The branch is resolved once per activation and carried on the
 * run: a player cannot change branch mid-flight, and the scheduled leaps have
 * no context to re-read it from anyway.
 *
 * [WORKING] Every value here is a declared fixture carried over from the source
 * branch, where they were labelled provisional calibration. None has been
 * played.
 */
public final class BoundingAbility implements Ability {

    /** One in-flight bound run: how many leaps remain, the shared heading, and branch state. */
    private static final class Run {
        int remaining;
        final Vector heading;
        final String branch;
        /** Track to Track: a bonus bound may not itself earn another. */
        boolean lastWasBonus;
        /** Drift to Drift: invisibility persists until the NEXT bound leaves the ground. */
        boolean drifting;
        Run(int remaining, Vector heading, String branch) {
            this.remaining = remaining; this.heading = heading; this.branch = branch;
        }
    }

    private final MobaPlugin plugin;
    private final ConfigurationSection config;
    private final Map<UUID, Run> running = new HashMap<>();

    public BoundingAbility(MobaPlugin plugin, ConfigurationSection config) {
        this.plugin = plugin;
        this.config = Objects.requireNonNull(config);
        if (config.getLong("cooldownTicks") < 0)
            throw new IllegalArgumentException("Negative cooldown: bounding");
        if (config.getInt("bounds") <= 0)
            throw new IllegalArgumentException("Bounding must make at least one bound.");
    }

    @Override public String id() { return "bounding"; }
    @Override public String displayName() { return Objects.requireNonNull(config.getString("displayName")); }
    @Override public long cooldownTicks() { return config.getLong("cooldownTicks"); }

    @Override public Map<String, String> branches() {
        return Map.of("drift_to_drift", "Drift to Drift",
                      "branch_to_branch", "Branch to Branch",
                      "track_to_track", "Track to Track");
    }
    @Override public List<String> branchIds() {
        return List.of("drift_to_drift", "branch_to_branch", "track_to_track");
    }

    /** True while the player is mid-run. {@link AbilityInputs} uses this to refuse attacks. */
    @Override public boolean active(Player player) { return running.containsKey(player.getUniqueId()); }

    @Override public void cancel(Player player) {
        Run run = running.remove(player.getUniqueId());
        // Leaving a cancelled drift invisible would be a permanent stealth bug
        // reachable by dying mid-bound, which is the cheapest thing in the game
        // to do on purpose.
        if (run != null && run.drifting) player.setInvisible(false);
    }

    @Override
    public boolean execute(Player p, AbilityContext ctx) {
        if (active(p)) return false;
        Vector heading = p.getLocation().getDirection().setY(0);
        // Looking straight up or down leaves no horizontal heading, and
        // normalizing a zero vector produces NaN -- which would teleport the
        // player out of the world rather than fail visibly.
        if (heading.lengthSquared() < 1.0e-6) heading = new Vector(0, 0, 1);
        running.put(p.getUniqueId(),
                new Run(config.getInt("bounds"), heading.normalize(), ctx.branchFor(id())));
        p.sendActionBar(Component.text("Bounding"));
        launch(p);
        return true;
    }

    private void launch(Player p) {
        Run run = running.get(p.getUniqueId());
        if (run == null) return;
        if (run.remaining <= 0 || !p.isOnline() || p.isDead()) { cancel(p); return; }

        // Drift to Drift ends here: the invisibility granted by the PREVIOUS
        // takeoff lasts exactly until this bound leaves the ground.
        if (run.drifting) { p.setInvisible(false); run.drifting = false; }

        boolean bonus = run.lastWasBonus;
        run.lastWasBonus = false;
        run.remaining--;

        double speed = config.getDouble("boundSpeed");
        double lift = config.getDouble("boundLift");

        if ("branch_to_branch".equals(run.branch) && takingOffFrom(p, "leaves")) {
            // Resolved a section at a time rather than by a dotted literal.
            // ConfigKeysDefinedTest checks dotted strings from the config
            // ROOT, so a section-relative "branches.x.y" reads as undefined --
            // and the way to satisfy that guard is to stop writing a dotted
            // path, not to hide one from the scanner.
            ConfigurationSection b = branchConfig(run.branch);
            if (b != null) {
                speed *= b.getDouble("speedMultiplier", 1.35);
                lift += b.getDouble("extraLift", 0.35);
            }
        }

        if ("drift_to_drift".equals(run.branch) && takingOffFrom(p, "snow")) {
            p.setInvisible(true);
            run.drifting = true;
        }

        // A bonus bound cannot earn another, so the check is skipped entirely
        // on one -- otherwise dirt-family terrain would make Bounding endless.
        if ("track_to_track".equals(run.branch) && !bonus && takingOffFrom(p, "dirt")) {
            run.remaining++;
            run.lastWasBonus = true;
        }

        Vector leap = run.heading.clone().multiply(speed).setY(lift);
        p.setVelocity(leap);

        double radius = config.getDouble("animalRadius");
        for (Entity e : p.getNearbyEntities(radius, radius, radius))
            if (AnimalSenses.species(e) != null) e.setVelocity(leap.clone());

        Bukkit.getScheduler().runTaskLater(plugin, () -> launch(p), config.getLong("boundTicks"));
    }

    /**
     * Whether the player is leaving the named terrain family.
     *
     * Both the block at the feet and the one below count, because the two
     * cases genuinely differ: a snow LAYER occupies the feet block while the
     * player stands inside it, and a full snow BLOCK is beneath them. Checking
     * only one would make the branch work on half of its own terrain.
     *
     * Matched by material NAME rather than an enumerated set, so a new wood's
     * leaves or a new snow variant joins the family without a code change.
     */
    static boolean matchesFamily(Material at, Material below, String family) {
        return matches(at, family) || matches(below, family);
    }

    private static boolean matches(Material m, String family) {
        String n = m.name();
        return switch (family) {
            case "snow" -> n.contains("SNOW");
            case "leaves" -> n.endsWith("LEAVES");
            case "dirt" -> n.contains("DIRT") || n.equals("GRASS_BLOCK")
                        || n.equals("PODZOL") || n.equals("MYCELIUM")
                        || n.equals("ROOTED_DIRT") || n.equals("MUD");
            default -> false;
        };
    }

    /** This branch's tunable values, or null where the branch declares none. */
    private ConfigurationSection branchConfig(String branch) {
        if (branch == null) return null;
        ConfigurationSection all = config.getConfigurationSection("branches");
        return all == null ? null : all.getConfigurationSection(branch);
    }

    private boolean takingOffFrom(Player p, String family) {
        Location at = p.getLocation();
        return matchesFamily(at.getBlock().getType(),
                             at.clone().subtract(0, 1, 0).getBlock().getType(), family);
    }
}
