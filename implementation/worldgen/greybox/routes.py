"""Terrain-aware, curved provisional corridors; never physical Route surfaces."""

from __future__ import annotations

import heapq
import math

from successor.config import CELL_SIZE, NORTH_HOME, NX, NZ, SOUTH_HOME
from successor.grid import hash01, index_to_world, inside, world_to_index

DIRECTIONS = ((-1, -1), (0, -1), (1, -1), (-1, 0), (1, 0), (-1, 1), (0, 1), (1, 1))


def _astar(terrain, start, goal):
    sx, sz = world_to_index(*start)
    gx, gz = world_to_index(*goal)
    start_state = (sz * NX + sx, -1)
    queue = [(0.0, start_state)]
    cost = {start_state: 0.0}
    previous = {}
    final = None
    while queue:
        _, state = heapq.heappop(queue)
        current, prior_direction = state
        iz, ix = divmod(current, NX)
        if (ix, iz) == (gx, gz):
            final = state
            break
        for direction, (dx, dz) in enumerate(DIRECTIONS):
            qx, qz = ix + dx, iz + dz
            if not inside(qx, qz):
                continue
            q = qz * NX + qx
            kind = terrain["terrain"][q]
            if kind == "ocean":
                continue
            distance = CELL_SIZE * (math.sqrt(2) if dx and dz else 1)
            rise = abs(terrain["heights"][q] - terrain["heights"][current])
            surface = {"coast": 1.08, "forest": 1.09, "foothill": 1.05, "scree": 1.27, "rock": 1.18, "snow": 1.16}.get(kind, 1.0)
            turn = 0.0
            if prior_direction >= 0:
                a = DIRECTIONS[prior_direction]
                dot = (a[0] * dx + a[1] * dz) / ((math.hypot(*a) * math.hypot(dx, dz)))
                turn = (1 - dot) * 2.8
            next_cost = cost[state] + distance * surface * (1 + min(2.4, rise * .18)) + turn
            next_state = (q, direction)
            if next_cost < cost.get(next_state, math.inf):
                cost[next_state] = next_cost
                previous[next_state] = state
                heuristic = math.hypot((qx - gx) * CELL_SIZE, (qz - gz) * CELL_SIZE)
                heapq.heappush(queue, (next_cost + heuristic, next_state))
    if final is None:
        return [start]
    cells = []
    while final != start_state:
        cells.append(final[0])
        final = previous[final]
    cells.append(start_state[0])
    cells.reverse()
    return [index_to_world(i % NX, i // NX) for i in cells]


def _chaikin(points, iterations=2):
    points = [(float(x), float(z)) for x, z in points]
    for _ in range(iterations):
        output = [points[0]]
        for a, b in zip(points, points[1:]):
            output.extend(((a[0] * .75 + b[0] * .25, a[1] * .75 + b[1] * .25), (a[0] * .25 + b[0] * .75, a[1] * .25 + b[1] * .75)))
        output.append(points[-1])
        points = output
    return points


def _resample(points, spacing=3.0):
    output = [points[0]]
    carry = 0.0
    for a, b in zip(points, points[1:]):
        dx, dz = b[0] - a[0], b[1] - a[1]
        length = math.hypot(dx, dz)
        if not length:
            continue
        position = spacing - carry
        while position < length:
            t = position / length
            output.append((a[0] + dx * t, a[1] + dz * t))
            position += spacing
        carry = max(0.0, length - (position - spacing))
    output.append(points[-1])
    rounded = []
    for point in output:
        value = (round(point[0]), round(point[1]))
        if not rounded or value != rounded[-1]:
            rounded.append(value)
    return rounded


def _select_targets(team, ecology):
    sign = -1 if team == "north" else 1
    items = [x for x in ecology["instances"] if x["category"] in ("settlement", "major_poi") and x["pos"][1] * sign >= -80]
    west = min(items, key=lambda x: x["pos"][0])
    east = max(items, key=lambda x: x["pos"][0])
    center = min(items, key=lambda x: abs(x["pos"][0]) + abs(x["pos"][1]))
    targets = [tuple(west["pos"]), tuple(center["pos"]), tuple(east["pos"])]
    if len(set(targets)) < 3:
        for item in sorted(items, key=lambda x: abs(x["pos"][1])):
            if tuple(item["pos"]) not in targets:
                targets[1] = tuple(item["pos"])
                break
    return targets


def _guidepoints(terrain, start, target, key):
    dx, dz = target[0] - start[0], target[1] - start[1]
    length = math.hypot(dx, dz)
    if length < 110:
        return []
    normal = (-dz / length, dx / length)
    fractions = (.22, .45, .7, .86) if length >= 420 else (.27, .54, .78) if length >= 260 else (.38, .68)
    output = []
    side = -1 if hash01(key, 0, 19) < .5 else 1
    for index, fraction in enumerate(fractions):
        preferred = side * (24 + 18 * math.sin(math.pi * fraction) + 9 * hash01(key, index, 17))
        candidates = []
        for delta in range(-25, 26, 5):
            offset = preferred + delta
            x = round((start[0] + dx * fraction + normal[0] * offset) / 5) * 5
            z = round((start[1] + dz * fraction + normal[1] * offset) / 5) * 5
            ix, iz = world_to_index(x, z)
            if not inside(ix, iz):
                continue
            i = iz * NX + ix
            if terrain["terrain"][i] == "ocean":
                continue
            nearby = []
            for qx, qz in ((ix - 1, iz), (ix + 1, iz), (ix, iz - 1), (ix, iz + 1)):
                if inside(qx, qz):
                    nearby.append(abs(terrain["heights"][qz * NX + qx] - terrain["heights"][i]))
            surface = {"rock": 4, "scree": 5, "snow": 3, "forest": 1.5, "coast": 2}.get(terrain["terrain"][i], 0)
            candidates.append((sum(nearby) + surface + abs(delta) * .03, (x, z)))
        if candidates:
            output.append(min(candidates)[1])
    return output


def generate_routes(seed, terrain, ecology):
    branches = []
    mask = [False] * (NX * NZ)
    for team, home in (("north", NORTH_HOME), ("south", SOUTH_HOME)):
        sign = -1 if team == "north" else 1
        junction = (round(((seed % 37) - 18) / 5) * 5, round((home[1] - sign * 72) / 5) * 5)
        trunk = _astar(terrain, home, junction)
        for branch_index, target in enumerate(_select_targets(team, ecology)):
            guides = _guidepoints(terrain, junction, target, seed ^ (branch_index + 1) ^ (0xA0 if team == "north" else 0xB0))
            raw = trunk[:-1]
            current = junction
            for waypoint in guides + [target]:
                leg = _astar(terrain, current, waypoint)
                raw += leg if not raw else leg[1:]
                current = waypoint
            centerline = _resample(_chaikin(raw, 3), 3.0)
            centerline[0] = tuple(home)
            centerline[-1] = tuple(target)
            for x, z in centerline:
                ix, iz = world_to_index(x, z)
                if inside(ix, iz):
                    mask[iz * NX + ix] = True
            length = sum(math.hypot(b[0] - a[0], b[1] - a[1]) for a, b in zip(centerline, centerline[1:]))
            branches.append({
                "id": f"{team}-route-{branch_index + 1}",
                "team": team,
                "branch": branch_index + 1,
                "status": "provisional_curved_analytical_corridor",
                "home": list(home),
                "junction": list(junction),
                "target": list(target),
                "centerline": [list(x) for x in centerline],
                "length_blocks": round(length),
            })
    return {
        "branches": branches,
        "mask": mask,
        "visual_language_finalized": False,
        "physical_surface": False,
        "speed_bonus": 0.0,
        "locomotion_exhaustion_reduction": .10,
    }
