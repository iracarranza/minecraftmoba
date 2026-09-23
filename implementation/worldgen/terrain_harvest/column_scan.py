"""Read actual columns out of a world, for the facts surface samples cannot reach.

The candidate feature grid samples one surface height every eight blocks. That
is enough to talk about relief, canopy and buildability, and it is structurally
incapable of answering two questions the measured objective contracts turn on:

  VERTICAL CLEARANCE   the End Spike is 76-103 blocks tall over an 11-block
                       footprint. Whether that column is actually free -- of
                       terrain, of an overhang, of a hill twenty blocks away
                       that the sample grid recorded as merely "high" -- is a
                       property of the column, not of its top face.

  GROUND CONTACT       the Bastion body meets the ground across 506 columns.
                       Whether a footprint supports that, or spans a ravine
                       whose sampled corners both read as solid, is again a
                       column question.

Both were named in `structures.UNVERIFIABLE_FROM_SAMPLES` and neither had a way
to be established. This establishes them, by reading the world.

Nothing here modifies anything. It is a reader, so a verification failure is
evidence about the recognizer rather than a thing to be authored away.
"""
from __future__ import annotations

import json
import math
from collections import defaultdict
from pathlib import Path

from serialization.nbt import plain
from serialization.region import read_region
from vanilla_search.extract import VanillaChunk

from .respect import is_structure, is_tree

# Blocks that do not obstruct a column.
#
# Trees are NOT in here and are not obstructions either -- they are handled
# separately, by `is_tree`. The first version of this scanner treated a canopy
# as terrain, and it rejected two of six sited objectives on 99887766 because
# oak leaves stood over the pad. That is a false positive: authoring already
# fells trees whole before it builds (`respect.fell`), and doctrine treats
# clearing vegetation as bounded integration work while reserving rejection for
# landscape surgery -- cliff reconstruction, lake filling, excavation.
#
# So vegetation is counted and REPORTED as clearing cost rather than counted as
# an obstruction, and `is_tree` / `is_structure` are imported rather than
# restated, so there is one definition of what a tree is.
TRANSPARENT = frozenset({
    'minecraft:air', 'minecraft:cave_air', 'minecraft:void_air',
    'minecraft:short_grass', 'minecraft:tall_grass', 'minecraft:fern',
    'minecraft:large_fern', 'minecraft:dead_bush', 'minecraft:snow',
    'minecraft:vine', 'minecraft:seagrass', 'minecraft:tall_seagrass',
})

LIQUID = frozenset({'minecraft:water', 'minecraft:lava',
                    'minecraft:flowing_water', 'minecraft:flowing_lava'})

MIN_Y, MAX_Y = -64, 320


class World:
    """A lazily loaded, chunk-cached block reader over a world directory."""

    def __init__(self, path: Path):
        self.path = Path(path)
        self._chunks: dict[tuple[int, int], VanillaChunk | None] = {}
        self._regions: set[tuple[int, int]] = set()

    def _load_region(self, rx, rz):
        if (rx, rz) in self._regions:
            return
        self._regions.add((rx, rz))
        f = self.path / 'region' / f'r.{rx}.{rz}.mca'
        if not f.exists():
            return
        for cx, cz, _, root in read_region(f):
            data = plain(root)
            self._chunks[(cx, cz)] = (VanillaChunk(data)
                                      if data.get('Status') == 'minecraft:full' else None)

    def chunk(self, cx, cz):
        if (cx, cz) not in self._chunks:
            self._load_region(cx >> 5, cz >> 5)
        return self._chunks.get((cx, cz))

    def block(self, x, y, z):
        c = self.chunk(x >> 4, z >> 4)
        return None if c is None else c.block(x, y, z)

    def surface(self, x, z):
        """Highest non-transparent, non-liquid block. None if the chunk is absent."""
        c = self.chunk(x >> 4, z >> 4)
        if c is None:
            return None
        y = c.height('MOTION_BLOCKING_NO_LEAVES', x, z)
        # The heightmap is a hint; confirm downward so leaves and snow do not
        # read as ground.
        for probe in range(min(y + 2, MAX_Y - 1), MIN_Y, -1):
            b = c.block(x, probe, z)
            if b not in TRANSPARENT and b not in LIQUID:
                return probe
        return None


def _disc(radius):
    return [(dx, dz) for dx in range(-radius, radius + 1)
            for dz in range(-radius, radius + 1)
            if dx * dx + dz * dz <= radius * radius + 1]


