"""Generate, screen, diversify, render, and report geography-first greyboxes."""

from __future__ import annotations

import json
import math
import shutil
import time
from pathlib import Path

from successor.analysis import opportunity_fairness, packing_analysis, traversal_analysis
from successor.config import NORTH_HOME, NX, NZ, SCALE_MODEL, SOUTH_HOME
from successor.ecology import generate_ecology
from successor.grid import index_to_world, rle
from successor.validation import validate

from . import GENERATOR_NAME
from .diagnostics import geography_diagnostics
from .render import render_candidate
from .routes import generate_routes
from .terrain import generate_terrain

REGRESSION_SEEDS = (920261010, 920261022)


def generate_candidate(seed):
    terrain = generate_terrain(seed)
    ecology = generate_ecology(seed, terrain)
    routes = generate_routes(seed, terrain, ecology)
    traversal = traversal_analysis(terrain, routes, ecology)
    candidate = {
        "schema_version": 2,
        "generator": GENERATOR_NAME,
        "seed": seed,
        "design_status": "prototype/test geography greybox; not selected Default",
        "scope": "terrain, hydrological terrain, coast, forest regions, analytical corridors and opportunity markers only",
        "scale_model": {**SCALE_MODEL, "name": "nonuniform_homeland_depth_v2_geography_greybox"},
        "homelands": {"north": list(NORTH_HOME), "south": list(SOUTH_HOME)},
        "terrain": terrain,
        "ecology": ecology,
        "routes": routes,
        "traversal": traversal,
    }
    candidate["packing"] = packing_analysis(terrain, routes, ecology)
    candidate["fairness"] = opportunity_fairness(ecology, traversal)
    candidate["validation"] = validate(candidate)
    route_detail = {branch["id"]: bool(branch["centerline"]) and branch["centerline"][0] == branch["home"] and all(math.hypot(a[0] - b[0], a[1] - b[1]) <= 4.5 for a, b in zip(branch["centerline"], branch["centerline"][1:])) for branch in routes["branches"]}
    candidate["validation"]["hard_contract"]["checks"]["route_connectivity"] = all(route_detail.values())
    finite_access = all(math.isfinite(report["path_cost"]) for team in ("north", "south") for report in traversal["teams"][team]["access"].values())
    candidate["validation"]["hard_contract"]["checks"]["all_markers_on_traversable_land"] = finite_access
    candidate["validation"]["descriptive_metrics"]["route_connectivity_detail"] = route_detail
    candidate["validation"]["hard_contract"]["failures"] = [key for key, value in candidate["validation"]["hard_contract"]["checks"].items() if not value]
    candidate["validation"]["hard_contract"]["pass"] = not candidate["validation"]["hard_contract"]["failures"]
    candidate["validation"]["pass"] = candidate["validation"]["hard_contract"]["pass"]
    candidate["geography_diagnostics"] = geography_diagnostics(terrain, ecology, routes)
    candidate["summary"] = summarize(candidate)
    return candidate


