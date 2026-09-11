#!/usr/bin/env python3
"""Run destination-first analysis on existing finalist terrain; never author worlds."""
import base64
from html import escape
import json
from pathlib import Path
import re
import subprocess
import sys
import time

from successor.grid import unrle
from fit_default_task_a import ROOT,SOURCE,OUTPUT,REFINED_OUTPUT,digest,encode,world_snapshot
from fit_default_task_a_destinations import OUT as PREVIOUS
from vanilla_search.task_a_network import analyze
from vanilla_search.task_a_local_report import report as local_report, selection_changes

BASELINE=ROOT/'implementation/worldgen/results/default_destination_first_2026-09-10'
OUT=ROOT/'implementation/worldgen/results/default_local_continuation_2026-09-11'


def svg(fit,old,background):
    w=fit['sampling']['width'];h=fit['sampling']['height'];s=5;ox=30;oy=75;right=ox+w*s+25
    width=right+420;height=max(h*s+150,950)
    parts=[f'<svg xmlns="http://www.w3.org/2000/svg" width="{width}" height="{height}" viewBox="0 0 {width} {height}">',
           '<rect width="100%" height="100%" fill="#15211e"/><g fill="#f2f1e6" font-family="sans-serif">',
           f'<text x="30" y="28" font-size="22">Destination-first analysis · {fit["seed"]}</text>',
           '<text x="30" y="50" font-size="12">No imposed center, old departure or deeper Route target · analytical only</text>',
           f'<image x="{ox}" y="{oy}" width="{w*s}" height="{h*s}" href="data:image/png;base64,{base64.b64encode(background).decode()}"/>']
    def pt(sample):return ox+(sample[0]+.5)*s,oy+(sample[1]+.5)*s
    def line(path,color,width,dash=''):
        points=' '.join(f'{x},{y}' for x,y in map(pt,path))
        parts.append(f'<polyline points="{points}" fill="none" stroke="{color}" stroke-width="{width}" stroke-dasharray="{dash}"/>')
    def text(x,y,message,size=12):parts.append(f'<text x="{x}" y="{y}" font-size="{size}">{escape(message)}</text>')
    labels=unrle(fit['region_grid_rle']);boundaries=[]
    for z in range(h):
        for x in range(w):
            i=z*w+x
            if not labels[i]:continue
            if x+1<w and labels[i]!=labels[i+1]:boundaries.append(f'M{ox+(x+1)*s},{oy+z*s}v{s}')
            if z+1<h and labels[i]!=labels[i+w]:boundaries.append(f'M{ox+x*s},{oy+(z+1)*s}h{s}')
    parts.append(f'<path d="{" ".join(boundaries)}" stroke="#fff" stroke-opacity=".25" stroke-width=".7" fill="none"/>')
    for r in old['routes']:
        if r.get('sample_path'):line(r['sample_path'],'#555',1,'3 5')
    row=105
    for team,color in (('north','#48baff'),('south','#ff8974')):
        home=fit['homelands'][team];x0,x1,z0,z1=home['logical_footprint_samples']
        parts.append(f'<rect x="{ox+x0*s}" y="{oy+z0*s}" width="{(x1-x0+1)*s}" height="{(z1-z0+1)*s}" fill="none" stroke="{color}" stroke-width="3"/>')
        text(right,row,team.upper()+' HANDOFFS',16);row+=25
        for n,r in enumerate(fit['selected_handoffs'][team],1):
            c=next(c for c in fit['starter_corridors'][team] if c['destination']==r['id'])
            if c.get('sample_path'):line(c['sample_path'],color,4)
            x,y=pt(r['anchor']['sample']);name=team[0].upper()+str(n)
            parts.append(f'<circle cx="{x}" cy="{y}" r="5" fill="white" stroke="{color}"><title>{escape(r["id"])}</title></circle>')
            text(x+7,y-6,name);cont=r['local_continuation'];deep=r['deep_network_reach']['modest']
            text(right,row,f'{name}: {r["kind"]}');row+=18
            text(right,row,f'HD {r["homeland_depth"]:.1f} / travel {r["travel_cost"]:.1f} / {cont["continuation_class"]}');row+=18
            text(right,row,f'Local gain {cont["depth_gain"]:.1f}; deep regions {len(deep["deeper_regions_reached"])}');row+=18
            text(right,row,f'Local extent {cont["physical_extent"]:.1f} physical / {cont["effective_extent"]:.1f} effective');row+=24
        text(right,row,f'Unresolved choices: {fit["unresolved_choices"][team]}');row+=40
    for message in ('HD: geometric Homeland Depth','Travel: independent terrain-weighted cost','Thin boundaries: coarse terrain components','Grey dashed: historical comparison only','No new physical infrastructure or Task C',f'Regions: {len(fit["regions"])} (descriptive)',
                    'Continuation and convergence: companion JSON','Major / conditional water links are not validated'):
        text(right,row,message);row+=22
    text(30,height-35,'Terrain/network diagnostics are sampled evidence, not a seed verdict or desired topology.')
    parts.append('</g></svg>');return '\n'.join(parts)+'\n'


