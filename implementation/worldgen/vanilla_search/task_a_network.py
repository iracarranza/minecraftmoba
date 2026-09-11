"""Destination-first terrain analysis. Historical Routes are not solver inputs.

Graph thresholds are inherited sampled-analysis resolutions, not desired map
topology. Components summarize evidence; sample graph floods establish reach.
"""
import heapq
import itertools
import math
from collections import Counter, defaultdict

from successor.grid import rle
from .task_a import Terrain, shortest, path_to, UNAVAILABLE
from .task_a_refine import PARAMETERS as A1, components, overlap
from .task_a_destinations import destinations, feasibility, PARAMETERS as PREVIOUS

MODES=('natural','modest','major')
PARAMETERS={
    'natural_sample_rise':2,'modest_sample_rise':A1['max_traversable_sample_rise'],
    'noise_component_samples':A1['formation_min_samples'],
    'elevation_resolution':32,'potential_crossing_span':A1['central_approach_blocks'],
    'joint_pool_limit':36,
}
LIMITS={**UNAVAILABLE,
    'homeland_depth':'Shortest horizontal graph length from outward homeland edges, independent of rise/canopy cost. Potential water/major-grade relationships are optimistic geographic links, not proven traversal. Travel fields separately expose dependency.',
    'component_resolution':'Broad-family/elevation/relief/cover/snow segmentation with sub-24-sample noise absorption. These are analysis resolutions, not target region counts; large regions can conceal passes and internal hierarchy.',
    'water_semantics':'Connected inland water systems and opposite-bank sample runs distinguish access, crossing candidates and barriers. Up-to-32-block runs are conditional modest candidates, not verified shallow water or approved bridges. Longer/ocean links enter only the major-or-unverified sensitivity graph: boats could avoid major construction, but transport and fords are unmeasured.',
    'continuation':'Forward reach excludes samples shallower than the handoff. Backtracking is measured against unrestricted reach. Classes describe regional continuation: no deeper region is a regional dead_end even if some same-component depth gain exists. Branches are groups of adjacent first-neighbor regions, not proven player-perceived valleys or independently navigable paths.',
    'selection':'Existing 65-effective-block travel calibration remains a soft travel cost only. Homeland Depth has no invented target; a map-relative depth cost favors earlier handoff. Joint pool/exit caps are computational, not desired topology.',
    'topology':'Matrices use same-sample forward minimax-depth meetings, not merely touching a large region. All depth/branch/convergence counts are descriptive; there is no mathematical-center requirement or winning seed.',
    'manual_evidence':'The supplied manual review of 930010639 establishes useful geographic hierarchy as an analyzer test. Missing recognition is an analyzer limitation, not automatic seed rejection.',
}


def home_cells(t,home):
    x0,x1,z0,z1=home['logical_footprint_samples']
    return {z*t.w+x for z in range(z0,z1+1) for x in range(x0,x1+1)}


def family(biome):
    for name,words in (('alpine',('peak','slope','grove','mountain')),('snow',('snow','frozen','ice')),
                       ('woodland',('forest','taiga','jungle')),('wetland',('swamp','mangrove')),
                       ('arid',('desert','badland','savanna')),('shore',('beach','shore'))):
        if any(word in biome for word in words): return name
    return 'open'


