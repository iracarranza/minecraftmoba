"""Hard contracts, experimental thresholds, and descriptive metrics stay distinct."""

from __future__ import annotations

import math
from collections import deque

from .config import CELL_SIZE, EXPERIMENTAL_THRESHOLDS, NORTH_HOME, NX, NZ, SOUTH_HOME
from .ecology import CROPS, LIVESTOCK, ORES
from .grid import distance_field, inside, world_to_index


def _largest_land_component(terrain):
    unseen = {i for i,k in enumerate(terrain["terrain"]) if k != "ocean"}; largest = 0
    while unseen:
        start=unseen.pop(); queue=[start]; count=1
        while queue:
            i=queue.pop(); iz,ix=divmod(i,NX)
            for qx,qz in ((ix-1,iz),(ix+1,iz),(ix,iz-1),(ix,iz+1)):
                q=qz*NX+qx
                if inside(qx,qz) and q in unseen:
                    unseen.remove(q); queue.append(q); count+=1
        largest=max(largest,count)
    return largest


def _route_checks(routes):
    checks = {}
    for branch in routes["branches"]:
        points=branch["centerline"]
        checks[branch["id"]] = bool(points) and points[0] == branch["home"] and all(abs(a[0]-b[0])+abs(a[1]-b[1]) <= CELL_SIZE for a,b in zip(points,points[1:]))
    return checks


