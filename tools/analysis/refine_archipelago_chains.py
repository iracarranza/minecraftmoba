"""Approximate additional two-large-island prospects; never certifies gameplay."""
import json,subprocess,concurrent.futures
from pathlib import Path
import numpy as np
from filter_pale_gameplay import ROOT,save,scoop,map_types
from refine_pale_mansion_search import label
OUT=ROOT/'implementation/worldgen/reports/archipelago_qualified_search_2026-10-12'
ART=ROOT/'artifacts/worldgen/archipelago_qualified_search_2026-10-12/probes'
BIN=ROOT/'artifacts/worldgen/pale_gameplay_2026-10-12/build/pale_gameplay_probe'
OCEAN=[0,10,24,44,45,46,47,48,49,50]
def refine(row):
 seed=row['seed'];cx,cz=row['centre'];p=ART/f'{seed}-chain-16.bin'
 if not p.exists():subprocess.run([str(BIN),str(seed),str(cx),str(cz),'1024','16',str(p)],check=True)
 with p.open('rb') as f:f.readline();a=np.frombuffer(f.read(),np.int16).reshape(128,128,2)
 land=~np.isin(a[:,:,1],OCEAN);labs,n=label(land);areas=sorted((np.bincount(labs.ravel())[1:]*256).tolist(),reverse=True)
 sym={axis:scoop.symmetry(np.maximum(a[:,:,0],63).ravel().tolist(),128,128,axis,spacing_hint=16,contested=land.ravel().tolist())['deviation_over_relief'] for axis in ['z','x']}
 if min(sym.values())>map_types.symmetry_bound()['value'] or len(areas)<5 or areas[1]<160000:return
 return dict(row,step=16,fine_symmetry=sym,best_fine_symmetry=min(sym.values()),fine_island_areas_proxy=areas[:20],fine_islands=n,approximation_only=True)
if __name__=='__main__':
 existing=json.loads((OUT/'broad-refined.json').read_text());known={r['seed'] for r in existing}
 rows=[r for r in json.loads((OUT/'broad-screen.json').read_text()) if r['seed'] not in known]
 rows.sort(key=lambda r:(-min(len(r['island_areas_proxy']),8),-r['score']))
 with concurrent.futures.ThreadPoolExecutor(max_workers=2) as pool:results=[r for r in pool.map(refine,rows[:40]) if r]
 results.sort(key=lambda r:(r['best_fine_symmetry'],-r['fine_islands']))
 save(OUT/'chain-refined.json',results);save(OUT/'broad-refined.json',existing+results)
 print(json.dumps([{k:r[k] for k in ['seed','centre','best_fine_symmetry','fine_islands','fine_island_areas_proxy']} for r in results],indent=2))
