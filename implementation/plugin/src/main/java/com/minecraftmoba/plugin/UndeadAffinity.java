package com.minecraftmoba.plugin;

import org.bukkit.entity.EntityType;

import java.util.Set;

/**
 * Skeleton Crew's passive, in the half that can exist yet.
 *
 * <h2>The passive has two halves and only one is buildable</h2>
 *
 * classes.md: <i>"Hostile undead detect and pursue Skeleton Crew from
 * substantially farther away than normal. Killing eligible hostile undead
 * generates Crew stacks."</i>
 *
 * The first sentence is a targeting range. The second mints <b>Crew</b> --
 * worker entities with a Combat/Logistics/self-preservation priority order,
 * cargo, Supply Line deposits and sunlight burning -- which is the class's
 * entire resource system and does not exist. Building a counter that nothing
 * spends would be a placeholder one layer in, so this is the pursuit half
 * alone, and the class's passive is real instead of absent.
 *
 * <h2>Pursuit is granted, not attribute-modified</h2>
 *
 * The obvious implementation raises {@code FOLLOW_RANGE} on nearby undead.
 * That leaves residue: a mob whose range was raised keeps it after the player
 * dies, switches class or walks away, and nothing takes it back. {@link
 * Passives} already carries that lesson in {@code withdrawSpeed}, where the
 * removal path is called out as the one that matters.
 *
 * So pursuit is granted per scan instead -- an untargeted mob in the extended
 * band is pointed at the player, and nothing persists on the mob. There is
 * nothing to withdraw, because nothing was lent.
 */
public final class UndeadAffinity {

    /**
     * Vanilla's own follow range for the common undead, in blocks.
     *
     * Inside it, vanilla already pursues, so the passive claims nothing there
     * -- which is what keeps its contribution measurable rather than mixed in
     * with behaviour the game was doing anyway.
     */
    public static final double VANILLA_FOLLOW = 16.0;

    /**
     * How far pursuit reaches with the passive.
     *
     * [FIXTURE -- expect to tune] classes.md records the detection radius as
     * unresolved. "Substantially farther" is the only constraint it gives, and
     * doubling is the reading taken.
     */
    public static final double DEFAULT_EXTENDED = 32.0;

    /**
     * The undead this acts on.
     *
     * [OPEN] classes.md leaves "eligible undead" unresolved; this is a working
     * set, and two exclusions are deliberate rather than oversights:
     *
     * <ul>
     *   <li><b>Zombified piglins</b> are undead but NEUTRAL, and they retaliate
     *       as a group. Making them pursue would not extend a danger Skeleton
     *       Crew already lives with -- it would manufacture a new one, and a
     *       lethal one, out of mobs that were ignoring everybody.</li>
     *   <li><b>The Wither</b> is a boss, summoned deliberately. Pulling it is
     *       not converting ambient night-time danger into opportunity, which is
     *       what the passive is for.</li>
     * </ul>
     */
    public static final Set<EntityType> ELIGIBLE = Set.of(
            EntityType.ZOMBIE, EntityType.HUSK, EntityType.DROWNED, EntityType.ZOMBIE_VILLAGER,
            EntityType.SKELETON, EntityType.STRAY, EntityType.BOGGED, EntityType.WITHER_SKELETON,
            EntityType.PHANTOM, EntityType.ZOMBIE_HORSE, EntityType.SKELETON_HORSE);

    public static boolean eligible(EntityType type) { return type != null && ELIGIBLE.contains(type); }

    /**
     * Whether this mob should be pointed at the Skeleton Crew player now.
     *
     * @param distance   from the mob to the player
     * @param normal     the range vanilla already covers
     * @param extended   the range the passive reaches
     * @param hasTarget  whether the mob is already pursuing something
     *
     * <h2>An engaged mob is left alone</h2>
     *
     * {@code hasTarget} refusing is the load-bearing one. Converting night-time
     * danger into opportunity means drawing mobs that were not fighting anyone;
     * pulling one off an ally mid-fight is a <b>taunt</b>, which is a different
     * and much stronger ability, and not one the design asked for.
     *
     * <h2>And so is the lower bound</h2>
     *
     * Inside the vanilla range the game already does this. Re-targeting there
     * would mean the passive silently overriding ordinary mob behaviour -- and
     * it would make "how much does this passive do" unanswerable, because its
     * effect and vanilla's would be the same blocks.
     */
    public static boolean shouldPursue(double distance, double normal, double extended, boolean hasTarget) {
        if (hasTarget) return false;
        if (extended <= normal) return false;
        return distance > normal && distance <= extended;
    }

    private UndeadAffinity() {}
}
