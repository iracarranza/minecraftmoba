package com.minecraftmoba.plugin;

import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

/**
 * The draft hall in the world: stands that wear what players are considering,
 * and the ghost state that says whose turn it is.
 *
 * {@link DraftHall} decides the arrangement as arithmetic. This is the half
 * that needs a server, kept separate for the same reason `LobbyHall` is: the
 * properties worth asserting -- mirroring, no overlap, an unbroken pit -- are
 * testable without one, and none of what is here is.
 *
 * THE GHOST STATE IS LEGIBILITY, NOT ENFORCEMENT. The 25 September amendment
 * moved enforcement onto the verb, because with a stand of your own and a chat
 * command, walking is no longer the selection act. So invisibility and flight
 * are applied to say WHOSE TURN IT IS -- "the most visible property in the
 * room" -- while `ClassDraft` refuses an off-turn `ban` or `pick` regardless
 * of what a player can see or where they are standing.
 *
 * NOT SPECTATOR MODE, deliberately. Ghosts keep a body and stay in the hall.
 * Spectator would let them leave, see through terrain and vanish entirely,
 * which costs the three properties the amendment keeps: they cannot obstruct,
 * they still broadcast intent by hovering, and solidifying is a moment.
 */
public final class DraftHallView {

    private final MobaPlugin plugin;
    private final Map<UUID, ArmorStand> stands = new HashMap<>();
    private final Set<UUID> ghosts = new HashSet<>();

    public DraftHallView(MobaPlugin plugin) { this.plugin = plugin; }

    /**
     * Redraw everything the draft state implies.
     *
     * Called after every accepted verb rather than on a tick, so the room
     * changes at the moment a player acts and never between.
     */
    public void refresh(Match match) {
        if (match == null || !match.selectingClasses() || match.draft() == null) return;
        ClassDraft draft = match.draft();
        Map<UUID, String> showing = draft.stands();

        for (var entry : match.participantsByTeam().entrySet()) {
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player == null) continue;
            dress(player, showing.get(entry.getKey()));
            applyTurnState(player, draft.isGhost(entry.getKey()));
        }
    }

    /** What a player's own stand is wearing: their pick, or their hover. */
    private void dress(Player player, String classId) {
        ArmorStand stand = stands.get(player.getUniqueId());
        if (stand == null || stand.isDead()) return;
        stand.setCustomName(classId == null
                ? player.getName()
                : player.getName() + " — " + classId);
        stand.setCustomNameVisible(true);
    }

    /**
     * Solid or ghost.
     *
     * Invisibility ALONE would remove the ghost's location, and "faintly
     * visible, located somewhere specific" is the property the amendment
     * keeps. So the body stays -- armour cleared, since invisibility still
     * renders worn armour and floating armour in a hall of armour stands
     * would read as a station rather than a player.
     */
    private void applyTurnState(Player player, boolean ghost) {
        boolean was = ghosts.contains(player.getUniqueId());
        if (ghost == was && player.isFlying() == ghost) return;

        if (ghost) {
            ghosts.add(player.getUniqueId());
            player.getInventory().setArmorContents(null);
            player.addPotionEffect(new PotionEffect(
                    PotionEffectType.INVISIBILITY, PotionEffect.INFINITE_DURATION, 0,
                    false, false, false));
            player.setAllowFlight(true);
            player.setFlying(true);
            // Collision off so ghosts pass THROUGH players. Terrain collision
            // stays on, which is what bounds flight: the hall is enclosed, so
            // no push-back or return teleport is needed. See the amendment.
            player.setCollidable(false);
        } else {
            ghosts.remove(player.getUniqueId());
            player.removePotionEffect(PotionEffectType.INVISIBILITY);
            player.setFlying(false);
            player.setAllowFlight(player.getGameMode() == GameMode.CREATIVE);
            player.setCollidable(true);
        }
    }

    /** Release every player from the ghost state when the draft ends. */
    public void release() {
        for (UUID id : new ArrayList<>(ghosts)) {
            Player player = Bukkit.getPlayer(id);
            if (player != null) applyTurnState(player, false);
        }
        ghosts.clear();
    }

    /** Register the stand that belongs to a player. */
    public void bindStand(UUID player, ArmorStand stand) { stands.put(player, stand); }

    public boolean isGhosted(UUID player) { return ghosts.contains(player); }
}
