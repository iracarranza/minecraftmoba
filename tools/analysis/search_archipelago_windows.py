"""Broader approximate archipelago windows, biased toward substantial usable islands."""
import sys,json,math,subprocess,concurrent.futures
from pathlib import Path
import numpy as np
from filter_pale_gameplay import ROOT,save,scoop,map_types
from refine_pale_mansion_search import label
OUT=ROOT/'implementation/worldgen/reports/archipelago_qualified_search_2026-10-12';ART=ROOT/'artifacts/worldgen/archipelago_qualified_search_2026-10-12/probes';ART.mkdir(parents=True,exist_ok=True)
BIN=ROOT/'artifacts/worldgen/pale_gameplay_2026-10-12/build/pale_gameplay_probe'
OCEAN=[0,10,24,44,45,46,47,48,49,50]
def search(seed):
 p=ART/f'{seed}-64.bin'
 if not p.exists():subprocess.run([str(BIN),str(seed),'0','0','1536','64',str(p)],check=True)
 with p.open('rb') as f:f.readline();a=np.frombuffer(f.read(),np.int16).reshape(48,48,2)
 best=None
 for j in [0,8,16]:
  for i in [0,8,16]:
   grid=a[j:j+32,i:i+32];land=~np.isin(grid[:,:,1],OCEAN);wet=1-land.mean()
   if not .4<=wet<=.75:continue
   labs,n=label(land);sizes=sorted(np.bincount(labs.ravel())[1:].tolist(),reverse=True);areas=[q*4096 for q in sizes]
   if len(areas)<3 or areas[1]<160000 or areas[2]<50000:continue
   hh=np.maximum(grid[:,:,0],63).ravel().tolist();ll=land.ravel().tolist();values=[]
   for axis in ['z','x']:
    sym=scoop.symmetry(hh,32,32,axis,spacing_hint=64,contested=ll);v=sym['deviation_over_relief']
    if v>map_types.symmetry_bound()['value']:continue
    # Approximate flat dry cells in both Core fitter bands; real terrain is still required.
    g=np.maximum(grid[:,:,0],63);grade=np.zeros(g.shape)
    for dz,dx in [(1,0),(-1,0),(0,1),(0,-1)]:grade=np.maximum(grade,np.abs(g-np.roll(g,(dz,dx),(0,1))))
    good=land&(grade<=8);orient=good if axis=='z' else good.T
    counts=[int(orient[2:10,6:26].sum()),int(orient[22:30,6:26].sum())]
    if min(counts)<12:continue
    score=min(counts)*.1+min(areas[1]/100000,8)+min(areas[2]/100000,4)-v*12-abs(wet-.58)*5
    r={'seed':seed,'centre':[-512+i*64,-512+j*64],'size':[2048,2048],'axis':axis,'approximate_symmetry':v,'ocean_fraction':float(wet),'island_areas_proxy':areas[:8],'coarse_flat_land_counts':counts,'score':score,'step':64}
    if best is None or score>best['score']:best=r
 return best
if __name__=='__main__':
 rows=[]
 with concurrent.futures.ThreadPoolExecutor(max_workers=2) as pool:
  for k,r in enumerate(pool.map(search,range(890001001,890002001)),1):
   if r:rows.append(r)
   if k%100==0:print('broader screen',k,'eligible',len(rows),flush=True);save(OUT/'broad-screen.json',sorted(rows,key=lambda q:q['score'],reverse=True))
 rows.sort(key=lambda q:q['score'],reverse=True);save(OUT/'broad-screen.json',rows);print('top',json.dumps(rows[:8]),flush=True)
