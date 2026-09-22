"""Does the balance machinery work on seeds that are not ocean-and-highland?

The staged screen only ever accepted one composition, so every balance number
this project has was measured on that one geography. The claim worth testing is
that the STRUCTURAL measures -- homeland quality, per-team regional depth, route
fitting -- do not care what the biomes are.

This runs Task A's existing fit over finalist candidates and reports the same
figures for each, alongside the region-character pairing the new screen assigns.
Nothing here is new analysis: it is the project's own fit, applied to seeds it
was never pointed at.

Authoring is the point of the exercise. A seed whose two teams start unequal is
not thereby a bad seed -- it is a seed whose imbalance authored opportunities
may be able to correct, and identifying those is what this comparison is for.
"""
from __future__ import annotations

import argparse
import json
from pathlib import Path

from vanilla_search.task_a import fit

HERE = Path(__file__).resolve().parents[1]
FINALISTS = HERE / 'results' / 'staged_default_2026-09-09' / 'finalists'


def asymmetry(a: float, b: float) -> float:
    """Normalised difference, so teams can be compared across seeds."""
    total = a + b
    return 0.0 if total == 0 else abs(a - b) / total


def measure(seed: str) -> dict:
    candidate = json.loads((FINALISTS / seed / 'candidate.json').read_text())
    r = fit(candidate)
    north, south = r['homelands']['north'], r['homelands']['south']
    depth = r['regional_depth']

    def land(team):
        return sum(depth[team]['land_band_counts'].values())

    routes = [x for x in r['routes'] if x.get('team')]
    per_team = {t: sum(1 for x in routes if x['team'] == t) for t in ('north', 'south')}

    return {
        'seed': seed,
        'homeland_quality': {'north': north['quality'], 'south': south['quality']},
        'homeland_asymmetry': asymmetry(north['quality'], south['quality']),
        'land_samples': {'north': land('north'), 'south': land('south')},
        'land_asymmetry': asymmetry(land('north'), land('south')),
        'routes_fitted': per_team,
        'failures': r['failures'],
    }


def main(argv=None):
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('--screen', type=Path, help='region-screen JSONL, to label each seed')
    p.add_argument('--out', type=Path)
    a = p.parse_args(argv)

    labels = {}
    if a.screen and a.screen.exists():
        for line in a.screen.read_text().splitlines():
            if not line.strip().endswith('}'):
                continue
            row = json.loads(line)
            best = labels.get(str(row['seed']))
            if best is None or row['score'] > best['score']:
                labels[str(row['seed'])] = row

    rows = []
    for seed in sorted(d.name for d in FINALISTS.iterdir() if d.is_dir()):
        try:
            m = measure(seed)
        except Exception as ex:                      # a candidate that cannot be fitted
            print(f'{seed}: fit failed: {ex}')
            continue
        label = labels.get(seed)
        m['pairing'] = f"{label['west']}/{label['east']}" if label else 'not screened'
        m['contrast'] = label['contrast'] if label else None
        rows.append(m)
        print(f"{seed}  {m['pairing']:16} homeland {m['homeland_quality']['north']:.3f}/"
              f"{m['homeland_quality']['south']:.3f} asym {m['homeland_asymmetry']:.4f}   "
              f"land asym {m['land_asymmetry']:.4f}   failures {len(m['failures'])}")

    if a.out:
        a.out.parent.mkdir(parents=True, exist_ok=True)
        a.out.write_text(json.dumps({'schema': 'seed_balance_comparison/1', 'seeds': rows}, indent=1))
    return rows


if __name__ == '__main__':
    main()
