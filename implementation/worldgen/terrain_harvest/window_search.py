"""Find WHERE on a seed the Default window should sit, instead of judging 0,0.

The compiler's window is fixed at chunks -27..26 by -33..32 around the origin.
That makes a seed's fate depend entirely on what happens to sit at spawn: a seed
whose regional shape is ideal three thousand blocks away is rejected for being
in the wrong place. Across 40 unseen seeds, 33 were rejected on regional shape;
an unknown fraction of those are seeds with good geography somewhere else.

Biome generation is a pure function of the seed, so the right place can be
SEARCHED FOR rather than hoped for. This scans a large area off-server, slides
the window over it, and returns the offsets whose biome pattern best satisfies
the two ocean checks.

WHAT MAKES THIS SOUND. The pipeline was already offset-capable and nobody had
noticed: `harvest` takes `chunk_bounds` as a parameter, `extract_region` takes
arbitrary bounds, and a candidate records absolute `block_bounds`. Nothing
downstream of harvesting assumes the origin. The only hardcoded window is the
generation plugin's chunk rectangle and the default in `vanilla_search.pipeline`.

WHAT THIS DOES NOT DECIDE. The same four checks that the seed screen cannot
evaluate -- surface water, homeland developability, and the two elevation
metrics -- still need a generated world. A high-scoring offset is a PLACE WORTH
GENERATING, not a map. Ranking offsets by the two ocean checks alone would be a
mistake if it were read as ranking maps.

ALL EIGHT ORIENTATIONS. The real evaluator tries four rotations by reflection.
For the ocean checks that reduces to: split the window on either axis, and allow
either side to be the "east". Both are evaluated, so a window is never rejected
for having its ocean on the wrong side.
"""
from __future__ import annotations

import os
import subprocess
import tempfile

# The compiler's window, in chunks, and the block span it implies.
WINDOW_CHUNKS = (-27, 26, -33, 32)
WINDOW_W = (WINDOW_CHUNKS[1] - WINDOW_CHUNKS[0] + 1) * 16      # 864
WINDOW_H = (WINDOW_CHUNKS[3] - WINDOW_CHUNKS[2] + 1) * 16      # 1056

EAST_OCEAN_MIN = 0.10
OCEAN_ADVANTAGE_MIN = 0.10

# A window must also be mostly LAND.
#
# The first version of this scored `east_ocean + advantage` and maximised it,
# which drove every result to an eastern third that was 100% ocean -- the
# highest-scoring offsets on three test seeds were all 1.0/1.0. Those are
# windows drowning in sea, and they would have sailed through the two checks
# this searches on and then failed `region_not_mostly_water` after a full
# generation. Ranking by "more ocean is better" was a scoring bug, not a
# preference.
#
# The real gate bounds actual surface water at 0.75, and actual water is at
# least ocean water (it also counts rivers and lakes), so a bound on scanned
# ocean is a conservative proxy. NON-CANON FIXTURE.
MAX_WINDOW_OCEAN = 0.55

# Where the ocean checks are comfortably satisfied without the window drowning.
# Scoring TOWARD this rather than maximising is the whole correction.
TARGET_EAST_OCEAN = 0.30

SCAN_ENV = 'MOBA_CUBIOMES_SCAN'


def scan_path() -> str | None:
    explicit = os.environ.get(SCAN_ENV)
    if explicit and os.path.exists(explicit):
        return explicit
    import shutil
    return shutil.which('mobascan')


def scan(seed: int, half: int = 4096, step: int = 32, scanner: str | None = None):
    """An ocean/land bitmap over a square of side 2*half, centred on the origin."""
    scanner = scanner or scan_path()
    if not scanner:
        raise RuntimeError(f'no mobascan on PATH or ${SCAN_ENV}')
    with tempfile.NamedTemporaryFile(suffix='.bin', delete=False) as tmp:
        out = tmp.name
    try:
        subprocess.run([scanner, str(seed), str(half), str(step), out, 'x'],
                       check=True, capture_output=True, timeout=300)
        with open(out, 'rb') as fh:
            header = fh.readline().decode().split()
            n = int(header[3])
            body = fh.read(n * n)
        grid = [[body[j * n + i] for i in range(n)] for j in range(n)]
        return grid, half, step
    finally:
        os.unlink(out)


