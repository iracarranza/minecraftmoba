"""Bounded destination-anchored analytical refit. Never reads/writes world blocks.

Keep homelands, departures and deeper targets. Change opening geometry only in
the existing 32-block approach neighborhood; retain the original deeper suffix.
All added scoring weights are prototype assumptions, not gameplay balance.
"""
import itertools
import math
from collections import Counter

from .task_a import fit, shortest, path_to, starter
from .task_a_refine import (PARAMETERS as A1, conservative_graph, components,
                            indices, overlap, refine)

PARAMETERS = {
    'corridor_buffer_blocks': A1['central_approach_blocks'],
    'candidate_depth_ceiling': 150,  # secondary transition ends; never build tertiary travel
    'candidate_depth_floor': 40,  # immediate fringe ends
    'minimum_feature_samples': A1['formation_band_min_samples'],
    'retained_options_per_feature': 3,
    'retained_options_per_route': 12,
    'distance_weight': 1., 'construction_weight': 3.,
    'evidence_weight': 1., 'network_detour_weight': 1.,
    'handoff_weight': 1.,
    'duplicate_feature_weight': 3., 'duplicate_kind_weight': 1.,
    'overlap_weight': 3., 'near_destination_weight': 3.,
    'minimum_opportunity_kinds': 2,  # prototype diagnostic, not a biome quota
}
LIMITS = {
    'destination_semantics': 'Opportunity means access to observed geography only; no resources, rewards, ecology or gameplay role inferred.',
    'destination_detection': '8-block dry-bank, highland-boundary, forest-boundary, slope-foot, coast and persistent biome-interface samples; not verified passes, valleys, saddles, junctions, sightlines or fords.',
    'destination_search': 'Bounded 32-block corridor neighborhood, 40–150 effective-depth candidate envelope, top 3 per feature then 12 per Route. These are prototype search restrictions, not balance or exhaustive global optimality.',
    'destination_tuning': 'Normalized additive costs and joint duplicate/overlap weights are explicit uncalibrated prototype assumptions. No measured early-game utility is available.',
    'settlement_access': 'Existing village piece bounding boxes only; projected sampled edge is not a verified village entrance or occupied settlement. Other structures are not treated as settlements.',
}


def destinations(t, candidate, graph, p):
    """Interface nodes, not arbitrary interiors of large biome components."""
    masks={}
    def add(kind,i): masks.setdefault(kind,set()).add(i)
    for i in sorted(graph):
        ns=list(t.neighbors(i))
        if any(t.v['water'][q] and not t.ocean[q] for q in ns): add('inland water access',i)
        if any(t.ocean[q] for q in ns): add('coast access',i)
        if not t.v['highland'][i] and any(t.v['highland'][q] for q in ns): add('highland approach',i)
        if not t.v['forest'][i] and any(t.v['forest'][q] for q in ns): add('forest entrance',i)
        # A sampled slope foot: flat approach behind, sustained rising ground
        # ahead. Reuse 32-block scale, 2-block flatness and 8-block relief.
        x,z=t.xy(i);span=int(p['corridor_buffer_blocks']/t.s)
        for dx,dz in ((1,0),(-1,0),(0,1),(0,-1)):
            if not all(0<=x+dx*k<t.w and 0<=z+dz*k<t.h for k in (-span,span)): continue
            ahead=[(z+dz*k)*t.w+x+dx*k for k in range(1,span+1)]
            behind=[(z-dz*k)*t.w+x-dx*k for k in range(1,span+1)]
            height=t.v['height'][i]
            if (all(q in graph for q in ahead+behind)
                and max(abs(t.v['height'][q]-height) for q in behind)<=t.p['fountain_max_grade_y']
                and t.v['height'][ahead[0]]>height
                and all(t.v['height'][b]>=t.v['height'][a] for a,b in zip(ahead,ahead[1:]))
                and t.v['height'][ahead[-1]]-height>=t.p['severe_rise_y']):
                add('slope-foot approach',i)
        # Require repeated evidence on both sides; single-sample biome speckles
        # and transitions wholly inside a forest are not separate opportunities.
        counts=Counter(t.v['biome'][q] for q in [i]+ns if q in graph)
        for biome,n in sorted(counts.items()):
            if biome!=t.v['biome'][i] and n>=p['minimum_feature_samples'] and counts[t.v['biome'][i]]>=p['minimum_feature_samples']:
                pair=' / '.join(sorted((biome,t.v['biome'][i])))
                add('biome interface: '+pair,i)
    records=[]
    for kind,mask in sorted(masks.items()):
        for cells in components(graph,mask):
            if len(cells)<p['minimum_feature_samples']: continue
            records.append({'id':f'{kind}:{cells[0]}','kind':kind,'cells':cells,
                            'strength':.5 if kind.startswith('biome interface') else .75 if kind=='slope-foot approach' else 1.,
                            'evidence':'adjacent sampled surface classifications; connected interface',
                            'weak_evidence':(['biome boundary alone does not establish a recognizable formation'] if kind.startswith('biome interface') else ['32-block flat-to-rising profile; hill scale and human-perceived significance unverified'] if kind=='slope-foot approach' else ['sampled access edge; physical entrance and visual legibility unverified'])})
    for structure in candidate.get('actual_structures',[]):
        if not structure.get('type','').startswith('minecraft:village'): continue
        boxes=structure.get('bounding_boxes',[])
        footprint={i for i in graph if any(b[0]-t.s<=t.coords[i][0]<=b[3]+t.s and b[2]-t.s<=t.coords[i][1]<=b[5]+t.s for b in boxes)}
        cells=sorted(i for i in footprint if any(q not in footprint for q in t.neighbors(i)))
        if not cells: continue
        records.append({'id':f'village:{structure["chunk"]}','kind':'settlement approach','cells':cells,'strength':1.,
                        'evidence':'actual_structures village piece bounding boxes projected onto dry surface samples',
                        'weak_evidence':['village entrance, occupancy and unobstructed sightline unverified']})
    return records


