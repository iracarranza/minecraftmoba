package com.minecraftmoba.plugin;

import org.junit.jupiter.api.Test;

import static com.minecraftmoba.plugin.TargetForm.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

/**
 * The cast-type vocabulary, and what each label promises.
 *
 * These exist so that presenting a new ability does not require inventing
 * machinery for it. The tests assert the promises rather than the labels.
 */
class TargetFormTest {

    @Test void threeFormsAreServedTodayAndOneIsNot() {
        assertTrue(UNIT.supported());
        assertTrue(AREA.supported());
        assertTrue(DIRECTION.supported());
        assertFalse(VECTOR.supported(),
                "two designations in one activation collides with Double cast's input");
    }

    /**
     * UNIT needs a target carried from the click, and gets one of each kind.
     *
     * Block and entity are separate because the client sends one interaction
     * or the other; an ability reads whichever its form expects and refuses
     * when neither is there.
     */
    @Test void unitTargetingHasBothATargetAndAWayToRefuse() {
        var nothing = new Ability.AbilityContext(null, null, null);
        assertFalse(nothing.onBlock());
        assertFalse(nothing.onEntity());

        var onBlock = new Ability.AbilityContext(null, null, null, null, null,
                mock(org.bukkit.block.Block.class), org.bukkit.block.BlockFace.UP);
        assertTrue(onBlock.onBlock());
        assertFalse(onBlock.onEntity());

        var onEntity = new Ability.AbilityContext(null, null, null, null, null,
                null, null, mock(org.bukkit.entity.Player.class));
        assertTrue(onEntity.onEntity());
        assertFalse(onEntity.onBlock());
    }

    /**
     * AREA is what preview() already returns, which is why it gets a targeting
     * preview for free and is the form cast modes were built around.
     */
    @Test void areaTargetingIsWhatThePreviewContractAlreadyIs() {
        Ability area = new Ability() {
            public String id() { return "vein"; }
            public String displayName() { return "Vein"; }
            public long cooldownTicks() { return 0; }
            public boolean execute(org.bukkit.entity.Player p, AbilityContext c) { return true; }
            @Override public java.util.Collection<org.bukkit.block.Block> preview(
                    org.bukkit.entity.Player p, AbilityContext c) {
                return java.util.List.of(mock(org.bukkit.block.Block.class));
            }
        };
        assertFalse(area.preview(mock(org.bukkit.entity.Player.class),
                new Ability.AbilityContext(null, null, null)).isEmpty());
    }

    /** Every form is nameable, since these are the words abilities get described in. */
    @Test void everyFormHasAName() {
        for (TargetForm form : values()) assertFalse(form.name().isBlank());
        assertEquals(5, values().length,
                "SELF was added when the vocabulary was tested against the real roster "
                        + "and three shipped abilities fitted none of the other four; "
                        + "adding a sixth is a design decision, not a convenience");
    }
}
