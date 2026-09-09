"""Terrain-linked resource instances, followed by derived opportunity regions."""

from __future__ import annotations

import math
import random
from collections import Counter, defaultdict

from .config import NORTH_HOME, NX, NZ, SOUTH_HOME
from .grid import index_to_world

LIVESTOCK = ("cow", "sheep", "pig", "chicken")
CROPS = ("wheat", "carrot", "potato")
ORES = ("coal", "iron", "copper", "gold", "redstone", "lapis", "diamond", "emerald")


def _distance(a, b):
    return math.hypot(a[0] - b[0], a[1] - b[1])


def _pick(rng, terrain, predicate, count, separation, existing=()):
    cells = [i for i, kind in enumerate(terrain["terrain"]) if predicate(i, kind, terrain["heights"][i])]
    rng.shuffle(cells)
    chosen = []
    anchors = list(existing)
    for i in cells:
        p = index_to_world(i % NX, i // NX)
        if all(_distance(p, q) >= separation for q in anchors + chosen):
            chosen.append(p)
            if len(chosen) == count:
                break
    return chosen


def _instance(identifier, kind, category, pos, terrain, **extra):
    ix = round((pos[0] + 420) / 5); iz = round((pos[1] + 520) / 5)
    i = iz * NX + ix
    data = {
        "id": identifier, "type": kind, "category": category,
        "pos": list(pos), "elevation": terrain["heights"][i],
        "terrain": terrain["terrain"][i],
    }
    data.update(extra)
    return data


def generate_ecology(seed: int, terrain: dict) -> dict:
    rng = random.Random(seed ^ 0xEC0109)
    instances = []
    corrections = []
    occupied = []

    # Explicit homeland vocabulary. These are baseline allocations, not mirrored blocks.
    for team, home, offset in (("north", NORTH_HOME, -1), ("south", SOUTH_HOME, 1)):
        for j, kind in enumerate(("wood", "soil", "water", "stone", "coal", "iron", "cave")):
            angle = (j * 2.399 + rng.uniform(-.25, .25))
            radius = 18 + (j % 3) * 9 + rng.uniform(-3, 3)
            p = (round(home[0] + math.cos(angle) * radius), round(home[1] + math.sin(angle) * radius))
            instances.append(_instance(f"baseline-{team}-{kind}", kind, "homeland_baseline", p, terrain, team=team, quantity=1))

    ordinary = lambda i, k, h: k in ("open", "foothill", "forest") and abs(index_to_world(i % NX, i // NX)[1]) < 300
    village_positions = _pick(rng, terrain, ordinary, 4, 155, existing=((0, 0),))
    for j, p in enumerate(village_positions):
        instances.append(_instance(f"village-{j+1}", "village", "settlement", p, terrain,
            portfolio={"beds": rng.randint(4, 10), "workstations": rng.randint(2, 6), "crop_support": rng.choice(list(CROPS)), "livestock_support": rng.choice(list(LIVESTOCK))}))
        occupied.append(p)

    major_pred = lambda i, k, h: k not in ("ocean", "water") and abs(index_to_world(i % NX, i // NX)[1]) < 330
    major_positions = _pick(rng, terrain, major_pred, 4, 125, existing=occupied)
    for j, p in enumerate(major_positions):
        instances.append(_instance(f"major-poi-{j+1}", rng.choice(("geode", "ruined_portal", "trail_ruin", "mineshaft_access")), "major_poi", p, terrain, strategic_value=round(rng.uniform(.75, 1.0), 2)))
        occupied.append(p)
    minor_positions = _pick(rng, terrain, major_pred, 6, 75, existing=occupied)
    for j, p in enumerate(minor_positions):
        instances.append(_instance(f"minor-poi-{j+1}", rng.choice(("fossil", "spring", "monster_room", "rock_shelter", "desert_well")), "minor_poi", p, terrain, strategic_value=round(rng.uniform(.3, .65), 2)))

    # Four broad mixed ranges, then physical animals inside each range.
    range_positions = _pick(rng, terrain, lambda i,k,h: k in ("open", "foothill", "forest") and abs(index_to_world(i % NX, i // NX)[1]) < 315, 4, 150, existing=village_positions)
    rotations = (("cow","sheep"),("pig","chicken"),("cow","pig","chicken"),("sheep","pig","chicken"))
    livestock_ranges = []
    for j, center in enumerate(range_positions):
        species = list(rotations[(j + seed) % len(rotations)])
        livestock_ranges.append({"id": f"livestock-range-{j+1}", "center": list(center), "radius_blocks": rng.randint(48, 72), "species": species, "terrain_link": "mixed lowland/foothill forage"})
        for s, kind in enumerate(species):
            p = (center[0] + rng.randint(-22, 22), center[1] + rng.randint(-22, 22))
            instances.append(_instance(f"animal-{j+1}-{s+1}", kind, "livestock", p, terrain, range_id=f"livestock-range-{j+1}", population=rng.randint(3, 8)))
    present = {x["type"] for x in instances if x["category"] == "livestock"}
    for kind in set(LIVESTOCK) - present:
        center = range_positions[rng.randrange(len(range_positions))]
        p = (center[0] + rng.randint(-18, 18), center[1] + rng.randint(-18, 18))
        instances.append(_instance(f"animal-correction-{kind}", kind, "livestock", p, terrain, population=4))
        corrections.append({"kind": "guarantee", "resource": kind, "reason": "mixed-range draw omitted required species"})

    horse_positions = _pick(rng, terrain, lambda i,k,h: k == "open" and abs(index_to_world(i % NX, i // NX)[1]) < 310, 2, 180, existing=range_positions)
    for j, p in enumerate(horse_positions):
        instances.append(_instance(f"horse-range-{j+1}", "horse", "transport_ecology", p, terrain, population=rng.randint(3, 6)))

    # Crops favor villages but remain separate occurrences and include naturalized patches.
    for crop in CROPS:
        anchors = rng.sample(village_positions, min(2, len(village_positions)))
        positions = [(a[0] + rng.randint(-35, 35), a[1] + rng.randint(-35, 35)) for a in anchors]
        positions += _pick(rng, terrain, ordinary, 1, 95, existing=positions + occupied)
        for j, p in enumerate(positions):
            context = "settlement_adjacency" if j < len(anchors) else "naturalized_fertile_patch"
            instances.append(_instance(f"crop-{crop}-{j+1}", crop, "crop_starter", p, terrain, context=context, patch_area=rng.randint(12, 32)))

    # Terrain formations and cold ecology are concrete locations.
    sand_positions = _pick(rng, terrain, lambda i,k,h: k == "coast", 3, 90)
    gravel_positions = _pick(rng, terrain, lambda i,k,h: k in ("scree", "foothill") and h >= 70, 4, 75)
    snow_positions = _pick(rng, terrain, lambda i,k,h: k == "snow", 4, 70)
    for kind, positions in (("sand",sand_positions),("gravel",gravel_positions),("snow",snow_positions)):
        for j, p in enumerate(positions):
            instances.append(_instance(f"formation-{kind}-{j+1}", kind, "formation", p, terrain, area=rng.randint(80, 260)))

    # Ores are actual deposits linked to depth/exposure. Exceptional resources skew west.
    for ore in ORES:
        rare = ore in ("diamond", "emerald", "gold")
        predicate = (lambda i,k,h: k in ("rock","scree","snow") and h >= (82 if rare else 72)) if rare else (lambda i,k,h: k in ("rock","scree","foothill","forest") and h >= 67)
        count = 2 if rare else 3
        for j, p in enumerate(_pick(rng, terrain, predicate, count, 80 if rare else 60)):
            instances.append(_instance(f"ore-{ore}-{j+1}", ore, "exceptional_geology" if rare else "ordinary_geology", p, terrain,
                exposure=rng.choice(("cave_linked", "exposed", "buried")), relative_concentration=round(rng.uniform(.55, 1.0), 2)))

    cave_positions = _pick(rng, terrain, lambda i,k,h: k in ("rock","scree","foothill") and h >= 73, 6, 80)
    for j, p in enumerate(cave_positions):
        instances.append(_instance(f"cave-{j+1}", "cave", "geological_opportunity", p, terrain, internal_extent=rng.randint(35, 110), entrance_count=rng.choice((1,1,2))))

    # Contract correction is deliberately modest and fully counted. Terrain-linked
    # fallback placement is preferable to emitting a candidate with fictional regions.
    required = set(LIVESTOCK) | set(CROPS) | {"horse","sand","gravel","snow"} | set(ORES)
    present_types = {x["type"] for x in instances}
    for kind in sorted(required - present_types):
        if kind == "snow":
            eligible = [i for i,m in enumerate(terrain["mountain_mask"]) if m]
            i = max(eligible, key=lambda q: terrain["heights"][q]); terrain["terrain"][i] = "snow"
            category = "formation"
        elif kind == "sand":
            eligible = [i for i,k in enumerate(terrain["terrain"]) if k not in ("ocean","water")]
            i = max(eligible, key=lambda q: index_to_world(q%NX,q//NX)[0]); terrain["terrain"][i] = "coast"
            category = "formation"
        elif kind == "gravel":
            eligible = [i for i,m in enumerate(terrain["mountain_mask"]) if m]; i = min(eligible, key=lambda q: terrain["heights"][q]); terrain["terrain"][i] = "scree"
            category = "formation"
        elif kind == "horse":
            eligible = [i for i,k in enumerate(terrain["terrain"]) if k == "open"]; i = rng.choice(eligible); category = "transport_ecology"
        elif kind in ORES:
            eligible = [i for i,m in enumerate(terrain["mountain_mask"]) if m]; i = rng.choice(eligible); category = "exceptional_geology" if kind in ("diamond","emerald","gold") else "ordinary_geology"
        elif kind in CROPS:
            eligible = [i for i,k in enumerate(terrain["terrain"]) if k in ("open","forest")]; i = rng.choice(eligible); category = "crop_starter"
        else:
            eligible = [i for i,k in enumerate(terrain["terrain"]) if k in ("open","forest","foothill")]; i = rng.choice(eligible); category = "livestock"
        p = index_to_world(i % NX, i // NX)
        instances.append(_instance(f"contract-correction-{kind}", kind, category, p, terrain, correction=True, quantity=1))
        corrections.append({"kind":"terrain_linked_contract_correction", "resource":kind, "reason":"natural placement did not produce guaranteed vocabulary"})

    forest_regions = derive_forest_regions(terrain)
    opportunity_regions = derive_opportunity_regions(instances)
    return {
        "instances": instances, "livestock_ranges": livestock_ranges,
        "forest_regions": forest_regions, "opportunity_regions": opportunity_regions,
        "corrections": corrections,
    }


def derive_forest_regions(terrain: dict) -> list[dict]:
    forest = {i for i, k in enumerate(terrain["terrain"]) if k == "forest"}
    regions = []
    while forest:
        start = forest.pop(); stack = [start]; cells = [start]
        while stack:
            i = stack.pop(); iz, ix = divmod(i, NX)
            for q in (i-1, i+1, i-NX, i+NX):
                if q in forest and abs((q % NX) - ix) <= 1:
                    forest.remove(q); stack.append(q); cells.append(q)
        if len(cells) >= 20:
            xs = [index_to_world(i % NX, i // NX)[0] for i in cells]
            zs = [index_to_world(i % NX, i // NX)[1] for i in cells]
            regions.append({"id": f"forest-{len(regions)+1}", "cells": len(cells), "area_blocks2": len(cells)*25,
                "centroid": [round(sum(xs)/len(xs)), round(sum(zs)/len(zs))], "width_blocks": max(xs)-min(xs)+5, "depth_blocks": max(zs)-min(zs)+5})
    return sorted(regions, key=lambda r: r["cells"], reverse=True)


def derive_opportunity_regions(instances: list[dict], link_distance=65) -> list[dict]:
    relevant = [x for x in instances if x["category"] != "homeland_baseline"]
    remaining = set(range(len(relevant))); regions = []
    while remaining:
        seed = remaining.pop(); group = [seed]; queue = [seed]
        while queue:
            a = queue.pop()
            linked = [b for b in remaining if _distance(relevant[a]["pos"], relevant[b]["pos"]) <= link_distance]
            for b in linked:
                remaining.remove(b); queue.append(b); group.append(b)
        members = [relevant[i] for i in group]
        regions.append({"id": f"opportunity-region-{len(regions)+1}", "centroid": [round(sum(x["pos"][0] for x in members)/len(members)), round(sum(x["pos"][1] for x in members)/len(members))],
            "instance_ids": [x["id"] for x in members], "categories": dict(Counter(x["category"] for x in members)), "resource_types": sorted({x["type"] for x in members})})
    return regions
