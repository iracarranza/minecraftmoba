"""Seed -> prospect -> generate -> harvest -> compile, in one pass.

The end-to-end wiring. Until now `prospect` produced window targets that
nothing consumed, `acquire.generate_world` took `chunk_bounds` that nothing
searched for, and `compile_batch` took candidate files somebody made by hand.
Three halves of a pipeline with no middle.

WHY THE ORDER MATTERS. Generation is ~91% of batch cost, so every window
generated without being searched for first is the expensive step spent on a
guess. `prospect` costs ~1.4s per seed and returns the windows worth the
money; on 20 seeds it found a median of 13 targets and 9 inside the symmetry
gate, with no seed yielding nothing.

WHAT THIS MEASURES. READY maps per hour of generation -- the number the pool
needs, and the one nothing has ever produced. A match claims a READY
realization from a stocked pool, so the question is refill rate against
consumption rate, not whether one map can be made.
"""
from __future__ import annotations

import json
import time
from pathlib import Path

from . import compile_batch, harvest_seed, prospect as prospect_mod
from vanilla_search import acquire


def forge(seed: int, *, server_jar: Path, java: Path, root: Path,
          per_seed: int = 1, gate_only: bool = True,
          half: int = 4096, step: int = 32, budget: int = 3,
          scanner: str | None = None, log=print) -> dict:
    """Prospect one seed, generate its best targets, and compile them."""
    root = Path(root)
    root.mkdir(parents=True, exist_ok=True)
    timing = {}

    t0 = time.perf_counter()
    found = prospect_mod.prospect(seed, half=half, step=step, budget=budget,
                                  scanner=scanner)
    timing['prospect_seconds'] = round(time.perf_counter() - t0, 2)

    targets = [t for t in found['targets'] if t['generatable']]
    if gate_only:
        targets = [t for t in targets if t['within_symmetry_gate']]
    targets = targets[:per_seed]
    if not targets:
        return {'seed': seed, 'targets_generated': 0, 'compilations': [],
                'why': 'no target inside the symmetry gate',
                'prospect': {k: found[k] for k in
                             ('considered', 'within_gate', 'generatable',
                              'described_only', 'dropped_open_water')},
                'timing': timing}

    results = []
    for n, target in enumerate(targets):
        bounds = tuple(target['chunk_bounds'])
        work = root / f'{seed}_{n}'
        runtime = work / 'runtime'
        # `generate_world`'s `root` is the SERVER directory; the world lands
        # inside it at `level-name`, which the written server.properties sets
        # to "world". Passing the world path as root put the region files one
        # level deeper than the harvest looked, and extraction reported
        # "expected 3564 full chunks, extracted 0" with empty statuses -- a
        # generated world read as an empty one.
        server_dir = work / 'server'
        world = server_dir / 'world'
        try:
            t = time.perf_counter()
            acquire.prepare_runtime(server_jar, java, runtime)
            acquire.generate_world(seed, server_jar, java, server_dir, runtime, bounds)
            gen = round(time.perf_counter() - t, 1)

            t = time.perf_counter()
            candidate = harvest_seed.harvest(world, seed, chunk_bounds=bounds)
            harvest = round(time.perf_counter() - t, 1)

            # `compile_batch` keys `worlds` by SEED, not by path, and
            # materialises the build world itself under `build_root`. Passing
            # a path-keyed map would look right and silently hand the compiler
            # no world at all; copying the build world here as well would
            # duplicate a copytree it already does.
            # Carry the Types this window was SELECTED for, so `recognize`
            # knows which template's contract applies to it.
            candidate['prospect_types'] = target.get('types') or []
            # The scoop measurement, so `discovered` can report Scale. Without
            # it `certify` passed an empty description and Scale came back
            # None on a map that had a measured area and Homebase separation
            # all along.
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
            t = time.perf_counter()
            compiled = compile_batch.run([path], worlds={seed: world},
                                         build_root=work)
            results.append({
                'target': {k: target[k] for k in
                           ('centre', 'chunk_bounds', 'team_axis',
                            'deviation_over_relief', 'relief_blocks',
                            'water_fraction', 'types')},
                'generate_seconds': gen, 'harvest_seconds': harvest,
                'compile_seconds': round(time.perf_counter() - t, 1),
                'compiled': compiled,
            })
            log(f'  {seed} target {n} at {target["centre"]}: '
                f'gen {gen}s harvest {harvest}s')
        except Exception as exc:                      # noqa: BLE001
            # A failure on one target is data, not the end of the batch. The
            # whole point is a yield number, and dying on the first bad window
            # would report no yield rather than a low one.
            results.append({'target': target['centre'],
                            'error': f'{type(exc).__name__}: {exc}'})
            log(f'  {seed} target {n} FAILED: {type(exc).__name__}: {exc}')

    return {'seed': seed, 'targets_generated': len(results),
            'prospect': {k: found[k] for k in
                         ('considered', 'within_gate', 'generatable',
                          'described_only', 'dropped_open_water')},
            'compilations': results, 'timing': timing}
