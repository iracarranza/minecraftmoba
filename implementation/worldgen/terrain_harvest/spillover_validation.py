"""Measure how well the v2 Route-spillover model predicts real collateral effects.

The first physical authoring pass exposed a failure: the optimizer scored a
Route only by its own target's reach, but a corridor speeds travel to everything
near it, and the two teams' corridor sets do not pass the same things. That
moved measured balance by up to 0.049 in either direction.

The v2 optimizer answers with `spillover_reach`, a regional estimate of how much
each opportunity's reach improves because some corridor happens to run past it.
This module asks the only question that settles whether the model learned the
right lesson:

    does the predicted saving match the saving the authored corridor actually
    produced?

Four reach values per opportunity per team:

    regional_raw        the optimizer's pre-Route estimate
    regional_spillover  its estimate once corridors are assumed
    exact_bare          measured on the unauthored world
    exact_authored      measured on the same world with corridors built

Two differences matter, and they are not the same thing:

    predicted saving = regional_raw    - regional_spillover
    actual saving    = exact_sites_only - exact_authored

The baseline is deliberately the **sites-only** world, not the bare one. An
authored world differs from bare by 32 site pads as well as by corridors, and
each pad clears and flattens terrain. Measuring against bare attributes the pad
effect to the Routes -- roughly half of the total saving, as it turns out -- and
would judge the spillover model against an effect it does not claim to predict.

The model is judged on the second pair, not on whether its absolute reach was
right. A model can be wrong about how long a trip takes and still be right about
how much a corridor shortens it -- and for balance, only the second matters.
"""
from __future__ import annotations

import argparse
import json
import math
import statistics
import sys
from pathlib import Path

SCHEMA = 'route_spillover_validation/1'

KIND_OF = {'founders': 'founder_crop', 'renewables': 'renewable_range',
           'worksites': 'mining_worksite', 'pois': 'poi'}


def optimizer_module(repo: Path):
    """Import the committed optimizer, so its own spillover model is used.

    Reimplementing `spillover_reach` here would validate a copy of the model
    rather than the model, which is the mistake this whole exercise exists to
    catch.
    """
    sys.path.insert(0, str(repo / 'tools' / 'analysis'))
    import map_authoring_optimizer as opt
    return opt


def predicted(opt, cfg):
    """Regional raw and spillover reach per opportunity, per team."""
    out = {}
    for section, kind in KIND_OF.items():
        for item in cfg[section]:
            key = (kind, tuple(item['cell']))
            out.setdefault(key, []).append({
                'raw': {'north': item['n'], 'south': item['s']},
                'spillover': {
                    'north': opt.spillover_reach(item, 'north', cfg),
                    'south': opt.spillover_reach(item, 'south', cfg)},
            })
    for team, items in cfg['route_targets'].items():
        for item in items:
            key = ('route_target', tuple(item['cell']))
            out.setdefault(key, []).append({
                'raw': {'north': item['n'], 'south': item['s']},
                'spillover': {
                    'north': opt.spillover_reach(item, 'north', cfg),
                    'south': opt.spillover_reach(item, 'south', cfg)},
            })
    return out


def measured_index(rescan: dict):
    out = {}
    for m in rescan['measured']:
        out.setdefault((m['kind'], tuple(m['cell'])), []).append(
            {'north': m['n'], 'south': m['s']})
    return out


def compare(opt, cfg, bare: dict, authored: dict):
    pred = predicted(opt, cfg)
    b, a = measured_index(bare), measured_index(authored)
    rows = []
    for key, entries in sorted(pred.items()):
        kind, cell = key
        if key not in b or key not in a:
            continue
        for i, p in enumerate(entries):
            if i >= len(b[key]) or i >= len(a[key]):
                continue
            for team in ('north', 'south'):
                raw, spill = p['raw'][team], p['spillover'][team]
                eb, ea = b[key][i][team], a[key][i][team]
                rows.append({
                    'kind': kind, 'cell': list(cell), 'team': team,
                    'regional_raw': round(raw, 3),
                    'regional_spillover': round(spill, 3),
                    'exact_baseline': round(eb, 3),
                    'exact_authored': round(ea, 3),
                    'predicted_saving': round(raw - spill, 3),
                    'actual_saving': round(eb - ea, 3),
                    'saving_error': round((raw - spill) - (eb - ea), 3),
                })
    return rows


