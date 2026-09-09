"""Multiscale geography and hydrological terrain, without physicalized water."""

from __future__ import annotations

import math
import random
import heapq
from collections import defaultdict

from successor.config import CELL_SIZE, NX, NZ, SEA_LEVEL, TOPOLOGIES
from successor.grid import fractal, gaussian, hash01, index_to_world, inside, smoothstep


def _oriented_gaussian(x, z, cx, cz, along, across, angle, amplitude):
    ca, sa = math.cos(angle), math.sin(angle)
    dx, dz = x - cx, z - cz
    u = (dx * ca + dz * sa) / along
    v = (-dx * sa + dz * ca) / across
    return amplitude * math.exp(-(u * u + v * v))


def _topology_relief(name: str, seed: int, x: float, z: float) -> float:
    """Warped family grammar. Primitives overlap; none is a visible region mask."""
    wx = x + 24 * fractal(seed + 701, x, z, (210, 92), (1, .32))
    wz = z + 31 * fractal(seed + 709, x, z, (240, 105), (1, .35))
    g = lambda cx, cz, sx, sz, a: gaussian(wx, wz, cx, cz, sx, sz) * a
    if name == "twin_massif":
        return g(-315, -155, 82, 118, 17) + g(-322, 145, 90, 122, 16) - g(-282, -4, 92, 70, 9)
    if name == "split_spine":
        return g(-330, -25, 68, 325, 16) + g(-375, -205, 52, 78, 9) - g(-292, 80, 64, 95, 11)
    if name == "horseshoe":
        return g(-324, -145, 92, 102, 15) + g(-342, 145, 92, 106, 16) + g(-382, 0, 64, 188, 10) - g(-305, 0, 88, 105, 12)
    if name == "broken_peaks":
        return sum(g(-304 - (i % 2) * 45, cz, 54, 70, 10 + (i % 3) * 2) for i, cz in enumerate((-235, -120, 0, 118, 230))) - g(-286, 52, 82, 52, 6)
    if name == "long_ridge":
        return g(-348, 0, 68, 350, 17) - sum(g(-300, cz, 88, 48, 7) for cz in (-188, -25, 158))
    if name == "highland_ravines":
        c1 = 78 * math.sin((wx + seed % 97) / 86) - 58
        c2 = 86 * math.sin((wx - seed % 131) / 95) + 112
        return g(-342, 0, 124, 325, 14) - 7 * math.exp(-((wz - c1) / 25) ** 2) - 7 * math.exp(-((wz - c2) / 24) ** 2)
    if name == "offset_basin":
        return g(-350, -82, 116, 220, 17) + g(-292, 190, 78, 108, 12) - g(-344, -52, 76, 92, 14)
    return g(-344, 0, 90, 330, 15) + sum(g(-390, cz + 42, 55, 45, 6) - g(-286, cz, 96, 34, 7) for cz in (-238, -120, 0, 126, 238))


def _coast_profile(seed: int):
    rng = random.Random(seed ^ 0xC0457)
    phase = rng.uniform(-150, 150)
    offset = rng.uniform(-18, 18)
    profile = []
    for iz in range(NZ):
        _, z = index_to_world(0, iz)
        warp = 24 * math.sin((z + phase) / 154) + 9 * math.sin((z - phase * .4) / 53)
        warp += 18 * fractal(seed + 41, 0, z, (280, 115, 44), (1, .42, .14))
        warp += 5 * math.sin((z + phase * .7) / 27) + 4 * fractal(seed + 43, 0, z, (36, 17), (1, .28))
        x = 298 + offset + warp
        exposure = fractal(seed + 53, x, z, (190, 68, 26), (1, .45, .18))
        width = round(9 + 11 * (exposure + 1) / 2 + 8 * max(0, fractal(seed + 59, x, z, (74, 31), (1, .3))))
        if exposure > .62:
            kind = "rocky_shore"
            width = max(5, round(width * .45))
        elif exposure < -.52:
            kind = "coastal_flat"
            width = round(width * 1.45)
        elif abs(fractal(seed + 61, x, z, (92, 37), (1, .32))) > .72:
            kind = "steep_shore"
            width = max(4, round(width * .55))
        else:
            kind = "beach"
        profile.append({"boundary_x": round(x), "transition_width": width, "shore_type": kind})
    return profile


