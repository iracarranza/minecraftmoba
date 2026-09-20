"""Author physical Routes between a team's homeland and its Route targets.

The authoring contract is specific about what a Route is:

    routes: Author physical path quality; do not implement an intrinsic
            movement-speed bonus.

and the design document evaluates Route targets by *how physical path quality
changes Practical Reach*. So a Route is terrain work -- a surfaced, cleared,
bridged corridor -- and nothing else. An earlier pass marked the targets with
cairns and wrote no path, which satisfied neither document.

The corridor follows the same terrain-weighted graph that sited the team
structures and produced the travel matrix, so an authored Route runs where the
analysis already said the cheapest crossing is, rather than along a straight
line drawn over whatever terrain is in the way.

Nothing here grants speed. The path is dirt_path over land and a plank deck over
water; both are ordinary blocks. The effect on Practical Reach is whatever the
rescan measures, which is the point of authoring it physically.
"""
from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path

from serialization.nbt import plain
from serialization.region import read_region
from serialization.world import block
from vanilla_search.extract import VanillaChunk
from vanilla_search.task_a import Terrain, path_to, shortest

from .build_structures import WorldEditor

SCHEMA = 'map_routes_authored/1'

SURFACE = block('dirt_path')
DECK = block('oak_planks')
AIR = block('air')
WATER = {'minecraft:water', 'minecraft:flowing_water'}
SKIP = {'minecraft:air', 'minecraft:cave_air', 'minecraft:void_air'}

HALF_WIDTH = 1      # a 3-wide corridor
HEADROOM = 3        # blocks cleared above the walking surface


def load_chunks(world: Path) -> dict:
    out = {}
    for f in sorted((world / 'region').glob('*.mca')):
        for cx, cz, _, root in read_region(f):
            out[(cx, cz)] = VanillaChunk(plain(root))
    return out


def column(chunks, x: int, z: int, hi: int = 200, lo: int = -64):
    """Topmost non-air block in a column: (name, y), or None outside the world."""
    c = chunks.get((x >> 4, z >> 4))
    if c is None:
        return None
    for y in range(hi, lo - 1, -1):
        try:
            name = c.block(x, y, z)
        except Exception:
            return None
        if name and name not in SKIP:
            return (name, y)
    return None


def densify(points):
    """Walk a polyline of grid samples one block at a time, without repeats."""
    out = []
    for (x0, z0), (x1, z1) in zip(points, points[1:]):
        steps = max(abs(x1 - x0), abs(z1 - z0))
        for i in range(steps):
            out.append((round(x0 + (x1 - x0) * i / steps),
                        round(z0 + (z1 - z0) * i / steps)))
    out.append(points[-1])
    seen, unique = set(), []
    for p in out:
        if p not in seen:
            seen.add(p); unique.append(p)
    return unique


def carve(editor, chunks, centreline):
    """Surface, clear and bridge a corridor. Returns per-segment statistics."""
    laid = bridged = missing = 0
    for x, z in centreline:
        for dx in range(-HALF_WIDTH, HALF_WIDTH + 1):
            for dz in range(-HALF_WIDTH, HALF_WIDTH + 1):
                cx, cz = x + dx, z + dz
                top = column(chunks, cx, cz)
                if top is None:
                    missing += 1
                    continue
                name, y = top
                if name in WATER:
                    # Deck at water level rather than filling the water in.
                    editor.set(cx, y, cz, DECK)
                    bridged += 1
                else:
                    editor.set(cx, y, cz, SURFACE)
                    laid += 1
                for dy in range(1, HEADROOM + 1):
                    editor.set(cx, y + dy, cz, AIR)
    return {'surfaced': laid, 'bridged': bridged, 'unwritable_columns': missing}


def crossings_of(line, placements, pad: int = 2):
    """Authored sites a corridor runs through.

    A Route crossing a POI is strategically meaningful rather than damage -- the
    design document values POIs partly for the traffic they attract -- so the
    corridor is not diverted. But it does overwrite surface blocks, so every
    crossing is recorded here instead of being found later by a readback.
    """
    hit = []
    columns = set(line)
    for site in placements or []:
        x, _, z = site['world_xyz']
        r = site.get('radius') or 0
        if any(abs(cx - x) <= r + pad and abs(cz - z) <= r + pad
               for cx, cz in columns):
            hit.append({'kind': site['kind'], 'detail': site.get('detail'),
                        'cell': site['cell'], 'world_xz': [x, z]})
    return hit


