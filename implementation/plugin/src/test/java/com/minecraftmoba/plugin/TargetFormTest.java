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

    @Test void everyFormButOneIsServedToday() {
        for (TargetForm form : values())
            if (form != VECTOR) assertTrue(form.supported(), form + " is served");
        assertFalse(VECTOR.supported(),
                "two designations in one activation collides with Double cast's input");
    }

    @Test void whatIsAimedAtIsBlockOrCreatureAndTheVocabularySaysWhich() {
        // UNIT used to cover both -- its own doc said "Wax this block. Heal
        // this teammate." The code had always distinguished them, since
        // AbilityContext carries the block and the entity separately, so the
        // implementation knew and the vocabulary did not.
        assertTrue(UNIT_BLOCK.blocks() && UNIT_BLOCK.unit());
        assertTrue(UNIT_ENTITY.entities() && UNIT_ENTITY.unit());
        assertTrue(AREA_BLOCKS.blocks() && AREA_BLOCKS.area());
        assertTrue(AREA_ENTITIES.entities() && AREA_ENTITIES.area());

        assertFalse(SELF.blocks() || SELF.entities(), "SELF aims at nothing");
        assertFalse(DIRECTION.unit() || DIRECTION.area(), "a facing is neither");
    }

    @Test void onlyBlockFormsGetAPreviewForFree() {
        // The reason AREA split rather than absorbing creatures. preview()
        // returns blocks, so an area of creatures labelled AREA would inherit
        // a guarantee that is false for it, and a cast mode would offer a
        // preview with nothing in it.
        assertTrue(AREA_BLOCKS.previewsAsBlocks());
        assertFalse(AREA_ENTITIES.previewsAsBlocks(),
                "Crash Landing damages everyone near the impact and has no blocks to show");
        assertTrue(UNIT_BLOCK.previewsAsBlocks());
        assertFalse(UNIT_ENTITY.previewsAsBlocks());
    }

    @Test void aCreatureIsNotAPointAndEverythingElseIs() {
        // CombatBehavior asked this as "AREA || DIRECTION", which read every
        // unit target as a creature. A block is a unit target and is a point.
        assertFalse(UNIT_ENTITY.pointable());
        assertFalse(SELF.pointable(), "there is no point to mark");
        assertFalse(VECTOR.pointable(), "not served at all");
        for (TargetForm form : new TargetForm[]{UNIT_BLOCK, AREA_BLOCKS, AREA_ENTITIES, DIRECTION})
            assertTrue(form.pointable(), form + " can be aimed at a place");
    }

    /**
     * A unit form needs its target carried from the click, and gets one of
     * each kind.
     *
     * Block and entity are separate because the client sends one interaction
     * or the other; an ability reads whichever its form expects and refuses
     * when neither is there. That separation is what the split in the enum
     * finally says out loud.
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
     * AREA_BLOCKS is what preview() already returns, which is why it gets a
     * targeting preview for free and is the form cast modes were built around.
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
        assertEquals(7, values().length,
                "SELF was added when the vocabulary was tested against the real roster. "
                        + "UNIT and AREA then each split by WHAT is aimed at, because the "
                        + "code already distinguished a block from a creature and the words "
                        + "did not. Adding an eighth is a design decision, not a convenience");
    }
}
