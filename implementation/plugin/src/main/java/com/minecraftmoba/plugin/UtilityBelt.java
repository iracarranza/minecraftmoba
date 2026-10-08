package com.minecraftmoba.plugin;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Toolbox's passive: incoming damage runs the inventory circuit.
 *
 * <h2>A machine reacting to being hit, not a combat state</h2>
 *
 * Any damage procs it -- players, mobs, fall, fire, suffocation. That is a
 * deliberate divergence from every other combat-gated ability in this plugin:
 * the Utility Belt is not a thing Toolbox does in combat, it is a thing that
 * happens to Toolbox when struck, and a belt that ignored a Ravager or a lava
 * pool would be a belt with a rule nobody could predict.
 *
 * <h2>The circuit is piloted, not pre-programmed</h2>
 *
 * A trigger reads the program's SHAPE once -- which is fixed the moment the
 * inventory is read -- and then resolves each component on its own tick
 * against whatever the world looks like then. Facing, target positions,
 * visibility and block geometry are all sampled at resolution.
 *
 * That is why this schedules individual steps rather than executing a circuit
 * in a loop: the gap between components is where the player does their half of
 * the work.
 *
 * <h2>Consumption is per resolution, not per cell</h2>
 *
 * Every resolved component drops one item from its slot, and the same physical
 * slot may fire more than once when a hook and ordinary flow both reach it --
 * each firing costs another item. So geometry buys execution density per
 * occupied coordinate, never free resources.
 *
 * <h2>[OPEN] The cooldown starts at trigger</h2>
 *
 * Implemented at trigger, which is what the duty-cycle table assumes. Starting
 * it at completion would make the Lv25 cycle 3.25 + 6 seconds and drop duty
 * from 54% to 35% -- the difference between a near-continuous machine and a
 * bursty one. Recorded in the grammar as unresolved; this is the arm that was
 * costed, not a decision to close it.
 */
public final class UtilityBelt implements Listener {

    private final MobaPlugin plugin;
    private final ToolboxStatuses statuses;

    /** When each Toolbox's belt is ready again. */
    private final Map<UUID, Long> ready = new HashMap<>();
    /** The circuit origin, which an Observer moves off Toolbox. */
    /**
     * One circuit activation's own state.
     *
     * Was two maps keyed by PLAYER, which worked only because the passive
     * cannot overlap itself -- its cooldown guarantees one run at a time. A2
     * fires the same components on its own charges, so two runs can now be in
     * flight for one player, and player-keyed state would have them reading
     * and clearing each other's origin and amplification.
     *
     * Per run, both are ordinary fields and the question disappears.
     */
    static final class Run {
        UUID projectedOrigin;
        int amplification;
    }

    /** Amplification for the NEXT component only: Base, +1 or +2. */


    public UtilityBelt(MobaPlugin plugin, ToolboxStatuses statuses) {
        this.plugin = plugin;
        this.statuses = statuses;
    }

    /** The class this belongs to. Anything else is not running a circuit. */
    private static final String CLASS_ID = "toolbox";

    private boolean isToolbox(Player p) {
        var data = plugin.data(p);
        return data != null && CLASS_ID.equals(data.classId);
    }

    // ---- the trigger ------------------------------------------------------

    /**
     * MONITOR, and never cancelling.
     *
     * The belt reacts to damage that actually landed; it does not participate
     * in deciding whether damage lands. Reading a cancelled event would fire
     * the machine off a hit that never happened.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void damaged(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player p) || !isToolbox(p)) return;
        if (!plugin.enrolled(p)) return;
        trigger(p);
    }

    /** Run the circuit if the cooldown allows it. Returns whether it fired. */
    public boolean trigger(Player p) {
        long now = plugin.getServer().getCurrentTick();
        Long until = ready.get(p.getUniqueId());
        if (until != null && until > now) return false;

        var circuit = read(p);
        if (circuit.isEmpty()) return false;

        ready.put(p.getUniqueId(), now + cooldownTicks(p));

        Run run = new Run();
        for (var step : circuit.steps())
            plugin.getServer().getScheduler().runTaskLater(plugin,
                    () -> resolve(p, step, run), step.tick());
        return true;
    }

