"""Seam 5: what authoring is FOR, and how much of it a map may need.

The reframe this module implements: authoring's purpose is **not** to inject
resources into the world -- that is what Worksites do -- but to measure the
difference in resource accessibility between the two sides and split the
difference using renewables. That separates two jobs that were running
together on one axis:

    Worksites  INJECT concentrated exceptional opportunity, on a cadence,
               contested, and deliberately unequal in the moment.
    Authoring  EQUALISES what the terrain already gave unequally, using
               regenerative sources, and aims at no new capability.

It also makes the intervention budget measurable instead of arbitrary.
Authoring is allowed exactly as much as closes the measured gap, and no more.
That is the line between a compiler and a level editor, stated as a rule.

THE FAILURE THIS IS DESIGNED AGAINST, which already happened. On 2026-09-22 a
near-miss rescue on seed 930010639 authored a 0.3173 accessible-land gap down
to a passing balance scalar of 0.0161 **while the gap itself stayed at
0.3173, untouched**. The objective measured travel-cost equality to the
opportunities authoring had just placed, so authoring satisfied the measure by
moving the thing the measure pointed at. The scalar agreed the map was
rescued. See docs/analysis/2026-09-22-nearmiss-rescue-930010639.md.

The rule that follows, and it is structural rather than a caution:

    THE GAP IS MEASURED ON TERRAIN, AND RE-MEASURED ON TERRAIN AFTER
    AUTHORING. A measure that authoring can satisfy by placing the thing it
    measures distance to will be satisfied that way.

So `gap` reads only what the terrain gave -- ore by material per team's
opening, opening exit capacity, permitted verbs -- and never counts authored
sources. `verify` re-runs the same measurement and requires the TERRAIN gap
to have closed, not a scalar derived from it.

NO BUDGET NUMBER IS SET. How much renewable placement is acceptable, and
whether some gaps are too large to close at all, are decisions this module
reports into and does not make.
"""
from __future__ import annotations


def _team_totals(validity: dict, team: str, opening_cost: float) -> dict:
    """What lies in THIS team's opening, per material.

    `resource_validity` sets `in_opening` from the MINIMUM cost across teams,
    which is the right flag for a ceiling check -- a windfall inside anyone's
    opening breaks progression -- and the wrong one here. Reading it credited
    both teams with every opening cell, so a 30/6 split reported 36 against
    36 and a real gap measured as 0.0.
    """
    per_material = {}
    for f in (validity or {}).get('findings') or ():
        depth = (f.get('strategic_depth_cost') or {}).get(team)
        if depth is None or depth > opening_cost:
            continue
        per_material[f['material']] = per_material.get(f['material'], 0) + f['count']
    return per_material


def _gap(a: float, b: float):
    total = a + b
    return 0.0 if total == 0 else round(abs(a - b) / total, 4)


def gap(validity: dict, access: dict | None = None,
        floor: dict | None = None, *, teams=('north', 'south'),
        opening_cost: float | None = None) -> dict:
    """What the TERRAIN gave unequally, before any authoring.

    Reads only natural opportunity. Nothing authored is counted, because a
    measure that includes authored sources can be closed by authoring without
    the terrain changing -- which is exactly how the near-miss rescue passed.
    """
    if opening_cost is None:
        opening_cost = (validity or {}).get('opening_cost', 120.0)
    totals = {t: _team_totals(validity, t, opening_cost) for t in teams}
    materials = sorted({m for d in totals.values() for m in d})
    per_material = {m: {'per_team': {t: totals[t].get(m, 0) for t in teams},
                        'gap': _gap(totals[teams[0]].get(m, 0),
                                    totals[teams[1]].get(m, 0))}
                    for m in materials}

    out = {'measured': True, 'source': 'natural opportunity only',
           'opening_cost': opening_cost,
           'per_material': per_material,
           'largest_material_gap': max(
               (v['gap'] for v in per_material.values()), default=0.0)}

    if access and access.get('comparison'):
        out['exit_capacity'] = {
            'exit_count_gap': access['comparison'].get('exit_count_gap'),
            'widest_exit_gap': access['comparison'].get('widest_exit_gap'),
            'note': 'access disparity is not closable by renewables. A team '
                    'with fewer or narrower ways out of its opening has a '
                    'geometry problem, and placing a crop patch does not '
                    'answer it.',
        }
    if floor and floor.get('blocked'):
        out['blocked_verbs'] = floor['blocked']
        out['floor_first'] = ('a blocked fundamental verb is not a gap to '
                              'split. maps.md requires the opening to permit '
                              'every verb, so this is a floor failure and '
                              'precedes any equalisation.')
    out['closable_by_renewables'] = [
        m for m in materials
        if m in ('copper',) or per_material[m]['gap'] > 0]
    out['not_closable_by_renewables'] = [
        'exit capacity and opening geometry',
        'finite ore, which renewables do not produce',
        'Strategic Depth, which is where the terrain put things',
    ]
    return out


def verify(before: dict, after: dict, *, tolerance: float = 0.0) -> dict:
    """Did authoring close the TERRAIN gap, or only a number derived from it?

    `after` must be a `gap()` computed by re-measuring the authored world, not
    a scalar recomputed from the same reading. If the two gaps are identical
    while a downstream balance figure has moved, that is the near-miss failure
    recurring and it is reported as such rather than passed.
    """
    a = (before or {}).get('per_material') or {}
    b = (after or {}).get('per_material') or {}
    rows, unchanged = [], []
    for m in sorted(set(a) | set(b)):
        was = a.get(m, {}).get('gap')
        now = b.get(m, {}).get('gap')
        rows.append({'material': m, 'before': was, 'after': now,
                     'closed': None if was is None or now is None
                     else round(was - now, 4)})
        if was is not None and now is not None and abs(was - now) <= tolerance:
            unchanged.append(m)
    return {
        'per_material': rows,
        'unchanged_materials': unchanged,
        'terrain_gap_moved': bool(rows) and len(unchanged) < len(rows),
        'code': None if (rows and len(unchanged) < len(rows))
        else 'TERRAIN_GAP_UNCHANGED',
        'why': 'the gap is re-measured on terrain. A balance scalar that '
               'improves while these numbers stand still is the 930010639 '
               'failure, where 0.3173 became a passing 0.0161 with the gap '
               'itself untouched.',
        'budget': 'none set. How much renewable placement is acceptable, and '
                  'whether some gaps should not be closed at all, are '
                  'decisions this reports into.',
    }
