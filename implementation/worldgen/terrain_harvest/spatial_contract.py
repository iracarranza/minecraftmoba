"""The spatial roles a candidate map has to be able to express.

This is a model, not a search. It exists because the previous vocabulary could
not represent the current doctrine: "homeland" was a large radius around the
Fountain, everything inside a team's half was treated as that team's opening
ground, and the two team ends were compared to each other as though similarity
were the goal. All three of those are superseded.

The roles:

  CORE        compact, authored, MIRRORED between the teams. It holds only
              geometry that genuinely requires symmetry -- Fountain, spawn and
              reconstruction positions, the immediately necessary safe space,
              internal connections, and the explicit exits. Symmetry terminates
              at those exits, NOT at a radius. The inclusion test is whether
              leaving the feature to natural generation would create arbitrary
              opening combat or defensive differences between the teams.

  HINTERLAND  compact and NATURAL: the envelope that is no longer Core but has
              not yet become unrestricted Wilderness. It must permit every
              fundamental verb and resolve none of them.

  WILDERNESS  everything else, and it surrounds the Hinterland on every side --
              toward the midline, east and west, and BEHIND the Homebase toward
              that team's own pole. Poleward Wilderness is the case the old
              model could not express at all, because it assumed a team's end
              of the map was that team's ground.

Two things follow that the old code did the opposite of. Wilderness is NOT
mirrored and need not resemble the opponent's, so nothing here compares the two
ends for similarity. And the three defensive objectives are Wilderness
structures: an objective classified into a Hinterland is a contract violation,
not a close call.

The Lair is the exception that proves the rule. There is exactly one, shared,
and it is the single place where parity is a genuine competitive constraint --
so `lair_access_asymmetry` is computed, and no acceptable threshold is asserted,
because none has been established empirically.
"""
from __future__ import annotations

CORE = 'core'
HINTERLAND = 'hinterland'
WILDERNESS = 'wilderness'
ROLES = (CORE, HINTERLAND, WILDERNESS)

# The verbs the Hinterland must permit. Qualitative by design: the spec forbids
# turning this into one quota per archetype, or into X trees / Y cows / Z iron.
FUNDAMENTAL_VERBS = ('construction', 'extraction', 'development', 'production',
                     'exploration', 'logistics', 'combat')

# Opening-ceiling exclusions: opportunities that let a team skip meaningful
# early progression. NOT exhaustive -- the spec says to expect more, so this is
# a seam rather than a closed list.
CEILING_EXCLUSIONS = {
    'village': 'compresses several opening verbs at once',
    'carrot': 'resolves opening food and skips an intended economic step',
    'equipment_sufficient_iron': 'hands a player a finished equipment package as an '
                                 'opening resource; ordinary iron extraction stays permitted',
}


def classify(cell_role: dict) -> dict:
    """Summarise a role assignment, and check the shape doctrine requires."""
    counts = {r: 0 for r in ROLES}
    for role in cell_role.values():
        if role not in counts:
            raise ValueError(f'unknown spatial role: {role}')
        counts[role] += 1
    total = sum(counts.values()) or 1
    return {
        'counts': counts,
        'fractions': {r: counts[r] / total for r in ROLES},
        # Stated rather than assumed: whatever is not Core or Hinterland is
        # Wilderness, including ground behind the Homebase.
        'residual_is_wilderness': True,
    }


def hinterland_problems(summary: dict, team_end_fraction: float) -> list[str]:
    """The Hinterland is compact. It is not the team's end of the map.

    `team_end_fraction` is how much of the sampled world that team's end is, so
    a Hinterland approaching it is the old giant-radial-homeland failure coming
    back under a new name. No exact ceiling is asserted -- what is asserted is
    that the Hinterland must be a proper part of the end, and that some
    Wilderness must exist within it.
    """
    problems = []
    h = summary['fractions'][HINTERLAND]
    if h >= team_end_fraction:
        problems.append(f'hinterland occupies {h:.3f} of the sample, at or beyond the '
                        f'team end ({team_end_fraction:.3f}): a compact opening envelope '
                        f'cannot be the whole end of the map')
    if summary['fractions'][WILDERNESS] == 0:
        problems.append('no Wilderness: the Hinterland must be surrounded, including poleward')
    if summary['fractions'][CORE] == 0:
        problems.append('no Core: the mirrored authored structure has to exist somewhere')
    return problems


