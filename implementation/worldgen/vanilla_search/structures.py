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

from .task_a import Terrain, path_to, shortest

# Footprint radius in samples, and where each layer sits on the advance axis.
#
# `depth` is the fraction of the way from a team's own homeland to the MIDLINE
# between the two homelands: 0 is at home, 1 is at the midline. Forward
# therefore means toward the rival, which is what a defensive layer ordering
# requires. Measuring it as raw distance from home instead pushed the most
# forward layer into a far map corner, pointing away from the contested ground.
#
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


def score_site(t: Terrain, centre: int, layer, home_distance, advance, approach=None):
    """approach is optional: it costs a Dijkstra, so it is computed only for
    shortlisted sites rather than for every sample on the grid.

    `advance` is 0 at the team's own homeland and 0.5 at the midline between the
    two homelands, so a layer's intended position is a point on the line toward
    the rival rather than a radius around home.
    """
    pad = pad_quality(t, centre, layer['radius_samples'])
    if pad is None: return None
    if not math.isfinite(home_distance) or advance is None: return None

    # Depth fit along the advance axis: home -> midline.
    target = 0.5 * layer['depth']
    depth_error = abs(advance - target) / 0.5

    # Higher is better. Coefficients are a fixture, not balance.
    quality = (2.0 * pad['buildable_fraction']
               - 0.05 * pad['relief_y']
               - 1.0 * pad['canopy_fraction']
               - 1.5 * pad['water_fraction']
               - 1.5 * depth_error
               + (0.0005 * approach['cheapest_approach_mean'] if approach else 0.0))
    return {'sample': list(t.xy(centre)), 'world_xyz': t.point(centre)['world_xyz'],
            'quality': round(quality, 4), 'home_distance': round(home_distance, 3),
            'advance': round(advance, 4), 'depth_error': round(depth_error, 4),
            'pad': pad, 'approach': approach, 'centre_index': centre}


def advance_of(own, rival):
    """0 at own homeland, 0.5 at the midline, 1 at the rival homeland."""
    if not math.isfinite(own): return None
    if rival is None or not math.isfinite(rival):
        return None
    total = own + rival
    if total <= 0: return 0.0
    return own / total


def corridor_cells(t: Terrain, home: int, rival_home: int, radius: int):
    """Samples within `radius` of the cheapest route between the two homelands.

    High advance alone rewards ground that is merely expensive for the rival to
    reach, which a lateral map corner satisfies. A forward defensive layer
    should sit on the ground an attacker actually crosses, so forward layers are
    constrained to the corridor and rear layers are not.
    """
    costs, previous = shortest(t.adj, {home: 0.0})
    if rival_home not in costs: return None
    path = path_to(previous, rival_home)
    if not path: return None
    near = set()
    for node in path:
        x, z = t.xy(node)
        for dz in range(-radius, radius + 1):
            for dx in range(-radius, radius + 1):
                xx, zz = x + dx, z + dz
                if 0 <= xx < t.w and 0 <= zz < t.h: near.add(zz * t.w + xx)
    return near


