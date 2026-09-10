"""Adapt vanilla samples to functional Default-map geography measurements."""

from __future__ import annotations

import heapq
import math
import time
from collections import Counter, deque

FOREST_WORDS = ("forest", "taiga", "jungle", "grove", "wooded", "pale_garden", "mangrove")
SNOW_WORDS = ("snow", "frozen", "ice_spikes", "grove", "jagged_peaks")


def orient(rows, rotation, reflected):
    raw_h, raw_w = len(rows), len(rows[0])
    logical_w, logical_h = (raw_w, raw_h) if rotation in (0, 180) else (raw_h, raw_w)
    output = []
    for z in range(logical_h):
        row = []
        for x in range(logical_w):
            qx = logical_w - 1 - x if reflected else x
            if rotation == 0: rx, rz = qx, z
            elif rotation == 90: rx, rz = z, raw_h - 1 - qx
            elif rotation == 180: rx, rz = raw_w - 1 - qx, raw_h - 1 - z
            else: rx, rz = raw_w - 1 - z, qx
            row.append(rows[rz][rx])
        output.append(row)
    return output


def _percentile(values, fraction):
    values = sorted(values)
    return values[min(len(values) - 1, round((len(values) - 1) * fraction))]


def _components(mask, width, height):
    unseen = {i for i, value in enumerate(mask) if value}
    sizes = []
    while unseen:
        start = unseen.pop(); stack = [start]; size = 1
        while stack:
            i = stack.pop(); x, z = i % width, i // width
            for qx, qz in ((x-1,z),(x+1,z),(x,z-1),(x,z+1)):
                q = qz * width + qx
                if 0 <= qx < width and 0 <= qz < height and q in unseen:
                    unseen.remove(q); stack.append(q); size += 1
        sizes.append(size)
    return sorted(sizes, reverse=True)


def _distance(mask, width, height):
    values = [10**9] * len(mask); queue = deque()
    for i, value in enumerate(mask):
        if value: values[i] = 0; queue.append(i)
    while queue:
        i = queue.popleft(); x, z = i % width, i // width
        for qx, qz in ((x-1,z),(x+1,z),(x,z-1),(x,z+1)):
            if 0 <= qx < width and 0 <= qz < height:
                q = qz * width + qx
                if values[q] > values[i] + 1: values[q] = values[i] + 1; queue.append(q)
    return values


