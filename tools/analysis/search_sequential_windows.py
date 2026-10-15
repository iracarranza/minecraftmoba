"""Screen every cached shifted window for chain morphology before shared gates."""
import json,subprocess,concurrent.futures
import numpy as np
from screen_sequential_archipelagos import rank_shape,OUT,ART,OCEAN,ROOT
from filter_pale_gameplay import scoop,map_types
BIN=ROOT/'artifacts/worldgen/pale_gameplay_2026-10-12/build/pale_gameplay_probe'
def scan(p):
 seed=int(p.name.split('-')[0])
 with p.open('rb') as f:f.readline();a=np.frombuffer(f.read(),np.int16).reshape(48,48,2)
 best=None
 for j in [0,8,16]:
  for i in [0,8,16]:
   g=a[j:j+32,i:i+32];land=~np.isin(g[:,:,1],OCEAN)
   if not .431<1-land.mean()<.85:continue
   row={'seed':seed,'centre':[-512+i*64,-512+j*64]};r=rank_shape(land,64,row)
   if r and (best is None or r['score']>best['score']):best=r
 return best
OUT.mkdir(parents=True,exist_ok=True)
with concurrent.futures.ThreadPoolExecutor(max_workers=2) as pool:rows=[q for q in pool.map(scan,sorted(ART.glob('*-64.bin'))) if q]
rows.sort(key=lambda r:-r['score']);(OUT/'all-window-coarse.json').write_text(json.dumps(rows,indent=2)+'\n')
print('all-window coarse candidates',len(rows),flush=True)
def refine(row):
 seed=row['seed'];cx,cz=row['centre'];p=ART/f'{seed}-sequence-{cx}-{cz}-16.bin'
 if not p.exists():subprocess.run([str(BIN),str(seed),str(cx),str(cz),'1024','16',str(p)],check=True)
 with p.open('rb') as f:f.readline();a=np.frombuffer(f.read(),np.int16).reshape(128,128,2)
 land=~np.isin(a[:,:,1],OCEAN);r=rank_shape(land,16,row)
 if not r:return
 sym={axis:scoop.symmetry(np.maximum(a[:,:,0],63).ravel().tolist(),128,128,axis,spacing_hint=16,contested=land.ravel().tolist())['deviation_over_relief'] for axis in ['z','x']}
 r.update(fine_symmetry=sym,source_probe=str(p),step=16,minimum_shared_symmetry=min(sym.values()),approximate_shared_symmetry_pass=min(sym.values())<=map_types.symmetry_bound()['value'])
 return r
with concurrent.futures.ThreadPoolExecutor(max_workers=2) as pool:fine=[q for q in pool.map(refine,rows[:225]) if q]
fine.sort(key=lambda r:-r['score']);(OUT/'all-window-fine.json').write_text(json.dumps(fine,indent=2)+'\n')
print('all-window fine',len(fine),json.dumps([(r['seed'],r['centre'],round(r['score'],2),r['approximate_shared_symmetry_pass']) for r in fine[:12]]),flush=True)
