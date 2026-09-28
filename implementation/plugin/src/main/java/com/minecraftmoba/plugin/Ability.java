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
    default void tick() {}
    record AbilityContext(MobaPlugin plugin, Provenance provenance, AbilityInputs inputs,
                           ClassDefinition classDefinition, PlayerData playerData) {
        public AbilityContext(MobaPlugin plugin, Provenance provenance, AbilityInputs inputs) {
            this(plugin, provenance, inputs, null, null);
        }
        public String branchFor(String abilityId) {
            if (playerData != null) {
                String selected = playerData.classState.get("branch." + abilityId);
                if (selected != null) return selected;
            }
            return classDefinition == null ? null : classDefinition.branchFor(abilityId);
        }
    }
}
