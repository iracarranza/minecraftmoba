"""Find the ONE Giant Monster Lair socket, and measure its access from both teams.

Doctrine makes the Lair the exception, and the exception is narrow. Asymmetric
Wilderness terrain is allowed; one team having structurally privileged access to
the single indivisible shared objective is a genuine competitive concern. So
this module does two different jobs and keeps them apart:

  PHYSICAL VIABILITY   can this place host all three encounters at all?
  ACCESS PARITY        A_L = |R_N - R_S| / mean(R_N, R_S), from both Fountains.

Physical viability is a filter. Access parity is MEASURED AND REPORTED, and
deliberately not a filter, because no acceptable threshold has been established
and inventing one here would silently decide the one number where a wrong
constant does real competitive damage. Ranking prefers low A_L among sites that
are already viable, which is "minimise it" without pretending to know where the
line is.

What the sample grid cannot prove is stated rather than assumed. An eight-block
surface grid has no column data, so the Dragon's air volume is approximated by
how far the surrounding terrain stays below the rim -- an open basin reads
differently from a slot canyon -- and that approximation is reported as such.
"""
from __future__ import annotations

import math

from vanilla_search.task_a import Terrain, shortest


def _open_sky_proxy(t: Terrain, centre: int, radius: int) -> float:
    """How much of the neighbourhood sits at or below the site.

    A proxy, and named one. Terrain that rises steeply all around a point is a
    shaft, not an arena; terrain that stays low is open above. This cannot see
    overhangs, caves or actual sky, which is why the Dragon's volume is only
    ever reported as approximate.
    """
    x, z = t.xy(centre)
    here = t.v['height'][centre]
    seen = below = 0
    for dz in range(-radius, radius + 1):
        for dx in range(-radius, radius + 1):
            xx, zz = x + dx, z + dz
            if not (0 <= xx < t.w and 0 <= zz < t.h):
                continue
            seen += 1
            if t.v['height'][zz * t.w + xx] <= here + 8:
                below += 1
    return below / max(seen, 1)


def _approaches(t: Terrain, centre: int, radius: int) -> int:
    """How many of the eight compass directions arrive over passable ground."""
    x, z = t.xy(centre)
    count = 0
    for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1), (1, 1), (1, -1), (-1, 1), (-1, -1)):
        ok = True
        for step in range(1, radius + 1):
            xx, zz = x + dx * step, z + dz * step
            if not (0 <= xx < t.w and 0 <= zz < t.h):
                ok = False
                break
            i = zz * t.w + xx
            if t.v['water'][i] or t.grade[i] > 8:
                ok = False
                break
        count += ok
    return count


def viability(t: Terrain, centre: int, radius: int, min_approaches: int,
              min_usable: float, min_open: float) -> list[str]:
    """Why this cell cannot host the Lair, if it cannot."""
    cells = [i for i in t.cells(centre)] if radius == t.p.get('homeland_radius_samples') else None
    x, z = t.xy(centre)
    if not (radius <= x < t.w - radius and radius <= z < t.h - radius):
        return ['site is not wholly inside the sampled window']
    patch = [zz * t.w + xx
             for dz in range(-radius, radius + 1)
             for dx in range(-radius, radius + 1)
             for xx, zz in ((x + dx, z + dz),)]
    problems = []
    usable = sum(t.v['buildable'][i] and not t.v['water'][i] for i in patch) / len(patch)
    if usable < min_usable:
        problems.append(f'usable ground {usable:.2f} < {min_usable:.2f}: no room for '
                        f'the Giant encounter, players, building or retreat')
    open_sky = _open_sky_proxy(t, centre, radius * 2)
    if open_sky < min_open:
        problems.append(f'open-volume proxy {open_sky:.2f} < {min_open:.2f}: the '
                        f'surrounding terrain encloses the site, and the Dragon is '
                        f'the limiting volume case')
    approaches = _approaches(t, centre, radius)
    if approaches < min_approaches:
        problems.append(f'{approaches} practical approaches < {min_approaches}')
    return problems


def search(candidate, fountains, *, radius=6, min_approaches=3,
           min_usable=0.55, min_open=0.75, keep=25):
    """Find viable Lair sockets and measure both teams' reach to each.

    `fountains` maps team to a [x, z] SAMPLE coordinate, the same anchor the
    objective siting uses. Returns every viable site with its A_L, so the
    distribution can be inspected before anyone chooses a threshold.
    """
    t = Terrain(candidate)
    homes = {team: xz[1] * t.w + xz[0] for team, xz in fountains.items()}
    costs = {team: shortest(t.adj, {i: 0.0})[0] for team, i in homes.items()}

    # Nowhere near either Homebase: the Lair is a Wilderness landmark, and a
    # compact opening Hinterland must not contain it.
    keep_out = radius * 2 + int(t.p.get('homeland_radius_samples', 4)) + 2
    considered = viable = 0
    sites = []
    for i in range(t.n):
        x, z = t.xy(i)
        if any(max(abs(x - t.xy(h)[0]), abs(z - t.xy(h)[1])) < keep_out for h in homes.values()):
            continue
        considered += 1
        problems = viability(t, i, radius, min_approaches, min_usable, min_open)
        if problems:
            continue
        reach = {team: costs[team].get(i, math.inf) for team in homes}
        if any(not math.isfinite(v) for v in reach.values()):
            continue
        viable += 1
        mean = sum(reach.values()) / len(reach)
        asymmetry = abs(reach['north'] - reach['south']) / mean if mean else None
        sites.append({
            'sample': [x, z],
            'world_xz': list(t.coords[i]),
            'reach': {k: round(v, 2) for k, v in reach.items()},
            'access_asymmetry': round(asymmetry, 4) if asymmetry is not None else None,
            'usable_fraction': round(sum(
                t.v['buildable'][zz * t.w + xx] and not t.v['water'][zz * t.w + xx]
                for dz in range(-radius, radius + 1)
                for dx in range(-radius, radius + 1)
                for xx, zz in ((x + dx, z + dz),)) / ((2 * radius + 1) ** 2), 4),
            'open_volume_proxy': round(_open_sky_proxy(t, i, radius * 2), 4),
            'approaches': _approaches(t, i, radius),
        })
    # Minimise disparity among the already-viable. Not a threshold.
    sites.sort(key=lambda s: (s['access_asymmetry'] if s['access_asymmetry'] is not None else 9,
                              -s['usable_fraction']))
    return {
        'considered': considered,
        'viable': viable,
        'parameters': {'radius_samples': radius, 'min_approaches': min_approaches,
                       'min_usable_fraction': min_usable,
                       'min_open_volume_proxy': min_open},
        'approximations': {
            'dragon_air_volume': 'approximated by an open-sky proxy over the height '
                                 'grid; an 8-block surface sample has no column data, '
                                 'so actual encounter volume is UNVERIFIED here',
        },
        'sites': sites[:keep],
        'selected': sites[0] if sites else None,
    }