def _choose_home(rows, north, buildable, terrain_fit=False):
    height, width = len(rows), len(rows[0]); target_z = round(height * (.18 if north else .82))
    best = None
    for z in range(max(5, target_z - height // 8), min(height - 5, target_z + height // 8 + 1)):
        for x in range(width // 5, width * 4 // 5):
            i = z * width + x
            if not buildable[i]: continue
            local = [qz * width + qx for qz in range(z-4,z+5) for qx in range(x-4,x+5)]
            score = sum(buildable[q] for q in local) + len({rows[q//width][q%width]["biome"] for q in local}) * 2 - abs(z-target_z) * .2
            if terrain_fit:
                heights=[rows[q//width][q%width]['terrain_y'] for q in local]
                score -= (max(heights)-min(heights))*.8 + sum(rows[q//width][q%width].get('canopy_overhead',False) for q in local)*.3
            if best is None or score > best[0]: best = (score, x, z, sum(buildable[q] for q in local)/len(local))
    if best is not None:
        return best
    # Keep unsuitable regions measurable instead of crashing the batch. This
    # fallback is penalized by its zero developability and remains descriptive.
    for z in range(max(2, target_z - height // 6), min(height - 2, target_z + height // 6 + 1)):
        for x in range(width // 8, width * 7 // 8):
            cell = rows[z][x]
            if not cell["actual_surface_water"]:
                score = -abs(z-target_z) - abs(x-width//2)*.1
                if best is None or score > best[0]: best = (score, x, z, 0.0)
    return best


def _astar(rows, start, goal, buildable, uncapped_relief=False):
    height, width = len(rows), len(rows[0]); start_i=start[1]*width+start[0]; goal_i=goal[1]*width+goal[0]
    queue=[(0,start_i)]; costs={start_i:0}; previous={}
    while queue:
        _,i=heapq.heappop(queue)
        if i==goal_i: break
        x,z=i%width,i//width; h=rows[z][x]["terrain_y"]
        for dx,dz in ((-1,-1),(0,-1),(1,-1),(-1,0),(1,0),(-1,1),(0,1),(1,1)):
            qx,qz=x+dx,z+dz
            if not (0<=qx<width and 0<=qz<height): continue
            q=qz*width+qx; cell=rows[qz][qx]; rise=abs(cell["terrain_y"]-h)
            relief = rise*.5 + (rise*.8 if rise>8 else 0) if uncapped_relief else min(4,rise*.25)
            water_cost = (10 if uncapped_relief else 2.5) if cell["actual_surface_water"] else 0
            step=math.hypot(dx,dz)*(1+relief+water_cost+(0 if buildable[q] else .35))
            cost=costs[i]+step
            if cost<costs.get(q,math.inf): costs[q]=cost; previous[q]=i; heapq.heappush(queue,(cost+math.hypot(goal[0]-qx,goal[1]-qz),q))
    if goal_i not in costs: return []
    path=[]; i=goal_i
    while i!=start_i: path.append((i%width,i//width)); i=previous[i]
    path.append(start); return list(reversed(path))


def _route_analysis(rows, homes, buildable, uncapped_relief=False, flexible_anchors=False):
    height,width=len(rows),len(rows[0]); anchors=[]
    for fraction in (.18,.5,.82):
        x0=round((width-1)*fraction); candidates=[]
        for z in range(height*2//5,height*3//5):
            for x in range(max(2,x0-8),min(width-2,x0+9)):
                i=z*width+x
                if buildable[i]: candidates.append((abs(x-x0)+abs(z-height//2)*.15,x,z))
        if flexible_anchors:
            # Endpoints are landscape opportunities, never mandatory offshore
            # coordinates. Prefer the principal mainland over isolated islands.
            unseen={(x,z) for z in range(height) for x in range(width) if not rows[z][x]['actual_surface_water']}; groups=[]
            while unseen:
                start=unseen.pop();group={start};stack=[start]
                while stack:
                    x,z=stack.pop()
                    for q in ((x-1,z),(x+1,z),(x,z-1),(x,z+1)):
                        if q in unseen: unseen.remove(q);group.add(q);stack.append(q)
                groups.append(group)
            mainland=max(groups,key=len)
            def target_cost(x,z):
                balance=abs(math.dist((x,z),homes[0])-math.dist((x,z),homes[1]))
                return abs(x-x0)+abs(z-height//2)*2+balance*.3+(0 if buildable[z*width+x] else 6)
            choices=[(target_cost(x,z),x,z) for x,z in mainland if height//3<=z<height*2//3]
            if not choices: choices=[(abs(x-x0)+abs(z-height//2),x,z) for x,z in mainland]
            separated=[q for q in choices if all(math.hypot(q[1]-a[0],q[2]-a[1])>=8 for a in anchors)]
            choices=separated or choices
            anchors.append(min(choices)[1:])
        else:
            anchors.append(min(candidates)[1:] if candidates else (x0,height//2))
    branches=[]
    for team,home in (("north",homes[0]),("south",homes[1])):
        for index,anchor in enumerate(anchors,1):
            path=_astar(rows,home,anchor,buildable,uncapped_relief=uncapped_relief)
            rises=[abs(rows[b[1]][b[0]]["terrain_y"]-rows[a[1]][a[0]]["terrain_y"]) for a,b in zip(path,path[1:])]
            branches.append({"id":f"{team}-route-{index}","team":team,"target_role":("western_highland_approach","central_wilderness","eastern_coast_approach")[index-1],"sample_path":[list(p) for p in path],"raw_world_path":[[rows[z][x]["x"],rows[z][x]["z"]] for x,z in path],"length_blocks":round(sum(math.hypot(b[0]-a[0],b[1]-a[1])*8 for a,b in zip(path,path[1:]))),"water_steps":sum(rows[z][x]["actual_surface_water"] for x,z in path),"mean_step_y":round(sum(rises)/max(1,len(rises)),3),"max_step_y":max(rises,default=0)})
    return {"status":"analytical compatibility only; no Route generated in Minecraft","branches":branches,"all_six_connected":len(branches)==6 and all(b["sample_path"] for b in branches),"mean_water_steps":round(sum(b["water_steps"] for b in branches)/6,2),"maximum_step_y":max(b["max_step_y"] for b in branches)}


def evaluate_orientation(extracted, rotation, reflected, fit_routes=True):
    started=time.perf_counter(); rows=orient(extracted["rows"],rotation,reflected); height,width=len(rows),len(rows[0]); cells=[c for row in rows for c in row]
    land=[not c["actual_surface_water"] for c in cells]; land_heights=[c["terrain_y"] for c in cells if not c["actual_surface_water"]]; median=_percentile(land_heights,.5); high_cut=max(median+12,_percentile(land_heights,.77))
    slopes=[]
    for z in range(height):
        for x in range(width):
            h=cells[z*width+x]["terrain_y"]; slopes.append(max([abs(h-cells[qz*width+qx]["terrain_y"]) for qx,qz in ((x-1,z),(x+1,z),(x,z-1),(x,z+1)) if 0<=qx<width and 0<=qz<height] or [0]))
    forest=[land[i] and any(word in cells[i]["biome"] for word in FOREST_WORDS) for i in range(len(cells))]
    snow=[land[i] and (any(word in cells[i]["biome"] for word in SNOW_WORDS) or "snow" in cells[i]["top_block"]) for i in range(len(cells))]
    buildable=[land[i] and slopes[i]<=2 and cells[i]["terrain_y"]<high_cut for i in range(len(cells))]
    west_all=[i for i in range(len(cells)) if i%width<width//3]; west=[i for i in west_all if land[i]]; center=[i for i in range(len(cells)) if width//3<=i%width<width*2//3 and land[i]]; east=[i for i in range(len(cells)) if i%width>=width*2//3]
    high=[land[i] and cells[i]["terrain_y"]>=high_cut for i in range(len(cells))]
    column_high=[sum(high[z*width+x] for z in range(height))/height for x in range(width)]
    high_depth=max((sum(1 for q in column_high[:x+1] if q>.12) for x in range(width)),default=0)*8
    home_n=_choose_home(rows,True,buildable,terrain_fit=not fit_routes); home_s=_choose_home(rows,False,buildable,terrain_fit=not fit_routes)
    if not home_n or not home_s: return None
    homes=((home_n[1],home_n[2]),(home_s[1],home_s[2])); routes=_route_analysis(rows,homes,buildable) if fit_routes else {"all_six_connected":False,"mean_water_steps":0,"maximum_step_y":0,"branches":[],"status":"deferred to Stage D"}
    water=[c["actual_surface_water"] for c in cells]; water_sizes=_components(water,width,height); forest_sizes=_components(forest,width,height); forest_depth=_distance([not x for x in forest],width,height)
    coast_edges=0
    for z in range(height):
        for x in range(width-1): coast_edges += water[z*width+x] != water[z*width+x+1]
    actual_river=sum(c["actual_surface_water"] and "river" in c["biome"] for c in cells)
    coast_boundaries=[]
    for z in range(height):
        if not rows[z][-1]["actual_surface_water"]:
            continue
        land_x=[x for x in range(width) if not rows[z][x]["actual_surface_water"]]
        if land_x: coast_boundaries.append(max(land_x))
    coast_mean=sum(coast_boundaries)/max(1,len(coast_boundaries)); coast_variation=math.sqrt(sum((x-coast_mean)**2 for x in coast_boundaries)/max(1,len(coast_boundaries)))*8
    coast_turns=0
    deltas=[b-a for a,b in zip(coast_boundaries,coast_boundaries[1:]) if b!=a]
    for a,b in zip(deltas,deltas[1:]): coast_turns += (a<0<b) or (a>0>b)
    river_mouths=0; coastal_buildable=0
    for i,cell in enumerate(cells):
        x,z=i%width,i//width; neighbors=[qz*width+qx for qx,qz in ((x-1,z),(x+1,z),(x,z-1),(x,z+1)) if 0<=qx<width and 0<=qz<height]
        if cell["actual_surface_water"] and "river" in cell["biome"] and any(cells[q]["actual_surface_water"] and "ocean" in cells[q]["biome"] for q in neighbors): river_mouths+=1
        if buildable[i] and any(cells[q]["actual_surface_water"] for q in neighbors): coastal_buildable+=1
    approach_rows=[]
    boundary_x=width//3
    for z in range(height):
        i=z*width+boundary_x; approach_rows.append(land[i] and slopes[i]<=4)
    approach_bands=0; run=0
    for value in approach_rows+[False]:
        if value: run+=1
        elif run: approach_bands+=run>=2; run=0
    western_steep=[i for i in west if slopes[i]>=5]; steep_by_column=Counter(i%width for i in western_steep)
    wall_concentration=max(steep_by_column.values(),default=0)/max(1,len(western_steep))
    east_ocean=sum(cells[i]["actual_surface_water"] and "ocean" in cells[i]["biome"] for i in east)/max(1,len(east)); west_ocean=sum(cells[i]["actual_surface_water"] and "ocean" in cells[i]["biome"] for i in west_all)/max(1,len(west_all)); west_mean=sum(cells[i]["terrain_y"] for i in west)/max(1,len(west)); center_mean=sum(cells[i]["terrain_y"] for i in center)/max(1,len(center))
    home_bias=abs(home_n[3]-home_s[3]); open_fraction=sum(buildable)/max(1,sum(land))
    score=35*east_ocean+12*(east_ocean-west_ocean)+1.5*(west_mean-center_mean)+high_depth/20+min(12,coast_edges*8/500)+min(10,max(forest_sizes,default=0)*64/60000)+open_fraction*12-home_bias*18-routes["mean_water_steps"]*.12-routes["maximum_step_y"]*.4
    metrics={"western_mean_elevation_advantage_y":round(west_mean-center_mean,3),"highland_depth_blocks":high_depth,"highland_fraction_west":round(sum(high[i] for i in west)/max(1,len(west)),4),"local_relief_p90_y":_percentile(slopes,.9),"steep_fraction":round(sum(s>=5 for s in slopes)/len(slopes),4),"western_approach_bands":approach_bands,"western_wall_concentration":round(wall_concentration,4),"snow_cold_cells_west":sum(snow[i] for i in west),"coastline_sample_edge_blocks":coast_edges*8,"east_coast_boundary_variation_blocks":round(coast_variation,2),"east_coast_headland_cove_turns":coast_turns,"coastal_buildable_sample_cells":coastal_buildable,"east_actual_ocean_fraction":round(east_ocean,4),"west_actual_ocean_fraction":round(west_ocean,4),"east_over_west_ocean_advantage":round(east_ocean-west_ocean,4),"actual_surface_water_fraction":round(sum(water)/len(water),4),"actual_river_water_cells":actual_river,"actual_river_mouth_sample_cells":river_mouths,"actual_water_body_count":len(water_sizes),"largest_actual_water_body_blocks2":max(water_sizes,default=0)*64,"forest_region_count":len(forest_sizes),"largest_forest_region_blocks2":max(forest_sizes,default=0)*64,"forest_interior_depth_blocks":max(forest_depth,default=0)*8,"open_buildable_fraction_of_land":round(open_fraction,4),"homeland_developability_bias":round(home_bias,4)}
    checks={"western_relief_advantage":metrics["western_mean_elevation_advantage_y"]>=3,"western_highland_depth":metrics["highland_depth_blocks"]>=150,"eastern_ocean_present":metrics["east_actual_ocean_fraction"]>=.10,"ocean_gradient_points_east":metrics["east_over_west_ocean_advantage"]>=.10,"region_not_mostly_water":metrics["actual_surface_water_fraction"]<=.75,"homelands_reasonably_viable":metrics["homeland_developability_bias"]<=.35,"six_corridors_connect":routes["all_six_connected"]}
    return {"orientation":{"rotation_degrees_clockwise":rotation,"east_west_reflected":reflected,"logical_dimensions_blocks":[width*8,height*8]},"score":round(score,3),"metrics":metrics,"experimental_screen":{"status":"prototype/test; selection screen, not canonical contract","checks":checks,"warnings":[k for k,v in checks.items() if not v]},"homelands":{"north":{"logical_sample":list(homes[0]),"raw_world":[rows[homes[0][1]][homes[0][0]]["x"],rows[homes[0][1]][homes[0][0]]["z"]],"developable_fraction":round(home_n[3],4)},"south":{"logical_sample":list(homes[1]),"raw_world":[rows[homes[1][1]][homes[1][0]]["x"],rows[homes[1][1]][homes[1][0]]["z"]],"developable_fraction":round(home_s[3],4)}},"routes":routes,"evaluation_seconds":round(time.perf_counter()-started,3),"_rows":rows,"_masks":{"forest":forest,"buildable":buildable,"highland":high}}


def evaluate_region(extracted):
    candidates=[evaluate_orientation(extracted,r,f) for r in (0,90,180,270) for f in (False,True)]; candidates=[c for c in candidates if c]
    passing=[c for c in candidates if not c["experimental_screen"]["warnings"]]
    best=max(passing or candidates,key=lambda c:c["score"]); best["orientation_alternatives"]=[{"rotation":c["orientation"]["rotation_degrees_clockwise"],"reflected":c["orientation"]["east_west_reflected"],"score":c["score"],"warnings":c["experimental_screen"]["warnings"]} for c in sorted(candidates,key=lambda c:c["score"],reverse=True)]
    return best
