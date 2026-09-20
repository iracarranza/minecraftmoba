"""Export a terrain-weighted travel matrix between every named site on the map.

The expedition calculator needs `route_from` distances that the cave scan does
not measure, and an off-repo consumer needs them without the world files. This
exporter runs the existing Task A terrain graph -- the same weighted graph the
structure siting used, so travel costs here are consistent with the siting
decisions -- and reduces it to a pairwise matrix small enough to hand to anyone.

Cost is terrain-weighted blocks: straight-line distance inflated by rise, water,
non-buildable ground and canopy. Seconds are reported at vanilla walk and sprint
speed. That conversion treats a unit of weighted cost as a unit of walked
distance, which is the same assumption the siting already makes; it is flagged
in the output rather than hidden.
"""
from __future__ import annotations

import argparse
import json
import math
from pathlib import Path

from vanilla_search.task_a import Terrain, shortest

from . import vanilla

SCHEMA = 'expedition_travel_matrix/1'


def nearest_node(t: Terrain, x: int, z: int) -> int:
    """Index of the grid sample closest to a world x/z."""
    return min(range(t.n), key=lambda i: math.dist(t.coords[i], (x, z)))


def named_sites(candidate: dict, structures: dict, manifest: dict) -> list[dict]:
    sites = []
    for team, home in candidate['homelands'].items():
        x, z = home['raw_world']
        sites.append({'id': f'{team}_homeland', 'kind': 'homeland',
                      'team': team, 'world_xz': [x, z]})
    for s in structures['structures']:
        sites.append({'id': f"{s['team']}_{s['structure']}", 'kind': 'structure',
                      'team': s['team'], 'world_xz': [s['world_xyz'][0], s['world_xyz'][2]]})
    # One entry per scanned cave cell, not per resource; the cells are the
    # distinct places a player travels to.
    seen = set()
    for m in manifest['manifestations']:
        key = tuple(m['position'])
        if key in seen:
            continue
        seen.add(key)
        sites.append({'id': f'cave_{key[0]}_{key[1]}', 'kind': 'cave_cell',
                      'team': None, 'world_xz': list(key),
                      'surface_y': m['surface_y']})
    return sites


def run(candidate_path: Path, structures_path: Path, manifest_path: Path,
        output: Path) -> dict:
    candidate = json.loads(candidate_path.read_text())
    structures = json.loads(structures_path.read_text())
    manifest = json.loads(manifest_path.read_text())

    t = Terrain(candidate)
    sites = named_sites(candidate, structures, manifest)

    inside, outside = [], []
    bounds = candidate['region']['block_bounds']
    for s in sites:
        x, z = s['world_xz']
        if bounds[0] <= x <= bounds[1] and bounds[2] <= z <= bounds[3]:
            s['node'] = nearest_node(t, x, z)
            s['node_world_xz'] = list(t.coords[s['node']])
            s['node_offset_blocks'] = round(math.dist(t.coords[s['node']], (x, z)), 1)
            inside.append(s)
        else:
            # Outside the analysed region there is no terrain to path over.
            # Report it rather than projecting the site onto the boundary.
            s['node'] = None
            outside.append(s)

    matrix = {}
    for s in inside:
        distance, _ = shortest(t.adj, {s['node']: 0.0})
        matrix[s['id']] = {
            o['id']: round(distance[o['node']], 1)
            for o in inside if o['node'] in distance
        }

    unreachable = [(a, b) for a in matrix for b in matrix if b not in matrix[a]]

    doc = {
        'schema': SCHEMA,
        'evidence_state': 'DERIVED MEASUREMENT',
        'volume_id': manifest.get('volume_id'),
        'seed': candidate.get('seed'),
        'block_bounds': bounds,
        'sample_spacing_blocks': t.s,
        'cost_units': 'terrain-weighted blocks',
        'cost_model': {
            'source': 'vanilla_search.task_a.Terrain, the same weighted graph used '
                      'to site the team structures',
            'parameters': t.p,
        },
        'speeds_blocks_per_second': {'walk': vanilla.WALK_BPS,
                                     'sprint': vanilla.SPRINT_BPS},
        'sites': sites,
        'cost_matrix': matrix,
        'sites_outside_analysed_region': [s['id'] for s in outside],
        'unreachable_pairs': unreachable,
        'not_covered': [
            'a weighted-cost unit is converted to seconds as if it were a walked '
            'block; the weighting already inflates for terrain, so this is a '
            'travel-time proxy, not a measured traversal',
            'vertical descent into a cave, which the surface graph does not model',
            'boats, Routes, and any infrastructure that changes traversal',
            'contested ground; costs are the same for both teams',
        ],
    }
    output.write_text(json.dumps(doc, indent=1, sort_keys=True) + '\n')
    return doc


def main(argv=None):
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('--candidate', type=Path, required=True)
    p.add_argument('--structures', type=Path, required=True)
    p.add_argument('--manifest', type=Path, required=True)
    p.add_argument('--output', type=Path, required=True)
    a = p.parse_args(argv)
    doc = run(a.candidate, a.structures, a.manifest, a.output)
    n = len(doc['cost_matrix'])
    print(f'{n} sites reachable, '
          f'{len(doc["sites_outside_analysed_region"])} outside region, '
          f'{len(doc["unreachable_pairs"])} unreachable pairs -> {a.output}')
    if n:
        home = 'north_homeland'
        if home in doc['cost_matrix']:
            near = sorted(doc['cost_matrix'][home].items(), key=lambda kv: kv[1])[1:6]
            print(f'  nearest to {home}:')
            for k, v in near:
                print(f'    {k:28s} {v:8.0f} cost  '
                      f'{v / vanilla.SPRINT_BPS / 60:5.1f} min sprinting')


if __name__ == '__main__':
    main()
