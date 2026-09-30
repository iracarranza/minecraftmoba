"""Which hostile Mob Swarms a map's own ecology can support.

Decided 29 September 2026 (docs/reconciliation/2026-09-29-opportunity-fields.md):
a swarm has biome eligibility and temporal eligibility, and an opportunity owns
its day and night tables. The runtime picks the definition when the opportunity
manifests, from the biome under the chosen locus and the time of day. This side
answers only whether a CELL is ecologically capable of hosting one.

MACHINERY, NOT CALIBRATION. Sizes, bands and the biome-share threshold are
declared fixtures. Nothing here sets a difficulty tier or a value.

Only swarms that need no new runtime behavior are listed, matching
`SwarmDefinitions.java`. Sand Creeper and Infested Slime wait on the swarm
behavior listener; listing them would derive sources the runtime would refuse.

The biome groups live in fixtures/swarm-biome-groups.json, which the plugin's
`BiomeGroups` is checked against, so the two sides read the same categories.
"""
from __future__ import annotations

import json
from pathlib import Path

FIXTURE = Path(__file__).resolve().parent.parent / 'fixtures' / 'swarm-biome-groups.json'


def biome_groups() -> dict[str, frozenset]:
    data = json.loads(FIXTURE.read_text())['groups']
    return {name: frozenset(ids) for name, ids in data.items()}


# Mirrors SwarmDefinitions.DEFAULTS. `id` is the definition id the runtime
# resolves; `kind` is the RenewableKinds vocabulary entry the source carries.
SWARMS = {
    'mountain_ravager': {
        'kind': 'ravagers', 'groups': ('mountain',), 'time': 'DAY',
        'band': 'deeper', 'size': 1, 'radius': 32, 'recover_ticks': 18000,
    },
}

# NON-CANON FIXTURE: the share of a cell's sampled columns that must lie in the
# swarm's ecology. Below it the cell is a fringe, and an absent opportunity is a
# real answer.
MIN_BIOME_SHARE = 0.25


def biome_share(cell: dict, groups: tuple) -> float:
    """Fraction of the cell's sampled columns whose biome is in these groups.

    `regional_character` is opportunity_map's top-three biome counts, so the
    denominator is the sampled columns those three cover, and a cell dominated
    by a fourth biome under-reports rather than over-reports.
    """
    table = biome_groups()
    admitted = set().union(*(table[g] for g in groups))
    counts = cell.get('regional_character') or cell.get('biomes') or {}
    total = sum(counts.values())
    if not total:
        return 0.0
    return sum(n for b, n in counts.items() if b in admitted) / total


def supports(cell: dict, swarm_id: str) -> bool:
    spec = SWARMS[swarm_id]
    return biome_share(cell, spec['groups']) >= MIN_BIOME_SHARE