def sites_for_team(t: Terrain, home: int, per_layer: int, rival_costs=None,
                   corridor=None, corridor_from_depth=0.45):
    """Sites stay on the team's own side, and forward means toward the rival.

    Two corrections real candidate data forced. Sites were being proposed in
    enemy territory, and "forward" measured as raw distance from home sent the
    most forward layer to a map corner away from the contested ground. Both are
    fixed by scoring along the home-to-rival advance axis.
    """
    costs = shortest(t.adj, {home: 0.0})[0]
    advance = {}
    for i in range(t.n):
        a = advance_of(costs.get(i, math.inf),
                       None if rival_costs is None else rival_costs.get(i, math.inf))
        if a is not None: advance[i] = a
    # A defensive layer must not sit further from the threat than the base it
    # defends. Advance alone cannot tell "just in front of home" from "behind
    # home": both are cheap from home and dear from the rival, so a tower was
    # being placed 64 blocks behind its own fountain.
    home_to_rival = math.inf if rival_costs is None else rival_costs.get(home, math.inf)
    own_half = {i for i, a in advance.items()
                if a <= 0.5
                and (rival_costs is None or not math.isfinite(home_to_rival)
                     or rival_costs.get(i, math.inf) <= home_to_rival)}
    finite = [costs[i] for i in own_half if math.isfinite(costs.get(i, math.inf))]
    band = max(finite) if finite else 0.0
    out = {}
    # Reserve chosen ground so two structures cannot claim the same site. The
    # fountain is placed first because it is the rearmost and least movable.
    taken = []
    for layer in sorted(LAYERS, key=lambda l: l['depth']):
        scored = []
        # Forward layers must sit on the corridor an attacker crosses; rear
        # layers may sit anywhere on their own side.
        eligible = own_half
        if corridor is not None and layer['depth'] >= corridor_from_depth:
            eligible = own_half & corridor
            if not eligible: eligible = own_half     # no corridor here; say so below
        for i in eligible:
            s = score_site(t, i, layer, costs.get(i, math.inf), advance.get(i))
            if s is not None: scored.append(s)
        scored.sort(key=lambda s: -s['quality'])
        # Exclude reserved ground BEFORE shortlisting. Filtering only the
        # shortlist made a layer report no viable site whenever its best few
        # happened to fall inside an earlier structure's clearance.
        before_exclusion = len(scored)
        scored = [s for s in scored
                  if not any(max(abs(s['sample'][0] - tx), abs(s['sample'][1] - tz))
                             <= layer['radius_samples'] + 2 + tr
                             for tx, tz, tr in taken)]
        excluded = before_exclusion - len(scored)
        # Defensibility costs a Dijkstra each, so rescore only the shortlist.
        shortlist = scored[:max(per_layer * 4, 8)]
        rescored = []
        for s in shortlist:
            approach = approach_cost(t, s['centre_index'], layer['radius_samples'])
            full = score_site(t, s['centre_index'], layer,
                              costs.get(s['centre_index'], math.inf),
                              advance.get(s['centre_index']), approach)
            if full is not None: rescored.append(full)
        rescored.sort(key=lambda s: -s['quality'])
        if rescored:
            bx, bz = rescored[0]['sample']
            taken.append((bx, bz, layer['radius_samples']))
        for s in rescored: s.pop('centre_index', None)
        entry = {'label': layer['label'], 'radius_samples': layer['radius_samples'],
                 'intended_depth_fraction': layer['depth'],
                 'candidates': rescored[:per_layer]}
        if not rescored:
            # Say which of the two reasons it was, rather than an empty list.
            entry['refusal'] = ('all viable ground lies within an already-placed structure\'s '
                                'clearance' if excluded else
                                'no sample on this team\'s side accepts the footprint')
            entry['excluded_by_clearance'] = excluded
        out[layer['id']] = entry
    return out, band, len(own_half)


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
    homes = {team: hs[1] * t.w + hs[0] for team, hs in homelands.items()}
    all_costs = {team: shortest(t.adj, {h: 0.0})[0] for team, h in homes.items()}
    teams = {}
    bands = {}
    halves = {}
    for team, home in homes.items():
        rival_team = next((o for o in homes if o != team), None)
        rival = all_costs[rival_team] if rival_team else None
        corridor = (corridor_cells(t, home, homes[rival_team],
                                   int(t.p.get('homeland_radius_samples', 4)) + 2)
                    if rival_team else None)
        teams[team], bands[team], halves[team] = sites_for_team(t, home, per_layer, rival, corridor)
    result = {'schema': 'team_structure_sites/1', 'evidence_state': 'DERIVED MEASUREMENT',
              'resolution_blocks': t.s,
              'method': 'homeland-relative depth bands over the existing oriented feature grid',
              'teams': teams, 'travel_band': {k: round(v, 3) for k, v in bands.items()},
              'own_half_samples': halves,
              'corridor': 'forward layers are constrained to the cheapest route between the two '
                          'homelands, so they sit on ground an attacker crosses rather than merely '
                          'on ground the rival finds expensive',
              'containment': 'each team\'s sites are restricted to samples closer to its own '
                             'homeland than to the rival, and no further from the rival than the '
                             'homeland itself, so nothing is proposed in enemy territory or '
                             'behind its own base',
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
