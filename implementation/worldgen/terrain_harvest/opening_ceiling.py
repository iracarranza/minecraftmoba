"""The Opening Hinterland ceiling: opportunities the opening must not contain.

maps.md has said since 22 September that the Opening Hinterland "should exclude
opportunities that skip meaningful early progression", naming villages, carrots
and enough accessible iron to equip a player, and that "serious ceiling
violations should reject a socket". Nothing implemented any of it.

The first live playtest found the consequence: a savanna village 68 blocks from
the south Fountain on a map that passed physical verification and certified
READY, with the nearest village to the north Fountain 728 blocks away. So the
rule was violated, and violated ASYMMETRICALLY -- one team opens beside a
village and the other does not. Neither half was noticed, because nothing was
looking.

What this module does and does not settle:

  IMPLEMENTED  villages, via the structure recogniser that already exists in
               `respect`. A village is the one exclusion that is both named in
               canon and recognisable from blocks alone.

  NOT SETTLED  carrots, and "enough accessible iron to equip a player". The
               iron rule needs a declared equipment target, which does not
               exist; maps.md says so and says the measurement stays UNMEASURED
               rather than passing by default. Reporting them as unchecked is
               the honest state. A check that silently passed them would be
               worse than none, because it would look like coverage.
"""
from __future__ import annotations

from pathlib import Path

from .respect import is_structure

# How far out from a Fountain the opening envelope reaches, in blocks.
#
# NON-CANON PROVISIONAL. Doctrine says the Opening Hinterland is "compact" and
# gives no number, and this is not the place to invent one -- so the radius is a
# parameter with a declared default rather than a constant pretending to be
# canon.
#
# 96 is chosen to be larger than the authored footprints (the Bastion's is 33)
# and large enough to contain the 68-block village that motivated the check,
# without reaching the midline on any compiled map so far. It is a starting
# point for measurement, not a finding. Raising it makes the compiler stricter
# and will reject more maps; that trade has not been measured either.
DEFAULT_RADIUS = 96

# Sampled, not exhaustive: a village is tens of blocks across, so a stride well
# below its size cannot miss one, and scanning every column in a 193-block
# square would load the whole opening for every candidate.
STRIDE = 8

UNCHECKED = ('carrots', 'accessible_iron')


def survey(world, cx: int, cz: int, *, radius: int = DEFAULT_RADIUS, stride: int = STRIDE):
    """Count standing built blocks in the opening envelope around one Fountain.

    Reads only. `world` is a `column_scan.World`.
    """
    found = []
    for dx in range(-radius, radius + 1, stride):
        for dz in range(-radius, radius + 1, stride):
            if dx * dx + dz * dz > radius * radius:
                continue
            x, z = cx + dx, cz + dz
            surface = world.surface(x, z)
            if surface is None:
                continue
            # A building stands above the ground it sits on; a couple of blocks
            # of headroom is enough to catch a wall or a roof without walking
            # the whole column.
            for y in range(surface - 1, surface + 6):
                block = world.block(x, y, z)
                if block and is_structure(block):
                    found.append({'block': block, 'at': [x, y, z]})
                    break
    return found


def certify(world_path, fountains, *, radius: int = DEFAULT_RADIUS, stride: int = STRIDE):
    """Check both teams' opening envelopes against the ceiling.

    `fountains` maps team to [x, y, z] world coordinates.

    Reports each team separately rather than as one verdict, because the
    asymmetry is the finding: a village beside one Fountain and none beside the
    other is a worse outcome than a village beside both, and a single boolean
    would hide the difference.
    """
    from .column_scan import World
    world = World(Path(world_path))
    per_team, problems = {}, []
    for team, xyz in fountains.items():
        hits = survey(world, xyz[0], xyz[2], radius=radius, stride=stride)
        per_team[team] = {'built_block_samples': len(hits), 'examples': hits[:5]}
        if hits:
            problems.append({
                'code': 'OPENING_CEILING_VIOLATED',
                'team': team,
                'detail': f'{len(hits)} sampled columns within {radius} blocks of the '
                          f'{team} Fountain stand on built structure; maps.md excludes '
                          f'villages from the Opening Hinterland, and a serious ceiling '
                          f'violation should reject a socket',
                'example': hits[0],
            })
    return {
        'certified': not problems,
        'problems': problems,
        'per_team': per_team,
        'radius': radius,
        'stride': stride,
        'unchecked': list(UNCHECKED),
        'note': 'Villages only. The carrot and accessible-iron exclusions remain '
                'UNMEASURED -- the iron rule needs a declared equipment target that '
                'does not exist, and a check that silently passed them would look '
                'like coverage it does not have.',
    }
