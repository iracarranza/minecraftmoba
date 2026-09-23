"""Export measured world data into the calculator's map-manifest schema.

docs/analysis/extraction-expedition-calculator.md section 21 asks for exactly
this step: turn the worldgen resource geography into manifestations the
expedition simulator can consume, so the first benchmark run is data plumbing
rather than a fresh fixture.

Inputs are artefacts this repository already produces:

  --caves       terrain_cave_accessibility/1  (terrain_harvest.caves)
  --structures  team_structures_built/1       (terrain_harvest.build_structures)

Every manifestation field is either copied from a measurement or left absent.
Where the source does not measure something the simulator needs -- notably the
walking route from a destination to a cave mouth -- the field is emitted as
null and the simulator reports it unresolved rather than inventing a number
(calculator spec invariant 10).
"""
from __future__ import annotations

import argparse
import json
from pathlib import Path

SCHEMA = 'expedition_map_manifest/1'


BRANCH_HEIGHT = 3  # a 2-tall branch plus the floor it exposes


def best_band(ore_hist: dict, solid_hist: dict, height: int = BRANCH_HEIGHT):
    """Richest contiguous y-band, as ore blocks per solid block.

    A competent miner picks a depth. Averaging a resource over the whole column
    would understate branch mining by counting layers nobody digs, so the
    manifest records the best band a miner could actually choose, and the y it
    sits at, rather than a whole-cell mean.
    """
    if not ore_hist or not solid_hist:
        return None
    ore = {int(y): n for y, n in ore_hist.items()}
    solid = {int(y): n for y, n in solid_hist.items()}
    ys = range(min(solid), max(solid) - height + 2)
    best = None
    for y0 in ys:
        band = range(y0, y0 + height)
        o = sum(ore.get(y, 0) for y in band)
        s = sum(solid.get(y, 0) for y in band)
        if s <= 0:
            continue
        d = o / s
        if best is None or d > best['density']:
            best = {'y': y0, 'density': round(d, 6), 'ore': o, 'solid': s}
    return best


def cave_manifestations(caves: dict) -> list[dict]:
    """One manifestation per resource per scanned cell.

    A cell is the unit the cave scan actually measured, so it is the unit whose
    ore count, exposed share, depth and deepslate share are real numbers.
    """
    size = caves['cell_size_blocks']
    out = []
    for cell in caves['cells']:
        x0, z0 = cell['cell_origin']
        centre = [x0 + size // 2, z0 + size // 2]
        exposed, buried = cell['ore_exposed'], cell['ore_buried']
        mean_y = cell.get('ore_mean_y', {})
        deep = cell.get('ore_deepslate_fraction', {})
        hist = cell.get('ore_y_histogram', {})
        solid = cell.get('solid_y_histogram', {})
        for resource in sorted(set(exposed) | set(buried)):
            e, b = exposed.get(resource, 0), buried.get(resource, 0)
            out.append({
                'id': f'{resource}_{x0}_{z0}',
                'resource': resource,
                'position': centre,
                'physical_blocks': e + b,
                'blocks_exposed': e,
                'blocks_buried': b,
                'mean_y': mean_y.get(resource),
                'deepslate_fraction': deep.get(resource),
                'surface_y': cell['mean_surface_y'],
                'cave': {
                    'explorable_volume': cell['explorable_cave_volume'],
                    'components': cell['cave_components'],
                    'largest_component': cell['largest_cave_component'],
                    'mean_y': cell.get('cave_mean_y'),
                    'exposed_ore_per_1k_cave': cell['exposed_ore_per_1k_cave'],
                },
                'best_band': best_band(hist.get(resource, {}), solid),
                'discovery': 'unknown',
                # Join key into expedition_travel_matrix/1, which supplies the
                # routes the cave scan cannot measure.
                'travel_site_id': f'cave_{centre[0]}_{centre[1]}',
                'route_from': None,
                'source': 'MAP MEASUREMENT',
            })
    return out


def destinations(structures: dict) -> list[dict]:
    return [{
        'id': f"{s['team']}_{s['structure']}",
        'team': s['team'],
        'kind': s['structure'],
        'position': [s['world_xyz'][0], s['world_xyz'][2]],
        'y': s['world_xyz'][1],
        'source': 'MAP MEASUREMENT',
    } for s in structures['structures']]


def run(caves_path: Path, structures_path: Path, output: Path,
        volume_id: str | None = None) -> dict:
    caves = json.loads(caves_path.read_text())
    structures = json.loads(structures_path.read_text())

    manifestations = cave_manifestations(caves)
    doc = {
        'schema': SCHEMA,
        'evidence_state': 'DERIVED FROM RAW WORLD OBSERVATION',
        'volume_id': volume_id or caves.get('volume_id'),
        'cell_size_blocks': caves['cell_size_blocks'],
        'y_range': caves.get('y_range'),
        'destinations': destinations(structures),
        'manifestations': manifestations,
        'infrastructure': [],
        'worksites': [],
        'coverage': {
            'cells_scanned': caves.get('cells_scanned'),
            'not_covered': caves.get('not_covered', []),
        },
        'not_covered': [
            'traversable route length from any destination to any cave mouth; '
            'the cave scan measures volume, not paths',
            'which cave components actually reach open sky',
            'hostile pressure underground, which the opportunity map reads as zero',
            'Worksite and Supply Line state, which no map artefact yet emits',
        ],
        'source_files': {
            'caves': caves.get('source_files_sha256', {}),
            'structures': structures.get('world'),
        },
    }
    output.write_text(json.dumps(doc, indent=1, sort_keys=True) + '\n')
    return doc


def main(argv=None):
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('--caves', type=Path, required=True)
    p.add_argument('--structures', type=Path, required=True)
    p.add_argument('--output', type=Path, required=True)
    p.add_argument('--volume-id')
    a = p.parse_args(argv)
    doc = run(a.caves, a.structures, a.output, a.volume_id)
    print(f"{len(doc['manifestations'])} manifestations, "
          f"{len(doc['destinations'])} destinations -> {a.output}")


if __name__ == '__main__':
    main()
