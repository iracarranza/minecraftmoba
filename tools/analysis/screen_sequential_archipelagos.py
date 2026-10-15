"""Prototype visual-sequence ranking, never a gameplay or design acceptance gate."""
import json, itertools, math
from pathlib import Path
import numpy as np
import matplotlib
matplotlib.use('Agg')
import matplotlib.pyplot as plt
from refine_pale_mansion_search import label
ROOT=Path(__file__).resolve().parents[2]
OUT=ROOT/'implementation/worldgen/reports/archipelago_sequence_search_2026-10-13'
OLD=ROOT/'implementation/worldgen/reports/archipelago_qualified_search_2026-10-12'
ART=ROOT/'artifacts/worldgen/archipelago_qualified_search_2026-10-12/probes'
OCEAN=[0,10,24,44,45,46,47,48,49,50]
def rank_shape(land,step,row):
 seed=row["seed"]
 labs,n=label(land)
 comps=[]
 for k in range(1,n+1):
  yy,xx=np.where(labs==k);area=len(xx)*step*step
  if area>=4096:comps.append({'id':k,'area':area,'xy':np.array([xx.mean(),yy.mean()])*step})
 comps.sort(key=lambda c:-c['area']);best=None
 for u,v in itertools.combinations(comps[:5],2):
  if min(u['area'],v['area'])<160000:continue
  delta=v['xy']-u['xy'];dist=np.linalg.norm(delta)
  if dist<800:continue
  axis=delta/dist;middle=(u['xy']+v['xy'])/2
  others=[]
  for q in comps:
   if q['id'] in (u['id'],v['id']):continue
   t=np.dot(q['xy']-u['xy'],axis)/dist
   off=np.linalg.norm(q['xy']-(u['xy']+t*delta))
   if .08<t<.92 and off<dist*.38:others.append((t,off,q))
  sides=[sorted([q for q in others if q[0]<.5],key=lambda q:q[0]),sorted([q for q in others if q[0]>=.5],key=lambda q:-q[0])]
  if min(map(len,sides))<2:continue
  paths=[];score=0
  for home,side in zip((u,v),sides):
   candidates=[]
   for count in range(2,min(5,len(side))+1):
    for chain in itertools.combinations(side,count):
     qs=[c[2] for c in chain];areas=[q['area'] for q in qs]
     steps=[np.linalg.norm(b['xy']-a['xy']) for a,b in zip([home]+qs,qs)]
     growth=sum(min(1,math.log2(b/a)) for a,b in zip(areas,areas[1:]))
     # Reward several legible stops, small first island, inward growth, and moderate gaps.
     vectors=[b['xy']-a['xy'] for a,b in zip([home]+qs,qs)]
     turns=sum(math.acos(float(np.clip(np.dot(a,b)/(np.linalg.norm(a)*np.linalg.norm(b)),-1,1))) for a,b in zip(vectors,vectors[1:]))
     s=count*2+growth+min(2,math.log2(home['area']/areas[0]))-max(steps)/350-sum(c[1] for c in chain)/dist-turns/math.pi*2
     candidates.append((s,qs))
   s,path=max(candidates,key=lambda c:c[0]);score+=s;paths.append(path)
  item={'seed':seed,'centre':row['centre'],'score':float(score),'home_islands':[{'id':q['id'],'area_proxy':q['area']} for q in (u,v)],'chains':[[{'id':q['id'],'area_proxy':q['area'],'relative_xz':q['xy'].tolist()} for q in path] for path in paths],'home_relative_xz':[u['xy'].tolist(),v['xy'].tolist()],'approximation_only':True,'gameplay_qualified':False}
  if best is None or score>best['score']:best=item
 return best

if __name__=="__main__":
 OUT.mkdir(parents=True,exist_ok=True)
 results=[];grids={}
 census=OUT/'broad-screen.json'
 for row in json.loads(census.read_text() if census.exists() else (OLD/'broad-screen.json').read_text()):
  seed=row['seed'];p=ART/f'{seed}-chain-16.bin'
  step=16
  if p.exists():
   with p.open('rb') as f:f.readline();a=np.frombuffer(f.read(),np.int16).reshape(128,128,2)
  else:
   p=ART/f'{seed}-64.bin';step=64
   if not p.exists():continue
   with p.open('rb') as f:f.readline();full=np.frombuffer(f.read(),np.int16).reshape(48,48,2)
   i=(row['centre'][0]+512)//64;j=(row['centre'][1]+512)//64;a=full[j:j+32,i:i+32]
  land=~np.isin(a[:,:,1],OCEAN);best=rank_shape(land,step,row)
  if best:results.append(best);grids[seed]=land
 results.sort(key=lambda q:-q['score'])
 (OUT/'ranked-prospects.json').write_text(json.dumps({'method':'Prototype straight-corridor centroid sequence; growth is ranked, not required; native biome approximation only; curved paths need visual review','ranked':results},indent=2)+'\n')
 cols=min(4,max(1,len(results)));nr=max(1,math.ceil(min(12,len(results))/cols));fig,axes=plt.subplots(nr,cols,figsize=(4*cols,4*nr),squeeze=False)
 for ax,row in zip(axes.flat,results[:12]):
  ax.imshow(grids[row['seed']],cmap='Blues_r',origin='upper',extent=[0,2048,2048,0],vmin=0,vmax=1)
  for home,chain,color in zip(row['home_relative_xz'],row['chains'],['#e96a23','#9225cf']):
   pts=np.array([home]+[q['relative_xz'] for q in chain]);ax.plot(pts[:,0],pts[:,1],'-o',color=color,lw=2,ms=4)
  ax.set_title(f"{row['seed']} | ranking {row['score']:.1f}");ax.set_xticks([]);ax.set_yticks([])
 for ax in axes.flat[len(results[:12]):]:ax.axis('off')
 fig.suptitle('Approximate island sequences — visual screening only, no gameplay certification');fig.tight_layout();fig.savefig(OUT/'sequence-prospects.png',dpi=140);plt.close(fig)
 print(json.dumps(results[:8],indent=2))
