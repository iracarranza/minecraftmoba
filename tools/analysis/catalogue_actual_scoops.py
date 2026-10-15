"""Read-only Default-oriented catalogue of cached vanilla surface windows.

Prototype/test catalogue only. Rejections and complete measurements survive;
partial Homebase/recognition passes are never map qualification.
"""
from __future__ import annotations

import argparse
from collections import Counter
import errno
import fcntl
from functools import lru_cache
import hashlib
import json
import math
import os
from pathlib import Path
import re
import subprocess
import sys
import time

from filter_pale_gameplay import ROOT, save, evaluate_orientation, enrich, _public, mc, scoop, map_types
from vanilla_search import task_a
from default_region_measurements import measure_without_home_pair

OUT = ROOT / 'implementation/worldgen/reports/default_actual_catalogue_2026-10-14'
SCHEMA = 1
USER_REJECTED = {890000020, 890001592}
UNKNOWN = ['defensive placement and all-nine-anchor 175-block spacing',
           'hinterland and Route fit', 'exact physical block-column fit',
           'Lair fit, access and discoverability', 'resources and opening ceiling',
           'manual strategic balance and runtime readiness']


def append(path, value):
    with path.open('a') as handle:
        handle.write(json.dumps(value, separators=(',', ':'), allow_nan=False) + '\n')


def process_state(pid):
    if not isinstance(pid,int) or pid < 1:
        return 'missing'
    try:
        os.kill(pid,0)
        return 'alive'
    except OSError as error:
        # EPERM is a live PID protected by the sandbox, never permission to duplicate it.
        return 'permission_protected' if error.errno==errno.EPERM else 'missing' if error.errno==errno.ESRCH else 'unknown'


def worker_status():
    OUT.mkdir(parents=True,exist_ok=True)
    metadata_path=OUT/'worker.json'; checkpoint_path=OUT/'checkpoint.json'
    metadata=json.loads(metadata_path.read_text()) if metadata_path.exists() else {}
    checkpoint=json.loads(checkpoint_path.read_text()) if checkpoint_path.exists() else {}
    pid=metadata.get('pid') or checkpoint.get('worker_pid')
    if pid is None and (OUT/'worker.pid').exists():
        pid=int((OUT/'worker.pid').read_text().strip())
    with (OUT/'worker.lock').open('a') as guard:
        try:
            fcntl.flock(guard,fcntl.LOCK_EX|fcntl.LOCK_NB)
            locked=False
            fcntl.flock(guard,fcntl.LOCK_UN)
        except BlockingIOError:
            locked=True
    process=process_state(pid)
    active=locked or process in ('alive','permission_protected','unknown')
    complete=checkpoint.get('status')=='complete'
    return {'pid':pid,'process_state':process,'lock_held':locked,'active_or_protected':active,
            'checkpoint':str(checkpoint_path),'next_job':checkpoint.get('next_job',0),
            'status':checkpoint.get('status'),'stop_requested':(OUT/'STOP').exists(),
            'legacy_worker_without_lock':active and not locked,
            'catalogue_complete':complete,
            'safe_to_start':not active and not (OUT/'STOP').exists() and not complete}


def acquire_worker():
    """Guard new writers and bridge the already-running pre-lock worker by PID."""
    OUT.mkdir(parents=True,exist_ok=True)
    guard=(OUT/'worker.lock').open('a')
    try:
        fcntl.flock(guard,fcntl.LOCK_EX|fcntl.LOCK_NB)
    except BlockingIOError:
        guard.close(); return None,'worker_lock_held'
    metadata_path=OUT/'worker.json'
    metadata=json.loads(metadata_path.read_text()) if metadata_path.exists() else {}
    legacy_pid=metadata.get('pid')
    if legacy_pid is None and (OUT/'worker.pid').exists():
        legacy_pid=int((OUT/'worker.pid').read_text().strip())
    if legacy_pid != os.getpid() and process_state(legacy_pid) in ('alive','permission_protected','unknown'):
        guard.close(); return None,'existing_worker_pid_alive_or_protected'
    metadata={'pid':os.getpid(),'command':[sys.executable,str(Path(__file__).resolve()),*sys.argv[1:]],
              'started_unix':time.time(),'checkpoint':str(OUT/'checkpoint.json'),
              'manifest':str(OUT/'manifest.json'),'lock':str(OUT/'worker.lock'),
              'legacy_worker_without_lock':False,'status':'running'}
    save(metadata_path,metadata); (OUT/'worker.pid').write_text(str(os.getpid())+'\n')
    return guard,None


