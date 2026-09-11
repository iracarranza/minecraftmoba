"""Bounded unsupported handoff geography, distinct from eventual network reach.

All resolutions below are provisional computational guards, not balance targets.
The depth horizon covers the remainder of the current operational band plus the
next existing band. The old band markers now index geometric Homeland Depth;
they do not convert terrain burden into depth or define a desired handoff depth.
"""
import heapq
import math
from collections import Counter

from .task_a import DEPTH_BANDS, shortest
from .task_a_refine import components, PARAMETERS as A1

POLICY = {
    'horizon': 'Upper edge of the next existing operational depth band after the band containing the handoff; exact boundaries enter the next band.',
    'open_ended_guard': 'Beyond the last finite band, extend by its existing width (350-250), computationally only; no new deep gameplay band.',
    'backward_guard': 'Fixed start-depth minus one sample diagonal, never reset during traversal.',
    'lateral_detour_guard': 'Physical graph path budget = twice the derived forward horizon span; bounds sideways floods and long water jumps, not a travel-cost ceiling.',
    'core_removal': 'Remove the one-sample-diagonal physical path neighborhood around the terminus.',
    'branch_evidence': 'Residual connected components merge all within-horizon reconvergence. Require existing four-sample feature support and at least three sample spacings of radial extent beyond the removed core, plus one sample of depth gain or a persistent new region.',
    'shape': '0 dead_end, 1 directed, 2 branching, 3+ junction. No ordinal quality or branch bonus.',
    'resolution_limit': 'Open connected land counts as one choice; sampled paths do not prove legible valleys/passes. Branching beyond the removed core in one connected component is deliberately not separate immediate choices.',
}


def horizon(start, spacing):
    limits=[float(v) for v,_ in DEPTH_BANDS]
    index=next((i for i,v in enumerate(limits) if start<v),len(limits))
    if index+1<len(limits):
        ceiling=limits[index+1];extended=False
    else:
        ceiling=max(start,limits[-1])+(limits[-1]-limits[-2]);extended=True
    return {'start_depth':start,'analysis_depth_limit':ceiling,
            'depth_floor':max(0.,start-math.sqrt(2)*spacing),
            'physical_path_budget':2*(ceiling-start),'open_ended_guard_used':extended}


def bounded_reach(t,graph,node,depth,limits,floor):
    """Geometric Dijkstra: no cost-driven horizon and no cumulative backsteps."""
    dist={node:0.};queue=[(0.,node)]
    while queue:
        value,i=heapq.heappop(queue)
        if value!=dist[i]:continue
        for q,_ in graph.get(i,()):
            if q not in depth or not floor-1e-8<=depth[q]<=limits['analysis_depth_limit']+1e-8:continue
            length=math.dist(t.xy(i),t.xy(q))*t.s
            cost=value+length
            if cost<=limits['physical_path_budget']+1e-8 and cost<dist.get(q,math.inf):
                dist[q]=cost;heapq.heappush(queue,(cost,q))
    return dist


def branch_evidence(t,graph,node,depth,membership,reach):
    core=math.sqrt(2)*t.s
    residual={i for i,d in reach.items() if d>core+1e-8}
    branches=[];discarded=[];support=A1['formation_band_min_samples']
    for group in components(graph,residual):
        counts=Counter(membership[i] for i in group)
        transitioned=sorted(r for r,n in counts.items() if r!=membership[node] and n>=support)
        gain=max(depth[i] for i in group)-depth[node]
        radial=max(reach[i] for i in group)-core
        meaningful=(len(group)>=support and radial+1e-8>=(support-1)*t.s
                    and (gain+1e-8>=t.s or bool(transitioned)))
        record={'sample_count':len(group),'max_depth':round(max(depth[i] for i in group),3),
                'radial_extent_beyond_core':round(radial,3),'persistent_new_regions':transitioned,
                'regions':sorted(counts),'representative_sample':min(group)}
        (branches if meaningful else discarded).append(record)
    return branches,discarded


