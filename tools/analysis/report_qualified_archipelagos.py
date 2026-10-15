"""Report only candidates that pass every implemented shared stage and spacing/type audit."""
import sys,json,math,itertools,hashlib,argparse
from pathlib import Path
import numpy as np
import matplotlib
matplotlib.use('Agg')
import matplotlib.pyplot as plt
from matplotlib.colors import ListedColormap
from matplotlib.patches import Circle,Patch
from filter_pale_gameplay import ROOT,save,map_types
from vanilla_search.evaluate import _components
OUT=ROOT/'implementation/worldgen/reports/archipelago_qualified_search_2026-10-12'
parser=argparse.ArgumentParser();parser.add_argument('--seeds',nargs='+',type=int);parser.add_argument('--mode');parser.add_argument('--report-dir',type=Path);args=parser.parse_args();RPT=args.report_dir or OUT;RPT.mkdir(parents=True,exist_ok=True)
passed={};tested=[]
for p in OUT.glob('*-r*-p[0-9]*.json'):
 d=json.loads(p.read_text())
 if args.seeds and d['seed'] not in args.seeds:continue
 if args.mode and d.get('core_search_mode')!=args.mode:continue
 tested.append({'seed':d['seed'],'rotation':d['rotation'],'variant':d['core_pair_variant'],'status':d['status'],'stage':d['deepest_stage_reached'],'rejections':[r['code'] for r in d['rejections']],'path':str(p)})
 if d['status']!='shared_checks_pass' or d.get('archipelago_type_review',{}).get('status')=='reject':continue
 assert not d['rejections'] and d['deepest_stage_reached']=='characterize'
 cp=Path(d['candidate_path']);c=json.loads(cp.read_text());ex=json.loads((cp.parent/'surface.json').read_text());rows=ex['rows'];w,h=ex['width'],ex['height'];water=[r['actual_surface_water'] for row in rows for r in row];bodies=_components([not q for q in water],w,h);features={'water':sum(water)/len(water),'land_bodies':len(bodies)}
 if not map_types.predicates()['archipelago'](features):continue
 e=d['evidence'];audit={};anchors=[];distances={}
 for team,objs in e['objective_world_xz'].items():
  pts=dict(objs,aether_fountain=[e['fountains'][team][0],e['fountains'][team][2]]);ds={f'{a}/{b}':math.dist(x,y) for (a,x),(b,y) in itertools.combinations(pts.items(),2)};audit[team]=min(ds.values());assert audit[team]>=175;anchors+=list(pts.values());distances[team]=ds
 audit['lair']=min(math.dist(e['lair_site']['world_xz'],p) for p in anchors);assert audit['lair']>=175;audit['global_minimum']=min(math.dist(a,b) for a,b in itertools.combinations(anchors+[e['lair_site']['world_xz']],2));assert audit['global_minimum']>=175
 d.update(source_compilation=str(p),independent_spacing_audit=audit,pairwise_distances=distances,actual_archipelago_features=features,island_area_proxies=[a*64 for a in bodies[:10]],bounds=c['region']['block_bounds'])
 if d['seed'] not in passed or e['lair_site']['access_asymmetry']<passed[d['seed']]['evidence']['lair_site']['access_asymmetry']:passed[d['seed']]=d
