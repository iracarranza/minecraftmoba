"""Small standard-library grid/noise/mask helpers."""

from __future__ import annotations

import math
from collections import deque

from .config import CELL_SIZE, NX, NZ, X_MIN, Z_MIN


def index_to_world(ix: int, iz: int) -> tuple[int, int]:
    return X_MIN + ix * CELL_SIZE, Z_MIN + iz * CELL_SIZE


def world_to_index(x: float, z: float) -> tuple[int, int]:
    return round((x - X_MIN) / CELL_SIZE), round((z - Z_MIN) / CELL_SIZE)


def inside(ix: int, iz: int) -> bool:
    return 0 <= ix < NX and 0 <= iz < NZ


def hash01(seed: int, x: int, z: int) -> float:
    n = (x * 374761393 + z * 668265263 + seed * 1442695041) & 0xFFFFFFFF
    n = ((n ^ (n >> 13)) * 1274126177) & 0xFFFFFFFF
    return ((n ^ (n >> 16)) & 0xFFFFFFFF) / 4294967295.0


def smoothstep(t: float) -> float:
    return t * t * (3.0 - 2.0 * t)


def value_noise(seed: int, x: float, z: float, scale: float) -> float:
    gx, gz = x / scale, z / scale
    x0, z0 = math.floor(gx), math.floor(gz)
    tx, tz = smoothstep(gx - x0), smoothstep(gz - z0)
    a = hash01(seed, x0, z0) * 2 - 1
    b = hash01(seed, x0 + 1, z0) * 2 - 1
    c = hash01(seed, x0, z0 + 1) * 2 - 1
    d = hash01(seed, x0 + 1, z0 + 1) * 2 - 1
    return (a * (1 - tx) + b * tx) * (1 - tz) + (c * (1 - tx) + d * tx) * tz


def fractal(seed: int, x: float, z: float, scales=(180.0, 75.0, 32.0), amps=(1.0, .48, .2)) -> float:
    return sum(value_noise(seed + i * 7919, x, z, s) * a for i, (s, a) in enumerate(zip(scales, amps)))


def gaussian(x: float, z: float, cx: float, cz: float, sx: float, sz: float) -> float:
    return math.exp(-(((x - cx) / sx) ** 2 + ((z - cz) / sz) ** 2))


def line_distance(p: tuple[float, float], a: tuple[float, float], b: tuple[float, float]) -> float:
    px, pz = p; ax, az = a; bx, bz = b
    dx, dz = bx - ax, bz - az
    if dx == dz == 0:
        return math.hypot(px - ax, pz - az)
    t = max(0.0, min(1.0, ((px - ax) * dx + (pz - az) * dz) / (dx * dx + dz * dz)))
    return math.hypot(px - (ax + t * dx), pz - (az + t * dz))


def polyline_distance(p: tuple[float, float], points: list[tuple[int, int]]) -> float:
    return min(line_distance(p, a, b) for a, b in zip(points, points[1:]))


def disk_cells(x: int, z: int, radius: float):
    ix, iz = world_to_index(x, z)
    r = math.ceil(radius / CELL_SIZE)
    for dz in range(-r, r + 1):
        for dx in range(-r, r + 1):
            qx, qz = ix + dx, iz + dz
            if inside(qx, qz):
                wx, wz = index_to_world(qx, qz)
                if math.hypot(wx - x, wz - z) <= radius:
                    yield qx, qz


def distance_field(mask: list[bool]) -> list[int]:
    """Four-neighbor distance in cells to the nearest True cell."""
    inf = NX + NZ + 1
    dist = [inf] * (NX * NZ)
    queue = deque()
    for i, value in enumerate(mask):
        if value:
            dist[i] = 0
            queue.append(i)
    while queue:
        i = queue.popleft(); iz, ix = divmod(i, NX); nd = dist[i] + 1
        for qx, qz in ((ix - 1, iz), (ix + 1, iz), (ix, iz - 1), (ix, iz + 1)):
            if inside(qx, qz):
                qi = qz * NX + qx
                if nd < dist[qi]:
                    dist[qi] = nd; queue.append(qi)
    return dist


def rle(values: list) -> list[list]:
    if not values:
        return []
    out, current, count = [], values[0], 1
    for value in values[1:]:
        if value == current:
            count += 1
        else:
            out.append([current, count]); current, count = value, 1
    out.append([current, count])
    return out


def unrle(runs: list[list]) -> list:
    return [value for value, count in runs for _ in range(count)]
