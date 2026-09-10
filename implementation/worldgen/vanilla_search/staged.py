"""Native breadth, selective official generation, analytical finalist fit."""
from __future__ import annotations
import argparse
import hashlib
import json
import math
import resource
import subprocess
import time
from pathlib import Path

from . import VERSION, DATA_VERSION, SERVER_SHA1
from .acquire import verify_server_jar, prepare_runtime, generate_world
from .extract import extract_region
from .evaluate import evaluate_orientation
from .pipeline import _public, _configure_world
from .render import render
from .fit import enrich, greybox, presentation

ROOT=Path(__file__).resolve().parents[3]
PIN='e61f90580cbdd883214a8054670dacae655e59c0'

def save(path, value):
    path.parent.mkdir(parents=True,exist_ok=True)
    path.write_text(json.dumps(value,indent=2)+'\n')

def native(build):
    source=build/'cubiomes'
    if not source.exists():
        subprocess.run(['git','clone','https://github.com/Cubitect/cubiomes.git',str(source)],check=True)
        subprocess.run(['git','-C',str(source),'checkout',PIN],check=True)
    actual=subprocess.check_output(['git','-C',str(source),'rev-parse','HEAD'],text=True).strip()
    if actual!=PIN: raise ValueError('Cubiomes revision differs from pinned source')
    subprocess.run(['make','-C',str(source),'-j4'],check=True,stdout=subprocess.DEVNULL)
    binary=build/'coarse'
    subprocess.run(['cc','-O3','-fwrapv','-I',str(source),str(Path(__file__).with_name('coarse.c')),str(source/'libcubiomes.a'),'-lm','-o',str(binary)],check=True)
    return binary