def objective_placement_problems(roles: dict) -> list[str]:
    """Defensive objectives are Wilderness structures. None belongs in a Hinterland."""
    problems = []
    for sid, role in roles.items():
        if sid == 'aether_fountain':
            if role != CORE:
                problems.append(f'aether_fountain is in {role}: the Fountain is Core geometry')
            continue
        if role != WILDERNESS:
            problems.append(f'{sid} is in {role}: the three defensive objectives are '
                            f'Wilderness structures and none belongs in the Hinterland')
    return problems


def lair_access_asymmetry(reach_north: float, reach_south: float) -> dict:
    """A_L = |Rn - Rs| / mean(Rn, Rs), over INITIAL practical reach.

    Initial, because map-authored Routes exist at match start and so shape
    opening reach, while later player infrastructure changes runtime reach only
    and must not retroactively rebalance an authored map.

    No acceptable percentage is returned. The spec forbids inventing one in this
    pass, and this is the one number where an invented threshold would decide a
    genuine competitive constraint by accident.
    """
    mean = (reach_north + reach_south) / 2
    if mean == 0:
        return {'asymmetry': None, 'acceptable': 'unknown',
                'why': 'neither team has measured reach to the Lair'}
    return {
        'reach': {'north': reach_north, 'south': reach_south},
        'asymmetry': abs(reach_north - reach_south) / mean,
        'acceptable': 'unknown',
        'why': 'minimise; no canonical threshold is established. Unlike Wilderness '
               'terrain contrast, structurally privileged access to the singular '
               'shared objective is a genuine competitive concern.',
    }


def lair_problems(sites: list) -> list[str]:
    """Exactly one Lair. Not one per boss, not one per team, not a new site per night."""
    if len(sites) == 1:
        return []
    if not sites:
        return ['no Lair socket: the permanent landmark is unconfigured, which is '
                'reportable, but a map with no Lair cannot run the cadence']
    return [f'{len(sites)} Lair sockets: there is exactly one Lair in the world, and '
            f'its occupant changes rather than its location']


def ceiling_findings(observed: dict) -> dict:
    """Opening-ceiling evidence for a Hinterland, including what is not known.

    `observed` maps an exclusion name to True/False/None. None means UNMEASURED
    and stays unmeasured: iron in particular cannot be judged without a declared
    equipment target, and "no iron found by this scan" is not the same claim as
    "not enough iron to equip a player". Basic Extraction must remain possible,
    so a bare iron count would fail the wrong maps.
    """
    violations, unknown = [], []
    for name, why in CEILING_EXCLUSIONS.items():
        seen = observed.get(name)
        if seen is True:
            violations.append(f'{name} in the opening Hinterland: {why}')
        elif seen is None:
            unknown.append(name)
    return {
        'violations': violations,
        'unmeasured': unknown,
        'exhaustive': False,
        'note': 'The exclusion list is a seam, not a closed set; more progression-'
                'defying opportunities are expected. Serious violations should reject '
                'a Socket rather than be authored away by deleting natural features.',
    }


def socket_problems(north: dict, south: dict) -> list[str]:
    """Each Socket must be independently acceptable. They need not resemble each other.

    This function exists mostly to be the place where the old comparison ISN'T.
    A Socket is judged on whether the standardized Core fits it with bounded
    integration -- footprint, interface, clearance, and destruction of meaningful
    natural terrain -- and never on whether it looks like the opposing team's.
    """
    problems = []
    for name, socket in (('north', north), ('south', south)):
        for cause in socket.get('landscape_surgery', ()):
            problems.append(f'{name} socket needs {cause}: reject the socket rather than '
                            f'repair the terrain -- find terrain that makes simple '
                            f'authoring reliable, do not make authoring powerful enough '
                            f'to repair bad terrain')
        if not socket.get('core_fits', True):
            problems.append(f'{name} socket cannot accept the standardized Core')
    return problems
