"""Terrain-aware traversal, round-trip Hunger, packing, and opportunity analysis."""

from __future__ import annotations

import heapq
import math
from collections import Counter, defaultdict

from .config import CELL_SIZE, NORTH_HOME, NX, NZ, SOUTH_HOME
from .grid import disk_cells, distance_field, index_to_world, inside, world_to_index


def _dijkstra(terrain: dict, routes: dict, home) -> tuple[list[float], list[int | None]]:
    sx, sz = world_to_index(*home); start = sz * NX + sx
    dist = [math.inf] * (NX * NZ); prev = [None] * (NX * NZ); dist[start] = 0.0
    queue = [(0.0, start)]
    while queue:
        cost, current = heapq.heappop(queue)
        if cost != dist[current]: continue
        iz, ix = divmod(current, NX)
        for qx, qz in ((ix-1,iz),(ix+1,iz),(ix,iz-1),(ix,iz+1)):
            if not inside(qx,qz): continue
            q = qz * NX + qx; kind = terrain["terrain"][q]
            if kind == "ocean": continue
            on_route = routes["mask"][current] and routes["mask"][q]
            rise = abs(terrain["heights"][q] - terrain["heights"][current])
            # Routes get no speed multiplier. Their value is clean terrain and geometry.
            slope_factor = 1.0 if on_route else 1.0 + min(2.5, rise * .22)
            surface_factor = 1.0 if on_route else {"water": 2.35, "scree": 1.5, "rock": 1.25, "forest": 1.16, "snow": 1.2}.get(kind, 1.0)
            nc = cost + CELL_SIZE * slope_factor * surface_factor
            if nc < dist[q]:
                dist[q] = nc; prev[q] = current; heapq.heappush(queue, (nc, q))
    return dist, prev


def _journey(index: int, dist: list[float], prev: list[int|None], terrain: dict, routes: dict) -> dict:
    path = []; current = index
    while current is not None:
        path.append(current); current = prev[current]
    path.reverse()
    route_steps = water_steps = difficult_steps = jumps = ascent = descent = 0
    for a, b in zip(path, path[1:]):
        route_steps += int(routes["mask"][a] and routes["mask"][b])
        water_steps += int(terrain["terrain"][b] == "water")
        difficult_steps += int(terrain["terrain"][b] in ("scree","rock","snow","forest"))
        delta = terrain["heights"][b] - terrain["heights"][a]
        ascent += max(0, delta); descent += max(0, -delta)
        jumps += int(delta > 1 and not (routes["mask"][a] and routes["mask"][b]))
    steps = max(1, len(path)-1); length = steps * CELL_SIZE; route_share = route_steps / steps
    outbound_exhaustion = length * .10 + jumps * .20
    outbound_exhaustion -= route_steps * CELL_SIZE * .10 * .10
    round_trip_exhaustion = outbound_exhaustion * 2
    mode = "route_heavy" if route_share >= .67 else "mixed" if route_share >= .25 else "deep_off_route"
    return {
        "path_cost": round(dist[index], 2), "actual_path_length": length,
        "detour_ratio": None, "route_share": round(route_share, 3), "mode": mode,
        "ascent": ascent, "descent": descent, "jump_transitions": jumps,
        "water_blocks": water_steps * CELL_SIZE, "difficult_blocks": difficult_steps * CELL_SIZE,
        "round_trip": {
            "unsupported_exhaustion": round(round_trip_exhaustion, 2),
            "L1_sprint_budget": 16.0, "L3_Hunger_sprint_budget": 24.0,
            "L1_pre_cutoff_viable_with_20pct_operating_reserve": round_trip_exhaustion <= 12.8,
            "L3_pre_cutoff_viable_with_20pct_operating_reserve": round_trip_exhaustion <= 19.2,
            "failure_state_note": "Budget excess predicts loss of sprint/tempo, not inability to walk home.",
        },
    }


