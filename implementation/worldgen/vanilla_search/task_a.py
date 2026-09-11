"""Task A: deterministic analytical fitting on committed, already oriented grids.

Extends the staged representation and traversal cost. Multi-source Dijkstra is
needed for homeland-edge depth and network distances (the earlier A* returned
only a point-to-point path). No acquisition or world-writing dependencies.
"""
from __future__ import annotations

import heapq
import itertools
import math
from collections import Counter

from successor.grid import rle, unrle
from .evaluate import orient, _choose_home, _components, _distance

PARAMETERS = {
    'homeland_radius_samples': 4, 'fountain_max_grade_y': 2,
    'rise_cost': .5, 'severe_rise_y': 8, 'severe_rise_cost': .8,
    'water_cost': 10., 'nonbuildable_cost': .35, 'canopy_cost': .5,
    'reuse_search_penalty': 3., 'departure_separation_samples': 3,
    'starter_target': 65., 'starter_soft_min': 58., 'starter_soft_max': 76.,
    'target_min_depth': 110., 'target_reference_depth': 250.,
    'regional_core_min_samples': 12, 'shortcut_ratio': .85,
}
UNAVAILABLE = {
    'resource_equivalence': 'No ore, food, animal, crop or full homeland resource portfolios in the committed grid.',
    'water_depth_and_fords': 'Water mask and terrain height do not provide a reliable water-column depth or crossing clearance.',
    'cave_mouths_ravines_natural_bridges': 'Section-palette presence cannot locate entrances, tunnels, overhangs or bridge geometry.',
    'block_walkability_and_construction_volume': 'Eight-block samples cannot prove collision clearance, exact grades, road earthwork or tree-removal volume.',
    'visibility_and_landmark_prominence': 'No sightline/canopy-volume analysis; landmarks are surface-geography candidates.',
    'true_chokepoints': 'Surface clearance proxies cannot establish mandatory chokepoints with caves, boats and player modification available.',
    'travel_time_hunger_and_level_access': 'Effective blocks are an explicit analytical cost, not measured time, Hunger expenditure or level gates.',
    'physical_network_connectivity': 'Finite surface costs include water opportunities; inferred joins and reachable bands do not prove block-level traversability or grade-separated intersections.',
}


DEPTH_BANDS = ((40, 'fringe'), (70, 'opening'), (110, 'secondary_core'),
                        (150, 'secondary_transition'), (210, 'tertiary_core'),
                        (250, 'deep_transition'), (350, 'deep_core'))


def depth_band(value):
    for limit, name in DEPTH_BANDS:
        if value < limit:
            return name
    return 'deep_core_350_plus' if math.isfinite(value) else 'unreachable'


def shortest(adjacency, sources, blocked=frozenset(), penalty=None):
    """Stable Dijkstra; tie-break by node identity and sorted neighbors."""
    distance = dict(sources); previous = {}; queue = [(d, i) for i, d in sources.items()]
    heapq.heapify(queue)
    while queue:
        d, i = heapq.heappop(queue)
        if d != distance[i]:
            continue
        for q, cost in sorted(adjacency[i]):
            if q in blocked:
                continue
            nd = d + cost * (1 + (penalty.get(q, 0) if penalty else 0))
            if nd < distance.get(q, math.inf):
                distance[q] = nd; previous[q] = i; heapq.heappush(queue, (nd, q))
    return distance, previous


def path_to(previous, target):
    path = [target]
    while path[-1] in previous:
        path.append(previous[path[-1]])
    return list(reversed(path))


