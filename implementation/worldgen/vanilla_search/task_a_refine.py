"""A.1 semantic refinement of the existing A.0 fit, without changing geometry.

All measurements are sampled analytical evidence, never physical validation.
The same rules apply to every candidate. No acquisition or world-writing code.
"""
import copy
import itertools
import math
from collections import Counter

from successor.grid import rle
from .task_a import Terrain, shortest, path_to, network_graph, depth_band


PARAMETERS = {
    'max_traversable_sample_rise': 4,
    'central_fraction_low': .25, 'central_fraction_high': .75,
    'central_approach_blocks': 32, 'approach_separation_blocks': 24,
    'minimum_shared_area_samples': 12,
    'early_continuation_effective_blocks': 110,
    'corridor_tolerance_samples': 1, 'collapse_overlap': .65,
    'minimum_continuation_blocks': 32, 'minimum_angle_degrees': 20,
    'formation_min_samples': 24, 'formation_band_min_samples': 4,
    'maximum_open_region_fraction': .25,
    'connection_min_blocks': 24, 'connection_max_detour': 1.8,
    'connection_min_efficiency': .25, 'connection_min_novel_fraction': .5,
    'shortcut_ratio': .85, 'shortcut_min_saving': 32,
    'home_low_disparity': .1, 'home_moderate_disparity': .3,
    'home_high_disparity': .5, 'home_min_usable': .6,
    'home_max_water': .15, 'home_max_severe_grade': .25,
    'home_min_dry_exits': 3, 'starter_max_circuity': 2.5,
    'starter_min_score': .5, 'incidental_fraction_warning': .8,
}


def indices(t, path):
    return [z*t.w+x for x, z in path]


def components(graph, cells):
    remaining=set(cells); result=[]
    while remaining:
        start=min(remaining); remaining.remove(start); stack=[start]; group=[]
        while stack:
            i=stack.pop(); group.append(i)
            for q, _ in graph[i]:
                if q in remaining:
                    remaining.remove(q); stack.append(q)
        result.append(sorted(group))
    return sorted(result, key=lambda g: (-len(g), g[0]))


def conservative_graph(t, p, excluded=frozenset()):
    """Dry samples; reject steep edges and diagonal corner cutting.

    Buildability includes an elevation cutoff and must not gate Wilderness travel.
    It remains a soft cost in the shared Terrain model, not a hard exclusion.
    """
    usable={i for i in range(t.n) if i not in excluded and not t.v['water'][i]}
    graph={i:[] for i in usable}
    def allowed(a, b):
        return b in usable and abs(t.v['height'][a]-t.v['height'][b])<=p['max_traversable_sample_rise']
    for i in sorted(usable):
        x,z=t.xy(i)
        for q,cost in t.adj[i]:
            if not allowed(i,q): continue
            xx,zz=t.xy(q)
            if x!=xx and z!=zz:
                corners=(z*t.w+xx,zz*t.w+x)
                if not all(allowed(i,k) and allowed(q,k) for k in corners): continue
            graph[i].append((q,cost))
    return graph


def formations(t, graph, p):
    """Connected observable surface opportunities; no invented passes or caves."""
    masks={name:set() for name in ('highland','coastal margin','waterbank','forest gap/edge','open region')}
    for i in graph:
        neighbors=list(t.neighbors(i))
        if t.v['highland'][i]: masks['highland'].add(i)
        if t.ocean_distance[i]<=2: masks['coastal margin'].add(i)
        if any(t.v['water'][q] and not t.ocean[q] for q in neighbors): masks['waterbank'].add(i)
        if not t.v['canopy'][i] and any(t.v['canopy'][q] for q in neighbors): masks['forest gap/edge'].add(i)
        if t.v['open'][i]: masks['open region'].add(i)
    records=[]; membership={i:set() for i in graph}
    for kind,mask in masks.items():
        for group in components(graph,mask):
            if len(group)<p['formation_min_samples']: continue
            # Vast ordinary open land is traversable, but not a distinctive link.
            bounded=kind!='open region' or len(group)<=len(graph)*p['maximum_open_region_fraction']
            fid=f'{kind.replace(" ","-").replace("/","-")}-{group[0]}'
            record={'id':fid,'kind':kind,'cells':group,'distinct_geography':bounded,
                    'connection_formation':kind!='open region',
                    'evidence':'connected sampled surface mask, not visually validated landform'}
            records.append(record)
            for i in group: membership[i].add(fid)
    return records,membership