def seed_of(path):
    candidates = re.findall(r'\d{6,}', path.parent.name)
    if not candidates:
        candidates = re.findall(r'\d{6,}', path.name)
    return int(candidates[-1]) if candidates else None


def discover():
    """Only explicit extracted grids, never approximate probes or generated outputs."""
    base = ROOT / 'implementation/worldgen/reports'
    paths = sorted(set(base.rglob('surface.json')) | set(base.rglob('surface-sample.json')) |
                   set((base / 'pale_mansion_search_2026-10-12').glob('verified-*-surface.json')))
    records = []; seen = set()
    for path in paths:
        seed = seed_of(path)
        if seed is None:
            continue
        ex = json.loads(path.read_text())
        if not isinstance(ex,dict) or not ex.get('rows') or not ex.get('full_chunks') or ex.get('full_chunks') != ex.get('expected_chunks'):
            continue
        rows = ex['rows']; spacing = rows[0][1]['x'] - rows[0][0]['x']
        if spacing != 8:
            continue
        bounds = [rows[0][0]['x']-4, rows[0][-1]['x']+3,
                  rows[0][0]['z']-4, rows[-1][0]['z']+3]
        key = (seed, *bounds)
        if key in seen:
            continue
        seen.add(key)
        records.append({'seed': seed, 'source_surface': str(path), 'bounds': bounds,
                        'width': ex['width'], 'height': ex['height'],
                        'source_sha256': hashlib.sha256(path.read_bytes()).hexdigest(),
                        'user_rejected_seed': seed in USER_REJECTED})
    # Interleave maritime and inland source families instead of exhausting one seed.
    records.sort(key=lambda r: (r['seed'] % 11, r['seed'], -r['width']*r['height']))
    return records


def make_manifest():
    sources = discover(); windows = []; seen = set()
    for index, source in enumerate(sources):
        w, h = source['width'], source['height']
        sizes = {(108,132), (132,108), (128,128), (160,160), (192,192),
                 (224,224), (256,256), (128,192), (192,128), (w,h)}
        for nw, nh in sorted(sizes):
            if nw > w or nh > h:
                continue
            xs = sorted({round((w-nw)*f/2)*2 for f in (0,.25,.5,.75,1)})
            zs = sorted({round((h-nh)*f/2)*2 for f in (0,.25,.5,.75,1)})
            for j in zs:
                for i in xs:
                    bounds = [source['bounds'][0]+i*8, source['bounds'][0]+(i+nw)*8-1,
                              source['bounds'][2]+j*8, source['bounds'][2]+(j+nh)*8-1]
                    key = (source['seed'], *bounds)
                    if key in seen:
                        continue
                    seen.add(key)
                    for rotation in (0,90,180,270):
                        for reflected in (False,True):
                            identity = json.dumps([source['seed'],bounds,rotation,reflected], separators=(',',':'))
                            windows.append({'source_index': index, 'seed': source['seed'], 'bounds': bounds,
                                            'crop_samples': [i,j,nw,nh], 'rotation': rotation,
                                            'reflected': reflected,
                                            'id': hashlib.sha256(identity.encode()).hexdigest()[:20]})
    # Broad catalogue first: deterministic shuffle disperses seeds, sizes and axes.
    windows.sort(key=lambda r: r['id'])
    return {'schema_version': SCHEMA, 'sources': sources, 'jobs': windows,
            'source_count': len(sources), 'distinct_seeds': len({s['seed'] for s in sources}),
            'job_count': len(windows), 'scope': 'actual vanilla surface grids; Default regional contract, independent Homebase partial stage; all rejects retained'}


@lru_cache(maxsize=2)
def load_source(path):
    return json.loads(Path(path).read_text())


