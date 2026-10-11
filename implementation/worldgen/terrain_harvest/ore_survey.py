"""Survey a world's ore as a player would meet it: how much, of what, and how findable.

maps.md leaves the resource-by-resource richness table [OPEN], so this tool
**measures and assigns nothing**. For every measurement cell it reports each
ore material split three ways:

  cave    touches air that lies beneath the surface (an explorer can see it)
  sky     touches air at or above the surface (an outcrop)
  buried  touches no air at all (found only by digging, or by cheating)

The split is the point. Vanilla ore is near-uniform in amount from world to
world, but only a few percent of it is exposed, so exposed ore and the cave
volume it sits in are the quantities that can differ between maps.

Everything is exact over the scanned volume; nothing is sampled. Known limits,
stated so a number is not over-read:

  * "beneath the surface" uses the chunk's own WORLD_SURFACE heightmap
    (MOTION_BLOCKING when that is absent). A chunk with neither is treated as
    having no surface, so its ore cannot read as sky-exposed.
  * A neighbour in a chunk that was never generated is unknown and does not
    count as air; `unknown_neighbours` reports how often that happened.
  * Cave volume counts only air in sections that lie wholly beneath the
    surface, so it under-reports caves that straddle it, and it does not test
    connectivity, so a sealed pocket counts like a cavern. caves.py measures
    the explorable (connected) volume on sampled cells; the two should not be
    mixed.
  * Fluids are neither air nor solid for exposure.
"""
from __future__ import annotations
import argparse
import json
import statistics
import sys
from collections import Counter, defaultdict
from pathlib import Path

from .materialize import json_write
from .opportunity_map import ORE, cell_of

MIN_Y = -64
AIR = {'minecraft:air', 'minecraft:cave_air', 'minecraft:void_air'}
CATEGORIES = ('cave', 'sky', 'buried')
NEIGHBOURS = ((1, 0, 0), (-1, 0, 0), (0, 1, 0), (0, -1, 0), (0, 0, 1), (0, 0, -1))
BAND = 32


def unpack(container):
    """The 4096 palette indices of a block-state container, in y-z-x order."""
    palette = container['palette']
    if len(palette) == 1 or 'data' not in container:
        return [0] * 4096
    bits = max(4, (len(palette) - 1).bit_length())
    per_long = 64 // bits
    mask = (1 << bits) - 1
    out = []
    for signed in container['data']:
        word = signed & ((1 << 64) - 1)
        for _ in range(per_long):
            out.append(word & mask)
            word >>= bits
            if len(out) == 4096:
                return out
    if len(out) < 4096:
        raise ValueError('truncated block-state section')
    return out


def surface_heights(data):
    """Top-block Y for each of a chunk's 256 columns, or None when it has no heightmap."""
    maps = data.get('Heightmaps') or {}
    for kind in ('WORLD_SURFACE', 'MOTION_BLOCKING'):
        values = maps.get(kind)
        if values is None:
            continue
        out = []
        for signed in values:
            word = signed & ((1 << 64) - 1)
            for _ in range(7):
                out.append(MIN_Y + (word & 511) - 1)
                word >>= 9
        return out[:256]
    return None


class Chunk:
    """One chunk, with sections decoded only when something asks for them."""

    def __init__(self, data):
        self.heights = surface_heights(data)
        self._raw = {}
        for s in data.get('sections', []):
            bs = s.get('block_states')
            if bs is not None:
                self._raw[s['Y']] = bs
        self._decoded = {}

    def section_ys(self):
        return self._raw.keys()

    def palette(self, sy):
        return [p['Name'] for p in self._raw[sy]['palette']]

    def indices(self, sy):
        got = self._decoded.get(sy)
        if got is None:
            got = self._decoded[sy] = unpack(self._raw[sy])
        return got

    def name_at(self, x, y, z):
        sy = y >> 4
        if sy not in self._raw:
            return 'minecraft:air'
        return self.palette(sy)[self.indices(sy)[(y & 15) * 256 + (z & 15) * 16 + (x & 15)]]

    def height_at(self, x, z):
        return None if self.heights is None else self.heights[(z & 15) * 16 + (x & 15)]

    def min_height(self):
        return None if self.heights is None else min(self.heights)


