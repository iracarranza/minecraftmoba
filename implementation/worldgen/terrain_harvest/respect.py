"""What authoring must not cut through, and how to remove a tree properly.

Two failures with one cause: authoring writes blocks without looking at what is
already standing there.

A Route ran through a village house, because nothing in the corridor code knows
that a building is different from a hillside. And clearing headroom around the
Aether Fountain cut trees off at the pad's height, leaving their canopies
floating -- the trunk was inside the cleared volume and the leaves were above
it, so half of each tree was removed and the other half stayed.

Neither wants a smarter algorithm. They want authoring to ask two questions it
never asked: is this someone's building, and is this block part of a tree?
"""
from __future__ import annotations

# Blocks that only exist because something was BUILT here -- a village, a ruin,
# a vanilla structure. Authoring may route past them and must not overwrite
# them. Natural stone and dirt are absent on purpose: levelling a hillside is
# terrain work, demolishing a wall is not.
STRUCTURE = {
    'minecraft:cobblestone', 'minecraft:mossy_cobblestone', 'minecraft:cobblestone_stairs',
    'minecraft:cobblestone_slab', 'minecraft:stone_bricks', 'minecraft:mossy_stone_bricks',
    'minecraft:cracked_stone_bricks', 'minecraft:chiseled_stone_bricks',
    'minecraft:stone_brick_stairs', 'minecraft:stone_brick_slab',
    'minecraft:bricks', 'minecraft:smooth_stone_slab', 'minecraft:glass', 'minecraft:glass_pane',
    'minecraft:bookshelf', 'minecraft:crafting_table', 'minecraft:furnace', 'minecraft:barrel',
    'minecraft:chest', 'minecraft:bell', 'minecraft:lantern', 'minecraft:composter',
    'minecraft:smoker', 'minecraft:blast_furnace', 'minecraft:cartography_table',
    'minecraft:fletching_table', 'minecraft:smithing_table', 'minecraft:loom',
    'minecraft:stonecutter', 'minecraft:grindstone', 'minecraft:brewing_stand',
    'minecraft:cauldron', 'minecraft:hay_block', 'minecraft:bed', 'minecraft:ladder',
    'minecraft:torch', 'minecraft:wall_torch', 'minecraft:scaffolding', 'minecraft:farmland',
}

# Wood, planks, doors, fences and carpets, matched by suffix because every wood
# type repeats them. A log is deliberately NOT here: a tree is a natural
# feature, handled by felling rather than by refusal.
STRUCTURE_SUFFIX = (
    '_planks', '_door', '_trapdoor', '_fence', '_fence_gate', '_stairs', '_slab',
    '_sign', '_wall_sign', '_carpet', '_wool', '_terracotta', '_concrete', '_glass',
    '_glass_pane', '_bed', '_shingles',
)

LOG_SUFFIX = ('_log', '_wood', '_stem', '_hyphae')
LEAF_SUFFIX = ('_leaves',)
FOLIAGE = {'minecraft:mushroom_stem', 'minecraft:red_mushroom_block',
           'minecraft:brown_mushroom_block', 'minecraft:shroomlight'}


def is_structure(name: str | None) -> bool:
    """Whether this block exists because somebody built it."""
    if not name:
        return False
    if name in STRUCTURE:
        return True
    # A plank stair in a village and a plank stair in a Route deck are the same
    # block, so this is about what is ALREADY there, never about what is placed.
    return name.endswith(STRUCTURE_SUFFIX)


def is_log(name: str | None) -> bool:
    return bool(name) and name.endswith(LOG_SUFFIX)


def is_leaf(name: str | None) -> bool:
    return bool(name) and (name.endswith(LEAF_SUFFIX) or name in FOLIAGE)


def is_tree(name: str | None) -> bool:
    return is_log(name) or is_leaf(name)


def fell(editor, column, x: int, z: int, y: int, air, reach: int = 6, ceiling: int = 320):
    """Remove a whole tree rather than the part that was in the way.

    Clearing a fixed height above the ground takes the trunk and leaves the
    canopy hanging, which is the floating-leaf bug. Felling walks the trunk up
    and takes the leaves around it, so the result reads as a cleared tree
    instead of a damaged one.

    Returns the number of blocks removed.
    """
    removed = 0
    trunk = []
    yy = y
    while yy < ceiling:
        name = column(x, yy, z)
        if not is_tree(name):
            break
        trunk.append(yy)
        yy += 1
    if not trunk:
        return 0
    top = trunk[-1]
    for ty in range(y, top + 1):
        editor.set(x, ty, z, air); removed += 1
    # Canopy: leaves near the trunk's upper half, which is where they are.
    for ly in range(max(y, top - reach), top + reach + 1):
        for dx in range(-reach, reach + 1):
            for dz in range(-reach, reach + 1):
                if dx == 0 and dz == 0:
                    continue
                if is_leaf(column(x + dx, ly, z + dz)):
                    editor.set(x + dx, ly, z + dz, air); removed += 1
    return removed
