package com.minecraftmoba.plugin;

import org.bukkit.entity.Player;
import java.util.Map;
import java.util.List;

/** Extensible ability contract. A selected branch is resolved by the class definition. */
public interface Ability {
    String id();
    String displayName();
    long cooldownTicks();
    boolean execute(Player player, AbilityContext context);
    default Map<String, String> branches() { return Map.of(); }
    default List<String> branchIds() { return List.copyOf(branches().keySet()); }
    default boolean active(Player player) { return false; }
    default void cancel(Player player) {}

    /**
     * A second activation while already active.
     *
     * Returning true means the ability HANDLED it -- release the charge, end
     * the sustain, enter the next state -- and the blanket cancel is skipped.
     * Returning false, the default, leaves the existing behaviour intact: any
     * input during an active ability throws it away.
     *
     * That default is why this is opt-in rather than a new required method.
     * Channel cancellation is correct for everything that does not declare a
     * second state, and an ability that forgot to implement this would
     * otherwise silently become uncancellable.
     *
     * <h2>Both hold shapes go through here</h2>
     *
     * <ul>
     *   <li><b>Charge and release</b> -- the ability charges while active and
     *       fires here at whatever {@link Charge} has reached.</li>
     *   <li><b>Sustain</b> -- the ability runs while active and stops here.</li>
     * </ul>
     *
     * One hook covers both because the difference is not in the input: it is
     * whether the ability does work per tick while active. Giving them
     * separate contracts would make an author choose a category before
     * choosing a behaviour.
     */
    default boolean recast(Player player, AbilityContext context) { return false; }

    /** The charge in progress, if this ability charges and one is running. */
    default Charge charging(Player player) { return null; }

    /**
     * The blocks this activation would affect, for a targeting preview.
     *
     * Empty means <b>this ability does not target</b>, and it is the default:
     * a self-buff or an aimless channel ignores cast modes entirely rather
     * than growing a preview with nothing in it. An ability that returns
     * blocks opts into Hold and Double cast by doing so.
     *
     * Recomputed each tick while aiming, against the player's CURRENT aim --
     * a preview that froze at the first press would be showing where the
     * ability was going to go, which is the one thing aiming exists to change.
     */
    default java.util.Collection<org.bukkit.block.Block> preview(Player player, AbilityContext context) {
        return java.util.List.of();
    }
    default void tick() {}
    /**
     * What the activation knew, including WHAT IT WAS AIMED AT.
     *
     * <h2>Why the block is carried rather than re-found</h2>
     *
     * An ability could ask {@code getTargetBlockExact} at execute time, and it
     * would usually agree. It is not the same predicate: that re-raycasts from
     * the eye at a range the ability picks, with its own handling of fluids and
     * passable blocks, so it disagrees with what the client actually clicked at
     * reach edges. An ability defined as "used ON block a" wants the block the
     * player hit, not whatever they are looking at by the time it runs.
     *
     * Null when the activation came from air, from Swap Offhand, or from a
     * damage event -- which is a real answer, and lets an ability refuse
     * cleanly rather than silently acting on a block behind the target.
     *
     * <h2>The interaction is never entered</h2>
     *
     * Ability mode cancels {@link org.bukkit.event.player.PlayerInteractEvent}
     * unconditionally, BEFORE dispatching the input, so a chest does not open
     * and a lever does not flip. An ability whose conditions are unmet is
     * therefore refused or resolves differently -- it never falls through to
     * the block's own behaviour. A mode that sometimes passed clicks through
     * would be a mode nobody could trust.
     */
    record AbilityContext(MobaPlugin plugin, Provenance provenance, AbilityInputs inputs,
                           ClassDefinition classDefinition, PlayerData playerData,
                           org.bukkit.block.Block block, org.bukkit.block.BlockFace face) {
        public AbilityContext(MobaPlugin plugin, Provenance provenance, AbilityInputs inputs) {
            this(plugin, provenance, inputs, null, null, null, null);
        }
        public AbilityContext(MobaPlugin plugin, Provenance provenance, AbilityInputs inputs,
                              ClassDefinition classDefinition, PlayerData playerData) {
            this(plugin, provenance, inputs, classDefinition, playerData, null, null);
        }
        /** Whether this activation was aimed at a block at all. */
        public boolean onBlock() { return block != null; }
        public String branchFor(String abilityId) {
            if (playerData != null) {
                String selected = playerData.classState.get("branch." + abilityId);
                if (selected != null) return selected;
            }
            return classDefinition == null ? null : classDefinition.branchFor(abilityId);
        }
    }
}
