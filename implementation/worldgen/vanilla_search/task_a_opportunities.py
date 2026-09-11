"""Spec 4 shadow diagnostics. No active selection/fitting function is called.

Evidence categories are not scores. Additional water/cover connectivity indexes
describe reach over the frozen terrain, never replace its region segmentation.
"""
import copy
import math
from collections import Counter, defaultdict, deque

from successor.grid import unrle
from .task_a import Terrain, shortest, path_to
from .task_a_refine import components, PARAMETERS as A1
from .task_a_network import (terrain_graph, outward_field, home_cells,
                             destination_pool, PARAMETERS, MODES)
from .task_a_local import bounded_reach, horizon

STATES=('strong','supported','weak','unsupported')
POLICY={
    'mode':'Shadow only. Consume frozen local-continuation fits; never call selection, refit routes, or change eligibility.',
    'arrival':'Connected same-feature dry samples within the existing 32-block analytical approach scale; diagnostic only. One anchor can remain if evidence is sparse.',
    'cover_persistence':'Use existing forest/highland masks on the modest dry graph. Existing 24-sample formation support plus four inward sample layers distinguish a core from a thin strip. This diagnostic heuristic is not eligibility or resource proof.',
    'water':'Cardinal connected samples whose recorded surface block is water; ice/other surfaces excluded. Adjacent former ocean/inland IDs can share a diagnostic network, but active water identities are unchanged. Sample continuity is conditional, not boat clearance or depth proof.',
    'water_ports':'Connected shore contacts grouped by frozen region and diagnostic water network; at least four shore samples. Region ID alone is not distinct value. Different land components or persistent forest/highland systems provide projection context.',
    'water_shape':'Double-sweep geodesic extent is a lower bound. At least the inherited 32-block continuation scale and length >= twice area/length indicate corridor geometry provisionally. Multiple distinct shore contexts support network geometry. Neither proves payoff.',
    'payoff':'Only observed village footprints and demonstrated differentiated geographic-system projection count as observed. Remote persistent-system/landmass access can be potential. Ore, crops, animals, caves and other structure access/value are unmeasured. No authored locations.',
    'near_shore_payoff':'Village edge within the existing 32-block approach scale of a reachable shore is conditional landfall access; payoff may be far from the initial handoff along the water. No unlimited shore-to-any-village flood.',
    'comparison':'Compare claimed entered systems with outward homeland sources, and remote water contacts with origin shore/system context. Different biome alone is never payoff.',
    'commitment':'Qualitative dependency/geometry proxies, not fixed distance bands: natural starter low, modest medium, major high; exploitation/return unresolved where payoff or water navigation is unverified; exposure always unresolved.',
    'rejected_sample':'Deterministic best-utility examples per feature kind, local-dead-end, locally-valid, water, village and duplicate-opportunity strata; at most 12 per homeland, not exhaustive or a new ranking.',
    'completion':'Complete/Structurally Supported refer only to sampled analytical evidence, never physical readiness. Missing evidence is conservatively Incomplete with an unresolved reason, not proof of terrain failure.',
}


def evidence(state,**facts):return {'state':state,'evidence':facts}
def supported(e):return e.get('state') in ('strong','supported')
def index(t,point):return point['sample'][1]*t.w+point['sample'][0]
def cardinal(t,i):return [q for q in t.neighbors(i) if sum(abs(a-b) for a,b in zip(t.xy(i),t.xy(q)))==1]


def interpret(facts):
    """Pure semantic rules, independently testable without Minecraft fixtures."""
    usable=facts['handoff_usable'];g=facts.get('gateway',{});w=facts.get('water',{})
    observed=facts.get('observed',[]);potential=facts.get('potential',[])
    direct=facts.get('direct_interaction',False)
    persistent=g.get('threshold') and g.get('persistent') and g.get('commitment') and g.get('distinct_from_origin')
    water_reach=w.get('interface') and (w.get('corridor') or w.get('network')) and w.get('distinct_access')
    branches=facts.get('branches',[])
    different=any(set(a['contexts'])!=set(b['contexts']) and set(a['contexts']) and set(b['contexts'])
                  for n,a in enumerate(branches) for b in branches[n+1:])
    junction=len(branches)>=2 and different and not facts.get('rapid_reconvergence',False)
    functions={
        'gateway':evidence('supported' if usable and persistent else 'weak' if g.get('threshold') else 'unsupported',**g),
        'junction':evidence('supported' if usable and junction else 'weak' if len(branches)>=2 else 'unsupported',
            branches=branches,materially_different=different,rapid_reconvergence=facts.get('rapid_reconvergence',False)),
        'destination':evidence('strong' if usable and direct else 'unsupported',observed_direct_interaction=direct),
        'transition':evidence('supported' if usable and water_reach and observed else 'weak' if usable and water_reach else 'unsupported',
            **w,payoff_observed=bool(observed),payoff_potential=bool(potential))}
    structural=usable and (direct or persistent or water_reach or junction)
    complete=structural and bool(observed)
    affordances=[]
    def add(kind,state,**why):affordances.append({'type':kind,'evidence_state':state,'evidence':why})
    add('passage','supported' if facts.get('local_viable') else 'unsupported',local_terrestrial_viability=facts.get('local_viable',False))
    add('threshold','supported' if g.get('threshold') or w.get('interface') else 'unsupported',terrain_interface=bool(g.get('threshold')),water_interface=bool(w.get('interface')))
    add('barrier','supported' if w.get('barrier') else 'weak' if w else 'unsupported',opposite_bank_evidence=w.get('barrier',False))
    add('crossing','supported' if w.get('crossing') else 'unsupported',conditional_opposite_bank_access=w.get('crossing',False))
    add('corridor_access','supported' if usable and w.get('interface') and w.get('corridor') else 'unsupported',sampled_water_corridor=w.get('corridor',False))
    add('network_access','supported' if usable and ((w.get('interface') and w.get('network')) or junction) else 'unsupported',
        sampled_water_network=w.get('network',False),local_opportunity_junction=junction)
    add('direct_interaction','supported' if usable and direct else 'unsupported',village_footprint=direct)
    return {'supported_functions':functions,'affordances':affordances,
            'completion_state':'complete' if complete else 'structurally_supported' if structural else 'incomplete',
            'completion_evidence':{'handoff':usable,'appropriate_affordance_and_reach':bool(structural),'observed_payoff':bool(observed),
                'authored_completion_allowed':bool(structural and not complete),
                'reason':'sampled relationship with observed payoff' if complete else 'geographic relationship exists; payoff unresolved/potential' if structural else 'affordance/reach or usable handoff not established; hypothetical POI cannot rescue this'}}


