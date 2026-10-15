"""Independent sampled geographic grouping and spacing audit of a saved bank."""
import argparse,json,itertools,math
from pathlib import Path
import numpy as np
from refine_pale_mansion_search import label
p=argparse.ArgumentParser();p.add_argument('report',type=Path);a=p.parse_args();bank=json.loads((a.report/'qualified-summary.json').read_text());results=[]
for d in bank['qualified']:
 cp=Path(d['candidate_path']);ex=json.loads((cp.parent/'surface.json').read_text());rows=ex['rows'];land=np.array([[not q['actual_surface_water'] or 'river' in q['biome'] for q in row] for row in rows]);labs,n=label(land);areas=np.bincount(labs.ravel())[1:]*64;second=sorted(areas,reverse=True)[1];xy=np.array([[q['x'],q['z']] for row in rows for q in row]);flat=[q for row in rows for q in row]
 def sample(pt):
  i=int(np.argmin(((xy-pt)**2).sum(axis=1)));return {'geographic_island':int(labs.ravel()[i]),'sample_offset':math.dist(xy[i],pt),'actual_surface_water':flat[i]['actual_surface_water']}
 e=d['evidence'];membership={};pts={}
 for team,objs in e['objective_world_xz'].items():
  anchors=dict(objs,aether_fountain=[e['fountains'][team][0],e['fountains'][team][2]]);membership[team]={k:sample(v) for k,v in anchors.items()};pts.update({team+'/'+k:v for k,v in anchors.items()})
  ids=[membership[team][k]['geographic_island'] for k in objs];assert len(set(ids))==3 and 0 not in ids
 fs=[membership[t]['aether_fountain']['geographic_island'] for t in membership];assert fs[0]!=fs[1] and all(areas[k-1]>=second/2 for k in fs)
 pts['lair']=e['lair_site']['world_xz'];md=min(math.dist(x,y) for x,y in itertools.combinations(pts.values(),2));assert md>=175
 lair=sample(pts['lair']);results.append({'seed':d['seed'],'source_compilation':d['source_compilation'],'membership':membership,'lair_sample':lair,'minimum_all_nine_anchor_distance':md,'fountain_to_lair_straight_distances':{t:math.dist(pts[t+'/aether_fountain'],pts['lair']) for t in membership},'status':'Independent sampled grouping and spacing pass; desired sequential morphology not certified'})
(a.report/'independent-geographic-audit.json').write_text(json.dumps(results,indent=2)+'\n');print(json.dumps([{k:r[k] for k in ['seed','lair_sample','fountain_to_lair_straight_distances']} for r in results],indent=2))
