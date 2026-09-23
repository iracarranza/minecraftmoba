"""What the three defensive objectives physically are, and what is not yet known.

The repository can build four greybox meshes: `aether_fountain`,
`pillager_outpost`, `nether_bastion` and `end_tower`. Canon has since selected
specific vanilla forms, and two of the three no longer match what is built:

  PILLAGER OUTPOST  the WATCHTOWER ONLY. Cages, tents, log piles and the rest
                    of the compound are not part of the objective footprint.
  NETHER BASTION    the Bridge Bastion's RAMPART / CENTRAL BODY, with the long
                    projecting bridge removed. NOT the older Treasure Room.
  END SPIKE         the Ender Dragon arena's End Spike: obsidian pillar, End
                    Crystal, and the cage where applicable. NOT an End City
                    tower, and NOT the generic End Tower geometry.

`end_tower` is an End stone shaft with a purpur crown. That is the generic End
Tower the spec names and rejects. It is kept, under its own id, as HISTORICAL:
renaming it `end_spike` would be the cheapest possible way to make the
repository look compliant while still placing the wrong structure, and every
artifact already written against it would silently acquire a claim it cannot
support.

None of the three current forms has been MEASURED. The spec is explicit that
retained dimensions "should be measured from the selected form, not guessed",
so this module holds the requirement and the absence of evidence rather than a
plausible number. A candidate cannot be certified against the current contract
until the measurements exist; `certify` says so, by name, instead of passing.
"""
from __future__ import annotations

# The canonical current forms. `span_samples` stays None until someone measures
# the selected vanilla structure; a guess here would propagate into siting,
# clearance and separation as though it were evidence.
FORMS = {
    'pillager_outpost': {
        'label': 'Pillager Outpost',
        'form': 'watchtower only',
        'excludes': ('cages', 'tents', 'log piles', 'peripheral compound'),
        'span_samples': None,
        'source': 'vanilla pillager outpost watchtower',
    },
    'nether_bastion': {
        'label': 'Nether Bastion',
        'form': 'bridge bastion rampart / central body',
        'excludes': ('projecting bridge', 'treasure room'),
        'span_samples': None,
        'source': 'vanilla bastion remnant, bridge variant',
    },
    'end_spike': {
        'label': 'End Spike',
        'form': 'obsidian pillar + End Crystal (+ cage where applicable)',
        'excludes': ('End City tower', 'generic End Tower shaft'),
        'span_samples': None,
        'source': 'vanilla Ender Dragon arena end spike',
    },
}

# Built meshes that predate the selection above. They still place, and the
# artifacts that used them remain readable; they just do not certify.
HISTORICAL = {
    'end_tower': {
        'label': 'End Tower (historical)',
        'superseded_by': 'end_spike',
        'why': 'End stone shaft with a purpur crown; the spec names the generic '
               'End Tower geometry and rejects it. Not renamed: a rename would '
               'assert compliance the mesh does not have.',
    },
}

# Ordinal along the TEAM axis, midline outward to the Fountain. This is spatial,
# not a prerequisite chain: nothing here makes one objective invulnerable until
# another falls, because bypassing an outer defense by terrain, tunnelling,
# bridging or a Route is ordinary Minecraft play.
MIDLINE_TO_FOUNTAIN = ('pillager_outpost', 'nether_bastion', 'end_spike', 'aether_fountain')


def is_historical(structure_id: str) -> bool:
    return structure_id in HISTORICAL


def missing_evidence(structure_id: str) -> list[str]:
    """Why this id cannot be certified against the current contract, if it cannot."""
    if structure_id in HISTORICAL:
        h = HISTORICAL[structure_id]
        return [f"{structure_id} is historical geometry superseded by "
                f"{h['superseded_by']}: {h['why']}"]
    form = FORMS.get(structure_id)
    if form is None:
        return [f'{structure_id} is not a current defensive objective form']
    if form['span_samples'] is None:
        return [f"{structure_id} footprint unmeasured: the {form['form']} has not been "
                f"measured from {form['source']}, and the spec forbids guessing it"]
    return []


def certify(structure_ids) -> dict:
    """Does this set of placed structures satisfy the current objective contract?"""
    ids = list(structure_ids)
    problems = []
    for sid in ids:
        problems.extend(missing_evidence(sid))
    for required in ('pillager_outpost', 'nether_bastion', 'end_spike'):
        if required not in ids:
            problems.append(f'{required} absent: all three defensive objectives coexist')
    return {
        'certified': not problems,
        'missing_evidence': problems,
        'ordinal_contract': list(MIDLINE_TO_FOUNTAIN),
        'note': 'Ordering is spatial, not a prerequisite chain. W-E position and '
                'N-S spacing are variable; only the ordinal is invariant.',
    }


def ordinal_ok(placements) -> list[str]:
    """Check midline -> Outpost -> Bastion -> Spike -> Fountain along the team axis.

    `placements` maps structure id to its distance-toward-midline fraction, where
    1 is at the midline and 0 is at the team's own Fountain. Only the ORDER is
    checked: equal gaps, a straight lane and mirrored coordinates are all
    explicitly not required.
    """
    ordered = [s for s in MIDLINE_TO_FOUNTAIN if s in placements]
    problems = []
    for near, far in zip(ordered, ordered[1:]):
        if placements[near] <= placements[far]:
            problems.append(f'{near} must lie closer to the midline than {far} '
                            f'({placements[near]:.3f} vs {placements[far]:.3f})')
    return problems
