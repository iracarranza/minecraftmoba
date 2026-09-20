"""Publish an authored world in forms something other than Minecraft can read.

A 28 MB region directory is only inspectable by the game. This exports the same
world twice, so a reviewer without a Minecraft client -- or a model -- can both
look at it and compute against it:

  <name>-map.png    a top-down render: terrain shading, water, the authored
                    Routes, and every authored site marked by kind
  <name>-grid.json  the same sampled surface as data -- height, a coarse
                    material class, and the site/Route index -- so questions
                    like "how far is the nearest worksite from the north
                    homeland" are answerable directly from the file

Neither is a substitute for the region files, and the render is not a screenshot:
it is a sampled orthographic projection of the surface, at whatever spacing was
asked for. It cannot show anything below the top block.
"""
from __future__ import annotations

import argparse
import json
import zlib
from pathlib import Path

from vanilla_search.render import _png

from .rescan import AIR, WATER, load, sample

SCHEMA = 'authored_world_grid/1'

# Coarse surface classes. Deliberately few: this is for reading a map, not for
# reconstructing the world.
CLASSES = {
    'water': (54, 92, 160),
    'route': (214, 184, 122),
    'route_deck': (176, 138, 88),
    'farmland': (120, 84, 52),
    'crop': (188, 196, 84),
    'grass': (104, 142, 76),
    'sand': (214, 204, 156),
    'stone': (132, 132, 132),
    'deepslate': (84, 84, 88),
    'snow': (236, 240, 244),
    'wood': (150, 112, 70),
    'built': (180, 180, 190),
    'other': (110, 110, 110),
}

MATERIAL = [
    ('route', {'minecraft:dirt_path'}),
    ('route_deck', {'minecraft:oak_planks'}),
    ('farmland', {'minecraft:farmland'}),
    ('crop', {'minecraft:wheat', 'minecraft:carrots', 'minecraft:potatoes',
              'minecraft:beetroots'}),
    ('grass', {'minecraft:grass_block', 'minecraft:dirt', 'minecraft:coarse_dirt',
               'minecraft:podzol', 'minecraft:moss_block', 'minecraft:rooted_dirt',
               'minecraft:mud'}),
    ('sand', {'minecraft:sand', 'minecraft:red_sand', 'minecraft:gravel',
              'minecraft:clay'}),
    ('snow', {'minecraft:snow_block', 'minecraft:snow', 'minecraft:powder_snow',
              'minecraft:ice', 'minecraft:packed_ice'}),
    ('deepslate', {'minecraft:deepslate', 'minecraft:tuff',
                   'minecraft:polished_deepslate', 'minecraft:deepslate_bricks',
                   'minecraft:cobbled_deepslate'}),
    ('stone', {'minecraft:stone', 'minecraft:andesite', 'minecraft:granite',
               'minecraft:diorite', 'minecraft:cobblestone', 'minecraft:calcite',
               'minecraft:stone_bricks', 'minecraft:chiseled_stone_bricks'}),
    ('built', {'minecraft:sea_lantern', 'minecraft:glowstone', 'minecraft:lantern',
               'minecraft:white_banner', 'minecraft:bricks', 'minecraft:barrier'}),
]

SITE_COLOUR = {
    'founder_crop': (248, 236, 96),
    'renewable_range': (120, 220, 140),
    'mining_worksite': (255, 132, 60),
    'poi': (150, 130, 255),
    'route_target': (255, 96, 132),
    'homeland': (255, 255, 255),
}


def classify(name: str) -> str:
    if name in WATER:
        return 'water'
    for label, members in MATERIAL:
        if name in members:
            return label
    if name.endswith(('_log', '_leaves', '_planks', '_wood', '_fence')):
        return 'wood'
    return 'other'


def shade(colour, y, lo, hi):
    """Relief shading, so the render reads as terrain rather than a flat map."""
    span = max(hi - lo, 1)
    t = 0.65 + 0.7 * (y - lo) / span
    return tuple(max(0, min(255, int(c * t))) for c in colour)


def build(world: Path, bounds, spacing: int):
    chunks = load(world)
    xs = list(range(bounds[0], bounds[1] + 1, spacing))
    zs = list(range(bounds[2], bounds[3] + 1, spacing))
    grid = []
    for z in zs:
        row = []
        for x in xs:
            s = sample(chunks, x, z)
            row.append(None if s is None else (s['y'], classify(s['block'])))
        grid.append(row)
    return xs, zs, grid


def render(xs, zs, grid, sites, out: Path, scale: int):
    heights = [c[0] for row in grid for c in row if c]
    lo, hi = (min(heights), max(heights)) if heights else (0, 1)
    w, h = len(xs) * scale, len(zs) * scale
    pixels = [[(18, 18, 22)] * w for _ in range(h)]
    for j, row in enumerate(grid):
        for i, cell in enumerate(row):
            if cell is None:
                continue
            y, label = cell
            colour = shade(CLASSES[label], y, lo, hi)
            for dy in range(scale):
                for dx in range(scale):
                    pixels[j * scale + dy][i * scale + dx] = colour
    # Sites on top, as filled squares with a dark border so they read at size.
    x0, z0, step = xs[0], zs[0], xs[1] - xs[0] if len(xs) > 1 else 1
    for s in sites:
        sx, sz = s['world_xz']
        px = int((sx - x0) / step * scale)
        pz = int((sz - z0) / step * scale)
        colour = SITE_COLOUR.get(s['kind'], (255, 255, 255))
        r = 4 if s['kind'] == 'homeland' else 3
        for dz in range(-r, r + 1):
            for dx in range(-r, r + 1):
                y, x = pz + dz, px + dx
                if 0 <= y < h and 0 <= x < w:
                    edge = max(abs(dx), abs(dz)) == r
                    pixels[y][x] = (12, 12, 14) if edge else colour
    # vanilla_search.render._png takes one flat RGB byte sequence.
    flat = bytearray()
    for row in pixels:
        for r_, g_, b_ in row:
            flat += bytes((r_, g_, b_))
    _png(out, w, h, flat)
    return {'width': w, 'height': h, 'y_range': [lo, hi]}


