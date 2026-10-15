"""Extent-first Pale Mansion candidates and shared gameplay qualification.

Prototype/test: forest centrality relaxed from .75 to 1 equivalent radius.
Source terrain is Java 1.21.11 vanilla generation. Qualification is read-only;
no map pool publication, authored structures, or canonical design changes.
"""
from __future__ import annotations
import argparse
from collections import Counter
import hashlib
import json
import math
from pathlib import Path
import shutil
import struct
import subprocess
import sys
import numpy as np

ROOT=Path(__file__).resolve().parents[2]
sys.path.insert(0,str(ROOT/'implementation/worldgen'))
from terrain_harvest import map_compiler as mc, map_types, scoop
from vanilla_search import acquire, DATA_VERSION, GENERATOR, SERVER_SHA1, VERSION
from vanilla_search.evaluate import evaluate_orientation
from vanilla_search.extract import extract_region
from vanilla_search.fit import enrich
from vanilla_search.pipeline import _public
from refine_pale_mansion_search import detailed, label

OLD=ROOT/'implementation/worldgen/reports/pale_mansion_search_2026-10-12'
REPORT=ROOT/'implementation/worldgen/reports/pale_gameplay_2026-10-12'
ART=ROOT/'artifacts/worldgen/pale_gameplay_2026-10-12'
PROBE=ART/'build/pale_gameplay_probe'
JAR=ROOT/'artifacts/worldgen/seed_reevaluation_2026-10-02/build/server-1.21.11.jar'
JAVA=Path('/opt/homebrew/opt/openjdk@21/bin/java')
RUNTIME=ROOT/'artifacts/worldgen/pale_mansion_search_2026-10-12/verification-runtime'
OCEANS={0,10,24,44,45,46,47,48,49,50}


def save(path,value):
    path.parent.mkdir(parents=True,exist_ok=True)
    tmp=path.with_suffix(path.suffix+'.tmp')
    tmp.write_text(json.dumps(value,indent=2)+'\n');tmp.replace(path)


def forests(r,limit=1.):
    return [f for f in r['components'] if f['area_proxy_blocks2']>=100000 and
            f['centroid_offset_in_equivalent_radii']<=limit and f['nearest_distance_blocks']<=128]


def load_rows(path):
    return [json.loads(l) for l in path.read_text().splitlines()] if path.exists() else []


def prepare(top):
    merged={}
    for name in ['refined.jsonl','precision.jsonl','expanded.jsonl']:
        for r in load_rows(OLD/name):merged[(r['seed'],r['x'],r['z'])]=r
    bank=sorted([r for r in merged.values() if forests(r)],
                key=lambda r:forests(r)[0]['area_proxy_blocks2'],reverse=True)
    refined=[]
    for index,r in enumerate(bank[:top]):
        path=REPORT/'precision'/f"{r['seed']}_{r['x']}_{r['z']}.json"
        if path.exists():r=json.loads(path.read_text())
        elif r['step']!=8:
            r,grid=detailed(r['coarse'],step=8,radius=1536)
            save(ART/'biome-grids'/f"{r['seed']}_{r['x']}_{r['z']}.json",grid)
            save(path,r)
        else:save(path,r)
        if forests(r):refined.append(dict(r,selected_forest=forests(r)[0]))
        print(f'extent refine {index+1}/{min(top,len(bank))}: {r["seed"]}',flush=True)
    refined.sort(key=lambda r:r['selected_forest']['area_proxy_blocks2'],reverse=True)
    save(REPORT/'extent-shortlist.json',refined)
    gate=map_types.symmetry_bound()
    records=[]
    for rank,r in enumerate(refined,1):
        path=REPORT/'screens'/f"{r['seed']}_{r['x']}_{r['z']}.json"
        if path.exists():records.append(json.loads(path.read_text()));continue
        windows=screen_windows(r)
        record={'extent_rank':rank,'seed':r['seed'],'mansion_start':[r['x'],r['z']],
                'forest':r['selected_forest'],'windows':windows,
                'screen_pass_windows':sum(w['screen_pass'] for w in windows)}
        save(path,record);records.append(record)
        print(f'screen {rank}/{len(refined)}: {r["seed"]}, {record["screen_pass_windows"]} passing windows',flush=True)
    queue=[]
    for record in records:
        # Keep extent first. Window choice favours retained pale area, then
        # approximation symmetry; no blended interest/fairness score.
        eligible=sorted([w for w in record['windows'] if w['screen_pass'] and
                         (w.get('homebase_probe') or {}).get('frontier')],
                        key=lambda w:(-w['retained_forest_area_proxy'],w['symmetry']['deviation_over_relief']))
        if eligible:queue.append({'seed':record['seed'],'mansion_start':record['mansion_start'],
                                  'extent_rank':record['extent_rank'],'forest':record['forest'],
                                  'window':eligible[0],'alternate_windows':eligible[1:5]})
    save(REPORT/'generation-queue.json',queue)
    save(REPORT/'inputs.json',{'status':'Prototype/test','centrality_old':.75,'centrality_new':1.,
        'nearest_pale_limit_blocks':128,'extent_priority':'largest connected qualifying pale component first',
        'prior_candidate_bank':str(OLD),'bank_relaxed_candidates':len(bank),'refined':len(refined),
        'symmetry_gate':gate,'shared_provisional_constants':mc.PROVISIONAL,
        'map_type':'pale_forest; existing recognizer bypasses Default regional gradient',
        'writing':'Java 1.21.11 normal vanilla source generation only; read-only gameplay qualification',
        'bounds_sizes':[[864,1056],[1296,1584]],'approximate_screen_note':
        'Approximate heights rank/select generation candidates; final symmetry is reread on generated terrain. Screen failures are not certified terrain rejections.',
        'compiler_source_sha256':hashlib.sha256((ROOT/'implementation/worldgen/terrain_harvest/map_compiler.py').read_bytes()).hexdigest(),
        'server_jar_sha1':hashlib.sha1(JAR.read_bytes()).hexdigest(),'top_budget':top})
    return queue