def pad_level(world: World, x, z, span):
    """The height authoring would level a footprint to: its highest column.

    Clearance has to be measured from here, not from the centre column. The
    centre is arbitrary, and measuring from it reports ordinary ground
    undulation as an obstruction -- at the first site tested, a neighbouring
    column one block higher "blocked" a 103-block spike at dy=1. Authoring
    levels the pad before it builds, so what actually obstructs is terrain
    standing above the levelled pad: a cliff, an overhang, a tree.
    """
    highest = None
    for dx, dz in _disc(span):
        y = world.surface(x + dx, z + dz)
        if y is not None and (highest is None or y > highest):
            highest = y
    return highest


def clearance(world: World, x, z, base_y, needed, *, span=1):
    """How many blocks above `base_y` are free across the whole footprint.

    Stops at `needed`, so a tall clear sky is cheap to confirm. Returns the
    limiting column, because knowing WHICH column blocks a spike is the
    difference between moving it eight blocks and rejecting the site.
    """
    worst, blocker = needed, None
    vegetation = obstructed_by_structure = submerged = 0
    for dx, dz in _disc(span):
        for dy in range(1, needed + 1):
            b = world.block(x + dx, base_y + dy, z + dz)
            if b is None:
                return {'clear': 0, 'needed': needed,
                        'blocked_by': {'block': 'ungenerated chunk',
                                       'at': [x + dx, base_y + dy, z + dz]},
                        'sufficient': False}
            if b in TRANSPARENT:
                continue
            if is_tree(b):
                vegetation += 1        # felled by authoring, not an obstruction
                continue
            if b in LIQUID:
                submerged += 1
            if is_structure(b):
                obstructed_by_structure += 1
            if dy - 1 < worst:
                worst, blocker = dy - 1, {'block': b, 'at': [x + dx, base_y + dy, z + dz]}
            break
    return {'clear': worst, 'needed': needed, 'sufficient': worst >= needed,
            'vegetation_blocks_to_clear': vegetation,
            'standing_structure_blocks': obstructed_by_structure,
            'submerged_columns': submerged,
            **({'blocked_by': blocker} if blocker else {})}


