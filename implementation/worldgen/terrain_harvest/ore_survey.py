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

Three further measures describe what mining that ore is like, because totals
alone cannot tell "five thousand single blocks" from "forty big veins":

  veins      same-material ore blocks joined through any of their 26 face, edge
             or corner neighbours (vanilla veins are blobs, so face-only would
             shatter them). Size distribution per material.
  spacing    distance from each vein's centroid to the nearest other vein of the
             same material, within the same 32-block depth band, because ore
             generates at different heights and a pooled figure would mislead.
  distance   how many blocks of rock separate an ore block from the nearest
             cave air, walking through non-air blocks (a digging distance, 1 for
             ore touching a cave). SAMPLED: a deterministic stride over each
             material's ore, capped, because exact distances over a whole world
             are not affordable in Python.

Everything else is exact over the scanned volume. Known limits, stated so a
number is not over-read:

  * Veins are clipped by the Y range and by the edge of the generated area, so
    a vein cut by either reads as smaller than it is.
  * Two veins of one material that touch are one vein here.
  * Distance treats fluids as rock and unknown (ungenerated) neighbours as
    rock, and only air beneath the surface counts as a cave.

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
from itertools import product
from pathlib import Path

from .materialize import json_write
from .opportunity_map import ORE, cell_of

MIN_Y = -64
AIR = {'minecraft:air', 'minecraft:cave_air', 'minecraft:void_air'}
CATEGORIES = ('cave', 'sky', 'buried')
NEIGHBOURS = ((1, 0, 0), (-1, 0, 0), (0, 1, 0), (0, -1, 0), (0, 0, 1), (0, 0, -1))
BAND = 32
MATERIALS = sorted(set(ORE.values()))
DISTANCE_CAP = 12
DISTANCE_SAMPLES = 300
SPACING_CAP = 192
SPACING_GRID = 48

# An ore block is one integer: x, z and y packed into disjoint bit fields, so
# a neighbour is a constant offset and a million of them fit in a plain dict.
_OFF = 1 << 20
_ZMASK = (1 << 22) - 1


def key_of(x, y, z):
    return ((x + _OFF) << 32) | ((z + _OFF) << 10) | (y + 64)


def pos_of(k):
    return (k >> 32) - _OFF, (k & 1023) - 64, ((k >> 10) & _ZMASK) - _OFF


def _delta(dx, dy, dz):
    return (dx << 32) + (dz << 10) + dy


CONNECT = tuple(_delta(*d) for d in product((-1, 0, 1), repeat=3) if d != (0, 0, 0))


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
        for sec in data.get('sections', []):
            bs = sec.get('block_states')
            if bs is not None:
                self._raw[sec['Y']] = bs
        self._decoded = {}
        self._palettes = {}
        self._air = {}

    def section_ys(self):
        return self._raw.keys()

    def palette(self, sy):
        got = self._palettes.get(sy)
        if got is None:
            got = self._palettes[sy] = [p['Name'] for p in self._raw[sy]['palette']]
        return got

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

    def is_cave_air(self, x, y, z):
        """Air that lies beneath this column's surface."""
        h = self.height_at(x, z)
        if h is None or y >= h:
            return False
        sy = y >> 4
        if sy not in self._raw:
            return True
        flags = self._air.get(sy, 0)
        if flags == 0:
            air = [n in AIR for n in self.palette(sy)]
            flags = self._air[sy] = air if any(air) else None
        if flags is None:
            return False
        return flags[self.indices(sy)[(y & 15) * 256 + (z & 15) * 16 + (x & 15)]]


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


