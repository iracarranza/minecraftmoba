#!/usr/bin/env python3
"""Run spec 4 shadow analysis on frozen selections; never rerun the selector."""
import json
from pathlib import Path
import re
import subprocess
import sys
import time

from fit_default_task_a import ROOT,SOURCE,digest,encode,world_snapshot
from fit_default_task_a_network import OUT as BASELINE
from vanilla_search.task_a_opportunities import analyze_shadow,strip_shadow
from vanilla_search.task_a_opportunity_report import report,selected

OUT=ROOT/'implementation/worldgen/results/default_opportunity_shadow_2026-09-11'
SEEDS=(930005557,930006815,930007222,930010639,930012642,930015734,930016664,930019528)
ACTIVE=[ROOT/'implementation/worldgen/vanilla_search'/name for name in
    ('task_a.py','task_a_refine.py','task_a_destinations.py','task_a_network.py','task_a_local.py')]+[ROOT/'implementation/worldgen/fit_default_task_a_network.py']
IMPLEMENTATION=[Path(__file__),ROOT/'implementation/worldgen/vanilla_search/task_a_opportunities.py',
    ROOT/'implementation/worldgen/vanilla_search/task_a_opportunity_report.py']


def write(path,data):
    if path.is_symlink():raise ValueError('No symlink output')
    path.write_text(data)


def run():
    manifest=json.loads((SOURCE/'search_summary.json').read_text())
    if tuple(sorted(manifest['finalist_seeds']))!=SEEDS:raise ValueError('Requires exactly the original eight finalists')
    candidates=[json.loads((SOURCE/'finalists'/str(seed)/'candidate.json').read_text()) for seed in SEEDS]
    old_verification=json.loads((BASELINE/'verification.json').read_text())
    protected={ROOT/p for p in old_verification['protected_sha256']}
    protected.update(p for p in BASELINE.rglob('*') if p.is_file())
    protected.update(ACTIVE)
    protected.add(SOURCE/'search_summary.json')
    protected.add(ROOT/'specs/mapseedsearchspec4.md')
    hashes={str(p.relative_to(ROOT)):digest(p) for p in sorted(protected)}
    worlds=candidates+[{'seed':f'task_b_{s}','local_world_directory':str(ROOT/f'artifacts/worldgen/default_task_b_2026-09-10/{s}/world')} for s in (930010639,930012642)]
    before=world_snapshot(worlds)
    if OUT.is_symlink():raise ValueError('No symlink output')
    OUT.mkdir(exist_ok=True);fits=[];timings={}
    for c in candidates:
        seed=c['seed'];baseline=json.loads((BASELINE/str(seed)/'fit.json').read_text())
        if baseline['provenance']['candidate_sha256']!=digest(SOURCE/'finalists'/str(seed)/'candidate.json'):raise ValueError('Candidate provenance mismatch')
        start=time.monotonic();f=analyze_shadow(c,baseline);timings[str(seed)]=round(time.monotonic()-start,3)
        if strip_shadow(f)!=baseline:raise RuntimeError('Shadow analysis altered an active field')
        directory=OUT/str(seed)
        if directory.is_symlink():raise ValueError('No symlink candidate output')
        directory.mkdir(exist_ok=True);write(directory/'fit.json',encode(f));fits.append(f)
        print(f'{seed}: {len(selected(f))} selected unchanged; {len(f["opportunity_shadow"]["rejected_sample"])} unselected sampled; {timings[str(seed)]}s',flush=True)
    write(OUT/'comparison.json',encode({'schema':'default_opportunity_shadow_comparison_v1','selection_influence':False,
        'candidates':[{'seed':f['seed'],'selected_relationships':selected(f),'opening_opportunity_sets':f['opportunity_shadow']['opening_opportunity_sets'],
            'sampled_unselected_count':len(f['opportunity_shadow']['rejected_sample']),'fit':f'{f["seed"]}/fit.json'} for f in fits]}))
    write(OUT/'REPORT.md',report(fits))
    write(OUT/'TEST_RESULTS.md','Tests pending for this generated run. Execute the runner with --verify.\n')
    if hashes!={str(p.relative_to(ROOT)):digest(p) for p in sorted(protected)}:raise RuntimeError('Protected active implementation/prior output changed')
    if before!=world_snapshot(worlds):raise RuntimeError('World files changed')
    write(OUT/'verification.json',encode({'shadow_only':True,'exact_eight_finalists':list(SEEDS),'active_fit_fields_unchanged':True,
        'no_new_seeds':True,'no_world_materialization':True,'no_task_c':True,'existing_world_files_unchanged':True,
        'protected_sha256':hashes,'implementation_sha256':{str(p.relative_to(ROOT)):digest(p) for p in IMPLEMENTATION},
        'world_files_verified':{s:len(v.get('files',{})) for s,v in before.items()},'unavailable_worlds':[s for s,v in before.items() if not v['available']],
        'analysis_seconds':timings,'output_sha256':{str(p.relative_to(OUT)):digest(p) for p in sorted(OUT.rglob('*')) if p.is_file() and p.name!='verification.json'}}))


def verify():
    v=json.loads((OUT/'verification.json').read_text())
    for mapping in ('implementation_sha256','protected_sha256'):
        for name,sha in v[mapping].items():
            if digest(ROOT/name)!=sha:raise RuntimeError('Source/protected file changed: '+name)
    command=[sys.executable,'-m','unittest','discover','-s','implementation/worldgen/tests','-q']
    result=subprocess.run(command,cwd=ROOT,capture_output=True,text=True);output=result.stdout+result.stderr;print(output,flush=True)
    if result.returncode:raise RuntimeError('Tests failed')
    count,seconds=re.search(r'Ran (\d+) tests in ([\d.]+)s',output).groups()
    write(OUT/'TEST_RESULTS.md',f'# Test receipt\n\n`python3 -m unittest discover -s implementation/worldgen/tests -q`\n\n**{count} tests passed** in {seconds} seconds.\n\n```text\n{output.strip()}\n```\n')
    text=(OUT/'REPORT.md').read_text()
    text=re.sub(r'Test result: [^\n]*',f'Test result: **{count} tests passed** in {seconds} seconds; see `TEST_RESULTS.md`.',text)
    write(OUT/'REPORT.md',text)
    v['tests']={'count':int(count),'seconds':float(seconds),'result':'passed','command':command[1:]}
    v['output_sha256']={str(p.relative_to(OUT)):digest(p) for p in sorted(OUT.rglob('*')) if p.is_file() and p.name!='verification.json'}
    write(OUT/'verification.json',encode(v))


if __name__=='__main__':
    if sys.argv[1:]==['--verify']:verify()
    elif not sys.argv[1:]:run()
    else:raise SystemExit('Usage: fit_default_task_a_opportunities.py [--verify]')
