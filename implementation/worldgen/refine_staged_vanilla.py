#!/usr/bin/env python3
"""Cheap temperate-interior refinement after first-batch visual feedback."""
import json
import subprocess
import time
from vanilla_search.staged import ROOT,native,save,select

out=ROOT/'implementation/worldgen/results/staged_default_2026-09-09'
build=ROOT/'artifacts/worldgen/staged_default_2026-09-09/build'
binary=native(build)
all_records=[json.loads(l) for l in (out/'structural_survivors.jsonl').read_text().splitlines()]
records=sorted(all_records,key=lambda r:r['score'],reverse=True)[:5000]
query=''.join(f"{r['seed']} {r['center'][0]} {r['center'][1]} {r['rotation']}\n" for r in records)
start=time.perf_counter()
values=subprocess.run([str(binary),'--refine'],input=query,text=True,capture_output=True,check=True).stdout.splitlines()
elapsed=time.perf_counter()-start
assert len(values)==len(records)
for r,value in zip(records,values):
    temp,center,snowplain,peak=map(float,value.split());eo,wh,cold,op,forest,hn,hs,_=r['features']
    balance=min(1,eo/.25,wh/.2,cold/.06,op/.18,forest/.08,hn/.6,hs/.6)
    r['refinement']={'temperate_open':temp,'center_temperate_open':center,'snowy_plains':snowplain,'western_peak_slope_biomes':peak}
    r['refined_score']=round(40*balance+r['score']+45*center+15*temp+15*peak-30*snowplain,4)
ranked=sorted(records,key=lambda r:r['refined_score'],reverse=True)
used={r['seed'] for r in select(all_records,8)}
pool=[r for r in ranked[:60] if r['seed'] not in used]
chosen=pool[:1]
while len(chosen)<3:
    remaining=[r for r in pool if all(r['seed']!=q['seed'] for q in chosen)]
    def diversity(r):
        return min(sum((a-b)**2 for a,b in zip(r['features'],q['features']))**.5 for q in chosen)+r['refined_score']/300
    chosen.append(max(remaining,key=diversity))
save(out/'supplemental_queue.json',chosen)
save(out/'temperate_refinement.json',{'input_windows':len(records),'seconds':elapsed,'windows_per_second':len(records)/elapsed,'method':'same native biome samples; distinguish temperate open interior from snowy plains; soft ranking, not new hard contract','selected':chosen,'top_60':ranked[:60]})
print(json.dumps({'seconds':elapsed,'selected':chosen},indent=2))