def compile_partial(job, source, compiler_hash):
    started = time.time(); ex = load_source(source['source_surface'])
    i,j,nw,nh = job['crop_samples']; rows = [row[i:i+nw] for row in ex['rows'][j:j+nh]]
    crop = {**ex, 'rows': rows, 'width': nw, 'height': nh}
    result = {**job, 'schema_version': SCHEMA, 'source_surface': source['source_surface'],
              'source_sha256': source['source_sha256'], 'compiler_sha256': compiler_hash,
              'status': 'actual_partial_default_compilation', 'qualified': False,
              'ready': False, 'world_written': False, 'unknown_checks': UNKNOWN,
              'user_rejected_seed': source['user_rejected_seed'],
              'recommended_bank_eligible': False, 'partial_front_end_pass': False}
    candidate = evaluate_orientation(crop, job['rotation'], job['reflected'], fit_routes=False)
    if candidate is None:
        raw=measure_without_home_pair(crop,job['rotation'],job['reflected'])
        result.update(raw, rejections=['NO_COARSE_HOME_PAIR'], elapsed_seconds=round(time.time()-started,3))
        return result
    enrich(candidate)
    candidate.update(seed=job['seed'], prospect_types=['default'],
                     region={'block_bounds': job['bounds'],
                             'chunk_bounds': [job['bounds'][0]//16,job['bounds'][1]//16,
                                              job['bounds'][2]//16,job['bounds'][3]//16]})
    recognition = mc.Compilation(seed=job['seed'], candidate=candidate)
    mc.recognize(candidate, recognition)
    # Homebase is assessed independently even if Default's regional shape fails.
    committed = _public(candidate); terrain = task_a.Terrain(committed); homes = {}
    for team in ('north','south'):
        fitted = task_a.fit_homeland(terrain, team)
        if fitted:
            homes[team] = fitted[0]
    homecheck = mc.Compilation(seed=job['seed'], candidate=committed)
    mc.homebase({'homelands': homes}, homecheck)
    fountains = homecheck.evidence.get('fountains', {})
    separation = None
    if len(fountains) == 2:
        separation = math.dist([fountains['north'][0],fountains['north'][2]],
                               [fountains['south'][0],fountains['south'][2]])
    height = [max(63,v) for v in terrain.v['height']]
    symmetry = scoop.symmetry(height, terrain.w, terrain.h, 'z', spacing_hint=8,
                              contested=[not value for value in terrain.v['water']])
    deviation = symmetry.get('deviation_over_relief')
    symmetry_pass = deviation is not None and math.isfinite(deviation) and deviation <= map_types.symmetry_bound()['value']
    # JSON cannot preserve Infinity; record explicitly unavailable degenerate values.
    symmetry = {k: (None if isinstance(v,float) and not math.isfinite(v) else v) for k,v in symmetry.items()}
    regional_pass = not recognition.rejections; home_pass = not homecheck.rejections and len(fountains)==2
    spacing_pass = separation is not None and separation >= 175
    result.update(metrics=candidate['metrics'], stage_c=candidate['stage_c'],
                  experimental_screen=candidate['experimental_screen'],
                  default_regional_pass=regional_pass, homebase_partial_pass=home_pass,
                  recognition_rejections=[r.as_dict() for r in recognition.rejections],
                  homebase_rejections=[r.as_dict() for r in homecheck.rejections],
                  fitted_homelands=homes, fountains=fountains,
                  fountain_horizontal_separation=separation,
                  fountain_pair_minimum_175_pass=spacing_pass,
                  actual_symmetry=symmetry, actual_symmetry_pass=symmetry_pass,
                  partial_front_end_pass=regional_pass and home_pass and spacing_pass and symmetry_pass,
                  elapsed_seconds=round(time.time()-started,3))
    result['recommended_bank_eligible'] = result['partial_front_end_pass'] and not source['user_rejected_seed']
    result['rejections'] = ([r['code'] for r in result['recognition_rejections']] +
                            [r['code'] for r in result['homebase_rejections']] +
                            ([] if spacing_pass else ['FOUNTAIN_PAIR_175_UNPROVEN']) +
                            ([] if symmetry_pass else ['ACTUAL_SYMMETRY_BOUND']))
    return result


def main():
    parser = argparse.ArgumentParser(); parser.add_argument('--hours', type=float, default=4.5)
    parser.add_argument('--max-jobs', type=int, default=1000000)
    parser.add_argument('--prepare-only', action='store_true')
    parser.add_argument('--backfill-regional',action='store_true',help='Append missing Homebase-independent vectors in a sidecar; original observations stay intact.')
    parser.add_argument('--status',action='store_true')
    parser.add_argument('--ensure',action='store_true',help='Keep a live worker, or resume detached if safe.')
    args = parser.parse_args()
    if args.backfill_regional:
        print(json.dumps(backfill_regional())); return
    if args.status:
        print(json.dumps(worker_status())); return
    if args.ensure:
        status=worker_status()
        if not status['safe_to_start']:
            print(json.dumps({'action':'kept_existing_or_stopped','worker':status})); return
        command=[sys.executable,'-u',str(Path(__file__).resolve()),'--hours',str(args.hours),'--max-jobs',str(args.max_jobs)]
        with (OUT/'run.log').open('a') as log:
            process=subprocess.Popen(command,cwd=ROOT,stdin=subprocess.DEVNULL,stdout=log,
                                     stderr=subprocess.STDOUT,start_new_session=True)
        print(json.dumps({'action':'resume_requested','pid':process.pid,'command':command})); return
    if args.prepare_only:
        run_worker(args); return
    guard,reason=acquire_worker()
    if guard is None:
        print(json.dumps({'action':'no_duplicate_started','reason':reason})); return
    try:
        run_worker(args)
    finally:
        metadata_path=OUT/'worker.json'; metadata=json.loads(metadata_path.read_text())
        if metadata.get('pid')==os.getpid():
            metadata.update(status='stopped',stopped_unix=time.time()); save(metadata_path,metadata)
        guard.close()


def run_worker(args):
    OUT.mkdir(parents=True, exist_ok=True); manifest_path = OUT/'manifest.json'
    manifest = json.loads(manifest_path.read_text()) if manifest_path.exists() else make_manifest()
    save(manifest_path,manifest)
    module_paths = [Path(__file__), Path(mc.__file__), Path(scoop.__file__), Path(map_types.__file__),
                    ROOT/'implementation/worldgen/vanilla_search/evaluate.py',
                    ROOT/'implementation/worldgen/vanilla_search/fit.py', Path(task_a.__file__),
                    Path(__file__).with_name('default_region_measurements.py')]
    inputs = {'status':'Prototype/test partial catalogue',
              'analysis_sha256':{str(path):hashlib.sha256(path.read_bytes()).hexdigest() for path in module_paths},
              'shared_provisional_constants':mc.PROVISIONAL,
              'shared_symmetry_bound':map_types.symmetry_bound(),
              'partial_fountain_pair_spacing_blocks':175,
              'user_rejected_seeds_retained_as_negative_controls':sorted(USER_REJECTED),
              'full_qualification_performed':False}
    save(OUT/'analysis-inputs.json', inputs)
    if args.prepare_only:
        print(json.dumps({k: manifest[k] for k in ('source_count','distinct_seeds','job_count')})); return
    checkpoint_path = OUT/'checkpoint.json'
    state = json.loads(checkpoint_path.read_text()) if checkpoint_path.exists() else {
        'next_job': 0, 'actual_partial_compilations': 0, 'partial_front_end_passes': 0,
        'default_regional_passes': 0, 'homebase_partial_passes': 0,
        'recommended_partial_candidates': 0, 'errors': 0, 'rejection_counts': {}}
    compiler_hash = hashlib.sha256(Path(mc.__file__).read_bytes()).hexdigest()
    start = time.time(); deadline = start+args.hours*3600
    state.update(status='running', started_unix=start, stop_by_unix=deadline,worker_pid=os.getpid(),
                 source_count=manifest['source_count'], distinct_source_seeds=manifest['distinct_seeds'],
                 jobs_available=len(manifest['jobs']), stop_file=str(OUT/'STOP'),
                 fully_qualified_new_seeds=0, scope=manifest['scope'])
    save(checkpoint_path,state)
    (OUT/'README.md').write_text('''# Actual-surface Default catalogue — Prototype/test, 14 October 2026

Read-only reslicing of complete cached official-vanilla surface grids. Default regional recognition and the shared Homebase fitter retain their existing thresholds. Every orientation retains measurements and rejection reasons so later policies can be evaluated without rediscovering the seed.

`actual-partials.jsonl` is the append-only catalogue; `manifest.json` lists exact source files, SHA-256 fingerprints and pending window/orientation jobs; `checkpoint.json` is resumable. Create `STOP` to stop after the current job. Resume with the same script and an explicit duration.

Safe coordinator entry: `python tools/analysis/catalogue_actual_scoops.py --ensure --hours 4.5`; read-only status: `python tools/analysis/catalogue_actual_scoops.py --status`. New workers hold `worker.lock` for their entire lifetime and publish PID, command, start time and checkpoint in `worker.json`. Existing live PIDs, including sandbox-protected PIDs, prevent a duplicate writer. The initial PID 20533 predates the lock; its metadata bridge must be honored until it exits. Resume continues `manifest.json` at `checkpoint.next_job`, appending to existing JSONL and logs. A deliberate `STOP` file is respected.

`partial_front_end_pass` means Default regional recognition, independently acceptable sampled Homebases, two available Fountain anchors separated by at least 175 blocks, and the shared sampled symmetry bound. It does **not** establish spacing among all nine anchors, defensive ordering, Routes, Lair fit, exact columns, resources, manual balance or readiness. `recommended_bank_eligible` additionally excludes user-rejected seeds; rejected seeds remain negative evidence.

The cached source collection is biased toward earlier archipelago and pale-forest searches. This lane creates more actual regional windows, not new world seeds. Partial results and rejection vectors are useful even when the strict Default shape fails. No source worlds, canonical compiler constants or gameplay design documents are changed.

Every NO_COARSE_HOME_PAIR case also retains 24 Homebase-independent regional metrics from the unchanged evaluator formulas in a private diagnostic context. Homebase bias is explicitly unavailable; no placeholder Homebase or acceptance is retained. Older missing vectors are supplemented by original job ID in append-only `regional-vector-supplements.jsonl`; these are amendments, not new observations. `--backfill-regional` resumes that supplementation without rewriting original rows.
''')
    count = 0
    for index in range(state['next_job'],len(manifest['jobs'])):
        if time.time() >= deadline or count >= args.max_jobs or (OUT/'STOP').exists():
            break
        job = manifest['jobs'][index]; source = manifest['sources'][job['source_index']]
        try:
            result = compile_partial(job,source,compiler_hash)
            append(OUT/'actual-partials.jsonl', result)
            state['actual_partial_compilations'] += 1
            for field in ('partial_front_end_pass','default_regional_pass','homebase_partial_pass'):
                target = {'partial_front_end_pass':'partial_front_end_passes',
                          'default_regional_pass':'default_regional_passes',
                          'homebase_partial_pass':'homebase_partial_passes'}[field]
                state[target] += bool(result.get(field))
            state['recommended_partial_candidates'] += result['recommended_bank_eligible']
            reasons = Counter(state['rejection_counts']); reasons.update(result.get('rejections',[]))
            state['rejection_counts'] = dict(reasons)
        except Exception as error:
            append(OUT/'errors.jsonl', {'job':job,'source_surface':source['source_surface'],'error':repr(error)})
            state['errors'] += 1
        count += 1; state.update(next_job=index+1, updated_unix=time.time())
        save(checkpoint_path,state)
        if count % 10 == 0:
            print(json.dumps({k:state[k] for k in ('next_job','actual_partial_compilations','default_regional_passes','homebase_partial_passes','partial_front_end_passes','errors')}),flush=True)
    state.update(status='stopped_at_checkpoint' if state['next_job']<len(manifest['jobs']) else 'complete', updated_unix=time.time())
    save(checkpoint_path,state); print(json.dumps(state),flush=True)


def backfill_regional():
    OUT.mkdir(parents=True,exist_ok=True)
    with (OUT/'regional-vector-supplements.lock').open('a') as guard:
        try:
            fcntl.flock(guard,fcntl.LOCK_EX|fcntl.LOCK_NB)
        except BlockingIOError:
            return {'status':'backfill_already_running'}
        path=OUT/'regional-vector-supplements.jsonl'
        done={json.loads(line)['id'] for line in path.read_text().splitlines()} if path.exists() else set()
        # Producer may be concurrently appending; only inspect complete lines.
        raw=(OUT/'actual-partials.jsonl').read_bytes()
        complete=raw[:raw.rfind(b'\n')+1]
        records=[json.loads(line) for line in complete.splitlines()]
        added=0
        for record in records:
            if 'metrics' in record or record['id'] in done:
                continue
            source=load_source(record['source_surface']); i,j,w,h=record['crop_samples']
            crop={**source,'width':w,'height':h,'rows':[row[i:i+w] for row in source['rows'][j:j+h]]}
            raw=measure_without_home_pair(crop,record['rotation'],record['reflected'])
            supplement={key:record[key] for key in ('id','seed','bounds','rotation','reflected','source_surface','source_sha256')}
            supplement.update(raw,schema_version=1,supplemental_kind='raw_regional_measurement_backfill',
                              qualified=False,does_not_change_existing_acceptance=True,created_unix=time.time())
            append(path,supplement); done.add(record['id']); added+=1
        return {'status':'backfill_complete','added':added,'total_supplements':len(done),'sidecar':str(path)}


if __name__ == '__main__':
    main()
