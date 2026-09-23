"""Run many candidates through the complete compiler and report the yield.

One compiled map proves the pipeline can work. A yield curve says whether it
works repeatably, and where it actually spends its rejections -- which is the
number a map foundry has to plan around, not the number of successes.
"""
from __future__ import annotations

import collections
import json
from pathlib import Path

from .map_compiler import STAGES, compile_candidate


def run(candidates, worlds=None, build_root=None):
    runs = []
    for path in candidates:
        c = json.loads(Path(path).read_text())
        seed = c.get('seed')
        world = (worlds or {}).get(seed)
        build = None
        if build_root and world:
            build = Path(build_root) / f'build-{seed}'
        runs.append(compile_candidate(c, world, build).as_dict())
    stages = collections.Counter(r['deepest_stage_reached'] for r in runs)
    codes = collections.Counter(x['code'] for r in runs for x in r['rejections'])
    playable = [r['seed'] for r in runs if r['playable']]
    return {
        'schema': 'map_compilation_batch/1',
        'candidates': len(runs),
        'playable': playable,
        'yield': round(len(playable) / len(runs), 4) if runs else 0.0,
        'deepest_stage': {s: stages.get(s, 0) for s in STAGES},
        'rejection_codes': dict(codes),
        'runs': runs,
    }


def main(argv=None):
    import argparse
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('candidate', type=Path, nargs='+')
    p.add_argument('--world-root', type=Path,
                   help='directory holding mm-vs-<seed>/world')
    p.add_argument('--out', type=Path)
    a = p.parse_args(argv)
    worlds = {}
    if a.world_root:
        for d in Path(a.world_root).glob('mm-vs-*/world'):
            try:
                worlds[int(d.parent.name.removeprefix('mm-vs-'))] = d
            except ValueError:
                continue
    result = run(a.candidate, worlds)
    print(json.dumps({k: v for k, v in result.items() if k != 'runs'}, indent=1))
    if a.out:
        a.out.parent.mkdir(parents=True, exist_ok=True)
        a.out.write_text(json.dumps(result, indent=1))
    return result


if __name__ == '__main__':
    main()
