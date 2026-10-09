package com.minecraftmoba.plugin;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

/**
 * One thing an ability can do, and how it is aimed and gestured.
 *
 * <h2>Why the unit is an output and not an ability</h2>
 *
 * classes.md settled this by testing the vocabulary against a real roster
 * rather than reasoning about it: Pantheon's Q is a <b>tap</b> that stabs and a
 * <b>hold</b> that throws. Two outputs of one ability, with different target
 * forms and different input forms, and a single (target, input) pair on the
 * ability cannot describe it.
 *
 * Most abilities have exactly one output and the distinction never shows. It
 * shows the moment an ability wants a tap and a hold to do different things.
 *
 * <h2>The input form is the selector</h2>
 *
 * Where an ability has more than one output, the gesture is what chooses
 * between them -- so two outputs sharing an input form would be two things one
 * gesture selects, which is not a selection. {@link #of} refuses that rather
 * than letting the ambiguity reach a player.
 *
 * <h2>Who it reaches is a fourth question</h2>
 *
 * {@link Recipients} is carried because no other axis answers it. Crash
 * Landing is SELF and damages nearby enemies; Deathly Clutches declares
 * {@code combat: true} and touches nobody but the caster. Target form, input
 * form and combat-ness each answer something else.
 *
 * <h2>What this is not</h2>
 *
 * Not {@link CastMode}. A mode is the player's preference about whether their
 * aim is verified before committing, and it never changes what is cast. Target
 * and input form belong to the ability; the mode belongs to the person.
 */
public record AbilityOutput(String id, TargetForm target, InputForm input, Recipients affects,
                            boolean affectsCaster) {

    public AbilityOutput {
        Objects.requireNonNull(id, "An output needs an id.");
        Objects.requireNonNull(target, "An output must declare what it aims at.");
        Objects.requireNonNull(input, "An output must declare how it is gestured.");
        Objects.requireNonNull(affects, "An output must declare who it acts on.");
    }

    /** An output that does nothing to its own caster, which is most of them. */
    public AbilityOutput(String id, TargetForm target, InputForm input, Recipients affects) {
        this(id, target, input, affects, false);
    }

    /**
     * The ordinary case: one output, so the ability is fully described by a
     * target form, an input form and who it reaches.
     */
    public static List<AbilityOutput> single(String id, TargetForm target, InputForm input,
                                             Recipients affects) {
        return List.of(new AbilityOutput(id, target, input, affects));
    }

    /**
     * The same, for an output whose effect also lands on its caster.
     *
     * Separate from {@link Recipients} rather than a value in it, because the
     * caster is not an alternative to enemies and allies -- Crash Landing
     * damages nearby enemies AND takes fixed fall damage itself, and an enum
     * would need a value for every combination.
     */
    public static List<AbilityOutput> single(String id, TargetForm target, InputForm input,
                                             Recipients affects, boolean affectsCaster) {
        return List.of(new AbilityOutput(id, target, input, affects, affectsCaster));
    }

    /**
     * Several outputs on one ability, checked for a usable selector.
     *
     * <h2>The selector is the gesture OR what was aimed at</h2>
     *
     * This used to require distinct input forms, on the reading that a tap and
     * a hold are what choose between outputs. The roster disagrees: Skeleton
     * Crew's Graveyard Shift strikes an enemy you target and otherwise raises
     * a Crew Member, and Chef's Flip and Press flips a targeted enemy or
     * advances a targeted cooking station. One gesture, two outputs, chosen by
     * <b>what the activation found</b>.
     *
     * That is already how the ability layer works: {@code AbilityContext}
     * carries the clicked block and the interacted entity separately,
     * documented as "an ability reads whichever its form expects and refuses
     * cleanly when neither is there". Selection by target was in the code
     * before it was in the vocabulary.
     *
     * So what must be distinct is the <b>pair</b>. Two outputs sharing both a
     * target form and an input form have nothing to tell them apart, and that
     * is the only case refused.
     *
     * @throws IllegalArgumentException if two outputs share both forms.
     */
    public static List<AbilityOutput> of(AbilityOutput... outputs) {
        record Selector(TargetForm target, InputForm input) {}
        var seen = new LinkedHashSet<Selector>();
        for (AbilityOutput o : outputs)
            if (!seen.add(new Selector(o.target(), o.input())))
                throw new IllegalArgumentException("Outputs of one ability are selected by gesture or "
                        + "by what is aimed at, and '" + o.id() + "' shares both " + o.target()
                        + " and " + o.input() + " with another, so nothing selects it.");
        return List.of(outputs);
    }

    /** Whether the player's cast-mode preference can be honoured for this output. */
    public boolean permits(CastMode mode) { return input.permits(mode); }

    /** The mode actually used for this output, given the player's preference. */
    public CastMode resolve(CastMode preferred) { return input.resolve(preferred); }

    /**
     * Whether how long the input is held during the aim changes this output.
     *
     * This is exactly {@link InputForm#CHARGED}, and it is what the old boolean
     * on {@link Ability} was standing in for. Deriving it means an ability
     * cannot declare a form and then contradict it.
     */
    public boolean holdDependent() { return input == InputForm.CHARGED; }
}