def distance_cost(depth,t,p):
    """Soft everywhere inside the search envelope; no preferred-band rejection."""
    return p['distance_weight']*abs(depth-t.p['starter_target'])/t.p['starter_target']


def feasibility(t,path):
    physical=sum(math.dist(t.xy(a),t.xy(b))*t.s for a,b in zip(path,path[1:]))
    direct=math.dist(t.xy(path[0]),t.xy(path[-1]))*t.s
    rise=max((abs(t.v['height'][a]-t.v['height'][b]) for a,b in zip(path,path[1:])),default=0)
    water=sum(t.v['water'][i] for i in path)/len(path)
    canopy=sum(t.v['canopy'][i] for i in path)/len(path)
    score=max(0.,1-water-.4*canopy-max(0,rise-2)/12-max(0,physical/max(1,direct)-A1['starter_max_circuity'])*.25)
    return score


def route_options(t,entry,records,graph,p):
    route,full,outside,_=entry; gate=indices(t,[route['departure']['sample']])[0]
    inside=full[:full.index(gate)+1]; exit_cell=outside[0]
    radius=p['corridor_buffer_blocks']/t.s
    allowed=set()
    for i in outside:
        x,z=t.xy(i)
        for dz in range(-int(radius),int(radius)+1):
            for dx in range(-int(radius),int(radius)+1):
                if math.hypot(dx,dz)<=radius and 0<=x+dx<t.w and 0<=z+dz<t.h:
                    allowed.add((z+dz)*t.w+x+dx)
    local={i:[(q,c) for q,c in graph[i] if q in allowed] for i in sorted(allowed&graph.keys())}
    if exit_cell not in local: return [],{'reason':'departure outside conservative dry graph'}
    dist,prev=shortest(local,{exit_cell:t.cost(gate,exit_cell)/2})
    old_depth=[t.cost(gate,exit_cell)/2]
    for a,b in zip(outside,outside[1:]): old_depth.append(old_depth[-1]+t.cost(a,b))
    raw=[];candidate_rejections=Counter();nearest_depths={}
    for record in records:
        choices=[]
        for node in record['cells']:
            depth=dist.get(node,math.inf)
            if math.isfinite(depth): nearest_depths[record['kind']]=min(nearest_depths.get(record['kind'],math.inf),depth)
            if not math.isfinite(depth): candidate_rejections['outside_corridor_or_disconnected']+=1;continue
            if not p['candidate_depth_floor']<=depth<=p['candidate_depth_ceiling']:
                candidate_rejections['outside_depth_envelope']+=1;continue
            opening=path_to(prev,node)
            score=feasibility(t,inside+opening)
            if score<A1['starter_min_score']: candidate_rejections['insufficient_starter_feasibility']+=1;continue
            nearest=min(range(len(outside)),key=lambda k:(math.dist(t.xy(node),t.xy(outside[k])),k))
            # Preserve at least 32 effective blocks of unsupported continuation;
            # reconnect forward, never double back to an earlier Route sample.
            joins=[k for k in range(nearest,len(outside)) if old_depth[k]>=max(depth,old_depth[nearest])+A1['minimum_continuation_blocks']]
            if not joins: candidate_rejections['no_forward_suffix_join']+=1;continue
            join=joins[0]
            terms={'travel_depth':distance_cost(depth,t,p),'construction':p['construction_weight']*(1-score),
                   'geographic_evidence':p['evidence_weight']*(1-record['strength'])}
            choices.append({'node':node,'feature':{k:v for k,v in record.items() if k!='cells'},
                            'depth':depth,'opening':opening,'inside':inside,'join':join,'score_terms':terms})
        raw.extend(choices)
    cache={};options=[];rejected=Counter()
    for c in raw:
        join=c['join']; target=outside[join]
        if target not in local: rejected['join_outside_dry_graph']+=1;continue
        if join not in cache: cache[join]=shortest(local,{target:0.})
        d,back=cache[join]
        if c['node'] not in d: rejected['join_disconnected']+=1;continue
        tail=list(reversed(path_to(back,c['node'])))+outside[join+1:]
        newoutside=c['opening']+tail[1:]
        if len(set(newoutside))!=len(newoutside): rejected['backtracking_loop']+=1;continue
        if t.length(tail)<A1['minimum_continuation_blocks']: rejected['insufficient_unsupported_tail']+=1;continue
        oldcost=t.length(outside)
        detour=max(0,t.length(newoutside)-oldcost)/max(oldcost,1)
        if detour>A1['connection_max_detour']-1: rejected['excessive_detour']+=1;continue
        c['score_terms']['network_detour']=p['network_detour_weight']*detour
        exits=sum(q not in c['opening'] for q,_ in graph[c['node']])
        c['score_terms']['wilderness_handoff']=p['handoff_weight']*(1-min(exits,2)/2)
        c['dry_neighbor_choices']=exits
        c.update({'tail':tail,'outside':newoutside,'cost':sum(c['score_terms'].values()),
                  'original_suffix_join':t.point(target)})
        options.append(c)
    counts=Counter();retained=[]
    for c in sorted(options,key=lambda c:(c['cost'],c['feature']['id'],c['node'])):
        fid=c['feature']['id']
        if counts[fid]>=p['retained_options_per_feature']: continue
        # Preserve feature alternatives before adding near-duplicate anchors
        # from one interface to the joint solver's bounded pool.
        c['feature_alternative_rank']=counts[fid]
        counts[fid]+=1;retained.append(c)
    options=sorted(retained,key=lambda c:(c['feature_alternative_rank'],c['cost'],c['feature']['id'],c['node']))[:p['retained_options_per_route']]
    return options,{'features_considered':len(records),'pre_path_options':len(raw),'retained_options':len(options),
                    'candidate_rejections':dict(sorted(candidate_rejections.items())),
                    'nearest_feature_depth_by_kind':{k:round(v,3) for k,v in sorted(nearest_depths.items())},
                    'path_rejections':dict(sorted(rejected.items()))}


