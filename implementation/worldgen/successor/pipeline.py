"""Orchestration, candidate scoring, diversity preservation, and serialization."""

from __future__ import annotations

import json
import math
from pathlib import Path

from .analysis import opportunity_fairness, packing_analysis, traversal_analysis
from .config import NORTH_HOME, NX, NZ, SCALE_MODEL, SOUTH_HOME
from .ecology import generate_ecology
from .grid import rle
from .routes import generate_routes
from .terrain import generate_terrain
from .validation import validate


def generate_candidate(seed: int) -> dict:
    terrain = generate_terrain(seed)
    ecology = generate_ecology(seed, terrain)
    routes = generate_routes(seed, terrain, ecology)
    traversal = traversal_analysis(terrain, routes, ecology)
    candidate = {
        "schema_version": 1, "generator": "successor_default_candidate_v1", "seed": seed,
        "design_status": "prototype/test candidate; not selected Default",
        "scale_model": SCALE_MODEL, "homelands": {"north":list(NORTH_HOME),"south":list(SOUTH_HOME)},
        "terrain": terrain, "ecology": ecology, "routes": routes, "traversal": traversal,
    }
    candidate["packing"] = packing_analysis(terrain, routes, ecology)
    candidate["fairness"] = opportunity_fairness(ecology, traversal)
    candidate["validation"] = validate(candidate)
    candidate["summary"] = summarize(candidate)
    return candidate


