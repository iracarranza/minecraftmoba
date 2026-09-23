"""Cave and ore accessibility: is the underground findable, explorable and worth it?

Four criteria, each measured rather than asserted:

  difficult to reach   depth below the local surface
  findable without xray ore EXPOSED to cave air, versus fully buried
  genuinely explorable  connected cave volume, not isolated pockets
  guaranteed profit     exposed ore per unit of connected cave volume

The tension those criteria describe is real and has a measurable shape. Ore
sealed in stone is only findable by digging blind or by cheating; ore lining a
cave wall is found by exploring. **Exposed fraction is therefore the
discoverability statistic**, and it is the one that decides whether the
underground rewards play or rewards x-ray.

Sprawl is measured as connected components of cave air rather than as raw air
volume, because a thousand scattered one-block pockets are not explorable and
would otherwise score the same as one large system.

Nothing here sets a target. maps.md marks the depth gradient and regional
tables [OPEN]; this reports what the terrain already does so a target can be
chosen against evidence.
"""
from __future__ import annotations
import argparse
import json
from collections import Counter, defaultdict, deque
from pathlib import Path

from serialization.nbt import plain
from serialization.region import read_region
from vanilla_search.extract import VanillaChunk, _palette_value
from .materialize import json_write, sha, state_tuple
from .model import Mask, loads
from .opportunity_map import ORE

AIR = {'minecraft:air', 'minecraft:cave_air'}
FLUID = {'minecraft:water', 'minecraft:lava', 'minecraft:flowing_water', 'minecraft:flowing_lava'}


def scan_cell(chunkmap, x0, z0, size, lo, hi, surface_y):
    """Return cave air, connected components, and exposed/buried ore for one cell."""
    solid = {}
    ore_at = {}
    for x in range(x0, x0 + size):
        for z in range(z0, z0 + size):
            for y in range(lo, hi + 1):
                name = chunkmap(x, y, z)
                if name is None: continue
                if name in ORE: ore_at[(x, y, z)] = (ORE[name], name)
                solid[(x, y, z)] = name not in AIR and name not in FLUID

    # Cave air: open cells beneath the local surface. Air above ground is sky.
    cave = {p for p, s in solid.items() if not s and p[1] < surface_y.get((p[0], p[2]), hi)}

    # Connected components, so scattered pockets do not read as a cave system.
    seen, components = set(), []
    for start in cave:
        if start in seen: continue
        q, comp = deque([start]), 0
        seen.add(start)
        while q:
            cx, cy, cz = q.popleft()
            comp += 1
            for d in ((1,0,0),(-1,0,0),(0,1,0),(0,-1,0),(0,0,1),(0,0,-1)):
                n = (cx+d[0], cy+d[1], cz+d[2])
                if n in cave and n not in seen:
                    seen.add(n); q.append(n)
        components.append(comp)
    components.sort(reverse=True)

    exposed = Counter(); buried = Counter()
    # Depth is summed per resource so the expedition model can use a measured
    # descent rather than a guessed one. Deepslate share drives break time.
    y_sum = Counter(); deepslate = Counter()
    # Per-layer counts, so mining density at a chosen depth is measured rather
    # than inferred from a whole-cell average.
    y_hist = defaultdict(Counter)
    stone_hist = Counter()
    for (x, y, z), (material, name) in ore_at.items():
        touching = any((x+d[0], y+d[1], z+d[2]) in cave
                       for d in ((1,0,0),(-1,0,0),(0,1,0),(0,-1,0),(0,0,1),(0,0,-1)))
        (exposed if touching else buried)[material] += 1
        y_sum[material] += y
        y_hist[material][y] += 1
        if 'deepslate_' in name: deepslate[material] += 1

    # Diggable stone per layer is the denominator for any branch-mining rate.
    for (x, y, z), s_ in solid.items():
        if s_: stone_hist[y] += 1

    counts = exposed + buried
    return {
        'cave_air_blocks': len(cave),
        'cave_components': len(components),
        'largest_cave_component': components[0] if components else 0,
        'explorable_cave_volume': sum(c for c in components if c >= 64),
        'ore_exposed': dict(exposed.most_common()),
        'ore_buried': dict(buried.most_common()),
        'ore_mean_y': {m: round(y_sum[m] / counts[m], 1) for m in counts},
        'ore_deepslate_fraction': {m: round(deepslate[m] / counts[m], 4) for m in counts},
        'cave_mean_y': round(sum(p[1] for p in cave) / len(cave), 1) if cave else None,
        'ore_y_histogram': {m: dict(sorted(h.items())) for m, h in y_hist.items()},
        'solid_y_histogram': dict(sorted(stone_hist.items())),
    }


