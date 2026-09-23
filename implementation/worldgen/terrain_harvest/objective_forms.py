"""What the three defensive objectives physically are, measured from the game.

Canon selects specific vanilla forms:

  PILLAGER OUTPOST  the WATCHTOWER ONLY. Cages, tents, log piles and the rest
                    of the compound are not part of the objective footprint.
  NETHER BASTION    the Bridge Bastion's RAMPART / CENTRAL BODY, with the long
                    projecting bridge removed. NOT the older Treasure Room.
  END SPIKE         the Ender Dragon arena's End Spike: obsidian pillar, End
                    Crystal, and the cage where applicable. NOT an End City
                    tower, and NOT the generic End Tower geometry.

All three are now MEASURED rather than guessed, and the measurements come from
Minecraft itself (`reports/objective_forms_2026-09-23/measurements.json`, from
`terrain_harvest.vanilla_assets`):

  - The Outpost and Bastion are read out of the client jar's own structure NBT.
    "Watchtower only" is a clean cut because vanilla already ships the
    peripheral pieces as separate files; dropping the projecting bridge is
    likewise a file-level exclusion, not a judgement about one mesh.
  - The End Spike is not a structure file at all -- vanilla generates the arena
    spikes in code -- so it was measured from a real generated End dimension on
    a disposable server. Ten spikes, radius 2-5, height 76-103, two of ten
    caged.

The three contracts are deliberately NOT one bounding-box concept. What a socket
has to provide differs in kind:

  OUTPOST     modest footprint, modest height, real interior navigation, the
              whole thing meets the ground.
  BASTION     large irregular multilevel body, heavy ground contact, real
              interior navigation and approach requirements.
  END SPIKE   tiny footprint and essentially no interior, but an enormous
              vertical requirement -- up to 103 blocks of clear sky. That is
              the measurement most likely to reject a socket, and it would have
              been invisible under a single span number.

`end_tower` -- the End stone shaft with a purpur crown this repository builds --
is the generic End Tower geometry the spec names and rejects. It is kept, under
its own id, as HISTORICAL. Renaming it `end_spike` would be the cheapest
possible way to look compliant while still placing the wrong structure, and
every artifact already written against it would silently acquire a claim it
cannot support.
"""
from __future__ import annotations

import json
from pathlib import Path

MEASUREMENTS = (Path(__file__).resolve().parents[1]
                / 'reports' / 'objective_forms_2026-09-23' / 'measurements.json')


def _load() -> dict:
    if not MEASUREMENTS.exists():
        return {}
    return json.loads(MEASUREMENTS.read_text()).get('forms', {})


_MEASURED = _load()


def _authored_geometry(key) -> dict | None:
    """Measure the template that actually gets built.

    The contract used to come from a separate measurement pass over the source
    NBT, and the builder from its own template, and the two drifted: the
    Bastion's recorded height was the tallest single piece (32) while the
    assembled body is two pieces stacked (64). A site was then verified against
    one number and built to another.

    Deriving the contract from `build_structures.TEMPLATES` removes the class of
    bug rather than the instance. Provenance still comes from the measurement
    file -- what the form IS, and where it came from -- but its dimensions come
    from the thing that will be placed.
    """
    from . import build_structures
    template = build_structures.TEMPLATES.get(key)
    if template is None:
        return None
    blocks = template()
    if not blocks:
        return None
    xs = [d[0] for d in blocks]
    ys = [d[1] for d in blocks]
    zs = [d[2] for d in blocks]
    floor = min(ys)
    contact = {(d[0], d[2]) for d in blocks if d[1] == floor}
    return {
        'span_samples': max(max(xs) - min(xs), max(zs) - min(zs)) + 1,
        'height': max(ys) - min(ys) + 1,
        'vertical_clearance': max(ys) - min(ys) + 1,
        'ground_contact_columns': len(contact),
        'solid_blocks': len(blocks),
    }


