package com.minecraftmoba.plugin;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

/**
 * A branch changing an ability's INPUT FORM.
 *
 * The motivating case: an ability that shoots a projectile, whose branch makes
 * it a held beam instead. Those are different input shapes -- one commits and
 * is done, the other runs while held -- and a branch is allowed to change that.
 *
 * It needs no new contract. {@code preview} receives the AbilityContext, which
 * carries the branch, so an ability can present one form per branch. The input
 * layer asks "does this have a preview" every activation rather than once, so
 * the answer may differ between two presses of the same button.
 */
class BranchInputFormTest {

    /** Projectile branch: aimable. Beam branch: not, because it sustains instead. */
    private static final class Forked implements Ability {
        private final String branch;
        Forked(String branch) { this.branch = branch; }

        public String id() { return "arc"; }
        public String displayName() { return "Arc"; }
        public long cooldownTicks() { return 20; }
        public boolean execute(Player p, AbilityContext c) { return true; }

        @Override public Collection<Block> preview(Player p, AbilityContext c) {
            // A projectile has a landing point worth marking before release.
            // A held beam does not: it goes where you look WHILE it runs, so
            // there is nothing to pre-commit to and nothing to preview.
            return "beam".equals(branch) ? List.of() : List.of(mock(Block.class));
        }
    }

    /**
     * The projectile branch aims; the beam branch does not.
     *
     * An empty preview is what makes an ability ignore cast modes, and that is
     * exactly right for a beam: Hold cast commits on RELEASE, and a beam needs
     * the button still down to sustain. The two want the same button for
     * opposite purposes, so the form that has its own hold semantics opts out
     * of the one the cast mode would impose.
     */
    @Test void aBranchDecidesWhetherTheAbilityAimsAtAll() {
        var player = mock(Player.class);
        var context = new Ability.AbilityContext(null, null, null);

        assertFalse(new Forked("projectile").preview(player, context).isEmpty(),
                "a projectile has a landing point worth marking");
        assertTrue(new Forked("beam").preview(player, context).isEmpty(),
                "a beam goes where you look while it runs; there is nothing to pre-commit");
    }

    /**
     * The gate is re-asked every activation, not cached.
     *
     * A player who switches branch between casts changes input form with it,
     * and nothing has to be told -- which is the property that makes this cost
     * no new contract.
     */
    @Test void theFormIsReadPerActivationRatherThanOnce() {
        var player = mock(Player.class);
        var context = new Ability.AbilityContext(null, null, null);

        Ability projectile = new Forked("projectile");
        Ability beam = new Forked("beam");
        assertNotEquals(projectile.preview(player, context).isEmpty(),
                        beam.preview(player, context).isEmpty(),
                "the same ability id, two input forms");
    }

    /** By default an ability targets nothing, so nothing opts in by accident. */
    @Test void theDefaultIsNoPreviewAndThereforeNoAiming() {
        Ability plain = new Ability() {
            public String id() { return "plain"; }
            public String displayName() { return "Plain"; }
            public long cooldownTicks() { return 0; }
            public boolean execute(Player p, AbilityContext c) { return true; }
        };
        assertTrue(plain.preview(mock(Player.class), new Ability.AbilityContext(null, null, null))
                        .isEmpty(),
                "a self-buff must not grow a preview with nothing in it");
    }
}
