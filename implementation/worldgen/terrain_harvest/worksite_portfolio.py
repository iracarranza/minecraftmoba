"""Choose the Worksite sites a generated map offers, as a runtime binding.

Worksites are distributed opportunities, deliberately unlike the Lair: a
Worksite night pulls teams toward scattered sites while a Lair night
concentrates them at the single shared landmark, and that contrast is what stops
every important night resolving to "go middle". So this spreads sites out and
explicitly does NOT cluster them around the Lair.

It also does not balance them between the teams. Wilderness is not mirrored, and
the runtime already chooses randomly from the eligible pool each night; what the
map owes is a portfolio both teams can reach, not one that is symmetric.

The count is a runtime requirement rather than a preference: the cadence has
three Worksite nights and `activationsPerTier` opens two or three sites per
night, so a portfolio smaller than that would leave a night opening nothing.
"""
from __future__ import annotations

import math

from vanilla_search.task_a import Terrain, shortest

# PROVISIONAL_ALPHA. Enough for three nights at the configured 2/2/3 activations
# with room for the random draw to differ between matches, not a balance claim.
PORTFOLIO_SIZE = 12

# Sites must be separated so a night's activations are not all the same place.
MIN_SEPARATION_SAMPLES = 6


def choose(candidate, fountains, lair_xz=None, *, size=PORTFOLIO_SIZE,
           separation=MIN_SEPARATION_SAMPLES, lair_keepout=10):
    """Pick spread, buildable, reachable Worksite sites.

    `fountains` maps team to a sample coordinate. Sites are scored by how
    workable the ground is and rejected when either team cannot reach them at
    all -- unreachable is a structural failure, while merely nearer one team is
    ordinary competitive geography.
    """
    t = Terrain(candidate)
    homes = {team: xz[1] * t.w + xz[0] for team, xz in fountains.items()}
    costs = {team: shortest(t.adj, {i: 0.0})[0] for team, i in homes.items()}

    lair_cell = None
    if lair_xz:
        lair_cell = (lair_xz[0], lair_xz[1])

    scored = []
    for i in range(t.n):
        x, z = t.xy(i)
        if t.v['water'][i] or not t.v['buildable'][i] or t.grade[i] > 6:
            continue
        # Not clustered on the Lair: the two systems are supposed to pull in
        # different directions.
        if lair_cell and max(abs(x - lair_cell[0]), abs(z - lair_cell[1])) < lair_keepout:
            continue
        # Outside both opening envelopes; a Worksite is a reason to leave home.
        if any(max(abs(x - t.xy(h)[0]), abs(z - t.xy(h)[1])) < 8 for h in homes.values()):
            continue
        reach = {team: costs[team].get(i, math.inf) for team in homes}
        if any(not math.isfinite(v) for v in reach.values()):
            continue
        # A small local window, not Terrain.cells: that is the homeland
        # footprint helper and runs off the edge of the grid here.
        workable = 0
        for dz in (-1, 0, 1):
            for dx in (-1, 0, 1):
                xx, zz = x + dx, z + dz
                if 0 <= xx < t.w and 0 <= zz < t.h:
                    c = zz * t.w + xx
                    workable += t.v['buildable'][c] and not t.v['water'][c]
        scored.append((workable, -max(reach.values()), i, x, z, reach))
    scored.sort(reverse=True)

    picked = []
    for workable, _, i, x, z, reach in scored:
        if len(picked) >= size:
            break
        if any(max(abs(x - p['sample'][0]), abs(z - p['sample'][1])) < separation
               for p in picked):
            continue
        picked.append({
            'id': f'ws_{x}_{z}',
            'sample': [x, z],
            'world_xz': list(t.coords[i]),
            'workable_samples': workable,
            'reach': {k: round(v, 2) for k, v in reach.items()},
        })
    return picked
