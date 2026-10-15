"""Generate promising shifted windows and run the unchanged shared gates with joint 175-block siting."""
import sys,json,concurrent.futures,argparse
from pathlib import Path
from filter_pale_gameplay import ROOT,JAR,JAVA,RUNTIME,acquire,extract_region,evaluate_orientation,enrich,_public,VERSION,DATA_VERSION,GENERATOR,SERVER_SHA1,save
import search_archipelago_layouts as joint
OUT=joint.OUT;GEN=OUT/'generated';ART=ROOT/'artifacts/worldgen/archipelago_qualified_search_2026-10-12/source-worlds'
def generate(row):
 seed=row['seed'];cx,cz=row['centre'];bounds=(cx//16-64,cx//16+63,cz//16-64,cz//16+63);folder=GEN/str(seed);folder.mkdir(parents=True,exist_ok=True);root=ART/str(seed)/'server';world=root/'world'
 if (folder/'surface.json').exists():ex=json.loads((folder/'surface.json').read_text())
 else:
  acquire.verify_server_jar(JAR);world,timing=acquire.generate_world(seed,JAR,JAVA,root,RUNTIME,bounds,heap='2G',progress=lambda n,total:print('generate shifted',seed,n,total,flush=True) if n%10==0 or n==total else None)
  save(folder/'generation.json',timing);print('extract shifted',seed,flush=True);ex=extract_region(world,bounds,ground_scan=True);save(folder/'surface.json',ex)
 block=[bounds[0]*16,(bounds[1]+1)*16-1,bounds[2]*16,(bounds[3]+1)*16-1]
 assert ex['width']*8==block[1]-block[0]+1 and ex['height']*8==block[3]-block[2]+1
 for rotation in [0,90,180,270]:
  c=evaluate_orientation(ex,rotation,False,fit_routes=False)
  if c is None:continue
  enrich(c);c.update(seed=seed,schema_version=1,generator=GENERATOR,minecraft_java_version=VERSION,data_version=DATA_VERSION,region={'chunk_bounds':list(bounds),'block_bounds':block},actual_structures=ex['structures'],actual_block_volume_presence=ex['block_volume_presence'],data_completeness={'full_chunks':ex['full_chunks'],'expected_chunks':ex['expected_chunks']},local_world_directory=str(world),prospect_types=['archipelago'],worldgen_truth={'source':'official Mojang dedicated server','server_jar_sha1':SERVER_SHA1,'verified':True,'terrain_modified':False})
  save(folder/f'candidate-r{rotation}.json',_public(c))
 save(folder/'result.json',{'seed':seed,'world':str(world),'bounds':block,'screen':row});return seed

def main():
 p=argparse.ArgumentParser();p.add_argument('--seeds',nargs='+',type=int,default=[890001347,890001687]);p.add_argument('--budget',type=int,default=8);p.add_argument('--generate-only',action='store_true');p.add_argument('--prospects',type=Path,default=OUT/'broad-refined.json');a=p.parse_args()
 rows=json.loads(a.prospects.read_text());rows=[r for r in rows if r['seed'] in a.seeds]
 with concurrent.futures.ThreadPoolExecutor(max_workers=2) as ex:results=list(ex.map(generate,rows))
 if a.generate_only:return
 joint.OLD=GEN
 for seed in results:
  for rotation in [0,90,180,270]:
   if not (GEN/str(seed)/f'candidate-r{rotation}.json').exists():continue
   r=joint.run(seed,rotation,a.budget)
   if r:break
if __name__=='__main__':main()
