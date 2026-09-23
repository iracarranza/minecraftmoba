"""Measure what a compiled realization actually contains.

Four measurement modules in this repository are built, proven against the Alpha
map, and **unreachable from a compiled map**: `opportunity_map` counts ore by
material from real region files, `caves` measures cave volume, depth and the
exposed-ore fraction that decides whether the underground rewards exploring or
rewards x-ray, `rescan` reports per-team reach in seconds off an authored world,
and `routes` already walks the terrain graph out of each homeland.

None of them was ever wired to the compiler, because the compiler grew
separately from them. That is the finding this module acts on: the work is
wiring measurement that exists, not building measurement.

This is the first half -- the underground and the surface opportunity. It runs
the two block-level scanners over the compiled window and puts what they say
into the compilation's evidence.

IT MEASURES AND DOES NOT GATE. Nothing here rejects a map. maps.md marks the
depth gradient, regional tables and resource vocabulary [OPEN], and the repo's
habit is to record a distribution before anyone chooses a bound. A stage that
invented a threshold to look decisive would be the opposite of useful.

WHY IT MUST RUN ON THE COMPILED WINDOW. Ore, caves, exposure and accessibility
are carver and feature output -- the residue of a simulation, not a function of
the seed. No off-server model reaches them at any price, which is exactly where
the cubiomes layer stops. A searched window offset moves the map, so it moves
the underground with it: a window chosen for its coastline says nothing about
whether its caves are worth entering.
"""
from __future__ import annotations

from pathlib import Path

from .model import make_volume

# The measurement grid. Coarser than a chunk, fine enough that a Homebase
# envelope spans several cells. NON-CANON: an analysis unit, not a design
# quantity, and nothing downstream may read it as a region boundary.
CELL_SIZE = 64

# Full column. Ore is depth-distributed and the deep band is the whole point.
Y_RANGE = (-64, 320)


def window_volume(seed: int, block_bounds, y_range=Y_RANGE):
    """A terrain_volume describing the compiled window, for the scanners."""
    x0, x1, z0, z1 = block_bounds
    return make_volume(
        {'source_seed': int(seed),
         'source_dimension': 'minecraft:overworld',
         'source_bounds': {'x': [x0, x1], 'y': list(y_range), 'z': [z0, z1]},
         'minecraft_version': 'compiled-window',
         'worldgen_settings': {'generator': 'default'},
         'source_analysis_record': 'terrain_harvest.characterize'},
        # 'whole_map' is the existing vocabulary for a complete realization.
        # Deliberately not a new kind: the volume schema already classifies
        # these and adding a synonym would fork it.
        kind='whole_map', geometry='box')


def characterize(seed: int, world: Path, block_bounds, *,
                 cell_size: int = CELL_SIZE, y_range=Y_RANGE) -> dict:
    """Ore, vegetation, fauna and cave opportunity over the compiled window."""
    from . import opportunity_map
    # A world that is not there is not a map with no opportunity. Scanning an
    # absent directory returns zero cells, which would read as "measured, and
    # this realization contains nothing" -- the same shape of lie as the build
    # world that was never copied.
    if not Path(world).is_dir() or not (Path(world) / 'region').is_dir():
        raise FileNotFoundError(f'no world to characterize at {world}')
    volume = window_volume(seed, block_bounds, y_range)
    cells, consumed, origin = opportunity_map.scan(volume, Path(world), cell_size, y_range)
    summary = opportunity_map.summarise(cells, origin, cell_size)
    return {
        'evidence_state': 'RAW WORLD OBSERVATION',
        'cell_size_blocks': cell_size,
        'y_range': list(y_range),
        'window_block_bounds': list(block_bounds),
        'opportunity': summary,
        'source_files': len(consumed),
        'gates': 'none. This stage measures and does not reject. Depth gradient, '
                 'regional tables and resource vocabulary are OPEN in maps.md, and '
                 'a threshold invented here would be worse than the gap.',
        'not_covered': [
            'Strategic Depth and Regional Character, which need the cell grid',
            'whether a cave component reaches open sky, which needs a flood fill',
            'traversable distance to a cave mouth; the scan measures volume, not paths',
            'natural resource placement validity, which needs the opening rules',
        ],
    }