    /**
     * Fire the first {@code count} components of the same circuit, now.
     *
     * A2's entire mechanism. There is no separate A2 circuit -- that is the
     * point of the ability -- so this runs the same steps the passive would,
     * read from the same board, on its own {@link Run}.
     *
     * <h2>It does not touch the passive's cooldown</h2>
     *
     * Neither reading it nor setting it. A2 is charge-limited and the passive
     * is cooldown-limited, and classes.md wants them alternating rather than
     * gating one another: A2 spent while the belt is cooling is the intended
     * rhythm, not an exploit.
     *
     * <h2>The steps run immediately, without their authored delays</h2>
     *
     * A circuit's tick offsets are the program's timing, and the passive
     * honours them. A2 is a manual jolt of the first components rather than a
     * short run of the program, so it resolves them in order on this tick. A
     * two-component A2 that took a second to finish because slot two carried a
     * Repeater would be reporting the circuit's shape rather than firing it.
     *
     * @return how many components actually resolved, which Short Circuit reads
     */
    public int jumpstart(Player p, int count) {
        var circuit = circuitOf(p);
        if (circuit == null || circuit.isEmpty() || count <= 0) return 0;
        Run run = new Run();
        int fired = 0;
        for (var step : circuit.steps()) {
            if (fired >= count) break;
            if (resolve(p, step, run)) fired++;
        }
        return fired;
    }

    /**
     * The player's current circuit, for anything that needs to read the board
     * without firing it.
     *
     * A1 rearranges the same board A2 and the passive run, so it must see the
     * same reading -- including the unlocked width and the flow order. A second
     * copy of this conversion would be a second definition of what the circuit
     * IS, and they would drift.
     */
    public CircuitReader.Circuit circuitOf(Player p) { return read(p); }

    /** Read the board out of the live inventory at its unlocked width. */
    private CircuitReader.Circuit read(Player p) {
        ItemStack[] contents = p.getInventory().getContents();
        Material[] materials = new Material[contents.length];
        for (int i = 0; i < contents.length; i++)
            materials[i] = contents[i] == null ? null : contents[i].getType();
        return CircuitReader.read(materials, plugin.unlockedSlots(p));
    }

    /**
     * Cooldown on the ABILITY clock: 16 / 14 / 12 / 10 / 6 seconds.
     *
     * At Lv 0 / 5 / 10 / 20 / 25. The fourth step is at 20 rather than 15
     * because Lv15 already grants the Ultimate, and budgets need not be equal
     * at every event -- stacking an improvement there would make one level
     * enormous and leave Lv20 empty.
     *
     * Read from config so it stays a [WORKING] number rather than a constant.
     */
    long cooldownTicks(Player p) {
        int level = plugin.data(p) == null ? 0 : plugin.data(p).level;
        var section = plugin.getConfig().getConfigurationSection(
                "abilities.definitions.utility_belt.cooldownByLevel");
        if (section == null) return 16 * 20L;
        long chosen = 16 * 20L;
        for (String key : section.getKeys(false)) {
            int at = Integer.parseInt(key.replace("level", ""));
            if (level >= at) chosen = section.getLong(key);
        }
        return chosen;
    }

    // ---- resolution -------------------------------------------------------

    /**
     * One component, against the world as it is now.
     *
     * The step's own tick decided WHEN; everything about WHAT is read here, at
     * resolution, which is what makes the circuit piloted.
     */
    private boolean resolve(Player p, CircuitReader.Step step, Run run) {
        if (!p.isOnline() || p.isDead()) return false;
        // The player may have rearranged the inventory mid-circuit. The
        // program's shape was fixed at trigger, but an item that is no longer
        // there cannot be spent, and firing a component nobody is carrying
        // would make consumption a fiction.
        if (!spend(p, step)) return false;
        effect(p, step.component(), run);
        return true;
    }

