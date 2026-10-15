"""Read-only vanilla qualification with user-required 175-block anchor spacing."""
import json,math,itertools,sys,copy
from pathlib import Path
from unittest.mock import patch
from contextlib import ExitStack
import numpy as np
from filter_pale_gameplay import ROOT,JAR,JAVA,RUNTIME,acquire,extract_region,evaluate_orientation,enrich,_public,VERSION,DATA_VERSION,GENERATOR,SERVER_SHA1,save,mc,scoop,map_types
from terrain_harvest import lair_socket,objective_forms
from vanilla_search import structures
from vanilla_search.task_a import Terrain
OUT=ROOT/'implementation/worldgen/reports/archipelago_gameplay_2026-10-12';OUT.mkdir(parents=True,exist_ok=True)
ART=ROOT/'artifacts/worldgen/archipelago_gameplay_2026-10-12'
BOUNDS=(-64,63,-64,63)
orig_order=mc.order_constrained;orig_evaluate=structures.evaluate;orig_lair=lair_socket.search

def spacing_order(pools):
 chain=[k for k in objective_forms.DEFENSIVE if k in pools];best=None;score=-1
 for combo in itertools.product(*(pools[k] for k in chain)):
  vals=[q['advance'] for q in combo]
  if any(v<=0 for v in vals) or any(a<=b for a,b in zip(vals,vals[1:])):continue
  pts=[(q['world_xyz'][0],q['world_xyz'][2]) for q in combo]
  if any(math.dist(a,b)<175 for a,b in itertools.combinations(pts,2)) or mc._overlaps(chain,list(combo)):continue
  quality=sum(q.get('quality') or 0 for q in combo)
  if quality>score:best=dict(zip(chain,combo));score=quality
 return best

def evaluate_spaced(candidate,homes,**kw):
 kw['per_layer']=64;d=orig_evaluate(candidate,homes,**kw);t=Terrain(candidate)
 for team,layers in d['teams'].items():
  x,z=homes[team];f=t.coords[z*t.w+x]
  for kind,entry in layers.items():
   if kind not in objective_forms.DEFENSIVE:continue
   entry['candidates']=[q for q in entry.get('candidates',[]) if q.get('world_xyz') and math.dist(f,(q['world_xyz'][0],q['world_xyz'][2]))>=175]
 return d

