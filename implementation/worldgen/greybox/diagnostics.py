"""Diagnostics aimed at visible greybox defects, not final balance gates."""

from __future__ import annotations

import math
from collections import Counter

from successor.config import CELL_SIZE, NX, NZ
from successor.grid import distance_field, index_to_world, inside, world_to_index


def _entropy(values):
    counts = Counter(values)
    total = len(values)
    if total <= 1 or len(counts) <= 1:
        return 0.0
    raw = -sum((count / total) * math.log(count / total) for count in counts.values())
    return raw / math.log(len(counts))


def _terrain_quality(terrain):
    kinds = terrain["terrain"]
    heights = terrain["heights"]
    interior = [i for i, kind in enumerate(kinds) if kind not in ("ocean", "coast")]
    modal_matches = 0
    for ix in range(NX):
        column = [kinds[iz * NX + ix] for iz in range(NZ) if kinds[iz * NX + ix] not in ("ocean", "coast")]
        if column:
            modal_matches += Counter(column).most_common(1)[0][1]
    gradients = []
    boundary_runs = []
    for ix in range(NX - 1):
        run = 0
        for iz in range(NZ):
            a, b = kinds[iz * NX + ix], kinds[iz * NX + ix + 1]
            boundary = a != b and a not in ("ocean", "coast") and b not in ("ocean", "coast")
            if boundary:
                run += 1
            elif run:
                boundary_runs.append(run)
                run = 0
        if run:
            boundary_runs.append(run)
    for iz in range(NZ):
        for ix in range(NX):
            i = iz * NX + ix
            if ix + 1 < NX:
                gradients.append(abs(heights[i + 1] - heights[i]))
            if iz + 1 < NZ:
                gradients.append(abs(heights[i + NX] - heights[i]))
    residuals = []
    local_ranges = []
    for iz in range(4, NZ - 4, 3):
        for ix in range(4, NX - 4, 3):
            window = [heights[qz * NX + qx] for qz in range(iz - 4, iz + 5) for qx in range(ix - 4, ix + 5)]
            residuals.append(abs(heights[iz * NX + ix] - sum(window) / len(window)))
            local_ranges.append(max(window) - min(window))
    mean_gradient = sum(gradients) / len(gradients)
    gradient_cv = math.sqrt(sum((x - mean_gradient) ** 2 for x in gradients) / len(gradients)) / max(.01, mean_gradient)
    buckets = [0 if x < .5 else 1 if x < 1.5 else 2 if x < 2.5 else 3 if x < 4.5 else 4 for x in gradients]
    return {
        "column_modal_landcover_fraction": round(modal_matches / max(1, len(interior)), 4),
        "longest_north_south_class_boundary_blocks": max(boundary_runs, default=0) * CELL_SIZE,
        "medium_scale_height_residual_mean_y": round(sum(residuals) / len(residuals), 3),
        "medium_window_relief_mean_y": round(sum(local_ranges) / len(local_ranges), 3),
        "adjacent_gradient_mean_y": round(mean_gradient, 3),
        "adjacent_gradient_cv": round(gradient_cv, 3),
        "slope_class_entropy": round(_entropy(buckets), 4),
        "interpretation": "Lower column-modal/boundary values indicate less visible banding; residual, relief, CV and entropy describe multiscale variation rather than quality alone.",
    }


def _forest_quality(terrain, ecology):
    forest = [kind == "forest" for kind in terrain["terrain"]]
    edge = []
    straight = 0
    for i, value in enumerate(forest):
        if not value:
            continue
        iz, ix = divmod(i, NX)
        neighbors = {(dx, dz): forest[(iz + dz) * NX + ix + dx] for dx, dz in ((-1, 0), (1, 0), (0, -1), (0, 1)) if inside(ix + dx, iz + dz)}
        if neighbors and not all(neighbors.values()):
            edge.append(i)
            if (neighbors.get((-1, 0), False) == neighbors.get((1, 0), False)) != (neighbors.get((0, -1), False) == neighbors.get((0, 1), False)):
                straight += 1
    distance = distance_field([not x for x in forest])
    clearings = 0
    for i, value in enumerate(forest):
        if value:
            continue
        iz, ix = divmod(i, NX)
        if 2 <= ix < NX - 2 and 2 <= iz < NZ - 2:
            surrounding = sum(forest[qz * NX + qx] for qz in range(iz - 2, iz + 3) for qx in range(ix - 2, ix + 3))
            clearings += surrounding >= 18
    return {
        "region_count": len(ecology["forest_regions"]),
        "largest_interior_depth_blocks": max(distance, default=0) * CELL_SIZE,
        "edge_cells": len(edge),
        "locally_straight_edge_fraction": round(straight / max(1, len(edge)), 4),
        "embedded_clearing_cells": clearings,
        "region_areas_blocks2": [r["area_blocks2"] for r in ecology["forest_regions"][:8]],
    }


