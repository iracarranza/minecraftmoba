"""Resource-packing analysis: can the map hold everything canon asks it to?

maps.md has a section titled "Next spatial analysis" requesting exactly this,
and listing what needs territory: homelands, six authored Route branches,
villages, four livestock ranges, horses, crop patches, forests, sand, gravel,
ordinary and exceptional geology, major and minor POIs, and snow ecology.

That list is answered **per cell** rather than in aggregate, because a global
percentage hides the thing that actually bites. Forty percent usable land is
useless if it is all on one side of the map, and two consumers wanting the same
cell is a collision that an area total will never show.

Method: consume the measured opportunity map, classify each cell by what it can
host, then fit the declared consumers into cells greedily by suitability and
report what does not fit and why.

**It fits consumers; it does not choose them.** Consumer counts come from
config and default to the list maps.md names. Canon marks final scale and
separation unresolved, and the superseded 1300x1000 and 900-1100 fountain
figures are recorded for comparison rather than treated as requirements.
"""
from __future__ import annotations
import argparse
import json
import math
from pathlib import Path

from .materialize import json_write

# What maps.md lists, with footprints as ANALYTICAL FIXTURES. Canon gives the
# list and the separation concern; it does not give sizes, so these are
# explicit placeholders rather than derived values.
CONSUMERS = [
    # `separation` is between instances of the SAME consumer. Cross-type spacing
    # is `clearance`, which only stops footprints overlapping: a crop patch
    # should sit near a homeland, not 400 blocks from it.
    {'id': 'homeland',          'count': 2,  'span': 72, 'needs': 'development', 'separation': 400, 'priority': 0},
    {'id': 'aether_fountain', 'priority': 0,   'count': 2,  'span': 16, 'needs': 'development', 'separation': 400},
    {'id': 'end_tower', 'priority': 1,         'count': 2,  'span': 14, 'needs': 'buildable',   'separation': 120},
    {'id': 'nether_bastion', 'priority': 1,    'count': 2,  'span': 16, 'needs': 'buildable',   'separation': 120},
    {'id': 'pillager_outpost', 'priority': 1,  'count': 2,  'span': 10, 'needs': 'buildable',   'separation': 120},
    {'id': 'village', 'priority': 2,           'count': 2,  'span': 48, 'needs': 'development', 'separation': 200},
    {'id': 'livestock_range', 'priority': 3,   'count': 8,  'span': 40, 'needs': 'development', 'separation': 100},
    {'id': 'horse_range', 'priority': 3,       'count': 2,  'span': 40, 'needs': 'development', 'separation': 150},
    {'id': 'crop_patch', 'priority': 4,        'count': 12, 'span': 24, 'needs': 'development', 'separation': 60},
    {'id': 'major_poi', 'priority': 2,         'count': 4,  'span': 48, 'needs': 'buildable',   'separation': 250},
    {'id': 'minor_poi', 'priority': 3,         'count': 10, 'span': 24, 'needs': 'buildable',   'separation': 120},
    {'id': 'mining_worksite', 'priority': 2,   'count': 6,  'span': 32, 'needs': 'extraction',  'separation': 150},
]


def classify(cell, columns_per_cell):
    """What can this cell host? Measured, not assigned."""
    sampled = max(1, cell['sampled_columns'])
    farmable = cell['farmable_surface_samples'] / sampled
    water = cell['water_surface_samples'] / sampled
    ore = sum(cell['ore'].values())
    roles = set()
    # Thresholds are fixtures. They express "mostly land you can build on" and
    # "enough ore to be worth siting on", not a balance decision.
    if farmable >= 0.35: roles.add('development')
    if farmable >= 0.15 and water < 0.5: roles.add('buildable')
    if ore >= 500: roles.add('extraction')
    return {
        'roles': sorted(roles),
        'farmable_fraction': round(farmable, 3),
        'water_fraction': round(water, 3),
        'ore_blocks': ore,
        'fauna': sum(cell['fauna'].values()),
        'vegetation': sum(cell['vegetation'].values()),
    }


