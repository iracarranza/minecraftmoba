"""Seam 4: practical traversability, and what is already covered.

MOST OF THIS SEAM WAS ALREADY BUILT, which is worth stating before adding
anything. `expedition/travel.py` produces a full cost matrix between
homelands, team structures and cave cells, and its cost model is NOT raw
distance: `vanilla_search.task_a.Terrain` weights water at 10.0, canopy at
0.5, non-buildable ground at 0.35, rise at 0.5 and severe rise above 8 blocks
at 0.8. Terrain difficulty is therefore already in the number.

WHAT IS GENUINELY ABSENT IS DANGER AND SIGHTLINES, and both are marked
unresolved rather than forgotten. `rescan.py` says hostile exposure "remains
UNRESOLVED and is not reach", and `task_a.UNAVAILABLE` lists
`visibility_and_landmark_prominence` as beyond the committed grid. So a route
that is cheap and lethal reads the same as a route that is cheap and safe.

EQUAL TRAVEL TIME IS NOT THE TARGET. maps.md treats Wilderness as competitive
space that need not be mirrored, and the near-miss rescue test is the standing
warning: authoring drove a balance scalar to 0.0161 while the underlying
accessible-land gap stayed at 0.3173, because the objective measured
travel-cost equality to the opportunities authoring had just placed. So this
reports per-team profiles and a comparison, and asserts no bound.
"""
from __future__ import annotations

COVERED_BY_COST_MODEL = ('water', 'canopy', 'non-buildable ground',
                         'elevation rise', 'severe rise above 8 blocks')
NOT_COVERED = ('hostile exposure -- UNRESOLVED in rescan.py, and not reach',
               'visibility and sightlines -- UNAVAILABLE in task_a',
               'Hunger, provisioning, cargo, class movement methods',
               'infrastructure and Routes, which change reach during play')


def profile(travel: dict, *, teams=('north', 'south')) -> dict:
    """Per-team reach over the existing cost matrix, compared and unbounded."""
    sites = (travel or {}).get('sites') or []
    matrix = (travel or {}).get('cost_matrix') or {}
    if not sites or not matrix:
        return {'measured': False,
                'why': 'no travel matrix; run expedition.travel first'}

    homes = {s['team']: s['id'] for s in sites
             if s.get('kind') == 'homeland' and s.get('team')}
    shared = [s['id'] for s in sites if not s.get('team')]
    out = {}
    for team in teams:
        home = homes.get(team)
        if not home:
            out[team] = {'why': 'no homeland site for this team'}
            continue
        row = matrix.get(home) or {}
        costs = [row[k] for k in shared
                 if isinstance(row.get(k), (int, float))]
        costs.sort()
        out[team] = {
            'shared_sites': len(shared),
            'reachable': len(costs),
            'unreachable': len(shared) - len(costs),
            'nearest': costs[0] if costs else None,
            'median': costs[len(costs) // 2] if costs else None,
            'furthest': costs[-1] if costs else None,
        }

    measured = [t for t in teams if out.get(t, {}).get('reachable')]
    result = {'measured': True, 'per_team': out,
              'covered_by_cost_model': list(COVERED_BY_COST_MODEL),
              'not_covered': list(NOT_COVERED)}
    if len(measured) == 2:
        a, b = (out[t] for t in measured)
        def gap(x, y):
            if x is None or y is None:
                return None
            total = x + y
            return 0.0 if total == 0 else round(abs(x - y) / total, 4)
        result['comparison'] = {
            'teams': measured,
            'median_cost_gap': gap(a['median'], b['median']),
            'reachable_count_gap': gap(a['reachable'], b['reachable']),
            'not_a_target': 'equal travel cost is not the goal. Wilderness is '
                            'competitive space and need not be mirrored; this '
                            'is reported so a disparity is visible, not so it '
                            'can be minimised.',
        }
    result['bound'] = 'none.'
    return result
