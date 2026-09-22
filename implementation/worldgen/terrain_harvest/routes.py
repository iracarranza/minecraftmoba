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

HALF_WIDTH = 1      # an approximate 3-wide corridor, not an exact cross-section
HEADROOM = 2        # a player's body, cleared at the WALKING height
MAX_STEP = 1        # the largest rise a walker takes without jumping repeatedly
SMOOTH = 7          # columns of median filter: removes noise, keeps the slope
ASSIMILATE = 2      # deviation the ground is nudged to meet the profile
FILL = block('coarse_dirt')      # what a small hollow is made up with
BRIDGE = block('oak_planks')     # only where terrain genuinely cannot carry a walker


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


def raw_profile(chunks, centreline):
    """The terrain height under each centreline column, or None where unknown."""
    out = []
    for x, z in centreline:
        top = column(chunks, x, z)
        out.append(None if top is None else top[1])
    return out


def median_filter(values, window=SMOOTH):
    """Remove terrain NOISE while keeping terrain SHAPE.

    Bounding the step size is not enough on its own. A one-block step is
    climbable but still a jump, so ground that wobbles by a block every column
    -- which is most natural ground -- would hand a walker a jump every column,
    which is worse than the map has today. A median takes out the wobble and
    leaves a genuine slope untouched, because a slope is the median of itself.
    """
    half = window // 2
    out = []
    for i in range(len(values)):
        near = sorted(values[max(0, i - half):i + half + 1])
        out.append(near[len(near) // 2])
    return out


def walkable_profile(raw, max_step=MAX_STEP):
    """A height sequence that never rises or falls more than a walker can take.

    This is the whole correction. The previous pass gave every column its own
    terrain height, so a Route inherited terrain NOISE as well as terrain shape:
    40.8% of adjacent columns in the frozen map differ by a block, and 7.6%
    differ by two or more, which cannot be climbed at all. A corridor that
    follows a cliff edge laid path blocks down the face.

    A profile is fitted instead: the ground is median-filtered so its noise goes
    and its shape stays, then step-limited so it never rises more than
    `max_step`. Both halves are needed -- limiting alone still permits a jump at
    every single column, which is worse than the map has today. Where the ground
    then deviates from it, that deviation is the signal for how much work the
    Route needs -- which is where worn, assimilated and constructed come from,
    rather than being imposed as categories.

    Unknown columns are carried through rather than guessed at.
    """
    known = [y for y in raw if y is not None]
    if not known:
        return list(raw)
    profile = median_filter([known[0] if y is None else y for y in raw])
    for _ in range(2):                      # forward then backward, twice
        for i in range(1, len(profile)):
            profile[i] = clamp_step(profile[i - 1], profile[i], max_step)
        for i in range(len(profile) - 2, -1, -1):
            profile[i] = clamp_step(profile[i + 1], profile[i], max_step)
    return profile


def clamp_step(previous, wanted, max_step):
    if wanted > previous + max_step:
        return previous + max_step
    if wanted < previous - max_step:
        return previous - max_step
    return wanted


def treatment(deviation):
    """How much intervention this column needs, from a number already computed."""
    if deviation == 0:
        return 'worn'
    if abs(deviation) <= ASSIMILATE:
        return 'assimilated'
    return 'constructed'


def carve(editor, chunks, centreline):
    """Fit a walkable corridor to the terrain. Returns per-segment statistics."""
    raw = raw_profile(chunks, centreline)
    profile = walkable_profile(raw)
    stats = {'surfaced': 0, 'bridged': 0, 'filled': 0, 'shaved': 0,
             'unwritable_columns': 0, 'worn': 0, 'assimilated': 0, 'constructed': 0,
             'cleared': 0}
    # The acceptance measure, recorded where it is actually known.
    #
    # Counting steps between adjacent route COLUMNS over-reports, because two
    # corridor segments passing each other on a hillside are adjacent without
    # anyone ever stepping between them. The walk a player takes is along the
    # centreline, so that is what is measured here.
    walk = [abs(b - a) for a, b in zip(profile, profile[1:])]
    stats['walk_pairs'] = len(walk)
    stats['walk_jumps'] = sum(1 for d in walk if d >= 1)
    stats['walk_unclimbable'] = sum(1 for d in walk if d >= 2)
    stats['walk_worst_step'] = max(walk) if walk else 0

    for i, (x, z) in enumerate(centreline):
        y = profile[i]
        # Width drifts rather than being an exact cross-section every step, so
        # the corridor reads as a trail fitted to the ground instead of a
        # continuous replacement surface.
        half = HALF_WIDTH if (i % 7) else 0
        for dx in range(-half, half + 1):
            for dz in range(-half, half + 1):
                cx, cz = x + dx, z + dz
                top = column(chunks, cx, cz)
                if top is None:
                    stats['unwritable_columns'] += 1
                    continue
                name, ty = top
                deviation = y - ty
                stats[treatment(deviation)] += 1

                if name in WATER:
                    # Constructed only where the crossing actually is water.
                    editor.set(cx, y, cz, BRIDGE)
                    stats['bridged'] += 1
                elif deviation > 0:
                    # A hollow: make it up with natural material rather than
                    # laying path into the pit.
                    for fy in range(ty + 1, y + 1):
                        editor.set(cx, fy, cz, FILL)
                    stats['filled'] += 1
                elif deviation < 0:
                    # A protrusion: take it down to the walking height.
                    for sy in range(y + 1, ty + 1):
                        editor.set(cx, sy, cz, AIR)
                    stats['shaved'] += 1

                # Surface selectively. On ground the profile already matches,
                # the terrain IS the Route and only needs enough treatment to
                # stay legible; every column surfaced is what made the old
                # corridor look built.
                if name not in WATER and (deviation != 0 or (i + dx + dz) % 3 == 0):
                    editor.set(cx, y, cz, SURFACE)
                    stats['surfaced'] += 1

                # Headroom at the WALKING height, not three above each column's
                # own top. That is what cut stepped notches through slopes and
                # bulldozed every tree the centreline passed near.
                for dy in range(1, HEADROOM + 1):
                    editor.set(cx, y + dy, cz, AIR)
                    stats['cleared'] += 1
    return stats


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
    profiles = frontier.get('profiles', frontier)
    cfg = profiles[a.profile]['finalists'][a.rank]['configuration']
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
