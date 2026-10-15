"""Rank existing vanilla surface geography for sequence review; no qualification claim."""
import json
import numpy as np
from pathlib import Path
from screen_sequential_archipelagos import ROOT,OUT,rank_shape
base=ROOT/'implementation/worldgen/reports';paths=list((base/'archipelago_qualified_search_2026-10-12/generated').glob('*/surface.json'))+list((base/'archipelago_gameplay_2026-10-12').glob('*/surface.json'));results=[]
for p in paths:
 ex=json.loads(p.read_text());land=np.array([[not q['actual_surface_water'] or 'river' in q['biome'] for q in row] for row in ex['rows']]);h,w=land.shape;step=ex.get('sample_spacing',8);cx=(ex['rows'][0][0]['x']+ex['rows'][0][-1]['x'])/2;cz=(ex['rows'][0][0]['z']+ex['rows'][-1][0]['z'])/2
 r=rank_shape(land,step,{'seed':int(p.parent.name),'centre':[cx,cz]})
 if r:r.update(source_surface=str(p),approximation_only=False,gameplay_qualified=False);results.append(r)
results.sort(key=lambda r:-r['score']);(OUT/'actual-geography-ranked.json').write_text(json.dumps(results,indent=2)+'\n');print([(r['seed'],round(r['score'],2)) for r in results])
