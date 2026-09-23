"""Compare historical N/S terrain diagnostics under the Task A analytical fit.

22 September spatial doctrine: natural Wilderness differences are permitted.
The emitted deficit/opportunity/clearable/physical labels are legacy hypotheses,
not demonstrated competitive deficits or screening/compensation instructions.
The Core/interface contract and bounded Socket integration must be assessed
separately. No output schema, classifier or threshold is changed in this audit.
See docs/audit/2026-09-22-spatial-doctrine.md.
"""
from __future__ import annotations

import argparse
import json
from pathlib import Path

from vanilla_search.task_a import fit

HERE = Path(__file__).resolve().parents[1]
FINALISTS = HERE / 'results' / 'staged_default_2026-09-09' / 'finalists'

# The band that swamps every comparison. `deep_core_350_plus` is most of the
# map for both teams, so summing all bands made land_asymmetry read 0.0000 for
# every seed -- eight zeros that looked like eight balanced maps and were a
# degenerate statistic. The selected bands count dry sampled Wilderness outside
# both homeland footprints; they do not prove buildability or economic use.
ACCESSIBLE_BANDS = ('opening', 'fringe', 'secondary_core', 'secondary_transition',
                    'tertiary_core', 'deep_transition', 'deep_core')

TEAMS = ('north', 'south')


def gap(a: float, b: float) -> float:
    """Normalised difference between the two TEAM ends."""
    total = a + b
    return 0.0 if total == 0 else abs(a - b) / total


def accessible_land(depth, team) -> int:
    counts = depth[team]['land_band_counts']
    return sum(counts.get(b, 0) for b in ACCESSIBLE_BANDS)


def connected_land(depth, team) -> int:
    """Largest connected workable patch across the reachable bands."""
    access = depth[team].get('core_access') or {}
    return sum(v.get('largest_connected_patch_samples', 0) for v in access.values()
               if v.get('meaningful_area_available'))


def diagnose(north: dict, south: dict, depth) -> dict:
    """Name the deficit from the components that already exist in the fit."""
    dn, ds = north['diagnostics'], south['diagnostics']
    weaker, stronger = ('south', 'north') if south['quality'] < north['quality'] else ('north', 'south')
    w = ds if weaker == 'south' else dn
    s = dn if weaker == 'south' else ds

    components = {
        'physical_grade': w['severe_grade_fraction'] - s['severe_grade_fraction'],
        'physical_water': w['water_fraction'] - s['water_fraction'],
        'clearable_canopy': w['canopy_fraction'] - s['canopy_fraction'],
        'clearable_open': s['open_fraction'] - w['open_fraction'],
        'usable_extent': (s['usable_area_blocks2'] - w['usable_area_blocks2'])
                         / max(s['usable_area_blocks2'], 1),
    }
    dominant = max(components, key=lambda k: components[k])
    kind = ('physical' if dominant.startswith('physical')
            else 'clearable' if dominant.startswith('clearable')
            else 'opportunity')

    land = {t: accessible_land(depth, t) for t in TEAMS}
    opportunity_gap = gap(land['north'], land['south'])
    # A land deficit with no physical cause is what authoring is for.
    if opportunity_gap > gap(north['quality'], south['quality']) and kind != 'physical':
        kind = 'opportunity'
        dominant = 'accessible_land'

    return {'weaker_team': weaker, 'deficit_kind': kind, 'dominant_component': dominant,
            'components': {k: round(v, 4) for k, v in components.items()}}


def measure(seed: str) -> dict:
    candidate = json.loads((FINALISTS / seed / 'candidate.json').read_text())
    r = fit(candidate)
    homes = r['homelands']
    depth = r['regional_depth']

    land = {t: accessible_land(depth, t) for t in TEAMS}
    patch = {t: connected_land(depth, t) for t in TEAMS}
    usable = {t: homes[t]['diagnostics']['usable_area_blocks2'] for t in TEAMS}

    return {
        'seed': seed,
        'homeland_quality': {t: homes[t]['quality'] for t in TEAMS},
        'homeland_gap': gap(homes['north']['quality'], homes['south']['quality']),
        # Replaces the degenerate land_asymmetry.
        'accessible_land_samples': land,
        'accessible_land_gap': gap(land['north'], land['south']),
        'connected_workable_samples': patch,
        'connected_workable_gap': gap(patch['north'], patch['south']),
        'homeland_usable_blocks2': usable,
        'diagnosis': diagnose(homes['north'], homes['south'], depth),
        'failures': r['failures'],
    }


def main(argv=None):
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('--screen', type=Path)
    p.add_argument('--out', type=Path)
    a = p.parse_args(argv)

    labels = {}
    if a.screen and a.screen.exists():
        for line in a.screen.read_text().splitlines():
            if line.strip().endswith('}'):
                row = json.loads(line)
                best = labels.get(str(row['seed']))
                if best is None or row['score'] > best['score']:
                    labels[str(row['seed'])] = row

    rows = []
    print(f"{'seed':11}{'W-E pairing':16}{'N-S homeland':>14}{'land gap':>10}"
          f"{'connected':>11}  deficit")
    for seed in sorted(d.name for d in FINALISTS.iterdir() if d.is_dir()):
        try:
            m = measure(seed)
        except Exception as ex:
            print(f'{seed}: fit failed: {ex}')
            continue
        label = labels.get(seed)
        m['regional_pairing'] = f"{label['west']}/{label['east']}" if label else 'not screened'
        rows.append(m)
        d = m['diagnosis']
        print(f"{seed:11}{m['regional_pairing']:16}{m['homeland_gap']:>14.4f}"
              f"{m['accessible_land_gap']:>10.4f}{m['connected_workable_gap']:>11.4f}"
              f"  {d['deficit_kind']:12} ({d['dominant_component']})")

    if a.out:
        a.out.parent.mkdir(parents=True, exist_ok=True)
        a.out.write_text(json.dumps({'schema': 'seed_team_axis_diagnosis/1',
                                     'axes': {'W-E': 'regional, contrast intentional',
                                              'N-S': 'team, must be competitive'},
                                     'seeds': rows}, indent=1))
    return rows


if __name__ == '__main__':
    main()
