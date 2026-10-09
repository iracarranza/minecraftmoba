package com.minecraftmoba.plugin;

import org.bukkit.attribute.Attribute;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Dispatch for class passives, keyed by {@code ClassDefinition.passiveHook}.
 *
 * <h2>Why this exists</h2>
 *
 * {@code passiveHook} has been declared on every class and read by nothing. It
 * was parsed, stored, exposed on the record, and never dispatched -- so every
 * class's defining always-on characteristic was config that did nothing, and
 * the only implementation of one anywhere lived on an unmerged branch.
 *
 * This is the seam that makes the key mean something. A passive is named in
 * config and resolved here; adding one is a case in this class plus its own
 * arithmetic, not a change to the class registry.
 *
 * <h2>Passives never set combat state</h2>
 *
 * A passive is not an activation, and an always-on effect that marked combat
 * would leave its class permanently in combat -- unable to level up, Recall, or
 * hold any out-of-combat effect. So nothing here touches {@link CombatState}.
 * See docs/design/COMBAT_STATE.md §3.
 *
 * <h2>Speed is applied, not polled</h2>
 *
 * Damage and fall reduction are event-shaped and are read at the event. Speed
 * is a standing attribute, so it is written on a cadence and removed when the
 * player stops qualifying. The removal path is the one that matters: a speed
 * bonus left behind when the foxes wander off, or when the player switches
 * class, is a permanent buff nothing would ever take back.
 */
public final class Passives implements Listener {

    /** Lightfooted. Wolves mitigate damage, cats soften falls, foxes add speed. */
    public static final String ANIMAL_SENSES = "animal_senses";

    /**
     * Mole. Breaking sand or gravel sifts the material connected to it.
     *
     * Named for what classes.md calls the passive. The config key used to read
     * {@code mole_digging}, which named a different thing and dispatched to
     * nothing -- so the rename and the wiring landed together rather than
     * leaving a hook whose spelling still disagreed with the design.
     */
    public static final String SIFTH_SENSE = "sifth_sense";

    private final MobaPlugin plugin;
    /** Players currently carrying a passive speed modifier, so it can be taken back. */
    private final Set<UUID> speeded = new HashSet<>();

    public Passives(MobaPlugin plugin) { this.plugin = plugin; }

    public boolean enabled() { return plugin.getConfig().getBoolean("passives.enabled"); }

    /** The passive hook for a player's class, or empty when they have none. */
    private String hook(Player p) {
        if (!plugin.enrolled(p) || plugin.inputs() == null) return "";
        var data = plugin.data(p);
        if (data == null || data.classId == null) return "";
        ClassDefinition definition = plugin.inputs().definition(data.classId);
        return definition == null ? "" : definition.passiveHook();
    }

    private boolean has(Player p, String hook) { return enabled() && hook.equals(hook(p)); }

    private double radius() { return plugin.getConfig().getDouble("passives.animalSenses.radius"); }
    private int cap() { return plugin.getConfig().getInt("passives.animalSenses.speciesCap"); }

    // ---- damage -----------------------------------------------------------