def summarise(rows):
    def stats(values):
        if not values:
            return None
        return {'n': len(values),
                'mean': round(statistics.mean(values), 3),
                'median': round(statistics.median(values), 3),
                'max_abs': round(max(abs(v) for v in values), 3)}

    pred = [r['predicted_saving'] for r in rows]
    act = [r['actual_saving'] for r in rows]
    err = [r['saving_error'] for r in rows]

    # Does the model fire where the world actually changed?
    tp = sum(1 for r in rows if r['predicted_saving'] > 1 and r['actual_saving'] > 1)
    fp = sum(1 for r in rows if r['predicted_saving'] > 1 and r['actual_saving'] <= 1)
    fn = sum(1 for r in rows if r['predicted_saving'] <= 1 and r['actual_saving'] > 1)
    tn = len(rows) - tp - fp - fn

    # Correlation only over cases where either side claims an effect; including
    # the many untouched opportunities would inflate agreement for free.
    active = [r for r in rows if r['predicted_saving'] > 1 or r['actual_saving'] > 1]
    corr = None
    if len(active) > 2:
        px = [r['predicted_saving'] for r in active]
        ay = [r['actual_saving'] for r in active]
        mx, my = statistics.mean(px), statistics.mean(ay)
        num = sum((x - mx) * (y - my) for x, y in zip(px, ay))
        den = math.sqrt(sum((x - mx) ** 2 for x in px)
                        * sum((y - my) ** 2 for y in ay))
        corr = round(num / den, 4) if den else None

    by_kind = {}
    for kind in sorted({r['kind'] for r in rows}):
        sub = [r for r in rows if r['kind'] == kind]
        by_kind[kind] = {
            'samples': len(sub),
            'predicted_saving': stats([r['predicted_saving'] for r in sub]),
            'actual_saving': stats([r['actual_saving'] for r in sub]),
            'saving_error': stats([r['saving_error'] for r in sub]),
        }

    return {
        'samples': len(rows),
        'predicted_saving_s': stats(pred),
        'actual_saving_s': stats(act),
        'saving_error_s': stats(err),
        'detection': {'true_positive': tp, 'false_positive': fp,
                      'false_negative': fn, 'true_negative': tn,
                      'note': 'an effect is counted when a saving exceeds 1 second'},
        'correlation_over_active_cases': corr,
        'by_kind': by_kind,
    }


def run(repo: Path, frontier_path: Path, profile: str, rank: int,
        bare_path: Path, authored_path: Path, output: Path,
        baseline_path: Path | None = None) -> dict:
    opt = optimizer_module(repo)
    frontier = json.loads(frontier_path.read_text())
    profiles = frontier.get('profiles', frontier)
    finalist = profiles[profile]['finalists'][rank]
    cfg = finalist['configuration']
    bare = json.loads(bare_path.read_text())
    authored = json.loads(authored_path.read_text())
    # Routes are isolated against the sites-only world when one is supplied.
    baseline = json.loads(baseline_path.read_text()) if baseline_path else bare

    rows = compare(opt, cfg, baseline, authored)
    doc = {
        'schema': SCHEMA,
        'evidence_state': 'DERIVED FROM RAW WORLD OBSERVATION',
        'profile': profile, 'finalist_rank': rank,
        'frontier_sha256': authored.get('frontier_sha256'),
        'baseline': ('sites_only' if baseline_path else 'bare'),
        'baseline_note': 'actual saving is measured against the sites-only world, '
                         'so site-pad terrain clearing is not attributed to Routes',
        'spillover_parameters': {
            'radius_blocks': getattr(opt, 'ROUTE_SPILLOVER_RADIUS', None),
            'scale': getattr(opt, 'ROUTE_SPILLOVER_SCALE', None),
            'target_path_stretch': getattr(opt, 'ROUTE_TARGET_STRETCH', None),
            'status': 'NON-CANON ANALYTICAL FIXTURE'},
        'balance': {
            'regional_raw': finalist['metrics']['balance_asymmetry'],
            'regional_spillover': finalist['metrics']['route_spillover_balance_asymmetry'],
            'regional_effective': finalist['metrics']['effective_balance_asymmetry'],
            'exact_bare': bare['balance']['measured'].get('balance_asymmetry'),
            'exact_baseline': baseline['balance']['measured'].get('balance_asymmetry'),
            'exact_authored': authored['balance']['measured'].get('balance_asymmetry'),
        },
        'summary': summarise(rows),
        'per_opportunity': rows,
        'not_covered': [
            'whether the corridor geometry this validates is the only sensible '
            'one; a different Route width or routing rule would move the actuals',
            'hostile exposure, which remains UNRESOLVED',
            'reach from anywhere but the two homelands',
        ],
    }
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(json.dumps(doc, indent=1, sort_keys=True) + '\n')
    return doc


def main(argv=None):
    a = argparse.ArgumentParser(description=__doc__)
    a.add_argument('--repo', type=Path, required=True)
    a.add_argument('--frontier', type=Path, required=True)
    a.add_argument('--profile', required=True)
    a.add_argument('--rank', type=int, default=0)
    a.add_argument('--bare', type=Path, required=True)
    a.add_argument('--authored', type=Path, required=True)
    a.add_argument('--baseline', type=Path,
                   help='sites-only rescan; isolates Routes from site pads')
    a.add_argument('--output', type=Path, required=True)
    n = a.parse_args(argv)
    d = run(n.repo, n.frontier, n.profile, n.rank, n.bare, n.authored, n.output,
            n.baseline)
    s, b = d['summary'], d['balance']
    print(f"{n.profile:22s} samples={s['samples']:3d}  "
          f"predicted {s['predicted_saving_s']['mean']:+7.2f}s  "
          f"actual {s['actual_saving_s']['mean']:+7.2f}s  "
          f"err median {s['saving_error_s']['median']:+7.2f}s  "
          f"corr {s['correlation_over_active_cases']}")
    d_ = s['detection']
    print(f"  detection: tp={d_['true_positive']} fp={d_['false_positive']} "
          f"fn={d_['false_negative']} tn={d_['true_negative']}")
    print(f"  balance: raw {b['regional_raw']:.4f} spill {b['regional_spillover']:.4f} "
          f"| base {b['exact_baseline']:.4f} authored {b['exact_authored']:.4f}")


if __name__ == '__main__':
    main()
