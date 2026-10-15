"""Render approximation-only sequence prospects with explicit screening status."""
import json,math
import numpy as np
import matplotlib
matplotlib.use('Agg')
import matplotlib.pyplot as plt
from screen_sequential_archipelagos import OUT,OCEAN
rows=json.loads((OUT/'all-window-fine.json').read_text());rows=[r for r in rows if r['approximate_shared_symmetry_pass']][:8]
cols=min(2,max(1,len(rows)));nr=max(1,math.ceil(len(rows)/cols));fig,axes=plt.subplots(nr,cols,figsize=(6*cols,6*nr),squeeze=False)
for ax,row in zip(axes.flat,rows):
 with open(row['source_probe'],'rb') as f:f.readline();a=np.frombuffer(f.read(),np.int16).reshape(128,128,2)
 land=~np.isin(a[:,:,1],OCEAN);cx,cz=row['centre'];b=[cx-1024,cx+1024,cz+1024,cz-1024]
 ax.imshow(land,cmap='Blues_r',origin='upper',extent=b,vmin=0,vmax=1)
 for home,chain,color in zip(row['home_relative_xz'],row['chains'],['#e96a23','#9225cf']):
  pts=np.array([home]+[q['relative_xz'] for q in chain])+np.array([cx-1024,cz-1024]);ax.plot(pts[:,0],pts[:,1],'-o',color=color,lw=2,ms=4)
 ax.set_title(f"{row['seed']} · center ({cx}, {cz})\nApproximation only; placement and gameplay unchecked");ax.set_aspect('equal');ax.tick_params(labelsize=8)
for ax in axes.flat[len(rows):]:ax.axis('off')
fig.suptitle('Shape prospects: lines mark candidate island sequences, not gameplay routes');fig.tight_layout(rect=[0,0,1,.97]);fig.savefig(OUT/'fine-sequence-prospects.png',dpi=140);plt.close(fig)
print(json.dumps([(q['seed'],q['centre'],q['score']) for q in rows]))