def classify(chunks, x, y, z):
    """cave / sky / buried for the ore block at (x, y, z), plus whether a neighbour was unknown."""
    unknown = False
    verdict = 'buried'
    for dx, dy, dz in NEIGHBOURS:
        nx, ny, nz = x + dx, y + dy, z + dz
        c = chunks.get((nx >> 4, nz >> 4))
        if c is None:
            unknown = True
            continue
        if c.name_at(nx, ny, nz) not in AIR:
            continue
        h = c.height_at(nx, nz)
        if h is None:
            continue
        if ny < h:
            return 'cave', unknown          # beneath the surface: the strongest reading
        verdict = 'sky'
    return verdict, unknown


def survey_chunks(chunks, cell_size=128, y_range=(-60, 90), origin=None):
    """Survey already-loaded chunks ({(cx, cz): Chunk}); the pure core of the tool."""
    lo, hi = y_range
    if origin is None:
        origin = (min(cx for cx, _ in chunks) * 16, min(cz for _, cz in chunks) * 16)
    cells = defaultdict(lambda: {
        'ore': defaultdict(lambda: dict.fromkeys(CATEGORIES, 0)),
        'cave_volume': 0, 'chunks': 0,
    })
    bands = defaultdict(lambda: defaultdict(lambda: dict.fromkeys(CATEGORIES, 0)))
    unknown = 0
    for (cx, cz), c in chunks.items():
        cell = cells[cell_of(cx * 16, cz * 16, cell_size, origin)]
        cell['chunks'] += 1
        floor = c.min_height()
        for sy in c.section_ys():
            if sy * 16 > hi or sy * 16 + 15 < lo:
                continue
            palette = c.palette(sy)
            ore_idx = {i: ORE[n] for i, n in enumerate(palette) if n in ORE}
            if floor is not None and sy * 16 + 15 < floor:
                air_idx = {i for i, n in enumerate(palette) if n in AIR}
                if air_idx:
                    cell['cave_volume'] += sum(1 for i in c.indices(sy) if i in air_idx)
            if not ore_idx:
                continue
            for n, i in enumerate(c.indices(sy)):
                material = ore_idx.get(i)
                if material is None:
                    continue
                y = sy * 16 + (n >> 8)
                if y < lo or y > hi:
                    continue
                x = cx * 16 + (n & 15)
                z = cz * 16 + ((n >> 4) & 15)
                verdict, unk = classify(chunks, x, y, z)
                unknown += unk
                cell['ore'][material][verdict] += 1
                bands[material][(y // BAND) * BAND][verdict] += 1
    return summarise(cells, bands, origin, cell_size, y_range, unknown)


def summarise(cells, bands, origin, cell_size, y_range, unknown):
    cell_rows = []
    world = defaultdict(lambda: dict.fromkeys(CATEGORIES, 0))
    cave_total = 0
    for (ix, iz), c in sorted(cells.items()):
        ore = {m: dict(v) for m, v in sorted(c['ore'].items())}
        for m, v in ore.items():
            for k in CATEGORIES:
                world[m][k] += v[k]
        cave_total += c['cave_volume']
        exposed = sum(v['cave'] for v in ore.values())
        total = sum(sum(v.values()) for v in ore.values())
        cell_rows.append({
            'cell': [ix, iz], 'chunks': c['chunks'], 'ore': ore,
            'ore_total': total, 'cave_exposed': exposed,
            'cave_volume': c['cave_volume'],
            'cave_exposed_per_1k_cave': round(1000 * exposed / c['cave_volume'], 2)
                                        if c['cave_volume'] else None,
        })
    total = sum(sum(v.values()) for v in world.values())
    exposed = sum(v['cave'] for v in world.values())
    sky = sum(v['sky'] for v in world.values())
    chunks = sum(r['chunks'] for r in cell_rows)
    full = [r for r in cell_rows if r['chunks'] >= (cell_size // 16) ** 2]
    spread = lambda key: ({'min': min(r[key] for r in full), 'median': statistics.median(r[key] for r in full),
                           'max': max(r[key] for r in full)} if full else None)
    return {
        'schema': 'ore_survey/1',
        'evidence_state': 'RAW WORLD OBSERVATION',
        'cell_size_blocks': cell_size, 'cell_origin': list(origin), 'y_range': list(y_range),
        'chunks': chunks,
        'totals': {
            'ore': total,
            'ore_per_chunk': round(total / chunks, 1) if chunks else None,
            'cave_exposed': exposed, 'sky_exposed': sky, 'buried': total - exposed - sky,
            'cave_exposed_fraction': round(exposed / total, 4) if total else None,
            'cave_volume': cave_total,
            'cave_exposed_per_1k_cave': round(1000 * exposed / cave_total, 2) if cave_total else None,
            'unknown_neighbours': unknown,
        },
        'by_material': {m: {**v, 'share': round(sum(v.values()) / total, 4) if total else None,
                            'cave_exposed_fraction': round(v['cave'] / sum(v.values()), 4)}
                        for m, v in sorted(world.items())},
        'by_depth_band': {m: {str(y): dict(v) for y, v in sorted(b.items())}
                          for m, b in sorted(bands.items())},
        'cell_spread': {'full_cells': len(full), 'ore_total': spread('ore_total'),
                        'cave_exposed': spread('cave_exposed')},
        'cells': cell_rows,
        'not_covered': [
            'connectivity: a sealed pocket counts as cave volume (caves.py measures connected volume)',
            'caves straddling the surface, which the section-wise cave volume excludes',
            'neighbours in ungenerated chunks (reported as unknown_neighbours)',
            'what any value SHOULD be; maps.md leaves the richness table OPEN',
        ],
    }


def load_world(world: Path):
    from serialization.nbt import plain
    from serialization.region import read_region
    chunks = {}
    for f in sorted((world / 'region').glob('r.*.mca')):
        for cx, cz, _, root in read_region(f):
            chunks[(cx, cz)] = Chunk(plain(root))
    return chunks


def survey_world(world: Path, cell_size=128, y_range=(-60, 90)):
    chunks = load_world(world)
    if not chunks:
        raise SystemExit(f'no chunks found under {world}/region')
    return survey_chunks(chunks, cell_size, y_range)


def render(report, name=''):
    t = report['totals']
    lines = [f"{name}  chunks {report['chunks']}  ore {t['ore']} ({t['ore_per_chunk']}/chunk)  "
             f"cave-exposed {t['cave_exposed']} ({100 * (t['cave_exposed_fraction'] or 0):.1f}%)  "
             f"sky {t['sky_exposed']}  buried {t['buried']}  "
             f"exposed/1k cave {t['cave_exposed_per_1k_cave']}"]
    for m, v in report['by_material'].items():
        lines.append(f"    {m:15} {100 * v['share']:5.1f}%  cave {v['cave']:6}  sky {v['sky']:6}  "
                     f"buried {v['buried']:7}  exposed {100 * v['cave_exposed_fraction']:4.1f}%")
    return '\n'.join(lines)


def main(argv=None):
    p = argparse.ArgumentParser(description=__doc__.split('\n')[0])
    p.add_argument('worlds', type=Path, nargs='+', help='world folders (each holds region/)')
    p.add_argument('--cell-size', type=int, default=128)
    p.add_argument('--y-min', type=int, default=-60)
    p.add_argument('--y-max', type=int, default=90)
    p.add_argument('--output', type=Path, help='write JSON here (one world), or a directory (several)')
    a = p.parse_args(argv)
    for w in a.worlds:
        report = survey_world(w, a.cell_size, (a.y_min, a.y_max))
        print(render(report, w.name))
        if a.output:
            out = a.output / f'{w.name}.json' if len(a.worlds) > 1 else a.output
            out.parent.mkdir(parents=True, exist_ok=True)
            json_write(out, report)
    return 0


if __name__ == '__main__':
    sys.exit(main())
