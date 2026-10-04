"""Reevaluate seed windows and retain isolated, certified prototype test worlds.

Uses the existing compiler and thresholds. Does not install worlds in Minecraft
or change the live server/pool. All attempts and inputs are retained.
"""
from __future__ import annotations

import argparse
from concurrent.futures import ThreadPoolExecutor
import hashlib
import json
import os
from pathlib import Path
import random
import shutil
import subprocess
import sys
import time

REPO = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(REPO / 'implementation/worldgen'))
from terrain_harvest import compile_batch, foundry, prospect
from terrain_harvest.excluded_structures import trial_chambers
from terrain_harvest.harvest_seed import harvest
from vanilla_search import acquire


def save(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    temporary = path.with_suffix(path.suffix + '.tmp')
    temporary.write_text(json.dumps(data, indent=2) + '\n')
    temporary.replace(path)


def generate(job, args):
    seed, index, target = job
    work = args.artifacts / 'attempts' / f'{seed}_{index}'
    world = work / 'server/world'
    started = time.monotonic()
    try:
        candidate_path = args.reports / 'candidates' / f'{seed}_{index}.json'
        if candidate_path.exists() and (world / 'level.dat').exists():
            return {'seed': seed, 'index': index, 'world': str(world),
                    'work': str(work), 'candidate': str(candidate_path),
                    'types': target['types'], 'target': target,
                    'reused_generation': True}
        pack = REPO / 'implementation/datapacks/moba_test_no_trial_chambers'
        destination = world / 'datapacks' / pack.name
        shutil.copytree(pack, destination)
        acquire.generate_world(seed, args.server_jar, args.java,
                               work / 'server', args.runtime,
                               tuple(target['chunk_bounds']),
                               progress=lambda n, total: print(
                                   f'generate {seed}: {n}/{total} batches', flush=True))
        # The exclusion gate examines every saved chunk, including the halo.
        forbidden = trial_chambers(world)
        save(work / 'excluded-structures.json', forbidden)
        if forbidden:
            raise ValueError(f'trial chambers remain: {forbidden[:5]}')
        candidate = harvest(world, seed, args.server_jar,
                            chunk_bounds=tuple(target['chunk_bounds']))
        candidate['prospect_types'] = target['types']
        candidate['prospect_scoop'] = {
            'area_blocks2': target['measured'].get('area_blocks2'),
            'area_over_base': target['measured'].get('area_over_base'),
            'axes': {target['team_axis']: {
                'homebase_max_separation_blocks':
                    target.get('homebase_max_separation_blocks')}},
            'deviation_over_relief': target['deviation_over_relief'],
            'relief_blocks': target['relief_blocks'],
            'water_fraction': target.get('water_fraction'),
        }
        candidate['worldgen_truth']['test_generation_override'] = {
            'pack': pack.name,
            'purpose': 'Prevent excluded trial chambers before chunk generation',
            'biome_tag': 'minecraft:has_structure/trial_chambers',
            'saved_world_trial_chamber_findings': 0,
            'pack_files_sha256': {
                str(p.relative_to(pack)): hashlib.sha256(p.read_bytes()).hexdigest()
                for p in sorted(pack.rglob('*')) if p.is_file()},
            'unmodified_vanilla_world': False,
        }
        save(candidate_path, candidate)
        return {'seed': seed, 'index': index, 'world': str(world),
                'work': str(work), 'candidate': str(candidate_path),
                'types': target['types'], 'target': target,
                'seconds': round(time.monotonic() - started, 1)}
    except Exception as exc:
        return {'seed': seed, 'index': index, 'work': str(work),
                'error': f'{type(exc).__name__}: {exc}'}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--server-jar', type=Path, required=True)
    parser.add_argument('--java', type=Path, required=True)
    parser.add_argument('--runtime', type=Path, required=True)
    parser.add_argument('--scanner', type=Path, required=True)
    parser.add_argument('--structures', type=Path, required=True)
    parser.add_argument('--artifacts', type=Path, required=True)
    parser.add_argument('--reports', type=Path, required=True)
    parser.add_argument('--desired', type=int, default=3)
    parser.add_argument('--max-attempts', type=int, default=12)
    parser.add_argument('--resume', action='store_true')
    args = parser.parse_args()
    for name in ('server_jar', 'java', 'runtime', 'scanner', 'structures',
                 'artifacts', 'reports'):
        setattr(args, name, getattr(args, name).resolve())
    if (args.reports / 'summary.json').exists() and not args.resume:
        parser.error('report directory already contains a run; choose a fresh directory')
    acquire.verify_server_jar(args.server_jar)
    os.environ['MOBA_CUBIOMES_SCAN'] = str(args.scanner)
    os.environ['MOBA_CUBIOMES_STRUCT'] = str(args.structures)
    rng = random.Random(20261002)
    seeds = [2718281, 99887766, 120997530, 31337, 930016664, 930015734]
    seeds += [rng.randrange(1, 2**31) for _ in range(10)]
    args.artifacts.mkdir(parents=True, exist_ok=True)
    args.reports.mkdir(parents=True, exist_ok=True)
    report = {'schema': 'test_map_reevaluation/1',
              'status': 'running', 'design_status': 'Prototype/test',
              'compiler_git_head': subprocess.check_output(
                  ['git', 'rev-parse', 'HEAD'], cwd=REPO, text=True).strip(),
              'seed_rng': 20261002, 'seeds': seeds, 'attempts': [], 'published': [],
              'server_jar_sha1': hashlib.sha1(args.server_jar.read_bytes()).hexdigest(),
              'generation': 'Java 1.21.11 official server; test-only trial-chamber biome-tag override',
              'desired': args.desired, 'max_attempts': args.max_attempts}
    if args.resume and (args.reports / 'summary.json').exists():
        previous = json.loads((args.reports / 'summary.json').read_text())
        save(args.reports / f"summary-before-resume-{int(time.time())}.json", previous)
        report['previous_attempts'] = previous.get('attempts', [])
        report['published'] = previous.get('published', [])
    save(args.reports / 'summary.json', report)
    targets = []
    for seed in seeds:
        existing = args.reports / 'prospects' / f'{seed}.json'
        result = (json.loads(existing.read_text()) if args.resume and existing.exists()
                  else prospect.prospect(seed, budget=3))
        save(args.reports / 'prospects' / f'{seed}.json', result)
        eligible = [t for t in result['targets']
                    if t['generatable'] and t['within_symmetry_gate']]
        # Retain two distinct windows per seed, prioritizing the measured-yield
        # landmass family without dropping other types from the evidence.
        eligible.sort(key=lambda t: ('landmass' not in t['types'],
                                      t['deviation_over_relief']))
        targets.extend((seed, index, target)
                       for index, target in enumerate(eligible[:2]))
        print(f'prospect {seed}: {len(eligible)} eligible windows', flush=True)
    # Round robin: reevaluate more seeds before spending a second window on one.
    targets.sort(key=lambda job: job[1])
    report['prospected_seeds'] = len(seeds)
    report['eligible_targets'] = len(targets)
    save(args.reports / 'summary.json', report)
    for offset in range(0, min(len(targets), args.max_attempts), 2):
        if len(report['published']) >= args.desired:
            break
        with ThreadPoolExecutor(max_workers=2) as workers:
            generated = list(workers.map(lambda job: generate(job, args),
                                         targets[offset:offset + 2]))
        for item in generated:
            if 'error' not in item:
                seed = item['seed']
                work = Path(item['work'])
                try:
                    result = compile_batch.run([Path(item['candidate'])],
                              worlds={seed: Path(item['world'])}, build_root=work)
                    save(args.reports / 'compilations' /
                         f"{seed}_{item['index']}.json", result)
                    record = result['runs'][0]
                    item['deepest_stage'] = record['deepest_stage_reached']
                    item['rejections'] = record['rejections']
                    item['ready'] = record.get('ready', False)
                    if item['ready']:
                        # Pool provenance must carry the test-generation override.
                        candidate = json.loads(Path(item['candidate']).read_text())
                        record['worldgen_truth'] = candidate['worldgen_truth']
                        ident = foundry.map_id(seed, record)
                        if any(m['map_id'] == ident for m in report['published']):
                            print(f'previously published {ident}', flush=True)
                            report['attempts'].append(item)
                            save(args.reports / 'summary.json', report)
                            continue
                        manifest = foundry.publish(args.artifacts / 'test-pool', seed,
                                                  work / f'build-{seed}', record,
                                                  authoring_version='2026-10-02-test')
                        report['published'].append(manifest)
                    print(f"compile {seed}: {item['deepest_stage']} "
                          f"ready={item['ready']}", flush=True)
                except Exception as exc:
                    item['error'] = f'{type(exc).__name__}: {exc}'
            report['attempts'].append(item)
            save(args.reports / 'summary.json', report)
            if 'error' in item:
                print(f"attempt {item['seed']}: {item['error']}", flush=True)
    report['status'] = 'complete' if len(report['published']) >= args.desired else 'shortfall'
    save(args.reports / 'summary.json', report)
    print(f"finished: {len(report['published'])} published test maps", flush=True)


if __name__ == '__main__':
    main()
