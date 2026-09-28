package com.minecraftmoba.plugin;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.util.Vector;

import java.util.*;

/**
 * Spending a progression choice by finding or making somewhere to do it.
 *
 * Supersedes the chest menu in {@link Rewards} as the in-match interaction.
 * See docs/design/IN_WORLD_SELECTION_AND_CHANNEL_CONDITIONS.md.
 *
 * <h2>The principle</h2>
 *
 * Levelling requires a <b>space</b> -- not a menu openable anywhere, mid-stride,
 * mid-fight. This is the point of the mechanic rather than a cost bolted on:
 * Mole stops tunnelling and digs a chamber and is thereby the best class in the
 * game at manufacturing a level-up space, without anything granting it that; a
 * player caught in the open must find terrain, fortify or retreat. Levelling
 * gets a place, which is how this project treats world state generally.
 *
 * <h2>The input is a posture, not a keypress</h2>
 *
 * Raw key input through a predicate is a proven dead end in this project, and
 * Swap Offhand is already spent on ability activation. So the player enters a
 * state and the state is the input -- the draft hall already does the same
 * thing with ghosting.
 *
 * <h2>Structure</h2>
 *
 * The arithmetic lives in {@link SelectionBeat} and {@link SelectionLayout} and
 * is tested without a server. This class supplies the real conditions, drives
 * the bossbar, and builds the entities.
 *
 * [OPEN] Entity composition. Text displays for labels with interaction entities
 * for hitboxes is the obvious shape named in the design, and is what this does.
 * [OPEN] Whether opponents see the labels, a glow, particles, or nothing but
 * the crouch -- currently the displays are world entities and so are visible to
 * everyone, which matches the project's standing preference for legibility.
 * [OPEN] Whether a confirming input sits on top of the posture; there is none.
 */
public final class InWorldSelection implements Listener {

    /** One summoned option: the label, the hitbox, and what picking it means. */
    private record Slot(TextDisplay label, Interaction hitbox, int level, String optionId) {}

    private final MobaPlugin plugin;
    private final RewardCatalog catalog;
    private final SelectionBeat beat;
    private final Map<UUID, List<Slot>> summoned = new HashMap<>();
    private final Map<UUID, Location> lastBlock = new HashMap<>();
    private final Map<UUID, BossBar> bars = new HashMap<>();

    public InWorldSelection(MobaPlugin plugin, RewardCatalog catalog) {
        this.plugin = plugin;
        this.catalog = catalog;
        this.beat = new SelectionBeat(
                plugin.getConfig().getLong("selection.beatTicks"),
                plugin.getConfig().getLong("selection.stillnessTicks"));
    }

    public boolean enabled() { return plugin.getConfig().getBoolean("selection.enabled"); }
    public SelectionBeat beat() { return beat; }
    public boolean hasSummoned(Player p) { return summoned.containsKey(p.getUniqueId()); }

    // ---- the tick ---------------------------------------------------------

