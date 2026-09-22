package com.minecraftmoba.plugin;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerJoinEvent;

/**
 * A dedicated lobby world, built rather than generated.
 *
 * The lobby used to be the server's default world: a vanilla superflat with a
 * vanilla village in it. Nobody authored that, nobody chose it, and it was the
 * first thing every player saw. It also meant the lobby shared a world with the
 * terrain gallery, so "make the lobby safe" could only ever be a listener
 * cancelling damage -- setting the world peaceful would have decided mob
 * spawning, sleeping and despawn for the gallery too.
 *
 * Its own world removes that objection rather than working around it. Safety
 * here is structural: nothing hostile spawns, the weather never changes, the
 * sun never sets, and the room has no exit. {@link LobbySafety} still runs,
 * because it covers the cases this world cannot -- a spectator, or a player in
 * the instance world before the match starts.
 *
 * Everything about appearance is config. This class decides extent, enclosure,
 * lighting and where a player arrives, which are function; what the room is
 * made of is a design decision and is not made here.
 */
public final class LobbyWorld implements Listener {
    private final MobaPlugin plugin;
    private World world;

    public LobbyWorld(MobaPlugin plugin) { this.plugin = plugin; }

    public boolean enabled() { return plugin.getConfig().getBoolean("features.lobbyWorld.enabled"); }

    private String name() { return plugin.getConfig().getString("features.lobbyWorld.name", "moba_lobby"); }
    private int radius() { return plugin.getConfig().getInt("features.lobbyWorld.radius", 16); }
    private int floorY() { return plugin.getConfig().getInt("features.lobbyWorld.floorY", 64); }
    private int height() { return plugin.getConfig().getInt("features.lobbyWorld.height", 8); }

    public World world() { return world != null ? world : Bukkit.getWorld(name()); }

    /** Where a player stands when they arrive. */
    public Location spawn() {
        World w = world();
        if (w == null) return null;
        int[] at = LobbyHall.spawn(floorY());
        return new Location(w, at[0] + 0.5, at[1], at[2] + 0.5, 0f, 0f);
    }

    /**
     * Load or create the world, then make sure the room is standing in it.
     *
     * Idempotent on purpose: it runs on every enable, so a lobby damaged by an
     * admin, a stray explosion or a half-finished build repairs itself on the
     * next restart rather than needing a command nobody remembers.
     */
    public World ensure() {
        if (!enabled()) return null;
        World w = Bukkit.getWorld(name());
        if (w == null) {
            w = new WorldCreator(name())
                    .generator(new VoidGenerator())
                    .environment(World.Environment.NORMAL)
                    // NORMAL, not FLAT: FLAT makes Paper parse generator
                    // settings we do not supply and log "No key layers in
                    // MapLike[{}]" at every start. The generator produces
                    // nothing either way, so the type only decides which
                    // complaint we get.
                    .type(WorldType.NORMAL)
                    .generateStructures(false)
                    .createWorld();
        }
        if (w == null) {
            plugin.getLogger().warning("could not create the lobby world '" + name() + "'");
            return null;
        }
        this.world = w;
        applyRules(w);
        build(w);
        w.setSpawnLocation(0, LobbyHall.spawn(floorY())[1], 0);
        return w;
    }

    /**
     * The safety that a separate world buys, stated as world state rather than
     * as a listener intercepting consequences.
     */
    private void applyRules(World w) {
        w.setDifficulty(Difficulty.PEACEFUL);
        w.setGameRule(GameRule.DO_MOB_SPAWNING, false);
        w.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
        w.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
        w.setGameRule(GameRule.DO_FIRE_TICK, false);
        w.setGameRule(GameRule.KEEP_INVENTORY, true);
        w.setGameRule(GameRule.FALL_DAMAGE, false);
        w.setGameRule(GameRule.DROWNING_DAMAGE, false);
        w.setGameRule(GameRule.FIRE_DAMAGE, false);
        w.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false);
        w.setTime(6000);          // midday, so the room reads the same every visit
        w.setStorm(false);
    }

    /** Place the greybox. Materials are config; extent and enclosure are not. */
    public int build(World w) {
        var cfg = plugin.getConfig();
        Material floor = material(cfg.getString("features.lobbyWorld.floorMaterial", "SMOOTH_STONE"));
        Material wall = material(cfg.getString("features.lobbyWorld.wallMaterial", "STONE_BRICKS"));
        Material ceiling = material(cfg.getString("features.lobbyWorld.ceilingMaterial", "STONE_BRICKS"));
        Material light = material(cfg.getString("features.lobbyWorld.lightMaterial", "SEA_LANTERN"));
        Material platform = material(cfg.getString("features.lobbyWorld.platformMaterial", "POLISHED_ANDESITE"));
        int spacing = cfg.getInt("features.lobbyWorld.lightSpacing", 6);

        int placed = 0;
        for (var piece : LobbyHall.hall(radius(), floorY(), height(), spacing)) {
            Material m = switch (piece.role()) {
                case FLOOR -> floor;
                case WALL -> wall;
                case CEILING -> ceiling;
                case LIGHT -> light;
                case PLATFORM -> platform;
            };
            var block = w.getBlockAt(piece.x(), piece.y(), piece.z());
            if (block.getType() != m) { block.setType(m, false); placed++; }
        }
        return placed;
    }

    private static Material material(String name) {
        Material m = Material.matchMaterial(name);
        if (m == null) throw new IllegalArgumentException("features.lobbyWorld: not a material: " + name);
        return m;
    }

    /** Put a player in the lobby, in a game mode that cannot damage it. */
    public void send(Player p) {
        Location at = spawn();
        if (at == null) return;
        p.teleport(at);
        if (plugin.getConfig().getBoolean("features.lobbyWorld.adventureMode", true))
            p.setGameMode(GameMode.ADVENTURE);
    }

    public boolean isLobby(World w) { return w != null && world() != null && w.equals(world()); }

    /**
     * Arrivals go to the lobby unless they are mid-match.
     *
     * A player reconnecting into a running match belongs where they left off,
     * not at the start line, so participation is checked rather than assumed.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void join(PlayerJoinEvent e) {
        if (!enabled() || !plugin.getConfig().getBoolean("features.lobbyWorld.sendOnJoin", true)) return;
        Match match = plugin.match();
        if (match != null && match.running() && match.participant(e.getPlayer().getUniqueId()) != null) return;
        Bukkit.getScheduler().runTask(plugin, () -> send(e.getPlayer()));
    }

    /**
     * The lobby is furniture. Adventure mode already prevents most of this;
     * these cover an operator in survival and anything adventure mode misses.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void place(BlockPlaceEvent e) {
        if (isLobby(e.getBlock().getWorld()) && !e.getPlayer().isOp()) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void breakBlock(BlockBreakEvent e) {
        if (isLobby(e.getBlock().getWorld()) && !e.getPlayer().isOp()) e.setCancelled(true);
    }

    public String report() {
        World w = world();
        return "LOBBY enabled=" + enabled() + " world=" + name()
                + (w == null ? " (not loaded)" : " loaded, spawn " + spawn().toVector())
                + " radius=" + radius() + " height=" + height();
    }
}