def survey_chunks(chunks, cell_size=128, y_range=(-60, 90), origin=None,
                  distance_cap=DISTANCE_CAP, distance_samples=DISTANCE_SAMPLES, keep_veins=False):
    """Survey already-loaded chunks ({(cx, cz): Chunk}); the pure core of the tool."""
    lo, hi = y_range
    if origin is None:
        origin = (min(cx for cx, _ in chunks) * 16, min(cz for _, cz in chunks) * 16)
    cells = defaultdict(lambda: {
        'ore': defaultdict(lambda: dict.fromkeys(CATEGORIES, 0)),
        'cave_volume': 0, 'chunks': 0, 'veins': 0,
    })
    bands = defaultdict(lambda: defaultdict(lambda: dict.fromkeys(CATEGORIES, 0)))
    ore = {}                      # key -> material index * 4 + category index
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
                ore[key_of(x, y, z)] = MATERIALS.index(material) * 4 + CATEGORIES.index(verdict)
    veins = find_veins(ore)
    for v in veins:
        cells[cell_of(int(v['x']), int(v['z']), cell_size, origin)]['veins'] += 1
    report = summarise(cells, bands, origin, cell_size, y_range, unknown)
    report['veins'] = vein_report(veins, report['chunks'])
    report['vein_spacing'] = spacing_report(veins)
    report['distance_to_cave'] = distance_report(chunks, ore, distance_cap, distance_samples)
    if keep_veins:
        report['vein_list'] = veins
    return report


def find_veins(ore):
    """Group same-material ore through all 26 neighbours; one record per vein."""
    seen = set()
    out = []
    for start, code in ore.items():
        if start in seen:
            continue
        material = code >> 2
        seen.add(start)
        stack = [start]
        size = sx = sy = sz = cave = 0
        ymin, ymax = 10 ** 6, -10 ** 6
        while stack:
            k = stack.pop()
            x, y, z = pos_of(k)
            size += 1
            sx += x; sy += y; sz += z
            ymin = min(ymin, y); ymax = max(ymax, y)
            if ore[k] & 3 == 0:
                cave += 1
            for d in CONNECT:
                n = k + d
                if n not in seen:
                    other = ore.get(n)
                    if other is not None and other >> 2 == material:
                        seen.add(n)
                        stack.append(n)
        out.append({'material': MATERIALS[material], 'size': size,
                    'x': round(sx / size, 1), 'y': round(sy / size, 1), 'z': round(sz / size, 1),
                    'y_min': ymin, 'y_max': ymax, 'cave_exposed_blocks': cave})
    return out


def percentile(sorted_values, q):
    if not sorted_values:
        return None
    return sorted_values[min(len(sorted_values) - 1, int(q * len(sorted_values)))]


SIZE_BUCKETS = ((1, 1), (2, 3), (4, 7), (8, 15), (16, 31), (32, None))


def _bucket_label(lo, hi):
    return f'{lo}' if lo == hi else (f'{lo}+' if hi is None else f'{lo}-{hi}')


def vein_report(veins, chunks):
    by = defaultdict(list)
    for v in veins:
        by[v['material']].append(v)
    out = {}
    for m, vs in sorted(by.items()):
        sizes = sorted(v['size'] for v in vs)
        blocks = sum(sizes)
        out[m] = {
            'veins': len(vs),
            'veins_per_chunk': round(len(vs) / chunks, 3) if chunks else None,
            'blocks': blocks,
            'size': {'min': sizes[0], 'median': percentile(sizes, 0.5), 'mean': round(blocks / len(vs), 2),
                     'p90': percentile(sizes, 0.9), 'max': sizes[-1]},
            'size_buckets': {_bucket_label(lo, hi): sum(1 for z in sizes if z >= lo and (hi is None or z <= hi))
                             for lo, hi in SIZE_BUCKETS},
            'ore_in_veins_of_3_or_fewer': round(sum(z for z in sizes if z <= 3) / blocks, 4),
            'veins_with_cave_exposure': sum(1 for v in vs if v['cave_exposed_blocks']),
        }
    return out