def run():
    seeds=sorted(json.loads((SOURCE/'search_summary.json').read_text())['finalist_seeds'])
    if len(seeds)!=8 or len(set(seeds))!=8:raise ValueError('Requires the existing eight-finalist manifest')
    candidates=[json.loads((SOURCE/'finalists'/str(s)/'candidate.json').read_text()) for s in seeds]
    protected=[SOURCE/'finalists'/str(s)/'candidate.json' for s in seeds]
    for directory in (OUTPUT,REFINED_OUTPUT,PREVIOUS,BASELINE,ROOT/'implementation/worldgen/results/default_task_b_2026-09-10'):
        protected.extend(p for p in sorted(directory.rglob('*')) if p.is_file())
    hashes={str(p.relative_to(ROOT)):digest(p) for p in protected}
    worlds=candidates+[{'seed':f'task_b_{s}','local_world_directory':str(ROOT/f'artifacts/worldgen/default_task_b_2026-09-10/{s}/world')} for s in (930010639,930012642)]
    before=world_snapshot(worlds);old={};baseline={};fits=[];timings={}
    if OUT.is_symlink():raise ValueError('No symlink output')
    OUT.mkdir(exist_ok=True)
    def write(path,data):
        if path.is_symlink():raise ValueError('No symlink output')
        path.write_text(data)
    for c in candidates:
        seed=c['seed'];old[seed]=json.loads((PREVIOUS/str(seed)/'fit.json').read_text())
        baseline[seed]=json.loads((BASELINE/str(seed)/'fit.json').read_text())
        if old[seed]['source_candidate_sha256']!=digest(SOURCE/'finalists'/str(seed)/'candidate.json'):raise ValueError('Source provenance mismatch')
        start=time.monotonic();f=analyze(c,old[seed]['homelands']);timings[str(seed)]=round(time.monotonic()-start,3)
        f['provenance']={'candidate_sha256':digest(SOURCE/'finalists'/str(seed)/'candidate.json'),'homeland_source_sha256':digest(PREVIOUS/str(seed)/'fit.json'),
                         'historical_route_use':'comparison/debug only; not passed to analyze'}
        f['historical_comparison']={'previous_destination_sets':old[seed]['destination_sets'],'routes':old[seed]['routes']}
        directory=OUT/str(seed)
        if directory.is_symlink():raise ValueError('No symlink candidate output')
        directory.mkdir(exist_ok=True);write(directory/'fit.json',encode(f))
        write(directory/'greybox.svg',svg(f,old[seed],(SOURCE/'finalists'/str(seed)/'05_actual_ground_canopy.png').read_bytes()))
        fits.append(f);print(f'{seed}: selected '+str({t:len(v) for t,v in f['selected_handoffs'].items()})+f'; unresolved {f["unresolved_choices"]}; {timings[str(seed)]}s',flush=True)
    write(OUT/'comparison.json',encode({'schema':'destination_first_local_comparison_v2','baseline_commit':'4ec2dc5b10e317f6a0025907333c16d0a8512c73','status':'descriptive, not a seed verdict','candidates':[
        {'seed':f['seed'],'regions':len(f['regions']),'selected_handoffs':f['selected_handoffs'],'unresolved_choices':f['unresolved_choices'],
         'topology':f['topology'],'previous_selected_handoffs':baseline[f['seed']]['selected_handoffs'],'diagnostics':f['diagnostics'],
         'selection_changes':selection_changes(f,baseline[f['seed']]),
         'fit':f'{f["seed"]}/fit.json','svg':f'{f["seed"]}/greybox.svg'} for f in fits]}))
    write(OUT/'REPORT.md',local_report(fits,baseline))
    if before!=world_snapshot(worlds):raise RuntimeError('World preservation failed')
    if hashes!={str(p.relative_to(ROOT)):digest(p) for p in protected}:raise RuntimeError('Prior artifacts changed')
    outputs=sorted(p for p in OUT.rglob('*') if p.is_file() and p.name!='verification.json')
    write(OUT/'verification.json',encode({'prior_inputs_and_artifacts_unchanged':True,'worlds_unchanged':True,'protected_sha256':hashes,
        'implementation_sha256':{str(p.relative_to(ROOT)):digest(p) for p in (Path(__file__),*[ROOT/'implementation/worldgen/vanilla_search'/name for name in ('task_a.py','task_a_network.py','task_a_local.py','task_a_local_report.py')])},
        'world_files_verified':{s:len(v.get('files',{})) for s,v in before.items()},'unavailable_worlds':[s for s,v in before.items() if not v['available']],
        'fit_seconds':timings,'output_sha256':{str(p.relative_to(OUT)):digest(p) for p in outputs}}))


