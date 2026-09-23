package com.minecraftmoba.plugin;

import io.papermc.paper.event.player.PlayerPickBlockEvent;
import io.papermc.paper.event.player.PlayerPickEntityEvent;
import io.papermc.paper.event.player.PlayerPickItemEvent;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import java.util.*;

/**
 * Contextual pings on pick-block, with world-space markers.
 *
 * Paper exposes PlayerPickBlockEvent and PlayerPickEntityEvent, both
 * cancellable, and both carrying what the player was actually looking at. That
 * makes pick-block a complete ping input on its own: no raycast, no hotbar
 * slot, and the target is resolved by the client's own crosshair so it matches
 * exactly what the player saw.
 *
 * Cancelling suppresses the vanilla slot swap, so the gesture costs no hotbar
 * state — which matters in a game where hotbar space is contested.
 *
 * Vocabulary, kept deliberately small. Research across MOBAs found constrained
 * wheels of six to ten entries far more learnable than large chord systems:
 *
 *   pick a block    HERE     a location
 *   pick an entity  TARGET   that enemy or creature
 *   ctrl + either   ASSIST   requesting help there
 *
 * **Sneak + pick is not a ping.** It toggles Infrastructure Mode, always, on
 * any target. Sharing the gesture would have meant a hotkey whose effect
 * depends on what happens to be under the crosshair, which is the wrong
 * trade for a mode toggle.
 *
 * Targeted pings therefore cover only what a crosshair can name. The calls
 * with no world target — DANGER, RETREAT, GROUP, HOLD — are **targetless
 * pings**, anchored on the sender's own position and raised from a menu
 * rather than a crosshair. That split is the reason the vocabulary stays
 * small on the gesture side without losing the calls a team actually needs.
 *
 * Ctrl arrives as isIncludeData(), vanilla's copy-with-data modifier, so the
 * second axis is free rather than a new binding.
 *
 * **Output is built for a first-person camera.** A minimap dot is useless to a
 * teammate looking elsewhere, so a ping places a visible world marker, plays a
 * directional sound, and sends an action-bar line naming the compass direction
 * and distance. That is the Deadlock model rather than the League one, and it
 * is the difference that matters when the camera is a ninety-degree cone.
 *
 * Disable with features.pings.enabled.
 */
public final class Pings implements Listener {
    public enum Kind {
        // Targeted: raised by pick-block, anchored on what the crosshair named.
        HERE(Particle.HAPPY_VILLAGER, Sound.BLOCK_NOTE_BLOCK_BELL, ChatColor.AQUA, "Here", false),
        TARGET(Particle.CRIT, Sound.BLOCK_NOTE_BLOCK_PLING, ChatColor.RED, "Target", false),
        ASSIST(Particle.END_ROD, Sound.BLOCK_NOTE_BLOCK_CHIME, ChatColor.LIGHT_PURPLE, "Assist", false),
        // Targetless: raised from a menu, anchored on the sender.
        DANGER(Particle.LARGE_SMOKE, Sound.BLOCK_NOTE_BLOCK_BASS, ChatColor.GOLD, "Danger", true),
        RETREAT(Particle.SMOKE, Sound.BLOCK_NOTE_BLOCK_DIDGERIDOO, ChatColor.YELLOW, "Retreat", true),
        GROUP(Particle.COMPOSTER, Sound.BLOCK_NOTE_BLOCK_HARP, ChatColor.GREEN, "Group", true),
        HOLD(Particle.ENCHANT, Sound.BLOCK_NOTE_BLOCK_FLUTE, ChatColor.BLUE, "Hold", true);

        final Particle particle; final Sound sound; final ChatColor colour; final String label;
        final boolean targetless;
        Kind(Particle p, Sound s, ChatColor c, String l, boolean t) {
            particle = p; sound = s; colour = c; label = l; targetless = t;
        }
        public boolean targetless() { return targetless; }
    }

    private final MobaPlugin plugin;
    private final Map<UUID, Long> lastPing = new HashMap<>();
    private long sent;

    public Pings(MobaPlugin plugin) { this.plugin = plugin; }

    public boolean enabled() { return plugin.getConfig().getBoolean("features.pings.enabled"); }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPickBlock(PlayerPickBlockEvent e) {
        if (toggledInfra(e.getPlayer())) { e.setCancelled(true); return; }
        if (handle(e.getPlayer(), e, e.getBlock().getLocation().add(0.5, 0.5, 0.5), false)) e.setCancelled(true);
    }

