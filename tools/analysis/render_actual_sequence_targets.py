"""Actual vanilla terrain overview; suggested sequences are not objective placements."""
import json
from pathlib import Path
import numpy as np
import matplotlib
matplotlib.use('Agg')
import matplotlib.pyplot as plt
from matplotlib.colors import ListedColormap
ROOT=Path(__file__).resolve().parents[2];OUT=ROOT/'implementation/worldgen/reports/archipelago_sequence_search_2026-10-13';GEN=OUT.parent/'archipelago_qualified_search_2026-10-12/generated'
rows=json.loads((OUT/'vanilla-targets.json').read_text());fig,axes=plt.subplots(1,2,figsize=(14,7))
for ax,row in zip(axes,rows):
 seed=row['seed'];p=GEN/str(seed)/'surface.json'
 if not p.exists():ax.set_title(f'{seed} · extraction pending');ax.axis('off');continue
 ex=json.loads(p.read_text());rr=ex['rows'];pixels=np.array([[3 if q['actual_surface_water'] else 2 if any(s in q['biome'] for s in ['forest','jungle','taiga']) else 1 if q['terrain_y']>=90 else 0 for q in r] for r in rr]);b=[rr[0][0]['x']-4,rr[0][-1]['x']+4,rr[-1][0]['z']+4,rr[0][0]['z']-4]
 ax.imshow(pixels,cmap=ListedColormap(['#b9ce93','#a9a47a','#456d46','#639dc5']),origin='upper',extent=b,vmin=0,vmax=3)
 cx,cz=row['centre']
 for home,chain,color in zip(row['home_relative_xz'],row['chains'],['#ff6a17','#ae28ed']):
  pts=np.array([home]+[q['relative_xz'] for q in chain])+[cx-1024,cz-1024];ax.plot(pts[:,0],pts[:,1],'-o',color=color,lw=2,ms=4)
 ax.set_title(f'{seed} · actual Java 1.21.11 terrain\nSuggested island sequence; placement qualification separate');ax.set_aspect('equal');ax.set_xlabel('World X');ax.set_ylabel('World Z · north at top')
fig.suptitle('Vanilla verification targets · no terrain modifications');fig.tight_layout(rect=[0,0,1,.95]);fig.savefig(OUT/'actual-sequence-targets.png',dpi=150);plt.close(fig)
