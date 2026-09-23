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

WHY THIS IS STILL NOT WIRED INTO THE FOUNDRY. The saving has now been measured,
and it is NEGATIVE.

Note first what this module does NOT do: `sample` reads `region/*.mca` from a
world that has ALREADY been generated. The 0-false-negative result proves the
screen's JUDGEMENT, not any saving -- it was validated by replaying finished
worlds. To save generation the lattice has to be generated on its own first, and
that is what costs.

Measured 23 September 2026, generating a stride-4 lattice alone against the
full 4320-chunk window, same seeds:

    seed          lattice ms    full ms    ratio
    1041727550         84820     101266     0.84
    1248940432         65373     173695     0.38
    741772126          58702     152698     0.38
                                     mean    0.49

A lattice of 221 points -- 5% of the chunks -- costs about half a full
generation, because a chunk cannot reach FULL until its neighbours exist and 221
scattered points drag halos that between them touch most of the window anyway.

At the measured 30% discard rate:

    without screening   1.00x per seed
    with screening      1.19x per seed   (0.30 x 0.49 + 0.70 x 1.49)

so screening costs about 19% MORE. **Break-even needs a 49% discard rate.**

Three seeds is a small sample and the ratios vary widely (0.38 to 0.84), so
treat 19% as the sign of the result rather than its magnitude. The sign is not
in doubt: the screen discards 30% and would need to discard half.

That points somewhere useful rather than closing the question. The screen is
deliberately permissive -- MIN_GRADIENT_Y, MIN_OCEAN_AT_LOW_END and
MAX_WATER_FRACTION below are set to reject only what is obviously not Default
geography. A sharper screen reaching 49% discard WITHOUT acquiring a false
negative would pay for itself. Whether that is possible is unmeasured; what is
settled is that the current one does not.

The larger lesson is that lattice generation is the wrong primitive. Throughput
wants worldgen evaluated without a server at all.
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