def traversal_analysis(terrain: dict, routes: dict, ecology: dict) -> dict:
    team_data = {}
    for team, home in (("north", NORTH_HOME), ("south", SOUTH_HOME)):
        dist, prev = _dijkstra(terrain, routes, home)
        accesses = {}
        for item in ecology["instances"]:
            if item["category"] == "homeland_baseline": continue
            ix, iz = world_to_index(*item["pos"]); index = iz * NX + ix
            report = _journey(index, dist, prev, terrain, routes)
            straight = math.hypot(item["pos"][0]-home[0], item["pos"][1]-home[1])
            report["detour_ratio"] = round(report["actual_path_length"] / max(1, straight), 3)
            accesses[item["id"]] = report
        team_data[team] = {"access": accesses}
    return {
        "model": {
            "status": "prototype/test", "neighbor_model": "4-neighbor 5-block analytical surface",
            "primary_measure": "least terrain-aware path cost, with reconstructed physical path",
            "costs": {"slope": "1 + min(2.5, elevation_step*0.22)", "water": 2.35, "scree": 1.5, "rock": 1.25, "forest": 1.16, "snow": 1.2, "route_speed_bonus": 0.0},
            "hunger": {"L1_food": 10, "L3_food": 12, "saturation": 0, "sprint_cutoff_food": 6, "route_locomotion_exhaustion_reduction": .10, "round_trip": True},
        },
        "teams": team_data,
    }


def opportunity_fairness(ecology: dict, traversal: dict) -> dict:
    biases = []; by_type = defaultdict(list); route_shares = []; category_costs = defaultdict(lambda: [[],[]])
    for item in ecology["instances"]:
        if item["category"] == "homeland_baseline": continue
        n = traversal["teams"]["north"]["access"][item["id"]]
        s = traversal["teams"]["south"]["access"][item["id"]]
        bias = (n["path_cost"] - s["path_cost"]) / (n["path_cost"] + s["path_cost"] + 1e-9)
        biases.append(bias); by_type[item["type"]].append(bias)
        category_costs[item["category"]][0].append(n["path_cost"])
        category_costs[item["category"]][1].append(s["path_cost"])
        route_shares.extend((n["route_share"], s["route_share"]))
    type_means = {kind: round(sum(values)/len(values), 4) for kind, values in sorted(by_type.items())}
    portfolio_biases = {}
    for category,(north,south) in category_costs.items():
        nc=sum(north)/len(north); sc=sum(south)/len(south)
        portfolio_biases[category]=(nc-sc)/(nc+sc+1e-9)
    discovery = {}
    for team in ("north","south"):
        ranked = sorted(traversal["teams"][team]["access"].items(), key=lambda x: x[1]["path_cost"])
        discovery[team] = [identifier for identifier, _ in ranked[:12]]
    top_n, top_s = set(discovery["north"][:8]), set(discovery["south"][:8])
    counts = Counter(x["type"] for x in ecology["instances"] if x["category"] != "homeland_baseline")
    alternatives = {kind: count for kind, count in sorted(counts.items())}
    return {
        "historical_normalized_access_bias": "(north_cost-south_cost)/(north_cost+south_cost)",
        "mean_directional_bias": round(sum(biases)/len(biases), 4),
        "mean_absolute_bias": round(sum(abs(x) for x in portfolio_biases.values())/len(portfolio_biases), 4),
        "maximum_absolute_bias": round(max(map(abs,portfolio_biases.values())), 4),
        "individual_opportunity_mean_absolute_bias": round(sum(abs(x) for x in biases)/len(biases), 4),
        "individual_opportunity_maximum_absolute_bias": round(max(map(abs,biases)), 4),
        "mean_contestedness": round(sum(1-abs(x) for x in biases)/len(biases), 4),
        "directional_bias_by_type": type_means,
        "portfolio_bias_by_category": {k:round(v,4) for k,v in sorted(portfolio_biases.items())},
        "discovery_order": discovery,
        "discovery_top8_overlap": round(len(top_n & top_s) / max(1, len(top_n | top_s)), 3),
        "alternative_opportunities": alternatives,
        "mean_route_share": round(sum(route_shares)/len(route_shares), 3),
        "route_monopolization_fraction": round(sum(x >= .8 for x in route_shares)/len(route_shares), 3),
        "homeland_baseline_viability": _baseline_viability(ecology),
        "artificial_correction_count": len(ecology["corrections"]),
    }


