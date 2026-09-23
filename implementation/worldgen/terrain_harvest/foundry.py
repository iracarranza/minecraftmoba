"""Compile seeds into a READY map pool, offline.

The target architecture is

    MAP FOUNDRY -> READY MAP POOL -> /moba match start -> claim -> IN_USE -> USED

and the point of the pool is that `/moba match start` never searches. Seed
screening rejects about four seeds in five at recognition alone and authoring a
world takes minutes, so a synchronous search inside match start would be a
player staring at a loading screen while a server brute-forces geography. The
foundry does that work beforehand; the runtime only claims.

A pool entry is a directory holding the authored world and a manifest. The
manifest carries provenance rather than just a name, because a map that turns
out to be unbalanced later is only diagnosable if it says which seed, which
compiler constants and which verification evidence produced it.

State lives in the manifest and moves one way: READY -> IN_USE -> USED. Claiming
is the runtime's job and has to be atomic there; this module only ever writes
READY entries, so a foundry run and a live server cannot race over the same file.
"""
from __future__ import annotations

import hashlib
import json
import shutil
import time
from pathlib import Path

from . import build_structures
from .column_scan import World
from .map_compiler import PROVISIONAL, compile_candidate

READY, IN_USE, USED = 'READY', 'IN_USE', 'USED'

SCHEMA = 'moba_map_pool_entry/1'


def _fingerprint(world: Path) -> str:
    """SHA-256 over the region files, so a claimed map can prove it is intact."""
    digest = hashlib.sha256()
    for f in sorted((world / 'region').glob('*.mca')):
        digest.update(f.name.encode())
        digest.update(hashlib.sha256(f.read_bytes()).digest())
    return digest.hexdigest()


def map_id(seed, compilation) -> str:
    """Stable id from the seed and what was actually built on it."""
    payload = json.dumps({'seed': seed,
                          'sites': compilation['evidence'].get('objective_world_xz'),
                          'lair': compilation['evidence'].get('lair_site')},
                         sort_keys=True).encode()
    return f"{seed}-{hashlib.sha256(payload).hexdigest()[:10]}"


def publish(pool: Path, seed, source_world: Path, compilation: dict,
            *, authoring_version='2026-09-23', copy=True) -> dict:
    """Write one verified map into the pool as READY.

    Refuses anything that is not playable. A pool of maps that might be fine is
    not a pool; the whole value of the foundry is that claiming is unconditional
    because everything in it already passed.
    """
    # VERIFIED is not READY. A map can satisfy every physical check and still be
    # unusable -- that is exactly how a realization with an unmanifested Lair
    # entered the pool and would have reached UNCONFIGURED on night 2.
    if not compilation.get('verified', compilation.get('playable')):
        raise ValueError(f'seed {seed} is not a VerifiedMap '
                         f'(reached {compilation.get("deepest_stage_reached")})')
    if not compilation.get('ready'):
        problems = (compilation['evidence'].get('readiness') or {}).get('problems', [])
        raise ValueError(f'seed {seed} is verified but NOT READY: '
                         f'{[p.get("code") for p in problems]}. The pool holds only '
                         f'realizations a match can actually start on.')
    resolved = compilation['evidence'].get('runtime_bindings')
    if not resolved:
        raise ValueError(f'seed {seed} carries no runtime bindings')
    ident = map_id(seed, compilation)
    entry = Path(pool) / ident
    if entry.exists():
        raise FileExistsError(f'{ident} is already in the pool')
    if copy and not Path(source_world).is_dir():
        raise FileNotFoundError(f'no authored world at {source_world}')
    entry.mkdir(parents=True)
    world = entry / 'world'
    if copy:
        shutil.copytree(source_world, world)
    evidence = compilation['evidence']
    manifest = {
        'schema': SCHEMA,
        'map_id': ident,
        'state': READY,
        'published_utc': time.strftime('%Y-%m-%dT%H:%M:%SZ', time.gmtime()),
        'provenance': {
            'seed': seed,
            'map_type': 'default',
            'authoring_version': authoring_version,
            'worldgen_truth': compilation.get('worldgen_truth'),
            'compiler_constants': dict(PROVISIONAL),
            'objective_forms': sorted(build_structures.TEMPLATES),
            'not_reproduced': dict(build_structures.VANILLA_PLACEMENT_GAPS),
        },
        # Everything the match runtime resolves, so a claim does not rediscover
        # the map. No Alpha coordinates and no config fallback.
        'runtime_bindings': resolved,
        'characteristics': {
            # What a future draft may show players. Broad classification only --
            # players know the classification and discover the realization -- so
            # seed, geography, POIs and Lair location stay out of it.
            'map_type': 'default',
            'map_scale': 'normal',
            'resource_density': 'unmeasured',
            'note': 'Resource Density is a discovered property and is not yet '
                    'measured; it is recorded as unmeasured rather than guessed.',
        },
        'evidence': {
            'homelands': evidence.get('homelands'),
            'hinterland': evidence.get('hinterland'),
            'objective_world_xz': evidence.get('objective_world_xz'),
            'lair_site': evidence.get('lair_site'),
            'physical_verification': (evidence.get('physical_verification') or {}).get('verified'),
            'readback': (evidence.get('readback') or {}).get('verified'),
            'lair_verification': evidence.get('lair_verification'),
            'readiness': (evidence.get('readiness') or {}).get('certified'),
            'blocks_written': (evidence.get('authored') or {}).get('blocks_written'),
        },
        'world_fingerprint': _fingerprint(world) if copy else None,
        'history': [{'state': READY, 'at': time.strftime('%Y-%m-%dT%H:%M:%SZ', time.gmtime())}],
    }
    (entry / 'map.json').write_text(json.dumps(manifest, indent=1))
    return manifest