def generate_terrain(seed: int) -> dict:
    rng = random.Random(seed)
    topology = TOPOLOGIES[seed % len(TOPOLOGIES)]
    coast_profile = _coast_profile(seed)
    events = []
    for _ in range(15):
        events.append((rng.uniform(-390, 230), rng.uniform(-390, 390), rng.uniform(32, 95), rng.uniform(22, 70), rng.uniform(-math.pi, math.pi), rng.uniform(-5.5, 7.0)))
    ravines = [(rng.uniform(-385, -245), rng.uniform(-250, 250), rng.uniform(-.9, .9), rng.uniform(4.0, 7.5)) for _ in range(2)]
    drainage_axes = [(rng.uniform(-300, 300), rng.uniform(24, 68), rng.uniform(90, 175), rng.uniform(-math.pi, math.pi), rng.uniform(24, 43), rng.uniform(1.7, 3.4)) for _ in range(3)]

    heights = []
    raw_westness = []
    mountain = []
    for iz in range(NZ):
        _, z = index_to_world(0, iz)
        coast_x = coast_profile[iz]["boundary_x"]
        mountain_edge = -112 + 25 * fractal(seed + 17, 0, z, (260, 105, 40), (1, .38, .13)) + 10 * math.sin((z + seed % 211) / 79)
        for ix in range(NX):
            x, _ = index_to_world(ix, iz)
            domain_x = x + 20 * fractal(seed + 83, x, z, (210, 78), (1, .3))
            domain_z = z + 24 * fractal(seed + 89, x, z, (230, 88), (1, .32))
            w = max(0.0, min(1.0, (mountain_edge - domain_x) / 310.0))
            w = smoothstep(w)
            base = 64.0
            base += 2.0 * fractal(seed, domain_x, domain_z, (230, 96, 39, 17), (1, .55, .25, .11))
            base += 2.05 * fractal(seed + 11, domain_x, domain_z, (38, 18, 9), (1, .42, .16))
            base += 1.3 * math.sin((domain_x + domain_z * .37 + seed % 101) / 74)
            relief = 4.0 * w + 20.0 * w ** 1.7
            relief += _topology_relief(topology, seed, domain_x, domain_z) * (.25 + .75 * w)
            relief += (4.8 * fractal(seed + 101, domain_x, domain_z, (145, 66, 29, 13), (1, .55, .26, .10))) * w ** 1.15
            for cx, cz, along, across, angle, amp in events:
                relief += _oriented_gaussian(domain_x, domain_z, cx, cz, along, across, angle, amp) * (.35 + .65 * w if cx < -100 else 1)
            for rx, rz, phase, depth in ravines:
                center = rz + 42 * math.sin((domain_x - rx) / 72 + phase)
                relief -= depth * math.exp(-((domain_z - center) / 18) ** 2) * smoothstep(max(0, min(1, (-125 - domain_x) / 220)))
            for origin_z, amplitude, wavelength, phase, width, depth in drainage_axes:
                center = origin_z + amplitude * math.sin((domain_x + 405) / wavelength + phase) + 12 * math.sin((domain_x - seed % 83) / 47 + phase * .3)
                relief -= depth * math.exp(-((domain_z - center) / width) ** 2) * (.55 + .45 * max(0, min(1, (coast_x - domain_x) / 520)))
            coast_distance = coast_x - x
            if 0 <= coast_distance < 90:
                coastal_target = SEA_LEVEL + 1 + 1.8 * fractal(seed + 117, x, z, (90, 35), (1, .25))
                blend = smoothstep((90 - coast_distance) / 90)
                base = base * (1 - .68 * blend) + coastal_target * (.68 * blend)
                relief *= 1 - .72 * blend
            height = round(base + relief)
            if x > coast_x:
                depth = 5 + min(9, round((x - coast_x) / 34))
                height = min(height, SEA_LEVEL - depth)
            heights.append(max(49, min(118, height)))
            raw_westness.append(w)
            entry_threshold = .018 + .025 * max(-.5, fractal(seed + 27, domain_x, domain_z, (120, 47), (1, .3)))
            mountain.append(w > entry_threshold)

    slopes = _slopes(heights)
    terrain = []
    coast_zones = []
    for i, height in enumerate(heights):
        iz, ix = divmod(i, NX)
        x, z = index_to_world(ix, iz)
        profile = coast_profile[iz]
        d = profile["boundary_x"] - x
        w = raw_westness[i]
        local = fractal(seed + 307, x, z, (185, 73, 31, 14), (1, .48, .22, .1))
        if d < 0:
            kind = "ocean"
            coast_zone = "ocean"
        elif d <= profile["transition_width"]:
            kind = "coast"
            coast_zone = profile["shore_type"]
        elif d <= profile["transition_width"] + (35 if profile["shore_type"] == "coastal_flat" else 18):
            kind = "open"
            coast_zone = "inland_coastal_transition"
        elif mountain[i]:
            cold_line = 91 + 3 * fractal(seed + 313, x, z, (145, 55), (1, .28))
            if height >= cold_line and w > .45 and local > -.55:
                kind = "snow"
            elif slopes[i] >= 2.6 and local > -.35:
                kind = "rock"
            elif slopes[i] >= 1.55 and local < .2:
                kind = "scree"
            elif w < .31:
                moisture = fractal(seed + 331, x, z, (155, 64, 27), (1, .42, .16))
                if slopes[i] < 1.1 and moisture > .25:
                    kind = "forest"
                elif slopes[i] < 1.0 and local < .25:
                    kind = "open"
                else:
                    kind = "foothill"
            elif height < 78 and local < .48:
                kind = "foothill"
            else:
                kind = "rock" if local > .52 else "foothill" if local < -.35 else "scree"
            coast_zone = "inland"
        else:
            moisture = local + .34 * fractal(seed + 331, x, z, (68, 27, 12), (1, .4, .14))
            terrain_response = .20 * max(0, 1.7 - slopes[i]) - .16 * max(0, height - 76)
            clearing = fractal(seed + 337, x, z, (48, 21), (1, .28)) > .70
            home_distance = min(math.hypot(x, z + 365), math.hypot(x, z - 365))
            homeland_pressure = .72 * math.exp(-((home_distance / 72) ** 2))
            kind = "forest" if moisture + terrain_response - homeland_pressure > .43 and not clearing else "open"
            coast_zone = "inland"
        terrain.append(kind)
        coast_zones.append(coast_zone)

    hydrology = _hydrological_terrain(seed, heights, terrain)
    markers = _mountain_markers(seed, heights, mountain, slopes, ravines, hydrology)
    return {
        "topology_family": topology,
        "heights": heights,
        "terrain": terrain,
        "mountain_mask": mountain,
        "coast_boundary": [x["boundary_x"] for x in coast_profile],
        "coast_profile": coast_profile,
        "coast_zones": coast_zones,
        "hydrology": hydrology["features"],
        "hydrological_terrain": {k: v for k, v in hydrology.items() if k != "features"},
        "mountain_feature_markers": markers,
        "representation_note": "Greybox terrain only: channels, basins, coast mouths, and catchments are analytical opportunities; no water or lava was stamped into terrain.",
    }


