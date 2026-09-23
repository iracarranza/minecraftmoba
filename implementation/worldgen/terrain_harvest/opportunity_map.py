"""Measure where opportunity already exists in a harvested volume.

maps.md forbids sprinkling regenerative nodes to satisfy a density target, and
instead asks for the opposite procedure: *identify real candidate
opportunities, evaluate their regional conditions, and observe the eligible
subset*. This tool performs that procedure on a materialized world.

It therefore **measures, it does not project**. Everything reported is counted
from blocks and entities that are already in the snapshot:

  ore                finite deposits, counted by material
  vegetation         naturally generated plants, counted by kind
  farmland potential grass or dirt with sky access near water
  fauna              animals actually present, counted by species
  hostiles           hostile entities actually present
  biomes             composition per cell, at the native quart resolution

Those map onto canon's own vocabulary: the counts are **Candidate Density**, and
the set of kinds with any presence in a cell is that cell's **Regenerative
Vocabulary**. Neither is invented here; both are read off the world.

What it deliberately does not do: assign depth tiers, assign values, decide
regenerative eligibility, or place any node. maps.md marks the depth gradient
and regional tables [OPEN], and a tool that filled them in would be inventing
the content canon withholds.
"""
from __future__ import annotations
import argparse
import json
import math
from collections import Counter, defaultdict
from pathlib import Path

from serialization.nbt import plain
from serialization.region import read_region
from vanilla_search.extract import VanillaChunk, _palette_value
from .materialize import json_write, read_entity_region, sha, state_tuple
from .model import Mask, loads

ORE = {
    'minecraft:coal_ore': 'coal', 'minecraft:deepslate_coal_ore': 'coal',
    'minecraft:copper_ore': 'copper', 'minecraft:deepslate_copper_ore': 'copper',
    'minecraft:iron_ore': 'iron', 'minecraft:deepslate_iron_ore': 'iron',
    'minecraft:gold_ore': 'gold', 'minecraft:deepslate_gold_ore': 'gold',
    'minecraft:redstone_ore': 'redstone', 'minecraft:deepslate_redstone_ore': 'redstone',
    'minecraft:lapis_ore': 'lapis', 'minecraft:deepslate_lapis_ore': 'lapis',
    'minecraft:diamond_ore': 'diamond', 'minecraft:deepslate_diamond_ore': 'diamond',
    'minecraft:emerald_ore': 'emerald', 'minecraft:deepslate_emerald_ore': 'emerald',
    'minecraft:ancient_debris': 'ancient_debris',
}

# Plant opportunity. Naturally generated plants, plus authored crops.
#
# An earlier version excluded crops on the grounds that wheat and potatoes do
# not generate outside villages. That reasoning was wrong for this project:
# crop patches are authored content here, exactly as herds and swarms are, so a
# crop block in the world is a placed opportunity and counting it is the point.
# Farmable land is still counted separately, as the candidate for an unplaced
# patch.
VEGETATION = {
    'minecraft:wheat': 'wheat', 'minecraft:potatoes': 'potatoes',
    'minecraft:carrots': 'carrots', 'minecraft:beetroots': 'beetroot',
    'minecraft:sugar_cane': 'sugar_cane', 'minecraft:bamboo': 'bamboo',
    'minecraft:kelp': 'kelp', 'minecraft:kelp_plant': 'kelp',
    'minecraft:sweet_berry_bush': 'sweet_berries', 'minecraft:cactus': 'cactus',
    'minecraft:melon': 'melon', 'minecraft:pumpkin': 'pumpkin',
    'minecraft:brown_mushroom': 'mushrooms', 'minecraft:red_mushroom': 'mushrooms',
    'minecraft:cocoa': 'cocoa',
}

FARMABLE = {'minecraft:grass_block', 'minecraft:dirt', 'minecraft:coarse_dirt',
            'minecraft:podzol', 'minecraft:rooted_dirt', 'minecraft:moss_block'}

WATER = {'minecraft:water'}

ANIMALS = {'minecraft:sheep', 'minecraft:cow', 'minecraft:pig', 'minecraft:chicken',
           'minecraft:rabbit', 'minecraft:horse', 'minecraft:donkey', 'minecraft:llama',
           'minecraft:cod', 'minecraft:salmon', 'minecraft:squid', 'minecraft:glow_squid',
           'minecraft:bee', 'minecraft:turtle', 'minecraft:fox', 'minecraft:goat',
           'minecraft:wolf', 'minecraft:polar_bear', 'minecraft:axolotl', 'minecraft:frog'}

HOSTILES = {'minecraft:zombie', 'minecraft:skeleton', 'minecraft:spider', 'minecraft:creeper',
            'minecraft:cave_spider', 'minecraft:husk', 'minecraft:stray', 'minecraft:drowned',
            'minecraft:witch', 'minecraft:slime', 'minecraft:pillager', 'minecraft:vindicator',
            'minecraft:guardian', 'minecraft:elder_guardian', 'minecraft:ravager',
            'minecraft:enderman', 'minecraft:phantom', 'minecraft:zombie_villager'}


