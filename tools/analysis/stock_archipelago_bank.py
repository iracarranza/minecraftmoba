"""Resume the 175-block seed bank with exact-count acceleration and stage evidence."""
import sys,json,argparse,time
from pathlib import Path
from contextlib import ExitStack
from unittest.mock import patch
from exact_section_histogram import exact_counts,validate
import search_archipelago_layouts as joint
from terrain_harvest import opportunity_map

def main():
 p=argparse.ArgumentParser();p.add_argument('--seeds',nargs='+',type=int,required=True);p.add_argument('--budget',type=int,default=12);p.add_argument('--wide-core',action='store_true');p.add_argument('--reserve-lair',action='store_true');p.add_argument('--island-chains',action='store_true');p.add_argument('--source-root',type=Path);p.add_argument('--variant-offset',type=int,default=0);p.add_argument('--shape-prospects',type=Path);a=p.parse_args()
 shapes={r['seed']:r for r in json.loads(a.shape_prospects.read_text())} if a.shape_prospects else {}
 proof=joint.OUT/'exact-count-validation.json'
 if not proof.exists():validate(joint.ROOT)
 passed=[]
 with ExitStack() as stack:
  stack.enter_context(patch.object(opportunity_map,'_section_counts',new=exact_counts))
  for name in ['fit','objectives','lair','characterize']:
   original=getattr(joint.mc,name)
   def progress(*args,_fn=original,_name=name,**kw):
    first=args[0];seed=first.get('seed') if isinstance(first,dict) else first.seed
    print('stage',seed,_name,'start',flush=True);started=time.perf_counter();value=_fn(*args,**kw)
    print('stage',seed,_name,'done',round(time.perf_counter()-started,2),flush=True);return value
   stack.enter_context(patch.object(joint.mc,name,side_effect=progress))
  for seed in a.seeds:
   if a.source_root:joint.OLD=a.source_root
   elif (joint.OUT/'generated'/str(seed)/'result.json').exists():joint.OLD=joint.OUT/'generated'
   else:joint.OLD=joint.ROOT/'implementation/worldgen/reports/archipelago_gameplay_2026-10-12'
   shape=shapes.get(seed);home_targets=[[v[0]+shape['centre'][0]-1024,v[1]+shape['centre'][1]-1024] for v in shape['home_relative_xz']] if shape else None
   for rotation in [0,90,180,270]:
    if not (joint.OLD/str(seed)/f'candidate-r{rotation}.json').exists():continue
    result=joint.run(seed,rotation,a.budget,core_mode='wide-islands' if a.island_chains else 'wide-reserved' if a.reserve_lair else 'wide' if a.wide_core else 'legacy',variant_offset=a.variant_offset,home_world_targets=home_targets)
    if result:passed.append({'seed':seed,'source':str(joint.OUT/f'{seed}-r{rotation}-p{result["core_pair_variant"]}.json')});break
 joint.save(joint.OUT/('stock-batch-'+ '-'.join(map(str,a.seeds))+('-crop' if a.source_root else '-islands' if a.island_chains else '-reserved' if a.reserve_lair else '-wide' if a.wide_core else '')+'.json'),{'status':'completed','seeds':a.seeds,'passing':passed,'minimum_spacing':175,'all_shared_gates_preserved':True,'exact_counts_validation':str(proof),'production_ready':False})
if __name__=='__main__':main()