def qualify(seed,resite=False):
 folder=OUT/str(seed);folder.mkdir(exist_ok=True);root=ART/str(seed)/'server';surface=folder/'surface.json'
 if surface.exists():extracted=json.loads(surface.read_text());world=root/'world'
 else:
  acquire.verify_server_jar(JAR)
  world,timing=acquire.generate_world(seed,JAR,JAVA,root,RUNTIME,BOUNDS,heap='2G',progress=lambda n,total:print('generate',seed,n,total,flush=True) if n%5==0 or n==total else None)
  save(folder/'generation.json',timing);print('extract',seed,flush=True);extracted=extract_region(world,BOUNDS,ground_scan=True);save(surface,extracted)
 runs=[]
 prefix='resite-' if resite else ''
 for rotation in [0,90,180,270]:
  c=evaluate_orientation(extracted,rotation,False,fit_routes=False)
  if c is None:runs.append({'rotation':rotation,'status':'reject','reason':'NO_HOME_SITES'});continue
  enrich(c);c.update(seed=seed,schema_version=1,generator=GENERATOR,minecraft_java_version=VERSION,data_version=DATA_VERSION,region={'chunk_bounds':list(BOUNDS),'block_bounds':[-1024,1023,-1024,1023]},actual_structures=extracted['structures'],actual_block_volume_presence=extracted['block_volume_presence'],data_completeness={'full_chunks':extracted['full_chunks'],'expected_chunks':extracted['expected_chunks']},local_world_directory=str(world),prospect_types=['archipelago'],worldgen_truth={'source':'official Mojang dedicated server','server_jar_sha1':SERVER_SHA1,'verified':True,'terrain_modified':False})
  c=_public(c);save(folder/f'{prefix}candidate-r{rotation}.json',c);t=Terrain(c)
  sym=scoop.symmetry([max(63,h) for h in t.v['height']],t.w,t.h,'z',spacing_hint=8,contested=[not q for q in t.v['water']])
  if sym.get('deviation_over_relief',math.inf)>map_types.symmetry_bound()['value']:
   runs.append({'rotation':rotation,'status':'reject','reason':'GENERATED_SYMMETRY_OUTSIDE_BOUND','symmetry':sym});continue
  pair=None
  if resite:
   from rebalance_pale_homebases import slots,picker
   from vanilla_search import task_a
   ns=slots(t,'north')[0];ss=slots(t,'south')[0]
   if not ns or not ss:
    runs.append({'rotation':rotation,'status':'reject','reason':'NO_ALTERNATIVE_CORE_PAIR','symmetry':sym});continue
   n,s=max(itertools.product(ns,ss),key=lambda q:math.dist((q[0]['fountain']['world_xyz'][0],q[0]['fountain']['world_xyz'][2]),(q[1]['fountain']['world_xyz'][0],q[1]['fountain']['world_xyz'][2])))
   pair={'north':n,'south':s}
  # Lair siting follows defensive siting. Keep a larger pool and filter it against ALL anchors.
  selected_objectives={}
  original_objectives=mc.objectives
  def objectives(c,result,out):
   original_objectives(c,result,out);selected_objectives.clear();selected_objectives.update(copy.deepcopy(out.evidence.get('objective_world_xz',{})))
  def safe_lair(c,fountains,**kw):
   found=orig_lair(c,fountains,keep=10000,**kw);tt=Terrain(c)
   anchors=[(kind,pos) for objs in selected_objectives.values() for kind,pos in objs.items()]
   for x,z in fountains.values():anchors.append(('aether_fountain',list(tt.coords[z*tt.w+x])))
   # 175 centre spacing also guarantees separation from a 48-block encounter half-span plus authored footprints.
   sites=[q for q in found['sites'] if all(math.dist(q['world_xz'],pos)>=max(175,48+mc._footprint_radius(kind)) for kind,pos in anchors)]
   found['sites']=sites;found['selected']=sites[0] if sites else None;found['parameters']['min_landmark_spacing_blocks']=175;return found
  with ExitStack() as stack:
   if pair:stack.enter_context(patch.object(task_a,'_choose_home',side_effect=picker(pair)))
   stack.enter_context(patch.object(mc,'order_constrained',side_effect=spacing_order));stack.enter_context(patch.object(structures,'evaluate',side_effect=evaluate_spaced));stack.enter_context(patch.object(mc,'objectives',side_effect=objectives));stack.enter_context(patch.object(lair_socket,'search',side_effect=safe_lair))
   out=mc.compile_candidate(c,world)
  d=out.as_dict();d.update(rotation=rotation,symmetry=sym,minimum_center_distance_blocks=175,ready=False,playable=False,qualification_only=True,status='shared_checks_pass' if not out.rejections else 'reject')
  # Independently verify the spacing in the final selected evidence.
  if not out.rejections:
   e=d['evidence'];audit={}
   for team,objs in e['objective_world_xz'].items():
    pts=list(objs.values())+[[e['fountains'][team][0],e['fountains'][team][2]]];audit[team]=min(math.dist(a,b) for a,b in itertools.combinations(pts,2));assert audit[team]>=175
   audit['lair_minimum']=min(math.dist(e['lair_site']['world_xz'],p) for objs in e['objective_world_xz'].values() for p in objs.values());assert audit['lair_minimum']>=175;d['spacing_audit']=audit
  save(folder/f'{prefix}compilation-r{rotation}.json',d);runs.append(d);print('qualify',seed,rotation,d['status'],[q.code for q in out.rejections],flush=True)
 save(folder/f'{prefix}result.json',{'seed':seed,'runs':runs,'world':str(world),'bounds':[-1024,1023,-1024,1023]});return seed

def main():
 # First-stage symmetry is approximate; held candidates remain available for shifted-window searches.
 bank=json.loads((ROOT/'implementation/worldgen/reports/archipelago_search_2026-10-12/summary.json').read_text())['candidates'];screen=[];queue=[]
 for r in bank[:6]:
  with (ROOT/f'artifacts/worldgen/archipelago_search_2026-10-12/{r["seed"]}-16.bin').open('rb') as f:f.readline();a=np.frombuffer(f.read(),np.int16).reshape(128,128,2)
  land=~np.isin(a[:,:,1],[0,10,24,44,45,46,47,48,49,50]);values=[scoop.symmetry(np.maximum(a[:,:,0],63).ravel().tolist(),128,128,axis,spacing_hint=16,contested=land.ravel().tolist())['deviation_over_relief'] for axis in ['z','x']]
  eligible=min(values)<=map_types.symmetry_bound()['value'];screen.append({'seed':r['seed'],'approximate_symmetry':values,'generate':eligible})
  if eligible:queue.append(r['seed'])
 save(OUT/'screen.json',screen)
 for seed in queue:qualify(seed)
 results=[json.loads((OUT/str(seed)/'result.json').read_text()) for seed in queue]
 save(OUT/'summary.json',{'status':'Completed bounded read-only qualification','min_distance':175,'distance_definition':'horizontal centre-to-centre; every same-team F/O/B/S pair and every Fountain/defensive objective to Lair','screen':screen,'results':results,'production_ready':False,'limitations':['Actual boss volume/runtime certification remains unverified; surface proxies only.','Archipelago navigation uses existing terrain traversal cost, not a calibrated boat/logistics model.','Remaining canonically Open exclusions and joint territorial acceptance thresholds cannot be certified.','Held or rejected origin windows do not prove an entire seed impossible.']})
if __name__=='__main__':main()
