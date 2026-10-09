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
     * Affects one block, identified by the click.
     *
     * Wax this block.
     *
     * <b>Supported.</b> {@code AbilityContext} carries the clicked block.
     * Carried rather than re-found: "used ON that block" and "whatever my
     * raycast finds now" are different predicates at reach edges.
     */
    UNIT_BLOCK,

    /**
     * Affects one creature, identified by the click.
     *
     * Heal this teammate.
     *
     * <b>Supported.</b> {@code AbilityContext} carries the interacted entity
     * separately from the block, because the client sends one interaction or
     * the other -- so an ability reads whichever its form expects and refuses
     * cleanly when neither is there. A heal that re-found its target could
     * heal whoever stepped into the line after the click.
     */
    UNIT_ENTITY,

    /**
     * Selects a set of blocks to affect.
     *
     * Set this grass patch alight. Break this vein.
     *
     * <b>Supported, and previews for free.</b> This is exactly what {@code
     * Ability.preview} returns, which is why it is the form cast modes were
     * built around.
     */
    AREA_BLOCKS,

    /**
     * Selects a set of creatures to affect.
     *
     * Crash Landing damages everyone near the impact. Irresistible Buffet
     * seats every enemy the table rolls through.
     *
     * <b>Supported, and does NOT preview for free.</b> That is the whole
     * reason it is separate from {@link #AREA_BLOCKS} rather than folded into
     * it: {@code preview} returns blocks, so an area of creatures labelled
     * AREA would inherit a guarantee that is false for it, and a cast mode
     * would offer a preview with nothing in it.
     */
    AREA_ENTITIES,

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

    /** Whether this form aims at one thing rather than a set. */
    public boolean unit() { return this == UNIT_BLOCK || this == UNIT_ENTITY; }

    /** Whether this form aims at a set rather than one thing. */
    public boolean area() { return this == AREA_BLOCKS || this == AREA_ENTITIES; }

    /** Whether what is aimed at is terrain. */
    public boolean blocks() { return this == UNIT_BLOCK || this == AREA_BLOCKS; }

    /** Whether what is aimed at is a creature. */
    public boolean entities() { return this == UNIT_ENTITY || this == AREA_ENTITIES; }

    /**
     * Whether {@code Ability.preview} can describe this form for free.
     *
     * Only block forms. A preview is a set of blocks, so a form that aims at
     * creatures has nothing to hand it -- which is the distinction the split
     * exists to make visible rather than a limitation of the preview.
     */
    public boolean previewsAsBlocks() { return blocks(); }

    /**
     * Whether an aim can be expressed as a point in the world.
     *
     * A creature is not a point: you aim at it, and where it is standing is
     * incidental. Everything else can be marked.
     */
    public boolean pointable() { return this != UNIT_ENTITY && this != SELF && supported(); }
}
