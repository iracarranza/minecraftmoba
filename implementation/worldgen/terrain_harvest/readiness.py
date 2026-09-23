"""Readiness certification: a VerifiedMap is not automatically claimable.

The distinction the pipeline was missing. A **VerifiedMap** is a compiler
result -- the geography satisfies the encoded competitive, spatial and physical
contract. **READY** is an inventory state meaning a real match can be started on
it right now.

They came apart in exactly the way that matters. A map could satisfy every
physical check and enter the pool with its Lair socket chosen, measured and
never actually established, so `alpha.lair.site` stayed empty and the cadence
would have reached UNCONFIGURED on a map the pool called READY. The compiler was
not wrong about the geography; nothing was asking whether the runtime could
bind to it.

So readiness asks one question the compiler does not: **can every system the
match needs resolve a real position on this map?** It is deliberately about
bindings rather than about quality, and it fails closed -- a missing binding is
not a warning.
"""
from __future__ import annotations

from . import objective_forms

# Every binding a match start requires. Named rather than implied, because the
# failure mode here is a system nobody remembered to check.
# The three Worksite nights consume this many sites between them.
#
# It used to be 3 -- one per night -- which was correct only while sunrise
# returned an activated Worksite to Dormant and refilled the eligible pool.
# Activation is now permanent (objectives.md 17C), so the pool drains and never
# refills: at the configured 2/2/3 activations the match needs seven distinct
# sites, and a map with three would open nothing on night 5. The number tracks
# `alpha.worksites.activationsPerTier`, which is itself a NON-CANON FIXTURE.
MIN_WORKSITES = 7

REQUIRED = (
    'world',
    'homelands',
    'fountains',
    'objectives',
    'lair',
    'worksites',
)


def certify(bindings: dict) -> dict:
    """Why this realization cannot be claimed for a match, if it cannot."""
    problems = []

    for key in REQUIRED:
        if not bindings.get(key):
            problems.append({'code': 'MISSING_BINDING', 'binding': key,
                             'detail': f'the match runtime needs {key} and the '
                                       f'manifest does not supply it'})

    world = bindings.get('world') or {}
    if not world.get('name'):
        problems.append({'code': 'NO_WORLD_IDENTITY',
                         'detail': 'the realization does not name its world'})

    for team in ('north', 'south'):
        home = (bindings.get('homelands') or {}).get(team)
        if not home:
            problems.append({'code': 'NO_HOMEBASE', 'team': team,
                             'detail': f'{team} has no Homebase Core position'})
        fountain = (bindings.get('fountains') or {}).get(team)
        if not fountain:
            problems.append({'code': 'NO_FOUNTAIN', 'team': team,
                             'detail': f'{team} has no Aether Fountain position, so '
                                       f'respawn and reconstruction cannot bind'})
        sited = (bindings.get('objectives') or {}).get(team) or {}
        for objective in objective_forms.DEFENSIVE:
            if objective not in sited:
                problems.append({'code': 'NO_OBJECTIVE_BINDING', 'team': team,
                                 'objective': objective,
                                 'detail': f'{team} {objective} has no position; all '
                                           f'three defensive objectives coexist and '
                                           f'each must be reachable by the runtime'})

    lair = bindings.get('lair') or {}
    if not lair.get('anchor'):
        # The defect this module exists for.
        problems.append({'code': 'LAIR_UNCONFIGURED',
                         'detail': 'the Lair socket was chosen but never manifested, '
                                   'so the cadence would reach UNCONFIGURED on night 2 '
                                   'of a map the pool called READY'})
    if lair.get('count', 1) != 1:
        problems.append({'code': 'LAIR_NOT_SINGULAR',
                         'detail': f"there is exactly one Lair; this map records "
                                   f"{lair.get('count')}"})

    sites = bindings.get('worksites') or []
    if len(sites) < MIN_WORKSITES:
        problems.append({'code': 'WORKSITE_PORTFOLIO_TOO_SMALL',
                         'count': len(sites),
                         'minimum': MIN_WORKSITES,
                         'detail': 'activation is permanent, so the eligible pool only '
                                   'shrinks; the three Worksite nights consume '
                                   f'{MIN_WORKSITES} sites between them and none return'})

    return {
        'certified': not problems,
        'problems': problems,
        'checked': list(REQUIRED),
        'note': 'Readiness is about runtime bindings, not about map quality. A '
                'VerifiedMap that fails here is still a VerifiedMap; it is not '
                'claimable until every system can resolve a position on it.',
    }
