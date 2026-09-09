"""Provisional analytical Route topology; deliberately not a visual vocabulary."""

from __future__ import annotations

import heapq
import math

from .config import NORTH_HOME, NX, NZ, SOUTH_HOME
from .grid import index_to_world, inside, world_to_index


def _astar(terrain: dict, start, goal) -> list[int]:
    sx, sz = world_to_index(*start); gx, gz = world_to_index(*goal)
    start_i, goal_i = sz * NX + sx, gz * NX + gx
    queue = [(0.0, start_i)]; cost = {start_i: 0.0}; prev = {}
    while queue:
        _, current = heapq.heappop(queue)
        if current == goal_i:
            break
        iz, ix = divmod(current, NX)
        for qx, qz in ((ix-1,iz),(ix+1,iz),(ix,iz-1),(ix,iz+1)):
            if not inside(qx,qz): continue
            q = qz * NX + qx; kind = terrain["terrain"][q]
            if kind == "ocean": continue
            step = 5 + abs(terrain["heights"][q] - terrain["heights"][current]) * 2.2
            step *= {"water": 2.2, "scree": 1.45, "rock": 1.25, "forest": 1.12}.get(kind, 1.0)
            nc = cost[current] + step
            if nc < cost.get(q, 1e99):
                cost[q] = nc; prev[q] = current
                wx, wz = index_to_world(qx, qz)
                heuristic = math.hypot(wx-goal[0], wz-goal[1])
                heapq.heappush(queue, (nc + heuristic, q))
    if goal_i not in prev:
        return [start_i]
    path = [goal_i]
    while path[-1] != start_i: path.append(prev[path[-1]])
    return list(reversed(path))


def _select_targets(team: str, ecology: dict) -> list[tuple[int,int]]:
    sign = -1 if team == "north" else 1
    items = [x for x in ecology["instances"] if x["category"] in ("settlement","major_poi") and x["pos"][1] * sign >= -80]
    west = min(items, key=lambda x: x["pos"][0])
    east = max(items, key=lambda x: x["pos"][0])
    center = min(items, key=lambda x: abs(x["pos"][0]) + abs(x["pos"][1]))
    targets = [tuple(west["pos"]), tuple(center["pos"]), tuple(east["pos"])]
    # Preserve three branches even if a compound opportunity was selected twice.
    if len(set(targets)) < 3:
        alternatives = sorted(items, key=lambda x: abs(x["pos"][1]))
        for item in alternatives:
            if tuple(item["pos"]) not in targets:
                targets[1] = tuple(item["pos"]); break
    return targets


def generate_routes(seed: int, terrain: dict, ecology: dict) -> dict:
    branches, mask = [], [False] * (NX * NZ)
    for team, home in (("north", NORTH_HOME), ("south", SOUTH_HOME)):
        sign = -1 if team == "north" else 1
        junction = ((seed % 37) - 18, home[1] - sign * 72)
        trunk = _astar(terrain, home, junction)
        for branch_index, target in enumerate(_select_targets(team, ecology)):
            path = trunk[:-1] + _astar(terrain, junction, target)
            for i in path: mask[i] = True
            points = [list(index_to_world(i % NX, i // NX)) for i in path]
            branches.append({"id": f"{team}-route-{branch_index+1}", "team": team, "branch": branch_index+1,
                "status": "provisional_topological_analytical", "home": list(home), "junction": list(junction), "target": list(target),
                "centerline": points, "length_blocks": max(0, len(points)-1)*5})
    return {"branches": branches, "mask": mask, "visual_language_finalized": False, "speed_bonus": 0.0, "locomotion_exhaustion_reduction": 0.10}
