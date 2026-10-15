"""Deeper joint placement search; no shared thresholds relaxed or terrain edits."""
import sys,json,math,itertools,copy,hashlib,argparse,time,weakref
from pathlib import Path
from contextlib import ExitStack
from unittest.mock import patch
import numpy as np
from filter_pale_gameplay import ROOT,mc,save,scoop,map_types
from rebalance_pale_homebases import slots,picker
from vanilla_search import task_a,structures
from vanilla_search.task_a import Terrain,shortest
from terrain_harvest import objective_forms,column_scan,lair_socket,characterize,opening_ceiling
OLD=ROOT/'implementation/worldgen/reports/archipelago_gameplay_2026-10-12'
OUT=ROOT/'implementation/worldgen/reports/archipelago_qualified_search_2026-10-12';OUT.mkdir(parents=True,exist_ok=True)
orig_lair=lair_socket.search
orig_viability=lair_socket.viability
orig_pad=structures.pad_quality

def order(pools):
 chain=list(objective_forms.DEFENSIVE)
 if any(not pools.get(k) for k in chain):return None
 arr=[np.array([[q['world_xyz'][0],q['world_xyz'][2]] for q in pools[k]]) for k in chain]
 adv=[np.array([q['advance'] for q in pools[k]]) for k in chain];quality=[np.array([q['quality'] for q in pools[k]]) for k in chain]
 def far(a,b):return np.sum((a[:,None,:]-b[None,:,:])**2,axis=2)>=175**2
 ab=far(arr[0],arr[1])&(adv[0][:,None]>adv[1][None,:]);bc=far(arr[1],arr[2])&(adv[1][:,None]>adv[2][None,:]);ac=far(arr[0],arr[2])
 if all(all('island_id' in q for q in pools[k]) for k in chain):
  ids=[np.array([q['island_id'] for q in pools[k]]) for k in chain]
  ab &= ids[0][:,None]!=ids[1][None,:];bc &= ids[1][:,None]!=ids[2][None,:];ac &= ids[0][:,None]!=ids[2][None,:]
 valid=ab[:,:,None]&bc[None,:,:]&ac[:,None,:]&(adv[2][None,None,:]>0)
 if not valid.any():return None
 scores=quality[0][:,None,None]+quality[1][None,:,None]+quality[2][None,None,:];idx=np.unravel_index(np.argmax(np.where(valid,scores,-np.inf)),valid.shape)
 combo=[pools[k][j] for k,j in zip(chain,idx)];assert not mc._overlaps(chain,combo)
 return dict(zip(chain,combo))

