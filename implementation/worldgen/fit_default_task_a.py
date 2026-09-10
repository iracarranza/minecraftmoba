#!/usr/bin/env python3
"""Run Task A on the eight committed staged finalists; never open a world to write."""
import argparse
import hashlib
import json
from pathlib import Path

from vanilla_search.task_a import fit, UNAVAILABLE
from vanilla_search.task_a_render import svg

ROOT=Path(__file__).resolve().parents[2]
SOURCE=ROOT/'implementation/worldgen/results/staged_default_2026-09-09'
OUTPUT=ROOT/'implementation/worldgen/results/default_task_a_2026-09-10'
REFINED_OUTPUT=ROOT/'implementation/worldgen/results/default_task_a_1_2026-09-10'


def digest(path): return hashlib.sha256(path.read_bytes()).hexdigest()


def world_snapshot(candidates):
    result={}
    for candidate in candidates:
        world=Path(candidate['local_world_directory'])
        if not world.is_dir():
            result[str(candidate['seed'])]={'available':False};continue
        result[str(candidate['seed'])]={'available':True,'files':{
            str(p.relative_to(world)):digest(p) for p in sorted(world.rglob('*')) if p.is_file()}}
    return result


def encode(value): return json.dumps(value,sort_keys=True,separators=(',',':'),allow_nan=False)+'\n'


def run(out=OUTPUT, refined=False):
    out=Path(out).resolve()
    results=(ROOT/'implementation/worldgen/results').resolve()
    if not out.is_relative_to(results) or out==results or out==SOURCE or out.is_relative_to(SOURCE):
        raise ValueError('Task A outputs must use a separate directory below implementation/worldgen/results')
    if refined and (out==OUTPUT or out.is_relative_to(OUTPUT)):
        raise ValueError('A.1 must preserve A.0 artifacts')
    summary=json.loads((SOURCE/'search_summary.json').read_text())
    seeds=sorted(summary['finalist_seeds'])
    if len(seeds)!=8 or len(set(seeds))!=8: raise ValueError('Task A requires the existing eight-finalist manifest')
    source_files=[SOURCE/'finalists'/str(seed)/'candidate.json' for seed in seeds]
    before_inputs={str(p.relative_to(ROOT)):digest(p) for p in source_files}
    candidates=[json.loads(p.read_text()) for p in source_files]
    prior_files=sorted(p for p in OUTPUT.rglob('*') if p.is_file()) if refined else []
    before_prior={str(p.relative_to(ROOT)):digest(p) for p in prior_files}
    before_worlds=world_snapshot(candidates)
    out.mkdir(parents=True,exist_ok=True);aggregate=[];outputs=[];fitted=[]
    for c in candidates:
        if refined:
            from vanilla_search.task_a_refine import refine
            baseline=json.loads((OUTPUT/str(c['seed'])/'fit.json').read_text())
            source_hash=before_inputs[str((SOURCE/'finalists'/str(c['seed'])/'candidate.json').relative_to(ROOT))]
            if baseline['seed']!=c['seed'] or baseline['source_candidate_sha256']!=source_hash:
                raise ValueError('A.0 fit does not match the committed candidate input')
            result=refine(c,baseline)
            result['source_a0_fit_sha256']=digest(OUTPUT/str(c['seed'])/'fit.json')
        else:
            result=fit(c)
        result['source_candidate_sha256']=before_inputs[str((SOURCE/'finalists'/str(c['seed'])/'candidate.json').relative_to(ROOT))]
        fitted.append(result)
        directory=out/str(c['seed'])
        if directory.is_symlink(): raise ValueError('output candidate directory must not be a symlink')
        directory.mkdir(exist_ok=True)
        for name,data in [('fit.json',encode(result)),('greybox.svg',svg(result,c,(SOURCE/'finalists'/str(c['seed'])/'05_actual_ground_canopy.png').read_bytes()))]:
            p=directory/name
            if p.is_symlink(): raise ValueError('output file must not be a symlink')
            p.write_text(data);outputs.append(p)
        aggregate.append({'seed':c['seed'],'network_metrics':result['network_metrics'],
                          'failures':result['failures'],'unavailable_metric_keys':sorted(result['uncertainties']),
                          'fit':f'{c["seed"]}/fit.json','svg':f'{c["seed"]}/greybox.svg'})
        print(f"{c['seed']}: {len(result['routes'])} corridors; {len(result['failures'])} failure diagnostics",flush=True)
    (out/'comparison.json').write_text(encode({'schema':'default_task_a_1_comparison_v1' if refined else 'default_task_a_comparison_v1','status':'analytical / non-authoritative; no ranking or winner','candidates':aggregate,'unavailable_metrics':fitted[0]['uncertainties']}))
    if refined:
        from vanilla_search.task_a_review import readiness, comparison_report
        gate=readiness(fitted)
        (out/'readiness_gate.json').write_text(encode(gate))
        (out/'comparative_validation.md').write_text(comparison_report(fitted,gate))
        outputs.extend(out/name for name in ('comparison.json','readiness_gate.json','comparative_validation.md'))
    after_inputs={str(p.relative_to(ROOT)):digest(p) for p in source_files}
    after_worlds=world_snapshot(candidates)
    if before_inputs!=after_inputs or before_worlds!=after_worlds: raise RuntimeError('input/world immutability check failed')
    if before_prior!={str(p.relative_to(ROOT)):digest(p) for p in prior_files}: raise RuntimeError('A.0 preservation check failed')
    verification={'candidate_inputs_unchanged':True,'available_worlds_unchanged':True,
                  'world_files_verified':{s:len(v.get('files',{})) for s,v in before_worlds.items()},
                  'unavailable_worlds':[s for s,v in before_worlds.items() if not v['available']],
                  'candidate_inputs_sha256':before_inputs,'output_sha256':{str(p.relative_to(out)):digest(p) for p in outputs}}
    if refined: verification.update({'a0_artifacts_unchanged':True,'a0_artifact_sha256':before_prior})
    (out/'verification.json').write_text(encode(verification))
    return verification


if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('--out',type=Path)
    parser.add_argument('--refine',action='store_true',help='A.1 semantic refinement; preserve and reuse committed A.0 fits')
    args=parser.parse_args()
    run(args.out or (REFINED_OUTPUT if args.refine else OUTPUT),refined=args.refine)