def _coast_quality(terrain):
    profile = terrain["coast_profile"]
    boundaries = [x["boundary_x"] for x in profile]
    widths = [x["transition_width"] for x in profile]
    length = sum(math.hypot(CELL_SIZE, b - a) for a, b in zip(boundaries, boundaries[1:]))
    direct = (len(boundaries) - 1) * CELL_SIZE
    mean_width = sum(widths) / len(widths)
    width_cv = math.sqrt(sum((x - mean_width) ** 2 for x in widths) / len(widths)) / max(1, mean_width)
    turns = 0
    for i in range(4, len(boundaries) - 4):
        window = boundaries[i - 4:i + 5]
        if max(window) - min(window) >= 3 and (boundaries[i] == max(window) or boundaries[i] == min(window)):
            turns += 1
    types = Counter(x["shore_type"] for x in profile)
    return {
        "shore_type_rows": dict(sorted(types.items())),
        "shore_type_count": len(types),
        "transition_width_min_max_mean": [min(widths), max(widths), round(mean_width, 2)],
        "transition_width_cv": round(width_cv, 3),
        "coastline_sinuosity": round(length / direct, 4),
        "headland_or_cove_turns": turns,
        "steeper_or_rocky_fraction": round(sum(x["shore_type"] in ("rocky_shore", "steep_shore") for x in profile) / len(profile), 4),
        "coastal_outlets": terrain["hydrological_terrain"]["coastal_outlets"],
    }


def _hydrology_quality(terrain):
    result = dict(terrain["hydrological_terrain"])
    longest = []
    direction_sets = []
    sinuosities = []
    for feature in terrain["hydrology"]:
        headings = [(b[0] - a[0], b[1] - a[1]) for a, b in zip(feature["path"], feature["path"][1:])]
        runs = []
        if headings:
            current, count = headings[0], 1
            for heading in headings[1:]:
                if heading == current:
                    count += 1
                else:
                    runs.append(count)
                    current, count = heading, 1
            runs.append(count)
        longest.append(max(runs, default=0) / max(1, len(headings)))
        direction_sets.append(len(set(headings)))
        sinuosities.append(feature["sinuosity"])
    result.update({
        "principal_channel_mean_longest_heading_fraction": round(sum(longest) / max(1, len(longest)), 4),
        "principal_channel_heading_diversity_mean": round(sum(direction_sets) / max(1, len(direction_sets)), 3),
        "principal_channel_sinuosity_mean": round(sum(sinuosities) / max(1, len(sinuosities)), 3),
        "representation_limit": "D8 paths remain grid-resolution drainage hypotheses; visible diagonal/cardinal runs are not finished river geometry.",
    })
    return result