def _nearest(points, cap=SPACING_CAP, grid=SPACING_GRID):
    """Nearest-neighbour distance for each (x, y, z) point; None when none lies within cap."""
    if len(points) < 2:
        return [None] * len(points)
    cells = defaultdict(list)
    for i, (x, y, z) in enumerate(points):
        cells[(int(x // grid), int(y // grid), int(z // grid))].append(i)
    out = []
    for i, (x, y, z) in enumerate(points):
        cx, cy, cz = int(x // grid), int(y // grid), int(z // grid)
        best = float('inf')
        r = 0
        while True:
            for dx, dy, dz in product(range(-r, r + 1), repeat=3):
                if max(abs(dx), abs(dy), abs(dz)) != r:
                    continue
                for j in cells.get((cx + dx, cy + dy, cz + dz), ()):
                    if j != i:
                        px, py, pz = points[j]
                        d = ((x - px) ** 2 + (y - py) ** 2 + (z - pz) ** 2) ** 0.5
                        if d < best:
                            best = d
            if best <= r * grid or r * grid >= cap:
                break
            r += 1
        out.append(best if best <= cap else None)
    return out


def spacing_report(veins):
    groups = defaultdict(list)
    for v in veins:
        groups[(v['material'], (int(v['y'] // BAND)) * BAND)].append((v['x'], v['y'], v['z']))
    out = defaultdict(dict)
    for (m, band), pts in sorted(groups.items()):
        near = _nearest(pts)
        found = sorted(d for d in near if d is not None)
        out[m][str(band)] = {
            'veins': len(pts), 'isolated_within_cap': sum(1 for d in near if d is None),
            'nearest_median': round(percentile(found, 0.5), 1) if found else None,
            'nearest_p10': round(percentile(found, 0.1), 1) if found else None,
            'nearest_p90': round(percentile(found, 0.9), 1) if found else None,
        }
    return {'band_blocks': BAND, 'cap_blocks': SPACING_CAP, 'distance': 'between vein centroids, same material and band',
            'by_material': dict(sorted(out.items()))}


def steps_to_cave_air(chunks, x, y, z, cap):
    """Blocks of rock to dig from (x, y, z) to the nearest cave air, or None beyond `cap`."""
    start = (x, y, z)
    seen = {start}
    frontier = [start]
    for step in range(1, cap + 1):
        nxt = []
        for px, py, pz in frontier:
            for dx, dy, dz in NEIGHBOURS:
                q = (px + dx, py + dy, pz + dz)
                if q in seen:
                    continue
                seen.add(q)
                c = chunks.get((q[0] >> 4, q[2] >> 4))
                if c is None:
                    continue
                if c.is_cave_air(*q):
                    return step
                nxt.append(q)
        frontier = nxt
    return None


def _distance_buckets(cap):
    edges = [1, 2, 4, 8]
    edges = [e for e in edges if e < cap] + [cap]
    labels, lo = [], 1
    for e in edges:
        labels.append((lo, e, f'{lo}' if lo == e else f'{lo}-{e}'))
        lo = e + 1
    return labels


def distance_report(chunks, ore, cap, samples):
    by = defaultdict(list)
    for k, code in ore.items():
        by[MATERIALS[code >> 2]].append(k)
    buckets = _distance_buckets(cap)
    out = {}
    for m, keys in sorted(by.items()):
        keys.sort()
        stride = max(1, len(keys) // samples)
        picked = keys[::stride][:samples]
        counts = Counter()
        found = []
        for k in picked:
            if ore[k] & 3 == 0:
                d = 1                                  # touches cave air: one block to reach it
            else:
                d = steps_to_cave_air(chunks, *pos_of(k), cap)
            if d is None:
                counts[f'>{cap}'] += 1
            else:
                found.append(d)
                for lo, hi, label in buckets:
                    if lo <= d <= hi:
                        counts[label] += 1
        found.sort()
        beyond = counts[f'>{cap}']
        total = len(picked)
        out[m] = {
            'ore': len(keys), 'sampled': total, 'stride': stride,
            'buckets': {label: counts[label] for _, _, label in buckets} | {f'>{cap}': beyond},
            'median': (percentile(sorted(found + [cap + 1] * beyond), 0.5)
                       if total else None),
            'share_within_4': round(sum(1 for d in found if d <= 4) / total, 4) if total else None,
            'share_beyond_cap': round(beyond / total, 4) if total else None,
        }
    return {'cap_blocks': cap, 'method': 'path through non-air blocks to cave air; deterministic stride sample',
            'by_material': out}


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
            'ore_total': total, 'cave_exposed': exposed, 'veins': c['veins'],
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
        'schema': 'ore_survey/2',
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
                        'cave_exposed': spread('cave_exposed'), 'veins': spread('veins')},
        'cells': cell_rows,
        'not_covered': [
            'connectivity: a sealed pocket counts as cave volume (caves.py measures connected volume)',
            'caves straddling the surface, which the section-wise cave volume excludes',
            'neighbours in ungenerated chunks (reported as unknown_neighbours)',
            'veins cut by the Y range or the edge of the generated area read smaller than they are',
            'distance to cave is a stride sample, not every ore block',
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


def survey_world(world: Path, cell_size=128, y_range=(-60, 90), **kw):
    chunks = load_world(world)
    if not chunks:
        raise SystemExit(f'no chunks found under {world}/region')
    return survey_chunks(chunks, cell_size, y_range, **kw)


def render(report, name=''):
    t = report['totals']
    lines = [f"{name}  chunks {report['chunks']}  ore {t['ore']} ({t['ore_per_chunk']}/chunk)  "
             f"cave-exposed {t['cave_exposed']} ({100 * (t['cave_exposed_fraction'] or 0):.1f}%)  "
             f"sky {t['sky_exposed']}  buried {t['buried']}  "
             f"exposed/1k cave {t['cave_exposed_per_1k_cave']}"]
    for m, v in report['by_material'].items():
        lines.append(f"    {m:15} {100 * v['share']:5.1f}%  cave {v['cave']:6}  sky {v['sky']:6}  "
                     f"buried {v['buried']:7}  exposed {100 * v['cave_exposed_fraction']:4.1f}%")
    v, d = report.get('veins'), report.get('distance_to_cave')
    if v:
        lines.append('  veins (26-neighbour) / nearest same-band vein / dig distance to cave air:')
        for m, row in v.items():
            near = [b['nearest_median'] for b in report['vein_spacing']['by_material'].get(m, {}).values()
                    if b['nearest_median'] is not None]
            dist = d['by_material'].get(m, {})
            lines.append(f"    {m:15} {row['veins']:5} veins  size med {row['size']['median']:>3} p90 {row['size']['p90']:>3} "
                         f"max {row['size']['max']:>4}  in veins<=3 {100 * row['ore_in_veins_of_3_or_fewer']:4.1f}%  "
                         f"spacing med {statistics_median(near)}  dig<=4 {100 * (dist.get('share_within_4') or 0):4.1f}%  "
                         f">{d['cap_blocks']} {100 * (dist.get('share_beyond_cap') or 0):4.1f}%")
    return '\n'.join(lines)


def statistics_median(values):
    return f'{statistics.median(values):.0f}' if values else '-'


def main(argv=None):
    p = argparse.ArgumentParser(description=__doc__.split('\n')[0])
    p.add_argument('worlds', type=Path, nargs='+', help='world folders (each holds region/)')
    p.add_argument('--cell-size', type=int, default=128)
    p.add_argument('--y-min', type=int, default=-60)
    p.add_argument('--y-max', type=int, default=90)
    p.add_argument('--veins', action='store_true', help='include every vein in the JSON')
    p.add_argument('--distance-samples', type=int, default=DISTANCE_SAMPLES)
    p.add_argument('--distance-cap', type=int, default=DISTANCE_CAP)
    p.add_argument('--output', type=Path, help='write JSON here (one world), or a directory (several)')
    a = p.parse_args(argv)
    for w in a.worlds:
        report = survey_world(w, a.cell_size, (a.y_min, a.y_max), keep_veins=a.veins,
                              distance_cap=a.distance_cap, distance_samples=a.distance_samples)
        print(render(report, w.name))
        if a.output:
            out = a.output / f'{w.name}.json' if len(a.worlds) > 1 else a.output
            out.parent.mkdir(parents=True, exist_ok=True)
            json_write(out, report)
    return 0


if __name__ == '__main__':
    sys.exit(main())