def author(candidate: dict, configuration: dict, world: Path, report: Path,
           dry_run: bool = True, placements: list | None = None) -> dict:
    t = Terrain(candidate)
    chunks = load_chunks(world)
    present = set(chunks)

    def node_at(x, z):
        return min(range(t.n), key=lambda i: (t.coords[i][0] - x) ** 2
                                             + (t.coords[i][1] - z) ** 2)

    homes = {team: node_at(*h['raw_world'])
             for team, h in candidate['homelands'].items()}

    editor = WorldEditor(world)
    routes, skipped = [], []
    for team, targets in (configuration.get('route_targets') or {}).items():
        home = homes[team]
        distance, previous = shortest(t.adj, {home: 0.0})
        for target in targets:
            goal = node_at(*target['center'])
            if goal not in distance:
                skipped.append({'team': team, 'cell': target['cell'],
                                'reason': 'no path from homeland in the terrain graph'})
                continue
            nodes = path_to(previous, goal)
            points = [t.coords[i] for i in nodes]
            line = densify(points)
            # Refuse a corridor that leaves the world rather than part-writing it.
            outside = [(x, z) for x, z in line
                       if ((x >> 4), (z >> 4)) not in present]
            if outside:
                skipped.append({'team': team, 'cell': target['cell'],
                                'reason': f'{len(outside)} of {len(line)} columns '
                                          'lie outside the harvested volume'})
                continue
            stats = carve(editor, chunks, line)
            crossed = crossings_of(line, placements)
            routes.append({'team': team, 'cell': target['cell'],
                           'crosses': crossed,
                           'from': list(t.coords[home]), 'to': list(t.coords[goal]),
                           'nodes': len(nodes), 'columns': len(line),
                           'weighted_cost': round(distance[goal], 1), **stats})

    # editor.written only counts on flush, so a dry run reports what it would
    # write rather than a misleading zero.
    pending = sum(len(cells) for cells in editor.pending.values())
    if not dry_run:
        editor.flush()

    result = {
        'schema': SCHEMA,
        'evidence_state': 'RAW WORLD OBSERVATION' if not dry_run else 'DERIVED MEASUREMENT',
        'world': str(world), 'dry_run': dry_run,
        'blocks_written': editor.written,
        'blocks_pending': pending,
        'corridor_width': HALF_WIDTH * 2 + 1,
        'headroom': HEADROOM,
        'routes': routes, 'skipped': skipped,
        'crossings': [c for r in routes for c in r['crosses']],
        'cost_model': 'vanilla_search.task_a.Terrain, the graph that sited the '
                      'team structures and produced the travel matrix',
        'nature': 'physical path quality only: surfacing, clearing and decking. '
                  'No movement-speed effect is granted or implied.',
        'not_covered': [
            'stairs or grading on steep steps; the corridor follows terrain',
            'what the Route does to Practical Reach, which only the rescan measures',
            'Route width, surface material and headroom as design decisions; these '
            'are authoring fixtures, not canon',
            'diverting a corridor around an authored site; crossings are recorded '
            'rather than avoided, because rerouting would alter the strategic '
            'geometry the optimizer selected',
        ],
    }
    report.parent.mkdir(parents=True, exist_ok=True)
    report.write_text(json.dumps(result, indent=1, sort_keys=True) + '\n')
    return result


def main(argv=None):
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('--candidate', type=Path, required=True)
    p.add_argument('--frontier', type=Path, required=True)
    p.add_argument('--profile', required=True)
    p.add_argument('--rank', type=int, default=0)
    p.add_argument('--world', type=Path, required=True)
    p.add_argument('--report', type=Path, required=True)
    p.add_argument('--placements', type=Path,
                   help='portfolio report, so corridor crossings are recorded')
    p.add_argument('--apply', action='store_true')
    a = p.parse_args(argv)
    frontier = json.loads(a.frontier.read_text())
    cfg = frontier[a.profile]['finalists'][a.rank]['configuration']
    placements = (json.loads(a.placements.read_text())['placements']
                  if a.placements else None)
    r = author(json.loads(a.candidate.read_text()), cfg, a.world, a.report,
               dry_run=not a.apply, placements=placements)
    print(f"{a.profile} #{a.rank}: {len(r['routes'])} routes, "
          f"{sum(x['columns'] for x in r['routes'])} columns, "
          f"{r['blocks_written'] or r['blocks_pending']} blocks"
          f"{'' if a.apply else ' (dry run)'}, {len(r['skipped'])} skipped")
    for c in r['crossings']:
        print(f"  crosses {c['kind']} {c['detail'] or ''} at {c['world_xz']}")
    for s in r['skipped']:
        print(f"  skipped {s['team']} {s['cell']}: {s['reason']}")


if __name__ == '__main__':
    main()