class ShadowTerrain:
    def __init__(self,candidate,fit):
        self.t=t=Terrain(candidate);self.fit=fit
        excluded=set.union(*(home_cells(t,h) for h in fit['homelands'].values()))
        self.graphs,self.depth_graph,self.edges,self.systems,self.water_id=terrain_graph(t,excluded,fit['parameters'])
        self.membership={i:r for i,r in enumerate(unrle(fit['region_grid_rle'])) if r is not None}
        self.regions={r['id']:dict(r,cells=[]) for r in fit['regions']}
        for i,r in self.membership.items():self.regions[r]['cells'].append(i)
        self.catalog={r['id']:r for r in destination_pool(t,candidate,self.graphs['major'],list(self.regions.values()),
            self.membership,fit['terrain_connectivity_graph'],self.systems,self.water_id)}
        self.fields={team:outward_field(t,team,fit['homelands'],self.graphs,self.depth_graph,fit['parameters']) for team in ('north','south')}
        # A dry physical graph, without the conditional cross-water edges.
        self.dry={i:[(q,c) for q,c in es if math.dist(t.xy(i),t.xy(q))<=math.sqrt(2)+1e-8] for i,es in self.graphs['modest'].items()}
        self.land={};self.land_groups={}
        for group in components(self.dry,set(self.dry)):
            lid='dry-component-'+str(min(group));self.land_groups[lid]=set(group)
            for i in group:self.land[i]=lid
        self.cover={};self.cover_at=defaultdict(set)
        for kind,key in (('forest','forest'),('highland','highland')):
            mask={i for i in self.dry if t.v[key][i]}
            for group in components(self.dry,mask):
                cells=set(group);cid=kind+'-system-'+str(min(cells))
                boundary={i for i in cells if any(q not in cells for q in cardinal(t,i)) or len(cardinal(t,i))<4}
                inward={i:1 for i in boundary};queue=deque(sorted(boundary))
                while queue:
                    i=queue.popleft()
                    for q in cardinal(t,i):
                        if q in cells and q not in inward:inward[q]=inward[i]+1;queue.append(q)
                core={i for i,n in inward.items() if n>=A1['formation_band_min_samples']}
                self.cover[cid]={'id':cid,'kind':kind,'cells':cells,'core':core,'inward':inward,
                    'persistent':len(cells)>=A1['formation_min_samples'] and bool(core),
                    'relief':max(t.v['height'][i] for i in cells)-min(t.v['height'][i] for i in cells)}
                for i in cells:self.cover_at[i].add(cid)
        self.water_graph={i:[(q,t.s) for q in cardinal(t,i) if t.v['water'][q] and t.v['top'][q]=='minecraft:water']
            for i in range(t.n) if t.v['water'][i] and t.v['top'][i]=='minecraft:water'}
        self.water_networks={};self.network_at={};self.bank_networks=defaultdict(set)
        self.water_distance_cache={}
        for group in components(self.water_graph,set(self.water_graph)):
            wid='water-network-'+str(min(group));cells=set(group)
            for i in cells:self.network_at[i]=wid
            dist,_=shortest(self.water_graph,{min(cells):0});far=max(dist,key=lambda i:(dist[i],-i))
            dist,_=shortest(self.water_graph,{far:0});span=max(dist.values(),default=0)
            bank={q for i in cells for q in cardinal(t,i) if q in self.dry}
            ports=[];by_region=defaultdict(set)
            for i in bank:self.bank_networks[i].add(wid);by_region[self.membership[i]].add(i)
            for rid,mask in sorted(by_region.items()):
                for shore in components(self.dry,mask):
                    if len(shore)<A1['formation_band_min_samples']:continue
                    contacts=sorted({q for i in shore for q in cardinal(t,i) if q in cells})
                    covers=sorted({cid for i in shore for q in [i]+list(t.neighbors(i)) for cid in self.cover_at[q] if self.cover[cid]['persistent']})
                    ports.append({'id':wid+':shore-'+str(min(shore)),'region':rid,'region_type':self.regions[rid]['type'],
                        'land_component':self.land[shore[0]],'cover_systems':covers,'cells':shore,'water_contacts':contacts})
            contexts={(p['land_component'],tuple(p['cover_systems'])) for p in ports}
            breadth=len(cells)*t.s*t.s/max(span,t.s)
            corridor=span>=A1['minimum_continuation_blocks'] and span>=2*breadth
            network=span>=A1['minimum_continuation_blocks'] and len(contexts)>=2
            self.water_networks[wid]={'id':wid,'cells':cells,'ports':ports,'extent_lower_bound_blocks':round(span,3),
                'mean_breadth_proxy':round(breadth,3),'corridor':corridor,'maritime_network':network,
                'original_water_system_ids':sorted({self.water_id[i] for i in cells}),
                'touches_window_edge':any(t.xy(i)[0] in (0,t.w-1) or t.xy(i)[1] in (0,t.h-1) for i in cells),
                'navigation_state':'conditional sampled open-water continuity; depth, channel width, embarkation and obstacles unverified'}
        self.villages={r['id']:r for r in self.catalog.values() if r['kind']=='settlement approach'}
        self.village_near=defaultdict(set)
        physical_dry={i:[(q,math.dist(t.xy(i),t.xy(q))*t.s) for q,_ in edges] for i,edges in self.dry.items()}
        for vid,village in self.villages.items():
            distances,_=shortest(physical_dry,{i:0 for i in village['cells']})
            for i,d in distances.items():
                if d<=A1['central_approach_blocks']:self.village_near[i].add(vid)
        for network in self.water_networks.values():
            contexts=set()
            for port in network['ports']:
                if len(self.land_groups[port['land_component']])>=A1['formation_min_samples']:
                    contexts.add(('land',port['land_component']))
                contexts.update(('cover',cid) for cid in port['cover_systems'])
                contexts.update(('village',vid) for i in port['cells'] for vid in self.village_near[i])
            network['maritime_network']=network['extent_lower_bound_blocks']>=A1['minimum_continuation_blocks'] and (
                (len(network['ports'])>=2 and len(contexts)>=2) or sum(k=='village' for k,_ in contexts)>=2)
            network['projection_contexts']=[list(c) for c in sorted(contexts)]

    def envelope(self,r):
        t=self.t;node=index(t,r['anchor']);record=self.catalog.get(r['id'],{})
        allowed=set(record.get('cells',()))|{node};limit=A1['central_approach_blocks']
        graph={i:[(q,math.dist(t.xy(i),t.xy(q))*t.s) for q,_ in self.dry.get(i,()) if q in allowed and math.dist(t.xy(node),t.xy(q))*t.s<=limit] for i in allowed}
        dist,_=shortest(graph,{node:0});cells=sorted(i for i,d in dist.items() if d<=limit)
        return {'samples':[list(t.xy(i)) for i in cells],'sample_count':len(cells),
            'world_points':[t.point(i)['world_xyz'] for i in cells],'radius_guard_blocks':limit,
            'scope':'same detected feature, modest dry connectivity; practical/visual equivalence unverified; no path refit'},cells

    def origin(self,team,r,corridor):
        t=self.t;node=index(t,r['anchor']);field=self.fields[team]
        if corridor and corridor.get('sample_path'):
            path=[z*t.w+x for x,z in corridor['sample_path']];gate=index(t,corridor['departure'])
            cells=home_cells(t,self.fit['homelands'][team]);source=next((i for i in path if i not in cells),node)
            method='frozen current destination-first corridor'
        else:
            mode=r['starter_dependency'];path=path_to(field['previous'][mode],node)
            source=path[0];gate=field['origins'][mode].get(source,source);method='current homeland-wide traversal field; no historical departure'
        x0,x1,z0,z1=self.fit['homelands'][team]['logical_footprint_samples'];x,z=t.xy(source)
        vector=[round(x-(x0+x1)/2,3),round(z-(z0+z1)/2,3)]
        return {'homeland_id':team,'implied_departure':t.point(gate),'perimeter_source':t.point(source),
            'opening_direction':{'logical_xz_vector':vector},'derivation':method},path

    def persistence(self,team,r,node,local_nodes):
        t=self.t;field=self.fields[team];depth=field['depth'];claims=[]
        neighbors=[node]+list(t.neighbors(node))
        direct_dry={node}|{q for q,_ in self.dry[node]}
        touched=sorted({cid for i in neighbors for cid in self.cover_at[i]})
        for cid in touched:
            c=self.cover[cid];cells=c['cells'];entries={i for i in neighbors if i in cells and i in local_nodes and i in direct_dry}
            graph={i:[(q,cost) for q,cost in self.dry[i] if q in cells] for i in cells} if entries else {}
            dist,_=shortest(graph,{i:0 for i in entries});reached=set(dist)
            core=reached&c['core'];local=reached&local_nodes
            threshold=node not in cells and any(q in cells for q in t.neighbors(node))
            equivalent=bool(cells&set(field['sources']))
            claims.append({'system_id':cid,'kind':c['kind'],'threshold':threshold,'sample_count':len(reached),
                'raw_neighbor_feature_present':True,'immediate_modest_entry':bool(entries),
                'whole_system_sample_count':len(cells),'whole_system_core_samples':len(c['core']),
                'whole_system_persistent':c['persistent'],'whole_system_max_inward_layers':max(c['inward'].values(),default=0),
                'entry_limitation':None if entries else 'mask present next to anchor, but no immediate modest dry connection within the existing local reach; steep/diagonal/sampling dependency unresolved',
                'local_entered_samples':len(local),'core_samples':len(core),
                'maximum_inward_layers':max((c['inward'].get(i,0) for i in reached),default=0),
                'persistent':c['persistent'] and bool(core),
                'commitment':len(local)>=A1['formation_band_min_samples'] and max((depth.get(i,0) for i in core),default=0)>depth[node],
                'distinct_from_origin':not equivalent,'homeland_already_accesses_same_system':equivalent,
                'max_system_homeland_depth':round(max((depth.get(i,0) for i in reached),default=0),3),
                'core_travel_cost_from_threshold':round(min((dist[i] for i in core),default=0),3) if core else None,
                'relief_blocks':c['relief'],'reachable_regions':sorted({self.membership[i] for i in reached}),
                'core_regions':sorted({self.membership[i] for i in core}),
                'limitation':'cover/relief/core evidence only; no ore, resource yield, visibility or exact physical entrance proof'})
        return claims

    def water_reach(self,team,r,envelope):
        t=self.t;node=index(t,r['anchor']);depth=self.fields[team]['depth']
        networks=sorted({wid for i in envelope for wid in self.bank_networks[i]})
        output=[];observed=[];potential=[]
        for wid in networks:
            network=self.water_networks[wid];cells=network['cells']
            banks=[i for i in envelope if wid in self.bank_networks[i]]
            # Water-cell height can be seabed, not surface elevation. Never
            # infer embarkation cliffs or fords from that height difference.
            usable=[i for i in banks if self.dry[i]]
            starts={q for i in banks for q in cardinal(t,i) if q in cells}
            dist,_=shortest(self.water_graph,{i:0 for i in starts})
            origin_land={self.land[i] for i in banks};origin_cover={cid for i in banks for cid in self.cover_at[i]}
            shores=[]
            for port in network['ports']:
                d=min((dist.get(i,math.inf) for i in port['water_contacts']),default=math.inf)
                if not math.isfinite(d):continue
                remote=d>=A1['minimum_continuation_blocks']
                cover=set(port['cover_systems'])-origin_cover
                separate=port['land_component'] not in origin_land and len(self.land_groups[port['land_component']])>=A1['formation_min_samples']
                opening=bool(set(port['cells'])&set(self.fields[team]['sources'])) or max((depth.get(i,0) for i in port['cells']),default=0)<=r['homeland_depth']
                distinct=remote and not opening and (separate or bool(cover))
                village_ids=sorted({vid for b in port['cells'] for vid in self.village_near[b]})
                village_comparison={vid:{'homeland_depth':round(min(depth.get(i,math.inf) for i in self.villages[vid]['cells']),3),
                    'homeland_modest_travel_cost':min((self.fields[team]['travel']['modest'].get(i,math.inf) for i in self.villages[vid]['cells']),default=math.inf),
                    'water_path_to_village_shore_blocks':round(min((dist[q] for b in port['cells'] if vid in self.village_near[b] for q in cardinal(t,b) if q in dist),default=0),3)} for vid in village_ids}
                for v in village_comparison.values():
                    v['homeland_modest_travel_cost']=round(v['homeland_modest_travel_cost'],3) if math.isfinite(v['homeland_modest_travel_cost']) else None
                    v['homeland_depth']=v['homeland_depth'] if math.isfinite(v['homeland_depth']) else None
                downstream_villages=[vid for vid,v in village_comparison.items() if v['homeland_depth'] is not None and v['homeland_depth']>r['homeland_depth']
                    and v['water_path_to_village_shore_blocks']>=A1['minimum_continuation_blocks']
                    and (v['homeland_modest_travel_cost'] is None or v['homeland_modest_travel_cost']>r.get('travel_cost',self.fields[team]['travel']['modest'].get(node,0)))]
                if usable and (network['corridor'] or network['maritime_network'] or downstream_villages):
                    for vid in downstream_villages:
                        observed.append({'id':vid,'kind':'village','via':wid,'shore':port['id'],
                            'evidence':'observed village footprint near a reachable remote shore; conditional embarkation/navigation/landfall',
                            'alternative_land_access':village_comparison[vid],
                            'water_advantage':'unresolved: sampled water-path length is not calibrated against land Travel Cost; coherence does not establish dominance',
                            'value_dimensions':{'structure_poi_value':'supported','projection':'supported','scarcity':'unresolved','resources':'unresolved'}})
                    if distinct and not downstream_villages:
                        potential.append({'id':'projection:'+port['id'],'kind':'remote shore projection','via':wid,'region':port['region'],
                            'land_component':port['land_component'],'cover_systems':sorted(cover),
                            'evidence':'separate dry reach component or persistent geographic system beyond opening shore; no observed gameplay-bearing POI at this contact',
                            'value_dimensions':{'projection':'supported','development_potential':'unresolved','extraction_potential':'unresolved'},
                            'authored_location':None})
                shores.append({k:v for k,v in port.items() if k not in ('cells','water_contacts')}|
                    {'sample_count':len(port['cells']),'representative':t.point(min(port['cells'])),
                     'water_path_blocks':round(d,3),'distinct_remote_access':distinct,'opening_equivalent':opening,'nearby_villages':village_ids,
                     'downstream_villages':downstream_villages,'village_comparison':village_comparison})
            crossings=[]
            for system in self.systems:
                if system['id'] not in network['original_water_system_ids']:continue
                for crossing in system['crossings']:
                    a,b=crossing['banks']
                    if not any(math.dist(t.xy(node),t.xy(i))*t.s<=A1['central_approach_blocks'] for i in (a,b)):continue
                    different=self.land.get(a)!=self.land.get(b) or self.regions[self.membership[a]]['type']!=self.regions[self.membership[b]]['type']
                    crossings.append(crossing|{'opposite_context_distinct':different,'bank_regions':[self.membership[a],self.membership[b]]})
            useful_crossing=any(c['dependency']=='modest' and c['opposite_context_distinct'] for c in crossings)
            barrier=any(self.land.get(c['banks'][0])!=self.land.get(c['banks'][1]) for c in crossings)
            output.append({'network_id':wid,'interface':bool(usable),'interface_samples':[t.point(i) for i in usable],
                'interface_confidence':'sampled dry shore contact only; water surface height/embarkation unverified',
                'corridor':network['corridor'],'network':network['maritime_network'] or any(s['downstream_villages'] for s in shores),
                'network_basis':'multiple supported shore contexts, or measured distinct origin-to-village shoreline access in one connected water system',
                'distinct_access':any(s['distinct_remote_access'] for s in shores) or any(s['downstream_villages'] for s in shores),
                'crossing':useful_crossing,'barrier':barrier,'local_feature':not network['corridor'] and not network['maritime_network'],
                'shore_access':shores,'crossings':crossings,'reachable_open_water_samples':len(dist),
                'maximum_water_path_blocks':round(max(dist.values(),default=0),3),
                'same_network_contacts_at_homeland_perimeter':sum(wid in self.bank_networks[i] for i in self.fields[team]['sources']),
                'touches_window_edge':network['touches_window_edge'],'navigation_state':network['navigation_state'],
                'original_water_system_ids':network['original_water_system_ids']})
        return output,observed,potential

    def relationship(self,team,r,slot=None):
        t=self.t;node=index(t,r['anchor']);field=self.fields[team];depth=field['depth']
        corridor=next((c for c in self.fit['starter_corridors'][team] if c['destination']==r['id']),None)
        arrival,envelope=self.envelope(r);origin,path=self.origin(team,r,corridor)
        local=r['local_continuation'];limits=horizon(depth[node],t.s)
        local_dist=bounded_reach(t,self.graphs['modest'],node,depth,limits,limits['depth_floor']);local_nodes=set(local_dist)
        claims=self.persistence(team,r,node,local_nodes)
        water,water_observed,water_potential=self.water_reach(team,r,envelope)
        own_village=r['id'] in self.villages
        observed=[];potential=[]
        if own_village:observed.append({'id':r['id'],'kind':'village','evidence':'current arrival envelope reaches observed vanilla village edge',
            'value_dimensions':{'structure_poi_value':'supported','occupancy':'unresolved','resource_equivalence':'unresolved'}})
        gateway_claims=[c for c in claims if c['threshold'] and c['persistent'] and c['commitment'] and c['distinct_from_origin']]
        for c in gateway_claims:
            # Persistent terrain is structural evidence, not automatic value.
            near_villages=[vid for vid,v in self.villages.items() if set(v['cells'])&self.cover[c['system_id']]['cells']
                and min(depth.get(i,math.inf) for i in v['cells'])>r['homeland_depth']]
            for vid in near_villages:observed.append({'id':vid,'kind':'village','via':c['system_id'],'evidence':'observed village edge inside the entered connected system',
                'value_dimensions':{'structure_poi_value':'supported','projection':'supported'}})
            contexts={self.regions[rid]['type'] for rid in c['core_regions']}
            # Relief + persistence + multiple core contexts constitute observed
            # geographic projection, not inferred mineral or crop value.
            if c['kind']=='highland' and c['relief_blocks']>=t.p['severe_rise_y'] and len(contexts)>=2:
                observed.append({'id':c['system_id'],'kind':'differentiated highland system','evidence':'persistent entered highland core spans relief and multiple existing geographic contexts beyond equivalent homeland access',
                    'value_dimensions':{'projection':'supported','strategic_position':'supported','extraction_potential':'unresolved','scarcity':'unresolved'}})
            elif not near_villages:
                potential.append({'id':c['system_id'],'kind':'persistent '+c['kind']+' interior','evidence':'entered system has a sustained core and increasing commitment, but gameplay-bearing resources/POI not established',
                    'value_dimensions':{'projection':'supported','development_potential':'unresolved','extraction_potential':'unresolved'},'authored_location':None})
        observed.extend(water_observed);potential.extend(water_potential)
        unique=lambda items:list({(r['id'],r.get('via')):r for r in items}.values())
        observed=unique(observed);potential=unique(potential)
        best=max(claims,key=lambda c:(c in gateway_claims,c['threshold'],c['persistent'],c['core_samples'],c['system_id']),default={})
        viable_water=[w for w in water if w['interface']]
        water_fact={k:any(w[k] for w in viable_water) for k in ('interface','corridor','network','distinct_access','crossing','barrier')} if water else {}
        if water:water_fact['barrier']=any(w['barrier'] for w in water)
        branches=[]
        residual={i for i,d in local_dist.items() if d>math.sqrt(2)*t.s+1e-8}
        branch_cells={min(group):group for group in components(self.graphs['modest'],residual)}
        for b in local['branches']:
            cells=branch_cells.get(b['representative_sample'],[])
            cover_counts=Counter(cid for i in cells for cid in self.cover_at[i] if self.cover[cid]['persistent'])
            contexts={self.cover[cid]['kind']+' system' for cid,n in cover_counts.items() if n>=A1['formation_band_min_samples']}
            if any(self.village_near[i] for i in cells):contexts.add('village relationship')
            if any(self.water_networks[wid]['corridor'] for i in cells for wid in self.bank_networks[i]):contexts.add('water corridor')
            if any(self.water_networks[wid]['maritime_network'] for i in cells for wid in self.bank_networks[i]):contexts.add('water network')
            branches.append({'contexts':sorted(contexts),'unresolved_context':not contexts,
                'persistent_samples':b['sample_count'],'extent':b['radial_extent_beyond_core']})
        facts={'handoff_usable':r['constructability']>=A1['starter_min_score'] and r['starter_dependency']!='major',
            'local_viable':local['continuation_class']!='dead_end','direct_interaction':own_village,
            'gateway':best,'water':water_fact,'branches':branches,'rapid_reconvergence':False,'observed':observed,'potential':potential}
        semantic=interpret(facts)
        if semantic['completion_state']=='incomplete':potential=[] # no hypothetical authored rescue
        starter_dependency=(corridor or {}).get('travel_dependency',r['starter_dependency'])
        commitment={'access_burden':{'state':{'natural':'low','modest':'medium','major':'high'}[starter_dependency],
                'evidence':'current Starter dependency proxy; provided infrastructure is assumed, not built'},
            'exploitation_burden':{'state':'unresolved' if water or not observed else 'medium' if gateway_claims else 'low',
                'evidence':'water/unknown payoff unmeasured; persistent interior requires unsupported operation'},
            'return_burden':{'state':'unresolved' if water else 'medium' if local['backtracking_dependency'] else 'low',
                'evidence':'symmetric sampled land graph; cargo, food and actual traversal time unmeasured'},
            'infrastructure_dependency':{'state':'unresolved' if water else 'high' if local['major_intervention_dependency'] else 'medium' if r['starter_dependency']=='modest' else 'low',
                'evidence':'sampled traversal proxy; water transport unknown, no construction planned'},
            'exposure':{'state':'unresolved','evidence':'no sightline/player/combat measurement'}}
        contexts=sorted({c['system_id'] for c in gateway_claims}|{w['network_id'] for w in water if w['interface'] and (w['network'] or w['corridor'])}|({r['id']} if own_village else set()))
        desc=f'{r["kind"]} → '+('/'.join(k.title() for k,v in semantic['supported_functions'].items() if supported(v)) or 'function not established')+' → '+semantic['completion_state']
        if water and not observed:desc+='; water mobility has no demonstrated valuable payoff'
        return {'relationship_id':f'{team}:{r["id"]}','homeland':team,'source_candidate_id':r['id'],'slot':slot,
            'current_selection':{'selected':slot is not None,'eligible':r['eligible'],'utility':r['utility'],'local_rejection':r.get('local_rejection')},
            'origin':origin,'handoff':{'anchor':r['anchor'],'arrival_envelope':arrival,'feature_kind':r['kind'],
                'feature_component':r['opportunity_group'],'homeland_depth':r['homeland_depth'],'travel_cost':r['travel_cost'],
                'starter_constructability':r['constructability'],'local_context':self.membership[node]},
            **semantic,'reach':{'local_continuation':copy.deepcopy(local),'geographic_persistence':claims,
                'reachable_regions':local['regions_reached'],'reachable_region_types':local['region_types_reached'],
                'reachable_features':sorted({v['id'] for v in observed if v['kind']=='village'}),
                'water_reach':water,'crossing_reach':[c for w in water for c in w['crossings']],
                'intervention_dependencies':r['continuation_dependencies'],'deep_network_reach':copy.deepcopy(r['deep_network_reach'])},
            'payoff':{'observed':observed,'potential':potential,'authored':[]},'commitment_profile':commitment,
            'context_ids':contexts,'same_team_relationships':[],'opponent_relationships':[],
            'stopping_principle':{'principle':'Starter infrastructure should stop where further pre-established infrastructure would solve the opportunity rather than merely expose it.',
                'diagnostic':'arrival at edge; exploitation remains unsupported' if own_village or gateway_claims or water else 'exposed opportunity is not established',
                'automatic_length_change':False},
            'evidence_summary':{'description':desc,'weak_evidence':r['weak_evidence'],
                'limitations':['Eight-block sampling does not prove usable embarkation, navigation, POI occupancy, resource value or player-perceived access privilege.',
                    'Frozen regional identities and sample persistence are evidence, not strategic importance scores.'],
                'selection_disagreement':('selected but relationship incomplete' if slot is not None and semantic['completion_state']=='incomplete' else
                    'unselected but coherent shadow relationship' if slot is None and semantic['completion_state']!='incomplete' else
                    'selected structural relationship needs payoff evidence' if slot is not None and semantic['completion_state']=='structurally_supported' else None)}}


