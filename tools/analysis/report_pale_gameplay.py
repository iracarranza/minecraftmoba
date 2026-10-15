"""Extent-first gameplay funnel and generated-world evidence, read-only."""
import hashlib
import json
import math
from pathlib import Path
import sys
from collections import Counter

import matplotlib
matplotlib.use('Agg')
import matplotlib.pyplot as plt
from matplotlib.colors import ListedColormap
from matplotlib.patches import Patch
import numpy as np

ROOT=Path(__file__).resolve().parents[2]
sys.path.insert(0,str(ROOT/'implementation/worldgen'))
from vanilla_search.task_a import Terrain,shortest
REPORT=ROOT/'implementation/worldgen/reports/pale_gameplay_2026-10-12'


def save(path,d):path.write_text(json.dumps(d,indent=2)+'\n')


def mansion_access(candidate,run,job):
    homes=run.get('evidence',{}).get('fountains',{})
    if len(homes)!=2:return {'measured':False,'why':'no accepted Fountain pair'}
    t=Terrain(candidate);mx,mz=job['mansion_start']
    grounds=[i for i,(x,z) in enumerate(t.coords) if 80<=math.hypot(x-mx,z-mz)<=112
             and not t.v['water'][i] and t.grade[i]<=8]
    if not grounds:return {'measured':False,'why':'no sampled dry ground in mansion approach ring'}
    costs={}
    for team,xyz in homes.items():
        i=min(range(t.n),key=lambda i:math.dist(t.coords[i],(xyz[0],xyz[2])))
        ds=shortest(t.adj,{i:0.})[0]
        costs[team]=min(ds.get(j,math.inf) for j in grounds)
    values=list(costs.values());mean=sum(values)/2
    return {'measured':True,'effective_block_cost':costs,
            'normalized_disparity':abs(values[0]-values[1])/mean if mean else None,
            'grounds_ring_blocks':[80,112],'gated':False,
            'why_not_gated':'No canonical Mansion access disparity bound is established. Reported separately from the existing 0.15 Lair gate.',
            'limitations':'Sampled surface traversal cost, not travel time, entrances, underground routes, or player-built shortcuts.'}