def terrain_graph(t,excluded,p):
    """Dry sample graph plus explicit opposite-bank links; no water walking."""
    dry={i for i in range(t.n) if not t.v['water'][i] and i not in excluded}
    edges={};water_graph={i:[(q,1) for q in t.neighbors(i) if t.v['water'][q] and t.ocean[i]==t.ocean[q]] for i in range(t.n) if t.v['water'][i]}
    systems=[];water_id={}
    for group in components(water_graph,set(water_graph)):
        wid=f'water-{min(group)}';ocean=any(t.ocean[i] for i in group)
        systems.append({'id':wid,'ocean':ocean,'cells':group,'crossings':[]})
        for i in group: water_id[i]=wid
    by_water={s['id']:s for s in systems}
    def put(a,b,rank,length,cost,water=None):
        key=tuple(sorted((a,b)))
        old=edges.get(key)
        e={'a':key[0],'b':key[1],'rank':rank,'length':length,'cost':cost,'water_system':water,
           'dependency_evidence':('unverified_water_transport_or_major_bridge' if rank==2 else 'conditional_modest_crossing') if water else MODES[rank]+'_sampled_grade_proxy'}
        if old is None or (rank,cost)<(old['rank'],old['cost']): edges[key]=e
    for i in sorted(dry):
        x,z=t.xy(i)
        for q,cost in t.adj[i]:
            if q not in dry: continue
            xx,zz=t.xy(q)
            if x!=xx and z!=zz and (z*t.w+xx not in dry or zz*t.w+x not in dry): continue
            rise=abs(t.v['height'][i]-t.v['height'][q])
            # Corner rises also constrain diagonal classification.
            if x!=xx and z!=zz:
                rise=max(rise,*(abs(t.v['height'][a]-t.v['height'][b]) for a in (i,q) for b in (z*t.w+xx,zz*t.w+x)))
            rank=0 if rise<=p['natural_sample_rise'] else 1 if rise<=p['modest_sample_rise'] else 2
            put(i,q,rank,math.dist(t.xy(i),t.xy(q))*t.s,cost)
        for dx,dz in ((1,0),(0,1)):
            run=[];xx=x+dx;zz=z+dz
            while 0<=xx<t.w and 0<=zz<t.h and t.v['water'][zz*t.w+xx]:
                run.append(zz*t.w+xx);xx+=dx;zz+=dz
            q=zz*t.w+xx
            if not run or not (0<=xx<t.w and 0<=zz<t.h) or q not in dry: continue
            length=(len(run)+1)*t.s
            inland=all(not t.ocean[k] for k in run)
            rank=1 if inland and length<=p['potential_crossing_span'] and abs(t.v['height'][i]-t.v['height'][q])<=p['modest_sample_rise'] else 2
            wid=water_id[run[0]]
            put(i,q,rank,length,length*(1+t.p['water_cost']),wid)
            by_water[wid]['crossings'].append({'banks':[i,q],'span_blocks':length,'dependency':MODES[rank],'conditional':True})
    graphs={mode:{i:[] for i in dry} for mode in MODES}
    depth_graph={i:[] for i in dry}
    for e in edges.values():
        a,b=e['a'],e['b'];depth_graph[a].append((b,e['length']));depth_graph[b].append((a,e['length']))
        for rank,mode in enumerate(MODES):
            if e['rank']<=rank:
                graphs[mode][a].append((b,e['cost']));graphs[mode][b].append((a,e['cost']))
    return graphs,depth_graph,edges,systems,water_id


def geographic_components(t,graph,edges,p):
    labels={}
    for i in graph:
        labels[i]=(t.v['height'][i]//p['elevation_resolution'],t.grade[i]>p['modest_sample_rise'],
                   bool(t.v['forest'][i]),bool(t.v['highland'][i]),family(t.v['biome'][i]),
                   any(word in t.v['biome'][i] for word in ('snow','frozen','ice','peak')),t.ocean_distance[i]<=2)
    # Do not connect geographically equal labels across a water crossing edge.
    adjacent={i:[(q,c) for q,c in graph[i] if math.dist(t.xy(i),t.xy(q))<=math.sqrt(2)+1e-8] for i in graph}
    masks=defaultdict(set)
    for i,label in labels.items(): masks[label].add(i)
    groups=[]
    for label,mask in sorted(masks.items()): groups.extend(components(adjacent,mask))
    membership={i:k for k,g in enumerate(groups) for i in g};active={k:set(g) for k,g in enumerate(groups)}
    initial=len(active);absorbed=0
    while True:
        changed=False
        for k in sorted(list(active),key=lambda k:(len(active[k]),min(active[k]))):
            if k not in active or len(active[k])>=p['noise_component_samples']: continue
            boundary=Counter(membership[q] for i in active[k] for q,_ in adjacent[i] if membership[q]!=k)
            if not boundary: continue
            original=labels[min(active[k])]
            target=min(boundary,key=lambda j:(sum(a!=b for a,b in zip(original,labels[min(active[j])])),-boundary[j],-len(active[j]),j))
            for i in active[k]: membership[i]=target
            active[target].update(active.pop(k));absorbed+=1;changed=True
        if not changed: break
    regions=[];assigned={}
    for cells in sorted(active.values(),key=min):
        rid=f'region-{min(cells)}';counts=Counter(labels[i] for i in cells);dominant=min(counts,key=lambda k:(-counts[k],k))
        kind='/'.join((dominant[4],'highland' if dominant[3] else 'lowland','wooded' if dominant[2] else 'open','rough' if dominant[1] else 'gentle','coastal' if dominant[6] else 'inland'))
        regions.append({'id':rid,'type':kind,'cells':sorted(cells),'area_blocks2':len(cells)*t.s*t.s,
                        'height_range':[min(t.v['height'][i] for i in cells),max(t.v['height'][i] for i in cells)],
                        'dominant_signature':list(dominant),'signature_purity':round(counts[dominant]/len(cells),4)})
        for i in cells: assigned[i]=rid
    links={}
    for e in edges.values():
        a,b=assigned[e['a']],assigned[e['b']]
        if a==b: continue
        key=tuple(sorted((a,b)))
        link=links.setdefault(key,{'regions':list(key),'counts_by_dependency':Counter(),'portals':{}})
        mode=MODES[e['rank']];link['counts_by_dependency'][mode]+=1
        old=link['portals'].get(mode)
        if old is None or (e['cost'],e['a'],e['b'])<(old['cost'],old['a'],old['b']):link['portals'][mode]=e
    return regions,assigned,list(links.values()),{'initial_components':initial,'absorbed_noise_components':absorbed,'final_components':len(regions)}