def expand_pool(fit,r):
    out=copy.deepcopy(r)
    if not out.get('deep_network_reach'):return out
    for c in out['deep_network_reach'].values():
        for key in ('deeper_regions_reached','additional_deeper_regions_requiring_backtracking','distinct_deeper_region_types'):
            ref=c.pop(key+'_ref');c.pop(key+'_count')
            c[key]=fit['continuation_type_sets' if key=='distinct_deeper_region_types' else 'continuation_region_sets'][ref]
    out['continuation_dependencies']={k.removesuffix('_ref'):fit['continuation_region_sets'][v] for k,v in out['continuation_dependencies'].items()}
    return out


def rejected_sample(fit,team):
    chosen=fit['selected_handoffs'][team];selected={r['id'] for r in chosen};groups={r['opportunity_group'] for r in chosen}
    pool=sorted((r for r in fit['destination_pools'][team] if r['id'] not in selected and r.get('anchor')),key=lambda r:(-r['utility'],r['id']))
    strata={kind:lambda r,kind=kind:r['kind']==kind for kind in sorted({r['kind'] for r in pool})}
    strata.update({'local_dead_end':lambda r:r['local_continuation']['continuation_class']=='dead_end',
        'locally_valid':lambda r:r['local_continuation']['continuation_class']!='dead_end',
        'same_opportunity_group':lambda r:r['opportunity_group'] in groups})
    picked={}
    for name,predicate in strata.items():
        candidates=[r for r in pool if predicate(r)]
        for r in candidates[:1]:picked.setdefault(r['id'],{'record':r,'strata':[]})['strata'].append(name)
    for r in pool:
        if len(picked)>=12:break
        picked.setdefault(r['id'],{'record':r,'strata':['highest remaining active utility']})
    return list(picked.values())[:12]


