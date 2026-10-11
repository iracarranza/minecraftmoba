package com.minecraftmoba.plugin;

import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

/**
 * What a click would have carried, for a body that has no mouse.
 *
 * A real player's click reaches the plugin as an event that names what was under the crosshair:
 * the block and the face for a click on a block, the entity for a right-click on one, nothing for a
 * click on air. {@link AbilityInputs} carries that rather than re-finding it, because "used ON that
 * block" and "whatever my ray finds now" differ at the edges of reach, and several abilities
 * (Hotswap, Clip, Flip and Press, Graveyard Shift) read it first.
 *
 * A body cast used to send none of that, so every ability took its fallback path and the click path
 * was never exercised by a test, and a ground ability aimed at a wall behaved differently for a body
 * than for a player. This resolves the same thing a client would.
 *
 * <h2>The rules, as the game has them</h2>
 *
 * <ul>
 *   <li>Reach is vanilla survival: {@link #BLOCK_REACH} for a block, {@link #ENTITY_REACH} for a creature.
 *   <li>The crosshair is on a creature when one is in reach and no block is nearer; otherwise on the
 *       block; otherwise on air.
 *   <li>What an event carries depends on the INPUT, not just the target: a right-click carries the block
 *       and face, or the creature; a left-click carries the block and face but NOT a creature, because
 *       hitting a creature is a damage event; the hand swap and the drop carry nothing.
 * </ul>
 */
public final class ClickResolver {
    private ClickResolver() {}

    /** Vanilla survival interaction ranges, in blocks. */
    public static final double BLOCK_REACH = 4.5, ENTITY_REACH = 3.0;

    public enum Kind { AIR, BLOCK, ENTITY }

    /** What is under the crosshair. {@code face} is the face of the block that was hit, pointing back at the caster. */
    public record Click(Kind kind, Block block, BlockFace face, Entity entity) {
        public static final Click AIR = new Click(Kind.AIR, null, null, null);

        public String describe() {
            return switch (kind) {
                case AIR -> "air";
                case BLOCK -> block.getType() + " " + face;
                case ENTITY -> entity.getName();
            };
        }
    }

    /** What the event for an input carries. */
    public record Carries(boolean block, boolean entity) {
        public static final Carries NOTHING = new Carries(false, false);
    }

    /**
     * Pure: which thing the crosshair is on, given how far away the nearest creature and the nearest block in reach are
     * (null when there is none in reach). A creature wins a tie and anything nearer than the block.
     */
    public static Kind choose(Double entityDistance, Double blockDistance) {
        if (entityDistance != null && (blockDistance == null || entityDistance <= blockDistance)) return Kind.ENTITY;
        if (blockDistance != null) return Kind.BLOCK;
        return Kind.AIR;
    }

    /** Pure: what the event for this input carries when the crosshair is on this kind of thing. */
    public static Carries carries(AbilityInputs.Input input, Kind kind) {
        return switch (input) {
            case RIGHT_CLICK -> switch (kind) {
                case BLOCK -> new Carries(true, false);
                case ENTITY -> new Carries(false, true);
                case AIR -> Carries.NOTHING;
            };
            case LEFT_CLICK -> kind == Kind.BLOCK ? new Carries(true, false) : Carries.NOTHING;
            case SWAP_HAND, DROP -> Carries.NOTHING;
        };
    }

    /** What a client standing where {@code body} stands, looking where it looks, would have clicked on. */
    public static Click resolve(Player body) {
        Location eye = body.getEyeLocation();
        var direction = eye.getDirection();
        var blockHit = body.getWorld().rayTraceBlocks(eye, direction, BLOCK_REACH, FluidCollisionMode.NEVER, false);
        var entityHit = body.getWorld().rayTraceEntities(eye, direction, ENTITY_REACH, e -> !e.equals(body));
        Double blockDistance = blockHit == null ? null : eye.toVector().distance(blockHit.getHitPosition());
        Double entityDistance = entityHit == null ? null : eye.toVector().distance(entityHit.getHitPosition());
        return switch (choose(entityDistance, blockDistance)) {
            case ENTITY -> new Click(Kind.ENTITY, null, null, entityHit.getHitEntity());
            case BLOCK -> new Click(Kind.BLOCK, blockHit.getHitBlock(), blockHit.getHitBlockFace(), null);
            case AIR -> Click.AIR;
        };
    }
}
