"""Screen smaller vanilla subwindows without generating or modifying terrain."""
import json,argparse,concurrent.futures
import numpy as np
from filter_pale_gameplay import ROOT,save,scoop,map_types,extract_region,evaluate_orientation,enrich,_public,VERSION,DATA_VERSION,GENERATOR,SERVER_SHA1
from vanilla_search.evaluate import orient,_components
from search_archipelago_layouts import OUT
CROP=OUT/'cropped-1792'
def screen(seed):
 f=OUT/'generated'/str(seed);ex=json.loads((f/'surface.json').read_text());original=json.loads((f/'candidate-r0.json').read_text());b=original['region']['block_bounds'];options=[]
 for j in [0,16,32]:
  for i in [0,16,32]:
   rows=[row[i:i+224] for row in ex['rows'][j:j+224]];water=[[q['actual_surface_water'] for q in row] for row in rows];heights=[[max(63,q['terrain_y']) for q in row] for row in rows];bodies=_components([not q for row in water for q in row],224,224);features={'water':sum(q for row in water for q in row)/224**2,'land_bodies':len(bodies)}
   if not map_types.predicates()['archipelago'](features):continue
   for rotation in [0,90,180,270]:
    hh=orient(heights,rotation,False);ww=orient(water,rotation,False);s=scoop.symmetry([q for row in hh for q in row],224,224,'z',spacing_hint=8,contested=[not q for row in ww for q in row])['deviation_over_relief']
    if s>map_types.symmetry_bound()['value']:continue
    cb=[b[0]+i*8,b[0]+i*8+1791,b[2]+j*8,b[2]+j*8+1791]
    options.append({'seed':seed,'bounds':cb,'rotation':rotation,'symmetry':s,'island_areas':[v*64 for v in bodies[:20]],'water':features['water']})
 if not options:return
 options.sort(key=lambda q:(-sum(a>=4096 for a in q['island_areas']),q['symmetry']))
 return options[0]
def extract(row):
 seed=row['seed'];f=OUT/'generated'/str(seed);info=json.loads((f/'result.json').read_text());world=info['world'];b=row['bounds'];chunks=(b[0]//16,b[1]//16,b[2]//16,b[3]//16);dest=CROP/str(seed);dest.mkdir(parents=True,exist_ok=True)
 print('extract crop',seed,row['symmetry'],flush=True)
 if (dest/'surface.json').exists():ex=json.loads((dest/'surface.json').read_text())
 else:ex=extract_region(world,chunks,ground_scan=True);save(dest/'surface.json',ex)
 for rotation in [0,90,180,270]:
  c=evaluate_orientation(ex,rotation,False,fit_routes=False)
  if c is None:continue
  enrich(c);c.update(seed=seed,schema_version=1,generator=GENERATOR,minecraft_java_version=VERSION,data_version=DATA_VERSION,region={'chunk_bounds':list(chunks),'block_bounds':b},actual_structures=ex['structures'],actual_block_volume_presence=ex['block_volume_presence'],data_completeness={'full_chunks':ex['full_chunks'],'expected_chunks':ex['expected_chunks']},local_world_directory=world,prospect_types=['archipelago'],worldgen_truth={'source':'official Mojang dedicated server','server_jar_sha1':SERVER_SHA1,'verified':True,'terrain_modified':False},search_window_note='Prototype/test 1792-block subwindow of a fully generated 2048-block vanilla source window')
  save(dest/f'candidate-r{rotation}.json',_public(c))
 save(dest/'result.json',{'seed':seed,'world':world,'bounds':b,'screen':row});return row
if __name__=='__main__':
 p=argparse.ArgumentParser();p.add_argument('--seeds',nargs='+',type=int,required=True);p.add_argument('--extract',action='store_true');a=p.parse_args();rows=[r for seed in a.seeds if (r:=screen(seed))];save(OUT/'crop-1792-screen.json',rows);print(json.dumps(rows,indent=2),flush=True)
 if a.extract:
  with concurrent.futures.ThreadPoolExecutor(max_workers=2) as pool:list(pool.map(extract,rows))