qualified=sorted(passed.values(),key=lambda d:d['seed']);save(RPT/'qualified-summary.json',{'status':'Prototype/test geography qualification; not authored or runtime READY','qualified':qualified,'tested_layouts':tested,'minimum_distance':175,'spacing_scope':'All nine Fountain/Outpost/Bastion/Spike/Lair anchors, including opposing teams','seeds_in_broader_census':1000,'shifted_windows_per_seed':9,'fine_refined_prospects':len(json.loads((OUT/'broad-refined.json').read_text())),'search_mode':args.mode,'compiler_sha256':hashlib.sha256((ROOT/'implementation/worldgen/terrain_harvest/map_compiler.py').read_bytes()).hexdigest(),'all_shared_thresholds_preserved':True,'limitations':['Actual all-boss encounter volume and runtime readiness remain unverified.','Equipment-sufficient accessible-iron ceiling remains canonically Open; carrot observations are covered by resource validity.','Terrain-based travel cost is not a calibrated boat/logistics model.','Spanned terrain and per-team reach are diagnostics; broader joint-territory acceptance remains Open.']})
if not qualified:print('No qualified rows yet');sys.exit(0)
n=len(qualified);cols=min(2,n);nr=math.ceil(n/cols);fig,axes=plt.subplots(nr,cols,figsize=(8*cols,8*nr+1.8),squeeze=False);fig.subplots_adjust(bottom=.12/nr,top=1-.12/(nr+1),hspace=.2,wspace=.18)
for ax,d in zip(axes.flat,qualified):
 c=json.loads(Path(d['candidate_path']).read_text());ex=json.loads((Path(d['candidate_path']).parent/'surface.json').read_text());e=d['evidence'];b=d['bounds']
 pixels=np.array([[3 if q['actual_surface_water'] else 2 if any(v in q['biome'] for v in ['forest','jungle','taiga']) else 1 if q['terrain_y']>=90 else 0 for q in row] for row in ex['rows']])
 colors=['#b8cc92','#a19d8a','#456c48','#659fcb'];ax.imshow(pixels,cmap=ListedColormap(colors),vmin=0,vmax=3,origin='lower',extent=[b[0],b[1]+1,b[2],b[3]+1],interpolation='nearest')
 for team,xyz in e['fountains'].items():
  color='#bd3635' if team=='north' else '#245bd0';ax.scatter(xyz[0],xyz[2],s=90,c=color,edgecolor='white',zorder=5);ax.annotate(team[0].upper()+'F',(xyz[0],xyz[2]),xytext=(8,8),textcoords='offset points',color=color,fontweight='bold',bbox=dict(facecolor='white',alpha=.85,edgecolor='none'))
  pts=[[xyz[0],xyz[2]]]
  for kind,pos in e['objective_world_xz'][team].items():
   pts.append(pos);ax.annotate({'pillager_outpost':'O','nether_bastion':'B','end_spike':'S'}[kind],pos,ha='center',va='center',fontsize=11,fontweight='bold',color=color,bbox=dict(facecolor='white',alpha=.9,edgecolor='none',pad=2))
  for pos in pts:ax.add_patch(Circle(pos,87.5,fill=False,color=color,lw=.6,alpha=.4))
 lair=e['lair_site']['world_xz'];ax.scatter(*lair,s=110,marker='D',c='#80338a',edgecolor='white',zorder=6);ax.add_patch(Circle(lair,48,fill=False,color='#80338a',lw=1.2));ax.annotate('Lair',lair,xytext=(8,8),textcoords='offset points',color='#80338a',bbox=dict(facecolor='white',alpha=.85,edgecolor='none'))
 ax.set_xlim(b[0],b[1]+1);ax.set_ylim(b[3]+1,b[2]);ax.set_aspect('equal');ax.set_xlabel('World X');ax.set_ylabel('World Z · north at top');audit=d['independent_spacing_audit'];ax.set_title(f"Seed {d['seed']} · ocean {d['actual_archipelago_features']['water']:.0%}\nClosest anchor pair: {audit['global_minimum']:.1f} blocks\nLair access disparity {e['lair_site']['access_asymmetry']:.1%} · all implemented gates passed",fontsize=12)
for ax in list(axes.flat)[n:]:ax.set_visible(False)
fig.suptitle('Archipelago prospects · Java 1.21.11\nImplemented gameplay checks + 175-block minimum spacing',fontsize=15)
fig.text(.5,.035/nr,'F: Fountain · O: Outpost · B: Bastion · S: Spike · ◆: Lair\nFaint team circles: 87.5-block half-spacing. Purple ring: sampled Lair half-span.\nRead-only geography qualification; boss volume and runtime readiness remain unverified.',ha='center',fontsize=10)
fig.savefig(RPT/'qualified-archipelagos.png',dpi=160)
fig.canvas.draw()
for ax,d in zip(axes.flat,qualified):
 bbox=ax.get_tightbbox(fig.canvas.get_renderer()).transformed(fig.dpi_scale_trans.inverted()).expanded(1.04,1.04)
 fig.savefig(RPT/f"seed-{d['seed']}-map.png",dpi=160,bbox_inches=bbox)
