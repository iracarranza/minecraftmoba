"""Describe the N/S terrain contrast under the Task A analytical fit.

This used to name a WEAKER TEAM and a DEFICIT KIND. It no longer does, and the
change is not cosmetic.

Canonical doctrine (specs/superspatialdoctrinespec.md §1): Homebase is symmetric
space, Wilderness is competitive space, and functional opportunity is balanced
while Wilderness is not mirrored. A difference in canopy, elevation, flat land,
biome, coastline or local resources between the two team ends is NOT by itself a
competitive deficit. A competitive deficit exists when geography prevents or
severely distorts a required strategic possibility.

The measurements here remain useful and are unchanged; what is superseded is the
inference drawn from them. So the module still reports the contrast, its
dominant component and every raw number, and it now reports the competitive
consequence as UNKNOWN rather than asserting one -- because nothing in this fit
can tell the two cases apart.

That is not a precaution, it is a finding. On 2026-09-22 the near-miss rescue
test on seed 930010639 authored a 0.3173 accessible-land gap down to a passing
balance scalar of 0.0161 while leaving the gap itself at 0.3173, untouched: the
objective measured travel-cost equality to the opportunities authoring placed,
not which depth bands the terrain has. The scalar agreed the map was rescued.
Whether the gap was ever felt in play was never established, and remains the
open playtest question. See docs/analysis/2026-09-22-nearmiss-rescue-930010639.md
and docs/audit/2026-09-22-spatial-doctrine.md.

Historical JSON written under schema seed_team_axis_diagnosis/1 keeps its
`diagnosis` block and its `weaker_team` / `deficit_kind` keys; this emits
`contrast` under schema /2. The old files are evidence of what was believed,
and rewriting them would destroy that.
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


def describe_contrast(north: dict, south: dict, depth) -> dict:
    """Describe how the two team ends differ, without calling the difference a fault."""
    dn, ds = north['diagnostics'], south['diagnostics']
    lower, higher = ('south', 'north') if south['quality'] < north['quality'] else ('north', 'south')
    w = ds if lower == 'south' else dn
    s = dn if lower == 'south' else ds

    components = {
        'physical_grade': w['severe_grade_fraction'] - s['severe_grade_fraction'],
        'physical_water': w['water_fraction'] - s['water_fraction'],
        'clearable_canopy': w['canopy_fraction'] - s['canopy_fraction'],
        'clearable_open': s['open_fraction'] - w['open_fraction'],
        'usable_extent': (s['usable_area_blocks2'] - w['usable_area_blocks2'])
                         / max(s['usable_area_blocks2'], 1),
    }
    dominant = max(components, key=lambda k: components[k])
    # The character of the difference, as a description of terrain. This is NOT
    # a severity ranking and not a repair instruction: "clearable" says leaves
    # and grass account for most of the contrast, not that the map needs
    # clearing, and "opportunity" says neither grade nor water explains it, not
    # that authoring owes the lower end anything.
    character = ('relief_and_water' if dominant.startswith('physical')
                 else 'vegetation_and_openness' if dominant.startswith('clearable')
                 else 'extent')

    land = {t: accessible_land(depth, t) for t in TEAMS}
    land_gap = gap(land['north'], land['south'])
    if land_gap > gap(north['quality'], south['quality']) and character != 'relief_and_water':
        character = 'extent'
        dominant = 'accessible_land'

    return {
        'lower_quality_end': lower,
        'contrast_character': character,
        'dominant_component': dominant,
        'components': {k: round(v, 4) for k, v in components.items()},
        # The thing this fit cannot answer, stated rather than assumed away.
        'competitive_consequence': 'unknown',
        'consequence_note': (
            'Descriptive only. Establishing a competitive deficit requires showing a '
            'required strategic possibility is prevented or severely distorted -- '
            'Lair access parity, Socket viability, or opening floor/ceiling evidence. '
            'Terrain contrast alone does not screen a seed or justify authoring.'),
    }


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
        'contrast': describe_contrast(homes['north'], homes['south'], depth),
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
          f"{'connected':>11}  contrast (competitive consequence: unknown)")
    for seed in sorted(d.name for d in FINALISTS.iterdir() if d.is_dir()):
        try:
            m = measure(seed)
        except Exception as ex:
            print(f'{seed}: fit failed: {ex}')
            continue
        label = labels.get(seed)
        m['regional_pairing'] = f"{label['west']}/{label['east']}" if label else 'not screened'
        rows.append(m)
        c = m['contrast']
        print(f"{seed:11}{m['regional_pairing']:16}{m['homeland_gap']:>14.4f}"
              f"{m['accessible_land_gap']:>10.4f}{m['connected_workable_gap']:>11.4f}"
              f"  {c['contrast_character']:23} ({c['dominant_component']})")

    if a.out:
        a.out.parent.mkdir(parents=True, exist_ok=True)
        a.out.write_text(json.dumps({
            'schema': 'seed_team_axis_contrast/2',
            'supersedes': 'seed_team_axis_diagnosis/1',
            'axes': {'W-E': 'regional, contrast intentional',
                     'N-S': 'team, relationship -- contrast permitted, opportunity balanced'},
            'interpretation': (
                'Descriptive measurements. N/S Wilderness contrast is permitted by doctrine '
                'and is not a defect by default; no row here screens a seed or requests '
                'authoring. Competitive consequence is unknown in every row.'),
            'seeds': rows}, indent=1))
    return rows


if __name__ == '__main__':
    main()
