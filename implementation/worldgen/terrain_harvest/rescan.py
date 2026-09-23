"""Measure Practical Reach on an authored world, replacing the regional proxy.

The authoring handoff is explicit that this step is mandatory before anything
freezes:

    rescan: Every exact authored placement and Route must be rescanned. Replace
            regional reach proxies with measured exact-world values before
            freezing the map.

The proxy it replaces is an inverse-distance interpolation over the travel
matrix, with 7-8% median relative leave-one-out error, fit *before* any Route
existed. This module instead reads the authored world's own blocks, rebuilds the
traversal graph from them, and runs the same Dijkstra to each authored site.

Two properties make proxy and measurement comparable rather than merely both
plausible:

  * the cost model is `vanilla_search.task_a.PARAMETERS`, unchanged -- the same
    weights that sited the structures, produced the travel matrix, and fed the
    proxy;
  * reach is reported in seconds as `weighted cost / SPRINT`, the optimizer's
    own convention, so a measured value can be substituted directly into its
    balance metric.

No speed bonus is introduced anywhere. A Route is cheaper only because the
terrain work is real: decking removes the water penalty, and clearing headroom
removes the canopy penalty. Surfacing with dirt_path grants nothing on its own,
because in vanilla it does not.
"""
from __future__ import annotations

import argparse
import json
import math
import statistics
from pathlib import Path

from serialization.nbt import plain
from serialization.region import read_region
from vanilla_search.extract import VanillaChunk
from vanilla_search.task_a import PARAMETERS, shortest

SCHEMA = 'map_reach_rescan/1'

SPRINT = 5.612
SPACING = 8
AIR = {'minecraft:air', 'minecraft:cave_air', 'minecraft:void_air'}
WATER = {'minecraft:water', 'minecraft:flowing_water'}
# Blocks that are a walkable authored or natural surface.
NONBUILDABLE = {'minecraft:lava', 'minecraft:flowing_lava', 'minecraft:magma_block'}
CANOPY_DEPTH = 5


def load(world: Path) -> dict:
    out = {}
    for f in sorted((world / 'region').glob('*.mca')):
        for cx, cz, _, root in read_region(f):
            out[(cx, cz)] = VanillaChunk(plain(root))
    return out


def sample(chunks, x: int, z: int, hi: int = 200, lo: int = -64):
    """Surface height and traversal properties of one column, as authored."""
    c = chunks.get((x >> 4, z >> 4))
    if c is None:
        return None
    top = None
    for y in range(hi, lo - 1, -1):
        n = c.block(x, y, z)
        if n and n not in AIR:
            top = (n, y)
            break
    if top is None:
        return None
    name, y = top
    # Canopy is anything solid overhead. Clearing a Route's headroom removes it,
    # which is why an authored corridor measures faster than the raw ground.
    canopy = 0
    for dy in range(1, CANOPY_DEPTH + 1):
        b = c.block(x, y + dy, z)
        if b and b not in AIR:
            canopy = 1
            break
    return {'y': y, 'water': 1 if name in WATER else 0,
            'buildable': 0 if (name in WATER or name in NONBUILDABLE) else 1,
            'canopy': canopy, 'block': name}


