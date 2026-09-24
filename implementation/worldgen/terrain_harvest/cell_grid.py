"""Strategic Depth and Regional Character, per analysis cell.

The layer four other seams are waiting on. Natural resource placement validity,
the Opening Hinterland floor, practical traversability and the bounded
authorability verdict all need to know, for a given piece of ground, how far it
is from each team and what kind of country it is.

STRATEGIC DEPTH IS NOT RADIAL DISTANCE. maps.md says so directly:

    Strategic Depth is not simply radial distance from a Fountain. Existing
    effective-distance factors include terrain, elevation, usable paths, food,
    Hunger, movement methods, cargo, classes, and infrastructure.

So depth here is terrain-weighted traversal cost from each team's Fountain over
the same graph that sited the team structures, authored the Routes and produced
the travel matrix. Reusing that graph is the point: a cell's depth and a Route's
chosen crossing then agree by construction instead of by coincidence.

It is TEAM-RELATIVE and reported per team, never averaged. A cell deep for
north and shallow for south is the ordinary case, not an error to smooth away,
and the two numbers are what make it legible.

NO BANDS, NO THRESHOLDS. maps.md marks the depth gradient and regional tables
OPEN and warns that "a depth band is an analytical grouping, not an authored
resource polygon", that candidate density and defensibility are "not monotonic
with depth", and that "empty or weak deep terrain is legitimate". This emits
costs and character; it does not classify shallow from deep, and nothing
downstream may read a band out of it until one is chosen from evidence.

REGIONAL CHARACTER is the cell's own biome composition, taken from the
characterization rather than recomputed, so there is one measurement of it.
"""
from __future__ import annotations

from vanilla_search.task_a import Terrain, shortest


def _nearest_node(terrain: Terrain, x: int, z: int) -> int:
    return min(range(terrain.n),
               key=lambda i: (terrain.coords[i][0] - x) ** 2
                             + (terrain.coords[i][1] - z) ** 2)


def build(candidate: dict, characterization: dict, fountains: dict) -> dict:
    """Attach per-team traversal cost and biome character to each measured cell.

    `fountains` maps team to world [x, y, z]; only x and z are used.
    """
    cells = (characterization or {}).get('opportunity') or []
    if not cells or not fountains:
        return {'measured': False,
                'why': 'no characterized cells' if not cells else 'no fountains',
                'cells': []}

    terrain = Terrain(candidate)
    costs = {}
    for team, xyz in fountains.items():
        home = _nearest_node(terrain, xyz[0], xyz[2])
        costs[team], _ = shortest(terrain.adj, {home: 0.0})

    out = []
    for cell in cells:
        cx, cz = cell.get('sampled_centroid') or cell['world_origin']
        node = _nearest_node(terrain, cx, cz)
        depth = {}
        for team, table in costs.items():
            value = table.get(node)
            # Unreachable is a fact about the cell, not a missing measurement.
            depth[team] = None if value is None or value == float('inf') else round(value, 2)
        out.append({
            'cell': cell['cell'],
            'world_origin': cell['world_origin'],
            'centroid': [cx, cz],
            # Team-relative, never averaged.
            'strategic_depth_cost': depth,
            'regional_character': cell.get('biomes') or {},
            'candidate_density': cell.get('candidate_density'),
            'regenerative_vocabulary': cell.get('regenerative_vocabulary') or [],
            'mean_surface_y': cell.get('mean_surface_y'),
            # CARRY THE OPPORTUNITY COUNTS THROUGH.
            #
            # This enrichment dropped `ore`, `vegetation`, `fauna` and
            # `hostiles`, keeping only the derived summaries. Every seam that
            # reads a cell reads those four: `opening_floor` asks whether a
            # verb can begin and `resource_validity` asks how much is here.
            # Without them the floor found no ore, no animals and no mobs
            # anywhere and blocked 10 verbs -- five per team, i.e. everything
            # -- on two maps that had just compiled to READY. A barren map and
            # a dropped field look identical downstream.
            'ore': cell.get('ore') or {},
            'vegetation': cell.get('vegetation') or {},
            'fauna': cell.get('fauna') or {},
            'hostiles': cell.get('hostiles') or {},
        })

    reachable = [c for c in out if all(v is not None for v in c['strategic_depth_cost'].values())]
    return {
        'measured': True,
        'cells': out,
        'reachable_cells': len(reachable),
        'unreachable_cells': len(out) - len(reachable),
        'units': 'terrain-weighted traversal cost from each team Fountain, over the '
                 'same graph that sited structures and authored Routes',
        'bands': 'none. maps.md marks the depth gradient OPEN and warns that a depth '
                 'band is an analytical grouping rather than an authored polygon; no '
                 'shallow/deep classification is asserted here.',
        'not_covered': [
            'Practical Reach during play, which Routes and infrastructure change',
            'food, cargo, class and movement-method effects on effective distance',
            'whether a cell is defensible, which is not monotonic with depth',
        ],
    }
