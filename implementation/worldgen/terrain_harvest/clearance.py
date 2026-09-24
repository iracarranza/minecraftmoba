"""What must be out of the way before authoring writes a structure.

Two failures with one cause, and it is the same cause `respect` was written
for: authoring wrote blocks without asking what was already there. `respect`
fixed it for Routes. Only Routes.

BUILDINGS. `clear_and_foundation` treated a built block as "not terrain" and
stepped over it, so an objective sited on a village house would build AROUND
the house and embed it. Meanwhile `column_scan.verify_site` FAILS a site with
standing structure in its footprint. The builder and the verifier therefore
held opposite policies on the same condition -- this project's recurring defect,
two implementations of one idea -- and which one applied depended only on which
ran. The verifier is right: a site that needs a building demolished or absorbed
is the wrong site. So the builder refuses too, and refuses BEFORE writing
anything, because a partial build leaves a world that is neither the old one nor
the new one.

ENTITIES. Nothing moved animals out of a footprint, so a sheep standing where
the Bastion went was entombed. Small in effect and easy to dismiss, but it is
the same omission stated again: authoring considered blocks and nothing else.
Entities live in their own region files, so they are edited there rather than
hoped about.
"""
from __future__ import annotations

from pathlib import Path

from serialization.nbt import plain
from serialization.region import read_region, write_region

from .respect import is_structure


def standing_structures(reader, x: int, z: int, half: int, base_y: int,
                        headroom: int, *, stride: int = 1):
    """Built blocks standing inside a footprint. Reads only."""
    found = []
    for dx in range(-half, half + 1, stride):
        for dz in range(-half, half + 1, stride):
            if dx * dx + dz * dz > half * half:
                continue
            for dy in range(-1, headroom):
                block = reader(x + dx, base_y + dy, z + dz)
                if block and is_structure(block):
                    found.append({'block': block, 'at': [x + dx, base_y + dy, z + dz]})
                    break
    return found


def refuse_built_sites(reader, placements, extents):
    """Which placements sit on something already built.

    Returned rather than raised, so the caller reports every offending site at
    once instead of the first one. A build that stops at the first refusal makes
    the next run discover the second, and the run after that the third.
    """
    refusals = []
    for p in placements:
        extent = extents.get(p['structure'])
        if not extent:
            continue
        half, headroom = extent
        x, y, z = p['world_xyz']
        hits = standing_structures(reader, x, z, half, y, headroom)
        if hits:
            refusals.append({
                'structure': p['structure'], 'team': p.get('team'),
                'world_xyz': [x, y, z],
                'standing_structure_blocks': len(hits),
                'example': hits[0],
                'detail': 'authoring must not demolish or absorb a building; '
                          'this is the wrong site, not a site to clear',
            })
    return refusals


def _inside(pos, footprints):
    x, y, z = pos[0], pos[1], pos[2]
    for cx, cy, cz, half, headroom in footprints:
        if abs(x - cx) <= half and abs(z - cz) <= half and -2 <= y - cy <= headroom:
            return True
    return False


def evict_entities(world: Path, placements, extents) -> dict:
    """Remove entities standing inside an authored footprint.

    They are deleted rather than displaced. Displacing them means choosing a
    destination, and there is no non-arbitrary one; a passive mob that would
    have been entombed is not worth inventing a relocation rule for. What
    matters is that the world does not contain a suffocating animal because
    authoring never looked.
    """
    footprints = []
    for p in placements:
        extent = extents.get(p['structure'])
        if not extent:
            continue
        half, headroom = extent
        x, y, z = p['world_xyz']
        footprints.append((x, y, z, half + 1, headroom))
    if not footprints:
        return {'regions': 0, 'removed': 0, 'kinds': {}}

    entities_dir = Path(world) / 'entities'
    if not entities_dir.is_dir():
        return {'regions': 0, 'removed': 0, 'kinds': {},
                'note': 'no entities directory; nothing has been simulated here yet'}

    removed, kinds, touched = 0, {}, 0
    for region in sorted(entities_dir.glob('r.*.mca')):
        chunks, dirty = {}, False
        for cx, cz, name, root in read_region(region):
            # EDIT THE TAG TREE, NOT A PLAIN COPY.
            #
            # This read the chunk, converted it with `plain()`, filtered that,
            # and assigned the plain dict back as `root`. `write_region` needs
            # Tag objects, so it raised "'dict' object has no attribute 'kind'"
            # -- and only on a chunk that actually had an entity to evict,
            # which is why seven of eight seeds in the first batch passed
            # straight through it and the eighth crashed.
            #
            # `plain()` is still used to READ positions, because comparing
            # coordinates is what it is good for. Only the write side changed.
            entities = (root.value or {}).get('Entities') if root.value else None
            if entities is not None and entities.value:
                keep_tags, dropped = [], 0
                for tag in entities.value:
                    pos = plain(tag).get('Pos') or (0, 0, 0)
                    if _inside(pos, footprints):
                        kind = plain(tag).get('id', 'unknown')
                        kinds[kind] = kinds.get(kind, 0) + 1
                        dropped += 1
                    else:
                        keep_tags.append(tag)
                if dropped:
                    removed += dropped
                    entities.value = keep_tags
                    dirty = True
            chunks[(cx, cz)] = (name, root)
        if dirty:
            write_region(region, chunks)
            touched += 1
    return {'regions': touched, 'removed': removed, 'kinds': kinds}