def summarize(candidate):
    terrain = candidate["terrain"]
    diagnostics = candidate["geography_diagnostics"]
    mountain_heights = [h for h, value in zip(terrain["heights"], terrain["mountain_mask"]) if value]
    opportunities = diagnostics["opportunity_locations"]
    return {
        "topology_family": terrain["topology_family"],
        "mountain": {
            "elevation_range": [min(mountain_heights), max(mountain_heights)],
            "depth_blocks": candidate["validation"]["descriptive_metrics"]["mountain_depth_blocks"],
            "commitment_gain_y": candidate["validation"]["descriptive_metrics"]["mountain_commitment_gain_y"],
            "landmark_counts": _counts(x["kind"] for x in terrain["mountain_feature_markers"]),
        },
        "hydrological_terrain": {
            "potential_channels": len(terrain["hydrology"]),
            "channel_network_cells": terrain["hydrological_terrain"]["potential_channel_cells"],
            "tributary_junctions": terrain["hydrological_terrain"]["tributary_junctions"],
            "basins": len(terrain["hydrological_terrain"]["basins"]),
            "coastal_outlets": terrain["hydrological_terrain"]["coastal_outlets"],
            "ocean_draining_fraction": terrain["hydrological_terrain"]["ocean_draining_fraction"],
            "principal_channel_mean_longest_heading_fraction": diagnostics["hydrological_terrain"]["principal_channel_mean_longest_heading_fraction"],
            "principal_channel_sinuosity_mean": diagnostics["hydrological_terrain"]["principal_channel_sinuosity_mean"],
        },
        "coast": diagnostics["coast"],
        "forest": diagnostics["forest"],
        "routes": {
            "branch_lengths": [x["length_blocks"] for x in candidate["routes"]["branches"]],
            **{k: diagnostics["routes"][k] for k in ("mean_longest_constant_heading_fraction", "right_angle_or_sharper_turns", "heading_diversity")},
        },
        "opportunity_markers": {
            "count": len(opportunities),
            "mean_developable_fraction": round(sum(x["developable_fraction"] for x in opportunities) / max(1, len(opportunities)), 3),
            "mean_local_relief_y": round(sum(x["local_relief_y"] for x in opportunities) / max(1, len(opportunities)), 3),
        },
        "empty_connective_fraction": candidate["packing"]["space_overlays"].get("empty_connective_territory", {}).get("fraction_of_land", 0),
        "deep_off_route_fraction": candidate["packing"]["space_overlays"].get("deep_off_route_wilderness", {}).get("fraction_of_land", 0),
        "packing_hotspot": candidate["packing"]["packed_hotspot_max"],
        "mean_absolute_opportunity_bias": candidate["fairness"]["mean_absolute_bias"],
        "quality_warnings": diagnostics["experimental_quality_checks"]["warnings"],
    }


def _counts(values):
    output = {}
    for value in values:
        output[value] = output.get(value, 0) + 1
    return dict(sorted(output.items()))


def quality_score(candidate):
    d = candidate["geography_diagnostics"]
    t, h, c, f, r = d["terrain_multiscale"], d["hydrological_terrain"], d["coast"], d["forest"], d["routes"]
    warnings = len(d["experimental_quality_checks"]["warnings"])
    if not math.isfinite(candidate["fairness"]["mean_absolute_bias"]):
        return -999.0
    return round(
        100
        - warnings * 12
        - t["column_modal_landcover_fraction"] * 10
        - t["longest_north_south_class_boundary_blocks"] / 80
        + min(9, t["medium_window_relief_mean_y"] * .8)
        + min(7, h["tributary_junctions"] / 3)
        + min(5, h["coastal_outlets"] / 2)
        + c["transition_width_cv"] * 8
        + min(5, c["headland_or_cove_turns"] / 8)
        - f["locally_straight_edge_fraction"] * 6
        - r["mean_longest_constant_heading_fraction"] * 8
        - candidate["fairness"]["mean_absolute_bias"] * 18
        - max(0, candidate["packing"]["packed_hotspot_max"] - 7) * 2,
        3,
    )


def _fingerprint(candidate):
    d = candidate["geography_diagnostics"]
    s = candidate["summary"]
    return [
        candidate["validation"]["descriptive_metrics"]["mountain_depth_blocks"] / 320,
        s["mountain"]["elevation_range"][1] / 120,
        d["terrain_multiscale"]["medium_window_relief_mean_y"] / 20,
        d["hydrological_terrain"]["ocean_draining_fraction"],
        d["coast"]["transition_width_cv"],
        d["forest"]["largest_interior_depth_blocks"] / 150,
        s["empty_connective_fraction"] * 2,
        d["routes"]["mean_longest_constant_heading_fraction"] * 2,
        candidate["fairness"]["mean_absolute_bias"] * 3,
    ]