def _route_quality(routes, terrain):
    per_branch = {}
    aggregate_headings = []
    right_angles = 0
    straight_fractions = []
    for branch in routes["branches"]:
        points = branch["centerline"]
        headings = []
        for a, b in zip(points, points[1:]):
            headings.append(round(math.degrees(math.atan2(b[1] - a[1], b[0] - a[0])) / 5) * 5)
        runs = []
        if headings:
            current, count = headings[0], 1
            for value in headings[1:]:
                if value == current:
                    count += 1
                else:
                    runs.append(count)
                    current, count = value, 1
            runs.append(count)
        turns = []
        for a, b in zip(headings, headings[1:]):
            delta = abs((b - a + 180) % 360 - 180)
            if delta:
                turns.append(delta)
                right_angles += delta >= 80
        direct = math.hypot(points[-1][0] - points[0][0], points[-1][1] - points[0][1])
        straight = max(runs, default=0) / max(1, len(headings))
        straight_fractions.append(straight)
        elevations = []
        for x, z in points:
            ix, iz = world_to_index(x, z)
            if inside(ix, iz):
                elevations.append(terrain["heights"][iz * NX + ix])
        per_branch[branch["id"]] = {
            "length_blocks": branch["length_blocks"],
            "sinuosity": round(branch["length_blocks"] / max(CELL_SIZE, direct), 3),
            "longest_constant_heading_fraction": round(straight, 3),
            "turn_count": len(turns),
            "mean_nonzero_turn_degrees": round(sum(turns) / max(1, len(turns)), 2),
            "elevation_range_y": max(elevations, default=0) - min(elevations, default=0),
        }
        aggregate_headings.extend(headings)
    return {
        "neighbor_basis": "8-direction terrain-cost search followed by endpoint-preserving curve refinement",
        "per_branch": per_branch,
        "mean_longest_constant_heading_fraction": round(sum(straight_fractions) / len(straight_fractions), 4),
        "right_angle_or_sharper_turns": right_angles,
        "heading_diversity": len(set(aggregate_headings)),
    }


def _opportunity_geography(terrain, ecology, routes):
    route_distance = distance_field(routes["mask"])
    result = []
    for item in ecology["instances"]:
        if item["category"] not in ("settlement", "major_poi", "minor_poi"):
            continue
        ix, iz = world_to_index(*item["pos"])
        local = []
        for qz in range(max(0, iz - 4), min(NZ, iz + 5)):
            for qx in range(max(0, ix - 4), min(NX, ix + 5)):
                local.append(terrain["heights"][qz * NX + qx])
        result.append({
            "id": item["id"],
            "category": item["category"],
            "pos": item["pos"],
            "terrain": item["terrain"],
            "local_relief_y": max(local) - min(local),
            "developable_fraction": round(sum(abs(h - item["elevation"]) <= 2 for h in local) / len(local), 3),
            "route_distance_blocks": route_distance[iz * NX + ix] * CELL_SIZE,
            "status": "analytical location/footprint only",
        })
    return result


def geography_diagnostics(terrain, ecology, routes):
    quality = {
        "terrain_multiscale": _terrain_quality(terrain),
        "hydrological_terrain": _hydrology_quality(terrain),
        "coast": _coast_quality(terrain),
        "forest": _forest_quality(terrain, ecology),
        "routes": _route_quality(routes, terrain),
        "opportunity_locations": _opportunity_geography(terrain, ecology, routes),
    }
    t, h, c, f, r = quality["terrain_multiscale"], quality["hydrological_terrain"], quality["coast"], quality["forest"], quality["routes"]
    checks = {
        "terrain_bands_reduced": t["column_modal_landcover_fraction"] <= .78 and t["longest_north_south_class_boundary_blocks"] <= 240,
        "macro_grade_contains_medium_relief": t["medium_scale_height_residual_mean_y"] >= .48 and t["medium_window_relief_mean_y"] >= 4.0,
        "forest_edges_fragmented": f["locally_straight_edge_fraction"] <= .72 and f["embedded_clearing_cells"] > 0,
        "drainage_is_terrain_not_water_stamp": h["potential_channel_cells"] > 0 and h["largest_catchment_cells"] >= 20,
        "drainage_has_relationships": h["tributary_junctions"] > 0 and (h["coastal_outlets"] > 0 or bool(h["basins"])),
        "principal_drainage_not_one_straight_trace": h["principal_channel_mean_longest_heading_fraction"] <= .38 and h["principal_channel_heading_diversity_mean"] >= 3,
        "coast_not_uniform_ribbon": c["shore_type_count"] >= 3 and c["transition_width_cv"] >= .22 and c["headland_or_cove_turns"] >= 8,
        "routes_not_cardinal_polylines": r["heading_diversity"] >= 12 and r["mean_longest_constant_heading_fraction"] <= .55,
    }
    quality["experimental_quality_checks"] = {"status": "prototype/test; shortlist screen, not design contracts", "checks": checks, "warnings": [key for key, value in checks.items() if not value]}
    return quality