def encode_rows(grid, labels):
    rows = []
    for row in grid:
        out, run = [], None
        for cell in row:
            key = None if cell is None else [cell[0], labels.index(cell[1])]
            if run and run[0] == key:
                run[1] += 1
            else:
                run = [key, 1]
                out.append(run)
        rows.append(out)
    return rows


def decode_rows(rows, labels):
    grid = []
    for row in rows:
        out = []
        for key, count in row:
            value = None if key is None else (key[0], labels[key[1]])
            out.extend([value] * count)
        grid.append(out)
    return grid


def run(world: Path, candidate: Path, portfolio: Path, routes: Path | None,
        outdir: Path, name: str, spacing: int, scale: int) -> dict:
    cand = json.loads(candidate.read_text())
    port = json.loads(portfolio.read_text())
    rts = json.loads(routes.read_text()) if routes else None
    bounds = cand['region']['block_bounds']

    sites = [{'kind': 'homeland', 'detail': team, 'world_xz': h['raw_world']}
             for team, h in cand['homelands'].items()]
    sites += [{'kind': p['kind'], 'detail': p['detail'], 'cell': p['cell'],
               'world_xz': [p['world_xyz'][0], p['world_xyz'][2]],
               'y': p['world_xyz'][1]} for p in port['placements']]

    xs, zs, grid = build(world, bounds, spacing)
    outdir.mkdir(parents=True, exist_ok=True)
    png = outdir / f'{name}-map.png'
    meta = render(xs, zs, grid, sites, png, scale)

    # Encode then decode every sample; refuse a publication that does not round-trip.
    labels = sorted(CLASSES)
    rows = encode_rows(grid, labels)
    decoded = decode_rows(rows, labels)
    if decoded != grid:
        raise AssertionError('published grid RLE does not round-trip to sampled world')

    doc = {
        'schema': SCHEMA,
        'evidence_state': 'RAW WORLD OBSERVATION',
        'world': str(world), 'profile': port['profile'],
        'finalist_rank': port['finalist_rank'],
        'frontier_sha256': port.get('frontier_sha256'),
        'block_bounds': bounds,
        'sample_spacing_blocks': spacing,
        'origin_xz': [xs[0], zs[0]],
        'width_samples': len(xs), 'height_samples': len(zs),
        'surface_classes': labels,
        'row_encoding': 'run-length: [[[y, class_index] | null, count], ...] '
                        'per row, rows north to south, samples west to east',
        'rows': rows,
        'verification': {'round_trip_matches_source_grid': True,
                         'decoded_samples': len(xs) * len(zs),
                         'all_rows_match_declared_width': all(len(r) == len(xs) for r in decoded)},
        'sites': sites,
        'routes': [{'team': r['team'], 'cell': r['cell'], 'from': r['from'],
                    'to': r['to'], 'columns': r['columns'],
                    'weighted_cost': r['weighted_cost'],
                    'crosses': r.get('crosses', [])}
                   for r in (rts or {}).get('routes', [])],
        'render': {'file': png.name, **meta,
                   'site_colours': {k: list(v) for k, v in SITE_COLOUR.items()},
                   'class_colours': {k: list(v) for k, v in CLASSES.items()}},
        'not_covered': [
            'anything below the top block; this is a surface projection',
            'entities, which are not in the region files this reads',
            'block-exact detail between samples',
        ],
    }
    grid_path = outdir / f'{name}-grid.json'
    grid_path.write_text(json.dumps(doc, separators=(',', ':'), sort_keys=True) + '\n')
    return {'png': png, 'grid': grid_path, 'meta': meta,
            'samples': len(xs) * len(zs), 'sites': len(sites)}


def main(argv=None):
    a = argparse.ArgumentParser(description=__doc__)
    a.add_argument('--world', type=Path, required=True)
    a.add_argument('--candidate', type=Path, required=True)
    a.add_argument('--portfolio', type=Path, required=True)
    a.add_argument('--routes', type=Path)
    a.add_argument('--outdir', type=Path, required=True)
    a.add_argument('--name', required=True)
    a.add_argument('--spacing', type=int, default=4)
    a.add_argument('--scale', type=int, default=2)
    n = a.parse_args(argv)
    r = run(n.world, n.candidate, n.portfolio, n.routes, n.outdir, n.name,
            n.spacing, n.scale)
    print(f"{r['png']}  {r['meta']['width']}x{r['meta']['height']}px  "
          f"{r['png'].stat().st_size // 1024} KB")
    print(f"{r['grid']}  {r['samples']} samples, {r['sites']} sites, "
          f"{r['grid'].stat().st_size // 1024} KB")


if __name__ == '__main__':
    main()
