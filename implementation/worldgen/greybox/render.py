"""Cheap, consistent PNG review renders for geography comparison."""

from __future__ import annotations

import math
import struct
import zlib
from pathlib import Path

from successor.config import NORTH_HOME, NX, NZ, SOUTH_HOME
from successor.grid import index_to_world, world_to_index

SCALE = 3
COLORS = {
    "open": (105, 151, 72), "forest": (43, 102, 50), "foothill": (119, 132, 77),
    "scree": (139, 132, 118), "rock": (119, 122, 121), "snow": (226, 235, 236),
    "coast": (207, 190, 133), "ocean": (48, 96, 164),
}
COAST = {"beach": (225, 204, 144), "rocky_shore": (105, 111, 112), "coastal_flat": (143, 165, 92), "steep_shore": (139, 122, 107), "inland_coastal_transition": (126, 157, 82), "ocean": (48, 96, 164)}


def _png(path, width, height, pixels):
    def chunk(kind, data):
        return struct.pack(">I", len(data)) + kind + data + struct.pack(">I", zlib.crc32(kind + data) & 0xFFFFFFFF)
    raw = b"".join(b"\x00" + bytes(pixels[y * width * 3:(y + 1) * width * 3]) for y in range(height))
    path.write_bytes(b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", width, height, 8, 2, 0, 0, 0)) + chunk(b"IDAT", zlib.compress(raw, 9)) + chunk(b"IEND", b""))


def _canvas(base):
    pixels = [0] * (NX * SCALE * NZ * SCALE * 3)
    for iz in range(NZ):
        for ix in range(NX):
            color = base(iz * NX + ix)
            for dz in range(SCALE):
                for dx in range(SCALE):
                    offset = (((iz * SCALE + dz) * NX * SCALE) + ix * SCALE + dx) * 3
                    pixels[offset:offset + 3] = color
    return pixels


def _put(pixels, ix, iz, color, radius=0):
    width, height = NX * SCALE, NZ * SCALE
    cx, cz = ix * SCALE + SCALE // 2, iz * SCALE + SCALE // 2
    for dz in range(-radius, radius + 1):
        for dx in range(-radius, radius + 1):
            if dx * dx + dz * dz > radius * radius:
                continue
            x, z = cx + dx, cz + dz
            if 0 <= x < width and 0 <= z < height:
                offset = (z * width + x) * 3
                pixels[offset:offset + 3] = color


def _line(pixels, a, b, color, radius=1):
    ax, az = world_to_index(*a)
    bx, bz = world_to_index(*b)
    steps = max(abs(bx - ax), abs(bz - az), 1)
    for step in range(steps + 1):
        t = step / steps
        _put(pixels, round(ax + (bx - ax) * t), round(az + (bz - az) * t), color, radius)


def _shade(color, value):
    return tuple(max(0, min(255, round(x * value))) for x in color)


def render_candidate(candidate, outdir):
    outdir = Path(outdir)
    outdir.mkdir(parents=True, exist_ok=True)
    terrain = candidate["terrain"]
    heights = terrain["heights"]
    kinds = terrain["terrain"]

    def topographic(i):
        iz, ix = divmod(i, NX)
        east = heights[i + 1] if ix + 1 < NX else heights[i]
        south = heights[i + NX] if iz + 1 < NZ else heights[i]
        light = max(.62, min(1.28, 1.02 - (east - heights[i]) * .055 - (south - heights[i]) * .035))
        elevation = max(0, min(1, (heights[i] - 55) / 60))
        base = (88 + round(95 * elevation), 119 + round(80 * elevation), 78 + round(90 * elevation))
        if kinds[i] == "ocean":
            base = COLORS["ocean"]
        elif heights[i] >= 92:
            base = (192, 205, 201)
        if heights[i] % 8 == 0 and kinds[i] != "ocean":
            light *= .73
        return _shade(base, light)
    pixels = _canvas(topographic)
    for marker in terrain["mountain_feature_markers"]:
        ix, iz = world_to_index(*marker["pos"])
        color = (245, 245, 245) if marker["kind"] == "ridge_or_peak" else (74, 47, 116) if "ravine" in marker["kind"] else (35, 55, 86)
        _put(pixels, ix, iz, color, 4)
    _png(outdir / "01_topography.png", NX * SCALE, NZ * SCALE, pixels)

    def hydrology_base(i):
        zone = terrain["coast_zones"][i]
        if zone != "inland":
            return COAST.get(zone, (116, 142, 88))
        h = heights[i]
        return _shade((151, 159, 126), .78 + max(0, min(.3, (h - 60) / 150)))
    pixels = _canvas(hydrology_base)
    for point in terrain["hydrological_terrain"]["channel_cells"]:
        ix, iz = world_to_index(*point)
        _put(pixels, ix, iz, (82, 153, 195), 1)
    for feature in terrain["hydrology"]:
        for a, b in zip(feature["path"], feature["path"][1:]):
            _line(pixels, a, b, (22, 91, 184), 1)
    for basin in terrain["hydrological_terrain"]["basins"]:
        ix, iz = world_to_index(*basin["pos"])
        _put(pixels, ix, iz, (31, 54, 122), 5)
        _put(pixels, ix, iz, (116, 205, 226), 2)
    for point in terrain["hydrological_terrain"]["junction_markers"]:
        ix, iz = world_to_index(*point)
        _put(pixels, ix, iz, (30, 219, 220), 2)
    _png(outdir / "02_hydrological_terrain.png", NX * SCALE, NZ * SCALE, pixels)

    def overview(i):
        iz, ix = divmod(i, NX)
        east = heights[i + 1] if ix + 1 < NX else heights[i]
        south = heights[i + NX] if iz + 1 < NZ else heights[i]
        return _shade(COLORS[kinds[i]], max(.7, min(1.2, 1.02 - (east - heights[i]) * .045 - (south - heights[i]) * .03)))
    pixels = _canvas(overview)
    for branch in candidate["routes"]["branches"]:
        for a, b in zip(branch["centerline"], branch["centerline"][1:]):
            _line(pixels, a, b, (238, 190, 43), 1)
    for home, color in ((NORTH_HOME, (42, 74, 215)), (SOUTH_HOME, (204, 48, 50))):
        ix, iz = world_to_index(*home)
        _put(pixels, ix, iz, color, 12)
        _put(pixels, ix, iz, (245, 245, 245), 4)
    marker_colors = {"settlement": (242, 137, 38), "major_poi": (169, 64, 207), "minor_poi": (139, 207, 68), "exceptional_geology": (247, 221, 71), "ordinary_geology": (185, 168, 75), "crop_starter": (232, 232, 129), "transport_ecology": (206, 120, 63)}
    for item in candidate["ecology"]["instances"]:
        if item["category"] not in marker_colors:
            continue
        ix, iz = world_to_index(*item["pos"])
        _put(pixels, ix, iz, marker_colors[item["category"]], 4 if item["category"] in ("settlement", "major_poi") else 2)
    _png(outdir / "03_geography_and_markers.png", NX * SCALE, NZ * SCALE, pixels)
    return [outdir / name for name in ("01_topography.png", "02_hydrological_terrain.png", "03_geography_and_markers.png")]
