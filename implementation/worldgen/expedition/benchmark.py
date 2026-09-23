"""Time-to-benchmark: how long extracting Q units of a resource actually takes.

This is the focused case of the pending 7v7 simulation. It answers one question
per run -- given the map as measured, how many minutes of a match does a player
spend to put Q units of a resource in hand, and which strategy does the map
reward -- without needing any of the combat, XP or objective modelling that the
full simulation would.

Outputs per strategy and per resource, so the comparison that matters is the
headline: if blind digging beats walking caves, the map is not rewarding
practised underground behaviour, and the measured exposed fraction is why.
"""
from __future__ import annotations

import argparse
import json
from pathlib import Path

from . import vanilla
from .strategy import (SWEEP_BLOCKS_PER_SECOND, Deposit, Kit, branch_mining_seconds,
                       cave_exploration_seconds, quarry_seconds)

SCHEMA = 'expedition_benchmark/1'


def deposits(manifest: dict, resource: str) -> list[Deposit]:
    out = []
    for m in manifest['manifestations']:
        if m['resource'] != resource:
            continue
        band = m.get('best_band')
        out.append(Deposit(
            resource=resource,
            blocks_exposed=m['blocks_exposed'],
            blocks_buried=m['blocks_buried'],
            deepslate_fraction=m['deepslate_fraction'] or 0.0,
            mean_y=m['mean_y'] or 0.0,
            cave_volume=m['cave']['explorable_volume'],
            density_at_depth=band['density'] if band else 0.0,
        ))
    return out


def pooled(deposits_: list[Deposit], resource: str) -> Deposit:
    """Aggregate the scanned cells into one representative deposit.

    Weighting deepslate share and density by physical blocks keeps the pooled
    figure a real average of what was measured rather than an average of ratios.
    """
    total = sum(d.blocks for d in deposits_) or 1
    return Deposit(
        resource=resource,
        blocks_exposed=sum(d.blocks_exposed for d in deposits_),
        blocks_buried=sum(d.blocks_buried for d in deposits_),
        deepslate_fraction=sum(d.deepslate_fraction * d.blocks for d in deposits_) / total,
        mean_y=sum(d.mean_y * d.blocks for d in deposits_) / total,
        cave_volume=sum(d.cave_volume for d in deposits_),
        density_at_depth=sum(d.density_at_depth * d.blocks for d in deposits_) / total,
    )


def evaluate(deposit: Deposit, kit: Kit, quantity: float) -> dict:
    """Every strategy's time to reach `quantity` items of the resource."""
    blocks = vanilla.blocks_required(quantity, deposit.resource, kit.fortune)
    if not vanilla.can_harvest(deposit.resource, kit.tool):
        return {'unresolved': f'{kit.tool} pickaxe cannot harvest {deposit.resource}'}

    results = {
        'branch_mining': branch_mining_seconds(deposit, kit, blocks),
        'quarry': quarry_seconds(deposit, kit, blocks),
    }
    for sweep in SWEEP_BLOCKS_PER_SECOND:
        results[f'cave_exploration@{int(sweep)}'] = cave_exploration_seconds(
            deposit, kit, blocks, sweep)

    return {
        'quantity': quantity,
        'blocks_required': blocks,
        'expected_harvest': round(vanilla.expected_harvest(blocks, deposit.resource,
                                                           kit.fortune), 1),
        'blocks_available_exposed': deposit.blocks_exposed,
        'blocks_available_total': deposit.blocks,
        'ore_break_seconds': round(deposit.mean_break_time(kit), 3),
        'overburden_break_seconds': round(deposit.overburden_break_time(kit), 3),
        'density_at_depth': round(deposit.density_at_depth, 6),
        'strategies': {
            k: ({**v, 'minutes': round(v['seconds'] / 60, 1)} if v.get('feasible') else v)
            for k, v in results.items()
        },
    }


