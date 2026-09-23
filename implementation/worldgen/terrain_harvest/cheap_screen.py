"""A coarse pre-generation screen for Default regional shape -- and its limits.

Eighty per cent of the foundry's expensive work is thrown away at the regional
shape gate: 24 of 30 unseen seeds were rejected at `recognize`, after a full
4320-chunk world had already been generated for each. That is the obvious place
to look for machine time.

This computes the same broad facts -- an elevation gradient, ocean at the low
end, overall water -- from a SPARSE chunk lattice instead of a full window, so a
seed could be rejected before the expensive generation.

Measured against the real screen over all 30 unseen seeds, using a stride-4
lattice (about 286 chunks of 4320):

    false negatives (good seeds discarded)   0
    seeds correctly discarded               9 / 30   (30%)
    seeds kept that later failed           15 / 30

Zero false negatives matters more than the discard rate: losing a seed that
would have compiled is the expensive error, and the first version of this had
one. It assumed the highland end was low-x, while the real screen tries all
eight orientations before judging -- and it discarded 99887766, which went on to
become one of the two maps that compiled. Hence `best_gradient`, which takes the
best of the four axis directions.

WHY THIS IS NOT WIRED INTO THE FOUNDRY YET. The 30% discard rate is measured;
the saving is not. The screen's own cost -- generating a scattered lattice,
which pulls in neighbour work and is much less efficient per chunk than a
contiguous window -- has not been measured, and at 30% it could plausibly eat
most of the benefit. Claiming a speedup here without measuring the screen's cost
would be exactly the kind of number this project keeps having to retract.
"""
from __future__ import annotations

from pathlib import Path

from serialization.nbt import plain
from serialization.region import read_region
from vanilla_search.extract import VanillaChunk

# PROVISIONAL_ALPHA, and deliberately permissive. A pre-screen should reject
# only what is obviously not Default geography; anything sharper trades false
# negatives for a discard rate, and a lost good seed costs more than a wasted
# generation.
MIN_GRADIENT_Y = 2.0
MIN_OCEAN_AT_LOW_END = 0.03
MAX_WATER_FRACTION = 0.75

STRIDE = 4


def sample(world: Path, stride: int = STRIDE) -> list:
    """Surface points from a sparse chunk lattice."""
    points = []
    for f in sorted((Path(world) / 'region').glob('*.mca')):
        for cx, cz, _, root in read_region(f):
            if cx % stride or cz % stride:
                continue
            data = plain(root)
            if data.get('Status') != 'minecraft:full':
                continue
            chunk = VanillaChunk(data)
            for lx in (4, 12):
                for lz in (4, 12):
                    x, z = cx * 16 + lx, cz * 16 + lz
                    y = chunk.height('MOTION_BLOCKING_NO_LEAVES', x, z)
                    wet = ('water' in chunk.block(x, y, z)
                           or 'water' in chunk.block(x, y - 1, z))
                    points.append((x, z, y, wet))
    return points


def best_gradient(points) -> tuple[float, float]:
    """The strongest highland-to-ocean gradient over the four axis directions.

    Orientation-agnostic on purpose. The real screen evaluates four rotations
    and two reflections and keeps the best; assuming one orientation here
    discarded a seed that later compiled.
    """
    best = (-999.0, 0.0)
    for axis, sign in ((0, 1), (0, -1), (1, 1), (1, -1)):
        values = sorted(p[axis] * sign for p in points)
        lo, hi = values[0], values[-1]
        third = (hi - lo) / 3
        high = [p for p in points if p[axis] * sign <= lo + third]
        low = [p for p in points if p[axis] * sign >= hi - third]
        if not high or not low:
            continue
        gradient = sum(p[2] for p in high) / len(high) - sum(p[2] for p in low) / len(low)
        ocean = sum(p[3] for p in low) / len(low)
        if gradient > best[0]:
            best = (gradient, ocean)
    return best


def screen(world: Path, stride: int = STRIDE) -> dict:
    """Could this region plausibly be Default? Permissive by design."""
    points = sample(world, stride)
    if not points:
        return {'plausible': False, 'why': ['no generated chunks in the lattice']}
    gradient, ocean = best_gradient(points)
    water = sum(p[3] for p in points) / len(points)
    why = []
    if gradient < MIN_GRADIENT_Y:
        why.append(f'no highland-to-ocean gradient in any direction '
                   f'({gradient:.1f}Y < {MIN_GRADIENT_Y})')
    if ocean < MIN_OCEAN_AT_LOW_END:
        why.append(f'no ocean at the low end ({ocean:.2f} < {MIN_OCEAN_AT_LOW_END})')
    if water > MAX_WATER_FRACTION:
        why.append(f'mostly water ({water:.2f} > {MAX_WATER_FRACTION})')
    return {
        'plausible': not why,
        'why': why,
        'best_gradient_y': round(gradient, 2),
        'ocean_at_low_end': round(ocean, 4),
        'water_fraction': round(water, 4),
        'sampled_points': len(points),
        'status': 'PROVISIONAL_ALPHA; 0 false negatives over 30 unseen seeds, '
                  '30% correctly discarded. NOT wired into the foundry: the '
                  'discard rate is measured and the net saving is not.',
    }