def free_spot(info, consumer, cell_size):
    """A position inside the cell that clears what the cell already holds."""
    ox, oz = info['world_origin']
    half = consumer['span'] / 2
    step = max(8, consumer['span'] // 2)
    for dz in range(int(half), cell_size - int(half) + 1, step):
        for dx in range(int(half), cell_size - int(half) + 1, step):
            x, z = ox + dx, oz + dz
            if all(math.dist((x, z), (px, pz)) >= (consumer['span'] + pspan) / 2 + 4
                   for px, pz, pspan in info.setdefault('occupied', [])):
                return (x, z)
    return None


def fit(cells, cell_size, consumers, clearance):
    """Greedy placement, most demanding first.

    Separation applies between instances of the SAME consumer; different
    consumers only need footprint clearance. Applying the larger of two
    separations to every pair made a homeland's radius exclude the whole map,
    which produced a spurious does-not-fit rather than a measurement.
    """
    placed, unplaced = [], []
    taken = []                       # (x, z, consumer_id, separation)
    # Priority first: a fountain matters more than a crop patch and must not
    # starve merely because its footprint is smaller.
    order = sorted(consumers, key=lambda c: (c.get('priority', 9), -c['span']))
    for consumer in order:
        for n in range(consumer['count']):
            spot = None
            for key, info in sorted(cells.items(),
                                    key=lambda kv: (-kv[1]['farmable_fraction'], -kv[1]['ore_blocks'])):
                if consumer['needs'] not in info['roles']: continue
                if consumer['span'] > cell_size: break      # never fits in one cell
                # Usable area, not the whole cell: a cell that is half cliff
                # cannot host as much as one that is all meadow.
                if info['remaining_area'] < consumer['span'] ** 2: continue
                # Position within the cell, not at its origin. Placing every
                # consumer at the origin put them all 0 blocks apart, so any two
                # in one cell always conflicted and the area budget did nothing.
                spot_xy = free_spot(info, consumer, cell_size)
                if spot_xy is None: continue
                wx, wz = spot_xy
                blocked = False
                for tx, tz, tid, tsep, tspan in taken:
                    # Same type: the declared separation. Different types: only
                    # enough room for both footprints, since a crop patch beside
                    # a livestock range is adjacency, not a conflict.
                    required = (consumer['separation'] if tid == consumer['id']
                                else (consumer['span'] + tspan) / 2 + clearance)
                    if math.dist((wx, wz), (tx, tz)) < required: blocked = True; break
                if blocked: continue
                spot = (key, wx, wz, info)
                break
            if spot is None:
                same = sum(1 for t in taken if t[2] == consumer['id'])
                unplaced.append({'id': consumer['id'], 'instance': n + 1,
                                 'needs': consumer['needs'], 'span': consumer['span'],
                                 'separation': consumer['separation'],
                                 'already_placed_of_this_type': same,
                                 'reason': 'no free cell with role "' + consumer['needs']
                                           + '" outside this type\'s own separation radius'})
                continue
            key, wx, wz, info = spot
            taken.append((wx, wz, consumer['id'], consumer['separation'], consumer['span']))
            info['remaining_area'] -= consumer['span'] ** 2
            info.setdefault('occupied', []).append((wx, wz, consumer['span']))
            placed.append({'id': consumer['id'], 'instance': n + 1, 'cell': list(key),
                           'world_origin': [wx, wz], 'role_used': consumer['needs'],
                           'farmable_fraction': info['farmable_fraction'],
                           'ore_blocks': info['ore_blocks']})
            if info['remaining_area'] <= 0: cells.pop(key, None)
    return placed, unplaced


def run(opportunity: Path, output: Path, consumers=None):
    data = json.loads(opportunity.read_text())
    cell_size = data['cell_size_blocks']
    consumers = consumers or CONSUMERS
    classified = {}
    for c in data['cells']:
        info = {**classify(c, cell_size), 'world_origin': c['world_origin']}
        # Usable area within the cell, from the measured farmable fraction.
        info['remaining_area'] = int(cell_size * cell_size * max(info['farmable_fraction'], 0.05))
        classified[tuple(c['cell'])] = info

    role_counts = {r: sum(1 for v in classified.values() if r in v['roles'])
                   for r in ('development', 'buildable', 'extraction')}
    demanded = sum(c['count'] for c in consumers)
    # Margin between unlike neighbours, on top of half of each footprint.
    clearance = 8
    placed, unplaced = fit(dict(classified), cell_size, consumers, clearance)

    result = {
        'schema': 'terrain_packing_analysis/1',
        'evidence_state': 'DERIVED MEASUREMENT',
        'source_opportunity_map': str(opportunity),
        'volume_id': data.get('volume_id'),
        'cell_size_blocks': cell_size,
        'cells_measured': len(classified),
        'cells_by_role': role_counts,
        'consumers_demanded': demanded,
        'cross_type_margin_blocks': clearance,
        'cross_type_rule': 'half of each footprint plus the margin; same-type pairs use '
                           'the declared separation instead',
        'placed': placed,
        'unplaced': unplaced,
        'fits': not unplaced,
        'note': 'Footprints, separations and counts are ANALYTICAL FIXTURES. maps.md supplies the '
                'list of consumers and the concern about separation; it does not supply sizes, and '
                'final scale remains unresolved there.',
        'superseded_reference': {
            'earlier_map_target': '1300 x 1000 blocks',
            'earlier_fountain_separation': '900-1100 blocks',
            'status': 'explicitly superseded in maps.md; recorded for comparison only',
        },
        'not_covered': [
            'terrain shape within a cell; a cell is treated as uniform',
            'vertical space, caves and overhangs',
            'whether a consumer is desirable where it fits',
            'route corridors, which are linear and not cell-shaped',
            'placement order is by declared priority, which is an authoring choice',
        ],
    }
    json_write(output, result)
    return result


if __name__ == '__main__':
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('--opportunity', type=Path, required=True)
    p.add_argument('--output', type=Path, required=True)
    a = p.parse_args()
    r = run(a.opportunity.resolve(), a.output.resolve())
    print(f"cells {r['cells_measured']} by role {r['cells_by_role']}")
    print(f"consumers demanded {r['consumers_demanded']}, placed {len(r['placed'])}, "
          f"unplaced {len(r['unplaced'])}")
    print("FITS" if r['fits'] else "DOES NOT FIT")
    for u in r['unplaced'][:8]:
        print(f"  unplaced {u['id']} #{u['instance']} needs={u['needs']} sep={u['separation']}")
