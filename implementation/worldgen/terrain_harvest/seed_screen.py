"""Reject seeds before a world is generated, using worldgen computed off-server.

The regional-shape gate rejects about 82% of random seeds, and until now every
rejection paid a full world generation first -- 65 to 170 seconds each. The
obvious fix, generating a sparse lattice and screening that, was measured on
23 September and is a NET LOSS: a chunk cannot reach FULL until its neighbours
exist, so 221 scattered points cost about half a full window, and at the 30%
discard rate that is ~19% more expensive per seed, not less. See
`cheap_screen`, which records the measurement and stays unwired.

This screens without generating anything. `cubiomes` reimplements Minecraft's
biome generation as a pure function of the seed, so ocean and land can be
classified over the candidate window in a fraction of a second:

    mobaprobe <seed> (14,256 cells)     0.23 s
    full world generation               65-170 s

WHAT IT CAN AND CANNOT DECIDE. Of the six regional-shape checks, only the two
ocean checks are pure biome facts. `region_not_mostly_water` counts real water
blocks including lakes, which are placed features; `homelands_reasonably_viable`
reads block-level buildability; and the two elevation checks depend on a surface
height that cubiomes only approximates. Measured against 61,479 samples over 69
generated worlds, that approximation is unbiased (mean -0.27) but heavy-tailed:
81% of cells within 5 blocks, 93% within 10, and outliers past 100 where deep
ocean diverges. **That is why elevation is not screened here.** A per-cell
threshold like `highland_depth_blocks` would inherit those tails, and a false
negative is the expensive error.

So this does not replace `recognize`. Every surviving seed still generates and
still faces the full gate. This only discards seeds that fail on facts that can
be known exactly.

VALIDATION. Against the 69 seeds compiled on 23 September, of which 13 passed
the real gate:

    margin   discard   false negatives
    0.00       26%           0
    0.02       25%           0
    0.05       20%           0
    0.08       16%           0

Zero at every margin, including none at all. 0.05 is chosen anyway: only 13 of
the 69 were positives, so the false-negative evidence rests on a small set, and
four points of discard is a cheap price for headroom against the seeds that set
has not seen.

ALL EIGHT ORIENTATIONS. The real screen evaluates four rotations by reflection
and takes the best, so this must too. Judging one fixed frame is precisely how
the first version of `cheap_screen` discarded 99887766 -- which went on to
become one of only two maps that ever compiled.

FAILS OPEN. If the probe is missing or errors, the seed is KEPT. A screen that
cannot measure must not discard; the cost of being wrong that way is one wasted
generation, and the cost the other way is a good seed lost silently.
"""
from __future__ import annotations

import json
import os
import shutil
import subprocess

# Ocean biome ids in cubiomes' numbering.
OCEANS = frozenset({0, 10, 24, 44, 45, 46, 47, 48, 49, 50})

# The candidate window's own sampling, coarsened. The real evaluator reads every
# 8 blocks; 16 is enough to classify ocean against land and halves the work.
STEP = 16

# Thresholds the real screen uses, and the slack this one allows itself.
EAST_OCEAN_MIN = 0.10
OCEAN_ADVANTAGE_MIN = 0.10
MARGIN = 0.05

PROBE_ENV = 'MOBA_CUBIOMES_PROBE'


def probe_path() -> str | None:
    """The mobaprobe binary, or None when it has not been built."""
    explicit = os.environ.get(PROBE_ENV)
    if explicit and os.path.exists(explicit):
        return explicit
    return shutil.which('mobaprobe')


def _orient(rows, rotation, reflected):
    out = rows
    for _ in range(rotation // 90):
        out = [list(col) for col in zip(*out[::-1])]
    if reflected:
        out = [row[::-1] for row in out]
    return out


def _ocean_fractions(biomes):
    width = len(biomes[0])
    third = max(1, width // 3)
    west = [b for row in biomes for b in row[:third]]
    east = [b for row in biomes for b in row[-third:]]
    return (sum(1 for b in east if b in OCEANS) / max(1, len(east)),
            sum(1 for b in west if b in OCEANS) / max(1, len(west)))


def best_orientation(biomes):
    """The best eastern-ocean fraction and advantage over all eight orientations."""
    best = (-1.0, -1.0)
    for rotation in (0, 90, 180, 270):
        for reflected in (False, True):
            east, west = _ocean_fractions(_orient(biomes, rotation, reflected))
            if (east, east - west) > best:
                best = (east, east - west)
    return best


def sample(seed: int, probe: str, step: int = STEP) -> list[list[int]]:
    """Biome ids over the candidate window, as a grid. No world is generated."""
    raw = subprocess.run([probe, str(seed), str(step)],
                         capture_output=True, text=True, timeout=120, check=True)
    doc = json.loads(raw.stdout)
    xs = sorted({c[0] for c in doc['cells']})
    zs = sorted({c[1] for c in doc['cells']})
    xi = {v: i for i, v in enumerate(xs)}
    zi = {v: i for i, v in enumerate(zs)}
    grid = [[0] * len(xs) for _ in zs]
    for x, z, _y, biome in doc['cells']:
        grid[zi[z]][xi[x]] = biome
    return grid


def screen(seed: int, *, margin: float = MARGIN, step: int = STEP) -> dict:
    """Keep or discard a seed. Discards only on facts known exactly."""
    probe = probe_path()
    if not probe:
        return {'keep': True, 'screened': False,
                'why': f'no cubiomes probe on PATH or ${PROBE_ENV}; '
                       f'a screen that cannot measure must not discard'}
    try:
        biomes = sample(seed, probe, step)
    except Exception as failure:                      # noqa: BLE001 - reported, not swallowed
        return {'keep': True, 'screened': False,
                'why': f'probe failed ({type(failure).__name__}); seed kept'}

    east, advantage = best_orientation(biomes)
    fails_east = east < EAST_OCEAN_MIN - margin
    fails_advantage = advantage < OCEAN_ADVANTAGE_MIN - margin
    # BOTH must miss. Either alone is within the noise of a coarse sample, and
    # the whole point is to discard only what cannot pass.
    discard = fails_east and fails_advantage
    return {
        'keep': not discard,
        'screened': True,
        'east_ocean_fraction': round(east, 4),
        'ocean_advantage': round(advantage, 4),
        'margin': margin,
        'why': ('no orientation gives this seed an eastern ocean or an ocean '
                'gradient, so it cannot satisfy Default regional shape'
                if discard else 'may satisfy the ocean checks; generate it'),
        'proves': 'only the two ocean checks. Elevation, surface water and '
                  'buildability still need a generated world.',
    }