def _distance(a, b):
    return math.sqrt(sum((x - y) ** 2 for x, y in zip(_fingerprint(a), _fingerprint(b))))


def search_candidates(start_seed=920261000, attempts=48, finalists=6):
    started = time.perf_counter()
    evaluated = [generate_candidate(start_seed + offset) for offset in range(attempts)]
    hard_pass = [x for x in evaluated if x["validation"]["pass"]]
    quality_pass = [x for x in hard_pass if not x["geography_diagnostics"]["experimental_quality_checks"]["warnings"]]
    pool = quality_pass or sorted(hard_pass, key=lambda x: (len(x["summary"]["quality_warnings"]), -quality_score(x)))
    ranked = sorted(pool, key=quality_score, reverse=True)
    selected = ranked[:1]
    while len(selected) < min(finalists, len(ranked)):
        remaining = [x for x in ranked if x not in selected]
        unused = [x for x in remaining if x["terrain"]["topology_family"] not in {q["terrain"]["topology_family"] for q in selected}]
        if unused:
            remaining = unused
        selected.append(max(remaining, key=lambda x: min(_distance(x, q) for q in selected) + quality_score(x) / 350))
    for index, candidate in enumerate(selected):
        candidate["selection"] = {
            "quality_score": quality_score(candidate),
            "selection_method": "quality screen followed by topology/metric diversity; not aggregate top-N",
            "strongest_reason_to_keep": _keep_reason(candidate),
            "strongest_reason_to_reject": _reject_reason(candidate),
            "minimum_shortlist_fingerprint_distance": round(min((_distance(candidate, other) for other in selected if other is not candidate), default=0), 3),
            "rank_role": "strongest screened evidence" if index == 0 else "diverse geography",
        }
    elapsed = time.perf_counter() - started
    report = {
        "generator": GENERATOR_NAME,
        "start_seed": start_seed,
        "attempts": attempts,
        "runtime_seconds": round(elapsed, 3),
        "hard_contract_passes": len(hard_pass),
        "hard_rejected_count": len(evaluated) - len(hard_pass),
        "experimental_quality_passes": len(quality_pass),
        "quality_screened_out_count": len(hard_pass) - len(quality_pass),
        "shortlist_size": len(selected),
        "selection_method": "Hard contracts first; experimental geography checks screen visible defects; shortlist then preserves topology and metric diversity. Score alone never selects top-N.",
        "regression_seeds": list(REGRESSION_SEEDS),
        "evaluated_summary": [_evaluated_row(x, x in selected) for x in evaluated],
    }
    return selected, evaluated, report


def _keep_reason(candidate):
    s = candidate["summary"]
    family = s["topology_family"]
    reasons = {
        "highland_ravines": "negative-relief mountain structure and catchment opportunities",
        "sawtooth_valleys": "repeated internal valleys without reverting to a western wall",
        "horseshoe": "deep enclosing highland identity with multiple low approaches",
        "broken_peaks": "fragmented western landmarks and strong forest/open alternation",
        "long_ridge": "clear deep-west commitment with comparatively balanced opportunity access",
        "twin_massif": "two readable western highland districts and substantial connective space",
        "split_spine": "divided ridge system with distinct crossing choices",
        "offset_basin": "offset interior basin and contrasting Route enclosure",
    }
    return reasons[family]


