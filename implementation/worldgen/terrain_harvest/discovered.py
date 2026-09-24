"""Seam 6: the two properties a realization advertises, measured not requested.

The draft shows players Map Type, Scale and Resource Density
(docs/design/MATCH_LIFECYCLE_OBJECTIVES_AND_OPENING_DECISIONS.md), under the
boundary "players know the broad classification; they discover the
realization". Two of the three are properties of the map rather than of the
Type, and the lifecycle design already establishes that Resource Density is
**discovered and measured, not requested from the author**.

SCALE is nearly free: scoop area and Homebase separation, both of which fall
out of `scoop.describe`. Where they cut into Normal/Large/Vast is a labelling
decision that maps.md deliberately leaves unfixed, so this reports the
scalars and no name.

RESOURCE DENSITY is the classification of `resource_validity` findings, and it
is deliberately NOT a single number. The recovery document keeps four
materials on separate budgets with different roles -- Copper ordinary, Iron
volume, Diamond capital, Ancient Debris apex -- and the macro thesis is
stated per material. Collapsing them into one "rich/light" scalar would erase
the distinction the whole economy is built on, so density is reported per
material and the shorthand is derived only where the thesis gives one.

RESOURCE DENSITY IS NOT PRACTICAL ECONOMIC OPPORTUNITY YET. The lifecycle
design says it "should eventually measure practical economic opportunity, not
merely count raw blocks -- accessibility, concentration, cave access, useful
structures, and capabilities unlocked matter." This counts blocks and their
concentration. `caves.py` measures access and is not read here, so the gap is
named rather than closed.
"""
from __future__ import annotations

from .resource_validity import BUDGETS


def resource_density(validity: dict) -> dict:
    """Per-material density, from the classification rather than a new count."""
    findings = (validity or {}).get('findings') or []
    per_material = {}
    for material in BUDGETS:
        rows = [f for f in findings if f['material'] == material]
        total = sum(f['count'] for f in rows)
        lo, hi = BUDGETS[material]['ordinary_per_team']
        per_material[material] = {
            'total_observed': total,
            'cells_holding': len(rows),
            'ordinary_per_team_budget': [lo, hi],
            # Two teams share one map, so the whole-map reference is 2x the
            # per-team budget. Stated rather than assumed, because the budget
            # is written per team and reading it as per map would halve it.
            'whole_map_reference': [lo * 2, hi * 2],
            'relative_to_reference': round(total / (lo * 2), 3) if lo else None,
            'verdicts': {v: sum(1 for f in rows if f['verdict'] == v)
                         for v in ('ordinary', 'exceptional', 'worksite_scale',
                                   'unbudgeted')},
        }
    return {
        'per_material': per_material,
        'single_scalar': 'deliberately absent. Copper, Iron, Diamond and '
                         'Ancient Debris carry different economic roles and '
                         'separate budgets; one rich/light number would erase '
                         'the distinction the economy is built on.',
        'not_yet_practical': 'counts blocks and concentration. Accessibility, '
                             'cave access, useful structures and capabilities '
                             'unlocked are what this should eventually measure; '
                             'caves.py measures access and is not read here.',
    }


def scale(scoop_description: dict) -> dict:
    """The two scalars that separate Normal from Large from Vast."""
    axes = (scoop_description or {}).get('axes') or {}
    seps = {a: d.get('homebase_max_separation_blocks')
            for a, d in axes.items()}
    return {
        'area_blocks2': (scoop_description or {}).get('area_blocks2'),
        'area_over_base': (scoop_description or {}).get('area_over_base'),
        'homebase_max_separation_blocks': seps,
        'named_scale': None,
        'why_unnamed': 'maps.md records these as the two scalars and leaves '
                       'where they cut into Normal/Large/Vast deliberately '
                       'unfixed; naming one here would invent the cut.',
    }


def classify(validity: dict, scoop_description: dict) -> dict:
    return {
        'evidence_state': 'DERIVED MEASUREMENT',
        'resource_density': resource_density(validity),
        'scale': scale(scoop_description),
        'map_type': 'not classified here. A Type is a region in the '
                    'description space and is fitted by map_types predicates, '
                    'which may match several or none.',
    }