def verify_tests():
    """Run the real suite, then seal its receipt and report with fresh hashes."""
    verification=OUT/'verification.json'
    v=json.loads(verification.read_text())
    for path,sha in v['implementation_sha256'].items():
        if digest(ROOT/path)!=sha:raise RuntimeError('Implementation changed since fit: '+path)
    command=[sys.executable,'-m','unittest','discover','-s','implementation/worldgen/tests','-q']
    result=subprocess.run(command,cwd=ROOT,capture_output=True,text=True)
    output=result.stdout+result.stderr;print(output,flush=True)
    if result.returncode:raise RuntimeError('Tests failed; no successful receipt written')
    match=re.search(r'Ran (\d+) tests in ([\d.]+)s',output)
    if not match:raise RuntimeError('Missing unittest result')
    count,seconds=match.groups()
    receipt=OUT/'TEST_RESULTS.md'
    for path in (receipt,OUT/'REPORT.md',verification):
        if path.is_symlink():raise ValueError('No symlink output')
    receipt.write_text('# Verification receipt\n\nCommand: `python3 -m unittest discover -s implementation/worldgen/tests -q`\n\n'
        f'Result: **{count} tests passed**, {seconds} seconds.\n\n```text\n{output.strip()}\n```\n\n'
        f'World files unchanged: {sum(v["world_files_verified"].values())}; protected prior files unchanged: {len(v["protected_sha256"])}.\n'
        'All eight output pairs parsed by tests; prior fields/regions/bounds and unchanged-anchor deep reach match the baseline.\n')
    report_path=OUT/'REPORT.md'
    text=report_path.read_text()
    text=re.sub(r'- Test count/result: .*',f'- Test count/result: **{count} tests passed** in {seconds} seconds; see TEST_RESULTS.md.',text)
    report_path.write_text(text)
    v['tests']={'command':command[1:],'count':int(count),'seconds':float(seconds),'result':'passed'}
    v['output_sha256']={str(p.relative_to(OUT)):digest(p) for p in sorted(OUT.rglob('*')) if p.is_file() and p.name!='verification.json'}
    verification.write_text(encode(v))
    for path,sha in v['output_sha256'].items():
        if digest(OUT/path)!=sha:raise RuntimeError('Final output verification failed')


if __name__=='__main__':
    if sys.argv[1:]==['--verify']:verify_tests()
    elif not sys.argv[1:]:run()
    else:raise SystemExit('Usage: fit_default_task_a_network.py [--verify]')