    /** Drive every enrolled player's beat. Called from the plugin's scheduler. */
    public void tick() {
        if (!enabled()) return;
        long now = plugin.getServer().getCurrentTick();
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            if (!plugin.enrolled(p)) { cleanup(p); continue; }
            trackMovement(p, now);
            if (catalog.pending(plugin.data(p)).isEmpty() || hasSummoned(p)) {
                if (!hasSummoned(p)) hideBar(p);
                continue;
            }
            showBar(p, now);
            if (beat.due(p.getUniqueId(), now)) {
                beat.restart(p.getUniqueId(), now);
                evaluate(p, now);
            }
        }
    }

    /** Stillness is measured in whole blocks, the same reading Recall uses for "no movement". */
    private void trackMovement(Player p, long now) {
        Location seen = lastBlock.get(p.getUniqueId());
        Location here = p.getLocation();
        if (seen == null || !seen.getWorld().equals(here.getWorld())
                || seen.getBlockX() != here.getBlockX()
                || seen.getBlockY() != here.getBlockY()
                || seen.getBlockZ() != here.getBlockZ()) {
            beat.moved(p.getUniqueId(), now);
            lastBlock.put(p.getUniqueId(), here.clone());
        }
    }

    private SelectionBeat.Conditions conditions(Player p, long now) {
        CombatState combat = plugin.combatState();
        return new SelectionBeat.Conditions(
                p.isSneaking(),
                ((org.bukkit.entity.LivingEntity) p).isOnGround(),
                beat.stationary(p.getUniqueId(), now),
                combat != null && combat.inCombat(p),
                aimPoint(p) != null);
    }

    private void evaluate(Player p, long now) {
        var c = conditions(p, now);
        if (c.satisfied()) { summon(p); return; }
        // Combat is already reported by the bossbar swap, so saying it again
        // here would be the same fact twice. The other three are things the
        // player chose and can correct.
        String why = c.unmetReason();
        if (why != null && !c.inCombat())
            p.sendActionBar(Component.text(why, NamedTextColor.GRAY));
    }

    // ---- the bossbar ------------------------------------------------------

    /**
     * Whichever clock is actually running.
     *
     * In combat the bar shows the combat countdown, and returns to the level-up
     * countdown when combat ends. The two are sequential and never concurrent,
     * so no paused progress has to be held anywhere: combat reaching zero IS
     * the signal to start a fresh level-up countdown. A player who takes a hit
     * sees the bar flip, watches combat run out, and watches the level-up clock
     * start -- one continuous explanation of why they cannot level up yet.
     *
     * The two durations stay independently tuned. That combat is 7 seconds and
     * the beat is 5 is a coincidence of first values, not a relationship, and
     * sharing a bar must not become a reason to collapse them into one number.
     */
    private void showBar(Player p, long now) {
        int count = catalog.pending(plugin.data(p)).size();
        CombatState combat = plugin.combatState();
        boolean inCombat = combat != null && combat.inCombat(p);

        BossBar bar = bars.computeIfAbsent(p.getUniqueId(), id -> {
            BossBar b = BossBar.bossBar(Component.empty(), 1, BossBar.Color.YELLOW, BossBar.Overlay.PROGRESS);
            p.showBossBar(b);
            return b;
        });

        if (inCombat) {
            long left = combat.remaining(p);
            bar.color(BossBar.Color.RED);
            bar.progress((float) Math.min(1, Math.max(0, (double) left / combat.durationTicks())));
            // No seconds. The bar's own length is the countdown, and a number
            // beside it is a second reading of the same fact that has to be
            // read rather than glanced at.
            bar.name(Component.text("In combat"));
            // Restarting here is what makes the two clocks sequential: the
            // level-up countdown begins from the moment combat ends, not from
            // whenever it happened to be when the fight started.
            beat.restart(p.getUniqueId(), now);
            return;
        }
        bar.color(BossBar.Color.YELLOW);
        bar.progress((float) Math.min(1, Math.max(0, beat.progress(p.getUniqueId(), now))));
        bar.name(Component.text(count + (count == 1 ? " unspent level point" : " unspent level points")
                + " available — crouch to summon, M2 to select"));
    }

    private void hideBar(Player p) {
        BossBar bar = bars.remove(p.getUniqueId());
        if (bar != null) p.hideBossBar(bar);
    }

    // ---- eligible space ---------------------------------------------------

    /**
     * The point the player is looking at, if there is room there for the options.
     *
     * Unobstructed space at the aim point, <b>not</b> a flat surface to stand
     * them against. Requiring a flat face would make the mechanic fail in
     * exactly the places players most often are -- a tunnel, a cave, a slope --
     * while under the space rule a wall, open air and underwater all work. The
     * player must be looking somewhere sensible; they need not be looking at
     * masonry.
     */
    private Location aimPoint(Player p) {
        double distance = plugin.getConfig().getDouble("selection.aimDistance");
        int slots = plugin.getConfig().getInt("selection.slots");
        double spacing = plugin.getConfig().getDouble("selection.spacing");

        Vector direction = p.getEyeLocation().getDirection().normalize();
        var hit = p.getWorld().rayTraceBlocks(p.getEyeLocation(), direction, distance);
        // Against a wall, back off to the last clear block rather than placing
        // options inside it. In open air, use the full reach.
        Location centre = hit != null && hit.getHitPosition() != null
                ? hit.getHitPosition().toLocation(p.getWorld()).subtract(direction.clone().multiply(0.5))
                : p.getEyeLocation().clone().add(direction.clone().multiply(distance));

        int radius = SelectionLayout.clearRadiusBlocks(slots, spacing);
        Vector across = across(direction);
        for (int i = -radius; i <= radius; i++) {
            Location at = centre.clone().add(across.clone().multiply(i));
            if (!at.getBlock().isPassable()) return null;
        }
        return centre;
    }

    /** The horizontal axis across the player's facing, so the row spans left to right. */
    private static Vector across(Vector facing) {
        Vector flat = new Vector(facing.getX(), 0, facing.getZ());
        if (flat.lengthSquared() < 1.0e-6) return new Vector(1, 0, 0); // looking straight up or down
        return flat.normalize().crossProduct(new Vector(0, 1, 0)).normalize();
    }

    // ---- summoning --------------------------------------------------------

    private void summon(Player p) {
        Location centre = aimPoint(p);
        if (centre == null) return;
        var pending = catalog.pending(plugin.data(p));
        if (pending.isEmpty()) return;
        int level = pending.getFirst();
        var options = catalog.levels().get(level);
        if (options == null || options.isEmpty()) return;

        double spacing = plugin.getConfig().getDouble("selection.spacing");
        double[] offsets = SelectionLayout.offsets(options.size(), spacing);
        Vector across = across(p.getEyeLocation().getDirection());
        var slots = new ArrayList<Slot>();

        for (int i = 0; i < options.size(); i++) {
            var option = options.get(i);
            Location at = centre.clone().add(across.clone().multiply(offsets[i]));
            TextDisplay label = p.getWorld().spawn(at, TextDisplay.class, d -> {
                d.text(Component.text(option.label()));
                d.setBillboard(Display.Billboard.CENTER);
                d.setSeeThrough(true);
            });
            Interaction hitbox = p.getWorld().spawn(at, Interaction.class, h -> {
                h.setInteractionWidth((float) spacing);
                h.setInteractionHeight(1.0f);
            });
            slots.add(new Slot(label, hitbox, level, option.id()));
        }
        summoned.put(p.getUniqueId(), slots);
        p.sendActionBar(Component.text("Choose — level " + level, NamedTextColor.YELLOW));
    }

    /**
     * Picking an option.
     *
     * A summon spends ONE choice. A player holding four banked allocations
     * needs four separate safe moments -- this is the lever that decides
     * whether banking costs anything. Spending every pending choice from a
     * single summon would make hoarding strictly free and the whole condition
     * set decorative.
     */
    @EventHandler
    public void onInteract(PlayerInteractEntityEvent e) {
        Player p = e.getPlayer();
        List<Slot> slots = summoned.get(p.getUniqueId());
        if (slots == null) return;
        for (Slot slot : slots) {
            if (!slot.hitbox().equals(e.getRightClicked())) continue;
            e.setCancelled(true);
            // Despawn BEFORE applying, so the summon token is consumed and the
            // same summon cannot be spent twice by a second click arriving in
            // the same tick. Rewards.click guards its menu the same way.
            despawn(p);
            apply(p, slot);
            return;
        }
    }

    /**
     * Record the choice exactly as the menu does, so both writers share one route.
     *
     * A Task option records the TREE it allocates rather than its own id, so
     * TaskLedger can count tiers straight from the save; every other option
     * records its id. applyAndSave then projects the effects, which is where a
     * Task allocation becomes visible without relogging.
     *
     * Deliberately does NOT re-summon when more choices remain. The menu
     * reopens itself; this must not, because one summon spends one choice and
     * that is the lever making a banked allocation cost something.
     */
    private void apply(Player p, Slot slot) {
        if (!plugin.enrolled(p)) return;
        var data = plugin.data(p);
        var pending = catalog.pending(data);
        if (pending.isEmpty() || pending.getFirst() != slot.level()) return;
        var options = catalog.levels().get(slot.level());
        if (options == null) return;
        options.stream().filter(o -> o.id().equals(slot.optionId())).findFirst().ifPresent(option -> {
            data.choices.add(new PlayerData.ChoiceRecord(slot.level(),
                    option.task() != null ? option.task() : option.id()));
            plugin.applyAndSave(p);
            p.sendActionBar(Component.text("Chose " + option.label(), NamedTextColor.GREEN));
        });
    }

    // ---- lifecycle --------------------------------------------------------

    private void despawn(Player p) {
        List<Slot> slots = summoned.remove(p.getUniqueId());
        if (slots == null) return;
        for (Slot slot : slots) { slot.label().remove(); slot.hitbox().remove(); }
    }

    public void cleanup(Player p) {
        despawn(p);
        hideBar(p);
        beat.clear(p.getUniqueId());
        lastBlock.remove(p.getUniqueId());
    }

    @EventHandler public void onQuit(PlayerQuitEvent e) { cleanup(e.getPlayer()); }

    public String report(Player p) {
        long now = plugin.getServer().getCurrentTick();
        var c = conditions(p, now);
        return "SELECTION enabled=" + enabled()
                + " pending=" + (plugin.enrolled(p) ? catalog.pending(plugin.data(p)).size() : 0)
                + " beatTicks=" + beat.beatTicks()
                + " remaining=" + beat.remaining(p.getUniqueId(), now)
                + " summoned=" + hasSummoned(p)
                + " conditions=" + c
                + " unmet=" + c.unmetReason();
    }
}