def outward_field(t,team,homes,graphs,depth_graph,p=None):
    p=p or PARAMETERS
    cells=home_cells(t,homes[team])
    x0,x1,z0,z1=homes[team]['logical_footprint_samples'];center=((x0+x1)/2,(z0+z1)/2)
    sources={};gates={}
    for i in sorted(cells):
        xy=t.xy(i)
        if t.v['water'][i]: continue
        for q,cost in t.adj[i]:
            if q not in depth_graph or sum((t.xy(q)[k]-xy[k])*(xy[k]-center[k]) for k in (0,1))<=0: continue
            qx,qz=t.xy(q)
            if qx!=xy[0] and qz!=xy[1] and any(t.v['water'][k] for k in (xy[1]*t.w+qx,qz*t.w+xy[0])):continue
            length=math.dist(xy,t.xy(q))*t.s/2
            sources[q]=min(sources.get(q,math.inf),length)
            gates.setdefault(q,[]).append((i,cost/2))
    depth,_=shortest(depth_graph,sources)
    travel={};previous={};origins={}
    for rank,mode in enumerate(MODES):
        starts={}
        for q,choices in gates.items():
            valid=[(i,c) for i,c in choices if abs(t.v['height'][i]-t.v['height'][q])<= (p['natural_sample_rise'] if rank==0 else p['modest_sample_rise'] if rank==1 else math.inf)]
            if valid:
                i,c=min(valid,key=lambda a:(a[1],a[0]));starts[q]=c;origins.setdefault(mode,{})[q]=i
        travel[mode],previous[mode]=shortest(graphs[mode],starts)
    return {'depth':depth,'travel':travel,'previous':previous,'origins':origins,'gates':gates,'sources':sources}


def destination_pool(t,candidate,graph,regions,membership,links,systems,water_id):
    surface_graph={i:[(q,c) for q,c in edges if math.dist(t.xy(i),t.xy(q))<=math.sqrt(2)+1e-8] for i,edges in graph.items()}
    raw=destinations(t,candidate,surface_graph,PREVIOUS);records=[]
    # Water identity is system-wide, never an arbitrary connected bank fragment.
    for s in systems:
        bank=sorted({q for i in s['cells'] for q in t.neighbors(i) if q in graph})
        if not bank: continue
        records.append({'id':s['id'],'opportunity_group':s['id'],'kind':'coastal access' if s['ocean'] else 'inland-water relationship',
                        'cells':bank,'strength':1.,'evidence':'connected sampled water system and dry-bank relationships',
                        'water_semantics':{'access':True,'coastal_access':s['ocean'],'crossing_candidates':len(s['crossings']),
                            'conditional_modest_crossings':sum(c['dependency']=='modest' for c in s['crossings']),
                            'barrier':True,'barrier_scope':'water is excluded from natural foot connectivity; detours/boats may exist'},
                        'weak_evidence':['water depth, ford/bridge viability and navigational significance unavailable']})
    for r in raw:
        if r['kind'] in ('inland water access','coast access') or r['kind'].startswith('biome interface'):continue
        r=dict(r);r['opportunity_group']=r['id'];records.append(r)
    by_region={r['id']:r for r in regions}
    boundary=defaultdict(set)
    for i in graph:
        for q,_ in graph[i]:
            a,b=membership[i],membership[q]
            if a!=b and by_region[a]['type']!=by_region[b]['type'] and math.dist(t.xy(i),t.xy(q))<=math.sqrt(2)+1e-8:
                boundary[tuple(sorted((a,b)))].update((i,q))
    for pair,cells in sorted(boundary.items()):
        if len(cells)<PREVIOUS['minimum_feature_samples']:continue
        rid='transition:'+':'.join(pair)
        records.append({'id':rid,'opportunity_group':rid,'kind':'persistent regional threshold','cells':sorted(cells),
                        'strength':.5,'evidence':'boundary between noise-absorbed coherent geographic components',
                        'regions':list(pair),'weak_evidence':['component boundary is not a verified landmark or natural pass']})
    # Unify threshold fragments leading into the same actual cover/highland mass.
    masks={'forest entrance':{i for i in graph if t.v['forest'][i]},'highland approach':{i for i in graph if t.v['highland'][i]}}
    for kind,mask in masks.items():
        identity={i:min(g) for g in components(surface_graph,mask) for i in g}
        for r in records:
            if r['kind']!=kind:continue
            touched=Counter(identity[q] for i in r['cells'] for q in t.neighbors(i) if q in identity)
            if touched:r['opportunity_group']=kind+':'+str(min(touched,key=lambda k:(-touched[k],k)))
    return sorted(records,key=lambda r:r['id'])