    /**
     * One component's effect, with no inventory cost.
     *
     * Split out of {@link #resolve} because the Ultimate activates components
     * that are <b>world blocks</b> rather than inventory items. A world piston
     * is not a piston in a slot, so there is nothing to spend -- but its effect
     * is identical, and a second copy of this switch would be a second
     * definition of what each component DOES.
     *
     * Dispenser and Dropper still cost an item, because they draw from
     * Toolbox's inventory by definition rather than by being a component in it.
     */
    void effect(Player p, CircuitComponent component, Run run) {
        Entity origin = origin(p, run);
        int amp = run.amplification;
        boolean amplifies = false;

        switch (component) {
            case OBSERVER -> observe(p, origin, run);
            case TORCH -> { if (origin != p) statuses.illuminate(origin); }
            case PISTON -> impulse(p, origin, p.getLocation().getDirection(), amp);
            case STICKY_PISTON -> {
                impulse(p, origin, p.getLocation().getDirection().multiply(-1), amp);
                if (origin instanceof LivingEntity living && Targetability.status(living, p))
                    statuses.root(living);
            }
            case COMPARATOR -> { amplify(run); amplifies = true; }
            case DISPENSER -> fire(p, origin, false);
            case DROPPER -> fire(p, origin, true);
            case HOPPER -> vacuum(p);
            case NOTE_BLOCK -> p.getWorld().playSound(p.getLocation(),
                    org.bukkit.Sound.BLOCK_NOTE_BLOCK_HARP, 1f, 1f);
            case DUST -> discharge(p);
            case TRIPWIRE_HOOK, REPEATER, INERT -> { /* structural: timing and routing only */ }
        }

        // Amplification is for the NEXT component only, so anything that is
        // not itself a Comparator clears it after use.
        if (!amplifies) run.amplification = 0;
    }

    /** Take one item from the step's slot, or decline to resolve. */
    private boolean spend(Player p, CircuitReader.Step step) {
        var inventory = p.getInventory();
        ItemStack item = inventory.getItem(step.slot());
        if (item == null || CircuitComponent.of(item.getType()) != step.component()) return false;
        item.setAmount(item.getAmount() - 1);
        if (item.getAmount() <= 0) inventory.setItem(step.slot(), null);
        return true;
    }

    /** The current circuit origin: Toolbox, or whatever an Observer acquired. */
    private Entity origin(Player p, Run run) {
        UUID id = run.projectedOrigin;
        if (id == null) return p;
        Entity target = plugin.getServer().getEntity(id);
        // A lost target falls back to Toolbox rather than making the rest of
        // the circuit a no-op: the machine keeps running from where it stands.
        if (target == null || target.isDead() || !statuses.isObserved(target)) {
            run.projectedOrigin = null;
            return p;
        }
        return target;
    }

    /**
     * Acquire what Toolbox is aiming at FROM THE CURRENT ORIGIN, and project there.
     *
     * Searching from the origin rather than from Toolbox is what permits relay:
     * repeated Observers leapfrog Toolbox to A to B to C. It is expensive in
     * quartz-heavy components, drops, circuit time and live aim, and most of
     * such a machine is spent on projection alone -- which is the point.
     */
    private void observe(Player p, Entity origin, Run run) {
        Entity found = aimedAt(p, origin);
        if (found == null) return;
        statuses.observe(found);
        run.projectedOrigin = found.getUniqueId();
    }

    /** The nearest valid target along Toolbox's live facing, from a given origin. */
    private Entity aimedAt(Player p, Entity origin) {
        Location from = origin.getLocation().add(0, origin.getHeight() * 0.9, 0);
        Vector direction = p.getLocation().getDirection();
        double range = plugin.getConfig().getDouble("abilities.definitions.utility_belt.observeRange", 24.0);
        var hit = origin.getWorld().rayTraceEntities(from, direction, range, 1.0,
                e -> e != origin && e != p && Targetability.status(e, p));
        return hit == null ? null : hit.getHitEntity();
    }