class Terrain:
    def __init__(self, candidate, parameters=None):
        self.c = candidate; self.p = {**PARAMETERS, **(parameters or {})}
        g = candidate['feature_grid']; self.w = g['width']; self.h = g['height']
        self.s = g['sample_spacing_blocks']; self.n = self.w * self.h
        if g['order'] != 'oriented row-major':
            raise ValueError('Task A requires the existing oriented representation')
        keys = {'height': 'height_rle', 'water': 'actual_surface_water_rle',
                'biome': 'biome_rle', 'top': 'surface_block_rle',
                'buildable': 'buildable_mask_rle', 'canopy': 'canopy_mask_rle',
                'open': 'open_ground_mask_rle', 'highland': 'highland_mask_rle',
                'forest': 'forest_mask_rle'}
        self.v = {k: unrle(g[v]) for k, v in keys.items()}
        if any(len(v) != self.n for v in self.v.values()):
            raise ValueError('incomplete committed feature grid')
        b = candidate['region']['block_bounds']; s = self.s
        raw = [[(x, z) for x in range(b[0]+s//2, b[1]+1, s)]
               for z in range(b[2]+s//2, b[3]+1, s)]
        o = candidate['orientation']
        oriented=orient(raw, o['rotation_degrees_clockwise'], o['east_west_reflected'])
        if len(oriented)!=self.h or len(oriented[0])!=self.w:
            raise ValueError('orientation/grid dimensions disagree')
        self.coords = sum(oriented, [])
        if len(self.coords) != self.n:
            raise ValueError('bounds/grid dimensions disagree')
        self.adj = [[] for _ in range(self.n)]
        self.grade = [0.] * self.n
        for i in range(self.n):
            for q in self.neighbors(i):
                rise = abs(self.v['height'][q]-self.v['height'][i])
                self.grade[i] = max(self.grade[i], rise)
                length = math.dist(self.xy(i), self.xy(q))*s
                # Symmetric cost makes return travel explicit and comparable.
                factor = (1 + rise*self.p['rise_cost']
                          + (rise*self.p['severe_rise_cost'] if rise>self.p['severe_rise_y'] else 0)
                          + self.p['water_cost']*max(self.v['water'][i], self.v['water'][q])
                          + self.p['nonbuildable_cost']*(2-self.v['buildable'][i]-self.v['buildable'][q])/2
                          + self.p['canopy_cost']*(self.v['canopy'][i]+self.v['canopy'][q])/2)
                self.adj[i].append((q, length*factor))
        self.ocean = [self.v['water'][i] and 'ocean' in self.v['biome'][i] for i in range(self.n)]
        self.ocean_distance = _distance(self.ocean, self.w, self.h)

    def xy(self, i): return (i % self.w, i // self.w)

    def neighbors(self, i):
        x, z = self.xy(i)
        for dz in (-1, 0, 1):
            for dx in (-1, 0, 1):
                if (dx or dz) and 0<=x+dx<self.w and 0<=z+dz<self.h:
                    yield (z+dz)*self.w+x+dx

    def point(self, i):
        x, z = self.coords[i]
        return {'sample': list(self.xy(i)), 'world_xyz': [x, self.v['height'][i]+1, z]}

    def cost(self, a, b): return next(cost for q, cost in self.adj[a] if q==b)

    def length(self, path): return sum(self.cost(a, b) for a, b in zip(path, path[1:]))

    def cells(self, center):
        x,z=self.xy(center);r=self.p['homeland_radius_samples']
        return {zz*self.w+xx for zz in range(z-r,z+r+1) for xx in range(x-r,x+r+1)}


def fit_homeland(t, team):
    rows = [[{'terrain_y': t.v['height'][z*t.w+x], 'biome': t.v['biome'][z*t.w+x],
              'actual_surface_water': t.v['water'][z*t.w+x], 'canopy_overhead': t.v['canopy'][z*t.w+x]}
             for x in range(t.w)] for z in range(t.h)]
    # Reuse the current nine-by-nine homeland search, including terrain relief.
    if t.p['homeland_radius_samples'] != 4:
        raise ValueError('current shared homeland fitter supports a nine-sample footprint')
    picked = _choose_home(rows, team=='north', t.v['buildable'], terrain_fit=True)
    if not picked: return None
    _,x,z,_=picked
    if not (4<=x<t.w-4 and 4<=z<t.h-4): return None
    cells=t.cells(z*t.w+x); heights=[t.v['height'][i] for i in cells]
    perimeter=sorted(i for i in cells if any(q not in cells for q in t.neighbors(i)))
    valid=[i for i in sorted(cells) if not t.v['water'][i] and t.v['buildable'][i]
           and not t.v['canopy'][i] and t.grade[i]<=t.p['fountain_max_grade_y']]
    fountain=min(valid,key=lambda i:(sum(math.dist(t.xy(i),t.xy(q)) for q in perimeter)
                                    + t.grade[i]*8, i)) if valid else None
    quality=sum(t.v['buildable'][i] for i in cells)/len(cells)
    diagnostics={'usable_fraction': round(quality,4), 'open_fraction': round(sum(t.v['open'][i] for i in cells)/len(cells),4),
                 'relief_y': max(heights)-min(heights), 'severe_grade_fraction': round(sum(t.grade[i]>8 for i in cells)/len(cells),4),
                 'water_fraction': round(sum(t.v['water'][i] for i in cells)/len(cells),4),
                 'canopy_fraction': round(sum(t.v['canopy'][i] for i in cells)/len(cells),4),
                 'usable_area_blocks2': sum(t.v['buildable'][i] for i in cells)*t.s*t.s,
                 'dry_boundary_exits': sum(not t.v['water'][i] for i in perimeter)}
    score=quality-.01*diagnostics['relief_y']-.3*diagnostics['canopy_fraction']-diagnostics['water_fraction']
    raw=[t.coords[i] for i in cells]; half=t.s//2
    result={'team':team, 'logical_footprint_samples':[x-4,x+4,z-4,z+4],
            'world_bounds':[min(v[0] for v in raw)-half,max(v[0] for v in raw)+half-1,
                            min(v[1] for v in raw)-half,max(v[1] for v in raw)+half-1],
            'quality':round(score,4), 'diagnostics':diagnostics,
            'fountain':t.point(fountain) if fountain is not None else None,
            'fountain_diagnostics':{'valid_samples':len(valid),'method':'usable dry uncanopied sample minimizing distance to footprint boundary plus local grade'},
            'failures':[] if fountain is not None else ['no_valid_fountain_space']}
    return result, cells, perimeter, fountain


def edge_depth(t, cells):
    sources={}
    for i in sorted(cells):
        for q,cost in t.adj[i]:
            if q not in cells: sources[q]=min(sources.get(q,math.inf),cost/2)
    distance,_=shortest(t.adj,sources,blocked=cells)
    return {**distance, **{i:0. for i in cells}}


def starter(t, outside_path, edge_cost):
    cumulative=[edge_cost]
    for a,b in zip(outside_path,outside_path[1:]): cumulative.append(cumulative[-1]+t.cost(a,b))
    options=[k for k,d in enumerate(cumulative) if t.p['starter_soft_min']<=d<=t.p['starter_soft_max']]
    choices=options or list(range(len(outside_path)))
    k=min(choices,key=lambda k:(abs(cumulative[k]-t.p['starter_target'])
                               + 20*t.v['water'][outside_path[k]]+3*t.v['canopy'][outside_path[k]], k))
    path=outside_path[:k+1]; rise=max((abs(t.v['height'][a]-t.v['height'][b]) for a,b in zip(path,path[1:])),default=0)
    water=sum(t.v['water'][i] for i in path)/len(path);canopy=sum(t.v['canopy'][i] for i in path)/len(path)
    feasibility=max(0.,1-water-.4*canopy-max(0,rise-2)/12)
    return {'sample_path':[list(t.xy(i)) for i in path], 'world_path':[t.point(i)['world_xyz'] for i in path],
            'terminus':t.point(path[-1]), 'effective_blocks_from_edge':round(cumulative[k],3),
            'return_effective_blocks':round(cumulative[k],3), 'round_trip_effective_blocks':round(2*cumulative[k],3),
            'legibility_construction_score':round(feasibility,4), 'max_sample_rise_y':rise,
            'water_fraction':round(water,4),'canopy_fraction':round(canopy,4),
            'failures':(['no_handoff_in_soft_range'] if not options else [])+(['severe_starter_construction'] if feasibility<.5 else [])}, path[-1]


def region_targets(t, role, home_z, north, depths, homes):
    def eligible(i):
        x,z=t.xy(i)
        if i in homes or t.v['water'][i] or depths.get(i,0)<t.p['target_min_depth']: return False
        if (z-home_z)*(1 if north else -1)<=4: return False
        if role=='highland': return x<t.w*.45 and t.v['highland'][i]
        if role=='coast': return x>t.w*.5 and t.ocean_distance[i]<=2
        return t.w*.25<x<t.w*.75 and (t.v['open'][i] or (t.v['forest'][i] and not t.v['canopy'][i]))
    # Connected geographic opportunities; representatives are never authored POIs.
    unseen={i for i in range(t.n) if eligible(i)}; groups=[]
    while unseen:
        start=min(unseen);unseen.remove(start);group=[start];stack=[start]
        while stack:
            for q in t.neighbors(stack.pop()):
                if q in unseen: unseen.remove(q);stack.append(q);group.append(q)
        groups.append(sorted(group))
    targets=[]
    for group in sorted(groups,key=lambda g:(-len(g),g[0]))[:12]:
        x=sum(t.xy(i)[0] for i in group)/len(group);z=sum(t.xy(i)[1] for i in group)/len(group)
        i=min(group,key=lambda i:(math.dist(t.xy(i),(x,z))+t.grade[i]*.25,i))
        targets.append((i,len(group)))
    return targets


def fit_routes(t, team, homeland, home_cells, perimeter, fountain, depths, all_homes):
    if fountain is None: return [],[]
    inside_dist,inside_prev=shortest(t.adj,{fountain:0.},blocked=set(range(t.n))-home_cells)
    center=t.xy(fountain);sign=1 if team=='north' else -1
    departures=[];routes=[];paths=[];used={}
    for slot,role in enumerate(('highland','interior','coast')):
        desired=(center[0]+(-4 if slot==0 else 4 if slot==2 else 0),center[1]+(4*sign if slot==1 else 0))
        pairs=[(i,q) for i in perimeter for q in t.neighbors(i) if q not in all_homes
               and not t.v['water'][i] and not t.v['water'][q] and i in inside_dist
               and all(math.dist(t.xy(i),t.xy(old))>=t.p['departure_separation_samples'] for old in departures)]
        if not pairs:
            routes.append({'id':f'{team}-{slot+1}','team':team,'role':role,'failures':['no_distinct_dry_departure']});continue
        gate,exit_cell=min(pairs,key=lambda iq:(math.dist(t.xy(iq[0]),desired)*t.s+t.cost(*iq)*.3+t.grade[iq[1]]*2,iq))
        departures.append(gate)
        _,prev=shortest(t.adj,{exit_cell:0.},blocked=all_homes,penalty=used)
        targets=region_targets(t,role,center[1],team=='north',depths,all_homes)
        viable=[]
        for target,area in targets:
            path=path_to(prev,target)
            if path[0]!=exit_cell: continue
            cost=t.length(path)+t.cost(gate,exit_cell)/2
            rank=abs(cost-t.p['target_reference_depth'])*.02+cost*.005-min(area,200)*.05
            viable.append((rank,target,path))
        if not viable:
            routes.append({'id':f'{team}-{slot+1}','team':team,'role':role,'departure':t.point(gate),'failures':['no_reachable_geographic_target']});continue
        _,target,outside=min(viable,key=lambda v:(v[0],v[1]))
        full=path_to(inside_prev,gate)+outside
        early,terminus=starter(t,outside,t.cost(gate,exit_cell)/2)
        # Include the edge-to-first-sample grade in the feasibility assessment.
        edge_rise=abs(t.v['height'][gate]-t.v['height'][exit_cell])
        early['max_sample_rise_y']=max(early['max_sample_rise_y'],edge_rise)
        early['legibility_construction_score']=round(max(0.,early['legibility_construction_score']-max(0,edge_rise-2)/12),4)
        if early['legibility_construction_score']<.5 and 'severe_starter_construction' not in early['failures']: early['failures'].append('severe_starter_construction')
        a=t.point(gate)['world_xyz'];b=t.point(exit_cell)['world_xyz']
        early['homeland_edge_xyz']=[(x+y)/2 for x,y in zip(a,b)]
        early['homeland_facing_path']=[t.point(i)['world_xyz'] for i in path_to(inside_prev,gate)]
        supported=full[:full.index(terminus)+1]
        early['supported_sample_path']=[list(t.xy(i)) for i in supported]
        early['supported_world_path']=[t.point(i)['world_xyz'] for i in supported]
        effective=t.length(outside)+t.cost(gate,exit_cell)/2
        physical=sum(math.dist(t.xy(a),t.xy(b))*t.s for a,b in zip(full,full[1:]))
        quality=physical/max(physical,t.length(full))
        route={'id':f'{team}-{slot+1}','team':team,'role':role,'departure':t.point(gate),'target':t.point(target),
               'representation':'designated corridor; sample spine is a reference, not a physical road design',
               'sample_path':[list(t.xy(i)) for i in full],'world_path':[t.point(i)['world_xyz'] for i in full],
               'physical_path_blocks':round(physical,3),'effective_blocks_from_fountain':round(t.length(full),3),
               'effective_blocks_from_edge':round(effective,3),'corridor_quality_score':round(quality,4),
               'starter':early,'failures':list(early['failures'])}
        routes.append(route);paths.append((route,full,outside,terminus))
        for i in outside:
            used[i]=t.p['reuse_search_penalty']
            for q in t.neighbors(i): used[q]=max(used.get(q,0),t.p['reuse_search_penalty']/2)
    return routes,paths


def divergence(paths, team, t):
    own=[p for p in paths if p[0]['team']==team]
    termini=[p[3] for p in own]
    pairs=[math.dist(t.xy(a),t.xy(b))*t.s for a,b in itertools.combinations(termini,2)]
    openings=[set(p[2][:p[2].index(p[3])+1]) for p in own]
    overlaps=[len(a&b)/max(1,min(len(a),len(b))) for a,b in itertools.combinations(openings,2)]
    return {'distinct_departures':len({tuple(p[0]['departure']['sample']) for p in own}),
            'minimum_starter_terminus_separation_blocks':round(min(pairs,default=0),3),
            'maximum_opening_overlap_fraction':round(max(overlaps,default=1),4),
            'differentiated':len(own)==3 and min(pairs,default=0)>=t.s*3 and max(overlaps,default=1)<.5}


def network_graph(t, paths):
    graph={};membership={}
    for route,path,*_ in paths:
        for a,b in zip(path,path[1:]):
            ax,az=t.xy(a);bx,bz=t.xy(b)
            nodes=[(2*ax,2*az),(ax+bx,az+bz),(2*bx,2*bz)]
            for node in nodes: membership.setdefault(node,set()).add(route['id']);graph.setdefault(node,{})
            for u,v in zip(nodes,nodes[1:]): graph[u][v]=graph[v][u]=t.cost(a,b)/2
    return {k:sorted(v.items()) for k,v in graph.items()},membership


def network_analysis(t, paths, all_homes):
    graph,members=network_graph(t,paths)
    def outside(node): return round(node[1]/2)*t.w+round(node[0]/2) not in all_homes
    shared={n for n,m in members.items() if len(m)>1 and outside(n)}
    intersections=[]
    while shared:
        start=min(shared);shared.remove(start);group=[start];stack=[start]
        while stack:
            for q,_ in graph[stack.pop()]:
                if q in shared: shared.remove(q);stack.append(q);group.append(q)
        representative=min(group,key=lambda n:(sum(math.dist(n,q) for q in group),n))
        intersections.append({'logical_sample':[v/2 for v in representative],
                              'routes':sorted(set().union(*(members[n] for n in group))),
                              'shared_network_vertices':len(group),'evidence':'sample-spine intersection/overlap; grade separation unverified'})
    connections=[];shortcuts=[]
    # One candidate connection per Route pair, between their early/middle spines.
    for left,right in itertools.combinations(paths,2):
        la=left[2];ra=right[2]
        anchors_l=sorted(set((left[3],la[len(la)//2])))
        anchors_r=sorted(set((right[3],ra[len(ra)//2])))
        a,b=min(itertools.product(anchors_l,anchors_r),key=lambda ab:(math.dist(t.xy(ab[0]),t.xy(ab[1])),ab))
        if a==b: continue
        d,prev=shortest(t.adj,{a:0.},blocked=all_homes)
        if b not in d: continue
        path=path_to(prev,b);u=tuple(2*v for v in t.xy(a));v=tuple(2*v for v in t.xy(b))
        existing,_=shortest(graph,{u:0.})
        prior=existing.get(v,math.inf)
        record={'id':f'connection-{len(connections)+1}','routes':[left[0]['id'],right[0]['id']],
                'from':t.point(a),'to':t.point(b),'sample_path':[list(t.xy(i)) for i in path],
                'world_path':[t.point(i)['world_xyz'] for i in path],'effective_blocks':round(d[b],3),
                'existing_network_effective_blocks':round(prior,3) if math.isfinite(prior) else None,
                'lateral':abs(t.xy(a)[0]-t.xy(b)[0])>=abs(t.xy(a)[1]-t.xy(b)[1]),
                'evidence':'additional terrain-cost connection; no construction or new movement mechanic'}
        connections.append(record)
        if math.isfinite(prior) and d[b]<prior*t.p['shortcut_ratio']:
            shortcuts.append({'connection':record['id'],'effective_saving_blocks':round(prior-d[b],3),'kind':'surface bypass relative to fitted Route network'})
    remaining=set(graph);components=0
    while remaining:
        start=min(remaining);seen,_=shortest(graph,{start:0.});remaining-=seen.keys();components+=1
    return intersections,connections,shortcuts,components


def depth_summary(t, depths, regions, excluded=frozenset()):
    counts=Counter(depth_band(depths.get(i,math.inf)) for i in range(t.n) if not t.v['water'][i] and i not in excluded)
    cores={}
    for role,bands in (('secondary',{'secondary_core'}),('tertiary',{'tertiary_core'}),('deep',{'deep_core','deep_core_350_plus'})):
        mask=[i not in excluded and not t.v['water'][i] and depth_band(depths.get(i,math.inf)) in bands for i in range(t.n)]
        sizes=_components(mask,t.w,t.h)
        landmarks=[r['id'] for r in regions if depth_band(depths.get(r['sample'][1]*t.w+r['sample'][0],math.inf)) in bands]
        cores[role]={'land_samples':sum(mask),'largest_connected_patch_samples':max(sizes,default=0),
                     'represented_geographies':landmarks,'meaningful_area_available':max(sizes,default=0)>=t.p['regional_core_min_samples'],
                     'geographic_interest_diagnostic':'regional reference present' if landmarks else 'no regional representative in band; geographic interest unverified'}
    codes=['fringe','opening','secondary_core','secondary_transition','tertiary_core','deep_transition','deep_core','deep_core_350_plus','unreachable']
    return {'land_band_counts':dict(sorted(counts.items())),'core_access':cores,
            'land_statistics_scope':'dry sampled Wilderness excluding both homeland footprints; depth grid retains all samples',
            'depth_grid':{'order':'oriented row-major','width':t.w,'height':t.h,'band_codes':codes,
                          'bands_rle':rle([codes.index(depth_band(depths.get(i,math.inf))) for i in range(t.n)])},
            'geography_depths':[{'id':r['id'],'effective_blocks':round(depths[r['sample'][1]*t.w+r['sample'][0]],3),
                                'band':depth_band(depths[r['sample'][1]*t.w+r['sample'][0]])} for r in regions],
            'compressed_geography_fraction':round(sum(depths[r['sample'][1]*t.w+r['sample'][0]]<70 for r in regions)/max(1,len(regions)),4)}


def fit(candidate, parameters=None, route_revision=None):
    t=Terrain(candidate,parameters);failures=[];homes={};internal={}
    for team in ('north','south'):
        result=fit_homeland(t,team)
        if result is None:
            failures.append(f'{team}: no homeland footprint');continue
        homes[team]=result[0];internal[team]=result[1:]
        failures.extend(f'{team}: {f}' for f in result[0]['failures'])
    base={'schema':'default_task_a_v1','status':'Working analytical fit; not a design recommendation',
          'seed':candidate['seed'],'source_bounds':candidate['region']['block_bounds'],
          'logical_orientation':candidate['orientation'],'playable_bounds':candidate['region']['block_bounds'],
          'bounds_decision':'retain full existing window to preserve useful geography','parameters':t.p,
          'uncertainties':UNAVAILABLE,'homelands':homes,'failures':failures}
    if len(internal)!=2 or any(v[2] is None for v in internal.values()):
        return {**base,'routes':[{'id':f'{team}-{k}','team':team,'starter':None,'failures':['homeland_or_fountain_unavailable']} for team in ('north','south') for k in (1,2,3)],
                'regional_depth':{},'intersections':[],'cross_connections':[],'crossings':[],
                'chokepoints':[],'shortcuts':[],'key_locations':[],'landmarks':[],
                'center':None,'network_metrics':{'complete_skeleton':False}}
    all_homes=set.union(*(v[0] for v in internal.values()));routes=[];paths=[];depths={}
    for team,(cells,perimeter,fountain) in internal.items():
        depths[team]=edge_depth(t,cells)
        fitted,p=fit_routes(t,team,homes[team],cells,perimeter,fountain,depths[team],all_homes)
        routes+=fitted;paths+=p
    if route_revision is not None:
        routes,paths=route_revision(t,routes,paths,all_homes)
    intersections,connections,shortcuts,components=network_analysis(t,paths,all_homes)
    landmarks=[]
    for region in candidate.get('greybox',{}).get('landscape_regions',[]):
        x,z=region['logical_sample'];i=z*t.w+x
        landmarks.append({'id':region['id'],'kind':region['role'],**t.point(i),
                          'area_blocks2':region['area_blocks2'],'evidence':'existing extracted connected surface region; visual prominence unavailable'})
    crossings=[];chokes=[];seen=set()
    clearance=_distance([t.v['water'][i] or t.grade[i]>8 for i in range(t.n)],t.w,t.h)
    for route,_,outside,_ in paths:
        run=[]
        for i in outside+[None]:
            if i is not None and t.v['water'][i]: run.append(i)
            elif run:
                q=run[len(run)//2];crossings.append({'id':f'crossing-{len(crossings)+1}',**t.point(q),'route':route['id'],
                    'kind':'river crossing candidate' if 'river' in t.v['biome'][q] else 'surface-water crossing candidate',
                    'water_samples':len(run),'depth':None,'evidence':'actual sampled water on corridor; ford/bridge feasibility unavailable'});run=[]
        for i in outside:
            if i in seen or t.v['water'][i] or t.grade[i]>8 or clearance[i]>1:continue
            favorable=sum(not t.v['water'][q] and t.grade[q]<=8 for q in t.neighbors(i))
            if favorable<=3:
                chokes.append({'id':f'neck-{len(chokes)+1}',**t.point(i),'route':route['id'],
                               'kind':'narrow surface approach candidate','mandatory':None,'evidence':'at most three favorable neighboring samples; alternate modes unmeasured'})
                seen.update(t.neighbors(i));seen.add(i)
    keys=[{**c,'kind':c['kind']} for c in crossings+chokes]
    for r in landmarks:
        if r['kind'] in ('open country','coast','inland water'):
            i=r['sample'][1]*t.w+r['sample'][0]
            if any(min(math.dist(t.xy(i),t.xy(q)) for q in p[2])*t.s<=40 for p in paths):
                keys.append({**r,'kind':r['kind']+' network-adjacent geography','gameplay_assignment':None})
    # Center is a sampled junction area selected after the traversal network.
    candidates=[i for i in range(t.n) if t.w//3<=i%t.w<2*t.w//3 and t.h//3<=i//t.w<2*t.h//3 and not t.v['water'][i]]
    def center_rank(i):
        nearby=sum(any(math.dist(t.xy(i),t.xy(q))*t.s<=96 for q in p[2]) for p in paths)
        return (-nearby,-t.v['open'][i],t.grade[i],abs(i//t.w-t.h/2)+abs(i%t.w-t.w/2),i)
    center_i=min(candidates,key=center_rank) if candidates else None
    near=[p[0]['id'] for p in paths if center_i is not None and any(math.dist(t.xy(center_i),t.xy(q))*t.s<=96 for q in p[2])]
    center={'reference':t.point(center_i) if center_i is not None else None,'reference_radius_blocks':96,
            'nearby_routes':near,'bypassing_routes':[p[0]['id'] for p in paths if p[0]['id'] not in near],
            'role':'junction/reference geography only','starter_termini_inside_reference':[p[0]['id'] for p in paths if center_i is not None and math.dist(t.xy(center_i),t.xy(p[3]))*t.s<=96]}
    divergences={team:divergence(paths,team,t) for team in homes}
    center['nearby_intersections']=sum(center_i is not None and math.dist(t.xy(center_i),r['logical_sample'])*t.s<=96 for r in intersections)
    center['nearby_cross_connections']=[c['id'] for c in connections if center_i is not None and any(math.dist(t.xy(center_i),q)*t.s<=96 for q in c['sample_path'])]
    regional={team:depth_summary(t,d,landmarks,all_homes) for team,d in depths.items()}
    for team in homes:
        if not divergences[team]['differentiated']: failures.append(f'{team}: insufficient early Route divergence')
        for role,diagnostic in regional[team]['core_access'].items():
            if not diagnostic['meaningful_area_available']: failures.append(f'{team}: missing or fragmented {role} depth')
        if regional[team]['compressed_geography_fraction']>.5:failures.append(f'{team}: geographic references compressed near home')
    for r in routes: failures.extend(f'{r["id"]}: {f}' for f in r['failures'])
    for r in routes: r.setdefault('starter',None)
    if center_i is None or len(near)<3: failures.append('weak central/interior network connectivity')
    if center['starter_termini_inside_reference']:failures.append('Starter Route reaches central reference area')
    quality=sum(p[0]['corridor_quality_score'] for p in paths)/max(1,len(paths))
    route_cells={q for p in paths for q in p[1]}
    if abs(homes['north']['quality']-homes['south']['quality'])>.2: failures.append('homeland terrain-quality disparity exceeds 0.2')
    metrics={'complete_skeleton':len(paths)==6,'homeland_quality':{k:v['quality'] for k,v in homes.items()},
             'homeland_quality_difference':round(abs(homes['north']['quality']-homes['south']['quality']),4),
             'resource_equivalence':None,'route_divergence':divergences,'route_network_quality':round(quality,4),
             'starter_legibility':{p[0]['id']:p[0]['starter']['legibility_construction_score'] for p in paths},
             'route_network_components':components,'cross_connections':len(connections),'lateral_connections':sum(c['lateral'] for c in connections),
             'intersections':len(intersections),'surface_shortcuts':len(shortcuts),'center_nearby_routes':len(near),
             'off_route_available_land_fraction':round(sum(not t.v['water'][i] and i not in route_cells and i not in all_homes for i in range(t.n))/t.n,4),
             'highland_access':{team:any(p[0]['team']==team and p[0]['role']=='highland' for p in paths) for team in homes},
             'coast_access':{team:any(p[0]['team']==team and p[0]['role']=='coast' for p in paths) for team in homes},
             'regional_depth_access':{team:r['core_access'] for team,r in regional.items()}}
    return {**base,'routes':routes,'regional_depth':regional,'intersections':intersections,
            'cross_connections':connections,'crossings':crossings,'chokepoints':chokes,'shortcuts':shortcuts,
            'key_locations':keys,'landmarks':landmarks,'center':center,'network_metrics':metrics,
            'failures':sorted(set(failures))}