def targets(r,kind=None):
    return {v['id'] for k in ('observed','potential') for v in r['payoff'][k] if kind is None or v['kind']==kind}


def set_analysis(fit,relationships):
    sets={};opposing=[];by_team={t:[r for r in relationships if r['homeland']==t and r['slot'] is not None] for t in ('north','south')}
    topo=fit['topology']['by_dependency']['modest']
    for team,rr in by_team.items():
        pairs=[]
        for i,a in enumerate(rr):
            for j,b in enumerate(rr[i+1:],i+1):
                shared=targets(a)&targets(b);contexts=set(a['context_ids'])&set(b['context_ids']);labels=[]
                if shared or contexts:labels.append('overlapping')
                substantive=bool(targets(a)) and bool(targets(b))
                substitutes=substantive and targets(a)==targets(b) and set(a['context_ids'])==set(b['context_ids'])
                if substitutes:labels.append('substitutes')
                if substantive and not substitutes and targets(a)!=targets(b):labels.append('complementary')
                spatial=topo['same_team_lateral_matrices'][team][i][j]
                if spatial:labels.append('connected')
                p={'relationships':[a['relationship_id'],b['relationship_id']],'labels':labels,'shared_payoffs':sorted(shared),
                    'shared_contexts':sorted(contexts),'spatial_connection':spatial,
                    'confidence':'sampled/conditional; complementarity means different accessible opportunities, not measured productive synergy'}
                pairs.append(p);a['same_team_relationships'].append(p);b['same_team_relationships'].append(p)
        removal=[]
        for a in rr:
            others=[b for b in rr if b is not a]
            unique=targets(a)-set().union(*(targets(b) for b in others))
            removal.append({'relationship':a['relationship_id'],'unique_payoffs':sorted(unique),
                'lost_meaningful_opportunity':'supported/potential' if unique else 'not established; others overlap or payoff unresolved'})
        aff=[{v['type'] for v in r['affordances'] if v['evidence_state'] in ('strong','supported')} for r in rr]
        state=('redundant' if pairs and all('substitutes' in p['labels'] for p in pairs) else
            'partially overlapping' if any('overlapping' in p['labels'] for p in pairs) else
            'strongly differentiated' if len(rr)>1 and all(v['unique_payoffs'] for v in removal) and len({tuple(sorted(a)) for a in aff})>1 else
            'complementary' if any('complementary' in p['labels'] for p in pairs) else 'unresolved')
        sets[team]={'diversity_state':state,'geographic_contexts':sorted({c for r in rr for c in r['context_ids']}),
            'affordance_sets':[sorted(a) for a in aff],'payoff_kinds':sorted({v['kind'] for r in rr for k in ('observed','potential') for v in r['payoff'][k]}),
            'commitment_profiles':[r['commitment_profile'] for r in rr],'removal_diagnostic':removal,'same_team_pairs':pairs,
            'projection_coverage':{'context_count':len({c for r in rr for c in r['context_ids']}),'opportunity_ids':sorted(set().union(*(targets(r) for r in rr)))},
            'authored_payoff_dependencies':[r['relationship_id'] for r in rr if r['completion_state']=='structurally_supported'],
            'incomplete_not_rescuable_by_authored_poi':[r['relationship_id'] for r in rr if r['completion_state']=='incomplete']}
    for i,a in enumerate(by_team['north']):
        for j,b in enumerate(by_team['south']):
            shared=targets(a)&targets(b);systems=set(a['context_ids'])&set(b['context_ids'])
            spatial=topo['north_south_convergence_matrix'][i][j]
            labels=['shared'] if shared or systems else ['parallel'] if set(a['context_ids']) and set(b['context_ids']) else []
            if shared:labels.append('contested')
            if targets(a) and targets(b) and not shared and not systems:labels.append('exclusive')
            p={'relationships':[a['relationship_id'],b['relationship_id']],'labels':labels,'spatial_convergence':spatial,
                'opportunity_convergence':{'state':'supported' if shared else 'weak' if systems else 'unresolved',
                    'shared_payoffs':sorted(shared),'shared_movement_systems':sorted(systems)},
                'scope':'potential contestation / exclusivity within sampled evidence only; no actual combat or universal access proof'}
            opposing.append(p);a['opponent_relationships'].append(p);b['opponent_relationships'].append(p)
    for team in sets:
        sets[team]['credible_eventual_contestation']=any(p['opportunity_convergence']['shared_payoffs'] for p in opposing)
        sets[team]['spatial_opponent_pathway']=any(p['spatial_convergence'] for p in opposing)
    compressed=any(p['spatial_convergence'] and p['spatial_convergence']['depth_a']<=by_team['north'][i//max(1,len(by_team['south']))]['handoff']['homeland_depth']
        and p['spatial_convergence']['depth_b']<=by_team['south'][i%max(1,len(by_team['south']))]['handoff']['homeland_depth'] for i,p in enumerate(opposing))
    phases={'differentiation':{t:s['diversity_state'] for t,s in sets.items()},
        'networking':{t:topo['same_team_lateral_matrices'][t] for t in sets},
        'contestation':{'opportunity_pairs':sum(bool(p['opportunity_convergence']['shared_payoffs']) for p in opposing),
            'shared_system_only_pairs':sum(bool(p['opportunity_convergence']['shared_movement_systems']) and not p['opportunity_convergence']['shared_payoffs'] for p in opposing),
            'spatial_pair_coverage':topo['convergence_pair_coverage']}}
    return {'homelands':sets,'opponent_pairs':opposing,'topology_phases':phases,
        'map_scale':{'state':'compressed topology' if compressed else 'unresolved',
            'evidence':'opening-depth opposing spatial overlap' if compressed else 'phase observations available, but payoff incompleteness, conditional water reach and no desired distance targets prevent a healthy/diffuse scale verdict',
            'deeper_space':{t:{'maximum_sampled_terrestrial_depth':max((r['reach']['deep_network_reach']['modest']['maximum_homeland_depth'] for r in rr),default=None),
                'same_team_connections_after_both_handoffs':sum(p['spatial_connection'] is not None and p['spatial_connection']['connection_homeland_depth']>
                    max(next(r['handoff']['homeland_depth'] for r in rr if r['relationship_id']==rid) for rid in p['relationships']) for p in sets[t]['same_team_pairs']),
                'resource_space':'unresolved: no resource inventory','water_vs_terrestrial_depth':'water path length is a distinct modality; no invented travel-depth conversion'} for t,rr in by_team.items()},
            'dimensions_changed':False,'no_numerical_recommendation':True}}


def analyze_shadow(candidate,fit):
    """Attach diagnostics to frozen fields; input data remains byte-equivalent."""
    model=ShadowTerrain(candidate,fit);relationships=[];sampled=[]
    for team in ('north','south'):
        for n,r in enumerate(fit['selected_handoffs'][team],1):relationships.append(model.relationship(team,r,team[0].upper()+str(n)))
        for entry in rejected_sample(fit,team):
            r=model.relationship(team,expand_pool(fit,entry['record']));r['sample_strata']=entry['strata'];sampled.append(r)
    sets=set_analysis(fit,relationships)
    result=copy.deepcopy(fit)
    for team,rows in result['selected_handoffs'].items():
        for row,relationship in zip(rows,[r for r in relationships if r['homeland']==team]):row['opportunity_relationship']=relationship
    result['opportunity_shadow']={'schema':'default_opportunity_shadow_v1','policy':POLICY,'selection_influence':False,
        'opening_opportunity_sets':sets,'rejected_sample':sampled,
        'water_network_catalog':[{k:v for k,v in w.items() if k not in ('cells','ports')}|
            {'sample_count':len(w['cells']),'shore_port_count':len(w['ports'])} for w in model.water_networks.values()],
        'structure_limitations':'Only existing village detections receive gameplay-bearing Destination evidence. Other structure boxes are not surface access/strategic relevance proof.',
        'other_structure_types':dict(Counter(s['type'] for s in candidate.get('actual_structures',[]) if not s.get('type','').startswith('minecraft:village')))}
    return result


def strip_shadow(fit):
    result=copy.deepcopy(fit);result.pop('opportunity_shadow',None)
    for rows in result['selected_handoffs'].values():
        for r in rows:r.pop('opportunity_relationship',None)
    return result
