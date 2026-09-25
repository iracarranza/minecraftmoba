"""Read a `mobascan` grid: approximate surface height and biome off the seed.

This is the input `scoop.search` needs and the piece that was missing. Every
scoop measurement so far ran on `feature_grid.height_rle` from candidates that
had already been GENERATED, which meant the search could only ever re-read the
worlds the old pipeline had already chosen. A scan is a pure function of the
seed, so it reaches everything the pipeline never generated.

COST. An 8192-block square at 32-block sampling -- 65,536 cells -- takes about
one second. Generating the same area on a server is hours.

TRUSTED FOR. Biome exactly; height approximately. Measured against 61,479
samples over 69 generated worlds the height is unbiased (mean -0.27) but
heavy-tailed: 81% of cells within 5 blocks, 93% within 10, and outliers past
100 blocks where deep ocean diverges. That is good enough to rank symmetry and
to find flat ground, and NOT good enough to certify a build site -- which is
why `homebase_pair` returns places worth testing rather than sockets.

FAILS LOUDLY. `seed_screen` deliberately keeps every seed when its probe is
absent, because the screen is an optimisation and skipping it costs only
throughput. This is not that: a scoop search with no scan has nothing to
search, and silently returning no scoops would read as "this seed has no
symmetric region" when it means "nothing looked".
"""
from __future__ import annotations

import os
import shutil
import struct
import subprocess
import tempfile

SCAN_ENV = 'MOBA_CUBIOMES_SCAN'

# From the built library rather than transcribed: see native/cubiomes/README.
OCEAN_IDS = frozenset({0, 24, 10, 50, 44, 45, 46, 47, 48, 49})
RIVER_IDS = frozenset({7, 11})
SHORE_IDS = frozenset({16, 26, 25})

# Biome FAMILIES, for Map Types whose premise is a kind of country rather than
# a shape. Ids confirmed against the built library, not transcribed.
#
# These are what make six catalogue entries -- Arid, Frozen, Swamplands,
# Jungle, Badlands, Pale Forest -- recognisable with predicates instead of new
# measurement: family share is a summed-area quantity, so it costs four
# lookups per candidate at the screening tier where budget is allocated.
BIOME_FAMILIES = {
    'arid': frozenset({2, 37, 38, 165}),          # desert + badlands
    'badlands': frozenset({37, 38, 165}),
    'frozen': frozenset({12, 30, 179, 181, 140, 10, 11, 26, 50}),
    'swamp': frozenset({6, 184}),
    'jungle': frozenset({21, 168, 23}),
    'pale': frozenset({186}),                     # pale_garden
    'dark_forest': frozenset({29}),
}


def scanner_path() -> str | None:
    explicit = os.environ.get(SCAN_ENV)
    if explicit and os.path.exists(explicit):
        return explicit
    return shutil.which('mobascan')


class Scan:
    """A square of height and biome samples centred on the origin."""

    def __init__(self, seed, n, half, step, height, biome):
        self.seed, self.n, self.half, self.step = seed, n, half, step
        self.height, self.biome = height, biome

    @property
    def width(self):
        return self.n

    @property
    def depth(self):
        return self.n

    def block_at(self, i: int, j: int):
        return (-self.half + i * self.step, -self.half + j * self.step)

    def ocean_mask(self):
        return [b in OCEAN_IDS for b in self.biome]

    def feature_grid(self) -> dict:
        """The shape `scoop.describe` and `prominence.describe` already read."""
        runs = []
        for v in self.height:
            if runs and runs[-1][0] == v:
                runs[-1][1] += 1
            else:
                runs.append([v, 1])
        return {'width': self.n, 'height': self.n,
                'sample_spacing_blocks': self.step, 'height_rle': runs,
                'order': 'row-major, x fastest, z ascending'}


def read(path: str) -> Scan:
    with open(path, 'rb') as fh:
        header = fh.readline().decode('ascii').split()
        if len(header) != 6 or header[0] != 'mobascan':
            raise ValueError(f'not a mobascan file: {header[:2]}')
        version = int(header[1])
        if version != 2:
            raise ValueError(
                f'mobascan v{version}; this reads v2. v1 carried one ocean/land '
                f'byte per cell and no height, which cannot feed a symmetry '
                f'search. Rebuild native/cubiomes/mobascan.c.')
        seed, n, half, step = (int(h) for h in header[2:])
        body = fh.read(n * n * 4)
    if len(body) != n * n * 4:
        raise ValueError(f'truncated scan: {len(body)} bytes for {n}x{n}')
    flat = struct.unpack(f'<{n * n * 2}h', body)
    return Scan(seed, n, half, step, list(flat[0::2]), list(flat[1::2]))


def scan(seed: int, *, half: int = 4096, step: int = 32,
         scanner: str | None = None, timeout: int = 600) -> Scan:
    scanner = scanner or scanner_path()
    if not scanner:
        raise RuntimeError(
            f'no mobascan on $PATH or ${SCAN_ENV}. Build it: see '
            f'native/cubiomes/README.md. This is not optional the way the '
            f'seed screen is -- without a scan there is nothing to search.')
    with tempfile.NamedTemporaryFile(suffix='.bin', delete=False) as tmp:
        out = tmp.name
    try:
        subprocess.run([scanner, str(seed), str(half), str(step), out],
                       check=True, capture_output=True, timeout=timeout)
        return read(out)
    finally:
        os.unlink(out)
