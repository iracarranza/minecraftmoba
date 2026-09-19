"""Score and propose sites for the four team structures, symmetrically.

objectives.md gives each team a Pillager Outpost, a Nether Bastion, an End
Tower and an Aether Fountain, with the End Tower named as the final defensive
layer before the Fountain. That ordering is a *depth* ordering, so sites are
proposed along a homeland-relative depth gradient rather than anywhere.

What this can and cannot do, stated plainly:

  - It works on the existing oriented feature grid at 8-block sample spacing,
    so a proposal is a **candidate region**, not a block-exact placement. The
    final build needs the terrain-harvest tooling and a human eye.
  - It scores terrain *suitability for an authored build*. These structures are
    built, not generated, so the question is whether the ground accommodates a
    footprint, not whether something already stands there.
  - It reports symmetry as a measured difference between the two teams rather
    than forcing a mirror. maps.md requires asymmetric natural geography with
    equivalent baseline opportunity, so a numeric gap is the honest output.

No structure is selected here and no site is accepted. Every score is a
DERIVED MEASUREMENT over an ANALYTICAL FIXTURE footprint.
"""
from __future__ import annotations
import argparse
import json
import math
from pathlib import Path

from .task_a import Terrain, shortest

# Footprint radius in samples, and where each layer sits on the homeland-relative
# depth gradient (0 = at the homeland, 1 = at the far edge of the search band).
# ANALYTICAL FIXTURE: objectives.md fixes the ordering, not these numbers.
LAYERS = [
    {'id': 'pillager_outpost', 'radius_samples': 1, 'depth': 0.75, 'label': 'Pillager Outpost'},
    {'id': 'nether_bastion',   'radius_samples': 2, 'depth': 0.55, 'label': 'Nether Bastion'},
    {'id': 'end_tower',        'radius_samples': 2, 'depth': 0.35, 'label': 'End Tower'},
    {'id': 'aether_fountain',  'radius_samples': 2, 'depth': 0.15, 'label': 'Aether Fountain'},
]


def footprint(t: Terrain, centre: int, radius: int):
    x, z = t.xy(centre)
    return [zz * t.w + xx
            for zz in range(z - radius, z + radius + 1)
            for xx in range(x - radius, x + radius + 1)
            if 0 <= xx < t.w and 0 <= zz < t.h]


def pad_quality(t: Terrain, centre: int, radius: int):
    """Can this ground accept a built footprint? Relief, buildability, obstruction."""
    cells = footprint(t, centre, radius)
    expected = (2 * radius + 1) ** 2
    if len(cells) < expected:
        return None  # clipped by the region edge; not a usable pad
    heights = [t.v['height'][i] for i in cells]
    relief = max(heights) - min(heights)
    buildable = sum(t.v['buildable'][i] for i in cells) / len(cells)
    canopy = sum(t.v['canopy'][i] for i in cells) / len(cells)
    water = sum(t.v['water'][i] for i in cells) / len(cells)
    return {'relief_y': relief, 'buildable_fraction': round(buildable, 4),
            'canopy_fraction': round(canopy, 4), 'water_fraction': round(water, 4),
            'cells': len(cells)}


