package com.minecraftmoba.plugin;

import org.bukkit.*;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.*;

/**
 * A team-coloured glow around each defensive objective and Aether Fountain.
 *
 * Raised from the first live playtest: standing beside an authored Pillager
 * Outpost, it read as scenery rather than as the thing this team defends.
 * Nothing said whose it was, that it was contestable, or that it belonged to
 * the same system as the Fountain the player respawns at. The shared signature
 * is the point -- finding an objective should tell a player it is connected to
 * their Fountain, without a HUD marker saying so.
 *
 * WHY A VISIBLE BOX AND NOT A PURE OUTLINE. The glow is traced from what the
 * model renders. A display carrying `barrier`, which renders nothing, produces
 * no outline at all -- tested. So there is no invisible-but-outlined form, and
 * "retexture it away" would defeat the effect it is meant to preserve. Clear
 * glass is the subtlest body that still traces: a dark frame with a clear
 * interior. A purpose-built wireframe item model, twelve thin edge cuboids on
 * an `item_display`, is the intended successor -- it is authored FOR outlining,
 * and unlike retexturing a block it changes nothing players can place.
 *
 * WHY THE COLOUR LIVES ON EACH PLAYER'S OWN SCOREBOARD. Glow colour comes from
 * the entity's scoreboard team, resolved on the board the VIEWING client holds.
 * {@link Hud} gives every player a private scoreboard, so a team registered on
 * the main board is invisible to them and every glowing entity renders default
 * white. That is not a Minecraft limitation; it was ours, and it cost an hour
 * of prototyping to find. Registering per viewer is also what would later allow
 * showing an enemy a different colour than an ally sees.
 *
 * Deliberately absent, because canon does not settle them:
 *  - the radius. DEFERRED by explicit decision: it is the vision/information
 *    question, not a presentation setting. Until it is taken up, the entity
 *    tracking range is the de facto limit and no radius is asserted here.
 *  - any reaction to Defensive Capacity. A signature that dimmed as capacity
 *    fell would make a siege legible at a glance, and whether that is wanted is
 *    open.
 *  - the Lair. It is neutral and conspicuous by design, so it may want a
 *    contrasting signature rather than the team one. It gets none here.
 */
public final class ObjectiveGlow {

    /** Half-extents and height of each authored form, from its template. */
    private record Extent(int halfWidth, int height) {}

    private static final EnumMap<TeamObjectives.Kind, Extent> EXTENTS =
            new EnumMap<>(Map.of(
                    TeamObjectives.Kind.PILLAGER_OUTPOST, new Extent(7, 21),   // watchtower.nbt
                    TeamObjectives.Kind.NETHER_BASTION, new Extent(16, 33),    // bridge bastion
                    TeamObjectives.Kind.END_SPIKE, new Extent(4, 89)));        // radius 3, height 88
    private static final Extent FOUNTAIN = new Extent(6, 6);

    /**
     * How far past the structure the box reaches, in blocks.
     *
     * The box marks a PLACE, not a building: it takes in the ground around the
     * objective so the glow says "this area is contested" rather than outlining
     * masonry. NON-CANON FIXTURE -- an appearance value, not a design decision.
     */
    private static final int MARGIN = 5;

    private static final String TAG = "moba_objective_glow";

    private final MobaPlugin plugin;
    private final List<UUID> spawned = new ArrayList<>();

    public ObjectiveGlow(MobaPlugin plugin) { this.plugin = plugin; }

    public boolean enabled() {
        return plugin.getConfig().getBoolean("features.objectiveGlow.enabled", false);
    }

    private String teamName(Team team) { return "moba_glow_" + team.lower(); }

    private ChatColor colour(Team team) {
        String key = "features.objectiveGlow.colour." + team.lower();
        String configured = plugin.getConfig().getString(key, team == Team.NORTH ? "AQUA" : "RED");
        try {
            return ChatColor.valueOf(configured.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException bad) {
            plugin.getLogger().warning(key + "='" + configured + "' is not a ChatColor; "
                    + "glow falls back to WHITE, which is what an uncoloured team looks like");
            return ChatColor.WHITE;
        }
    }

    /** Rebuild every box for the bound map. Safe to call repeatedly. */
    public int rebuild(World world) {
        clear();
        if (!enabled() || world == null) return 0;
        var objectives = plugin.teamObjectives();
        var match = plugin.match();
        for (Team team : Team.values()) {
            if (objectives != null)
                for (TeamObjectives.Kind kind : TeamObjectives.Kind.values()) {
                    Location at = objectives.site(team, kind);
                    if (at != null) box(world, at, EXTENTS.get(kind), team);
                }
            Location fountain = match == null ? null : match.homeland(team);
            if (fountain != null) box(world, fountain, FOUNTAIN, team);
        }
        for (Player p : Bukkit.getOnlinePlayers()) apply(p);
        return spawned.size();
    }

    /** One glowing box, centred on the structure's footprint. */
    private void box(World world, Location centre, Extent extent, Team team) {
        if (extent == null) return;
        int half = extent.halfWidth() + MARGIN;
        int side = half * 2 + 1;
        int tall = extent.height() + MARGIN;
        Location at = new Location(world, centre.getBlockX(), centre.getBlockY(), centre.getBlockZ());
        BlockDisplay display = world.spawn(at, BlockDisplay.class, d -> {
            d.setBlock(Material.GLASS.createBlockData());
            d.setGlowing(true);
            d.setPersistent(false);        // never saved: it is a view, not world state
            d.setBrightness(new Display.Brightness(15, 15));
            d.addScoreboardTag(TAG);
            d.setTransformation(new Transformation(
                    new Vector3f(-side / 2f, 0f, -side / 2f),
                    new AxisAngle4f(0f, 0f, 0f, 1f),
                    new Vector3f(side, tall, side),
                    new AxisAngle4f(0f, 0f, 0f, 1f)));
        });
        spawned.add(display.getUniqueId());
    }

    /**
     * Put the glow teams on this player's own scoreboard.
     *
     * The board {@link Hud} hands the player is the one their client resolves
     * glow colour against, so this must run against `p.getScoreboard()` and not
     * against the main board. Called on join and after every rebuild.
     */
    public void apply(Player p) {
        if (!enabled()) return;
        Scoreboard board = p.getScoreboard();
        if (board == null) return;
        for (Team team : Team.values()) {
            var scoreboardTeam = board.getTeam(teamName(team));
            if (scoreboardTeam == null) scoreboardTeam = board.registerNewTeam(teamName(team));
            scoreboardTeam.setColor(colour(team));
            for (UUID id : spawned) {
                Entity e = Bukkit.getEntity(id);
                if (e == null) continue;
                if (!e.getScoreboardTags().contains(TAG)) continue;
                scoreboardTeam.addEntry(id.toString());
            }
        }
    }

    /** Remove every box. Also sweeps by tag, for displays a reload orphaned. */
    public void clear() {
        for (UUID id : spawned) {
            Entity e = Bukkit.getEntity(id);
            if (e != null) e.remove();
        }
        spawned.clear();
        for (World w : Bukkit.getWorlds())
            for (Entity e : w.getEntitiesByClass(BlockDisplay.class))
                if (e.getScoreboardTags().contains(TAG)) e.remove();
    }

    public String report() {
        return "OBJECTIVE_GLOW enabled=" + enabled() + " boxes=" + spawned.size()
                + " colours={north=" + colour(Team.NORTH) + ", south=" + colour(Team.SOUTH) + "}";
    }
}