def _contract(key, **extra) -> dict:
    m = _MEASURED.get(key, {})
    built = _authored_geometry(key)
    out = {
        'evidence': m.get('evidence', 'missing'),
        'source': m.get('source'),
        'form': m.get('form'),
        'dimensions_from': 'authored template' if built else 'measurement file',
        'span_samples': m.get('span_samples'),
        'height': m.get('height') or m.get('vertical_clearance'),
        'vertical_clearance': m.get('vertical_clearance'),
        'ground_contact_columns': m.get('ground_contact_columns'),
        'solid_blocks': m.get('solid_blocks'),
        'interior_navigation': m.get('interior_navigation'),
    }
    if built:
        out.update(built)
    out.update(extra)
    return out


# The current canonical forms, with the physical contract a socket recognizer
# actually needs from each. `needs` names what terrain must supply; it is not a
# uniform bounding box.
FORMS = {
    'pillager_outpost': _contract(
        'pillager_outpost',
        excludes=('cages', 'tents', 'log piles', 'peripheral compound'),
        needs=('footprint support', 'terrain contact across the base',
               'one usable entrance approach', 'modest vertical clearance'),
        multilevel=False,
    ),
    'nether_bastion': _contract(
        'nether_bastion',
        excludes=('projecting bridge', 'supporting legs', 'treasure room'),
        needs=('large irregular footprint', 'heavy terrain contact',
               'multilevel interior navigation', 'several approach faces',
               'clearance around the rampart'),
        multilevel=True,
    ),
    'end_spike': _contract(
        'end_spike',
        excludes=('End City tower', 'generic End Tower shaft'),
        needs=('small ground footprint', 'very large vertical clearance',
               'exposure/visibility', 'one approach to the base'),
        multilevel=False,
        pillar_radius_range=_MEASURED.get('end_spike', {}).get('pillar_radius_range'),
        height_range=_MEASURED.get('end_spike', {}).get('height_range'),
        caged_fraction=_MEASURED.get('end_spike', {}).get('caged_fraction'),
    ),
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

DEFENSIVE = ('pillager_outpost', 'nether_bastion', 'end_spike')


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
    if form['evidence'] != 'measured':
        return [f"{structure_id} is not measured ({form['evidence']}): the spec "
                f"forbids guessing a footprint, so it cannot be certified"]
    if form['span_samples'] is None or form['vertical_clearance'] is None:
        return [f'{structure_id} measurement is incomplete']
    return []


def certify(structure_ids) -> dict:
    """Does this set of placed structures satisfy the current objective contract?"""
    ids = list(structure_ids)
    problems = []
    for sid in ids:
        problems.extend(missing_evidence(sid))
    for required in DEFENSIVE:
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
    explicitly not required, and neither is lateral (W-E) proximity.
    """
    ordered = [s for s in MIDLINE_TO_FOUNTAIN if s in placements]
    problems = []
    for near, far in zip(ordered, ordered[1:]):
        if placements[near] <= placements[far]:
            problems.append(f'{near} must lie closer to the midline than {far} '
                            f'({placements[near]:.3f} vs {placements[far]:.3f})')
    return problems


def site_requirements(structure_id: str) -> dict:
    """What a candidate socket must physically provide for this objective.

    Separated per objective on purpose. Collapsing these into one span would
    hide the fact that the End Spike's binding constraint is 103 blocks of sky
    over an 11-block footprint, while the Bastion's is 506 columns of ground
    contact and a multilevel interior.
    """
    form = FORMS.get(structure_id)
    if form is None or form['evidence'] != 'measured':
        return {'known': False, 'why': missing_evidence(structure_id)}
    return {
        'known': True,
        'span_samples': form['span_samples'],
        'vertical_clearance': form['vertical_clearance'],
        'ground_contact_columns': form['ground_contact_columns'],
        'interior_navigation': form['interior_navigation'],
        'multilevel': form.get('multilevel', False),
        'needs': list(form['needs']),
    }