def approach_cost(t: Terrain, centre: int, radius: int):
    """Defensibility proxy: how expensive is the cheapest way in, per direction.

    A site reachable cheaply from every side is more exposed than one with a
    few costly approaches. This is a proxy over sampled terrain cost, not a
    claim about sightlines, verticality or player behaviour.
    """
    ring = []
    x, z = t.xy(centre)
    r = radius + 2
    for dz in range(-r, r + 1):
        for dx in range(-r, r + 1):
            if max(abs(dx), abs(dz)) != r: continue
            xx, zz = x + dx, z + dz
            if 0 <= xx < t.w and 0 <= zz < t.h: ring.append(zz * t.w + xx)
    if not ring: return None
    costs = shortest(t.adj, {centre: 0.0})[0]
    values = sorted(costs[i] for i in ring if math.isfinite(costs.get(i, math.inf)))
    if not values: return None
    cheapest = values[:max(1, len(values) // 8)]
    return {'cheapest_approach_mean': round(sum(cheapest) / len(cheapest), 3),
            'approach_spread': round(values[-1] - values[0], 3),
            'ring_samples': len(values)}


def score_site(t: Terrain, centre: int, layer, home_distance, band):
    pad = pad_quality(t, centre, layer['radius_samples'])
    if pad is None: return None
    approach = approach_cost(t, centre, layer['radius_samples'])
    if approach is None: return None
    if not math.isfinite(home_distance): return None

    # Depth fit: how close this site sits to the layer's intended band position.
    target = band * layer['depth']
    depth_error = abs(home_distance - target) / max(band, 1e-6)

    # Higher is better. Coefficients are a fixture, not balance.
    quality = (2.0 * pad['buildable_fraction']
               - 0.05 * pad['relief_y']
               - 1.0 * pad['canopy_fraction']
               - 1.5 * pad['water_fraction']
               - 1.5 * depth_error
               + 0.0005 * approach['cheapest_approach_mean'])
    return {'sample': list(t.xy(centre)), 'world_xyz': t.point(centre)['world_xyz'],
            'quality': round(quality, 4), 'home_distance': round(home_distance, 3),
            'depth_error': round(depth_error, 4), 'pad': pad, 'approach': approach}


def sites_for_team(t: Terrain, home: int, per_layer: int):
    costs = shortest(t.adj, {home: 0.0})[0]
    finite = [c for c in costs.values() if math.isfinite(c)]
    band = max(finite) if finite else 0.0
    out = {}
    for layer in LAYERS:
        scored = []
        for i in range(t.n):
            s = score_site(t, i, layer, costs.get(i, math.inf), band)
            if s is not None: scored.append(s)
        scored.sort(key=lambda s: -s['quality'])
        out[layer['id']] = {'label': layer['label'], 'radius_samples': layer['radius_samples'],
                            'intended_depth_fraction': layer['depth'],
                            'candidates': scored[:per_layer]}
    return out, band


def symmetry(a, b):
    """Measured difference between the teams' best sites, per layer."""
    rows = []
    for layer in LAYERS:
        ca = a[layer['id']]['candidates']
        cb = b[layer['id']]['candidates']
        if not ca or not cb:
            rows.append({'layer': layer['id'], 'status': 'UNRESOLVED',
                         'reason': 'no viable site for at least one team'})
            continue
        rows.append({'layer': layer['id'],
                     'quality_gap': round(abs(ca[0]['quality'] - cb[0]['quality']), 4),
                     'home_distance_gap': round(abs(ca[0]['home_distance'] - cb[0]['home_distance']), 3),
                     'note': 'equivalent baseline opportunity is the requirement; '
                             'identical geography is not'})
    return rows


def evaluate(candidate, homelands, per_layer=5, parameters=None):
    t = Terrain(candidate, parameters)
    teams = {}
    bands = {}
    for team, home_sample in homelands.items():
        home = home_sample[1] * t.w + home_sample[0]
        teams[team], bands[team] = sites_for_team(t, home, per_layer)
    result = {'schema': 'team_structure_sites/1', 'evidence_state': 'DERIVED MEASUREMENT',
              'resolution_blocks': t.s,
              'method': 'homeland-relative depth bands over the existing oriented feature grid',
              'teams': teams, 'travel_band': {k: round(v, 3) for k, v in bands.items()},
              'symmetry': symmetry(*teams.values()) if len(teams) == 2 else [],
              'not_covered': [
                  'block-exact placement; proposals are candidate regions at sample resolution',
                  'sightlines, verticality and interior layout',
                  'the authored builds themselves, including the Fountain water and glowstone',
                  'disable conditions, exposure and reactivation, which objectives.md leaves unresolved',
                  'final selection or acceptance of any site']}
    return result


if __name__ == '__main__':
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('--candidate', type=Path, required=True)
    p.add_argument('--home', action='append', required=True, metavar='TEAM=X,Z',
                   help='homeland sample coordinate per team, repeatable')
    p.add_argument('--per-layer', type=int, default=5)
    p.add_argument('--output', type=Path, required=True)
    a = p.parse_args()
    homes = {}
    for entry in a.home:
        team, coord = entry.split('=', 1)
        x, z = coord.split(',')
        homes[team] = (int(x), int(z))
    result = evaluate(json.loads(a.candidate.read_text()), homes, a.per_layer)
    a.output.parent.mkdir(parents=True, exist_ok=True)
    a.output.write_text(json.dumps(result, indent=2, sort_keys=True) + '\n')
    for row in result['symmetry']:
        print(row.get('layer'), row.get('status', ''),
              'quality_gap=' + str(row.get('quality_gap', '-')),
              'distance_gap=' + str(row.get('home_distance_gap', '-')))