def build_graph(chunks, bounds, spacing=SPACING):
    """Traversal graph over the authored world, on the analysis sample grid."""
    xs = list(range(bounds[0] + spacing // 2, bounds[1] + 1, spacing))
    zs = list(range(bounds[2] + spacing // 2, bounds[3] + 1, spacing))
    w, h = len(xs), len(zs)
    cells, coords = [], []
    for z in zs:
        for x in xs:
            cells.append(sample(chunks, x, z))
            coords.append((x, z))

    p = PARAMETERS
    adj = [[] for _ in cells]
    for i, a in enumerate(cells):
        if a is None:
            continue
        ax, az = i % w, i // w
        for dz in (-1, 0, 1):
            for dx in (-1, 0, 1):
                if not (dx or dz):
                    continue
                bx, bz = ax + dx, az + dz
                if not (0 <= bx < w and 0 <= bz < h):
                    continue
                j = bz * w + bx
                b = cells[j]
                if b is None:
                    continue
                rise = abs(b['y'] - a['y'])
                length = math.hypot(dx, dz) * spacing
                factor = (1 + rise * p['rise_cost']
                          + (rise * p['severe_rise_cost'] if rise > p['severe_rise_y'] else 0)
                          + p['water_cost'] * max(a['water'], b['water'])
                          + p['nonbuildable_cost'] * (2 - a['buildable'] - b['buildable']) / 2
                          + p['canopy_cost'] * (a['canopy'] + b['canopy']) / 2)
                adj[i].append((j, length * factor))
    return {'adj': adj, 'coords': coords, 'cells': cells, 'w': w, 'h': h}


def nearest(graph, x, z):
    best, bd = None, None
    for i, (cx, cz) in enumerate(graph['coords']):
        if graph['cells'][i] is None:
            continue
        d = (cx - x) ** 2 + (cz - z) ** 2
        if bd is None or d < bd:
            best, bd = i, d
    return best


def measure(world: Path, candidate: dict, portfolio: dict, routes: dict | None):
    o = candidate['orientation']
    if o['rotation_degrees_clockwise'] or o['east_west_reflected']:
        raise ValueError('rescan assumes the unrotated, unreflected grid this '
                         'seed uses; orient the samples before comparing')
    chunks = load(world)
    bounds = candidate['region']['block_bounds']
    graph = build_graph(chunks, bounds)

    reach = {}
    for team, home in candidate['homelands'].items():
        start = nearest(graph, *home['raw_world'])
        distance, _ = shortest(graph['adj'], {start: 0.0})
        reach[team] = distance

    measured, unreachable = [], []
    for p in portfolio['placements']:
        x, _, z = p['world_xyz']
        node = nearest(graph, x, z)
        row = {'kind': p['kind'], 'detail': p['detail'], 'cell': p['cell'],
               'world_xz': [x, z]}
        ok = True
        for team in ('north', 'south'):
            d = reach[team].get(node)
            if d is None:
                ok = False
                break
            row[team[0]] = round(d / SPRINT, 3)
        if not ok:
            unreachable.append(row)
            continue
        row['gap'] = round(abs(row['n'] - row['s']), 3)
        row['min'] = min(row['n'], row['s'])
        measured.append(row)
    return graph, measured, unreachable


def ndiff(a, b):
    return 0.0 if a + b == 0 else abs(a - b) / ((a + b) / 2)


def rebalance(configuration: dict, measured: list) -> dict:
    """Recompute the optimizer's balance components from measured reach.

    Mirrors map_authoring_optimizer.metrics, restricted to the balance vector.
    Sites are matched by cell and kind, which is how the portfolio names them.
    """
    index = {}
    for m in measured:
        index.setdefault((m['kind'], tuple(m['cell'])), []).append(m)

    def take(kind, items):
        out = []
        for it in items:
            got = index.get((kind, tuple(it['cell'])))
            if got:
                out.append({**it, 'n': got[0]['n'], 's': got[0]['s']})
        return out

    f = take('founder_crop', configuration['founders'])
    r = take('renewable_range', configuration['renewables'])
    w = take('mining_worksite', configuration['worksites'])
    p = take('poi', configuration['pois'])
    rt = {t: take('route_target', v)
          for t, v in configuration['route_targets'].items()}
    if not (f and r and w and p and rt['north'] and rt['south']):
        return {'unresolved': 'not every configuration site was measured'}

    def side(items, team, key):
        return sum(x[key] for x in items if x['bias'] == team)
    comps = {
        'founder': ndiff(side(f, 'north', 'n'), side(f, 'south', 's')),
        'renewable': ndiff(side(r, 'north', 'n'), side(r, 'south', 's')),
        'worksite': ndiff(statistics.mean(x['n'] for x in w),
                          statistics.mean(x['s'] for x in w)),
        'poi': ndiff(sum(x['n'] for x in p), sum(x['s'] for x in p)),
        'route': ndiff(statistics.mean(x['n'] for x in rt['north']),
                       statistics.mean(x['s'] for x in rt['south'])),
    }
    return {'balance_asymmetry': sum(comps.values()) / len(comps),
            'components': comps}


def run(world: Path, candidate_path: Path, portfolio_path: Path,
        routes_path: Path | None, frontier_path: Path, output: Path) -> dict:
    candidate = json.loads(candidate_path.read_text())
    portfolio = json.loads(portfolio_path.read_text())
    routes = json.loads(routes_path.read_text()) if routes_path else None
    frontier = json.loads(frontier_path.read_text())
    profiles = frontier.get('profiles', frontier)
    finalist = profiles[portfolio['profile']]['finalists'][portfolio['finalist_rank']]

    graph, measured, unreachable = measure(world, candidate, portfolio, routes)

    # Proxy value per site, for the error the handoff asks us to retire.
    proxy = {}
    cfg = finalist['configuration']
    for kind, items in (('founder_crop', cfg['founders']),
                        ('renewable_range', cfg['renewables']),
                        ('mining_worksite', cfg['worksites']),
                        ('poi', cfg['pois'])):
        for it in items:
            proxy[(kind, tuple(it['cell']))] = (it['n'], it['s'])
    for team, items in cfg['route_targets'].items():
        for it in items:
            proxy[('route_target', tuple(it['cell']))] = (it['n'], it['s'])

    errors = []
    for m in measured:
        key = (m['kind'], tuple(m['cell']))
        if key not in proxy:
            continue
        pn, ps = proxy[key]
        for team, pv, mv in (('north', pn, m['n']), ('south', ps, m['s'])):
            errors.append({'kind': m['kind'], 'cell': m['cell'], 'team': team,
                           'proxy_s': round(pv, 3), 'measured_s': round(mv, 3),
                           'absolute_s': round(mv - pv, 3),
                           'relative': round((mv - pv) / pv, 4) if pv else None})

    abs_err = [abs(e['absolute_s']) for e in errors]
    rel_err = [abs(e['relative']) for e in errors if e['relative'] is not None]
    result = {
        'schema': SCHEMA,
        'evidence_state': 'RAW WORLD OBSERVATION',
        'world': str(world),
        'profile': portfolio['profile'],
        'finalist_rank': portfolio['finalist_rank'],
        'frontier_sha256': portfolio.get('frontier_sha256'),
        'sample_spacing_blocks': SPACING,
        'graph_nodes': sum(1 for c in graph['cells'] if c is not None),
        'cost_model': 'vanilla_search.task_a.PARAMETERS, unchanged',
        'reach_units': 'seconds, weighted cost / 5.612 sprint blocks per second',
        'routes_present': bool(routes and routes.get('routes')),
        'route_columns': sum(r['columns'] for r in (routes or {}).get('routes', [])),
        'measured': measured,
        'unreachable': unreachable,
        'proxy_error': {
            'samples': len(errors),
            'median_absolute_s': round(statistics.median(abs_err), 3) if abs_err else None,
            'median_relative': round(statistics.median(rel_err), 4) if rel_err else None,
            'max_absolute_s': round(max(abs_err), 3) if abs_err else None,
            'per_site': errors,
        },
        'balance': {
            'proxy': {'balance_asymmetry': finalist['metrics']['balance_asymmetry'],
                      'components': finalist['metrics']['components']},
            'measured': rebalance(cfg, measured),
        },
        'not_covered': [
            'reach from anywhere other than the two homelands',
            'underground travel; this is a surface graph',
            'hostile exposure, which remains UNRESOLVED and is not reach',
            'whether the measured balance passes any threshold; the 0.12 filter '
            'is a NON-CANON ANALYTICAL FIXTURE',
        ],
    }
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(json.dumps(result, indent=1, sort_keys=True) + '\n')
    return result


def main(argv=None):
    a = argparse.ArgumentParser(description=__doc__)
    a.add_argument('--world', type=Path, required=True)
    a.add_argument('--candidate', type=Path, required=True)
    a.add_argument('--portfolio', type=Path, required=True)
    a.add_argument('--routes', type=Path)
    a.add_argument('--frontier', type=Path, required=True)
    a.add_argument('--output', type=Path, required=True)
    n = a.parse_args(argv)
    r = run(n.world, n.candidate, n.portfolio, n.routes, n.frontier, n.output)
    e, b = r['proxy_error'], r['balance']
    mb = b['measured'].get('balance_asymmetry')
    print(f"{r['profile']:22s} nodes={r['graph_nodes']:6d} "
          f"sites={len(r['measured']):3d} unreachable={len(r['unreachable'])}")
    print(f"  proxy error: median {e['median_absolute_s']}s "
          f"({e['median_relative']:.1%} rel), max {e['max_absolute_s']}s"
          if e['median_relative'] is not None else '  proxy error: unresolved')
    print(f"  balance: proxy {b['proxy']['balance_asymmetry']:.4f} -> "
          + (f"measured {mb:.4f}" if mb is not None
             else f"measured {b['measured'].get('unresolved')}"))


if __name__ == '__main__':
    main()
