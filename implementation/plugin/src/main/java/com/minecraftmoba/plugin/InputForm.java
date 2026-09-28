package com.minecraftmoba.plugin;

/**
 * The third axis: how the input itself must be made.
 *
 * Alongside {@link TargetForm} (what is aimed at) and {@link CastMode} (whether
 * the aim is verified first). Input form is the gesture the ability requires,
 * and it is the ability's property rather than the player's.
 *
 * <h2>Why it is not a preference</h2>
 *
 * Because an ability may use input form to <b>select which output you get</b>.
 * Pantheon's Q is the reference case: a tap is a short spear stab, a hold is a
 * long spear throw. Those are two outputs of one ability, chosen by gesture.
 *
 * When that is true, a cast-mode preference cannot reshape the gesture without
 * taking an output away from the player. So the ability's form wins, and the
 * preference applies only where it does not collide.
 */
public enum InputForm {

    /**
     * The press is the whole input.
     *
     * The stab. All three cast modes can apply, because none of their gestures
     * conflicts with a bare press -- Quick IS the bare press, and Double's
     * first press can show an indicator instead of firing because a HELD first
     * press is still distinguishable from a tapped one.
     */
    INSTANT,

    /**
     * The input must be held for the ability to occur at all.
     *
     * The throw. Channelling is mandatory to its activation: there is no press
     * that produces it.
     *
     * <b>Not Quick</b>, which is a bare press and would produce the other
     * output. <b>Not Double</b> either, and that is the less obvious one --
     * press-and-hold is already spoken for, so a double-cast throw would have
     * to be press, release, press, hold, which is not a gesture anyone makes.
     * Hold is the only mode whose shape it already is.
     */
    CHANNELED,

    /**
     * Held, and <b>how long</b> changes the output.
     *
     * The widening beam. The hold does two jobs at once -- it shows the
     * indicator and it grows the effect -- and the release ends the growth and
     * commits in one gesture.
     *
     * Everything CHANNELED cannot do, this cannot do, for the same reasons.
     * The difference is only that duration is measured rather than merely
     * required, which is what {@link Ability#holdDependent} declares.
     */
    CHARGED;

    /** Whether a bare press can produce this form. Only INSTANT can. */
    public boolean pressable() { return this == INSTANT; }

    /**
     * Whether a cast mode's gesture is compatible with this form.
     *
     * The collision is about GESTURES, not intent. Double cast needs a first
     * press that does not commit, which a held form cannot give it -- the hold
     * is the activation, so there is nothing for a second press to confirm.
     */
    public boolean permits(CastMode mode) {
        return switch (this) {
            case INSTANT -> true;
            case CHANNELED, CHARGED -> mode == CastMode.HOLD;
        };
    }

    /** The mode actually used, falling back to the only one this form allows. */
    public CastMode resolve(CastMode preferred) {
        return permits(preferred) ? preferred : CastMode.HOLD;
    }
}