def run(manifest_path: Path, output: Path, quantity: float, resources, kits) -> dict:
    manifest = json.loads(manifest_path.read_text())
    report = {
        'schema': SCHEMA,
        'evidence_state': 'DERIVED FROM RAW WORLD OBSERVATION + VANILLA FORMULAS',
        'manifest': str(manifest_path),
        'volume_id': manifest.get('volume_id'),
        'cells_scanned': manifest.get('coverage', {}).get('cells_scanned'),
        'results': {},
        'parameter_sources': {
            'block break time': 'CANON (vanilla formula)',
            'block hardness': 'MAP MEASUREMENT (Paper fixture, moba curve 2026-09-20)',
            'walk/sprint speed': 'CANON (vanilla)',
            'ore counts, depth, deepslate share, cave volume': 'MAP MEASUREMENT',
            'Fortune expected multipliers': 'CANON / WORKING DESIGN (calculator spec s8)',
            'cave sweep rate': 'SENSITIVITY / NON-CANON (reported as a band)',
            'branch inspected-per-dug = 4': 'CANON (1x2 branch geometry)',
        },
        'not_covered': [
            'travel from base to the cave mouth; the manifest has no measured routes',
            'search time to find the deposit in the first place',
            'Hunger, durability, inventory trips and return delivery, which the '
            'full calculator adds once routes exist',
            'hostile interruption underground',
        ],
    }
    for resource in resources:
        ds = deposits(manifest, resource)
        if not ds:
            report['results'][resource] = {'unresolved': 'no manifestation in manifest'}
            continue
        pool = pooled(ds, resource)
        report['results'][resource] = {
            'cells': len(ds),
            'mean_y': round(pool.mean_y, 1),
            'deepslate_fraction': round(pool.deepslate_fraction, 4),
            'by_kit': {name: evaluate(pool, kit, quantity) for name, kit in kits.items()},
        }
    output.write_text(json.dumps(report, indent=1, sort_keys=True) + '\n')
    return report


DEFAULT_KITS = {
    # The project's working progression landmarks: a starting stone pick, then
    # Lv4 Efficiency I + Unbreaking I, then Lv7 Yield I / Fortune I.
    'stone_lv1': Kit(tool='stone'),
    'iron_lv4': Kit(tool='iron', efficiency=1),
    'iron_lv7': Kit(tool='iron', efficiency=1, fortune=1),
    'diamond_lv7': Kit(tool='diamond', efficiency=1, fortune=1),
}


def main(argv=None):
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('--manifest', type=Path, required=True)
    p.add_argument('--output', type=Path, required=True)
    p.add_argument('--quantity', type=float, default=182)
    p.add_argument('--resources', nargs='+', default=['copper', 'iron', 'diamond'])
    a = p.parse_args(argv)
    report = run(a.manifest, a.output, a.quantity, a.resources, DEFAULT_KITS)

    for resource, r in report['results'].items():
        if 'unresolved' in r:
            print(f"{resource}: UNRESOLVED {r['unresolved']}"); continue
        print(f"\n== {resource}  Q={a.quantity}  "
              f"meanY={r['mean_y']} deepslate={r['deepslate_fraction']}")
        for kit, e in r['by_kit'].items():
            if 'unresolved' in e:
                print(f"  {kit:12s} UNRESOLVED {e['unresolved']}"); continue
            best = min((k for k, v in e['strategies'].items() if v.get('feasible')),
                       key=lambda k: e['strategies'][k]['seconds'], default=None)
            print(f"  {kit:12s} blocks={e['blocks_required']:5d} "
                  f"break={e['ore_break_seconds']}s")
            for name, v in e['strategies'].items():
                if not v.get('feasible'):
                    print(f"      {name:22s} infeasible: {v['reason']}"); continue
                short = f" SHORT BY {v['short_by']}" if v.get('short_by') else ''
                mark = ' <-- fastest' if name == best else ''
                print(f"      {name:22s} {v['minutes']:7.1f} min{short}{mark}")
    print(f"\nwrote {a.output}")


if __name__ == '__main__':
    main()