def pair_cost(t,a,b,p):
    sep=math.dist(t.xy(a['node']),t.xy(b['node']))*t.s
    shared=max(overlap(t,a['opening'],b['opening'],A1['corridor_tolerance_samples']),
               overlap(t,b['opening'],a['opening'],A1['corridor_tolerance_samples']))
    return (p['duplicate_feature_weight']*(a['feature']['id']==b['feature']['id'])+
            p['duplicate_kind_weight']*(a['feature']['kind']==b['feature']['kind'])+
            p['overlap_weight']*shared+
            p['near_destination_weight']*max(0,1-sep/A1['approach_separation_blocks']))


def choose_joint(t,pools,p):
    if not pools or any(not pool for pool in pools): return None
    return min(itertools.product(*pools),key=lambda group:(
        sum(c['cost'] for c in group)+sum(pair_cost(t,a,b,p) for a,b in itertools.combinations(group,2)),
        tuple((c['feature']['id'],c['node']) for c in group)))


def refit(candidate,parameters=None):
    p={**PARAMETERS,**(parameters or {})}; audit={};catalog=[]
    def revise(t,routes,paths,homes):
        graph=conservative_graph(t,A1,homes);records=destinations(t,candidate,graph,p)
        catalog.extend({k:v for k,v in r.items() if k!='cells'}|{'sample_count':len(r['cells'])} for r in records)
        revised=[]
        for team in ('north','south'):
            entries=[e for e in paths if e[0]['team']==team];pools=[];search={}
            for e in entries:
                pool,diagnostic=route_options(t,e,records,graph,p)
                pools.append(pool);search[e[0]['id']]=diagnostic
            # A missing route must not prevent supported routes from being refit.
            viable=[i for i,pool in enumerate(pools) if pool]
            chosen=choose_joint(t,[pools[i] for i in viable],p)
            by_index=dict(zip(viable,chosen or []))
            for index,(r,full,outside,terminus) in enumerate(entries):
                if index not in by_index:
                    r['starter']['destination_revision']=True
                    r['starter']['destination']=None
                    r['starter']['status']='unresolved: old Starter geometry retained for diagnosis only, not an accepted new handoff'
                    r['starter']['failures'].append('no_supported_destination')
                    r['failures'].append('no_supported_destination')
                    revised.append((r,full,outside,terminus));continue
                c=by_index[index];opening=c['opening'];newfull=c['inside']+c['outside']
                # Reuse Starter diagnostic construction on the selected prefix;
                # explicit replacement below, never let its legacy cutoff choose.
                early,_=starter(t,[c['node']],c['depth'])
                early.update({'destination_revision':True,'destination':c['feature']|{'anchor':t.point(c['node']),
                              'score_terms':c['score_terms'],'original_suffix_join':c['original_suffix_join'],
                              'dry_neighbor_choices':c['dry_neighbor_choices'],
                              'unsupported_continuation_effective_blocks':round(t.length(c['tail']),3)},
                              'sample_path':[list(t.xy(i)) for i in opening],
                              'world_path':[t.point(i)['world_xyz'] for i in opening],
                              'supported_sample_path':[list(t.xy(i)) for i in c['inside']+opening],
                              'supported_world_path':[t.point(i)['world_xyz'] for i in c['inside']+opening],
                              'homeland_edge_xyz':r['starter']['homeland_edge_xyz'],
                              'homeland_facing_path':r['starter']['homeland_facing_path'],
                              'legibility_construction_score':round(feasibility(t,c['inside']+opening),4),
                              'max_sample_rise_y':max(abs(t.v['height'][a]-t.v['height'][b]) for a,b in zip((c['inside']+opening),(c['inside']+opening)[1:])),
                              'water_fraction':sum(t.v['water'][i] for i in opening)/len(opening),
                              'canopy_fraction':sum(t.v['canopy'][i] for i in opening)/len(opening),
                              'outside_preferred_depth_band':not t.p['starter_soft_min']<=c['depth']<=t.p['starter_soft_max'],
                              'termination_rule':'destination ends provided infrastructure, not the designated Route',
                              'failures':[]})
                physical=sum(math.dist(t.xy(a),t.xy(b))*t.s for a,b in zip(newfull,newfull[1:]))
                r.update({'starter':early,'sample_path':[list(t.xy(i)) for i in newfull],
                          'world_path':[t.point(i)['world_xyz'] for i in newfull],
                          'physical_path_blocks':round(physical,3),'effective_blocks_from_fountain':round(t.length(newfull),3),
                          'effective_blocks_from_edge':round(t.length(c['outside'])+t.cost(c['inside'][-1],opening[0])/2,3),
                          'corridor_quality_score':round(physical/max(physical,t.length(newfull)),4),'failures':[]})
                revised.append((r,newfull,c['outside'],c['node']))
            selected=list(by_index.values())
            audit[team]={'search':search,'anchored_routes':len(selected),
                         'distinct_features':len({c['feature']['id'] for c in selected}),
                         'distinct_opportunity_kinds':len({c['feature']['kind'] for c in selected}),
                         'joint_pair_penalty':round(sum(pair_cost(t,a,b,p) for a,b in itertools.combinations(selected,2)),4)}
        return routes,revised
    baseline=fit(candidate,route_revision=revise)
    result=refine(candidate,baseline)
    result.update({'schema':'default_task_a_destination_v1','status':'Analytical refit; human acceptance required before any physical materialization',
                   'destination_parameters':p,'destination_catalog':catalog,'destination_sets':audit})
    result['uncertainties'].update(LIMITS)
    for team,a in audit.items():
        diff=result['route_differentiation'][team]
        diff['geometry_only_three_choices_supported']=diff['three_meaningful_choices_supported']
        diff['three_meaningful_choices_supported'] &= a['anchored_routes']==3 and a['distinct_features']==3
        diff['narrow_opportunity_kind_warning']=a['distinct_opportunity_kinds']<p['minimum_opportunity_kinds']
        diff['destination_coverage']=a
        if not diff['three_meaningful_choices_supported']: result['failures'].append(f'{team}: three meaningful openings not established')
        if a['distinct_features']<3: result['failures'].append(f'{team}: three distinct destination formations not established')
        for r in result['routes']:
            if r['team']==team and r.get('starter'):
                r['starter']['refined_feasibility']['collective_choices_supported']=diff['three_meaningful_choices_supported']
    result['failures']=sorted(set(result['failures']))
    return result