def integration_cost(world: World, x, z, span):
    """What levelling this footprint would actually cost, in blocks moved.

    NOT "how many columns are flat". An early version compared the terrain's
    flatness against the MESH's ground-contact count -- 506 columns for the
    Bastion -- and those are two different quantities: one is a property of the
    structure, the other of the ground. Every site failed, which is the shape of
    a wrong comparison rather than of bad terrain.

    Doctrine asks a different question anyway. A socket is rejected when
    inserting the structure needs major excavation or flattening, so what
    matters is the VOLUME of cut and fill, measured against the datum that
    minimises it -- the footprint's median surface. Per-column, that is directly
    comparable across footprints of different sizes.
    """
    heights = {}
    for dx in range(-span // 2, span // 2 + 1):
        for dz in range(-span // 2, span // 2 + 1):
            y = world.surface(x + dx, z + dz)
            if y is not None:
                heights[(dx, dz)] = y
    if not heights:
        return {'columns': 0, 'why': 'no generated chunks under the footprint'}
    values = sorted(heights.values())
    datum = values[len(values) // 2]
    cut = sum(max(0, v - datum) for v in values)
    fill = sum(max(0, datum - v) for v in values)
    liquid = sum(1 for (dx, dz), y in heights.items()
                 if world.block(x + dx, y, z + dz) in LIQUID)
    return {
        'columns': len(heights),
        'datum_y': datum,
        'cut_blocks': cut,
        'fill_blocks': fill,
        'moved_per_column': round((cut + fill) / len(heights), 3),
        'relief_y': values[-1] - values[0],
        'liquid_columns': liquid,
        'liquid_fraction': round(liquid / len(heights), 4),
    }


def verify_site(world: World, x, z, requirement, *, max_moved_per_column=None,
                max_liquid_fraction=0.10) -> dict:
    """Check one sited objective against its measured physical contract."""
    span = requirement['span_samples']
    base = pad_level(world, x, z, max(1, span // 2))
    if base is None:
        return {'ok': False, 'problems': ['site is outside generated terrain']}
    needed = requirement['vertical_clearance']
    head = clearance(world, x, z, base, needed, span=max(1, span // 2))
    cost = integration_cost(world, x, z, span)
    problems = []
    if head.get('submerged_columns'):
        # Water standing over the pad means the footprint is underwater, which
        # is a different finding from a cliff in the way and deserves its own
        # name -- "clearance blocked by water" reads like an obstruction to
        # clear, and filling a lake is exactly what doctrine refuses.
        problems.append(f"{head['submerged_columns']} columns of the footprint are "
                        f"submerged; the site is underwater and filling it is "
                        f"landscape surgery")
    elif not head.get('sufficient'):
        problems.append(
            f"vertical clearance {head['clear']} < {needed} required"
            + (f" (blocked by {head['blocked_by']['block']} at "
               f"{head['blocked_by']['at']})" if head.get('blocked_by') else ''))
    if max_moved_per_column is not None and cost.get('moved_per_column', 0) > max_moved_per_column:
        problems.append(f"levelling this footprint moves {cost['moved_per_column']} blocks per "
                        f"column, over the {max_moved_per_column} bound: this is terrain "
                        f"surgery, and the socket should be rejected rather than repaired")
    if head.get('standing_structure_blocks'):
        # A village house is not terrain. Authoring must route past it, so a
        # site that needs it demolished is the wrong site.
        problems.append(f"{head['standing_structure_blocks']} blocks of standing "
                        f"structure are inside the column; authoring must not "
                        f"demolish a building to place an objective")
    if cost.get('liquid_fraction', 0) > max_liquid_fraction:
        problems.append(f"{cost['liquid_fraction']:.0%} of the footprint is liquid")
    return {'ok': not problems, 'problems': problems,
            'pad_y': base, 'clearance': head, 'integration_cost': cost}


def verify_placements(world_path: Path, placements, requirements,
                      *, max_moved_per_column=None) -> dict:
    """Verify every sited objective in a world. Reads only."""
    world = World(world_path)
    out, problems = {}, []
    for team, sites in placements.items():
        out[team] = {}
        for objective, xz in sites.items():
            req = requirements.get(objective)
            if not req or not req.get('known'):
                problems.append(f'{team}/{objective}: no measured requirement to verify against')
                continue
            result = verify_site(world, xz[0], xz[1], req,
                                 max_moved_per_column=max_moved_per_column)
            out[team][objective] = result
            problems.extend(f'{team}/{objective}: {p}' for p in result['problems'])
    # Overlap is a property of the SET, not of any one site, so it is checked
    # after all of them. Two objectives sharing ground means whichever is built
    # second silently destroys the first.
    flat = [(team, name, xz) for team, sites in placements.items()
            for name, xz in sites.items()]
    for i, (ta, na, xa) in enumerate(flat):
        for tb, nb, xb in flat[i + 1:]:
            ra = requirements.get(na, {}).get('span_samples', 0) / 2.0
            rb = requirements.get(nb, {}).get('span_samples', 0) / 2.0
            if math.dist((xa[0], xa[1]), (xb[0], xb[1])) < ra + rb:
                problems.append(f'{ta}/{na} and {tb}/{nb} footprints overlap; whichever '
                                f'is authored second destroys the first')
    return {'schema': 'column_verification/1',
            'evidence_state': 'RAW WORLD OBSERVATION',
            'world': str(world_path), 'sites': out, 'problems': problems,
            'verified': not problems}


def readback(world_path: Path, built, templates, *, stride=37) -> dict:
    """Confirm the authored blocks are actually in the world.

    The last check, and the one that caught what analysis could not. Siting
    reasons in eight-block samples; the Bastion is thirty-two blocks across. A
    Bastion and an Outpost were sited eleven blocks apart, both "verified", and
    the second one built simply overwrote the first. Nothing upstream noticed,
    because nothing upstream looked at the blocks afterwards.

    Sampled by stride rather than exhaustively: a structure that was overwritten
    loses contiguous regions, not scattered blocks, so a deterministic stride
    finds it while reading a fraction of the volume.
    """
    world = World(world_path)
    results, problems = [], []
    for b in built:
        x, y, z = b['world_xyz']
        template = templates[b['structure']]()
        present = absent = 0
        first_missing = None
        for (dx, dy, dz), state in list(template.items())[::stride]:
            got = world.block(x + dx, y + dy, z + dz)
            want = str(state[0] if isinstance(state, tuple) else state)
            want = want.split('[')[0].replace("('", '').strip("',)")
            if got and got.split('[')[0] == want:
                present += 1
            else:
                absent += 1
                if first_missing is None:
                    first_missing = {'at': [x + dx, y + dy, z + dz],
                                     'expected': want, 'found': got}
        entry = {'structure': b['structure'], 'team': b.get('team'),
                 'sampled': present + absent, 'present': present, 'absent': absent}
        if first_missing:
            entry['first_missing'] = first_missing
            problems.append(f"{b.get('team')}/{b['structure']}: {absent} of "
                            f"{present + absent} sampled blocks are not in the world; "
                            f"first at {first_missing['at']} expected "
                            f"{first_missing['expected']}, found {first_missing['found']}")
        results.append(entry)
    return {'schema': 'authored_readback/1', 'evidence_state': 'RAW WORLD OBSERVATION',
            'world': str(world_path), 'structures': results,
            'problems': problems, 'verified': not problems}