def _integral(grid):
    """Summed-area table, so any rectangle's ocean count is four lookups."""
    n = len(grid)
    table = [[0] * (n + 1) for _ in range(n + 1)]
    for j in range(n):
        rowsum = 0
        for i in range(n):
            rowsum += grid[j][i]
            table[j + 1][i + 1] = table[j][i + 1] + rowsum
    return table


def _rect(table, i0, j0, i1, j1):
    """Ocean count in cells [i0,i1) x [j0,j1)."""
    return (table[j1][i1] - table[j0][i1] - table[j1][i0] + table[j0][i0])


def describe(seed: int, *, half: int = 4096, step: int = 32, stride_cells: int = 8,
             scanner: str | None = None) -> dict:
    """Every window's cheap feature vector, ranked by nothing.

    The describe-then-label half of `search`. `search` returns the offsets that
    pass Default's ocean checks; this returns **every** window with what it
    contains, including the ones that fail, because a window that is a poor
    Valley may be a good Chasm and today it is simply deleted.

    Cheap tier only: these are biome facts, so they are exactly computable off
    the seed. Ocean distribution is reported on both axes and in both
    directions, deliberately: a vocabulary naming only Default's features
    would only ever recognise Default, whatever order it is evaluated in.

    NOT LAND-BODY COUNT, which this docstring used to claim. Only fractions
    are computed here, and a fraction cannot separate one island from twenty
    -- see `scoop.water_structure`, which does the component analysis this
    claimed to be doing.
    """
    grid, half, step = scan(seed, half, step, scanner)
    n = len(grid)
    table = _integral(grid)
    wc, hc = WINDOW_W // step, WINDOW_H // step
    if wc >= n or hc >= n:
        return {'seed': seed, 'windows': [], 'why': 'scan area smaller than the window'}

    third_w, third_h = max(1, wc // 3), max(1, hc // 3)
    windows = []
    for j in range(0, n - hc, stride_cells):
        for i in range(0, n - wc, stride_cells):
            tw, th = third_w * hc, third_h * wc
            low_x = _rect(table, i, j, i + third_w, j + hc) / tw
            high_x = _rect(table, i + wc - third_w, j, i + wc, j + hc) / tw
            low_z = _rect(table, i, j, i + wc, j + third_h) / th
            high_z = _rect(table, i, j + hc - third_h, i + wc, j + hc) / th
            whole = _rect(table, i, j, i + wc, j + hc) / (wc * hc)
            best = max((high_x, high_x - low_x), (low_x, low_x - high_x),
                       (high_z, high_z - low_z), (low_z, low_z - high_z))
            windows.append({
                'centre': [-half + i * step + WINDOW_W // 2,
                           -half + j * step + WINDOW_H // 2],
                # type-neutral
                'ocean_fraction': round(whole, 4),
                'land_fraction': round(1 - whole, 4),
                # axis distribution, both axes, no privileged direction
                'ocean_x_low': round(low_x, 4), 'ocean_x_high': round(high_x, 4),
                'ocean_z_low': round(low_z, 4), 'ocean_z_high': round(high_z, 4),
                # Default's own reading of the same numbers
                'best_edge_ocean': round(best[0], 4),
                'best_gradient': round(best[1], 4),
            })
    return {
        'seed': seed,
        'scanned_half': half,
        'step': step,
        'stride_cells': stride_cells,
        'windows': windows,
        'proves': 'biome distribution only. Elevation, buildability, ore, caves and '
                  'water all need the window generated.',
        'ranks': 'nothing. This is the description layer; templates are not fitted '
                 'here and no window is rejected.',
    }


def search(seed: int, *, half: int = 4096, step: int = 32, stride_cells: int = 4,
           scanner: str | None = None, keep: int = 12) -> dict:
    """Rank window offsets by how well their biomes satisfy the ocean checks."""
    grid, half, step = scan(seed, half, step, scanner)
    n = len(grid)
    table = _integral(grid)
    wc, hc = WINDOW_W // step, WINDOW_H // step          # window in scan cells
    if wc >= n or hc >= n:
        return {'seed': seed, 'offsets': [], 'why': 'scan area smaller than the window'}

    third_w, third_h = max(1, wc // 3), max(1, hc // 3)
    found = []
    for j in range(0, n - hc, stride_cells):
        for i in range(0, n - wc, stride_cells):
            total_w = third_w * hc
            total_h = third_h * wc
            # x-axis split
            low_x = _rect(table, i, j, i + third_w, j + hc) / total_w
            high_x = _rect(table, i + wc - third_w, j, i + wc, j + hc) / total_w
            # z-axis split
            low_z = _rect(table, i, j, i + wc, j + third_h) / total_h
            high_z = _rect(table, i, j + hc - third_h, i + wc, j + hc) / total_h
            # Either axis may be regional, and either side may be "east".
            best = max((high_x, high_x - low_x), (low_x, low_x - high_x),
                       (high_z, high_z - low_z), (low_z, low_z - high_z))
            east, advantage = best
            if east < EAST_OCEAN_MIN or advantage < OCEAN_ADVANTAGE_MIN:
                continue
            whole = _rect(table, i, j, i + wc, j + hc) / (wc * hc)
            if whole > MAX_WINDOW_OCEAN:
                continue          # a window drowning in sea, not a coastline
            bx = -half + i * step
            bz = -half + j * step
            found.append({
                'block_x': bx + WINDOW_W // 2,      # window CENTRE
                'block_z': bz + WINDOW_H // 2,
                'east_ocean_fraction': round(east, 4),
                'ocean_advantage': round(advantage, 4),
                'window_ocean_fraction': round(whole, 4),
                'off_target': round(abs(east - TARGET_EAST_OCEAN), 4),
            })
    # Rank by closeness to a coastline first, gradient strength second.
    #
    # The first two attempts at this were both wrong in instructive ways. Adding
    # east + advantage maximised sea and returned windows that were 100% ocean
    # on one side. Replacing it with `advantage - |east - target|` looked
    # principled and was DEGENERATE: whenever the western third holds no ocean,
    # advantage equals east, and the expression collapses to a constant 0.30 for
    # every window above target. Every offset tied, the sort became arbitrary,
    # and two different seeds returned the identical best coordinate -- which is
    # the only reason the bug was caught.
    #
    # A tuple sort cannot cancel. Closest to a coastline wins; among equals, the
    # strongest land-to-sea gradient.
    found.sort(key=lambda o: (o['off_target'], -o['ocean_advantage']))
    # Offsets overlapping heavily are the same place; keep them spread out.
    spread, taken = [], []
    for o in found:
        if all(abs(o['block_x'] - t['block_x']) > WINDOW_W // 2
               or abs(o['block_z'] - t['block_z']) > WINDOW_H // 2 for t in taken):
            spread.append(o); taken.append(o)
        if len(spread) >= keep:
            break
    return {
        'seed': seed,
        'scanned_half': half,
        'step': step,
        'candidate_offsets': len(found),
        'offsets': spread,
        'proves': 'biome ocean pattern only. Elevation, surface water and '
                  'buildability still need the window generated.',
    }


def chunk_bounds_for(centre_x: int, centre_z: int):
    """The chunk rectangle to generate for a window centred here."""
    cx, cz = centre_x // 16, centre_z // 16
    return (cx + WINDOW_CHUNKS[0], cx + WINDOW_CHUNKS[1],
            cz + WINDOW_CHUNKS[2], cz + WINDOW_CHUNKS[3])