def _baseline_viability(ecology: dict) -> dict:
    required = {"wood","soil","water","stone","coal","iron","cave"}
    output = {}
    for team in ("north","south"):
        found = {x["type"] for x in ecology["instances"] if x.get("team") == team and x["category"] == "homeland_baseline"}
        output[team] = {"pass": required <= found, "missing": sorted(required-found)}
    return output


def packing_analysis(terrain: dict, routes: dict, ecology: dict) -> dict:
    opportunity_points = [tuple(x["pos"]) for x in ecology["instances"] if x["category"] not in ("homeland_baseline", "livestock")]
    opportunity_mask = [False] * (NX*NZ)
    for x,z in opportunity_points:
        ix,iz=world_to_index(x,z); opportunity_mask[iz*NX+ix]=True
    route_dist = distance_field(routes["mask"]); opp_dist = distance_field(opportunity_mask)
    forest_mask = [k == "forest" for k in terrain["terrain"]]; forest_edge = distance_field([not x for x in forest_mask])
    mountain_edge = distance_field([not x for x in terrain["mountain_mask"]])
    category_counts = Counter(); land_cells = 0
    for i, kind in enumerate(terrain["terrain"]):
        if kind == "ocean": continue
        land_cells += 1; x,z=index_to_world(i%NX,i//NX)
        home_distance=min(math.hypot(x-NORTH_HOME[0],z-NORTH_HOME[1]),math.hypot(x-SOUTH_HOME[0],z-SOUTH_HOME[1]))
        if routes["mask"][i]: category="route_corridors"
        elif kind == "coast": category="coast"
        elif kind == "forest" and forest_edge[i]*CELL_SIZE >= 20: category="forest_interiors"
        elif terrain["mountain_mask"][i] and mountain_edge[i]*CELL_SIZE >= 80: category="mountain_interior"
        elif terrain["mountain_mask"][i]: category="mountain_transition"
        elif route_dist[i]*CELL_SIZE >= 125 and home_distance >= 150: category="deep_off_route_wilderness"
        elif 25 <= route_dist[i]*CELL_SIZE <= 100 and abs(x) < 250: category="between_route_wilderness"
        elif opp_dist[i]*CELL_SIZE >= 75 and route_dist[i]*CELL_SIZE >= 25 and home_distance >= 75: category="empty_connective_territory"
        else: category="open_terrain"
        category_counts[category] += 1
    allocation = {key: {"area_blocks2": value*CELL_SIZE*CELL_SIZE, "fraction_of_land": round(value/max(1,land_cells),4)} for key,value in sorted(category_counts.items())}
    overlays = {
        "route_corridors": set(i for i,x in enumerate(routes["mask"]) if x),
        "between_route_wilderness": set(), "deep_off_route_wilderness": set(),
        "forest_interiors": set(), "open_terrain": set(), "mountain_transition": set(),
        "mountain_interior": set(), "coast": set(), "village_poi_influence": set(),
        "livestock_ecology": set(), "crop_opportunities": set(), "ordinary_geology": set(),
        "exceptional_geology_resources": set(), "empty_connective_territory": set(),
    }
    for i,kind in enumerate(terrain["terrain"]):
        if kind=="ocean": continue
        x,z=index_to_world(i%NX,i//NX); home_distance=min(math.hypot(x-NORTH_HOME[0],z-NORTH_HOME[1]),math.hypot(x-SOUTH_HOME[0],z-SOUTH_HOME[1]))
        if 25<=route_dist[i]*CELL_SIZE<=100 and abs(x)<250: overlays["between_route_wilderness"].add(i)
        if route_dist[i]*CELL_SIZE>=125 and home_distance>=150: overlays["deep_off_route_wilderness"].add(i)
        if forest_mask[i] and forest_edge[i]*CELL_SIZE>=20: overlays["forest_interiors"].add(i)
        if kind in ("open","foothill"): overlays["open_terrain"].add(i)
        if terrain["mountain_mask"][i] and mountain_edge[i]*CELL_SIZE<80: overlays["mountain_transition"].add(i)
        if terrain["mountain_mask"][i] and mountain_edge[i]*CELL_SIZE>=80: overlays["mountain_interior"].add(i)
        if kind=="coast": overlays["coast"].add(i)
        if opp_dist[i]*CELL_SIZE>=75 and route_dist[i]*CELL_SIZE>=25 and home_distance>=75: overlays["empty_connective_territory"].add(i)
    influence_map={"settlement":("village_poi_influence",70),"major_poi":("village_poi_influence",70),"minor_poi":("village_poi_influence",45),"crop_starter":("crop_opportunities",35),"ordinary_geology":("ordinary_geology",40),"exceptional_geology":("exceptional_geology_resources",55),"geological_opportunity":("ordinary_geology",45)}
    for item in ecology["instances"]:
        if item["category"] in influence_map:
            key,radius=influence_map[item["category"]]
            overlays[key].update(qz*NX+qx for qx,qz in disk_cells(*item["pos"],radius))
    for region in ecology["livestock_ranges"]:
        overlays["livestock_ecology"].update(qz*NX+qx for qx,qz in disk_cells(*region["center"],region["radius_blocks"]))
    overlay_report={key:{"area_blocks2":len(cells)*25,"fraction_of_land":round(len(cells)/max(1,land_cells),4)} for key,cells in overlays.items()}

    nearest = []; hotspots = []
    for i,p in enumerate(opportunity_points):
        ds=sorted(math.hypot(p[0]-q[0],p[1]-q[1]) for j,q in enumerate(opportunity_points) if i!=j)
        if ds: nearest.append(ds[0])
        hotspots.append(sum(math.hypot(p[0]-q[0],p[1]-q[1]) <= 70 for q in opportunity_points))
    influence = {"village_poi": 70, "livestock": 60, "crop": 35, "ordinary_geology": 40, "exceptional_geology": 55}
    overlap_pairs=0; plausible_pairs=0
    important=[x for x in ecology["instances"] if x["category"] not in ("homeland_baseline","livestock")]
    for i,a in enumerate(important):
        for b in important[i+1:]:
            d=math.hypot(a["pos"][0]-b["pos"][0],a["pos"][1]-b["pos"][1])
            if d <= 55:
                overlap_pairs += 1
                plausible_pairs += int(a["category"] == b["category"] or {a["category"],b["category"]} <= {"ordinary_geology","exceptional_geology","geological_opportunity"})
    return {
        "method": {"status":"prototype/test", "allocation":"exclusive analytical cell classification", "clustering":"65-block single-link derived regions", "hotspot":"important opportunities within 70 blocks including self", "principle":"penalize strategic collapse while allowing plausible ecology/geology clusters"},
        "space_allocation": allocation,
        "space_overlays": overlay_report,
        "ecology_and_opportunity_influence_radii_blocks": influence,
        "opportunity_count": len(opportunity_points), "derived_region_count": len(ecology["opportunity_regions"]),
        "mean_nearest_opportunity_distance": round(sum(nearest)/len(nearest),2),
        "p10_nearest_opportunity_distance": round(sorted(nearest)[max(0,len(nearest)//10-1)],2),
        "packed_hotspot_max": max(hotspots), "packed_hotspot_mean": round(sum(hotspots)/len(hotspots),2),
        "overlap_pairs_within_55": overlap_pairs, "plausibly_related_overlap_pairs": plausible_pairs,
        "unrelated_overlap_pairs": overlap_pairs-plausible_pairs,
    }
