package com.minecraftmoba.plugin;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;

/**
 * You cannot be hurt while you are not in a match.
 *
 * Waiting to join is not gameplay, and dying to a creeper on the way to the
 * lobby is not a difficulty curve -- it is an interruption of the one thing the
 * player is trying to do. So a player who is not a living participant of a
 * running match takes no damage, loses no hunger, and is not targeted.
 *
 * The rule is stated in terms of match participation rather than of a hub
 * region, so it needs no coordinates and cannot drift out of alignment with a
 * lobby that moves. It also covers the gallery, spectators, and anyone standing
 * in the instance world before the match has started.
 *
 * Deliberately NOT done by setting the lobby world peaceful: that would also
 * decide mob spawning, sleeping and hostile despawn for a world the terrain
 * gallery shares, which is a much larger claim than "the player is safe".
 */
public final class LobbySafety implements Listener {
    private final MobaPlugin plugin;

    public LobbySafety(MobaPlugin plugin) { this.plugin = plugin; }

    public boolean enabled() { return plugin.getConfig().getBoolean("features.lobbySafety.enabled"); }

    /** True while this player's actions and risks belong to a running match. */
    boolean inMatch(Player p) {
        Match match = plugin.match();
        if (match == null || !match.running()) return false;
        Match.Participant part = match.participant(p.getUniqueId());
        return part != null && part.alive;
    }

    private boolean protect(Player p) { return enabled() && !inMatch(p); }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void damage(EntityDamageEvent e) {
        if (e.getEntity() instanceof Player p && protect(p)) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void target(EntityTargetLivingEntityEvent e) {
        // Cancelling damage alone leaves a creeper hissing at a player who
        // cannot be hurt, which reads as a bug rather than as safety.
        if (e.getTarget() instanceof Player p && protect(p)) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void hunger(FoodLevelChangeEvent e) {
        if (e.getEntity() instanceof Player p && protect(p)
                && e.getFoodLevel() < p.getFoodLevel()) e.setCancelled(true);
    }

    public String report(Player p) {
        return "LOBBY_SAFETY enabled=" + enabled() + " inMatch=" + inMatch(p)
                + " protected=" + protect(p);
    }
}