def run(seed,rotation,budget=8,core_mode="legacy",variant_offset=0,home_world_targets=None):
 folder=OLD/str(seed);base=json.loads((folder/f'candidate-r{rotation}.json').read_text());world=Path(json.loads((folder/'result.json').read_text())['world']);t=Terrain(base)
 sym=scoop.symmetry([max(63,h) for h in t.v['height']],t.w,t.h,'z',spacing_hint=8,contested=[not w for w in t.v['water']])
 from vanilla_search.evaluate import _components
 bodies=_components([not w for w in t.v['water']],t.w,t.h)
 archipelago_features={'water':sum(t.v['water'])/t.n,'land_bodies':len(bodies)}
 if not map_types.predicates()['archipelago'](archipelago_features):
  print('actual type reject',seed,rotation,archipelago_features,flush=True);return
 if sym['deviation_over_relief']>map_types.symmetry_bound()['value']:return
 from refine_pale_mansion_search import label
 geographic_land=np.array([not water or 'river' in biome for water,biome in zip(t.v['water'],t.v['biome'])]).reshape(t.h,t.w);labels,_=label(geographic_land);island_sizes=np.bincount(labels.ravel());ranked_islands=sorted(range(1,len(island_sizes)),key=lambda i:-island_sizes[i]);largest=set(ranked_islands[:2]);large={i for i in ranked_islands if island_sizes[i]>=.5*island_sizes[ranked_islands[1]]}
 def island_at(pos):
  i=min(range(t.n),key=lambda i:(t.coords[i][0]-pos[0])**2+(t.coords[i][1]-pos[1])**2)
  return int(labels.ravel()[i])
 if home_world_targets:
  # Map approximate endpoint centroids to nearest actual substantial dry island.
  dry_large=[i for i in range(t.n) if not t.v['water'][i] and int(labels.ravel()[i]) in large]
  targets={int(labels.ravel()[min(dry_large,key=lambda i:math.dist(t.coords[i],pos))]) for pos in home_world_targets}
  if len(targets)!=2:
   print('shape endpoint islands unresolved',seed,rotation,sorted(targets),flush=True);return
  large=targets
 pad_cache={};viability_cache={};checked_terrain=weakref.WeakKeyDictionary()
 def cached_pad(terrain,centre,radius):
  if terrain is not t:return orig_pad(terrain,centre,radius)
  key=(centre,radius)
  if key not in pad_cache:pad_cache[key]=orig_pad(terrain,centre,radius)
  return pad_cache[key]
 def cached_viability(terrain,centre,radius,min_approaches,min_usable,min_open):
  if terrain not in checked_terrain:checked_terrain[terrain]=(terrain.w==t.w and terrain.h==t.h and terrain.coords==t.coords and terrain.v==t.v and terrain.p==t.p)
  if not checked_terrain[terrain]:return orig_viability(terrain,centre,radius,min_approaches,min_usable,min_open)
  key=(centre,radius,min_approaches,min_usable,min_open)
  if key not in viability_cache:viability_cache[key]=tuple(orig_viability(terrain,centre,radius,min_approaches,min_usable,min_open))
  return list(viability_cache[key])
 reader=column_scan.World(world);req={k:objective_forms.site_requirements(k) for k in (*objective_forms.DEFENSIVE,'aether_fountain')};cache={}
 def physical(kind,pos):
  key=(kind,*pos)
  if key not in cache:cache[key]=column_scan.verify_site(reader,*pos,req[kind],max_moved_per_column=mc.PROVISIONAL['max_levelling_moved_per_column'])
  return cache[key]['ok']
 if core_mode.startswith('wide'):
  from examine_scoop_axes import socket
  def wider_slots(team):
   lo,hi=((.05,.48) if team=='north' else (.52,.95)) if core_mode=='wide-islands' else ((.05,.4) if team=='north' else (.6,.95));options=[]
   for z in range(max(5,int(t.h*lo)),min(t.h-5,int(t.h*hi))):
    for x in range(max(5,int(t.w*.1)),min(t.w-5,int(t.w*.9))):
     i=z*t.w+x;q=socket(t,i)
     if home_world_targets and q is None and not t.v['water'][i]:
      cells=t.cells(i);heights=[t.v['height'][j] for j in cells];usable=sum(t.v['buildable'][j] for j in cells)/len(cells);canopy=sum(t.v['canopy'][j] for j in cells)/len(cells);water=sum(t.v['water'][j] for j in cells)/len(cells);quality=usable-.01*(max(heights)-min(heights))-.3*canopy-water
      # The shared fitter chooses a valid Fountain within the footprint; its center need not be that Fountain.
      if quality>=.52 and any(not t.v['water'][j] and t.v['buildable'][j] and not t.v['canopy'][j] and t.grade[j]<=t.p['fountain_max_grade_y'] for j in cells):q={'quality':round(quality,4),'sample':[x,z],'diagnostics':{'usable_fraction':usable}}
     if q and (not home_world_targets or int(labels.ravel()[i]) in large):options.append({'index':i,'sample':[x,z],**q})
   options.sort(key=lambda q:q['quality'],reverse=True);chosen=[]
   for q in options:
    if any(math.dist(t.coords[q['index']],t.coords[p['index']])<(48 if core_mode=='wide-islands' else 64) for p in chosen):continue
    if core_mode=='wide-islands' and sum(int(labels.ravel()[p['index']])==int(labels.ravel()[q['index']]) for p in chosen)>=24:continue
    chosen.append(q)
    if len(chosen)>=(72 if core_mode=='wide-islands' else 24):break
   return chosen
  sets={team:wider_slots(team) for team in ('north','south')}
 else:sets={team:slots(t,team)[0] for team in ('north','south')}
 print('Core census',seed,rotation,core_mode,{k:len(v) for k,v in sets.items()},flush=True)
 if any(not v for v in sets.values()):print('no Core pair',seed,rotation,flush=True);return
 # Use actual fitted Fountain positions, reject bad source columns before full compilation.
 actual={}
 for team in sets:
  viable=[]
  for s in sets[team]:
   with patch.object(task_a,'_choose_home',side_effect=picker({'north':s,'south':s})):h=task_a.fit_homeland(t,team)
   if not h or not h[0].get('fountain') or h[0]['quality']<.52:continue
   xyz=h[0]['fountain']['world_xyz'];xz=[xyz[0],xyz[2]]
   if home_world_targets and island_at(xz) not in large:continue
   if not physical('aether_fountain',xz):continue
   # Opening built structures are a genuine gate; use the same implementation.
   if opening_ceiling.survey(reader,xyz[0],xyz[2]):continue
   viable.append(s);actual[team,s['index']]=h[0]
  sets[team]=viable;print('Core physical',seed,rotation,team,len(viable),flush=True)
 if any(not v for v in sets.values()):print('no physically valid Core pair',seed,rotation,flush=True);return
 pairs=list(itertools.product(sets['north'],sets['south']))
 coverage={}
 if core_mode=='wide-islands':
  # Exact cheap pad predicates and complete reach fields rank Core pairs by
  # possible island spread before Homebase quality. Physical checks follow.
  pad_masks={}
  for radius in [1,2]:
   mask=np.array(t.v['water']).reshape(t.h,t.w)==False;mask[:radius,:]=False;mask[-radius:,:]=False;mask[:,:radius]=False;mask[:,-radius:]=False;pad_masks[radius]=mask.ravel()
  points=np.array(t.coords);fields={};anchors={}
  for team,sites in sets.items():
   for site in sites:
    xyz=actual[team,site['index']]['fountain']['world_xyz'];pos=np.array([xyz[0],xyz[2]]);idx=int(np.argmin(np.sum((points-pos)**2,axis=1)));costs=shortest(t.adj,{idx:0.})[0]
    fields[team,site['index']]=(idx,np.array([costs.get(i,math.inf) for i in range(t.n)]));anchors[team,site['index']]=pos
  possible=[]
  for n,s in pairs:
   ni=int(labels.ravel()[n['index']]);si=int(labels.ravel()[s['index']])
   if ni==si or ni not in large or si not in large:continue
   evidence={}
   for team,site,rival,rival_site in [('north',n,'south',s),('south',s,'north',n)]:
    idx,a=fields[team,site['index']];_,b=fields[rival,rival_site['index']];D=b[idx];own=(a>0)&(a<=b)&(b<=D)&(np.sum((points-anchors[team,site['index']])**2,axis=1)>=175**2);corridor=(a+b)<=1.35*D
    O=set(np.unique(labels.ravel()[own&corridor&pad_masks[1]]).tolist());B=set(np.unique(labels.ravel()[own&corridor&pad_masks[2]]).tolist());S=set(np.unique(labels.ravel()[own&pad_masks[1]]).tolist());evidence[team]={'outpost_islands':sorted(O),'bastion_islands':sorted(B),'spike_islands':sorted(S),'all':len(O|B|S),'forward':len(O|B)}
   coverage[n['index'],s['index']]=evidence
   if min(q['all'] for q in evidence.values())<3 or min(q['forward'] for q in evidence.values())<2 or any(not q['outpost_islands'] or not q['bastion_islands'] or not q['spike_islands'] for q in evidence.values()):continue
   possible.append((n,s))
  pairs=possible
  pairs.sort(key=lambda p:(-min(min(q['forward'],6) for q in coverage[p[0]['index'],p[1]['index']].values()),-min(min(q['all'],6) for q in coverage[p[0]['index'],p[1]['index']].values()),-min(actual['north',p[0]['index']]['quality'],actual['south',p[1]['index']]['quality']),math.dist(anchors['north',p[0]['index']],anchors['south',p[1]['index']])))
  print('Core pairs with possible island spread',seed,rotation,len(pairs),flush=True)
 else:
  pairs.sort(key=lambda p:(-min(actual['north',p[0]['index']]['quality'],actual['south',p[1]['index']]['quality']),-math.dist((p[0]['fountain']['world_xyz'][0],p[0]['fountain']['world_xyz'][2]),(p[1]['fountain']['world_xyz'][0],p[1]['fountain']['world_xyz'][2]))))
 selected=[]
 for n,s in pairs:
  if core_mode=='wide-islands':
   ni=int(labels.ravel()[n['index']]);si=int(labels.ravel()[s['index']])
   if ni==si or ni not in large or si not in large:continue
  if any(math.dist(t.coords[n['index']],t.coords[q['north']['index']])<64 and math.dist(t.coords[s['index']],t.coords[q['south']['index']])<64 for q in selected):continue
  selected.append({'north':n,'south':s,**({'island_coverage_upper_bound':coverage[n['index'],s['index']]} if core_mode=='wide-islands' else {})})
  if len(selected)>=budget:break
 save(OUT/f'{seed}-r{rotation}-core-pairs-{core_mode}-v6.json',selected);measured=None
 for ordinal,pair in enumerate(selected,1):
  pi=variant_offset+ordinal+(1000 if core_mode=='wide-islands' else 300 if core_mode=='wide-reserved' else 100 if core_mode=='wide' else 0)
  dest=OUT/f'{seed}-r{rotation}-p{pi}.json'
  if dest.exists():
   d=json.loads(dest.read_text())
   if d['status']=='shared_checks_pass':return d
   continue
  selected_objs={};poolstats={};allcosts={};reserved={};occupied=[];pool_islands={}
  def evaluate(candidate,homes,**kw):
   indices={team:xz[1]*t.w+xz[0] for team,xz in homes.items()};allcosts.update({team:shortest(t.adj,{h:0.})[0] for team,h in indices.items()});teams={}
   occupied.extend([list(t.coords[h]) for h in indices.values()])
   if core_mode in ('wide-reserved','wide-islands'):
    found=orig_lair(candidate,homes,keep=t.n)
    sites=[q for q in found['sites'] if q['access_asymmetry']<=.15 and all(math.dist(q['world_xz'],p)>=175 for p in occupied)]
    if core_mode=='wide-islands':
     sites.sort(key=lambda q:(max(math.dist(q['world_xz'],p) for p in occupied),q['access_asymmetry']))
     found['sites']=sites
    if sites:reserved.update(found=found,site=sites[0]);occupied.append(sites[0]['world_xz'])
   for team,h in indices.items():
    rival='south' if team=='north' else 'north'
    corridor={i for i in range(t.n) if allcosts[team].get(i,math.inf)+allcosts[rival].get(i,math.inf)<=1.35*allcosts[rival].get(h,math.inf)} if core_mode=='wide-islands' else structures.corridor_cells(t,h,indices[rival],6)
    pools,_=structures.pools_for_team(t,h,t.n,allcosts[rival],corridor)
    entries={}
    for kind in objective_forms.DEFENSIVE:
     candidates=[];seen=[];source=0;bins={};island_counts={}
     for q in pools[kind]:
      if q['advance']<=0:continue
      band=min(9,int(q['advance']*20))
      island_id=int(labels.ravel()[q['centre_index']])
      if core_mode=='wide-islands':
       band=(band,island_id)
       if island_counts.get(island_id,0)>=16:continue
      if bins.get(band,0)>=(4 if core_mode=='wide-islands' else 8):continue
      pos=[q['world_xyz'][0],q['world_xyz'][2]]
      if math.dist(pos,t.coords[h])<175:continue
      if core_mode in ('wide-reserved','wide-islands') and any(math.dist(pos,p)<175 for p in occupied):continue
      pad=q['pad']
      if core_mode=='wide-islands':
       if t.v['water'][q['centre_index']]:continue
      elif pad['water_fraction'] or pad['buildable_fraction']<.4 or pad['relief_y']>12:continue
      if any(math.dist(pos,old)<24 for old in seen):continue
      seen.append(pos);source+=1
      if core_mode=='wide-islands':q=dict(q,island_id=int(labels.ravel()[q['centre_index']]))
      candidates.append(q);bins[band]=bins.get(band,0)+1;island_counts[island_id]=island_counts.get(island_id,0)+1
      if len(candidates)>=80:break
      if source>=500:break
     entries[kind]={'candidates':candidates,'refusal':'No candidate survives full spacing and physical fit in retained terrain-compatible pool'}
     poolstats[team,kind]=(len(pools[kind]),source,len(candidates))
     pool_islands[team,kind]=dict(island_counts)
    # Test complete spaced layouts first; inspect only the pads they use.
    attempts=0
    while attempts<160:
     chosen=order({k:v['candidates'] for k,v in entries.items()})
     if chosen is None:break
     bad=[]
     for kind,q in chosen.items():
      if not physical(kind,[q['world_xyz'][0],q['world_xyz'][2]]):bad.append((kind,q))
     if not bad:
      entries={k:{'candidates':[q]} for k,q in chosen.items()}
      if core_mode in ('wide-reserved','wide-islands'):occupied.extend([[q['world_xyz'][0],q['world_xyz'][2]] for q in chosen.values()])
      break
     for kind,q in bad:entries[kind]['candidates'].remove(q)
     attempts+=1
    teams[team]=entries
   if core_mode=='wide-islands':print('island pool coverage',seed,rotation,pi,{f'{k[0]}/{k[1]}':v for k,v in pool_islands.items()},flush=True)
   print('objective pools',seed,rotation,pi,{f'{k[0]}/{k[1]}':v[2] for k,v in poolstats.items()},flush=True)
   return {'teams':teams}
  orig_obj=mc.objectives
  def objectives(c,result,out):
   orig_obj(c,result,out);selected_objs.update(copy.deepcopy(out.evidence.get('objective_world_xz',{})))
   if not out.rejections:
    points=[p for objs in selected_objs.values() for p in objs.values()]+[[v[0],v[2]] for v in out.evidence['fountains'].values()]
    minimum=min(math.dist(a,b) for a,b in itertools.combinations(points,2))
    if minimum<175:out.fail('objectives','GLOBAL_LANDMARK_MIN_SPACING','Any opposing or same-team major anchors must also meet the user-required 175-block distance',minimum_distance=minimum,bound=175)
  def lair(c,homes,**kw):
   # Same physical viability and access cost. All viable sites considered, then spacing applies.
   found=copy.deepcopy(reserved['found']) if reserved else orig_lair(c,homes,keep=t.n,**kw)
   anchors=[pos for objs in selected_objs.values() for pos in objs.values()]+[list(t.coords[xz[1]*t.w+xz[0]]) for xz in homes.values()]
   sites=[q for q in found['sites'] if all(math.dist(q['world_xz'],p)>=175 for p in anchors)]
   found.update(sites=sites,selected=sites[0] if sites else None);found['parameters']['min_landmark_center_distance']=175
   return found
  with ExitStack() as stack:
   stack.enter_context(patch.object(structures,'pad_quality',new=cached_pad));stack.enter_context(patch.object(lair_socket,'viability',new=cached_viability))
   stack.enter_context(patch.object(task_a,'_choose_home',side_effect=picker(pair)))
   stack.enter_context(patch.object(structures,'evaluate',side_effect=evaluate));stack.enter_context(patch.object(mc,'order_constrained',side_effect=order));stack.enter_context(patch.object(mc,'objectives',side_effect=objectives));stack.enter_context(patch.object(lair_socket,'search',side_effect=lair))
   if measured:
    def reuse(seed_arg,world_arg,bounds,**kw):
     assert seed_arg==seed and Path(world_arg).resolve()==world.resolve() and list(bounds)==base['region']['block_bounds'];return copy.deepcopy(measured)
    stack.enter_context(patch.object(characterize,'characterize',side_effect=reuse))
   out=mc.compile_candidate(base,world)
  if core_mode=='wide-islands' and not out.rejections:
   e=out.evidence;home_ids=[island_at([v[0],v[2]]) for v in e['fountains'].values()]
   if len(set(home_ids))!=2 or not set(home_ids).issubset(large):out.fail('archipelago_topology','ARCHIPELAGO_HOME_ISLANDS','Prototype search requires fitted Fountains on two distinct substantial sampled islands',island_ids=home_ids,eligible_large_islands=sorted(large))
   for team,objs in e['objective_world_xz'].items():
    ids=[island_at(v) for v in objs.values()]
    if len(set(ids))!=3 or 0 in ids:out.fail('archipelago_topology','ARCHIPELAGO_DEFENSIVE_CONCENTRATION','Prototype search requires three distinct dry surface islands per team',team=team,island_ids=ids)
  d=out.as_dict();d.update(status='shared_checks_pass' if not out.rejections else 'reject',ready=False,playable=False,qualification_only=True,rotation=rotation,core_pair_variant=pi,core_pair=pair,core_search_mode=core_mode,world=str(world),candidate_path=str(folder/f'candidate-r{rotation}.json'),symmetry=sym,minimum_distance=175,archipelago_features=archipelago_features,land_body_area_proxies=[q*t.s*t.s for q in bodies[:12]],poolstats={f'{k[0]}/{k[1]}':v for k,v in poolstats.items()},physical_site_cache_entries=len(cache),search_preserves='Existing own practical-cost half, rival-distance cap, forward corridor, defensive order and all shared gates; broader pools and Core placements only.')
  if core_mode=='wide-islands':
   d['shape_endpoint_targets']={'approximate_world_xz':home_world_targets,'actual_geographic_islands':sorted(large)} if home_world_targets else None
   d['island_pool_counts']={f'{k[0]}/{k[1]}':v for k,v in pool_islands.items()}
   d['archipelago_search_preferences']={'prototype_test':True,'island_definition':'4-neighbor geographic land with river and frozen-river water restored for grouping; physical checks retain actual water','shoreline_search':'dry anchors ranked by shared pad quality, then exact physical column checks; coarse water/buildability/relief prefilters removed','forward_corridor':'all cells within 135% of minimum Fountain-to-Fountain modeled cost; permits alternative routes','core_pair_ranking':'possible defensive island spread before Homebase quality; shorter Fountain separation breaks ties','home_islands':'two distinct substantial surface components, each at least half the area of the second-largest component','defensive_structures':'three distinct surface islands per team','lair_ranking':'minimum worst-team straight-line distance among viable sites with access disparity <=15%; not calibrated travel time'}
   if d['status']=='shared_checks_pass':
    e=d['evidence'];d['island_membership']={team:{k:island_at(v) for k,v in objs.items()} for team,objs in e['objective_world_xz'].items()}
    d['lair_fountain_distances']={team:math.dist([v[0],v[2]],e['lair_site']['world_xz']) for team,v in e['fountains'].items()}
  if out.evidence.get('characterization',{}).get('measured'):measured=copy.deepcopy(out.evidence['characterization'])
  if not out.rejections:
   e=d['evidence'];audit={};anchors=[]
   for team,objs in e['objective_world_xz'].items():
    pts=list(objs.values())+[[e['fountains'][team][0],e['fountains'][team][2]]];audit[team]=min(math.dist(a,b) for a,b in itertools.combinations(pts,2));assert audit[team]>=175;anchors+=pts
   audit['lair_minimum']=min(math.dist(e['lair_site']['world_xz'],p) for p in anchors);anchors.append(e['lair_site']['world_xz']);audit['global_minimum']=min(math.dist(a,b) for a,b in itertools.combinations(anchors,2));assert min(audit.values())>=175;d['spacing_audit']=audit
  save(dest,d);print('joint search',seed,rotation,pi,d['status'],[r.code for r in out.rejections],d.get('spacing_audit'),flush=True)
  if not out.rejections:return d
 return None

def main():
 p=argparse.ArgumentParser();p.add_argument('--seeds',nargs='+',type=int,default=[890000020,890000062,890000067]);p.add_argument('--budget',type=int,default=8);args=p.parse_args();passed=[]
 for seed in args.seeds:
  for rotation in [0,90,180,270]:
   r=run(seed,rotation,args.budget)
   if r:passed.append(r);break
 save(OUT/'summary.json',{'status':'Prototype/test shared gameplay qualification','passed':passed,'minimum_spacing':175,'compiler_sha256':hashlib.sha256((ROOT/'implementation/worldgen/terrain_harvest/map_compiler.py').read_bytes()).hexdigest(),'production_ready':False})
if __name__=='__main__':main()