    /** Sneak + pick is the Infrastructure Mode toggle, on any target. */
    private boolean toggledInfra(Player p) {
        if (!p.isSneaking()) return false;
        if (plugin.infraMode() == null || !plugin.infraMode().enabled()) return false;
        plugin.infraMode().toggle(p);
        return true;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPickEntity(PlayerPickEntityEvent e) {
        if (toggledInfra(e.getPlayer())) { e.setCancelled(true); return; }
        Entity target = e.getEntity();
        if (handle(e.getPlayer(), e, target.getLocation().add(0, target.getHeight() / 2, 0), true))
            e.setCancelled(true);
    }

    /** Targetless ping, anchored on the sender. Raised from a menu or command. */
    public boolean raise(Player p, Kind kind) {
        if (!enabled() || !plugin.enrolled(p)) return false;
        if (!kind.targetless()) return false;
        long now = System.currentTimeMillis();
        if (now - lastPing.getOrDefault(p.getUniqueId(), 0L)
                < plugin.getConfig().getLong("features.pings.cooldownMillis", 1500L)) return false;
        lastPing.put(p.getUniqueId(), now);
        broadcast(p, kind, p.getLocation().add(0, 1, 0));
        sent++;
        return true;
    }

    private Kind kindFor(Player p, PlayerPickItemEvent e, boolean entity) {
        if (e.isIncludeData()) return Kind.ASSIST;       // ctrl, vanilla's copy-with-data
        return entity ? Kind.TARGET : Kind.HERE;
    }

    private boolean handle(Player p, PlayerPickItemEvent e, Location at, boolean entity) {
        if (!enabled() || !plugin.enrolled(p)) return false;
        long now = System.currentTimeMillis();
        long cooldown = plugin.getConfig().getLong("features.pings.cooldownMillis", 1500L);
        // Rate limiting is part of the design, not an optimisation: an
        // unlimited ping key becomes spam in every game that has shipped one.
        if (now - lastPing.getOrDefault(p.getUniqueId(), 0L) < cooldown) return true;
        lastPing.put(p.getUniqueId(), now);

        Kind kind = kindFor(p, e, entity);
        broadcast(p, kind, at);
        sent++;
        return true;
    }

    private void broadcast(Player from, Kind kind, Location at) {
        double radius = plugin.getConfig().getDouble("features.pings.audienceRadius", 512.0);
        int seconds = plugin.getConfig().getInt("features.pings.markerSeconds", 5);
        for (Player viewer : from.getWorld().getPlayers()) {
            if (viewer.getLocation().distance(at) > radius) continue;
            viewer.playSound(viewer.getLocation(), kind.sound, 1.0f, kind.ordinal() == 0 ? 1.4f : 0.9f);
            // Direction and distance, because a marker behind you is invisible.
            viewer.sendActionBar(kind.colour + "[" + kind.label + "] "
                    + from.getName() + " · " + bearing(viewer, at)
                    + " · " + (int) viewer.getLocation().distance(at) + "m");
        }
        marker(from, kind, at, seconds);
    }

    /** Compass bearing from the viewer to the ping, so it reads without a map. */
    private String bearing(Player viewer, Location at) {
        double dx = at.getX() - viewer.getLocation().getX();
        double dz = at.getZ() - viewer.getLocation().getZ();
        double angle = Math.toDegrees(Math.atan2(-dx, dz)) - viewer.getLocation().getYaw();
        angle = ((angle % 360) + 360) % 360;
        String[] arrows = {"ahead", "ahead-right", "right", "behind-right",
                           "behind", "behind-left", "left", "ahead-left"};
        return arrows[(int) Math.round(angle / 45.0) % 8];
    }

    private void marker(Player from, Kind kind, Location at, int seconds) {
        var world = at.getWorld();
        if (world == null) return;
        final int ticks = Math.max(1, seconds) * 20;
        new org.bukkit.scheduler.BukkitRunnable() {
            int elapsed = 0;
            @Override public void run() {
                if (elapsed >= ticks) { cancel(); return; }
                // A vertical column reads from far away and through terrain
                // gaps, which a single point at ground level does not.
                for (double dy = 0; dy < 3.0; dy += 0.5)
                    world.spawnParticle(kind.particle, at.getX(), at.getY() + dy, at.getZ(), 2, 0.1, 0.1, 0.1, 0);
                elapsed += 5;
            }
        }.runTaskTimer(plugin, 0L, 5L);
    }

    public String report() {
        return "PINGS enabled=" + enabled() + " sent=" + sent
                + " targeted=[HERE, TARGET, ASSIST] targetless="
                + Arrays.stream(Kind.values()).filter(Kind::targetless).map(k -> k.label).toList()
                + " (block=HERE, entity=TARGET, ctrl=ASSIST, sneak=INFRA MODE)";
    }
}