def select(records,count):
    # Keep a quality pool, then farthest-first composition diversity. One seed each.
    def quality(r):
        eo,wh,cold,op,forest,hn,hs,center=r['features']
        # Soft bottleneck ranking prevents one abundant feature buying away an
        # absent landscape role. Values are search heuristics, not design rules.
        balance=min(1,eo/.25,wh/.20,cold/.06,op/.18,forest/.08,hn/.60,hs/.60)
        return 40*balance+r['score']
    ranked=sorted(records,key=quality,reverse=True)
    pool=ranked[:max(80,len(ranked)//100)]
    selected=pool[:1]
    scales=[.4,.3,.2,.3,.3,.5,.5,.4]
    while len(selected)<count:
        options=[r for r in pool if all(r['seed']!=s['seed'] for s in selected)]
        if not options: raise ValueError('not enough distinct seeds in quality pool')
        def value(r):
            distance=min(math.sqrt(sum(((a-b)/scale)**2 for a,b,scale in zip(r['features'],s['features'],scales))) for s in selected)
            return distance+.6*quality(r)/max(1,quality(ranked[0]))
        selected.append(max(options,key=value))
    return selected

def main():
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument('--server-jar',type=Path,required=True)
    p.add_argument('--java',type=Path,required=True)
    p.add_argument('--accept-eula',action='store_true')
    p.add_argument('--seed',type=int,default=930001000)
    p.add_argument('--seeds',type=int,default=20000)
    p.add_argument('--generate',type=int,default=8)
    p.add_argument('--out',type=Path,default=ROOT/'implementation/worldgen/results/staged_default_2026-09-09')
    p.add_argument('--artifacts',type=Path,default=ROOT/'artifacts/worldgen/staged_default_2026-09-09')
    p.add_argument('--screen-only',action='store_true')
    p.add_argument('--supplemental-queue',type=Path)
    p.add_argument('--review-exclusions',type=Path)
    args=p.parse_args()
    if not 1<=args.generate<=16: p.error('select 1–16 official survivors')
    if not args.accept_eula and not args.screen_only: p.error('--accept-eula required')
    out=args.out.resolve(); bulk=args.artifacts.resolve(); build=bulk/'build'
    out.mkdir(parents=True,exist_ok=True); build.mkdir(parents=True,exist_ok=True)
    binary=native(build)
    config={'seed':args.seed,'seeds':args.seeds,'cubiomes_commit':PIN,'window_dimensions':[864,1056]}
    config_path=out/'search_config.json'
    if config_path.exists() and json.loads(config_path.read_text())!=config:
        raise ValueError('existing output has different search configuration; choose another --out')
    save(config_path,config)
    if not (out/'screen_summary.json').exists():
        print('native screening started',flush=True)
        with (out/'structural_survivors.jsonl').open('w') as dest:
            run=subprocess.run([str(binary),str(args.seed),str(args.seeds)],stdout=dest,stderr=subprocess.PIPE,text=True,check=True)
        save(out/'screen_summary.json',json.loads(run.stderr))
    records=[json.loads(line) for line in (out/'structural_survivors.jsonl').read_text().splitlines()]
    chosen=select(records,args.generate)
    if args.supplemental_queue:
        extra=json.loads(args.supplemental_queue.read_text())
        known={(r['seed'],tuple(r['center']),r['rotation']) for r in records}
        if any((r['seed'],tuple(r['center']),r['rotation']) not in known for r in extra): raise ValueError('supplements must be existing B survivors')
        chosen+=extra
        if len(chosen)>16 or len({r['seed'] for r in chosen})!=len(chosen): raise ValueError('generation budget or seed uniqueness violated')
    exclusions=json.loads(args.review_exclusions.read_text()) if args.review_exclusions else {}
    if (out/'search_summary.json').exists():
        previous=json.loads((out/'search_summary.json').read_text())
        previous_seeds={c['seed'] for c in previous['generated']}
        if not previous_seeds<={r['seed'] for r in chosen}:
            raise ValueError('completed output includes additional generated worlds; pass its supplemental queue or choose another output')
        if previous.get('review_exclusions') and not args.review_exclusions:
            raise ValueError('pass the existing review exclusions explicitly; do not silently revive excluded candidates')
    save(out/'generation_queue.json',chosen)
    print(json.dumps(json.loads((out/'screen_summary.json').read_text())),flush=True)
    if args.screen_only: return
    verify_server_jar(args.server_jar)
    prepare_runtime(args.server_jar.resolve(),args.java.resolve(),build/'runtime')
    candidates=[]
    for record in chosen:
        seed=record['seed']; cx,cz=record['center']; key=f'{seed}_{cx}_{cz}'
        work=build/key; world=work/'world'; checkpoint=build/(key+'.extracted.json')
        bounds=[cx//16-27,cx//16+26,cz//16-33,cz//16+32]
        timing_path=out/'acquisition'/f'{key}.json'
        if not timing_path.exists():
            if work.exists(): raise RuntimeError(f'incomplete acquisition at {work}; inspect before retry')
            print(f'official generation {key}',flush=True)
            usage=resource.getrusage(resource.RUSAGE_CHILDREN)
            world,timing=generate_world(seed,args.server_jar.resolve(),args.java.resolve(),work,build/'runtime',bounds,args.accept_eula,lambda i,n: print(f'{key} batch {i}/{n}',flush=True) if i%5==0 or i==n else None)
            after=resource.getrusage(resource.RUSAGE_CHILDREN)
            timing['server_cpu_seconds']=round(after.ru_utime+after.ru_stime-usage.ru_utime-usage.ru_stime,3)
            save(timing_path,timing)
        timing=json.loads(timing_path.read_text())
        extracted=json.loads(checkpoint.read_text()) if checkpoint.exists() else None
        if not extracted or not extracted.get('ground_scan'):
            extracted=extract_region(world,bounds,ground_scan=True); save(checkpoint,extracted)
        started=time.perf_counter()
        # Axis chosen cheaply; inspect all four axes using real surface evidence.
        alternatives=[]
        for rotation in (0,90,180,270):
            c=evaluate_orientation(extracted,rotation,False,fit_routes=False)
            if c:
                enrich(c); alternatives.append(c)
        candidate=max(alternatives,key=lambda c:c['stage_c']['score'])
        candidate.update({'seed':seed,'schema_version':2,'generator':'staged_vanilla_spec1','minecraft_java_version':VERSION,'data_version':DATA_VERSION,'worldgen_truth':{'source':'official Mojang dedicated server','server_jar_sha1':SERVER_SHA1,'terrain_modified':False},'region':{'source_center':[cx,cz],'chunk_bounds':bounds,'block_bounds':[bounds[0]*16,bounds[1]*16+15,bounds[2]*16,bounds[3]*16+15]},'actual_structures':extracted['structures'],'actual_block_volume_presence':extracted['block_volume_presence'],'data_completeness':{'full_chunks':extracted['full_chunks'],'expected_chunks':extracted['expected_chunks'],'ores':'deferred','entities':'deferred','caves_and_fluids':'section palette only; entrances manual'},'cheap_proxy':record,'timing':{**timing,'extraction_seconds':extracted['extraction_seconds']},'local_world_directory':str(world),'orientation_alternatives':[{'orientation':a['orientation'],'metrics':a['stage_c']} for a in alternatives]})
        candidate['timing']['stage_c_analysis_seconds']=round(time.perf_counter()-started,3)
        candidates.append(candidate)
        print(f"actual {key}: {candidate['stage_c']}",flush=True)
    # No silent padding: failed substrate candidates stay in the comparison only.
    finalists=[c for c in candidates if not c['stage_c']['hard_failures'] and str(c['seed']) not in exclusions]
    for c in candidates:
        if str(c['seed']) in exclusions:
            c['review_exclusion']=exclusions[str(c['seed'])]
            old=out/'finalists'/str(c['seed']); archived=out/'review_excluded'/str(c['seed'])
            if old.exists() and not archived.exists(): archived.parent.mkdir(parents=True,exist_ok=True);old.rename(archived)
    for c in finalists:
        started=time.perf_counter(); greybox(c)
        c['timing']['stage_d_fit_seconds']=round(time.perf_counter()-started,3)
        world=Path(c['local_world_directory']); destination=bulk/'worlds'/f"Default_{c['seed']}_{c['region']['source_center'][0]}_{c['region']['source_center'][1]}"
        destination.parent.mkdir(parents=True,exist_ok=True)
        if not destination.exists():
            # Keep checkpoint path valid on resumptions, without duplicating worlds.
            world.rename(destination); world.symlink_to(destination,target_is_directory=True)
        c['local_world_directory']=str(destination)
        c['inspection_spawn']=_configure_world(destination,destination.name,c)
        d=out/'finalists'/str(c['seed']); render(c,d); presentation(c,d)
        save(d/'candidate.json',_public(c))
        save(destination/'GREYBOX.json',c['greybox'])
    summary={'screen':json.loads((out/'screen_summary.json').read_text()),'review_exclusions':exclusions,'full_generation_count':len(candidates),'finalist_count':len(finalists),'finalist_seeds':[c['seed'] for c in finalists],'generated':[{'seed':c['seed'],'region':c['region'],'orientation':c['orientation'],'stage_c':c['stage_c'],'timing':c['timing'],'retained':c in finalists} for c in candidates]}
    save(out/'search_summary.json',summary)
    print(f'Completed {len(finalists)} finalists: {out}',flush=True)
