"""Author one map configuration from the base terrain, all steps, in order.

The pipeline has four steps and nothing used to enforce that a configuration ran
all of them. Authoring the portfolio and building the team structures are
separate commands, so a re-author that ran the first and not the second produced
a map with no Aether Fountains -- which is unplayable, because the homelands are
pinned in config and both the victory condition and respawn turn on the
Fountain. It exported cleanly and the failure surfaced as spawning in a field.

So the steps live here together, and the checks that would have caught it run
before anything is published:

    copy base -> portfolio -> routes -> team structures -> verify -> export

Verification is deliberately about the things whose absence is SILENT. A missing
Fountain still exports. A Route with an unclimbable step still exports. Neither
should reach the catalogue.
"""
from __future__ import annotations

import argparse
import json
import shutil
import subprocess
import sys
import time
from pathlib import Path

from serialization.world import block  # noqa: F401  (imported for parity with the steps)
from .map_diff import export
from .verify_portfolio import world_reader

HERE = Path(__file__).resolve().parents[1]
FOUNTAIN = 'minecraft:chiseled_quartz_block'


def run(module: str, *args: str):
    """Run a pipeline step, and stop the whole thing if it fails."""
    command = [sys.executable, '-m', module, *args]
    started = time.time()
    result = subprocess.run(command, cwd=HERE, capture_output=True, text=True)
    if result.returncode != 0:
        raise SystemExit(f'{module} failed:\n{result.stdout}\n{result.stderr}')
    tail = [l for l in result.stdout.strip().splitlines() if l.strip()]
    print(f'  {module.split(".")[-1]:18} {time.time() - started:5.1f}s  '
          f'{tail[0] if tail else ""}')
    return result.stdout


def check_fountains(world: Path, homelands) -> list:
    """The check that was missing. A map without Fountains cannot be played."""
    at, top = world_reader(world)
    problems = []
    for name, (x, z) in homelands.items():
        t = top(x, z)
        if t is None or t[0] != FOUNTAIN:
            problems.append(f'{name} homeland at {x},{z} has '
                            f'{t[0] if t else "nothing"}, expected {FOUNTAIN}')
    return problems


def check_routes(report: Path) -> list:
    """Walkability, from the figures the Route authoring now records itself."""
    data = json.loads(report.read_text())
    problems = []
    for route in data['routes']:
        if route.get('walk_unclimbable', 0):
            problems.append(f"route {route['team']} -> {route['to']} has "
                            f"{route['walk_unclimbable']} unclimbable step(s)")
        if route.get('walk_worst_step', 0) > 1:
            problems.append(f"route {route['team']} -> {route['to']} steps "
                            f"{route['walk_worst_step']} blocks at its worst")
    return problems


def reauthor(base: Path, profile: str, name: str, maps: Path, work: Path,
             frontier: Path, opportunity: Path, candidate: Path, siting: Path,
             note: str = '') -> dict:
    world = work / name
    if world.exists():
        shutil.rmtree(world)
    work.mkdir(parents=True, exist_ok=True)
    shutil.copytree(base, world)
    print(f'{name}: base copied')

    portfolio = work / f'portfolio-{name}.json'
    routes = work / f'routes-{name}.json'
    structures = work / f'structures-{name}.json'

    run('terrain_harvest.author_portfolio',
        '--frontier', str(frontier), '--opportunity', str(opportunity),
        '--profile', profile, '--best', '--world', str(world),
        '--report', str(portfolio), '--apply')
    run('terrain_harvest.routes',
        '--candidate', str(candidate), '--frontier', str(frontier),
        '--profile', profile, '--world', str(world),
        '--report', str(routes), '--placements', str(portfolio), '--apply')
    run('terrain_harvest.build_structures',
        '--world', str(world), '--sites', str(siting), '--report', str(structures))

    homelands = {'north': (-2173, -461), 'south': (-2005, 411)}
    problems = check_fountains(world, homelands) + check_routes(routes)
    if problems:
        raise SystemExit(f'{name} is not publishable:\n  ' + '\n  '.join(problems))
    print(f'  verified           fountains present, no unclimbable Route steps')

    out = maps / f'{name}.json.gz'
    payload, _ = export(base, world, name, out, note)
    print(f'  exported           {payload["blocks"]} blocks, '
          f'{out.stat().st_size / 1024:.0f} KB -> {out}')
    return payload


def main(argv=None):
    reports = HERE / 'reports' / 'expedition_2026-09-20'
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('--base', type=Path,
                   default=Path('/Users/iracarranza/minecraftmoba/artifacts/worldgen/alpha-0.1/base-terrain'))
    p.add_argument('--maps', type=Path,
                   default=Path('/Users/iracarranza/minecraftmoba/artifacts/worldgen/alpha-0.1/maps'))
    p.add_argument('--work', type=Path, default=Path('/tmp/reauthor'))
    p.add_argument('--profile', action='append', required=True,
                   help='profile:name, e.g. consolidative:consolidative_v2')
    p.add_argument('--frontier', type=Path, default=reports / 'portfolios' / 'scenario_frontier.json')
    p.add_argument('--opportunity', type=Path, default=reports / 'opportunity-map.json')
    p.add_argument('--candidate', type=Path,
                   default=HERE / 'results' / 'staged_default_2026-09-09' / 'finalists'
                           / '930012642' / 'candidate.json')
    p.add_argument('--siting', type=Path, default=reports / 'export' / 'data' / 'structure-siting.json')
    p.add_argument('--note', default='')
    a = p.parse_args(argv)

    a.maps.mkdir(parents=True, exist_ok=True)
    for spec in a.profile:
        profile, _, name = spec.partition(':')
        reauthor(a.base.resolve(), profile, name or profile, a.maps.resolve(),
                 a.work.resolve(), a.frontier.resolve(), a.opportunity.resolve(),
                 a.candidate.resolve(), a.siting.resolve(), a.note)


if __name__ == '__main__':
    main()