def flood(graph,start,depth,floor=None,minimax=False):
    if start not in graph or start not in depth:return {}
    if not minimax:
        reached={start:depth[start]};stack=[start]
        while stack:
            i=stack.pop()
            for q,_ in graph[i]:
                if q not in reached and q in depth and (floor is None or depth[q]+1e-8>=floor):
                    reached[q]=depth[q];stack.append(q)
        return reached
    reached={start:depth[start]};queue=[(depth[start],start)]
    while queue:
        value,i=heapq.heappop(queue)
        if value!=reached[i]:continue
        for q,_ in graph[i]:
            if q not in depth or (floor is not None and depth[q]+1e-8<floor):continue
            cost=max(value,depth[q]) if minimax else depth[q]
            if q not in reached or (minimax and cost<reached[q]):
                reached[q]=cost;heapq.heappush(queue,(cost,q))
    return reached


def continuation(graph,node,depth,regions,membership,unrestricted):
    floor=depth[node];reach=flood(graph,node,depth,floor)
    by_id={r['id']:r for r in regions};own=membership[node]
    deeper=sorted({membership[i] for i in reach if depth[i]>floor and membership[i]!=own})
    all_deeper={membership[i] for i in unrestricted if depth.get(i,-1)>floor and membership[i]!=own}
    # First regional neighbors are coarse ports. Adjacent ports of the same type
    # coalesce; downstream re-merging does not erase distinct initial choices.
    rg=defaultdict(set)
    for i in reach:
        for q,_ in graph[i]:
            if q in reach and membership[i]!=membership[q]:rg[membership[i]].add(membership[q])
    ports=set(rg[own])&set(deeper);port_graph={r:[(q,1) for q in rg[r]&ports if by_id[q]['type']==by_id[r]['type']] for r in ports}
    branches=components(port_graph,ports) if ports else []
    # A neighboring region straddling this depth surface offers potential
    # lateral travel. This is regional evidence, not an invented distance band.
    lateral=sorted(r for r in rg[own] if any(depth.get(i,math.inf)<=floor for i in by_id[r]['cells'])
                   and any(depth.get(i,-math.inf)>=floor for i in by_id[r]['cells']))
    gain=max((depth[i] for i in reach),default=floor)-floor
    kind='dead_end' if not deeper else 'directed' if len(branches)<=1 else 'junction' if lateral else 'branching'
    return {'maximum_homeland_depth':round(floor+gain,3),'depth_gain':round(gain,3),
            'deeper_regions_reached':deeper,'distinct_deeper_region_types':sorted({by_id[r]['type'] for r in deeper}),
            'meaningful_branch_count':len(branches),'branch_region_groups':branches,'continuation_class':kind,
            'lateral_connection_opportunities':lateral,'backtracking_dependency':bool(all_deeper-set(deeper)),
            'additional_deeper_regions_requiring_backtracking':sorted(all_deeper-set(deeper)),
            'reachable_samples_without_backtracking':len(reach)}


