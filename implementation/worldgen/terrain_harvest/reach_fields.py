"""Practical-Reach space: the two team cost fields, and what sits in them.

Decided 29 September 2026: the axis of a resource field is route cost, not
Euclidean radius. A location is characterized by BOTH team-relative costs,
C_N(x,z) and C_S(x,z), so it can be cheap for one side, expensive for the
other, expensive for both, or equally reachable. `portfolio.py`'s per-team
bands are the early, simple form of this measurement and are preserved: this
module reads them, it does not replace them.

Nothing here places anything. It describes, so that biome composition can be
reported by team side and depth band, which is what keeps a biome-specific field
from becoming accidentally one-sided.

Thresholds are declared fixtures. The relation labels are descriptive, not a
quality score.
"""
from __future__ import annotations

from collections import defaultdict

from . import portfolio

TEAMS = ('north', 'south')

# NON-CANON FIXTURE: |C_N - C_S| / (C_N + C_S) at or below this reads as equally
# reachable and contestable.
EQUAL_TOLERANCE = 0.25


def relation(cell: dict, opening_cost: float = 120.0, *, teams=TEAMS) -> str:
    """Where this cell sits between the two teams' cost fields."""
    cost = cell.get('strategic_depth_cost') or {}
    a, b = (cost.get(t) for t in teams)
    if a is None and b is None:
        return 'unreachable'
    if a is None or b is None:
        return f'only_{teams[0] if a is not None else teams[1]}'
    near = [t for t, c in zip(teams, (a, b)) if c <= opening_cost]
    if len(near) == 2:
        return 'contested_opening'
    if len(near) == 1:
        return f'cheap_{near[0]}'
    if abs(a - b) / (a + b or 1) <= EQUAL_TOLERANCE:
        return 'equal'
    return f'behind_{teams[0] if a < b else teams[1]}'


def bands(cells, opening_cost: float = 120.0, *, teams=TEAMS) -> dict:
    """Each cell's band FOR EACH TEAM, by exactly `portfolio.derive`'s rule.

    Ranked per team among that team's own cells beyond the opening. Reusing
    `portfolio._band_of` rather than restating the rule is the point: two
    definitions of "near" is the failure the compiler already paid for once.
    """
    out: dict = defaultdict(dict)
    for team in teams:
        priced = [c for c in (cells or ())
                  if (c.get('strategic_depth_cost') or {}).get(team) is not None]
        ordered = sorted(priced, key=lambda c: c['strategic_depth_cost'][team])
        beyond = [c for c in ordered if c['strategic_depth_cost'][team] > opening_cost]
        n = len(beyond) or 1
        for c in ordered:
            cost = c['strategic_depth_cost'][team]
            rank = ((beyond.index(c) + 1) / n) if cost > opening_cost else 0.0
            out[tuple(c.get('cell') or ())][team] = portfolio._band_of(cost, opening_cost, rank)
    return dict(out)


def profile(cells, opening_cost: float = 120.0, *, teams=TEAMS) -> dict:
    """Biome composition by team relation and by each team's depth band.

    Counts are sampled columns from `regional_character`, so a small biome is
    reported as small. Two views, because they answer different questions:
    `by_relation` says who a biome sits close to, `by_team_band` says how deep
    it is for each team.
    """
    cells = list(cells or ())
    per_cell_bands = bands(cells, opening_cost, teams=teams)
    by_relation: dict = defaultdict(lambda: defaultdict(int))
    by_team_band: dict = {t: defaultdict(lambda: defaultdict(int)) for t in teams}
    for c in cells:
        counts = c.get('regional_character') or c.get('biomes') or {}
        if not counts:
            continue
        rel = relation(c, opening_cost, teams=teams)
        cell_bands = per_cell_bands.get(tuple(c.get('cell') or ()), {})
        for biome, n in counts.items():
            by_relation[biome][rel] += n
            for team, band in cell_bands.items():
                by_team_band[team][band][biome] += n
    return {
        'unit': 'sampled columns (top three biomes per cell), not blocks',
        'opening_cost': opening_cost,
        'by_relation': {b: dict(r) for b, r in sorted(by_relation.items())},
        'by_team_band': {t: {band: dict(v) for band, v in sorted(d.items())}
                         for t, d in by_team_band.items()},
        'is': 'a description of where biomes sit in Practical-Reach space; '
              'not a score and not a placement',
    }