def _slopes(heights):
    output = [0.0] * len(heights)
    for i, h in enumerate(heights):
        iz, ix = divmod(i, NX)
        ds = []
        for qx, qz in ((ix - 1, iz), (ix + 1, iz), (ix, iz - 1), (ix, iz + 1)):
            if inside(qx, qz):
                ds.append(abs(heights[qz * NX + qx] - h))
        output[i] = sum(ds) / max(1, len(ds))
    return output


def _hydrological_terrain(seed, heights, terrain):
    """Priority-flood spill relationships over unchanged source elevations."""
    downstream = [None] * len(heights)
    land = [k != "ocean" for k in terrain]
    filled = list(heights)
    visited = [False] * len(heights)
    queue = []
    for i, is_land in enumerate(land):
        if not is_land:
            visited[i] = True
            x, z = index_to_world(i % NX, i // NX)
            heapq.heappush(queue, (filled[i], -x, abs(z), i))
    while queue:
        level, _, _, current = heapq.heappop(queue)
        iz, ix = divmod(current, NX)
        for qz in range(max(0, iz - 1), min(NZ, iz + 2)):
            for qx in range(max(0, ix - 1), min(NX, ix + 2)):
                q = qz * NX + qx
                if visited[q]:
                    continue
                visited[q] = True
                filled[q] = max(heights[q], level)
                downstream[q] = current
                x, z = index_to_world(q % NX, q // NX)
                heapq.heappush(queue, (filled[q], -x, abs(z), q))
    accumulation = [1 if land[i] else 0 for i in range(len(heights))]
    for i in sorted((i for i in range(len(heights)) if land[i]), key=lambda q: (filled[q], heights[q]), reverse=True):
        if downstream[i] is not None:
            accumulation[downstream[i]] += accumulation[i]

    upstream = defaultdict(list)
    for i, q in enumerate(downstream):
        if q is not None:
            upstream[q].append(i)
    network_threshold = max(20, sorted((accumulation[i] for i in range(len(heights)) if land[i]))[int(sum(land) * .96)])
    channel_cells = {i for i in range(len(heights)) if land[i] and accumulation[i] >= network_threshold}
    junctions = [i for i in channel_cells if sum(q in channel_cells for q in upstream[i]) >= 2]

    candidates = [i for i in channel_cells if heights[i] >= 68 and index_to_world(i % NX, i // NX)[0] < 210]
    candidates.sort(key=lambda i: (accumulation[i] * max(1, heights[i] - 61), hash01(seed, i, 9)), reverse=True)
    features = []
    claimed_sources = []
    for source in candidates:
        p = index_to_world(source % NX, source // NX)
        if any(math.hypot(p[0] - q[0], p[1] - q[1]) < 95 for q in claimed_sources):
            continue
        path = [source]
        while downstream[path[-1]] is not None and len(path) < 260:
            path.append(downstream[path[-1]])
            if terrain[path[-1]] == "ocean":
                break
        if len(path) < 10:
            continue
        points = [index_to_world(i % NX, i // NX) for i in path]
        length = sum(math.hypot(b[0] - a[0], b[1] - a[1]) for a, b in zip(points, points[1:]))
        direct = math.hypot(points[-1][0] - points[0][0], points[-1][1] - points[0][1])
        features.append({
            "kind": "potential_channel",
            "source": list(points[0]),
            "mouth": list(points[-1]),
            "path": [list(x) for x in points],
            "length_blocks": round(length),
            "sinuosity": round(length / max(CELL_SIZE, direct), 3),
            "catchment_cells": accumulation[source],
            "terminus": "coastal_outlet" if terrain[path[-1]] == "ocean" else "local_minimum",
            "downhill_fraction": round(sum(heights[b] <= heights[a] for a, b in zip(path, path[1:])) / max(1, len(path) - 1), 3),
            "maximum_spill_rise_y": max((max(0, heights[b] - heights[a]) for a, b in zip(path, path[1:])), default=0),
            "spill_cuts": 0,
            "connected": True,
            "stamped": False,
            "status": "analytical terrain opportunity; not finished water",
        })
        claimed_sources.append(p)
        if len(features) >= 5:
            break

    basins = []
    depression = {i for i in range(len(heights)) if land[i] and filled[i] > heights[i]}
    while depression:
        start = depression.pop()
        stack, cells = [start], [start]
        while stack:
            i = stack.pop()
            iz, ix = divmod(i, NX)
            for qx, qz in ((ix - 1, iz), (ix + 1, iz), (ix, iz - 1), (ix, iz + 1)):
                q = qz * NX + qx
                if inside(qx, qz) and q in depression:
                    depression.remove(q)
                    stack.append(q)
                    cells.append(q)
        if len(cells) >= 3:
            floor = min(cells, key=lambda q: heights[q])
            basins.append({"pos": list(index_to_world(floor % NX, floor // NX)), "floor_y": heights[floor], "spill_y": max(filled[q] for q in cells), "maximum_fill_depth_y": max(filled[q] - heights[q] for q in cells), "depression_cells": len(cells), "contributing_cells_at_floor": accumulation[floor], "status": "priority-flood collection/spill relationship; fill behavior unresolved"})
    basins.sort(key=lambda x: (x["depression_cells"] * max(1, x["maximum_fill_depth_y"])), reverse=True)
    basins = basins[:8]
    coastal_outlets = sum(1 for i, q in enumerate(downstream) if q is not None and terrain[q] == "ocean" and accumulation[i] >= network_threshold)
    return {
        "representation": "D8 downhill flow, accumulation, terminal catchments, local minima and possible channels over unmodified terrain",
        "features": features,
        "network_threshold_cells": network_threshold,
        "potential_channel_cells": len(channel_cells),
        "tributary_junctions": len(junctions),
        "local_minima": sum(land[i] and all(heights[i] <= heights[qz * NX + qx] for qz in range(max(0, i // NX - 1), min(NZ, i // NX + 2)) for qx in range(max(0, i % NX - 1), min(NX, i % NX + 2)) if qz * NX + qx != i) for i in range(len(heights))),
        "basins": basins,
        "coastal_outlets": coastal_outlets,
        "ocean_draining_fraction": 1.0,
        "largest_catchment_cells": max(accumulation, default=0),
        "depression_cell_fraction": round(sum(land[i] and filled[i] > heights[i] for i in range(len(heights))) / max(1, sum(land)), 4),
        "channel_cells": [list(index_to_world(i % NX, i // NX)) for i in sorted(channel_cells)],
        "junction_markers": [list(index_to_world(i % NX, i // NX)) for i in junctions[:30]],
    }


def _mountain_markers(seed, heights, mountain, slopes, ravines, hydrology):
    cells = [i for i, value in enumerate(mountain) if value]
    def select(order, count, separation=80):
        output = []
        for i in order:
            point = index_to_world(i % NX, i // NX)
            if all(math.hypot(point[0] - q[0], point[1] - q[1]) >= separation for q in output):
                output.append(point)
            if len(output) == count:
                break
        return output
    ridges = select(sorted(cells, key=lambda i: heights[i] + slopes[i] * 1.5, reverse=True), 4)
    valleys = select(sorted((i for i in cells if index_to_world(i % NX, i // NX)[0] < -220), key=lambda i: heights[i]), 3)
    approaches = select(sorted((i for i in cells if -190 <= index_to_world(i % NX, i // NX)[0] <= -90), key=lambda i: heights[i]), 3, 95)
    markers = []
    for kind, points in (("ridge_or_peak", ridges), ("valley_or_saddle", valleys), ("foothill_approach", approaches)):
        for point in points:
            i = round((point[1] + 520) / 5) * NX + round((point[0] + 420) / 5)
            markers.append({"kind": kind, "pos": list(point), "elevation": heights[i], "status": "descriptive analytical marker"})
    for rx, rz, _, _ in ravines:
        markers.append({"kind": "ravine_or_cut", "pos": [round(rx), round(rz)], "status": "generated negative-relief axis"})
    for basin in hydrology["basins"][:3]:
        markers.append({"kind": "catchment_basin", "pos": basin["pos"], "elevation": basin["floor_y"], "status": "hydrological terrain marker"})
    return markers
