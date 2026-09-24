"""Seam 1: is a natural resource opportunity appropriate for where it sits?

THIS IS A CEILING CHECK, NOT A FLOOR CHECK, and that is doctrine rather than
convenience. maps.md is explicit that candidate density, actual density,
exposure and defensibility are **not monotonic with depth**, that "empty or
weak deep terrain is legitimate", that map resource guarantees are
"vocabulary/functionality guarantees, not blanket abundance or regenerative
quotas", and -- on regenerative eligibility -- "do not sprinkle enough
regenerative nodes onto empty cells to satisfy a percentage".

So there is no per-depth quantity a cell owes. Asking "is there enough here"
is the wrong question everywhere except the opening, where the floor is a
VOCABULARY question and belongs to `opening_floor`.

NO NEW NUMBER IS INVENTED HERE. Every figure comes from
`docs/reconciliation/2026-09-14-economic-calibration-recovery.md`, and the
rejecting rule is derived from the Worksite progression in objectives.md 17C
rather than chosen:

    A natural opportunity is progression-breaking where its concentration
    reaches the concentrated exceptional opportunity of the Worksite tier
    that is supposed to introduce that material.

A natural Iron concentration in the opening approaching a Worksite I's ~45 ore
IS Worksite I, arriving free and early. That follows from the existing economy;
it is not a threshold someone picked for the compiler.

WHERE THE REJECTING RULE APPLIES. Only in the opening, because maps.md states
an Opening ceiling there -- "supplies distributed subsistence, not concentrated
windfalls, and should exclude opportunities that skip meaningful early
progression" -- and marks depth bands OPEN everywhere else. Outside the
opening this reports the classification and rejects nothing, because doctrine
gives no band to reject against and inventing one would be worse than the gap.
"""
from __future__ import annotations

# Every entry cites the recovery document. `ordinary_per_team` is a whole-map
# budget for one team, NOT a per-cell figure; `worksite` is the concentrated
# exceptional opportunity of the tier that introduces the material.
#
# NON-CANON CALIBRATION FIXTURES, every one. The recovery document marks them
# so, and they are opportunity-calibration values rather than worldgen quotas.
BUDGETS = {
    'copper': {'ordinary_per_team': (45, 55), 'worksite': (8, 12),
               'worksite_tier': None,
               'note': 'the Copper Worksite was REMOVED; Copper is abundant '
                       'enough to sit in the ordinary opening economy, so the '
                       'worksite figure is historical and cannot be a ceiling'},
    'iron': {'ordinary_per_team': (130, 140), 'worksite': (40, 50),
             'worksite_tier': 1,
             'note': 'Worksite I is Iron + Coal: the first concentrated '
                     'exceptional Iron opportunity'},
    'diamond': {'ordinary_per_team': (14, 18), 'worksite': (8, 12),
                'worksite_tier': 2,
                'note': 'Worksite II is Diamond + Lapis. Ordinary figure is '
                        'late/deep, not opening'},
    'ancient_debris': {'ordinary_per_team': (4, 8), 'worksite': (8, 12),
                       'worksite_tier': 3,
                       'note': 'Worksite III is Ancient Debris + Diamond. Not '
                               'Fortune-amplified; 4 debris per upgrade'},
}

SOURCE = 'docs/reconciliation/2026-09-14-economic-calibration-recovery.md §5,6,8'

# The macro equipment thesis, for reading a classification rather than gating.
THESIS = ('universal Copper through ordinary development; majority Iron '
          'through ordinary development; universal Iron indicates exceptional '
          'opportunity, specialization or mature late play; Diamond stays '
          'selective capital; Netherite stays apex item-level investment')


# The Opening ceiling's NON-ORE exclusions. maps.md lists villages, carrots and
# equipment-sufficient accessible Iron, and records that "carrots and accessible
# iron remain unchecked". Villages are checked by `opening_ceiling`; carrots are
# checked here; the Iron rule still needs a declared equipment target.
#
# CARROTS ARE CHRONOLOGY, NOT A CONTRADICTION. An older contract treated
# wheat/carrot/potato as ordinary starter crop vocabulary -- Alpha was built
# with three starter patches of each, and maps.md still lists carrots among
# "near, basic and staple" candidates under a Historical illustrative label.
# The later compact-Hinterland ceiling supersedes that for the OPENING only.
# It does not make carrots deep; it keeps them out of a small envelope that has
# Wilderness on every side, which is a far weaker restriction than the same
# words would have been under the older broader homeland model.
OPENING_EXCLUDED_VEGETATION = ('carrot',)


def _vegetation_of(cell, kind) -> int:
    veg = cell.get('vegetation') or {}
    return sum(int(v) for k, v in veg.items()
               if isinstance(v, int) and kind in str(k))


def _ore_of(cell, material) -> int:
    ore = cell.get('ore') or cell.get('regional_character') or {}
    if material in ore:
        return int(ore[material])
    # region files name blocks; accept both spellings without guessing others
    for key in (f'{material}_ore', f'deepslate_{material}_ore'):
        if key in ore:
            return int(ore[key])
    return sum(int(v) for k, v in ore.items()
               if isinstance(v, int) and material in str(k))


