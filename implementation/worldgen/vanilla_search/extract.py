"""Extract actual surface, biome, fluid, cave/lava-presence and structure data."""

from __future__ import annotations

import math
import time
from collections import Counter
from pathlib import Path

from serialization.nbt import plain
from serialization.region import read_region

MIN_Y = -64
SAMPLE_SPACING = 8
FULL_STATUS = "minecraft:full"
WATER_BLOCKS = {"minecraft:water", "minecraft:ice", "minecraft:frosted_ice"}


def _palette_value(container, index, minimum_bits):
    palette = container["palette"]
    if "data" not in container:
        return palette[0]
    bits = max(minimum_bits, (len(palette) - 1).bit_length())
    per = 64 // bits
    word = container["data"][index // per] & ((1 << 64) - 1)
    return palette[(word >> ((index % per) * bits)) & ((1 << bits) - 1)]


class VanillaChunk:
    def __init__(self, data):
        self.data = data
        self.sections = {section["Y"]: section for section in data["sections"]}

    def height(self, kind, x, z):
        values = self.data["Heightmaps"][kind]
        index = (z & 15) * 16 + (x & 15)
        per = 64 // 9
        word = values[index // per] & ((1 << 64) - 1)
        return MIN_Y + ((word >> ((index % per) * 9)) & 511) - 1

    def block(self, x, y, z):
        section = self.sections.get(math.floor(y / 16))
        if not section or "block_states" not in section:
            return "minecraft:air"
        entry = _palette_value(section["block_states"], (y & 15) * 256 + (z & 15) * 16 + (x & 15), 4)
        return entry["Name"]

    def biome(self, x, y, z):
        section = self.sections.get(math.floor(y / 16))
        if not section:
            return "minecraft:plains"
        index = ((y & 15) // 4) * 16 + ((z & 15) // 4) * 4 + ((x & 15) // 4)
        return _palette_value(section["biomes"], index, 1)


def extract_region(world, bounds):
    started = time.perf_counter()
    min_cx, max_cx, min_cz, max_cz = bounds
    chunks = {}
    structures = []
    presence = Counter()
    statuses = Counter()
    for path in sorted((Path(world) / "region").glob("*.mca")):
        for cx, cz, _, root in read_region(path):
            if not (min_cx <= cx <= max_cx and min_cz <= cz <= max_cz):
                continue
            data = plain(root)
            statuses[data.get("Status")] += 1
            if data.get("Status") != FULL_STATUS:
                continue
            chunk = VanillaChunk(data)
            chunks[(cx, cz)] = chunk
            for section in data["sections"]:
                if "block_states" not in section:
                    continue
                names = {entry["Name"] for entry in section["block_states"]["palette"]}
                presence["sections_with_cave_air"] += "minecraft:cave_air" in names
                presence["sections_with_water"] += "minecraft:water" in names
                presence["sections_with_lava"] += "minecraft:lava" in names
            for key, value in data.get("structures", {}).get("starts", {}).items():
                if value.get("id") in (None, "INVALID", "minecraft:invalid"):
                    continue
                children = value.get("Children", [])
                boxes = [child.get("BB") for child in children if child.get("BB")]
                structures.append({"type": key, "id": value.get("id"), "chunk": [cx, cz], "pos": [cx * 16 + 8, cz * 16 + 8], "piece_count": len(children), "bounding_boxes": boxes})
    expected = (max_cx - min_cx + 1) * (max_cz - min_cz + 1)
    if len(chunks) != expected:
        raise RuntimeError(f"expected {expected} full chunks, extracted {len(chunks)}; statuses={dict(statuses)}")
    rows = []
    for cz in range(min_cz, max_cz + 1):
        for oz in (4, 12):
            row = []
            z = cz * 16 + oz
            for cx in range(min_cx, max_cx + 1):
                chunk = chunks[(cx, cz)]
                for ox in (4, 12):
                    x = cx * 16 + ox
                    surface_y = chunk.height("WORLD_SURFACE", x, z)
                    motion_y = chunk.height("MOTION_BLOCKING_NO_LEAVES", x, z)
                    ocean_floor_y = chunk.height("OCEAN_FLOOR", x, z)
                    top_block = chunk.block(x, surface_y, z)
                    motion_block = chunk.block(x, motion_y, z)
                    actual_water = top_block in WATER_BLOCKS or motion_block in WATER_BLOCKS
                    terrain_y = ocean_floor_y if actual_water else motion_y
                    row.append({"x": x, "z": z, "surface_y": surface_y, "terrain_y": terrain_y, "biome": chunk.biome(x, max(terrain_y, 63), z), "top_block": top_block, "ground_block": chunk.block(x, terrain_y, z), "actual_surface_water": actual_water})
            rows.append(row)
    # More than one structure start may legitimately share a start chunk.
    # Deduplicate repeated serialization evidence without collapsing types.
    unique_structures = {(item["type"], *item["chunk"]): item for item in structures}
    return {"sample_spacing": SAMPLE_SPACING, "width": len(rows[0]), "height": len(rows), "rows": rows, "structures": list(unique_structures.values()), "block_volume_presence": dict(presence), "full_chunks": len(chunks), "expected_chunks": expected, "status_counts": dict(statuses), "extraction_seconds": round(time.perf_counter() - started, 3)}