def _reject_reason(candidate):
    warnings = candidate["summary"]["quality_warnings"]
    if warnings:
        return warnings[0].replace("_", " ")
    d = candidate["geography_diagnostics"]
    s = candidate["summary"]
    if s["packing_hotspot"] >= 9:
        return "analytical opportunity packing is near the experimental ceiling"
    if s["mean_absolute_opportunity_bias"] >= .17:
        return "North/South opportunity portfolio asymmetry is comparatively high"
    if s["forest"]["largest_interior_depth_blocks"] < 55:
        return "forest geography is fragmented enough to limit meaningful interiors"
    if d["hydrological_terrain"]["principal_channel_mean_longest_heading_fraction"] > .30:
        return "principal drainage still exposes long D8 grid-direction runs"
    concerns = [
        (d["terrain_multiscale"]["column_modal_landcover_fraction"], "some west/east landcover structure remains legible"),
        (d["forest"]["locally_straight_edge_fraction"], "forest boundaries remain locally regular"),
        (d["routes"]["mean_longest_constant_heading_fraction"], "some provisional corridor runs remain long"),
        (candidate["fairness"]["mean_absolute_bias"], "opportunity portfolio asymmetry needs review"),
    ]
    return max(concerns)[1]


def _evaluated_row(candidate, shortlisted):
    return {
        "seed": candidate["seed"],
        "topology": candidate["terrain"]["topology_family"],
        "hard_pass": candidate["validation"]["pass"],
        "hard_failures": candidate["validation"]["hard_contract"]["failures"],
        "quality_score": quality_score(candidate),
        "quality_warnings": candidate["summary"]["quality_warnings"],
        "shortlisted": shortlisted,
    }


