"""Terrain and hydrology generation using P2D's vocabulary, not its silhouette."""

from __future__ import annotations

import math
import random

from .config import NX, NZ, SEA_LEVEL, TOPOLOGIES
from .grid import fractal, gaussian, hash01, index_to_world, inside


def _topology_relief(name: str, seed: int, x: float, z: float) -> float:
    g = lambda cx, cz, sx, sz, a: gaussian(x, z, cx, cz, sx, sz) * a
    jitter = (hash01(seed, round(x / 35), round(z / 35)) - .5) * 2
    if name == "twin_massif":
        return g(-315, -155, 75, 105, 19) + g(-320, 145, 82, 110, 17) - g(-285, 0, 95, 62, 10)
    if name == "split_spine":
        return g(-320, -20, 62, 310, 17) + g(-365, -205, 48, 75, 10) - g(-300, 78, 56, 88, 12)
    if name == "horseshoe":
        return g(-325, -135, 82, 85, 17) + g(-340, 140, 82, 88, 18) + g(-380, 0, 55, 175, 11) - g(-310, 0, 78, 92, 13)
    if name == "broken_peaks":
        return sum(g(-300 - (i % 2) * 55, cz, 45, 58, 12 + (i % 3) * 2) for i, cz in enumerate((-230, -115, 0, 115, 225))) - g(-295, 55, 72, 45, 7)
    if name == "long_ridge":
        return g(-345, 0, 58, 335, 20) - sum(g(-305, cz, 80, 38, 8) for cz in (-185, -25, 155))
    if name == "highland_ravines":
        center1 = 75 * math.sin((x + seed % 97) / 82) - 55
        center2 = 80 * math.sin((x - seed % 131) / 91) + 105
        return g(-340, 0, 112, 310, 15) - 9 * math.exp(-((z - center1) / 22) ** 2) - 8 * math.exp(-((z - center2) / 20) ** 2)
    if name == "offset_basin":
        return g(-345, -75, 105, 205, 19) + g(-290, 185, 65, 92, 14) - g(-345, -55, 62, 75, 16)
    return g(-340, 0, 78, 315, 17) + sum(g(-385, cz + 38, 48, 38, 7) - g(-295, cz, 86, 28, 8) for cz in (-235, -120, 0, 125, 235)) + jitter


def generate_terrain(seed: int) -> dict:
    rng = random.Random(seed)
    topology = TOPOLOGIES[seed % len(TOPOLOGIES)]
    coast_phase = rng.uniform(-110, 110)
    coast_offset = rng.uniform(-22, 22)
    heights: list[int] = []
    terrain: list[str] = []
    mountain: list[bool] = []
    coast_boundary: list[int] = []
    for iz in range(NZ):
        _, z = index_to_world(0, iz)
        coast_x = 300 + coast_offset + 34 * math.sin((z + coast_phase) / 145) + 16 * fractal(seed + 41, 0, z, (260, 105, 48), (1, .35, .12))
        coast_boundary.append(round(coast_x))
        east_edge = -120 + 20 * math.sin((z + rng.random() * 3) / 175) + 13 * fractal(seed + 17, 0, z, (230, 90, 40), (1, .3, .1))
        for ix in range(NX):
            x, _ = index_to_world(ix, iz)
            westness = max(0.0, min(1.0, (east_edge - x) / 285.0))
            base = 64 + 2.4 * fractal(seed, x, z)
            # Broad broken transition, followed by stronger internal relief.
            relief = (5 * westness + 25 * westness ** 1.65) if westness > 0 else 0
            relief += _topology_relief(topology, seed, x, z) * westness
            relief += 4.2 * fractal(seed + 101, x, z, (105, 42, 19), (1, .45, .13)) * westness ** 1.25
            h = round(base + relief)
            is_mountain = westness > .04 and abs(z) < 390 + round(25 * fractal(seed + 29, x, z, (250,), (1,)))
            if x > coast_x:
                h = min(h, SEA_LEVEL - 5 - round(3 * max(0, (x - coast_x) / 100)))
                kind = "ocean"
            elif x > coast_x - 25 and h <= SEA_LEVEL + 4:
                kind = "coast"
            elif is_mountain and westness > .62 and h >= 91:
                kind = "snow"
            elif is_mountain and (westness > .24 or h > 77):
                slope_noise = fractal(seed + 211, x, z, (65, 27), (1, .25))
                kind = "scree" if slope_noise > .35 else "rock"
            elif is_mountain:
                kind = "foothill"
            else:
                moisture = fractal(seed + 307, x, z, (190, 72, 31), (1, .4, .16))
                kind = "forest" if moisture > .38 and abs(z) < 330 and x < 235 else "open"
            heights.append(max(48, min(119, h)))
            terrain.append(kind)
            mountain.append(is_mountain)

    hydrology = _generate_hydrology(seed, heights, terrain, mountain)
    return {
        "topology_family": topology,
        "heights": heights,
        "terrain": terrain,
        "mountain_mask": mountain,
        "coast_boundary": coast_boundary,
        "hydrology": hydrology,
        "mountain_feature_markers": _mountain_markers(heights, mountain),
    }