    /**
     * A directional impulse, in Toolbox's facing at THIS tick.
     *
     * Applied to everything around the origin that can be moved, which
     * deliberately includes dropped items -- denying a kill's drops, or
     * recovering one's own shed components, are both real uses.
     *
     * Velocity stacks. Concurrent pushes add, because velocity is not damage
     * and nothing in vanilla gates it the way invulnerability frames gate
     * hits; a bank of Pistons is one enormous launch. That is intended.
     */
    private void impulse(Player p, Entity origin, Vector direction, int amplification) {
        double power = plugin.getConfig().getDouble(
                "abilities.definitions.utility_belt.pistonPower", 1.2) * (1 + amplification * 0.5);
        double radius = plugin.getConfig().getDouble(
                "abilities.definitions.utility_belt.pistonRadius", 3.0);
        for (Entity target : origin.getWorld().getNearbyEntities(
                origin.getLocation(), radius, radius, radius)) {
            if (!Targetability.impulse(target, p)) continue;
            Vector push = direction.clone().normalize().multiply(power);
            if (statuses.consumeIlluminated(target))
                push.multiply(plugin.getConfig().getDouble(
                        "abilities.definitions.utility_belt.illuminatedKnockback", 2.5));
            target.setVelocity(target.getVelocity().add(push));
        }
    }

    /** Comparator: convert the amplification ladder one step, to a ceiling of +2. */
    private void amplify(Run run) {
        run.amplification = Math.min(2, run.amplification + 1);
    }

    /**
     * Take the FIRST item that is not program and use it from the origin.
     *
     * Neither Dispenser nor Dropper filters for eligible items: both take the
     * first, whatever it is. Arranging the inventory so that TNT is first IS
     * the programming, and a machine that helpfully searched past a diamond
     * would be making a strategic decision on Toolbox's behalf.
     */
    private void fire(Player p, Entity origin, boolean throwIt) {
        var inventory = p.getInventory();
        var circuit = read(p);
        int slot = circuit.magazine();
        if (slot < 0) return;
        ItemStack ammunition = inventory.getItem(slot);
        if (ammunition == null || ammunition.getType().isAir()) return;

        Location from = origin.getLocation().add(0, origin.getHeight() * 0.9, 0);
        Vector direction = p.getLocation().getDirection();
        ItemStack one = ammunition.clone();
        one.setAmount(1);

        if (!throwIt && one.getType() == Material.ARROW) {
            // Vanilla's own dispenser velocity, which is what makes a
            // projected arrow a third of a bow shot rather than a free one.
            Arrow arrow = origin.getWorld().spawnArrow(from, direction, 1.1f, 6f);
            arrow.setShooter(p);
        } else if (!throwIt && one.getType() == Material.TNT) {
            TNTPrimed tnt = origin.getWorld().spawn(from, TNTPrimed.class);
            tnt.setSource(p);
        } else {
            Item dropped = origin.getWorld().dropItem(from, one);
            dropped.setVelocity(direction.clone().multiply(throwIt ? 0.8 : 0.3));
            dropped.setThrower(p.getUniqueId());
        }

        ammunition.setAmount(ammunition.getAmount() - 1);
        if (ammunition.getAmount() <= 0) inventory.setItem(slot, null);
    }

    /** Hopper: recover what the machine shed, which is what makes it load-bearing. */
    private void vacuum(Player p) {
        double radius = plugin.getConfig().getDouble(
                "abilities.definitions.utility_belt.hopperRadius", 6.0);
        for (Entity entity : p.getNearbyEntities(radius, radius, radius)) {
            if (!(entity instanceof Item item) || item.isDead()) continue;
            var leftover = p.getInventory().addItem(item.getItemStack());
            if (leftover.isEmpty()) item.remove();
            else item.setItemStack(leftover.values().iterator().next());
        }
    }

    /**
     * Dust resolved as an instruction discharges the stack for mitigation.
     *
     * Only the stack it resolves: a fat Dust stack is a deliberate defensive
     * investment rather than a formula applied to everything carried.
     *
     * [OPEN] Exact mitigation. Implemented as brief Resistance scaled by the
     * stack, which is a stand-in for a number the design has not chosen.
     */
    private void discharge(Player p) {
        int ticks = plugin.getConfig().getInt(
                "abilities.definitions.utility_belt.dischargeTicks", 40);
        p.addPotionEffect(new org.bukkit.potion.PotionEffect(
                org.bukkit.potion.PotionEffectType.RESISTANCE, ticks, 1, false, false, true));
    }

    /** Match-scoped, like everything else the belt holds. */
    /**
     * Drop cooldowns for a match reset.
     *
     * Run state needs no clearing now that it lives on the run: a run in
     * flight when the match ends holds the only reference to its own state,
     * and both go away together.
     */
    public void reset() { ready.clear(); }
}
