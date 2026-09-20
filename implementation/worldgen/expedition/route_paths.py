"""Precompute the actual corridor polyline for every candidate Route target.

The v2 spillover model awarded collateral savings by proximity to the *straight*
segment from a homeland to a Route target. Physical Route authoring instead
follows the terrain-weighted cheapest path, which deviates furthest exactly
where terrain is broken -- which is where a corridor matters most. Validation
measured the consequence: roughly the right total saving, spread over largely
the wrong opportunities (recall 30%, correlation ~0.1).

This exports the real thing. One shortest path per candidate Route target from
its own team's homeland, over `vanilla_search.task_a.Terrain` -- the same graph
that sited the structures, produced the travel matrix, and that
`terrain_harvest.routes` walks when it actually builds a corridor.

It is a fixed, configuration-independent artefact: the path from a homeland to
a candidate does not depend on which other candidates a configuration selected.
So 63 paths are computed once here rather than thousands of times inside the
optimizer's sampling loop.
"""
from __future__ import annotations

import argparse
import json
from pathlib import Path

from vanilla_search.task_a import Terrain, path_to, shortest

SCHEMA = 'route_candidate_paths/1'


def simplify(points, tolerance: float = 6.0):
    """Drop points that lie close to the line between their neighbours.

    A path at 8-block sample spacing is mostly straight runs. Keeping every
    node would make the artefact large and the distance test slower without
    changing which opportunities fall near the corridor.
    """
    if len(points) <= 2:
        return list(points)
    keep = [points[0]]
    for prev, cur, nxt in zip(points, points[1:], points[2:]):
        ax, az = prev
        bx, bz = nxt
        dx, dz = bx - ax, bz - az
        den = dx * dx + dz * dz
        if den <= 1e-9:
            keep.append(cur)
            continue
        t = ((cur[0] - ax) * dx + (cur[1] - az) * dz) / den
        t = max(0.0, min(1.0, t))
        d = ((cur[0] - (ax + t * dx)) ** 2 + (cur[1] - (az + t * dz)) ** 2) ** 0.5
        if d > tolerance:
            keep.append(cur)
    keep.append(points[-1])
    return keep


def run(candidate_path: Path, catalog_path: Path, output: Path,
        tolerance: float = 6.0) -> dict:
    candidate = json.loads(candidate_path.read_text())
    catalog = json.loads(catalog_path.read_text())
    t = Terrain(candidate)

    def node_at(x, z):
        return min(range(t.n),
                   key=lambda i: (t.coords[i][0] - x) ** 2 + (t.coords[i][1] - z) ** 2)

    homes = {team: node_at(*h['raw_world'])
             for team, h in candidate['homelands'].items()}

    paths, unreachable = {}, []
    for team, targets in catalog['route_targets'].items():
        distance, previous = shortest(t.adj, {homes[team]: 0.0})
        for target in targets:
            centre = target.get('center') or target.get('world_center')
            goal = node_at(*centre)
            if goal not in distance:
                unreachable.append({'team': team, 'cell': target['cell']})
                continue
            pts = [list(t.coords[i]) for i in path_to(previous, goal)]
            key = f"{team}:{target['cell'][0]},{target['cell'][1]}"
            paths[key] = {
                'team': team, 'cell': target['cell'], 'centre': list(centre),
                'polyline': [list(map(int, p)) for p in simplify(pts, tolerance)],
                'nodes': len(pts),
                'weighted_cost': round(distance[goal], 1),
            }

    doc = {
        'schema': SCHEMA,
        'evidence_state': 'DERIVED MEASUREMENT',
        'source_graph': 'vanilla_search.task_a.Terrain, the graph used for '
                        'structure siting, the travel matrix, and physical '
                        'Route authoring',
        'sample_spacing_blocks': t.s,
        'simplify_tolerance_blocks': tolerance,
        'homelands': {team: list(t.coords[n]) for team, n in homes.items()},
        'paths': paths,
        'unreachable': unreachable,
        'not_covered': [
            'the corridor width and clearing rules that terrain_harvest.routes '
            'applies; this is the centreline only',
            'any effect of one corridor on another',
        ],
    }
    output.write_text(json.dumps(doc, separators=(',', ':'), sort_keys=True) + '\n')
    return doc


def main(argv=None):
    a = argparse.ArgumentParser(description=__doc__)
    a.add_argument('--candidate', type=Path, required=True)
    a.add_argument('--catalog', type=Path, required=True)
    a.add_argument('--output', type=Path, required=True)
    a.add_argument('--tolerance', type=float, default=6.0)
    n = a.parse_args(argv)
    d = run(n.candidate, n.catalog, n.output, n.tolerance)
    pts = sum(len(p['polyline']) for p in d['paths'].values())
    print(f"{len(d['paths'])} candidate paths, {pts} polyline points, "
          f"{len(d['unreachable'])} unreachable -> {n.output} "
          f"({n.output.stat().st_size // 1024} KB)")


if __name__ == "__main__":
    main()