def _mountain_markers(heights: list[int], mountain: list[bool]) -> list[dict]:
    """Descriptive markers selected from actual height cells, not extra stamps."""
    cells=[i for i,m in enumerate(mountain) if m]
    def separated(order, count):
        out=[]
        for i in order:
            p=index_to_world(i%NX,i//NX)
            if all(math.hypot(p[0]-q[0],p[1]-q[1])>=85 for q in out): out.append(p)
            if len(out)>=count: break
        return out
    high=separated(sorted(cells,key=lambda i:heights[i],reverse=True),3)
    deep=[i for i in cells if index_to_world(i%NX,i//NX)[0] < -260]
    low=separated(sorted(deep,key=lambda i:heights[i]),3)
    approach=[i for i in cells if -175 <= index_to_world(i%NX,i//NX)[0] <= -105]
    approaches=separated(sorted(approach,key=lambda i:heights[i]),3)
    markers=[]
    for kind,points in (("ridge_or_peak",high),("basin_or_valley",low),("foothill_approach",approaches)):
        for p in points:
            i=round((p[1]+520)/5)*NX+round((p[0]+420)/5)
            markers.append({"kind":kind,"pos":list(p),"elevation":heights[i],"status":"descriptive analytical marker"})
    return markers


def _generate_hydrology(seed: int, heights: list[int], terrain: list[str], mountain: list[bool]) -> list[dict]:
    """Trace connected catchments; cuts lower local barriers rather than stamping pools."""
    rng = random.Random(seed ^ 0xA11CE)
    candidates = [i for i, m in enumerate(mountain) if m and heights[i] >= 82 and divmod(i, NX)[1] < NX // 2]
    rng.shuffle(candidates)
    features = []
    for source in candidates:
        if len(features) >= 3:
            break
        path = [source]; current = source; downhill = 0; cuts = 0
        seen = {source}
        for _ in range(150):
            iz, ix = divmod(current, NX)
            neighbors = [qz * NX + qx for qx, qz in ((ix-1,iz),(ix+1,iz),(ix,iz-1),(ix,iz+1)) if inside(qx,qz)]
            # Prefer downhill/eastward drainage; permit a one-block spill cut at a basin.
            nxt = min(neighbors, key=lambda q: (heights[q] + (divmod(q, NX)[1] < ix) * 1.2, hash01(seed, q, len(path))))
            if nxt in seen:
                break
            if heights[nxt] >= heights[current]:
                target = max(SEA_LEVEL, heights[current] - 1)
                if heights[nxt] - target > 2:
                    break
                heights[nxt] = target; cuts += 1; downhill += 1
            else:
                downhill += 1
            current = nxt; path.append(nxt); seen.add(nxt)
            if terrain[current] in ("coast", "ocean") or heights[current] <= SEA_LEVEL:
                break
        if len(path) >= 18:
            for i in path:
                if terrain[i] != "ocean": terrain[i] = "water"
            points = [index_to_world(i % NX, i // NX) for i in path]
            features.append({
                "kind": "stream", "source": points[0], "mouth": points[-1],
                "path": points, "length_blocks": (len(points)-1) * 5,
                "downhill_fraction": round(downhill / max(1, len(path)-1), 3),
                "spill_cuts": cuts, "connected": True, "stamped": False,
            })
    return features