def overlap(t, a, b, radius):
    if not a or not b: return 0.
    bb=set(b)
    def touches(i):
        x,z=t.xy(i)
        return any((z+dz)*t.w+x+dx in bb for dz in range(-radius,radius+1)
                   for dx in range(-radius,radius+1) if 0<=x+dx<t.w and 0<=z+dz<t.h)
    return sum(touches(i) for i in a)/len(a)


def continuation(t, route, limit=None):
    path=indices(t,route['sample_path']); end=indices(t,[route['starter']['terminus']['sample']])[0]
    path=path[path.index(end):]; selected=[path[0]]; distance=0.
    for a,b in zip(path,path[1:]):
        distance+=t.cost(a,b)
        if limit is not None and distance>limit: break
        selected.append(b)
    return selected


def route_differentiation(t, routes, membership, p):
    output={}
    for team in ('north','south'):
        own=[r for r in routes if r['team']==team and r.get('starter')]
        pairs=[]
        for a,b in itertools.combinations(own,2):
            sa=indices(t,a['starter']['sample_path']); sb=indices(t,b['starter']['sample_path'])
            ea=continuation(t,a,p['early_continuation_effective_blocks']); eb=continuation(t,b,p['early_continuation_effective_blocks'])
            ba=continuation(t,a); bb=continuation(t,b)
            early=max(overlap(t,ea,eb,p['corridor_tolerance_samples']),overlap(t,eb,ea,p['corridor_tolerance_samples']))
            broad=max(overlap(t,ba,bb,p['corridor_tolerance_samples']),overlap(t,bb,ba,p['corridor_tolerance_samples']))
            ga=set().union(*(membership.get(i,set()) for i in ea)); gb=set().union(*(membership.get(i,set()) for i in eb))
            va=[a['starter']['terminus']['sample'][k]-a['departure']['sample'][k] for k in (0,1)]
            vb=[b['starter']['terminus']['sample'][k]-b['departure']['sample'][k] for k in (0,1)]
            norm=math.hypot(*va)*math.hypot(*vb)
            angle=math.degrees(math.acos(max(-1,min(1,sum(x*y for x,y in zip(va,vb))/norm)))) if norm else None
            enough=min(t.length(ea),t.length(eb))>=p['minimum_continuation_blocks']
            collapsed=enough and early>=p['collapse_overlap'] and broad>=p['collapse_overlap']
            pairs.append({'routes':[a['id'],b['id']],
                          'opening_overlap':round(len(set(sa)&set(sb))/max(1,min(len(sa),len(sb))),4),
                          'terminus_separation_blocks':round(math.dist(t.xy(sa[-1]),t.xy(sb[-1]))*t.s,3),
                          'direction_separation_degrees':round(angle,3) if angle is not None else None,
                          'weak_directional_separation':angle is not None and angle<p['minimum_angle_degrees'],
                          'early_corridor_overlap':round(early,4),'broader_branch_overlap':round(broad,4),
                          'early_geography_a':sorted(ga),'early_geography_b':sorted(gb),
                          'distinct_geography_evidence':bool(ga^gb),
                          'continuation_measurable':enough,'functional_branch_collapse':collapsed})
        distinct=len({tuple(r['departure']['sample']) for r in own})
        output[team]={'distinct_departures':distinct,'pairs':pairs,
                      'three_meaningful_choices_supported':len(own)==3 and distinct==3 and all(
                          q['continuation_measurable'] and not q['functional_branch_collapse'] and q['opening_overlap']<.5
                          and q['terminus_separation_blocks']>=p['approach_separation_blocks'] for q in pairs),
                      'scope':'sampled differentiation evidence; experiential choice awaits human/player review'}
    return output


def homeland_equivalence(homes,p):
    quality={team:home['quality'] for team,home in homes.items()}
    if set(quality)!={'north','south'}: return {'classification':'Unavailable','independently_usable':{},'quality':quality}
    disparity=abs(quality['north']-quality['south'])
    classification=next((label for key,label in (('home_low_disparity','Low'),('home_moderate_disparity','Moderate'),('home_high_disparity','High')) if disparity<=p[key]),'Severe')
    usable={}; checks={}
    for team,home in homes.items():
        d=home['diagnostics']
        checks[team]={'usable_area':d['usable_fraction']>=p['home_min_usable'],
                      'water':d['water_fraction']<=p['home_max_water'],
                      'grades':d['severe_grade_fraction']<=p['home_max_severe_grade'],
                      'exits':d['dry_boundary_exits']>=p['home_min_dry_exits'],
                      'fountain':home['fountain'] is not None}
        usable[team]=all(checks[team].values())
    return {'quality':quality,'absolute_disparity':round(disparity,4),'classification':classification,
            'component_disparities':{k:round(abs(homes['north']['diagnostics'][k]-homes['south']['diagnostics'][k]),4) for k in homes['north']['diagnostics']},
            'independently_usable':usable,'minimum_usability_checks':checks,
            'competitive_equivalence':'not established; terrain evidence only','resource_equivalence':None}