def inventory(pool: Path) -> dict:
    """What the pool holds, by state. The number a foundry runs against."""
    counts = {READY: 0, IN_USE: 0, USED: 0}
    entries = []
    for m in sorted(Path(pool).glob('*/map.json')):
        data = json.loads(m.read_text())
        counts[data.get('state', USED)] = counts.get(data.get('state', USED), 0) + 1
        entries.append({'map_id': data['map_id'], 'state': data['state'],
                        'seed': data['provenance']['seed']})
    return {'pool': str(pool), 'counts': counts, 'entries': entries}


def job(pool: Path, candidates, worlds, *, target_depth, max_attempts=None,
        build_root=Path('/tmp')) -> dict:
    """Publish until the pool holds `target_depth` READY maps, or inputs run out.

    Bounded on purpose. A foundry that runs until it succeeds is a foundry that
    hangs when the geography will not cooperate, so it stops at a target, an
    attempt limit, or the end of its candidates, and says which.
    """
    started = time.time()
    attempts = 0
    published, rejected = [], []
    for path in candidates:
        if inventory(pool)['counts'][READY] >= target_depth:
            return _job_result('target reached', pool, published, rejected, attempts, started)
        if max_attempts is not None and attempts >= max_attempts:
            return _job_result('attempt bound reached', pool, published, rejected,
                               attempts, started)
        attempts += 1
        c = json.loads(Path(path).read_text())
        seed = c.get('seed')
        world = worlds.get(seed)
        if world is None or not Path(world).is_dir():
            rejected.append({'seed': seed, 'why': 'no generated world'})
            continue
        build = Path(build_root) / f'foundry-{seed}'
        if build.exists():
            shutil.rmtree(build)
        shutil.copytree(world, build)
        try:
            result = compile_candidate(c, world, build).as_dict()
            if result.get('ready'):
                published.append(publish(pool, seed, build, result))
            else:
                rejected.append({'seed': seed, 'stage': result['deepest_stage_reached'],
                                 'verified': result.get('verified'),
                                 'codes': [x['code'] for x in result['rejections']]})
        finally:
            shutil.rmtree(build, ignore_errors=True)
    return _job_result('candidates exhausted', pool, published, rejected, attempts, started)


def _job_result(reason, pool, published, rejected, attempts, started):
    elapsed = time.time() - started
    counts = {}
    for r in rejected:
        for code in r.get('codes', [r.get('why', 'unknown')]):
            counts[code] = counts.get(code, 0) + 1
    return {
        'stopped_because': reason,
        'attempts': attempts,
        'published': [m['map_id'] for m in published],
        'rejected': rejected,
        'rejection_codes': counts,
        'inventory': inventory(pool)['counts'],
        'elapsed_seconds': round(elapsed, 1),
        'seconds_per_ready_map': round(elapsed / len(published), 1) if published else None,
    }


def main(argv=None):
    import argparse
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('--pool', type=Path, required=True)
    p.add_argument('--target-depth', type=int,
                   help='publish until the pool holds this many READY maps')
    p.add_argument('--max-attempts', type=int)
    p.add_argument('--candidate', type=Path, action='append', default=[])
    p.add_argument('--world-root', type=Path, default=Path('/tmp'))
    p.add_argument('--build-root', type=Path)
    p.add_argument('--inventory', action='store_true')
    a = p.parse_args(argv)
    if a.inventory:
        print(json.dumps(inventory(a.pool), indent=1))
        return
    if a.target_depth is not None:
        worlds = {}
        for path in a.candidate:
            seed = json.loads(Path(path).read_text()).get('seed')
            world = a.world_root / f'mm-vs-{seed}' / 'world'
            if world.is_dir():
                worlds[seed] = world
        print(json.dumps(job(a.pool, a.candidate, worlds,
                             target_depth=a.target_depth,
                             max_attempts=a.max_attempts,
                             build_root=a.build_root or Path('/tmp')), indent=1))
        return
    published, rejected = [], []
    for path in a.candidate:
        c = json.loads(path.read_text())
        seed = c['seed']
        world = a.world_root / f'mm-vs-{seed}' / 'world'
        if not world.exists():
            rejected.append({'seed': seed, 'why': 'no generated world'})
            continue
        build = (a.build_root or Path('/tmp')) / f'foundry-{seed}'
        if build.exists():
            shutil.rmtree(build)
        shutil.copytree(world, build)
        result = compile_candidate(c, world, build).as_dict()
        if result.get('ready'):
            published.append(publish(a.pool, seed, build, result))
        else:
            rejected.append({'seed': seed, 'stage': result['deepest_stage_reached'],
                             'verified': result.get('verified'),
                             'codes': [x['code'] for x in result['rejections']]})
        shutil.rmtree(build, ignore_errors=True)
    print(json.dumps({'published': [m['map_id'] for m in published],
                      'rejected': rejected,
                      'inventory': inventory(a.pool)['counts']}, indent=1))


if __name__ == '__main__':
    main()