    /**
     * Animal Senses' two mitigations.
     *
     * Listening to EntityDamageEvent rather than the by-entity form is
     * deliberate here and is the opposite of {@link CombatState}'s choice: fall
     * damage is environmental, and softening a fall is exactly what the cats
     * are for. Wolf mitigation applies to all damage for the same reason it
     * reads as protection rather than as a combat bonus.
     *
     * HIGH and ignoreCancelled so it scales a figure other plugins have already
     * settled, and never resurrects a hit something else cancelled.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player p) || !has(p, ANIMAL_SENSES)) return;
        var bonus = AnimalSenses.nearby(p, radius());
        if (bonus.none()) return;

        double scale = AnimalSenses.damageMultiplier(bonus.wolves(),
                plugin.getConfig().getDouble("passives.animalSenses.wolfDamageReduction"), cap());
        if (e.getCause() == EntityDamageEvent.DamageCause.FALL)
            scale *= AnimalSenses.fallMultiplier(bonus.cats(),
                    plugin.getConfig().getDouble("passives.animalSenses.catFallReduction"), cap());
        e.setDamage(e.getDamage() * scale);
    }

    // ---- speed ------------------------------------------------------------

    /** Apply or withdraw the fox speed bonus. Called from the plugin's timer. */
    public void tick() {
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            if (!has(p, ANIMAL_SENSES)) { withdrawSpeed(p); continue; }
            double bonus = AnimalSenses.speedBonus(AnimalSenses.nearby(p, radius()).foxes(),
                    plugin.getConfig().getDouble("passives.animalSenses.foxSpeedBonus"), cap());
            if (bonus <= 0) { withdrawSpeed(p); continue; }
            applySpeed(p, bonus);
        }
    }

    private void applySpeed(Player p, double bonus) {
        var attribute = p.getAttribute(Attribute.MOVEMENT_SPEED);
        if (attribute == null) return;
        attribute.setBaseValue(attribute.getDefaultValue() * (1 + bonus));
        speeded.add(p.getUniqueId());
    }

    /**
     * Put movement speed back to default.
     *
     * Only for players this class actually modified. Resetting everyone's base
     * value every tick would silently overwrite any other speed effect in the
     * game, which is the sort of thing that is only noticed much later and from
     * a completely unrelated direction.
     */
    private void withdrawSpeed(Player p) {
        if (!speeded.remove(p.getUniqueId())) return;
        var attribute = p.getAttribute(Attribute.MOVEMENT_SPEED);
        if (attribute != null) attribute.setBaseValue(attribute.getDefaultValue());
    }

    // ---- Sifth Sense -------------------------------------------------------

    /**
     * Sift the material connected to a broken sand or gravel block.
     *
     * MONITOR and ignoreCancelled: the break has to have actually happened.
     * Sifting a mass around a block that some protection then refused would
     * clear terrain nobody was allowed to touch.
     *
     * The reach is {@link SifthSense}; this supplies the world. Everything
     * here is the part that cannot be tested without a server -- what a
     * position holds, whether a player put it there, and taking it away.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent e) {
        Player p = e.getPlayer();
        if (!has(p, SIFTH_SENSE)) return;
        Block broken = e.getBlock();
        World world = broken.getWorld();

        var origin = new SifthSense.At(broken.getX(), broken.getY(), broken.getZ());
        var found = SifthSense.reach(origin, at -> siftable(world, at), radiusBlocks(), volume());
        if (found.isEmpty()) return;

        var tool = p.getInventory().getItemInMainHand();
        for (SifthSense.At at : found) {
            Block block = world.getBlockAt(at.x(), at.y(), at.z());
            block.breakNaturally(tool);
        }
    }

    /**
     * Whether a position holds material this sense acts on.
     *
     * The provenance check is not incidental. Sifting is faster than mining
     * and reaches through a whole connected mass, so without it a built sand
     * or gravel wall would come apart in a single hit -- a siege ability
     * nobody designed. Tunneling already refuses player-placed blocks for the
     * weaker version of the same reason.
     */
    private boolean siftable(World world, SifthSense.At at) {
        Block block = world.getBlockAt(at.x(), at.y(), at.z());
        if (!SIFTABLE.contains(block.getType())) return false;
        return !plugin.provenance().isPlayerPlaced(block);
    }

    /**
     * The material Sifth Sense acts on.
     *
     * classes.md says "sand and gravel", so that is what this is. Concrete
     * powder also falls, and is deliberately absent: it is a BUILT material,
     * and sifting it would make this an answer to somebody else's
     * construction rather than a way of moving through terrain.
     */
    private static final java.util.Set<Material> SIFTABLE =
            java.util.Set.of(Material.SAND, Material.RED_SAND, Material.GRAVEL);

    private int radiusBlocks() {
        return plugin.getConfig().getInt("passives.sifthSense.radius", SifthSense.DEFAULT_RADIUS);
    }

    private int volume() {
        return plugin.getConfig().getInt("passives.sifthSense.volume", SifthSense.DEFAULT_VOLUME);
    }

    @EventHandler public void onQuit(PlayerQuitEvent e) { speeded.remove(e.getPlayer().getUniqueId()); }

    public String report(Player p) {
        String hook = hook(p);
        var bonus = AnimalSenses.nearby(p, radius());
        return "PASSIVES enabled=" + enabled() + " hook=" + (hook.isEmpty() ? "none" : hook)
                + " wolves=" + bonus.wolves() + " foxes=" + bonus.foxes() + " cats=" + bonus.cats()
                + " cap=" + cap();
    }
}
