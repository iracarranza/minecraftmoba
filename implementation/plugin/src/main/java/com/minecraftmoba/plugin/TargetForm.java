package com.minecraftmoba.plugin;

/**
 * The four ways an ability can be aimed.
 *
 * <h2>Why these are named</h2>
 *
 * So that presenting a new ability does not require inventing machinery for it.
 * Each label says what the ability needs from the input layer and what it gets,
 * and an ability that fits one of these is already implementable.
 *
 * The labels are a <b>vocabulary</b>, not a hierarchy: an ability is described
 * by one of them, and nothing here ranks or nests them.
 *
 * <h2>What each one costs</h2>
 *
 * Three are fully supported today. {@link #VECTOR} is not, and is named anyway
 * because naming it is what stops it being reinvented badly the first time an
 * ability wants it.
 */
public enum TargetForm {

    /**
     * Affects the caster, or nothing outside them.
     *
     * Reconfiguratron swaps two components in your own inventory. A channel
     * with no aim. A self-buff. These target nothing, and saying so is not a
     * formality: an ability with no target has nothing to preview, which is
     * what makes it ignore cast modes entirely.
     *
     * Added when the vocabulary was tested against the real roster and three
     * shipped abilities fitted none of the other four.
     */
    SELF,

    /**
     * Affects whatever was targeted, immediately.
     *
     * Wax this block. Heal this teammate. One thing, already identified by the
     * click, and the ability acts on it.
     *
     * <b>Supported.</b> {@code AbilityContext} carries the clicked block and
     * the interacted entity, so the ability reads whichever its form expects
     * and refuses cleanly when neither is there. Carried rather than re-found:
     * "used ON that player" and "whatever my raycast finds now" are different
     * predicates, and a heal that re-found its target could heal whoever
     * stepped into the line after the click.
     */
    UNIT,

    /**
     * Selects a set of blocks to affect.
     *
     * Set this grass patch alight. Break this vein.
     *
     * <b>Supported.</b> This is exactly what {@code Ability.preview} returns,
     * which means an area ability gets a targeting preview for free and is the
     * form cast modes were built around.
     */
    AREA,

    /**
     * Aims in a direction and fires.
     *
     * Lunge this way. Observe the player I am looking at.
     *
     * <b>Supported.</b> Facing is read at RESOLUTION rather than at trigger,
     * which is what lets a multi-step machine point three different ways as
     * the player moves the mouse. A direction ability may preview the blocks
     * along its line, or nothing at all when the line is the obvious part.
     */
    DIRECTION,

    /**
     * Designates both a start and an end.
     *
     * <b>NOT SUPPORTED, and named deliberately.</b> Nothing uses it yet, and
     * naming it now is what stops it being reinvented badly the first time
     * something does.
     *
     * What it needs that the others do not: <b>two designations in one
     * activation</b>. {@link AimState} holds one aim and resolves it, so a
     * vector wants a first press that fixes the start and a second that fixes
     * the end -- which is Double cast's shape used for targeting rather than
     * for commitment, and would collide with it on the same input.
     *
     * That collision is the design question to settle before building it, not
     * an implementation detail: under Double cast a vector ability would need
     * three presses, and under Hold it would need a press, a drag and a
     * release that the input layer cannot see.
     */
    VECTOR;

    /** Whether the input layer can serve this form today. */
    public boolean supported() { return this != VECTOR; }
}
