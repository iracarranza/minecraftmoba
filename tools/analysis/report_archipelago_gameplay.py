import json,itertools,math
from pathlib import Path
import matplotlib
matplotlib.use('Agg')
import matplotlib.pyplot as plt
from matplotlib.colors import ListedColormap
from matplotlib.patches import Circle
import numpy as np
ROOT=Path(__file__).resolve().parents[2];OUT=ROOT/'implementation/worldgen/reports/archipelago_gameplay_2026-10-12'
s=json.loads((OUT/'summary.json').read_text());rows=[]
fig,axes=plt.subplots(2,2,figsize=(15,15),squeeze=False);fig.subplots_adjust(bottom=.08,top=.9,wspace=.22,hspace=.3)
for ax,result in zip(axes.flat,s['results']):
 seed=result['seed'];runs=result['runs'];stage={'recognize':0,'homebase':1,'hinterland':2,'objectives':3,'lair':4,'author':5,'verify':6,'characterize':7}
 best=max(runs,key=lambda r:(r.get('status')=='shared_checks_pass',stage.get(r.get('deepest_stage_reached'),-1),-r.get('symmetry',{}).get('deviation_over_relief',999)))
 passed=best['status']=='shared_checks_pass';e=best.get('evidence',{});ex=json.loads((OUT/str(seed)/'surface.json').read_text());pix=np.array([[3 if c['actual_surface_water'] else 2 if 'forest' in c['biome'] or 'jungle' in c['biome'] or 'taiga' in c['biome'] else 1 if c['terrain_y']>=90 else 0 for c in row] for row in ex['rows']])
 ax.imshow(pix,cmap=ListedColormap(['#b9ce97','#9a9683','#456b46','#659fcb']),vmin=0,vmax=3,origin='lower',extent=[-1024,1024,-1024,1024],interpolation='nearest')
 for team,xyz in e.get('fountains',{}).items():
  color='#b32f32' if team=='north' else '#245bcc';ax.scatter(xyz[0],xyz[2],s=90,c=color,edgecolor='white');ax.annotate(team[0].upper()+'F',(xyz[0],xyz[2]),xytext=(6,6),textcoords='offset points',color=color,bbox=dict(facecolor='white',alpha=.8,edgecolor='none'))
 for team,objs in e.get('objective_world_xz',{}).items():
  for kind,pos in objs.items():ax.annotate({'pillager_outpost':'O','nether_bastion':'B','end_spike':'S'}[kind],pos,ha='center',va='center',color='#b32f32' if team=='north' else '#245bcc',bbox=dict(facecolor='white',alpha=.8,edgecolor='none'))
 if e.get('lair_site'):
  l=e['lair_site']['world_xz'];ax.scatter(*l,marker='D',s=80,c='#80318b',edgecolor='white');ax.add_patch(Circle(l,48,fill=False,color='#80318b'));ax.annotate('Lair',l,xytext=(7,7),textcoords='offset points')
 reasons=list(dict.fromkeys(r['code'] for r in best.get('rejections',[])));reasons=reasons or ([best['reason']] if best.get('reason') else []);label='Shared checks + 175 spacing passed' if passed else 'Rejected: '+('\n'.join(reasons) or best.get('reason','no candidate'))
 ax.invert_yaxis();ax.set_aspect('equal');ax.set_xlabel('World X');ax.set_ylabel('World Z · north at top');ax.set_title(f'Seed {seed} · rotation {best["rotation"]}°\n{label}',fontsize=11)
 rows.append({'seed':seed,'passed':passed,'rotation':best['rotation'],'stage':best.get('deepest_stage_reached') or 'symmetry','rejections':reasons,'spacing':best.get('spacing_audit')})
fig.suptitle('Archipelago gameplay qualification · Java 1.21.11 vanilla surface terrain\n175-block minimum centre distance for same-team F/O/B/S and Lair-to-landmark pairs',fontsize=14)
fig.text(.5,.035,'Blue: surface water · green: land/forest · gray: high land · F: Fountain · O/B/S: defenses · ◆: Lair\nProposed siting only; rejected panels are diagnostic. No playable map or runtime certification.',ha='center',fontsize=11)
fig.savefig(OUT/'gameplay-layouts.png',dpi=150)
lines=['# Archipelago gameplay + 175-block spacing','', '**Prototype/test read-only qualification.** Screened the six pictured origin windows, then the remaining twelve refined prospects. Four promising windows were generated with the official Java 1.21.11 server; all four orientations were evaluated against actual terrain. Alternative legal Core pairs were also tested where recorded. No authored playable map is produced.','', '| Seed | Outcome | Best stage | Rejections |','|---|---|---|---|']
for r in rows:lines.append(f"| {r['seed']} | {'Implemented checks passed' if r['passed'] else 'Rejected tested layout'} | {r['stage']} | {', '.join(r['rejections'])} |")
lines+=['','Checks: existing contested-land symmetry, independent Core quality >=0.52, compact hinterland, defensive ordering and physical fit, Lair viability and <=15% prototype access disparity, column clearance/levelling, opening ceilings/floors and resource validity where earlier stages pass. Failing a stage stops qualification. Default biome gradient is not applied to Archipelago.','', '**User-required spacing:** all six same-team horizontal centre distances among Fountain/Outpost/Bastion/Spike must be >=175 blocks. Every Fountain/defensive anchor must also be >=175 blocks from the Lair; existing building footprints and a 48-block encounter half-span remain excluded. Spacing is enforced during objective selection and independently remeasured on any passing final layout. Defensive candidate pools expanded to 64 sites per form; no shared quality/parity/physical thresholds loosened.','', 'Limitations: full physical boss volume, live water travel/boat access, canonically Open exclusions and broader territorial advantage thresholds remain unverified. The search tests origin-centred windows and the current bounded Core fitter; rejection is not proof the whole seed is impossible.','', '[Generated-world layout visual](gameplay-layouts.png)','[Full evidence](summary.json)']
(OUT/'README.md').write_text('\n'.join(lines)+'\n');print(json.dumps(rows,indent=2))