def connection_classification(physical,direct,effective,prior,novel,formation,p):
    coherent=(formation and physical>=p['connection_min_blocks'] and physical/max(direct,1)<=p['connection_max_detour']
              and physical/max(effective,1)>=p['connection_min_efficiency'] and novel>=p['connection_min_novel_fraction'])
    if not coherent: return 'incidental_traversability'
    if prior is None or (effective<prior*p['shortcut_ratio'] and prior-effective>=p['shortcut_min_saving']):
        return 'strategic_shortcut'
    return 'useful_lateral_connection'


def connections(t, routes, graph, regions, p):
    paths=[(r,indices(t,r['sample_path'])) for r in routes if r.get('starter')]
    network,_=network_graph(t,paths)
    route_cells=set(i for _,path in paths for i in path)
    records=[]; seen=set(); cache={}
    # Each observed formation can connect several corridors. No output quota.
    # Open regions are also assessed, but never promoted by mere traversability.
    for region in regions:
        cells=set(region['cells']); local={i:[(q,c) for q,c in graph[i] if q in cells] for i in cells}
        contacts={r['id']:sorted(set(continuation(t,r))&cells) for r in routes if r.get('starter')}
        for left,right in itertools.combinations(sorted(contacts),2):
            if not contacts[left] or not contacts[right]: continue
            # Nearest nontrivial attachment pair; stable ties. Shared samples are
            # intersections, not new cross-connections.
            pairs=((math.dist(t.xy(a),t.xy(b))*t.s,a,b) for a in contacts[left] for b in contacts[right])
            pair=min((v for v in pairs if v[0]>=p['connection_min_blocks']),default=None)
            if pair is None: continue
            direct,a,b=pair; distances,prev=shortest(local,{a:0.})
            if b not in distances: continue
            path=path_to(prev,b); signature=tuple(sorted((tuple(path),tuple(reversed(path))))[0])
            if signature in seen: continue
            seen.add(signature)
            physical=sum(math.dist(t.xy(x),t.xy(y))*t.s for x,y in zip(path,path[1:]))
            u=tuple(2*v for v in t.xy(a)); v=tuple(2*v for v in t.xy(b))
            if u not in cache: cache[u]=shortest(network,{u:0.})[0]
            prior=cache[u].get(v); novel=sum(i not in route_cells for i in path)/len(path)
            kind=connection_classification(physical,direct,distances[b],prior,novel,region['connection_formation'],p)
            records.append({'id':f'link-{len(records)+1}','routes':[left,right],'classification':kind,
                            'formation':region['id'],'formation_kind':region['kind'],
                            'sample_path':[list(t.xy(i)) for i in path],'world_path':[t.point(i)['world_xyz'] for i in path],
                            'effective_blocks':round(distances[b],3),'physical_blocks':round(physical,3),
                            'detour_ratio':round(physical/max(direct,1),4),'novel_fraction':round(novel,4),
                            'existing_network_effective_blocks':round(prior,3) if prior is not None else None,
                            'saving_effective_blocks':round(prior-distances[b],3) if prior is not None else None,
                            'joins_disconnected_branches':prior is None,
                            'lateral':abs(t.xy(a)[0]-t.xy(b)[0])>=abs(t.xy(a)[1]-t.xy(b)[1]),
                            'evidence':'dry sampled formation-restricted path; block-level reality unverified'})
    return records


