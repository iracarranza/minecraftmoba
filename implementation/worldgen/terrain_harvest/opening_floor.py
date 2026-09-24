"""Seam 2: does the Hinterland let a team BEGIN every fundamental verb?

maps.md states the floor and its shape:

    The Hinterland must let a team *begin* every fundamental verb --
    Construction, Extraction, Development, Production, Exploration, the
    movement and organization Logistics needs, and ordinary Combat/PvE. It is
    qualitative: this is deliberately **not** one quota per archetype, and
    invents no counts of trees, animals or ore.

    > The Hinterland must permit every fundamental verb, but should not
    > resolve any of them.

So this is a PRESENCE check, not a quantity one, and the pairing with
`resource_validity` is exact: that module is the ceiling and this is the
floor, and neither asks the other's question. No mobs, no Combat. No ore, no
Extraction. No animals or crops, no Development.

SEVEN VERBS, NOT FIVE. An earlier reading of this seam listed Construction,
Extraction, Logistics, Production and Combat and dropped Development and
Exploration -- the two with named infrastructure anchors in
infrastructure.md, which is what makes them checkable at all.

EVIDENCE IS THE ARCHETYPE'S OWN ANCHOR where one exists, because
infrastructure.md ties each archetype to an ordinary Minecraft object rather
than an abstraction: Exploration to Banners, Logistics to Copper Chests,
Construction to blocks, Development to developable resources and populations.
An archetype that cannot reach its anchor material cannot begin.

PRODUCTION IS NOT INDEPENDENTLY EVIDENCED, and that is reported rather than
papered over. Production is "efficient conversion of acquired inputs into
useful outputs" -- it acts AFTER acquisition, so no ground feature evidences
it on its own. Claiming it from the presence of ore would be asserting a verb
from another verb's evidence.
"""
from __future__ import annotations

# Each verb, its anchor, and what in a characterization stands for it.
# `any_of` means the verb can begin if ANY listed category is non-empty.
VERBS = {
    'extraction': {
        'anchor': 'ore', 'any_of': ('ore',),
        'why': 'no ore, no Extraction'},
    'construction': {
        'anchor': 'ordinary blocks', 'any_of': ('vegetation', 'stone'),
        'why': 'ordinary blocks, not the Construction Block category. '
               'classes.md is explicit that Bricks/Mud Bricks/Terracotta/'
               'Concrete/Glass are not required for valid construction and '
               'that ordinary blocks remain fully useful, so requiring them '
               'here would invent a floor doctrine denies'},
    'development': {
        'anchor': 'developable resources and populations',
        'any_of': ('vegetation', 'fauna'),
        'why': 'Development Zones need something developable -- crops or '
               'animals. infrastructure.md names the anchor directly'},
    'exploration': {
        'anchor': 'Banners', 'any_of': ('fauna',),
        'why': 'Banners are Exploration\'s anchor and need wool, so the '
               'floor is a fauna question. NOTE: this does not check for '
               'SHEEP specifically, only for fauna, because the scan reports '
               'fauna kinds and no recovered rule says which species count'},
    'logistics': {
        'anchor': 'Copper Chests', 'any_of': ('copper',),
        'why': 'Copper Chests are the Logistics anchor. Copper is a Primary '
               'Material and abundant enough that its Worksite was removed, '
               'so its absence from an opening is a real fault'},
    'combat': {
        'anchor': 'hostile mobs', 'any_of': (),
        'why': 'NOT EVIDENCED BY THIS GENERATION METHOD. Worlds are produced '
               'by force-loading chunks with no player present, so hostile '
               'mobs largely never spawn or persist: on seed 3141592 only 7 '
               'of 238 cells hold any hostile at all, against 86 holding '
               'fauna. Reading that as "this map has no Combat" would be '
               'measuring how the world was generated, not what it contains. '
               'Combat needs a spawn-rule check against biome, light and '
               'surface, which nothing does yet'},
    'production': {
        'anchor': None, 'any_of': (),
        'why': 'NOT INDEPENDENTLY EVIDENCED. Production converts acquired '
               'inputs after acquisition, so no ground feature evidences it '
               'alone; claiming it from ore would assert one verb from '
               'another verb\'s evidence'},
}

QUALITATIVE = ('presence only. maps.md makes this floor explicitly qualitative '
               'and "invents no counts of trees, animals or ore", so nothing '
               'here counts anything.')


def _has(cell, category) -> bool:
    if category == 'copper':
        ore = cell.get('ore') or {}
        return any('copper' in str(k) for k in ore)
    if category == 'stone':
        # Stone is not reported as opportunity; a cell with measured surface
        # is standing on it. Treated as satisfied wherever the cell exists,
        # and named so the assumption is visible rather than silent.
        return True
    return bool(cell.get(category))


def permits(cells, *, opening_cost: float = 120.0) -> dict:
    """Which verbs the opening permits, per team.

    A cell counts toward a team's opening when its traversal cost from that
    team's Fountain is within `opening_cost` -- the same NON-CANON fixture
    `opening_access` uses, since maps.md calls the Hinterland compact and
    gives no figure.
    """
    teams = sorted({t for c in (cells or ())
                    for t in (c.get('strategic_depth_cost') or {})})
    if not teams:
        return {'measured': False,
                'why': 'no per-team depth on these cells; run cell_grid.build first'}

    out, missing_any = {}, False
    for team in teams:
        opening = [c for c in cells
                   if (c.get('strategic_depth_cost') or {}).get(team) is not None
                   and c['strategic_depth_cost'][team] <= opening_cost]
        verdicts = {}
        for verb, spec in VERBS.items():
            if not spec['any_of']:
                verdicts[verb] = {'permitted': None, 'why': spec['why']}
                continue
            found = sorted({cat for cat in spec['any_of']
                            for c in opening if _has(c, cat)})
            verdicts[verb] = {'permitted': bool(found), 'evidence': found,
                              'anchor': spec['anchor'], 'why': spec['why']}
            if not found:
                missing_any = True
        out[team] = {'opening_cells': len(opening), 'verbs': verdicts}

    blocked = [{'team': t, 'verb': v, 'code': 'OPENING_FLOOR_VERB_BLOCKED',
                'anchor': VERBS[v]['anchor'], 'detail': VERBS[v]['why']}
               for t, d in out.items()
               for v, r in d['verbs'].items() if r['permitted'] is False]
    return {
        'measured': True, 'opening_cost': opening_cost, 'per_team': out,
        'blocked': blocked, 'rejects': bool(blocked),
        'qualitative': QUALITATIVE,
        'unevidenced_verbs': [v for v, s in VERBS.items() if not s['any_of']],
        'why_unevidenced': {v: s['why'] for v, s in VERBS.items()
                            if not s['any_of']},
        'floor_not_ceiling': 'permits, never resolves. Whether an opening holds '
                             'too MUCH is resource_validity.',
        'not_covered': [
            'whether the evidence is REACHABLE; a cell within traversal cost '
            'still may not expose its ore without a cave mouth',
            'species-level requirements, e.g. sheep for Banners, which no '
            'recovered rule specifies',
        ],
    }