def serializable(candidate):
    output = {key: value for key, value in candidate.items() if key not in ("terrain", "routes")}
    terrain = {key: value for key, value in candidate["terrain"].items() if key not in ("heights", "terrain", "mountain_mask", "coast_zones")}
    terrain["hydrological_terrain"] = {key: value for key, value in terrain["hydrological_terrain"].items() if key not in ("channel_cells",)}
    channel_cells = {tuple(point) for point in candidate["terrain"]["hydrological_terrain"]["channel_cells"]}
    terrain["grid_encoding"] = {
        "order": "row-major z then x",
        "nx": NX,
        "nz": NZ,
        "height_rle": rle(candidate["terrain"]["heights"]),
        "terrain_rle": rle(candidate["terrain"]["terrain"]),
        "mountain_mask_rle": rle(candidate["terrain"]["mountain_mask"]),
        "coast_zone_rle": rle(candidate["terrain"]["coast_zones"]),
        "potential_channel_mask_rle": rle([index_to_world(i % NX, i // NX) in channel_cells for i in range(NX * NZ)]),
    }
    output["terrain"] = terrain
    output["routes"] = {key: value for key, value in candidate["routes"].items() if key != "mask"}
    output["routes"]["mask_rle"] = rle(candidate["routes"]["mask"])
    return output


def write_results(outdir, selected, evaluated, report):
    outdir = Path(outdir)
    if outdir.exists():
        shutil.rmtree(outdir)
    outdir.mkdir(parents=True, exist_ok=True)
    for candidate in selected:
        seed_dir = outdir / "shortlist" / str(candidate["seed"])
        seed_dir.mkdir(parents=True, exist_ok=True)
        (seed_dir / "candidate.json").write_text(json.dumps(serializable(candidate), separators=(",", ":")) + "\n")
        render_candidate(candidate, seed_dir)
    by_seed = {x["seed"]: x for x in evaluated}
    for seed in REGRESSION_SEEDS:
        candidate = by_seed[seed]
        seed_dir = outdir / "regressions" / str(seed)
        seed_dir.mkdir(parents=True, exist_ok=True)
        (seed_dir / "candidate.json").write_text(json.dumps(serializable(candidate), separators=(",", ":")) + "\n")
        render_candidate(candidate, seed_dir)
    (outdir / "search_summary.json").write_text(json.dumps(report, indent=2) + "\n")
    (outdir / "SHORTLIST.md").write_text(_markdown(selected, by_seed, report))


def _markdown(selected, by_seed, report):
    lines = [
        "# Default geography greybox v2 shortlist", "",
        "> Status: prototype/test terrain exploration. No final Default map is selected, and none of these candidates is a playable Minecraft world.", "",
        f"One deterministic Python batch evaluated **{report['attempts']}** seeds in **{report['runtime_seconds']} seconds**. {report['hard_contract_passes']} passed hard contracts; {report['experimental_quality_passes']} also passed every experimental geography screen. The shortlist retains **{len(selected)}** for quality plus geographic diversity, not aggregate top-N.", "",
        "Each seed has three compact renders: topography/landmarks; hydrological terrain/coast morphology; and forests, analytical Route corridors, homelands, and opportunity markers. Blue drainage is potential flow terrain—not finished water. Yellow lines are corridors—not roads.", "",
        "| Seed | Family | Mountain Y/depth | Channels/junctions/basins | Coast types/width CV | Forest depth | Empty/deep off-Route | Route straight-run | Bias |", "|---:|---|---|---|---|---:|---|---:|---:|",
    ]
    for c in selected:
        s = c["summary"]
        lines.append(f"| {c['seed']} | {s['topology_family']} | {s['mountain']['elevation_range'][0]}–{s['mountain']['elevation_range'][1]} / {s['mountain']['depth_blocks']} | {s['hydrological_terrain']['potential_channels']}/{s['hydrological_terrain']['tributary_junctions']}/{s['hydrological_terrain']['basins']} | {s['coast']['shore_type_count']} / {s['coast']['transition_width_cv']:.2f} | {s['forest']['largest_interior_depth_blocks']} | {s['empty_connective_fraction']:.1%}/{s['deep_off_route_fraction']:.1%} | {s['routes']['mean_longest_constant_heading_fraction']:.2f} | {s['mean_absolute_opportunity_bias']:.3f} |")
    lines += ["", "## Shortlisted candidates", ""]
    for c in selected:
        s, d = c["summary"], c["geography_diagnostics"]
        lines += [
            f"### {c['seed']} — {s['topology_family']}", "",
            f"![Topography](shortlist/{c['seed']}/01_topography.png)", "",
            f"![Hydrological terrain](shortlist/{c['seed']}/02_hydrological_terrain.png)", "",
            f"![Geography and markers](shortlist/{c['seed']}/03_geography_and_markers.png)", "",
            f"- Geographic identity: {s['topology_family'].replace('_', ' ')} with western depth {s['mountain']['depth_blocks']} blocks and Y{s['mountain']['elevation_range'][0]}–{s['mountain']['elevation_range'][1]}.",
            f"- Hydrological terrain: {s['hydrological_terrain']['potential_channels']} principal potential channels, {s['hydrological_terrain']['tributary_junctions']} network junctions, {s['hydrological_terrain']['basins']} collection basins, and {s['hydrological_terrain']['coastal_outlets']} coast outlets; mean principal sinuosity {s['hydrological_terrain']['principal_channel_sinuosity_mean']}, heading-run fraction {s['hydrological_terrain']['principal_channel_mean_longest_heading_fraction']}; no water blocks or finished rivers.",
            f"- Coast: {s['coast']['shore_type_count']} shore characters, width {s['coast']['transition_width_min_max_mean'][0]}–{s['coast']['transition_width_min_max_mean'][1]} blocks, sinuosity {s['coast']['coastline_sinuosity']}, with {s['coast']['headland_or_cove_turns']} headland/cove turns.",
            f"- Forest/open space: {s['forest']['region_count']} substantial regions, {s['forest']['largest_interior_depth_blocks']} block maximum interior, {s['forest']['embedded_clearing_cells']} embedded clearing cells; empty/deep-off-Route {s['empty_connective_fraction']:.1%}/{s['deep_off_route_fraction']:.1%}.",
            f"- Routes and opportunities: six curved analytical corridors, straight-run diagnostic {s['routes']['mean_longest_constant_heading_fraction']:.3f}; {s['opportunity_markers']['count']} village/POI location markers average {s['opportunity_markers']['mean_developable_fraction']:.1%} locally developable area. Packing hotspot {s['packing_hotspot']}; opportunity bias {s['mean_absolute_opportunity_bias']:.3f}.",
            f"- Previously visible defects: " + ", ".join(f"{key.replace('_', ' ')}={'improved' if value else 'still flagged'}" for key, value in d["experimental_quality_checks"]["checks"].items()) + ".",
            f"- Strongest reason to keep: {c['selection']['strongest_reason_to_keep']}.",
            f"- Strongest reason to reject: {c['selection']['strongest_reason_to_reject']}.", "",
        ]
    lines += ["## Regression references", ""]
    for seed in REGRESSION_SEEDS:
        c = by_seed[seed]
        s, checks = c["summary"], c["geography_diagnostics"]["experimental_quality_checks"]
        lines += [
            f"### {seed} — regenerated {s['topology_family']}", "",
            f"![Regression geography](regressions/{seed}/03_geography_and_markers.png)", "",
            f"The v2 seed is intentionally not output-identical to its v1 world. It passes {sum(checks['checks'].values())}/{len(checks['checks'])} visible-defect screens. Its hydrology is now unmodified D8 catchment terrain with {s['hydrological_terrain']['tributary_junctions']} tributary junctions and {s['hydrological_terrain']['basins']} basins; coast width CV is {s['coast']['transition_width_cv']:.3f}; forest-edge straightness is {s['forest']['locally_straight_edge_fraction']:.3f}; Route straight-run diagnostic is {s['routes']['mean_longest_constant_heading_fraction']:.3f}. Remaining warnings: {', '.join(checks['warnings']) or 'none in the current experimental screens'}.", "",
        ]
    lines += [
        "## Comparison with the serialized v1 worlds", "",
        "The v2 regression renders preserve the horseshoe and offset-basin seed identities without preserving their exact v1 geometry. Domain-warped overlapping relief replaces conspicuous macro/material bands; medium and small relief now interrupts the westward grade; forest edges interlock and contain clearings; coast width and shore character vary by row; and terrain-cost Routes use diagonals plus curve refinement instead of cardinal/right-angle centerlines. Hydrology is no longer physicalized as static water or lava: priority-flood catchments, spills, minima, basins, accumulation, tributaries, and possible coast outlets are evaluated over an unchanged heightfield.", "",
        "These are source-generator improvements, not palette fixes. The pass deliberately does not test whether any channel will behave like vanilla water, whether markers deserve structures, or whether a provisional corridor should become a constructed Route.", "",
        "## Remaining visible generator defects", "",
        "- D8 accumulation still exposes occasional cardinal/diagonal runs and parallel western traces. Principal paths are screened for excessive straightness, but later hydrology needs sub-cell channel shaping and Minecraft water-behavior tests.",
        "- The east remains one continuous ocean boundary. Coves, headlands, flats, shore types, and widths now vary, but estuary/delta behavior and erosion are only opportunities, not simulations.",
        "- Some candidates retain legible west/east landcover organization, and topology families can read more clearly in metrics than at whole-map thumbnail scale.",
        "- Routes are smoothed 5-block terrain-cost polylines. They are less angular than v1 but still analytical centerlines, with no corridor-width, crossing, bridge, or final segment-language decisions.",
        "- Forests are spatial regions rather than trees; opportunity suitability is local 2D relief/developability rather than block-scale sight-line or construction testing.", "",
        "## Known limits", "",
        "This pass can compare heightfield structure, slope, catchments, potential channels, coast morphology, forest regions, corridor geometry, packing, and opportunity location context. It cannot prove vanilla water behavior, cave quality, sight lines, Minecraft block-scale traversal, resource extraction, final Route construction, or gameplay balance. Experimental screens describe visible generator risks; they are not new canonical design thresholds.", "",
        "Stop condition reached: review these greyboxes before any further Minecraft serialization.", "",
    ]
    return "\n".join(lines)