def cell_of(x, z, size, origin):
    return ((x - origin[0]) // size, (z - origin[1]) // size)


def scan(volume, source: Path, cell_size: int, y_range, stride: int = 1):
    mask = Mask(volume)
    b = mask.bounds
    origin = (b['x'][0], b['z'][0])
    lo = max(b['y'][0], y_range[0])
    hi = min(b['y'][1], y_range[1])

    cells = defaultdict(lambda: {
        'ore': Counter(), 'vegetation': Counter(), 'fauna': Counter(),
        'hostiles': Counter(), 'biomes': Counter(),
        'farmable_surface': 0, 'water_surface': 0, 'sampled_columns': 0,
        'surface_y_sum': 0, 'surface_y_samples': 0,
        'sample_x_sum': 0, 'sample_z_sum': 0,
    })
    consumed = {}

    rx0, rx1 = b['x'][0] // 16 // 32, b['x'][1] // 16 // 32
    rz0, rz1 = b['z'][0] // 16 // 32, b['z'][1] // 16 // 32
    for rz in range(rz0, rz1 + 1):
        for rx in range(rx0, rx1 + 1):
            f = source / 'region' / f'r.{rx}.{rz}.mca'
            if not f.exists(): continue
            consumed[str(f.relative_to(source))] = sha(f)
            for cx, cz, _, root in read_region(f):
                chunk = VanillaChunk(plain(root))
                sections = {s.value['Y'].value: plain(s).get('block_states')
                            for s in root.value['sections'].value}
                for sy, container in sections.items():
                    if container is None: continue
                    if sy * 16 > hi or sy * 16 + 15 < lo: continue
                    # A full-depth scan of a whole map is hundreds of millions of
                    # blocks, so deep passes sample every Nth column and scale the
                    # result. Counts then become estimates and are labelled so.
                    for y in range(max(lo, sy * 16), min(hi, sy * 16 + 15) + 1, stride):
                        for z in range(cz * 16, cz * 16 + 16, stride):
                            for x in range(cx * 16, cx * 16 + 16, stride):
                                if not mask.include_block(x, y, z): continue
                                name = state_tuple(_palette_value(
                                    container, (y & 15) * 256 + (z & 15) * 16 + (x & 15), 4))[0]
                                if name in ORE:
                                    cells[cell_of(x, z, cell_size, origin)]['ore'][ORE[name]] += 1
                                elif name in VEGETATION:
                                    cells[cell_of(x, z, cell_size, origin)]['vegetation'][VEGETATION[name]] += 1
                # Surface sampling for farmable land and biome composition.
                for z in range(cz * 16, cz * 16 + 16, 4):
                    for x in range(cx * 16, cx * 16 + 16, 4):
                        if not mask.include_block(x, min(hi, 70), z): continue
                        cell = cells[cell_of(x, z, cell_size, origin)]
                        cell['sampled_columns'] += 1
                        # Centroid of the columns actually inside the volume.
                        # An edge cell's geometric centre can be outside it, and
                        # anything sited there cannot be written.
                        cell['sample_x_sum'] += x; cell['sample_z_sum'] += z
                        top = surface(chunk, x, z, lo, hi)
                        if top is None: continue
                        name, y = top
                        if name in FARMABLE: cell['farmable_surface'] += 1
                        elif name in WATER: cell['water_surface'] += 1
                        # Surface height, so anything sited in this cell can be
                        # placed at a measured y instead of a guessed one.
                        cell['surface_y_sum'] += y; cell['surface_y_samples'] += 1
                        try: cell['biomes'][chunk.biome(x, max(y, 63), z)] += 1
                        except Exception: pass

    for f in sorted((source / 'entities').glob('*.mca')):
        _, rx, rz = f.stem.split('.')
        if not (rx0 <= int(rx) <= rx1 and rz0 <= int(rz) <= rz1): continue
        consumed[str(f.relative_to(source))] = sha(f)
        for cx, cz, _, root in read_entity_region(f):
            for tag in root.value['Entities'].value:
                d = tag.value
                if 'Pos' not in d or 'id' not in d: continue
                px, _, pz = (p.value for p in d['Pos'].value)
                if not mask.include_block(int(px), b['y'][0], int(pz)): continue
                kind = d['id'].value
                cell = cells[cell_of(int(px), int(pz), cell_size, origin)]
                if kind in ANIMALS: cell['fauna'][kind.split(':')[1]] += 1
                elif kind in HOSTILES: cell['hostiles'][kind.split(':')[1]] += 1
    return cells, consumed, origin


def surface(chunk: VanillaChunk, x, z, lo, hi):
    for y in range(hi, lo - 1, -1):
        try: name = chunk.block(x, y, z)
        except Exception: return None
        if name and name != 'minecraft:air': return (name, y)
    return None


def summarise(cells, origin, cell_size):
    out = []
    for (ix, iz), c in sorted(cells.items()):
        vocabulary = sorted(set(c['ore']) | set(c['vegetation'])
                            | set(c['fauna']) | set(c['hostiles']))
        if not vocabulary and not c['farmable_surface']: continue
        out.append({
            'cell': [ix, iz],
            'world_origin': [origin[0] + ix * cell_size, origin[1] + iz * cell_size],
            'ore': dict(c['ore'].most_common()),
            'vegetation': dict(c['vegetation'].most_common()),
            'fauna': dict(c['fauna'].most_common()),
            'hostiles': dict(c['hostiles'].most_common()),
            'farmable_surface_samples': c['farmable_surface'],
            'water_surface_samples': c['water_surface'],
            'sampled_columns': c['sampled_columns'],
            'mean_surface_y': (round(c['surface_y_sum'] / c['surface_y_samples'], 1)
                               if c['surface_y_samples'] else None),
            # Fraction of the cell that lies inside the volume. A cell on the
            # boundary is a partial sample and must not be ranked as if it were
            # a whole one.
            'coverage': round(c['sampled_columns'] / ((cell_size // 4) ** 2), 4),
            'sampled_centroid': ([round(c['sample_x_sum'] / c['sampled_columns']),
                                  round(c['sample_z_sum'] / c['sampled_columns'])]
                                 if c['sampled_columns'] else None),
            'biomes': dict(c['biomes'].most_common(3)),
            # Canon's terms, measured rather than assigned.
            'candidate_density': sum(c['ore'].values()) + sum(c['vegetation'].values())
                                 + sum(c['fauna'].values()) + sum(c['hostiles'].values()),
            'regenerative_vocabulary': sorted(set(c['vegetation']) | set(c['fauna']) | set(c['hostiles'])),
        })
    return out


def run(gallery: Path, volume_id: str, source: Path, cell_size: int, y_range, output: Path,
        stride: int = 1):
    dest = gallery / 'dimensions/harvest' / volume_id
    volume = loads((dest / 'terrain_volume.json').read_text())
    cells, consumed, origin = scan(volume, source, cell_size, y_range, stride)
    rows = summarise(cells, origin, cell_size)
    result = {
        'schema': 'terrain_opportunity_map/1',
        'evidence_state': 'RAW WORLD OBSERVATION',
        'volume_id': volume_id,
        'cell_size_blocks': cell_size,
        'y_range': list(y_range),
        'stride': stride,
        'counts_are': 'exact' if stride == 1 else f'estimated; every {stride}th block sampled, '
                      f'raw counts NOT scaled up',
        'method': 'ore and vegetation counted per block in mask; surface sampled every 4 blocks '
                  'for farmable land and biome; entities counted from the snapshot',
        'cells': rows,
        'totals': {
            'cells_with_opportunity': len(rows),
            'ore': dict(sum((Counter(r['ore']) for r in rows), Counter()).most_common()),
            'vegetation': dict(sum((Counter(r['vegetation']) for r in rows), Counter()).most_common()),
            'fauna': dict(sum((Counter(r['fauna']) for r in rows), Counter()).most_common()),
            'hostiles': dict(sum((Counter(r['hostiles']) for r in rows), Counter()).most_common()),
        },
        'source_files_sha256': consumed,
        'not_covered': [
            'depth tiers and regional tables, which maps.md marks OPEN',
            'regenerative eligibility, which is a design rule and not a block property',
            'placement of any node; this reports where candidates already are',
            'entity counts are a snapshot and vary with spawning, unlike block counts',
        ],
    }
    json_write(output, result)
    return result


if __name__ == '__main__':
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('--gallery', type=Path, required=True)
    p.add_argument('--volume-id', required=True)
    p.add_argument('--source', type=Path, required=True)
    p.add_argument('--cell-size', type=int, default=128)
    p.add_argument('--y-min', type=int, default=-64)
    p.add_argument('--y-max', type=int, default=200)
    p.add_argument('--stride', type=int, default=1,
                   help='sample every Nth block; use >1 for deep full-map passes')
    p.add_argument('--output', type=Path, required=True)
    a = p.parse_args()
    r = run(a.gallery.resolve(), a.volume_id, a.source.resolve(), a.cell_size,
            (a.y_min, a.y_max), a.output.resolve(), a.stride)
    if a.stride > 1: print(f"stride {a.stride}: counts are SAMPLES, not totals")
    t = r['totals']
    print(f"{t['cells_with_opportunity']} cells with opportunity")
    for k in ('ore', 'vegetation', 'fauna', 'hostiles'):
        print(f"  {k}: {t[k]}")