lines=['# Qualified archipelago prospects','', '**Prototype/test geography qualification, Java 1.21.11.** All implemented shared checks passed, with the user-required 175-block horizontal centre spacing. Source worlds are unmodified; no playable-map build or READY publication.','', '| Seed | Window bounds X / Z | Surface water | Minimum all-anchor spacing | Closest Lair–landmark | Lair access disparity |','|---|---|---:|---:|---:|---:|']
for d in qualified:
 a=d['independent_spacing_audit'];b=d['bounds'];lines.append(f"| [{d['seed']}](seed-{d['seed']}-map.png) | {b[0]}…{b[1]} / {b[2]}…{b[3]} | {d['actual_archipelago_features']['water']:.1%} | {a['global_minimum']:.1f} | {a['lair']:.1f} | {d['evidence']['lair_site']['access_asymmetry']:.1%} |")
lines+=['','[Map visual](qualified-archipelagos.png)','[Full qualification and spacing evidence](qualified-summary.json)','', 'Search: broadened to 1,000 seeds and nine shifted windows per seed; 147 initial coarse prospects; additional island-focused refinement is saved in `chain-refined.json`. Reused four prior vanilla source worlds and generated larger-island shifted prospects. Prototype/test search considers both the original Core bands and wider northern/southern Core positions, while preserving fitted Homebase acceptance. Defensive candidates are distributed across spatial positions and practical-advance bands. An additional search pass reserves a balanced Lair and spaces opposing-team pads during placement. Existing own-team cost half, rival-distance cap, forward corridor, strict defensive ordering and authored footprints remain enforced. Column checks reject unsuitable pads. Candidate diversification changes search coverage, not acceptance thresholds. Exact packed-section counting is accelerated locally, with synthetic, vanilla-section and full-window equivalence evidence saved alongside these reports.','', 'Applied gates: actual Archipelago predicate (surface water >43.1% and more than one disconnected land body); contested-land terrain symmetry <=0.2371; independent Homebase quality >=0.52; compact hinterland <=0.5; strict Outpost/Bastion/Spike ordering; all 36 pair distances across the nine Fountain/Outpost/Bastion/Spike/Lair anchors >=175, including opposing teams; unchanged Lair usable-ground/open-volume/approach proxies and access disparity <=15%; measured objective/Fountain column geometry, levelling <=10 blocks per column, opening ceiling, opening floor and resource validity. Final pair distances independently recomputed from saved evidence.','', 'Limits: Lair air-volume proxy does not certify actual Giant/Ghast/Dragon encounter collision and flight geometry. Equipment-sufficient accessible-iron ceiling and broader territorial acceptance thresholds remain Open. Travel is analytical surface cost, not calibrated boat speed or live Logistics. No authored Lair manifestation or runtime readiness certification.','', 'Scripts: `tools/analysis/search_archipelago_windows.py`, `search_archipelago_layouts.py`, `generate_archipelago_prospects.py`, `report_qualified_archipelagos.py`. Existing reports and worlds are preserved.']
if args.mode=='wide-islands':
 lines += ['', 'Additional Prototype/test search preferences: fitted Fountains on two substantial distinct 8-block surface components (at least half the second-largest component area); each team’s three defensive anchors on distinct dry components; Lair ranked by shortest worst-team straight-line Fountain distance among sites passing physical proxies, spacing and <=15% modeled disparity. Island-aware pooling retains lower-ranked strategic islands; the final local forward search includes alternate routes within 135% of minimum modeled Fountain-to-Fountain cost; Core pairs are ranked by potential island spread before accepted Homebase quality. These preferences are not final balance, and surface components do not prove practical chokepoints or live discovery time.']
(RPT/'README.md').write_text('\n'.join(lines)+'\n');print(json.dumps([{'seed':d['seed'],'spacing':d['independent_spacing_audit'],'lair_disparity':d['evidence']['lair_site']['access_asymmetry']} for d in qualified],indent=2))