def main():
    inp=json.loads((REPORT/'inputs.json').read_text())
    bank=json.loads((REPORT/'extent-shortlist.json').read_text())
    queue=json.loads((REPORT/'generation-queue.json').read_text())
    results=[]
    for path in sorted((REPORT/'attempts').glob('*/result.json')):
        r=json.loads(path.read_text());r['attempt_directory']=str(path.parent)
        alternative_path=path.parent/'core-rebalance/summary.json'
        if alternative_path.exists():
            alternative=json.loads(alternative_path.read_text()).get('selected')
            if alternative:
                alternative['source_compilation_path']=str(path.parent/'core-rebalance'/
                    f"compilation-r{alternative['rotation']}-p{alternative['core_pair_variant']}.json")
                r['runs'].append(alternative)
        for run in r.get('runs',[]):
            cp=path.parent/f"candidate-r{run['rotation']}.json"
            if cp.exists() and run.get('evidence',{}).get('fountains') and not run.get('mansion_access'):
                diagnostic_path=path.parent/f"mansion-access-r{run['rotation']}.json"
                if diagnostic_path.exists():d=json.loads(diagnostic_path.read_text())
                else:
                    d=mansion_access(json.loads(cp.read_text()),run,r['job']);save(diagnostic_path,d)
                run['mansion_access']=d
        results.append(r)
    results.sort(key=lambda r:r['job']['forest']['area_proxy_blocks2'],reverse=True)
    accepted=[r for r in results if r['shared_gameplay_pass']]
    stages={'symmetry':0,'recognize':1,'homebase':2,'hinterland':3,'objectives':4,'lair':5,
            'select':6,'author':7,'verify':8,'characterize':9,'ready':10}
    rows=[]
    for r in results:
        passing=[run for run in r.get('runs',[]) if run['status']=='shared_checks_pass']
        pool=passing or r.get('runs',[])
        best=min(passing,key=lambda run:(run.get('mansion_access') or {}).get('normalized_disparity',math.inf)) if passing else max(pool,key=lambda run:stages.get(run.get('deepest_stage_reached','symmetry'),0)) if pool else None
        rows.append({'seed':r['seed'],'forest_area_proxy':r['job']['forest']['area_proxy_blocks2'],
            'centrality_in_equivalent_radii':r['job']['forest']['centroid_offset_in_equivalent_radii'],
            'mansion_start':r['job']['mansion_start'],'actual_target_mansion_exists':r.get('actual_target_mansion_exists'),
            'shared_gameplay_pass':r['shared_gameplay_pass'],'best_rotation':best.get('rotation') if best else None,
            'deepest_stage':best.get('deepest_stage_reached','symmetry') if best else None,
            'rejection_codes':[j['code'] for j in best.get('rejections',[])] if best else [],
            'homeland_quality':best.get('evidence',{}).get('homelands') if best else None,
            'actual_symmetry':best.get('actual_symmetry') if best else None,
            'lair_access_disparity':best.get('evidence',{}).get('lair_site',{}).get('access_asymmetry') if best else None,
            'mansion_access':best.get('mansion_access') if best else None,
            'actual_pale_area_in_window_proxy':r.get('actual_pale_area_in_window_proxy'),
            'world':r.get('world'),'attempt_directory':r['attempt_directory'],'error':r.get('error'),
            'core_pair_variant':best.get('core_pair_variant') if best else None,
            'source_compilation_path':best.get('source_compilation_path') if best else None,
            'generated_block_bounds':r.get('generated_block_bounds')})
    summary={'status':'Prototype/test gameplay qualification','centrality_limit_old':.75,'centrality_limit_new':1.,
        'extent_ranked_bank':len(bank),'screen_eligible_locations':len(queue),'generated_attempts':len(results),
        'shared_gameplay_pass_seeds':[r['seed'] for r in accepted],
        'ranked_results':rows,'production_ready':False,'inputs':inp,
        'limitations':['Qualification is read-only; no authored map or runtime readiness certification.',
          'Only the implemented shared checks are bounded; remaining opening exclusions, reachability and Pale-specific compensation remain Open.',
          'Approximate holds are not certified geographic failures; generated checks cover a bounded extent-ranked shortlist.',
          'Mansion access parity is measured separately and has no established acceptance threshold.'],
        'source_compiler_current_sha256':hashlib.sha256((ROOT/'implementation/worldgen/terrain_harvest/map_compiler.py').read_bytes()).hexdigest()}
    save(REPORT/'filtered-summary.json',summary)
    previous=REPORT/'summary.json'
    if previous.exists() and not (REPORT/'historical-first-pass-summary.json').exists():
        save(REPORT/'historical-first-pass-summary.json',json.loads(previous.read_text()))
    save(previous,{'status':'complete','qualification_only':True,'production_ready':False,
        'generated_attempts':len(results),'shared_gameplay_pass_seeds':summary['shared_gameplay_pass_seeds'],
        'screen_eligible_locations':len(queue),'authoritative_report':'filtered-summary.json',
        'analysis_errors':[r['seed'] for r in rows if r['error']],
        'notes':'Corrected generated-bound analysis; selected Core variants and full evidence in filtered-summary.json.'})

    shown=[r for r in rows if r['shared_gameplay_pass']][:4]
    if not shown:shown=rows[:4]
    colors=['#c9c7bf','#24473c','#bfdfd0','#779db0']
    if shown:
        fig,axes=plt.subplots(1,len(shown),figsize=(6*len(shown),6.6),layout='constrained')
        for ax,row in zip(np.atleast_1d(axes),shown):
            folder=Path(row['attempt_directory'])
            extracted=json.loads((folder/'surface-sample.json').read_text())
            w,h=extracted['width'],extracted['height'];pixels=np.zeros((h,w),dtype=int)
            for j,sr in enumerate(extracted['rows']):
                for i,c in enumerate(sr):pixels[j,i]=3 if c['actual_surface_water'] else 2 if c['biome']=='minecraft:pale_garden' else 1 if c['biome']=='minecraft:dark_forest' else 0
            b=row['generated_block_bounds'] or [extracted['rows'][0][0]['x']-4,
                extracted['rows'][0][-1]['x']+3,extracted['rows'][0][0]['z']-4,extracted['rows'][-1][0]['z']+3]
            ax.imshow(pixels,cmap=ListedColormap(colors),vmin=0,vmax=3,origin='lower',
                      extent=[b[0],b[1]+1,b[2],b[3]+1],interpolation='nearest')
            ax.scatter(*row['mansion_start'],s=150,marker='*',c='#efb629',edgecolor='black',zorder=5)
            compilation_path=Path(row['source_compilation_path']) if row['source_compilation_path'] else folder/f"compilation-r{row['best_rotation']}.json"
            record=json.loads(compilation_path.read_text()) if compilation_path.exists() else {}
            evidence=record.get('evidence',{})
            for team,xyz in evidence.get('fountains',{}).items():
                color='#ac3a3a' if team=='north' else '#295dce'
                ax.scatter(xyz[0],xyz[2],c=color,s=60,edgecolor='white',zorder=6)
                ax.annotate(team,(xyz[0],xyz[2]),xytext=(4,4),textcoords='offset points',fontsize=8,color=color)
            for team,objs in evidence.get('objective_world_xz',{}).items():
                for kind,pos in objs.items():
                    symbol={'pillager_outpost':'O','nether_bastion':'B','end_spike':'S'}.get(kind,'?')
                    ax.annotate(symbol,pos,ha='center',va='center',fontsize=8,color='#a63131' if team=='north' else '#194cad',
                                bbox={'facecolor':'white','edgecolor':'none','alpha':.65,'pad':1})
            lair=evidence.get('lair_site',{}).get('world_xz')
            if lair:ax.scatter(*lair,marker='D',s=45,c='#7c368a',edgecolor='white',zorder=6)
            ax.invert_yaxis();ax.set_xlabel('World X');ax.set_ylabel('World Z (north at top)')
            ax.set_title(f"Seed {row['seed']} · forest ≈{row['forest_area_proxy']/1000:.0f}k blocks²\n"
                         f"{'Shared checks passed' if row['shared_gameplay_pass'] else 'Current fit rejected'}; "
                         f"rotation {row['best_rotation']}°\nMansion {row['mansion_start']}",fontsize=11)
        fig.suptitle('Extent-first Pale Mansion gameplay shortlist — generated Java 1.21.11 terrain\n'
                     'Star: natural mansion; circles: proposed Fountains; O/B/S: defensive sites; diamond: Lair socket',fontsize=13)
        fig.legend(handles=[Patch(color=colors[2],label='Pale Garden'),Patch(color=colors[1],label='Dark Forest'),
                            Patch(color=colors[0],label='Other land'),Patch(color=colors[3],label='Surface water')],loc='outside lower center',ncol=4)
        fig.savefig(REPORT/'gameplay-shortlist.png',dpi=170);plt.close(fig)

    lines=['# Extent-first Pale Mansion gameplay qualification','','**Prototype/test.** Forest centrality relaxed from '
        '0.75 to 1.0 equivalent forest radii; nearest pale terrain still within 128 blocks of the mansion start. '
        'Connected pale forest area is ranked first. Shared gameplay bounds remain unchanged.','',
        f"Refined {len(bank)} extent-leading candidates; {len(queue)} locations had initial symmetry/land-Homebase screen passes. "
        f"Generated and evaluated {len(results)} selected windows; {len(accepted)} passed the implemented shared checks. "
        'These are geography qualifications, not READY worlds.','',
        '## Filtered candidates','',
        '| Seed | Mansion start X, Z | Connected forest estimate | Current gameplay verdict | Reason if rejected |',
        '|---|---|---|---|---|']
    for r in rows:
        label='Shared checks passed' if r['shared_gameplay_pass'] else 'Analysis error' if r['error'] else 'Current window rejected'
        reason=r['error'] or ', '.join(r['rejection_codes']) or '—'
        lines.append(f"| `{r['seed']}` | {r['mansion_start'][0]}, {r['mansion_start'][1]} | ≈{r['forest_area_proxy']:,} blocks² | {label} | {reason} |")
    lines+=['','A rejected window or fitter configuration is not proof that the entire seed cannot support the Type. '
        'Source worlds and full rejection evidence are retained. Approximate screen holds remain unverified until generated. '
        'The original 311,000-block² standout `880041481` was additionally generated despite its approximate hold: '
        'best actual symmetry ratio 0.2614, above 0.2371, so its tested window fails the current shared symmetry gate. '
        'The 314,000-block² extent leader `880018254` passed actual symmetry on one axis but failed the chosen north Core; '
        'a sampled socket census found no acceptable north socket within that fitter band.', '',
        '## Gameplay checks applied', '',
        '- Contested-land symmetry, using the existing p25 ratio bound of 0.2371, reread on generated substrate heights.',
        '- Two independently acceptable Homebase sockets, minimum quality 0.52, and a compact Hinterland.',
        '- Coexisting, ordered, non-overlapping Outpost → Bastion → Spike → Fountain defensive layout.',
        '- Viable Lair socket with at most 0.15 analytical access disparity.',
        '- Physical Fountain/objective placement, clearance and bounded levelling: at most 10 moved blocks per footprint column.',
        '- Opening ceiling (implemented built-structure checks), qualitative opening floor and resource validity.', '',
        'The Default highland/ocean gradient was recorded diagnostically and not used to reject Pale Forest geography. '
        'Both physical team axes and both team assignments were checked. No shared compiler thresholds were loosened.', '',
        '## Extent and centrality', '',
        'The previous 0.75 centroid-radius limit excluded `880018254` (≈314,000 blocks²) and '
        '`880025303` (≈273,000 blocks²). Both enter under 1.0. The nearest-pale 128-block limit prevents '
        'a large distant forest from standing in for the premise. Candidate windows retain the existing '
        'middle-third mansion-in-map fixture; shifting windows toward forest extent remains a Prototype/test search choice. '
        'Nominal window sizes were 864×1056 and 1296×1584, with transpositions. The saved chunk rectangle is '
        'authoritative for generated bounds and coordinate transforms.', '',
        'Forest extents are eight-block biome-component estimates; the generated-window pale-area counts '
        'are actual sampled surface observations and may include smaller adjacent pale components.', '',
        '## Mansion access and remaining Open questions', '',
        'Mansion approach costs are measured from each accepted Fountain to an 80–112-block approach ring. '
        'They are effective-block surface traversal proxies, not seconds or verified entrance routes. '
        'No Mansion-specific access-disparity bound is established, so these measurements are reported independently '
        'rather than silently copying the Lair bound. For the leading shared-filter survivors, '
        'alternative Core sites were searched within the fitter’s existing legal bands, using the unchanged '
        'quality formula and rerunning every shared check. The selected passing variant minimizes Mansion '
        'approach disparity. Resource observations were reused only for the same unchanged source world '
        'and exact bounds; opening depth, floor and ceiling checks were recomputed for each Core pair.', '',
        '| Passing candidate | Homebase quality north / south | Lair access disparity | Mansion grounds cost disparity (diagnostic) |',
        '|---|---|---|---|']
    for r in rows:
        if not r['shared_gameplay_pass']:continue
        h=r['homeland_quality'];m=r.get('mansion_access') or {};ld=r.get('lair_access_disparity')
        md=m.get('normalized_disparity')
        lines.append(f"| `{r['seed']}` | {h['north']:.3f} / {h['south']:.3f} | {ld:.1%} | {md:.1%} |" if ld is not None and md is not None else f"| `{r['seed']}` | {h['north']:.3f} / {h['south']:.3f} | {ld} | Unmeasured |")
    lines+=['', '**Open:** the Pale Type’s poor visibility/resource poverty versus concentrated central prize relation, '
        'live traversal and entrance accessibility, unevidenced opening exclusions such as carrots/iron equipment sufficiency, '
        'and actual match balance. These have no recovered numerical rejection bounds. No authored objectives, '
        'Lair manifestation, match bindings, pool publication, or READY certification were performed.','',
        '## Evidence and reproduction', '',
        '- [Generated gameplay comparison](gameplay-shortlist.png)',
        '- [Filtered results, diagnostics and bounds](filtered-summary.json)',
        '- [Extent shortlist](extent-shortlist.json)',
        '- [Initial generated-window queue](generation-queue.json)',
        '- `attempts/` contains candidates, surface samples, compilations and rejection records per orientation.', '',
        'Code: `tools/analysis/filter_pale_gameplay.py`, `tools/analysis/report_pale_gameplay.py`, '
        '`tools/analysis/rebalance_pale_homebases.py`, '
        '`implementation/worldgen/native/cubiomes/pale_gameplay_probe.c`. Uses the previously pinned, '
        'official-biome-report-validated Java 1.21.11 Cubiomes model. World generation uses the SHA-1-verified '
        'official 1.21.11 server and Java 21; worlds are isolated under '
        '`artifacts/worldgen/pale_gameplay_2026-10-12/source-worlds/`. Qualification reads source terrain; it does not modify it.','',
        'Run `filter_pale_gameplay.py --top 32 --max-attempts 12 --desired 3`, then '
        '`rebalance_pale_homebases.py` and `report_pale_gameplay.py`. NumPy is required for analysis and Matplotlib for the report. '
        'Saved attempts resume without regeneration. An initial larger-window coordinate mismatch is retained '
        'as a historical analysis-error record; corrected analysis uses actual saved chunk bounds, and '
        'those errors are not geography rejections.']
    (REPORT/'README.md').write_text('\n'.join(lines)+'\n')
    print(json.dumps({'generated_attempts':len(results),'accepted':summary['shared_gameplay_pass_seeds'],
                      'analysis_errors':[r['seed'] for r in rows if r['error']]},indent=2))


if __name__=='__main__':main()