def analyze(candidate,homelands,parameters=None):
    """No baseline Route argument. Only terrain, fixed homelands and analysis parameters."""
    p={**PARAMETERS,**(parameters or {})};t=Terrain(candidate)
    excluded=set.union(*(home_cells(t,h) for h in homelands.values()))
    graphs,depth_graph,edges,systems,water_id=terrain_graph(t,excluded,p)
    regions,membership,links,compression=geographic_components(t,graphs['major'],edges,p)
    catalog=destination_pool(t,candidate,graphs['major'],regions,membership,links,systems,water_id)
    fields={team:outward_field(t,team,homelands,graphs,depth_graph,p) for team in ('north','south')}
    # Cache unrestricted physical-mode components once, not once per destination.
    unrestricted={mode:{} for mode in MODES}
    for mode in MODES:
        for group in components(graphs[mode],set(graphs[mode])):
            group=set(group)
            for i in group:unrestricted[mode][i]=group
    pools={};selected={};corridors={};diagnostics=[];reconsiderations={}
    for team,field in fields.items():
        pool=[];maxdepth=max(field['depth'].values(),default=1)
        continuation_cache={}
        for feature in catalog:
            eligible=[i for i in feature['cells'] if i in field['depth']]
            if not eligible:
                pool.append({'id':feature['id'],'kind':feature['kind'],'eligible':False,'continuation':None,
                             'unavailable':'No outward homeland geographic path in the sampled graph'})
                continue
            # Anchor choices remain homeland-wide; no old Route, radius or suffix.
            modes=[m for m in ('natural','modest') if any(i in field['travel'][m] for i in eligible)] or ['major']
            alternatives=[]
            for mode in modes:
                for node in eligible:
                    if node not in field['travel'][mode]:continue
                    path=path_to(field['previous'][mode],node);root=path[0];gate=field['origins'][mode][root]
                    score=feasibility(t,[gate]+path)
                    utility=PREVIOUS['construction_weight']*feature['strength']*score-abs(field['travel'][mode][node]-t.p['starter_target'])/t.p['starter_target']-field['depth'][node]/maxdepth
                    alternatives.append((-utility,MODES.index(mode),node,path,gate,score))
            if not alternatives:
                pool.append({'id':feature['id'],'kind':feature['kind'],'eligible':False,'continuation':None,
                             'unavailable':'No traversal-field path from a valid outward perimeter sample'})
                continue
            _,rank,node,path,gate,score=min(alternatives,key=lambda a:a[:3]);mode=MODES[rank]
            if node not in continuation_cache:
                continuation_cache[node]={m:continuation(graphs[m],node,field['depth'],regions,membership,unrestricted[m].get(node,set())) for m in MODES}
            cont=continuation_cache[node]
            depth=field['depth'][node];cost=field['travel'][mode][node]
            terms={'evidence_and_constructability':PREVIOUS['construction_weight']*feature['strength']*score,
                   'soft_travel_cost':abs(cost-t.p['starter_target'])/t.p['starter_target'],
                   'soft_homeland_depth':depth/maxdepth,
                   'continuation_viability':0 if cont['modest']['depth_gain']>0 else 1,
                   'network_participation':0 if cont['modest']['deeper_regions_reached'] else 1}
            utility=terms['evidence_and_constructability']-sum(v for k,v in terms.items() if k!='evidence_and_constructability')
            pool.append({'id':feature['id'],'opportunity_group':feature['opportunity_group'],'kind':feature['kind'],
                         'anchor':t.point(node),'homeland_depth':round(depth,3),'travel_cost':round(cost,3),
                         'starter_dependency':mode,'conditional_water_dependency':any(edges[tuple(sorted((a,b)))]['water_system'] for a,b in zip(path,path[1:])),
                         'constructability':round(score,4),'evidence':feature['evidence'],'strength':feature['strength'],
                         'weak_evidence':feature['weak_evidence'],'continuation':cont,'selection_terms':terms,
                         'continuation_dependencies':{
                             'major_only_deeper_regions':sorted(set(cont['major']['deeper_regions_reached'])-set(cont['modest']['deeper_regions_reached'])),
                             'modest_additional_deeper_regions':sorted(set(cont['modest']['deeper_regions_reached'])-set(cont['natural']['deeper_regions_reached']))},
                         'opening_regions':sorted({membership[i] for i in path}),
                         'utility':utility,'eligible':mode!='major' and score>=A1['starter_min_score'] and utility>0,
                         '_path':path,'_gate':gate,'_node':node})
        selected[team]=select_joint(t,pool,p)
        corridors[team]=finalize_corridors(t,selected[team],field,graphs,homelands[team],p)
        failed={c['destination'] for c in corridors[team] if c['status']!='provisional'}
        reconsiderations[team]=[]
        if failed:
            locked=[r for r in selected[team] if r['id'] not in failed]
            reconsiderations[team].append({'reconsidered_destinations':sorted(failed),'retained_destinations':[r['id'] for r in locked],
                                           'reason':'local exit and reuse-penalized corridor alternatives did not establish differentiation'})
            selected[team]=select_joint(t,[r for r in pool if r['id'] not in failed],p,locked)
            corridors[team]=finalize_corridors(t,selected[team],field,graphs,homelands[team],p)
        pools[team]=pool
        unresolved=3-sum(c['status']=='provisional' for c in corridors[team])
        if unresolved:diagnostics.append(f'{team}: {unresolved} unresolved choices; evidence/constructability/joint differentiation or analysis scope, not a seed rejection')
    topology=whole_topology(t,selected,fields,graphs,membership)
    region_output=[]
    for r in regions:
        region_output.append({k:v for k,v in r.items() if k!='cells'}|{'sample_count':len(r['cells']),
            'homeland_depth_ranges':{team:[round(min((f['depth'][i] for i in r['cells'] if i in f['depth']),default=0),3),round(max((f['depth'][i] for i in r['cells'] if i in f['depth']),default=0),3)] for team,f in fields.items()}})
    clean=lambda r:{k:v for k,v in r.items() if not k.startswith('_')}
    region_sets=[];type_sets=[];region_refs={};type_refs={}
    def intern(values,table,refs):
        key=tuple(values)
        if key not in refs:refs[key]=len(table);table.append(values)
        return refs[key]
    def compact(r):
        out=clean(r)
        if not r.get('continuation'):return out
        out['continuation']={}
        for mode,c in r['continuation'].items():
            entry=dict(c)
            for key in ('deeper_regions_reached','additional_deeper_regions_requiring_backtracking'):
                values=entry.pop(key);entry[key+'_count']=len(values)
                entry[key+'_ref']=intern(values,region_sets,region_refs)
            key='distinct_deeper_region_types';values=entry.pop(key)
            entry[key+'_count']=len(values);entry[key+'_ref']=intern(values,type_sets,type_refs)
            out['continuation'][mode]=entry
        out['continuation_dependencies']={k+'_ref':intern(v,region_sets,region_refs) for k,v in r['continuation_dependencies'].items()}
        return out
    compact_pools={team:[compact(r) for r in pool] for team,pool in pools.items()}
    return {'schema':'default_destination_first_v1','seed':candidate['seed'],'status':'analytical/descriptive; no physical readiness or seed verdict',
            'sampling':{'width':t.w,'height':t.h,'spacing_blocks':t.s,'order':'oriented row-major'},
            'parameters':p,'homelands':homelands,'playable_bounds':candidate['region']['block_bounds'],'logical_orientation':candidate['orientation'],
            'region_compression':compression,'regions':region_output,'region_grid_rle':rle([membership.get(i) for i in range(t.n)]),
            'terrain_connectivity_graph':links,'water_systems':[{k:v for k,v in s.items() if k!='cells'}|{'sample_count':len(s['cells'])} for s in systems],
            'destination_catalog':[{k:v for k,v in r.items() if k!='cells'}|{'sample_count':len(r['cells'])} for r in catalog],
            'homeland_fields':{team:{'outward_sources':[t.point(i) for i in sorted(f['sources'])],
                'homeland_depth_rle':rle([round(f['depth'][i],3) if i in f['depth'] else None for i in range(t.n)]),
                'travel_cost_rle':{m:rle([round(f['travel'][m][i],3) if i in f['travel'][m] else None for i in range(t.n)]) for m in MODES}} for team,f in fields.items()},
            'destination_pools':compact_pools,'continuation_region_sets':region_sets,'continuation_type_sets':type_sets,
            'pool_reference_encoding':'*_ref indexes continuation_region_sets, except distinct_deeper_region_types_ref indexes continuation_type_sets; selected handoffs remain expanded',
            'selected_handoffs':{team:[clean(r) for r in ss] for team,ss in selected.items()},
            'unresolved_choices':{team:3-sum(c['status']=='provisional' for c in cc) for team,cc in corridors.items()},'starter_corridors':corridors,
            'corridor_reconsiderations':reconsiderations,
            'topology':topology,'diagnostics':diagnostics,'limitations':LIMITS}