def summarize(c: dict) -> dict:
    terrain, ecology, routes = c["terrain"], c["ecology"], c["routes"]
    kinds={k:terrain["terrain"].count(k) for k in set(terrain["terrain"])}
    mountain_heights=[h for h,m in zip(terrain["heights"],terrain["mountain_mask"]) if m]
    coast=c["terrain"]["coast_boundary"]
    access_summary={}
    for team in ("north","south"):
        values=list(c["traversal"]["teams"][team]["access"].values())
        costs=sorted(x["path_cost"] for x in values)
        access_summary[team]={"median_path_cost":round(costs[len(costs)//2],2),"mean_detour_ratio":round(sum(x["detour_ratio"] for x in values)/len(values),3),"L1_round_trip_viable_fraction":round(sum(x["round_trip"]["L1_pre_cutoff_viable_with_20pct_operating_reserve"] for x in values)/len(values),3),"L3_round_trip_viable_fraction":round(sum(x["round_trip"]["L3_pre_cutoff_viable_with_20pct_operating_reserve"] for x in values)/len(values),3)}
    return {
        "topology_family":terrain["topology_family"],
        "major_terrain_characteristics":{"land_cells":sum(k!="ocean" for k in terrain["terrain"]),"terrain_cell_counts":dict(sorted(kinds.items())),"forest_regions":len(ecology["forest_regions"])},
        "mountain_characteristics":{"elevation_min":min(mountain_heights),"elevation_max":max(mountain_heights),"depth_blocks":c["validation"]["descriptive_metrics"]["mountain_depth_blocks"],"commitment_gain_y":c["validation"]["descriptive_metrics"]["mountain_commitment_gain_y"],"feature_markers":terrain["mountain_feature_markers"],"cold_ecology_instances":sum(x["type"]=="snow" for x in ecology["instances"]),"caves":sum(x["type"]=="cave" and x["category"]!="homeland_baseline" for x in ecology["instances"])},
        "hydrology_summary":{"features":len(terrain["hydrology"]),"total_length_blocks":sum(x["length_blocks"] for x in terrain["hydrology"]),"mean_downhill_fraction":round(sum(x["downhill_fraction"] for x in terrain["hydrology"])/max(1,len(terrain["hydrology"])),3),"spill_cuts":sum(x["spill_cuts"] for x in terrain["hydrology"])},
        "coast_characteristics":{"boundary_x_min":min(coast),"boundary_x_max":max(coast),"variation_blocks":max(coast)-min(coast),"coast_cells":kinds.get("coast",0),"ocean_cells":kinds.get("ocean",0)},
        "route_topology":{"branches_per_team":3,"branch_lengths":[x["length_blocks"] for x in routes["branches"]],"visual_status":"provisional analytical only"},
        "resource_ecology":{"instances":len(ecology["instances"]),"livestock_ranges":len(ecology["livestock_ranges"]),"opportunity_regions":len(ecology["opportunity_regions"]),"villages":sum(x["category"]=="settlement" for x in ecology["instances"]),"major_pois":sum(x["category"]=="major_poi" for x in ecology["instances"]),"minor_pois":sum(x["category"]=="minor_poi" for x in ecology["instances"]),"corrections":len(ecology["corrections"])},
        "terrain_aware_accessibility":access_summary,
        "major_validator_warnings":c["validation"]["experimental_thresholds"]["warnings"],
    }


def score(c):
    warnings=len(c["validation"]["experimental_thresholds"]["warnings"])
    p=c["packing"]; f=c["fairness"]; d=c["validation"]["descriptive_metrics"]
    return round(100 - warnings*9 - f["mean_absolute_bias"]*30 - f["artificial_correction_count"]*2 + min(8,d["mountain_depth_blocks"]/30) + min(6,p["mean_nearest_opportunity_distance"]/20),3)


def _fingerprint(c):
    p=c["packing"]; d=c["validation"]["descriptive_metrics"]; s=c["summary"]
    return [d["mountain_depth_blocks"]/200, d["forest_interior_depth_blocks"]/80, p["mean_nearest_opportunity_distance"]/100,
        p["space_overlays"].get("deep_off_route_wilderness",{}).get("fraction_of_land",0)*4,
        s["coast_characteristics"]["variation_blocks"]/100, c["fairness"]["mean_absolute_bias"]*3]


def _distance(a,b): return math.sqrt(sum((x-y)**2 for x,y in zip(_fingerprint(a),_fingerprint(b))))


def search_candidates(start_seed: int, attempts: int = 24, finalists: int = 4) -> tuple[list[dict], dict]:
    evaluated=[generate_candidate(start_seed+i) for i in range(attempts)]
    eligible=[c for c in evaluated if c["validation"]["pass"]]
    ranked=sorted(eligible,key=score,reverse=True)
    selected=[]
    if ranked: selected.append(ranked[0])
    while len(selected)<min(finalists,len(ranked)):
        pool=[c for c in ranked if c not in selected]
        different=[c for c in pool if c["terrain"]["topology_family"] not in {x["terrain"]["topology_family"] for x in selected}]
        if different: pool=different
        choice=max(pool,key=lambda c: min(_distance(c,x) for x in selected) + score(c)/250)
        selected.append(choice)
    reasons={"long_ridge":"deep_off_route_long_ridge_stress","offset_basin":"coast_variation_and_route_dependence","broken_peaks":"fragmented_mountain_low_route_share","highland_ravines":"ravine_traversal_stress","horseshoe":"basin_and_multiple_approach_structure"}
    for index,c in enumerate(selected):
        label="best_balanced_evidence" if index==0 else reasons.get(c["terrain"]["topology_family"],"diverse_topology_and_metric_profile")
        c["selection"]={"search_score":score(c),"survival_reason":label,"minimum_finalist_fingerprint_distance":round(min((_distance(c,x) for x in selected if x is not c),default=0),3)}
    report={"generator":"successor_default_candidate_v1","start_seed":start_seed,"attempts":attempts,"hard_contract_passes":len(eligible),"shortlist_size":len(selected),
        "selection_method":"First retain strongest multi-metric evidence, then maximize metric/topology diversity with a modest quality term; not aggregate top-N.",
        "hard_rejected":[{"seed":c["seed"],"hard_failures":c["validation"]["hard_contract"]["failures"],"experimental_warnings":c["validation"]["experimental_thresholds"]["warnings"]} for c in evaluated if not c["validation"]["pass"]],
        "eligible_not_shortlisted":[{"seed":c["seed"],"topology":c["terrain"]["topology_family"],"score":score(c),"experimental_warnings":c["validation"]["experimental_thresholds"]["warnings"]} for c in evaluated if c["validation"]["pass"] and c not in selected],
        "evaluated_summary":[{"seed":c["seed"],"topology":c["terrain"]["topology_family"],"score":score(c),"hard_pass":c["validation"]["pass"],"warnings":c["validation"]["experimental_thresholds"]["warnings"]} for c in evaluated]}
    return selected,report


def serializable(c: dict) -> dict:
    out={k:v for k,v in c.items() if k not in ("terrain","routes")}
    out["terrain"]={k:v for k,v in c["terrain"].items() if k not in ("heights","terrain","mountain_mask")}
    out["terrain"]["grid_encoding"]={"order":"row-major z then x","nx":NX,"nz":NZ,"height_rle":rle(c["terrain"]["heights"]),"terrain_rle":rle(c["terrain"]["terrain"]),"mountain_mask_rle":rle(c["terrain"]["mountain_mask"])}
    out["routes"]={k:v for k,v in c["routes"].items() if k!="mask"}
    out["routes"]["mask_rle"]=rle(c["routes"]["mask"])
    return out


def write_results(outdir: Path, selected: list[dict], report: dict):
    outdir.mkdir(parents=True,exist_ok=True)
    for c in selected:
        # Candidate grids are machine artifacts; compact encoding keeps the
        # review commit small while FINALISTS.md remains the human interface.
        (outdir/f"candidate_{c['seed']}.json").write_text(json.dumps(serializable(c),separators=(",",":"))+"\n")
    (outdir/"search_summary.json").write_text(json.dumps(report,indent=2)+"\n")
    (outdir/"FINALISTS.md").write_text(_markdown(selected,report))


def _markdown(selected,report):
    lines=["# Successor Default candidate finalists","","> Status: prototype/test candidates. This comparison does not select the final Default map.","",f"Search evaluated **{report['attempts']}** consecutive seeds and retained **{len(selected)}** using contract fitness plus geographic diversity—not top-N aggregate score.","","| Seed | Family | Mountain depth | Empty / deep off-Route | Packing hotspot | Mean abs bias | Corrections | Warnings | Survived for |","|---:|---|---:|---:|---:|---:|---:|---|---|"]
    for c in selected:
        a=c["packing"]["space_overlays"]; empty=a.get("empty_connective_territory",{}).get("fraction_of_land",0); deep=a.get("deep_off_route_wilderness",{}).get("fraction_of_land",0)
        lines.append(f"| {c['seed']} | {c['terrain']['topology_family']} | {c['validation']['descriptive_metrics']['mountain_depth_blocks']} | {empty:.1%} / {deep:.1%} | {c['packing']['packed_hotspot_max']} | {c['fairness']['mean_absolute_bias']:.3f} | {len(c['ecology']['corrections'])} | {', '.join(c['validation']['experimental_thresholds']['warnings']) or 'none'} | {c['selection']['survival_reason'].replace('_',' ')} |")
    lines += ["","## Candidate tradeoffs",""]
    for c in selected:
        s=c["summary"]; p=c["packing"]; f=c["fairness"]
        n=s["terrain_aware_accessibility"]["north"]; so=s["terrain_aware_accessibility"]["south"]
        lines += [f"### `{c['seed']}` — {s['topology_family']}","",f"- Terrain: mountain Y{s['mountain_characteristics']['elevation_min']}–{s['mountain_characteristics']['elevation_max']}, {s['mountain_characteristics']['depth_blocks']} blocks analytical interior depth, +{s['mountain_characteristics']['commitment_gain_y']} median Y from approach to deep west; {s['major_terrain_characteristics']['forest_regions']} forest regions.",f"- Water/coast: {s['hydrology_summary']['features']} connected drainage features ({s['hydrology_summary']['total_length_blocks']} blocks); coast varies {s['coast_characteristics']['variation_blocks']} blocks.",f"- Opportunities: {s['resource_ecology']['villages']} villages, {s['resource_ecology']['major_pois']} major and {s['resource_ecology']['minor_pois']} minor POIs, {s['resource_ecology']['opportunity_regions']} derived regions.",f"- Packing: mean nearest opportunity {p['mean_nearest_opportunity_distance']} blocks; hotspot {p['packed_hotspot_max']}; unrelated close-overlap pairs {p['unrelated_overlap_pairs']}.",f"- Access: median terrain cost N/S {n['median_path_cost']}/{so['median_path_cost']}; mean detour N/S {n['mean_detour_ratio']}/{so['mean_detour_ratio']}; L3 unsupported round-trip viable N/S {n['L3_round_trip_viable_fraction']:.1%}/{so['L3_round_trip_viable_fraction']:.1%}; portfolio mean absolute bias {f['mean_absolute_bias']}; mean Route share {f['mean_route_share']}.",f"- Why retained: {c['selection']['survival_reason'].replace('_',' ')}. Tradeoff: {', '.join(c['validation']['experimental_thresholds']['warnings']) or 'no experimental warning in this analytical model'}. Backend correction count: {len(c['ecology']['corrections'])}.",""]
    lines += ["## Reading the evidence","","Hard contract failures reject a seed. Experimental thresholds create warnings and are not balance decisions. Descriptive metrics never gate a candidate. Terrain-aware costs are analytical surface estimates; visual review and actual Minecraft traversal remain required before selecting or serializing a playable world.",""]
    return "\n".join(lines)
