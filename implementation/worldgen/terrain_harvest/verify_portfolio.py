"""Read an authored world back out of its region files and check it.

Block counters report what the editor thought it wrote. This reads the world
instead, which is how both real authoring bugs so far were caught: sites
stacked on one another at a shared cell centre, and a Route corridor
overwriting a POI foundation.

Route corridors are allowed to cross authored sites -- the design document
values POIs partly for the traffic they attract, and diverting a corridor would
alter the strategic geometry the optimizer chose. So a crossing recorded in the
Route report is accepted here, and an unrecorded one is a failure.
"""
from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path

from serialization.nbt import plain
from serialization.region import read_region
from vanilla_search.extract import VanillaChunk

CROP = {'wheat': 'minecraft:wheat', 'carrot': 'minecraft:carrots',
        'potato': 'minecraft:potatoes', 'beetroot': 'minecraft:beetroots'}
ROUTE_SURFACE = {'minecraft:dirt_path', 'minecraft:oak_planks'}
SOLID_SKIP = {'minecraft:air', 'minecraft:cave_air'}


def world_reader(world: Path):
    chunks = {}
    for f in sorted((world / 'region').glob('*.mca')):
        for cx, cz, _, root in read_region(f):
            chunks[(cx, cz)] = VanillaChunk(plain(root))

    def at(x, y, z):
        # Look the chunk up per coordinate: a footprint crosses chunk edges,
        # and reusing the centre's chunk silently reads the wrong blocks.
        c = chunks.get((x >> 4, z >> 4))
        return c.block(x, y, z) if c else None

    def top(x, z, hi=200, lo=-64):
        c = chunks.get((x >> 4, z >> 4))
        if c is None:
            return None
        for y in range(hi, lo - 1, -1):
            n = c.block(x, y, z)
            if n and n not in SOLID_SKIP:
                return (n, y)
        return None
    return at, top


def check_portfolio(at, report, crossed):
    ok, failures = 0, []
    seen = {}
    for p in report['placements']:
        x, y, z = p['world_xyz']
        kind = p['kind']
        if (x, z) in seen:
            failures.append(f'COLLISION {kind} and {seen[(x, z)]} at {x},{z}')
        seen[(x, z)] = kind
        tolerant = (x, z) in crossed

        if kind == 'founder_crop':
            checks = [(at(x, y - 1, z), 'minecraft:water'),
                      (at(x + 2, y - 1, z), 'minecraft:farmland'),
                      (at(x + 2, y, z), CROP[p['detail']]),
                      (at(x, y - 1, z + 3), 'minecraft:farmland')]
        elif kind == 'renewable_range':
            checks = [(at(x, y - 1, z), 'minecraft:grass_block')]
        elif kind == 'mining_worksite':
            checks = [(at(x, y - 3, z), 'minecraft:air'),
                      (at(x, y, z + 3), 'minecraft:polished_deepslate')]
        elif kind == 'poi':
            checks = [(at(x, y - 1, z), 'minecraft:dirt'),
                      (at(x + 24, y + 6, z + 24), 'minecraft:sea_lantern')]
        elif kind == 'route_target':
            checks = [(at(x, y, z), 'minecraft:chiseled_stone_bricks'),
                      (at(x, y + 3, z), 'minecraft:lantern')]
        else:
            continue

        for got, want in checks:
            if got == want or (tolerant and got in ROUTE_SURFACE):
                ok += 1
            else:
                failures.append(f'{kind} {p["detail"] or ""} @{x},{y},{z}: '
                                f'got {got} want {want}')
    return ok, failures


def check_routes(at, top, report):
    ok, failures = 0, []
    for r in report['routes']:
        for label in ('from', 'to'):
            x, z = r[label]
            t = top(x, z)
            if not t or t[0] not in ROUTE_SURFACE:
                failures.append(f'route {r["team"]} {label} {x},{z} not surfaced: {t}')
                continue
            ok += 1
            if all(at(x, t[1] + dy, z) == 'minecraft:air' for dy in (1, 2, 3)):
                ok += 1
            else:
                failures.append(f'route {r["team"]} {label} {x},{z} lacks headroom')
    return ok, failures


def verify(world: Path, portfolio: Path, routes: Path | None):
    at, top = world_reader(world)
    port = json.loads(portfolio.read_text())
    route = json.loads(routes.read_text()) if routes else None
    crossed = {tuple(c['world_xz']) for c in (route or {}).get('crossings', [])}
    ok, failures = check_portfolio(at, port, crossed)
    if route:
        o, f = check_routes(at, top, route)
        ok += o; failures += f
    return ok, failures


def main(argv=None):
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('--world', type=Path, required=True)
    p.add_argument('--portfolio', type=Path, required=True)
    p.add_argument('--routes', type=Path)
    p.add_argument('--label', default='')
    a = p.parse_args(argv)
    ok, failures = verify(a.world, a.portfolio, a.routes)
    for f in failures:
        print(f'  FAIL {f}')
    print(f'{a.label or a.world.name:22s} {ok:3d} assertions ok, {len(failures)} failed')
    return 1 if failures else 0


if __name__ == '__main__':
    sys.exit(main())