def validate(candidate: dict) -> dict:
    terrain, ecology, routes = candidate["terrain"], candidate["ecology"], candidate["routes"]
    types={x["type"] for x in ecology["instances"]}
    ranges=ecology["livestock_ranges"]
    species={s for r in ranges for s in r["species"]} | {x["type"] for x in ecology["instances"] if x["category"]=="livestock"}
    route_detail=_route_checks(routes)
    baseline_required={"wood","soil","water","stone","coal","iron","cave"}
    baselines={}
    for team in ("north","south"):
        found={x["type"] for x in ecology["instances"] if x.get("team")==team and x["category"]=="homeland_baseline"}
        baselines[team]={"pass":baseline_required<=found,"missing":sorted(baseline_required-found)}
    hard = {
        "north_south_homelands": candidate["homelands"]["north"] == list(NORTH_HOME) and candidate["homelands"]["south"] == list(SOUTH_HOME),
        "one_continuous_land_wilderness": _largest_land_component(terrain) >= sum(k != "ocean" for k in terrain["terrain"]) * .98,
        "three_routes_per_team": sum(x["team"]=="north" for x in routes["branches"])==3 and sum(x["team"]=="south" for x in routes["branches"])==3,
        "route_connectivity": all(route_detail.values()),
        "four_mixed_livestock_ranges": len(ranges)>=4 and all(len(r["species"])>=2 for r in ranges),
        "livestock_species": set(LIVESTOCK) <= species,
        "crop_starters": set(CROPS) <= types and all(sum(x["type"]==crop for x in ecology["instances"])>=2 for crop in CROPS),
        "horses": "horse" in types,
        "sand_gravel_snow": {"sand","gravel","snow"} <= types,
        "ore_vocabulary": set(ORES) <= types,
        "villages_and_pois": sum(x["category"]=="settlement" for x in ecology["instances"])>=3 and any(x["category"]=="major_poi" for x in ecology["instances"]) and any(x["category"]=="minor_poi" for x in ecology["instances"]),
        "forests_and_caves": bool(ecology["forest_regions"]) and "cave" in types,
        "homeland_baseline_viability": all(x["pass"] for x in baselines.values()),
    }
    mountain_dist=distance_field([not x for x in terrain["mountain_mask"]])
    forest_mask=[k=="forest" for k in terrain["terrain"]]
    forest_dist=distance_field([not x for x in forest_mask])
    packing=candidate["packing"]; fairness=candidate["fairness"]
    empty=packing["space_overlays"].get("empty_connective_territory",{}).get("fraction_of_land",0)
    deep=packing["space_overlays"].get("deep_off_route_wilderness",{}).get("fraction_of_land",0)
    hydro=terrain["hydrology"]
    approach_heights=[]; deep_heights=[]; adjacent_steps=[]
    for i,m in enumerate(terrain["mountain_mask"]):
        if not m: continue
        x=-420+(i%NX)*CELL_SIZE
        if -180<=x<=-110: approach_heights.append(terrain["heights"][i])
        if x<=-330: deep_heights.append(terrain["heights"][i])
        iz,ix=divmod(i,NX)
        for qx,qz in ((ix+1,iz),(ix,iz+1)):
            if inside(qx,qz) and terrain["mountain_mask"][qz*NX+qx]: adjacent_steps.append(abs(terrain["heights"][qz*NX+qx]-terrain["heights"][i]))
    median=lambda values: sorted(values)[len(values)//2] if values else 0
    commitment_gain=median(deep_heights)-median(approach_heights)
    experimental = {
        "mountain_depth": max(mountain_dist)*CELL_SIZE >= EXPERIMENTAL_THRESHOLDS["mountain_depth_blocks_min"],
        "mountain_westward_commitment": commitment_gain >= EXPERIMENTAL_THRESHOLDS["mountain_commitment_gain_y_min"],
        "forest_interior_depth": max(forest_dist)*CELL_SIZE >= EXPERIMENTAL_THRESHOLDS["forest_interior_depth_blocks_min"],
        "connected_downhill_hydrology": bool(hydro) and sum(x["downhill_fraction"] for x in hydro)/len(hydro) >= EXPERIMENTAL_THRESHOLDS["hydrology_downhill_fraction_min"] and all(x["connected"] and not x["stamped"] for x in hydro),
        "wilderness_empty_separation": empty >= EXPERIMENTAL_THRESHOLDS["empty_connective_fraction_min"],
        "deep_off_route_space": deep >= EXPERIMENTAL_THRESHOLDS["deep_off_route_fraction_min"],
        "resource_packing": packing["packed_hotspot_max"] <= EXPERIMENTAL_THRESHOLDS["packed_hotspot_max"],
        "mean_opportunity_bias": fairness["mean_absolute_bias"] <= EXPERIMENTAL_THRESHOLDS["mean_absolute_bias_max"],
        "no_severe_opportunity_bias": fairness["maximum_absolute_bias"] <= EXPERIMENTAL_THRESHOLDS["severe_opportunity_bias_max"],
        "modest_correction": fairness["artificial_correction_count"] <= EXPERIMENTAL_THRESHOLDS["corrections_max"],
    }
    descriptive = {
        "mountain_depth_blocks": max(mountain_dist)*CELL_SIZE,
        "forest_interior_depth_blocks": max(forest_dist)*CELL_SIZE,
        "mountain_elevation_range": [min(h for h,m in zip(terrain["heights"],terrain["mountain_mask"]) if m), max(h for h,m in zip(terrain["heights"],terrain["mountain_mask"]) if m)],
        "mountain_approach_median_y":median(approach_heights), "mountain_deep_median_y":median(deep_heights), "mountain_commitment_gain_y":commitment_gain,
        "mountain_adjacent_steps_gt8":sum(x>8 for x in adjacent_steps), "mountain_adjacent_steps_gt12":sum(x>12 for x in adjacent_steps),
        "hydrology_feature_count": len(hydro), "largest_land_component_cells": _largest_land_component(terrain),
        "route_connectivity_detail": route_detail,
    }
    warnings=[name for name,passed in experimental.items() if not passed]
    failures=[name for name,passed in hard.items() if not passed]
    return {"pass": not failures, "hard_contract": {"status":"hard", "pass":not failures, "checks":hard, "failures":failures},
        "experimental_thresholds":{"status":"prototype/test", "checks":experimental, "warnings":warnings, "values":EXPERIMENTAL_THRESHOLDS},
        "descriptive_metrics":{"status":"descriptive; not gates", **descriptive}}