def screen_windows(r):
    half,step=1536,16;n=2*half//step;x,z=r['x'],r['z']
    path=ART/'probes'/f'{r["seed"]}_{x}_{z}.bin';path.parent.mkdir(parents=True,exist_ok=True)
    if not path.exists():subprocess.run([str(PROBE),str(r['seed']),str(x),str(z),str(half),str(step),str(path)],check=True)
    with path.open('rb') as fh:
        header=fh.readline().decode().split();assert header[:2]==['palegameplay','1']
        values=struct.unpack(f'<{n*n*2}h',fh.read())
    height=list(values[::2]);biomes=list(values[1::2]);forest=r['selected_forest']
    fx,fz=forest['centroid'];centres=[(x,z),((x+fx)/2,(z+fz)/2),(fx,fz)]
    centres.extend(((x+fx)/2+dx,(z+fz)/2+dz) for dx,dz in [(96,0),(-96,0),(0,96),(0,-96)])
    centres=sorted({(round(a/16)*16,round(b/16)*16) for a,b in centres})
    sizes=[(864,1056),(1056,864),(1296,1584),(1584,1296)]
    # The selected component's bbox is not filled forest. Retained area below
    # is measured from the actual pale samples, not bbox intersection area.
    precision_path=ART/'biome-grids'/f'{r["seed"]}_{x}_{z}.json'
    if precision_path.exists():fg=json.loads(precision_path.read_text())
    else:
        folder='expanded-grids' if r['radius']>1536 else 'precision-grids'
        fg=json.loads((OLD/folder/f'{r["seed"]}_{x}_{z}.json').read_text())
    fn=fg['n'];fs=r['step'];fr=r['radius']
    component_grid,count=label(np.array(fg['biomes']).reshape(fn,fn)==186)
    areas=np.bincount(component_grid.ravel())
    ids=[k for k in range(1,count+1) if areas[k]*fs*fs==forest['area_proxy_blocks2']]
    assert ids, 'selected forest must be recoverable from its preserved sample grid'
    def centroid_error(k):
        yy,xx=np.where(component_grid==k)
        return math.dist([x-fr+float(xx.mean())*fs,z-fr+float(yy.mean())*fs],forest['centroid'])
    selected_component=min(ids,key=centroid_error)
    yy,xx=np.where(component_grid==selected_component)
    pale_points=list(zip((x-fr+xx*fs).tolist(),(z-fr+yy*fs).tolist()))
    bound=map_types.symmetry_bound()['value'];windows=[]
    for cx,cz in centres:
        for w,d in sizes:
            # Retain the existing middle-third mansion-in-map fixture.
            if abs(x-cx)>w*.17 or abs(z-cz)>d*.17:continue
            x0,z0=cx-w//2,cz-d//2;cw,cd=w//step,d//step
            # 1296 and 1584 are both exactly divisible by 16.
            assert w%step==0 and d%step==0
            i0,j0=(x0-(x-half))//step,(z0-(z-half))//step
            if min(i0,j0)<0 or i0+cw>n or j0+cd>n:continue
            sub=[height[(j0+jj)*n+i0+ii] for jj in range(cd) for ii in range(cw)]
            bio=[biomes[(j0+jj)*n+i0+ii] for jj in range(cd) for ii in range(cw)]
            land=[b not in OCEANS for b in bio]
            playable=[max(63,h) for h in sub]
            bounds=[x0,x0+w-1,z0,z0+d-1]
            retained=sum(x0<=px<x0+w and z0<=pz<z0+d for px,pz in pale_points)*fs*fs
            for axis in ['z','x']:
                sym=scoop.symmetry(playable,cw,cd,axis,spacing_hint=step,contested=land)
                passed=sym.get('contested_pairs',0)>0 and sym.get('deviation_over_relief',math.inf)<=bound
                windows.append({'centre':[cx,cz],'size':[w,d],'block_bounds':bounds,
                    'chunk_bounds':[x0//16,(x0+w-1)//16,z0//16,(z0+d-1)//16],
                    'team_axis':axis,'symmetry':sym,'pale_share':bio.count(186)/len(bio),
                    'water_biome_share':sum(not v for v in land)/len(land),
                    'retained_forest_area_proxy':retained,'retained_forest_fraction':retained/forest['area_proxy_blocks2'],
                    'retention_note':'samples belonging to the selected connected forest only',
                    'screen_pass':passed,'rejections':[] if passed else ['APPROXIMATE_SYMMETRY_HOLD']})
    # Flatness is diagnostic, not an independently acceptable Core proof.
    for item in sorted([w for w in windows if w['screen_pass']],
                       key=lambda w:(-w['retained_forest_area_proxy'],w['symmetry']['deviation_over_relief']))[:6]:
        x0,_,z0,_=item['block_bounds'];w,d=item['size'];cw,cd=w//step,d//step
        i0,j0=(x0-(x-half))//step,(z0-(z-half))//step
        sub=[max(63,height[(j0+jj)*n+i0+ii]) for jj in range(cd) for ii in range(cw)]
        land=[biomes[(j0+jj)*n+i0+ii] not in OCEANS for jj in range(cd) for ii in range(cw)]
        item['homebase_probe']=scoop.homebase_pair(sub,cw,cd,item['team_axis'],spacing=step,
                                                 land_mask=land,stride=4)
        if not item['homebase_probe']['frontier']:
            item['screen_pass']=False;item['rejections'].append('NO_LAND_HOMEBASE_PROBE_PAIR')
    return windows


def candidate_from(extracted,job,rotation,world):
    c=evaluate_orientation(extracted,rotation,False,fit_routes=False)
    if c is None:return None
    enrich(c)
    chunks=job['window']['chunk_bounds']
    # Odd chunk counts put a rectangle centre eight blocks off the chunk
    # lattice. The saved chunk rectangle is authoritative for all coordinate
    # transforms; never reuse nominal pre-generation block bounds here.
    b=[chunks[0]*16,(chunks[1]+1)*16-1,chunks[2]*16,(chunks[3]+1)*16-1]
    assert extracted['width']*8==b[1]-b[0]+1
    assert extracted['height']*8==b[3]-b[2]+1
    c.update({'schema_version':1,'generator':GENERATOR,'minecraft_java_version':VERSION,
        'data_version':DATA_VERSION,'worldgen_truth':{'source':'official Mojang dedicated server',
            'server_jar_sha1':SERVER_SHA1,'verified':True,'terrain_modified':False},
        'seed':job['seed'],'region':{'chunk_bounds':chunks,'block_bounds':b},
        'actual_structures':extracted['structures'],'actual_block_volume_presence':extracted['block_volume_presence'],
        'data_completeness':{'full_chunks':extracted['full_chunks'],'expected_chunks':extracted['expected_chunks']},
        'local_world_directory':str(world),'prospect_types':['pale_forest','central_mansion'],
        'pale_search':{'forest':job['forest'],'mansion_start':job['mansion_start'],'centrality_limit':1.}})
    return _public(c)


def qualify(job,index):
    seed=job['seed'];folder=REPORT/'attempts'/f'{index:02d}_{seed}'
    status_path=folder/'result.json'
    if status_path.exists():
        previous=json.loads(status_path.read_text())
        if previous.get('error')!='ValueError: orientation/grid dimensions disagree':return previous
        save(folder/'historical-analysis-alignment-error.json',previous)
    root=ART/'source-worlds'/f'{index:02d}_{seed}'/'server'
    try:
        acquire.verify_server_jar(JAR)
        if not (root/'world/level.dat').exists():
            old=ROOT/'artifacts/worldgen/pale_mansion_search_2026-10-12/verification-worlds'/str(seed)/'world'
            if old.exists():shutil.copytree(old,root/'world')
        if (folder/'surface-sample.json').exists() and (root/'world/level.dat').exists():
            world=root/'world';timing={'reused_preserved_generation':True}
            extracted=json.loads((folder/'surface-sample.json').read_text())
        else:
            world,timing=acquire.generate_world(seed,JAR,JAVA,root,RUNTIME,
                tuple(job['window']['chunk_bounds']),heap='2G',
                progress=lambda n,total:print(f'generate {seed}: {n}/{total}',flush=True))
            extracted=extract_region(world,tuple(job['window']['chunk_bounds']),ground_scan=True)
            save(folder/'surface-sample.json',extracted)
        actual_mansions=[s for s in extracted['structures'] if 'mansion' in s['type']]
        target_chunk=[job['mansion_start'][0]//16,job['mansion_start'][1]//16]
        target_exists=any(s['chunk']==target_chunk for s in actual_mansions)
        heights=[s['terrain_y'] for row in extracted['rows'] for s in row]
        land=[not s['actual_surface_water'] for row in extracted['rows'] for s in row]
        width,depth=extracted['width'],extracted['height'];runs=[]
        for rotation in [0,90,180,270]:
            c=candidate_from(extracted,job,rotation,world)
            if c is None:
                runs.append({'rotation':rotation,'status':'reject','rejections':[{'code':'NO_ORIENTATION_HOME_SITES'}]});continue
            axis='z' if rotation in [0,180] else 'x'
            sym=scoop.symmetry([max(63,h) for h in heights],width,depth,axis,
                               spacing_hint=8,contested=land)
            # Reverse halves for reversed orientation so the existing
            # normalization is read for both team assignments.
            if rotation in [180,270]:
                from vanilla_search.evaluate import orient
                rr=orient(extracted['rows'],rotation,False)
                hh=[max(63,s['terrain_y']) for row in rr for s in row]
                ll=[not s['actual_surface_water'] for row in rr for s in row]
                sym=scoop.symmetry(hh,len(rr[0]),len(rr),'z',spacing_hint=8,contested=ll)
            save(folder/f'candidate-r{rotation}.json',c)
            if sym.get('deviation_over_relief',math.inf)>map_types.symmetry_bound()['value']:
                runs.append({'rotation':rotation,'status':'reject','actual_symmetry':sym,
                    'rejections':[{'stage':'symmetry','code':'GENERATED_SYMMETRY_OUTSIDE_BOUND'}]});continue
            out=mc.compile_candidate(c,world)
            record=out.as_dict();record.update(rotation=rotation,actual_symmetry=sym,
                status='shared_checks_pass' if not out.rejections else 'reject')
            # Runtime/authoring readiness is not part of read-only filtering.
            record['ready']=False;record['playable']=False
            record['qualification_only']=True
            save(folder/f'compilation-r{rotation}.json',record);runs.append(record)
            print(f'compile {seed} r{rotation}: {out.reached}, {[r.code for r in out.rejections]}',flush=True)
        cells=[c for row in extracted['rows'] for c in row]
        result={'seed':seed,'job':job,'world':str(world),'timing':timing,
            'actual_target_mansion_exists':target_exists,'mansion_starts':actual_mansions,
            'generated_block_bounds':[job['window']['chunk_bounds'][0]*16,
                (job['window']['chunk_bounds'][1]+1)*16-1,job['window']['chunk_bounds'][2]*16,
                (job['window']['chunk_bounds'][3]+1)*16-1],
            'actual_pale_area_in_window_proxy':sum(c['biome']=='minecraft:pale_garden' for c in cells)*64,
            'actual_water_fraction':sum(c['actual_surface_water'] for c in cells)/len(cells),
            'runs':runs,'shared_gameplay_pass':target_exists and any(r['status']=='shared_checks_pass' for r in runs),
            'production_ready':False,'note':'Read-only shared checks; no authored map or runtime readiness certification.'}
    except Exception as exc:
        result={'seed':seed,'job':job,'error':f'{type(exc).__name__}: {exc}',
                'shared_gameplay_pass':False,'production_ready':False}
    save(status_path,result);return result


def main():
    p=argparse.ArgumentParser();p.add_argument('--top',type=int,default=32)
    p.add_argument('--max-attempts',type=int,default=8)
    p.add_argument('--desired',type=int,default=3)
    p.add_argument('--prepare-only',action='store_true');a=p.parse_args()
    REPORT.mkdir(parents=True,exist_ok=True)
    queue=prepare(a.top)
    if a.prepare_only:return
    results=[]
    for i,job in enumerate(queue[:a.max_attempts],1):
        r=qualify(job,i);results.append(r)
        summary={'status':'running','attempts':results,
            'shared_gameplay_pass_seeds':[r['seed'] for r in results if r['shared_gameplay_pass']],
            'screen_eligible_locations':len(queue),'desired':a.desired,
            'qualification_only':True,'production_ready':False}
        save(REPORT/'summary.json',summary)
        print(f'qualified {r["seed"]}: {r["shared_gameplay_pass"]}, error={r.get("error")}',flush=True)
        if len(summary['shared_gameplay_pass_seeds'])>=a.desired:break
    summary['status']='complete';save(REPORT/'summary.json',summary)


if __name__=='__main__':main()