def local_continuation(t,graphs,node,depth,regions,membership):
    limits=horizon(depth[node],t.s);by_id={r['id']:r for r in regions};own=membership[node]
    modes={};reaches={}
    for mode,graph in graphs.items():
        reach=bounded_reach(t,graph,node,depth,limits,limits['depth_floor']);reaches[mode]=reach
        branches,discarded=branch_evidence(t,graph,node,depth,membership,reach)
        region_ids=sorted({membership[i] for i in reach})
        # Compute effective extent on this SAME local induced subgraph, not on
        # a cheaper outside path that could leave and re-enter the horizon.
        induced={i:[(q,c) for q,c in graph[i] if q in reach] for i in reach}
        effective,_=shortest(induced,{node:0.})
        relaxed=bounded_reach(t,graph,node,depth,limits,0.)
        relaxed_branches,_=branch_evidence(t,graph,node,depth,membership,relaxed)
        strict=bounded_reach(t,graph,node,depth,limits,depth[node])
        strict_branches,_=branch_evidence(t,graph,node,depth,membership,strict)
        small_detour=bool(branches) and not strict_branches
        beyond_floor=not branches and bool(relaxed_branches)
        count=len(branches)
        modes[mode]={
            **{k:round(v,3) if isinstance(v,float) else v for k,v in limits.items()},
            'physical_extent':round(max(reach.values(),default=0),3),
            'euclidean_extent':round(max((math.dist(t.xy(node),t.xy(i))*t.s for i in reach),default=0),3),
            'effective_extent':round(max(effective.values(),default=0),3),
            'minimum_depth_reached':round(min(depth[i] for i in reach),3),
            'max_depth_reached':round(max(depth[i] for i in reach),3),
            'depth_gain':round(max(depth[i] for i in reach)-depth[node],3),
            'regions_reached':region_ids,'region_types_reached':sorted({by_id[r]['type'] for r in region_ids}),
            'entered_new_region':any(r!=own for r in region_ids),
            'entered_region_types':sorted({by_id[r]['type'] for r in region_ids}-{by_id[own]['type']}),
            'meaningful_branch_count':count,'continuation_class':('dead_end','directed','branching')[count] if count<3 else 'junction',
            'branches':branches,'discarded_pockets':discarded,'reachable_samples':len(reach),
            'backtracking_dependency':small_detour or beyond_floor,
            'small_backward_detour_dependency':small_detour,
            'beyond_local_floor_dependency':beyond_floor,
            'strict_forward_branch_count':len(strict_branches),
            'additional_samples_below_floor_detour':len(relaxed.keys()-reach.keys()),
            'relaxed_floor_branch_count':len(relaxed_branches),
            'confidence':'provisional sampled local connectivity; not verified player-perceived formations',
            'guard_contact':{
                'depth_ceiling':any(depth.get(q,0)>limits['analysis_depth_limit'] for i in reach for q,_ in graph[i]),
                'depth_floor':any(depth.get(q,math.inf)<limits['depth_floor'] for i in reach for q,_ in graph[i]),
                'physical_budget':any(q not in reach and limits['depth_floor']<=depth.get(q,-1)<=limits['analysis_depth_limit'] for i in reach for q,_ in graph[i])}}
    result=dict(modes['modest'])
    result.update({mode+'_branch_count':modes[mode]['meaningful_branch_count'] for mode in modes})
    result['major_intervention_dependency']=not modes['modest']['meaningful_branch_count'] and bool(modes['major']['meaningful_branch_count'])
    result['major_additional_regions']=sorted(set(modes['major']['regions_reached'])-set(modes['modest']['regions_reached']))
    result['by_dependency']=modes
    result['dependency_count_semantics']='Cumulative proxy graphs; counts need not increase because new links can merge branches. Major includes unverified water transport, not proof of required earthwork.'
    return result


def locally_viable(record):
    return record.get('local_continuation',{}).get('continuation_class') in ('directed','branching','junction')
