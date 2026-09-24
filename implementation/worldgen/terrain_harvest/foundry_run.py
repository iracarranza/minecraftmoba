"""Run the pipeline in parallel, and delete worlds once they are harvested.

THE TWO HALVES PARALLELISE DIFFERENTLY, which is why this is not one pool.

Generation spawns a JVM and waits on its console, so it is I/O-bound from
Python's side and threads are enough. It is bounded by MEMORY, not cores:
each server takes -Xmx3G on a 16 GB machine, so three or four concurrent, not
eight. The cap is a parameter and is measured rather than assumed.

Compilation is CPU-bound pure Python in one process, so it needs PROCESSES to
use more than one core. It is also the bigger half now -- 28.6 minutes against
generation's 26.3 in the seamed batch -- which reverses the long-standing
"generation is 91% of cost" figure and makes this the larger win.

WORLDS ARE DELETED AFTER HARVEST. Each target leaves 379 MB on a volume that
is already 84% full with 70 GB free, so roughly 180 targets before pressure.
Nothing deleted them before, which was survivable at 5 maps/hour and is not at
20. The candidate JSON and the compilation record are kept; the region files
are reproducible from the seed and the chunk bounds, which is the whole point
of a deterministic generator.
"""
from __future__ import annotations

import json
import shutil
import time
from concurrent.futures import ProcessPoolExecutor, ThreadPoolExecutor, as_completed
from pathlib import Path

from . import compile_batch, harvest_seed, prospect as prospect_mod
from vanilla_search import acquire

DEFAULT_GENERATE_WORKERS = 3          # memory-bound; measured, not assumed
DEFAULT_COMPILE_WORKERS = 6           # CPU-bound; 8 cores, leave headroom


def _generate_one(job) -> dict:
    """Generate and harvest one window, then drop the world."""
    seed, target, work, server_jar, java, keep_world, runtime = job
    work = Path(work)
    bounds = tuple(target['chunk_bounds'])
    server_dir = work / 'server'
    world = server_dir / 'world'
    try:
        t = time.perf_counter()
        acquire.generate_world(seed, server_jar, java, server_dir,
                               Path(runtime), bounds)
        generated = round(time.perf_counter() - t, 1)

        t = time.perf_counter()
        candidate = harvest_seed.harvest(world, seed, chunk_bounds=bounds)
        candidate['prospect_types'] = target.get('types') or []
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
        path = work / 'candidate.json'
        path.write_text(json.dumps(candidate))
        return {'seed': seed, 'work': str(work), 'candidate': str(path),
                'world': str(world), 'generate_seconds': generated,
                'harvest_seconds': round(time.perf_counter() - t, 1),
                'target': target, 'keep_world': keep_world}
    except Exception as exc:                      # noqa: BLE001
        # One window failing is data. Dying here would report no yield rather
        # than a low one, which is the opposite of what a batch is for.
        if not keep_world:
            shutil.rmtree(work, ignore_errors=True)
        return {'seed': seed, 'error': f'{type(exc).__name__}: {exc}'}


def _compile_one(job) -> dict:
    seed, candidate, world, work = job
    t = time.perf_counter()
    try:
        out = compile_batch.run([Path(candidate)], worlds={seed: Path(world)},
                                build_root=Path(work))
        return {'seed': seed, 'compile_seconds': round(time.perf_counter() - t, 1),
                'compiled': out}
    except Exception as exc:                      # noqa: BLE001
        return {'seed': seed, 'error': f'{type(exc).__name__}: {exc}'}


def run(seeds, *, server_jar: Path, java: Path, root: Path,
        per_seed: int = 2, generate_workers: int = DEFAULT_GENERATE_WORKERS,
        compile_workers: int = DEFAULT_COMPILE_WORKERS,
        keep_worlds: bool = False, scanner: str | None = None,
        budget: int = 3, log=print) -> dict:
    root = Path(root)
    root.mkdir(parents=True, exist_ok=True)
    started = time.perf_counter()

    # Prospect is ~1.4s a seed and needs no server, so it runs first and
    # serially. Parallelising it would optimise 2% of the cost.
    # ONE shared runtime, prepared once before any worker starts. Preparing it
    # per target ran the jar N times and, in parallel, had N workers racing to
    # download the same cached jar.
    runtime = root / '_runtime'
    prep_started = time.perf_counter()
    if not (runtime / 'libraries').exists():
        acquire.prepare_runtime(server_jar, java, runtime)
    prepared = round(time.perf_counter() - prep_started, 1)

    prospect_started = time.perf_counter()
    jobs, prospects = [], {}
    for seed in seeds:
        found = prospect_mod.prospect(seed, budget=budget, scanner=scanner)
        prospects[seed] = {k: found[k] for k in
                           ('considered', 'within_gate', 'generatable',
                            'described_only', 'dropped_open_water')}
        targets = [t for t in found['targets']
                   if t['generatable'] and t['within_symmetry_gate']][:per_seed]
        for n, target in enumerate(targets):
            jobs.append((seed, target, root / f'{seed}_{n}', server_jar, java,
                         keep_worlds, runtime))
    # Timed from after the runtime prep, not from `started`. Folding a
    # one-off 15s jar setup into the per-seed scan cost reported 18.0s for two
    # seeds against a measured 1.38s each.
    prospected = round(time.perf_counter() - prospect_started, 1)
    log(f'prospected {len(seeds)} seeds -> {len(jobs)} targets in {prospected}s')

    harvested, failures = [], []
    t = time.perf_counter()
    with ThreadPoolExecutor(max_workers=generate_workers) as pool:
        for done in as_completed([pool.submit(_generate_one, j) for j in jobs]):
            row = done.result()
            (failures if 'error' in row else harvested).append(row)
            log(f"  {row['seed']}: "
                + (row['error'] if 'error' in row
                   else f"gen {row['generate_seconds']}s "
                        f"harvest {row['harvest_seconds']}s"))
    generation = round(time.perf_counter() - t, 1)

    compiled = []
    t = time.perf_counter()
    with ProcessPoolExecutor(max_workers=compile_workers) as pool:
        futures = [pool.submit(_compile_one, (h['seed'], h['candidate'],
                                              h['world'], h['work']))
                   for h in harvested]
        for done in as_completed(futures):
            compiled.append(done.result())
    compilation = round(time.perf_counter() - t, 1)

    # Only now are the worlds expendable: `compile_batch` reads them.
    freed = 0
    if not keep_worlds:
        for h in harvested:
            for sub in ('server', f"build-{h['seed']}"):
                target = Path(h['work']) / sub
                if target.exists():
                    freed += 1
                    shutil.rmtree(target, ignore_errors=True)

    playable = [c for c in compiled if c.get('compiled', {}).get('playable')]
    elapsed = time.perf_counter() - started
    return {
        'seeds': len(seeds), 'targets': len(jobs),
        'generated': len(harvested), 'generation_failures': failures,
        'playable': sorted({s for c in playable
                            for s in c['compiled']['playable']}),
        'playable_count': len(playable),
        'ready_per_hour': round(len(playable) / (elapsed / 3600), 2),
        'timing': {'runtime_prep_seconds': prepared,
                   'prospect_seconds': prospected,
                   'generation_seconds': generation,
                   'compilation_seconds': compilation,
                   'wall_seconds': round(elapsed, 1)},
        'workers': {'generate': generate_workers, 'compile': compile_workers},
        'worlds_deleted': freed,
        'prospects': prospects,
        'compilations': compiled,
    }