def select_joint(t,pool,p,locked=()):
    by_group={}
    for r in sorted((r for r in pool if r['eligible']),key=lambda r:(-r['utility'],r['id'])):
        by_group.setdefault(r['opportunity_group'],r)
    options=list(by_group.values())[:p['joint_pool_limit']]
    for r in locked:
        if r not in options:options.append(r)
    required={r['id'] for r in locked}
    def pair(a,b):
        if a['opportunity_group']==b['opportunity_group']:return None
        shared=max(overlap(t,a['_path'],b['_path'],0),overlap(t,b['_path'],a['_path'],0))
        if shared>=.5:return None
        sep=math.dist(t.xy(a['_node']),t.xy(b['_node']))*t.s
        if sep<A1['approach_separation_blocks']:return None
        departure=math.dist(t.xy(a['_gate']),t.xy(b['_gate']))
        geo_a=set(a['opening_regions']);geo_b=set(b['opening_regions'])
        early_shared=len(geo_a&geo_b)/max(1,len(geo_a|geo_b))
        return (PREVIOUS['overlap_weight']*shared+PREVIOUS['duplicate_kind_weight']*(a['kind']==b['kind'])+
                PREVIOUS['near_destination_weight']*max(0,1-departure/t.p['departure_separation_samples'])+
                PREVIOUS['duplicate_kind_weight']*early_shared)
    pairwise={(a['id'],b['id']):pair(a,b) for a,b in itertools.combinations(options,2)}
    best=(0,());chosen=[]
    for size in range(1,4):
        for group in itertools.combinations(options,size):
            if not required<={r['id'] for r in group}:continue
            penalties=[pairwise[(a['id'],b['id'])] for a,b in itertools.combinations(group,2)]
            if any(x is None for x in penalties):continue
            value=sum(r['utility'] for r in group)-sum(penalties)
            key=(value,tuple(r['id'] for r in group))
            if value>best[0] or (value==best[0] and key[1]<best[1]):best=key;chosen=list(group)
    return sorted(chosen,key=lambda r:r['id'])


