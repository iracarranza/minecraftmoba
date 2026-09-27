package com.minecraftmoba.plugin;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.HashMap;
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
 * [WORKING] Every value here is a declared fixture carried over from the source
 * branch, where they were labelled provisional calibration. None has been
 * played.
 */
public final class BoundingAbility implements Ability {

    /** One in-flight bound run: how many leaps remain, and the heading they share. */
    private static final class Run {
        int remaining;
        final Vector heading;
        Run(int remaining, Vector heading) { this.remaining = remaining; this.heading = heading; }
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

    /** True while the player is mid-run. {@link AbilityInputs} uses this to refuse attacks. */
    @Override public boolean active(Player player) { return running.containsKey(player.getUniqueId()); }

    @Override public void cancel(Player player) { running.remove(player.getUniqueId()); }

    @Override
    public boolean execute(Player p, AbilityContext ctx) {
        if (active(p)) return false;
        Vector heading = p.getLocation().getDirection().setY(0);
        // Looking straight up or down leaves no horizontal heading, and
        // normalizing a zero vector produces NaN -- which would teleport the
        // player out of the world rather than fail visibly.
        if (heading.lengthSquared() < 1.0e-6) heading = new Vector(0, 0, 1);
        running.put(p.getUniqueId(), new Run(config.getInt("bounds"), heading.normalize()));
        p.sendActionBar(Component.text("Bounding"));
        launch(p);
        return true;
    }

    private void launch(Player p) {
        Run run = running.get(p.getUniqueId());
        if (run == null) return;
        if (run.remaining <= 0 || !p.isOnline() || p.isDead()) { running.remove(p.getUniqueId()); return; }
        run.remaining--;

        double speed = config.getDouble("boundSpeed");
        double lift = config.getDouble("boundLift");
        Vector leap = run.heading.clone().multiply(speed).setY(lift);
        p.setVelocity(leap);

        double radius = config.getDouble("animalRadius");
        for (Entity e : p.getNearbyEntities(radius, radius, radius))
            if (AnimalSenses.species(e) != null) e.setVelocity(leap.clone());

        Bukkit.getScheduler().runTaskLater(plugin, () -> launch(p), config.getLong("boundTicks"));
    }
}
