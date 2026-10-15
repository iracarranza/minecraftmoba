"""Java 1.21.11 archipelago reconnaissance, approximate grids only."""
import sys,json,subprocess,concurrent.futures
from pathlib import Path
import numpy as np
import matplotlib
matplotlib.use('Agg')
import matplotlib.pyplot as plt
from matplotlib.colors import ListedColormap
from refine_pale_mansion_search import label
ROOT=Path(__file__).resolve().parents[2]
OUT=ROOT/'implementation/worldgen/reports/archipelago_search_2026-10-12';OUT.mkdir(parents=True,exist_ok=True)
ART=ROOT/'artifacts/worldgen/archipelago_search_2026-10-12';ART.mkdir(parents=True,exist_ok=True)
BIN=ROOT/'artifacts/worldgen/pale_gameplay_2026-10-12/build/pale_gameplay_probe'
OCEAN=[0,10,24,44,45,46,47,48,49,50]
def probe(seed,step=64):
 p=ART/f'{seed}-{step}.bin';subprocess.run([str(BIN),str(seed),'0','0','1024',str(step),str(p)],check=True)
 with p.open('rb') as f:
  header=f.readline().decode().split();n=int(header[3]);a=np.frombuffer(f.read(),dtype=np.int16).reshape(n,n,2)
 water=np.isin(a[:,:,1],OCEAN);land=~water
 labs,count=label(land);sizes=sorted(np.bincount(labs.ravel())[1:].tolist(),reverse=True)
 big=[s for s in sizes if s*step*step>=16384];share=water.mean()
 if not .40<=share<=.85 or len(big)<3:return None
 areas=[int(s*step*step) for s in big]
 # Prefer several substantial islands; penalize one overwhelmingly dominant landmass.
 dominance=max(big)/sum(sizes);score=len(big)+min(areas[1]/50000,5)+min(areas[2]/30000,4)-dominance*8
 return {'seed':seed,'centre':[0,0],'bounds':[-1024,1024,-1024,1024],'ocean_fraction':float(share),'substantial_land_bodies':len(big),'largest_island_areas_proxy':areas[:10],'dominant_island_land_share':dominance,'score':score,'step':step},water,a
rows=[]
with concurrent.futures.ThreadPoolExecutor(max_workers=2) as ex:
 for k,result in enumerate(ex.map(probe,range(890000001,890000201)),1):
  if result:rows.append(result)
  if k%25==0:print('screened',k,'candidates',len(rows),flush=True)
rows.sort(key=lambda r:r[0]['score'],reverse=True)
fine=[]
for r,_,_ in rows[:18]:
 v=probe(r['seed'],16)
 if v:fine.append(v)
fine.sort(key=lambda r:r[0]['score'],reverse=True);shown=fine[:6]
(OUT/'summary.json').write_text(json.dumps({'status':'Prototype/test reconnaissance, not gameplay qualification','version':'Java 1.21.11','seeds_screened':200,'coarse_candidates':len(rows),'refined':len(fine),'candidates':[r for r,_,_ in fine],'limitations':['Ocean-biome mask and four-neighbour connected dry-biome samples; not generated blocks.','Areas and isolation are proxies; narrow channels, rivers and boundary-cut islands need vanilla verification.','No minimum landmark spacing, opening resources, Lair volume or runtime checks performed.','Search window centred at origin; this is not a spawn-location guarantee.']},indent=2)+'\n')
fig,axes=plt.subplots(2,3,figsize=(15,11),layout='constrained')
for ax,(r,water,a) in zip(axes.flat,shown):
 ax.imshow(water,cmap=ListedColormap(['#a9bf83','#629bc4']),origin='lower',extent=[-1024,1024,-1024,1024],interpolation='nearest');ax.invert_yaxis();ax.set_title(f"Seed {r['seed']}\nOcean {r['ocean_fraction']:.0%} · {r['substantial_land_bodies']} land bodies ≥16k blocks²");ax.set_xlabel('World X');ax.set_ylabel('World Z · north at top')
for ax in list(axes.flat)[len(shown):]:ax.set_visible(False)
fig.suptitle('Archipelago prospects · Java 1.21.11 · 2048 × 2048 blocks\n16-block ocean-biome samples; green land / blue ocean. Geography prospects, not qualified layouts.',fontsize=15)
fig.savefig(OUT/'archipelago-candidates.png',dpi=150)
lines=['# Archipelago seed prospects','', 'Prototype/test reconnaissance for Java 1.21.11. Searched 200 seeds around world origin in 2048×2048 windows; refined the best coarse candidates at 16-block spacing. No generated-world or gameplay certification.','', '| Seed | Ocean | Substantial land bodies | Three largest area proxies (blocks²) |','|---|---:|---:|---|']
for r,_,_ in shown:lines.append(f"| {r['seed']} | {r['ocean_fraction']:.1%} | {r['substantial_land_bodies']} | {r['largest_island_areas_proxy'][:3]} |")
lines+=['','[Map comparison](archipelago-candidates.png)','', 'Ocean-biome connectivity is approximate. Islands touching the window boundary may continue into mainland. Rivers and narrow channels are not fully represented. Minimum landmark spacing, joint territorial placement, encounter volume, opening resources and vanilla-world verification remain required.']
(OUT/'README.md').write_text('\n'.join(lines)+'\n');print(json.dumps([r for r,_,_ in shown],indent=2),flush=True)
