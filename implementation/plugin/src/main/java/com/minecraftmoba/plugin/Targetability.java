package com.minecraftmoba.plugin;

import org.bukkit.entity.*;

/**
 * What an ability may act on, and in which of two ways.
 *
 * <h2>Why this is not one predicate</h2>
 *
 * "Can this ability target that?" looks like a single question and is not.
 * Toolbox's own dictionary settles it: Piston applies a directional impulse
 * to eligible entities <b>including dropped items</b>, while Observer,
 * Redstone Torch and Sticky Piston apply Observed, Illuminated and Root. A
 * dropped item can be pushed and cannot be Rooted; an Armour Stand can be
 * pushed and has nothing to acquire.
 *
 * So targetability has two axes, and collapsing them is what forces every
 * ability to re-derive the difference at its own call site:
 *
 * <ul>
 *   <li>{@link #impulse} -- physics. Anything with a position and a velocity.</li>
 *   <li>{@link #status} -- Observed, Illuminated, Root, Stun, and "first enemy
 *       struck". Anything that can meaningfully be said to be affected.</li>
 * </ul>
 *
 * Status implies impulse; the converse is false. That asymmetry is the whole
 * content of this class.
 *
 * <h2>Mobs are targets, and livestock are not</h2>
 *
 * Hostile and neutral mobs are full targets: damaging one procs the Utility
 * Belt, and they can be Observed, Illuminated, Rooted and Stunned. A Toolbox
 * fighting a Ravager is doing the thing the class does, and a kit that went
 * inert outside player combat would make the whole PvE half of a match
 * classless.
 *
 * <b>Passive livestock are excluded</b>, and the reason is economic rather
 * than thematic. Animals are this project's renewable resource -- bound per
 * map, manifested at derived coordinates, farmed deliberately. Letting an
 * Observer lock a cow means the best way to hold a circuit's target is to
 * stand in a pen, and it makes "first enemy struck" fire on a breeding
 * accident. They are the economy, not the opposition.
 *
 * <h2>Allies are never targets</h2>
 *
 * Settled: no friendly fire. Incidental damage -- TNT, suffocation, fall --
 * is a separate question and is NOT decided here; this governs what an
 * ability may deliberately select.
 */
public final class Targetability {

    private Targetability() {}

    /**
     * Anything a directional impulse can move.
     *
     * Deliberately permissive. Pushing dropped items is a stated Piston
     * behaviour and a real tactic -- denying a kill's drops, or moving one's
     * own ammunition -- and there is no case where refusing to push a boat
     * makes the game better.
     *
     * Still excludes allies, so a Piston cannot be used to fling a teammate
     * off a cliff, and excludes the caster, because a component that shoved
     * its own operator every time it fired would make every circuit unusable.
     */
    public static boolean impulse(Entity target, Player source) {
        if (target == null || source == null) return false;
        if (target.equals(source) || target.isDead()) return false;
        return !allied(target, source);
    }

    /**
     * Anything a status may be applied to, or that counts as "struck".
     *
     * Living things only: a status describes a condition of something that can
     * be in one. An Observer locking a dropped item, or Root on an Armour
     * Stand, is not a restriction worth writing rules around -- it is a
     * category error, and permitting it would make "the Observed target" mean
     * nothing.
     */
    public static boolean status(Entity target, Player source) {
        if (!impulse(target, source)) return false;
        return target instanceof LivingEntity;
    }

    /**
     * What a living entity is -- descriptive only.
     *
     * Nothing here gates targetability. It exists so an ability that genuinely
     * wants to distinguish populations can say so AT ITS OWN CALL SITE, where
     * the exception is visible, rather than having one baked into the shared
     * predicate where no other class would ever see it.
     */
    public enum Category {
        /** An enemy player. Always a full target. */
        ENEMY,
        /** Hostile or neutral. A full target: procs, acquires, roots. */
        MOB,
        /** Passive breedable livestock. Still a full target; see the class note. */
        LIVESTOCK
    }

    /**
     * Classify by behaviour rather than by an entity-type list.
     *
     * A list would need editing every time Minecraft adds a mob, and would be
     * wrong in the interval. {@link Animals} is the interface vanilla already
     * uses for breedable passive creatures, which is exactly the population
     * this excludes -- so the rule tracks the game rather than a table.
     *
     * Note that {@link Animals} covers wolves and cats, which are breedable
     * and tameable but will fight. They are livestock here: a tamed wolf is
     * somebody's property and the same farming argument applies.
     */
    public static Category category(LivingEntity entity) {
        if (entity instanceof Player) return Category.ENEMY;
        if (entity instanceof Animals) return Category.LIVESTOCK;
        return Category.MOB;
    }

    /**
     * Same-team players. Everything non-player is nobody's ally.
     *
     * Reads the scoreboard team, which is where team membership already lives
     * and what the existing sweep abilities use, rather than introducing a
     * second source that could disagree with it.
     */
    public static boolean allied(Entity target, Player source) {
        if (!(target instanceof Player other)) return false;
        var manager = org.bukkit.Bukkit.getScoreboardManager();
        boolean sameScoreboardTeam = false;
        if (manager != null) {
            var board = manager.getMainScoreboard();
            var team = board.getEntryTeam(source.getName());
            sameScoreboardTeam = team != null && team.equals(board.getEntryTeam(other.getName()));
        }
        var teams = matchTeams;
        return decide(teams == null ? null : teams.apply(source), teams == null ? null : teams.apply(other), sameScoreboardTeam);
    }

    /**
     * Where a player's match team comes from.
     *
     * Set once by the plugin to read the running match's participants. It exists because nothing
     * in the plugin ever put match players on a MAIN-scoreboard team, which is the only place
     * {@link #allied} used to look, so in a running match nobody was anybody's ally: no-friendly-fire
     * refused nothing and the sweep abilities struck teammates. The scenario bench's friendly-fire
     * scenario found it.
     */
    private static volatile java.util.function.Function<Player, Team> matchTeams;

    public static void matchTeams(java.util.function.Function<Player, Team> resolver) { matchTeams = resolver; }

    /**
     * Who counts as an ally. The match's own teams are authoritative when BOTH players are in it;
     * otherwise the scoreboard decides, which keeps the original rule for anything outside a match
     * (the datapack tests, a hand-built team).
     */
    static boolean decide(Team a, Team b, boolean sameScoreboardTeam) {
        if (a != null && b != null) return a == b;
        return sameScoreboardTeam;
    }
}
