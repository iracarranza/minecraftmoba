package com.minecraftmoba.plugin;

/**
 * Who an ability output acts on, which no other axis says.
 *
 * <h2>Why this is not derivable</h2>
 *
 * Not from {@link TargetForm}: Crash Landing is {@code SELF} -- it ends the
 * caster's own movement, and nothing is aimed -- and it damages every enemy
 * near the impact. Target form answers "what is aimed at", and an ability can
 * affect people it never aimed at.
 *
 * Not from {@code combat} either. Deathly Clutches declares {@code combat:
 * true} and touches nobody but the caster: it is combat because it acts on a
 * combatant's capacity to fight, which the rule explicitly says includes
 * mitigating and healing yourself.
 *
 * So three existing declarations each answer a different question, and this is
 * the fourth. It exists because the combat chamber needs to know which dummy
 * should be standing there -- an enemy dummy receives damage and control, an
 * ally dummy receives heals and buffs -- and no combination of the others
 * tells it.
 */
public enum Recipients {

    /** The caster, or nobody. A dig, a travel ability, a self-buff. */
    NONE,

    /** Damage, control, debuffs. */
    ENEMIES,

    /** Heals, buffs, mitigation granted to somebody else. */
    ALLIES,

    /**
     * Both, in one output.
     *
     * Not "either depending on the branch" -- a branch that changes the answer
     * is a different declaration, which is what {@code outputs(player,
     * context)} is for. This is an output that reaches both sides at once.
     */
    BOTH;

    public boolean reaches(Recipients side) {
        if (this == side) return true;
        return this == BOTH && (side == ENEMIES || side == ALLIES);
    }
}