def run(gallery: Path, volume_id: str, source: Path, cell_size: int, y_range,
        sample_cells: int, output: Path):
    dest = gallery / 'dimensions/harvest' / volume_id
    volume = loads((dest / 'terrain_volume.json').read_text())
    mask = Mask(volume); b = mask.bounds
    lo, hi = max(b['y'][0], y_range[0]), min(b['y'][1], y_range[1])

    chunks = {}
    consumed = {}
    for f in sorted((source / 'region').glob('*.mca')):
        consumed[str(f.relative_to(source))] = sha(f)
        for cx, cz, _, root in read_region(f):
            chunks[(cx, cz)] = VanillaChunk(plain(root))

    def block(x, y, z):
        c = chunks.get((x >> 4, z >> 4))
        if c is None: return None
        try: return c.block(x, y, z)
        except Exception: return None

    # A handful of cells, fully scanned. Partial depth over the whole map would
    # measure nothing useful about connectivity.
    xs = list(range(b['x'][0], b['x'][1] - cell_size, max(cell_size, (b['x'][1]-b['x'][0])//sample_cells)))
    zs = list(range(b['z'][0], b['z'][1] - cell_size, max(cell_size, (b['z'][1]-b['z'][0])//sample_cells)))
    rows = []
    for x0 in xs[:sample_cells]:
        for z0 in zs[:sample_cells]:
            surface = {}
            for x in range(x0, x0 + cell_size):
                for z in range(z0, z0 + cell_size):
                    for y in range(hi, lo - 1, -1):
                        n = block(x, y, z)
                        if n and n not in AIR:
                            surface[(x, z)] = y; break
            if not surface: continue
            info = scan_cell(block, x0, z0, cell_size, lo, hi, surface)
            total_ore = sum(info['ore_exposed'].values()) + sum(info['ore_buried'].values())
            exposed = sum(info['ore_exposed'].values())
            info.update({
                'cell_origin': [x0, z0],
                'mean_surface_y': round(sum(surface.values()) / len(surface), 1),
                'total_ore': total_ore,
                # The discoverability statistic: what an explorer can find.
                'exposed_fraction': round(exposed / total_ore, 4) if total_ore else None,
                # The profit statistic: reward per unit of cave actually walked.
                'exposed_ore_per_1k_cave': round(1000 * exposed / info['explorable_cave_volume'], 2)
                                           if info['explorable_cave_volume'] else None,
            })
            rows.append(info)

    agg_exposed = sum(sum(r['ore_exposed'].values()) for r in rows)
    agg_buried = sum(sum(r['ore_buried'].values()) for r in rows)
    result = {
        'schema': 'terrain_cave_accessibility/1',
        'evidence_state': 'RAW WORLD OBSERVATION',
        'volume_id': volume_id, 'cell_size_blocks': cell_size,
        'y_range': [lo, hi], 'cells_scanned': len(rows),
        'cells': rows,
        'totals': {
            'ore_exposed': agg_exposed, 'ore_buried': agg_buried,
            'exposed_fraction': round(agg_exposed / (agg_exposed + agg_buried), 4)
                                if (agg_exposed + agg_buried) else None,
        },
        'reading': {
            'exposed_fraction': 'share of ore an explorer can see from a cave. Low means the '
                                'underground rewards x-ray or blind digging rather than play.',
            'explorable_cave_volume': 'air in connected components of 64 blocks or more; scattered '
                                      'pockets are excluded because they are not explorable.',
            'exposed_ore_per_1k_cave': 'reward density per unit of cave actually traversed.',
        },
        'source_files_sha256': consumed,
        'not_covered': [
            'whether a cave connects to the surface, which needs a flood fill from open sky',
            'travel difficulty along a cave, as distinct from straight-line depth',
            'structures, ravines and mineshafts as distinct from natural cave',
            'what any of these values SHOULD be; maps.md leaves the gradient OPEN',
        ],
    }
    json_write(output, result)
    return result


if __name__ == '__main__':
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('--gallery', type=Path, required=True)
    p.add_argument('--volume-id', required=True)
    p.add_argument('--source', type=Path, required=True)
    p.add_argument('--cell-size', type=int, default=48)
    p.add_argument('--y-min', type=int, default=-60)
    p.add_argument('--y-max', type=int, default=90)
    p.add_argument('--sample-cells', type=int, default=3)
    p.add_argument('--output', type=Path, required=True)
    a = p.parse_args()
    r = run(a.gallery.resolve(), a.volume_id, a.source.resolve(), a.cell_size,
            (a.y_min, a.y_max), a.sample_cells, a.output.resolve())
    t = r['totals']
    print(f"cells {r['cells_scanned']}  exposed {t['ore_exposed']}  buried {t['ore_buried']}  "
          f"exposed_fraction {t['exposed_fraction']}")
    for c in r['cells'][:6]:
        print(f"  {c['cell_origin']} surface_y={c['mean_surface_y']} "
              f"cave={c['explorable_cave_volume']} comps={c['cave_components']} "
              f"ore={c['total_ore']} exposed={c['exposed_fraction']} "
              f"density/1k={c['exposed_ore_per_1k_cave']}")