def finalize_corridors(t,selected,field,graphs,home,p):
    """Destinations fixed first; try different local exits before giving up."""
    cells=home_cells(t,home);fountain=home['fountain']['sample'];fountain=fountain[1]*t.w+fountain[0]
    _,inside_previous=shortest(t.adj,{fountain:0},blocked=set(range(t.n))-cells)
    used=[];result=[]
    for feature in selected:
        target=feature['_node'];mode=feature['starter_dependency']
        options=[]
        for penalize in (False,True) if used else (False,):
            penalty={i:t.p['reuse_search_penalty'] for old,_ in used for i in old} if penalize else None
            dist,previous=shortest(graphs[mode],{target:0},penalty=penalty)
            for root,pairs in field['gates'].items():
                if root not in dist:continue
                for gate,edgecost in pairs:
                    if abs(t.v['height'][gate]-t.v['height'][root])>p['modest_sample_rise']:continue
                    path=list(reversed(path_to(previous,root)));inside=path_to(inside_previous,gate)
                    score=feasibility(t,inside+path)
                    if score<A1['starter_min_score']:continue
                    shared=max((overlap(t,path,old,0) for old,_ in used),default=0)
                    departure_separated=all(math.dist(t.xy(gate),t.xy(g))>=t.p['departure_separation_samples'] for _,g in used)
                    actual=sum(next(c for q,c in graphs[mode][a] if q==b) for a,b in zip(path,path[1:]))+edgecost
                    options.append((not departure_separated,shared,actual,gate,inside,path,score))
        if not options:
            result.append({'destination':feature['id'],'status':'unresolved corridor; destination retained for review'});continue
        separated,shared,cost,gate,inside,path,score=min(options,key=lambda o:o[:4]);used.append((path,gate))
        full=inside+path
        water_links=[[t.point(a),t.point(b)] for a,b in zip(path,path[1:]) if math.dist(t.xy(a),t.xy(b))>math.sqrt(2)+1e-8]
        result.append({'destination':feature['id'],'status':'provisional' if not separated and shared<.5 else 'corridor differentiation unresolved',
                       'departure':t.point(gate),'terminus':t.point(target),'sample_path':[list(t.xy(i)) for i in full],
                       'world_path':[t.point(i)['world_xyz'] for i in full],'travel_cost_from_edge':round(cost,3),
                       'homeland_depth':feature['homeland_depth'],'constructability':round(score,4),
                       'travel_dependency':'modest' if abs(t.v['height'][gate]-t.v['height'][path[0]])>p['natural_sample_rise'] else mode,
                       'conditional_water_dependency':bool(water_links),'conditional_crossing_links':water_links,
                       'opening_overlap_with_prior':round(shared,4),'provided_infrastructure_ends_here':True,
                       'designated_route_continues':'terrain continuation graph; no historical suffix or imposed deeper target'})
    return result


def meeting(a,b,membership):
    common=a.keys()&b.keys()
    if not common:return None
    node=min(common,key=lambda i:(max(a[i],b[i]),a[i]+b[i],i))
    regions=sorted({membership[i] for i in common})
    return {'first_shared_sample':node,'first_shared_region':membership[node],
            'depth_a':round(a[node],3),'depth_b':round(b[node],3),
            'shared_regions':regions,'shared_region_count':len(regions)}