def classify(count: int, material: str) -> dict:
    """Where a single concentration sits against the recovered budgets."""
    b = BUDGETS.get(material)
    if not b:
        return {'material': material, 'count': count, 'verdict': 'unbudgeted',
                'why': f'no recovered calibration for {material}; it is '
                       f'reported and not judged'}
    lo, hi = b['worksite']
    olo, ohi = b['ordinary_per_team']
    if count >= lo:
        verdict = 'worksite_scale'
        why = (f'{count} reaches the {lo}-{hi} a Worksite of tier '
               f'{b["worksite_tier"]} concentrates')
    elif count >= olo:
        verdict = 'exceptional'
        why = f'{count} is a whole-team ordinary budget ({olo}-{ohi}) in one cell'
    elif count > 0:
        verdict = 'ordinary'
        why = f'{count} is within ordinary distributed opportunity'
    else:
        verdict = 'absent'
        why = 'none here, which doctrine permits at any depth'
    return {'material': material, 'count': count, 'verdict': verdict,
            'why': why, 'worksite_tier': b['worksite_tier'],
            'budget_source': SOURCE}


def assess(cells, *, opening_cost: float = 120.0,
           materials=('iron', 'diamond', 'ancient_debris', 'copper')) -> dict:
    """Classify every cell's concentrations, and reject only in the opening.

    `cells` are `cell_grid.build` rows, which carry `strategic_depth_cost`
    per team. `opening_cost` is the same NON-CANON fixture `opening_access`
    uses for the Hinterland edge; maps.md calls the Hinterland compact and
    gives no figure.
    """
    findings, breaking = [], []
    for cell in cells or ():
        depth = cell.get('strategic_depth_cost') or {}
        reachable = [v for v in depth.values() if v is not None]
        in_opening = bool(reachable) and min(reachable) <= opening_cost
        for material in materials:
            count = _ore_of(cell, material)
            if not count:
                continue
            c = classify(count, material)
            row = {**c, 'cell': cell.get('cell'),
                   'world_origin': cell.get('world_origin'),
                   'strategic_depth_cost': depth, 'in_opening': in_opening}
            findings.append(row)
            # The Opening ceiling is the ONLY place doctrine gives a rule to
            # reject against. Copper is exempt by design: its Worksite was
            # removed precisely because it belongs in the opening economy.
            if (in_opening and c['verdict'] == 'worksite_scale'
                    and BUDGETS[material]['worksite_tier'] is not None):
                breaking.append({**row, 'code': 'OPENING_CEILING_WORKSITE_SCALE',
                                 'detail': f'a Worksite-tier-'
                                           f'{c["worksite_tier"]} {material} '
                                           f'concentration inside the opening '
                                           f'skips the progression that tier '
                                           f'is supposed to introduce'})
    for cell in cells or ():
        depth = cell.get('strategic_depth_cost') or {}
        reachable = [v for v in depth.values() if v is not None]
        if not (reachable and min(reachable) <= opening_cost):
            continue
        for kind in OPENING_EXCLUDED_VEGETATION:
            count = _vegetation_of(cell, kind)
            if count:
                breaking.append({
                    'material': kind, 'count': count, 'verdict': 'excluded',
                    'cell': cell.get('cell'),
                    'world_origin': cell.get('world_origin'),
                    'strategic_depth_cost': depth, 'in_opening': True,
                    'code': 'OPENING_CEILING_EXCLUDED_RESOURCE',
                    'detail': f'{kind} is an explicit Opening ceiling exclusion '
                              f'in maps.md and was previously unchecked'})

    return {
        'measured': True,
        'opening_cost': opening_cost,
        'opening_exclusions_checked': list(OPENING_EXCLUDED_VEGETATION),
        'opening_exclusions_not_checked': [
            'equipment-sufficient accessible Iron, which needs a declared '
            'equipment target that does not exist',
            'villages, which opening_ceiling.py checks separately'],
        'findings': findings,
        'progression_breaking': breaking,
        'rejects': bool(breaking),
        'ceiling_only': 'this is a ceiling check. maps.md marks depth bands OPEN, '
                        'says empty or weak deep terrain is legitimate, and makes '
                        'resource guarantees vocabulary rather than abundance, so '
                        'no cell is rejected for holding too little at any depth.',
        'thesis': THESIS,
        'not_covered': [
            'accessibility, which decides whether a concentration is reachable '
            'at all; caves.py measures volume and exposure and is not read here',
            'Yield, which changes realized material from the same geology',
            'food and renewables, which have no recovered quantity anywhere. '
            'Species and depth placement are explicitly OPEN in maps.md, and a '
            'required-species checklist would reintroduce ecological symmetry '
            'that doctrine denies -- functional opportunity is balanced while '
            'Wilderness is not mirrored',
        ],
    }