def central_area(t,routes,graph,links,p):
    x0=int(t.w*p['central_fraction_low']); x1=int(t.w*p['central_fraction_high'])-1
    z0=int(t.h*p['central_fraction_low']); z1=int(t.h*p['central_fraction_high'])-1
    cells={i for i in graph if x0<=i%t.w<=x1 and z0<=i//t.w<=z1}
    groups=components(graph,cells); clusters=[]; cluster_cells={}
    route_paths={r['id']:continuation(t,r) for r in routes if r.get('starter')}
    for index,group in enumerate(groups):
        if len(group)<p['minimum_shared_area_samples']: continue
        # Cost-limited approach through the same conservative terrain graph.
        distances,prev=shortest(graph,{i:0. for i in group})
        entries=[]; represented=[]
        for rid,path in route_paths.items():
            available=[i for i in path if distances.get(i,math.inf)<=p['central_approach_blocks']]
            if not available: continue
            represented.append(rid)
            for i in (available[0],available[-1]):
                entry=path_to(prev,i)[0]
                if not any(math.dist(t.xy(entry),t.xy(old))*t.s<p['approach_separation_blocks'] for old in entries): entries.append(entry)
        teams=sorted({rid.split('-')[0] for rid in represented})
        crossing_x=min(i%t.w for i in group)==x0 and max(i%t.w for i in group)==x1
        crossing_z=min(i//t.w for i in group)==z0 and max(i//t.w for i in group)==z1
        pair_count=len(represented)*(len(represented)-1)//2
        clusters.append({'id':f'central-{index+1}','land_samples':len(group),'routes':represented,'teams':teams,
                         'distinct_approaches':len(entries),'approach_samples':[list(t.xy(i)) for i in entries],
                         'route_pair_connections':pair_count,'north_south_interaction_supported':len(teams)==2 and len(entries)>=2,
                         'west_east_crossing_supported':crossing_x,'north_south_area_crossing':crossing_z,
                         'logical_bounds':[min(i%t.w for i in group),max(i%t.w for i in group),min(i//t.w for i in group),max(i//t.w for i in group)]})
        cluster_cells[f'central-{index+1}']=set(group)
    shared=[c for c in clusters if len(c['teams'])==2]
    halo=math.ceil(p['central_approach_blocks']/t.s)
    nearby={i for i in graph if x0-halo<=i%t.w<=x1+halo and z0-halo<=i//t.w<=z1+halo}
    linked_clusters=[]
    for group in components(graph,nearby):
        group=set(group)
        members=[c for c in clusters if cluster_cells[c['id']]&group]
        if len(members)<2: continue
        teams=sorted({team for c in members for team in c['teams']})
        linked_clusters.append({'clusters':[c['id'] for c in members],'teams':teams,
                                'routes':sorted({r for c in members for r in c['routes']}),
                                'evidence':'connected through dry sample graph inside central area plus approach halo'})
    touching=[c for c in links if c['classification']!='incidental_traversability' and any(i in cells for i in indices(t,c['sample_path']))]
    # A single sampled spine hub is a warning, not a reward for six spokes.
    membership={}
    for rid,path in route_paths.items():
        for i in path:
            if i in cells: membership.setdefault(i,set()).add(rid)
    peak=max((len(v) for v in membership.values()),default=0)
    area_mask=[i in cells for i in range(t.n)]
    return {'logical_bounds':[x0,x1,z0,z1],'traversable_mask_rle':rle(area_mask),
            'clusters':clusters,'cluster_count':len(clusters),'shared_team_cluster_count':len(shared),
            'shared_traversable_area_blocks2':sum(c['land_samples'] for c in shared)*t.s*t.s,
            'routes_approaching':sorted(set(r for c in clusters for r in c['routes'])),
            'teams_represented':sorted(set(team for c in clusters for team in c['teams'])),
            'distinct_approaches':sum(c['distinct_approaches'] for c in clusters),
            'north_south_interaction_supported':any(c['north_south_interaction_supported'] for c in clusters) or any(len(c['teams'])==2 for c in linked_clusters),
            'west_east_crossing_supported':any(c['west_east_crossing_supported'] for c in shared),
            'meaningful_links_through_area':[c['id'] for c in touching],
            'lateral_links_through_area':[c['id'] for c in touching if c['lateral']],
            'maximum_routes_at_one_sample':peak,'overcentralization_warning':peak>=5,
            'nearby_connected_cluster_groups':linked_clusters,
            'cluster_relationship':'nearby clusters connected through halo' if linked_clusters else 'one shared cluster' if len(shared)==1 else 'several separate conservative clusters' if shared else 'no shared team cluster',
            'mandatory_convergence':None,
            'scope':'connected dry sample graph; isolated clusters may have unmeasured bridges/caves/water alternatives'}


def operational_depth(t, homes, graph, regions,p):
    output={}
    for team,home in homes.items():
        x0,x1,z0,z1=home['logical_footprint_samples']
        own={z*t.w+x for z in range(z0,z1+1) for x in range(x0,x1+1)}
        sources={}
        for i in sorted(own):
            if t.v['water'][i] or not t.v['buildable'][i]: continue
            for q,cost in t.adj[i]:
                if q in graph and abs(t.v['height'][i]-t.v['height'][q])<=p['max_traversable_sample_rise']:
                    # Cardinal boundary edges avoid unsupported diagonal exits.
                    if abs(i%t.w-q%t.w)+abs(i//t.w-q//t.w)==1: sources[q]=min(sources.get(q,math.inf),cost/2)
        distances,_=shortest(graph,sources)
        bands={}
        for name,codes in (('secondary',{'secondary_core'}),('tertiary',{'tertiary_core'}),('deep',{'deep_core','deep_core_350_plus'})):
            cells={i for i,d in distances.items() if depth_band(d) in codes}
            patches=components(graph,cells)
            evidence=[]
            for region in regions:
                intersection=set(region['cells'])&cells
                # Require connected support within the band, not a centroid hit.
                overlap_groups=components(graph,intersection)
                largest=len(overlap_groups[0]) if overlap_groups else 0
                if region['distinct_geography'] and largest>=p['formation_band_min_samples']:
                    evidence.append({'formation':region['id'],'kind':region['kind'],'band_samples':len(intersection),'largest_patch_samples':largest})
            traversable=bool(patches and len(patches[0])>=p['minimum_shared_area_samples'])
            bands[name]={'traversable_depth_supported':traversable,'connected_land_samples':len(cells),
                         'largest_patch_samples':len(patches[0]) if patches else 0,'patch_count':len(patches),
                         'geographic_depth_supported':bool(evidence),'geographic_evidence':evidence,
                         'diagnostic':'surface geographic evidence present' if evidence else 'geographic depth unverified; no supported formation core'}
        extents=[]
        for r in regions:
            values=[distances[i] for i in r['cells'] if i in distances]
            if values and r['distinct_geography']:
                extents.append({'formation':r['id'],'minimum_effective_blocks':round(min(values),3),
                                'maximum_effective_blocks':round(max(values),3)})
        output[team]={'bands':bands,'reachable_samples':len(distances),'unreachable_usable_samples':len(graph)-len(distances),
                      'formation_depth_extents':extents,
                      'fully_opening_compressed_fraction':round(sum(r['maximum_effective_blocks']<70 for r in extents)/max(1,len(extents)),4),
                      'scope':'conservative surface travel from homeland edge; physical access and geographic significance await review'}
    return output


def refine(candidate, baseline, parameters=None):
    result=copy.deepcopy(baseline); p={**PARAMETERS,**(parameters or {})}; t=Terrain(candidate,baseline.get('parameters'))
    result['schema']='default_task_a_1_v1'; result['refinement_parameters']=p
    result['status']='Working A.1 analytical evidence; awaiting A.2 human selection'
    homes=result['homelands']; all_homes=set()
    for h in homes.values():
        x0,x1,z0,z1=h['logical_footprint_samples']
        all_homes.update(z*t.w+x for z in range(z0,z1+1) for x in range(x0,x1+1))
    graph=conservative_graph(t,p,all_homes); regions,membership=formations(t,graph,p)
    routes=result['routes']; diff=route_differentiation(t,routes,membership,p)
    equivalence=homeland_equivalence(homes,p); links=connections(t,routes,graph,regions,p)
    central=central_area(t,routes,graph,links,p); depth=operational_depth(t,homes,graph,regions,p)
    issues=[]
    for r in routes:
        if not r.get('starter'): issues.append(f'{r["id"]}: unavailable Starter'); continue
        s=r['starter']; path=indices(t,s['supported_sample_path'])
        physical=sum(math.dist(t.xy(a),t.xy(b))*t.s for a,b in zip(path,path[1:]))
        direct=math.dist(t.xy(path[0]),t.xy(path[-1]))*t.s
        rises=[abs(t.v['height'][a]-t.v['height'][b]) for a,b in zip(path,path[1:])]
        water=sum(t.v['water'][i] for i in path)/len(path); canopy=sum(t.v['canopy'][i] for i in path)/len(path)
        score=max(0,1-water-.4*canopy-max(0,max(rises,default=0)-2)/12-max(0,physical/max(direct,1)-p['starter_max_circuity'])*.25)
        s['refined_feasibility']={'score':round(score,4),'full_supported_max_rise':max(rises,default=0),
                                 'full_supported_water_fraction':round(water,4),'full_supported_canopy_fraction':round(canopy,4),
                                 'circuity':round(physical/max(direct,1),4),'deep_water_burden':None,
                                 'collective_choices_supported':diff[r['team']]['three_meaningful_choices_supported']}
        if score<p['starter_min_score']: issues.append(f'{r["id"]}: forced Starter construction proxy')
        if not s.get('destination_revision') and not t.p['starter_soft_min']<=s['effective_blocks_from_edge']<=t.p['starter_soft_max']: issues.append(f'{r["id"]}: Starter handoff outside soft band')
        if s.get('destination_revision') and not s.get('destination'): issues.append(f'{r["id"]}: no supported destination')
    for team,d in diff.items():
        if not d['three_meaningful_choices_supported']: issues.append(f'{team}: three meaningful openings not established')
        for q in d['pairs']:
            if q['functional_branch_collapse']: issues.append('Route collapse: '+','.join(q['routes']))
    if equivalence['classification']=='Severe': issues.append('Severe homeland disparity')
    for team,usable in equivalence['independently_usable'].items():
        if not usable: issues.append(f'{team}: homeland below minimum sampled usability')
    if not central['north_south_interaction_supported']: issues.append('Under-convergence: no shared central interaction')
    if central['overcentralization_warning']: issues.append('Over-centralization: five or more spines share one central sample')
    for team,d in depth.items():
        if d['fully_opening_compressed_fraction']>.5: issues.append(f'{team}: geographic formations compressed into opening depth')
        for band,v in d['bands'].items():
            if not v['traversable_depth_supported']: issues.append(f'{team}: missing connected {band} traversable depth')
            if not v['geographic_depth_supported']: issues.append(f'{team}: {band} geographic depth unverified')
    counts=Counter(c['classification'] for c in links)
    incidental=counts['incidental_traversability']/max(1,len(links))
    if links and incidental>=p['incidental_fraction_warning']: issues.append('Excessive incidental links: little distinguished lateral geography')
    useful=[c for c in links if c['classification']!='incidental_traversability']
    supported_cells={j for r in routes if r.get('starter') for j in indices(t,r['starter']['supported_sample_path'])}
    result['a0_diagnostics']={'failures':baseline['failures'],'network_metrics':baseline['network_metrics']}
    result['failures']=sorted(set(issues)); result['connection_analysis']=links
    result['cross_connections']=useful
    result['shortcuts']=[c for c in links if c['classification']=='strategic_shortcut']
    result['central_connective_area']=central; result['operational_depth']=depth
    result['surface_formations']=[{k:v for k,v in r.items() if k!='cells'}|{'sample_count':len(r['cells'])} for r in regions]
    result['homeland_equivalence']=equivalence; result['route_differentiation']=diff
    result['network_metrics']={
        'homeland_equivalence':equivalence,'route_differentiation':diff,
        'connection_counts':{key:counts[key] for key in ('incidental_traversability','useful_lateral_connection','strategic_shortcut')},
        'incidental_fraction':round(incidental,4),'central_connectivity':{k:v for k,v in central.items() if k not in ('traversable_mask_rle','clusters')},
        'operational_depth':{team:d['bands'] for team,d in depth.items()},
        'starter_scores':{r['id']:r['starter']['refined_feasibility']['score'] for r in routes if r.get('starter')},
        'corridor_quality':{r['id']:r.get('corridor_quality_score') for r in routes},
        'highland_access':baseline['network_metrics'].get('highland_access'),
        'coast_access':baseline['network_metrics'].get('coast_access'),
        'off_route_available_land_fraction':baseline['network_metrics'].get('off_route_available_land_fraction'),
        'unprovided_wilderness_fraction':round(sum(i not in supported_cells for i in graph)/max(1,len(graph)),4),
        'mandatory_chokepoints':None,'physical_readiness':None}
    result['uncertainties']={**result['uncertainties'],
        'formation_semantics':'Surface masks establish geographic candidates, not verified valleys, passes, saddles, landmark prominence or human-perceived interest.',
        'conservative_graph':'Dry samples and rise-limited edges can miss traversable rough ground, bridges, boats and caves; disconnected is not physically impossible. Buildability is not a hard Wilderness travel gate.',
        'human_review':'No human A.2 candidate selection or player-scale validation is supplied by this fitter.'}
    return result