def connective_growth(north,south,membership):
    """Events at observed reach depths; no fitted center or target depth bands."""
    events=defaultdict(list);region_evidence={}
    for a_index,a in enumerate(north):
        for b_index,b in enumerate(south):
            earliest={}
            for i in a.keys()&b.keys():
                rid=membership[i];ceiling=max(a[i],b[i])
                earliest[rid]=min(ceiling,earliest.get(rid,math.inf))
            for rid,ceiling in earliest.items():events[ceiling].append((rid,a_index,b_index))
    seen_regions=set();seen_pairs=set();relations=0;profile=[]
    for ceiling,records in sorted(events.items()):
        for rid,a,b in records:
            seen_regions.add(rid);seen_pairs.add((a,b));relations+=1
            entry=region_evidence.setdefault(rid,{'region':rid,'first_opposing_overlap_depth_ceiling':round(ceiling,3),'opposing_pairs':[]})
            entry['opposing_pairs'].append([a,b])
        profile.append({'homeland_depth_ceiling':round(ceiling,3),'regions_with_opposing_overlap':len(seen_regions),
                        'opposing_pairs_connected':len(seen_pairs),'pair_region_relations':relations})
    return {'observed_depth_profile':profile,'connective_regions':sorted(region_evidence.values(),key=lambda r:(r['first_opposing_overlap_depth_ceiling'],r['region']))}


def whole_topology(t,selected,fields,graphs,membership):
    modes={}
    for mode in MODES:
        reach={team:[flood(graphs[mode],r['_node'],fields[team]['depth'],fields[team]['depth'][r['_node']],True) for r in ss] for team,ss in selected.items()}
        lateral={team:[[meeting(a,b,membership) if i!=j else None for j,b in enumerate(rr)] for i,a in enumerate(rr)] for team,rr in reach.items()}
        for matrix in lateral.values():
            for row in matrix:
                for m in row:
                    if m:m['connection_homeland_depth']=max(m['depth_a'],m['depth_b'])
        opposing=[[meeting(a,b,membership) for b in reach['south']] for a in reach['north']]
        meetings=[m for row in opposing for m in row if m]
        for matrix in list(lateral.values())+[opposing]:
            for row in matrix:
                for m in row:
                    if m:m['first_shared_point']=t.point(m['first_shared_sample'])
        earliest=min(meetings,key=lambda m:(max(m['depth_a'],m['depth_b']),m['depth_a']+m['depth_b'],m['first_shared_sample'])) if meetings else None
        isolation={'north':[selected['north'][i]['id'] for i,row in enumerate(opposing) if not any(row)],
                   'south':[selected['south'][j]['id'] for j in range(len(selected['south'])) if not any(row[j] for row in opposing)]}
        exclusivity={team:[{'destination':r['id'],'first_same_team_connection_depth':min((m['connection_homeland_depth'] for m in lateral[team][i] if m),default=None),
                           'exclusive_depth_gain':round(max(0,min((m['connection_homeland_depth'] for m in lateral[team][i] if m),default=r['homeland_depth'])-r['homeland_depth']),3) if any(lateral[team][i]) else None} for i,r in enumerate(ss)] for team,ss in selected.items()}
        modes[mode]={'same_team_lateral_matrices':lateral,'north_south_convergence_matrix':opposing,
                     'connective_geography_growth':connective_growth(reach['north'],reach['south'],membership),
                     'earliest_opponent_convergence':earliest,'broader_shared_regions':sorted({r for m in meetings for r in m['shared_regions']}),
                     'convergence_pair_coverage':len(meetings)/max(1,len(selected['north'])*len(selected['south'])),
                     'isolated_branches':isolation,'opening_exclusivity':exclusivity}
    return {'matrix_order':{team:[r['id'] for r in ss] for team,ss in selected.items()},'by_dependency':modes,
            'earliest_convergence_convention':'Minimum of maximum A/B Homeland Depth, then summed depths; a descriptive balanced-depth meeting, not measured first-arrival time.',
            'major_dependency_scope':'Requires an edge absent from the modest proxy graph: steep terrain OR unverified water transport. This is not proof that Minecraft requires major earthwork; boats, fords and actual slopes are unresolved.',
            'major_dependency_pairs':[[i,j] for i,row in enumerate(modes['major']['north_south_convergence_matrix']) for j,m in enumerate(row) if m and not modes['modest']['north_south_convergence_matrix'][i][j]],
            'connective_geography':'Shared terrain reach, not a prescribed central point; same-sample meeting removes coarse-region overlap false positives.'}
