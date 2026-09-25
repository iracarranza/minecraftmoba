package com.minecraftmoba.plugin;

import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;
import org.bukkit.boss.*;

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
    private final DraftHallWorld hall;
    private final Map<UUID, ArmorStand> stands = new HashMap<>();
    private final Set<UUID> ghosts = new HashSet<>();
    private final Map<UUID, BossBar> bars = new HashMap<>();
    private ClassDraft.Phase timerPhase;
    private long phaseEnds;
    private static final long PHASE_TICKS = 30 * 20L;

    public DraftHallView(MobaPlugin plugin) {
        this.plugin = plugin; this.hall = new DraftHallWorld(plugin);
        Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 1L, 1L);
    }

    private void tick() {
        Match match = plugin.match();
        if (match == null || !match.selectingClasses() || match.draft() == null) return;
        ClassDraft d = match.draft();
        if (timerPhase != d.phase()) { timerPhase = d.phase(); phaseEnds = Bukkit.getCurrentTick() + PHASE_TICKS; }
        if (Bukkit.getCurrentTick() >= phaseEnds) {
            d.timeout();
            timerPhase = null;
        }
        refresh(match);
    }

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
            String turn = draft.onTurn().contains(entry.getKey()) ? "YOUR TURN" : "WAITING";
            String ban = draft.banned().stream().findFirst().orElse("NONE");
            String hover = draft.hovering().getOrDefault(entry.getKey(), "NONE");
            String picked = draft.picks().getOrDefault(entry.getKey(), "NONE");
            player.sendActionBar("CLASS " + draft.phase() + " | " + turn
                    + " | Ban: " + ban + "  Hover: " + hover + "  Picked: " + picked);
            BossBar bar = bars.computeIfAbsent(entry.getKey(), k -> Bukkit.createBossBar("", BarColor.BLUE, BarStyle.SOLID));
            long left = Math.max(0, phaseEnds - Bukkit.getCurrentTick());
            String active = draft.onTurn().stream().map(id -> {
                Player p = Bukkit.getPlayer(id); return p == null ? "?" : p.getName();
            }).reduce((a,b) -> a + ", " + b).orElse("none");
            bar.setTitle("CLASS " + draft.phase() + " — " + active + " picking — " + ((left + 19) / 20) + "s");
            bar.setProgress(Math.max(0.0, Math.min(1.0, left / (double) PHASE_TICKS)));
            if (!bar.getPlayers().contains(player)) bar.addPlayer(player);
            bar.setVisible(true);
        }
    }

    /** Bring participants to the shared lobby hall used for the draft. */
    public void enter(Match match) {
        hall.ensure();
        Map<Team, Integer> next = new EnumMap<>(Team.class);
        next.put(Team.NORTH, 0); next.put(Team.SOUTH, 0);
        for (UUID id : match.participantsByTeam().keySet()) {
            Player player = Bukkit.getPlayer(id);
            if (player != null) {
                Team team = match.participant(id).team;
                player.teleport(hall.spawn(team));
                int index = Math.min(next.get(team), DraftHall.PER_TEAM - 1);
                next.put(team, index + 1);
                bindStand(id, hall.stand(hall.nearest(team, index)));
            }
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
        for (BossBar bar : bars.values()) { bar.removeAll(); bar.setVisible(false); }
        bars.clear(); timerPhase = null;
    }

    /** Register the stand that belongs to a player. */
    public void bindStand(UUID player, ArmorStand stand) { stands.put(player, stand); }

    public boolean isGhosted(UUID player) { return ghosts.contains(player); }
}
